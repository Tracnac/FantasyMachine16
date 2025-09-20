package com.fantasy.cpu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CpuInterruptSystemTest {
    private Cpu cpu;

    @BeforeEach
    void setUp() {
        cpu = new Cpu();
    }

    @Test
    void testIntCtrlInitialization() {
        // INT_CTRL should initialize with NMI always enabled (bit 3 = 1)
        int intCtrl = cpu.readByte(Cpu.INT_CTRL) & 0xFF;
        assertEquals(Cpu.IM_NMI, intCtrl, "INT_CTRL should initialize with NMI enabled only");
    }

    @Test
    void testIntCtrlMaskWriteRead() {
        // Test writing interrupt masks
        int maskValue = Cpu.IM_IRQ | Cpu.IM_DMA | Cpu.IM_VSYNC | Cpu.IM_NMI;
        cpu.writeByte(Cpu.INT_CTRL, (byte) maskValue);
        
        int readValue = cpu.readByte(Cpu.INT_CTRL) & 0xFF;
        assertEquals(maskValue, readValue, "Should be able to write and read interrupt masks");
    }

    @Test
    void testIntCtrlNmiAlwaysEnabled() {
        // Try to disable NMI - should remain enabled
        cpu.writeByte(Cpu.INT_CTRL, (byte) 0x00); // Try to disable all interrupts
        
        int readValue = cpu.readByte(Cpu.INT_CTRL) & 0xFF;
        assertTrue((readValue & Cpu.IM_NMI) != 0, "NMI should always remain enabled");
    }

    @Test
    void testIntCtrlStatusBitsReadOnly() {
        // Set some status bits directly (simulating interrupt pending)
        cpu.intCtrl = Cpu.IM_NMI | Cpu.IS_IRQ | Cpu.IS_DMA;
        
        // Try to write to status bits - they should be preserved
        cpu.writeByte(Cpu.INT_CTRL, (byte) (Cpu.IM_IRQ | Cpu.IM_VSYNC));
        
        int readValue = cpu.readByte(Cpu.INT_CTRL) & 0xFF;
        // Status bits should be preserved, mask bits updated
        int expectedValue = (Cpu.IS_IRQ | Cpu.IS_DMA) | (Cpu.IM_IRQ | Cpu.IM_VSYNC | Cpu.IM_NMI);
        assertEquals(expectedValue, readValue, "Status bits should be read-only, mask bits writable");
    }
}