package com.fantasy.cpu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CpuOpcodesTest {
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

    // INC
    @Test
    void testIncReg() {
        cpu.regs[0] = 0x1234;
        loadProgram(new int[]{makeInstr(4,1,0,0,0,0)}); // INC.W R0
        cpu.step();
        assertEquals(0x1235, cpu.regs[0]);
    }

    // DEC
    @Test
    void testDecReg() {
        cpu.regs[0] = 0x1234;
        loadProgram(new int[]{makeInstr(5,1,0,0,0,0)}); // DEC.W R0
        cpu.step();
        assertEquals(0x1233, cpu.regs[0]);
    }

    // NOT
    @Test
    void testNot() {
        cpu.regs[0] = 0xAAAA;
        loadProgram(new int[]{makeInstr(13,1,0,0,0,0)}); // NOT.W R0
        cpu.step();
        assertEquals(0x5555, cpu.regs[0]);
    }

    // AND/OR/XOR
    @Test
    void testAndOrXor() {
        cpu.regs[0] = 0xFFFF;
        cpu.regs[1] = 0x0F0F;
        loadProgram(new int[]{makeInstr(10,1,0,0,0,1)}); // AND.W R0, R1
        cpu.step();
        assertEquals(0x0F0F, cpu.regs[1]);

        cpu.regs[0] = 0x0F0F;
        cpu.regs[1] = 0xF0F0;
        loadProgram(new int[]{makeInstr(11,1,0,0,0,1)}); // OR.W R0, R1
        cpu.step();
        assertEquals(0xFFFF, cpu.regs[1]);

        cpu.regs[0] = 0xFFFF;
        cpu.regs[1] = 0xAAAA;
        loadProgram(new int[]{makeInstr(12,1,0,0,0,1)}); // XOR.W R0, R1
        cpu.step();
        assertEquals(0x5555, cpu.regs[1]);
    }

    // MULU/DIVU/MUL/DIV
    @Test
    void testMulDiv() {
        cpu.regs[0] = 0x1000;
        cpu.regs[1] = 0x2000;
        loadProgram(new int[]{makeInstr(16,1,0,0,0,1)}); // MULU.W R0, R1
        cpu.step();
        assertEquals(0x0000, cpu.regs[1]); // Low in destination R1
        assertEquals(0x0200, cpu.regs[2]); // High in R1+1 = R2 (0x1000 * 0x2000 = 0x02000000)

        cpu.regs[0] = 0x0002;
        cpu.regs[1] = 0x1234;
        loadProgram(new int[]{makeInstr(17,1,0,0,0,1)}); // DIVU.W R0, R1
        cpu.step();
        assertEquals(0x091A, cpu.regs[1]); // Quotient in destination R1
        assertEquals(0x0000, cpu.regs[2]); // Remainder in R1+1 = R2

        cpu.regs[0] = 0x0002;
        cpu.regs[1] = 0x0003;
        loadProgram(new int[]{makeInstr(22,1,0,0,0,1)}); // MUL.W R0, R1
        cpu.step();
        assertEquals(0x0006, cpu.regs[1]); // Result in destination R1
        assertEquals(0x0000, cpu.regs[2]); // High part in R1+1 = R2

        cpu.regs[0] = 0x0002;
        cpu.regs[1] = 0x0006;
        loadProgram(new int[]{makeInstr(23,1,0,0,0,1)}); // DIV.W R0, R1
        cpu.step();
        assertEquals(0x0003, cpu.regs[1]); // Quotient in destination R1
        assertEquals(0x0000, cpu.regs[2]); // Remainder in R1+1 = R2
    }

    // BTST/BSET/BCLR
    @Test
    void testBitOps() {
        cpu.regs[0] = 0x0000; // Bit 0
        cpu.regs[1] = 0x0001;
        loadProgram(new int[]{makeInstr(28,1,0,0,0,1)}); // BTST.W R0, R1
        cpu.step();

        cpu.regs[0] = 0x0000; // Bit 0
        cpu.regs[1] = 0x0000;
        loadProgram(new int[]{makeInstr(29,1,0,0,0,1)}); // BSET.W R0, R1
        cpu.step();
        assertEquals(0x0001, cpu.regs[1]);

        cpu.regs[0] = 0x0000; // Bit 0
        cpu.regs[1] = 0x0001;
        loadProgram(new int[]{makeInstr(30,1,0,0,0,1)}); // BCLR.W R0, R1
        cpu.step();
        assertEquals(0x0000, cpu.regs[1]);
    }
}
