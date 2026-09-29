ElemRV-N (Nitrogen)
###################

ElemRV-N is the third-tier Nonmetal platform. It builds on Carbon and targets
larger firmware: the CPU gains a write-back data cache, branch prediction, a user
mode with physical memory protection, and external HyperRAM for code and data.
The shared bus is replaced by a TileLink crossbar, and memories and peripherals
run in their own clock domains. The peripheral set grows to 20 IO pins with a
second I2C controller and UART, an SPI controller and a CRC engine.

Specifications
**************

.. list-table::
   :widths: 30 70
   :header-rows: 0

   * - **CPU**
     - VexiiRiscv RV32IMC_zicsr_zifencei_zicntr_zihpm, single-issue in-order,
       M and U mode, 8 PMP regions (4 kB granularity)
   * - **I-Cache**
     - 4 kB (64 sets, 1 way, 64 B line)
   * - **D-Cache**
     - 4 kB write-back (64 sets, 1 way, 64 B line), no hardware coherency
   * - **Branch prediction**
     - BTB (16 sets), GShare (256 B), return address stack
   * - **Clock**
     - 30 MHz system, 60 MHz HyperBus and XIP, 15 MHz peripherals (60 MHz reference)
   * - **Interconnect**
     - TileLink crossbar (instruction, data and I/O buses)
   * - **On-chip SRAM**
     - 8 kB
   * - **HyperRAM**
     - 64 MB window, 8 MB per chip select
   * - **SPI Flash**
     - 512 kB, Quad SPI XIP
   * - **IO pins**
     - 20 (via pinmux)
   * - **Debug**
     - JTAG

Memory Map
**********

.. list-table::
   :header-rows: 1
   :widths: 30 25 15 50

   * - Region
     - Base address
     - Size
     - Description
   * - On-chip SRAM
     - ``0x80000000``
     - 8 kB
     - Data and stack, cached
   * - HyperRAM
     - ``0x90000000``
     - 64 MB
     - Application code and data, cached
   * - SPI Flash (XIP)
     - ``0xa0000000``
     - 512 kB
     - Boot ROM and application image, cached
   * - HyperRAM (uncached)
     - ``0xb0000000``
     - 64 MB
     - Uncached alias of the HyperRAM window
   * - Peripherals
     - ``0xf0000000``
     - 16 MB
     - See peripheral map below, uncached

The boot ROM runs from flash and copies the application image from flash to
HyperRAM through the uncached alias, so no stale cache lines remain, before it
jumps to ``0x90000000``.

Peripherals
***********

Offsets are relative to the peripheral base address at ``0xf0000000``. User
peripherals run in the 15 MHz peripheral clock domain behind a clock-domain
crossing; system peripherals run in the system clock domain.

User peripherals
================

.. list-table::
   :header-rows: 1
   :widths: 20 15 15 50

   * - Peripheral
     - Offset
     - Size
     - Description
   * - GPIO0
     - ``0x0000``
     - 4 kB
     - 20-pin GPIO controller - see :ref:`hardware-peripherals-gpio`
   * - I2C0
     - ``0x1000``
     - 4 kB
     - I2C controller with one interrupt pin - see :ref:`hardware-peripherals-i2c-controller`
   * - I2C1
     - ``0x2000``
     - 4 kB
     - Lightweight I2C controller - see :ref:`hardware-peripherals-i2c-controller`
   * - PIO0
     - ``0x3000``
     - 4 kB
     - Programmable IO, 3 pins - see :ref:`hardware-peripherals-pio`
   * - PWM0
     - ``0x4000``
     - 4 kB
     - PWM controller, 2 channels - see :ref:`hardware-peripherals-pwm`
   * - SPI0
     - ``0x5000``
     - 4 kB
     - SPI controller, 1 chip select, 2 data lines
   * - UART0
     - ``0x6000``
     - 4 kB
     - UART with full handshake (TX, RX, CTS, RTS) - see :ref:`hardware-peripherals-uart`
   * - UART1
     - ``0x7000``
     - 4 kB
     - Lightweight UART (TX, RX) - see :ref:`hardware-peripherals-uart`
   * - CRC32
     - ``0x8000``
     - 4 kB
     - CRC engine - see :ref:`hardware-crypto-crc`
   * - PRNG
     - ``0x9000``
     - 4 kB
     - Pseudo Random Number Generator - see :ref:`hardware-crypto-prng`
   * - Pinmux
     - ``0x10000``
     - 4 kB
     - Pin multiplexer for all 20 IO pins - see :ref:`hardware-peripherals-pinmux`

System peripherals
==================

.. list-table::
   :header-rows: 1
   :widths: 20 15 15 50

   * - Peripheral
     - Offset
     - Size
     - Description
   * - MachineTimer
     - ``0x20000``
     - 4 kB
     - RISC-V machine-mode timer (mtime / mtimecmp)
   * - ResetController
     - ``0x21000``
     - 4 kB
     - Reset domain controller - see :ref:`hardware-system-reset`
   * - ClockController
     - ``0x22000``
     - 4 kB
     - Clock domain controller - see :ref:`hardware-system-clock`
   * - Syscon
     - ``0x23000``
     - 4 kB
     - System controller (board identity, feature flags) - see :ref:`hardware-system-syscon`
   * - SpiFlash (config)
     - ``0x24000``
     - 4 kB
     - SPI XIP controller configuration - see :ref:`hardware-memory-spixip`
   * - SpiFlash (XIP ctrl)
     - ``0x25000``
     - 4 kB
     - SPI XIP controller - see :ref:`hardware-memory-spixip`
   * - Timer
     - ``0x26000``
     - 4 kB
     - General-purpose timer - see :ref:`hardware-peripherals-timer`
   * - Watchdog
     - ``0x27000``
     - 4 kB
     - Watchdog timer with windowed mode and lock protection - see :ref:`hardware-system-watchdog`
   * - ESM
     - ``0x28000``
     - 4 kB
     - Error Signaling Module (optional) - see :ref:`hardware-system-esm`
   * - HyperBus
     - ``0x29000``
     - 4 kB
     - HyperBus controller configuration - see :ref:`hardware-memory-hyperbus`
   * - PLIC
     - ``0x800000``
     - 4 MB
     - Platform-Level Interrupt Controller

