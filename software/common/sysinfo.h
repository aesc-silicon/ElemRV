/*
 * SPDX-FileCopyrightText: 2026 aesc silicon
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#ifndef ELEMRV_SYSINFO_H
#define ELEMRV_SYSINFO_H

/* Prints the SoC name on its own line, e.g. "ElemRV-N" */
void print_banner(const char *name);

/*
 * Prints the syscon identity, build date and reference clock, the ISA and privilege modes
 * from misa, and the IP cores the syscon reports. Z extensions have no M-mode discovery
 * register and are not shown.
 */
void print_system_info(void);

#endif
