package com.fantasy.cpu;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Very small assembler for the Fantasy CPU instruction format used in tests.
 *
 * Supported features (minimal):
 * - labels ("label:")
 * - instructions: NOP, MOV, ADD, SUB, CMP, JMP, JCOND, etc...
 * - operand syntax: Rn, [Rn], $hhhh
 * - words and extensions written in BIG-ENDIAN
 */
public class Assembler {
    private Map<String,Integer> labels = new HashMap<>();
    private List<Line> lines = new ArrayList<>();
    // record data ranges (start, length) to emit into a small embedded map
    private List<int[]> dataRanges = new ArrayList<>();
    private List<String> dataRangeTypes = new ArrayList<>();
    private boolean inDataSection = false;  // track if we're in .data section

    private static class Line {
        String label;
        String op;
        String[] ops;
        int addr;
        boolean isDirective;
        String sourceFile;
        int sourceLine;
        String sourceSnippet;
    }
    // track currently parsing source context to produce richer errors
    private String currentSourceFile = null;
    private int currentSourceLine = -1;
    private String currentSourceSnippet = null;

    public void parse(Path asmFile) throws IOException {
        List<String> src = Files.readAllLines(asmFile);
        int loc = 0;
        this.currentSourceFile = asmFile.toString();
    boolean sawEnd = false;
    boolean sawStart = false;
    boolean sawOrg = false;
    // allowed directives (lower-case)
    final Set<String> allowedDirectives = new HashSet<>(Arrays.asList(
        ".org", ".start", ".data", ".end", ".byte", ".bytes", ".ascii", ".word"
    ));
        for (String raw : src) {
            this.currentSourceLine++;
            // remove inline comments starting with ';'
            int ci = raw.indexOf(';');
            String s = (ci >= 0) ? raw.substring(0, ci).trim() : raw.trim();
            if (sawEnd && !s.isEmpty()) {
                throw new AssembleException("Content after .end is not allowed", currentSourceFile, currentSourceLine, -1, s);
            }
            if (s.isEmpty()) continue;
            Line ln = new Line();
            // If the line starts with a dot it *may* be a directive (.org, .word, .data, .end, etc.)
            // It can also be a label that begins with a dot (e.g. .start: produced by the disassembler).
            // Rules:
            // - If the first token ends with ':' it's a label EXCEPT when the bare token (without ':')
            //   is one of the allowed directives (e.g. '.data:') — in that case we treat it as a misuse
            //   of the directive form and throw an error telling the user to use '.data' (no ':').
            // - If the first token does NOT end with ':' it's a directive and must be one of the allowed set.
            if (s.startsWith(".")) {
                String[] dotTokens = s.split("\\s+",2);
                String firstDot = dotTokens[0];
                boolean endsWithColon = firstDot.endsWith(":");
                String baseToken = endsWithColon ? firstDot.substring(0, firstDot.length()-1).toLowerCase() : firstDot.toLowerCase();

                if (endsWithColon) {
                    // Using ':' with an allowed directive is an error; any other dot-prefixed token with ':' is also an error
                    if (allowedDirectives.contains(baseToken)) {
                        throw new AssembleException(String.format("Directive should not use trailing ':'; use '%s' not '%s:'", baseToken, baseToken), currentSourceFile, currentSourceLine, -1, s);
                    }
                    throw new AssembleException("Unknown dot-prefixed token: " + firstDot + " (allowed: " + allowedDirectives + ")", currentSourceFile, currentSourceLine, -1, s);
                } else {
                    // It's a directive; ensure it's allowed
                    if (!allowedDirectives.contains(baseToken)) {
                        throw new AssembleException("Unknown directive: " + firstDot + " (allowed: " + allowedDirectives + ")", currentSourceFile, currentSourceLine, -1, s);
                    }

                    ln.addr = loc;
                    // store the snippet (trimmed content after removing comments and label)
                    ln.sourceSnippet = s;
                    ln.isDirective = true;
                    ln.op = baseToken; // already lower-case
                    if (dotTokens.length > 1) {
                        String rest = dotTokens[1].trim();
                        if (ln.op.equals(".ascii")) {
                            ln.ops = new String[] { rest };
                        } else {
                            ln.ops = rest.split("\\s*,\\s*");
                        }
                    } else {
                        ln.ops = new String[0];
                    }

                    // handle specific directives affecting location and uniqueness
                    switch (ln.op) {
                    case ".org": {
                        if (ln.ops.length >= 1) {
                            String a = ln.ops[0];
                            int v = parseAddressOrLabelToken(a, ln);
                            loc = v;
                            ln.addr = loc;
                            sawOrg = true;
                        }
                        break;
                    }
                    case ".word": {
                        loc += 2 * ln.ops.length;
                        break;
                    }
                    case ".byte": case ".bytes": {
                        loc += ln.ops.length;
                        break;
                    }
                    case ".ascii": {
                        if (ln.ops.length >= 1) {
                            String sld = ln.ops[0].trim();
                            if (sld.startsWith("\"") && sld.endsWith("\"")) sld = sld.substring(1, sld.length()-1);
                            loc += sld.getBytes().length;
                            ln.ops = new String[] { sld };
                            ln.sourceSnippet = sld;
                        }
                        break;
                    }
                    case ".data": {
                        if (inDataSection) {
                            throw new AssembleException("Only one .data section is allowed", currentSourceFile, currentSourceLine, -1, s);
                        }
                        inDataSection = true;
                        break;
                    }
                    case ".start": {
                        if (sawOrg) {
                            throw new AssembleException("Directive .start is not allowed after .org (use .org to set start)", currentSourceFile, currentSourceLine, -1, s);
                        }
                        if (sawStart) {
                            throw new AssembleException("Only one .start is allowed", currentSourceFile, currentSourceLine, -1, s);
                        }
                        sawStart = true;
                        break;
                    }
                    case ".end": {
                        // stop parsing; require program to explicitly end with .end
                        if (sawEnd) {
                            throw new AssembleException("Only one .end is allowed", currentSourceFile, currentSourceLine, -1, s);
                        }
                        sawEnd = true;
                        lines.add(ln);
                        lines.add(null); // sentinel
                        return;
                    }
                    default: {
                        break;
                    }
                    }
                    lines.add(ln);
                    continue;
                }
            }
            // label (non-directive)
            if (s.contains(":")) {
                String[] parts = s.split(":",2);
                ln.label = parts[0].trim();
                // validate and store labels case-insensitively
                if (!isValidLabelName(ln.label)) {
                    throw new AssembleException("Invalid label name: '" + ln.label + "' (allowed: letters, digits, underscore, dash)", this.currentSourceFile, this.currentSourceLine, -1, s);
                }
                labels.put(ln.label.toLowerCase(), loc);
                ln.sourceFile = this.currentSourceFile;
                ln.sourceLine = this.currentSourceLine;
                s = parts[1].trim();
                if (s.isEmpty()) { lines.add(ln); continue; }
                // If the remainder after a label starts with a dot it may be a directive
                if (s.startsWith(".")) {
                    String[] dotTokens = s.split("\\s+",2);
                    String firstDot = dotTokens[0];
                    boolean endsWithColon = firstDot.endsWith(":");
                    String baseToken = endsWithColon ? firstDot.substring(0, firstDot.length()-1).toLowerCase() : firstDot.toLowerCase();

                    if (endsWithColon) {
                        if (allowedDirectives.contains(baseToken)) {
                            throw new AssembleException(String.format("Directive should not use trailing ':'; use '%s' not '%s:'", baseToken, baseToken), currentSourceFile, currentSourceLine, -1, s);
                        }
                        throw new AssembleException("Unknown dot-prefixed token: " + firstDot + " (allowed: " + allowedDirectives + ")", currentSourceFile, currentSourceLine, -1, s);
                    } else {
                        if (!allowedDirectives.contains(baseToken)) {
                            throw new AssembleException("Unknown directive: " + firstDot + " (allowed: " + allowedDirectives + ")", currentSourceFile, currentSourceLine, -1, s);
                        }
                        ln.addr = loc;
                        ln.sourceSnippet = s;
                        ln.isDirective = true;
                        ln.op = baseToken;
                        if (dotTokens.length > 1) {
                            String rest = dotTokens[1].trim();
                            if (ln.op.equals(".ascii")) ln.ops = new String[] { rest };
                            else ln.ops = rest.split("\\s*,\\s*");
                        } else ln.ops = new String[0];

                        switch (ln.op) {
                        case ".org": {
                            if (ln.ops.length >= 1) {
                                String a = ln.ops[0];
                                int v = parseNumber(a);
                                loc = v;
                                ln.addr = loc;
                                sawOrg = true;
                            }
                            break;
                        }
                        case ".word": { loc += 2 * ln.ops.length; break; }
                        case ".byte": case ".bytes": { loc += ln.ops.length; break; }
                        case ".ascii": {
                            if (ln.ops.length >= 1) {
                                String sld = ln.ops[0].trim();
                                if (sld.startsWith("\"") && sld.endsWith("\"")) sld = sld.substring(1, sld.length()-1);
                                loc += sld.getBytes().length;
                                ln.ops = new String[] { sld };
                                ln.sourceSnippet = sld;
                            }
                            break;
                        }
                        case ".data": { if (inDataSection) throw new AssembleException("Only one .data section is allowed", currentSourceFile, currentSourceLine, -1, s); inDataSection = true; break; }
                        case ".start": { if (sawOrg) throw new AssembleException("Directive .start is not allowed after .org (use .org to set start)", currentSourceFile, currentSourceLine, -1, s); if (sawStart) throw new AssembleException("Only one .start is allowed", currentSourceFile, currentSourceLine, -1, s); sawStart = true; break; }
                        case ".end": { if (sawEnd) throw new AssembleException("Only one .end is allowed", currentSourceFile, currentSourceLine, -1, s); sawEnd = true; lines.add(ln); lines.add(null); return; }
                        default: break;
                        }
                        lines.add(ln);
                        continue;
                    }
                }
            }
            // instruction, directive, or operands
            String[] tokens = s.split("\\s+",2);
            String first = tokens[0];
            ln.addr = loc;
            // store the snippet (trimmed content after removing comments and label)
            ln.sourceSnippet = s;
            ln.sourceFile = this.currentSourceFile;
            ln.sourceLine = this.currentSourceLine;

            // instruction + operands
            ln.op = first.toUpperCase();
            if (tokens.length > 1) ln.ops = tokens[1].split("\\s*,\\s*"); else ln.ops = new String[0];
            // Immediate values (literals) can be written as:
            //  - hexadecimal: '0xABCD'
            //  - decimal: '1234'
            //  - binary: '%101010' (use '%' prefix only)
            // Addresses still use '$' (e.g. '$ABCD') and labels remain identifiers.

            // Reject any use of invalid legacy characters in source: immediates must be written as 0x, %, or decimal.
            if (ln.ops != null) {
                for (String opToken : ln.ops) {
                    String oc = opToken.split(";")[0].trim();
                            if (oc.contains("#")) {
                                throw new AssembleException("Invalid immediate syntax in operand '" + oc + "' (use 0x..., %, or decimal for immediates; use $ADDR or label for addresses)", this.currentSourceFile, this.currentSourceLine, ln.addr, ln.sourceSnippet);
                            }
                }
            }


            // instruction size: base word + possibly extensions
            loc += 2;
            int opcode = opcodeOf(ln.op.split("\\.")[0], ln);
            // Special-case JCOND: ops[0] is a condition mnemonic (no extension), ops[1] is the target
            if (opcode == Cpu.JCOND) {
                if (ln.ops.length >= 2) loc += 2; // target absolute extension
            } else {
                    for (String o : ln.ops) {
                        String opclean = o.split(";")[0].trim();
                        // Immediate or absolute literal ($..., 0x..., %..., decimal) require an extension word
                        if (opclean.startsWith("$") || opclean.startsWith("0x") || opclean.startsWith("0X") || opclean.startsWith("%") || opclean.matches("[0-9]+")) {
                            loc += 2;
                            continue;
                        }
                        // Bare identifiers (labels) will be treated as absolute addresses and need an extension
                        if (!opclean.isEmpty() && !opclean.startsWith("R") && !opclean.startsWith("[")) {
                            loc += 2;
                        }
                    }
            }
            lines.add(ln);
        }
        // After processing all lines, require explicit .end directive
        if (!sawEnd) {
            throw new AssembleException("Program must end with .end directive", currentSourceFile, currentSourceLine, -1, null);
        }
    }

