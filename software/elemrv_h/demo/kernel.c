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

#define GPIO_IRQ_NO	3

void isr_handle(unsigned int mcause)
{
	unsigned char chr;
	unsigned int source;

	/* Claim until no source is pending (pending clears on the claim read);
	 * dispatch on the claimed id. mret restores MIE, so don't touch it. */
	while ((source = plic_irq_claim(&plic)) != 0) {
		if (source == UART0CTRL_IRQ) {
			while (uart_getc(&uart, &chr) == 0) {
				uart_putc(&uart, chr);
			}
			uart_irq_rx_clear(&uart);
		}
		if (source == GPIO0CTRL_IRQ) {
			uart_puts(&uart, (unsigned char *)"IRQ GPIO: ");
			uart_putc(&uart, '0' + GPIO_IRQ_NO);
			uart_puts(&uart, (unsigned char *)"\r\n");

			gpio_irq_clear(&gpio, GPIO_IRQ_NO, GPIO_IRQ_FALLING_EDGE);
		}

		plic_irq_complete(&plic, source);
	}
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
	gpio_irq_enable(&gpio, GPIO_IRQ_NO, GPIO_IRQ_FALLING_EDGE);

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
