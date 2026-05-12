// SPDX-FileCopyrightText: 2025 aesc silicon
//
// SPDX-License-Identifier: CERN-OHL-W-2.0

// Pure Verilator testbench for WishboneUart (lightweight).
// Validates IpIdentification and clock divider register.
// Lightweight: TX/RX only (no CTS/RTS).

#include "verilated.h"
#include "VWishboneUart.h"
#include "wb_helpers.h"

// Tie-off: UART RXD high (idle mark)
static void uart_lite_tieoff(VWishboneUart* top) {
    top->io_uart_rxd = 1;
}

int main(int argc, char** argv) {
    Verilated::commandArgs(argc, argv);

    auto* top = new VWishboneUart();

    printf("=== UART Lite Pure Verilator Testbench ===\n\n");

    wb_reset(top, uart_lite_tieoff);
    wb_clock(top, 10, uart_lite_tieoff);

    // IpIdentification registers
    verify_nonzero("UART_LITE", "IP_HEADER", 0x000, wb_read(top, 0x000, uart_lite_tieoff));
    verify_nonzero("UART_LITE", "IP_VERSION", 0x004, wb_read(top, 0x004, uart_lite_tieoff));
    verify_nonzero("UART_LITE", "IP_FEATURES", 0x008, wb_read(top, 0x008, uart_lite_tieoff));

    // Write clock divider (at 0x020 for UART) and read back
    wb_write(top, 0x020, 0x14, uart_lite_tieoff);
    verify("UART_LITE", "CLK_DIV", 0x020, wb_read(top, 0x020, uart_lite_tieoff), 0x00000014);

    top->final();
    delete top;

    return summary("UART_LITE");
}
