package com.fantasy.cpu;

public class Cpu {
    // Registers
    public int[] regs = new int[8]; // R0-R7, 16-bit
    public int sp; // Stack Pointer, 16-bit
    public int pc; // Program Counter, 16-bit
    public int flags; // FLAGS register: bit 7: I, 6: ?, 5: ?, 4: X, 3: V, 2: Z, 1: C, 0: N

    // Memory: 128 KiB, physical addresses 0x00000-0x1FFFF
    public byte[] memory = new byte[0x20000];

    // Banking
    public int bankReg = 0; // 0=Bank0, 1=Bank1

    // I/O Registers (always in Bank0 physical)
    public int videoCtrl = 0;
    public int vsyncStat = 0;
    public int dmaSrc = 0;
    public int dmaDst = 0;
    public int dmaLen = 0;
    public int cpuCtrl = 0; // HLT, DBG, RST
    public int dmaCtrl = 0;
    public int intCtrl = 0; // Interrupt control

    // NOTE: I/O region (addresses >= IO_BASE) is always mapped to Bank0 physical
    // regardless of the current bank register. This simplifies access to device
    // registers and matches the test-suite assumptions.

    // Constants
    public static final int MEMORY_SIZE = 0x20000;
    public static final int BANK0_BASE = 0x00000;
    public static final int BANK1_BASE = 0x10000;
    public static final int IO_BASE = 0xFE00; // I/O region start (512 bytes)

    // Stack boundaries (4 KiB: 0xEC00-0xFBFF)
    public static final int SP_MIN = 0xEC00;
    public static final int SP_MAX = 0xFBFF;

    // I/O Register addresses (all updated according to new memory map)
    public static final int BANK_REG   = 0xFE00; // Bank register (0=Bank0, 1=Bank1)
    public static final int VIDEO_CTRL = 0xFE01; // Video control register  
    public static final int VSYNC_STAT = 0xFE02; // VSync status register
    public static final int DMA_SRC    = 0xFE04; // DMA source address (16 bits)
    public static final int DMA_DST    = 0xFE06; // DMA destination address (16 bits)
    public static final int DMA_LEN    = 0xFE08; // DMA length (16 bits)
    public static final int CPU_CTRL   = 0xFE0A; // CPU control register (8 bits)
    public static final int DMA_CTRL   = 0xFE0C; // DMA control register (8 bits)
    public static final int INT_CTRL   = 0xFF0B; // Interrupt control register (8 bits)

    // Flags bits
    public static final int FLAG_N = 1 << 0;
    public static final int FLAG_C = 1 << 1;
    public static final int FLAG_Z = 1 << 2;
    public static final int FLAG_V = 1 << 3;
    public static final int FLAG_X = 1 << 4;
    public static final int FLAG_I = 1 << 7;

    // CPU Ctrl bits
    public static final int CPU_HLT = 1 << 0;
    public static final int CPU_DBG = 1 << 1;
    public static final int CPU_RST = 1 << 2;

    // DMA Ctrl bits
    public static final int DMA_BUSY = 1 << 0;
    public static final int DMA_STRT = 1 << 1;

    // INT_CTRL bits (0xFF0B) - Interrupt Mask (bits 0-3) and Status (bits 4-7)
    public static final int IM_IRQ = 1 << 0;    // Interrupt Mask for IRQ
    public static final int IM_DMA = 1 << 1;    // Interrupt Mask for DMA
    public static final int IM_VSYNC = 1 << 2;  // Interrupt Mask for VSYNC
    public static final int IM_NMI = 1 << 3;    // Interrupt Mask for NMI (always 1)
    public static final int IS_IRQ = 1 << 4;    // Interrupt Status for IRQ
    public static final int IS_DMA = 1 << 5;    // Interrupt Status for DMA
    public static final int IS_VSYNC = 1 << 6;  // Interrupt Status for VSYNC
    public static final int IS_NMI = 1 << 7;    // Interrupt Status for NMI

