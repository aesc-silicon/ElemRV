load("@rules_scala//scala:scala.bzl", "scala_library")

scala_library(
    name = "elements",
    #     lazy val nafarr = RootProject(file("../nafarr/"))
    # lazy val vexRiscv = RootProject(file("../vexriscv/"))
    # lazy val spinalCrypto = RootProject(file("../SpinalCrypto/"))
    srcs = glob(["{}/**/*.scala".format(folder) for folder in [
        "nafarr",
        #"vexriscv",
        "SpinalCrypto",
    ]]),
    plugins = [
        #"@elemrv_maven//:com_github_spinalhdl_spinalhdl_idsl_plugin_2_12",
    ],
    scalacopts = [],
    visibility = ["//visibility:public"],
    deps = [
        "@elemrv_maven//:com_github_spinalhdl_spinalhdl_core_2_12",
        "@elemrv_maven//:com_github_spinalhdl_spinalhdl_idsl_plugin_2_12",
        "@elemrv_maven//:com_github_spinalhdl_spinalhdl_lib_2_12",
        "@elemrv_maven//:org_scalatest_scalatest_2_12",
        "@elemrv_maven//:org_yaml_snakeyaml_1_8",
    ],
)
