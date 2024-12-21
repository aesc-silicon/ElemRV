#include "soc.h"
#include "gpio.h"

extern void hang(void);
extern void setup_sys(void (*)(void));

void _kernel(void)
{
	struct gpio_driver gpio;

	gpio_init(&gpio, GPIO_STATUS_BASE_ADDR);	
	gpio_dir_set(&gpio, 0);
	gpio_value_clr(&gpio, 0);

	hang();
}