    // Interrupt vectors (addresses in Bank0)
    public static final int RESET_VECTOR = 0xFFE0;
    public static final int NMI_VECTOR = 0xFFE2;
    public static final int IRQ_VECTOR = 0xFFE4;
    public static final int DMA_VECTOR = 0xFFE6;
    public static final int VSYNC_VECTOR = 0xFFE8;
    public static final int DEBUG_VECTOR = 0xFFEA;

    // Opcodes
    public static final int NOP = 0x00;
    public static final int MOV = 0x01;
    public static final int ADD = 0x02;
    public static final int SUB = 0x03;
    public static final int INC = 0x04;
    public static final int DEC = 0x05;
    public static final int NEG = 0x06;
    public static final int JMP = 0x07;
    public static final int CMP = 0x08;
    public static final int TST = 0x09;
    public static final int AND = 0x0A;
    public static final int OR = 0x0B;
    public static final int XOR = 0x0C;
    public static final int NOT = 0x0D;
    public static final int RETI = 0x0E;
    // 0x0F RESERVED
    public static final int MULU = 0x10;
    public static final int DIVU = 0x11;
    public static final int PUSH = 0x12;
    public static final int POP = 0x13;
    public static final int CALL = 0x14;
    public static final int RET = 0x15;
    public static final int MUL = 0x16;
    public static final int DIV = 0x17;
    public static final int ROL = 0x18;
    public static final int ROR = 0x19;
    public static final int SHL = 0x1A;
    public static final int SHR = 0x1B;
    public static final int BTST = 0x1C;
    public static final int BSET = 0x1D;
    public static final int BCLR = 0x1E;
    public static final int JCOND = 0x1F;

    // Addressing modes
    public static final int MODE_REG = 0; // Rn
    public static final int MODE_IND = 1; // [Rn]
    public static final int MODE_ABS = 2; // $addr
    public static final int MODE_IMM = 3; // immediate

    public Cpu() {
        reset();
    }

    public void reset() {
        pc = readWord(RESET_VECTOR);
        sp = SP_MAX; // Initialize SP to top of stack area (0xFBFF)
        flags = 0;
        bankReg = 0;
        cpuCtrl = 0;
        // Initialize INT_CTRL with NMI always enabled
        intCtrl = IM_NMI; // NMI mask bit always 1, all others 0, no pending interrupts
        // Clear registers
        for (int i = 0; i < 8; i++) regs[i] = 0;
    }

    // Memory access with banking
    public int logicalToPhysical(int addr) {
        if (addr >= IO_BASE) {
            return BANK0_BASE + addr; // IO always Bank0
        } else {
            return (bankReg == 0 ? BANK0_BASE : BANK1_BASE) + addr;
        }
    }

    public byte readByte(int addr) {
        int phys = logicalToPhysical(addr);
        // Handle I/O reads
        if (addr >= IO_BASE) {
            return (byte) handleIoRead(addr);
        }
        return memory[phys];
    }

    public void writeByte(int addr, byte value) {
        int phys = logicalToPhysical(addr);
        memory[phys] = value;
        // Handle I/O writes
        if (addr >= IO_BASE) {
            handleIoWrite(addr, value & 0xFF);
        }
    }

    public int readWord(int addr) {
        // Handle I/O reads for specific registers
        if (addr >= IO_BASE) {
            return handleIoReadWord(addr);
        }
        // BIG ENDIAN (word stored high byte first)
        int high = readByte(addr) & 0xFF;
        int low = readByte(addr + 1) & 0xFF;
        return (high << 8) | low;
    }

    public void writeWord(int addr, int value) {
        // Handle I/O writes for specific registers
        if (addr >= IO_BASE) {
            handleIoWriteWord(addr, value);
            return;
        }
        // BIG ENDIAN (word stored high byte first)
        writeByte(addr, (byte) ((value >> 8) & 0xFF));
        writeByte(addr + 1, (byte) (value & 0xFF));
    }