    // encode and write to binary file (big-endian words)
    public void assemble(Path outFile) throws IOException {
        // Emit a packed image: write only actual chunks (no zero padding). Record .org directives as length-0 ranges.
        class Chunk { int start; ByteArrayOutputStream baos = new ByteArrayOutputStream(); int len() { return baos.size(); } }
        List<Chunk> chunks = new ArrayList<>();

        // reset recorded data ranges; we'll rebuild them from chunks and explicit directives
        dataRanges.clear();
        dataRangeTypes.clear();
        inDataSection = false;  // reset data section flag

        for (Line ln : lines) {
            if (ln == null) break; // sentinel
            this.currentSourceFile = ln.sourceFile;
            this.currentSourceLine = ln.sourceLine;
            this.currentSourceSnippet = ln.sourceSnippet;
            if (ln.op == null) continue; // label-only

            // (we may create or reuse a chunk below when needed)

            Chunk cur = null;
            if (!chunks.isEmpty()) {
                Chunk last = chunks.get(chunks.size()-1);
                if (last.start + last.len() == ln.addr) cur = last;
            }

            if (ln.isDirective) {
                switch (ln.op) {
                    case ".word":
                        // ensure we have a chunk to append directive bytes; create one if needed
                        if (cur == null) {
                            cur = new Chunk(); cur.start = ln.addr; chunks.add(cur);
                        }
                        // If we're in data section, create typed data range entry
                        if (inDataSection) {
                            dataRanges.add(new int[] { ln.addr, ln.ops.length * 2 });
                            dataRangeTypes.add(".word");
                        }
                        for (String v : ln.ops) {
                            int val = parseNumber(v);
                            cur.baos.write((val >> 8) & 0xFF);
                            cur.baos.write(val & 0xFF);
                        }
                        break;
                    case ".byte":
                        if (cur == null) { cur = new Chunk(); cur.start = ln.addr; chunks.add(cur); }
                        // If we're in data section, create typed data range entry
                        if (inDataSection) {
                            dataRanges.add(new int[] { ln.addr, ln.ops.length });
                            dataRangeTypes.add(".byte");
                        }
                        for (String v : ln.ops) {
                            String tok = v.trim();
                            if (tok.startsWith("$")) {
                                throw new AssembleException(String.format("Invalid .byte operand '%s' at $%04X: use numeric literal or label", v, ln.addr), currentSourceFile, ln.sourceLine, ln.addr, ln.sourceSnippet);
                            }
                            if (tok.contains("#")) {
                                throw new AssembleException(String.format("Invalid .byte operand '%s' at $%04X: invalid immediate syntax", v, ln.addr), currentSourceFile, ln.sourceLine, ln.addr, ln.sourceSnippet);
                            }
                            int val = parseNumber(tok);
                            if ((val & 0xFF) != val) throw new AssembleException(String.format("Immediate %s value 0x%X too large for .byte at $%04X", v, val, ln.addr), currentSourceFile, ln.sourceLine, ln.addr, ln.sourceSnippet);
                            cur.baos.write(val & 0xFF);
                        }
                        break;
                    case ".ascii":
                        if (ln.ops.length >= 1) {
                            if (cur == null) { cur = new Chunk(); cur.start = ln.addr; chunks.add(cur); }
                            String sld = ln.ops[0];
                            if (sld.startsWith("\"") && sld.endsWith("\"")) sld = sld.substring(1, sld.length()-1);
                            // If we're in data section, create typed data range entry
                            if (inDataSection) {
                                dataRanges.add(new int[] { ln.addr, sld.getBytes().length });
                                dataRangeTypes.add(".ascii");
                            }
                            cur.baos.write(sld.getBytes());
                        }
                        break;
                    case ".org":
                        // record explicit .org entry in the map (length 0, type .org)
                        if (ln.ops.length >= 1) {
                            int addr = parseAddressOrLabelToken(ln.ops[0], ln);
                            dataRanges.add(new int[] { addr, 0 });
                            dataRangeTypes.add(".org");
                        }
                        break;
                    case ".data":
                        // Mark that we're now in a data section
                        inDataSection = true;
                        break;
                    default:
                        break;
                }
                continue;
            }

            // non-directive: ensure we have an active chunk at ln.addr
            if (cur == null) {
                Chunk nc = new Chunk();
                nc.start = ln.addr;
                // Only add implicit .org if we don't already have a range starting at this address
                boolean hasRangeAtAddr = false;
                for (int[] range : dataRanges) {
                    if (range[0] == nc.start) {
                        hasRangeAtAddr = true;
                        break;
                    }
                }
                if ((!chunks.isEmpty() || nc.start != 0) && !hasRangeAtAddr) {
                    dataRanges.add(new int[] { nc.start, 0 });
                    dataRangeTypes.add(".org");
                }
                chunks.add(nc);
                cur = nc;
            }

            int instr = encodeInstr(ln);
            boolean isByte = ln.op != null && ln.op.toUpperCase().contains(".B");
            cur.baos.write((instr >> 8) & 0xFF);
            cur.baos.write(instr & 0xFF);

            String[] ops = ln.ops != null ? ln.ops : new String[0];
            if (opcodeOf(ln.op.split("\\.")[0], ln) == Cpu.JCOND) {
                if (ops.length >= 2 && operandNeedsExt(ops[1])) {
                    int v = operandValue(ops[1], ln);
                    cur.baos.write((v >> 8) & 0xFF);
                    cur.baos.write(v & 0xFF);
                }
                continue;
            }

            String srcText = null, dstText = null;
            if ((opcodeOf(ln.op.split("\\.")[0], ln) == Cpu.JMP || opcodeOf(ln.op.split("\\.")[0], ln) == Cpu.CALL) && ops.length == 1) {
                dstText = ops[0];
            } else {
                if (ops.length >= 1) srcText = ops[0];
                if (ops.length >= 2) dstText = ops[1];
            }

            if (srcText != null && operandNeedsExt(srcText)) {
                int v = operandValue(srcText, ln);
                Parsed ps = parseOperand(srcText);
                if (isByte && ps.mode == Cpu.MODE_IMM) validateImmediateSize(srcText, v, 8, ln.addr, ln.sourceSnippet);
                cur.baos.write((v >> 8) & 0xFF);
                cur.baos.write(v & 0xFF);
            }
            if (dstText != null && operandNeedsExt(dstText)) {
                int v = operandValue(dstText, ln);
                Parsed pd = parseOperand(dstText);
                if (isByte && pd.mode == Cpu.MODE_IMM) validateImmediateSize(dstText, v, 8, ln.addr, ln.sourceSnippet);
                cur.baos.write((v >> 8) & 0xFF);
                cur.baos.write(v & 0xFF);
            }
        }

        // Before emitting, record the map entries in the correct order for physical reconstruction.
        // 1. .org markers (length 0, no physical space)
        // 2. chunks (define physical layout) 
        // 3. data type entries (override code interpretation for specific ranges)
        
        List<int[]> tempRanges = new ArrayList<>(dataRanges);
        List<String> tempTypes = new ArrayList<>(dataRangeTypes);
        
        // Clear and rebuild in the correct order
        dataRanges.clear();
        dataRangeTypes.clear();
        
        // Add .org markers first (they have length 0 so don't consume physical space)
        for (int i = 0; i < tempRanges.size(); i++) {
            if (".org".equals(tempTypes.get(i))) {
                dataRanges.add(tempRanges.get(i));
                dataRangeTypes.add(tempTypes.get(i));
            }
        }
        
        // Add chunks (they occupy physical space in order)
        for (Chunk c : chunks) {
            dataRanges.add(new int[] { c.start, c.len() });
            dataRangeTypes.add(".chunk");
        }
        
        // Add data type entries (for correct disassembly of data portions)
        for (int i = 0; i < tempRanges.size(); i++) {
            if (!".org".equals(tempTypes.get(i)) && !".chunk".equals(tempTypes.get(i))) {
                dataRanges.add(tempRanges.get(i));
                dataRangeTypes.add(tempTypes.get(i));
            }
        }        // Emit chunks to file in packed form
        ByteArrayOutputStream finalOut = new ByteArrayOutputStream();
        for (Chunk c : chunks) {
            finalOut.write(c.baos.toByteArray());
        }
        Files.write(outFile, finalOut.toByteArray());
        appendMapIfRequested(outFile);
    }

