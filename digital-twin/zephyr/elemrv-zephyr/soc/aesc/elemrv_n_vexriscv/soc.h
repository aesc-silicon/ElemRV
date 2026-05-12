/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * ElemRV-N VexRiscv SoC helpers: LiteX CSR access functions.
 * Mirrors soc/litex/litex_vexriscv/soc.h so upstream LiteX drivers work.
 */

#ifndef __RISCV32_ELEMRV_N_VEXRISCV_SOC_H_
#define __RISCV32_ELEMRV_N_VEXRISCV_SOC_H_

#include <zephyr/devicetree.h>
#include <zephyr/arch/riscv/sys_io.h>

#ifndef _ASMLANGUAGE

static inline unsigned char litex_read8(unsigned long addr)
{
#if CONFIG_LITEX_CSR_DATA_WIDTH >= 32
	return sys_read32(addr) & 0xff;
#else
	return sys_read8(addr);
#endif
}

static inline unsigned short litex_read16(unsigned long addr)
{
#if CONFIG_LITEX_CSR_DATA_WIDTH == 8
	return (sys_read8(addr) << 8) | sys_read8(addr + 0x4);
#elif CONFIG_LITEX_CSR_DATA_WIDTH >= 32
	return sys_read32(addr) & 0xffff;
#else
	return sys_read16(addr);
#endif
}

static inline unsigned int litex_read32(unsigned long addr)
{
#if CONFIG_LITEX_CSR_DATA_WIDTH == 8
	return (sys_read8(addr) << 24)
		| (sys_read8(addr + 0x4) << 16)
		| (sys_read8(addr + 0x8) << 8)
		| sys_read8(addr + 0xc);
#else
	return sys_read32(addr);
#endif
}

static inline uint64_t litex_read64(unsigned long addr)
{
#if CONFIG_LITEX_CSR_DATA_WIDTH == 32
	return ((uint64_t)sys_read32(addr) << 32) | (uint64_t)sys_read32(addr + 0x4);
#else
	return (((uint64_t)sys_read8(addr)) << 56)
		| ((uint64_t)sys_read8(addr + 0x4) << 48)
		| ((uint64_t)sys_read8(addr + 0x8) << 40)
		| ((uint64_t)sys_read8(addr + 0xc) << 32)
		| ((uint64_t)sys_read8(addr + 0x10) << 24)
		| ((uint64_t)sys_read8(addr + 0x14) << 16)
		| ((uint64_t)sys_read8(addr + 0x18) << 8)
		| (uint64_t)sys_read8(addr + 0x1c);
#endif
}

static inline void litex_write8(unsigned char value, unsigned long addr)
{
#if CONFIG_LITEX_CSR_DATA_WIDTH >= 32
	sys_write32((uint32_t)value, addr);
#else
	sys_write8(value, addr);
#endif
}

static inline void litex_write16(unsigned short value, unsigned long addr)
{
#if CONFIG_LITEX_CSR_DATA_WIDTH == 8
	sys_write8(value >> 8, addr);
	sys_write8(value, addr + 0x4);
#elif CONFIG_LITEX_CSR_DATA_WIDTH >= 32
	sys_write32((uint32_t)value, addr);
#else
	sys_write16(value, addr);
#endif
}

static inline void litex_write32(unsigned int value, unsigned long addr)
{
#if CONFIG_LITEX_CSR_DATA_WIDTH == 8
	sys_write8(value >> 24, addr);
	sys_write8(value >> 16, addr + 0x4);
	sys_write8(value >> 8, addr + 0x8);
	sys_write8(value, addr + 0xC);
#else
	sys_write32(value, addr);
#endif
}

static inline void litex_write64(uint64_t value, unsigned long addr)
{
#if CONFIG_LITEX_CSR_DATA_WIDTH == 32
	sys_write32(value >> 32, addr);
	sys_write32(value, addr + 0x4);
#else
	sys_write8(value >> 56, addr);
	sys_write8(value >> 48, addr + 0x4);
	sys_write8(value >> 40, addr + 0x8);
	sys_write8(value >> 32, addr + 0xC);
	sys_write8(value >> 24, addr + 0x10);
	sys_write8(value >> 16, addr + 0x14);
	sys_write8(value >> 8, addr + 0x18);
	sys_write8(value, addr + 0x1C);
#endif
}

static inline void litex_write(uint32_t addr, uint32_t size, uint32_t value)
{
	switch (size) {
	case 1:
		litex_write8(value, addr);
		break;
	case 2:
		litex_write16(value, addr);
		break;
	case 4:
		litex_write32(value, addr);
		break;
	default:
		break;
	}
}

static inline uint32_t litex_read(uint32_t addr, uint32_t size)
{
	switch (size) {
	case 1:
		return litex_read8(addr);
	case 2:
		return litex_read16(addr);
	case 4:
		return litex_read32(addr);
	default:
		return 0;
	}
}

#endif /* _ASMLANGUAGE */

#endif /* __RISCV32_ELEMRV_N_VEXRISCV_SOC_H_ */
