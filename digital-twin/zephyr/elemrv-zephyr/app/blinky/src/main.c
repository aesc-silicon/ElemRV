/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * ElemRV Blinky: Validates GPIO output + Timer-based k_sleep.
 * Toggles LED0 (GPIO0 pin 0) with 250 ms interval using k_sleep.
 */

#include <zephyr/kernel.h>
#include <zephyr/drivers/gpio.h>

#define SLEEP_TIME_MS 250
#define NUM_TOGGLES   8

#define LED0_NODE DT_ALIAS(led0)

static const struct gpio_dt_spec led = GPIO_DT_SPEC_GET(LED0_NODE, gpios);

int main(void)
{
	int ret;

	printk("ElemRV Blinky (GPIO + Timer test)\n");

	if (!gpio_is_ready_dt(&led)) {
		printk("ERROR: GPIO device not ready\n");
		return -1;
	}

	ret = gpio_pin_configure_dt(&led, GPIO_OUTPUT_ACTIVE);
	if (ret < 0) {
		printk("ERROR: gpio_pin_configure failed: %d\n", ret);
		return -1;
	}

	printk("GPIO configured, starting %d toggles at %d ms interval\n",
	       NUM_TOGGLES, SLEEP_TIME_MS);

	for (int i = 0; i < NUM_TOGGLES; i++) {
		gpio_pin_toggle_dt(&led);
		printk("Toggle %d\n", i);
		k_sleep(K_MSEC(SLEEP_TIME_MS));
	}

	printk("GPIO + Timer Test PASSED\n");
	return 0;
}
