Firmware Flow
#############

ElemRV platforms support two firmware flows: a bare-metal flow for low-level
applications without an RTOS, and a Zephyr flow for applications built on top
of the Zephyr RTOS. Both flows produce an image container that is flashed onto
the SPI flash of the target hardware.

The ``SOC`` variable selects the platform (default: ``ElemRV-N``) and the
``TARGET`` variable selects the ASIC target / process node (default:
``SG13G2``).

Bare-Metal
**********

The bare-metal flow compiles two applications:

- **bootrom** - minimal startup code that initialises the CPU and hands off to
  the application in flash.
- **demo** - a bare-metal demonstration application showcasing the platform
  peripherals.

Both are compiled and packed into ``baremetal_container.img``.

Compile
=======

::

    task baremetal:compile

To target a specific platform::

    task baremetal:compile SOC=ElemRV-N TARGET=SG13G2

The compiled image is written to::

    build/<SOC>/firmware/baremetal_container.img

Zephyr
******

The Zephyr flow compiles the bootrom alongside a Zephyr application and packs
them into ``zephyr_container.img``.

Compile
=======

::

    task zephyr:compile

To target a specific platform::

    task zephyr:compile SOC=ElemRV-N TARGET=SG13G2

The compiled image is written to::

    build/<SOC>/firmware/zephyr_container.img

Flash Layout
************

The SPI flash is partitioned as defined in ``software/<platform>/spi.layout``.
For ElemRV-N the layout is::

    00000000:0001ffff flash

The image container is written into the ``flash`` region starting at address
``0x00000000``.

Flash
*****

Flashing is independent of the firmware flow: both ``baremetal:compile`` and
``zephyr:compile`` update the ``image_container.img`` symlink, and the
``flash`` namespace programs whichever firmware was built last. Two transports
are available.

Bus Pirate
==========

Programs the SPI flash using `flashrom <https://www.flashrom.org>`_ over a
`Bus Pirate 5 <https://buspirate.com>`_. flashrom requires a chip-sized image,
so the task pads a throwaway copy of the container to 32 MiB before writing::

    task flash:buspirate

To target a specific platform::

    task flash:buspirate SOC=ElemRV-H TARGET=SG13CMOS5L

JTAG
====

Programs the SPI flash over JTAG using OpenOCD. Unlike the Bus Pirate path,
JTAG writes the unpadded image directly::

    task flash:jtag

.. note::

   ``flash:jtag`` is a placeholder until the toolchain container ships OpenOCD.

The flash chip, Bus Pirate device path and SPI speed are configured in
``Taskfile.flash.yml`` and can be overridden on the command line (for example
``FLASH_CHIP=MT25QL128`` or ``BUSPIRATE_DEV=/dev/serial/by-id/...``). The
defaults target a Micron MT25QL256 connected to a Bus Pirate 5 at
``/dev/serial/by-id/usb-Bus_Pirate_Bus_Pirate_5_5buspirate-if02`` at 8 MHz.
