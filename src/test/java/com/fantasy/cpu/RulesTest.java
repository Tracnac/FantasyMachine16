package com.fantasy.cpu;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class RulesTest {

    @Test
    public void orgIsOptionalAndDefaultStartAt0000() throws Exception {
        Path p = Files.createTempFile("torgopt", ".asm");
        Files.writeString(p, "START: NOP\n.end\n");
        Assembler a = new Assembler();
        a.parse(p);
        Path bout = Files.createTempFile("torgopt", ".bin");
        a.assemble(bout);
        byte[] data = Files.readAllBytes(bout);
        String dis = Disassembler.disassemble(data);
        // Expect instruction emitted at $0000
        assertTrue(dis.contains("$0000"), "Disassembly should contain $0000 when no .org is present");
    }

    @Test
    public void multipleOrgAndStartAfterOrg() throws Exception {
        // Create a temporary asm file that contains an .org followed by a .start — this must be rejected
        Path p = Files.createTempFile("torgstart", ".asm");
        Files.writeString(p, ".org $0100\n.start\nNOP\n.end\n");
        Assembler a = new Assembler();
        // Since .start appears after .org in this file, the assembler must reject it
        AssembleException ex = assertThrows(AssembleException.class, () -> a.parse(p));
        assertTrue(ex.getMessage().toLowerCase().contains(".start") || ex.getMessage().toLowerCase().contains("org"));
    }

    @Test
    public void duplicateStartIsError() throws Exception {
        Path p = Files.createTempFile("tdupstart", ".asm");
        Files.writeString(p, ".start\n.start\n.end\n");
        Assembler a = new Assembler();
        AssembleException ex = assertThrows(AssembleException.class, () -> a.parse(p));
        assertTrue(ex.getMessage().toLowerCase().contains("only one .start is allowed") || ex.getMessage().toLowerCase().contains("only one .start"));
    }

    @Test
    public void startWithoutOrgStartsAt0000() throws Exception {
        Path p = Files.createTempFile("tstartnoorg", ".asm");
        Files.writeString(p, ".start\nNOP\n.end\n");
        Assembler a = new Assembler();
        a.parse(p);
        Path bout = Files.createTempFile("tstartnoorg", ".bin");
        a.assemble(bout);
        byte[] data = Files.readAllBytes(bout);
        String dis = Disassembler.disassemble(data);
        assertTrue(dis.contains("$0000"), "Disassembly should place code at $0000 when .start present without .org");
    }

    @Test
    public void startBeforeOrgIsAllowedAndOrgStillSetsLocation() throws Exception {
        Path p = Files.createTempFile("tstartbeforeorg", ".asm");
        // .start is present first, then .org should move the location for following code
        Files.writeString(p, ".start\nNOP\n.org $0200\nNOP\n.end\n");
        Assembler a = new Assembler();
        a.parse(p);
        Path bout = Files.createTempFile("tstartbeforeorg", ".bin");
        a.assemble(bout);
        byte[] data = Files.readAllBytes(bout);
        String dis = Disassembler.disassemble(data);
        // Ensure we have an instruction at $0000 (before .org) and at $0200 (after .org)
        assertTrue(dis.contains("$0000"), "Disassembly should contain code at $0000 for instructions before .org");
        assertTrue(dis.contains("$0200"), "Disassembly should contain code at $0200 for instructions after .org");
    }
}