    // After writing the main image, optionally append a 512-byte map block and overwrite file.
    // New map format (no count): sequence of entries, each 4 bytes: start(2), lenField(2).
    // lenField: top 2 bits = type (00=.byte, 01=.word, 10=.ascii, 11=reserved), low 14 bits = length in bytes.
    // The list is terminated by an entry 0x0000 0x0000. The whole footer is padded with zeros to 512 bytes.
    private void appendMapIfRequested(Path outFile) throws IOException {
        // Only append if we have data ranges
        if (dataRanges.isEmpty()) return;
        byte[] existing = Files.readAllBytes(outFile);
        ByteArrayOutputStream map = new ByteArrayOutputStream();
        final int footerSize = 512;
        // Emit entries in the same order they were recorded, without merging. This keeps
        // zero-length .org markers separate and avoids accidental combination of distant
        // ranges into a single oversized entry. Each recorded range must fit into 14 bits
        // length unless it's an .org (len==0) which will be encoded with type==3 (0xC000).
        for (int i = 0; i < dataRanges.size(); i++) {
            int start = dataRanges.get(i)[0] & 0xFFFF;
            int len = dataRanges.get(i)[1] & 0xFFFF;
            String t = (i < dataRangeTypes.size()) ? dataRangeTypes.get(i) : "";
            int typeCode;
            if (".byte".equals(t)) typeCode = 0;
            else if (".word".equals(t)) typeCode = 1;
            else if (".ascii".equals(t)) typeCode = 2;
            else if (".org".equals(t)) typeCode = 3; // encode explicit .org markers as reserved/type 3
            else typeCode = 3; // default reserved

            if (".org".equals(t)) {
                // explicit .org is encoded as type==3 and length==0 -> lenField == 0xC000
                int lenField = 0xC000;
                if (map.size() + 4 + 4 > footerSize) break;
                map.write((start >> 8) & 0xFF);
                map.write(start & 0xFF);
                map.write((lenField >> 8) & 0xFF);
                map.write(lenField & 0xFF);
                continue;
            }

            if (len > 0x3FFF) {
                // If a recorded chunk is larger than the 14-bit limit, split it into
                // multiple consecutive entries so each fits.
                int remaining = len;
                int chunkStart = start;
                while (remaining > 0) {
                    int take = Math.min(remaining, 0x3FFF);
                    int lenField = ((typeCode & 0x3) << 14) | (take & 0x3FFF);
                    if (map.size() + 4 + 4 > footerSize) break;
                    map.write((chunkStart >> 8) & 0xFF);
                    map.write(chunkStart & 0xFF);
                    map.write((lenField >> 8) & 0xFF);
                    map.write(lenField & 0xFF);
                    chunkStart += take;
                    remaining -= take;
                }
                continue;
            }

            int lenField = ((typeCode & 0x3) << 14) | (len & 0x3FFF);
            if (map.size() + 4 + 4 > footerSize) break;
            map.write((start >> 8) & 0xFF);
            map.write(start & 0xFF);
            map.write((lenField >> 8) & 0xFF);
            map.write(lenField & 0xFF);
        }
        // terminator
        map.write(0);
        map.write(0);
        map.write(0);
        map.write(0);

        byte[] mapBytes = map.toByteArray();
        if (mapBytes.length == 0) return; // shouldn't happen
        // write existing + mapBytes, pad to footerSize
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(existing);
        out.write(mapBytes);
        while (out.size() < existing.length + footerSize) out.write(0);
        Files.write(outFile, out.toByteArray());
    }

