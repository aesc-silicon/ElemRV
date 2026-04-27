# SPDX-FileCopyrightText: 2026 aesc silicon
#
# SPDX-License-Identifier: Apache-2.0

# Nothing to do here

FW_DIR=${BUILD_ROOT}/${SOC}/${BOARD}/firmware/
IMG_CONTAINER=${FW_DIR}/image_container.img

dd if=/dev/zero of=${IMG_CONTAINER} bs=32M count=1
dd if=${FW_DIR}/demo/kernel.img of=${IMG_CONTAINER} conv=notrunc
