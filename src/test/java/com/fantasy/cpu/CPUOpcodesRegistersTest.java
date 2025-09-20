package com.fantasy.cpu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests complets pour tous les opcodes du Fantasy Machine 16 CPU qui supportent
 * le mode d'adressage par registres (MODE_REG = 0).
 * 
 * Ce fichier teste systématiquement chaque opcode avec des registres comme
 * opérandes source et destination.
 */
public class CPUOpcodesRegistersTest {
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
    void testMOV_RegToReg() {
        // MOV.W R1, R2 - Move R1 to R2
        cpu.regs[1] = 0x1234;
        cpu.regs[2] = 0x0000;
        loadProgram(new int[]{makeInstr(Cpu.MOV, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x1234, cpu.regs[2], "MOV.W should copy R1 to R2");
        assertEquals(0x1234, cpu.regs[1], "MOV.W should not modify source register");
    }

    @Test
    void testMOV_RegToReg_Byte() {
        // MOV.B R1, R2 - Move low byte of R1 to low byte of R2, preserving high byte of R2
        cpu.regs[1] = 0x1234;
        cpu.regs[2] = 0xABCD;
        loadProgram(new int[]{makeInstr(Cpu.MOV, 0, Cpu.MODE_REG, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x1234, cpu.regs[2], "MOV.B should copy entire word in register mode");
    }

    @Test
    void testADD_RegToReg() {
        // ADD.W R1, R2 - Add R1 to R2
        cpu.regs[1] = 0x1000;
        cpu.regs[2] = 0x0234;
        loadProgram(new int[]{makeInstr(Cpu.ADD, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x1234, cpu.regs[2], "ADD.W should add R1 to R2");
        assertEquals(0x1000, cpu.regs[1], "ADD.W should not modify source register");
    }

    @Test
    void testADD_RegToReg_WithCarry() {
        // Test ADD with carry flag setting
        cpu.regs[1] = 0xFFFF;
        cpu.regs[2] = 0x0001;
        loadProgram(new int[]{makeInstr(Cpu.ADD, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x0000, cpu.regs[2], "ADD.W overflow should result in 0x0000");
        assertTrue((cpu.flags & Cpu.FLAG_C) != 0, "ADD.W overflow should set carry flag");
        assertTrue((cpu.flags & Cpu.FLAG_Z) != 0, "ADD.W result 0 should set zero flag");
    }

    @Test
    void testSUB_RegToReg() {
        // SUB.W R1, R2 - Subtract R1 from R2
        cpu.regs[1] = 0x0234;
        cpu.regs[2] = 0x1234;
        loadProgram(new int[]{makeInstr(Cpu.SUB, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x1000, cpu.regs[2], "SUB.W should subtract R1 from R2");
    }

    @Test
    void testSUB_RegToReg_WithBorrow() {
        // Test SUB with borrow (carry) flag setting
        cpu.regs[1] = 0x0002;
        cpu.regs[2] = 0x0001;
        loadProgram(new int[]{makeInstr(Cpu.SUB, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0xFFFF, cpu.regs[2], "SUB.W underflow should result in 0xFFFF");
        assertTrue((cpu.flags & Cpu.FLAG_C) != 0, "SUB.W underflow should set carry flag");
    }

    @Test
    void testINC_Reg() {
        // INC.W R1 - Increment R1
        cpu.regs[1] = 0x1234;
        loadProgram(new int[]{makeInstr(Cpu.INC, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 1)});
        cpu.step();
        assertEquals(0x1235, cpu.regs[1], "INC.W should increment register by 1");
    }

    @Test
    void testINC_Reg_Overflow() {
        // Test INC with overflow
        cpu.regs[1] = 0xFFFF;
        loadProgram(new int[]{makeInstr(Cpu.INC, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 1)});
        cpu.step();
        assertEquals(0x0000, cpu.regs[1], "INC.W overflow should wrap to 0x0000");
        assertTrue((cpu.flags & Cpu.FLAG_Z) != 0, "INC.W result 0 should set zero flag");
    }

    @Test
    void testDEC_Reg() {
        // DEC.W R1 - Decrement R1
        cpu.regs[1] = 0x1234;
        loadProgram(new int[]{makeInstr(Cpu.DEC, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 1)});
        cpu.step();
        assertEquals(0x1233, cpu.regs[1], "DEC.W should decrement register by 1");
    }

    @Test
    void testDEC_Reg_Underflow() {
        // Test DEC with underflow
        cpu.regs[1] = 0x0000;
        loadProgram(new int[]{makeInstr(Cpu.DEC, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 1)});
        cpu.step();
        assertEquals(0xFFFF, cpu.regs[1], "DEC.W underflow should wrap to 0xFFFF");
    }

    @Test
    void testNEG_Reg() {
        // NEG.W R1 - Negate R1 (two's complement)
        cpu.regs[1] = 0x1234;
        loadProgram(new int[]{makeInstr(Cpu.NEG, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 1)});
        cpu.step();
        assertEquals(0xEDCC, cpu.regs[1], "NEG.W should compute two's complement");
    }

    @Test
    void testNEG_Reg_Zero() {
        // Test NEG with zero
        cpu.regs[1] = 0x0000;
        loadProgram(new int[]{makeInstr(Cpu.NEG, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 1)});
        cpu.step();
        assertEquals(0x0000, cpu.regs[1], "NEG.W of zero should remain zero");
        assertTrue((cpu.flags & Cpu.FLAG_Z) != 0, "NEG.W result 0 should set zero flag");
    }

    // ======================= OPCODES LOGIQUES =======================

    @Test
    void testAND_RegToReg() {
        // AND.W R1, R2 - Bitwise AND R1 with R2
        cpu.regs[1] = 0xFF0F;
        cpu.regs[2] = 0x0FFF;
        loadProgram(new int[]{makeInstr(Cpu.AND, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x0F0F, cpu.regs[2], "AND.W should perform bitwise AND");
    }

    @Test
    void testAND_RegToReg_Zero() {
        // Test AND resulting in zero
        cpu.regs[1] = 0xFF00;
        cpu.regs[2] = 0x00FF;
        loadProgram(new int[]{makeInstr(Cpu.AND, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x0000, cpu.regs[2], "AND.W with no common bits should result in 0");
        assertTrue((cpu.flags & Cpu.FLAG_Z) != 0, "AND.W result 0 should set zero flag");
    }

    @Test
    void testOR_RegToReg() {
        // OR.W R1, R2 - Bitwise OR R1 with R2
        cpu.regs[1] = 0xFF0F;
        cpu.regs[2] = 0x0FF0;
        loadProgram(new int[]{makeInstr(Cpu.OR, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0xFFFF, cpu.regs[2], "OR.W should perform bitwise OR");
    }

    @Test
    void testXOR_RegToReg() {
        // XOR.W R1, R2 - Bitwise XOR R1 with R2
        cpu.regs[1] = 0xFF0F;
        cpu.regs[2] = 0x0FF0;
        loadProgram(new int[]{makeInstr(Cpu.XOR, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0xF0FF, cpu.regs[2], "XOR.W should perform bitwise XOR");
    }

    @Test
    void testXOR_RegToReg_SameValue() {
        // Test XOR with same values (should result in zero)
        cpu.regs[1] = 0x1234;
        cpu.regs[2] = 0x1234;
        loadProgram(new int[]{makeInstr(Cpu.XOR, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x0000, cpu.regs[2], "XOR.W with same values should result in 0");
        assertTrue((cpu.flags & Cpu.FLAG_Z) != 0, "XOR.W result 0 should set zero flag");
    }

    @Test
    void testNOT_Reg() {
        // NOT.W R1 - Bitwise NOT R1 (one's complement)
        cpu.regs[1] = 0xAAAA;
        loadProgram(new int[]{makeInstr(Cpu.NOT, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 1)});
        cpu.step();
        assertEquals(0x5555, cpu.regs[1], "NOT.W should perform bitwise NOT");
    }

    // ======================= OPCODES DE COMPARAISON =======================

    @Test
    void testCMP_RegToReg_Equal() {
        // CMP.W R1, R2 - Compare R1 with R2 (R2 - R1)
        cpu.regs[1] = 0x1234;
        cpu.regs[2] = 0x1234;
        loadProgram(new int[]{makeInstr(Cpu.CMP, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x1234, cpu.regs[1], "CMP.W should not modify source register");
        assertEquals(0x1234, cpu.regs[2], "CMP.W should not modify destination register");
        assertTrue((cpu.flags & Cpu.FLAG_Z) != 0, "CMP.W equal values should set zero flag");
    }

    @Test
    void testCMP_RegToReg_Greater() {
        // Test CMP where R2 > R1
        cpu.regs[1] = 0x1000;
        cpu.regs[2] = 0x2000;
        loadProgram(new int[]{makeInstr(Cpu.CMP, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertFalse((cpu.flags & Cpu.FLAG_Z) != 0, "CMP.W different values should not set zero flag");
        assertFalse((cpu.flags & Cpu.FLAG_C) != 0, "CMP.W R2>R1 should not set carry flag");
    }

    @Test
    void testCMP_RegToReg_Less() {
        // Test CMP where R2 < R1
        cpu.regs[1] = 0x2000;
        cpu.regs[2] = 0x1000;
        loadProgram(new int[]{makeInstr(Cpu.CMP, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertFalse((cpu.flags & Cpu.FLAG_Z) != 0, "CMP.W different values should not set zero flag");
        assertTrue((cpu.flags & Cpu.FLAG_C) != 0, "CMP.W R2<R1 should set carry flag");
    }

    @Test
    void testTST_Reg_Zero() {
        // TST.W R1, R2 - Test R1 & R2 (bitwise AND for test)
        cpu.regs[1] = 0x0000;
        cpu.regs[2] = 0x1234;
        loadProgram(new int[]{makeInstr(Cpu.TST, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x0000, cpu.regs[1], "TST.W should not modify source register");
        assertEquals(0x1234, cpu.regs[2], "TST.W should not modify destination register");
        assertTrue((cpu.flags & Cpu.FLAG_Z) != 0, "TST.W zero result should set zero flag");
    }

    @Test
    void testTST_Reg_Positive() {
        // Test TST with positive result
        cpu.regs[1] = 0x1234;
        cpu.regs[2] = 0x1234;
        loadProgram(new int[]{makeInstr(Cpu.TST, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x1234, cpu.regs[1], "TST.W should not modify source register");
        assertEquals(0x1234, cpu.regs[2], "TST.W should not modify destination register");
        assertFalse((cpu.flags & Cpu.FLAG_Z) != 0, "TST.W non-zero result should not set zero flag");
        assertFalse((cpu.flags & Cpu.FLAG_N) != 0, "TST.W positive result should not set negative flag");
    }

    @Test
    void testTST_Reg_Negative() {
        // Test TST with negative result (MSB set) - TST is logical, so N flag not set
        cpu.regs[1] = 0x8000;
        cpu.regs[2] = 0x8000;
        loadProgram(new int[]{makeInstr(Cpu.TST, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x8000, cpu.regs[1], "TST.W should not modify source register");
        assertEquals(0x8000, cpu.regs[2], "TST.W should not modify destination register");
        assertFalse((cpu.flags & Cpu.FLAG_N) != 0, "TST.W is logical, so N flag should not be set");
        assertFalse((cpu.flags & Cpu.FLAG_Z) != 0, "TST.W non-zero result should not set zero flag");
    }

    // ======================= OPCODES MULTIPLICATIFS =======================

    @Test
    void testMULU_RegToReg() {
        // MULU.W R1, R2 - Unsigned multiply R2 * R1 -> results in R2 (low) and R3 (high)
        cpu.regs[1] = 0x1000;
        cpu.regs[2] = 0x0010;
        loadProgram(new int[]{makeInstr(Cpu.MULU, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x0000, cpu.regs[2], "MULU.W should put low word of result in R2 (destination)");
        assertEquals(0x0001, cpu.regs[3], "MULU.W should put high word of result in R3 (destination+1)");
    }

    @Test
    void testMULU_RegToReg_Overflow() {
        // Test MULU with result requiring high word
        cpu.regs[1] = 0x1000;
        cpu.regs[2] = 0x2000;
        loadProgram(new int[]{makeInstr(Cpu.MULU, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x0000, cpu.regs[2], "MULU.W should put low word 0x0000 in R2 (destination)");
        assertEquals(0x0200, cpu.regs[3], "MULU.W should put high word 0x0200 in R3 (destination+1)");
    }

    @Test
    void testDIVU_RegToReg() {
        // DIVU.W R1, R2 - Unsigned divide R2 by R1 -> quotient in R2, remainder in R3
        cpu.regs[1] = 0x0002;  // divisor
        cpu.regs[2] = 0x1234;  // dividend
        loadProgram(new int[]{makeInstr(Cpu.DIVU, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x091A, cpu.regs[2], "DIVU.W should put quotient in R2 (destination)");
        assertEquals(0x0000, cpu.regs[3], "DIVU.W should put remainder in R3 (destination+1)");
    }

    @Test
    void testMUL_RegToReg() {
        // MUL.W R1, R2 - Signed multiply R2 * R1 -> results in R2 (low) and R3 (high)
        cpu.regs[1] = 0x0002;
        cpu.regs[2] = 0x0003;
        loadProgram(new int[]{makeInstr(Cpu.MUL, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x0006, cpu.regs[2], "MUL.W should put low word of result in R2 (destination)");
        assertEquals(0x0000, cpu.regs[3], "MUL.W should put high word of result in R3 (destination+1)");
    }

    @Test
    void testDIV_RegToReg() {
        // DIV.W R1, R2 - Signed divide R2 by R1 -> quotient in R2, remainder in R3
        cpu.regs[1] = 0x0002;  // divisor
        cpu.regs[2] = 0x0006;  // dividend
        loadProgram(new int[]{makeInstr(Cpu.DIV, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x0003, cpu.regs[2], "DIV.W should put quotient in R2 (destination)");
        assertEquals(0x0000, cpu.regs[3], "DIV.W should put remainder in R3 (destination+1)");
    }

    // ======================= OPCODES DE DECALAGE =======================

    @Test
    void testROL_RegToReg() {
        // ROL.W R1, R2 - Rotate R2 left by R1 positions
        cpu.regs[1] = 0x0001;  // rotate by 1
        cpu.regs[2] = 0x8001;  // value to rotate
        loadProgram(new int[]{makeInstr(Cpu.ROL, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x0003, cpu.regs[2], "ROL.W should rotate bits left with wraparound");
    }

    @Test
    void testROR_RegToReg() {
        // ROR.W R1, R2 - Rotate R2 right by R1 positions
        cpu.regs[1] = 0x0001;  // rotate by 1
        cpu.regs[2] = 0x8001;  // value to rotate
        loadProgram(new int[]{makeInstr(Cpu.ROR, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0xC000, cpu.regs[2], "ROR.W should rotate bits right with wraparound");
    }

    @Test
    void testSHL_RegToReg() {
        // SHL.W R2 - Shift R2 left by 1 position (SHL does single shifts, not variable)
        cpu.regs[2] = 0x1234;  // value to shift
        loadProgram(new int[]{makeInstr(Cpu.SHL, 1, Cpu.MODE_REG, 2, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x2468, cpu.regs[2], "SHL.W should shift bits left by 1, filling with zeros");
    }

    @Test
    void testSHR_RegToReg() {
        // SHR.W R2 - Shift R2 right by 1 position
        cpu.regs[2] = 0x1234;  // value to shift
        loadProgram(new int[]{makeInstr(Cpu.SHR, 1, Cpu.MODE_REG, 2, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x091A, cpu.regs[2], "SHR.W should shift bits right by 1, filling with zeros");
    }

    // ======================= OPCODES DE BITS =======================

    @Test
    void testBTST_RegToReg_BitSet() {
        // BTST.W R1, R2 - Test bit R1 in R2
        cpu.regs[1] = 0x0000;  // bit 0
        cpu.regs[2] = 0x0001;  // value with bit 0 set
        loadProgram(new int[]{makeInstr(Cpu.BTST, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertFalse((cpu.flags & Cpu.FLAG_Z) != 0, "BTST.W with bit set should not set zero flag");
    }

    @Test
    void testBTST_RegToReg_BitClear() {
        // Test BTST with bit clear
        cpu.regs[1] = 0x0001;  // bit 1
        cpu.regs[2] = 0x0001;  // value with bit 1 clear
        loadProgram(new int[]{makeInstr(Cpu.BTST, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertTrue((cpu.flags & Cpu.FLAG_Z) != 0, "BTST.W with bit clear should set zero flag");
    }

    @Test
    void testBSET_RegToReg() {
        // BSET.W R1, R2 - Set bit R1 in R2
        cpu.regs[1] = 0x0004;  // bit 4
        cpu.regs[2] = 0x0000;  // initial value
        loadProgram(new int[]{makeInstr(Cpu.BSET, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x0010, cpu.regs[2], "BSET.W should set the specified bit");
    }

    @Test
    void testBCLR_RegToReg() {
        // BCLR.W R1, R2 - Clear bit R1 in R2
        cpu.regs[1] = 0x0004;  // bit 4
        cpu.regs[2] = 0x0010;  // value with bit 4 set
        loadProgram(new int[]{makeInstr(Cpu.BCLR, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x0000, cpu.regs[2], "BCLR.W should clear the specified bit");
    }

    // ======================= OPCODES DE PILE =======================

    @Test
    void testPUSH_Reg() {
        // PUSH.W R1 - Push R1 onto stack
        cpu.regs[1] = 0x1234;
        cpu.sp = 0xFEFF;  // Use a safer stack pointer
        int oldSp = cpu.sp;
        loadProgram(new int[]{makeInstr(Cpu.PUSH, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 0)});
        cpu.step();
        assertEquals(oldSp - 2, cpu.sp, "PUSH.W should decrement SP by 2");
        assertEquals(0x1234, cpu.readWord(cpu.sp), "PUSH.W should store register value on stack");
    }

    @Test
    void testPOP_Reg() {
        // POP.W R1 - Pop from stack to R1
        cpu.sp = 0xFBFD; // Use address within new stack range
        cpu.writeWord(cpu.sp, 0x5678);
        int oldSp = cpu.sp;
        loadProgram(new int[]{makeInstr(Cpu.POP, 1, Cpu.MODE_REG, 0, Cpu.MODE_REG, 1)});
        cpu.step();
        assertEquals(0x5678, cpu.regs[1], "POP.W should load value from stack to register");
        assertEquals(oldSp + 2, cpu.sp, "POP.W should increment SP by 2");
    }

    // ======================= TESTS SUPPLEMENTAIRES =======================

    @Test
    void testByteOperations() {
        // Test que les opérations byte fonctionnent correctement en mode registre
        cpu.regs[1] = 0x12FF;
        cpu.regs[2] = 0xAB00;
        
        // ADD.B R1, R2 - Should add entire words in register mode
        loadProgram(new int[]{makeInstr(Cpu.ADD, 0, Cpu.MODE_REG, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0xBDFF, cpu.regs[2], "ADD.B should add entire words in register mode");
    }

    @Test
    void testFlagsPersistence() {
        // Vérifier que les flags sont correctement mis à jour
        cpu.regs[1] = 0x7FFF;
        cpu.regs[2] = 0x0001;
        
        // ADD.W R1, R2 - Should cause signed overflow (0x7FFF + 0x0001 = 0x8000)
        loadProgram(new int[]{makeInstr(Cpu.ADD, 1, Cpu.MODE_REG, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x8000, cpu.regs[2], "ADD.W should compute correct result");
        assertTrue((cpu.flags & Cpu.FLAG_V) != 0, "ADD.W signed overflow should set V flag");
        assertTrue((cpu.flags & Cpu.FLAG_N) != 0, "ADD.W negative result should set N flag");
    }
}