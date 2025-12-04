/*
 * SPDX-FileCopyrightText: 2025 aesc silicon
 *
 * SPDX-License-Identifier: Apache-2.0
 */

/* Specify the memory areas */

MEMORY
{
	OCRAM (xrw): ORIGIN = 0x80000000, LENGTH = 4k
	MEM   (xrw): ORIGIN = 0x90000000, LENGTH = 64k
	FLASH (xr ): ORIGIN = 0xA0010000, LENGTH = 64k
}
stack_size = 4k;
heap_size = 0;

/* define beginning and ending of stack */
__stack_start = ORIGIN(OCRAM) + LENGTH(OCRAM);
__stack_end = __stack_start - stack_size;

ENTRY(_head)

SECTIONS {
	.text : {
		 KEEP(*(.text.head))
		*(.text .text.*)
	} > MEM

	.irs_handler : {
		*(.irs_handler)
	} > MEM

	.rodata : {
		*(.rodata)
	} > MEM

	.data : {
		*(.data)
	} > MEM

	.bss (NOLOAD) : {
		__bss_start = . ;
		*(.bss .bss.*)
		*(COMMON)
		*(.sbss .sbss.*)
		__bss_end = . ;
	} > MEM
}
