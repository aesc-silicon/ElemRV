// SPDX-FileCopyrightText: 2025 aesc silicon
//
// SPDX-License-Identifier: CERN-OHL-W-2.0

// Pure Verilator testbench for WishboneGpio (N-variant, 20 pins).
// Same register map as H GPIO but with 20 pins instead of 12.

#include "verilated.h"
#include "VWishboneGpio.h"
#include "wb_helpers.h"

// Tie-off: GPIO pins read input = 0 (no external connections)
static void gpio_n_tieoff(VWishboneGpio* top) {
    top->io_gpio_pins_read = 0;
}

int main(int argc, char** argv) {
    Verilated::commandArgs(argc, argv);

    auto* top = new VWishboneGpio();

    printf("=== GPIO (N, 20-pin) Pure Verilator Testbench ===\n\n");

    wb_reset(top, gpio_n_tieoff);
    wb_clock(top, 10, gpio_n_tieoff);

    // IpIdentification registers
    verify_nonzero("GPIO_N", "IP_HEADER", 0x000, wb_read(top, 0x000, gpio_n_tieoff));
    verify_nonzero("GPIO_N", "IP_VERSION", 0x004, wb_read(top, 0x004, gpio_n_tieoff));
    verify_nonzero("GPIO_N", "IP_FEATURES", 0x008, wb_read(top, 0x008, gpio_n_tieoff));

    // Write output data = 0x0F (pins 0-3 high), read back
    wb_write(top, 0x010, 0x0F, gpio_n_tieoff);
    verify("GPIO_N", "OUTPUT", 0x010, wb_read(top, 0x010, gpio_n_tieoff), 0x0000000F);

    // Write output data = 0x55555 (20-bit pattern), read back
    wb_write(top, 0x010, 0x55555, gpio_n_tieoff);
    verify("GPIO_N", "OUTPUT_20BIT", 0x010, wb_read(top, 0x010, gpio_n_tieoff), 0x00055555);

    top->final();
    delete top;

    return summary("GPIO_N");
}
