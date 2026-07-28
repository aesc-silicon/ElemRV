// SPDX-FileCopyrightText: 2026 aesc silicon
//
// SPDX-License-Identifier: CERN-OHL-W-2.0

package elemrv_c.SG13CMOS5L

import elemrv_c.ElemRV

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.bus.tilelink.{BusParameter => TileLinkParameter}

import nafarr.system.reset._
import nafarr.system.reset.ResetControllerCtrl._
import nafarr.system.clock._
import nafarr.system.clock.ClockControllerCtrl._
import nafarr.blackboxes.lattice.ecp5._
import nafarr.memory.ocram.ihp.TileLinkIhpOnChipRam
import nafarr.memory.hyperbus.sim.W956A8MBYA
import nafarr.memory.spi.MT25Q

import zibal.misc._
import zibal.misc.ElementsConfig._
import zibal.platform.Carbon
import zibal.board.{KitParameter, BoardParameter}

import elements.sdk.ElementsApp
import elements.board.ECPIX5
import elements.board.ElemRVFlask

case class ECPIX5Board() extends Component {
  val io = new Bundle {
    val clock = inout(Analog(Bool))
    val reset = inout(Analog(Bool))
    val spi = new Bundle {
      val cs = inout(Analog(Bool))
      val sck = inout(Analog(Bool))
      val dq = Vec(inout(Analog(Bool)), 4)
      val rst = inout(Analog(Bool))
    }
    val pins = Vec(inout(Analog(Bool())), 12)
  }

  val top = ECPIX5Top()
  val analogFalse = Analog(Bool)
  analogFalse := False
  val analogTrue = Analog(Bool)
  analogTrue := True

  top.io.clock.PAD := io.clock

  top.io.jtag.tms.PAD := analogFalse
  top.io.jtag.tck.PAD := analogFalse
  top.io.jtag.tdi.PAD := analogFalse
  analogFalse := top.io.jtag.tdo.PAD

  val spiNor = MT25Q.MultiProtocol()
  spiNor.io.clock := io.clock
  spiNor.io.dataClock := io.spi.sck
  spiNor.io.reset := io.reset
  spiNor.io.chipSelect := io.spi.cs
  spiNor.io.rst := io.spi.rst
  io.spi.cs := top.io.spi.cs(0).PAD
  io.spi.sck := top.io.spi.sck.PAD
  io.spi.rst := top.io.spi.rst.PAD
  for (index <- 0 until top.io.spi.dq.length) {
    spiNor.io.dqIn(index) := io.spi.dq(index)
    io.spi.dq(index) := top.io.spi.dq(index).PAD
    top.io.spi.dq(index).PAD := spiNor.io.dqOut(index)
  }
  top.io.spi.rst.PAD := analogFalse

  // UART RX is pins 4 - drive this pin high to prevent interrupts
  for (index <- 0 until top.io.pins.length if index != 4) {
    io.pins(index) <> top.io.pins(index).PAD
  }
  top.io.pins(4).PAD := analogTrue
  io.pins(4) := analogTrue

  for (index <- 0 until top.io.ledPullDown.length) {
    top.io.ledPullDown(index).PAD := analogFalse
  }

  val baudPeriod = top.soc.socParameter.uart0.init.getBaudPeriod()

  def simHook() {
    for ((domain, index) <- top.soc.parameter.getKitParameter.clocks.zipWithIndex) {
      val clockDomain = top.soc.clockCtrl.getClockDomainByName(domain.name)
      SimulationHelper.generateEndlessClock(clockDomain.clock, domain.frequency)
    }
  }
}

