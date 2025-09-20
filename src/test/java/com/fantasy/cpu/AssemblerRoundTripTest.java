package com.fantasy.cpu;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class AssemblerRoundTripTest {

    @Test
    public void roundTripAsmDisasm() throws Exception {
    String asm = "START: MOV.W 0x0001, R0\n" +
             "       CMP.W R0, 0x0000\n" +
             "       JCOND EQ, $0008\n" +
             "       JMP START\n" +
             "LOOP:  NOP\n" +
             ".end\n";

        Path tmpAsm1 = Files.createTempFile("prog1", ".asm");
        Path outBin1 = Files.createTempFile("prog1", ".bin");
        Files.writeString(tmpAsm1, asm);

        Assembler a = new Assembler();
        a.parse(tmpAsm1);
        a.assemble(outBin1);

        byte[] bin1 = Files.readAllBytes(outBin1);

        // Disassemble
        String dis = Disassembler.disassemble(bin1);

        // Assemble disassembly
        Path tmpAsm2 = Files.createTempFile("prog2", ".asm");
        Path outBin2 = Files.createTempFile("prog2", ".bin");
        Files.writeString(tmpAsm2, dis);

        Assembler a2 = new Assembler();
        a2.parse(tmpAsm2);
        a2.assemble(outBin2);

        byte[] bin2 = Files.readAllBytes(outBin2);

        assertArrayEquals(bin1, bin2, "Round-trip assembled binaries should match");
    }

    @Test
    public void roundTripFullParserTest() throws Exception {
        // Test round-trip assembly/disassembly of FullParserTest.asm
        Path fullParserAsm = Path.of("ASM/FullParserTest.asm");
        assertTrue(Files.exists(fullParserAsm), "FullParserTest.asm should exist in project root");
        
        // Assemble the original file
        Path outBin1 = Files.createTempFile("fullparser1", ".bin");
        Assembler a1 = new Assembler();
        a1.parse(fullParserAsm);
        a1.assemble(outBin1);
        byte[] bin1 = Files.readAllBytes(outBin1);

        // Disassemble to get assembly source
        String disassembly = Disassembler.disassemble(bin1);
        
        // Reassemble the disassembly
        Path tmpAsm = Files.createTempFile("fullparser_disasm", ".asm");
        Path outBin2 = Files.createTempFile("fullparser2", ".bin");
        Files.writeString(tmpAsm, disassembly);
        
        Assembler a2 = new Assembler();
        a2.parse(tmpAsm);
        a2.assemble(outBin2);
        byte[] bin2 = Files.readAllBytes(outBin2);

        assertArrayEquals(bin1, bin2, "FullParserTest.asm round-trip binaries should match");
        
        // Clean up temp files
        Files.deleteIfExists(outBin1);
        Files.deleteIfExists(outBin2);
        Files.deleteIfExists(tmpAsm);
    }

    @Test
    public void roundTripRulesAsm() throws Exception {
        // Test round-trip assembly/disassembly of RULES.asm
        Path rulesAsm = Path.of("ASM/RULES.asm");
        assertTrue(Files.exists(rulesAsm), "RULES.asm should exist in project root");
        
        // Assemble the original file
        Path outBin1 = Files.createTempFile("rules1", ".bin");
        Assembler a1 = new Assembler();
        a1.parse(rulesAsm);
        a1.assemble(outBin1);
        byte[] bin1 = Files.readAllBytes(outBin1);

        // Disassemble to get assembly source
        String disassembly = Disassembler.disassemble(bin1);
        
        // Reassemble the disassembly
        Path tmpAsm = Files.createTempFile("rules_disasm", ".asm");
        Path outBin2 = Files.createTempFile("rules2", ".bin");
        Files.writeString(tmpAsm, disassembly);
        
        Assembler a2 = new Assembler();
        a2.parse(tmpAsm);
        a2.assemble(outBin2);
        byte[] bin2 = Files.readAllBytes(outBin2);

        assertArrayEquals(bin1, bin2, "RULES.asm round-trip binaries should match");
        
        // Clean up temp files
        Files.deleteIfExists(outBin1);
        Files.deleteIfExists(outBin2);
        Files.deleteIfExists(tmpAsm);
    }
}
