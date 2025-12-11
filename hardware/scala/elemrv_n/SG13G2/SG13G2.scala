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
import nafarr.blackboxes.ihp.sg13g2._
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
    io.hyperbus.dq(index) := w956a8mbya.io.dqOut(index)
  }
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
}

case class SG13G2Top() extends Component {
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
    (resetCtrl: ResetControllerCtrl, reset: Bool, _) => {
      resetCtrl.buildDummy(reset)
    },
    (clockCtrl: ClockControllerCtrl, resetCtrl: ResetControllerCtrl, clock: Bool) => {
      clockCtrl.buildDummy(clock, resetCtrl)
    },
    (parameter: BmbParameter, ramSize: BigInt) => {
      val ram = BmbIhpOnChipRam.OnePort1Macro(parameter, ramSize.toInt)
      (ram, ram.io.bus)
    }
  )

  val io = new Bundle {
    val clock = IhpCmosIo("south", 13)
    val reset = IhpCmosIo("south", 12, "clk_main")
    val jtag = new Bundle {
      val tms = IhpCmosIo("west", 2, "clk_jtag")
      val tdi = IhpCmosIo("west", 3, "clk_jtag")
      val tdo = IhpCmosIo("west", 4, "clk_jtag")
      val tck = IhpCmosIo("west", 5, "clk_jtag")
    }
    val hyperbus = new Bundle {
      val cs = Vec(
        IhpCmosIo("north", 12, "clk_main"),
        IhpCmosIo("north", 13, "clk_main")
      )
      val ck = IhpCmosIo("east", 13, "clk_main")
      val reset = IhpCmosIo("north", 10, "clk_main")
      val rwds = IhpCmosIo("north", 11, "clk_main")
      val dq = Vec(
        IhpCmosIo("north", 2, "clk_main"),
        IhpCmosIo("north", 3, "clk_main"),
        IhpCmosIo("north", 4, "clk_main"),
        IhpCmosIo("north", 5, "clk_main"),
        IhpCmosIo("north", 6, "clk_main"),
        IhpCmosIo("north", 7, "clk_main"),
        IhpCmosIo("north", 8, "clk_main"),
        IhpCmosIo("north", 9, "clk_main")
      )
    }
    val spi = new Bundle {
      val cs = Vec(
        IhpCmosIo("east", 2, "clk_main")
      )
      val sck = IhpCmosIo("east", 3, "clk_main")
      val dq = Vec(
        IhpCmosIo("east", 7, "clk_main"),
        IhpCmosIo("east", 6, "clk_main"),
        IhpCmosIo("east", 5, "clk_main"),
        IhpCmosIo("east", 4, "clk_main")
      )
    }
    val pins = Vec(
      IhpCmosIo("west", 8, "clk_main"),
      IhpCmosIo("west", 9, "clk_main"),
      IhpCmosIo("west", 10, "clk_main"),
      IhpCmosIo("west", 11, "clk_main"),
      IhpCmosIo("west", 12, "clk_main"),
      IhpCmosIo("west", 13, "clk_main"),
      IhpCmosIo("east", 8, "clk_main"),
      IhpCmosIo("east", 9, "clk_main"),
      IhpCmosIo("east", 10, "clk_main"),
      IhpCmosIo("east", 11, "clk_main"),
      IhpCmosIo("south", 2, "clk_main"),
      IhpCmosIo("south", 3, "clk_main"),
      IhpCmosIo("south", 4, "clk_main"),
      IhpCmosIo("south", 5, "clk_main"),
      IhpCmosIo("south", 6, "clk_main"),
      IhpCmosIo("south", 7, "clk_main"),
      IhpCmosIo("south", 8, "clk_main"),
      IhpCmosIo("south", 9, "clk_main"),
      IhpCmosIo("south", 10, "clk_main"),
      IhpCmosIo("south", 11, "clk_main")
    )
    val forFutureUse = Vec(
      IhpCmosIo("east", 12),
      IhpCmosIo("west", 6),
      IhpCmosIo("west", 7)
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
  io.hyperbus.ck <> IOPadOut30mA(soc.io_plat.hyperbus.ck)
  io.hyperbus.reset <> IOPadOut4mA(soc.io_plat.hyperbus.reset)
  for (index <- 0 until io.hyperbus.dq.length) {
    io.hyperbus.dq(index) <> IOPadInOut30mA(soc.io_plat.hyperbus.dq(index))
  }
  io.hyperbus.rwds <> IOPadInOut30mA(soc.io_plat.hyperbus.rwds)

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

  for (index <- 0 until io.forFutureUse.length) {
    io.forFutureUse(index) <> IOPadIn(False)
  }

  for (index <- 0 until 4) { IOPadIOVdd() }
  for (index <- 0 until 4) { IOPadIOVss() }
  for (index <- 0 until 4) { IOPadVdd() }
  for (index <- 0 until 4) { IOPadVss() }
}

object SG13G2Generate extends ElementsApp {
  val report = elementsConfig.genASICSpinalConfig.generateVerilog {
    val top = SG13G2Top()
    top.soc.prepareBaremetal("bootrom", elementsConfig)
    top.soc.prepareBaremetal("demo", elementsConfig)

    top
  }

  val cpu = OpenROADTools.IHP.Config(elementsConfig, OpenROADTools.PDKs.IHP.sg13g2, true)
  cpu.dieArea = (0, 0, 799.68, 797.58)
  cpu.coreArea = (22.56, 22.68, 780, 778.68)
  cpu.pdnRingWidth = 5.0
  cpu.pdnRingSpace = 2.0
  cpu.addClock(report.toplevel.soc.clockCtrl.getClockDomainByName("cpu").clock, 20 MHz)
  cpu.generate("VexRiscv")

  val chip = OpenROADTools.IHP.Config(elementsConfig, OpenROADTools.PDKs.IHP.sg13g2)
  chip.dieArea = (0, 0, 2522.4, 2521.26)
  chip.coreArea = (394.08, 396.9, 2125.44, 2124.36)
  chip.hasIoRing = true
  chip.addBlock("VexRiscv")
  chip.addMacro(
    report.toplevel.soc.system.onChipRam.ctrl.asInstanceOf[BmbIhpOnChipRam.OnePort1Macro].ram,
    444.96,
    448.35,
    "MX"
  )
  chip.addClock(report.toplevel.io.clock.PAD, 20 MHz, "clk_main")
  chip.addClock(report.toplevel.io.jtag.tck.PAD, 10 MHz, "clk_jtag")
  chip.setFalsePath("clk_main", "clk_jtag")
  chip.io = Some(report.toplevel.io)
  chip.pdnRingWidth = 30.0
  chip.pdnRingSpace = 5.0
  chip.addPad("south", 0, "sg13g2_IOPadIOVdd")
  chip.addPad("south", 1, "sg13g2_IOPadIOVss")
  chip.addPad("south", 14, "sg13g2_IOPadVss")
  chip.addPad("south", 15, "sg13g2_IOPadVdd")
  chip.addPad("east", 0, "sg13g2_IOPadIOVdd")
  chip.addPad("east", 1, "sg13g2_IOPadIOVss")
  chip.addPad("east", 14, "sg13g2_IOPadVss")
  chip.addPad("east", 15, "sg13g2_IOPadVdd")
  chip.addPad("north", 0, "sg13g2_IOPadVdd")
  chip.addPad("north", 1, "sg13g2_IOPadVss")
  chip.addPad("north", 14, "sg13g2_IOPadIOVss")
  chip.addPad("north", 15, "sg13g2_IOPadIOVdd")
  chip.addPad("west", 0, "sg13g2_IOPadVdd")
  chip.addPad("west", 1, "sg13g2_IOPadVss")
  chip.addPad("west", 14, "sg13g2_IOPadIOVss")
  chip.addPad("west", 15, "sg13g2_IOPadIOVdd")
  chip.generate
}

object SG13G2Simulate extends ElementsApp {
  val compiled = elementsConfig.genFPGASimConfig.compile {
    val board = SG13G2Board()
    BinTools.initRam(board.spiNor.deviceOut.data, elementsConfig.swStorageBaremetalImage("bootrom"))
    BinTools.initRam(board.w956a8mbya.device.data, elementsConfig.swStorageBaremetalImage("demo"))
    board
  }
  simType match {
    case "simulate" =>
      compiled.doSimUntilVoid("simulate") { dut =>
        val testCases = TestCases()
        testCases.addClock(
          dut.io.clock,
          ElemRVBoard.SystemClock.frequency,
          simDuration.toString.toInt ms
        )
        testCases.addReset(dut.io.reset, 100 us)
        testCases.uartRxIdle(dut.io.pins(14))
      }
  }
}
