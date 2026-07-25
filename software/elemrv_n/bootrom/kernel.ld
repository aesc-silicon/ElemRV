/*
 * SPDX-FileCopyrightText: 2025 aesc silicon
 *
 * SPDX-License-Identifier: Apache-2.0
 */

/* Specify the memory areas */

MEMORY
{
	OCRAM (xrw): ORIGIN = 0x80000000, LENGTH = 8k
	MEM   (xrw): ORIGIN = 0x90000000, LENGTH = 64k
	FLASH (xr ): ORIGIN = 0xA0000000, LENGTH = 4k
}
stack_size = 8k;
heap_size = 0;

/* define beginning and ending of stack */
__stack_start = ORIGIN(OCRAM) + LENGTH(OCRAM);
__stack_end = __stack_start - stack_size;

__hyperram_uncached_base = 0xb0000000;
__hyperram_start         = ORIGIN(MEM);
__hyperram_load_start    = ORIGIN(FLASH) + LENGTH(FLASH);
__hyperram_load_size     = 16k;
__hyperram_load_end      = __hyperram_load_start + __hyperram_load_size;
__hyperram_load_dest     = __hyperram_uncached_base;

ENTRY(_head)

SECTIONS {
	.text : {
		 KEEP(*(.text.head))
		*(.text .text.*)
	} > FLASH

	.irs_handler : {
		*(.irs_handler)
	} > FLASH

	.rodata : {
		*(.rodata)
	} > FLASH

	.data : {
		*(.data)
	} > OCRAM

	.bss (NOLOAD) : {
		__bss_start = . ;
		*(.bss .bss.*)
		*(COMMON)
		*(.sbss .sbss.*)
		__bss_end = . ;
	} > OCRAM
}
