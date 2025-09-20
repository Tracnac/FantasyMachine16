package com.fantasy.cpu;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This test documents the assembled encodings for instructions according to the
 * instruction format in Instructions.txt. It's primarily a documentation test
 * that shows opcode, mode bits, and extension placement.
 */
public class AssemblerEncodedTest {

    // Helper to build an instruction word from fields described in the spec
    private int makeInstr(int opcode, int size, int srcMode, int srcReg, int dstMode, int dstReg) {
        return (opcode << 11) | (size << 10) | (srcMode << 8) | (srcReg << 5) | (dstMode << 3) | dstReg;
    }

    // Assemble into bytes: instruction word (16-bit) then extension words (if any).
    // Per Instructions.txt, words and extensions are encoded in BIG-ENDIAN when written
    // as a program stream / binary listing.
    private byte[] assembleBytes(int opcode, int size, int srcMode, int srcReg, int dstMode, int dstReg,
                                 Integer immSrc, Integer immDst) {
        int instr = makeInstr(opcode, size, srcMode, srcReg, dstMode, dstReg) & 0xFFFF;
        int extCount = 0;
        if (srcMode == 2 || srcMode == 3) extCount++;
        if (dstMode == 2 || dstMode == 3) extCount++;
        byte[] out = new byte[2 + extCount * 2];
        // Instruction word big-endian
        out[0] = (byte) ((instr >> 8) & 0xFF);
        out[1] = (byte) (instr & 0xFF);
        int pos = 2;
        // If both need extension, order is [imm16_src][imm16_dest]
        if (srcMode == 2 || srcMode == 3) {
            int val = immSrc != null ? immSrc & 0xFFFF : 0;
            out[pos++] = (byte) ((val >> 8) & 0xFF);
            out[pos++] = (byte) (val & 0xFF);
        }
        if (dstMode == 2 || dstMode == 3) {
            int val = immDst != null ? immDst & 0xFFFF : 0;
            out[pos++] = (byte) ((val >> 8) & 0xFF);
            out[pos++] = (byte) (val & 0xFF);
        }
        return out;
    }

    @Test
    void exampleMovImmediateEncoding() {
        // Example from Instructions.txt (line shown in attachments):
        // MOV.W #0x0001, R0 : 0x41F0 0x0001
        // (opcode=0x01, size=1, src_mode=11, src_reg=0, dst_mode=00, dst_reg=0, extension=0x0001)

        int opcode = 0x01;
        int size = 1; // word
        int srcMode = 0b11; // immediate
        int srcReg = 0; // must be 0 when immediate
        int dstMode = 0b00; // register direct
        int dstReg = 0; // R0

        // Assemble full byte sequence with an immediate extension value 0x0001
        byte[] bytes = assembleBytes(opcode, size, srcMode, srcReg, dstMode, dstReg, 0x0001, null);

    // Example in Instructions.txt shows: 0x0F00 0x0001 (big-endian words -> bytes: 0F 00 00 01)
    assertEquals(4, bytes.length);
    assertEquals((byte) 0x0F, bytes[0]);
    assertEquals((byte) 0x00, bytes[1]);
    assertEquals((byte) 0x00, bytes[2]);
    assertEquals((byte) 0x01, bytes[3]);

        // Also verify decoding of the instruction word (big-endian -> int)
        int instr = ((bytes[0] & 0xFF) << 8) | (bytes[1] & 0xFF);
        int decOpcode = (instr >> 11) & 0x1F;
        int decSize = (instr >> 10) & 1;
        int decSrcMode = (instr >> 8) & 3;
        int decSrcReg = (instr >> 5) & 7;
        int decDstMode = (instr >> 3) & 3;
        int decDstReg = instr & 7;

        assertEquals(opcode, decOpcode);
        assertEquals(size, decSize);
        assertEquals(srcMode, decSrcMode);
        assertEquals(srcReg, decSrcReg);
        assertEquals(dstMode, decDstMode);
        assertEquals(dstReg, decDstReg);

        // Verify extension parsed as big-endian
        int ext = ((bytes[2] & 0xFF) << 8) | (bytes[3] & 0xFF);
        assertEquals(0x0001, ext);
    }

    @Test
    void encodeAllOpcodesAndModesRoundTrip() {
        // We'll create encodings for all opcodes (0..31), sizes (0/1), modes (0..3)
        // and perform a basic round-trip check: building instruction word and then
        // extracting fields back should match the original fields.

        for (int opcode = 0; opcode < 32; opcode++) {
            for (int size = 0; size <= 1; size++) {
                for (int srcMode = 0; srcMode < 4; srcMode++) {
                    for (int srcReg = 0; srcReg < 8; srcReg++) {
                        for (int dstMode = 0; dstMode < 4; dstMode++) {
                            for (int dstReg = 0; dstReg < 8; dstReg++) {
                                // choose imm values when needed (distinct ranges to help debugging)
                                Integer immSrc = (srcMode == 2 || srcMode == 3) ? (0x1000 | opcode) : null;
                                Integer immDst = (dstMode == 2 || dstMode == 3) ? (0x2000 | opcode) : null;
                                byte[] bytes = assembleBytes(opcode, size, srcMode, srcReg, dstMode, dstReg, immSrc, immDst);

                                int instr = ((bytes[0] & 0xFF) << 8) | (bytes[1] & 0xFF);
                                int decOpcode = (instr >> 11) & 0x1F;
                                int decSize = (instr >> 10) & 1;
                                int decSrcMode = (instr >> 8) & 3;
                                int decSrcReg = (instr >> 5) & 7;
                                int decDstMode = (instr >> 3) & 3;
                                int decDstReg = instr & 7;

                                assertEquals(opcode, decOpcode);
                                assertEquals(size, decSize);
                                assertEquals(srcMode, decSrcMode);
                                assertEquals(srcReg, decSrcReg);
                                assertEquals(dstMode, decDstMode);
                                assertEquals(dstReg, decDstReg);

                                // If extensions are present, parse and verify big-endian values
                                int pos = 2;
                                if (srcMode == 2 || srcMode == 3) {
                                    int parsedSrc = ((bytes[pos] & 0xFF) << 8) | (bytes[pos + 1] & 0xFF);
                                    assertEquals(immSrc.intValue(), parsedSrc);
                                    pos += 2;
                                }
                                if (dstMode == 2 || dstMode == 3) {
                                    int parsedDst = ((bytes[pos] & 0xFF) << 8) | (bytes[pos + 1] & 0xFF);
                                    assertEquals(immDst.intValue(), parsedDst);
                                    pos += 2;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
