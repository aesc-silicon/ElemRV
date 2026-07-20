Co-simulation
#############

Co-simulation is a hybrid of :doc:`emulation` and :doc:`rtl-simulation`: the CPU,
interconnect, on-chip RAM, flash, and a few timing-sensitive blocks (UART
console, PLIC, machine timer) stay as fast Renode functional models, while the
**peripherals run as real Verilated RTL**. This lets you develop and validate a
driver against the actual hardware description without paying for a full-SoC RTL
simulation - Renode's system bus simply routes each peripheral's address window
to its Verilated model.

Each co-simulated peripheral is one Verilated library built from a ``TileLink*``
module of the generated SoC netlist. The shared C++ integration (a TileLink-UL
bus driver and a generic entry point) lives in Zibal under
``hardware/renode/cosim/``; the platform wiring is
``hardware/scala/<soc>/<soc>_cosim.repl``.

Building
********

Co-simulation requires the generated netlist (run the relevant flow's
``asic:prepare``/``fpga:prepare`` step first) and the
``renode-verilator-integration`` checkout provided by the manifest. One library
per peripheral is verilated and compiled automatically the first time you run
co-simulation (see `Running`_ below).

The libraries are produced under ``build/<SOC>/<board>/cosim/<peripheral>/``.

Running
*******

``sim:cosim`` rebuilds the libraries (incrementally - unchanged peripherals are a
no-op) and boots the firmware::

    task sim:cosim SOC=ElemRV-H TARGET=SG13CMOS5L

For the full Renode GUI, add ``gui=1``::

    task sim:cosim gui=1 SOC=ElemRV-H TARGET=SG13CMOS5L

The UART console (GUI window and host TCP socket on port 3456) works exactly as
in :doc:`emulation`.

When to use
***********

- Validating a peripheral driver against the real RTL with a fast boot.
- Catching driver/RTL mismatches that a functional model would hide.

Notes
*****

- Only peripherals are Verilated; the CPU and bus remain functional, so this is
  not a substitute for full-SoC cycle accuracy (:doc:`rtl-simulation`).
- ``uart0``, the PLIC, and the machine timer are intentionally kept functional.
- The netlist already encodes each SoC's parameters, so no Verilog is generated
  or committed - co-simulation consumes the build artifact directly.
