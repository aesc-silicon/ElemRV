#!/bin/bash
# SPDX-FileCopyrightText: 2025 aesc silicon
#
# SPDX-License-Identifier: CERN-OHL-W-2.0

# Digital Twin test runner (ElemRV-N + GPIO + UART slice).
# Runs inside the simulation container at
# /workspace/elemrv/digital-twin/renode/.
# Callers (CI, manual) invoke via:
#   docker exec <container> bash -c 'cd /workspace/elemrv/digital-twin/renode && bash run_tests.sh'

set -euo pipefail

PASS=0
FAIL=0
TOTAL=0
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ -t 1 ]; then
    GREEN='\033[0;32m'
    RED='\033[0;31m'
    NC='\033[0m'
else
    GREEN=''
    RED=''
    NC=''
fi

run_test() {
    local name="$1"
    local script="$2"
    local pass_marker="${3:-PASSED}"
    TOTAL=$((TOTAL + 1))
    local logfile="/tmp/dt_test_${TOTAL}.log"

    echo ""
    echo "=== TEST $TOTAL: $name ==="

    if [ ! -f "$SCRIPT_DIR/$script" ]; then
        echo -e "  ${RED}FAIL${NC}: Script not found: $script"
        FAIL=$((FAIL + 1))
        return
    fi

    if renode --disable-xwt --console \
        -e "include @$SCRIPT_DIR/$script" \
        > "$logfile" 2>&1; then
        :
    fi

    echo "  --- output tail ---"
    tail -10 "$logfile" | sed 's/^/  | /'
    echo "  ---"

    # Filter benign Renode/Zicsr noise so genuine errors still trip the test.
    local filtered_errors
    filtered_errors=$(grep -i "error" "$logfile" \
        | grep -vi "Could not tokenize" \
        | grep -vi "Couldn't find" \
        | grep -vi "error_count" \
        | grep -vi "Zicsr instruction set is not enabled" \
        | grep -vi "([0-9]\+ error" \
        | grep -vi "0 error" \
        || true)

    if grep -q "$pass_marker" "$logfile" && [ -z "$filtered_errors" ]; then
        echo -e "  RESULT: ${GREEN}PASS${NC}"
        PASS=$((PASS + 1))
    else
        echo -e "  RESULT: ${RED}FAIL${NC}"
        if [ -n "$filtered_errors" ]; then
            echo "  Errors found:"
            echo "$filtered_errors" | sed 's/^/    /'
        fi
        if ! grep -q "$pass_marker" "$logfile"; then
            echo "  Missing pass marker: '$pass_marker'"
        fi
        FAIL=$((FAIL + 1))
    fi
}

run_verilator_testbenches() {
    local name="N Pure Verilator Testbenches"
    TOTAL=$((TOTAL + 1))
    local logfile="/tmp/dt_verilator_tb.log"
    local makefile="$SCRIPT_DIR/verilated/testbenches/Makefile.nitrogen"

    echo ""
    echo "=== TEST $TOTAL: $name ==="

    if [ ! -f "$makefile" ]; then
        echo -e "  ${RED}FAIL${NC}: $makefile not found"
        FAIL=$((FAIL + 1))
        return
    fi

    if make -f "$makefile" -C "$SCRIPT_DIR/verilated/testbenches" run \
        > "$logfile" 2>&1; then
        :
    fi

    echo "  --- output tail ---"
    tail -8 "$logfile" | sed 's/^/  | /'
    echo "  ---"

    if grep -q "All N testbenches PASSED" "$logfile"; then
        echo -e "  RESULT: ${GREEN}PASS${NC}"
        PASS=$((PASS + 1))
    else
        echo -e "  RESULT: ${RED}FAIL${NC}"
        FAIL=$((FAIL + 1))
    fi
}

echo "============================================"
echo "  ElemRV Digital Twin Test Suite"
echo "  Slice: ElemRV-N + GPIO + UART"
echo "============================================"
echo "  Working dir: $SCRIPT_DIR"

# Test 1: N base platform (Renode only, sanity for sysbus + RunFor).
run_test "N Base Platform" "run_n_base_test.resc" \
    "ElemRV-N Base Platform Test PASSED"

# Test 2: N GPIO co-simulation (libgpio_n.so).
if [ -f "$SCRIPT_DIR/verilated/libs/libgpio_n.so" ]; then
    run_test "N GPIO Co-simulation" "run_n_cosim_gpio_test.resc" \
        "N GPIO Co-simulation Test PASSED"
else
    echo ""
    echo "=== TEST (skipped): N GPIO Co-simulation ==="
    echo "  libgpio_n.so not built; run build_n_cosim.sh"
fi

# Test 3: N UART Lite co-simulation (libuart_lite.so).
if [ -f "$SCRIPT_DIR/verilated/libs/libuart_lite.so" ]; then
    run_test "N UART Lite Co-simulation" "run_n_cosim_uart_lite_test.resc" \
        "N UART Lite Co-simulation Test PASSED"
else
    echo ""
    echo "=== TEST (skipped): N UART Lite Co-simulation ==="
    echo "  libuart_lite.so not built; run build_n_cosim.sh"
fi

# Test 4: standalone Verilator testbenches.
run_verilator_testbenches

# Test 5: Zephyr blinky on ElemRV-N.
if [ -f "$SCRIPT_DIR/../zephyr/elemrv-zephyr/build-n-blinky/zephyr/zephyr.elf" ]; then
    run_test "N Zephyr Blinky" "run_n_zephyr_blinky.resc" \
        "N Zephyr Blinky Test PASSED"
else
    echo ""
    echo "=== TEST (skipped): N Zephyr Blinky ==="
    echo "  build-n-blinky/zephyr/zephyr.elf not found"
    echo "  build with: west build -b elemrv_n app/blinky -d build-n-blinky"
fi

echo ""
echo "============================================"
echo "  SUMMARY: ${PASS}/${TOTAL} passed, ${FAIL} failed"
echo "============================================"
if [ "$FAIL" -ne 0 ]; then
    exit 1
fi
