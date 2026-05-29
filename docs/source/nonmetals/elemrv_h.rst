ElemRV-H (Hydrogen)
###################

ElemRV-H is the entry-level platform of the Nonmetal class. It is designed for
minimal area and simplicity: a single-issue in-order CPU, a TileLink shared bus,
no caches, and a small set of low-speed peripherals. All IO is routed through a
pinmux controller, making the 12 physical pins fully reconfigurable in software.

Specifications
**************

.. list-table::
   :widths: 30 70
   :header-rows: 0

   * - **CPU**
     - VexiiRiscv RV32I_ifenciz, single-issue in-order, M-mode only
   * - **Clock**
     - 50 MHz system
   * - **Interconnect**
     - TileLink shared bus
   * - **On-chip SRAM**
     - 8 kB
   * - **SPI Flash**
     - 512 kB, Quad SPI XIP
   * - **IO pins**
     - 12 (via pinmux)
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
   * - SPI Flash (XIP)
     - ``0xa0000000``
     - 512 kB
     - Code and read-only data
   * - On-chip SRAM
     - ``0x80000000``
     - 8 kB
     - Data, stack, and runtime code
   * - Peripherals
     - ``0xf0000000``
     - --
     - See peripheral map below

Peripherals
***********

Offsets are relative to the peripheral base address at ``0xf0000000``.

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
     - 12-pin GPIO controller, all pins input capable - see :ref:`hardware-peripherals-gpio`
   * - I2C0
     - ``0x1000``
     - 4 kB
     - I2C controller with interrupt - see :ref:`hardware-peripherals-i2c-controller`
   * - PIO0
     - ``0x2000``
     - 4 kB
     - Programmable IO, 3 pins - see :ref:`hardware-peripherals-pio`
   * - PWM0
     - ``0x3000``
     - 4 kB
     - PWM controller, 2 channels - see :ref:`hardware-peripherals-pwm`
   * - UART0
     - ``0x4000``
     - 4 kB
     - UART with full handshake (TX, RX, CTS, RTS) - see :ref:`hardware-peripherals-uart`
   * - PRNG
     - ``0x5000``
     - 4 kB
     - Pseudo Random Number Generator - see :ref:`hardware-crypto-prng`
   * - Pinmux
     - ``0x10000``
     - 4 kB
     - Pin multiplexer for all 12 IO pins

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
     - Reset domain controller
   * - ClockController
     - ``0x22000``
     - 4 kB
     - Clock domain controller
   * - Syscon
     - ``0x23000``
     - 4 kB
     - System controller (board identity, feature flags, IP counts)
   * - SpiFlash (config)
     - ``0x24000``
     - 4 kB
     - SPI XIP controller configuration
   * - SpiFlash (XIP ctrl)
     - ``0x25000``
     - 4 kB
     - SPI XIP controller
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
   * - PLIC
     - ``0x800000``
     - 4 MB
     - Platform-Level Interrupt Controller

Interrupts
**********

.. list-table::
   :header-rows: 1
   :widths: 20 80

   * - Source
     - Description
   * - GPIO0
     - Triggered by configurable pin edge or level events
   * - I2C0
     - Triggered on transfer completion or error
   * - UART0
     - Triggered on receive, transmit, or error conditions
   * - Timer
     - Triggered on counter expiry
   * - Watchdog
     - Triggered on timeout or window violation; error output triggers system reset
   * - ESM
     - Triggered on INFO or WARN severity events; ERROR/FATAL triggers system reset

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
     - PWM0_0
   * - 1
     - GPIO0_1
     - PIO0_0
   * - 2
     - GPIO0_2
     - PIO0_1
   * - 3
     - GPIO0_3
     - PIO0_2
   * - 4
     - UART0_TX
     - GPIO0_4
   * - 5
     - UART0_RX
     - GPIO0_5
   * - 6
     - UART0_CTS
     - GPIO0_6
   * - 7
     - UART0_RTS
     - GPIO0_7
   * - 8
     - GPIO0_8
     - PWM0_1
   * - 9
     - GPIO0_9
     - I2C0_SCL
   * - 10
     - GPIO0_10
     - I2C0_SDA
   * - 11
     - GPIO0_11
     - I2C0_INT_0

Board Targets
*************

ECPIX5
======

The ECPIX5 target maps ElemRV-H pins to the following board resources:

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
   * - 4
     - UART TX (UartStd)
   * - 5
     - UART RX (UartStd)
   * - 6
     - Pmod2 pin 0
   * - 7
     - Pmod2 pin 1
   * - 8
     - Pmod2 pin 2
   * - 9
     - Pmod2 pin 3
   * - 10
     - Pmod2 pin 4
   * - 11
     - Pmod2 pin 5

The SPI flash and JTAG are routed to Pmod6 and Pmod1 respectively.

IHP SG13G2
==========

The SG13G2 target places IO pads around the chip perimeter:

.. list-table::
   :header-rows: 1
   :widths: 15 15 70

   * - Edge
     - Position
     - Signal
   * - South
     - 0 - 3
     - JTAG (TMS, TDI, TDO, TCK)
   * - South
     - 4
     - Reset
   * - South
     - 5
     - Clock
   * - East
     - 0
     - SPI CS
   * - East
     - 1
     - SPI SCK
   * - East
     - 2 - 5
     - SPI DQ0 - DQ3
   * - West
     - 2 - 7
     - Pins 0 - 5
   * - North
     - 2 - 7
     - Pins 6 - 11
