/*
 * SPDX-FileCopyrightText: 2026 aesc silicon
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#include "soc.h"
#include "gpio.h"
#include "uart.h"
#include "mtimer.h"
#include "plic.h"
#include "tsetlin.h"
#include "console.h"
#include "sysinfo.h"
#include "command.h"

extern void hang(void);
extern void init_trap(void);
extern void interrupt_enable(void);
extern void interrupt_disable(void);

static struct uart_driver uart;
static struct gpio_driver gpio;
static struct plic_driver plic;
static struct tsetlin_driver tsetlin;

#define GPIO_IRQ_NO	3
#define LED_PIN		1

void isr_handle(unsigned int mcause)
{
	unsigned char chr;
	unsigned int source;

	/* Claim until no source is pending (pending clears on the claim read);
	 * dispatch on the claimed id. mret restores MIE, so don't touch it. */
	while ((source = plic_irq_claim(&plic)) != 0) {
		if (source == UART0CTRL_IRQ) {
			while (uart_getc(&uart, &chr) == 0) {
				command_input(chr);
			}
			uart_irq_rx_clear(&uart);
		}
		if (source == GPIO0CTRL_IRQ) {
			uart_puts(&uart, (unsigned char *)"IRQ GPIO: ");
			uart_putc(&uart, '0' + GPIO_IRQ_NO);
			uart_puts(&uart, (unsigned char *)"\r\n");

			gpio_irq_clear(&gpio, GPIO_IRQ_NO, GPIO_IRQ_FALLING_EDGE);
		}
		if (source == TSETLINCTRL_IRQ) {
			command_result();
		}

		plic_irq_complete(&plic, source);
	}
}

void _kernel(void)
{
	struct mtimer_driver mtimer;

	gpio_init(&gpio, GPIO0CTRL_BASE);
	mtimer_init(&mtimer, MTIMERCTRL_BASE);
	plic_init(&plic, PLICCTRL_BASE);
	uart_init(&uart, UART0CTRL_BASE,
		  UART_CALC_FREQUENCY(UART0CTRL_FREQ, UART0CTRL_BAUD, 8));
	tsetlin_init(&tsetlin, TSETLINCTRL_BASE);

	init_trap();
	interrupt_enable();
	plic_irq_enable(&plic, UART0CTRL_IRQ);
	plic_irq_enable(&plic, GPIO0CTRL_IRQ);
	plic_irq_enable(&plic, TSETLINCTRL_IRQ);

	gpio_dir_set(&gpio, 0);

	console_init(&uart);
	print_banner("ElemRV-C");
	print_system_info();
	if (command_init(&tsetlin, &gpio, LED_PIN) != 0) {
		print("Tsetlin layout does not match the console model\r\n");
	}
	print(">- ");
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
