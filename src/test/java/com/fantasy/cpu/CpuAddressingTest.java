package com.fantasy.cpu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CpuAddressingTest {
    private Cpu cpu;

    @BeforeEach
    void setUp() {
        cpu = new Cpu();
    }

    // Helper methods
    private void loadProgram(int[] words) {
        for (int i = 0; i < words.length; i++) {
            cpu.writeWord(i * 2, words[i]);
        }
        cpu.pc = 0;
    }

    private int makeInstr(int opcode, int size, int srcMode, int srcReg, int dstMode, int dstReg) {
        return (opcode << 11) | (size << 10) | (srcMode << 8) | (srcReg << 5) | (dstMode << 3) | dstReg;
    }

    // MOV addressing tests
    @Test
    void testMovRegToReg() {
        cpu.regs[0] = 0x1234;
        loadProgram(new int[]{makeInstr(1,1,0,0,0,1)}); // MOV.W R0, R1
        cpu.step();
        assertEquals(0x1234, cpu.regs[1]);
    }

    @Test
    void testMovImmToReg() {
        loadProgram(new int[]{makeInstr(1,1,3,0,0,0), 0xABCD}); // MOV.W #ABCD, R0
        cpu.step();
        assertEquals(0xABCD, cpu.regs[0]);
    }

    @Test
    void testMovAbsToReg() {
        cpu.writeWord(0x1000, 0x5678);
        loadProgram(new int[]{makeInstr(1,1,2,0,0,0), 0x1000}); // MOV.W $1000, R0
        cpu.step();
        assertEquals(0x5678, cpu.regs[0]);
    }

    @Test
    void testMovIndToReg() {
        cpu.regs[1] = 0x1000;
        cpu.writeWord(0x1000, 0x9ABC);
        loadProgram(new int[]{makeInstr(1,1,1,1,0,0)}); // MOV.W [R1], R0
        cpu.step();
        assertEquals(0x9ABC, cpu.regs[0]);
    }
}
