.. SPDX-FileCopyrightText: 2025 aesc silicon
..
.. SPDX-License-Identifier: CERN-OHL-W-2.0

ElemRV - End-to-end Open-Source RISC-V MCU Platform
====================================================

ElemRV is a platform of open-source RISC-V microcontrollers, fully implemented in SpinalHDL and designed to work seamlessly with the OpenROAD toolchain. Several SoC variants are available, ranging from minimal cost-efficient cores to high-performance configurations, while sharing a common toolchain and design methodology. Each variant is tailored to work with open PDKs such as IHP SG13G2, providing a complete open-source solution from RTL to GDSII without any reliance on proprietary software.

Features
########

* **SpinalHDL Implementation**: The design is entirely written in SpinalHDL, without using Verilog or VHDL.
* **Open-Source Toolchain**: The project exclusively uses open-source tools, ensuring compatibility without proprietary dependencies.

  * **Design Verification**: Conducted using Yosys and nextpnr.

  * **Chip Layout**: Created using the OpenROAD flow.

* **RISC-V**: Powered by a VexiiRiscv RISC-V CPU, with instruction set extensions tailored to each SoC variant.
* **Zephyr RTOS**: Firmware based on Zephyr RTOS for efficient real-time operation.
* **Memory**: All variants boot from external SPI Flash in XIP mode. Higher-tier SoCs add HyperRAM for additional memory.
* **Interfaces**: Includes common low-speed interfaces such as GPIO, UART, I2C, SPI, and programmable I/Os.

Layout Rendering
################

View of standard cells with the power distribution network.

.. image:: images/chip_gates.webp
  :alt: ElemRV Chip Layout

Top view of standard cells.

.. image:: images/chip_topview.webp
  :alt: ElemRV Chip Layout

A closer look at the power and ground I/O cells located in the I/O ring.

.. image:: images/chip_io.webp
  :alt: ElemRV Chip Layout

Another look at the I/O ring with the ``ElemRV`` label.

.. image:: images/chip_label.webp
  :alt: ElemRV Chip Layout

Installation
############

This project uses Taskfile as its task runner tool. You can install Taskfile using Snap or an alternative package manager for your distribution (e.g., Ubuntu). Once installed, run the `install` task to download and set up all dependencies.

- Install Taskfile and virtualenv::

        sudo apt install virtualenv curl podman
        sudo sh -c "$(curl --location https://taskfile.dev/install.sh)" -- -d -b /usr/local/bin

- Set up the project::

        task install
        task install-zephyr

- List all available tasks::

        task -a

Most tasks accept the ``SOC`` and ``PDK`` variables to select a specific SoC variant and target PDK. For example::

        task prepare SOC=ElemRV-H TARGET=SG13CMOS5L

**Note:** By default, the X-Server is required for the `view-klayout` and `view-openroad` tasks. On headless systems, you can bypass this requirement by adding `IS_HEADLESS=true` before the task command. This is particularly useful when accessing the system via SSH, as it allows you to run the container without the need for X-Server.

FPGA Flow
#########

Start by generating the necessary files for the ECPIX5 Board, then synthesize the design.

.. code-block:: text

    task fpga:prepare fpga:synthesize

Next, program the ECP5 FPGA with the synthesized bitstream.

.. code-block:: text

    task fpga:flash

ASIC Flow
#########

The ASIC flow closely resembles the FPGA flow. Begin by generating all required files, then proceed with chip layout creation and filler insertion.

.. code-block:: text

    task prepare layout filler

If the chip layout process fails, consult the **Known Issues** section for troubleshooting tips.

Finally, review the chip layout using OpenROAD or KLayout.

.. code-block:: text

    task view-klayout
    task view-openroad

Earlier stages of the layout process can also be reviewed in OpenROAD by passing the `stage` argument.

.. code-block:: text

    task view-openroad stage=6_final

Design Rule Checks
##################

Use the following tasks to perform Design Rule Checks (DRC) on the chip layout. Minimal checks can be run as follows:

.. code-block:: text

    task run-drc level=minimal
    task view-drc level=minimal

To run an enhanced rule set, use the standard DRC commands:

.. code-block:: text

    task run-drc
    task view-drc

Tape-out
########

The default task runs the complete RTL-to-GDSII tape-out flow in one step. The final GDS file undergoes a comprehensive DRC check and is prepared for tape-out.

.. code-block:: text

    task

Known Issues
############

- **X-Server**: If you encounter an error when running `view-klayout` or `view-openroad`, it may be due to permission restrictions with the X-Server. To resolve this, run the following command in your terminal to add the current user to the X-Server backend:

  .. code-block:: text

     xhost +si:localuser:$USER

License
#######

Copyright (c) 2025 aesc silicon. Released under the `CERN-OHL-W-2.0`_ and `Apache-2.0`_ license.

.. _CERN-OHL-W-2.0: LICENSES/CERN-OHL-W-2.0.txt
.. _Apache-2.0: LICENSES/Apache-2.0.txt
