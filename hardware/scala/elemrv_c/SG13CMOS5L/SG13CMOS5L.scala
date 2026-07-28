// SPDX-FileCopyrightText: 2026 aesc silicon
//
// SPDX-License-Identifier: CERN-OHL-W-2.0

package elemrv_c

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.bus.tilelink.{BusParameter => TileLinkParameter}

import nafarr.system.reset._
import nafarr.system.clock._
import nafarr.blackboxes.ihp.sg13cmos5l._
import nafarr.blackboxes.ihp.common._
import nafarr.memory.ocram.ihp.TileLinkIhpOnChipRam
import nafarr.memory.hyperbus.sim.W956A8MBYA
import nafarr.memory.spi.MT25Q

import zibal.misc._
import zibal.misc.ElementsConfig._
import zibal.platform.Carbon
import zibal.board.{KitParameter, BoardParameter}

import elements.sdk.ElementsApp
import elements.board.ElemRVFlask

case class SG13CMOS5LBoard() extends Component {
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

  val top = SG13CMOS5LTop()
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

  for (index <- 0 until top.io.forFutureUse.length) {
    top.io.forFutureUse(index).PAD := analogFalse
  }

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

  // UART RX is pins 4 - drive this pin high to prevent interrupts
  for (index <- 0 until top.io.pins.length if index != 4) {
    io.pins(index) <> top.io.pins(index).PAD
  }
  top.io.pins(4).PAD := analogTrue
  io.pins(4) := analogTrue

}

