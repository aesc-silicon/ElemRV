// SPDX-FileCopyrightText: 2025 aesc silicon
//
// SPDX-License-Identifier: CERN-OHL-W-2.0

package elemrv_n

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
import zibal.platform.Nitrogen
import zibal.board.{KitParameter, BoardParameter}

import elements.sdk.ElementsApp
import elements.board.ElemRVFlask

case class SG13CMOS5LBoard() extends Component {
  val io = new Bundle {
    val clock = inout(Analog(Bool))
    val reset = inout(Analog(Bool))
    val hyperbus = new Bundle {
      val cs = Vec(inout(Analog(Bool)), 2)
      val ck = inout(Analog(Bool))
      val reset = inout(Analog(Bool))
      val rwds = inout(Analog(Bool))
      val dq = Vec(inout(Analog(Bool())), 8)
    }
    val spi = new Bundle {
      val cs = inout(Analog(Bool))
      val sck = inout(Analog(Bool))
      val dq = Vec(inout(Analog(Bool)), 4)
      val rst = inout(Analog(Bool))
    }
    val pins = Vec(inout(Analog(Bool())), 20)
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

  val w956a8mbya = W956A8MBYA.W956A8MBYA()
  w956a8mbya.io.clock := io.clock
  w956a8mbya.io.ck := io.hyperbus.ck
  w956a8mbya.io.ckN := analogFalse
  for (index <- 0 until top.io.hyperbus.dq.length) {
    w956a8mbya.io.dqIn(index) := io.hyperbus.dq(index)
    io.hyperbus.dq(index).addTag(crossClockDomain)
    io.hyperbus.dq(index) := w956a8mbya.io.dqOut(index)
  }
  w956a8mbya.io.rwdsIn.addTag(crossClockDomain)
  w956a8mbya.io.rwdsIn := io.hyperbus.rwds
  io.hyperbus.rwds := w956a8mbya.io.rwdsOut
  w956a8mbya.io.csN := io.hyperbus.cs(0)
  w956a8mbya.io.resetN := io.hyperbus.reset

  top.io.hyperbus.cs(1).PAD := analogFalse
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
  spiNor.io.rst := io.spi.rst
  io.spi.cs := top.io.spi.cs(0).PAD
  io.spi.sck := top.io.spi.sck.PAD
  io.spi.rst := top.io.spi.rst.PAD
  for (index <- 0 until top.io.spi.dq.length) {
    spiNor.io.dqIn(index) := io.spi.dq(index)
    io.spi.dq(index) := top.io.spi.dq(index).PAD
    top.io.spi.dq(index).PAD := spiNor.io.dqOut(index)
  }

  // UART RX is pins 15 - drive this pin high to prevent interrupts
  for (index <- 0 until top.io.pins.length if index != 15) {
    io.pins(index) <> top.io.pins(index).PAD
  }
  top.io.pins(15).PAD := analogTrue
  io.pins(15) := analogTrue

}

case class SG13CMOS5LTop() extends Component {
  val resets = List[ResetParameter](
    ResetParameter("system", 4096),
    ResetParameter("debug", 4096),
    ResetParameter("hyperbus", 4096),
    ResetParameter("xip", 4096),
    ResetParameter("peripheral", 4096),
    // Released ~38 us before the CPU so the external SPI flash finishes its
    // reset recovery before the first XIP fetch (see io_plat.spiXip.reset).
    ResetParameter("flash", 256)
  )
  val inputClock = ClockParameter("input", ElemRVFlask.Nitrogen.oscillatorFrequency, "input")
  val clocks = List[ClockParameter](
    ClockParameter("system", inputClock.frequency / 2, "system"),
    ClockParameter("debug", inputClock.frequency / 2, "debug", synchronousWith = "system"),
    ClockParameter("hyperbus", inputClock.frequency, "hyperbus"),
    ClockParameter("xip", inputClock.frequency, "xip"),
    ClockParameter("peripheral", inputClock.frequency / 4, "peripheral")
  )
  val hyperbusPartitions = List[(BigInt, Boolean)](
    (8 MB, true),
    (8 MB, true)
  )
  val kitParameter = KitParameter(resets, clocks, inputClock)
  val boardParameter = ElemRVFlask.Hydrogen.Parameter(kitParameter)
  val socParameter = ElemRV.Parameter(boardParameter)
  val parameter = Nitrogen.Parameter(
    socParameter,
    onChipRamSize = 8 kB,
    spiFlashSize = 512 kB,
    hyperbusPartitions = hyperbusPartitions,
    iCacheSize = 4 kB,
    dCacheSize = 4 kB,
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
        List("system", "debug", "hyperbus", "xip", "peripheral")
      )
      clockCtrl
    },
    onChipRamLogic = (parameter: TileLinkParameter, ramSize: BigInt) => {
      val ram = TileLinkIhpOnChipRam.OnePort(parameter, ramSize.toInt)
      (ram, ram.io.bus)
    }
  )

  val io = new Bundle {
    val clock = IhpCmosIo(Edge.West, 9)
    val reset = IhpCmosIo(Edge.West, 8, "clk_main")
    val jtag = new Bundle {
      val tms = IhpCmosIo(Edge.West, 2, "clk_jtag")
      val tdi = IhpCmosIo(Edge.West, 3, "clk_jtag")
      val tdo = IhpCmosIo(Edge.West, 4, "clk_jtag")
      val tck = IhpCmosIo(Edge.West, 5)
    }
    val hyperbus = new Bundle {
      val cs = Vec(
        IhpCmosIo(Edge.North, 12, "clk_main"),
        IhpCmosIo(Edge.North, 13, "clk_main")
      )
      val ck = IhpCmosIo(Edge.East, 13, "clk_main")
      val reset = IhpCmosIo(Edge.North, 10, "clk_main")
      val rwds = IhpCmosIo(Edge.North, 11, "clk_main")
      val dq = Vec(
        IhpCmosIo(Edge.North, 2, "clk_main"),
        IhpCmosIo(Edge.North, 3, "clk_main"),
        IhpCmosIo(Edge.North, 4, "clk_main"),
        IhpCmosIo(Edge.North, 5, "clk_main"),
        IhpCmosIo(Edge.North, 6, "clk_main"),
        IhpCmosIo(Edge.North, 7, "clk_main"),
        IhpCmosIo(Edge.North, 8, "clk_main"),
        IhpCmosIo(Edge.North, 9, "clk_main")
      )
    }
    val spi = new Bundle {
      val cs = Vec(
        IhpCmosIo(Edge.East, 2, "clk_main")
      )
      val sck = IhpCmosIo(Edge.East, 3, "clk_main")
      val dq = Vec(
        IhpCmosIo(Edge.East, 7, "clk_main"),
        IhpCmosIo(Edge.East, 6, "clk_main"),
        IhpCmosIo(Edge.East, 5, "clk_main"),
        IhpCmosIo(Edge.East, 4, "clk_main")
      )
      val rst = IhpCmosIo(Edge.East, 8, "clk_main")
    }
    val pins = Vec(
      IhpCmosIo(Edge.West, 10, "clk_system"),
      IhpCmosIo(Edge.West, 11, "clk_system"),
      IhpCmosIo(Edge.West, 12, "clk_system"),
      IhpCmosIo(Edge.West, 13, "clk_system"),
      IhpCmosIo(Edge.East, 9, "clk_system"),
      IhpCmosIo(Edge.East, 10, "clk_system"),
      IhpCmosIo(Edge.East, 11, "clk_system"),
      IhpCmosIo(Edge.East, 12, "clk_system"),
      IhpCmosIo(Edge.South, 2, "clk_system"),
      IhpCmosIo(Edge.South, 3, "clk_system"),
      IhpCmosIo(Edge.South, 4, "clk_system"),
      IhpCmosIo(Edge.South, 5, "clk_system"),
      IhpCmosIo(Edge.South, 6, "clk_system"),
      IhpCmosIo(Edge.South, 7, "clk_system"),
      IhpCmosIo(Edge.South, 8, "clk_system"),
      IhpCmosIo(Edge.South, 9, "clk_system"),
      IhpCmosIo(Edge.South, 10, "clk_system"),
      IhpCmosIo(Edge.South, 11, "clk_system"),
      IhpCmosIo(Edge.South, 12, "clk_system"),
      IhpCmosIo(Edge.South, 13, "clk_system")
    )
    val forFutureUse = Vec(
      IhpCmosIo(Edge.West, 6),
      IhpCmosIo(Edge.West, 7)
    )
  }

  val soc = ElemRV(parameter)

  io.clock <> IOPadIn(soc.io_plat.clock)
  io.reset <> IOPadIn(soc.io_plat.reset)

  io.jtag.tms <> IOPadIn(soc.io_plat.jtag.tms)
  io.jtag.tdi <> IOPadIn(soc.io_plat.jtag.tdi)
  io.jtag.tdo <> IOPadOut4mA(soc.io_plat.jtag.tdo)
  io.jtag.tck <> IOPadIn(soc.io_plat.jtag.tck)

  for (index <- 0 until io.hyperbus.cs.length) {
    io.hyperbus.cs(index) <> IOPadOut4mA(soc.io_plat.hyperbus.cs(index))
  }
  io.hyperbus.ck <> IOPadOut4mA(soc.io_plat.hyperbus.ck)
  io.hyperbus.reset <> IOPadOut4mA(soc.io_plat.hyperbus.reset)
  for (index <- 0 until io.hyperbus.dq.length) {
    io.hyperbus.dq(index) <> IOPadInOut4mA(soc.io_plat.hyperbus.dq(index))
  }
  io.hyperbus.rwds <> IOPadInOut4mA(soc.io_plat.hyperbus.rwds)

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
    IhpPowerIo(Edge.South, 0, IhpPowerIoCell.SG13CMOS5L.IOVdd),
    IhpPowerIo(Edge.South, 1, IhpPowerIoCell.SG13CMOS5L.IOVss),
    IhpPowerIo(Edge.South, 14, IhpPowerIoCell.SG13CMOS5L.Vss),
    IhpPowerIo(Edge.South, 15, IhpPowerIoCell.SG13CMOS5L.Vdd),
    IhpPowerIo(Edge.East, 0, IhpPowerIoCell.SG13CMOS5L.IOVdd),
    IhpPowerIo(Edge.East, 1, IhpPowerIoCell.SG13CMOS5L.IOVss),
    IhpPowerIo(Edge.East, 14, IhpPowerIoCell.SG13CMOS5L.Vss),
    IhpPowerIo(Edge.East, 15, IhpPowerIoCell.SG13CMOS5L.Vdd),
    IhpPowerIo(Edge.North, 0, IhpPowerIoCell.SG13CMOS5L.Vdd),
    IhpPowerIo(Edge.North, 1, IhpPowerIoCell.SG13CMOS5L.Vss),
    IhpPowerIo(Edge.North, 14, IhpPowerIoCell.SG13CMOS5L.IOVss),
    IhpPowerIo(Edge.North, 15, IhpPowerIoCell.SG13CMOS5L.IOVdd),
    IhpPowerIo(Edge.West, 0, IhpPowerIoCell.SG13CMOS5L.Vdd),
    IhpPowerIo(Edge.West, 1, IhpPowerIoCell.SG13CMOS5L.Vss),
    IhpPowerIo(Edge.West, 14, IhpPowerIoCell.SG13CMOS5L.IOVss),
    IhpPowerIo(Edge.West, 15, IhpPowerIoCell.SG13CMOS5L.IOVdd)
  )
}

