<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Retained qualification corrections

1. Trial 01: baseline compilation required current TreePathViewerComparator, which
   was absent from the older supporting JFace binary. Added its exact pinned source
   to proof inputs; no target algorithm changed to hide the dependency gap.
2. Trial 02: the initial sparse-filter growth check slowed dense input and emptied
   maps retained large lanes. Corrected the recipe to select the dense loop once and
   release large empty lanes. Re-executed SDK, compilation and all behavior/API gates.
   The initial outputs and measured logs remain in evidence/rejected.
3. The first display attempt lacked the pinned xkbcomp executable at Xvfb's compiled
   path. The runtime driver creates a temporary link only when absent and removes
   it afterward. It retains authenticated X access.
4. Native client corrections: an IElementComparer must also accept the viewer input;
   SWT.VIRTUAL items require original SetData materialization before their data is
   observed; model mutations must precede incremental viewer notifications. These
   were baseline failures, not generated-map failures. Logs remain unchanged.
5. Even with the current TableViewer and AbstractTableViewer sources, sorted virtual
   incremental insertion leaves the new row unmapped before refresh in the frozen
   baseline. The contract-preserving comparison explicitly freezes that observation,
   then requires correct selection after refresh. It does not claim to fix this
   separate baseline behavior. Trial 04 / 22-baseline-native.log preserves the failure.

CPU measurements remain mixed. Lower allocation is measured separately from timing;
slower dense samples have not been dropped from the evidence.
