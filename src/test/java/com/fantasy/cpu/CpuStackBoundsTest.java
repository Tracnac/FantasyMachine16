package com.fantasy.cpu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CpuStackBoundsTest {
    private Cpu cpu;

    @BeforeEach
    void setUp() {
        cpu = new Cpu();
    }

    @Test
    void pushDecrementsSpByTwo() {
        cpu.sp = 0xFEFF;
        cpu.regs[0] = 0xBEEF;
        // perform a push by writing directly using pushWord helper via instruction
        int instr = (18 << 11) | (1 << 10); // PUSH.W R0 encoded minimally
        cpu.writeWord(0, instr);
        cpu.pc = 0;
        cpu.step();
        assertEquals(0xFEFD, cpu.sp);
        assertEquals(0xBEEF, cpu.readWord(0xFEFD));
    }

    @Test
    void popIncrementsSpByTwo() {
        cpu.sp = 0xFBFD; // Use address within new stack range
        cpu.writeWord(0xFBFD, 0xCAFE);
        int instr = (19 << 11) | (1 << 10); // POP.W R0
        cpu.writeWord(0, instr);
        cpu.pc = 0;
        cpu.step();
        assertEquals(0xFBFF, cpu.sp);
        assertEquals(0xCAFE, cpu.regs[0]);
    }

    // Note: This suite documents current emulator behavior for stack boundaries. If the
    // emulator should throw or trap on overflow/underflow, tests should be adjusted to
    // assert that behavior. Currently we assert the arithmetic on SP and basic memory access.
}
