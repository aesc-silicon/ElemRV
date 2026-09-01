/*
 * SPDX-FileCopyrightText: 2026 aesc silicon
 *
 * SPDX-License-Identifier: Apache-2.0
 */

/* Specify the memory areas */

MEMORY
{
	OCRAM (xrw): ORIGIN = 0x80000000, LENGTH = 8k
	FLASH (xr ): ORIGIN = 0xA0001000, LENGTH = 512k
}
stack_size = 8k;
heap_size = 0;

/* define beginning and ending of stack */
__stack_start = ORIGIN(OCRAM) + LENGTH(OCRAM);
__stack_end = __stack_start - stack_size;

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
		*(.rodata .rodata.*)
		*(.srodata .srodata.*)
	} > FLASH

	.data : {
		. = ALIGN(4);
		__data_start = . ;
		*(.data .data.*)
		*(.sdata .sdata.*)
		. = ALIGN(4);
		__data_end = . ;
	} > OCRAM AT> FLASH
	__data_load = LOADADDR(.data);

	.bss (NOLOAD) : {
		__bss_start = . ;
		*(.bss .bss.*)
		*(COMMON)
		*(.sbss .sbss.*)
		__bss_end = . ;
	} > OCRAM
}
