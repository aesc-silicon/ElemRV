/*
 * SPDX-FileCopyrightText: 2026 aesc silicon
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#include "soc.h"
#include "uart.h"

extern void hang(void);
extern int main(void);

/* start.s jumps here once bss is cleared and the stack is set up. Interrupts
 * stay disabled for the whole run so nothing perturbs the measurement.
 */
void _kernel(void)
{
	struct uart_driver uart;

	/* Banner before the benchmark starts: CoreMark itself prints nothing
	 * until the run completes, which takes a while.
	 */
	uart_init(&uart, UART0CTRL_BASE,
		  UART_CALC_FREQUENCY(UART0CTRL_FREQ, UART0CTRL_BAUD, 8));
	uart_puts(&uart, (unsigned char *)"\r\nCoreMark starting...\r\n");

	main();

	hang();
}

/* Referenced by _irq_wrapper in start.s. Unreachable while interrupts are
 * disabled, but the symbol is needed because the link uses --no-undefined.
 */
void isr_handle(unsigned int mcause)
{
	(void)mcause;
}
