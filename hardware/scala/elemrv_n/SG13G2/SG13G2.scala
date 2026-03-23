// SPDX-FileCopyrightText: 2025 aesc silicon
//
// SPDX-License-Identifier: CERN-OHL-W-2.0

package elemrv_n

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
import zibal.platform.Nitrogen
import zibal.board.{KitParameter, BoardParameter}
import zibal.sim.hyperram.W956A8MBYA
import zibal.sim.MT25Q

import elements.sdk.ElementsApp
import elements.board.ElemRVBoard

case class SG13G2Board() extends Component {
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
    }
    val pins = Vec(inout(Analog(Bool())), 20)
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

  for (index <- 0 until top.io.forFutureUse.length) {
    top.io.forFutureUse(index).PAD := analogFalse
  }

  val w956a8mbya = W956A8MBYA()
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

  io.pins(15) := analogTrue

}

case class SG13G2Top() extends Component {
  val resets = List[ResetParameter](
    ResetParameter("system", 128),
    ResetParameter("cpu", 128),
    ResetParameter("hyperbus", 128),
    ResetParameter("spiXip", 128),
    ResetParameter("debug", 128)
  )
  val inputClock = ClockParameter("input", 60 MHz, "input")
  val clocks = List[ClockParameter](
    ClockParameter("system", 30 MHz, "system"),
    ClockParameter("cpu", 30 MHz, "cpu", synchronousWith = "system"),
    ClockParameter("hyperbus", 60 MHz, "hyperbus"),
    ClockParameter("spiXip", 60 MHz, "spiXip"),
    ClockParameter("debug", 7.5 MHz, "debug", synchronousWith = "system")
  )
  val hyperbusPartitions = List[(BigInt, Boolean)](
    (8 MB, true),
    (8 MB, true)
  )
  val kitParameter = KitParameter(resets, clocks)
  val boardParameter = ElemRVBoard.Parameter(kitParameter)
  val socParameter = ElemRV.Parameter(boardParameter)
  val parameter = Nitrogen.Parameter(
    socParameter,
    4 kB,
    8 MB,
    hyperbusPartitions,
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
        List("system", "cpu", "hyperbus", "spiXip", "debug")
      )
      clockCtrl
    },
    (parameter: BmbParameter, ramSize: BigInt) => {
      val ram = BmbIhpOnChipRam.OnePort1Macro(parameter, ramSize.toInt)
      (ram, ram.io.bus)
    }
  )

  val io = new Bundle {
    val clock = IhpCmosIo(Edge.South, 13)
    val reset = IhpCmosIo(Edge.South, 12, "clk_main")
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
    }
    val pins = Vec(
      IhpCmosIo(Edge.West, 8, "clk_system"),
      IhpCmosIo(Edge.West, 9, "clk_system"),
      IhpCmosIo(Edge.West, 10, "clk_system"),
      IhpCmosIo(Edge.West, 11, "clk_system"),
      IhpCmosIo(Edge.West, 12, "clk_system"),
      IhpCmosIo(Edge.West, 13, "clk_system"),
      IhpCmosIo(Edge.East, 8, "clk_system"),
      IhpCmosIo(Edge.East, 9, "clk_system"),
      IhpCmosIo(Edge.East, 10, "clk_system"),
      IhpCmosIo(Edge.East, 11, "clk_system"),
      IhpCmosIo(Edge.South, 2, "clk_system"),
      IhpCmosIo(Edge.South, 3, "clk_system"),
      IhpCmosIo(Edge.South, 4, "clk_system"),
      IhpCmosIo(Edge.South, 5, "clk_system"),
      IhpCmosIo(Edge.South, 6, "clk_system"),
      IhpCmosIo(Edge.South, 7, "clk_system"),
      IhpCmosIo(Edge.South, 8, "clk_system"),
      IhpCmosIo(Edge.South, 9, "clk_system"),
      IhpCmosIo(Edge.South, 10, "clk_system"),
      IhpCmosIo(Edge.South, 11, "clk_system")
    )
    val forFutureUse = Vec(
      IhpCmosIo(Edge.East, 12),
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
    io.hyperbus.cs(index) <> IOPadOut30mA(soc.io_plat.hyperbus.cs(index))
  }
  io.hyperbus.ck <> IOPadOut30mA(soc.io_plat.hyperbus.ck)
  io.hyperbus.reset <> IOPadOut4mA(soc.io_plat.hyperbus.reset)
  for (index <- 0 until io.hyperbus.dq.length) {
    io.hyperbus.dq(index) <> IOPadInOut30mA(soc.io_plat.hyperbus.dq(index))
  }
  io.hyperbus.rwds <> IOPadInOut30mA(soc.io_plat.hyperbus.rwds)

  for (index <- 0 until io.spi.cs.length) {
    io.spi.cs(index) <> IOPadOut30mA(soc.io_plat.spi.cs(index))
  }
  io.spi.sck <> IOPadOut4mA(soc.io_plat.spi.sclk)
  for (index <- 0 until io.spi.dq.length) {
    io.spi.dq(index) <> IOPadInOut30mA(soc.io_plat.spi.dq(index))
  }

  for (index <- 0 until io.pins.length) {
    io.pins(index) <> IOPadInOut30mA(soc.io.pins.pins(index))
  }

  val forFutureUse = False
  for (index <- 0 until io.forFutureUse.length) {
    io.forFutureUse(index) <> IOPadOut4mA(forFutureUse)
  }

  val power = Seq(
    IhpPowerIo(Edge.South, 0, IhpPowerIoCell.SG13G2.IOVdd),
    IhpPowerIo(Edge.South, 1, IhpPowerIoCell.SG13G2.IOVss),
    IhpPowerIo(Edge.South, 14, IhpPowerIoCell.SG13G2.Vss),
    IhpPowerIo(Edge.South, 15, IhpPowerIoCell.SG13G2.Vdd),
    IhpPowerIo(Edge.East, 0, IhpPowerIoCell.SG13G2.IOVdd),
    IhpPowerIo(Edge.East, 1, IhpPowerIoCell.SG13G2.IOVss),
    IhpPowerIo(Edge.East, 14, IhpPowerIoCell.SG13G2.Vss),
    IhpPowerIo(Edge.East, 15, IhpPowerIoCell.SG13G2.Vdd),
    IhpPowerIo(Edge.North, 0, IhpPowerIoCell.SG13G2.Vdd),
    IhpPowerIo(Edge.North, 1, IhpPowerIoCell.SG13G2.Vss),
    IhpPowerIo(Edge.North, 14, IhpPowerIoCell.SG13G2.IOVss),
    IhpPowerIo(Edge.North, 15, IhpPowerIoCell.SG13G2.IOVdd),
    IhpPowerIo(Edge.West, 0, IhpPowerIoCell.SG13G2.Vdd),
    IhpPowerIo(Edge.West, 1, IhpPowerIoCell.SG13G2.Vss),
    IhpPowerIo(Edge.West, 14, IhpPowerIoCell.SG13G2.IOVss),
    IhpPowerIo(Edge.West, 15, IhpPowerIoCell.SG13G2.IOVdd)
  )
}

