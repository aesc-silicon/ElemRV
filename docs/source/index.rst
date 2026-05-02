ElemRV
######

ElemRV is a collection of end-to-end open-source RISC-V Microcontrollers, fully
implemented in SpinalHDL and designed to work seamlessly with open-source toolchains -
from RTL simulation and FPGA prototyping all the way to GDSII tape-out.

The existing platform configurations serve as verified reference designs. Peripherals
can be adapted to meet specific project requirements, while the core platform provides
a solid, tested foundation to build on.

The Name
********

**ElemRV** combines *element names* - each platform is named after a chemical element -
with *RV* for the RISC-V instruction set architecture. The atomic number determines the
capability tier: the higher the atomic number, the more capable the platform.

Platform Classes
****************

Platforms are grouped by chemical element group, which defines their class:

**Nonmetals (MCU)**
   Platforms in the Nonmetal group (Hydrogen, Carbon, Nitrogen, ...) target
   embedded workloads. They operate without an MMU and are designed for
   `Zephyr RTOS <https://zephyrproject.org>`_ or bare-metal firmware. The
   performance range spans from a minimal single-issue CPU to dual-issue
   out-of-order cores with FPU and hardware accelerators.

**Linux platforms (coming soon)**
   Higher element groups will target Linux-capable SoCs, bringing MMU support,
   DDR memory, and a full boot chain.

Technical Foundation
********************

All ElemRV platforms share a common technical foundation:

- **SpinalHDL** - the entire RTL is written in SpinalHDL, without any Verilog or VHDL.
- **VexiiRiscv** - a configurable, SpinalHDL-native RISC-V CPU core.
- **Open-source toolchain only** - no proprietary tools are required at any stage.
  Design verification uses Yosys and nextpnr; chip layout uses the OpenROAD flow.
- **Zephyr RTOS** - all nonmetal platform firmware is based on Zephyr.

Tape-out Targets
****************

ElemRV platforms can be taken from RTL to GDSII entirely with open-source tools.
Current and upcoming process node targets:

- **IHP SG13G2** - 130 nm BiCMOS open PDK, currently supported.
- **IHP CMOS5L** - coming soon.
- **GlobalFoundries GF180MCU** - coming soon.

.. figure:: images/chip_topview.webp
   :alt: ElemRV chip top view
   :align: center

   Top view of an ElemRV chip layout showing the standard cell array and I/O ring.


Content
*******

.. toctree::
   :maxdepth: 2

   installation.rst
   flows/index.rst
   nonmetals/index.rst
   hardware/index.rst