object SG13CMOS5LGenerate extends ElementsApp {
  val topCellName = scala.util.Properties.envOrElse("TOPCELL", "SG13CMOS5LTop")
  val report = elementsConfig.genASICSpinalConfig.ihpSramBlackboxes.generateVerilog {
    val top = SG13CMOS5LTop()
    top.setDefinitionName(topCellName)

    BaremetalTools
      .Header(elementsConfig, "bootrom")
      .generate(top.soc.baremetalDevices, top.soc.baremetalIrqs, top.soc.baremetalErrors)
    BaremetalTools
      .Header(elementsConfig, "demo")
      .generate(top.soc.baremetalDevices, top.soc.baremetalIrqs, top.soc.baremetalErrors)
    RenodeTools.dumpCosimManifest(top.soc, elementsConfig.cosimManifest)

    top
  }

  val spiXip = OpenROADTools.IHP.Config(elementsConfig, OpenROADTools.PDKs.IHP.sg13cmos5l, true)
  spiXip.dieArea = (0, 0, 604.8, 302.4)
  spiXip.coreArea = (22.56, 22.68, 582.24, 279.72)
  spiXip.pdnRingWidth = 5.0
  spiXip.pdnRingSpace = 2.0
  spiXip.addClock(
    report.toplevel.soc.clockCtrl.getClockDomainByName("xip").clock,
    report.toplevel.soc.clockCtrl.getClockDomainByName("xip").frequency.getValue
  )
  spiXip.setIoPinConstraint(report.toplevel.soc.system.spiXip.ctrl.io.bus, "top")
  spiXip.setIoPinConstraint(report.toplevel.soc.system.spiXip.ctrl.io.cfgSpiBus, "top")
  spiXip.setIoPinConstraint(report.toplevel.soc.system.spiXip.ctrl.io.cfgXipBus, "top")
  spiXip.setIoPinConstraint(report.toplevel.soc.system.spiXip.ctrl.io.spi, "right")
  spiXip.setIoPinConstraint(report.toplevel.soc.system.spiXip.ctrl.io.interrupt, "top")
  spiXip.generate("TileLinkSpiXipController")

