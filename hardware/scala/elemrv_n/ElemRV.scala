// SPDX-FileCopyrightText: 2025 aesc silicon
//
// SPDX-License-Identifier: CERN-OHL-W-2.0

package elemrv_n

import spinal.core._
import spinal.lib._

import zibal.misc._
import zibal.platform.Nitrogen
import zibal.board.BoardParameter
import zibal.soc.SocParameter

import nafarr.peripherals.io.gpio.{WishboneGpio, Gpio, GpioCtrl}
import nafarr.peripherals.io.pio.{WishbonePio, Pio, PioCtrl}
import nafarr.peripherals.io.pwm.{WishbonePwm, Pwm, PwmCtrl}
import nafarr.peripherals.com.uart.{WishboneUart, Uart, UartCtrl}
import nafarr.peripherals.com.i2c.{WishboneI2cController, I2c, I2cControllerCtrl}
import nafarr.peripherals.com.spi.{WishboneSpiController, Spi, SpiControllerCtrl}
import nafarr.peripherals.pinmux.{WishbonePinmux, Pinmux, PinmuxCtrl}

object ElemRV {
  def apply(parameter: Nitrogen.Parameter) = ElemRV(parameter)

  case class Parameter(boardParameter: BoardParameter)
      extends SocParameter(boardParameter, socInterrupts = 6) {
    val gpio0 = GpioCtrl.Parameter(Gpio.Parameter(20), 3, null, null, null)
    val i2c0 = I2cControllerCtrl.Parameter.default(1)
    val i2c1 = I2cControllerCtrl.Parameter.lightweight()
    val pio0 = PioCtrl.Parameter.default(3)
    val pwm0 = PwmCtrl.Parameter.default(2)
    val spi0 = SpiControllerCtrl.Parameter.default()
    val uart0 = UartCtrl.Parameter.full()
    val uart1 = UartCtrl.Parameter.lightweight()
    val pinmux = PinmuxCtrl.Parameter(Pinmux.Parameter(20), 40, 2)
  }