    private int parseNumber(String s) {
        s = s.trim();
        // Accept only these forms for numeric literals:
    // - hexadecimal: leading '0x' or leading '$' (caller may strip '$' before calling), e.g. 0x1F or DEAD
    // - binary: leading '%' (e.g. %1010)
    // - decimal: plain digits
    // Also accept labels (identifiers) resolved from the symbol table.
        if (s.isEmpty()) {
            throw new AssembleException("Empty numeric literal", currentSourceFile, currentSourceLine, -1, currentSourceSnippet);
        }
    // If it's a label, return its address (labels are stored lower-case)
    if (labels.containsKey(s.toLowerCase())) return labels.get(s.toLowerCase());

        // Leading '$' is allowed for hexadecimal addresses: $DEAD or $0xDEAD
        if (s.startsWith("$")) {
            String ss = s.substring(1);
            if (ss.startsWith("0x") || ss.startsWith("0X")) return Integer.parseInt(ss.substring(2), 16) & 0xFFFF;
            return Integer.parseInt(ss, 16) & 0xFFFF;
        }

        // Hex with 0x prefix
        if (s.startsWith("0x") || s.startsWith("0X")) {
            return Integer.parseInt(s.substring(2), 16) & 0xFFFF;
        }
        // Binary with '%' prefix (new RULES): %101010
        if (s.startsWith("%")) {
            return Integer.parseInt(s.substring(1), 2) & 0xFFFF;
        }
        // Hex when caller used $ (we may get a token like 'DEAD') -> but to be strict we only accept $-prefixed addresses
        // The callers should strip '$' before calling parseNumber; if we get bare hex-like string, attempt decimal only.
        // Decimal
        if (s.matches("[0-9]+")) {
            return Integer.parseInt(s, 10) & 0xFFFF;
        }
        // fallback: not recognized
    throw new AssembleException("Invalid numeric literal: " + s + " (expected decimal digits, 0x..., %..., or a label)", currentSourceFile, currentSourceLine, -1, currentSourceSnippet);
    }