    private void handleIoWrite(int addr, int value) {
        switch (addr) {
            case BANK_REG:   bankReg = value & 1; break;
            case VIDEO_CTRL: videoCtrl = value; break;
            case VSYNC_STAT: vsyncStat = value; break;
            case CPU_CTRL:   cpuCtrl = value; handleCpuCtrl(); break;
            case DMA_CTRL:   dmaCtrl = value; handleDmaCtrl(); break;
            case INT_CTRL:   handleIntCtrlWrite(value); break;
            // DMA registers (BIG-ENDIAN: high byte at lower address)
            // The DMA source/destination registers are 16-bit values split across
            // two consecutive I/O bytes. The HIGH byte is at the lower I/O address
            // (e.g. 0xFE04 = SRC high, 0xFE05 = SRC low) to match BIG-ENDIAN memory layout.
            case DMA_SRC:     dmaSrc = (dmaSrc & 0x00FF) | (value << 8); break; // high byte
            case DMA_SRC + 1: dmaSrc = (dmaSrc & 0xFF00) | value; break; // low byte
            case DMA_DST:     dmaDst = (dmaDst & 0x00FF) | (value << 8); break; // high byte
            case DMA_DST + 1: dmaDst = (dmaDst & 0xFF00) | value; break; // low byte
            case DMA_LEN:     dmaLen = (dmaLen & 0x00FF) | (value << 8); break; // high byte
            case DMA_LEN + 1: dmaLen = (dmaLen & 0xFF00) | value; break; // low byte
        }
    }

    private int handleIoRead(int addr) {
        switch (addr) {
            case BANK_REG:   return bankReg;
            case VIDEO_CTRL: return videoCtrl;
            case VSYNC_STAT: return vsyncStat;
            case CPU_CTRL:   return cpuCtrl;
            case DMA_CTRL:   return dmaCtrl;
            case INT_CTRL:   return intCtrl;
            // DMA registers (BIG-ENDIAN: high byte at lower address)
            case DMA_SRC:     return (dmaSrc >> 8) & 0xFF; // high byte
            case DMA_SRC + 1: return dmaSrc & 0xFF; // low byte
            case DMA_DST:     return (dmaDst >> 8) & 0xFF; // high byte
            case DMA_DST + 1: return dmaDst & 0xFF; // low byte
            case DMA_LEN:     return (dmaLen >> 8) & 0xFF; // high byte
            case DMA_LEN + 1: return dmaLen & 0xFF; // low byte
            default:
                // For other I/O addresses, read from memory
                int phys = logicalToPhysical(addr);
                return memory[phys] & 0xFF;
        }
    }

    private int handleIoReadWord(int addr) {
        switch (addr) {
            case BANK_REG:   return bankReg;
            case VIDEO_CTRL: return videoCtrl;
            case VSYNC_STAT: return vsyncStat;
            case CPU_CTRL:   return cpuCtrl;
            case DMA_CTRL:   return dmaCtrl;
            case INT_CTRL:   return intCtrl;
            // DMA registers (16-bit values)
            case DMA_SRC:    return dmaSrc;
            case DMA_DST:    return dmaDst;
            case DMA_LEN:    return dmaLen;
            default:
                // Fall back to byte-wise reading for other addresses
                int high = readByte(addr) & 0xFF;
                int low = readByte(addr + 1) & 0xFF;
                return (high << 8) | low;
        }
    }

    private void handleIoWriteWord(int addr, int value) {
        switch (addr) {
            case BANK_REG:   bankReg = value & 1; break;
            case VIDEO_CTRL: videoCtrl = value; break;
            case VSYNC_STAT: vsyncStat = value; break;
            case CPU_CTRL:   cpuCtrl = value; handleCpuCtrl(); break;
            case DMA_CTRL:   dmaCtrl = value; handleDmaCtrl(); break;
            case INT_CTRL:   handleIntCtrlWrite(value); break;
            // DMA registers (16-bit values)
            case DMA_SRC:    dmaSrc = value; break;
            case DMA_DST:    dmaDst = value; break;
            case DMA_LEN:    dmaLen = value; break;
            default:
                // Fall back to byte-wise writing for other addresses
                writeByte(addr, (byte) ((value >> 8) & 0xFF));
                writeByte(addr + 1, (byte) (value & 0xFF));
                break;
        }
    }

