# Update Chip Size

Recalculate and update the `dieArea` and `coreArea` for a chip target based on the current utilization density.

## Input

The user provides:
- **Chip**: e.g. `ElemRV-H` or `ElemRV-N` (maps to `hardware/scala/elemrv_h/` or `elemrv_n/`)
- **Platform**: e.g. `SG13G2` (maps to `<chip_dir>/SG13G2/SG13G2.scala`)
- **Current density**: the density reported by the place-and-route tool (e.g. `95%`)
- **Measured profile** (optional): which `FLOORPLAN` profile the density was
  measured with (`relaxed` or `tapeout`). Defaults to `relaxed`. Only relevant
  for targets whose file selects the floorplan via a `FLOORPLAN` match block.

## Target Density

- **tapeout**: ~75-80% (use 78% for the calculation unless the user overrides)
- **relaxed**: ~60%

Note that the post-repair design area grows with density pressure (the resizer
inserts buffers when the floorplan is tight), so after resizing, re-measure with
a new run and iterate if the reported density is still off target.

## PDK Reference

Look up the platform in this table to determine the PDK, manufacturer grid, and IO ring margins.

| Platform   | PDK      | X Grid (µm) | Y Grid (µm) | Core Margin Left (µm) | Core Margin Bottom (µm) | Core Margin Right (µm) | Core Margin Top (µm) |
|------------|----------|-------------|-------------|----------------------|------------------------|----------------------|---------------------|
| SG13G2     | IHP      | 0.48        | 3.78        | 394.08               | 396.9                  | 396.96               | 396.9               |
| SG13CMOS5l | IHP      | 0.48        | 3.78        | 394.08               | 396.9                  | 396.96               | 396.9               |
| GF180MCU   | GF180MCU | TBD         | TBD         | TBD                  | TBD                    | TBD                  | TBD                 |
| SKY130     | SKY130   | TBD         | TBD         | TBD                  | TBD                    | TBD                  | TBD                 |

If any required value is `TBD`, abort and tell the user that the PDK parameters for this platform are not yet configured.

## Procedure

1. **Locate the file**: Map the chip name to its directory under `hardware/scala/` (e.g. `ElemRV-H` → `elemrv_h`, `ElemRV-N` → `elemrv_n`). The platform file is at `hardware/scala/<chip_dir>/<Platform>/<Platform>.scala`.

2. **Look up PDK parameters** from the table above using the platform name. Abort if any values are TBD.

3. **Read the file** and extract the current `chip.dieArea` and `chip.coreArea` values. These are tuples of 4 doubles: `(x1, y1, x2, y2)`. Only update lines that set `chip.dieArea` and `chip.coreArea` — NOT sub-block die/core areas (e.g. `hyperbus.dieArea`, `cpu.dieArea`).
   - If the file selects the floorplan via a `FLOORPLAN` match block, there are
     TWO die/core pairs: the `case "tapeout"` branch and the default (relaxed)
     branch. Take the current values from the branch matching the **measured
     profile**, and compute new sizes for BOTH branches (tapeout at 78%,
     relaxed at 60%).
   - If there is no match block (single pair), size that pair at the tapeout
     target.

4. **Calculate new dimensions** (per profile):
   - Current core width = `x2 - x1` of coreArea (measured profile's branch)
   - Current core height = `y2 - y1` of coreArea
   - Current core area = width × height
   - Utilized area = current core area × (current density / 100)
   - New core area = utilized area / (target density / 100)
   - Scale factor = sqrt(new core area / current core area)
   - New core width = current core width × scale factor
   - New core height = current core height × scale factor

5. **Snap to manufacturer grid**:
   - X values must be multiples of the PDK's X Grid
   - Y values must be multiples of the PDK's Y Grid
   - Round new core dimensions **up** (ceiling) to the grid

6. **Compute new areas** using the PDK's IO ring margins:
   - New core: `(margin_left, margin_bottom, margin_left + new_core_width, margin_bottom + new_core_height)`
   - New die: `(0, 0, margin_left + new_core_width + margin_right, margin_bottom + new_core_height + margin_top)`
   - Snap the die `x2` up to the nearest multiple of X Grid and die `y2` up to the nearest multiple of Y Grid

7. **Update the file**: Replace only the `chip.dieArea` and `chip.coreArea` lines with the new values (both profile branches when a `FLOORPLAN` match block exists). Format numbers with 2 decimal places.

8. **Report the changes**: Show the old and new values per profile, the old and new core area in µm², and the expected new density (tapeout ~78%, relaxed ~60%).
