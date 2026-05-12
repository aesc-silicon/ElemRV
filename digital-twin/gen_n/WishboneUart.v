// Generator : SpinalHDL v1.13.0    git head : d9d72474863badf47d8585d187f3e04ae4749c59
// Component : WishboneUart
// Git hash  : 58d827afb766b3e54c8e02d57b05f468cf844e91

`timescale 1ns/1ps

module WishboneUart (
  input  wire          io_bus_CYC,
  input  wire          io_bus_STB,
  output wire          io_bus_ACK,
  input  wire          io_bus_WE,
  input  wire [9:0]    io_bus_ADR,
  output reg  [31:0]   io_bus_DAT_MISO,
  input  wire [31:0]   io_bus_DAT_MOSI,
  output wire          io_uart_txd,
  input  wire          io_uart_rxd,
  input  wire          io_uart_cts,
  output wire          io_uart_rts,
  output wire          io_interrupt,
  input  wire          clk,
  input  wire          resetn
);
  localparam ParityType_NONE = 2'd0;
  localparam ParityType_EVEN = 2'd1;
  localparam ParityType_ODD = 2'd2;
  localparam StopType_ONE = 1'd0;
  localparam StopType_TWO = 1'd1;

  wire                ctrl_io_readIsFull;
  reg                 io_read_queueWithOccupancy_io_pop_ready;
  reg        [1:0]    interruptCtrl_1_io_inputs;
  reg        [1:0]    interruptCtrl_1_io_clears;
  wire                ctrl_io_uart_txd;
  wire                ctrl_io_uart_rts;
  wire                ctrl_io_interrupt;
  wire                ctrl_io_write_ready;
  wire                ctrl_io_read_valid;
  wire       [8:0]    ctrl_io_read_payload;
  wire       [31:0]   mapper_idCtrl_io_header;
  wire       [31:0]   mapper_idCtrl_io_version;
  wire                mapper_tx_streamUnbuffered_queueWithOccupancy_io_push_ready;
  wire                mapper_tx_streamUnbuffered_queueWithOccupancy_io_pop_valid;
  wire       [8:0]    mapper_tx_streamUnbuffered_queueWithOccupancy_io_pop_payload;
  wire       [2:0]    mapper_tx_streamUnbuffered_queueWithOccupancy_io_occupancy;
  wire       [2:0]    mapper_tx_streamUnbuffered_queueWithOccupancy_io_availability;
  wire                io_read_queueWithOccupancy_io_push_ready;
  wire                io_read_queueWithOccupancy_io_pop_valid;
  wire       [8:0]    io_read_queueWithOccupancy_io_pop_payload;
  wire       [2:0]    io_read_queueWithOccupancy_io_occupancy;
  wire       [2:0]    io_read_queueWithOccupancy_io_availability;
  wire       [1:0]    interruptCtrl_1_io_pendings;
  wire       [2:0]    _zz_io_inputs;
  wire       [11:0]   _zz_3;
  wire                _zz_1;
  wire                _zz_2;
  reg                 _zz_io_bus_ACK;
  wire       [1:0]    mapper_permissionBits;
  reg                 _zz_mapper_tx_streamUnbuffered_valid;
  wire                mapper_tx_streamUnbuffered_valid;
  wire                mapper_tx_streamUnbuffered_ready;
  wire       [8:0]    mapper_tx_streamUnbuffered_payload;
  wire       [2:0]    mapper_tx_fifoVacancy;
  reg        [19:0]   mapper_config_cfg_clockDivider;
  reg                 mapper_config_cfg_clockDividerReload;
  reg        [1:0]    mapper_config_frameCfg_parity;
  reg        [0:0]    mapper_config_frameCfg_stop;
  reg        [3:0]    mapper_config_frameCfg_dataLength;
  reg        [2:0]    _zz_io_bus_DAT_MISO;
  reg        [2:0]    io_occupancy_regNext;
  reg        [1:0]    io_masks_driver;
  wire       [1:0]    _zz_mapper_config_frameCfg_parity;
  wire       [0:0]    _zz_mapper_config_frameCfg_stop;
  `ifndef SYNTHESIS
  reg [31:0] mapper_config_frameCfg_parity_string;
  reg [23:0] mapper_config_frameCfg_stop_string;
  reg [31:0] _zz_mapper_config_frameCfg_parity_string;
  reg [23:0] _zz_mapper_config_frameCfg_stop_string;
  `endif


  assign _zz_3 = ({2'd0,io_bus_ADR} <<< 2'd2);
  assign _zz_io_inputs = (_zz_io_bus_DAT_MISO + 3'b001);
  UartCtrl ctrl (
    .io_config_clockDivider       (mapper_config_cfg_clockDivider[19:0]                             ), //i
    .io_config_clockDividerReload (mapper_config_cfg_clockDividerReload                             ), //i
    .io_frameConfig_parity        (mapper_config_frameCfg_parity[1:0]                               ), //i
    .io_frameConfig_stop          (mapper_config_frameCfg_stop                                      ), //i
    .io_frameConfig_dataLength    (mapper_config_frameCfg_dataLength[3:0]                           ), //i
    .io_uart_txd                  (ctrl_io_uart_txd                                                 ), //o
    .io_uart_rxd                  (io_uart_rxd                                                      ), //i
    .io_uart_cts                  (io_uart_cts                                                      ), //i
    .io_uart_rts                  (ctrl_io_uart_rts                                                 ), //o
    .io_interrupt                 (ctrl_io_interrupt                                                ), //o
    .io_pendingInterrupts         (interruptCtrl_1_io_pendings[1:0]                                 ), //i
    .io_write_valid               (mapper_tx_streamUnbuffered_queueWithOccupancy_io_pop_valid       ), //i
    .io_write_ready               (ctrl_io_write_ready                                              ), //o
    .io_write_payload             (mapper_tx_streamUnbuffered_queueWithOccupancy_io_pop_payload[8:0]), //i
    .io_read_valid                (ctrl_io_read_valid                                               ), //o
    .io_read_ready                (io_read_queueWithOccupancy_io_push_ready                         ), //i
    .io_read_payload              (ctrl_io_read_payload[8:0]                                        ), //o
    .io_readIsFull                (ctrl_io_readIsFull                                               ), //i
    .clk                          (clk                                                              ), //i
    .resetn                       (resetn                                                           )  //i
  );
  IpIdentificationCtrl mapper_idCtrl (
    .io_header  (mapper_idCtrl_io_header[31:0] ), //o
    .io_version (mapper_idCtrl_io_version[31:0]), //o
    .clk        (clk                           ), //i
    .resetn     (resetn                        )  //i
  );
  StreamFifo mapper_tx_streamUnbuffered_queueWithOccupancy (
    .io_push_valid   (mapper_tx_streamUnbuffered_valid                                  ), //i
    .io_push_ready   (mapper_tx_streamUnbuffered_queueWithOccupancy_io_push_ready       ), //o
    .io_push_payload (mapper_tx_streamUnbuffered_payload[8:0]                           ), //i
    .io_pop_valid    (mapper_tx_streamUnbuffered_queueWithOccupancy_io_pop_valid        ), //o
    .io_pop_ready    (ctrl_io_write_ready                                               ), //i
    .io_pop_payload  (mapper_tx_streamUnbuffered_queueWithOccupancy_io_pop_payload[8:0] ), //o
    .io_flush        (1'b0                                                              ), //i
    .io_occupancy    (mapper_tx_streamUnbuffered_queueWithOccupancy_io_occupancy[2:0]   ), //o
    .io_availability (mapper_tx_streamUnbuffered_queueWithOccupancy_io_availability[2:0]), //o
    .clk             (clk                                                               ), //i
    .resetn          (resetn                                                            )  //i
  );
  StreamFifo io_read_queueWithOccupancy (
    .io_push_valid   (ctrl_io_read_valid                             ), //i
    .io_push_ready   (io_read_queueWithOccupancy_io_push_ready       ), //o
    .io_push_payload (ctrl_io_read_payload[8:0]                      ), //i
    .io_pop_valid    (io_read_queueWithOccupancy_io_pop_valid        ), //o
    .io_pop_ready    (io_read_queueWithOccupancy_io_pop_ready        ), //i
    .io_pop_payload  (io_read_queueWithOccupancy_io_pop_payload[8:0] ), //o
    .io_flush        (1'b0                                           ), //i
    .io_occupancy    (io_read_queueWithOccupancy_io_occupancy[2:0]   ), //o
    .io_availability (io_read_queueWithOccupancy_io_availability[2:0]), //o
    .clk             (clk                                            ), //i
    .resetn          (resetn                                         )  //i
  );
  InterruptCtrl interruptCtrl_1 (
    .io_inputs   (interruptCtrl_1_io_inputs[1:0]  ), //i
    .io_clears   (interruptCtrl_1_io_clears[1:0]  ), //i
    .io_masks    (io_masks_driver[1:0]            ), //i
    .io_pendings (interruptCtrl_1_io_pendings[1:0]), //o
    .clk         (clk                             ), //i
    .resetn      (resetn                          )  //i
  );
  `ifndef SYNTHESIS
  always @(*) begin
    case(mapper_config_frameCfg_parity)
      ParityType_NONE : mapper_config_frameCfg_parity_string = "NONE";
      ParityType_EVEN : mapper_config_frameCfg_parity_string = "EVEN";
      ParityType_ODD : mapper_config_frameCfg_parity_string = "ODD ";
      default : mapper_config_frameCfg_parity_string = "????";
    endcase
  end
  always @(*) begin
    case(mapper_config_frameCfg_stop)
      StopType_ONE : mapper_config_frameCfg_stop_string = "ONE";
      StopType_TWO : mapper_config_frameCfg_stop_string = "TWO";
      default : mapper_config_frameCfg_stop_string = "???";
    endcase
  end
  always @(*) begin
    case(_zz_mapper_config_frameCfg_parity)
      ParityType_NONE : _zz_mapper_config_frameCfg_parity_string = "NONE";
      ParityType_EVEN : _zz_mapper_config_frameCfg_parity_string = "EVEN";
      ParityType_ODD : _zz_mapper_config_frameCfg_parity_string = "ODD ";
      default : _zz_mapper_config_frameCfg_parity_string = "????";
    endcase
  end
  always @(*) begin
    case(_zz_mapper_config_frameCfg_stop)
      StopType_ONE : _zz_mapper_config_frameCfg_stop_string = "ONE";
      StopType_TWO : _zz_mapper_config_frameCfg_stop_string = "TWO";
      default : _zz_mapper_config_frameCfg_stop_string = "???";
    endcase
  end
  `endif

  assign io_uart_txd = ctrl_io_uart_txd;
  assign io_uart_rts = ctrl_io_uart_rts;
  assign io_interrupt = ctrl_io_interrupt;
  always @(*) begin
    io_bus_DAT_MISO = 32'h0;
    case(_zz_3)
      12'h0 : begin
        io_bus_DAT_MISO[31 : 0] = mapper_idCtrl_io_header;
      end
      12'h004 : begin
        io_bus_DAT_MISO[31 : 0] = mapper_idCtrl_io_version;
      end
      12'h008 : begin
        io_bus_DAT_MISO[31 : 0] = {{{8'h0,8'h05},8'h09},8'h14};
      end
      12'h00c : begin
        io_bus_DAT_MISO[31 : 0] = {{{8'h0,8'h01},8'h05},8'h02};
      end
      12'h010 : begin
        io_bus_DAT_MISO[31 : 0] = {{16'h0,8'h04},8'h04};
      end
      12'h014 : begin
        io_bus_DAT_MISO[31 : 0] = {30'h0,mapper_permissionBits};
      end
      12'h018 : begin
        io_bus_DAT_MISO[16 : 16] = (io_read_queueWithOccupancy_io_pop_valid ^ 1'b0);
        io_bus_DAT_MISO[8 : 0] = io_read_queueWithOccupancy_io_pop_payload;
      end
      12'h01c : begin
        io_bus_DAT_MISO[18 : 16] = mapper_tx_fifoVacancy;
        io_bus_DAT_MISO[26 : 24] = io_read_queueWithOccupancy_io_occupancy;
      end
      12'h020 : begin
        io_bus_DAT_MISO[19 : 0] = mapper_config_cfg_clockDivider;
      end
      12'h024 : begin
        io_bus_DAT_MISO[3 : 0] = mapper_config_frameCfg_dataLength;
        io_bus_DAT_MISO[9 : 8] = mapper_config_frameCfg_parity;
        io_bus_DAT_MISO[16 : 16] = mapper_config_frameCfg_stop;
      end
      12'h028 : begin
        io_bus_DAT_MISO[2 : 0] = _zz_io_bus_DAT_MISO;
      end
      12'h02c : begin
        io_bus_DAT_MISO[1 : 0] = interruptCtrl_1_io_pendings;
      end
      12'h030 : begin
        io_bus_DAT_MISO[1 : 0] = io_masks_driver;
      end
      default : begin
      end
    endcase
  end

  assign _zz_1 = (((io_bus_CYC && io_bus_STB) && ((io_bus_CYC && io_bus_ACK) && io_bus_STB)) && io_bus_WE);
  assign _zz_2 = (((io_bus_CYC && io_bus_STB) && ((io_bus_CYC && io_bus_ACK) && io_bus_STB)) && (! io_bus_WE));
  assign io_bus_ACK = (_zz_io_bus_ACK && io_bus_STB);
  assign mapper_permissionBits = {1'b1,1'b1};
  always @(*) begin
    _zz_mapper_tx_streamUnbuffered_valid = 1'b0;
    case(_zz_3)
      12'h018 : begin
        if(_zz_1) begin
          _zz_mapper_tx_streamUnbuffered_valid = 1'b1;
        end
      end
      default : begin
      end
    endcase
  end

  assign mapper_tx_streamUnbuffered_valid = _zz_mapper_tx_streamUnbuffered_valid;
  assign mapper_tx_streamUnbuffered_payload = io_bus_DAT_MOSI[8 : 0];
  assign mapper_tx_streamUnbuffered_ready = mapper_tx_streamUnbuffered_queueWithOccupancy_io_push_ready;
  assign mapper_tx_fifoVacancy = (3'b100 - mapper_tx_streamUnbuffered_queueWithOccupancy_io_occupancy);
  assign ctrl_io_readIsFull = (3'b011 <= io_read_queueWithOccupancy_io_occupancy);
  always @(*) begin
    io_read_queueWithOccupancy_io_pop_ready = 1'b0;
    case(_zz_3)
      12'h018 : begin
        if(_zz_2) begin
          io_read_queueWithOccupancy_io_pop_ready = 1'b1;
        end
      end
      default : begin
      end
    endcase
  end

  always @(*) begin
    interruptCtrl_1_io_clears = 2'b00;
    case(_zz_3)
      12'h02c : begin
        if(_zz_1) begin
          interruptCtrl_1_io_clears = io_bus_DAT_MOSI[1 : 0];
        end
      end
      default : begin
      end
    endcase
  end

  always @(*) begin
    interruptCtrl_1_io_inputs[0] = ((mapper_tx_streamUnbuffered_queueWithOccupancy_io_occupancy == _zz_io_bus_DAT_MISO) && (io_occupancy_regNext == _zz_io_inputs));
    interruptCtrl_1_io_inputs[1] = ctrl_io_read_valid;
  end

  assign _zz_mapper_config_frameCfg_parity = io_bus_DAT_MOSI[9 : 8];
  assign _zz_mapper_config_frameCfg_stop = io_bus_DAT_MOSI[16 : 16];
  always @(posedge clk or negedge resetn) begin
    if(!resetn) begin
      _zz_io_bus_ACK <= 1'b0;
      io_masks_driver <= 2'b00;
    end else begin
      _zz_io_bus_ACK <= (io_bus_STB && io_bus_CYC);
      case(_zz_3)
        12'h030 : begin
          if(_zz_1) begin
            io_masks_driver <= io_bus_DAT_MOSI[1 : 0];
          end
        end
        default : begin
        end
      endcase
    end
  end

  always @(posedge clk) begin
    mapper_config_cfg_clockDividerReload <= 1'b0;
    io_occupancy_regNext <= mapper_tx_streamUnbuffered_queueWithOccupancy_io_occupancy;
    case(_zz_3)
      12'h020 : begin
        if(_zz_1) begin
          mapper_config_cfg_clockDivider <= io_bus_DAT_MOSI[19 : 0];
          mapper_config_cfg_clockDividerReload <= 1'b1;
        end
      end
      12'h024 : begin
        if(_zz_1) begin
          mapper_config_frameCfg_dataLength <= io_bus_DAT_MOSI[3 : 0];
          mapper_config_frameCfg_parity <= _zz_mapper_config_frameCfg_parity;
          mapper_config_frameCfg_stop <= _zz_mapper_config_frameCfg_stop;
        end
      end
      12'h028 : begin
        if(_zz_1) begin
          _zz_io_bus_DAT_MISO <= io_bus_DAT_MOSI[2 : 0];
        end
      end
      default : begin
      end
    endcase
  end


endmodule

module InterruptCtrl (
  input  wire [1:0]    io_inputs,
  input  wire [1:0]    io_clears,
  input  wire [1:0]    io_masks,
  output wire [1:0]    io_pendings,
  input  wire          clk,
  input  wire          resetn
);

  reg        [1:0]    pendings;

  assign io_pendings = (pendings & io_masks);
  always @(posedge clk or negedge resetn) begin
    if(!resetn) begin
      pendings <= 2'b00;
    end else begin
      pendings <= ((pendings & (~ io_clears)) | io_inputs);
    end
  end


endmodule

//StreamFifo_1 replaced by StreamFifo

module StreamFifo (
  input  wire          io_push_valid,
  output wire          io_push_ready,
  input  wire [8:0]    io_push_payload,
  output wire          io_pop_valid,
  input  wire          io_pop_ready,
  output wire [8:0]    io_pop_payload,
  input  wire          io_flush,
  output wire [2:0]    io_occupancy,
  output wire [2:0]    io_availability,
  input  wire          clk,
  input  wire          resetn
);

  reg        [8:0]    logic_ram_spinal_port1;
  reg                 _zz_1;
  wire                logic_ptr_doPush;
  wire                logic_ptr_doPop;
  wire                logic_ptr_full;
  wire                logic_ptr_empty;
  reg        [2:0]    logic_ptr_push;
  reg        [2:0]    logic_ptr_pop;
  wire       [2:0]    logic_ptr_occupancy;
  wire       [2:0]    logic_ptr_popOnIo;
  wire                when_Stream_l1455;
  reg                 logic_ptr_wentUp;
  wire                io_push_fire;
  wire                logic_push_onRam_write_valid;
  wire       [1:0]    logic_push_onRam_write_payload_address;
  wire       [8:0]    logic_push_onRam_write_payload_data;
  wire                logic_pop_addressGen_valid;
  reg                 logic_pop_addressGen_ready;
  wire       [1:0]    logic_pop_addressGen_payload;
  wire                logic_pop_addressGen_fire;
  wire                logic_pop_sync_readArbitation_valid;
  wire                logic_pop_sync_readArbitation_ready;
  wire       [1:0]    logic_pop_sync_readArbitation_payload;
  reg                 logic_pop_addressGen_rValid;
  reg        [1:0]    logic_pop_addressGen_rData;
  wire                when_Stream_l477;
  wire                logic_pop_sync_readPort_cmd_valid;
  wire       [1:0]    logic_pop_sync_readPort_cmd_payload;
  wire       [8:0]    logic_pop_sync_readPort_rsp;
  wire                logic_pop_addressGen_toFlowFire_valid;
  wire       [1:0]    logic_pop_addressGen_toFlowFire_payload;
  wire                logic_pop_sync_readArbitation_translated_valid;
  wire                logic_pop_sync_readArbitation_translated_ready;
  wire       [8:0]    logic_pop_sync_readArbitation_translated_payload;
  wire                logic_pop_sync_readArbitation_fire;
  reg        [2:0]    logic_pop_sync_popReg;
  reg [8:0] logic_ram [0:3];

  always @(posedge clk) begin
    if(_zz_1) begin
      logic_ram[logic_push_onRam_write_payload_address] <= logic_push_onRam_write_payload_data;
    end
  end

  always @(posedge clk) begin
    if(logic_pop_sync_readPort_cmd_valid) begin
      logic_ram_spinal_port1 <= logic_ram[logic_pop_sync_readPort_cmd_payload];
    end
  end

  always @(*) begin
    _zz_1 = 1'b0;
    if(logic_push_onRam_write_valid) begin
      _zz_1 = 1'b1;
    end
  end

  assign when_Stream_l1455 = (logic_ptr_doPush != logic_ptr_doPop);
  assign logic_ptr_full = (((logic_ptr_push ^ logic_ptr_popOnIo) ^ 3'b100) == 3'b000);
  assign logic_ptr_empty = (logic_ptr_push == logic_ptr_pop);
  assign logic_ptr_occupancy = (logic_ptr_push - logic_ptr_popOnIo);
  assign io_push_ready = (! logic_ptr_full);
  assign io_push_fire = (io_push_valid && io_push_ready);
  assign logic_ptr_doPush = io_push_fire;
  assign logic_push_onRam_write_valid = io_push_fire;
  assign logic_push_onRam_write_payload_address = logic_ptr_push[1:0];
  assign logic_push_onRam_write_payload_data = io_push_payload;
  assign logic_pop_addressGen_valid = (! logic_ptr_empty);
  assign logic_pop_addressGen_payload = logic_ptr_pop[1:0];
  assign logic_pop_addressGen_fire = (logic_pop_addressGen_valid && logic_pop_addressGen_ready);
  assign logic_ptr_doPop = logic_pop_addressGen_fire;
  always @(*) begin
    logic_pop_addressGen_ready = logic_pop_sync_readArbitation_ready;
    if(when_Stream_l477) begin
      logic_pop_addressGen_ready = 1'b1;
    end
  end

  assign when_Stream_l477 = (! logic_pop_sync_readArbitation_valid);
  assign logic_pop_sync_readArbitation_valid = logic_pop_addressGen_rValid;
  assign logic_pop_sync_readArbitation_payload = logic_pop_addressGen_rData;
  assign logic_pop_sync_readPort_rsp = logic_ram_spinal_port1;
  assign logic_pop_addressGen_toFlowFire_valid = logic_pop_addressGen_fire;
  assign logic_pop_addressGen_toFlowFire_payload = logic_pop_addressGen_payload;
  assign logic_pop_sync_readPort_cmd_valid = logic_pop_addressGen_toFlowFire_valid;
  assign logic_pop_sync_readPort_cmd_payload = logic_pop_addressGen_toFlowFire_payload;
  assign logic_pop_sync_readArbitation_translated_valid = logic_pop_sync_readArbitation_valid;
  assign logic_pop_sync_readArbitation_ready = logic_pop_sync_readArbitation_translated_ready;
  assign logic_pop_sync_readArbitation_translated_payload = logic_pop_sync_readPort_rsp;
  assign io_pop_valid = logic_pop_sync_readArbitation_translated_valid;
  assign logic_pop_sync_readArbitation_translated_ready = io_pop_ready;
  assign io_pop_payload = logic_pop_sync_readArbitation_translated_payload;
  assign logic_pop_sync_readArbitation_fire = (logic_pop_sync_readArbitation_valid && logic_pop_sync_readArbitation_ready);
  assign logic_ptr_popOnIo = logic_pop_sync_popReg;
  assign io_occupancy = logic_ptr_occupancy;
  assign io_availability = (3'b100 - logic_ptr_occupancy);
  always @(posedge clk or negedge resetn) begin
    if(!resetn) begin
      logic_ptr_push <= 3'b000;
      logic_ptr_pop <= 3'b000;
      logic_ptr_wentUp <= 1'b0;
      logic_pop_addressGen_rValid <= 1'b0;
      logic_pop_sync_popReg <= 3'b000;
    end else begin
      if(when_Stream_l1455) begin
        logic_ptr_wentUp <= logic_ptr_doPush;
      end
      if(io_flush) begin
        logic_ptr_wentUp <= 1'b0;
      end
      if(logic_ptr_doPush) begin
        logic_ptr_push <= (logic_ptr_push + 3'b001);
      end
      if(logic_ptr_doPop) begin
        logic_ptr_pop <= (logic_ptr_pop + 3'b001);
      end
      if(io_flush) begin
        logic_ptr_push <= 3'b000;
        logic_ptr_pop <= 3'b000;
      end
      if(logic_pop_addressGen_ready) begin
        logic_pop_addressGen_rValid <= logic_pop_addressGen_valid;
      end
      if(io_flush) begin
        logic_pop_addressGen_rValid <= 1'b0;
      end
      if(logic_pop_sync_readArbitation_fire) begin
        logic_pop_sync_popReg <= logic_ptr_pop;
      end
      if(io_flush) begin
        logic_pop_sync_popReg <= 3'b000;
      end
    end
  end

  always @(posedge clk) begin
    if(logic_pop_addressGen_ready) begin
      logic_pop_addressGen_rData <= logic_pop_addressGen_payload;
    end
  end


endmodule

module IpIdentificationCtrl (
  output wire [31:0]   io_header,
  output wire [31:0]   io_version,
  input  wire          clk,
  input  wire          resetn
);
  localparam Ids_Gpio = 4'd0;
  localparam Ids_Pio = 4'd1;
  localparam Ids_Pwm = 4'd2;
  localparam Ids_Uart = 4'd3;
  localparam Ids_I2cController = 4'd4;
  localparam Ids_I2cDevice = 4'd5;
  localparam Ids_SpiController = 4'd6;
  localparam Ids_SpiXipController = 4'd7;
  localparam Ids_SpiDevice = 4'd8;
  localparam Ids_AesAccelerator = 4'd9;
  localparam Ids_AesMaskedAccelerator = 4'd10;
  localparam Ids_Reset = 4'd11;
  localparam Ids_Clock = 4'd12;

  wire       [15:0]   _zz_header;
  wire       [3:0]    _zz_header_1;
  wire       [31:0]   header;
  wire       [31:0]   version;

  assign _zz_header_1 = Ids_Uart;
  assign _zz_header = {12'd0, _zz_header_1};
  assign header = {{8'h0,8'h08},_zz_header};
  assign version = {{8'h01,8'h0},16'h0};
  assign io_header = header;
  assign io_version = version;

endmodule

module UartCtrl (
  input  wire [19:0]   io_config_clockDivider,
  input  wire          io_config_clockDividerReload,
  input  wire [1:0]    io_frameConfig_parity,
  input  wire [0:0]    io_frameConfig_stop,
  input  wire [3:0]    io_frameConfig_dataLength,
  output wire          io_uart_txd,
  input  wire          io_uart_rxd,
  input  wire          io_uart_cts,
  output wire          io_uart_rts,
  output wire          io_interrupt,
  input  wire [1:0]    io_pendingInterrupts,
  input  wire          io_write_valid,
  output wire          io_write_ready,
  input  wire [8:0]    io_write_payload,
  output wire          io_read_valid,
  input  wire          io_read_ready,
  output wire [8:0]    io_read_payload,
  input  wire          io_readIsFull,
  input  wire          clk,
  input  wire          resetn
);
  localparam ParityType_NONE = 2'd0;
  localparam ParityType_EVEN = 2'd1;
  localparam ParityType_ODD = 2'd2;
  localparam StopType_ONE = 1'd0;
  localparam StopType_TWO = 1'd1;

  wire                tx_io_cts;
  wire                clockDivider_1_io_tick;
  wire                tx_io_write_ready;
  wire                tx_io_txd;
  wire                rx_io_read_valid;
  wire       [8:0]    rx_io_read_payload;
  `ifndef SYNTHESIS
  reg [31:0] io_frameConfig_parity_string;
  reg [23:0] io_frameConfig_stop_string;
  `endif


  ClockDivider clockDivider_1 (
    .io_value  (io_config_clockDivider[19:0]), //i
    .io_reload (io_config_clockDividerReload), //i
    .io_tick   (clockDivider_1_io_tick      ), //o
    .clk       (clk                         ), //i
    .resetn    (resetn                      )  //i
  );
  UartCtrlTx tx (
    .io_config_parity     (io_frameConfig_parity[1:0]    ), //i
    .io_config_stop       (io_frameConfig_stop           ), //i
    .io_config_dataLength (io_frameConfig_dataLength[3:0]), //i
    .io_samplingTick      (clockDivider_1_io_tick        ), //i
    .io_write_valid       (io_write_valid                ), //i
    .io_write_ready       (tx_io_write_ready             ), //o
    .io_write_payload     (io_write_payload[8:0]         ), //i
    .io_txd               (tx_io_txd                     ), //o
    .io_cts               (tx_io_cts                     ), //i
    .clk                  (clk                           ), //i
    .resetn               (resetn                        )  //i
  );
  UartCtrlRx rx (
    .io_config_parity     (io_frameConfig_parity[1:0]    ), //i
    .io_config_stop       (io_frameConfig_stop           ), //i
    .io_config_dataLength (io_frameConfig_dataLength[3:0]), //i
    .io_samplingTick      (clockDivider_1_io_tick        ), //i
    .io_read_valid        (rx_io_read_valid              ), //o
    .io_read_ready        (io_read_ready                 ), //i
    .io_read_payload      (rx_io_read_payload[8:0]       ), //o
    .io_rxd               (io_uart_rxd                   ), //i
    .clk                  (clk                           ), //i
    .resetn               (resetn                        )  //i
  );
  `ifndef SYNTHESIS
  always @(*) begin
    case(io_frameConfig_parity)
      ParityType_NONE : io_frameConfig_parity_string = "NONE";
      ParityType_EVEN : io_frameConfig_parity_string = "EVEN";
      ParityType_ODD : io_frameConfig_parity_string = "ODD ";
      default : io_frameConfig_parity_string = "????";
    endcase
  end
  always @(*) begin
    case(io_frameConfig_stop)
      StopType_ONE : io_frameConfig_stop_string = "ONE";
      StopType_TWO : io_frameConfig_stop_string = "TWO";
      default : io_frameConfig_stop_string = "???";
    endcase
  end
  `endif

  assign io_interrupt = (|io_pendingInterrupts);
  assign io_write_ready = tx_io_write_ready;
  assign io_uart_txd = tx_io_txd;
  assign io_read_valid = rx_io_read_valid;
  assign io_read_payload = rx_io_read_payload;
  assign io_uart_rts = io_readIsFull;
  assign tx_io_cts = (! io_uart_cts);

endmodule

module UartCtrlRx (
  input  wire [1:0]    io_config_parity,
  input  wire [0:0]    io_config_stop,
  input  wire [3:0]    io_config_dataLength,
  input  wire          io_samplingTick,
  output wire          io_read_valid,
  input  wire          io_read_ready,
  output wire [8:0]    io_read_payload,
  input  wire          io_rxd,
  input  wire          clk,
  input  wire          resetn
);
  localparam ParityType_NONE = 2'd0;
  localparam ParityType_EVEN = 2'd1;
  localparam ParityType_ODD = 2'd2;
  localparam StopType_ONE = 1'd0;
  localparam StopType_TWO = 1'd1;
  localparam State_IDLE = 3'd0;
  localparam State_START = 3'd1;
  localparam State_DATA = 3'd2;
  localparam State_PARITY = 3'd3;
  localparam State_STOP = 3'd4;

  wire                io_rxd_buffercc_io_dataOut;
  wire                _zz_sampler_value;
  wire                _zz_sampler_value_1;
  wire                _zz_sampler_value_2;
  wire                _zz_sampler_value_3;
  wire                _zz_sampler_value_4;
  wire                _zz_sampler_value_5;
  wire                _zz_sampler_value_6;
  wire       [3:0]    _zz_when_UartCtrlRx_l129;
  wire       [0:0]    _zz_when_UartCtrlRx_l129_1;
  wire                sampler_synchroniser;
  wire                sampler_samples_0;
  reg                 sampler_samples_1;
  reg                 sampler_samples_2;
  reg                 sampler_samples_3;
  reg                 sampler_samples_4;
  reg                 sampler_value;
  reg                 sampler_tick;
  reg        [2:0]    bitTimer_counter;
  reg                 bitTimer_tick;
  wire                when_UartCtrlRx_l49;
  reg        [3:0]    bitCounter_value;
  reg        [2:0]    stateMachine_state;
  reg                 stateMachine_parity;
  reg        [8:0]    stateMachine_shifter;
  reg                 stateMachine_validReg;
  wire                when_UartCtrlRx_l83;
  wire                when_UartCtrlRx_l94;
  wire                when_UartCtrlRx_l102;
  wire                when_UartCtrlRx_l104;
  wire                when_UartCtrlRx_l116;
  wire                when_UartCtrlRx_l127;
  wire                when_UartCtrlRx_l129;
  `ifndef SYNTHESIS
  reg [31:0] io_config_parity_string;
  reg [23:0] io_config_stop_string;
  reg [47:0] stateMachine_state_string;
  `endif


  assign _zz_when_UartCtrlRx_l129_1 = ((io_config_stop == StopType_ONE) ? 1'b0 : 1'b1);
  assign _zz_when_UartCtrlRx_l129 = {3'd0, _zz_when_UartCtrlRx_l129_1};
  assign _zz_sampler_value = ((((1'b0 || ((_zz_sampler_value_1 && sampler_samples_1) && sampler_samples_2)) || (((_zz_sampler_value_2 && sampler_samples_0) && sampler_samples_1) && sampler_samples_3)) || (((1'b1 && sampler_samples_0) && sampler_samples_2) && sampler_samples_3)) || (((1'b1 && sampler_samples_1) && sampler_samples_2) && sampler_samples_3));
  assign _zz_sampler_value_3 = (((1'b1 && sampler_samples_0) && sampler_samples_1) && sampler_samples_4);
  assign _zz_sampler_value_4 = ((1'b1 && sampler_samples_0) && sampler_samples_2);
  assign _zz_sampler_value_5 = (1'b1 && sampler_samples_1);
  assign _zz_sampler_value_6 = 1'b1;
  assign _zz_sampler_value_1 = (1'b1 && sampler_samples_0);
  assign _zz_sampler_value_2 = 1'b1;
  (* keep_hierarchy = "TRUE" *) BufferCC io_rxd_buffercc (
    .io_dataIn  (io_rxd                    ), //i
    .io_dataOut (io_rxd_buffercc_io_dataOut), //o
    .clk        (clk                       ), //i
    .resetn     (resetn                    )  //i
  );
  `ifndef SYNTHESIS
  always @(*) begin
    case(io_config_parity)
      ParityType_NONE : io_config_parity_string = "NONE";
      ParityType_EVEN : io_config_parity_string = "EVEN";
      ParityType_ODD : io_config_parity_string = "ODD ";
      default : io_config_parity_string = "????";
    endcase
  end
  always @(*) begin
    case(io_config_stop)
      StopType_ONE : io_config_stop_string = "ONE";
      StopType_TWO : io_config_stop_string = "TWO";
      default : io_config_stop_string = "???";
    endcase
  end
  always @(*) begin
    case(stateMachine_state)
      State_IDLE : stateMachine_state_string = "IDLE  ";
      State_START : stateMachine_state_string = "START ";
      State_DATA : stateMachine_state_string = "DATA  ";
      State_PARITY : stateMachine_state_string = "PARITY";
      State_STOP : stateMachine_state_string = "STOP  ";
      default : stateMachine_state_string = "??????";
    endcase
  end
  `endif

  assign sampler_synchroniser = io_rxd_buffercc_io_dataOut;
  assign sampler_samples_0 = sampler_synchroniser;
  always @(*) begin
    bitTimer_tick = 1'b0;
    if(sampler_tick) begin
      if(when_UartCtrlRx_l49) begin
        bitTimer_tick = 1'b1;
      end
    end
  end

  assign when_UartCtrlRx_l49 = (bitTimer_counter == 3'b000);
  assign io_read_valid = stateMachine_validReg;
  assign when_UartCtrlRx_l83 = (sampler_tick && (! sampler_value));
  assign when_UartCtrlRx_l94 = (sampler_value == 1'b1);
  assign when_UartCtrlRx_l102 = (bitCounter_value == io_config_dataLength);
  assign when_UartCtrlRx_l104 = (io_config_parity == ParityType_NONE);
  assign when_UartCtrlRx_l116 = (stateMachine_parity == sampler_value);
  assign when_UartCtrlRx_l127 = (! sampler_value);
  assign when_UartCtrlRx_l129 = (bitCounter_value == _zz_when_UartCtrlRx_l129);
  assign io_read_payload = stateMachine_shifter;
  always @(posedge clk or negedge resetn) begin
    if(!resetn) begin
      sampler_samples_1 <= 1'b1;
      sampler_samples_2 <= 1'b1;
      sampler_samples_3 <= 1'b1;
      sampler_samples_4 <= 1'b1;
      sampler_value <= 1'b1;
      sampler_tick <= 1'b0;
      stateMachine_state <= State_IDLE;
      stateMachine_validReg <= 1'b0;
    end else begin
      if(io_samplingTick) begin
        sampler_samples_1 <= sampler_samples_0;
      end
      if(io_samplingTick) begin
        sampler_samples_2 <= sampler_samples_1;
      end
      if(io_samplingTick) begin
        sampler_samples_3 <= sampler_samples_2;
      end
      if(io_samplingTick) begin
        sampler_samples_4 <= sampler_samples_3;
      end
      sampler_value <= ((((((_zz_sampler_value || _zz_sampler_value_3) || (_zz_sampler_value_4 && sampler_samples_4)) || ((_zz_sampler_value_5 && sampler_samples_2) && sampler_samples_4)) || (((_zz_sampler_value_6 && sampler_samples_0) && sampler_samples_3) && sampler_samples_4)) || (((1'b1 && sampler_samples_1) && sampler_samples_3) && sampler_samples_4)) || (((1'b1 && sampler_samples_2) && sampler_samples_3) && sampler_samples_4));
      sampler_tick <= io_samplingTick;
      stateMachine_validReg <= 1'b0;
      case(stateMachine_state)
        State_IDLE : begin
          if(when_UartCtrlRx_l83) begin
            stateMachine_state <= State_START;
          end
        end
        State_START : begin
          if(bitTimer_tick) begin
            stateMachine_state <= State_DATA;
            if(when_UartCtrlRx_l94) begin
              stateMachine_state <= State_IDLE;
            end
          end
        end
        State_DATA : begin
          if(bitTimer_tick) begin
            if(when_UartCtrlRx_l102) begin
              if(when_UartCtrlRx_l104) begin
                stateMachine_state <= State_STOP;
                stateMachine_validReg <= 1'b1;
              end else begin
                stateMachine_state <= State_PARITY;
              end
            end
          end
        end
        State_PARITY : begin
          if(bitTimer_tick) begin
            if(when_UartCtrlRx_l116) begin
              stateMachine_state <= State_STOP;
              stateMachine_validReg <= 1'b1;
            end else begin
              stateMachine_state <= State_IDLE;
            end
          end
        end
        default : begin
          if(bitTimer_tick) begin
            if(when_UartCtrlRx_l127) begin
              stateMachine_state <= State_IDLE;
            end else begin
              if(when_UartCtrlRx_l129) begin
                stateMachine_state <= State_IDLE;
              end
            end
          end
        end
      endcase
    end
  end

  always @(posedge clk) begin
    if(sampler_tick) begin
      bitTimer_counter <= (bitTimer_counter - 3'b001);
    end
    if(bitTimer_tick) begin
      bitCounter_value <= (bitCounter_value + 4'b0001);
    end
    if(bitTimer_tick) begin
      stateMachine_parity <= (stateMachine_parity ^ sampler_value);
    end
    case(stateMachine_state)
      State_IDLE : begin
        if(when_UartCtrlRx_l83) begin
          bitTimer_counter <= 3'b010;
        end
      end
      State_START : begin
        if(bitTimer_tick) begin
          bitCounter_value <= 4'b0000;
          stateMachine_parity <= (io_config_parity == ParityType_ODD);
          stateMachine_shifter <= 9'h0;
        end
      end
      State_DATA : begin
        if(bitTimer_tick) begin
          stateMachine_shifter[bitCounter_value] <= sampler_value;
          if(when_UartCtrlRx_l102) begin
            bitCounter_value <= 4'b0000;
          end
        end
      end
      State_PARITY : begin
        if(bitTimer_tick) begin
          bitCounter_value <= 4'b0000;
        end
      end
      default : begin
      end
    endcase
  end


endmodule

module UartCtrlTx (
  input  wire [1:0]    io_config_parity,
  input  wire [0:0]    io_config_stop,
  input  wire [3:0]    io_config_dataLength,
  input  wire          io_samplingTick,
  input  wire          io_write_valid,
  output reg           io_write_ready,
  input  wire [8:0]    io_write_payload,
  output wire          io_txd,
  input  wire          io_cts,
  input  wire          clk,
  input  wire          resetn
);
  localparam ParityType_NONE = 2'd0;
  localparam ParityType_EVEN = 2'd1;
  localparam ParityType_ODD = 2'd2;
  localparam StopType_ONE = 1'd0;
  localparam StopType_TWO = 1'd1;
  localparam State_IDLE = 3'd0;
  localparam State_START = 3'd1;
  localparam State_DATA = 3'd2;
  localparam State_PARITY = 3'd3;
  localparam State_STOP = 3'd4;

  wire       [2:0]    _zz_txCtrl_clockDivider_counter_valueNext;
  wire       [0:0]    _zz_txCtrl_clockDivider_counter_valueNext_1;
  wire       [3:0]    _zz_when_UartCtrlTx_l95;
  wire       [0:0]    _zz_when_UartCtrlTx_l95_1;
  reg                 txEnable;
  wire                txCtrl_newClockEnable;
  reg                 txCtrl_clockDivider_counter_willIncrement;
  wire                txCtrl_clockDivider_counter_willClear;
  reg        [2:0]    txCtrl_clockDivider_counter_valueNext;
  reg        [2:0]    txCtrl_clockDivider_counter_value;
  wire                txCtrl_clockDivider_counter_willOverflowIfInc;
  wire                txCtrl_clockDivider_counter_willOverflow;
  reg        [3:0]    txCtrl_tickCounter_value;
  reg        [2:0]    txCtrl_stateMachine_state;
  reg                 txCtrl_stateMachine_parity;
  reg                 txCtrl_stateMachine_txd;
  wire                when_UartCtrlTx_l59;
  wire                when_UartCtrlTx_l74;
  wire                when_UartCtrlTx_l77;
  wire                when_UartCtrlTx_l95;
  wire       [2:0]    _zz_txCtrl_stateMachine_state;
  wire                when_UartCtrlTx_l103;
  reg                 txCtrl_stateMachine_txd_regNext;
  `ifndef SYNTHESIS
  reg [31:0] io_config_parity_string;
  reg [23:0] io_config_stop_string;
  reg [47:0] txCtrl_stateMachine_state_string;
  reg [47:0] _zz_txCtrl_stateMachine_state_string;
  `endif


  assign _zz_txCtrl_clockDivider_counter_valueNext_1 = txCtrl_clockDivider_counter_willIncrement;
  assign _zz_txCtrl_clockDivider_counter_valueNext = {2'd0, _zz_txCtrl_clockDivider_counter_valueNext_1};
  assign _zz_when_UartCtrlTx_l95_1 = ((io_config_stop == StopType_ONE) ? 1'b0 : 1'b1);
  assign _zz_when_UartCtrlTx_l95 = {3'd0, _zz_when_UartCtrlTx_l95_1};
  `ifndef SYNTHESIS
  always @(*) begin
    case(io_config_parity)
      ParityType_NONE : io_config_parity_string = "NONE";
      ParityType_EVEN : io_config_parity_string = "EVEN";
      ParityType_ODD : io_config_parity_string = "ODD ";
      default : io_config_parity_string = "????";
    endcase
  end
  always @(*) begin
    case(io_config_stop)
      StopType_ONE : io_config_stop_string = "ONE";
      StopType_TWO : io_config_stop_string = "TWO";
      default : io_config_stop_string = "???";
    endcase
  end
  always @(*) begin
    case(txCtrl_stateMachine_state)
      State_IDLE : txCtrl_stateMachine_state_string = "IDLE  ";
      State_START : txCtrl_stateMachine_state_string = "START ";
      State_DATA : txCtrl_stateMachine_state_string = "DATA  ";
      State_PARITY : txCtrl_stateMachine_state_string = "PARITY";
      State_STOP : txCtrl_stateMachine_state_string = "STOP  ";
      default : txCtrl_stateMachine_state_string = "??????";
    endcase
  end
  always @(*) begin
    case(_zz_txCtrl_stateMachine_state)
      State_IDLE : _zz_txCtrl_stateMachine_state_string = "IDLE  ";
      State_START : _zz_txCtrl_stateMachine_state_string = "START ";
      State_DATA : _zz_txCtrl_stateMachine_state_string = "DATA  ";
      State_PARITY : _zz_txCtrl_stateMachine_state_string = "PARITY";
      State_STOP : _zz_txCtrl_stateMachine_state_string = "STOP  ";
      default : _zz_txCtrl_stateMachine_state_string = "??????";
    endcase
  end
  `endif

  assign txCtrl_newClockEnable = (1'b1 && txEnable);
  always @(*) begin
    txCtrl_clockDivider_counter_willIncrement = 1'b0;
    if(io_samplingTick) begin
      txCtrl_clockDivider_counter_willIncrement = 1'b1;
    end
  end

  assign txCtrl_clockDivider_counter_willClear = 1'b0;
  assign txCtrl_clockDivider_counter_willOverflowIfInc = (txCtrl_clockDivider_counter_value == 3'b111);
  assign txCtrl_clockDivider_counter_willOverflow = (txCtrl_clockDivider_counter_willOverflowIfInc && txCtrl_clockDivider_counter_willIncrement);
  always @(*) begin
    txCtrl_clockDivider_counter_valueNext = (txCtrl_clockDivider_counter_value + _zz_txCtrl_clockDivider_counter_valueNext);
    if(txCtrl_clockDivider_counter_willClear) begin
      txCtrl_clockDivider_counter_valueNext = 3'b000;
    end
  end

  always @(*) begin
    txCtrl_stateMachine_txd = 1'b1;
    case(txCtrl_stateMachine_state)
      State_IDLE : begin
      end
      State_START : begin
        txCtrl_stateMachine_txd = 1'b0;
      end
      State_DATA : begin
        txCtrl_stateMachine_txd = io_write_payload[txCtrl_tickCounter_value];
      end
      State_PARITY : begin
        txCtrl_stateMachine_txd = txCtrl_stateMachine_parity;
      end
      default : begin
      end
    endcase
  end

  always @(*) begin
    io_write_ready = 1'b0;
    case(txCtrl_stateMachine_state)
      State_IDLE : begin
      end
      State_START : begin
      end
      State_DATA : begin
        if(txCtrl_clockDivider_counter_willOverflow) begin
          if(when_UartCtrlTx_l74) begin
            io_write_ready = 1'b1;
          end
        end
      end
      State_PARITY : begin
      end
      default : begin
      end
    endcase
  end

  assign when_UartCtrlTx_l59 = ((io_write_valid && txCtrl_clockDivider_counter_willOverflow) && io_cts);
  assign when_UartCtrlTx_l74 = (txCtrl_tickCounter_value == io_config_dataLength);
  assign when_UartCtrlTx_l77 = (io_config_parity == ParityType_NONE);
  assign when_UartCtrlTx_l95 = (txCtrl_tickCounter_value == _zz_when_UartCtrlTx_l95);
  assign _zz_txCtrl_stateMachine_state = (io_write_valid ? State_START : State_IDLE);
  assign when_UartCtrlTx_l103 = (io_write_valid || (! (txCtrl_stateMachine_state == State_IDLE)));
  assign io_txd = txCtrl_stateMachine_txd_regNext;
  always @(posedge clk or negedge resetn) begin
    if(!resetn) begin
      txEnable <= 1'b1;
      txCtrl_stateMachine_txd_regNext <= 1'b1;
    end else begin
      if(when_UartCtrlTx_l103) begin
        txEnable <= 1'b1;
      end else begin
        txEnable <= 1'b0;
      end
      txCtrl_stateMachine_txd_regNext <= txCtrl_stateMachine_txd;
    end
  end

  always @(posedge clk or negedge resetn) begin
    if(!resetn) begin
      txCtrl_clockDivider_counter_value <= 3'b000;
      txCtrl_stateMachine_state <= State_IDLE;
    end else begin
      if(txCtrl_newClockEnable) begin
        txCtrl_clockDivider_counter_value <= txCtrl_clockDivider_counter_valueNext;
        case(txCtrl_stateMachine_state)
          State_IDLE : begin
            if(when_UartCtrlTx_l59) begin
              txCtrl_stateMachine_state <= State_START;
            end
          end
          State_START : begin
            if(txCtrl_clockDivider_counter_willOverflow) begin
              txCtrl_stateMachine_state <= State_DATA;
            end
          end
          State_DATA : begin
            if(txCtrl_clockDivider_counter_willOverflow) begin
              if(when_UartCtrlTx_l74) begin
                if(when_UartCtrlTx_l77) begin
                  txCtrl_stateMachine_state <= State_STOP;
                end else begin
                  txCtrl_stateMachine_state <= State_PARITY;
                end
              end
            end
          end
          State_PARITY : begin
            if(txCtrl_clockDivider_counter_willOverflow) begin
              txCtrl_stateMachine_state <= State_STOP;
            end
          end
          default : begin
            if(txCtrl_clockDivider_counter_willOverflow) begin
              if(when_UartCtrlTx_l95) begin
                txCtrl_stateMachine_state <= _zz_txCtrl_stateMachine_state;
              end
            end
          end
        endcase
      end
    end
  end

  always @(posedge clk) begin
    if(txCtrl_newClockEnable) begin
      if(txCtrl_clockDivider_counter_willOverflow) begin
        txCtrl_tickCounter_value <= (txCtrl_tickCounter_value + 4'b0001);
      end
      if(txCtrl_clockDivider_counter_willOverflow) begin
        txCtrl_stateMachine_parity <= (txCtrl_stateMachine_parity ^ txCtrl_stateMachine_txd);
      end
      case(txCtrl_stateMachine_state)
        State_IDLE : begin
        end
        State_START : begin
          if(txCtrl_clockDivider_counter_willOverflow) begin
            txCtrl_stateMachine_parity <= (io_config_parity == ParityType_ODD);
            txCtrl_tickCounter_value <= 4'b0000;
          end
        end
        State_DATA : begin
          if(txCtrl_clockDivider_counter_willOverflow) begin
            if(when_UartCtrlTx_l74) begin
              txCtrl_tickCounter_value <= 4'b0000;
            end
          end
        end
        State_PARITY : begin
          if(txCtrl_clockDivider_counter_willOverflow) begin
            txCtrl_tickCounter_value <= 4'b0000;
          end
        end
        default : begin
        end
      endcase
    end
  end


endmodule

module ClockDivider (
  input  wire [19:0]   io_value,
  input  wire          io_reload,
  output wire          io_tick,
  input  wire          clk,
  input  wire          resetn
);

  reg        [19:0]   counter;
  wire                tick;
  wire                when_ClockDivider_l26;

  assign tick = (counter == 20'h0);
  assign when_ClockDivider_l26 = (tick || io_reload);
  assign io_tick = tick;
  always @(posedge clk or negedge resetn) begin
    if(!resetn) begin
      counter <= 20'h0;
    end else begin
      counter <= (counter - 20'h00001);
      if(when_ClockDivider_l26) begin
        counter <= io_value;
      end
    end
  end


endmodule

module BufferCC (
  input  wire          io_dataIn,
  output wire          io_dataOut,
  input  wire          clk,
  input  wire          resetn
);

  (* async_reg = "true" *) reg                 buffers_0;
  (* async_reg = "true" *) reg                 buffers_1;

  assign io_dataOut = buffers_1;
  always @(posedge clk or negedge resetn) begin
    if(!resetn) begin
      buffers_0 <= 1'b0;
      buffers_1 <= 1'b0;
    end else begin
      buffers_0 <= io_dataIn;
      buffers_1 <= buffers_0;
    end
  end


endmodule