object SG13G2Generate extends ElementsApp {
  val report = elementsConfig.genASICSpinalConfig.generateVerilog {
    val top = SG13G2Top()
    top.soc.prepareBaremetal("bootrom", elementsConfig)
    top.soc.prepareBaremetal("demo", elementsConfig)

    top
  }

  val hyperbus = OpenROADTools.IHP.Config(elementsConfig, OpenROADTools.PDKs.IHP.sg13g2, true)
  hyperbus.dieArea = (0, 0, 829.92, 343.98)
  hyperbus.coreArea = (22.56, 22.68, 807.36, 321.3)
  hyperbus.holdSlackMargin = 0.05
  hyperbus.pdnRingWidth = 5.0
  hyperbus.pdnRingSpace = 2.0
  hyperbus.addClock(
    report.toplevel.soc.clockCtrl.getClockDomainByName("hyperbus").clock,
    report.toplevel.soc.clockCtrl.getClockDomainByName("hyperbus").frequency.getValue
  )
  hyperbus.setIoPinConstraint(report.toplevel.soc.hyperbus.ctrl.io.hyperbus, "top")
  hyperbus.setIoPinConstraint(report.toplevel.soc.hyperbus.ctrl.io.dataBus, "bottom")
  hyperbus.setIoPinConstraint(report.toplevel.soc.hyperbus.ctrl.io.cfgBus, "bottom")
  hyperbus.generate("BmbHyperBusGenericPhyCluster")

