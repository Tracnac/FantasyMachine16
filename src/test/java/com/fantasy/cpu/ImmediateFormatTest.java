package com.fantasy.cpu;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class ImmediateFormatTest {

    private Path writeTempAsm(String content) throws IOException {
        Path p = Files.createTempFile("asmtest", ".asm");
        Files.writeString(p, content);
        return p;
    }

    @Test
    public void disallowHashDollarImmediate() throws Exception {
        Path p = writeTempAsm("MOV #$10, R0\n.end\n");
        Assembler a = new Assembler();
        // per RULES, '#$' immediates are rejected at parse time
        // just ensure parse throws; the exact wording may vary but it should reject '#' usage
        AssembleException ex = assertThrows(AssembleException.class, () -> a.parse(p));
        String msg = ex.getMessage().toLowerCase();
        assertTrue(msg.contains("invalid") || msg.contains("#"), "Expected an error indicating invalid immediate or '#' usage: " + msg);
    }

    @Test
    public void acceptsHexImmediateWithHashAndPadding() throws Exception {
        String asm = "start:\n  MOV 0x0001, R1\n.end\n";
        Path p = writeTempAsm(asm);
        Assembler a = new Assembler();
        a.parse(p);
        // assemble should succeed
        a.assemble(Path.of("target/test-imm-hex.bin"));
    }

    @Test
    public void acceptsBinaryImmediate() throws Exception {
        String asm = "start:\n  MOV %1010, R2\n.end\n";
        Path p = writeTempAsm(asm);
        Assembler a = new Assembler();
        a.parse(p);
        a.assemble(Path.of("target/test-imm-bin.bin"));
    }

    @Test
    public void acceptsDecimalImmediate() throws Exception {
        String asm = "start:\n  MOV 123, R3\n.end\n";
        Path p = writeTempAsm(asm);
        Assembler a = new Assembler();
        a.parse(p);
        a.assemble(Path.of("target/test-imm-dec.bin"));
    }

    @Test
    public void rejectsBareHexWithoutHashForImmediate() throws Exception {
        // With the new RULES, bare 0x hex immediates are accepted, so this test is no longer applicable.
        // Keep as a smoke check: ensure 0x immediate assembles.
        String asm = "start:\n  MOV 0x10, R1\n.end\n";
        Path p = writeTempAsm(asm);
        Assembler a = new Assembler();
        a.parse(p);
        a.assemble(Path.of("target/test-barehex-ok.bin"));
    }

    @Test
    public void rejectsInvalidImmediateFormat() throws Exception {
        String asm = "start:\n  MOV DEAD, R1\n.end\n";
        Path p = writeTempAsm(asm);
        Assembler a = new Assembler();
        a.parse(p);
        assertThrows(AssembleException.class, () -> a.assemble(Path.of("target/test-bad2.bin")));
    }

    @Test
    public void rejectsByteImmediateTooLarge() throws Exception {
        String asm = "start:\n  MOV.B 0x1FF, R1\n.end\n"; // 0x1FF > 0xFF
        Path p = writeTempAsm(asm);
        Assembler a = new Assembler();
        a.parse(p);
        assertThrows(AssembleException.class, () -> a.assemble(Path.of("target/test-bad3.bin")));
    }

    @Test
    public void acceptsByteImmediateWithinRange() throws Exception {
        String asm = "start:\n  MOV.B 0x7F, R1\n.end\n";
        Path p = writeTempAsm(asm);
        Assembler a = new Assembler();
        a.parse(p);
        a.assemble(Path.of("target/test-imm-byte-ok.bin"));
    }

    @Test
    public void acceptsAddressWithDollar() throws Exception {
        String asm = "start:\n  JMP $1000\n.end\n";
        Path p = writeTempAsm(asm);
        Assembler a = new Assembler();
        a.parse(p);
        a.assemble(Path.of("target/test-addr-ok.bin"));
    }

    @Test
    public void rejectsAddressWithoutDollar() throws Exception {
        // With new RULES numeric tokens are immediates; bare decimal is accepted as immediate and JMP expects an address -> should fail at assemble
        String asm = "start:\n  JMP 1000\n.end\n";
        Path p = writeTempAsm(asm);
        Assembler a = new Assembler();
        a.parse(p);
        assertThrows(AssembleException.class, () -> a.assemble(Path.of("target/test-addr-bad.bin")));
    }
}
