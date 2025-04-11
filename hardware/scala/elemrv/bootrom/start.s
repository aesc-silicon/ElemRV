.equ REGBYTES, 0x4

.section .text
_head:
	li	a0, 1
	jal	gpio_set_pin

	jal	_init_regs
	#jal	_init_memc
	jal	_init_bss

	li	a0, 0
	jal	gpio_set_pin

	jal	_relocate
	# Jump to application
	li	ra, 0x90000000
	ret

_init_regs:
	li	x2 , 0xA2A2A2A2
	li	x3 , 0xA3A3A3A3
	li	x4 , 0xA4A4A4A4
	li	x5 , 0xA5A5A5A5
	li	x6 , 0xA6A6A6A6
	li	x7 , 0xA7A7A7A7
	li	x8 , 0xA8A8A8A8
	li	x9 , 0xA9A9A9A9
	li	x10, 0xB0B0B0B0
	li	x11, 0xB1B1B1B1
	li	x12, 0xB2B2B2B2
	li	x13, 0xB3B3B3B3
	li	x14, 0xB4B4B4B4
	li	x15, 0xB5B5B5B5
	li	x16, 0xB6B6B6B6
	li	x17, 0xB7B7B7B7
	li	x18, 0xB8B8B8B8
	li	x19, 0xB9B9B9B9
	li	x20, 0xC0C0C0C0
	li	x21, 0xC1C1C1C1
	li	x22, 0xC2C2C2C2
	li	x23, 0xC3C3C3C3
	li	x24, 0xC4C4C4C4
	li	x25, 0xC5C5C5C5
	li	x26, 0xC6C6C6C6
	li	x27, 0xC7C7C7C7
	li	x28, 0xC8C8C8C8
	li	x29, 0xC9C9C9C9
	li	x30, 0xD0D0D0D0
	li	x31, 0xD1D1D1D1
	ret

_init_memc:
	li	t0, 0xf0023000
	li	t1, 0x20
	sw	t1, 0x14(t0) # reset pulse
	li	t1, 0x40
	sw	t1, 0x18(t0) # reset hold
	li	t1, 7
	sw	t1, 0x20(t0) # latency cycles
	li	t1, 1
	sw	t1, 0x10(t0) # reset chip
	ret

_init_bss:
	la	t0, __bss_start
	la	t1, __bss_end
	beq	t0, t1, loop_end
loop_head:
	sw	zero, 0(t0)
	beq	t0, t1, loop_end
	addi	t0, t0, 4
	j	loop_head
loop_end:
	ret
	nop

_relocate:
	li	t0, 0xa0010000
	li	t1, 0xa0012000
	li	t2, 0x90000000
	beq	t0, t1, relocate_loop_end
relocate_loop_head:
	lw	a0, 0x0(t0)
	sw	a0, 0x0(t2)
	addi	t0, t0, 4
	addi	t2, t2, 4
	bne	t0, t1, relocate_loop_head
relocate_loop_end:
	ret
	nop

# Basic driver for debugging

gpio_set_pin:
	li	t0, 0xf0000000
	sw	a0, 0x10(t0)
	sw	a0, 0x14(t0)
	ret
