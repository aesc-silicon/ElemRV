/*
 * SPDX-FileCopyrightText: 2025 aesc silicon
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#include "soc.h"
#include "gpio.h"
#include "uart.h"
#include "mtimer.h"

extern void hang(void);


void _kernel(void)
{
	struct gpio_driver gpio;
	struct mtimer_driver mtimer;
	struct uart_driver uart;
	unsigned char banner[15 + 1] = "\r\nElemRV-H\r\n>- ";

	gpio_init(&gpio, GPIO0CTRL_BASE);
	mtimer_init(&mtimer, MTIMERCTRL_BASE);
	uart_init(&uart, UART0CTRL_BASE,
		  UART_CALC_FREQUENCY(UART0CTRL_FREQ, UART0CTRL_BAUD, 8));

	gpio_dir_set(&gpio, 0);
	uart_puts(&uart, banner);

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