  val hyperbus = OpenROADTools.IHP.Config(elementsConfig, OpenROADTools.PDKs.IHP.sg13cmos5l, true)
  hyperbus.dieArea = (0, 0, 947.52, 714.42)
  hyperbus.coreArea = (22.56, 22.68, 924.96, 691.74)
  hyperbus.pdnRingWidth = 5.0
  hyperbus.pdnRingSpace = 2.0
  hyperbus.addClock(
    report.toplevel.soc.clockCtrl.getClockDomainByName("hyperbus").clock,
    report.toplevel.soc.clockCtrl.getClockDomainByName("hyperbus").frequency.getValue
  )
  hyperbus.setIoPinConstraint(report.toplevel.soc.system.hyperbus.cluster.io.hyperbus.cs, "top")
  hyperbus.setIoPinConstraint(report.toplevel.soc.system.hyperbus.cluster.io.hyperbus.reset, "top")
  hyperbus.setIoPinConstraint(report.toplevel.soc.system.hyperbus.cluster.io.hyperbus.rwds, "top")
  hyperbus.setIoPinConstraint(report.toplevel.soc.system.hyperbus.cluster.io.hyperbus.dq, "top")
  hyperbus.setIoPinConstraint(report.toplevel.soc.system.hyperbus.cluster.io.hyperbus.ck, "right")
  hyperbus.setIoPinConstraint(report.toplevel.soc.system.hyperbus.cluster.io.dataBus, "bottom")
  hyperbus.setIoPinConstraint(report.toplevel.soc.system.hyperbus.cluster.io.cfgBus, "bottom")
  hyperbus.generate("TileLinkHyperBusGenericPhyCluster")

