package elemrv

import spinal.core._
import spinal.core.sim._
import spinal.lib._

import nafarr.system.reset._
import nafarr.system.reset.ResetControllerCtrl._
import nafarr.system.clock._
import nafarr.system.clock.ClockControllerCtrl._
import nafarr.blackboxes.ihp.sg13g2._

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
    val uartStd = new Bundle {
      val txd = inout(Analog(Bool))
      val rxd = inout(Analog(Bool))
    }
    val gpioStatus = Vec(inout(Analog(Bool())), 4)
    val hyperbus = new Bundle {
      val cs = inout(Analog(Bool))
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
  }

  val top = SG13G2Top()
  val analogFalse = Analog(Bool)
  analogFalse := False
  val analogTrue = Analog(Bool)
  analogTrue := True

  top.io.clock.PAD := io.clock
  top.io.reset.PAD := io.reset

  top.io.uartStd.rxd.PAD := io.uartStd.rxd
  io.uartStd.txd := top.io.uartStd.txd.PAD
  top.io.uartStd.cts.PAD := analogFalse
  analogFalse := top.io.uartStd.rts.PAD

  top.io.jtag.tms.PAD := analogFalse
  top.io.jtag.tck.PAD := analogFalse
  top.io.jtag.tdi.PAD := analogFalse
  analogFalse := top.io.jtag.tdo.PAD
  for (index <- 0 until top.io.gpioStatus.length) {
    io.gpioStatus(index) <> top.io.gpioStatus(index).PAD
  }

  val w956a8mbya = W956A8MBYA()
  w956a8mbya.io.ck := io.hyperbus.ck
  w956a8mbya.io.ckN := analogFalse
  for (index <- 0 until top.io.hyperbus.dq.length) {
    w956a8mbya.io.dqIn(index) := io.hyperbus.dq(index)
    io.hyperbus.dq(index) := w956a8mbya.io.dqOut(index)
  }
  w956a8mbya.io.rwdsIn := io.hyperbus.rwds
  io.hyperbus.rwds := w956a8mbya.io.rwdsOut
  w956a8mbya.io.csN := io.hyperbus.cs
  w956a8mbya.io.resetN := io.hyperbus.reset

  io.hyperbus.cs := top.io.hyperbus.cs(0).PAD
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
}

case class SG13G2Top() extends Component {
  val resets = List[ResetParameter](ResetParameter("system", 128), ResetParameter("debug", 128))
  val clocks = List[ClockParameter](
    ClockParameter("system", 100 MHz, "system"),
    ClockParameter("debug", 10 MHz, "debug", synchronousWith = "system")
  )
  val hyperbusPartitions = List[(BigInt, Boolean)](
    (8 MB, false)
  )
  val kitParameter = KitParameter(resets, clocks)
  val boardParameter = ElemRVBoard.Parameter(kitParameter)
  val socParameter = ElemRV.Parameter(boardParameter)
  val parameter = Hydrogen.Parameter(
    socParameter,
    8 MB,
    hyperbusPartitions,
    (resetCtrl: ResetControllerCtrl, reset: Bool, _) => {
      resetCtrl.buildDummy(reset)
    },
    (clockCtrl: ClockControllerCtrl, resetCtrl: ResetControllerCtrl, clock: Bool) => {
      clockCtrl.buildDummy(clock)
    }
  )

  val io = new Bundle {
    val clock = IhpCmosIo("east", 5)
    val reset = IhpCmosIo("east", 6)
    val uartStd = new Bundle {
      val txd = IhpCmosIo("south", 10)
      val rxd = IhpCmosIo("south", 8)
      val cts = IhpCmosIo("south", 9)
      val rts = IhpCmosIo("south", 11)
    }
    val jtag = new Bundle {
      val tms = IhpCmosIo("south", 4)
      val tdi = IhpCmosIo("south", 5)
      val tdo = IhpCmosIo("south", 6)
      val tck = IhpCmosIo("south", 7)
    }
    val gpioStatus = Vec(
      IhpCmosIo("east", 4),
      IhpCmosIo("west", 8),
      IhpCmosIo("west", 9),
      IhpCmosIo("west", 10)
    )
    val hyperbus = new Bundle {
      val cs = Vec(
        IhpCmosIo("east", 8)
      )
      val ck = IhpCmosIo("east", 10)
      val reset = IhpCmosIo("east", 7)
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
      val rwds = IhpCmosIo("east", 9)
    }
    val spi = new Bundle {
      val cs = Vec(
        IhpCmosIo("west", 4)
      )
      val dq = Vec(
        IhpCmosIo("west", 6),
        IhpCmosIo("west", 7)
      )
      val sck = IhpCmosIo("west", 5)
    }
  }

  val soc = ElemRV(parameter)

  io.clock <> IOPadIn(soc.io_plat.clock)
  io.reset <> IOPadIn(soc.io_plat.reset)

  io.jtag.tms <> IOPadIn(soc.io_plat.jtag.tms)
  io.jtag.tdi <> IOPadIn(soc.io_plat.jtag.tdi)
  io.jtag.tdo <> IOPadOut4mA(soc.io_plat.jtag.tdo)
  io.jtag.tck <> IOPadIn(soc.io_plat.jtag.tck)

  io.uartStd.txd <> IOPadOut4mA(soc.io_per.uartStd.txd)
  io.uartStd.rxd <> IOPadIn(soc.io_per.uartStd.rxd)
  io.uartStd.cts <> IOPadIn(soc.io_per.uartStd.cts)
  io.uartStd.rts <> IOPadOut4mA(soc.io_per.uartStd.rts)

  for (index <- 0 until io.gpioStatus.length) {
    io.gpioStatus(index) <> IOPadInOut4mA(soc.io_per.gpioStatus.pins(index))
  }

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

  for (index <- 0 until 5) { IOPadIOVdd() }
  for (index <- 0 until 5) { IOPadIOVss() }
  for (index <- 0 until 3) { IOPadVdd() }
  for (index <- 0 until 3) { IOPadVss() }
}

