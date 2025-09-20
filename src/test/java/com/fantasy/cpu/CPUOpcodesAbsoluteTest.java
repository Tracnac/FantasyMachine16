package com.fantasy.cpu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests comple    @Test
    void t    @Test
    void testNEG_Abs() {
        // NEG.W $2000 - Negate value at absolute address (two's complement)
        cpu.writeWord(0x2000, 0x1234);
        loadProgram(new int[]{
            makeInstr(Cpu.NEG, 1, 0, 0, Cpu.MODE_ABS, 0),
            0x2000  // Absolute address
        });
        cpu.step();
        assertEquals(0xEDCC, cpu.readWord(0x2000), "NEG.W should compute two's complement of absolute address value");
    }) {
        // NEG.W $2000 - Neg    @Test
    void testNOT_Abs() {
        // NOT.W $2000 - Bitwise NOT of value at absolute address
        cpu.writeWord(0x2000, 0xAAAA);
        loadProgram(new int[]{
            makeInstr(Cpu.NOT, 1, 0, 0, Cpu.MODE_ABS, 0),
            0x2000  // Absolute address
        });
        cpu.step();
        assertEquals(0x5555, cpu.readWord(0x2000), "NOT.W should perform bitwise NOT on absolute address value");
    }t absolute address (two's complement)
        cpu.writeWord(0x2000, 0x1234);
        loadProgram(new int[]{
            makeInstr(Cpu.NEG, 1, 0, 0, Cpu.MODE_ABS, 0),
            0x2000  // Absolute address
        });
        cpu.step();
        assertEquals(0xEDCC, cpu.readWord(0x2000), "NEG.W should compute two's complement of absolute address value");
    }s les opcodes du Fantasy Machine 16 CPU qui supportent
 * le mode d'adressage absolu $addr (MODE_ABS = 2).
 * 
 * Ce fichier teste systématiquement chaque opcode avec l'adressage absolu,
 * où les adresses mémoire sont spécifiées directement dans l'instruction.
 */
public class CPUOpcodesAbsoluteTest {
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
    void testMOV_AbsToReg() {
        // MOV.W $2000, R2 - Move value from absolute address to register
        cpu.writeWord(0x2000, 0x1234);  // Store value at absolute address
        cpu.regs[2] = 0x0000;
        loadProgram(new int[]{
            makeInstr(Cpu.MOV, 1, Cpu.MODE_ABS, 0, Cpu.MODE_REG, 2),
            0x2000  // Absolute address
        });
        cpu.step();
        assertEquals(0x1234, cpu.regs[2], "MOV.W should copy value from absolute address to register");
        assertEquals(0x1234, cpu.readWord(0x2000), "MOV.W should not modify source memory");
    }

    @Test
    void testMOV_RegToAbs() {
        // MOV.W R1, $2000 - Move register to absolute address
        cpu.regs[1] = 0x5678;
        cpu.writeWord(0x2000, 0x0000);  // Initialize memory
        loadProgram(new int[]{
            makeInstr(Cpu.MOV, 1, Cpu.MODE_REG, 1, Cpu.MODE_ABS, 0),
            0x2000  // Absolute address
        });
        cpu.step();
        assertEquals(0x5678, cpu.readWord(0x2000), "MOV.W should store register value at absolute address");
        assertEquals(0x5678, cpu.regs[1], "MOV.W should not modify source register");
    }

    @Test
    void testMOV_AbsToAbs() {
        // MOV.W $2000, $3000 - Move from absolute address to absolute address
        cpu.writeWord(0x2000, 0xABCD);  // Source value
        cpu.writeWord(0x3000, 0x0000);  // Initialize destination
        loadProgram(new int[]{
            makeInstr(Cpu.MOV, 1, Cpu.MODE_ABS, 0, Cpu.MODE_ABS, 0),
            0x2000,  // Source absolute address
            0x3000   // Destination absolute address
        });
        cpu.step();
        assertEquals(0xABCD, cpu.readWord(0x3000), "MOV.W should copy value from source to destination address");
        assertEquals(0xABCD, cpu.readWord(0x2000), "MOV.W should not modify source memory");
    }

    @Test
    void testMOV_AbsToReg_Byte() {
        // MOV.B $2000, R2 - Move byte from absolute address to register
        cpu.writeWord(0x2000, 0x12AB);  // Store word in memory
        cpu.regs[2] = 0xFFFF;  // Initialize with different value
        loadProgram(new int[]{
            makeInstr(Cpu.MOV, 0, Cpu.MODE_ABS, 0, Cpu.MODE_REG, 2),
            0x2000  // Absolute address
        });
        cpu.step();
        // In byte mode from memory, should read the entire word
        assertEquals(0x12AB, cpu.regs[2], "MOV.B from absolute address should read entire word");
    }

    @Test
    void testADD_AbsToReg() {
        // ADD.W $2000, R2 - Add value at absolute address to register
        cpu.writeWord(0x2000, 0x1000);  // Source value
        cpu.regs[2] = 0x0234;
        loadProgram(new int[]{
            makeInstr(Cpu.ADD, 1, Cpu.MODE_ABS, 0, Cpu.MODE_REG, 2),
            0x2000  // Absolute address
        });
        cpu.step();
        assertEquals(0x1234, cpu.regs[2], "ADD.W should add absolute address value to register");
        assertEquals(0x1000, cpu.readWord(0x2000), "ADD.W should not modify source memory");
    }

    @Test
    void testADD_RegToAbs() {
        // ADD.W R1, $2000 - Add register to value at absolute address
        cpu.regs[1] = 0x1000;
        cpu.writeWord(0x2000, 0x0234);  // Initial memory value
        loadProgram(new int[]{
            makeInstr(Cpu.ADD, 1, Cpu.MODE_REG, 1, Cpu.MODE_ABS, 0),
            0x2000  // Absolute address
        });
        cpu.step();
        assertEquals(0x1234, cpu.readWord(0x2000), "ADD.W should add register to absolute address value");
        assertEquals(0x1000, cpu.regs[1], "ADD.W should not modify source register");
    }

    @Test
    void testADD_AbsToAbs() {
        // ADD.W $2000, $3000 - Add value from absolute to absolute address
        cpu.writeWord(0x2000, 0x1000);  // Source value
        cpu.writeWord(0x3000, 0x0234);  // Destination value
        loadProgram(new int[]{
            makeInstr(Cpu.ADD, 1, Cpu.MODE_ABS, 0, Cpu.MODE_ABS, 0),
            0x2000,  // Source absolute address
            0x3000   // Destination absolute address
        });
        cpu.step();
        assertEquals(0x1234, cpu.readWord(0x3000), "ADD.W should add source absolute to destination absolute");
        assertEquals(0x1000, cpu.readWord(0x2000), "ADD.W should not modify source memory");
    }

    @Test
    void testSUB_AbsToReg() {
        // SUB.W $2000, R2 - Subtract value at absolute address from register
        cpu.writeWord(0x2000, 0x0234);  // Source value
        cpu.regs[2] = 0x1234;
        loadProgram(new int[]{
            makeInstr(Cpu.SUB, 1, Cpu.MODE_ABS, 0, Cpu.MODE_REG, 2),
            0x2000  // Absolute address
        });
        cpu.step();
        assertEquals(0x1000, cpu.regs[2], "SUB.W should subtract absolute address value from register");
    }

    @Test
    void testINC_Abs() {
        // INC.W $2000 - Increment value at absolute address
        cpu.writeWord(0x2000, 0x1234);
        loadProgram(new int[]{
            makeInstr(Cpu.INC, 1, 0, 0, Cpu.MODE_ABS, 0),
            0x2000  // Absolute address
        });
        cpu.step();
        assertEquals(0x1235, cpu.readWord(0x2000), "INC.W should increment value at absolute address by 1");
    }

    @Test
    void testDEC_Abs() {
        // DEC.W $2000 - Decrement value at absolute address
        cpu.writeWord(0x2000, 0x1234);
        loadProgram(new int[]{
            makeInstr(Cpu.DEC, 1, 0, 0, Cpu.MODE_ABS, 0),
            0x2000  // Absolute address
        });
        cpu.step();
        assertEquals(0x1233, cpu.readWord(0x2000), "DEC.W should decrement value at absolute address by 1");
    }

    @Test
    void testNEG_Abs() {
        // NEG.W $2000 - Negate value at absolute address (two's complement)
        cpu.writeWord(0x2000, 0x1234);
        loadProgram(new int[]{
            makeInstr(Cpu.NEG, 1, 0, 0, Cpu.MODE_ABS, 0),
            0x2000  // Absolute address
        });
        cpu.step();
        assertEquals(0xEDCC, cpu.readWord(0x2000), "NEG.W should compute two's complement of absolute address value");
    }

    // ======================= OPCODES LOGIQUES =======================

    @Test
    void testAND_AbsToReg() {
        // AND.W $2000, R2 - Bitwise AND absolute address value with register
        cpu.writeWord(0x2000, 0xFF0F);  // Source value
        cpu.regs[2] = 0x0FFF;
        loadProgram(new int[]{
            makeInstr(Cpu.AND, 1, Cpu.MODE_ABS, 0, Cpu.MODE_REG, 2),
            0x2000  // Absolute address
        });
        cpu.step();
        assertEquals(0x0F0F, cpu.regs[2], "AND.W should perform bitwise AND with absolute address value");
    }

    @Test
    void testOR_AbsToReg() {
        // OR.W $2000, R2 - Bitwise OR absolute address value with register
        cpu.writeWord(0x2000, 0xFF0F);  // Source value
        cpu.regs[2] = 0x0FF0;
        loadProgram(new int[]{
            makeInstr(Cpu.OR, 1, Cpu.MODE_ABS, 0, Cpu.MODE_REG, 2),
            0x2000  // Absolute address
        });
        cpu.step();
        assertEquals(0xFFFF, cpu.regs[2], "OR.W should perform bitwise OR with absolute address value");
    }

    @Test
    void testXOR_AbsToReg() {
        // XOR.W $2000, R2 - Bitwise XOR absolute address value with register
        cpu.writeWord(0x2000, 0xFF0F);  // Source value
        cpu.regs[2] = 0x0FF0;
        loadProgram(new int[]{
            makeInstr(Cpu.XOR, 1, Cpu.MODE_ABS, 0, Cpu.MODE_REG, 2),
            0x2000  // Absolute address
        });
        cpu.step();
        assertEquals(0xF0FF, cpu.regs[2], "XOR.W should perform bitwise XOR with absolute address value");
    }

    @Test
    void testNOT_Abs() {
        // NOT.W $2000 - Bitwise NOT value at absolute address
        cpu.writeWord(0x2000, 0xAAAA);
        loadProgram(new int[]{
            makeInstr(Cpu.NOT, 1, 0, 0, Cpu.MODE_ABS, 0),
            0x2000  // Absolute address
        });
        cpu.step();
        assertEquals(0x5555, cpu.readWord(0x2000), "NOT.W should perform bitwise NOT on absolute address value");
    }

    // ======================= OPCODES DE COMPARAISON =======================

    @Test
    void testCMP_AbsToReg() {
        // CMP.W $2000, R2 - Compare absolute address value with register
        cpu.writeWord(0x2000, 0x1234);  // Memory value
        cpu.regs[2] = 0x1234;  // Register value
        loadProgram(new int[]{
            makeInstr(Cpu.CMP, 1, Cpu.MODE_ABS, 0, Cpu.MODE_REG, 2),
            0x2000  // Absolute address
        });
        cpu.step();
        assertEquals(0x1234, cpu.readWord(0x2000), "CMP.W should not modify source memory");
        assertEquals(0x1234, cpu.regs[2], "CMP.W should not modify destination register");
        assertTrue((cpu.flags & Cpu.FLAG_Z) != 0, "CMP.W equal values should set zero flag");
    }

    @Test
    void testTST_AbsToReg() {
        // TST.W $2000, R2 - Test absolute address value with register
        cpu.writeWord(0x2000, 0x1234);  // Memory value
        cpu.regs[2] = 0x1234;  // Register value
        loadProgram(new int[]{
            makeInstr(Cpu.TST, 1, Cpu.MODE_ABS, 0, Cpu.MODE_REG, 2),
            0x2000  // Absolute address
        });
        cpu.step();
        assertEquals(0x1234, cpu.readWord(0x2000), "TST.W should not modify source memory");
        assertEquals(0x1234, cpu.regs[2], "TST.W should not modify destination register");
        assertFalse((cpu.flags & Cpu.FLAG_Z) != 0, "TST.W non-zero result should not set zero flag");
    }

    // ======================= OPCODES MULTIPLICATIFS =======================

    @Test
    void testMULU_AbsToReg() {
        // MULU.W $2000, R2 - Unsigned multiply absolute address value with register
        cpu.writeWord(0x2000, 0x1000);  // Memory value
        cpu.regs[2] = 0x0010;
        loadProgram(new int[]{
            makeInstr(Cpu.MULU, 1, Cpu.MODE_ABS, 0, Cpu.MODE_REG, 2),
            0x2000  // Absolute address
        });
        cpu.step();
        assertEquals(0x0000, cpu.regs[2], "MULU.W should put low word in R2");
        assertEquals(0x0001, cpu.regs[3], "MULU.W should put high word in R3");
    }

    @Test
    void testDIVU_AbsToReg() {
        // DIVU.W $2000, R2 - Unsigned divide register by absolute address value
        cpu.writeWord(0x2000, 0x0002);  // Divisor in memory
        cpu.regs[2] = 0x1234;  // Dividend in register
        loadProgram(new int[]{
            makeInstr(Cpu.DIVU, 1, Cpu.MODE_ABS, 0, Cpu.MODE_REG, 2),
            0x2000  // Absolute address
        });
        cpu.step();
        assertEquals(0x091A, cpu.regs[2], "DIVU.W should put quotient in R2");
        assertEquals(0x0000, cpu.regs[3], "DIVU.W should put remainder in R3");
    }

    // ======================= OPCODES DE DECALAGE =======================

    @Test
    void testROL_Abs() {
        // ROL.W $2000 - Rotate value at absolute address left
        cpu.writeWord(0x2000, 0x8001);
        loadProgram(new int[]{
            makeInstr(Cpu.ROL, 1, 0, 0, Cpu.MODE_ABS, 0),
            0x2000  // Absolute address
        });
        cpu.step();
        assertEquals(0x0003, cpu.readWord(0x2000), "ROL.W should rotate absolute address value left");
    }

    @Test
    void testROR_Abs() {
        // ROR.W $2000 - Rotate value at absolute address right
        cpu.writeWord(0x2000, 0x8001);
        loadProgram(new int[]{
            makeInstr(Cpu.ROR, 1, 0, 0, Cpu.MODE_ABS, 0),
            0x2000  // Absolute address
        });
        cpu.step();
        assertEquals(0xC000, cpu.readWord(0x2000), "ROR.W should rotate absolute address value right");
    }

    @Test
    void testSHL_Abs() {
        // SHL.W $2000 - Shift value at absolute address left
        cpu.writeWord(0x2000, 0x1234);
        loadProgram(new int[]{
            makeInstr(Cpu.SHL, 1, 0, 0, Cpu.MODE_ABS, 0),
            0x2000  // Absolute address
        });
        cpu.step();
        assertEquals(0x2468, cpu.readWord(0x2000), "SHL.W should shift absolute address value left");
    }

    @Test
    void testSHR_Abs() {
        // SHR.W $2000 - Shift value at absolute address right
        cpu.writeWord(0x2000, 0x1234);
        loadProgram(new int[]{
            makeInstr(Cpu.SHR, 1, 0, 0, Cpu.MODE_ABS, 0),
            0x2000  // Absolute address
        });
        cpu.step();
        assertEquals(0x091A, cpu.readWord(0x2000), "SHR.W should shift absolute address value right");
    }

    // ======================= OPCODES DE BITS =======================

    @Test
    void testBTST_AbsToReg() {
        // BTST.W $2000, R2 - Test bit from absolute address in register
        cpu.writeWord(0x2000, 0x0000);  // Bit position 0
        cpu.regs[2] = 0x0001;  // Value with bit 0 set
        loadProgram(new int[]{
            makeInstr(Cpu.BTST, 1, Cpu.MODE_ABS, 0, Cpu.MODE_REG, 2),
            0x2000  // Absolute address
        });
        cpu.step();
        assertFalse((cpu.flags & Cpu.FLAG_Z) != 0, "BTST.W with bit set should not set zero flag");
    }

    @Test
    void testBSET_AbsToReg() {
        // BSET.W $2000, R2 - Set bit from absolute address position in register
        cpu.writeWord(0x2000, 0x0004);  // Bit position 4
        cpu.regs[2] = 0x0000;  // Initial value
        loadProgram(new int[]{
            makeInstr(Cpu.BSET, 1, Cpu.MODE_ABS, 0, Cpu.MODE_REG, 2),
            0x2000  // Absolute address
        });
        cpu.step();
        assertEquals(0x0010, cpu.regs[2], "BSET.W should set bit 4 in register");
    }

    @Test
    void testBCLR_AbsToReg() {
        // BCLR.W $2000, R2 - Clear bit from absolute address position in register
        cpu.writeWord(0x2000, 0x0004);  // Bit position 4
        cpu.regs[2] = 0x0010;  // Value with bit 4 set
        loadProgram(new int[]{
            makeInstr(Cpu.BCLR, 1, Cpu.MODE_ABS, 0, Cpu.MODE_REG, 2),
            0x2000  // Absolute address
        });
        cpu.step();
        assertEquals(0x0000, cpu.regs[2], "BCLR.W should clear bit 4 in register");
    }

    // ======================= OPCODES DE PILE =======================

    @Test
    void testPUSH_Abs() {
        // PUSH.W $2000 - Push absolute address value onto stack
        cpu.writeWord(0x2000, 0x1234);
        cpu.sp = 0xFEFF;  // Use a safe stack pointer
        int oldSp = cpu.sp;
        loadProgram(new int[]{
            makeInstr(Cpu.PUSH, 1, Cpu.MODE_ABS, 0, Cpu.MODE_REG, 0),
            0x2000  // Absolute address
        });
        cpu.step();
        assertEquals(oldSp - 2, cpu.sp, "PUSH.W should decrement SP by 2");
        assertEquals(0x1234, cpu.readWord(cpu.sp), "PUSH.W should store absolute address value on stack");
    }

    @Test
    void testPOP_Abs() {
        // POP.W $2000 - Pop from stack to absolute address
        cpu.sp = 0xFBFD; // Use safe stack address
        cpu.writeWord(cpu.sp, 0x5678);  // Value on stack
        cpu.writeWord(0x2000, 0x0000);  // Initialize memory
        int oldSp = cpu.sp;
        loadProgram(new int[]{
            makeInstr(Cpu.POP, 1, Cpu.MODE_REG, 0, Cpu.MODE_ABS, 0),
            0x2000  // Absolute address
        });
        cpu.step();
        assertEquals(0x5678, cpu.readWord(0x2000), "POP.W should load value from stack to absolute address");
        assertEquals(oldSp + 2, cpu.sp, "POP.W should increment SP by 2");
    }

    // ======================= OPCODES DE SAUT =======================

    @Test
    void testJMP_Abs() {
        // JMP $4000 - Jump to absolute address
        loadProgram(new int[]{
            makeInstr(Cpu.JMP, 1, Cpu.MODE_REG, 0, Cpu.MODE_ABS, 0),
            0x4000  // Target address
        });
        cpu.step();
        assertEquals(0x4000, cpu.pc, "JMP should jump to absolute address");
    }

    @Test
    void testCALL_Abs() {
        // CALL $4000 - Call subroutine at absolute address
        cpu.sp = 0xFEFF;  // Safe stack pointer
        int oldSp = cpu.sp;
        int returnAddr = cpu.pc + 4; // Expected return address (PC after instruction + extension word)
        loadProgram(new int[]{
            makeInstr(Cpu.CALL, 1, Cpu.MODE_REG, 0, Cpu.MODE_ABS, 0),
            0x4000  // Target address
        });
        cpu.step();
        assertEquals(0x4000, cpu.pc, "CALL should jump to absolute address");
        assertEquals(oldSp - 2, cpu.sp, "CALL should decrement SP by 2");
        assertEquals(returnAddr, cpu.readWord(cpu.sp), "CALL should push return address on stack");
    }

    // ======================= TESTS I/O REGISTERS =======================

    @Test
    void testMOV_IoRegisterRead() {
        // MOV.W $FE01, R0 - Read from VIDEO_CTRL register
        cpu.videoCtrl = 0x42;
        loadProgram(new int[]{
            makeInstr(Cpu.MOV, 1, Cpu.MODE_ABS, 0, Cpu.MODE_REG, 0),
            Cpu.VIDEO_CTRL  // I/O register address
        });
        cpu.step();
        assertEquals(0x42, cpu.regs[0], "MOV.W should read from I/O register");
    }

    @Test
    void testMOV_IoRegisterWrite() {
        // MOV.W R0, $FE01 - Write to VIDEO_CTRL register
        cpu.regs[0] = 0x85;
        loadProgram(new int[]{
            makeInstr(Cpu.MOV, 1, Cpu.MODE_REG, 0, Cpu.MODE_ABS, 0),
            Cpu.VIDEO_CTRL  // I/O register address
        });
        cpu.step();
        assertEquals(0x85, cpu.videoCtrl, "MOV.W should write to I/O register");
    }

    @Test
    void testBankRegisterAccess() {
        // Test BANK_REG access via absolute addressing
        cpu.bankReg = 0;
        
        // MOV.W $FE00, R0 - Read BANK_REG
        loadProgram(new int[]{
            makeInstr(Cpu.MOV, 1, Cpu.MODE_ABS, 0, Cpu.MODE_REG, 0),
            Cpu.BANK_REG
        });
        cpu.step();
        assertEquals(0, cpu.regs[0], "Should read BANK_REG value");
        
        // MOV.W R1, $FE00 - Write BANK_REG
        cpu.regs[1] = 1;
        cpu.pc = 0;  // Reset PC for next instruction
        loadProgram(new int[]{
            makeInstr(Cpu.MOV, 1, Cpu.MODE_REG, 1, Cpu.MODE_ABS, 0),
            Cpu.BANK_REG
        });
        cpu.step();
        assertEquals(1, cpu.bankReg, "Should write BANK_REG value");
    }

    // ======================= TESTS SUPPLEMENTAIRES =======================

    @Test
    void testAbsoluteAddressingBounds() {
        // Test absolute addressing at memory boundaries
        cpu.writeWord(0xFFFE, 0x1234);  // Near end of 64K space
        cpu.regs[2] = 0x0000;
        loadProgram(new int[]{
            makeInstr(Cpu.MOV, 1, Cpu.MODE_ABS, 0, Cpu.MODE_REG, 2),
            0xFFFE  // Absolute address near boundary
        });
        cpu.step();
        assertEquals(0x1234, cpu.regs[2], "Absolute addressing should work at memory boundaries");
    }

    @Test
    void testAbsoluteWithBanking() {
        // Test absolute addressing with banking - non-I/O addresses respect banking
        cpu.bankReg = 0;  // Bank0
        cpu.writeWord(0x1000, 0xABCD);  // Write to Bank0
        cpu.regs[2] = 0x0000;
        
        loadProgram(new int[]{
            makeInstr(Cpu.MOV, 1, Cpu.MODE_ABS, 0, Cpu.MODE_REG, 2),
            0x1000  // Absolute address
        });
        cpu.step();
        assertEquals(0xABCD, cpu.regs[2], "Absolute addressing should respect current bank");
        
        // Switch to Bank1 and verify different data
        cpu.writeByte(Cpu.BANK_REG, (byte)1);  // Switch to Bank1
        cpu.pc = 0;  // Reset PC
        cpu.regs[2] = 0x0000;  // Reset register
        cpu.step();
        // Bank1 should have different data (or zeros)
        assertNotEquals(0xABCD, cpu.regs[2], "Bank1 should have different data than Bank0");
    }

    @Test
    void testFlagsWithAbsoluteOperations() {
        // Test flag setting with absolute operations
        cpu.writeWord(0x2000, 0x7FFF);  // Maximum positive value
        cpu.regs[2] = 0x0001;
        
        // ADD.W $2000, R2 - Should cause overflow
        loadProgram(new int[]{
            makeInstr(Cpu.ADD, 1, Cpu.MODE_ABS, 0, Cpu.MODE_REG, 2),
            0x2000  // Absolute address
        });
        cpu.step();
        assertEquals(0x8000, cpu.regs[2], "ADD.W should compute correct result");
        assertTrue((cpu.flags & Cpu.FLAG_V) != 0, "ADD.W signed overflow should set V flag");
        assertTrue((cpu.flags & Cpu.FLAG_N) != 0, "ADD.W negative result should set N flag");
    }

    @Test
    void testMultipleExtensionWords() {
        // Test instruction with two extension words (abs src, abs dst)
        cpu.writeWord(0x2000, 0x1000);  // Source value
        cpu.writeWord(0x3000, 0x0234);  // Destination value
        
        // ADD.W $2000, $3000 - Two extension words
        loadProgram(new int[]{
            makeInstr(Cpu.ADD, 1, Cpu.MODE_ABS, 0, Cpu.MODE_ABS, 0),
            0x2000,  // First extension word (source)
            0x3000   // Second extension word (destination)
        });
        cpu.step();
        assertEquals(0x1234, cpu.readWord(0x3000), "ADD.W should handle two extension words correctly");
        assertEquals(6, cpu.pc, "PC should advance by 6 bytes (instruction + 2 extension words)");
    }
}