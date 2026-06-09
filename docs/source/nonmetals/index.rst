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
   * - Out-of-order
     -
     -
     -
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
     - ✓
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
     - ✓
     - ✓
     - ✓
     - ✓
   * - F (single-precision FPU)
     -
     -
     -
     -
     - ✓
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
   * - Zba/Zbb/Zbs (bit manipulation)
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
mode. Starting with Nitrogen, HyperRAM and tightly-coupled memory are available.

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
     - ✓
     - ✓
     - ✓
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
crossbar for better throughput.

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

A DMA controller is available starting with Nitrogen.

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
     - TBD
     - TBD
     - TBD
     - TBD

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
     - ✓
     - ✓
     - ✓

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
     - TBD
     - TBD
     - TBD
     - TBD


See the available platforms for detailed specifications and usage instructions.

.. toctree::
   :maxdepth: 2

   elemrv_h.rst
