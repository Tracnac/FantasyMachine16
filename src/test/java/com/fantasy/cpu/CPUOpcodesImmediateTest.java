package com.fantasy.cpu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests complets pour tous les opcodes du Fantasy Machine 16 CPU qui supportent
 * le mode d'adressage immédiat #imm (MODE_IMM = 3).
 * 
 * Ce fichier teste systématiquement chaque opcode avec l'adressage immédiat,
 * où les valeurs sont spécifiées directement dans l'instruction.
 * NOTE: L'adressage immédiat n'est valide QUE pour les opérandes source.
 */
public class CPUOpcodesImmediateTest {
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

    // ======================= OPCODES ARITHMETIQUES =======================

    @Test
    void testMOV_ImmToReg() {
        // MOV.W #1234, R2 - Move immediate value to register
        cpu.regs[2] = 0x0000;
        loadProgram(new int[]{
            makeInstr(Cpu.MOV, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 2),
            0x1234  // Immediate value
        });
        cpu.step();
        assertEquals(0x1234, cpu.regs[2], "MOV.W should copy immediate value to register");
    }

    @Test
    void testMOV_ImmToInd() {
        // MOV.W #5678, [R1] - Move immediate value to memory location
        cpu.regs[1] = 0x2000;  // R1 points to memory address
        cpu.writeWord(0x2000, 0x0000);  // Initialize memory
        loadProgram(new int[]{
            makeInstr(Cpu.MOV, 1, Cpu.MODE_IMM, 0, Cpu.MODE_IND, 1),
            0x5678  // Immediate value
        });
        cpu.step();
        assertEquals(0x5678, cpu.readWord(0x2000), "MOV.W should store immediate value to memory");
    }

    @Test
    void testMOV_ImmToAbs() {
        // MOV.W #ABCD, $3000 - Move immediate value to absolute address
        cpu.writeWord(0x3000, 0x0000);  // Initialize memory
        loadProgram(new int[]{
            makeInstr(Cpu.MOV, 1, Cpu.MODE_IMM, 0, Cpu.MODE_ABS, 0),
            0xABCD,  // Immediate value
            0x3000   // Absolute address
        });
        cpu.step();
        assertEquals(0xABCD, cpu.readWord(0x3000), "MOV.W should store immediate value to absolute address");
    }

    @Test
    void testMOV_ImmToReg_Byte() {
        // MOV.B #34, R2 - Move immediate byte value to register
        cpu.regs[2] = 0xFFFF;  // Initialize with different value
        loadProgram(new int[]{
            makeInstr(Cpu.MOV, 0, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 2),
            0x1234  // Immediate value (will use entire word)
        });
        cpu.step();
        assertEquals(0x1234, cpu.regs[2], "MOV.B with immediate should use entire immediate word");
    }

    @Test
    void testADD_ImmToReg() {
        // ADD.W #1000, R2 - Add immediate value to register
        cpu.regs[2] = 0x0234;
        loadProgram(new int[]{
            makeInstr(Cpu.ADD, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 2),
            0x1000  // Immediate value
        });
        cpu.step();
        assertEquals(0x1234, cpu.regs[2], "ADD.W should add immediate value to register");
    }

    @Test
    void testADD_ImmToInd() {
        // ADD.W #1000, [R1] - Add immediate value to memory location
        cpu.regs[1] = 0x2000;
        cpu.writeWord(0x2000, 0x0234);  // Initial memory value
        loadProgram(new int[]{
            makeInstr(Cpu.ADD, 1, Cpu.MODE_IMM, 0, Cpu.MODE_IND, 1),
            0x1000  // Immediate value
        });
        cpu.step();
        assertEquals(0x1234, cpu.readWord(0x2000), "ADD.W should add immediate value to memory");
    }

    @Test
    void testADD_ImmToAbs() {
        // ADD.W #1000, $2000 - Add immediate value to absolute address
        cpu.writeWord(0x2000, 0x0234);  // Initial memory value
        loadProgram(new int[]{
            makeInstr(Cpu.ADD, 1, Cpu.MODE_IMM, 0, Cpu.MODE_ABS, 0),
            0x1000,  // Immediate value
            0x2000   // Absolute address
        });
        cpu.step();
        assertEquals(0x1234, cpu.readWord(0x2000), "ADD.W should add immediate value to absolute address");
    }

