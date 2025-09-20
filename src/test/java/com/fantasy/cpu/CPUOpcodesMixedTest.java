package com.fantasy.cpu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests complets pour les combinaisons de modes d'adressage du Fantasy Machine 16 CPU.
 * 
 * Ce fichier teste systématiquement les combinaisons de modes d'adressage entre
 * source et destination pour tous les opcodes qui supportent multiple modes.
 * Couvre les cas réels d'utilisation et les interactions entre modes.
 */
public class CPUOpcodesMixedTest {
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

    // ======================= COMBINAISONS AVEC MOV =======================

    @Test
    void testMOV_AllCombinations() {
        // Test all valid MOV combinations systematically
        
        // Setup data
        cpu.regs[0] = 0x1111;      // Source register
        cpu.regs[1] = 0x2000;      // Pointer register
        cpu.regs[2] = 0x0000;      // Destination register
        cpu.writeWord(0x2000, 0x2222);  // Data at [R1]
        cpu.writeWord(0x3000, 0x3333);  // Data at absolute address
        cpu.writeWord(0x4000, 0x0000);  // Destination memory
        
        // MOV.W R0, [R1] - Register to Indirect
        loadProgram(new int[]{makeInstr(Cpu.MOV, 1, Cpu.MODE_REG, 0, Cpu.MODE_IND, 1)});
        cpu.step();
        assertEquals(0x1111, cpu.readWord(0x2000), "R0 should be stored at [R1]");
        
        // MOV.W [R1], R2 - Indirect to Register
        cpu.pc = 0;
        cpu.writeWord(0x2000, 0x2222);  // Restore data
        loadProgram(new int[]{makeInstr(Cpu.MOV, 1, Cpu.MODE_IND, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x2222, cpu.regs[2], "[R1] should be loaded to R2");
        
        // MOV.W #4444, [R1] - Immediate to Indirect
        cpu.pc = 0;
        loadProgram(new int[]{makeInstr(Cpu.MOV, 1, Cpu.MODE_IMM, 0, Cpu.MODE_IND, 1), 0x4444});
        cpu.step();
        assertEquals(0x4444, cpu.readWord(0x2000), "Immediate should be stored at [R1]");
        
        // MOV.W $3000, R2 - Absolute to Register
        cpu.pc = 0;
        cpu.regs[2] = 0x0000;
        loadProgram(new int[]{makeInstr(Cpu.MOV, 1, Cpu.MODE_ABS, 0, Cpu.MODE_REG, 2), 0x3000});
        cpu.step();
        assertEquals(0x3333, cpu.regs[2], "Absolute address content should be loaded to R2");
        
        // MOV.W R0, $4000 - Register to Absolute
        cpu.pc = 0;
        loadProgram(new int[]{makeInstr(Cpu.MOV, 1, Cpu.MODE_REG, 0, Cpu.MODE_ABS, 0), 0x4000});
        cpu.step();
        assertEquals(0x1111, cpu.readWord(0x4000), "R0 should be stored at absolute address");
    }

    @Test
    void testMOV_ComplexChaining() {
        // Test complex data movement chains
        cpu.regs[0] = 0x1000;  // Base address
        cpu.regs[1] = 0x2000;  // Another address
        cpu.writeWord(0x1000, 0xAAAA);
        cpu.writeWord(0x2000, 0xBBBB);
        
        // Chain: [R0] -> R2 -> [R1] -> R3
        loadProgram(new int[]{
            makeInstr(Cpu.MOV, 1, Cpu.MODE_IND, 0, Cpu.MODE_REG, 2),     // [R0] -> R2
            makeInstr(Cpu.MOV, 1, Cpu.MODE_REG, 2, Cpu.MODE_IND, 1),     // R2 -> [R1]
            makeInstr(Cpu.MOV, 1, Cpu.MODE_IND, 1, Cpu.MODE_REG, 3)      // [R1] -> R3
        });
        
        cpu.step();  // [R0] -> R2
        assertEquals(0xAAAA, cpu.regs[2], "First step: [R0] should be loaded to R2");
        
        cpu.step();  // R2 -> [R1]
        assertEquals(0xAAAA, cpu.readWord(0x2000), "Second step: R2 should be stored to [R1]");
        
        cpu.step();  // [R1] -> R3
        assertEquals(0xAAAA, cpu.regs[3], "Third step: [R1] should be loaded to R3");
    }

    // ======================= COMBINAISONS ARITHMETIQUES =======================

    @Test
    void testADD_MixedModes() {
        // Test ADD with different addressing mode combinations
        
        // Setup
        cpu.regs[0] = 0x1000;
        cpu.regs[1] = 0x2000;
        cpu.regs[2] = 0x0100;
        cpu.writeWord(0x2000, 0x0200);
        cpu.writeWord(0x3000, 0x0300);
        cpu.writeWord(0x4000, 0x0050);
        
        // ADD.W #100, R2 - Immediate to Register
        loadProgram(new int[]{makeInstr(Cpu.ADD, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 2), 0x0100});
        cpu.step();
        assertEquals(0x0200, cpu.regs[2], "ADD immediate to register");
        
        // ADD.W [R1], R2 - Indirect to Register
        cpu.pc = 0;
        cpu.regs[2] = 0x0100;
        loadProgram(new int[]{makeInstr(Cpu.ADD, 1, Cpu.MODE_IND, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x0300, cpu.regs[2], "ADD indirect to register");
        
        // ADD.W $3000, [R1] - Absolute to Indirect
        cpu.pc = 0;
        cpu.writeWord(0x2000, 0x0200);  // Reset [R1]
        loadProgram(new int[]{makeInstr(Cpu.ADD, 1, Cpu.MODE_ABS, 0, Cpu.MODE_IND, 1), 0x3000});
        cpu.step();
        assertEquals(0x0500, cpu.readWord(0x2000), "ADD absolute to indirect");
        
        // ADD.W R0, $4000 - Register to Absolute
        cpu.pc = 0;
        loadProgram(new int[]{makeInstr(Cpu.ADD, 1, Cpu.MODE_REG, 0, Cpu.MODE_ABS, 0), 0x4000});
        cpu.step();
        assertEquals(0x1050, cpu.readWord(0x4000), "ADD register to absolute");
    }

    @Test
    void testArithmeticOverflowMixed() {
        // Test overflow conditions with mixed addressing modes
        cpu.regs[0] = 0x8000;  // Pointer
        cpu.writeWord(0x8000, 0x7FFF);  // Max positive
        cpu.regs[1] = 0x0001;
        
        // ADD.W R1, [R0] - Should cause overflow in memory
        loadProgram(new int[]{makeInstr(Cpu.ADD, 1, Cpu.MODE_REG, 1, Cpu.MODE_IND, 0)});
        cpu.step();
        assertEquals(0x8000, cpu.readWord(0x8000), "Overflow should wrap to 0x8000");
        assertTrue((cpu.flags & Cpu.FLAG_V) != 0, "Overflow flag should be set");
        assertTrue((cpu.flags & Cpu.FLAG_N) != 0, "Negative flag should be set");
    }

    // ======================= COMBINAISONS LOGIQUES =======================

    @Test
    void testLogical_MixedModes() {
        // Test logical operations with mixed addressing modes
        
        // Setup test data
        cpu.regs[0] = 0xFF00;
        cpu.regs[1] = 0x2000;
        cpu.writeWord(0x2000, 0x00FF);
        cpu.writeWord(0x3000, 0x0F0F);
        
        // AND.W [R1], R0 - Indirect AND Register
        loadProgram(new int[]{makeInstr(Cpu.AND, 1, Cpu.MODE_IND, 1, Cpu.MODE_REG, 0)});
        cpu.step();
        assertEquals(0x0000, cpu.regs[0], "AND [R1], R0 should result in 0x0000");
        assertTrue((cpu.flags & Cpu.FLAG_Z) != 0, "Zero flag should be set");
        
        // OR.W #F0F0, [R1] - Immediate OR Indirect
        cpu.pc = 0;
        loadProgram(new int[]{makeInstr(Cpu.OR, 1, Cpu.MODE_IMM, 0, Cpu.MODE_IND, 1), 0xF0F0});
        cpu.step();
        assertEquals(0xF0FF, cpu.readWord(0x2000), "OR immediate to indirect");
        
        // XOR.W $3000, [R1] - Absolute XOR Indirect
        cpu.pc = 0;
        loadProgram(new int[]{makeInstr(Cpu.XOR, 1, Cpu.MODE_ABS, 0, Cpu.MODE_IND, 1), 0x3000});
        cpu.step();
        assertEquals(0xFFF0, cpu.readWord(0x2000), "XOR absolute with indirect");
    }

    // ======================= COMBINAISONS DE COMPARAISON =======================

    @Test
    void testComparison_MixedModes() {
        // Test comparison operations with mixed addressing modes
        
        cpu.regs[0] = 0x1234;
        cpu.regs[1] = 0x2000;
        cpu.writeWord(0x2000, 0x1234);
        cpu.writeWord(0x3000, 0x5678);
        
        // CMP.W [R1], R0 - Compare indirect with register (equal)
        loadProgram(new int[]{makeInstr(Cpu.CMP, 1, Cpu.MODE_IND, 1, Cpu.MODE_REG, 0)});
        cpu.step();
        assertTrue((cpu.flags & Cpu.FLAG_Z) != 0, "Equal comparison should set zero flag");
        assertEquals(0x1234, cpu.regs[0], "CMP should not modify register");
        assertEquals(0x1234, cpu.readWord(0x2000), "CMP should not modify memory");
        
        // CMP.W #5678, [R1] - Compare immediate with indirect
        cpu.pc = 0;
        cpu.flags = 0;  // Clear flags
        loadProgram(new int[]{makeInstr(Cpu.CMP, 1, Cpu.MODE_IMM, 0, Cpu.MODE_IND, 1), 0x5678});
        cpu.step();
        assertTrue((cpu.flags & Cpu.FLAG_C) != 0, "Memory < Immediate should set carry flag");
        
        // TST.W $3000, R0 - Test absolute with register
        cpu.pc = 0;
        cpu.flags = 0;
        loadProgram(new int[]{makeInstr(Cpu.TST, 1, Cpu.MODE_ABS, 0, Cpu.MODE_REG, 0), 0x3000});
        cpu.step();
        assertFalse((cpu.flags & Cpu.FLAG_Z) != 0, "TST with common bits should not set zero flag");
    }

    // ======================= COMBINAISONS DE MULTIPLICATION/DIVISION =======================

    @Test
    void testMultiply_MixedModes() {
        // Test multiplication with mixed addressing modes
        
        cpu.regs[0] = 0x1000;
        cpu.regs[1] = 0x2000;
        cpu.writeWord(0x2000, 0x0010);
        
        // MULU.W [R1], R0 - Multiply register by indirect
        loadProgram(new int[]{makeInstr(Cpu.MULU, 1, Cpu.MODE_IND, 1, Cpu.MODE_REG, 0)});
        cpu.step();
        assertEquals(0x0000, cpu.regs[0], "MULU low word result");
        assertEquals(0x0001, cpu.regs[1], "MULU high word result (overwrites R1)");
        
        // DIVU.W #8, R2 - Divide register by immediate
        cpu.regs[2] = 0x0040;  // 64 decimal
        cpu.pc = 0;
        loadProgram(new int[]{makeInstr(Cpu.DIVU, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 2), 0x0008});
        cpu.step();
        assertEquals(0x0008, cpu.regs[2], "DIVU quotient");
        assertEquals(0x0000, cpu.regs[3], "DIVU remainder");
    }

    // ======================= COMBINAISONS DE PILE =======================

    @Test
    void testStack_MixedModes() {
        // Test stack operations with mixed addressing modes
        
        cpu.sp = 0xFBFF;  // Use SP_MAX as starting point (valid stack pointer)
        cpu.regs[0] = 0x2000;
        cpu.writeWord(0x2000, 0x1234);
        cpu.writeWord(0x3000, 0x5678);
        
        // PUSH.W [R0] - Push indirect
        loadProgram(new int[]{makeInstr(Cpu.PUSH, 1, Cpu.MODE_IND, 0, Cpu.MODE_REG, 0)});
        cpu.step();
        assertEquals(0x1234, cpu.readWord(cpu.sp), "PUSH indirect should push memory content");
        
        // PUSH.W $3000 - Push absolute
        cpu.pc = 0;
        loadProgram(new int[]{makeInstr(Cpu.PUSH, 1, Cpu.MODE_ABS, 0, Cpu.MODE_REG, 0), 0x3000});
        cpu.step();
        assertEquals(0x5678, cpu.readWord(cpu.sp), "PUSH absolute should push memory content");
        
        // PUSH.W #9ABC - Push immediate
        cpu.pc = 0;
        loadProgram(new int[]{makeInstr(Cpu.PUSH, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 0), 0x9ABC});
        cpu.step();
        assertEquals(0x9ABC, cpu.readWord(cpu.sp), "PUSH immediate should push immediate value");
        
        // Now test POP with different destinations
        // POP.W [R0] - Pop to indirect
        cpu.writeWord(0x2000, 0x0000);  // Clear destination
        cpu.pc = 0;
        loadProgram(new int[]{makeInstr(Cpu.POP, 1, Cpu.MODE_REG, 0, Cpu.MODE_IND, 0)});
        cpu.step();
        assertEquals(0x9ABC, cpu.readWord(0x2000), "POP to indirect should store at memory location");
        
        // POP.W $3000 - Pop to absolute
        cpu.writeWord(0x3000, 0x0000);  // Clear destination
        cpu.pc = 0;
        loadProgram(new int[]{makeInstr(Cpu.POP, 1, Cpu.MODE_REG, 0, Cpu.MODE_ABS, 0), 0x3000});
        cpu.step();
        assertEquals(0x5678, cpu.readWord(0x3000), "POP to absolute should store at absolute address");
    }

    // ======================= COMBINAISONS DE SAUT =======================

    @Test
    void testJump_MixedModes() {
        // Test jump operations with mixed addressing modes
        
        cpu.regs[0] = 0x2000;
        cpu.writeWord(0x2000, 0x4000);  // Jump target in memory
        cpu.writeWord(0x3000, 0x5000);  // Another jump target
        
        // JMP [R0] - Jump indirect
        loadProgram(new int[]{makeInstr(Cpu.JMP, 1, Cpu.MODE_REG, 0, Cpu.MODE_IND, 0)});
        cpu.step();
        assertEquals(0x4000, cpu.pc, "JMP indirect should jump to address in memory");
        
        // CALL $5000 - Call absolute
        int callInstr = makeInstr(Cpu.CALL, 1, Cpu.MODE_REG, 0, Cpu.MODE_ABS, 0);
        
        // Load directly at desired PC address
        cpu.pc = 0x1000;  // Set target PC  
        cpu.writeWord(0x1000, callInstr);  // Load CALL instruction
        cpu.writeWord(0x1002, 0x5000);    // Load extension word
        cpu.sp = 0xFEFF;
        int expectedReturn = cpu.pc + 4;  // After instruction + extension word
        cpu.step();
        assertEquals(0x5000, cpu.pc, "CALL absolute should jump to target");
        assertEquals(expectedReturn, cpu.readWord(cpu.sp), "CALL should push return address");
        
        // CALL [R0] - Call indirect
        cpu.regs[0] = 0x2000;  // Point to jump target (address contains 0x4000)
        cpu.pc = 0x1500;  // Place instruction at different address
        cpu.writeWord(0x1500, makeInstr(Cpu.CALL, 1, Cpu.MODE_REG, 0, Cpu.MODE_IND, 0));  // Load CALL indirect instruction
        expectedReturn = cpu.pc + 2;  // After instruction only
        cpu.step();
        assertEquals(0x4000, cpu.pc, "CALL indirect should jump to address in memory");
        assertEquals(expectedReturn, cpu.readWord(cpu.sp), "CALL should push return address");
    }

    // ======================= TESTS DE PERFORMANCE ET EDGE CASES =======================

    @Test
    void testComplexAddressingChain() {
        // Test complex addressing chain: [[R0] + imm] type operations simulated
        
        // Setup: R0 points to address that contains another address
        cpu.regs[0] = 0x1000;           // R0 = 0x1000
        cpu.writeWord(0x1000, 0x2000);  // [R0] = 0x2000
        cpu.writeWord(0x2000, 0x1234);  // [[R0]] = 0x1234
        
        // First load the intermediate address
        cpu.regs[1] = 0x0000;
        loadProgram(new int[]{makeInstr(Cpu.MOV, 1, Cpu.MODE_IND, 0, Cpu.MODE_REG, 1)});
        cpu.step();
        assertEquals(0x2000, cpu.regs[1], "First indirection should load 0x2000");
        
        // Then use that address for another indirection
        cpu.pc = 0;
        cpu.regs[2] = 0x0000;
        loadProgram(new int[]{makeInstr(Cpu.MOV, 1, Cpu.MODE_IND, 1, Cpu.MODE_REG, 2)});
        cpu.step();
        assertEquals(0x1234, cpu.regs[2], "Second indirection should load final value");
    }

    @Test
    void testSelfModifyingCode() {
        // Test self-modifying code patterns using mixed addressing
        
        // Store an instruction in memory and then modify it
        int originalInstr = makeInstr(Cpu.MOV, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 0);
        cpu.writeWord(0x1000, originalInstr);
        cpu.writeWord(0x1002, 0x1234);  // Original immediate value
        
        // Modify the immediate value using absolute addressing
        loadProgram(new int[]{makeInstr(Cpu.MOV, 1, Cpu.MODE_IMM, 0, Cpu.MODE_ABS, 0), 0x5678, 0x1002});
        cpu.step();
        assertEquals(0x5678, cpu.readWord(0x1002), "Should modify the stored immediate value");
        
        // Execute the modified instruction
        cpu.pc = 0x1000;
        cpu.regs[0] = 0x0000;
        cpu.step();
        assertEquals(0x5678, cpu.regs[0], "Modified instruction should use new immediate value");
    }

    @Test
    void testMemoryToMemoryCopy() {
        // Test efficient memory-to-memory copying using different addressing modes
        
        // Copy data from one memory block to another
        int srcBase = 0x2000;
        int dstBase = 0x3000;
        int[] testData = {0x1111, 0x2222, 0x3333, 0x4444};
        
        // Initialize source data
        for (int i = 0; i < testData.length; i++) {
            cpu.writeWord(srcBase + i * 2, testData[i]);
            cpu.writeWord(dstBase + i * 2, 0x0000);  // Clear destination
        }
        
        // Copy using different addressing mode combinations
        loadProgram(new int[]{
            // MOV.W $2000, $3000 - Copy first word
            makeInstr(Cpu.MOV, 1, Cpu.MODE_ABS, 0, Cpu.MODE_ABS, 0), 0x2000, 0x3000,
            // Setup R0 and R1 as pointers
            makeInstr(Cpu.MOV, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 0), 0x2002,
            makeInstr(Cpu.MOV, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 1), 0x3002,
            // MOV.W [R0], [R1] - Copy second word using indirect
            makeInstr(Cpu.MOV, 1, Cpu.MODE_IND, 0, Cpu.MODE_IND, 1)
        });
        
        cpu.step();  // Copy first word
        assertEquals(testData[0], cpu.readWord(dstBase), "First word should be copied");
        
        cpu.step();  // Setup R0
        cpu.step();  // Setup R1
        cpu.step();  // Copy second word
        assertEquals(testData[1], cpu.readWord(dstBase + 2), "Second word should be copied");
    }

    @Test
    void testErrorConditionsWithMixedModes() {
        // Test error conditions across different addressing modes
        
        // Division by zero with different modes
        cpu.regs[0] = 0x2000;
        cpu.writeWord(0x2000, 0x0000);  // Zero divisor in memory
        cpu.regs[1] = 0x1234;           // Dividend
        
        // DIVU.W [R0], R1 - Divide by zero from memory
        loadProgram(new int[]{makeInstr(Cpu.DIVU, 1, Cpu.MODE_IND, 0, Cpu.MODE_REG, 1)});
        cpu.step();
        assertTrue((cpu.flags & Cpu.FLAG_X) != 0, "Division by zero should set X flag");
        
        // DIVU.W #0, R1 - Divide by zero immediate
        cpu.pc = 0;
        cpu.flags = 0;
        cpu.regs[1] = 0x1234;
        loadProgram(new int[]{makeInstr(Cpu.DIVU, 1, Cpu.MODE_IMM, 0, Cpu.MODE_REG, 1), 0x0000});
        cpu.step();
        assertTrue((cpu.flags & Cpu.FLAG_X) != 0, "Immediate division by zero should set X flag");
    }

    // ======================= TESTS DE BANKING AVEC MODES MIXTES =======================

    @Test
    void testBankingWithMixedModes() {
        // Test banking behavior with different addressing modes
        
        // Setup data in both banks
        cpu.bankReg = 0;  // Bank0
        cpu.writeWord(0x1000, 0xB000);  // Bank0 data
        
        // Switch to Bank1
        cpu.writeByte(Cpu.BANK_REG, (byte)1);
        cpu.writeWord(0x1000, 0xB001);  // Bank1 data
        
        // Switch back to Bank0
        cpu.writeByte(Cpu.BANK_REG, (byte)0);
        
        // Test absolute addressing respects banking
        cpu.regs[0] = 0x0000;
        loadProgram(new int[]{makeInstr(Cpu.MOV, 1, Cpu.MODE_ABS, 0, Cpu.MODE_REG, 0), 0x1000});
        cpu.step();
        assertEquals(0xB000, cpu.regs[0], "Should read from Bank0");
        
        // Switch to Bank1 and test again
        cpu.pc = 0;
        cpu.writeByte(Cpu.BANK_REG, (byte)1);
        // Need to reload the instruction after switching banks
        loadProgram(new int[]{makeInstr(Cpu.MOV, 1, Cpu.MODE_ABS, 0, Cpu.MODE_REG, 0), 0x1000});
        cpu.step();
        assertEquals(0xB001, cpu.regs[0], "Should read from Bank1");
        
        // Test that I/O addresses always map to Bank0
        cpu.writeByte(Cpu.BANK_REG, (byte)1);  // Stay in Bank1
        cpu.videoCtrl = 0x42;
        cpu.regs[0] = 0x0000;
        cpu.pc = 0;
        loadProgram(new int[]{makeInstr(Cpu.MOV, 1, Cpu.MODE_ABS, 0, Cpu.MODE_REG, 0), Cpu.VIDEO_CTRL});
        cpu.step();
        assertEquals(0x42, cpu.regs[0], "I/O registers should always be accessible regardless of banking");
    }
}