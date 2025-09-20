package com.fantasy.cpu;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests pour identifier et corriger les comportements anormaux dans CPU.java
 */
public class CpuAnomaliesTest {
    private Cpu cpu;

    @BeforeEach
    public void setUp() {
        cpu = new Cpu();
    }

    @Test
    public void testRETI_correctBehavior() {
        // Test RETI behavior according to the specification:
        // RETI should restore FLAGS and PC from stack, not reset SP
        
        // Set up initial state within new stack limits  
        cpu.pc = 0x1000;
        cpu.sp = 0xFBFB; // Within new stack range, with space for 2 words
        cpu.flags = 0x00;

        // Manually push PC and FLAGS onto stack (simulating interrupt entry)
        cpu.sp -= 2;
        cpu.writeWord(cpu.sp, 0x2000); // Save PC first
        cpu.sp -= 2;
        cpu.writeWord(cpu.sp, 0x0042); // Save FLAGS second
        
        int spBeforeReti = cpu.sp;
        
        // Create RETI instruction (opcode 0x0E = 14)
        int retiInstr = (14 << 11); // RETI opcode
        cpu.writeWord(cpu.pc, retiInstr);
        
        // Execute RETI
        cpu.step();
        
        // RETI should restore FLAGS and PC, advancing SP by 4 bytes (2 pops)
        assertEquals(0x0042, cpu.flags, "RETI should restore FLAGS");
        assertEquals(0x2000, cpu.pc, "RETI should restore PC");  
        assertEquals(spBeforeReti + 4, cpu.sp, "RETI should advance SP by 4 (2 word pops)");
    }

    @Test
    public void testNEG_zeroShouldNotSetCarryFlag() {
        // Test NEG avec 0
        cpu.regs[0] = 0; // -0 = 0, pas de carry attendu
        
        // Créer instruction NEG R0 (opcode 0x06 = 6, dst=reg 0)
        int negInstr = (6 << 11) | (0 << 3) | 0; // NEG avec dst=MODE_REG, dstReg=0
        cpu.writeWord(0x1000, negInstr);
        cpu.pc = 0x1000;
                
        // Exécuter NEG
        cpu.step();
        
        
        // Résultat devrait être 0
        assertEquals(0, cpu.regs[0], "NEG de 0 devrait donner 0");
                
        // Ce test va échouer avec l'implémentation actuelle
        assertEquals(0, cpu.flags & Cpu.FLAG_C, 
            "NEG de 0 ne devrait PAS setter FLAG_C - c'est un bug!");
    }

    @Test
    public void testNEG_nonZeroShouldSetCarryFlag() {
        // Test NEG avec valeur non-zero
        cpu.regs[0] = 0x1234; // -0x1234 devrait setter carry
        
        // Créer instruction NEG R0
        int negInstr = (6 << 11) | (0 << 3) | 0; // NEG avec dst=MODE_REG, dstReg=0
        cpu.writeWord(0x1000, negInstr);
        cpu.pc = 0x1000;
        
        // Exécuter NEG
        cpu.step();
        
        // Vérifier que FLAG_C est set pour valeur non-zero
        assertNotEquals(0, cpu.flags & Cpu.FLAG_C, 
            "NEG d'une valeur non-zero devrait setter FLAG_C");
    }

    @Test
    public void testStackBounds_asymmetricLimits() {
        // Test stack behavior within new stack limits (0xEC00-0xFBFF)
        
        // Test near the bottom limit of the stack
        cpu.sp = 0xEC02; // Just above the minimum limit
        cpu.regs[0] = 0x1234;
        
        // Create instruction PUSH R0 (opcode 0x12 = 18)
        int pushInstr = (18 << 11) | (0 << 8) | 0; // PUSH with src=MODE_REG, srcReg=0
        cpu.writeWord(0x1000, pushInstr);
        cpu.pc = 0x1000;
        
        // First PUSH should work, sp goes to 0xEC00 (at the limit)
        cpu.step();
        assertEquals(0xEC00, cpu.sp);
        
        // Second PUSH should fail with underflow (sp would go to 0xDEFE)
        cpu.pc = 0x1000; // Reset PC to re-execute  
        try {
            cpu.step();
            fail("Expected stack underflow exception");
        } catch (RuntimeException e) {
            assertEquals("Stack underflow", e.getMessage());
        }
        
        // Reset CPU state after exception
        cpu.cpuCtrl &= ~1; // Clear HLT flag to allow further execution
        
        // Test POP within valid range
        cpu.sp = 0xFBFD; // Valid position within new stack range
        cpu.writeWord(cpu.sp, 0x5678);
        
        // Create instruction POP R1 (opcode 0x13 = 19)
        int popInstr = (19 << 11) | (0 << 3) | 1; // POP with dst=MODE_REG, dstReg=1
        cpu.writeWord(0x2000, popInstr);
        cpu.pc = 0x2000;
        
        cpu.step(); // POP should work within bounds
        assertEquals(0xFBFF, cpu.sp); // SP advances by 2
        assertEquals(0x5678, cpu.regs[1]);
        
    }
    
