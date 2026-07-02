#!/bin/bash

# SPDX-FileCopyrightText: 2026 aesc silicon
#
# SPDX-License-Identifier: Apache-2.0

FW_DIR=${BUILD_ROOT}/${SOC}/firmware/
IMG_CONTAINER=${FW_DIR}/zephyr_container.img

dd if=/dev/zero of=${IMG_CONTAINER} bs=1k count=512
dd if=${FW_DIR}/bootrom/kernel.img of=${IMG_CONTAINER} conv=notrunc
dd if=${FW_DIR}/zephyr/zephyr/zephyr.bin of=${IMG_CONTAINER} seek=64 bs=1k conv=notrunc
echo "Generated ${IMG_CONTAINER}"
