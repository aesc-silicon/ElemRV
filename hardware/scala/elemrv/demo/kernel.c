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

		while (uart_getc(&uart, &chr) == 0) {
			uart_putc(&uart, chr);
		}

		uart_irq_rx_enable(&uart);
		plic_irq_claim(&plic, PLIC_UART_IRQ);
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
		plic_irq_claim(&plic, PLIC_GPIO_IRQ);
	}

	interrupt_enable();
}

void _kernel(void)
{
	struct mtimer_driver mtimer;
	unsigned char banner[17 + 1] = "\r\nElemRV 1.0\r\n>- ";

	gpio_init(&gpio, GPIO_BASE_ADDR);
	uart_init(&uart, UART_BASE_ADDR,
		  UART_CALC_FREQUENCY(UART_FREQ, UART_BAUD, 8));
	mtimer_init(&mtimer, MTIMER_BASE_ADDR);
	plic_init(&plic, PLIC_BASE_ADDR);

	init_trap();
	interrupt_enable();
	plic_irq_enable(&plic, PLIC_UART_IRQ);
	plic_irq_enable(&plic, PLIC_GPIO_IRQ);

	gpio_dir_set(&gpio, 0);
	uart_puts(&uart, banner);
	uart_irq_rx_enable(&uart);
	gpio_irq_enable(&gpio, 3, GPIO_IRQ_FALLING_EDGE);

	while(1) {
		// ON - 150ms - OFF - 50ms - ON - 150ms - OFF - 1000ms
		gpio_value_set(&gpio, 0);
		mtimer_sleep32(&mtimer, TIMER_MS(MTIMER_FREQ, 150));
		gpio_value_clr(&gpio, 0);
		mtimer_sleep32(&mtimer, TIMER_MS(MTIMER_FREQ, 50));
		gpio_value_set(&gpio, 0);
		mtimer_sleep32(&mtimer, TIMER_MS(MTIMER_FREQ, 150));
		gpio_value_clr(&gpio, 0);
		mtimer_sleep32(&mtimer, TIMER_MS(MTIMER_FREQ, 1000));
		gpio_value_set(&gpio, 0);
	}

	hang();
}