    private boolean isValidLabelName(String lab) {
        if (lab == null || lab.isEmpty()) return false;
        // Allow letters, digits, underscore and dash. Must start with letter or underscore.
        char c = lab.charAt(0);
        if (!String.valueOf(c).matches("[A-Za-z_]")) return false;
        return lab.matches("[A-Za-z0-9_\\-]+");
    }

    /**
     * Parse a token that must be either an address in the form `$HEX` (1-4 hex digits)
     * or a label. Returns the resolved integer address or throws AssembleException.
     */
    private int parseAddressOrLabelToken(String token, Line ln) {
    if (token == null) throw new AssembleException("Empty address token", ln.sourceFile, ln.sourceLine, -1, ln.sourceSnippet);
        String t = token.trim();
        if (t.startsWith("$")) {
            String hex = t.substring(1).trim();
            if (!hex.matches("[0-9A-Fa-f]{1,4}")) {
                throw new AssembleException("Invalid address format '" + t + "' (expected $ followed by 1-4 hex digits)", ln.sourceFile, ln.sourceLine, -1, ln.sourceSnippet);
            }
            return Integer.parseInt(hex, 16) & 0xFFFF;
        }
        // Not a $-address; must be a label
        if (!isValidLabelName(t)) {
            throw new AssembleException("Invalid address token '" + t + "' (expected $HEX or valid label)", ln.sourceFile, ln.sourceLine, -1, ln.sourceSnippet);
        }
        if (!labels.containsKey(t.toLowerCase())) {
            throw new AssembleException("Undefined label: " + t, ln.sourceFile, ln.sourceLine, -1, ln.sourceSnippet);
        }
        return labels.get(t.toLowerCase());
    }

