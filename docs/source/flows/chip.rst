Chip Flow
#########

The chip flow takes the design from RTL to a GDSII layout ready for tape-out,
using the OpenROAD flow exclusively with open-source tools. The default target
is the IHP SG13G2 130 nm BiCMOS open PDK.

Two Taskfile variables control which platform is built:

- ``SOC`` - the platform to build (default: ``ElemRV-N``)
- ``board`` - the process node target (default: ``SG13G2``)

To build a different platform, prepend the variables to any task::

    task SOC=ElemRV-H board=SG13G2 prepare

Full Flow
*********

The default task runs the complete RTL-to-GDSII flow in one step, including
layout, filler insertion, DRC, and log checks::

    task

Step by Step
************

**1. Prepare**

Generates the Verilog netlist, sealring, and copies bondpad macros into the
build directory::

    task prepare

**2. Layout**

Runs the full OpenROAD place-and-route flow to produce the chip layout::

    task layout

If this step fails, check the `Known Issues`_ section below.

**3. Filler**

Inserts filler cells and metal fill into the layout to meet density
requirements::

    task filler

Viewing the Layout
******************

Open the finished layout in KLayout::

    task view-klayout

Open it in OpenROAD for further analysis::

    task view-openroad

To inspect a specific intermediate stage rather than the final result, pass the
``stage`` argument::

    task view-openroad stage=6_final

Design Rule Checks
******************

Run DRC on the finished layout::

    task run-drc

To run a faster minimal check first::

    task run-drc level=minimal

Open the DRC results in KLayout::

    task view-drc
    task view-drc level=minimal

Log Checks
**********

Scan the build logs for warnings and errors after any flow step::

    task check-logs

Known Issues
************

- **X server** - If ``view-klayout`` or ``view-openroad`` fails with an X
  server permission error, grant access with::

      xhost +si:localuser:$USER