object SG13G2Generate extends ElementsApp {
  elementsConfig.genASICSpinalConfig.generateVerilog {
    val top = SG13G2Top()
    val config = OpenROADTools.IHP.Config(elementsConfig)
    config.generate(
      OpenROADTools.PDKs.IHP.sg13g2,
      (0, 0, 2015.04, 2014.74),
      (394.08, 396.92, 1622.4, 1617.86)
    )

    val sdc = OpenROADTools.IHP.Sdc(elementsConfig)
    sdc.addClock(top.io.clock.PAD, 25 MHz, "clk_core")
    sdc.addClock(top.io.jtag.tck.PAD, 10 MHz, "clk_jtag")
    sdc.setFalsePath("clk_core", "clk_jtag")
    sdc.generate(top.io)

    val io = OpenROADTools.IHP.Io(elementsConfig)
    io.addPad("south", 0, "sg13g2_IOPadIOVdd")
    io.addPad("south", 1, "sg13g2_IOPadIOVss")
    io.addPad("south", 2, "sg13g2_IOPadVss")
    io.addPad("south", 3, "sg13g2_IOPadVdd")
    io.addPad("east", 0, "sg13g2_IOPadIOVdd")
    io.addPad("east", 1, "sg13g2_IOPadIOVss")
    io.addPad("east", 2, "sg13g2_IOPadVss")
    io.addPad("east", 3, "sg13g2_IOPadVdd")
    io.addPad("north", 0, "sg13g2_IOPadIOVdd")
    io.addPad("north", 1, "sg13g2_IOPadIOVss")
    io.addPad("north", 10, "sg13g2_IOPadIOVss")
    io.addPad("north", 11, "sg13g2_IOPadIOVdd")
    io.addPad("west", 0, "sg13g2_IOPadIOVdd")
    io.addPad("west", 1, "sg13g2_IOPadIOVss")
    io.addPad("west", 2, "sg13g2_IOPadVss")
    io.addPad("west", 3, "sg13g2_IOPadVdd")
    io.generate(top.io)

    val pdn = OpenROADTools.IHP.Pdn(elementsConfig)
    pdn.generate()

    top
  }
}

object SG13G2Simulate extends ElementsApp {
  val compiled = elementsConfig.genFPGASimConfig.compile {
    val board = SG13G2Board()
    BinTools.initRam(board.spiNor.deviceOut.data, elementsConfig.bootromBinary)
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
        testCases.uartRxIdle(dut.io.uartStd.rxd)
      }
  }
}
