Nonmetal Platforms
##################

Chips based on the Nonmetal group are designed for embedded tasks.
They always come without MMU and are designed for Zephyr RTOS.

Platforms are ordered by atomic number, which reflects their capability
tier. Each platform is a strict superset of the one before it.

Feature Overview
****************

All platforms use a VexiiRiscv CPU. Higher-tier variants add caches, branch
prediction, and wider execution pipelines for increased performance.

.. list-table:: CPU
   :header-rows: 1
   :stub-columns: 1
   :align: left

   * -
     - Hydrogen
     - Carbon
     - Nitrogen
     - Oxygen
     - Phosphorus
     - Sulfur
   * - I-Cache
     -
     - ✓
     - ✓
     - ✓
     - ✓
     - ✓
   * - D-Cache
     -
     -
     - ✓
     - ✓
     - ✓
     - ✓
   * - Branch prediction
     -
     -
     - ✓
     - ✓
     - ✓
     - ✓
   * - Dual-issue
     -
     -
     -
     -
     - ✓
     - ✓
   * - FPU
     -
     -
     -
     -
     -
     - ✓
   * - Privilege levels
     - M
     - M
     - M+U
     - M+U
     - M+U
     - M+U

.. list-table:: RISC-V Extensions
   :header-rows: 1
   :stub-columns: 1
   :align: left

   * -
     - Hydrogen
     - Carbon
     - Nitrogen
     - Oxygen
     - Phosphorus
     - Sulfur
   * - M (multiply/divide)
     -
     - ✓
     - ✓
     - ✓
     - ✓
     - ✓
   * - A (atomics)
     -
     -
     -
     - ✓
     - ✓
     - ✓
   * - F (single-precision FPU)
     -
     -
     -
     -
     -
     - ✓
   * - D (double-precision FPU)
     -
     -
     -
     -
     -
     - ✓
   * - C (compressed)
     -
     - ✓
     - ✓
     - ✓
     - ✓
     - ✓
   * - Zba/Zbb/Zbc/Zbs (bit manipulation)
     -
     -
     -
     - ✓
     - ✓
     - ✓
   * - Zicbom (cache-block management)
     -
     -
     -
     - ✓
     - ✓
     - ✓
   * - Zicsr
     - ✓
     - ✓
     - ✓
     - ✓
     - ✓
     - ✓
   * - Zifencei
     - ✓
     - ✓
     - ✓
     - ✓
     - ✓
     - ✓

Every platform includes on-chip SRAM and boots from external SPI Flash in XIP
mode. HyperRAM is available starting with Nitrogen. Tightly-coupled memory is
planned.

.. list-table:: Memory
   :header-rows: 1
   :stub-columns: 1
   :align: left

   * -
     - Hydrogen
     - Carbon
     - Nitrogen
     - Oxygen
     - Phosphorus
     - Sulfur
   * - On-chip SRAM
     - ✓
     - ✓
     - ✓
     - ✓
     - ✓
     - ✓
   * - TCM
     -
     -
     -
     - TBD
     - TBD
     - TBD
   * - SPI Flash XIP
     - ✓
     - ✓
     - ✓
     - ✓
     - ✓
     - ✓
   * - HyperRAM
     -
     -
     - ✓
     - ✓
     - ✓
     - ✓

Simpler variants use a shared bus, while higher-tier platforms switch to a
crossbar for better throughput. Starting with Phosphorus, the memory side of the
crossbar is 64 bits wide by default (32 to 128 bits); the peripheral bus stays
32 bits wide on every platform.

.. list-table:: Interconnect
   :header-rows: 1
   :stub-columns: 1
   :align: left

   * -
     - Hydrogen
     - Carbon
     - Nitrogen
     - Oxygen
     - Phosphorus
     - Sulfur
   * - Shared bus
     - ✓
     - ✓
     -
     -
     -
     -
   * - Crossbar
     -
     -
     - ✓
     - ✓
     - ✓
     - ✓
   * - Memory bus width
     - 32 bit
     - 32 bit
     - 32 bit
     - 32 bit
     - 64 bit
     - 64 bit

A DMA controller is available starting with Oxygen.

.. list-table:: DMA
   :header-rows: 1
   :stub-columns: 1
   :align: left

   * -
     - Hydrogen
     - Carbon
     - Nitrogen
     - Oxygen
     - Phosphorus
     - Sulfur
   * - Channels
     -
     -
     -
     - 4
     - 4
     - 4

All platforms include a PLIC for interrupt handling.