case class ECPIX5Top() extends Component {
  val resets = List[ResetParameter](
    ResetParameter("system", 4096),
    ResetParameter("debug", 4096),
    // Released ~38 us before the CPU so the external SPI flash finishes its
    // reset recovery before the first XIP fetch (see io_plat.spiXip.reset).
    ResetParameter("flash", 256)
  )
  val inputClock = ClockParameter("input", ECPIX5.SystemClock.frequency, "input")
  val refClock = ElemRVFlask.Carbon.oscillatorFrequency
  val clocks = List[ClockParameter](
    ClockParameter("system", refClock, "system"),
    ClockParameter("debug", refClock, "debug", synchronousWith = "system")
  )
  val kitParameter = KitParameter(resets, clocks, inputClock)
  val boardParameter = ECPIX5.Parameter(kitParameter, ECPIX5.SystemClock.frequency)
  val socParameter = ElemRV.Parameter(boardParameter)
  val parameter = Carbon.Parameter(
    socParameter,
    onChipRamSize = 8 kB,
    spiFlashSize = 512 kB,
    iCacheSize = 4 kB,
    resetCtrl = (parameter: ResetControllerCtrl.Parameter) => {
      val resetCtrl = new ResetControllerCtrl.GeneratorResetController(parameter)
      resetCtrl
    },
    clockCtrl = (
        parameter: ClockControllerCtrl.Parameter,
        resetCtrl: ResetControllerCtrl.ResetControllerBase
    ) => {
      val clockCtrl = new ClockControllerCtrl.LatticeECP5PllController(
        parameter,
        inputClock,
        List("system", "debug")
      )
      clockCtrl
    },
    onChipRamLogic = (parameter: TileLinkParameter, ramSize: BigInt) => {
      val ram = TileLinkIhpOnChipRam.OnePort(parameter, ramSize.toInt)
      (ram, ram.io.bus)
    }
  )

  val io = new Bundle {
    val clock = LatticeCmosIo(ECPIX5.SystemClock.clock).clock(ECPIX5.SystemClock.frequency)
    val jtag = new Bundle {
      val tms = LatticeCmosIo(ECPIX5.Pmods.Pmod1.pin2)
      val tdi = LatticeCmosIo(ECPIX5.Pmods.Pmod1.pin3)
      val tdo = LatticeCmosIo(ECPIX5.Pmods.Pmod1.pin0)
      val tck = LatticeCmosIo(ECPIX5.Pmods.Pmod1.pin1)
    }
    val spi = new Bundle {
      val cs = Vec(
        LatticeCmosIo(ECPIX5.Pmods.Pmod6.pin0)
      )
      val sck = LatticeCmosIo(ECPIX5.Pmods.Pmod6.pin3).slewRateFast
      val dq = Vec(
        LatticeCmosIo(ECPIX5.Pmods.Pmod6.pin1).slewRateFast,
        LatticeCmosIo(ECPIX5.Pmods.Pmod6.pin2).slewRateFast,
        LatticeCmosIo(ECPIX5.Pmods.Pmod6.pin6).slewRateFast,
        LatticeCmosIo(ECPIX5.Pmods.Pmod6.pin7).slewRateFast
      )
      val rst = LatticeCmosIo(ECPIX5.Pmods.Pmod6.pin5)
    }
    val pins = Vec(
      LatticeCmosIo(ECPIX5.LEDs.LD5.blue),
      LatticeCmosIo(ECPIX5.LEDs.LD6.red),
      LatticeCmosIo(ECPIX5.Buttons.sw0),
      LatticeCmosIo(ECPIX5.UartStd.txd),
      LatticeCmosIo(ECPIX5.UartStd.rxd),
      LatticeCmosIo(ECPIX5.Pmods.Pmod2.pin0),
      LatticeCmosIo(ECPIX5.Pmods.Pmod2.pin1),
      LatticeCmosIo(ECPIX5.Pmods.Pmod2.pin2),
      LatticeCmosIo(ECPIX5.Pmods.Pmod2.pin3),
      LatticeCmosIo(ECPIX5.Pmods.Pmod2.pin4),
      LatticeCmosIo(ECPIX5.Pmods.Pmod2.pin5),
      LatticeCmosIo(ECPIX5.LEDs.LD7.green)
    )
    val ledPullDown = Vec(
      LatticeCmosIo(ECPIX5.LEDs.LD5.red),
      LatticeCmosIo(ECPIX5.LEDs.LD5.green),
      LatticeCmosIo(ECPIX5.LEDs.LD6.green),
      LatticeCmosIo(ECPIX5.LEDs.LD6.blue),
      LatticeCmosIo(ECPIX5.LEDs.LD7.red),
      LatticeCmosIo(ECPIX5.LEDs.LD7.blue),
      LatticeCmosIo(ECPIX5.LEDs.LD8.red),
      LatticeCmosIo(ECPIX5.LEDs.LD8.green),
      LatticeCmosIo(ECPIX5.LEDs.LD8.blue)
    )
  }

  val soc = ElemRV(parameter)

  io.clock <> FakeI(soc.io_plat.clock)

  io.jtag.tms <> FakeI(soc.io_plat.jtag.tms)
  io.jtag.tdi <> FakeI(soc.io_plat.jtag.tdi)
  io.jtag.tdo <> FakeO(soc.io_plat.jtag.tdo)
  io.jtag.tck <> FakeI(soc.io_plat.jtag.tck)

  for (index <- 0 until io.spi.cs.length) {
    io.spi.cs(index) <> FakeO(soc.io_plat.spiXip.spi.cs(index))
  }
  io.spi.sck <> FakeO(soc.io_plat.spiXip.spi.sclk)
  for (index <- 0 until io.spi.dq.length) {
    io.spi.dq(index) <> FakeIo(soc.io_plat.spiXip.spi.dq(index))
  }
  io.spi.rst <> FakeO(soc.io_plat.spiXip.reset)

  for (index <- 0 until 2) {
    io.pins(index) <> FakeIo(soc.io.pins.pins(index), true)
  }
  for (index <- 2 until io.pins.length - 1) {
    io.pins(index) <> FakeIo(soc.io.pins.pins(index))
  }
  io.pins(io.pins.length - 1) <> FakeIo(soc.io.pins.pins(io.pins.length - 1), true)

  for (index <- 0 until io.ledPullDown.length) {
    io.ledPullDown(index) <> FakeO(True)
  }

}

