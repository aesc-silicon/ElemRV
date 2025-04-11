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

	if (gpio_irq_ready(&gpio, 3, GPIO_IRQ_FALLING_EDGE)) {
		gpio_irq_disable(&gpio, 3, GPIO_IRQ_FALLING_EDGE);

		uart_putc(&uart, 'I');
		uart_putc(&uart, 'R');
		uart_putc(&uart, 'Q');
		uart_putc(&uart, ' ');
		uart_putc(&uart, 'G');
		uart_putc(&uart, 'P');
		uart_putc(&uart, 'I');
		uart_putc(&uart, 'O');
		uart_putc(&uart, ':');
		uart_putc(&uart, ' ');
		uart_putc(&uart, '3');
		uart_putc(&uart, '\r');
		uart_putc(&uart, '\n');

		gpio_irq_enable(&gpio, 3, GPIO_IRQ_FALLING_EDGE);
		plic_irq_claim(&plic, GPIO0CTRL_IRQ);
	}

	interrupt_enable();
}

void _kernel(void)
{
	struct mtimer_driver mtimer;
	unsigned char banner[19 + 1] = "\r\nElemRV-N 0.2\r\n>- ";

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
	//gpio_irq_enable(&gpio, 3, GPIO_IRQ_FALLING_EDGE);

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
