#!/bin/bash
# SPDX-FileCopyrightText: 2025 aesc silicon
#
# SPDX-License-Identifier: CERN-OHL-W-2.0

# Build the N-specific co-simulation libraries (minimal slice: gpio_n + uart_lite).
# Run inside Docker at /workspace/elemrv/digital-twin/renode/verilated/wrappers/

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_MODE="${1:-release}"

PERIPHERALS="gpio_n uart_lite"

echo "Building N-specific co-simulation libraries (${BUILD_MODE} mode)..."
echo ""

FAIL=0
TOTAL=0
for p in $PERIPHERALS; do
    TOTAL=$((TOTAL + 1))
    echo "=== Building ${p} ==="
    if make -f "${SCRIPT_DIR}/Makefile.${p}" -C "${SCRIPT_DIR}" clean 2>/dev/null && \
       make -f "${SCRIPT_DIR}/Makefile.${p}" -C "${SCRIPT_DIR}" BUILD_MODE="${BUILD_MODE}"; then
        echo "  OK: lib${p}.so"
    else
        echo "  FAILED: lib${p}.so"
        FAIL=$((FAIL + 1))
    fi
    echo ""
done

echo "============================================"
if [ $FAIL -eq 0 ]; then
    echo "All ${TOTAL} N-specific libraries built successfully."
else
    echo "${FAIL} library build(s) failed!"
    exit 1
fi
