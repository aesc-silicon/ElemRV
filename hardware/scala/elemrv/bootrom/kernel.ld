/* Specify the memory areas */

MEMORY
{
	FLASH (xr ): ORIGIN = 0xA0000000, LENGTH = 64k
	MEM   (xrw): ORIGIN = 0x90000000, LENGTH = 64k
}
stack_size = 32k;
heap_size = 0;

/* define beginning and ending of stack */
__stack_start = ORIGIN(MEM) + LENGTH(MEM);
__stack_end = __stack_start - stack_size;


SECTIONS {
	.text : {
		*(.text)
	} > FLASH

	.irs_handler : {
		*(.irs_handler)
	} > FLASH

	.rodata : {
		*(.rodata)
	} > FLASH

	.bss (NOLOAD) : {
		__bss_start = . ;
		*(.bss*)
		*(COMMON)
		__bss_end = . ;
	} > MEM

	.data : {
		*(.data)
	} > MEM

	COMMON : {
		*(COMMON)
	}

	/DISCARD/ : {
		*(*)
	}
}
