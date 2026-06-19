FPGA Flow
#########

The FPGA flow synthesizes the design for the `ECPIX5
<https://shop.lambdaconcept.com/home/46-ecpix-5.html>`_ development board,
which carries a Lattice ECP5 (um5g-45k, CABGA554) running at 100 MHz. It is the
primary way to prototype and verify an ElemRV platform before tape-out.

The flow uses Yosys for synthesis and nextpnr for place-and-route. Flashing
uses openFPGALoader targeting the ECPIX5 revision 3.

Full Flow
*********

To run prepare, synthesize, and flash in one step::

    task fpga SOC=ElemRV-H TARGET=SG13CMOS5L

Step by Step
************

**1. Prepare**

Generates the Verilog netlist and all metadata required for FPGA synthesis::

    task fpga:prepare SOC=ElemRV-H TARGET=SG13CMOS5L

**2. Synthesize**

Runs Yosys synthesis followed by nextpnr place-and-route to produce the
bitstream::

    task fpga:synthesize SOC=ElemRV-H TARGET=SG13CMOS5L

**3. Flash**

Programs the ECP5 with the generated bitstream over USB::

    task fpga:flash SOC=ElemRV-H TARGET=SG13CMOS5L

Simulation
**********

The design can be simulated at RTL level using Verilator before flashing::

    task fpga:simulate SOC=ElemRV-H TARGET=SG13CMOS5L

By default the simulation runs until it finishes. To limit the run to a
specific duration, pass ``duration`` in milliseconds::

    task fpga:simulate SOC=ElemRV-H TARGET=SG13CMOS5L duration=100

To inspect the waveforms afterwards, open the simulation output in GTKWave::

    task fpga:view-simulation SOC=ElemRV-H TARGET=SG13CMOS5L