object ECPIX5Generate extends ElementsApp {
  val report = elementsConfig.genFPGASpinalConfig.generateVerilog {
    val top = ECPIX5Top()

    val lpf = LatticeTools.Lpf(elementsConfig)
    lpf.generate(top.io)

    top
  }
  report.mergeRTLSource("ECPIX5TopBlackboxes")
}

object ECPIX5Simulate extends ElementsApp {
  val compiled = elementsConfig.genFPGASimConfig.compile {
    val board = ECPIX5Board()
    BinTools.initRam(board.spiNor.deviceOut.data, elementsConfig.swStorageImageContainer)
    for (domain <- board.top.soc.parameter.getKitParameter.clocks) {
      board.top.soc.clockCtrl.getClockDomainByName(domain.name).clock.simPublic()
    }
    board
  }
  simType match {
    case "simulate" =>
      compiled.doSimUntilVoid("simulate") { dut =>
        dut.simHook()
        val testCases = TestCases()
        testCases.addClock(
          dut.io.clock,
          ECPIX5.SystemClock.frequency,
          simDuration.toString.toInt ms
        )
        testCases.addReset(dut.io.reset, 1000 ns)
      }
    case "boot" =>
      compiled.doSimUntilVoid("boot") { dut =>
        dut.simHook()
        val testCases = TestCases()
        testCases.addClockWithTimeout(dut.io.clock, ECPIX5.SystemClock.frequency, 20 ms)
        testCases.boot(dut.io.pins(5), dut.baudPeriod)
      }
    case "mtimer" =>
      compiled.doSimUntilVoid("mtimer") { dut =>
        dut.simHook()
        val testCases = TestCases()
        testCases.addClockWithTimeout(dut.io.clock, ECPIX5.SystemClock.frequency, 20 ms)
        testCases.heartbeat(dut.io.pins(0), true)
      }
    case "reset" =>
      compiled.doSimUntilVoid("reset") { dut =>
        dut.simHook()
        val testCases = TestCases()
        testCases.addClockWithTimeout(dut.io.clock, ECPIX5.SystemClock.frequency, 25 ms)
        testCases.reset(dut.io.pins(5), dut.baudPeriod)
      }
    case _ =>
      println(s"Unknown simulation ${simType}")
  }
}