  val spiXip = OpenROADTools.IHP.Config(elementsConfig, OpenROADTools.PDKs.IHP.sg13g2, true)
  spiXip.dieArea = (0, 0, 200.16, 446.04)
  spiXip.coreArea = (22.56, 22.68, 177.6, 423.36)
  spiXip.pdnRingWidth = 5.0
  spiXip.pdnRingSpace = 2.0
  spiXip.addClock(
    report.toplevel.soc.clockCtrl.getClockDomainByName("spiXip").clock,
    report.toplevel.soc.clockCtrl.getClockDomainByName("spiXip").frequency.getValue
  )
  spiXip.setIoPinConstraint(report.toplevel.soc.spiXip.ctrl.io.dataBus, "left")
  spiXip.setIoPinConstraint(report.toplevel.soc.spiXip.ctrl.io.cfgSpiBus, "left")
  spiXip.setIoPinConstraint(report.toplevel.soc.spiXip.ctrl.io.cfgXipBus, "left")
  spiXip.setIoPinConstraint(report.toplevel.soc.spiXip.ctrl.io.spi, "right")
  spiXip.setIoPinConstraint(report.toplevel.soc.spiXip.ctrl.io.interrupt, "top")
  spiXip.generate("BmbSpiXipController")

  val cpu = OpenROADTools.IHP.Config(elementsConfig, OpenROADTools.PDKs.IHP.sg13g2, true)
  cpu.dieArea = (0, 0, 997.92, 997.92)
  cpu.coreArea = (22.56, 22.68, 975.36, 975.24)
  cpu.holdSlackMargin = 0.2
  cpu.pdnRingWidth = 6.0
  cpu.pdnRingSpace = 2.0
  cpu.pdnMetal5Pitch = 20.0
  cpu.addClock(
    report.toplevel.soc.clockCtrl.getClockDomainByName("cpu").clock,
    report.toplevel.soc.clockCtrl.getClockDomainByName("cpu").frequency.getValue
  )
  cpu.addMacro(report.toplevel.soc.core.internal.iCacheBanks(0).bank, 47.52, 41.58, "MX")
  cpu.addMacro(
    report.toplevel.soc.core.internal.iCacheTags(0).addr,
    47.52,
    427.15,
    "R0"
  )
  cpu.addMacro(
    report.toplevel.soc.core.internal.dCacheWays(0).addr,
    47.52,
    540.54,
    "Mx"
  )
  cpu.addMacro(report.toplevel.soc.core.internal.dCacheWays(0).data, 47.52, 820.15, "R0")
  cpu.setIoPinConstraint(report.toplevel.soc.core.internal.iBus, "right")
  cpu.setIoPinConstraint(report.toplevel.soc.core.internal.dBus, "right")
  cpu.setIoPinConstraint(report.toplevel.soc.core.internal.externalInterrupt, "top")
  cpu.setIoPinConstraint(report.toplevel.soc.core.internal.timerInterrupt, "top")
  cpu.setIoPinConstraint(report.toplevel.soc.core.internal.debugBus, "top")
  cpu.setIoPinConstraint(report.toplevel.soc.core.internal.debugResetOut, "top")
  cpu.generate("VexRiscv")

  val chip = OpenROADTools.IHP.Config(elementsConfig, OpenROADTools.PDKs.IHP.sg13g2)
  chip.dieArea = (0, 0, 2420.64, 2419.2)
  chip.coreArea = (394.08, 396.9, 2023.68, 2022.3)
  chip.hasIoRing = true
  chip.addBlock(report.toplevel.soc.core.cpu, "VexRiscv", 394.08, 396.9)
  chip.addBlock(report.toplevel.soc.hyperbus.ctrl, "BmbHyperBusGenericPhyCluster", 1193.76, 1678.32)
  chip.addBlock(report.toplevel.soc.spiXip.ctrl, "BmbSpiXipController", 1823.52, 700)
  chip.addMacro(
    report.toplevel.soc.system.onChipRam.ctrl.asInstanceOf[BmbIhpOnChipRam.OnePort1Macro].ram,
    444.96,
    1772.3,
    depth = 3
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
    "clk_cpu",
    report.toplevel.soc.clockCtrl.io.clocks,
    report.toplevel.soc.clockCtrl.getPortIndexByName("cpu"),
    report.toplevel.soc.clockCtrl.getClockDomainByName("cpu").frequency.getValue
  )
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
  chip.setFalsePath("clk_cpu", "clk_jtag")
  chip.setFalsePath("clk_debug", "clk_jtag")
  chip.io = Some(report.toplevel.io)
  chip.ioPower = Some(report.toplevel.power)
  chip.pdnRingWidth = 30.0
  chip.pdnRingSpace = 5.0
  chip.pdnTopMetal2Pitch = 55
  chip.generate
}

object SG13G2Simulate extends ElementsApp {
  val compiled = elementsConfig.genFPGASimConfig.compile {
    val board = SG13G2Board()
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
        testCases.uartRxIdle(dut.io.pins(14))
      }
  }
}
