// SPDX-FileCopyrightText: 2025 aesc silicon
//
// SPDX-License-Identifier: CERN-OHL-W-2.0

package elemrv_n

import spinal.core._
import spinal.core.sim._
import spinal.lib._

import spinal.lib.bus.bmb._

import nafarr.system.reset._
import nafarr.system.reset.ResetControllerCtrl._
import nafarr.system.clock._
import nafarr.system.clock.ClockControllerCtrl._
import nafarr.blackboxes.lattice.ecp5._
import nafarr.memory.ocram.ihp.sg13g2.BmbIhpOnChipRam

import zibal.misc._
import zibal.platform.Nitrogen
import zibal.board.{KitParameter, BoardParameter}
import zibal.sim.hyperram.W956A8MBYA
import zibal.sim.MT25Q

import elements.sdk.ElementsApp
import elements.board.ECPIX5

case class ECPIX5Board() extends Component {
  val io = new Bundle {
    val clock = inout(Analog(Bool))
    val reset = inout(Analog(Bool))
    val hyperbus = new Bundle {
      val cs = Vec(inout(Analog(Bool)), 4)
      val ck = inout(Analog(Bool))
      val reset = inout(Analog(Bool))
      val rwds = inout(Analog(Bool))
      val dq = Vec(inout(Analog(Bool())), 8)
    }
    val spi = new Bundle {
      val cs = inout(Analog(Bool))
      val sck = inout(Analog(Bool))
      val dq = Vec(inout(Analog(Bool)), 4)
    }
    val pins = Vec(inout(Analog(Bool())), 20)
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

  val w956a8mbya = W956A8MBYA()
  w956a8mbya.io.clock := io.clock
  w956a8mbya.io.ck := io.hyperbus.ck
  w956a8mbya.io.ckN := analogFalse
  for (index <- 0 until top.io.hyperbus.dq.length) {
    w956a8mbya.io.dqIn(index) := io.hyperbus.dq(index)
    io.hyperbus.dq(index) := w956a8mbya.io.dqOut(index)
  }
  w956a8mbya.io.rwdsIn := io.hyperbus.rwds
  io.hyperbus.rwds := w956a8mbya.io.rwdsOut
  w956a8mbya.io.csN := io.hyperbus.cs(0)
  w956a8mbya.io.resetN := io.hyperbus.reset

  top.io.hyperbus.cs(1).PAD := analogFalse
  top.io.hyperbus.cs(2).PAD := analogFalse
  top.io.hyperbus.cs(3).PAD := analogFalse
  io.hyperbus.cs(0) := top.io.hyperbus.cs(0).PAD
  io.hyperbus.ck := top.io.hyperbus.ck.PAD
  io.hyperbus.reset := top.io.hyperbus.reset.PAD
  io.hyperbus.rwds <> top.io.hyperbus.rwds.PAD
  for (index <- 0 until top.io.hyperbus.dq.length) {
    io.hyperbus.dq(index) <> top.io.hyperbus.dq(index).PAD
  }

  val spiNor = MT25Q.MultiProtocol()
  spiNor.io.clock := io.clock
  spiNor.io.dataClock := io.spi.sck
  spiNor.io.reset := io.reset
  spiNor.io.chipSelect := io.spi.cs
  io.spi.cs := top.io.spi.cs(0).PAD
  io.spi.sck := top.io.spi.sck.PAD
  for (index <- 0 until top.io.spi.dq.length) {
    spiNor.io.dqIn(index) := io.spi.dq(index)
    io.spi.dq(index) := top.io.spi.dq(index).PAD
    top.io.spi.dq(index).PAD := spiNor.io.dqOut(index)
  }
  top.io.spi.rst.PAD := analogFalse

  for (index <- 0 until top.io.pins.length) {
    io.pins(index) <> top.io.pins(index).PAD
  }

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
    ResetParameter("system", 128),
    ResetParameter("cpu", 128),
    ResetParameter("debug", 128)
  )
  val clocks = List[ClockParameter](
    ClockParameter("system", 20 MHz, "system"),
    ClockParameter("cpu", 20 MHz, "cpu", synchronousWith = "system"),
    ClockParameter("debug", 10 MHz, "debug", synchronousWith = "system")
  )
  val hyperbusPartitions = List[(BigInt, Boolean)](
    (8 MB, true),
    (8 MB, true),
    (8 MB, true),
    (8 MB, true)
  )
  val kitParameter = KitParameter(resets, clocks)
  val boardParameter = ECPIX5.Parameter(kitParameter, ECPIX5.SystemClock.frequency)
  val socParameter = ElemRV.Parameter(boardParameter)
  val parameter = Nitrogen.Parameter(
    socParameter,
    4 kB,
    8 MB,
    hyperbusPartitions,
    (resetCtrl: ResetControllerCtrl, _, clock: Bool) => { resetCtrl.buildXilinx(clock) },
    (clockCtrl: ClockControllerCtrl, resetCtrl: ResetControllerCtrl, clock: Bool) => {
      clockCtrl.buildLatticeECP5Pll(
        clock,
        resetCtrl,
        boardParameter.getOscillatorFrequency,
        List("system", "cpu", "debug")
      )
    },
    (parameter: BmbParameter, ramSize: BigInt) => {
      val ram = BmbIhpOnChipRam.OnePort1Macro(parameter, ramSize.toInt)
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
    val hyperbus = new Bundle {
      val cs = Vec(
        LatticeCmosIo(ECPIX5.Pmods.Pmod5.pin1),
        LatticeCmosIo(ECPIX5.Pmods.Pmod5.pin5),
        LatticeCmosIo(ECPIX5.Pmods.Pmod5.pin0),
        LatticeCmosIo(ECPIX5.Pmods.Pmod5.pin4)
      )
      val ck = LatticeCmosIo(ECPIX5.Pmods.Pmod5.pin2).slewRateFast
      val ckN = LatticeCmosIo(ECPIX5.Pmods.Pmod5.pin3).slewRateFast
      val reset = LatticeCmosIo(ECPIX5.Pmods.Pmod5.pin6)
      val rwds = LatticeCmosIo(ECPIX5.Pmods.Pmod5.pin7).slewRateFast
      val dq = Vec(
        LatticeCmosIo(ECPIX5.Pmods.Pmod4.pin0).slewRateFast,
        LatticeCmosIo(ECPIX5.Pmods.Pmod4.pin1).slewRateFast,
        LatticeCmosIo(ECPIX5.Pmods.Pmod4.pin2).slewRateFast,
        LatticeCmosIo(ECPIX5.Pmods.Pmod4.pin3).slewRateFast,
        LatticeCmosIo(ECPIX5.Pmods.Pmod4.pin7).slewRateFast,
        LatticeCmosIo(ECPIX5.Pmods.Pmod4.pin6).slewRateFast,
        LatticeCmosIo(ECPIX5.Pmods.Pmod4.pin5).slewRateFast,
        LatticeCmosIo(ECPIX5.Pmods.Pmod4.pin4).slewRateFast
      )
    }
    val spi = new Bundle {
      val cs = Vec(
        LatticeCmosIo(ECPIX5.Pmods.Pmod6.pin0)
      )
      val dq = Vec(
        LatticeCmosIo(ECPIX5.Pmods.Pmod6.pin1).slewRateFast,
        LatticeCmosIo(ECPIX5.Pmods.Pmod6.pin2).slewRateFast,
        LatticeCmosIo(ECPIX5.Pmods.Pmod6.pin6).slewRateFast,
        LatticeCmosIo(ECPIX5.Pmods.Pmod6.pin7).slewRateFast
      )
      val sck = LatticeCmosIo(ECPIX5.Pmods.Pmod6.pin3).slewRateFast
      val rst = LatticeCmosIo(ECPIX5.Pmods.Pmod6.pin5)
    }
    val pins = Vec(
      LatticeCmosIo(ECPIX5.LEDs.LD5.blue),
      LatticeCmosIo(ECPIX5.LEDs.LD6.red),
      LatticeCmosIo(ECPIX5.LEDs.LD7.green),
      LatticeCmosIo(ECPIX5.Buttons.sw0),
      LatticeCmosIo(ECPIX5.Pmods.Pmod2.pin0),
      LatticeCmosIo(ECPIX5.Pmods.Pmod2.pin1),
      LatticeCmosIo(ECPIX5.Pmods.Pmod2.pin2),
      LatticeCmosIo(ECPIX5.Pmods.Pmod2.pin3),
      LatticeCmosIo(ECPIX5.Pmods.Pmod2.pin4),
      LatticeCmosIo(ECPIX5.Pmods.Pmod2.pin5),
      LatticeCmosIo(ECPIX5.Pmods.Pmod2.pin6),
      LatticeCmosIo(ECPIX5.Pmods.Pmod2.pin7),
      LatticeCmosIo(ECPIX5.Pmods.Pmod3.pin0),
      LatticeCmosIo(ECPIX5.Pmods.Pmod3.pin1),
      LatticeCmosIo(ECPIX5.UartStd.txd),
      LatticeCmosIo(ECPIX5.UartStd.rxd),
      LatticeCmosIo(ECPIX5.Pmods.Pmod3.pin2),
      LatticeCmosIo(ECPIX5.Pmods.Pmod3.pin3),
      LatticeCmosIo(ECPIX5.Pmods.Pmod3.pin4),
      LatticeCmosIo(ECPIX5.Pmods.Pmod3.pin5)
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

  for (index <- 0 until io.hyperbus.cs.length) {
    io.hyperbus.cs(index) <> FakeO(soc.io_plat.hyperbus.cs(index))
  }
  io.hyperbus.ck <> FakeO(soc.io_plat.hyperbus.ck)
  io.hyperbus.ckN <> FakeO(False)
  io.hyperbus.reset <> FakeO(soc.io_plat.hyperbus.reset)
  for (index <- 0 until io.hyperbus.dq.length) {
    io.hyperbus.dq(index) <> FakeIo(soc.io_plat.hyperbus.dq(index))
  }
  io.hyperbus.rwds <> FakeIo(soc.io_plat.hyperbus.rwds)

  for (index <- 0 until io.spi.cs.length) {
    io.spi.cs(index) <> FakeO(soc.io_plat.spi.cs(index))
  }
  io.spi.sck <> FakeO(soc.io_plat.spi.sclk)
  for (index <- 0 until io.spi.dq.length) {
    io.spi.dq(index) <> FakeIo(soc.io_plat.spi.dq(index))
  }

  // Generate psuedo-reset for external SPI flash
  val spiResetClockDomain = ClockDomain(
    clock = io.clock.PAD,
    config = ClockDomainConfig(
      resetKind = BOOT
    )
  )

  val spiReset = new ClockingArea(spiResetClockDomain) {
    val done = RegInit(False)
    val counter = Reg(UInt(4 bits)).init(U(0))
    when(!done) {
      counter := counter + 1
    }
    when(counter === counter.maxValue) {
      done := True
    }
  }
  io.spi.rst <> FakeO(spiReset.done)

  for (index <- 0 until 3) {
    io.pins(index) <> FakeIo(soc.io.pins.pins(index), true)
  }
  for (index <- 3 until io.pins.length) {
    io.pins(index) <> FakeIo(soc.io.pins.pins(index))
  }

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
    BinTools.initRam(board.spiNor.deviceOut.data, elementsConfig.swStorageBaremetalImage("bootrom"))
    BinTools.initRam(board.w956a8mbya.device.data, elementsConfig.swStorageBaremetalImage("demo"))
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
        testCases.uartRxIdle(dut.io.pins(6))
      }
    case "boot" =>
      compiled.doSimUntilVoid("boot") { dut =>
        dut.simHook()
        val testCases = TestCases()
        testCases.addClockWithTimeout(dut.io.clock, ECPIX5.SystemClock.frequency, 20 ms)
        testCases.uartRxIdle(dut.io.pins(6))
        testCases.boot(dut.io.pins(5), dut.baudPeriod)
      }
    case "mtimer" =>
      compiled.doSimUntilVoid("mtimer") { dut =>
        dut.simHook()
        val testCases = TestCases()
        testCases.addClockWithTimeout(dut.io.clock, ECPIX5.SystemClock.frequency, 20 ms)
        testCases.uartRxIdle(dut.io.pins(6))
        testCases.heartbeat(dut.io.pins(0), true)
      }
    case "reset" =>
      compiled.doSimUntilVoid("reset") { dut =>
        dut.simHook()
        val testCases = TestCases()
        testCases.addClockWithTimeout(dut.io.clock, ECPIX5.SystemClock.frequency, 25 ms)
        testCases.uartRxIdle(dut.io.pins(6))
        testCases.reset(dut.io.pins(5), dut.baudPeriod)
      }
    case _ =>
      println(s"Unknown simulation ${simType}")
  }
}
