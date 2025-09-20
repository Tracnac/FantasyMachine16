; Fantasy CPU 16-bit Comprehensive Assembly Test Program
; Tests all instruction types, addressing modes, and CPU features
; Each instruction shows expected bytecode encoding

.start
    NOP                        ; 0x0000 - No operation
    
.org $1000                    ; Start program at 0x1000

; =============================================================================
; SECTION 1: Basic Data Movement and Immediate Values
; =============================================================================
    MOV.W 0x1234, R0          ; 0x0F00 0x1234 - Load immediate word
    MOV.B 0x56, R1            ; 0x0E08 0x0056 - Load immediate byte
    MOV.W R0, R2               ; 0x0302 - Register to register (word)
    MOV.B R1, R3               ; 0x020B - Register to register (byte)
    
    ; Test absolute addressing
    MOV.W 0xABCD, $2000     ; 0x0F80 0xABCD 0x2000 - Immediate to absolute
    MOV.W $2000, R4          ; 0x0304 0x2000 - Absolute to register
    MOV.B R4, $2002          ; 0x0284 0x2002 - Register to absolute (byte)

; =============================================================================
; SECTION 2: Indirect Addressing and Memory Operations
; =============================================================================
    ; Set up pointer in R5
    MOV.W 0x3000, R5          ; 0x0F05 0x3000 - Load address into R5
    
    ; Test indirect addressing
    MOV.W 0xDEAD, [R5]        ; 0x0F45 0xDEAD - Immediate to indirect
    MOV.W [R5], R6             ; 0x0346 - Indirect to register
    MOV.B R6, [R5]             ; 0x0246 - Register to indirect (byte)
    
    ; Complex indirect operations
    MOV.W [R5], [R0]           ; 0x0385 - Indirect to indirect
    INC.W [R5]                 ; 0x0945 - Increment memory location

; =============================================================================
; SECTION 3: Arithmetic Operations
; =============================================================================
    ; Basic arithmetic with various addressing modes
    ADD.W 0x0100, R0          ; 0x4F00 0x0100 - Add immediate to register
    ADD.B R1, R2               ; 0x4209 - Add register to register (byte)
    SUB.W R2, R0               ; 0x6002 - Subtract register from register
    SUB.B 0x10, R3            ; 0x6E18 0x0010 - Subtract immediate from register
    
    ; Memory arithmetic
    ADD.W [R5], R4             ; 0x4344 - Add memory to register
    SUB.B R4, [R5]             ; 0x6245 - Subtract register from memory
    
    ; Increment/Decrement operations
    INC.W R0                   ; 0x0900 - Increment register (word)
    INC.B R1                   ; 0x0808 - Increment register (byte)
    DEC.W [R5]                 ; 0x0A45 - Decrement memory (word)
    DEC.B $3001              ; 0x0A80 0x3001 - Decrement absolute (byte)
    
    ; Negation
    NEG.W R2                   ; 0x0C02 - Negate register (word)
    NEG.B [R0]                 ; 0x0C40 - Negate memory (byte)

; =============================================================================
; SECTION 4: Logical Operations
; =============================================================================
    ; Bitwise operations
    AND.W 0xFF00, R0          ; 0x14F0 0xFF00 - Mask upper byte
    OR.B 0x0F, R1             ; 0x16E8 0x000F - Set lower nibble
    XOR.W R0, R2               ; 0x1802 - XOR registers
    NOT.B R3                   ; 0x1A18 - Bitwise NOT
    
    ; Logical operations with memory
    AND.B [R5], R4             ; 0x1444 - AND memory with register
    OR.W R4, [R5]              ; 0x1645 - OR register with memory
    XOR.B 0x55, [R0]          ; 0x18E0 0x0055 - XOR immediate with memory