    private void validateImmediateSize(String token, int val, int sizeBits, int addr, String snippet) {
        if (sizeBits == 8) {
            if ((val & 0xFF) != val) {
                throw new AssembleException(String.format("Immediate %s value 0x%X too large for .B at $%04X", token, val, addr), currentSourceFile, currentSourceLine, addr, snippet);
            }
        } else if (sizeBits == 16) {
            if ((val & 0xFFFF) != val) {
                throw new AssembleException(String.format("Immediate %s value 0x%X too large for .W at $%04X", token, val, addr), currentSourceFile, currentSourceLine, addr, snippet);
            }
        }
    }

    private boolean operandNeedsExt(String op) {
        op = op.split(";")[0].trim();
            if (op.isEmpty()) return false;
            // immediates (0x, %, or decimal)
            if (op.startsWith("0x") || op.startsWith("0X") || op.startsWith("%") || op.matches("[0-9]+")) return true;
        if (op.startsWith("$")) return true;
        // labels (bare identifiers) are absolute addresses and need an extension (case-insensitive)
        if (labels.containsKey(op.toLowerCase())) return true;
        return false;
    }

    private int operandValue(String op, Line ln) {
        op = op.split(";")[0].trim();
        // immediates must be written using the allowed literal forms; detect and reject legacy/invalid characters
        if (op.contains("#")) {
            throw new AssembleException("Invalid immediate syntax in operand '" + op + "' (use 0x..., %..., or decimal for immediates; use $ADDR or label for addresses)", ln.sourceFile, ln.sourceLine, -1, ln.sourceSnippet);
        }
        // Immediate forms: 0x..., %binary, or decimal digits
        if (op.startsWith("0x") || op.startsWith("0X") || op.startsWith("%") || op.matches("[0-9]+")) {
            return parseNumber(op);
        }
        // Not an immediate: $address or label
        if (op.startsWith("$")) {
            return parseAddressOrLabelToken(op, ln);
        }
    // If the token is a label, return its address
    if (labels.containsKey(op.toLowerCase())) return labels.get(op.toLowerCase());
        throw new AssembleException("Unsupported operand value: " + op + " (expected immediate like 0x..., %b..., decimal; or $address; or label)", ln.sourceFile, ln.sourceLine, -1, ln.sourceSnippet);
    }

    private int regIndex(String token) {
        token = token.split(";")[0].trim().toUpperCase();
        if (token.startsWith("R")) return Integer.parseInt(token.substring(1).trim());
    throw new AssembleException("Invalid register: " + token, currentSourceFile, currentSourceLine, -1, currentSourceSnippet);
    }

