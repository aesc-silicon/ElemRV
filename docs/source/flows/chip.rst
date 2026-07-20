Chip Flow
#########

The chip flow takes the design from RTL to a GDSII layout ready for tape-out,
using the OpenROAD flow exclusively with open-source tools. The default target
is the IHP SG13G2 130 nm BiCMOS open PDK.

Three Taskfile variables control which platform is built and how:

- ``SOC`` - the platform to build (default: ``ElemRV-N``)
- ``TARGET`` - the ASIC target / process node (default: ``SG13G2``)
- ``FLOORPLAN`` - the floorplan profile (default: ``relaxed``)

To build a different platform, prepend the variables to any task::

    task SOC=ElemRV-H TARGET=SG13G2 asic:prepare

Floorplan Profiles
******************

Chip targets provide two floorplan profiles, selected via ``FLOORPLAN``:

- ``relaxed`` (default) - an oversized core (~60% utilization) that converges
  fast and reliably. Use it for day-to-day RTL iteration. The generate step
  prints a warning to the logs, because utilization, timing, and congestion
  results are not tape-out representative.
- ``tapeout`` - the tight signoff floorplan (~75-80% utilization). All numbers
  used for signoff must come from this profile::

    task SOC=ElemRV-H TARGET=SG13CMOS5L FLOORPLAN=tapeout

The die and core sizes per profile are maintained with the ``update-chip-size``
skill, which recalculates them from the density reported by a place-and-route
run.

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

    task asic:prepare

**2. Build**

Runs the full OpenROAD place-and-route flow to produce the chip layout::

    task asic:build

If this step fails, check the `Known Issues`_ section below.

**3. Fill**

Inserts filler cells and metal fill into the layout to meet density
requirements::

    task asic:fill

Viewing the Layout
******************

Open the finished layout in KLayout::

    task asic:klayout

Open it in OpenROAD for further analysis::

    task asic:openroad

To inspect a specific intermediate stage rather than the final result, pass the
``stage`` argument::

    task asic:openroad stage=6_final

Design Rule Checks
******************

Run DRC on the finished layout::

    task asic:drc

To run a faster minimal check first::

    task asic:drc level=minimal

Open the DRC results in KLayout::

    task asic:klayout mode=drc
    task asic:klayout mode=drc level=minimal

Log Checks
**********

Scan the build logs for warnings and errors after any flow step::

    task asic:check-logs

Known Issues
************

- **X server** - If ``asic:klayout`` or ``asic:openroad`` fails with an X
  server permission error, grant access with::

      xhost +si:localuser:$USER
