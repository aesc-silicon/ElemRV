// SPDX-FileCopyrightText: 2025 aesc silicon
//
// SPDX-License-Identifier: CERN-OHL-W-2.0

// Wishbone bus helper functions for pure Verilator testbenches.
// All 7 ElemRV peripherals share identical Wishbone ports:
//   io_bus_CYC, io_bus_STB, io_bus_ACK, io_bus_WE
//   io_bus_ADR[9:0]       word address
//   io_bus_DAT_MOSI[31:0] write data
//   io_bus_DAT_MISO[31:0] read data
//   clk, resetn
//
// These templates drive the bus directly (no Renode, no IntegrationLibrary).

#ifndef WB_HELPERS_H
#define WB_HELPERS_H

#include <cstdio>
#include <cstdint>
#include <cstdlib>

// ---- Clock helper ----
// Toggle one clock cycle. Calls optional tieoff() on each eval.
template <typename T>
void wb_clock_once(T* top, void (*tieoff)(T*) = nullptr) {
    if (tieoff) tieoff(top);
    top->clk = 1;
    top->eval();
    if (tieoff) tieoff(top);
    top->clk = 0;
    top->eval();
}

// Clock N cycles.
template <typename T>
void wb_clock(T* top, int n, void (*tieoff)(T*) = nullptr) {
    for (int i = 0; i < n; i++) {
        wb_clock_once(top, tieoff);
    }
}

// ---- Reset ----
// Active-low reset: hold resetn=0 for 10 cycles, then release.
template <typename T>
void wb_reset(T* top, void (*tieoff)(T*) = nullptr) {
    top->resetn = 0;
    top->clk = 0;
    top->io_bus_CYC = 0;
    top->io_bus_STB = 0;
    top->io_bus_WE = 0;
    top->io_bus_ADR = 0;
    top->io_bus_DAT_MOSI = 0;
    if (tieoff) tieoff(top);
    top->eval();

    wb_clock(top, 10, tieoff);

    top->resetn = 1;
    wb_clock_once(top, tieoff);
}

// ---- Wishbone Read ----
// Single Wishbone read transaction.
// Read protocol:
//   Cycle 1: assert CYC=1, STB=1, WE=0, ADR=word_addr → posedge → ACK=0
//   Cycle 2: posedge → ACK=1, capture DAT_MISO → deassert
template <typename T>
uint32_t wb_read(T* top, uint32_t byte_addr, void (*tieoff)(T*) = nullptr) {
    uint32_t word_addr = (byte_addr >> 2) & 0x3FF;

    // Assert bus signals
    top->io_bus_CYC = 1;
    top->io_bus_STB = 1;
    top->io_bus_WE = 0;
    top->io_bus_ADR = word_addr;
    top->io_bus_DAT_MOSI = 0;

    // Cycle 1: ACK not yet asserted
    wb_clock_once(top, tieoff);

    // Cycle 2: ACK rises, capture read data
    wb_clock_once(top, tieoff);
    uint32_t data = top->io_bus_DAT_MISO;

    // Deassert bus
    top->io_bus_CYC = 0;
    top->io_bus_STB = 0;
    wb_clock_once(top, tieoff);

    return data;
}

// ---- Wishbone Write ----
// Single Wishbone write transaction.
// Write protocol (3 cycles due to registered ACK + _zz_1 write condition):
//   Cycle 1: assert CYC=1, STB=1, WE=1, ADR, DAT_MOSI → posedge → ACK=0
//   Cycle 2: posedge → ACK=1, _zz_1 = CYC&&STB&&ACK&&WE becomes true (combinational)
//   Cycle 3: posedge → sequential write latches data into register → deassert
template <typename T>
void wb_write(T* top, uint32_t byte_addr, uint32_t data, void (*tieoff)(T*) = nullptr) {
    uint32_t word_addr = (byte_addr >> 2) & 0x3FF;

    // Assert bus signals
    top->io_bus_CYC = 1;
    top->io_bus_STB = 1;
    top->io_bus_WE = 1;
    top->io_bus_ADR = word_addr;
    top->io_bus_DAT_MOSI = data;

    // Cycle 1: ACK not yet asserted
    wb_clock_once(top, tieoff);

    // Cycle 2: ACK rises. _zz_1 becomes true combinationally.
    wb_clock_once(top, tieoff);

    // Cycle 3: Sequential write latches data. Keep bus asserted for latch.
    wb_clock_once(top, tieoff);

    // Deassert bus
    top->io_bus_CYC = 0;
    top->io_bus_STB = 0;
    top->io_bus_WE = 0;
    top->io_bus_DAT_MOSI = 0;
    wb_clock_once(top, tieoff);
}

// ---- Verification ----
// Print structured VERIFY line and return pass/fail.
// Format: VERIFY <peripheral> <reg_name> <byte_addr> <got> <expected> PASS|FAIL
static int g_pass_count = 0;
static int g_fail_count = 0;

inline bool verify(const char* peripheral, const char* reg_name,
                   uint32_t byte_addr, uint32_t got, uint32_t expected) {
    bool pass = (got == expected);
    printf("VERIFY %s %s 0x%08X 0x%08X 0x%08X %s\n",
           peripheral, reg_name, byte_addr, got, expected,
           pass ? "PASS" : "FAIL");
    if (pass) g_pass_count++;
    else g_fail_count++;
    return pass;
}

// Verify non-zero (for registers like mtime that auto-increment).
inline bool verify_nonzero(const char* peripheral, const char* reg_name,
                           uint32_t byte_addr, uint32_t got) {
    bool pass = (got != 0);
    printf("VERIFY %s %s 0x%08X 0x%08X non-zero %s\n",
           peripheral, reg_name, byte_addr, got,
           pass ? "PASS" : "FAIL");
    if (pass) g_pass_count++;
    else g_fail_count++;
    return pass;
}

// Print final summary and return exit code.
inline int summary(const char* peripheral) {
    printf("\n%s Testbench: %d passed, %d failed\n",
           peripheral, g_pass_count, g_fail_count);
    if (g_fail_count == 0) {
        printf("%s Testbench PASSED\n", peripheral);
        return 0;
    } else {
        printf("%s Testbench FAILED\n", peripheral);
        return 1;
    }
}

#endif // WB_HELPERS_H