    @Test
    void testADD_ImmWithCarry() {
        // Test ADD with immediate causing carry
        cpu.regs[2] = 0xFFFF;
        loadProgram(new int[]{
            makeInstr(Cpu.ADD, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 2),
            0x0001  // Immediate value
        });
        cpu.step();
        assertEquals(0x0000, cpu.regs[2], "ADD.W overflow should result in 0x0000");
        assertTrue((cpu.flags & Cpu.FLAG_C) != 0, "ADD.W overflow should set carry flag");
        assertTrue((cpu.flags & Cpu.FLAG_Z) != 0, "ADD.W result 0 should set zero flag");
    }

    @Test
    void testSUB_ImmToReg() {
        // SUB.W #234, R2 - Subtract immediate value from register
        cpu.regs[2] = 0x1234;
        loadProgram(new int[]{
            makeInstr(Cpu.SUB, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 2),
            0x0234  // Immediate value
        });
        cpu.step();
        assertEquals(0x1000, cpu.regs[2], "SUB.W should subtract immediate value from register");
    }

    @Test
    void testSUB_ImmWithBorrow() {
        // Test SUB with immediate causing borrow
        cpu.regs[2] = 0x0001;
        loadProgram(new int[]{
            makeInstr(Cpu.SUB, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 2),
            0x0002  // Immediate value
        });
        cpu.step();
        assertEquals(0xFFFF, cpu.regs[2], "SUB.W underflow should result in 0xFFFF");
        assertTrue((cpu.flags & Cpu.FLAG_C) != 0, "SUB.W underflow should set carry flag");
    }

    // ======================= OPCODES LOGIQUES =======================

    @Test
    void testAND_ImmToReg() {
        // AND.W #FF0F, R2 - Bitwise AND immediate value with register
        cpu.regs[2] = 0x0FFF;
        loadProgram(new int[]{
            makeInstr(Cpu.AND, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 2),
            0xFF0F  // Immediate value
        });
        cpu.step();
        assertEquals(0x0F0F, cpu.regs[2], "AND.W should perform bitwise AND with immediate value");
    }

    @Test
    void testAND_ImmZero() {
        // Test AND with zero immediate (common operation for clearing)
        cpu.regs[2] = 0xFFFF;
        loadProgram(new int[]{
            makeInstr(Cpu.AND, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 2),
            0x0000  // Immediate value
        });
        cpu.step();
        assertEquals(0x0000, cpu.regs[2], "AND.W with zero should clear register");
        assertTrue((cpu.flags & Cpu.FLAG_Z) != 0, "AND.W result 0 should set zero flag");
    }

    @Test
    void testOR_ImmToReg() {
        // OR.W #FF0F, R2 - Bitwise OR immediate value with register
        cpu.regs[2] = 0x0FF0;
        loadProgram(new int[]{
            makeInstr(Cpu.OR, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 2),
            0xFF0F  // Immediate value
        });
        cpu.step();
        assertEquals(0xFFFF, cpu.regs[2], "OR.W should perform bitwise OR with immediate value");
    }

    @Test
    void testXOR_ImmToReg() {
        // XOR.W #FF0F, R2 - Bitwise XOR immediate value with register
        cpu.regs[2] = 0x0FF0;
        loadProgram(new int[]{
            makeInstr(Cpu.XOR, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 2),
            0xFF0F  // Immediate value
        });
        cpu.step();
        assertEquals(0xF0FF, cpu.regs[2], "XOR.W should perform bitwise XOR with immediate value");
    }

    @Test
    void testXOR_ImmSelfCancel() {
        // Test XOR with same value (should result in zero)
        cpu.regs[2] = 0x1234;
        loadProgram(new int[]{
            makeInstr(Cpu.XOR, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 2),
            0x1234  // Same immediate value
        });
        cpu.step();
        assertEquals(0x0000, cpu.regs[2], "XOR.W with same value should result in 0");
        assertTrue((cpu.flags & Cpu.FLAG_Z) != 0, "XOR.W result 0 should set zero flag");
    }

    // ======================= OPCODES DE COMPARAISON =======================

