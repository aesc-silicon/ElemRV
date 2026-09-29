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
#include "dma.h"

extern void hang(void);
extern void init_trap(void);
extern void interrupt_enable(void);
extern void interrupt_disable(void);

static struct uart_driver uart;
static struct gpio_driver gpio;
static struct plic_driver plic;
static struct dma_driver dma;

/*
 * The banner lives in .rodata, which the bootrom copies to HyperRAM before the CPU
 * caches any of it, so the DMA reads the same bytes as the CPU. A buffer the CPU
 * writes must first be cleaned from the data cache (Zicbom cbo.clean).
 */
static const unsigned char banner[] = "\r\nElemRV-O\r\n>- ";

#define GPIO_IRQ_NO	3

void isr_handle(unsigned int mcause)
{
	unsigned char chr;
	unsigned int source;

	/* Claim until the PLIC has no more pending source (the gateway's pending
	 * bit clears on the claim read, so every source must be claimed). Dispatch
	 * on the claimed id; do not touch MIE - mret restores it via MPIE. */
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

/* Sends the banner to UART0 with DMA channel 0, paced by the UART0 TX request line. */
static void print_banner(void)
{
	dma_init(&dma, DMACTRL_BASE);
	if (dma.channels == 0) {
		uart_puts(&uart, (unsigned char *)banner);
		return;
	}

	dma_configure(&dma, 0,
		      DMA_CFG_SRC_INC | DMA_CFG_WIDTH_8 | DMA_CFG_REQ_ENABLE |
		      DMA_CFG_REQ_SEL(UART0CTRL_DMA_TX),
		      (uint32_t)(unsigned long)banner,
		      (uint32_t)(unsigned long)&uart.regs->read_write,
		      sizeof(banner) - 1);
	dma_start(&dma, 0);
	if (dma_wait(&dma, 0) != 0) {
		uart_puts(&uart, (unsigned char *)banner);
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

	init_trap();
	interrupt_enable();
	plic_irq_enable(&plic, UART0CTRL_IRQ);
	plic_irq_enable(&plic, GPIO0CTRL_IRQ);

	gpio_dir_set(&gpio, 0);

	print_banner();
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