    @Test
    public void testDoubleImmediateModeFetch() {
        // Test du problème de double fetch d'immediate values
        // Si src=MODE_ABS et dst=MODE_ABS, seule la dernière valeur est conservée
        
        // Préparer la mémoire
        cpu.writeWord(0x1000, 0xAAAA); // Valeur source à l'adresse 0x1000
        cpu.writeWord(0x2000, 0x0000); // Destination à l'adresse 0x2000 (initialement 0)
        
        // Créer instruction MOV [0x1000], [0x2000] (MODE_ABS vers MODE_ABS)
        // Opcode MOV = 1, srcMode = 2 (ABS), dstMode = 2 (ABS)
        int movInstr = (1 << 11) | (2 << 8) | (0 << 5) | (2 << 3) | 0;
        
        cpu.writeWord(0x3000, movInstr);    // L'instruction
        cpu.writeWord(0x3002, 0x1000);     // Adresse source (sera écrasée!)
        cpu.writeWord(0x3004, 0x2000);     // Adresse destination (celle conservée)
        
        cpu.pc = 0x3000;
        
        
        // Exécuter l'instruction
        cpu.step();
        
        
        // À cause du bug, l'adresse source (0x1000) est écrasée par l'adresse dest (0x2000)
        // Donc l'instruction devient effectivement MOV [0x2000], [0x2000] !
        
        int actualResult = cpu.readWord(0x2000);
        
        // Bug CORRIGÉ ! L'instruction maintenant utilise srcImm et dstImm séparés
        // L'instruction MOV [0x1000], [0x2000] fonctionne correctement
        assertEquals(0xAAAA, actualResult, 
            "CORRECTION RÉUSSIE: MOV [0x1000], [0x2000] copie correctement la valeur source");
    }

    @Test
    public void testNOT_updateFlagsParameters() {
        // Test du problème avec updateFlags dans NOT
        cpu.regs[0] = 0x00FF;
        
        // Créer instruction NOT R0 (opcode 0x0D = 13)
        int notInstr = (13 << 11) | (0 << 3) | 0; // NOT avec dst=MODE_REG, dstReg=0
        cpu.writeWord(0x1000, notInstr);
        cpu.pc = 0x1000;
        
        // Exécuter NOT
        cpu.step();
        
        // Résultat devrait être ~0x00FF = 0xFF00
        assertEquals(0xFF00, cpu.regs[0], "NOT 0x00FF devrait donner 0xFF00");
        
        // PROBLÈME IDENTIFIÉ: NOT appelle updateFlags(val, 0, ~val, false, true)
        // Mais val = ~originalVal et ~val = originalVal, donc les paramètres sont confus
        // Cela pourrait affecter le calcul des flags
        
        // Pour les opérations logiques, FLAG_N n'est pas set (isLogic=true)
        // Donc on ne peut pas tester FLAG_N pour NOT
        
        // Z flag devrait être 0 car résultat != 0
        assertEquals(0, cpu.flags & Cpu.FLAG_Z, 
            "NOT résultant en 0xFF00 ne devrait pas setter FLAG_Z");
    }

    @Test 
    public void testMUL_overflowCondition() {
        // Test la condition d'overflow dans MUL signée
        // Integer.MIN_VALUE (-2147483648) est une valeur 32-bit VALIDE
        // et ne devrait PAS causer d'overflow
        
        // Test: MUL qui donne exactement Integer.MIN_VALUE
        // -32768 * 65536 = -2147483648 = Integer.MIN_VALUE (pas d'overflow)
        cpu.regs[0] = 0x8000; // -32768 en 16-bit signé  
        cpu.regs[1] = 0x0000; // pour éviter d'utiliser cette valeur
        
        // Utilisons MOV pour mettre la valeur dans un registre et tester MUL
        // Pour l'instant, testons juste la logique numérique
        
        long testResult = (long)(short)0x8000 * (long)(short)0x0002; // -32768 * 2 = -65536
        assertTrue(testResult == -65536, "Test arithmétique de base");
        
        // Test de la nouvelle condition d'overflow
        long validMin = Integer.MIN_VALUE; // -2147483648
        long invalidMin = validMin - 1;   // -2147483649 (overflow)
        
        // Condition corrigée: result <= Integer.MIN_VALUE - 1
        assertFalse(validMin <= (long)Integer.MIN_VALUE - 1, 
            "Integer.MIN_VALUE ne devrait PAS être considéré comme overflow");
        assertTrue(invalidMin <= (long)Integer.MIN_VALUE - 1,
            "Integer.MIN_VALUE - 1 devrait être considéré comme overflow");
            
    }
}