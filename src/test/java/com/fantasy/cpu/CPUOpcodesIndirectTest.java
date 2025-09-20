package com.fantasy.cpu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests complets pour tous les opcodes du Fantasy Machine 16 CPU qui supportent
 * le mode d'adressage indirect [Rn] (MODE_IND = 1).
 * 
 * Ce fichier teste systématiquement chaque opcode avec l'adressage indirect,
 * où les registres contiennent des adresses pointant vers les données en mémoire.
 */
public class CPUOpcodesIndirectTest {
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
    void testMOV_IndToReg() {
        // MOV.W [R1], R2 - Move value at address in R1 to R2
        cpu.regs[1] = 0x2000;  // R1 points to address 0x2000
        cpu.writeWord(0x2000, 0x1234);  // Store value at address 0x2000
        cpu.regs[2] = 0x0000;
        loadProgram(new int[]{makeInstr(Cpu.MOV, 1, Cpu.MODE_IND, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x1234, cpu.regs[2], "MOV.W should copy value from memory to register");
        assertEquals(0x1234, cpu.readWord(0x2000), "MOV.W should not modify source memory");
    }

    @Test
    void testMOV_RegToInd() {
        // MOV.W R1, [R2] - Move R1 to address pointed by R2
        cpu.regs[1] = 0x5678;
        cpu.regs[2] = 0x2000;  // R2 points to address 0x2000
        cpu.writeWord(0x2000, 0x0000);  // Initialize memory
        loadProgram(new int[]{makeInstr(Cpu.MOV, 1, Cpu.MODE_REG, 1, Cpu.MODE_IND, 2)});
        cpu.step();
        assertEquals(0x5678, cpu.readWord(0x2000), "MOV.W should store register value to memory");
        assertEquals(0x5678, cpu.regs[1], "MOV.W should not modify source register");
    }

    @Test
    void testMOV_IndToInd() {
        // MOV.W [R1], [R2] - Move value from address in R1 to address in R2
        cpu.regs[1] = 0x2000;  // R1 points to source
        cpu.regs[2] = 0x3000;  // R2 points to destination
        cpu.writeWord(0x2000, 0xABCD);  // Source value
        cpu.writeWord(0x3000, 0x0000);  // Initialize destination
        loadProgram(new int[]{makeInstr(Cpu.MOV, 1, Cpu.MODE_IND, 1, Cpu.MODE_IND, 2)});
        cpu.step();
        assertEquals(0xABCD, cpu.readWord(0x3000), "MOV.W should copy value from source memory to destination memory");
        assertEquals(0xABCD, cpu.readWord(0x2000), "MOV.W should not modify source memory");
    }

    @Test
    void testMOV_IndToReg_Byte() {
        // MOV.B [R1], R2 - Move byte from memory to register (byte mode)
        cpu.regs[1] = 0x2000;
        cpu.writeWord(0x2000, 0x12AB);  // Store word in memory
        cpu.regs[2] = 0xFFFF;  // Initialize with different value
        loadProgram(new int[]{makeInstr(Cpu.MOV, 0, Cpu.MODE_IND, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        // In byte mode from memory, should read the entire word
        assertEquals(0x12AB, cpu.regs[2], "MOV.B from memory should read entire word");
    }

    @Test
    void testADD_IndToReg() {
        // ADD.W [R1], R2 - Add value at address in R1 to R2
        cpu.regs[1] = 0x2000;
        cpu.writeWord(0x2000, 0x1000);  // Source value
        cpu.regs[2] = 0x0234;
        loadProgram(new int[]{makeInstr(Cpu.ADD, 1, Cpu.MODE_IND, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x1234, cpu.regs[2], "ADD.W should add memory value to register");
        assertEquals(0x1000, cpu.readWord(0x2000), "ADD.W should not modify source memory");
    }

    @Test
    void testADD_RegToInd() {
        // ADD.W R1, [R2] - Add R1 to value at address in R2
        cpu.regs[1] = 0x1000;
        cpu.regs[2] = 0x2000;
        cpu.writeWord(0x2000, 0x0234);  // Initial memory value
        loadProgram(new int[]{makeInstr(Cpu.ADD, 1, Cpu.MODE_REG, 1, Cpu.MODE_IND, 2)});
        cpu.step();
        assertEquals(0x1234, cpu.readWord(0x2000), "ADD.W should add register to memory value");
        assertEquals(0x1000, cpu.regs[1], "ADD.W should not modify source register");
    }

    @Test
    void testADD_IndToInd() {
        // ADD.W [R1], [R2] - Add value from memory to memory
        cpu.regs[1] = 0x2000;  // Source address
        cpu.regs[2] = 0x3000;  // Destination address
        cpu.writeWord(0x2000, 0x1000);  // Source value
        cpu.writeWord(0x3000, 0x0234);  // Destination value
        loadProgram(new int[]{makeInstr(Cpu.ADD, 1, Cpu.MODE_IND, 1, Cpu.MODE_IND, 2)});
        cpu.step();
        assertEquals(0x1234, cpu.readWord(0x3000), "ADD.W should add source memory to destination memory");
        assertEquals(0x1000, cpu.readWord(0x2000), "ADD.W should not modify source memory");
    }

    @Test
    void testSUB_IndToReg() {
        // SUB.W [R1], R2 - Subtract value at address in R1 from R2
        cpu.regs[1] = 0x2000;
        cpu.writeWord(0x2000, 0x0234);  // Source value
        cpu.regs[2] = 0x1234;
        loadProgram(new int[]{makeInstr(Cpu.SUB, 1, Cpu.MODE_IND, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x1000, cpu.regs[2], "SUB.W should subtract memory value from register");
    }

    @Test
    void testINC_Ind() {
        // INC.W [R1] - Increment value at address in R1
        cpu.regs[1] = 0x2000;
        cpu.writeWord(0x2000, 0x1234);
        loadProgram(new int[]{makeInstr(Cpu.INC, 1, Cpu.MODE_IND, 1, Cpu.MODE_IND, 1)});
        cpu.step();
        assertEquals(0x1235, cpu.readWord(0x2000), "INC.W should increment memory value by 1");
    }

    @Test
    void testDEC_Ind() {
        // DEC.W [R1] - Decrement value at address in R1
        cpu.regs[1] = 0x2000;
        cpu.writeWord(0x2000, 0x1234);
        loadProgram(new int[]{makeInstr(Cpu.DEC, 1, Cpu.MODE_IND, 1, Cpu.MODE_IND, 1)});
        cpu.step();
        assertEquals(0x1233, cpu.readWord(0x2000), "DEC.W should decrement memory value by 1");
    }

    @Test
    void testNEG_Ind() {
        // NEG.W [R1] - Negate value at address in R1
        cpu.regs[1] = 0x2000;
        cpu.writeWord(0x2000, 0x1234);
        loadProgram(new int[]{makeInstr(Cpu.NEG, 1, Cpu.MODE_IND, 1, Cpu.MODE_IND, 1)});
        cpu.step();
        assertEquals(0xEDCC, cpu.readWord(0x2000), "NEG.W should compute two's complement of memory value");
    }

    // ======================= OPCODES LOGIQUES =======================

    @Test
    void testAND_IndToReg() {
        // AND.W [R1], R2 - Bitwise AND memory value with register
        cpu.regs[1] = 0x2000;
        cpu.writeWord(0x2000, 0xFF0F);  // Source value
        cpu.regs[2] = 0x0FFF;
        loadProgram(new int[]{makeInstr(Cpu.AND, 1, Cpu.MODE_IND, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x0F0F, cpu.regs[2], "AND.W should perform bitwise AND with memory value");
    }

    @Test
    void testOR_IndToReg() {
        // OR.W [R1], R2 - Bitwise OR memory value with register
        cpu.regs[1] = 0x2000;
        cpu.writeWord(0x2000, 0xFF0F);  // Source value
        cpu.regs[2] = 0x0FF0;
        loadProgram(new int[]{makeInstr(Cpu.OR, 1, Cpu.MODE_IND, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0xFFFF, cpu.regs[2], "OR.W should perform bitwise OR with memory value");
    }

    @Test
    void testXOR_IndToReg() {
        // XOR.W [R1], R2 - Bitwise XOR memory value with register
        cpu.regs[1] = 0x2000;
        cpu.writeWord(0x2000, 0xFF0F);  // Source value
        cpu.regs[2] = 0x0FF0;
        loadProgram(new int[]{makeInstr(Cpu.XOR, 1, Cpu.MODE_IND, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0xF0FF, cpu.regs[2], "XOR.W should perform bitwise XOR with memory value");
    }

    @Test
    void testNOT_Ind() {
        // NOT.W [R1] - Bitwise NOT memory value
        cpu.regs[1] = 0x2000;
        cpu.writeWord(0x2000, 0xAAAA);
        loadProgram(new int[]{makeInstr(Cpu.NOT, 1, Cpu.MODE_IND, 1, Cpu.MODE_IND, 1)});
        cpu.step();
        assertEquals(0x5555, cpu.readWord(0x2000), "NOT.W should perform bitwise NOT on memory value");
    }

    // ======================= OPCODES DE COMPARAISON =======================

    @Test
    void testCMP_IndToReg() {
        // CMP.W [R1], R2 - Compare memory value with register
        cpu.regs[1] = 0x2000;
        cpu.writeWord(0x2000, 0x1234);  // Memory value
        cpu.regs[2] = 0x1234;  // Register value
        loadProgram(new int[]{makeInstr(Cpu.CMP, 1, Cpu.MODE_IND, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x1234, cpu.readWord(0x2000), "CMP.W should not modify source memory");
        assertEquals(0x1234, cpu.regs[2], "CMP.W should not modify destination register");
        assertTrue((cpu.flags & Cpu.FLAG_Z) != 0, "CMP.W equal values should set zero flag");
    }

    @Test
    void testTST_IndToReg() {
        // TST.W [R1], R2 - Test memory value with register
        cpu.regs[1] = 0x2000;
        cpu.writeWord(0x2000, 0x1234);  // Memory value
        cpu.regs[2] = 0x1234;  // Register value
        loadProgram(new int[]{makeInstr(Cpu.TST, 1, Cpu.MODE_IND, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x1234, cpu.readWord(0x2000), "TST.W should not modify source memory");
        assertEquals(0x1234, cpu.regs[2], "TST.W should not modify destination register");
        assertFalse((cpu.flags & Cpu.FLAG_Z) != 0, "TST.W non-zero result should not set zero flag");
    }

    // ======================= OPCODES MULTIPLICATIFS =======================

    @Test
    void testMULU_IndToReg() {
        // MULU.W [R1], R2 - Unsigned multiply memory value with register
        cpu.regs[1] = 0x2000;
        cpu.writeWord(0x2000, 0x1000);  // Memory value
        cpu.regs[2] = 0x0010;
        loadProgram(new int[]{makeInstr(Cpu.MULU, 1, Cpu.MODE_IND, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x0000, cpu.regs[2], "MULU.W should put low word in R2");
        assertEquals(0x0001, cpu.regs[3], "MULU.W should put high word in R3");
    }

    @Test
    void testDIVU_IndToReg() {
        // DIVU.W [R1], R2 - Unsigned divide register by memory value
        cpu.regs[1] = 0x2000;
        cpu.writeWord(0x2000, 0x0002);  // Divisor in memory
        cpu.regs[2] = 0x1234;  // Dividend in register
        loadProgram(new int[]{makeInstr(Cpu.DIVU, 1, Cpu.MODE_IND, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x091A, cpu.regs[2], "DIVU.W should put quotient in R2");
        assertEquals(0x0000, cpu.regs[3], "DIVU.W should put remainder in R3");
    }

    // ======================= OPCODES DE DECALAGE =======================

    @Test
    void testROL_Ind() {
        // ROL.W [R1] - Rotate memory value left
        cpu.regs[1] = 0x2000;
        cpu.writeWord(0x2000, 0x8001);
        loadProgram(new int[]{makeInstr(Cpu.ROL, 1, Cpu.MODE_IND, 1, Cpu.MODE_IND, 1)});
        cpu.step();
        assertEquals(0x0003, cpu.readWord(0x2000), "ROL.W should rotate memory value left");
    }

    @Test
    void testROR_Ind() {
        // ROR.W [R1] - Rotate memory value right
        cpu.regs[1] = 0x2000;
        cpu.writeWord(0x2000, 0x8001);
        loadProgram(new int[]{makeInstr(Cpu.ROR, 1, Cpu.MODE_IND, 1, Cpu.MODE_IND, 1)});
        cpu.step();
        assertEquals(0xC000, cpu.readWord(0x2000), "ROR.W should rotate memory value right");
    }

    @Test
    void testSHL_Ind() {
        // SHL.W [R1] - Shift memory value left
        cpu.regs[1] = 0x2000;
        cpu.writeWord(0x2000, 0x1234);
        loadProgram(new int[]{makeInstr(Cpu.SHL, 1, Cpu.MODE_IND, 1, Cpu.MODE_IND, 1)});
        cpu.step();
        assertEquals(0x2468, cpu.readWord(0x2000), "SHL.W should shift memory value left");
    }

    @Test
    void testSHR_Ind() {
        // SHR.W [R1] - Shift memory value right
        cpu.regs[1] = 0x2000;
        cpu.writeWord(0x2000, 0x1234);
        loadProgram(new int[]{makeInstr(Cpu.SHR, 1, Cpu.MODE_IND, 1, Cpu.MODE_IND, 1)});
        cpu.step();
        assertEquals(0x091A, cpu.readWord(0x2000), "SHR.W should shift memory value right");
    }

    // ======================= OPCODES DE BITS =======================

    @Test
    void testBTST_IndToReg() {
        // BTST.W [R1], R2 - Test bit from memory in register
        cpu.regs[1] = 0x2000;
        cpu.writeWord(0x2000, 0x0000);  // Bit position 0
        cpu.regs[2] = 0x0001;  // Value with bit 0 set
        loadProgram(new int[]{makeInstr(Cpu.BTST, 1, Cpu.MODE_IND, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertFalse((cpu.flags & Cpu.FLAG_Z) != 0, "BTST.W with bit set should not set zero flag");
    }

    @Test
    void testBSET_IndToReg() {
        // BSET.W [R1], R2 - Set bit from memory position in register
        cpu.regs[1] = 0x2000;
        cpu.writeWord(0x2000, 0x0004);  // Bit position 4
        cpu.regs[2] = 0x0000;  // Initial value
        loadProgram(new int[]{makeInstr(Cpu.BSET, 1, Cpu.MODE_IND, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x0010, cpu.regs[2], "BSET.W should set bit 4 in register");
    }

    @Test
    void testBCLR_IndToReg() {
        // BCLR.W [R1], R2 - Clear bit from memory position in register
        cpu.regs[1] = 0x2000;
        cpu.writeWord(0x2000, 0x0004);  // Bit position 4
        cpu.regs[2] = 0x0010;  // Value with bit 4 set
        loadProgram(new int[]{makeInstr(Cpu.BCLR, 1, Cpu.MODE_IND, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x0000, cpu.regs[2], "BCLR.W should clear bit 4 in register");
    }

    // ======================= OPCODES DE PILE =======================

    @Test
    void testPUSH_Ind() {
        // PUSH.W [R1] - Push memory value onto stack
        cpu.regs[1] = 0x2000;
        cpu.writeWord(0x2000, 0x1234);
        cpu.sp = 0xFEFF;  // Use a safe stack pointer
        int oldSp = cpu.sp;
        loadProgram(new int[]{makeInstr(Cpu.PUSH, 1, Cpu.MODE_IND, 1, Cpu.MODE_REG, 0)});
        cpu.step();
        assertEquals(oldSp - 2, cpu.sp, "PUSH.W should decrement SP by 2");
        assertEquals(0x1234, cpu.readWord(cpu.sp), "PUSH.W should store memory value on stack");
    }

    @Test
    void testPOP_Ind() {
        // POP.W [R1] - Pop from stack to memory location
        cpu.regs[1] = 0x2000;  // Address to store popped value
        cpu.sp = 0xFBFD; // Use safe stack address
        cpu.writeWord(cpu.sp, 0x5678);  // Value on stack
        cpu.writeWord(0x2000, 0x0000);  // Initialize memory
        int oldSp = cpu.sp;
        loadProgram(new int[]{makeInstr(Cpu.POP, 1, Cpu.MODE_REG, 0, Cpu.MODE_IND, 1)});
        cpu.step();
        assertEquals(0x5678, cpu.readWord(0x2000), "POP.W should load value from stack to memory");
        assertEquals(oldSp + 2, cpu.sp, "POP.W should increment SP by 2");
    }

    // ======================= OPCODES DE SAUT =======================

    @Test
    void testJMP_Ind() {
        // JMP [R1] - Jump to address stored in memory
        cpu.regs[1] = 0x2000;
        cpu.writeWord(0x2000, 0x4000);  // Target address in memory
        loadProgram(new int[]{makeInstr(Cpu.JMP, 1, Cpu.MODE_REG, 0, Cpu.MODE_IND, 1)});
        cpu.step();
        assertEquals(0x4000, cpu.pc, "JMP should jump to address stored in memory");
    }

    @Test
    void testCALL_Ind() {
        // CALL [R1] - Call subroutine at address stored in memory
        cpu.regs[1] = 0x2000;
        cpu.writeWord(0x2000, 0x4000);  // Target address in memory
        cpu.sp = 0xFEFF;  // Safe stack pointer
        int oldSp = cpu.sp;
        int returnAddr = cpu.pc + 2; // Expected return address
        loadProgram(new int[]{makeInstr(Cpu.CALL, 1, Cpu.MODE_REG, 0, Cpu.MODE_IND, 1)});
        cpu.step();
        assertEquals(0x4000, cpu.pc, "CALL should jump to address stored in memory");
        assertEquals(oldSp - 2, cpu.sp, "CALL should decrement SP by 2");
        assertEquals(returnAddr, cpu.readWord(cpu.sp), "CALL should push return address on stack");
    }

    // ======================= TESTS SUPPLEMENTAIRES =======================

    @Test
    void testIndirectAddressingBounds() {
        // Test indirect addressing at memory boundaries
        cpu.regs[1] = 0xFFFE;  // Near end of 64K space
        cpu.writeWord(0xFFFE, 0x1234);
        cpu.regs[2] = 0x0000;
        loadProgram(new int[]{makeInstr(Cpu.MOV, 1, Cpu.MODE_IND, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x1234, cpu.regs[2], "Indirect addressing should work at memory boundaries");
    }

    @Test
    void testIndirectWithBanking() {
        // Test indirect addressing with banking
        cpu.bankReg = 0;  // Bank0
        cpu.regs[1] = 0x1000;
        cpu.writeWord(0x1000, 0xABCD);  // Write to Bank0
        cpu.regs[2] = 0x0000;
        
        loadProgram(new int[]{makeInstr(Cpu.MOV, 1, Cpu.MODE_IND, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0xABCD, cpu.regs[2], "Indirect addressing should respect current bank");
        
        // Switch to Bank1 and verify different data
        cpu.writeByte(Cpu.BANK_REG, (byte)1);  // Switch to Bank1
        cpu.pc = 0;  // Reset PC
        cpu.regs[2] = 0x0000;  // Reset register
        cpu.step();
        // Bank1 should have different data (or zeros)
        assertNotEquals(0xABCD, cpu.regs[2], "Bank1 should have different data than Bank0");
    }

    @Test
    void testFlagsWithIndirectOperations() {
        // Test flag setting with indirect operations
        cpu.regs[1] = 0x2000;
        cpu.writeWord(0x2000, 0x7FFF);  // Maximum positive value
        cpu.regs[2] = 0x0001;
        
        // ADD.W [R1], R2 - Should cause overflow
        loadProgram(new int[]{makeInstr(Cpu.ADD, 1, Cpu.MODE_IND, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x8000, cpu.regs[2], "ADD.W should compute correct result");
        assertTrue((cpu.flags & Cpu.FLAG_V) != 0, "ADD.W signed overflow should set V flag");
        assertTrue((cpu.flags & Cpu.FLAG_N) != 0, "ADD.W negative result should set N flag");
    }
}