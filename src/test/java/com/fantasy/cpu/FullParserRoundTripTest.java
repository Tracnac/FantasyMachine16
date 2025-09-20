package com.fantasy.cpu;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class FullParserRoundTripTest {

    @Test
    public void roundTripProducesIdenticalBinaries() throws Exception {
        Path asm = Path.of("ASM/FullParserTest.asm");
        Path bin1 = Path.of("target","test-output","program.bin");
        Path bin2 = Path.of("target","test-output","program2.bin");
        Files.createDirectories(bin1.getParent());

        // Assemble first time
        Assembler assembler = new Assembler();
        assembler.parse(asm);
        assembler.assemble(bin1);

        // Disassemble
        byte[] data = Files.readAllBytes(bin1);
        String asmOut = Disassembler.disassemble(data);
        Path roundAsm = bin1.getParent().resolve("roundtrip.asm");
        Files.writeString(roundAsm, asmOut);

        // Re-assemble
        Assembler assembler2 = new Assembler();
        assembler2.parse(roundAsm);
        assembler2.assemble(bin2);

        byte[] b1 = Files.readAllBytes(bin1);
        byte[] b2 = Files.readAllBytes(bin2);
        assertArrayEquals(b1, b2, "Re-assembled binary must match original binary exactly");
    }
}
