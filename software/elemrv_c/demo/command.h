/*
 * SPDX-FileCopyrightText: 2026 aesc silicon
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#ifndef ELEMRV_COMMAND_H
#define ELEMRV_COMMAND_H

#include "gpio.h"
#include "tsetlin.h"

/*
 * Loads the console model into the Tsetlin machine; returns -1 on a layout mismatch.
 * "led on" and "led off" drive led_pin of gpio.
 */
int command_init(struct tsetlin_driver *tsetlin, struct gpio_driver *gpio,
		 unsigned int led_pin);

/* Line editing for one received character; Enter starts the classification */
void command_input(unsigned char chr);

/* Runs the classified command; call on the Tsetlin interrupt */
void command_result(void);

#endif
