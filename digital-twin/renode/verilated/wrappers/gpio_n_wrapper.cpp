// SPDX-FileCopyrightText: 2025 aesc silicon
//
// SPDX-License-Identifier: CERN-OHL-W-2.0

// GPIO (N-variant, 20 pins) Co-simulation wrapper for Renode
// TriState pins (io_gpio_pins_*): left unconnected (read returns 0).
// io_interrupt output: ignored (not connected to CPU in co-sim test).

#include <cstdio>
#include <cstdint>
#include "src/buses/wishbone.h"
#include "src/renode_bus.h"
#include "verilated.h"
#include "VWishboneGpio.h"

#define DEBUG_ENABLE 0

#if DEBUG_ENABLE
  #define DEBUG_PRINT(fmt, ...) do { printf("[GPIO_N] " fmt "\n", ##__VA_ARGS__); fflush(stdout); } while(0)
#else
  #define DEBUG_PRINT(fmt, ...)
#endif

static VWishboneGpio* g_top = nullptr;

static uint64_t g_bridge_addr    = 0;
static uint64_t g_bridge_rd_dat  = 0;
static uint64_t g_bridge_wr_dat  = 0;

static uint8_t g_dummy_sel = 0xF;
static uint8_t g_dummy_stall = 0;

static void copyBridgeAndEval() {
  g_top->io_bus_ADR      = (uint16_t)((g_bridge_addr >> 2) & 0x3FF);
  g_top->io_bus_DAT_MOSI = (uint32_t)(g_bridge_wr_dat);

  // Tie GPIO pins read = 0 (no external connections)
  g_top->io_gpio_pins_read = 0;

  g_top->eval();

  g_bridge_rd_dat = (uint64_t)g_top->io_bus_DAT_MISO;
}

void evalModel() {
  if (!g_top) return;

  copyBridgeAndEval();

  if (g_top->io_bus_WE && g_top->io_bus_ACK &&
      g_top->io_bus_CYC && g_top->io_bus_STB) {
    g_top->clk = 1;
    copyBridgeAndEval();
    g_top->clk = 0;
    copyBridgeAndEval();
    DEBUG_PRINT("[WRITE-LATCH] addr=0x%03X wdata=0x%08X",
                g_top->io_bus_ADR, g_top->io_bus_DAT_MOSI);
  }

  DEBUG_PRINT("[EVAL] addr=0x%03X we=%d wdata=0x%08X rdata=0x%08X ack=%d",
              g_top->io_bus_ADR, g_top->io_bus_WE,
              g_top->io_bus_DAT_MOSI, g_top->io_bus_DAT_MISO,
              g_top->io_bus_ACK);
}

class GpioNPeripheral : public RenodeAgent {
public:
  GpioNPeripheral() : RenodeAgent(), top(nullptr), bus(nullptr), tickCounter(0) {}

  void initialize() {
    DEBUG_PRINT("=== INITIALIZATION START ===");

    top = new VWishboneGpio();
    g_top = top;

    bus = new Wishbone();

    bus->wb_clk = (uint8_t*)&top->clk;
    bus->wb_rst = (uint8_t*)&top->resetn;

    bus->wb_addr   = &g_bridge_addr;
    bus->wb_rd_dat = &g_bridge_rd_dat;
    bus->wb_wr_dat = &g_bridge_wr_dat;

    bus->wb_we    = (uint8_t*)&top->io_bus_WE;
    bus->wb_sel   = &g_dummy_sel;
    bus->wb_stb   = (uint8_t*)&top->io_bus_STB;
    bus->wb_cyc   = (uint8_t*)&top->io_bus_CYC;
    bus->wb_ack   = (uint8_t*)&top->io_bus_ACK;
    bus->wb_stall = &g_dummy_stall;

    bus->granularity = 1;
    bus->addr_lines = 32;

    bus->evaluateModel = evalModel;
    addBus(bus);

    top->resetn = 0;
    top->clk = 0;
    top->eval();

    for (int i = 0; i < 10; i++) {
      top->clk = 1; top->eval();
      top->clk = 0; top->eval();
    }

    top->resetn = 1;
    top->clk = 1; top->eval();
    top->clk = 0; top->eval();

    DEBUG_PRINT("=== INITIALIZATION COMPLETE ===");
  }

  ~GpioNPeripheral() {
    if (top) { top->final(); delete top; }
    if (bus) { delete bus; }
    g_top = nullptr;
  }

  void tick(bool countEnable, uint64_t steps) override {
    if (!top || steps == 0) return;

    for (uint64_t i = 0; i < steps; i++) {
      *bus->wb_clk = 1;
      top->eval();
      *bus->wb_clk = 0;
      top->eval();

      if (countEnable) tickCounter++;
    }
  }

  void reset() {
    if (!top) return;
    tickCounter = 0;

    *bus->wb_rst = 0;
    for (int i = 0; i < 10; i++) bus->tick(true, 1);
    *bus->wb_rst = 1;
    bus->tick(true, 1);
  }

private:
  VWishboneGpio* top;
  Wishbone* bus;
  uint64_t tickCounter;
};

static GpioNPeripheral* gpioNPeripheral = nullptr;

RenodeAgent* Init() {
  if (!gpioNPeripheral) {
    gpioNPeripheral = new GpioNPeripheral();
    gpioNPeripheral->connectNative();
    gpioNPeripheral->initialize();
  }
  return gpioNPeripheral;
}

int main(int argc, char** argv) {
  Verilated::commandArgs(argc, argv);
  RenodeAgent* agent = Init();
  agent->simulate();
  return 0;
}