  val chip = OpenROADTools.IHP.Config(elementsConfig, OpenROADTools.PDKs.IHP.sg13cmos5l)
  chip.setTopCellName(topCellName)
  scala.util.Properties.envOrElse("FLOORPLAN", "tapeout") match {
    case "tapeout" =>
      chip.dieArea = (0, 0, 3233.28, 3235.68)
      chip.coreArea = (394.08, 396.9, 2836.32, 2838.78)
    case "relaxed" =>
      SpinalWarning(
        "FLOORPLAN=relaxed: oversized iteration floorplan - utilization/timing/" +
          "congestion results are NOT tape-out representative."
      )
      chip.dieArea = (0, 0, 3344.64, 3349.08)
      chip.coreArea = (394.08, 396.9, 2947.68, 2952.18)
  }
  chip.hasIoRing = true
  chip.setAbcArea()
  chip.holdSlackMargin = 0.1

  val socCenterX = (chip.coreArea._1 + chip.coreArea._3) / 2
  val cpuChannel = 40.0
  val cpuMargin = 40.0
  val cacheDataWidth = 685.49
  val cacheDataHeight = 385.37
  val cacheTagWidth = 526.03
  val cacheTagHeight = 74.87
  val gshareWidth = 278.51

  val iCacheX = chip.coreArea._1 + cpuChannel - 2
  val gshareX = iCacheX + cacheDataWidth + cpuChannel + 7
  val dCacheX = gshareX + gshareWidth + cpuChannel + 8.5

  val cacheY = chip.coreArea._2 + cpuMargin
  val tagY = cacheY + cacheDataHeight + cpuChannel + 336
  val iCacheTagY = tagY + 4.0
  val dCacheTagY = tagY + 4.0

