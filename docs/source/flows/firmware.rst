Firmware Flow
#############

The firmware flow compiles bare-metal applications for nonmetal platforms and
flashes them onto the target hardware via SPI flash. Each platform ships with
two applications:

- **bootrom** - minimal startup code that initialises the CPU and hands off to
  the application in flash.
- **demo** - a bare-metal demonstration application showcasing the platform
  peripherals.

Both are compiled and packed together into a single image container that maps
onto the SPI flash layout.

The ``board`` variable selects the target process node, which also determines
which platform's linker scripts and startup code are used (default:
``SG13G2``). The ``SOC`` variable selects the platform (default: ``ElemRV-N``).

Compile
*******

Compiles the bootrom and demo, then assembles them into the image container::

    task firmware:compile

To target a specific platform::

    task firmware:compile SOC=ElemRV-N board=SG13G2

The compiled image is written to::

    build/<SOC>/<TECH>/firmware/image_container.img

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
Micron MT25QL256::

    task firmware:flash

.. note::

   The flash task expects the Bus Pirate 5 to be connected and enumerated at
   ``/dev/serial/by-id/usb-Bus_Pirate_Bus_Pirate_5_5buspirate-if02``. Adjust
   the device path in ``Taskfile.firmware.yml`` if your setup differs.
