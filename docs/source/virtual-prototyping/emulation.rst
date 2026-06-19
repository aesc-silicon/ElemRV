Emulation
#########

Emulation runs the platform in `Renode <https://renode.io>`_ using fast
*functional models* of the CPU and peripherals - no RTL is involved. It boots a
firmware image in a fraction of a second and runs at near real-time, which makes
it the tool of choice for software bring-up and rapid iteration.

The functional peripheral models live in Nafarr next to their SpinalHDL sources
(``*.cs`` files alongside the ``*.scala``); the platform wiring is described by
``hardware/scala/<soc>/<soc>_emul.repl`` and the boot script
``<soc>_emul.resc``.

Running
*******

Boot the platform's firmware image headless (console in the terminal)::

    task emulate SOC=ElemRV-H TARGET=SG13CMOS5L

To launch the full Renode GUI (monitor window plus peripheral analyzers, e.g. an
LED widget)::

    task emulate-gui SOC=ElemRV-H TARGET=SG13CMOS5L

The firmware image is taken from ``build/<SOC>/firmware/`` - build it with the
:doc:`../flows/firmware` flow first.

Console and I/O
***************

UART0 is exposed both as a Renode analyzer window (GUI) and as a TCP socket on
port 3456, which podman-compose publishes to the host. Connect from the host
with::

    telnet localhost 3456

Typing into either endpoint feeds the UART receiver. GPIO output changes are
written to the Renode log, so pin activity is visible even without the GUI.

When to use
***********

- Day-to-day firmware development and debugging (GDB, scripting, fast reboots).
- Bring-up that does not depend on exact hardware timing.

Because the peripherals are hand-written functional models, behaviour is only as
accurate as the model. To run software against the **real** peripheral RTL while
keeping this speed, use :doc:`co-simulation`; for full cycle accuracy use
:doc:`rtl-simulation`.