.. list-table:: Interrupt Controller
   :header-rows: 1
   :stub-columns: 1
   :align: left

   * -
     - Hydrogen
     - Carbon
     - Nitrogen
     - Oxygen
     - Phosphorus
     - Sulfur
   * - PLIC sources
     - ✓
     - ✓
     - ✓
     - ✓
     - ✓
     - ✓

JTAG debug is available on every platform. Higher-tier variants add trace
support for real-time instruction tracing.

.. list-table:: Debug
   :header-rows: 1
   :stub-columns: 1
   :align: left

   * -
     - Hydrogen
     - Carbon
     - Nitrogen
     - Oxygen
     - Phosphorus
     - Sulfur
   * - JTAG
     - ✓
     - ✓
     - ✓
     - ✓
     - ✓
     - ✓
   * - Trace
     -
     -
     -
     - TBD
     - TBD
     - TBD

Hydrogen and Carbon run on a single clock domain. Advanced power management
for higher-tier platforms is planned.

.. list-table:: Power
   :header-rows: 1
   :stub-columns: 1
   :align: left

   * -
     - Hydrogen
     - Carbon
     - Nitrogen
     - Oxygen
     - Phosphorus
     - Sulfur
   * - Clock domains
     - 1
     - 1
     - 4
     - 4
     - 4
     - 4


Performance
***********

CoreMark 1.0, measured on FPGA with the port in ``software/<soc>/coremark``.
Scores are dominated by memory and ISA rather than by the pipeline: Hydrogen has
no instruction cache and no hardware multiply, so every fetch is a SPI flash
transaction and every multiply a libgcc call. Carbon adds both, which accounts
for almost all of its advantage. Nitrogen's further gain comes from the data
cache, branch prediction and HyperRAM, and is much smaller because the core is
no longer starved for instructions.

.. list-table:: CoreMark
   :header-rows: 1
   :stub-columns: 1
   :align: left

   * -
     - CoreMark/MHz
     - Iterations/s
     - Clock
     - ISA
     - Executed from
   * - Hydrogen
     - 0.005
     - 0.24
     - 50 MHz
     - rv32i_zicsr_zifencei
     - XIP flash
   * - Carbon
     - 1.590
     - 79.48
     - 50 MHz
     - rv32imc_zicsr_zifencei
     - XIP flash
   * - Nitrogen
     - 2.360
     - 70.80
     - 30 MHz
     - rv32imc_zicsr_zifencei_zicntr_zihpm
     - HyperRAM
   * - Oxygen
     - 2.419
     - 72.56
     - 30 MHz
     - rv32imac_zicsr_zifencei_zicntr_zihpm_zba_zbb_zbc_zbs_zicbom
     - HyperRAM
   * - Phosphorus
     - 3.149
     - 94.47
     - 30 MHz
     - rv32imac_zicsr_zifencei_zicntr_zihpm_zba_zbb_zbc_zbs_zicbom
     - HyperRAM
   * - Sulfur
     -
     -
     -
     -
     -

Built with GCC 14.3.0 and ``-O2 -march=<isa> -mabi=ilp32``, ``MEM_METHOD=MEM_STACK``,
iterations auto-scaled so every run exceeds the ten seconds EEMBC requires. The
ports have no floating point, so CoreMark's own ``Iterations/Sec`` line is
computed from a truncated whole-second time; the figures above are derived from
the raw tick count and timer frequency that ``portable_fini()`` prints.

Nitrogen additionally reports ``mcycle`` and ``minstret``, giving 423,714 cycles
and 313,653 instructions per iteration, an IPC of 0.74. Its cycle count matches
the machine timer to seven digits, confirming that the CPU and the timer share
one clock.

Oxygen runs the same pipeline and memory system, so its 2.5% gain over Nitrogen
comes from the bit manipulation instructions alone: 302,672 instructions per
iteration (3.5% fewer) in 413,473 cycles, an IPC of 0.73. Atomics and the DMA
controller do not affect CoreMark, which has no atomic operations and no bulk
transfers in its timed loop.

Phosphorus executes the same 302,672 instructions per iteration as Oxygen, so
its 30% gain comes from the pipeline and the memory system alone: dual issue,
the 64-bit memory bus and the 512 B GShare bring an iteration down to 317,571
cycles (23% fewer), an IPC of 0.95. Of that, the larger GShare accounts for 1.6%
fewer cycles than the 256 B table of Oxygen.

See the available platforms for detailed specifications and usage instructions.

.. toctree::
   :maxdepth: 2

   elemrv_h.rst
   elemrv_c.rst
   elemrv_n.rst
   elemrv_o.rst
   elemrv_p.rst
   elemrv_s.rst
