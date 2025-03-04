.equ REGBYTES, 0x4

.section .text
.global hang
.global init_trap
.global interrupt_enable
.global interrupt_disable
.extern isr_handle
_head:
	li	a0, 1
	jal	gpio_set_pin

	jal	_init_bss
	li	sp, 0x90010000
	j	_kernel

hang:
	nop
	beqz	zero, hang

init_trap:
	la	t0, _irq_wrapper
	csrw	mtvec, t0
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

_irq_wrapper:
	add	sp, sp, -16*REGBYTES
	sw	a0,  1*REGBYTES(sp)
	sw	a1,  2*REGBYTES(sp)
	sw	a2,  3*REGBYTES(sp)
	sw	a3,  4*REGBYTES(sp)
	sw	a4,  5*REGBYTES(sp)
	sw	a5,  6*REGBYTES(sp)
	sw	a6,  7*REGBYTES(sp)
	sw	a7,  8*REGBYTES(sp)
	sw	ra,  9*REGBYTES(sp)
	sw	t0, 10*REGBYTES(sp)
	sw	t1, 11*REGBYTES(sp)
	sw	t2, 12*REGBYTES(sp)
	sw	t3, 13*REGBYTES(sp)
	sw	t4, 14*REGBYTES(sp)
	sw	t5, 15*REGBYTES(sp)
	sw	t6, 16*REGBYTES(sp)

	csrr	a0, mcause
	jal	isr_handle

	lw	a0,  1*REGBYTES(sp)
	lw	a1,  2*REGBYTES(sp)
	lw	a2,  3*REGBYTES(sp)
	lw	a3,  4*REGBYTES(sp)
	lw	a4,  5*REGBYTES(sp)
	lw	a5,  6*REGBYTES(sp)
	lw	a6,  7*REGBYTES(sp)
	lw	a7,  8*REGBYTES(sp)
	lw	ra,  9*REGBYTES(sp)
	lw	t0, 10*REGBYTES(sp)
	lw	t1, 11*REGBYTES(sp)
	lw	t2, 12*REGBYTES(sp)
	lw	t3, 13*REGBYTES(sp)
	lw	t4, 14*REGBYTES(sp)
	lw	t5, 15*REGBYTES(sp)
	lw	t6, 16*REGBYTES(sp)
	addi	sp, sp, 16*REGBYTES
	mret

timer_enable:
	csrr	a0, mie
	ori	a0, a0, 0x80
	csrw	mie, a0
	ret

timer_disable:
	csrr	a0, mie
	xori	a0, a0, 0x80
	csrw	mie, a0
	ret

interrupt_enable:
	csrr	a0, mie
	li	a1, 0x800
	or	a0, a0, a1
	csrw	mie, a0
	csrr	a0, mstatus
	ori	a0, a0, 0x8
	csrw	mstatus, a0
	ret

interrupt_disable:
	csrr	a0, mie
	li	a1, 0x800
	xor	a0, a0, a1
	csrw	mie, a0
	csrr	a0, mstatus
	xori	a0, a0, 0x8
	csrw	mstatus, a0
	ret

# Basic driver for debugging

gpio_set_pin:
	li	t0, 0xf0001000
	sw	a0, 0(t0)
	sw	a0, 4(t0)
	ret
