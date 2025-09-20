package com.fantasy.cpu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CpuCallConventionTest {
    private Cpu cpu;

    @BeforeEach
    void setUp() {
        cpu = new Cpu();
    }

    private void loadProgram(int[] words) {
        for (int i = 0; i < words.length; i++) {
            cpu.writeWord(i * 2, words[i]);
        }
        cpu.pc = 0;
    }

    private int makeInstr(int opcode, int size, int srcMode, int srcReg, int dstMode, int dstReg) {
        return (opcode << 11) | (size << 10) | (srcMode << 8) | (srcReg << 5) | (dstMode << 3) | dstReg;
    }

    @Test
    void callPushesNextInstruction() {
        // Arrange: CALL $2000 at address 0; CALL instruction occupies one word + immediate word.
        loadProgram(new int[]{makeInstr(20,1,0,0,2,0), 0x2000}); // CALL $2000
        cpu.sp = 0xFEFF;

        // Act
        cpu.step();

        // Assert: PC set to target and stack contains return address pointing to the next instruction
        assertEquals(0x2000, cpu.pc);
        // Address after CALL instruction is 4 (0->instr, 2->immediate, next pc=4)
        assertEquals(4, cpu.readWord(0xFEFD));
    }

    @Test
    void retRestoresPcFromStack() {
        // Arrange: push a return address and execute RET
        cpu.sp = 0xFBFD;
        cpu.writeWord(0xFBFD, 0x1234);
        loadProgram(new int[]{makeInstr(21,1,0,0,0,0)}); // RET

        // Act
        cpu.step();

        // Assert
        assertEquals(0x1234, cpu.pc);
    }
}