  case class ElemRV(parameter: Nitrogen.Parameter) extends Nitrogen.Nitrogen(parameter) {
    var socParameter = parameter.getSocParameter.asInstanceOf[Parameter]
    val io = new Bundle {
      val pins = Pinmux.Io(socParameter.pinmux.io)
    }

    val peripherals = new ClockingArea(clockCtrl.getClockDomainByName("system")) {

      val gpio0Ctrl = WishboneGpio(socParameter.gpio0, system.wishboneConfig)
      addPeripheralDevice(gpio0Ctrl.io.bus, 0x0000, 4 kB)
      addInterrupt(gpio0Ctrl.io.interrupt)
      for (pin <- 0 until socParameter.gpio0.io.width) {
        addPinmuxInput(gpio0Ctrl.io.gpio.pins(pin), s"gpio0_$pin")
      }

      val i2c0Ctrl = WishboneI2cController(socParameter.i2c0, system.wishboneConfig)
      addPeripheralDevice(i2c0Ctrl.io.bus, 0x1000, 4 kB)
      addInterrupt(i2c0Ctrl.io.interrupt)
      addPinmuxInput(i2c0Ctrl.io.i2c.scl, "i2c0_scl")
      addPinmuxInput(i2c0Ctrl.io.i2c.sda, "i2c0_sda")
      addPinmuxInput(i2c0Ctrl.io.i2c.interrupts(0), "i2c0_interrupt_0", output = false)

      val i2c1Ctrl = WishboneI2cController(socParameter.i2c1, system.wishboneConfig)
      addPeripheralDevice(i2c1Ctrl.io.bus, 0x2000, 4 kB)
      addInterrupt(i2c1Ctrl.io.interrupt)
      addPinmuxInput(i2c1Ctrl.io.i2c.scl, "i2c1_scl")
      addPinmuxInput(i2c1Ctrl.io.i2c.sda, "i2c1_sda")

      val pio0Ctrl = WishbonePio(socParameter.pio0, system.wishboneConfig)
      addPeripheralDevice(pio0Ctrl.io.bus, 0x3000, 4 kB)
      for (pin <- 0 until socParameter.pio0.io.width) {
        addPinmuxInput(pio0Ctrl.io.pio.pins(pin), s"pio0_$pin")
      }

      val pwm0Ctrl = WishbonePwm(socParameter.pwm0, system.wishboneConfig)
      addPeripheralDevice(pwm0Ctrl.io.bus, 0x4000, 4 kB)
      for (pin <- 0 until socParameter.pwm0.io.channels) {
        addPinmuxInput(pwm0Ctrl.io.pwm.output(pin), s"pwm0_$pin")
      }

      val spi0Ctrl = WishboneSpiController(socParameter.spi0, system.wishboneConfig)
      addPeripheralDevice(spi0Ctrl.io.bus, 0x5000, 4 kB)
      addInterrupt(spi0Ctrl.io.interrupt)
      addPinmuxInput(spi0Ctrl.io.spi.cs(0), "spi0_cs0")
      addPinmuxInput(spi0Ctrl.io.spi.sclk, "spi0_sclk")
      addPinmuxInput(spi0Ctrl.io.spi.dq(0), "spi0_dq0")
      addPinmuxInput(spi0Ctrl.io.spi.dq(1), "spi0_dq1")

      val uart0Ctrl = WishboneUart(socParameter.uart0, system.wishboneConfig)
      addPeripheralDevice(uart0Ctrl.io.bus, 0x6000, 4 kB)
      addInterrupt(uart0Ctrl.io.interrupt)
      addPinmuxInput(uart0Ctrl.io.uart.txd, "uart0_tx")
      addPinmuxInput(uart0Ctrl.io.uart.rxd, "uart0_rx", output = false)
      addPinmuxInput(uart0Ctrl.io.uart.cts, "uart0_cts", output = false)
      addPinmuxInput(uart0Ctrl.io.uart.rts, "uart0_rts")

      val uart1Ctrl = WishboneUart(socParameter.uart1, system.wishboneConfig)
      addPeripheralDevice(uart1Ctrl.io.bus, 0x7000, 4 kB)
      addInterrupt(uart1Ctrl.io.interrupt)
      addPinmuxInput(uart1Ctrl.io.uart.txd, "uart1_tx")
      addPinmuxInput(uart1Ctrl.io.uart.rxd, "uart1_rx", output = false)
      uart1Ctrl.io.uart.cts := True

      /* Pin Mapping */
      addPinmuxOption(0, List("gpio0_0", "i2c0_scl"))
      addPinmuxOption(1, List("gpio0_1", "i2c0_sda"))
      addPinmuxOption(2, List("gpio0_2", "i2c0_interrupt_0"))
      addPinmuxOption(3, List("gpio0_3", "i2c1_scl"))
      addPinmuxOption(4, List("gpio0_4", "i2c1_sda"))
      addPinmuxOption(5, List("gpio0_5", "pio0_0"))
      addPinmuxOption(6, List("gpio0_6", "pio0_1"))
      addPinmuxOption(7, List("gpio0_7", "pio0_2"))
      addPinmuxOption(8, List("gpio0_8", "pwm0_0"))
      addPinmuxOption(9, List("gpio0_9", "pwm0_1"))
      addPinmuxOption(10, List("gpio0_10", "spi0_cs0"))
      addPinmuxOption(11, List("gpio0_11", "spi0_sclk"))
      addPinmuxOption(12, List("gpio0_12", "spi0_dq0"))
      addPinmuxOption(13, List("gpio0_13", "spi0_dq1"))
      addPinmuxOption(14, List("uart0_tx", "gpio0_14"))
      addPinmuxOption(15, List("uart0_rx", "gpio0_15"))
      addPinmuxOption(16, List("uart0_cts", "gpio0_16"))
      addPinmuxOption(17, List("uart0_rts", "gpio0_17"))
      addPinmuxOption(18, List("gpio0_18", "uart1_tx"))
      addPinmuxOption(19, List("gpio0_19", "uart1_rx"))

      val pinmuxCtrl =
        WishbonePinmux(socParameter.pinmux, getPinmuxMapping(), system.wishboneConfig)
      addPeripheralDevice(pinmuxCtrl.io.bus, 0x10000, 4 kB)
      io.pins <> pinmuxCtrl.io.pins
      connectPinmuxInputs(pinmuxCtrl)

      connectPeripherals()
    }
  }
}
