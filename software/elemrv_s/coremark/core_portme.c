/*
 * SPDX-FileCopyrightText: 2026 aesc silicon
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#include <stdarg.h>

#include "coremark.h"
#include "core_portme.h"

#include "soc.h"
#include "uart.h"
#include "mtimer.h"

#if VALIDATION_RUN
volatile ee_s32 seed1_volatile = 0x3415;
volatile ee_s32 seed2_volatile = 0x3415;
volatile ee_s32 seed3_volatile = 0x66;
#endif
#if PERFORMANCE_RUN
volatile ee_s32 seed1_volatile = 0x0;
volatile ee_s32 seed2_volatile = 0x0;
volatile ee_s32 seed3_volatile = 0x66;
#endif
#if PROFILE_RUN
volatile ee_s32 seed1_volatile = 0x8;
volatile ee_s32 seed2_volatile = 0x8;
volatile ee_s32 seed3_volatile = 0x8;
#endif
volatile ee_s32 seed4_volatile = ITERATIONS;
volatile ee_s32 seed5_volatile = 0;

ee_u32 default_num_contexts = 1;

static struct uart_driver uart;
static struct mtimer_driver mtimer;

#define EE_TICKS_PER_SEC MTIMERCTRL_FREQ

/* Zicntr/Zihpm are present on this platform, so mcycle and minstret give the
 * cycle count and IPC alongside the wall-clock measurement.
 */
#define READ_CSR64(name)                                                       \
	({                                                                     \
		unsigned int hi, lo, hi2;                                      \
		do {                                                           \
			__asm__ volatile("csrr %0, " #name "h" : "=r"(hi));     \
			__asm__ volatile("csrr %0, " #name : "=r"(lo));        \
			__asm__ volatile("csrr %0, " #name "h" : "=r"(hi2));    \
		} while (hi != hi2);                                           \
		((unsigned long long)hi << 32) | lo;                           \
	})

/* The machine timer is a 64-bit up-counter split into two 32-bit registers.
 * Re-read the high word to discard a sample taken across a low-word carry.
 */
static unsigned long long mtimer_read(void)
{
	unsigned int high, low;

	do {
		high = mtimer.regs->cnt_high;
		low = mtimer.regs->cnt_low;
	} while (high != mtimer.regs->cnt_high);

	return ((unsigned long long)high << 32) | low;
}

static unsigned long long start_time_val, stop_time_val;
static unsigned long long start_cycles, stop_cycles;
static unsigned long long start_instret, stop_instret;

void start_time(void)
{
	start_instret = READ_CSR64(minstret);
	start_cycles = READ_CSR64(mcycle);
	start_time_val = mtimer_read();
}

void stop_time(void)
{
	stop_time_val = mtimer_read();
	stop_cycles = READ_CSR64(mcycle);
	stop_instret = READ_CSR64(minstret);
}

CORE_TICKS get_time(void)
{
	return (CORE_TICKS)(stop_time_val - start_time_val);
}

secs_ret time_in_secs(CORE_TICKS ticks)
{
	return (secs_ret)(ticks / EE_TICKS_PER_SEC);
}

/* Minimal formatted output over UART0. CoreMark needs %d, %u, %x, %s and the
 * 'l' length modifier; %f is never reached because HAS_FLOAT is 0.
 */
static void ee_putc(char c)
{
	if (c == '\n')
		uart_putc(&uart, '\r');
	uart_putc(&uart, (unsigned char)c);
}

static void ee_puts(const char *s)
{
	while (*s)
		ee_putc(*s++);
}

static void ee_put_unsigned(unsigned long value, unsigned int base,
			    unsigned int width, char pad)
{
	char buffer[32];
	unsigned int length = 0;

	do {
		unsigned int digit = (unsigned int)(value % base);

		buffer[length++] = (char)(digit < 10 ? '0' + digit
						     : 'a' + digit - 10);
		value /= base;
	} while (value);

	while (length < width)
		buffer[length++] = pad;

	while (length)
		ee_putc(buffer[--length]);
}

int ee_printf(const char *fmt, ...)
{
	va_list args;

	va_start(args, fmt);

	while (*fmt) {
		unsigned int width = 0;
		int is_long = 0;
		char pad = ' ';

		if (*fmt != '%') {
			ee_putc(*fmt++);
			continue;
		}
		fmt++;

		if (*fmt == '0') {
			pad = '0';
			fmt++;
		}
		while (*fmt >= '0' && *fmt <= '9')
			width = width * 10 + (unsigned int)(*fmt++ - '0');
		while (*fmt == 'l') {
			is_long = 1;
			fmt++;
		}

		switch (*fmt) {
		case 'd': {
			long value = is_long ? va_arg(args, long)
					     : va_arg(args, int);

			if (value < 0) {
				ee_putc('-');
				value = -value;
			}
			ee_put_unsigned((unsigned long)value, 10, width, pad);
			break;
		}
		case 'u':
			ee_put_unsigned(is_long ? va_arg(args, unsigned long)
						: va_arg(args, unsigned int),
					10, width, pad);
			break;
		case 'x':
			ee_put_unsigned(is_long ? va_arg(args, unsigned long)
						: va_arg(args, unsigned int),
					16, width, pad);
			break;
		case 'c':
			ee_putc((char)va_arg(args, int));
			break;
		case 's':
			ee_puts(va_arg(args, const char *));
			break;
		case '%':
			ee_putc('%');
			break;
		default:
			ee_putc('%');
			ee_putc(*fmt);
			break;
		}
		fmt++;
	}

	va_end(args);

	return 0;
}

void portable_init(core_portable *p, int *argc, char *argv[])
{
	(void)argc;
	(void)argv;

	mtimer_init(&mtimer, MTIMERCTRL_BASE);
	uart_init(&uart, UART0CTRL_BASE,
		  UART_CALC_FREQUENCY(UART0CTRL_FREQ, UART0CTRL_BAUD, 8));

	if (sizeof(ee_ptr_int) != sizeof(ee_u8 *))
		ee_printf("ERROR! ee_ptr_int does not hold a pointer!\n");
	if (sizeof(ee_u32) != 4)
		ee_printf("ERROR! ee_u32 is not 32 bits!\n");

	p->portable_id = 1;
}

void portable_fini(core_portable *p)
{
	p->portable_id = 0;

	/* time_in_secs() truncates to whole seconds because this port has no
	 * floating point. Print the raw inputs so the host can recompute
	 * iterations/sec exactly.
	 */
	ee_printf("Timer ticks      : %lu\n",
		  (long unsigned)(stop_time_val - start_time_val));
	ee_printf("Timer frequency  : %lu\n", (long unsigned)EE_TICKS_PER_SEC);
	ee_printf("CPU cycles       : %lu\n",
		  (long unsigned)(stop_cycles - start_cycles));
	ee_printf("Instructions     : %lu\n",
		  (long unsigned)(stop_instret - start_instret));
}