    @Test
    void testCMP_ImmToReg_Equal() {
        // CMP.W #1234, R2 - Compare immediate value with register
        cpu.regs[2] = 0x1234;
        loadProgram(new int[]{
            makeInstr(Cpu.CMP, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 2),
            0x1234  // Immediate value
        });
        cpu.step();
        assertEquals(0x1234, cpu.regs[2], "CMP.W should not modify destination register");
        assertTrue((cpu.flags & Cpu.FLAG_Z) != 0, "CMP.W equal values should set zero flag");
    }

    @Test
    void testCMP_ImmToReg_Greater() {
        // Test CMP where register > immediate
        cpu.regs[2] = 0x2000;
        loadProgram(new int[]{
            makeInstr(Cpu.CMP, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 2),
            0x1000  // Immediate value
        });
        cpu.step();
        assertFalse((cpu.flags & Cpu.FLAG_Z) != 0, "CMP.W different values should not set zero flag");
        assertFalse((cpu.flags & Cpu.FLAG_C) != 0, "CMP.W reg>imm should not set carry flag");
    }

    @Test
    void testCMP_ImmToReg_Less() {
        // Test CMP where register < immediate
        cpu.regs[2] = 0x1000;
        loadProgram(new int[]{
            makeInstr(Cpu.CMP, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 2),
            0x2000  // Immediate value
        });
        cpu.step();
        assertFalse((cpu.flags & Cpu.FLAG_Z) != 0, "CMP.W different values should not set zero flag");
        assertTrue((cpu.flags & Cpu.FLAG_C) != 0, "CMP.W reg<imm should set carry flag");
    }

    @Test
    void testTST_ImmToReg() {
        // TST.W #1234, R2 - Test immediate value with register (bitwise AND)
        cpu.regs[2] = 0x1234;
        loadProgram(new int[]{
            makeInstr(Cpu.TST, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 2),
            0x1234  // Immediate value
        });
        cpu.step();
        assertEquals(0x1234, cpu.regs[2], "TST.W should not modify destination register");
        assertFalse((cpu.flags & Cpu.FLAG_Z) != 0, "TST.W non-zero result should not set zero flag");
    }

    @Test
    void testTST_ImmToReg_Zero() {
        // Test TST with no common bits
        cpu.regs[2] = 0x00FF;
        loadProgram(new int[]{
            makeInstr(Cpu.TST, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 2),
            0xFF00  // Immediate value with no common bits
        });
        cpu.step();
        assertEquals(0x00FF, cpu.regs[2], "TST.W should not modify destination register");
        assertTrue((cpu.flags & Cpu.FLAG_Z) != 0, "TST.W zero result should set zero flag");
    }

    // ======================= OPCODES MULTIPLICATIFS =======================

    @Test
    void testMULU_ImmToReg() {
        // MULU.W #1000, R2 - Unsigned multiply immediate value with register
        cpu.regs[2] = 0x0010;
        loadProgram(new int[]{
            makeInstr(Cpu.MULU, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 2),
            0x1000  // Immediate value
        });
        cpu.step();
        assertEquals(0x0000, cpu.regs[2], "MULU.W should put low word in R2");
        assertEquals(0x0001, cpu.regs[3], "MULU.W should put high word in R3");
    }

    @Test
    void testMULU_ImmLarge() {
        // Test MULU with large immediate values
        cpu.regs[2] = 0x8000;
        loadProgram(new int[]{
            makeInstr(Cpu.MULU, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 2),
            0x0002  // Immediate value
        });
        cpu.step();
        assertEquals(0x0000, cpu.regs[2], "MULU.W should put low word 0x0000 in R2");
        assertEquals(0x0001, cpu.regs[3], "MULU.W should put high word 0x0001 in R3");
    }

    @Test
    void testDIVU_ImmToReg() {
        // DIVU.W #2, R2 - Unsigned divide register by immediate value
        cpu.regs[2] = 0x1234;  // Dividend in register
        loadProgram(new int[]{
            makeInstr(Cpu.DIVU, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 2),
            0x0002  // Immediate divisor
        });
        cpu.step();
        assertEquals(0x091A, cpu.regs[2], "DIVU.W should put quotient in R2");
        assertEquals(0x0000, cpu.regs[3], "DIVU.W should put remainder in R3");
    }

