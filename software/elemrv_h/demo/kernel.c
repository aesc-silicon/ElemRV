/*
 * SPDX-FileCopyrightText: 2025 aesc silicon
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#include "soc.h"
#include "gpio.h"
#include "uart.h"
#include "mtimer.h"
#include "plic.h"

extern void hang(void);
extern void init_trap(void);
extern void interrupt_enable(void);
extern void interrupt_disable(void);

static struct uart_driver uart;
static struct gpio_driver gpio;
static struct plic_driver plic;

void isr_handle(unsigned int mcause)
{
	unsigned char chr;

	interrupt_disable();

	if (uart_irq_rx_ready(&uart)) {
		uart_irq_rx_disable(&uart);
		plic_irq_claim(&plic, UART0CTRL_IRQ);

		while (uart_getc(&uart, &chr) == 0) {
			uart_putc(&uart, chr);
		}

		uart_irq_rx_enable(&uart);
	}

	interrupt_enable();
}

void _kernel(void)
{
	struct mtimer_driver mtimer;
	unsigned char banner[15 + 1] = "\r\nElemRV-H\r\n>- ";

	gpio_init(&gpio, GPIO0CTRL_BASE);
	mtimer_init(&mtimer, MTIMERCTRL_BASE);
	plic_init(&plic, PLICCTRL_BASE);
	uart_init(&uart, UART0CTRL_BASE,
		  UART_CALC_FREQUENCY(UART0CTRL_FREQ, UART0CTRL_BAUD, 8));

	init_trap();
	interrupt_enable();
	plic_irq_enable(&plic, UART0CTRL_IRQ);
	plic_irq_enable(&plic, GPIO0CTRL_IRQ);

	gpio_dir_set(&gpio, 0);

	uart_puts(&uart, banner);
	uart_irq_rx_enable(&uart);

	while(1) {
		// ON - 150ms - OFF - 50ms - ON - 150ms - OFF - 1000ms
		gpio_value_set(&gpio, 0);
		mtimer_sleep32(&mtimer, TIMER_MS(MTIMERCTRL_FREQ, 150));
		gpio_value_clr(&gpio, 0);
		mtimer_sleep32(&mtimer, TIMER_MS(MTIMERCTRL_FREQ, 50));
		gpio_value_set(&gpio, 0);
		mtimer_sleep32(&mtimer, TIMER_MS(MTIMERCTRL_FREQ, 150));
		gpio_value_clr(&gpio, 0);
		mtimer_sleep32(&mtimer, TIMER_MS(MTIMERCTRL_FREQ, 1000));
		gpio_value_set(&gpio, 0);
	}

	hang();
}
