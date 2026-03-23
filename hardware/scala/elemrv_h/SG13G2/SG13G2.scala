// SPDX-FileCopyrightText: 2025 aesc silicon
//
// SPDX-License-Identifier: CERN-OHL-W-2.0

package elemrv_h

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.bus.bmb._

import nafarr.system.reset._
import nafarr.system.clock._
import nafarr.blackboxes.ihp.sg13g2._
import nafarr.blackboxes.ihp.common._
import nafarr.memory.ocram.ihp.sg13g2.BmbIhpOnChipRam

import zibal.misc._
import zibal.platform.Hydrogen
import zibal.board.{KitParameter, BoardParameter}
import zibal.sim.hyperram.W956A8MBYA
import zibal.sim.MT25Q

import elements.sdk.ElementsApp
import elements.board.ElemRVBoard

case class SG13G2Board() extends Component {
  val io = new Bundle {
    val clock = inout(Analog(Bool))
    val reset = inout(Analog(Bool))
    val spi = new Bundle {
      val cs = inout(Analog(Bool))
      val sck = inout(Analog(Bool))
      val dq = Vec(inout(Analog(Bool)), 4)
    }
    val pins = Vec(inout(Analog(Bool())), 12)
  }

  val top = SG13G2Top()
  val analogFalse = Analog(Bool)
  analogFalse := False
  val analogTrue = Analog(Bool)
  analogTrue := True

  top.io.clock.PAD := io.clock
  top.io.reset.PAD := io.reset

  top.io.jtag.tms.PAD := analogFalse
  top.io.jtag.tck.PAD := analogFalse
  top.io.jtag.tdi.PAD := analogFalse
  analogFalse := top.io.jtag.tdo.PAD

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

  for (index <- 0 until top.io.pins.length) {
    io.pins(index) <> top.io.pins(index).PAD
  }

  val baudPeriod = top.soc.socParameter.uart0.init.getBaudPeriod()

  def simHook() {
    for ((domain, index) <- top.soc.parameter.getKitParameter.clocks.zipWithIndex) {
      val clockDomain = top.soc.clockCtrl.getClockDomainByName(domain.name)
      SimulationHelper.generateEndlessClock(clockDomain.clock, domain.frequency)
    }
  }
}

case class SG13G2Top() extends Component {
  val resets = List[ResetParameter](
    ResetParameter("system", 128),
    ResetParameter("debug", 128)
  )
  val inputClock = ClockParameter("input", 50 MHz, "input")
  val clocks = List[ClockParameter](
    ClockParameter("system", 50 MHz, "system"),
    ClockParameter("debug", 12.5 MHz, "debug", synchronousWith = "system")
  )
  val kitParameter = KitParameter(resets, clocks)
  val boardParameter = ElemRVBoard.Parameter(kitParameter)
  val socParameter = ElemRV.Parameter(boardParameter)
  val parameter = Hydrogen.Parameter(
    socParameter,
    8 kB,
    8 MB,
    (parameter: ResetControllerCtrl.Parameter) => {
      val resetCtrl = new ResetControllerCtrl.DummyResetController(parameter)
      resetCtrl
    },
    (
        parameter: ClockControllerCtrl.Parameter,
        resetCtrl: ResetControllerCtrl.ResetControllerBase
    ) => {
      val clockCtrl = new ClockControllerCtrl.ClockDividerController(
        parameter,
        inputClock,
        List("system", "debug")
      )
      clockCtrl
    },
    (parameter: BmbParameter, ramSize: BigInt) => {
      val ram = BmbIhpOnChipRam.OnePort1Macro(parameter, ramSize.toInt)
      (ram, ram.io.bus)
    }
  )

  val io = new Bundle {
    val clock = IhpCmosIo(Edge.South, 5)
    val reset = IhpCmosIo(Edge.South, 4, "clk_main")
    val jtag = new Bundle {
      val tms = IhpCmosIo(Edge.South, 0, "clk_jtag")
      val tdi = IhpCmosIo(Edge.South, 1, "clk_jtag")
      val tdo = IhpCmosIo(Edge.South, 2, "clk_jtag")
      val tck = IhpCmosIo(Edge.South, 3)
    }
    val spi = new Bundle {
      val cs = Vec(
        IhpCmosIo(Edge.East, 0, "clk_main")
      )
      val sck = IhpCmosIo(Edge.East, 1, "clk_main")
      val dq = Vec(
        IhpCmosIo(Edge.East, 5, "clk_main"),
        IhpCmosIo(Edge.East, 4, "clk_main"),
        IhpCmosIo(Edge.East, 3, "clk_main"),
        IhpCmosIo(Edge.East, 2, "clk_main")
      )
    }
    val pins = Vec(
      IhpCmosIo(Edge.West, 2, "clk_main"),
      IhpCmosIo(Edge.West, 3, "clk_main"),
      IhpCmosIo(Edge.West, 4, "clk_main"),
      IhpCmosIo(Edge.West, 5, "clk_main"),
      IhpCmosIo(Edge.West, 6, "clk_main"),
      IhpCmosIo(Edge.West, 7, "clk_main"),
      IhpCmosIo(Edge.North, 2, "clk_main"),
      IhpCmosIo(Edge.North, 3, "clk_main"),
      IhpCmosIo(Edge.North, 4, "clk_main"),
      IhpCmosIo(Edge.North, 5, "clk_main"),
      IhpCmosIo(Edge.North, 6, "clk_main"),
      IhpCmosIo(Edge.North, 7, "clk_main")
    )
  }

  val soc = ElemRV(parameter)

  io.clock <> IOPadIn(soc.io_plat.clock)
  io.reset <> IOPadIn(soc.io_plat.reset)

  io.jtag.tms <> IOPadIn(soc.io_plat.jtag.tms)
  io.jtag.tdi <> IOPadIn(soc.io_plat.jtag.tdi)
  io.jtag.tdo <> IOPadOut4mA(soc.io_plat.jtag.tdo)
  io.jtag.tck <> IOPadIn(soc.io_plat.jtag.tck)

  for (index <- 0 until io.spi.cs.length) {
    io.spi.cs(index) <> IOPadOut4mA(soc.io_plat.spi.cs(index))
  }
  io.spi.sck <> IOPadOut4mA(soc.io_plat.spi.sclk)
  for (index <- 0 until io.spi.dq.length) {
    io.spi.dq(index) <> IOPadInOut4mA(soc.io_plat.spi.dq(index))
  }

  for (index <- 0 until io.pins.length) {
    io.pins(index) <> IOPadInOut4mA(soc.io.pins.pins(index))
  }

  val power = Seq(
    IhpPowerIo(Edge.South, 6, IhpPowerIoCell.SG13G2.Vss),
    IhpPowerIo(Edge.South, 7, IhpPowerIoCell.SG13G2.Vdd),
    IhpPowerIo(Edge.East, 6, IhpPowerIoCell.SG13G2.IOVdd),
    IhpPowerIo(Edge.East, 7, IhpPowerIoCell.SG13G2.IOVss),
    IhpPowerIo(Edge.North, 0, IhpPowerIoCell.SG13G2.Vss),
    IhpPowerIo(Edge.North, 1, IhpPowerIoCell.SG13G2.Vdd),
    IhpPowerIo(Edge.West, 0, IhpPowerIoCell.SG13G2.IOVdd),
    IhpPowerIo(Edge.West, 1, IhpPowerIoCell.SG13G2.IOVss)
  )
}

