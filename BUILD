load("@bazel-orfs//:openroad.bzl", "orfs_flow")
load("@rules_scala//scala:scala.bzl", "scala_binary", "scala_library")

# Generate sbt files
# ${RUN} 'BOARD={{ .board }} sbt "runMain {{ .package }}.{{ .board }}Generate"'

scala_binary(
    name = "generate_verilog",
    srcs = glob(["hardware/scala/**/*.scala"]),
    main_class = "SG13G2Generate",
    plugins = [
        "@elemrv_maven//:com_github_spinalhdl_spinalhdl_idsl_plugin_2_12",
    ],
    scalacopts = [],
    deps = [
        "//modules/elements",
        "@elemrv_maven//:com_github_spinalhdl_spinalhdl_core_2_12",
        "@elemrv_maven//:com_github_spinalhdl_spinalhdl_lib_2_12",
        "@elemrv_maven//:org_scalatest_scalatest_2_12",
        "@elemrv_maven//:org_yaml_snakeyaml_1_8",
    ],
    #resources = glob(["src/main/resources/**/*"]),
)

genrule(
    name = "verilog",
    srcs = [],
    outs = ["array.sv"],
    #       board: "SG13G2"
    #   package: "elemrv"

    #       - ${RUN} 'BOARD={{ .board }} sbt "runMain {{ .package }}.{{ .board }}Generate"'
    # environment = {
    #     "BOARD": "SG13G2",
    # },
    cmd = """
    $(execpath :generate_verilog) elemrv.SG13G2Generate
    """,
    tools = [
        ":generate_verilog",
    ],
)

PDK_ROOT = "pdks/IHP-Open-PDK/ihp-sg13g2"

filegroup(
    name = "additional_lefs",
    srcs = [lef.format(PDK_ROOT) for lef in [
        "{}/libs.ref/sg13g2_io/lef/sg13g2_io.lef",
        "{}/libs.ref/sg13g2_sram/lef/RM_IHPSG13_1P_512x32_c2_bm_bist.lef",
        "{}/libs.ref/sg13g2_sram/lef/RM_IHPSG13_1P_1024x8_c2_bm_bist.lef",
    ]],
)

filegroup(
    name = "gds_files",
    srcs = [gds.format(PDK_ROOT) for gds in [
        "{}/libs.ref/sg13g2_stdcell/gds/sg13g2_stdcell.gds",
        "{}/libs.ref/sg13g2_io/gds/sg13g2_io.gds",
        "{}/libs.ref/sg13g2_sram/gds/RM_IHPSG13_1P_512x32_c2_bm_bist.gds",
        "{}/libs.ref/sg13g2_sram/gds/RM_IHPSG13_1P_1024x8_c2_bm_bist.gds",
    ]],
)

# FIXME remove need for cp -r build/ buildx/, Bazel is hardcoded to ignore build/ folders
orfs_flow(
    name = "SG13G2Top",
    arguments = {
        "PLATFORM": "ihp-sg13g2",
        "DIE_AREA": "0.0 0.0 2522.4 2521.26",
        "CORE_AREA": "394.08 396.9 2125.44 2124.36",
        "MAX_ROUTING_LAYER": "TopMetal2",
        "HAS_IO_RING": "1",
        "TNS_END_PERCENT": "100",
        "PLACE_DENSITY": "0.75",
        "GDS_ALLOW_EMPTY": "RM_IHPSG13_1P_BITKIT_16x2_*",
        "ABC_CLOCK_PERIOD_IN_PS": "5000",
        "ADDITIONAL_LEFS": "$(locations :additional_lefs) $(PLATFORM_DIR)/lef/bondpad_70x70.lef",
        "GDS_FILES": "$(locations :gds_files) $(PLATFORM_DIR)/gds/bondpad_70x70.gds",
    },
    pdk = "@docker_orfs//:ihp-sg13g2",
    sources = {
        "SDC_FILE": ["buildx/ElemRV/SG13G2/zibal/SG13G2Top.sdc"],
        "SEAL_GDS": ["buildx/ElemRV/SG13G2/zibal/macros/sealring/sealring.gds.gz"],
        "FOOTPRINT_TCL": ["buildx/ElemRV/SG13G2/zibal/SG13G2Top.pad.tcl"],
        "PDN_TCL": ["buildx/ElemRV/SG13G2/zibal/SG13G2Top.pdn.tcl"],
        "MACRO_PLACEMENT_TCL": ["buildx/ElemRV/SG13G2/zibal/SG13G2Top.macro.tcl"],
        "TECH_LEF": ["{}/libs.ref/sg13g2_stdcell/lef/sg13g2_tech.lef".format(PDK_ROOT)],
        "SC_LEF": ["{}/libs.ref/sg13g2_stdcell/lef/sg13g2_stdcell.lef".format(PDK_ROOT)],
        "LIB_FILES": [lib.format(PDK_ROOT) for lib in [
            "{}/libs.ref/sg13g2_stdcell/lib/sg13g2_stdcell_typ_1p20V_25C.lib",
            "{}/libs.ref/sg13g2_io/lib/sg13g2_io_typ_1p2V_3p3V_25C.lib",
            "{}/libs.ref/sg13g2_sram/lib/RM_IHPSG13_1P_512x32_c2_bm_bist_typ_1p20V_25C.lib",
            "{}/libs.ref/sg13g2_sram/lib/RM_IHPSG13_1P_1024x8_c2_bm_bist_typ_1p20V_25C.lib",
        ]],
        # FIXME Not pretty, but works, this hack this available to all stages
        "EXTRA_DATA": [
            ":additional_lefs",
            ":gds_files",
        ],
    },
    verilog_files = glob(["buildx/ElemRV/SG13G2/zibal/*.v"]),
)