    @Test
    void testDIVU_ImmByZero() {
        // Test DIVU division by zero
        cpu.regs[2] = 0x1234;
        loadProgram(new int[]{
            makeInstr(Cpu.DIVU, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 2),
            0x0000  // Division by zero
        });
        cpu.step();
        assertTrue((cpu.flags & Cpu.FLAG_X) != 0, "DIVU.W division by zero should set X flag");
    }

    @Test
    void testMUL_ImmToReg() {
        // MUL.W #2, R2 - Signed multiply immediate value with register
        cpu.regs[2] = 0x0003;
        loadProgram(new int[]{
            makeInstr(Cpu.MUL, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 2),
            0x0002  // Immediate value
        });
        cpu.step();
        assertEquals(0x0006, cpu.regs[2], "MUL.W should put low word of result in R2");
        assertEquals(0x0000, cpu.regs[3], "MUL.W should put high word of result in R3");
    }

    @Test
    void testDIV_ImmToReg() {
        // DIV.W #2, R2 - Signed divide register by immediate value
        cpu.regs[2] = 0x0006;  // Dividend in register
        loadProgram(new int[]{
            makeInstr(Cpu.DIV, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 2),
            0x0002  // Immediate divisor
        });
        cpu.step();
        assertEquals(0x0003, cpu.regs[2], "DIV.W should put quotient in R2");
        assertEquals(0x0000, cpu.regs[3], "DIV.W should put remainder in R3");
    }

    // ======================= OPCODES DE BITS =======================

    @Test
    void testBTST_ImmToReg() {
        // BTST.W #0, R2 - Test bit 0 in register
        cpu.regs[2] = 0x0001;  // Value with bit 0 set
        loadProgram(new int[]{
            makeInstr(Cpu.BTST, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 2),
            0x0000  // Bit position 0
        });
        cpu.step();
        assertFalse((cpu.flags & Cpu.FLAG_Z) != 0, "BTST.W with bit set should not set zero flag");
    }

    @Test
    void testBTST_ImmToReg_BitClear() {
        // Test BTST with bit clear
        cpu.regs[2] = 0x0001;  // Value with bit 1 clear
        loadProgram(new int[]{
            makeInstr(Cpu.BTST, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 2),
            0x0001  // Bit position 1
        });
        cpu.step();
        assertTrue((cpu.flags & Cpu.FLAG_Z) != 0, "BTST.W with bit clear should set zero flag");
    }

    @Test
    void testBSET_ImmToReg() {
        // BSET.W #4, R2 - Set bit 4 in register
        cpu.regs[2] = 0x0000;  // Initial value
        loadProgram(new int[]{
            makeInstr(Cpu.BSET, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 2),
            0x0004  // Bit position 4
        });
        cpu.step();
        assertEquals(0x0010, cpu.regs[2], "BSET.W should set bit 4 in register");
    }

    @Test
    void testBCLR_ImmToReg() {
        // BCLR.W #4, R2 - Clear bit 4 in register
        cpu.regs[2] = 0x0010;  // Value with bit 4 set
        loadProgram(new int[]{
            makeInstr(Cpu.BCLR, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 2),
            0x0004  // Bit position 4
        });
        cpu.step();
        assertEquals(0x0000, cpu.regs[2], "BCLR.W should clear bit 4 in register");
    }

    @Test
    void testBit_ImmToInd() {
        // BSET.W #7, [R1] - Set bit 7 in memory location
        cpu.regs[1] = 0x2000;
        cpu.writeWord(0x2000, 0x0000);  // Initial memory value
        loadProgram(new int[]{
            makeInstr(Cpu.BSET, 1, Cpu.MODE_IMM, 0, Cpu.MODE_IND, 1),
            0x0007  // Bit position 7
        });
        cpu.step();
        assertEquals(0x0080, cpu.readWord(0x2000), "BSET.W should set bit 7 in memory");
    }

    // ======================= OPCODES DE PILE =======================

    @Test
    void testPUSH_Imm() {
        // PUSH.W #1234 - Push immediate value onto stack
        cpu.sp = 0xFEFF;  // Use a safe stack pointer
        int oldSp = cpu.sp;
        loadProgram(new int[]{
            makeInstr(Cpu.PUSH, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 0),
            0x1234  // Immediate value
        });
        cpu.step();
        assertEquals(oldSp - 2, cpu.sp, "PUSH.W should decrement SP by 2");
        assertEquals(0x1234, cpu.readWord(cpu.sp), "PUSH.W should store immediate value on stack");
    }

