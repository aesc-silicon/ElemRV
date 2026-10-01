/*
 * SPDX-FileCopyrightText: 2026 aesc silicon
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#include "soc.h"
#include "syscon.h"
#include "console.h"
#include "sysinfo.h"

void print_banner(const char *name)
{
	print("\r\n");
	print(name);
	print("\r\n");
}

static const char *known(const char *name)
{
	return name != 0 ? name : "?";
}

static int misa_has(unsigned int misa, char ext)
{
	return (misa >> (ext - 'A')) & 1;
}

/* Base ISA and single-letter extensions in canonical order, then the privilege modes */
static void print_isa(void)
{
	const char *order = "IEMAFDQLCBJTPVNHX";
	unsigned int misa;

	__asm__ volatile("csrr %0, misa" : "=r"(misa));

	print("ISA:      ");
	print((misa >> 30) == 2 ? "RV64" : "RV32");
	for (const char *ext = order; *ext != '\0'; ext++) {
		if (misa_has(misa, *ext)) {
			print_char(*ext);
		}
	}
	print(" (misa ");
	print_hex(misa);
	print(")\r\nModes:    M");
	if (misa_has(misa, 'S')) {
		print(" S");
	}
	if (misa_has(misa, 'U')) {
		print(" U");
	}
	print("\r\n");
}

void print_system_info(void)
{
	struct syscon_driver syscon;

	syscon_init(&syscon, SYSCONCTRLMAPPER_BASE);

	print("Vendor:   ");
	print(known(syscon_vendor_name(syscon_vendor(&syscon))));
	print("\r\nProduct:  ");
	print(known(syscon_product_name(syscon_product(&syscon))));
	print(" rev ");
	print_dec(syscon_silicon_major(&syscon), 1);
	print_char('.');
	print_dec(syscon_silicon_minor(&syscon), 1);
	print("\r\nPlatform: ");
	print(known(syscon_platform_name(syscon_platform(&syscon))));
	print(" (");
	print(known(syscon_platform_class_name(syscon_platform_class(&syscon))));
	print(")\r\nBuilt:    ");
	print_date(syscon_build_date(&syscon));
	print("\r\nRefclk:   ");
	print_dec(syscon_ref_clock(&syscon), 1);
	print(" Hz\r\n");
	print_isa();
	print("IP:      ");
	for (unsigned int feature = 0; feature < SYSCON_FEATURE_COUNT; feature++) {
		if (syscon_has_feature(&syscon, feature)) {
			print_char(' ');
			print(syscon_feature_name(feature));
		}
	}
	print("\r\n");
}
