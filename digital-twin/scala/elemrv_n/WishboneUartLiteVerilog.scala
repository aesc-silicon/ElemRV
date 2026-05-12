// SPDX-FileCopyrightText: 2025 aesc silicon
//
// SPDX-License-Identifier: CERN-OHL-W-2.0

package elemrv_n.test

import spinal.core._
import spinal.lib._
import spinal.lib.bus.wishbone._
import nafarr.peripherals.com.uart._

// Generator for lightweight WishboneUart Verilog for ElemRV-N co-simulation
// Lightweight: TX/RX only (no CTS/RTS flow control)
object WishboneUartLiteVerilog extends App {
  val uartConfig = UartCtrl.Parameter.lightweight()

  // UART needs clock frequency for baud rate divider calculation.
  // ElemRV-N peripheral clock is 30 MHz (upstream rework on clock tree).
  SpinalConfig(
    targetDirectory = "gen_n",
    defaultConfigForClockDomains = ClockDomainConfig(resetKind = ASYNC, resetActiveLevel = LOW),
    defaultClockDomainFrequency = FixedFrequency(30 MHz)
  ).generateVerilog(
    WishboneUart(
      parameter = uartConfig,
      busConfig = WishboneConfig(32, 32)
    )
  )
}