Interrupts
**********

The PLIC source numbers match the ``<NAME>_IRQ`` defines in the generated ``soc.h``.

.. list-table::
   :header-rows: 1
   :widths: 20 15 65

   * - Source
     - PLIC source
     - Description
   * - Timer
     - 1
     - Triggered on counter expiry
   * - Watchdog
     - 2
     - Triggered on timeout or window violation; error output triggers system reset
   * - ESM
     - 3
     - Triggered on INFO or WARN severity events; ERROR/FATAL triggers system reset
   * - GPIO0
     - 4
     - Triggered by configurable pin edge or level events
   * - I2C0
     - 5
     - Triggered on transfer completion or error
   * - I2C1
     - 6
     - Triggered on transfer completion or error
   * - PIO0
     - 7
     - Triggered on state machine completion or error
   * - PWM0
     - 8
     - Triggered on period completion or error
   * - SPI0
     - 9
     - Triggered when the command FIFO is empty or a response is available
   * - UART0
     - 10
     - Triggered on receive, transmit, or error conditions
   * - UART1
     - 11
     - Triggered on receive, transmit, or error conditions

Pinmux
******

Each physical pin can be assigned to one of two functions in software via the
pinmux controller. The first function listed is the default.

.. list-table::
   :header-rows: 1
   :widths: 10 30 30

   * - Pin
     - Function 0
     - Function 1
   * - 0
     - GPIO0_0
     - I2C0_SCL
   * - 1
     - GPIO0_1
     - I2C0_SDA
   * - 2
     - GPIO0_2
     - I2C0_INT_0
   * - 3
     - GPIO0_3
     - I2C1_SCL
   * - 4
     - GPIO0_4
     - I2C1_SDA
   * - 5
     - GPIO0_5
     - PIO0_0
   * - 6
     - GPIO0_6
     - PIO0_1
   * - 7
     - GPIO0_7
     - PIO0_2
   * - 8
     - GPIO0_8
     - PWM0_0
   * - 9
     - GPIO0_9
     - PWM0_1
   * - 10
     - GPIO0_10
     - SPI0_CS0
   * - 11
     - GPIO0_11
     - SPI0_SCLK
   * - 12
     - GPIO0_12
     - SPI0_DQ0
   * - 13
     - GPIO0_13
     - SPI0_DQ1
   * - 14
     - UART0_TX
     - GPIO0_14
   * - 15
     - UART0_RX
     - GPIO0_15
   * - 16
     - UART0_CTS
     - GPIO0_16
   * - 17
     - UART0_RTS
     - GPIO0_17
   * - 18
     - GPIO0_18
     - UART1_TX
   * - 19
     - GPIO0_19
     - UART1_RX

Board Targets
*************

ECPIX5
======

The ECPIX5 target runs all clocks from a PLL and uses four 8 MB HyperRAM chip
selects (32 MB). It maps ElemRV-N pins to the following board resources:

.. list-table::
   :header-rows: 1
   :widths: 10 90

   * - Pin
     - ECPIX5 resource
   * - 0
     - LED LD5 (blue)
   * - 1
     - LED LD6 (red)
   * - 2
     - LED LD7 (green)
   * - 3
     - Button SW0
   * - 4 - 11
     - Pmod2 pins 0 - 7
   * - 12 - 13
     - Pmod3 pins 0 - 1
   * - 14
     - UART TX (UartStd)
   * - 15
     - UART RX (UartStd)
   * - 16 - 19
     - Pmod3 pins 2 - 5

JTAG is routed to Pmod1, the SPI flash to Pmod6, and HyperBus to Pmod4 (DQ0 - DQ7)
and Pmod5 (chip selects, clock, reset, RWDS).

IHP SG13CMOS5L
===============

The SG13CMOS5L target derives all clocks from the 60 MHz reference with clock
dividers and uses two 8 MB HyperRAM chip selects (16 MB). It places IO pads
around the chip perimeter:

.. list-table::
   :header-rows: 1
   :widths: 15 15 70

   * - Edge
     - Position
     - Signal
   * - West
     - 2 - 5
     - JTAG (TMS, TDI, TDO, TCK)
   * - West
     - 6 - 7
     - Reserved
   * - West
     - 8
     - Reset
   * - West
     - 9
     - Clock
   * - West
     - 10 - 13
     - Pins 0 - 3
   * - East
     - 2
     - SPI CS
   * - East
     - 3
     - SPI SCK
   * - East
     - 4 - 7
     - SPI DQ3 - DQ0
   * - East
     - 8
     - SPI flash reset
   * - East
     - 9 - 12
     - Pins 4 - 7
   * - East
     - 13
     - HyperBus CK
   * - North
     - 2 - 9
     - HyperBus DQ0 - DQ7
   * - North
     - 10
     - HyperBus reset
   * - North
     - 11
     - HyperBus RWDS
   * - North
     - 12 - 13
     - HyperBus CS0 - CS1
   * - South
     - 2 - 13
     - Pins 8 - 19
