RTL Simulation
##############

RTL simulation runs the **complete SoC netlist** in `Verilator
<https://www.veripool.org/verilator/>`_, cycle by cycle. It is the most accurate
way to run the design: every clock edge of the real RTL is evaluated, and the
run can be inspected as waveforms. It is correspondingly the slowest approach,
and is aimed at verifying the *hardware* rather than iterating on software.

Running
*******

To simulate the configured platform::

    task simulate SOC=ElemRV-H TARGET=SG13CMOS5L

By default the simulation runs to completion. Limit it to a fixed duration in
milliseconds with ``duration``::

    task simulate SOC=ElemRV-H TARGET=SG13CMOS5L duration=100

Waveforms
*********

Open the recorded waveforms in GTKWave after a run::

    task view-simulation SOC=ElemRV-H TARGET=SG13CMOS5L

When to use
***********

- Verifying RTL behaviour and exact cycle-level timing of the whole SoC.
- Debugging hardware issues that require signal-level visibility.

For software development prefer :doc:`emulation` (much faster), and to exercise
a driver against a single peripheral's real RTL without simulating the entire
chip, use :doc:`co-simulation`.
