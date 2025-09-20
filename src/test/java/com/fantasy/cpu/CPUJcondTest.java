package com.fantasy.cpu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CPUJcondTest {
    private Cpu cpu;

    @BeforeEach
    void setUp() {
        cpu = new Cpu();
    }

    private void loadProgram(int[] words) {
        for (int i = 0; i < words.length; i++) cpu.writeWord(i * 2, words[i]);
        cpu.pc = 0;
    }

    private int makeInstr(int opcode, int size, int srcMode, int srcReg, int dstMode, int dstReg) {
        return (opcode << 11) | (size << 10) | (srcMode << 8) | (srcReg << 5) | (dstMode << 3) | dstReg;
    }

    @Test
    void testJcondAL() {
        // AL (Always) - condition code 0 - should always jump
        int condCode = 0;
        int size = (condCode >> 3) & 1;
        int srcReg = condCode & 0x7;
        int instr = makeInstr(Cpu.JCOND, size, Cpu.MODE_IMM, srcReg, Cpu.MODE_REG, 0);
        int target = 0x1000;
        
        // Test with no flags
        cpu.flags = 0;
        loadProgram(new int[]{instr, target});
        cpu.step();
        assertEquals(target, cpu.pc, "AL should always jump (no flags)");
        
        // Test with all flags
        cpu = new Cpu();
        cpu.flags = Cpu.FLAG_N | Cpu.FLAG_Z | Cpu.FLAG_C | Cpu.FLAG_V | Cpu.FLAG_X;
        loadProgram(new int[]{instr, target});
        cpu.step();
        assertEquals(target, cpu.pc, "AL should always jump (all flags)");
    }

    @Test
    void testJcondEQ() {
        // EQ (Equal) - condition code 1 - jump if Z=1
        int condCode = 1;
        int size = (condCode >> 3) & 1;
        int srcReg = condCode & 0x7;
        int instr = makeInstr(Cpu.JCOND, size, Cpu.MODE_IMM, srcReg, Cpu.MODE_REG, 0);
        int target = 0x1000;
        
        // Should jump when Z=1
        cpu.flags = Cpu.FLAG_Z;
        loadProgram(new int[]{instr, target});
        cpu.step();
        assertEquals(target, cpu.pc, "EQ should jump when Z=1");
        
        // Should NOT jump when Z=0
        cpu = new Cpu();
        cpu.flags = 0;
        loadProgram(new int[]{instr, target});
        cpu.step();
        assertEquals(4, cpu.pc, "EQ should not jump when Z=0");
    }

    @Test
    void testJcondNE() {
        // NE (Not Equal) - condition code 2 - jump if Z=0
        int condCode = 2;
        int size = (condCode >> 3) & 1;
        int srcReg = condCode & 0x7;
        int instr = makeInstr(Cpu.JCOND, size, Cpu.MODE_IMM, srcReg, Cpu.MODE_REG, 0);
        int target = 0x1000;
        
        // Should jump when Z=0
        cpu.flags = 0;
        loadProgram(new int[]{instr, target});
        cpu.step();
        assertEquals(target, cpu.pc, "NE should jump when Z=0");
        
        // Should NOT jump when Z=1
        cpu = new Cpu();
        cpu.flags = Cpu.FLAG_Z;
        loadProgram(new int[]{instr, target});
        cpu.step();
        assertEquals(4, cpu.pc, "NE should not jump when Z=1");
    }

    @Test
    void testJcondCS() {
        // CS (Carry Set) - condition code 3 - jump if C=1
        int condCode = 3;
        int size = (condCode >> 3) & 1;
        int srcReg = condCode & 0x7;
        int instr = makeInstr(Cpu.JCOND, size, Cpu.MODE_IMM, srcReg, Cpu.MODE_REG, 0);
        int target = 0x1000;
        
        // Should jump when C=1
        cpu.flags = Cpu.FLAG_C;
        loadProgram(new int[]{instr, target});
        cpu.step();
        assertEquals(target, cpu.pc, "CS should jump when C=1");
        
        // Should NOT jump when C=0
        cpu = new Cpu();
        cpu.flags = 0;
        loadProgram(new int[]{instr, target});
        cpu.step();
        assertEquals(4, cpu.pc, "CS should not jump when C=0");
    }

    @Test
    void testJcondGT() {
        // GT (Greater Than) - condition code 9 - jump if Z=0 && N=V
        int condCode = 9;
        int size = (condCode >> 3) & 1;
        int srcReg = condCode & 0x7;
        int instr = makeInstr(Cpu.JCOND, size, Cpu.MODE_IMM, srcReg, Cpu.MODE_REG, 0);
        int target = 0x1000;
        
        // Should jump when Z=0 && N=0 && V=0 (both N and V same)
        cpu.flags = 0;
        loadProgram(new int[]{instr, target});
        cpu.step();
        assertEquals(target, cpu.pc, "GT should jump when Z=0, N=0, V=0");
        
        // Should jump when Z=0 && N=1 && V=1 (both N and V same)
        cpu = new Cpu();
        cpu.flags = Cpu.FLAG_N | Cpu.FLAG_V;
        loadProgram(new int[]{instr, target});
        cpu.step();
        assertEquals(target, cpu.pc, "GT should jump when Z=0, N=1, V=1");
        
        // Should NOT jump when Z=1 (regardless of N,V)
        cpu = new Cpu();
        cpu.flags = Cpu.FLAG_Z;
        loadProgram(new int[]{instr, target});
        cpu.step();
        assertEquals(4, cpu.pc, "GT should not jump when Z=1");
        
        // Should NOT jump when N!=V
        cpu = new Cpu();
        cpu.flags = Cpu.FLAG_N; // N=1, V=0
        loadProgram(new int[]{instr, target});
        cpu.step();
        assertEquals(4, cpu.pc, "GT should not jump when N=1, V=0");
    }

    @Test
    void testAllJcondCodesSystematic() {
        for (int condCode = 0; condCode < 16; condCode++) {
            testConditionCode(condCode);
        }
    }
    
    private void testConditionCode(int condCode) {
        int size = (condCode >> 3) & 1;
        int srcReg = condCode & 0x7;
        int instr = makeInstr(Cpu.JCOND, size, Cpu.MODE_IMM, srcReg, Cpu.MODE_REG, 0);
        int target = 0x2000 + condCode;
        
        // Test all possible flag combinations (32 combinations of 5 flags)
        for (int flagCombo = 0; flagCombo < 32; flagCombo++) {
            int flags = 0;
            if ((flagCombo & 1) != 0) flags |= Cpu.FLAG_N;
            if ((flagCombo & 2) != 0) flags |= Cpu.FLAG_Z;
            if ((flagCombo & 4) != 0) flags |= Cpu.FLAG_C;
            if ((flagCombo & 8) != 0) flags |= Cpu.FLAG_V;
            if ((flagCombo & 16) != 0) flags |= Cpu.FLAG_X;
            
            boolean expectedJump = shouldJump(condCode, flags);
            
            cpu = new Cpu();
            cpu.flags = flags;
            loadProgram(new int[]{instr, target});
            cpu.step();
            
            if (expectedJump) {
                assertEquals(target, cpu.pc, 
                    String.format("Condition %d should jump with flags=0x%02X", condCode, flags));
            } else {
                assertEquals(4, cpu.pc, 
                    String.format("Condition %d should NOT jump with flags=0x%02X", condCode, flags));
            }
        }
    }
    
    private boolean shouldJump(int condCode, int flags) {
        boolean n = (flags & Cpu.FLAG_N) != 0;
        boolean z = (flags & Cpu.FLAG_Z) != 0;
        boolean c = (flags & Cpu.FLAG_C) != 0;
        boolean v = (flags & Cpu.FLAG_V) != 0;
        
        switch (condCode) {
            case 0: return true; // AL
            case 1: return z; // EQ
            case 2: return !z; // NE
            case 3: return c; // CS
            case 4: return !c; // CC
            case 5: return n; // MI
            case 6: return !n; // PL
            case 7: return v; // VS
            case 8: return !v; // VC
            case 9: return !z && (n == v); // GT
            case 10: return n == v; // GE
            case 11: return n != v; // LT
            case 12: return z || (n != v); // LE
            case 13: return !c && !z; // HI
            case 14: return !c; // HS
            case 15: return c || z; // LO
            default: return false;
        }
    }
}
