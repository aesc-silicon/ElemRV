<!--
SPDX-FileCopyrightText: 2026 aesc silicon

SPDX-License-Identifier: CERN-OHL-W-2.0
-->

# Contributing to ElemRV

Thanks for your interest. ElemRV is an end-to-end open-source RISC-V MCU platform:
SpinalHDL to GDSII, with no proprietary tools anywhere in the flow. Contributions
of every size are welcome — a corrected register offset in the documentation is
as useful as a new IP core.

This guide lives in the ElemRV repository and covers all of the project's
repositories.

## Which repository does my change belong in?

This is the first thing to get right, and the easiest thing to get wrong. The
project is split across four repositories that are assembled into one workspace:

| I want to change | Repository | Path in the workspace |
| --- | --- | --- |
| An IP core, its register map, its tests, its documentation, its bare-metal driver or its Renode model | [elements-nafarr](https://github.com/aesc-silicon/elements-nafarr) | `modules/elements/zibal/ext/nafarr` |
| A platform (Hydrogen, Carbon, Nitrogen), the interconnect, or the EDA/build tooling — OpenROAD, Lattice, OpenOCD, report generators | [elements-zibal](https://github.com/aesc-silicon/elements-zibal) | `modules/elements/zibal` |
| An SoC variant, a chip top level, a Taskfile flow, the platform documentation, or board bring-up | **ElemRV** (this one) | the workspace root |
| A Zephyr driver, board or devicetree binding | [elements-zephyr](https://github.com/aesc-silicon/elements-zephyr) | `modules/elements/zephyr` |

A rule of thumb: if it describes *a piece of hardware that could be reused in
another chip*, it belongs in Nafarr. If it describes *how chips are assembled or
built*, it belongs in Zibal. If it describes *a specific chip*, it belongs here.

Zephyr changes are special: `elements-zephyr` is a fork, and everything in it is
meant to go upstream to
[zephyrproject-rtos/zephyr](https://github.com/zephyrproject-rtos/zephyr).
Please follow Zephyr's own contribution guidelines for those, and expect to open
the pull request against upstream Zephyr rather than the fork.

If you are unsure, open the issue anyway and say so — we would rather move a
ticket than lose it.

## Setting up

You need `podman`, `virtualenv`, `curl` and Taskfile on the host. Everything else —
the toolchains, OpenROAD, Yosys, nextpnr, Renode, the Zephyr SDK — lives inside a
container that the setup task builds for you. There is no need to install any EDA
tool locally.

```bash
sudo apt install virtualenv curl podman
sudo sh -c "$(curl --location https://taskfile.dev/install.sh)" -- -d -b /usr/local/bin

git clone https://github.com/aesc-silicon/ElemRV.git
cd ElemRV
task install          # fetches the other repositories and builds the container
task install-zephyr   # only needed if you are working on firmware
```

`task install` uses Google's `repo` tool with `manifest.xml` to fetch Nafarr,
Zibal, VexiiRiscv, the PDKs and the OpenROAD flow scripts into the workspace.
`task install-zephyr` initialises the west workspace on top.

List everything you can run with:

```bash
task -a
```

Most tasks take `SOC` and `TARGET`. For example:

```bash
task sim:simulate SOC=ElemRV-H            # RTL simulation
task sim:emulate  SOC=ElemRV-H            # Renode, no hardware needed
task fpga:prepare fpga:synthesize SOC=ElemRV-H
task asic:prepare SOC=ElemRV-H TARGET=SG13CMOS5L
```

On a headless machine, prefix tasks that would otherwise want an X server with
`IS_HEADLESS=true`.

If a step in this section does not work for you, that is a bug in the
documentation — please report it.

## Before you open a pull request

Run the checks that CI runs. All of them are fast except the simulations.

**Scala formatting** — enforced in every repository:

```bash
sbt scalafmt        # fix
sbt scalafmtCheck   # what CI runs
```

**Licensing** — enforced by `reuse lint` in CI. Every file needs an
`SPDX-FileCopyrightText` and an `SPDX-License-Identifier` header, or an entry in
`REUSE.toml`. The project uses two licences and the boundary matters:

* `CERN-OHL-W-2.0` for hardware, tooling and documentation
* `Apache-2.0` for software — bare-metal drivers, firmware, Zephyr code

Copy the header style from a neighbouring file in the same directory.

```bash
reuse lint
```

**Documentation** builds with Sphinx and is checked in CI:

```bash
pip install -r docs/requirements.txt
sphinx-build -b html docs/source docs/build/html
```

Note that `docs/source/hardware/` is a symlink into Nafarr, so the Nafarr
repository has to be present in the workspace for the build to resolve.

## Coding style

**SpinalHDL / Scala.** `scalafmt` handles most of it, with two rules it cannot
enforce:

* Never pad `=` or `:=` with extra spaces to align them across lines. Exactly one
  space on each side.
* A Scala `if` used directly as the right-hand side of a `:=` must be wrapped in
  parentheses, or the parser rejects it:
  `d.data := (if (cond) a else b)`

**C.** Bare-metal drivers in Nafarr follow the style of the surrounding files.
Zephyr code follows
[Zephyr's coding style](https://docs.zephyrproject.org/latest/contribute/style/index.html)
— tabs, 100 columns, and `scripts/ci/check_compliance.py` must pass.

**Comments.** Explain why something is the way it is, not what the next line
does. A comment that says a delay exists is noise; a comment that says the delay
is 38 us because the external flash needs that long to recover from reset is worth
keeping.

## Commits and pull requests

Commit subjects use the area prefix already visible in the history:

```
hardware: elemrv_h: Add clock controller
Taskfile: asic: Add LVS task
docs: non-metals: Fix platform overview
```

Keep one logical change per commit — a build fix and a feature do not belong
together. Write a body explaining *why*, wrapped at about 72 columns.

Every commit needs a `Signed-off-by:` line, certifying the
[Developer Certificate of Origin](https://developercertificate.org/):

```bash
git commit -s
```

In the pull request, please say what you tested and how. For hardware changes
that means naming the target — RTL simulation, Renode, FPGA or silicon — because
those are very different levels of evidence. "Builds and boots on an ECPIX5" tells
a reviewer far more than "tested".

If you do not have hardware, say so. Somebody with a board can run the on-target
part; that is not a reason to hold back a change.

## Good first contributions

Issues labelled `good first issue` are scoped so that they can be finished without
a deep tour of the codebase. A few categories are particularly approachable:

* **Renode models** — a functional model of an IP core is self-contained C#. You
  need no SpinalHDL knowledge; the register map comes from the IP's `Regs` class.
* **Bare-metal driver tests** — plain C, run on the host, no hardware needed.
* **Documentation** — several IP cores and one SoC have no page yet, and a few
  register tables have drifted from the RTL.

## Reporting bugs

Open an issue using the bug report template. For hardware behaviour, the details
that matter most are: which SoC, which target (simulation, Renode, FPGA, silicon),
and the exact commands you ran. A hang on an FPGA and a hang in simulation usually
have completely different causes, and we cannot tell which one you have without
that context.

## Questions

Open a
[discussion](https://github.com/aesc-silicon/ElemRV/discussions) if you
are not sure whether something is a bug, want to propose a larger change, or are
looking for a place to start.
