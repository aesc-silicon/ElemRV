Virtual Prototyping
###################

Virtual prototyping runs an ElemRV platform **without physical hardware**. It is
how you develop and debug software, and verify the RTL, long before an FPGA
bitstream or a tape-out exists.

ElemRV offers three approaches that trade simulation accuracy for speed:

.. list-table::
   :header-rows: 1
   :widths: 22 18 30 30

   * - Approach
     - Tool
     - What runs
     - Use it for
   * - :doc:`rtl-simulation`
     - Verilator
     - The full SoC as cycle-accurate RTL, with waveforms.
     - Verifying the hardware itself.
   * - :doc:`emulation`
     - Renode
     - Fast functional models of the CPU and peripherals (no RTL).
     - Rapid software bring-up and iteration.
   * - :doc:`co-simulation`
     - Renode + Verilator
     - Functional CPU plus the real peripheral RTL.
     - Running software against actual peripheral hardware, at near-emulation speed.

Pick **emulation** for the fast software loop, **co-simulation** to validate a
driver against the real peripheral RTL while keeping that speed, and **RTL
simulation** when you need full cycle accuracy and waveforms of the whole SoC.

All three are driven by `Taskfile <https://taskfile.dev>`_ inside the container
and select the platform with ``SOC`` and ``PDK`` (see :doc:`../installation`),
for example::

    task sim:emulate SOC=ElemRV-H TARGET=SG13CMOS5L

.. toctree::
   :maxdepth: 2

   rtl-simulation.rst
   emulation.rst
   co-simulation.rst