    private int encodeInstr(Line ln) {
        String opRaw = ln.op;
        String op = opRaw.split("\\.")[0]; // strip .W/.B suffix if present
        String[] ops = ln.ops;
    int opcode = opcodeOf(op, ln);
    // Default size: 1 = word (16-bit). Disassembler emits no ".W" (word is default),
    // so assembler must also treat instructions as word-sized by default.
    int size = 1;
    if (opRaw.toUpperCase().contains(".B")) size = 0;
    else if (opRaw.toUpperCase().contains(".W")) size = 1;
        int srcMode = 0, srcReg = 0, dstMode = 0, dstReg = 0;

        // Validation: enforce operand counts/types per opcode and provide clear errors with address
        int expectedMin = 0, expectedMax = 2;
        switch (opcode) {
            case Cpu.NOP: expectedMin = expectedMax = 0; break;
            case Cpu.MOV: case Cpu.ADD: case Cpu.SUB: case Cpu.CMP:
            case Cpu.AND: case Cpu.OR: case Cpu.XOR: case Cpu.TST:
            case Cpu.MULU: case Cpu.DIVU: case Cpu.MUL: case Cpu.DIV:
            case Cpu.BTST: case Cpu.BSET: case Cpu.BCLR:
                expectedMin = expectedMax = 2; break;
            case Cpu.INC: case Cpu.DEC: case Cpu.NEG: case Cpu.NOT:
                expectedMin = expectedMax = 1; break;
            case Cpu.ROL: case Cpu.ROR: case Cpu.SHL: case Cpu.SHR:
                expectedMin = expectedMax = 1; break;
            case Cpu.PUSH: case Cpu.POP:
                expectedMin = expectedMax = 1; break;
            case Cpu.JMP: case Cpu.CALL:
                expectedMin = expectedMax = 1; break;
            case Cpu.RET: case Cpu.RETI:
                expectedMin = expectedMax = 0; break;
            case Cpu.JCOND:
                expectedMin = expectedMax = 2; break;
            default: expectedMin = 0; expectedMax = 2; break;
        }
        if (ln.ops == null) ln.ops = new String[0];
    if (ln.ops.length < expectedMin || ln.ops.length > expectedMax) {
        throw new AssembleException(String.format("%s at $%04X expects %d..%d operands, got %d",
            ln.op, ln.addr, expectedMin, expectedMax, ln.ops.length), ln.sourceFile, ln.sourceLine, ln.addr);
    }

        // Special handling for JCOND: first operand is a condition mnemonic (e.g. EQ),
        // encoded as cond_code = (cond_msb<<3) | cond_low3; size bit becomes cond_msb,
        // src_mode should be immediate (11) but there is NO extension for the cond.
        if (opcode == Cpu.JCOND) {
            String cond = ops[0].trim().toUpperCase();
            int condCode = condCodeFromName(cond, ln);
            size = (condCode >> 3) & 1;
            srcReg = condCode & 7;
            srcMode = Cpu.MODE_IMM; // per spec, but no extension emitted for cond

            // dst is target address (abs)
            dstMode = Cpu.MODE_ABS; dstReg = 0;
            // If the JCOND target was provided as an immediate and size==.B, validate it fits
            if (ops != null && ops.length >= 2) {
                String tgt = ops[1].split(";")[0].trim();
                if (size == 0) {
                    Parsed pt = parseOperand(tgt);
                    if (pt.mode == Cpu.MODE_IMM) {
                        int vv = operandValue(tgt, ln);
                        validateImmediateSize(tgt, vv, 8, ln.addr, ln.sourceSnippet);
                    }
                }
            }
            return ((opcode & 0x1F) << 11) | ((size & 1) << 10) | ((srcMode & 3) << 8) | ((srcReg & 7) << 5) | ((dstMode & 3) << 3) | (dstReg & 7);
        }

        // Special-case single-operand JMP/CALL: operand is the destination (target)
        if ((opcode == Cpu.JMP || opcode == Cpu.CALL) && ops.length == 1) {
            Parsed p = parseOperand(ops[0]);
            // JMP/CALL destination must not be an immediate value; allow ABS, IND, REG
            if (p.mode == Cpu.MODE_IMM) {
                throw new AssembleException("JMP/CALL destination must be an address or register/indirect, not an immediate: " + ops[0], ln.sourceFile, ln.sourceLine, ln.addr);
            }
            dstMode = p.mode; dstReg = p.reg;
        } else if (ops.length == 1) {
            // Default: single-operand instructions are destination-only (INC/DEC/NEG/NOT/ROL/...)
            // Exception: PUSH is source-only
            if (opcode == Cpu.PUSH) {
                Parsed p = parseOperand(ops[0]);
                srcMode = p.mode; srcReg = p.reg;
            } else {
                Parsed p = parseOperand(ops[0]);
                dstMode = p.mode; dstReg = p.reg;
            }
        } else {
            if (ops.length >= 1) {
                Parsed p = parseOperand(ops[0]);
                srcMode = p.mode; srcReg = p.reg;
            }
            if (ops.length >= 2) {
                Parsed p = parseOperand(ops[1]);
                dstMode = p.mode; dstReg = p.reg;
            }
    // Centralized validation: if instruction size is .B (size==0), any immediate operand
    // must fit in 8 bits. Immediates must be written as 0x..., decimal, or %binary.
        if (ops != null && size == 0) {
            for (String tok : ops) {
                String tclean = tok.split(";")[0].trim();
                Parsed pt = parseOperand(tclean);
                if (pt.mode == Cpu.MODE_IMM) {
                    int vv = operandValue(tclean, ln);
                    validateImmediateSize(tclean, vv, 8, ln.addr, ln.sourceSnippet);
                }
            }
        }
        }
        return ((opcode & 0x1F) << 11) | ((size & 1) << 10) | ((srcMode & 3) << 8) | ((srcReg & 7) << 5) | ((dstMode & 3) << 3) | (dstReg & 7);
    }