    private void handleCpuCtrl() {
        if ((cpuCtrl & CPU_RST) != 0) {
            reset();
            cpuCtrl &= ~CPU_RST; // Clear after reset
        }
        if ((cpuCtrl & CPU_DBG) != 0) {
            // Trigger debug interrupt
            interrupt(DEBUG_VECTOR);
            cpuCtrl &= ~CPU_DBG; // Clear after trigger
        }
        // HLT handled in run loop
    }

    private void handleDmaCtrl() {
        if ((dmaCtrl & DMA_STRT) != 0) {
            startDma();
            dmaCtrl &= ~DMA_STRT;
        }
    }

    private void handleIntCtrlWrite(int value) {
        // Only allow writing to interrupt mask bits (0-3)
        // Status bits (4-7) are read-only and managed by the system
        int maskBits = value & 0x0F; // Extract bits 0-3 (interrupt masks)
        int statusBits = intCtrl & 0xF0; // Preserve bits 4-7 (interrupt status)
        
        // NMI is always enabled (bit 3 always 1)
        maskBits |= IM_NMI;
        
        intCtrl = statusBits | maskBits;
    }

    private void startDma() {
        dmaCtrl |= DMA_BUSY;
        // DMA from Bank0 logical to Bank1 logical
        for (int i = 0; i < dmaLen; i++) {
            byte data = readByte(dmaSrc + i); // From Bank0 logical
            int dstPhys = BANK1_BASE + ((dmaDst + i) & 0xFFFF); // Bank1 physical
            memory[dstPhys] = data;
        }
        dmaLen = 0;
        dmaCtrl &= ~DMA_BUSY;
        
        // Trigger DMA completion interrupt if enabled
        triggerDmaInterrupt();
    }

    // Public methods for peripherals to trigger interrupts
    public void triggerIrqInterrupt() {
        intCtrl |= IS_IRQ;
    }
    
    public void triggerDmaInterrupt() {
        intCtrl |= IS_DMA;
    }
    
    public void triggerVsyncInterrupt() {
        intCtrl |= IS_VSYNC;
    }
    
    public void triggerNmiInterrupt() {
        intCtrl |= IS_NMI;
    }

    private void checkAndHandleInterrupts() {
        // Check interrupts in priority order: NMI > IRQ > DMA > VSYNC
        
        // NMI - Non-Maskable Interrupt (highest priority)
        if ((intCtrl & IS_NMI) != 0) {
            intCtrl &= ~IS_NMI; // Clear pending status
            interrupt(NMI_VECTOR);
            return;
        }
        
        // IRQ - Generic interrupt request
        if ((intCtrl & IS_IRQ) != 0 && (intCtrl & IM_IRQ) != 0) {
            intCtrl &= ~IS_IRQ; // Clear pending status
            interrupt(IRQ_VECTOR);
            return;
        }
        
        // DMA - DMA completion interrupt
        if ((intCtrl & IS_DMA) != 0 && (intCtrl & IM_DMA) != 0) {
            intCtrl &= ~IS_DMA; // Clear pending status
            interrupt(DMA_VECTOR);
            return;
        }
        
        // VSYNC - Video sync interrupt
        if ((intCtrl & IS_VSYNC) != 0 && (intCtrl & IM_VSYNC) != 0) {
            intCtrl &= ~IS_VSYNC; // Clear pending status
            interrupt(VSYNC_VECTOR);
            return;
        }
    }

    public void interrupt(int vectorAddr) {
        // Push PC and FLAGS
        pushWord(pc);
        pushWord(flags);
        flags |= FLAG_I; // Disable interrupts
        pc = readWord(vectorAddr);
    }

