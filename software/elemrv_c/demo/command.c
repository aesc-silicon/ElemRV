/*
 * SPDX-FileCopyrightText: 2026 aesc silicon
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#include "command.h"
#include "console.h"
#include "sysinfo.h"
#include "console_64x80.h"

#define COMMAND_LINE_MAX	32

#define FEATURES		CONSOLE_64X80_FEATURES

enum command_class {
	COMMAND_HELP,
	COMMAND_VERSION,
	COMMAND_LED_ON,
	COMMAND_LED_OFF,
	COMMAND_CLEAR,
};

static struct tsetlin_driver *tm;
static struct gpio_driver *led;
static unsigned int led_pin;
static char line[COMMAND_LINE_MAX + 1];
static unsigned int length;
static unsigned int pending;
static unsigned char last;

int command_init(struct tsetlin_driver *tsetlin, struct gpio_driver *gpio,
		 unsigned int pin)
{
	tm = tsetlin;
	led = gpio;
	led_pin = pin;

	if (tm->rows != CONSOLE_64X80_ROWS || tm->literals != CONSOLE_64X80_LITERALS ||
	    tm->classes != CONSOLE_64X80_CLASSES ||
	    tm->clauses_per_class != CONSOLE_64X80_ROWS_PER_CLASS) {
		return -1;
	}
	for (unsigned int row = 0; row < CONSOLE_64X80_ROWS; row++) {
		if (tsetlin_write_clause(tm, row, console_64x80_include[row]) != 0) {
			return -1;
		}
	}
	tsetlin_irq_enable(tm);
	gpio_dir_set(led, led_pin);

	return 0;
}

/*
 * Column 0 is the valid column, feature f sits in column 1 + f and its negation in
 * column 1 + FEATURES + f; unused columns are searched with 1.
 */
static void command_literals(const char *str, unsigned int *literals)
{
	unsigned int features[(FEATURES + 31) / 32];
	unsigned int column;

	muninn_bigrams(str, FEATURES, features);
	for (unsigned int w = 0; w < CONSOLE_64X80_WORDS; w++) {
		literals[w] = 0xffffffff;
	}
	literals[0] &= ~1u;
	for (unsigned int f = 0; f < FEATURES; f++) {
		if ((features[f / 32] >> (f % 32)) & 1) {
			column = 1 + FEATURES + f;
		} else {
			column = 1 + f;
		}
		literals[column / 32] &= ~(1u << (column % 32));
	}
}

static void command_prompt(void)
{
	length = 0;
	pending = 0;
	print(">- ");
}

static void command_submit(void)
{
	unsigned int literals[CONSOLE_64X80_WORDS];

	print("\r\n");
	if (length == 0) {
		command_prompt();
		return;
	}
	line[length] = '\0';
	command_literals(line, literals);
	if (tsetlin_search_start(tm, literals) != 0) {
		print("tsetlin busy\r\n");
		command_prompt();
		return;
	}
	pending = 1;
}

void command_input(unsigned char chr)
{
	unsigned char prev = last;

	last = chr;
	if (pending) {
		return;
	}
	if (chr == '\r' || (chr == '\n' && prev != '\r')) {
		command_submit();
	} else if (chr == 0x7f || chr == '\b') {
		if (length > 0) {
			length--;
			print("\b \b");
		}
	} else if (chr >= ' ' && chr < 0x7f && length < COMMAND_LINE_MAX) {
		line[length++] = chr;
		print_char(chr);
	}
}

static void command_help(void)
{
	print("Commands (typos are tolerated):\r\n");
	for (unsigned int cls = 0; cls < CONSOLE_64X80_CLASSES; cls++) {
		if (cls != CONSOLE_64X80_UNKNOWN) {
			print("  ");
			print(console_64x80_labels[cls]);
			print("\r\n");
		}
	}
}

void command_result(void)
{
	int sum;
	int cls = tsetlin_result(tm, &sum);

	if (!pending) {
		return;
	}
	if (cls == CONSOLE_64X80_UNKNOWN || sum < CONSOLE_64X80_MIN_SUM) {
		print("unknown command: ");
		print(line);
		print("\r\n");
		command_prompt();
		return;
	}

	print("[");
	print(console_64x80_labels[cls]);
	print(", sum ");
	print_dec(sum, 1);
	print("]\r\n");

	switch (cls) {
	case COMMAND_HELP:
		command_help();
		break;
	case COMMAND_VERSION:
		print_banner("ElemRV-C");
		print_system_info();
		break;
	case COMMAND_LED_ON:
		gpio_value_set(led, led_pin);
		break;
	case COMMAND_LED_OFF:
		gpio_value_clr(led, led_pin);
		break;
	case COMMAND_CLEAR:
		print("\033[2J\033[H");
		break;
	}
	command_prompt();
}