case class SG13CMOS5LTop() extends Component {
  val resets = List[ResetParameter](
    ResetParameter("system", 4096),
    ResetParameter("debug", 4096),
    // Released ~38 us before the CPU so the external SPI flash finishes its
    // reset recovery before the first XIP fetch (see io_plat.spiXip.reset).
    ResetParameter("flash", 256)
  )
  val inputClock = ClockParameter("input", ElemRVFlask.Carbon.oscillatorFrequency, "input")
  val clocks = List[ClockParameter](
    ClockParameter("system", inputClock.frequency, "system"),
    ClockParameter("debug", inputClock.frequency, "debug", synchronousWith = "system")
  )
  val kitParameter = KitParameter(resets, clocks, inputClock)
  val boardParameter = ElemRVFlask.Carbon.Parameter(kitParameter)
  val socParameter = ElemRV.Parameter(boardParameter)
  val parameter = Carbon.Parameter(
    socParameter,
    onChipRamSize = 8 kB,
    spiFlashSize = 512 kB,
    iCacheSize = 4 kB,
    resetCtrl = (parameter: ResetControllerCtrl.Parameter) => {
      val resetCtrl = new ResetControllerCtrl.DummyResetController(parameter)
      resetCtrl
    },
    clockCtrl = (
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
    onChipRamLogic = (parameter: TileLinkParameter, ramSize: BigInt) => {
      val ram = TileLinkIhpOnChipRam.OnePort(parameter, ramSize.toInt)
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
      val rst = IhpCmosIo(Edge.East, 6, "clk_main")
    }
    val pins = Vec(
      IhpCmosIo(Edge.West, 2, "clk_main"),
      IhpCmosIo(Edge.West, 3, "clk_main"),
      IhpCmosIo(Edge.West, 4, "clk_main"),
      IhpCmosIo(Edge.West, 5, "clk_main"),
      IhpCmosIo(Edge.West, 6, "clk_main"),
      IhpCmosIo(Edge.West, 7, "clk_main"),
      IhpCmosIo(Edge.West, 8, "clk_main"),
      IhpCmosIo(Edge.North, 2, "clk_main"),
      IhpCmosIo(Edge.North, 3, "clk_main"),
      IhpCmosIo(Edge.North, 4, "clk_main"),
      IhpCmosIo(Edge.North, 5, "clk_main"),
      IhpCmosIo(Edge.North, 6, "clk_main")
    )
    val forFutureUse = Vec(
      IhpCmosIo(Edge.South, 6),
      IhpCmosIo(Edge.North, 7),
      IhpCmosIo(Edge.North, 8)
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
    io.spi.cs(index) <> IOPadOut4mA(soc.io_plat.spiXip.spi.cs(index))
  }
  io.spi.sck <> IOPadOut4mA(soc.io_plat.spiXip.spi.sclk)
  for (index <- 0 until io.spi.dq.length) {
    io.spi.dq(index) <> IOPadInOut4mA(soc.io_plat.spiXip.spi.dq(index))
  }
  io.spi.rst <> IOPadOut4mA(soc.io_plat.spiXip.reset)

  for (index <- 0 until io.pins.length) {
    io.pins(index) <> IOPadInOut4mA(soc.io.pins.pins(index))
  }

  val forFutureUse = False
  for (index <- 0 until io.forFutureUse.length) {
    io.forFutureUse(index) <> IOPadOut4mA(forFutureUse)
  }

  val power = Seq(
    IhpPowerIo(Edge.South, 7, IhpPowerIoCell.SG13CMOS5L.Vss),
    IhpPowerIo(Edge.South, 8, IhpPowerIoCell.SG13CMOS5L.Vdd),
    IhpPowerIo(Edge.East, 7, IhpPowerIoCell.SG13CMOS5L.IOVss),
    IhpPowerIo(Edge.East, 8, IhpPowerIoCell.SG13CMOS5L.IOVdd),
    IhpPowerIo(Edge.North, 0, IhpPowerIoCell.SG13CMOS5L.Vdd),
    IhpPowerIo(Edge.North, 1, IhpPowerIoCell.SG13CMOS5L.Vss),
    IhpPowerIo(Edge.West, 0, IhpPowerIoCell.SG13CMOS5L.IOVdd),
    IhpPowerIo(Edge.West, 1, IhpPowerIoCell.SG13CMOS5L.IOVss)
  )
}

object SG13CMOS5LGenerate extends ElementsApp {
  val topCellName = scala.util.Properties.envOrElse("TOPCELL", "SG13CMOS5LTop")
  val report = elementsConfig.genASICSpinalConfig.ihpSramBlackboxes.generateVerilog {
    val top = SG13CMOS5LTop()
    top.setDefinitionName(topCellName)

    BaremetalTools
      .Header(elementsConfig, "demo")
      .generate(top.soc.baremetalDevices, top.soc.baremetalIrqs, top.soc.baremetalErrors)
    RenodeTools.dumpCosimManifest(top.soc, elementsConfig.cosimManifest)

    top
  }

  val chip = OpenROADTools.IHP.Config(elementsConfig, OpenROADTools.PDKs.IHP.sg13cmos5l)
  chip.setTopCellName(topCellName)
  scala.util.Properties.envOrElse("FLOORPLAN", "tapeout") match {
    case "tapeout" =>
      chip.dieArea = (0, 0, 2392.80, 2400.30)
      chip.coreArea = (394.08, 396.9, 1995.84, 2003.40)
    case "relaxed" =>
      SpinalWarning(
        "FLOORPLAN=relaxed: oversized iteration floorplan - utilization/timing/" +
          "congestion results are NOT tape-out representative."
      )
      chip.dieArea = (0, 0, 2619.84, 2623.32)
      chip.coreArea = (394.08, 396.9, 2222.88, 2226.42)
  }
  chip.hasIoRing = true
  chip.setAbcArea()

  val sramNorthY = chip.coreArea._4 - 669.06
  val sramNorthX =
    scala.math.floor((chip.coreArea._1 + chip.coreArea._3 - 416.64) / 0.96) * 48 / 100
  chip.addMacro(
    report.toplevel.soc.system.onChipRam.ctrl.asInstanceOf[TileLinkIhpOnChipRam.OnePort].rams(0),
    sramNorthX,
    sramNorthY,
    "R0",
    depth = 3
  )

  chip.addMacro(
    report.toplevel.soc.core.cpu.iCacheRams(0),
    434.08,
    790.02,
    "R0",
    depth = 3
  )
  chip.addMacro(
    report.toplevel.soc.core.cpu.iCacheTagRams(0),
    434.08,
    1217.16,
    "MX",
    depth = 3
  )

  chip.addClock(report.toplevel.io.clock.PAD, report.toplevel.inputClock.frequency, "clk_main")
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
  chip.setAsynchronousClockGroups(Seq("clk_main", "clk_debug"), Seq("clk_jtag"))
  chip.io = Some(report.toplevel.io)
  chip.ioPower = Some(report.toplevel.power)
  chip.pdnRingWidth = 30.0
  chip.pdnRingSpace = 5.0
  chip.additionalVerilogFiles ++= report.blackboxesSourcesPaths
  chip.generate

  val reporter = ReportTools.Report(report.toplevel.soc, elementsConfig)
  reporter.extractPads(report.toplevel.io, report.toplevel.power)
  reporter.generateAll()
  reporter.generateJson()
}

object SG13CMOS5LSimulate extends ElementsApp {
  val compiled = elementsConfig.genASICSimConfig.compile {
    val board = SG13CMOS5LBoard()
    BinTools.initRam(board.spiNor.deviceOut.data, elementsConfig.swStorageImageContainer)
    board
  }
  simType match {
    case "simulate" =>
      compiled.doSimUntilVoid("simulate") { dut =>
        val testCases = TestCases()
        testCases.addClock(
          dut.io.clock,
          dut.top.inputClock.frequency,
          simDuration.toString.toInt ms
        )
        testCases.addReset(dut.io.reset, 100 us)
      }
  }
}