    private void pushWord(int value) {
        sp -= 2;
        // Stack bounds checking with new limits
        if (sp < SP_MIN) {
            cpuCtrl |= CPU_HLT; // CPU PANIC on underflow
            throw new RuntimeException("Stack underflow");
        }
        writeWord(sp, value);
    }

    // private int popWord() {
    //     int value = readWord(sp);
    //     sp += 2;
    //     // Stack bounds checking - ensure SP stays within valid range
    //     if (sp > SP_MAX + 2) {
    //         cpuCtrl |= CPU_HLT; // CPU PANIC on overflow  
    //         throw new RuntimeException("Stack overflow");
    //     }
    //     return value;
    // }

    private int popWord() {
        int value = readWord(sp);
        sp += 2;
        // Stack bounds checking - ensure SP stays within valid range
        if (sp > SP_MAX + 2) {
            cpuCtrl |= CPU_HLT; // CPU PANIC on overflow  
            throw new RuntimeException("Stack overflow");
        }
        return value;
    }


    // Addressing mode resolution
    public int getOperand(int mode, int reg, boolean isSrc, int imm) {
        switch (mode) {
            case MODE_REG: return regs[reg];
            case MODE_IND: return readWord(regs[reg]);
            case MODE_ABS: return readWord(imm);
            case MODE_IMM: return imm;
            default: throw new IllegalArgumentException("Invalid mode");
        }
    }

    public void setOperand(int mode, int reg, int value, int imm) {
        switch (mode) {
            case MODE_REG: regs[reg] = value; break;
            case MODE_IND: writeWord(regs[reg], value); break;
            case MODE_ABS: writeWord(imm, value); break;
            // IMM not for dst
        }
    }

    // Flag updates
    private void updateFlags(int result, int src, int dst, boolean isSub, boolean isLogic) {
        flags &= ~(FLAG_N | FLAG_Z | FLAG_C | FLAG_V | FLAG_X);
        int stored = result & 0xFFFF;
        if (stored == 0) flags |= FLAG_Z;
        if (!isLogic && (stored & 0x8000) != 0) flags |= FLAG_N;
        // C and V for arithmetic
        if (!isLogic) {
            // Carry for add/sub
            if (isSub) {
                if (dst < src) flags |= FLAG_C;
            } else {
                if (result > 0xFFFF) flags |= FLAG_C;
            }
            // Overflow
            if (isSub) {
                if (((dst & 0x8000) != (src & 0x8000)) && ((dst & 0x8000) != (stored & 0x8000))) flags |= FLAG_V;
            } else {
                if (((dst & 0x8000) == (src & 0x8000)) && ((dst & 0x8000) != (stored & 0x8000))) flags |= FLAG_V;
            }
        }
    }

    // Execute one instruction
    public void step() {
        if ((cpuCtrl & CPU_HLT) != 0) return; // Halted

        // Check for pending interrupts (if interrupts are enabled)
        if ((flags & FLAG_I) == 0) {
            checkAndHandleInterrupts();
        }

        int instr = readWord(pc);
        pc += 2;

        int opcode = (instr >> 11) & 0x1F;
        int size = (instr >> 10) & 1; // 0=byte, 1=word
        int srcMode = (instr >> 8) & 3;
        int srcReg = (instr >> 5) & 7;
        int dstMode = (instr >> 3) & 3;
        int dstReg = instr & 7;

        int imm = 0;
        int srcImm = 0;  // Separate immediate for source  
        int dstImm = 0;  // Separate immediate for destination
        
        if (srcMode == MODE_IMM || srcMode == MODE_ABS) {
            srcImm = readWord(pc);
            imm = srcImm; // Backward compatibility
            pc += 2;
        }
        if (dstMode == MODE_IMM || dstMode == MODE_ABS) {
            dstImm = readWord(pc); 
            imm = dstImm; // Backward compatibility (dst never uses IMM anyway)  
            pc += 2;
        }

        executeOpcode(opcode, size, srcMode, srcReg, dstMode, dstReg, imm, srcImm, dstImm);
    }

