/*
 * SPDX-FileCopyrightText: 2026 aesc silicon
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#ifndef ELEMRV_CONSOLE_H
#define ELEMRV_CONSOLE_H

#include "uart.h"

/* Selects the UART all print functions write to */
void console_init(struct uart_driver *uart);

void print(const char *str);
void print_char(char chr);

/* Decimal, zero-padded to at least `width` digits */
void print_dec(unsigned int value, unsigned int width);

/* Hexadecimal with 0x prefix, eight digits */
void print_hex(unsigned int value);

/* UNIX timestamp as YYYY-MM-DD hh:mm:ss UTC */
void print_date(unsigned int timestamp);

#endif
