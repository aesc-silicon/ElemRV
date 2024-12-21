/* Specify the memory areas */

MEMORY
{
	BOOTROM (xr ): ORIGIN = 0xA0000000, LENGTH = 8M
	RAM     (xrw): ORIGIN = 0x90000000, LENGTH = 128K
}

/* define beginning and ending of stack */


SECTIONS {
	__STACK_START = ORIGIN(RAM) + LENGTH(RAM);
	__STACK_END = __STACK_START - LENGTH(RAM);

	.init : {
		*(.init)
	} > BOOTROM

	.text : {
		*(.text)
	} > BOOTROM

	.irs_handler : {
		*(.irs_handler)
	} > BOOTROM

	.rodata : {
		*(.rodata)
	} > BOOTROM

	.data : {
		*(.data)
	} > RAM

	COMMON : {
		*(COMMON)
	}

	/DISCARD/ : {
		*(*)
	}
}