object SG13G2Generate extends ElementsApp {
  val report = elementsConfig.genASICSpinalConfig.generateVerilog {
    val top = SG13G2Top()

    top.soc.prepareBaremetal("demo", elementsConfig)

    top
  }

  val chip = OpenROADTools.IHP.Config(elementsConfig, OpenROADTools.PDKs.IHP.sg13g2)
  chip.dieArea = (0, 0, 2081.28, 2079)
  chip.coreArea = (394.08, 396.9, 1684.32, 1682.1)
  chip.hasIoRing = true
  chip.addMacro(
    report.toplevel.soc.system.onChipRam.ctrl.asInstanceOf[BmbIhpOnChipRam.OnePort1Macro].ram,
    444.96,
    448.35,
    "MX",
    depth = 3
  )
  chip.addClock(report.toplevel.io.clock.PAD, 50 MHz, "clk_main")
  chip.addClock(report.toplevel.io.jtag.tck.PAD, 10 MHz, "clk_jtag")
  chip.addGeneratedClock(
    report.toplevel.io.clock.PAD,
    report.toplevel.inputClock.frequency,
    "clk_debug",
    report.toplevel.soc.clockCtrl.io.clocks,
    report.toplevel.soc.clockCtrl.getPortIndexByName("debug"),
    report.toplevel.soc.clockCtrl.getClockDomainByName("debug").frequency.getValue
  )
  chip.addReset(report.toplevel.io.reset.PAD)
  chip.setFalsePath("clk_main", "clk_jtag")
  chip.setFalsePath("clk_system", "clk_jtag")
  chip.io = Some(report.toplevel.io)
  chip.ioPower = Some(report.toplevel.power)
  chip.pdnRingWidth = 30.0
  chip.pdnRingSpace = 5.0
  chip.generate
}

object SG13G2Simulate extends ElementsApp {
  val compiled = elementsConfig.genFPGASimConfig.compile {
    val board = SG13G2Board()
    BinTools.initRam(board.spiNor.deviceOut.data, elementsConfig.swStorageBaremetalImage("demo"))
    board
  }
  simType match {
    case "simulate" =>
      compiled.doSimUntilVoid("simulate") { dut =>
        val testCases = TestCases()
        testCases.addClock(
          dut.io.clock,
          50 MHz, // TODO fix missing PLL
          simDuration.toString.toInt ms
        )
        testCases.addReset(dut.io.reset, 100 us)
        testCases.uartRxIdle(dut.io.pins(1))
      }
  }
}
