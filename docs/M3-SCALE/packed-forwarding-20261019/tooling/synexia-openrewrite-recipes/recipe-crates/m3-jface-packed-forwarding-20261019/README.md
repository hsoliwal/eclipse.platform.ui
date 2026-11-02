<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# JFace packed M3 forwarding — review candidate

The complete private `CustomHashtable` now forwards to the internal `M3ViewerMap`.
It stores keys, values, cached hashes and links in parallel arrays, reuses released
slots, and quarantines removals during reentrant client callbacks. The existing
`M3ViewerArrays.filter` uses a dense-prefix decision and compact sparse-result growth.
Original Eclipse public names, comparer semantics and SWT/JNI owners remain intact.

This extends the merged first JFace pass. It is **not the entire Eclipse migration**.
The existing M3JDK framework snapshots remain separate additive adapters. Workbench,
JDT, PDE, Equinox internals, other UI bundles and intrinsic widget internals have not
been converted by this pass. No new native function is warranted for arbitrary Java
callbacks; real SWT JNI is exercised by the native-viewer proof.

## Evidence

- Actual Maven/OpenRewrite SDK execution: source seals, fixed point, missing/drift/
  duplicate/occupied-target refusal, reverse order and partial replay.
- 445,093 original JFace assertions plus 1,075,302 packed-map/selection assertions
  per variant; identical callback traces and public/protected descriptors.
- Real GTK native TableViewer (normal and virtual) and TreeViewer: 538 checks and
  372,235 callbacks match baseline and candidate; original widgets are retained.
- Instrumented 10,000-entry structural heap: 426,544 -> 292,656 bytes; structural
  objects 10,001 -> 2. Steady remove/reinsert allocation: 32 -> 0 bytes per pair.
  Empty large maps release their payload lanes; references are checked after removal.
- Filtering 1,024 rows: at 1/16 acceptance, allocation 4,384 -> 496 bytes/call;
  at 1/1024 acceptance, 4,136 -> 104. Dense acceptance costs 80 additional bytes.
  CPU results are mixed, including slower dense-filter/map samples. This is an
  allocation/structure improvement, **not a universal speedup claim**.

## Reproduce

Use Java 21, Maven 3.9.9, the exact offline Maven cache and pinned Eclipse artifacts
recorded by the proof. From this crate, with a new work directory outside the crate:

```sh
python proof/qualify.py recipe --work "$WORK" --jdk "$JDK" --maven "$MVN" --m2 "$M2" --deps "$DEPS"
python proof/qualify.py product --work "$WORK" --jdk "$JDK" --maven "$MVN" --m2 "$M2" --deps "$DEPS"
python proof/runtime.py --work "$WORK" --jdk "$JDK" --m2 "$M2" --deps "$DEPS" --xvfb "$XVFB"
```

The recipe compiles the unchanged canonical engines at `../../src/main/java`.
Only SDK outputs are product changes. Fix postimage templates and rerun the recipe;
never patch generated Java. The metadata recipe separately appends custody records
through the same canonical text engine, without rewriting historical records.

Native proof uses pinned current selected JFace sources and original JFace 3.37/SWT
3.130 supporting binaries. It is not a full source-built Eclipse or proof of the
current SVG-capability OSGi manifest. Full Tycho/PDE/API/platform admission remains
pending; the pinned upstream Linux CI already fails on NLS diagnostics and a baseline
version/content check. Keep the PR draft. Existing constructor/raw-type warnings
are retained in both variants; new recipe and proof compilation uses `-Werror`.

The historical first-pass recipes and useful cached-hash/prehashed-removal changes
are retained. See CONTRACT.md, CATALOGUE.tsv, SOURCE_PINS.json and evidence.
