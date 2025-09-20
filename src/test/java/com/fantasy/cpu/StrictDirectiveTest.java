package com.fantasy.cpu;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class StrictDirectiveTest {

    @Test
    public void unknownDotDirectiveIsError() throws Exception {
        Path p = Files.createTempFile("tunknown", ".asm");
        Files.writeString(p, ".FOO $0000\n.end\n");
        Assembler a = new Assembler();
        AssembleException ex = assertThrows(AssembleException.class, () -> a.parse(p));
        assertTrue(ex.getMessage().toLowerCase().contains("unknown directive"));
    }

    @Test
    public void trailingColonOnDirectiveIsError() throws Exception {
        Path p = Files.createTempFile("tcolon", ".asm");
        Files.writeString(p, ".data:\n.end\n");
        Assembler a = new Assembler();
        AssembleException ex = assertThrows(AssembleException.class, () -> a.parse(p));
        assertTrue(ex.getMessage().toLowerCase().contains("should not use trailing ':'"));
    }

    @Test
    public void missingEndIsError() throws Exception {
        Path p = Files.createTempFile("tend", ".asm");
        Files.writeString(p, "NOP\n");
        Assembler a = new Assembler();
        AssembleException ex = assertThrows(AssembleException.class, () -> a.parse(p));
        assertTrue(ex.getMessage().toLowerCase().contains("program must end with .end"));
    }
}
