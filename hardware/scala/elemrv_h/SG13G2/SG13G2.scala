// SPDX-FileCopyrightText: 2025 aesc silicon
//
// SPDX-License-Identifier: CERN-OHL-W-2.0

package elemrv_h

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
  val clocks = List[ClockParameter](
    ClockParameter("system", 50 MHz, "system"),
    ClockParameter("debug", 10 MHz, "debug", synchronousWith = "system")
  )
  val kitParameter = KitParameter(resets, clocks)
  val boardParameter = ElemRVBoard.Parameter(kitParameter)
  val socParameter = ElemRV.Parameter(boardParameter)
  val parameter = Hydrogen.Parameter(
    socParameter,
    8 kB,
    8 MB,
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
    val clock = IhpCmosIo("south", 5)
    val reset = IhpCmosIo("south", 4, "clk_main")
    val jtag = new Bundle {
      val tms = IhpCmosIo("south", 0, "clk_jtag")
      val tdi = IhpCmosIo("south", 1, "clk_jtag")
      val tdo = IhpCmosIo("south", 2, "clk_jtag")
      val tck = IhpCmosIo("south", 3, "clk_jtag")
    }
    val spi = new Bundle {
      val cs = Vec(
        IhpCmosIo("east", 0, "clk_main")
      )
      val sck = IhpCmosIo("east", 1, "clk_main")
      val dq = Vec(
        IhpCmosIo("east", 5, "clk_main"),
        IhpCmosIo("east", 4, "clk_main"),
        IhpCmosIo("east", 3, "clk_main"),
        IhpCmosIo("east", 2, "clk_main")
      )
    }
    val pins = Vec(
      IhpCmosIo("west", 2, "clk_main"),
      IhpCmosIo("west", 3, "clk_main"),
      IhpCmosIo("west", 4, "clk_main"),
      IhpCmosIo("west", 5, "clk_main"),
      IhpCmosIo("west", 6, "clk_main"),
      IhpCmosIo("west", 7, "clk_main"),
      IhpCmosIo("north", 2, "clk_main"),
      IhpCmosIo("north", 3, "clk_main"),
      IhpCmosIo("north", 4, "clk_main"),
      IhpCmosIo("north", 5, "clk_main"),
      IhpCmosIo("north", 6, "clk_main"),
      IhpCmosIo("north", 7, "clk_main")
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

  for (index <- 0 until 2) { IOPadIOVdd() }
  for (index <- 0 until 2) { IOPadIOVss() }
  for (index <- 0 until 2) { IOPadVdd() }
  for (index <- 0 until 2) { IOPadVss() }
}

object SG13G2Generate extends ElementsApp {
  val report = elementsConfig.genASICSpinalConfig.generateVerilog {
    val top = SG13G2Top()

    top.soc.prepareBaremetal("demo", elementsConfig)

    top
  }

  val chip = OpenROADTools.IHP.Config(elementsConfig, OpenROADTools.PDKs.IHP.sg13g2)
  chip.dieArea = (0, 0, 1881.6, 1882.44)
  chip.coreArea = (394.08, 396.9, 1484.64, 1485.54)
  chip.hasIoRing = true
  chip.addMacro(
    report.toplevel.soc.system.onChipRam.ctrl.asInstanceOf[BmbIhpOnChipRam.OnePort1Macro].ram,
    444.96,
    448.35,
    "MX"
  )

  chip.addClock(report.toplevel.io.clock.PAD, 50 MHz, "clk_main")
  chip.addClock(report.toplevel.io.jtag.tck.PAD, 10 MHz, "clk_jtag")
  chip.setFalsePath("clk_main", "clk_jtag")
  chip.io = Some(report.toplevel.io)
  chip.pdnRingWidth = 30.0
  chip.pdnRingSpace = 5.0
  chip.addPad("south", 6, "sg13g2_IOPadVss")
  chip.addPad("south", 7, "sg13g2_IOPadVdd")
  chip.addPad("east", 6, "sg13g2_IOPadIOVdd")
  chip.addPad("east", 7, "sg13g2_IOPadIOVss")
  chip.addPad("north", 0, "sg13g2_IOPadVss")
  chip.addPad("north", 1, "sg13g2_IOPadVdd")
  chip.addPad("west", 0, "sg13g2_IOPadIOVdd")
  chip.addPad("west", 1, "sg13g2_IOPadIOVss")
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
