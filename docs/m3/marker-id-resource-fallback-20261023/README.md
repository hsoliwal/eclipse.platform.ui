<!-- SPDX-License-Identifier: Apache-2.0 -->
# Eclipse M3 marker ID/resource collision safety gate

**Canonical recipe owner:** [Synexia PR #13201](https://github.com/hsoliwal/com.synexia/pull/13201), commit `6b84b895004f27a730fec0f0cf79c810ae9e7302`. Product owner: existing `M3MarkerLanes` and `M3MarkerDeltas` classes inside Eclipse UI. This is a source-sealed, **direct owner modification**; it adds no independent marker store, new Eclipse runtime, JNI symbol or parallel M3 adapter.

## Current scope and contract
The existing viewport lane and pending delta hash tables still key by a long marker ID. Marker handles with identical IDs but unequal resources cannot be represented simultaneously in those tables. The new explicit `(id, resource equality)` check refuses incompatible fast paths, preserving the original stock Eclipse rebuild rather than quietly replacing or losing a marker. The guard covers clean-build duplicate detection, pending delta coalescing, live lane updates, scope-diff comparison and marker item lookup. A null/unknown resource is treated as ambiguous.

**This is a fail-closed intermediate correction, not the final composite-key fast path.** Full M3 tuple-key indexing must later update lane storage, pending coalescing, `M3LongSet`, scope-sort/diff, window patch membership and tests atomically. The existing Eclipse reconstruction remains the correctness owner when a tuple collision appears.

## Full source-seal SHA-256 manifest
```tsv
bundles/org.eclipse.ui.ide/src/org/eclipse/ui/internal/views/markers/M3MarkerLanes.java	00194177b3ac3ccaa3e7d09f9c06be5652730fa0b506f0eace48468e9cdafa62	ad1068dfd913b3f520cbb3f37ce059004583667146fb6a3bfb5f4d86edc1e13d	M3MarkerLanes.java.txt
bundles/org.eclipse.ui.ide/src/org/eclipse/ui/internal/views/markers/M3MarkerDeltas.java	7d8f4917446651375dd2ba710d95dba5d3f12f216d2c3bb76428f78eb84cc6e5	5fc38b3d008adc874fba8d90a5276c3cd4e7715cbc4fae81b9eb874f055e7247	M3MarkerDeltas.java.txt
```

Frozen parent: `b28380334e2130ba2f92a8e408fa423ef255e43a`. Before sources are complete and pinned in the Synexia recipe crate. The two class source license headers (Apache-2.0 OR EPL-2.0) were retained verbatim. The modifications preserve existing Eclipse public interfaces and build classes.

## Qualification
- **Source custody:** exact pinned source and generated-postimage Git blob matching, no deletions.
- **Host helper proof:** `verify-identity.sh` extracts the actual `sameIdentity` method from this product source into Java21 stubs and exercises eight equality/collision/null cases, in normal and `-Xint`. Its job must pass; it is NOT Eclipse runtime acceptance.
- **Still HOLD:** canonical Maven/OpenRewrite fixed-point/drift-refusal; original Eclipse compiled Java25 JDT/PDE, OSGi/Equinox + actual SWT/JFace marker functional tests, full Tycho and multi-platform UI. No merge readiness claimed.

This receiver carries only generated product source and acceptance artifacts; Maven/OpenRewrite authoring remains in Synexia.