    private void executeOpcode(int opcode, int size, int srcMode, int srcReg, int dstMode, int dstReg, int imm, int srcImm, int dstImm) {
        switch (opcode) {
            case NOP: break;
            case MOV: {
                int val = getOperand(srcMode, srcReg, true, srcImm);
                setOperand(dstMode, dstReg, val, dstImm);
                break;
            }
            case ADD: {
                int src = getOperand(srcMode, srcReg, true, srcImm);
                int dst = getOperand(dstMode, dstReg, false, dstImm);
                int result = dst + src;
                setOperand(dstMode, dstReg, result & 0xFFFF, dstImm);
                updateFlags(result, src, dst, false, false);
                break;
            }
            case SUB: {
                int src = getOperand(srcMode, srcReg, true, srcImm);
                int dst = getOperand(dstMode, dstReg, false, dstImm);
                int result = dst - src;
                setOperand(dstMode, dstReg, result & 0xFFFF, dstImm);
                updateFlags(result, src, dst, true, false);
                break;
            }
            case INC: {
                int val = getOperand(dstMode, dstReg, false, imm) + 1;
                setOperand(dstMode, dstReg, val & 0xFFFF, imm);
                updateFlags(val, 1, val - 1, false, false);
                break;
            }
            case DEC: {
                int val = getOperand(dstMode, dstReg, false, imm) - 1;
                setOperand(dstMode, dstReg, val & 0xFFFF, imm);
                updateFlags(val, 1, val + 1, true, false);
                break;
            }
            case NEG: {
                int original = getOperand(dstMode, dstReg, false, imm) & 0xFFFF;
                int val = (-original) & 0xFFFF;
                setOperand(dstMode, dstReg, val, imm);
                updateFlags(val, 0, original, true, false);
                // NEG should set carry only if original value was non-zero
                if (original != 0) {
                    flags |= FLAG_C;
                }
                break;
            }
            case JMP: {
                if (dstMode == MODE_ABS) {
                    pc = imm;
                } else {
                    pc = getOperand(dstMode, dstReg, false, imm);
                }
                break;
            }
            case CMP: {
                int src = getOperand(srcMode, srcReg, true, imm);
                int dst = getOperand(dstMode, dstReg, false, imm);
                int result = dst - src;
                updateFlags(result, src, dst, true, false);
                break;
            }
            case TST: {
                int src = getOperand(srcMode, srcReg, true, imm);
                int dst = getOperand(dstMode, dstReg, false, imm);
                int result = dst & src;
                updateFlags(result, src, dst, false, true);
                break;
            }
            case AND: {
                int src = getOperand(srcMode, srcReg, true, imm);
                int dst = getOperand(dstMode, dstReg, false, imm);
                int result = dst & src;
                setOperand(dstMode, dstReg, result, imm);
                updateFlags(result, src, dst, false, true);
                break;
            }
            case OR: {
                int src = getOperand(srcMode, srcReg, true, imm);
                int dst = getOperand(dstMode, dstReg, false, imm);
                int result = dst | src;
                setOperand(dstMode, dstReg, result, imm);
                updateFlags(result, src, dst, false, true);
                break;
            }
            case XOR: {
                int src = getOperand(srcMode, srcReg, true, imm);
                int dst = getOperand(dstMode, dstReg, false, imm);
                int result = dst ^ src;
                setOperand(dstMode, dstReg, result, imm);
                updateFlags(result, src, dst, false, true);
                break;
            }
            case NOT: {
                int original = getOperand(dstMode, dstReg, false, imm) & 0xFFFF;
                int val = (~original) & 0xFFFF;
                setOperand(dstMode, dstReg, val, imm);
                updateFlags(val, 0, original, false, true);
                break;
            }
            case RETI: {
                flags = popWord(); // Restore FLAGS first 
                pc = popWord();    // Restore PC second
                // RETI doesn't modify SP (it's already adjusted by popWord calls)
                break;
            }
            case PUSH: {
                int val = getOperand(srcMode, srcReg, true, imm);
                pushWord(val);
                break;
            }
            case POP: {
                int val = popWord();
                setOperand(dstMode, dstReg, val, imm);
                break;
            }
            case CALL: {
                // CALL convention: push the address of the next instruction (current
                // `pc` points to the next instruction after decode/fetch). This means
                // RET will restore execution to the instruction immediately following
                // the CALL. Tests and callers expect the pushed return address to be
                // the address after the full CALL instruction (not an earlier offset).
                pushWord(pc);
                if (dstMode == MODE_ABS) {
                    pc = imm;
                } else {
                    int target = getOperand(dstMode, dstReg, false, imm);
                    pc = target;
                }
                break;
            }
            case RET: {
                pc = popWord();
                break;
            }
            case MULU: {
                int src = getOperand(srcMode, srcReg, true, imm);
                int dst = getOperand(dstMode, dstReg, false, imm);
                long result = (long) (dst & 0xFFFF) * (src & 0xFFFF);
                // Store result in the destination register and the next register
                setOperand(dstMode, dstReg, (int)(result & 0xFFFF), dstImm);
                if (dstMode == MODE_REG && dstReg < 7) {
                    regs[dstReg + 1] = (int) ((result >> 16) & 0xFFFF);
                }
                updateFlags((int) result, src, dst, false, false);
                if (result > 0xFFFF) flags |= FLAG_X;
                break;
            }
            case DIVU: {
                int src = getOperand(srcMode, srcReg, true, imm);
                int dst = getOperand(dstMode, dstReg, false, imm);
                if (src == 0) {
                    flags |= FLAG_X;
                    break;
                }
                int quotient = dst / src;
                int remainder = dst % src;
                // Store quotient in destination register and remainder in next register
                setOperand(dstMode, dstReg, quotient, dstImm);
                if (dstMode == MODE_REG && dstReg < 7) {
                    regs[dstReg + 1] = remainder;
                }
                updateFlags(quotient, src, dst, false, false);
                break;
            }
            case MUL: {
                int src = getOperand(srcMode, srcReg, true, imm);
                int dst = getOperand(dstMode, dstReg, false, imm);
                long result = (short) dst * (short) src;
                // Store result in the destination register and the next register
                setOperand(dstMode, dstReg, (int)(result & 0xFFFF), dstImm);
                if (dstMode == MODE_REG && dstReg < 7) {
                    regs[dstReg + 1] = (int) ((result >> 16) & 0xFFFF);
                }
                updateFlags((int) result, src, dst, false, false);
                if (result > 0x7FFFFFFFL || result <= (long)Integer.MIN_VALUE - 1) flags |= FLAG_X;
                break;
            }
            case DIV: {
                int src = getOperand(srcMode, srcReg, true, imm);
                int dst = getOperand(dstMode, dstReg, false, imm);
                if (src == 0) {
                    flags |= FLAG_X;
                    break;
                }
                int quotient = (short) dst / (short) src;
                int remainder = (short) dst % (short) src;
                // Store quotient in destination register and remainder in next register
                setOperand(dstMode, dstReg, quotient & 0xFFFF, dstImm);
                if (dstMode == MODE_REG && dstReg < 7) {
                    regs[dstReg + 1] = remainder & 0xFFFF;
                }
                updateFlags(quotient, src, dst, false, false);
                break;
            }
            case ROL: {
                int val = getOperand(dstMode, dstReg, false, imm);
                int oldVal = val;
                int msb = (oldVal & 0x8000) != 0 ? 1 : 0;
                val = ((oldVal << 1) | msb) & 0xFFFF;
                setOperand(dstMode, dstReg, val, imm);
                int newCarry = msb;
                updateFlags(val, 0, val, false, true);
                flags = (flags & ~FLAG_C) | (newCarry << 1);
                if (newCarry != 0) flags |= FLAG_X;
                break;
            }
            case ROR: {
                int val = getOperand(dstMode, dstReg, false, imm);
                int oldVal = val;
                int lsb = oldVal & 1;
                val = ((oldVal >> 1) | (lsb << 15)) & 0xFFFF;
                setOperand(dstMode, dstReg, val, imm);
                int newCarry = lsb;
                updateFlags(val, 0, val, false, true);
                flags = (flags & ~FLAG_C) | (newCarry << 1);
                if (newCarry != 0) flags |= FLAG_X;
                break;
            }
            case SHL: {
                int val = getOperand(dstMode, dstReg, false, imm);
                int oldVal = val;
                val = (oldVal << 1) & 0xFFFF;
                setOperand(dstMode, dstReg, val, imm);
                int newCarry = (oldVal & 0x8000) != 0 ? 1 : 0;
                updateFlags(val, 0, val, false, true);
                flags = (flags & ~FLAG_C) | (newCarry << 1);
                if (newCarry != 0) flags |= FLAG_X;
                break;
            }
            case SHR: {
                int val = getOperand(dstMode, dstReg, false, imm);
                int oldVal = val;
                val = (oldVal >> 1) & 0xFFFF;
                setOperand(dstMode, dstReg, val, imm);
                int newCarry = (oldVal & 1) != 0 ? 1 : 0;
                updateFlags(val, 0, val, false, true);
                flags = (flags & ~FLAG_C) | (newCarry << 1);
                if (newCarry != 0) flags |= FLAG_X;
                break;
            }
            case BTST: {
                int bit = getOperand(srcMode, srcReg, true, imm) & 15;
                int val = getOperand(dstMode, dstReg, false, imm);
                int mask = 1 << bit;
                flags = (flags & ~FLAG_Z) | (((val & mask) == 0) ? FLAG_Z : 0);
                break;
            }
            case BSET: {
                int bit = getOperand(srcMode, srcReg, true, imm) & 15;
                int val = getOperand(dstMode, dstReg, false, imm);
                val |= (1 << bit);
                setOperand(dstMode, dstReg, val, imm);
                break;
            }
            case BCLR: {
                int bit = getOperand(srcMode, srcReg, true, imm) & 15;
                int val = getOperand(dstMode, dstReg, false, imm);
                val &= ~(1 << bit);
                setOperand(dstMode, dstReg, val, imm);
                break;
            }
            case JCOND: {
                int condCode = ((size << 3) | srcReg) & 0xF;
                int target = imm;
                boolean jump = false;
                switch (condCode) {
                    case 0: jump = true; break; // AL
                    case 1: jump = (flags & FLAG_Z) != 0; break; // EQ
                    case 2: jump = (flags & FLAG_Z) == 0; break; // NE
                    case 3: jump = (flags & FLAG_C) != 0; break; // CS
                    case 4: jump = (flags & FLAG_C) == 0; break; // CC
                    case 5: jump = (flags & FLAG_N) != 0; break; // MI
                    case 6: jump = (flags & FLAG_N) == 0; break; // PL
                    case 7: jump = (flags & FLAG_V) != 0; break; // VS
                    case 8: jump = (flags & FLAG_V) == 0; break; // VC
                    case 9: jump = ((flags & FLAG_Z) == 0) && (((flags & FLAG_N) != 0) == ((flags & FLAG_V) != 0)); break; // GT
                    case 10: jump = ((flags & FLAG_N) != 0) == ((flags & FLAG_V) != 0); break; // GE
                    case 11: jump = ((flags & FLAG_N) != 0) != ((flags & FLAG_V) != 0); break; // LT
                    case 12: jump = ((flags & FLAG_Z) != 0) || (((flags & FLAG_N) != 0) != ((flags & FLAG_V) != 0)); break; // LE
                    case 13: jump = ((flags & FLAG_C) == 0) && ((flags & FLAG_Z) == 0); break; // HI
                    case 14: jump = (flags & FLAG_C) == 0; break; // HS
                    case 15: jump = ((flags & FLAG_C) != 0) || ((flags & FLAG_Z) != 0); break; // LO
                }
                if (jump) pc = target;
                break;
            }
            default: throw new IllegalArgumentException("Unknown opcode: " + opcode);
        }
    }
}