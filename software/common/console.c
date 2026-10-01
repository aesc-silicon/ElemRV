/*
 * SPDX-FileCopyrightText: 2026 aesc silicon
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#include "console.h"

static struct uart_driver *console_uart;

void console_init(struct uart_driver *uart)
{
	console_uart = uart;
}

void print(const char *str)
{
	uart_puts(console_uart, (unsigned char *)str);
}

void print_char(char chr)
{
	uart_putc(console_uart, chr);
}

void print_dec(unsigned int value, unsigned int width)
{
	char digits[10];
	unsigned int count = 0;

	do {
		digits[count++] = '0' + value % 10;
		value /= 10;
	} while (value != 0);
	while (count < width && count < sizeof(digits)) {
		digits[count++] = '0';
	}
	while (count > 0) {
		print_char(digits[--count]);
	}
}

void print_hex(unsigned int value)
{
	print("0x");
	for (int shift = 28; shift >= 0; shift -= 4) {
		print_char("0123456789abcdef"[(value >> shift) & 0xF]);
	}
}

/* Days since 1970-01-01 to a proleptic Gregorian date, after H. Hinnant's civil_from_days */
void print_date(unsigned int timestamp)
{
	unsigned int days = timestamp / 86400;
	unsigned int secs = timestamp % 86400;
	unsigned int z = days + 719468;
	unsigned int era = z / 146097;
	unsigned int doe = z - era * 146097;
	unsigned int yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
	unsigned int doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
	unsigned int mp = (5 * doy + 2) / 153;
	unsigned int day = doy - (153 * mp + 2) / 5 + 1;
	unsigned int month = mp < 10 ? mp + 3 : mp - 9;
	unsigned int year = yoe + era * 400 + (month <= 2);

	print_dec(year, 4);
	print_char('-');
	print_dec(month, 2);
	print_char('-');
	print_dec(day, 2);
	print_char(' ');
	print_dec(secs / 3600, 2);
	print_char(':');
	print_dec(secs / 60 % 60, 2);
	print_char(':');
	print_dec(secs % 60, 2);
	print(" UTC");
}
