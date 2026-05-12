# ElemRV Digital Twin

Cycle-accurate Verilator co-simulation of ElemRV peripherals driven from Renode, validated with real firmware (bare-metal and Zephyr) on a system simulator. No FPGA, no silicon.

This first slice ships the **ElemRV-N + GPIO + UART** end-to-end path: Scala -> Verilog -> Verilator -> Renode -> Zephyr blinky. Scope was set with the maintainer in https://github.com/aesc-silicon/ElemRV/issues/13.

## What is in this slice

| Layer | Files |
| --- | --- |
| Standalone SBT sub-project | `digital-twin/build.sbt`, `digital-twin/REUSE.toml` |
| SpinalHDL generators (ElemRV-N) | `digital-twin/scala/elemrv_n/WishboneGpioNVerilog.scala`, `WishboneUartLiteVerilog.scala` |
| Pre-built Verilog (reproducible) | `digital-twin/gen_n/WishboneGpio.v`, `WishboneUart.v` |
| Verilator wrappers + Makefiles | `digital-twin/renode/verilated/wrappers/{gpio_n_wrapper.cpp,uart_lite_wrapper.cpp,Makefile.gpio_n,Makefile.uart_lite,build_n_cosim.sh}` |
| Renode platforms (.repl) | `digital-twin/renode/platforms/elemrv_n{,_cosim_gpio,_cosim_uart_lite,_zephyr}.repl` |
| Renode run scripts (.resc) | `digital-twin/renode/run_n_{base,cosim_gpio,cosim_uart_lite,zephyr_blinky}_test.resc` |
| Pure Verilator testbenches | `digital-twin/renode/verilated/testbenches/{Makefile.nitrogen,tb_gpio_n.cpp,tb_uart_lite.cpp,wb_helpers.h}` |
| Test runner | `digital-twin/renode/run_tests.sh` (5 tests) |
| Zephyr 4.1 module | `digital-twin/zephyr/elemrv-zephyr/` (board `aesc/elemrv_n`, SoC `elemrv_n_vexriscv`, `app/blinky`) |
| CI workflow | `.github/workflows/digital-twin-tests.yaml` (`workflow_dispatch` only) |

## Container requirements

The Digital Twin reuses `aesc-silicon/elements-container` (https://github.com/aesc-silicon/elements-container/blob/main/Containerfile). At main today the image already provides sbt, JDK 11, Python 3 + `west`, CMake, ninja, the Zephyr SDK with `riscv64-zephyr-elf`, Renode and Verilator, so the cosim build and `west build` work out of the box.

The DT only relies on the simulation-side bits of that image. Worth double-checking when you bump versions:

- **Renode**: needs 1.16.0 or newer (we use the `RegisterAccessFlags` API path). The image is currently on 1.16.1, fine. The cosim Makefiles read `RENODE_ROOT` and default to `/opt/renode_1.16.0_portable`. If the install path in the container differs (the apt `.deb` puts pieces under `/usr/lib/renode`), export `RENODE_ROOT` to whatever directory contains `plugins/IntegrationLibrary/`. `renode` itself must be on `PATH`.
- **Verilator**: needs v5.006 or newer. The wrappers will not build against 4.x. The Makefiles invoke `verilator` from `PATH`.
- **Zephyr SDK**: needs the `riscv64-zephyr-elf` toolchain installed (Zephyr 4.1 works with SDK 0.17.0 or 1.0.x). `ZEPHYR_SDK_INSTALL_DIR` should point at the SDK root.
- **device-tree-compiler (`dtc`)**: Zephyr needs it. Currently not in the final stage of `elements-container`; adding `device-tree-compiler` to the apt list there is the only addition we expect.

That's it; everything else (sbt, JDK, CMake, ninja, west, Python deps, RISC-V toolchain) is already in the image.

## Build and run, end-to-end

All commands assume the simulation container is running with the repo mounted at `/workspace/elemrv` (CI does this automatically).

```bash
# 0. Bootstrap nafarr + zibal (idempotent)
task install

# 1. Generate ElemRV-N peripheral Verilog
cd digital-twin
sbt "runMain elemrv_n.test.WishboneGpioNVerilog"
sbt "runMain elemrv_n.test.WishboneUartLiteVerilog"
cd ..

# 2. Build Verilator co-simulation libraries
cd digital-twin/renode/verilated/wrappers
bash build_n_cosim.sh release
cd ../../../..
# Produces digital-twin/renode/verilated/libs/{libgpio_n.so,libuart_lite.so}.

# 3. Set up Zephyr and build the blinky app
cd digital-twin/zephyr
west init -l elemrv-zephyr 2>/dev/null || true
west update
cd elemrv-zephyr
west build -b elemrv_n app/blinky -d build-n-blinky
cd ../../..

# 4. Run the 5-test suite
cd digital-twin/renode
bash run_tests.sh
# SUMMARY: 5/5 passed
```

## Tests in this slice

| # | Name | Script | Pass marker |
| --- | --- | --- | --- |
| 1 | N Base Platform | `run_n_base_test.resc` | `ElemRV-N Base Platform Test PASSED` |
| 2 | N GPIO Co-simulation | `run_n_cosim_gpio_test.resc` | `N GPIO Co-simulation Test PASSED` |
| 3 | N UART Lite Co-simulation | `run_n_cosim_uart_lite_test.resc` | `N UART Lite Co-simulation Test PASSED` |
| 4 | N Pure Verilator Testbenches | `Makefile.nitrogen run` | `All N testbenches PASSED` |
| 5 | N Zephyr Blinky | `run_n_zephyr_blinky.resc` | `N Zephyr Blinky Test PASSED` |

Tests 2 and 3 skip cleanly if the corresponding `.so` is not built. Test 5 skips if the Zephyr ELF is missing.

## Memory map (ElemRV-N base)

| Region | Address | Size | Notes |
| --- | --- | --- | --- |
| RAM | `0x80000000` | 4 KiB | data (XIP keeps code in flash) |
| HyperRAM | `0x90000000` | 64 MiB | external |
| Flash | `0xA0000000` | 64 KiB | XIP code |
| GPIO0 | `0xF0000000` | 4 KiB | 20 pins; co-sim swaps to `WishboneGpio` |
| I2C0 | `0xF0001000` | 4 KiB | LiteX I2C |
| UART0 | `0xF0006000` | 4 KiB | LiteX UART (console) |
| UART1 (cosim slot) | `0xF0007000` | 4 KiB | swapped to `WishboneUart` in `elemrv_n_cosim_uart_lite.repl` |
| Timer0 | `0xF0020000` | 4 KiB | LiteX Timer (CSR32) |

## Follow-ups (separate PRs)

- ElemRV-H base + PWM driver and PWM co-simulation (Daniel flagged PWM as interesting in https://github.com/aesc-silicon/ElemRV/issues/13#issuecomment-4351154911).
- Pinmux, SPI, I2C co-simulation for ElemRV-N (drivers/spi/pinctrl wiring, `aesc,nafarr-spi` DT binding).
- BMB co-simulation + SPI XIP (Phase G).
- CPU-driven XIP via tlib executable-IO (Phase I).
- RTOS thread-aware debug demo, fault injection harness, multi-node IoT demo.
- Taskfile integration (Daniel offered to add the Taskfile/environment plumbing).

## License

Code is under Apache-2.0; generated Verilog and hardware-doc content are CERN-OHL-W-2.0 (see `digital-twin/REUSE.toml`). Existing SPDX headers take precedence; the REUSE annotations only fill gaps.
