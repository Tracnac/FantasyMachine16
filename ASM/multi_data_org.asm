; Fantasy Machine 16 - Sample program

.start
LOOP_START:
    MOV.B 0x00, R0    ; Charge 0 dans R0 (byte)
    CMP.B 0x00, R0    ; Compare R0 avec 0
    JCOND EQ, LOOP_START; Si égal (Z=1), saute à LOOP_START
    ; Unreachable code...
    JMP END

.org $200
LOOP_200:
    MOV.W MYWORD_2, R1 ; Charge $MYWORD_2 dans R1 (word)
    MOV.B 0x00, R0    ; Charge 0 dans R0 (byte)
    CMP.B 0x00, R0    ; Compare R0 avec 0
    JCOND EQ, LOOP_200  ; Si égal (Z=1), saute à LOOP_200
    ; Unreachable code...

.org $500
ORG_500:
    ; Stupid loop that does nothing
LOOP_500:
    MOV.B 0x00, R0    ; Charge 0 dans R0 (byte)
    CMP.B 0x00, R0    ; Compare R0 avec 0
    JCOND EQ, LOOP_500 ; Si égal (Z=1), saute à LOOP_500
    ; Unreachable code...
    JMP END

.org $1000
    ; Another stupid loop that does nothing
LOOP_1000:
    MOV.B 0x00, R0    ; Charge 1 dans R0 (byte)
    MOV.B %00000001, R1    ; Charge 1 dans R1 (word)
    CMP.B 0x00, R0    ; Compare R0 avec 0
    JCOND EQ, LOOP_1000      ; Si égal (Z=1), saute à LOOP_1000
    ; Unreachable code...
    JMP END

END:
    MOV.W MYWORD_2, R3 ; Charge MYWORD_2 dans R3 (word)
    MOV.B 0x01, $FF09       ; Halt the CPU

.data
    MYBYTE_2: .byte 0x55
    MYBYTES_1: .bytes 0x11, 0x22, 0x33, 0x44
    MYASCII_1: .ascii "Hello, World!", 0x00
    MYBYTE_3: .byte 0x77
    MYWORD_2: .word 0xEEFF
.end
