package com.fantasy.cpu;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Disassembler {
    private static final int OPCODE_COLUMN = 24;
    private static final int COMMENT_COLUMN = 56;

    public static String disassemble(byte[] data) {
        List<int[]> dataRangesFromMap = new ArrayList<>();
        List<Integer> dataRangeTypes = new ArrayList<>();
        if (data.length >= 512) {
            int mapStart = data.length - 512;
            int off = mapStart;
            while (off + 3 < data.length) {
                int start = ((data[off] & 0xFF) << 8) | (data[off+1] & 0xFF);
                int lenField = ((data[off+2] & 0xFF) << 8) | (data[off+3] & 0xFF);
                off += 4;
                if (start == 0 && lenField == 0) break;
                int type = (lenField >> 14) & 0x3;
                int size = lenField & 0x3FFF;
                dataRangesFromMap.add(new int[] { start, size });
                dataRangeTypes.add(type);
            }
            if (!dataRangesFromMap.isEmpty()) {
                // reconstruct a full 64K image
                byte[] mem = new byte[0x10000];
                int srcOff = 0;
                for (int i = 0; i < dataRangesFromMap.size(); i++) {
                    int start = dataRangesFromMap.get(i)[0];
                    int len = dataRangesFromMap.get(i)[1];
                    if (len > 0) {
                        if (srcOff + len > mapStart) break; // malformed
                        System.arraycopy(data, srcOff, mem, start, len);
                        srcOff += len;
                    }
                }
                data = mem;
            }
        }

        // First pass: collect branch targets (JMP/CALL/JCOND)
        Set<Integer> targets = new LinkedHashSet<>();
        if (!dataRangesFromMap.isEmpty()) {
            // If a footer map exists, only scan code ranges (type==3) to collect branch targets.
            for (int ri = 0; ri < dataRangesFromMap.size(); ri++) {
                int[] r = dataRangesFromMap.get(ri);
                int rs = r[0], rl = r[1];
                int rtype = (ri < dataRangeTypes.size()) ? dataRangeTypes.get(ri) : 3;
                if (rl <= 0) continue;
                if (rtype != 3) continue; // only code ranges contain instructions
                int end = rs + rl;
                int p = rs;
                while (p + 1 < end && p + 1 < data.length) {
                    int instr = ((data[p] & 0xFF) << 8) | (data[p+1] & 0xFF);
                    p += 2;
                    int opcode = (instr >> 11) & 0x1F;
                    int srcMode = (instr >> 8) & 3;
                    int dstMode = (instr >> 3) & 3;
                    if (opcode == Cpu.JCOND) {
                        if (p + 1 >= end || p + 1 >= data.length) break;
                        int ext = ((data[p] & 0xFF) << 8) | (data[p+1] & 0xFF);
                        p += 2;
                        targets.add(ext);
                    } else {
                        if (srcMode == Cpu.MODE_ABS || srcMode == Cpu.MODE_IMM) {
                            if (p + 1 >= end || p + 1 >= data.length) break;
                            p += 2;
                        }
                        if (dstMode == Cpu.MODE_ABS || dstMode == Cpu.MODE_IMM) {
                            if (p + 1 >= end || p + 1 >= data.length) break;
                            int ext = ((data[p] & 0xFF) << 8) | (data[p+1] & 0xFF);
                            p += 2;
                            if (opcode == Cpu.JMP || opcode == Cpu.CALL) targets.add(ext);
                        }
                    }
                }
            }
        } else {
            int pc = 0;
            while (pc + 1 < data.length) {
                int instr = ((data[pc] & 0xFF) << 8) | (data[pc+1] & 0xFF);
                pc += 2;
                int opcode = (instr >> 11) & 0x1F;
                int srcMode = (instr >> 8) & 3;
                int dstMode = (instr >> 3) & 3;
                if (opcode == Cpu.JCOND) {
                    if (pc + 1 >= data.length) break;
                    int ext = ((data[pc] & 0xFF) << 8) | (data[pc+1] & 0xFF);
                    pc += 2;
                    targets.add(ext);
                } else {
                    if (srcMode == Cpu.MODE_ABS || srcMode == Cpu.MODE_IMM) {
                        if (pc + 1 >= data.length) break;
                        pc += 2;
                    }
                    if (dstMode == Cpu.MODE_ABS || dstMode == Cpu.MODE_IMM) {
                        if (pc + 1 >= data.length) break;
                        int ext = ((data[pc] & 0xFF) << 8) | (data[pc+1] & 0xFF);
                        pc += 2;
                        if (opcode == Cpu.JMP || opcode == Cpu.CALL) targets.add(ext);
                    }
                }
            }
        }

        // assign labels
        Map<Integer,String> labelMap = new HashMap<>();
        for (int t : targets) labelMap.put(t, String.format("L_%04X", t));
        // also generate data labels D_XXXX for map-declared ranges so branches may reference them
        // but only for actual data ranges (type 0, 1, 2), not code ranges (type 3)
        for (int ri = 0; ri < dataRangesFromMap.size(); ri++) {
            int[] r = dataRangesFromMap.get(ri);
            int rtype = (ri < dataRangeTypes.size()) ? dataRangeTypes.get(ri) : 3;
            int a = r[0];
            if (rtype != 3) { // Only create D_xxxx labels for data ranges, not code ranges
                String dlab = String.format("D_%04X", a);
                if (!labelMap.containsKey(a)) labelMap.put(a, dlab);
            }
        }

        // Second pass: emit disassembly
        StringBuilder sb = new StringBuilder();
        if (!dataRangesFromMap.isEmpty()) {
            // When footer map is present, emit assembly per map entry
            boolean emittedDataSection = false;
            for (int ri = 0; ri < dataRangesFromMap.size(); ri++) {
                int[] r = dataRangesFromMap.get(ri);
                int rs = r[0], rl = r[1];
                int rtype = (ri < dataRangeTypes.size()) ? dataRangeTypes.get(ri) : 3;
                if (rl == 0 && rtype == 3) {
                    // .org marker - skip here, we'll emit it with the corresponding code section
                    continue;
                }
                if (rl <= 0) continue;
                switch (rtype) {
                    case 2: { // .ascii
                        if (!emittedDataSection) {
                            sb.append(".data").append(System.lineSeparator());
                            emittedDataSection = true;
                        }
                        int pc_ascii = rs;
                        
                        // Emit D_XXXX label if it exists for this address
                        if (labelMap.containsKey(pc_ascii)) {
                            sb.append(labelMap.get(pc_ascii)).append(":").append(System.lineSeparator());
                        }
                        
                        int avail = rl;
                        
                        // Check if this looks like a quoted string that was stored literally
                        boolean startsWithQuote = (avail > 0 && (data[pc_ascii] & 0xFF) == 0x22);
                        boolean hasLiteralHex = false;
                        
                        if (startsWithQuote && avail >= 6) {
                            // Look for pattern like ", 0x" which suggests literal hex was stored as text
                            for (int i = 1; i < avail - 4; i++) {
                                if ((data[pc_ascii + i] & 0xFF) == 0x22 && // quote
                                    (data[pc_ascii + i + 1] & 0xFF) == 0x2C && // comma
                                    (data[pc_ascii + i + 2] & 0xFF) == 0x20 && // space
                                    (data[pc_ascii + i + 3] & 0xFF) == 0x30 && // '0'
                                    (data[pc_ascii + i + 4] & 0xFF) == 0x78) { // 'x'
                                    hasLiteralHex = true;
                                    break;
                                }
                            }
                        }
                        
                        int run = 0;
                        StringBuilder str = new StringBuilder();
                        
                        if (startsWithQuote && hasLiteralHex) {
                            // Handle incorrectly encoded string: extract actual string content and hex values
                            int i = 1; // skip opening quote
                            while (i < avail && (data[pc_ascii + i] & 0xFF) != 0x22) {
                                str.append((char)(data[pc_ascii + i] & 0xFF));
                                i++;
                            }
                            run = i + 1; // include closing quote
                            
                            // Skip the literal ", 0x00" part for now and just add the hex value
                            sb.append("    ");
                            int afterMnem = sb.length();
                            int pad = OPCODE_COLUMN - afterMnem; if (pad < 2) pad = 2; for (int k = 0; k < pad; k++) sb.append(' ');
                            sb.append(".ascii \"").append(str.toString()).append("\", 0x00");
                            run = avail; // consume all bytes since we're handling it specially
                        } else {
                            // Normal ASCII string handling
                            while (run < avail) {
                                int b = data[pc_ascii + run] & 0xFF;
                                if (b >= 0x20 && b <= 0x7E) run++; else break;
                            }
                            
                            // Check if we should include trailing null bytes or other terminators
                            int totalRun = run;
                            if (run < avail) {
                                int b = data[pc_ascii + run] & 0xFF;
                                if (b == 0x00) totalRun++; // include null terminator
                            }
                            
                            for (int j = 0; j < run; j++) {
                                int b = data[pc_ascii + j] & 0xFF;
                                char c = (char) b;
                                if (c == '\\' || c == '"') str.append('\\').append(c); else str.append(c);
                            }
                            
                            sb.append("    ");
                            int afterMnem = sb.length();
                            int pad = OPCODE_COLUMN - afterMnem; if (pad < 2) pad = 2; for (int k = 0; k < pad; k++) sb.append(' ');
                            sb.append(".ascii \"").append(str.toString()).append("\"");
                            
                            // Add null terminator if present
                            if (totalRun > run) {
                                sb.append(", 0x00");
                                run = totalRun;
                            }
                        }
                        int lineLastNl = sb.lastIndexOf("\n");
                        int lstart = (lineLastNl == -1) ? 0 : lineLastNl + 1;
                        int llen = sb.length() - lstart;
                        int p2 = COMMENT_COLUMN - llen; if (p2 <= 1) sb.append(' '); else for (int k = 0; k < p2; k++) sb.append(' ');
                        sb.append("; $").append(String.format("%04X", pc_ascii)).append(" :");
                        
                        // Multi-line hex comment for long sequences
                        final int HEX_PER_LINE = 8; // Max hex values per line
                        for (int j = 0; j < run; j++) {
                            if (j > 0 && j % HEX_PER_LINE == 0) {
                                // Start new line for continuation
                                sb.append(System.lineSeparator());
                                // Add padding to align with comment column
                                for (int pad = 0; pad < COMMENT_COLUMN; pad++) sb.append(' ');
                                sb.append(";       :"); // Empty address field, just spacing to align hex values
                            }
                            sb.append(" 0x").append(String.format("%02X", data[pc_ascii + j] & 0xFF));
                        }
                        sb.append(System.lineSeparator());
                        break;
                    }
                    case 1: { // .word
                        if (!emittedDataSection) {
                            sb.append(".data").append(System.lineSeparator());
                            emittedDataSection = true;
                        }
                        int pc_word = rs;
                        
                        // Emit D_XXXX label if it exists for this address
                        if (labelMap.containsKey(pc_word)) {
                            sb.append(labelMap.get(pc_word)).append(":").append(System.lineSeparator());
                        }
                        
                        int words = rl / 2;
                        // Generate single .word directive with all words
                        sb.append("    ");
                        int afterMnem2 = sb.length();
                        int pad2 = OPCODE_COLUMN - afterMnem2; if (pad2 < 2) pad2 = 2; for (int k = 0; k < pad2; k++) sb.append(' ');
                        sb.append(".word ");
                        for (int j = 0; j < words; j++) {
                            if (j > 0) sb.append(", ");
                            int w = ((data[pc_word + j*2] & 0xFF) << 8) | (data[pc_word + j*2 + 1] & 0xFF);
                            sb.append(String.format("0x%04X", w & 0xFFFF));
                        }
                        sb.append(System.lineSeparator());
                        pc_word += words * 2;
                        int left = rl - words * 2;
                        if (left > 0) {
                            for (int j = 0; j < left; j++) {
                                sb.append("  ");
                                int afterMnem3 = sb.length();
                                int pad3 = OPCODE_COLUMN - afterMnem3; if (pad3 < 2) pad3 = 2; for (int k = 0; k < pad3; k++) sb.append(' ');
                                sb.append(".byte ").append(String.format("0x%02X", data[pc_word + j] & 0xFF)).append(System.lineSeparator());
                            }
                        }
                        break;
                    }
                    case 0: // .byte
                    default: {
                        if (!emittedDataSection) {
                            sb.append(".data").append(System.lineSeparator());
                            emittedDataSection = true;
                        }
                        
                        // Emit D_XXXX label if it exists for this address
                        if (labelMap.containsKey(rs)) {
                            sb.append(labelMap.get(rs)).append(":").append(System.lineSeparator());
                        }
                        
                        // Generate single .byte directive with all bytes  
                        sb.append("    ");
                        int afterMnem2 = sb.length();
                        int pad2 = OPCODE_COLUMN - afterMnem2; if (pad2 < 2) pad2 = 2; for (int k = 0; k < pad2; k++) sb.append(' ');
                        sb.append(".byte ");
                        for (int j = 0; j < rl; j++) {
                            if (j > 0) sb.append(", ");
                            sb.append(String.format("0x%02X", data[rs + j] & 0xFF));
                        }
                        sb.append(System.lineSeparator());
                        break;
                    }
                    case 3: { // code
                        int pc_code = rs;
                        int end = rs + rl;
                        
                        // Check if there's an .org marker for this code section
                        for (int oi = 0; oi < dataRangesFromMap.size(); oi++) {
                            int[] orgRange = dataRangesFromMap.get(oi);
                            int orgType = (oi < dataRangeTypes.size()) ? dataRangeTypes.get(oi) : 3;
                            if (orgRange[1] == 0 && orgType == 3 && orgRange[0] == rs) {
                                // Found .org marker for this code section
                                sb.append(String.format(".org $%04X", rs)).append(System.lineSeparator());
                                break;
                            }
                        }
                        
                        // Build list of data ranges to skip (types 0, 1, 2)
                        List<int[]> dataRangesToSkip = new ArrayList<>();
                        for (int di = 0; di < dataRangesFromMap.size(); di++) {
                            int dtype = (di < dataRangeTypes.size()) ? dataRangeTypes.get(di) : 3;
                            if (dtype != 3) { // not code, so it's data
                                int[] drange = dataRangesFromMap.get(di);
                                dataRangesToSkip.add(new int[]{drange[0], drange[0] + drange[1]});
                            }
                        }
                        
                        while (pc_code + 1 < end && pc_code + 1 < data.length) {
                            // Check if current address is covered by a data range
                            boolean inDataRange = false;
                            for (int[] skipRange : dataRangesToSkip) {
                                if (pc_code >= skipRange[0] && pc_code < skipRange[1]) {
                                    // Skip to end of this data range
                                    pc_code = skipRange[1];
                                    inDataRange = true;
                                    break;
                                }
                            }
                            if (inDataRange || pc_code + 1 >= end || pc_code + 1 >= data.length) {
                                continue;
                            }
                            
                            int start = pc_code;
                            // emit label if present
                            if (labelMap.containsKey(pc_code)) {
                                if (sb.length() > 0 && sb.charAt(sb.length()-1) != '\n') sb.append(System.lineSeparator());
                                sb.append(labelMap.get(pc_code)).append(":").append(System.lineSeparator());
                            }

                            int instr = ((data[pc_code] & 0xFF) << 8) | (data[pc_code+1] & 0xFF);
                            pc_code += 2;
                            int opcode = (instr >> 11) & 0x1F;
                            int size = (instr >> 10) & 1;
                            int srcMode = (instr >> 8) & 3;
                            int srcReg = (instr >> 5) & 7;
                            int dstMode = (instr >> 3) & 3;
                            int dstReg = instr & 7;

                            List<Integer> encodedWords = new ArrayList<>();
                            encodedWords.add(instr & 0xFFFF);

                            String opname = opcodeName(opcode);
                            if (opname == null) opname = String.format("OP_%02X", opcode);
                            boolean sizeful = true;
                            switch (opcode) {
                                case Cpu.NOP:
                                case Cpu.JMP:
                                case Cpu.CALL:
                                case Cpu.RET:
                                case Cpu.RETI:
                                case Cpu.JCOND:
                                    sizeful = false;
                                    break;
                                default:
                                    sizeful = true;
                            }
                            if (sizeful && size == 0) opname = opname + ".B";

                            List<String> ops = new ArrayList<>();
                            if (opcode == Cpu.JCOND) {
                                int condCode = ((size << 3) | srcReg) & 0xF;
                                ops.add(condName(condCode));
                                if (pc_code + 1 >= end || pc_code + 1 >= data.length) break;
                                int ext = ((data[pc_code] & 0xFF) << 8) | (data[pc_code+1] & 0xFF);
                                pc_code += 2;
                                encodedWords.add(ext & 0xFFFF);
                                if (labelMap.containsKey(ext)) ops.add(labelMap.get(ext)); else ops.add("$" + String.format("%04X", ext));
                            } else {
                                if (srcMode == Cpu.MODE_ABS || srcMode == Cpu.MODE_IMM) {
                                    if (pc_code + 1 >= end || pc_code + 1 >= data.length) break;
                                    int ext = ((data[pc_code] & 0xFF) << 8) | (data[pc_code+1] & 0xFF);
                                    pc_code += 2;
                                    encodedWords.add(ext & 0xFFFF);
                                    String lit;
                                    if (srcMode == Cpu.MODE_IMM) {
                                        if (size == 0) lit = String.format("0x%02X", ext & 0xFF);
                                        else lit = String.format("0x%04X", ext & 0xFFFF);
                                        ops.add(lit);
                                    } else {
                                        // For absolute addresses, use D_XXXX labels if available
                                        if (labelMap.containsKey(ext)) ops.add(labelMap.get(ext)); 
                                        else ops.add("$" + String.format("%04X", ext & 0xFFFF));
                                    }
                                } else if (srcMode == Cpu.MODE_IND) {
                                    ops.add("[R" + srcReg + "]");
                                } else {
                                    ops.add("R" + srcReg);
                                }
                                if (dstMode == Cpu.MODE_ABS || dstMode == Cpu.MODE_IMM) {
                                    if (pc_code + 1 >= end || pc_code + 1 >= data.length) break;
                                    int ext = ((data[pc_code] & 0xFF) << 8) | (data[pc_code+1] & 0xFF);
                                    pc_code += 2;
                                    encodedWords.add(ext & 0xFFFF);
                                    if (dstMode == Cpu.MODE_IMM) {
                                        if (size == 0) ops.add(String.format("0x%02X", ext & 0xFF)); 
                                        else ops.add(String.format("0x%04X", ext & 0xFFFF));
                                    } else {
                                        // For absolute addresses, use D_XXXX labels if available
                                        if (labelMap.containsKey(ext)) ops.add(labelMap.get(ext)); 
                                        else ops.add("$" + String.format("%04X", ext & 0xFFFF));
                                    }
                                } else if (dstMode == Cpu.MODE_IND) {
                                    ops.add("[R" + dstReg + "]");
                                } else {
                                    ops.add("R" + dstReg);
                                }
                            }

                            // indent and opcode column
                            int lastNl = sb.lastIndexOf("\n");
                            int lineStart = (lastNl == -1) ? 0 : lastNl + 1;
                            int lineLen = sb.length() - lineStart;
                            int padOpc = OPCODE_COLUMN - lineLen;
                            if (padOpc < 2) padOpc = 2;
                            for (int i = 0; i < padOpc; i++) sb.append(' ');
                            sb.append(opname);

                            if (opcode == Cpu.NOP) {
                            } else if (opcode == Cpu.JMP || opcode == Cpu.CALL) {
                                if (ops.size() >= 2) {
                                    if (srcMode == Cpu.MODE_REG && srcReg == 0) sb.append(' ').append(ops.get(1)); else sb.append(' ').append(ops.get(0)).append(", ").append(ops.get(1));
                                } else if (!ops.isEmpty()) sb.append(' ').append(ops.get(ops.size()-1));
                            } else if (opcode == Cpu.PUSH) {
                                if (!ops.isEmpty()) sb.append(' ').append(ops.get(0));
                            } else if (opcode == Cpu.POP) {
                                if (!ops.isEmpty()) sb.append(' ').append(ops.get(ops.size()-1));
                            } else if (opcode == Cpu.JCOND) {
                                if (!ops.isEmpty()) sb.append(' ').append(String.join(", ", ops));
                            } else {
                                switch (opcode) {
                                    case Cpu.INC: case Cpu.DEC: case Cpu.NEG: case Cpu.NOT:
                                    case Cpu.ROL: case Cpu.ROR: case Cpu.SHL: case Cpu.SHR:
                                        if (!ops.isEmpty()) sb.append(' ').append(ops.get(ops.size()-1));
                                        break;
                                    case Cpu.RET: case Cpu.RETI:
                                        break;
                                    default:
                                        if (!ops.isEmpty()) sb.append(' ').append(String.join(", ", ops));
                                        break;
                                }
                            }

                            // append comment
                            int lastNl2 = sb.lastIndexOf("\n");
                            int lstart = (lastNl2 == -1) ? 0 : lastNl2 + 1;
                            int llen = sb.length() - lstart;
                            int p2 = COMMENT_COLUMN - llen; if (p2 <= 1) sb.append(' '); else for (int i = 0; i < p2; i++) sb.append(' ');
                            sb.append("; $").append(String.format("%04X", start)).append(" :");
                            for (int w : encodedWords) sb.append(" 0x").append(String.format("%04X", w & 0xFFFF));
                            sb.append(System.lineSeparator());
                        }
                        break;
                    }
                }
            }
        } else {
            // No footer map: legacy behavior, disassemble from pc=0
            // If the file begins with a long run of zero bytes, prefer emitting a
            // .org directive to reproduce the same padding during re-assembly
            // instead of emitting many NOPs. Find first non-zero byte.
            int firstNonZero = 0;
            while (firstNonZero < data.length && data[firstNonZero] == 0) firstNonZero++;
            // threshold in bytes to prefer .org (avoid noise for small files)
            final int ORG_THRESHOLD = 8;
            int pc = 0;
            if (firstNonZero >= ORG_THRESHOLD && firstNonZero < data.length) {
                sb.append(String.format(".org $%04X", firstNonZero)).append(System.lineSeparator());
                pc = firstNonZero;
            }
            while (pc < data.length) {
                int start = pc;
                // emit label if present
                if (labelMap.containsKey(pc)) {
                    if (sb.length() > 0 && sb.charAt(sb.length()-1) != '\n') sb.append(System.lineSeparator());
                    sb.append(labelMap.get(pc)).append(":").append(System.lineSeparator());
                }

                if (pc + 1 >= data.length) break;
                int instr = ((data[pc] & 0xFF) << 8) | (data[pc+1] & 0xFF);
                pc += 2;
                int opcode = (instr >> 11) & 0x1F;
                int size = (instr >> 10) & 1;
                int srcMode = (instr >> 8) & 3;
                int srcReg = (instr >> 5) & 7;
                int dstMode = (instr >> 3) & 3;
                int dstReg = instr & 7;

                List<Integer> encodedWords = new ArrayList<>();
                encodedWords.add(instr & 0xFFFF);

                String opname = opcodeName(opcode);
                if (opname == null) opname = String.format("OP_%02X", opcode);
                boolean sizeful = true;
                switch (opcode) {
                    case Cpu.NOP:
                    case Cpu.JMP:
                    case Cpu.CALL:
                    case Cpu.RET:
                    case Cpu.RETI:
                    case Cpu.JCOND:
                        sizeful = false;
                        break;
                    default:
                        sizeful = true;
                }
                if (sizeful && size == 0) opname = opname + ".B";

                List<String> ops = new ArrayList<>();
                if (opcode == Cpu.JCOND) {
                    int condCode = ((size << 3) | srcReg) & 0xF;
                    ops.add(condName(condCode));
                    if (pc + 1 >= data.length) break;
                    int ext = ((data[pc] & 0xFF) << 8) | (data[pc+1] & 0xFF);
                    pc += 2;
                    encodedWords.add(ext & 0xFFFF);
                    if (labelMap.containsKey(ext)) ops.add(labelMap.get(ext)); else ops.add("$" + String.format("%04X", ext));
                } else {
                    if (srcMode == Cpu.MODE_ABS || srcMode == Cpu.MODE_IMM) {
                        if (pc + 1 >= data.length) break;
                        int ext = ((data[pc] & 0xFF) << 8) | (data[pc+1] & 0xFF);
                        pc += 2;
                        encodedWords.add(ext & 0xFFFF);
                        String lit;
                        if (srcMode == Cpu.MODE_IMM) {
                            if (size == 0) lit = String.format("0x%02X", ext & 0xFF);
                            else lit = String.format("0x%04X", ext & 0xFFFF);
                            ops.add(lit);
                        } else {
                            // For absolute addresses, use D_XXXX labels if available
                            if (labelMap.containsKey(ext)) ops.add(labelMap.get(ext)); 
                            else ops.add("$" + String.format("%04X", ext & 0xFFFF));
                        }
                    } else if (srcMode == Cpu.MODE_IND) {
                        ops.add("[R" + srcReg + "]");
                    } else {
                        ops.add("R" + srcReg);
                    }
                    if (dstMode == Cpu.MODE_ABS || dstMode == Cpu.MODE_IMM) {
                        if (pc + 1 >= data.length) break;
                        int ext = ((data[pc] & 0xFF) << 8) | (data[pc+1] & 0xFF);
                        pc += 2;
                        encodedWords.add(ext & 0xFFFF);
                        if (dstMode == Cpu.MODE_IMM) {
                            if (size == 0) ops.add(String.format("0x%02X", ext & 0xFF)); 
                            else ops.add(String.format("0x%04X", ext & 0xFFFF));
                        } else {
                            // For absolute addresses, use D_XXXX labels if available
                            if (labelMap.containsKey(ext)) ops.add(labelMap.get(ext)); 
                            else ops.add("$" + String.format("%04X", ext & 0xFFFF));
                        }
                    } else if (dstMode == Cpu.MODE_IND) {
                        ops.add("[R" + dstReg + "]");
                    } else {
                        ops.add("R" + dstReg);
                    }
                }

                // indent and opcode column
                int lastNl = sb.lastIndexOf("\n");
                int lineStart = (lastNl == -1) ? 0 : lastNl + 1;
                int lineLen = sb.length() - lineStart;
                int padOpc = OPCODE_COLUMN - lineLen;
                if (padOpc < 2) padOpc = 2;
                for (int i = 0; i < padOpc; i++) sb.append(' ');
                sb.append(opname);

                if (opcode == Cpu.NOP) {
                } else if (opcode == Cpu.JMP || opcode == Cpu.CALL) {
                    if (ops.size() >= 2) {
                        if (srcMode == Cpu.MODE_REG && srcReg == 0) sb.append(' ').append(ops.get(1)); else sb.append(' ').append(ops.get(0)).append(", ").append(ops.get(1));
                    } else if (!ops.isEmpty()) sb.append(' ').append(ops.get(ops.size()-1));
                } else if (opcode == Cpu.PUSH) {
                    if (!ops.isEmpty()) sb.append(' ').append(ops.get(0));
                } else if (opcode == Cpu.POP) {
                    if (!ops.isEmpty()) sb.append(' ').append(ops.get(ops.size()-1));
                } else if (opcode == Cpu.JCOND) {
                    if (!ops.isEmpty()) sb.append(' ').append(String.join(", ", ops));
                } else {
                    switch (opcode) {
                        case Cpu.INC: case Cpu.DEC: case Cpu.NEG: case Cpu.NOT:
                        case Cpu.ROL: case Cpu.ROR: case Cpu.SHL: case Cpu.SHR:
                            if (!ops.isEmpty()) sb.append(' ').append(ops.get(ops.size()-1));
                            break;
                        case Cpu.RET: case Cpu.RETI:
                            break;
                        default:
                            if (!ops.isEmpty()) sb.append(' ').append(String.join(", ", ops));
                            break;
                    }
                }

                // append comment
                int lastNl2 = sb.lastIndexOf("\n");
                int lstart = (lastNl2 == -1) ? 0 : lastNl2 + 1;
                int llen = sb.length() - lstart;
                int p2 = COMMENT_COLUMN - llen; if (p2 <= 1) sb.append(' '); else for (int i = 0; i < p2; i++) sb.append(' ');
                sb.append("; $").append(String.format("%04X", start)).append(" :");
                for (int w : encodedWords) sb.append(" 0x").append(String.format("%04X", w & 0xFFFF));
                sb.append(System.lineSeparator());
            }
        }

        // Ensure the disassembly explicitly terminates with .end so the assembler
        // enforces program termination and round-trips correctly.
        if (sb.length() == 0 || sb.charAt(sb.length()-1) != '\n') sb.append(System.lineSeparator());
        sb.append(".end").append(System.lineSeparator());
        return sb.toString();
    }

    private static String opcodeName(int op) {
        switch (op) {
            case Cpu.NOP: return "NOP";
            case Cpu.MOV: return "MOV";
            case Cpu.ADD: return "ADD";
            case Cpu.SUB: return "SUB";
            case Cpu.INC: return "INC";
            case Cpu.DEC: return "DEC";
            case Cpu.NEG: return "NEG";
            case Cpu.JMP: return "JMP";
            case Cpu.CMP: return "CMP";
            case Cpu.TST: return "TST";
            case Cpu.AND: return "AND";
            case Cpu.OR: return "OR";
            case Cpu.XOR: return "XOR";
            case Cpu.NOT: return "NOT";
            case Cpu.RETI: return "RETI";
            case Cpu.MULU: return "MULU";
            case Cpu.DIVU: return "DIVU";
            case Cpu.PUSH: return "PUSH";
            case Cpu.POP: return "POP";
            case Cpu.CALL: return "CALL";
            case Cpu.RET: return "RET";
            case Cpu.MUL: return "MUL";
            case Cpu.DIV: return "DIV";
            case Cpu.ROL: return "ROL";
            case Cpu.ROR: return "ROR";
            case Cpu.SHL: return "SHL";
            case Cpu.SHR: return "SHR";
            case Cpu.BTST: return "BTST";
            case Cpu.BSET: return "BSET";
            case Cpu.BCLR: return "BCLR";
            case Cpu.JCOND: return "JCOND";
            default: return "OP_" + op;
        }
    }

    private static String condName(int code) {
        switch (code) {
            case 0: return "AL";
            case 1: return "EQ";
            case 2: return "NE";
            case 3: return "CS";
            case 4: return "CC";
            case 5: return "MI";
            case 6: return "PL";
            case 7: return "VS";
            case 8: return "VC";
            case 9: return "GT";
            case 10: return "GE";
            case 11: return "LT";
            case 12: return "LE";
            case 13: return "HI";
            case 14: return "HS";
            case 15: return "LO";
            default: return "C" + code;
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: Disassembler <input.bin>");
            System.exit(2);
        }
        byte[] data = Files.readAllBytes(Path.of(args[0]));
        System.out.print(disassemble(data));
    }


}

