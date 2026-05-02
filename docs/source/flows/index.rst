Flows
#####

ElemRV supports three independent flows that can be used in any combination
depending on the goal:

- **FPGA** - synthesize and run the design on an ECPIX5 development board for
  rapid prototyping and RTL verification.
- **Chip** - run the full RTL-to-GDSII flow with OpenROAD to produce a layout
  ready for tape-out.
- **Firmware** - compile bare-metal applications and program them onto the SPI
  flash of a physical board.

All flows are driven by `Taskfile <https://taskfile.dev>`_ and run inside a
container, so no manual toolchain setup is required beyond the steps in
:doc:`../installation`.

.. toctree::
   :maxdepth: 2

   fpga.rst
   chip.rst
   firmware.rst
