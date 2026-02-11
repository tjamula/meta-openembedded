SUMMARY = "ExecuTorch C++ runtime and executor_runner for embedded targets"
HOMEPAGE = "https://github.com/pytorch/executorch"

# Version
PV = "1.0+git${SRCPV}"

# License (ExecuTorch core)
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://LICENSE;md5=da6742972636cd56d418bee2df32b873"

# -----------------------------------------------------------------------------
# Source
# -----------------------------------------------------------------------------

# Fetch ExecuTorch into a directory named exactly "executorch"
# (required by upstream CMake, see executorch issue #6475)
SRC_URI = "gitsm://github.com/pytorch/executorch.git;branch=main;protocol=https;destsuffix=executorch"
SRCREV = "${AUTOREV}"

S = "${UNPACKDIR}/executorch"

# -----------------------------------------------------------------------------
# Build system
# -----------------------------------------------------------------------------

inherit cmake pkgconfig

# -----------------------------------------------------------------------------
# TEMPORARY WORKAROUND (Bring-up only)
#
# ExecuTorch CMake uses FetchContent / ExternalProject extensively.
# In Yocto offline mode this causes extremely long or stuck do_configure.
#
# We temporarily allow network access so CMake can fetch what it needs.
# This should be revisited once upstream CMake becomes Yocto-friendly.
# -----------------------------------------------------------------------------

do_configure[network] = "1"
do_compile[network]   = "1"

EXTRA_OECMAKE:append = " \
  -DCMAKE_FIND_USE_PACKAGE_REGISTRY=OFF \
  -DCMAKE_FIND_USE_SYSTEM_PACKAGE_REGISTRY=OFF \
"
EXTRA_OECMAKE:append = " -DFETCHCONTENT_QUIET=OFF "
EXTRA_OECMAKE:append = " -DCMAKE_FIND_DEBUG_MODE=ON "
EXTRA_OECMAKE:append = " -DFETCHCONTENT_FULLY_DISCONNECTED=OFF "
# -----------------------------------------------------------------------------
# Dependencies
# -----------------------------------------------------------------------------

# flatbuffers-native -> provides 'flatc' code generator
# gflags             -> linked by executor_runner
DEPENDS += "flatbuffers-native gflags"

# -----------------------------------------------------------------------------
# CMake configuration
# -----------------------------------------------------------------------------

EXTRA_OECMAKE += "\
  -DCMAKE_BUILD_TYPE=Release \
  -DBUILD_TESTING=OFF \
  -DEXECUTORCH_BUILD_PYBIND=OFF \
  -DEXECUTORCH_BUILD_PORTABLE_OPS=ON \
  -DEXECUTORCH_BUILD_EXECUTOR_RUNNER=ON \
  -DEXECUTORCH_ENABLE_LOGGING=ON \
  -DEXECUTORCH_BUILD_XNNPACK=OFF \
  -DEXECUTORCH_BUILD_QNN=OFF \
  -DCMAKE_CXX_STANDARD=17 \
"

# -----------------------------------------------------------------------------
# Feature control (future-proofing)
# -----------------------------------------------------------------------------

PACKAGECONFIG ??= "portable-ops runner"

PACKAGECONFIG[runner] = "\
  -DEXECUTORCH_BUILD_EXECUTOR_RUNNER=ON,\
  -DEXECUTORCH_BUILD_EXECUTOR_RUNNER=OFF,\
  gflags,\
"

PACKAGECONFIG[portable-ops] = "\
  -DEXECUTORCH_BUILD_PORTABLE_OPS=ON,\
  -DEXECUTORCH_BUILD_PORTABLE_OPS=OFF,\
  ,\
"

# Optional: Qualcomm QNN backend (OFF by default)
PACKAGECONFIG[qnn] = "\
  -DEXECUTORCH_BUILD_QNN=ON -DQNN_SDK_ROOT=${QNN_SDK_ROOT},\
  -DEXECUTORCH_BUILD_QNN=OFF,\
  qnn-sdk,\
"

QNN_SDK_ROOT ?= "${STAGING_DIR_HOST}/opt/qnn-sdk"

# -----------------------------------------------------------------------------
# Install
# -----------------------------------------------------------------------------

do_install() {
    install -d ${D}${bindir}

    # executor_runner location depends on generator/layout
    if [ -f ${B}/executor_runner ]; then
        install -m 0755 ${B}/executor_runner ${D}${bindir}/executor_runner
    elif [ -f ${B}/examples/portable/executor_runner/executor_runner ]; then
        install -m 0755 ${B}/examples/portable/executor_runner/executor_runner \
            ${D}${bindir}/executor_runner
    else
        bbfatal "executor_runner binary not found after build"
    fi
}

# -----------------------------------------------------------------------------
# Packaging
# -----------------------------------------------------------------------------

PACKAGES += "${PN}-runner"
FILES:${PN}-runner = "${bindir}/executor_runner"

# Base package intentionally empty for now
ALLOW_EMPTY:${PN} = "1"