Nonmetal Platforms
##################

Chips based on the Nonmetal group are designed for embedded tasks.
They always come without MMU and are designed for Zephyr RTOS.

Platforms are ordered by atomic number, which reflects their capability
tier. Each platform is a strict superset of the one before it.

Feature Overview
****************

.. list-table::
   :header-rows: 2
   :stub-columns: 1

   * -
     - H
     - C
     - N
     - O
     - P
     - S
   * -
     - Hydrogen
     - Carbon
     - Nitrogen
     - Oxygen
     - Phosphorus
     - Sulfur
   * - **CPU**
     -
     -
     -
     -
     -
     -
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
   * - **Memory**
     -
     -
     -
     -
     -
     -
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
   * - **Interconnect**
     -
     -
     -
     -
     -
     -
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
   * - **DMA**
     -
     -
     -
     -
     -
     -
   * - Channels
     -
     -
     - TBD
     - TBD
     - TBD
     - TBD
   * - **Interrupt Controller**
     -
     -
     -
     -
     -
     -
   * - PLIC sources
     - ✓
     - ✓
     - ✓
     - ✓
     - ✓
     - ✓
   * - **Debug**
     -
     -
     -
     -
     -
     -
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
   * - **Power**
     -
     -
     -
     -
     -
     -
   * - Clock domains
     - 1
     - 1
     - TBD
     - TBD
     - TBD
     - TBD


.. toctree::
   :maxdepth: 2
   :caption: Platforms:

   elemrv_h.rst
