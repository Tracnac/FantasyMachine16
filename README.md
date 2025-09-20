# Fantasy Machine 16-bit CPU

## Description
Fantasy Machine is a 16-bit CPU architecture designed for educational and experimental purposes. It features a simple instruction set, memory banking, and a monochrome video output.

## Key Features
- **16-bit CPU** with 8 general-purpose registers (R0-R7).
- **128 KiB memory** split into two banks of 64 KiB each.
- **Monochrome display**: 640×480 resolution (1 bit per pixel).
- **Big Endian encoding**: Most significant byte stored first.
- **DMA support** for fast memory transfers between banks.
- **Interrupt handling** with a simple vector table.

## Memory Map Overview
- **Bank 0**: Code, data, and stack.
- **Bank 1**: Video frame buffer and additional video data.
- **Fixed I/O registers**: Always mapped to Bank 0.

## Instruction Set Highlights
- **32 instructions** with a 16-bit encoding format.
- **Addressing modes**: Direct, indirect, absolute, and immediate.
- **Support for arithmetic, logic, and control flow operations.**

For detailed technical documentation, refer to `Instructions.txt`.