    private int condCodeFromName(String cond, Line ln) {
        switch (cond) {
            case "AL": return 0x0;
            case "EQ": return 0x1;
            case "NE": return 0x2;
            case "CS": return 0x3;
            case "CC": return 0x4;
            case "MI": return 0x5;
            case "PL": return 0x6;
            case "VS": return 0x7;
            case "VC": return 0x8;
            case "GT": return 0x9;
            case "GE": return 0xA;
            case "LT": return 0xB;
            case "LE": return 0xC;
            case "HI": return 0xD;
            case "HS": return 0xE;
            case "LO": return 0xF;
        }
        throw new AssembleException("Unknown JCOND condition: " + cond, ln.sourceFile, ln.sourceLine, -1, ln.sourceSnippet);
    }

    private static class Parsed { int mode; int reg; }

    private Parsed parseOperand(String token) {
        token = token.split(";")[0].trim();
        Parsed p = new Parsed();
        // immediates may be written as 0x..., decimal, or %binary
            if (token.startsWith("0x") || token.startsWith("0X") || token.startsWith("%") || token.matches("[0-9]+")) { p.mode = Cpu.MODE_IMM; p.reg = 0; return p; }
        if (token.startsWith("$")) { p.mode = Cpu.MODE_ABS; p.reg = 0; return p; }
    // bare label/address (e.g. START) -> absolute (case-insensitive)
    if (labels.containsKey(token.toLowerCase())) { p.mode = Cpu.MODE_ABS; p.reg = 0; return p; }
        if (token.startsWith("[") && token.endsWith("]")) {
            String inner = token.substring(1, token.length()-1);
            p.mode = Cpu.MODE_IND; p.reg = regIndex(inner); return p;
        }
        // register direct
        p.mode = Cpu.MODE_REG; p.reg = regIndex(token); return p;
    }

    private int opcodeOf(String op, Line ln) {
        switch (op) {
            case "NOP": return Cpu.NOP;
            case "MOV": return Cpu.MOV;
            case "ADD": return Cpu.ADD;
            case "SUB": return Cpu.SUB;
            case "CMP": return Cpu.CMP;
            case "INC": return Cpu.INC;
            case "DEC": return Cpu.DEC;
            case "NEG": return Cpu.NEG;
            case "JMP": return Cpu.JMP;
            case "TST": return Cpu.TST;
            case "AND": return Cpu.AND;
            case "OR": return Cpu.OR;
            case "XOR": return Cpu.XOR;
            case "NOT": return Cpu.NOT;
            case "RETI": return Cpu.RETI;
            case "MULU": return Cpu.MULU;
            case "DIVU": return Cpu.DIVU;
            case "PUSH": return Cpu.PUSH;
            case "POP": return Cpu.POP;
            case "CALL": return Cpu.CALL;
            case "RET": return Cpu.RET;
            case "MUL": return Cpu.MUL;
            case "DIV": return Cpu.DIV;
            case "ROL": return Cpu.ROL;
            case "ROR": return Cpu.ROR;
            case "SHL": return Cpu.SHL;
            case "SHR": return Cpu.SHR;
            case "BTST": return Cpu.BTST;
            case "BSET": return Cpu.BSET;
            case "BCLR": return Cpu.BCLR;
            case "JCOND": return Cpu.JCOND;
        }
        throw new AssembleException("Unsupported opcode: " + op, ln.sourceFile, ln.sourceLine, -1, ln.sourceSnippet);
    }

    // Simple CLI
    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--debug-parse")) {
            System.err.println("Usage: Assembler --debug-parse <input.asm>");
            System.exit(2);
        }
        if (args.length == 2 && args[0].equals("--debug-parse")) {
            Assembler a = new Assembler();
            a.parse(Path.of(args[1]));
            System.out.println("Labels:");
            for (Map.Entry<String,Integer> e : a.labels.entrySet()) {
                System.out.printf("  %s -> $%04X\n", e.getKey(), e.getValue());
            }
            System.out.println("Lines:");
            for (Line ln : a.lines) {
                System.out.printf("  addr=$%04X op=%s ops=%s label=%s\n", ln.addr, ln.op, Arrays.toString(ln.ops), ln.label);
            }
            return;
        }
        if (args.length != 2) {
            System.err.println("Usage: Assembler <input.asm> <output.bin>");
            System.exit(2);
        }
        Assembler a = new Assembler();
        a.parse(Path.of(args[0]));
        a.assemble(Path.of(args[1]));
    }
}