    @Test
    void testPUSH_ImmMultiple() {
        // Test pushing multiple immediate values
        cpu.sp = 0xFEFF;
        
        // PUSH.W #1111
        loadProgram(new int[]{
            makeInstr(Cpu.PUSH, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 0),
            0x1111
        });
        cpu.step();
        assertEquals(0x1111, cpu.readWord(cpu.sp), "First PUSH should store first immediate");
        
        // PUSH.W #2222
        cpu.pc = 0;
        loadProgram(new int[]{
            makeInstr(Cpu.PUSH, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 0),
            0x2222
        });
        cpu.step();
        assertEquals(0x2222, cpu.readWord(cpu.sp), "Second PUSH should store second immediate");
        assertEquals(0x1111, cpu.readWord(cpu.sp + 2), "First value should still be on stack");
    }

    // ======================= TESTS SUPPLEMENTAIRES =======================

    @Test
    void testImmediateConstants() {
        // Test common immediate constants
        int[] constants = {0x0000, 0x0001, 0xFFFF, 0x8000, 0x7FFF, 0x00FF, 0xFF00};
        
        for (int constant : constants) {
            cpu.regs[0] = 0xAAAA;  // Initialize with known value
            cpu.pc = 0;  // Reset PC
            
            loadProgram(new int[]{
                makeInstr(Cpu.MOV, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 0),
                constant
            });
            cpu.step();
            assertEquals(constant, cpu.regs[0], "MOV.W should load constant " + Integer.toHexString(constant));
        }
    }

    @Test
    void testImmediateFlagUpdates() {
        // Test comprehensive flag updates with immediate values
        
        // Test zero flag
        cpu.regs[1] = 0x1234;
        loadProgram(new int[]{
            makeInstr(Cpu.XOR, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 1),
            0x1234  // Same value should result in zero
        });
        cpu.step();
        assertTrue((cpu.flags & Cpu.FLAG_Z) != 0, "XOR with same immediate should set zero flag");
        
        // Test negative flag
        cpu.regs[1] = 0x7FFF;
        cpu.pc = 0;
        loadProgram(new int[]{
            makeInstr(Cpu.ADD, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 1),
            0x0001  // Should cause signed overflow to negative
        });
        cpu.step();
        assertTrue((cpu.flags & Cpu.FLAG_N) != 0, "ADD immediate causing negative result should set N flag");
        assertTrue((cpu.flags & Cpu.FLAG_V) != 0, "ADD immediate causing signed overflow should set V flag");
    }

    @Test
    void testImmediateMixedOperations() {
        // Test complex operations with immediate values
        cpu.regs[0] = 0x0000;
        
        // Build value step by step using immediate operations
        loadProgram(new int[]{
            makeInstr(Cpu.ADD, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 0), 0x1000,  // R0 = 0x1000
            makeInstr(Cpu.OR, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 0), 0x0200,   // R0 |= 0x0200 = 0x1200
            makeInstr(Cpu.XOR, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 0), 0x0034   // R0 ^= 0x0034 = 0x1234
        });
        
        cpu.step();  // ADD
        assertEquals(0x1000, cpu.regs[0], "First step should add 0x1000");
        
        cpu.step();  // OR
        assertEquals(0x1200, cpu.regs[0], "Second step should OR with 0x0200");
        
        cpu.step();  // XOR
        assertEquals(0x1234, cpu.regs[0], "Final step should XOR with 0x0034 to get 0x1234");
    }

    @Test
    void testImmediateEdgeCases() {
        // Test edge cases with immediate values
        
        // Maximum immediate value
        cpu.regs[0] = 0x0000;
        loadProgram(new int[]{
            makeInstr(Cpu.MOV, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 0),
            0xFFFF  // Maximum 16-bit immediate
        });
        cpu.step();
        assertEquals(0xFFFF, cpu.regs[0], "Should handle maximum immediate value");
        
        // Minimum immediate value
        cpu.regs[0] = 0xFFFF;
        cpu.pc = 0;
        loadProgram(new int[]{
            makeInstr(Cpu.MOV, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 0),
            0x0000  // Minimum immediate value
        });
        cpu.step();
        assertEquals(0x0000, cpu.regs[0], "Should handle minimum immediate value");
    }
}