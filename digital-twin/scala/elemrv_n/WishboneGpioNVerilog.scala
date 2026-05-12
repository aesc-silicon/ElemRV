// SPDX-FileCopyrightText: 2025 aesc silicon
//
// SPDX-License-Identifier: CERN-OHL-W-2.0

package elemrv_n.test

import spinal.core._
import spinal.lib._
import spinal.lib.bus.wishbone._
import nafarr.peripherals.io.gpio._

// Generator for WishboneGpio Verilog for ElemRV-N co-simulation
// 20 GPIO pins, 3 interrupt triggers (matching ElemRV-N configuration)
object WishboneGpioNVerilog extends App {
  val gpioConfig = GpioCtrl.Parameter(Gpio.Parameter(20), 3, null, null, null)

  SpinalConfig(
    targetDirectory = "gen_n",
    defaultConfigForClockDomains = ClockDomainConfig(resetKind = ASYNC, resetActiveLevel = LOW)
  ).generateVerilog(
    WishboneGpio(
      parameter = gpioConfig,
      busConfig = WishboneConfig(32, 32)
    )
  )
}
