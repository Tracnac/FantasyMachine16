package com.fantasy.cpu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CpuMachineTest {
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

    // CPU behaviors
    @Test
    void testBankSwitch() {
        cpu.writeByte(0xFE00, (byte) 1); // BANK_REG new address
        assertEquals(1, cpu.bankReg);
        // Write to logical 0x0000 should go to Bank1 physical
        cpu.writeWord(0x0000, 0xABCD);
        assertEquals(0xABCD, cpu.readWord(0x10000));
    }

    @Test
    void testDma() {
        cpu.writeWord(0xFE04, 0x1000); // DMA_SRC new address
        cpu.writeWord(0xFE06, 0x2000); // DMA_DST new address
        cpu.writeByte(0xFE08, (byte) 0x04); // DMA_LEN new address = 4
        cpu.writeWord(0x1000, 0x1111);
        cpu.writeWord(0x1002, 0x2222);
        cpu.writeByte(0xFE0C, (byte) 2); // DMA_CTRL new address - STRT
        // DMA should copy to Bank1
        cpu.bankReg = 1;
        assertEquals(0x1111, cpu.readWord(0x2000));
        assertEquals(0x2222, cpu.readWord(0x2002));
    }

    // RETI
    @Test
    void testReti() {
        cpu.sp = 0xFBFC; // Use address within new stack range
        cpu.writeWord(0xFBFC, 0x5678); // FLAGS value (top of stack)
        cpu.writeWord(0xFBFE, 0x1234); // PC value (below FLAGS)
        loadProgram(new int[]{makeInstr(14,1,0,0,0,0)}); // RETI
        cpu.step();
        assertEquals(0x1234, cpu.pc);    // PC should be restored
        assertEquals(0x5678, cpu.flags); // FLAGS should be restored
        assertEquals(0xFC00, cpu.sp);    // SP should advance by 4 (2 pops)
    }

    // PUSH
    @Test
    void testPush() {
        cpu.regs[0] = 0xABCD;
        cpu.sp = 0xFBFF;
        loadProgram(new int[]{makeInstr(18,1,0,0,0,0)}); // PUSH.W R0
        cpu.step();
        assertEquals(0xFBFD, cpu.sp);
        assertEquals(0xABCD, cpu.readWord(0xFBFD));
    }

    // POP
    @Test
    void testPop() {
        cpu.sp = 0xFBFD; // Use address within new stack range
        cpu.writeWord(0xFBFD, 0xABCD);
        loadProgram(new int[]{makeInstr(19,1,0,0,0,0)}); // POP.W R0
        cpu.step();
        assertEquals(0xABCD, cpu.regs[0]);
        assertEquals(0xFBFF, cpu.sp);
    }

    // CALL
    @Test
    void testCall() {
        cpu.pc = 0;
        loadProgram(new int[]{makeInstr(20,1,0,0,2,0), 0x2000}); // CALL $2000
        cpu.step();
        assertEquals(0x2000, cpu.pc);
        assertEquals(4, cpu.readWord(cpu.sp)); // Return address (address after the full CALL instruction)
    }

    // RET
    @Test
    void testRet() {
        cpu.sp = 0xFBFD; // Use address within new stack range
        cpu.writeWord(0xFBFD, 0x1234);
        loadProgram(new int[]{makeInstr(21,1,0,0,0,0)}); // RET
        cpu.step();
        assertEquals(0x1234, cpu.pc);
    }

    // JMP
    @Test
    void testJmpAbs() {
        loadProgram(new int[]{makeInstr(7,1,0,0,2,0), 0x2000}); // JMP $2000
        cpu.step();
        assertEquals(0x2000, cpu.pc);
    }

    // JCOND
    @Test
    void testJcondEq() {
        cpu.flags |= Cpu.FLAG_Z;
        loadProgram(new int[]{makeInstr(31,0,3,1,0,0), 0x2000}); // JCOND EQ, $2000
        cpu.step();
        assertEquals(0x2000, cpu.pc);
    }

    @Test
    void testJcondNe() {
        loadProgram(new int[]{makeInstr(31,0,3,2,0,0), 0x2000}); // JCOND NE, $2000
        cpu.step();
        assertEquals(0x2000, cpu.pc);
    }
}