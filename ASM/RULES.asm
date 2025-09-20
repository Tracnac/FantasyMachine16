; Fantasy Machine 16 - Sample program
.org $500
    ; Stupid loop that does nothing
LOOP:
    MOV.B 0x00, R0    ; Charge 0 dans R0 (byte)
    CMP.B 0x00, R0    ; Compare R0 avec 0
    JCOND EQ, LOOP       ; Si égal (Z=1), saute à LOOP
    ; Unreachable code...
    JMP MAIN
.org $1000
    ; Another stupid loop that does nothing
MAIN:
    MOV.B 0x00, R0    ; Charge 1 dans R0 (byte)
    MOV.B %00000001, R1    ; Charge 1 dans R1 (word)
    CMP.B 0x00, R0    ; Compare R0 avec 0
    JCOND EQ, MAIN      ; Si égal (Z=1), saute à MAIN
    ; Unreachable code...
    JMP LOOP
.end