  chip.addMacro(report.toplevel.soc.core.cpu.iCacheRams(0), iCacheX, cacheY + 14, "MX", depth = 4)
  chip.addMacro(report.toplevel.soc.core.cpu.gshareRams(0), gshareX, cacheY + 26, "MX", depth = 4)
  chip.addMacro(report.toplevel.soc.core.cpu.dCacheRams(0), dCacheX, cacheY + 14, "MX", depth = 4)
  chip.addMacro(
    report.toplevel.soc.core.cpu.iCacheTagRams(0),
    chip.coreArea._1 + cpuMargin,
    iCacheTagY,
    "R0",
    depth = 4
  )
  chip.addMacro(
    report.toplevel.soc.core.cpu.dCacheTagRams(0),
    dCacheX + 150,
    dCacheTagY,
    "R0",
    depth = 4
  )
  chip.addMacro(
    report.toplevel.soc.system.onChipRam.ctrl.asInstanceOf[TileLinkIhpOnChipRam.OnePort].rams(0),
    chip.coreArea._1 + 50.0,
    chip.coreArea._4 - 626.7 - 49.2,
    "R0",
    depth = 3
  )
  chip.addBlock(
    report.toplevel.soc.system.spiXip.ctrl,
    "TileLinkSpiXipController",
    chip.coreArea._3 - spiXip.dieArea._3 - 22,
    cacheY
  )
  chip.addBlock(
    report.toplevel.soc.system.hyperbus.cluster,
    "TileLinkHyperBusGenericPhyCluster",
    chip.coreArea._3 - hyperbus.dieArea._3 - 30,
    chip.coreArea._4 - hyperbus.dieArea._4 - 30.0
  )

  chip.addClock(report.toplevel.io.clock.PAD, report.toplevel.inputClock.frequency, "clk_main")
  chip.addClock(report.toplevel.io.jtag.tck.PAD, 10 MHz, "clk_jtag")
  chip.addGeneratedClock(
    report.toplevel.io.clock.PAD,
    report.toplevel.inputClock.frequency,
    "clk_system",
    report.toplevel.soc.clockCtrl.io.clocks,
    report.toplevel.soc.clockCtrl.getPortIndexByName("system"),
    report.toplevel.soc.clockCtrl.getClockDomainByName("system").frequency.getValue
  )
  chip.addGeneratedClock(
    report.toplevel.io.clock.PAD,
    report.toplevel.inputClock.frequency,
    "clk_debug",
    report.toplevel.soc.clockCtrl.io.clocks,
    report.toplevel.soc.clockCtrl.getPortIndexByName("debug"),
    report.toplevel.soc.clockCtrl.getClockDomainByName("debug").frequency.getValue
  )
  chip.addGeneratedClock(
    report.toplevel.io.clock.PAD,
    report.toplevel.inputClock.frequency,
    "clk_peripheral",
    report.toplevel.soc.clockCtrl.io.clocks,
    report.toplevel.soc.clockCtrl.getPortIndexByName("peripheral"),
    report.toplevel.soc.clockCtrl.getClockDomainByName("peripheral").frequency.getValue
  )
  chip.addGeneratedClock(
    report.toplevel.io.clock.PAD,
    report.toplevel.inputClock.frequency,
    "clk_hyperbus",
    report.toplevel.soc.clockCtrl.io.clocks,
    report.toplevel.soc.clockCtrl.getPortIndexByName("hyperbus"),
    report.toplevel.soc.clockCtrl.getClockDomainByName("hyperbus").frequency.getValue
  )
  chip.addGeneratedClock(
    report.toplevel.io.clock.PAD,
    report.toplevel.inputClock.frequency,
    "clk_xip",
    report.toplevel.soc.clockCtrl.io.clocks,
    report.toplevel.soc.clockCtrl.getPortIndexByName("xip"),
    report.toplevel.soc.clockCtrl.getClockDomainByName("xip").frequency.getValue
  )
  chip.addReset(report.toplevel.io.reset.PAD)
  chip.setAsynchronousClockGroups(
    Seq("clk_main", "clk_system", "clk_debug"),
    Seq("clk_peripheral"),
    Seq("clk_hyperbus"),
    Seq("clk_xip"),
    Seq("clk_jtag")
  )
  chip.io = Some(report.toplevel.io)
  chip.ioPower = Some(report.toplevel.power)
  chip.pdnRingWidth = 30.0
  chip.pdnRingSpace = 5.0
  chip.pdnTopMetal1Pitch = 52
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
