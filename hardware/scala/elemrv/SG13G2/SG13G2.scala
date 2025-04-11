package elemrv

import spinal.core._
import spinal.core.sim._
import spinal.lib._

import nafarr.system.reset._
import nafarr.system.reset.ResetControllerCtrl._
import nafarr.system.clock._
import nafarr.system.clock.ClockControllerCtrl._
import nafarr.blackboxes.ihp.sg13g2._
import nafarr.memory.ocram.ihp.sg13g2.Axi4SharedIhpOnChipRam

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
      val mosi = inout(Analog(Bool))
      val miso = inout(Analog(Bool))
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

  val spiNor = MT25Q()
  spiNor.io.clock := io.clock
  spiNor.io.dataClock := io.spi.sck
  spiNor.io.reset := io.reset
  spiNor.io.chipSelect := io.spi.cs
  spiNor.io.dataIn := io.spi.mosi
  top.io.spi.dq(1).PAD := spiNor.io.dataOut
  io.spi.cs := top.io.spi.cs(0).PAD
  io.spi.sck := top.io.spi.sck.PAD
  io.spi.mosi := top.io.spi.dq(0).PAD
  top.io.spi.dq(1).PAD := io.spi.miso
  top.io.spi.dq(2).PAD := analogFalse
  top.io.spi.dq(3).PAD := analogFalse

  for (index <- 0 until top.io.pins.length) {
    io.pins(index) <> top.io.pins(index).PAD
  }
}

case class SG13G2Top() extends Component {
  val resets = List[ResetParameter](ResetParameter("system", 128), ResetParameter("debug", 128))
  val clocks = List[ClockParameter](
    ClockParameter("system", 20 MHz, "system"),
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
    32,
    8 MB,
    hyperbusPartitions,
    (resetCtrl: ResetControllerCtrl, reset: Bool, _) => {
      resetCtrl.buildDummy(reset)
    },
    (clockCtrl: ClockControllerCtrl, resetCtrl: ResetControllerCtrl, clock: Bool) => {
      clockCtrl.buildDummy(clock)
    },
    (ramSize: BigInt) => {
      val ram = Axi4SharedIhpOnChipRam.OnePort1024x8(
        dataWidth = 32,
        byteCount = ramSize,
        idWidth = 4
      )
      (ram, ram.io.axi, ram.engine.ram)
    }
  )

  val io = new Bundle {
    val clock = IhpCmosIo("south", 13)
    val reset = IhpCmosIo("south", 12)
    val jtag = new Bundle {
      val tms = IhpCmosIo("west", 2)
      val tdi = IhpCmosIo("west", 3)
      val tdo = IhpCmosIo("west", 4)
      val tck = IhpCmosIo("west", 5)
    }
    val hyperbus = new Bundle {
      val cs = Vec(
        IhpCmosIo("north", 12),
        IhpCmosIo("north", 13)
      )
      val ck = IhpCmosIo("east", 13)
      val reset = IhpCmosIo("north", 10)
      val rwds = IhpCmosIo("north", 11)
      val dq = Vec(
        IhpCmosIo("north", 2),
        IhpCmosIo("north", 3),
        IhpCmosIo("north", 4),
        IhpCmosIo("north", 5),
        IhpCmosIo("north", 6),
        IhpCmosIo("north", 7),
        IhpCmosIo("north", 8),
        IhpCmosIo("north", 9)
      )
    }
    val spi = new Bundle {
      val cs = Vec(
        IhpCmosIo("east", 2)
      )
      val sck = IhpCmosIo("east", 3)
      val dq = Vec(
        IhpCmosIo("east", 7),
        IhpCmosIo("east", 6),
        IhpCmosIo("east", 5),
        IhpCmosIo("east", 4)
      )
    }
    val pins = Vec(
      IhpCmosIo("west", 8),
      IhpCmosIo("west", 9),
      IhpCmosIo("west", 10),
      IhpCmosIo("west", 11),
      IhpCmosIo("west", 12),
      IhpCmosIo("west", 13),
      IhpCmosIo("east", 8),
      IhpCmosIo("east", 9),
      IhpCmosIo("east", 10),
      IhpCmosIo("east", 11),
      IhpCmosIo("south", 2),
      IhpCmosIo("south", 3),
      IhpCmosIo("south", 4),
      IhpCmosIo("south", 5),
      IhpCmosIo("south", 6),
      IhpCmosIo("south", 7),
      IhpCmosIo("south", 8),
      IhpCmosIo("south", 9),
      IhpCmosIo("south", 10),
      IhpCmosIo("south", 11)
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
    val config = OpenROADTools.IHP.Config(elementsConfig)
    config.generate(
      OpenROADTools.PDKs.IHP.sg13g2,
      (0, 0, 2522.4, 2521.26),
      (394.08, 396.9, 2125.44, 2124.36)
    )

    val macros = OpenROADTools.IHP.Macros(elementsConfig)
    macros.addMacro(
      top.soc.system.onChipCtrl.asInstanceOf[Axi4SharedIhpOnChipRam.OnePort1024x8].engine.ram,
      444.96,
      448.35,
      "MX"
    )
    macros.addMacro(top.soc.peripherals.aesCtrl.ctrl.ram, 936.48, 448.35, "MX")
    macros.generate()

    val sdc = OpenROADTools.IHP.Sdc(elementsConfig)
    sdc.addClock(top.io.clock.PAD, 20 MHz, "clk_core")
    sdc.addClock(top.io.jtag.tck.PAD, 10 MHz, "clk_jtag")
    sdc.setFalsePath("clk_core", "clk_jtag")
    sdc.generate(top.io)

    val io = OpenROADTools.IHP.Io(elementsConfig)
    io.addPad("south", 0, "sg13g2_IOPadIOVdd")
    io.addPad("south", 1, "sg13g2_IOPadIOVss")
    io.addPad("south", 14, "sg13g2_IOPadVss")
    io.addPad("south", 15, "sg13g2_IOPadVdd")
    io.addPad("east", 0, "sg13g2_IOPadIOVdd")
    io.addPad("east", 1, "sg13g2_IOPadIOVss")
    io.addPad("east", 14, "sg13g2_IOPadVss")
    io.addPad("east", 15, "sg13g2_IOPadVdd")
    io.addPad("north", 0, "sg13g2_IOPadVdd")
    io.addPad("north", 1, "sg13g2_IOPadVss")
    io.addPad("north", 14, "sg13g2_IOPadIOVss")
    io.addPad("north", 15, "sg13g2_IOPadIOVdd")
    io.addPad("west", 0, "sg13g2_IOPadVdd")
    io.addPad("west", 1, "sg13g2_IOPadVss")
    io.addPad("west", 14, "sg13g2_IOPadIOVss")
    io.addPad("west", 15, "sg13g2_IOPadIOVdd")
    io.generate(top.io)

    val pdn = OpenROADTools.IHP.Pdn(elementsConfig)
    pdn.generate()

    top.soc.prepareBaremetal("bootrom", elementsConfig)
    top.soc.prepareBaremetal("demo", elementsConfig)

    top
  }
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
