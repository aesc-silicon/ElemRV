Firmware Flow
#############

ElemRV platforms support two firmware flows: a bare-metal flow for low-level
applications without an RTOS, and a Zephyr flow for applications built on top
of the Zephyr RTOS. Both flows produce an image container that is flashed onto
the SPI flash of the target hardware.

The ``SOC`` variable selects the platform (default: ``ElemRV-N``) and the
``board`` variable selects the target process node (default: ``SG13G2``).

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

    task baremetal:compile SOC=ElemRV-N board=SG13G2

The compiled image is written to::

    build/<SOC>/<board>/firmware/baremetal_container.img

Zephyr
******

The Zephyr flow compiles the bootrom alongside a Zephyr application and packs
them into ``zephyr_container.img``.

Compile
=======

::

    task zephyr:compile

To target a specific platform::

    task zephyr:compile SOC=ElemRV-N board=SG13G2

The compiled image is written to::

    build/<SOC>/<board>/firmware/zephyr_container.img

Flash Layout
************

The SPI flash is partitioned as defined in ``software/<platform>/spi.layout``.
For ElemRV-N the layout is::

    00000000:0001ffff flash

The image container is written into the ``flash`` region starting at address
``0x00000000``.

Flash
*****

Programs the SPI flash using `flashrom <https://www.flashrom.org>`_ over a
`Bus Pirate 5 <https://buspirate.com>`_ at 8 MHz. The target flash chip is a
Micron MT25QL256.

For bare-metal::

    task baremetal:flash

For Zephyr::

    task zephyr:flash

.. note::

   The flash task expects the Bus Pirate 5 to be connected and enumerated at
   ``/dev/serial/by-id/usb-Bus_Pirate_Bus_Pirate_5_5buspirate-if02``. Adjust
   the device path in the respective Taskfile if your setup differs.
