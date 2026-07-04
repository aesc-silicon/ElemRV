// SPDX-FileCopyrightText: 2025 aesc silicon
//
// SPDX-License-Identifier: CERN-OHL-W-2.0

package elemrv_h

import spinal.core._
import spinal.lib._

import zibal.misc._
import zibal.platform.Hydrogen
import zibal.board.BoardParameter
import zibal.soc.SocParameter

import nafarr.peripherals.io.gpio.{TileLinkGpio, Gpio, GpioCtrl}
import nafarr.peripherals.io.pio.{TileLinkPio, Pio, PioCtrl}
import nafarr.peripherals.io.pwm.{TileLinkPwm, Pwm, PwmCtrl}
import nafarr.peripherals.com.uart.{TileLinkUart, Uart, UartCtrl}
import nafarr.peripherals.com.i2c.{TileLinkI2cController, I2c, I2cControllerCtrl}
import nafarr.peripherals.pinmux.{TileLinkPinmux, Pinmux, PinmuxCtrl}
import nafarr.crypto.aes.{TileLinkAesMaskedAccelerator, AesMaskedAcceleratorCtrl}
import nafarr.crypto.crc.{TileLinkCrc32, Crc32Ctrl}
import nafarr.crypto.prng.{TileLinkPrng, PrngCtrl}

object ElemRV {
  def apply(parameter: Hydrogen.Parameter) = ElemRV(parameter)

  case class Parameter(boardParameter: BoardParameter) extends SocParameter(boardParameter) {
    val gpio0 = GpioCtrl.Parameter(Gpio.Parameter(11), 3)
    val i2c0 = I2cControllerCtrl.Parameter.default(1)
    val pio0 = PioCtrl.Parameter.default(2)
    val pwm0 = PwmCtrl.Parameter.default(1)
    val uart0 = UartCtrl.Parameter.full()
    val prng = PrngCtrl.Parameter()
    val aesMasked = AesMaskedAcceleratorCtrl.Parameter()
    val crc32 = Crc32Ctrl.Parameter()
    val pinmux = PinmuxCtrl.Parameter(Pinmux.Parameter(11), 22, 2)

    override val irqSources = Seq(gpio0, i2c0, pio0, pwm0, uart0)
    override val errorSources = Seq(pio0, pwm0, uart0, prng)
  }

  case class ElemRV(parameter: Hydrogen.Parameter) extends Hydrogen.Hydrogen(parameter) {
    var socParameter = parameter.getSocParameter.asInstanceOf[Parameter]
    val io = new Bundle {
      val pins = Pinmux.Io(socParameter.pinmux.io)
    }

    val peripherals = new ClockingArea(clockCtrl.getClockDomainByName("system")) {

      val gpio0Ctrl = TileLinkGpio(socParameter.gpio0)
      addPeripheralDevice(gpio0Ctrl.io.bus, 0x0000, 4 kB)
      addInterrupt(gpio0Ctrl.io.interrupt)
      for (pin <- 0 until socParameter.gpio0.io.width) {
        addPinmuxInput(gpio0Ctrl.io.gpio.pins(pin), s"gpio0_$pin")
      }

      val i2c0Ctrl = TileLinkI2cController(socParameter.i2c0)
      addPeripheralDevice(i2c0Ctrl.io.bus, 0x1000, 4 kB)
      addInterrupt(i2c0Ctrl.io.interrupt)
      addPinmuxInput(i2c0Ctrl.io.i2c.scl, "i2c0_scl")
      addPinmuxInput(i2c0Ctrl.io.i2c.sda, "i2c0_sda")
      addPinmuxInput(i2c0Ctrl.io.i2c.interrupts(0), "i2c0_interrupt_0", output = false)

      val pio0Ctrl = TileLinkPio(socParameter.pio0)
      addPeripheralDevice(pio0Ctrl.io.bus, 0x2000, 4 kB)
      addInterrupt(pio0Ctrl.io.interrupt)
      addError(pio0Ctrl.io.error)
      for (pin <- 0 until socParameter.pio0.io.width) {
        addPinmuxInput(pio0Ctrl.io.pio.pins(pin), s"pio0_$pin")
      }

      val pwm0Ctrl = TileLinkPwm(socParameter.pwm0)
      addPeripheralDevice(pwm0Ctrl.io.bus, 0x3000, 4 kB)
      addInterrupt(pwm0Ctrl.io.interrupt)
      addError(pwm0Ctrl.io.error)
      for (pin <- 0 until socParameter.pwm0.io.channels) {
        addPinmuxInput(pwm0Ctrl.io.pwm.output(pin), s"pwm0_out_$pin")
        addPinmuxInput(pwm0Ctrl.io.pwm.compOutput(pin), s"pwm0_comp_$pin")
      }
      pwm0Ctrl.io.pwm.syncIn := False
      pwm0Ctrl.io.pwm.faultIn := False

      val uart0Ctrl = TileLinkUart(socParameter.uart0)
      addPeripheralDevice(uart0Ctrl.io.bus, 0x4000, 4 kB)
      addInterrupt(uart0Ctrl.io.interrupt)
      addError(uart0Ctrl.io.error)
      addPinmuxInput(uart0Ctrl.io.uart.txd, "uart0_tx")
      addPinmuxInput(uart0Ctrl.io.uart.rxd, "uart0_rx", output = false)
      addPinmuxInput(uart0Ctrl.io.uart.cts, "uart0_cts", output = false)
      addPinmuxInput(uart0Ctrl.io.uart.rts, "uart0_rts")

      /* Crypto */
      val prngCtrl = TileLinkPrng(socParameter.prng)
      addPeripheralDevice(prngCtrl.io.bus, 0x5000, 4 kB)
      addError(prngCtrl.io.error)

      val aesMaskedCtrl = TileLinkAesMaskedAccelerator(socParameter.aesMasked)
      addPeripheralDevice(aesMaskedCtrl.io.bus, 0x6000, 4 kB)

      /* Pin Mapping */
      addPinmuxOption(0, List("gpio0_0", "pwm0_out_0"))
      addPinmuxOption(1, List("gpio0_1", "pio0_0"))
      addPinmuxOption(2, List("gpio0_2", "pio0_1"))
      addPinmuxOption(3, List("uart0_tx", "gpio0_3"))
      addPinmuxOption(4, List("uart0_rx", "gpio0_4"))
      addPinmuxOption(5, List("uart0_cts", "gpio0_5"))
      addPinmuxOption(6, List("uart0_rts", "gpio0_6"))
      addPinmuxOption(7, List("gpio0_7", "pwm0_comp_0"))
      addPinmuxOption(8, List("gpio0_8", "i2c0_scl"))
      addPinmuxOption(9, List("gpio0_9", "i2c0_sda"))
      addPinmuxOption(10, List("gpio0_10", "i2c0_interrupt_0"))

      val pinmuxCtrl = TileLinkPinmux(socParameter.pinmux, getPinmuxMapping())
      addPeripheralDevice(pinmuxCtrl.io.bus, 0x10000, 4 kB)
      io.pins <> pinmuxCtrl.io.pins
      connectPinmuxInputs(pinmuxCtrl)

      connectPeripherals()
    }
  }
}