; =============================================================================
; SECTION 5: Comparison and Testing
; =============================================================================
    ; Compare operations (set flags but don't store result)
    CMP.W R0, 0x1000          ; 0x21F0 0x1000 - Compare register with immediate
    CMP.B R1, R2               ; 0x2009 - Compare registers (byte)
    CMP.W [R5], R6             ; 0x2146 - Compare memory with register
    
    ; Test operations (logical AND but don't store result)
    TST.B R3, 0xFF            ; 0x22F8 0x00FF - Test all bits
    TST.W R4, R4               ; 0x2304 - Test register against itself
    TST.B [R0], 0x80          ; 0x22E0 0x0080 - Test sign bit in memory

; =============================================================================
; SECTION 6: Conditional Jumps (Tests all condition codes)
; =============================================================================
    ; Set up test values for conditions
    MOV.W 0x0000, R0          ; 0x0F00 0x0000 - Load zero
    CMP.W R0, 0x0000          ; 0x21F0 0x0000 - Compare with zero (sets Z flag)
    
test_conditions:
    JCOND AL, always_jump      ; 0xF9C0 + target - Always jump
    JCOND EQ, equal_jump       ; 0xF9C8 + target - Jump if equal (Z=1)
    JCOND NE, not_equal_jump   ; 0xF9D0 + target - Jump if not equal (Z=0)
    JCOND CS, carry_set_jump   ; 0xF9D8 + target - Jump if carry set
    JCOND CC, carry_clear_jump ; 0xF9E0 + target - Jump if carry clear
    JCOND MI, minus_jump       ; 0xF9E8 + target - Jump if negative
    JCOND PL, plus_jump        ; 0xF9F0 + target - Jump if positive
    JCOND VS, overflow_jump    ; 0xF9F8 + target - Jump if overflow
    JCOND VC, no_overflow_jump ; 0xFDC0 + target - Jump if no overflow
    JCOND GT, greater_jump     ; 0xFDC8 + target - Jump if signed greater
    JCOND GE, greater_eq_jump  ; 0xFDD0 + target - Jump if signed greater/equal
    JCOND LT, less_jump        ; 0xFDD8 + target - Jump if signed less
    JCOND LE, less_eq_jump     ; 0xFDE0 + target - Jump if signed less/equal
    JCOND HI, higher_jump      ; 0xFDE8 + target - Jump if unsigned higher
    JCOND HS, higher_same_jump ; 0xFDF0 + target - Jump if unsigned higher/same
    JCOND LO, lower_jump       ; 0xFDF8 + target - Jump if unsigned lower/same

always_jump:
equal_jump:
not_equal_jump:
carry_set_jump:
carry_clear_jump:
minus_jump:
plus_jump:
overflow_jump:
no_overflow_jump:
greater_jump:
greater_eq_jump:
less_jump:
less_eq_jump:
higher_jump:
higher_same_jump:
lower_jump:

; =============================================================================
; SECTION 7: Stack Operations
; =============================================================================
stack_test:
    ; Push various data types onto stack
    PUSH.W 0x1111             ; 0x2700 0x1111 - Push immediate
    PUSH.W R0                  ; 0x2500 - Push register
    PUSH.W [R5]                ; 0x2545 - Push memory contents
    PUSH.W $3000             ; 0x2580 0x3000 - Push absolute address contents
    
    ; Pop data from stack
    POP.W R7                   ; 0x2607 - Pop to register
    POP.W [R5]                 ; 0x2645 - Pop to memory
    POP.W $3002              ; 0x2680 0x3002 - Pop to absolute address
    POP.W R6                   ; 0x2606 - Pop to register

; =============================================================================
; SECTION 8: Subroutine Calls
; =============================================================================
main_routine:
    ; Call subroutine with different addressing modes
    CALL subroutine            ; 0x2880 + target - Call absolute address
    CALL [R7]                  ; 0x2847 - Call indirect through register
    MOV.W interrupt_handler, R7 ; 0x0F07 + target - Load handler address
    CALL R7                    ; 0x2807 - Call register indirect
    JMP end_program            ; 0x0E80 + target - Jump to end

subroutine:
    ; Simple subroutine that modifies R0 and returns
    INC.W R0                   ; 0x0900 - Increment R0
    RET                        ; 0x2A00 - Return to caller

interrupt_handler:
    ; Interrupt service routine
    PUSH.W R0                  ; 0x2500 - Save R0
    PUSH.W R1                  ; 0x2508 - Save R1
    
    ; Do some interrupt work
    MOV.W 0x5555, R0          ; 0x0F00 0x5555 - Load test value
    NOT.W R0                   ; 0x1A00 - Invert all bits
    
    ; Restore registers and return from interrupt
    POP.W R1                   ; 0x2608 - Restore R1
    POP.W R0                   ; 0x2600 - Restore R0
    RETI                       ; 0x1C00 - Return from interrupt

; =============================================================================
; SECTION 9: Advanced Arithmetic (Multiplication and Division)
; =============================================================================
math_operations:
    ; Set up operands
    MOV.W 0x0010, R0          ; 0x0F00 0x0010 - Load 16
    MOV.W 0x0003, R1          ; 0x0F08 0x0003 - Load 3
    
    ; Unsigned operations (results go to R1:R0 for MUL, R0=quotient R1=remainder for DIV)
    MULU.W R1, R0              ; 0x2108 - Multiply R0 by R1 (unsigned)
    MOV.W 0x0064, R0          ; 0x0F00 0x0064 - Load 100
    MOV.W 0x0007, R1          ; 0x0F08 0x0007 - Load 7
    DIVU.W R1, R0              ; 0x2308 - Divide R0 by R1 (unsigned)
    
    ; Signed operations
    MOV.W 0xFFF0, R0          ; 0x0F00 0xFFF0 - Load -16 (signed)
    MOV.W 0x0003, R1          ; 0x0F08 0x0003 - Load 3
    MUL.W R1, R0               ; 0x2C08 - Multiply R0 by R1 (signed)
    MOV.W 0xFF9C, R0          ; 0x0F00 0xFF9C - Load -100 (signed)
    MOV.W 0x0007, R1          ; 0x0F08 0x0007 - Load 7
    DIV.W R1, R0               ; 0x2E08 - Divide R0 by R1 (signed)

; =============================================================================
; SECTION 10: Bit Manipulation and Shifts
; =============================================================================
bit_operations:
    MOV.W 0xF0F0, R0          ; 0x0F00 0xF0F0 - Load test pattern
    
    ; Bit testing, setting, and clearing
    BTST.W 4, R0              ; 0x38F0 0x0004 - Test bit 4
    BSET.B 2, R0              ; 0x3A40 0x0002 - Set bit 2
    BCLR.W 8, R0              ; 0x3CF0 0x0008 - Clear bit 8
    
    ; Test bit operations on memory
    BTST.B 7, [R5]            ; 0x38E5 0x0007 - Test bit 7 in memory
    BSET.W 15, $3000        ; 0x3B80 0x000F 0x3000 - Set bit 15 in absolute address
    
    ; Shift operations
    MOV.B %10101010, R1      ; 0x0E08 0x00AA - Load bit pattern
    SHL.B R1                   ; 0x3408 - Shift left (byte)
    SHR.W R0                   ; 0x3600 - Shift right (word)
    
    ; Rotation operations
    MOV.W 0x8001, R2          ; 0x0F02 0x8001 - Load pattern with bits at ends
    ROL.W R2                   ; 0x3002 - Rotate left
    ROR.B R1                   ; 0x3208 - Rotate right (byte)

; =============================================================================
; SECTION 11: I/O Operations and Special Registers
; =============================================================================
io_operations:
    ; Test video control
    MOV.B 0x0F, $FF01       ; 0x0E80 0x000F 0xFF01 - Enable display with all flags
    MOV.W $FF02, R0          ; 0x0300 0xFF02 - Read VSYNC status
    
    ; Test DMA setup
    MOV.W 0x1000, $FF04     ; 0x0F80 0x1000 0xFF04 - Set DMA source
    MOV.W 0x2000, $FF06     ; 0x0F80 0x2000 0xFF06 - Set DMA destination
    MOV.W 0x0100, $FF08     ; 0x0F80 0x0100 0xFF08 - Set DMA length (256 bytes)
    MOV.B 0x02, $FF0A       ; 0x0E80 0x0002 0xFF0A - Start DMA transfer
    
    ; Wait for DMA to complete
dma_wait:
    MOV.B $FF0A, R0          ; 0x0200 0xFF0A - Read DMA control
    TST.B R0, 0x01            ; 0x22F0 0x0001 - Test DMA_BUSY bit
    JCOND NE, dma_wait         ; 0xF9D0 + target - Jump if still busy
    
    ; Test bank switching
    MOV.B 0x01, $FF00       ; 0x0E80 0x0001 0xFF00 - Switch to Bank1 (video)
    MOV.W 0xFFFF, $0000     ; 0x0F80 0xFFFF 0x0000 - Write to frame buffer
    MOV.B 0x00, $FF00       ; 0x0E80 0x0000 0xFF00 - Switch back to Bank0

; =============================================================================
; SECTION 12: Complex Memory Operations
; =============================================================================
memory_test:
    ; Block copy simulation (without DMA)
    MOV.W 0x4000, R0          ; 0x0F00 0x4000 - Source address
    MOV.W 0x5000, R1          ; 0x0F08 0x5000 - Destination address
    MOV.W 0x0010, R2          ; 0x0F02 0x0010 - Counter (16 words)
    
copy_loop:
    MOV.W [R0], R3             ; 0x0340 - Load from source
    MOV.W R3, [R1]             ; 0x030B - Store to destination
    ADD.W 0x0002, R0          ; 0x4F00 0x0002 - Increment source pointer
    ADD.W 0x0002, R1          ; 0x4F08 0x0002 - Increment dest pointer
    DEC.W R2                   ; 0x0A02 - Decrement counter
    JCOND NE, copy_loop        ; 0xF9D0 + target - Continue if not zero

; =============================================================================
; SECTION 13: Edge Cases and Error Conditions
; =============================================================================
edge_cases:
    ; Test zero operations
    MOV.W 0x0000, R0          ; 0x0F00 0x0000 - Load zero
    ADD.W R0, R0               ; 0x4000 - Add zero to zero
    SUB.W R0, R0               ; 0x6000 - Subtract zero from zero
    
    ; Test maximum values
    MOV.W 0xFFFF, R1          ; 0x0F08 0xFFFF - Load maximum unsigned
    MOV.W 0x7FFF, R2          ; 0x0F02 0x7FFF - Load maximum positive signed
    MOV.W 0x8000, R3          ; 0x0F18 0x8000 - Load maximum negative signed
    
    ; Operations that should set various flags
    ADD.W R1, R1               ; 0x4009 - Should set carry flag
    ADD.W R2, R2               ; 0x400A - Should set overflow flag
    CMP.W R3, 0x0000          ; 0x21F8 0x0000 - Should set negative flag

; =============================================================================
; SECTION 14: No-Operation and Program End
; =============================================================================
end_program:
    NOP                        ; 0x0000 - No operation
    NOP                        ; 0x0000 - Another no operation
    
    ; Halt the CPU (write to CPU_CTRL register)
    MOV.B 0x01, $FF09       ; 0x0E80 0x0001 0xFF09 - Set HLT_FLAG
    
    ; This should not be reached due to halt
    JMP end_program            ; 0x0E80 + target - Infinite loop (safety)

; =============================================================================
; =============================================================================
; SECTION 15: ABSOLUTE MADNESS - Crazy Edge Cases and Stress Tests  
; =============================================================================
absolute_chaos:
    ; Test EVERY possible register combination with EVERY addressing mode
    MOV.B R0, R1               ; 0x0200 - R0->R1
    MOV.B R1, R2               ; 0x0209 - R1->R2  
    MOV.B R2, R3               ; 0x0212 - R2->R3
    MOV.B R3, R4               ; 0x021B - R3->R4
    MOV.B R4, R5               ; 0x0224 - R4->R5
    MOV.B R5, R6               ; 0x022D - R5->R6
    MOV.B R6, R7               ; 0x0236 - R6->R7
    MOV.B R7, R0               ; 0x023F - R7->R0 (complete cycle)

extreme_boundary_tests:
    ; Test maximum and minimum values
    MOV.W 0x0000, R0           ; Min value
    MOV.W 0xFFFF, R1           ; Max value
    MOV.B 0x00, R2             ; Min byte
    MOV.B 0xFF, R3             ; Max byte

stack_stress_tests:
    ; Stress test the stack
    PUSH R0
    PUSH R1  
    PUSH R2
    POP R2
    POP R1
    POP R0

; =============================================================================
; MAIN DATA SECTION - All primary test data
; =============================================================================
.data
    test_string:    .ascii "HELLO, FANTASY CPU!"  ; Test string data
    test_word:      .word 0x1234                  ; Test word data  
    test_bytes:     .byte 0x12, 0x34, 0x56, 0x78  ; Test byte array
    lookup_table:   .word 0x0000, 0x0001, 0x0004, 0x0009, 0x0010  ; Square numbers
.end
