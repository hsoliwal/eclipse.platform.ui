<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Packed JFace forwarding contract

The complete private CustomHashtable operations forward to M3ViewerMap. Preserve cached hash values (including negative hashes), old bucket sizing and threshold, chain order, comparer identity, query-to-stored equality direction, replacement key identity, prehashed removal, copy iteration order, toString callback order, and null/exception boundaries. No new synchronization or default fail-fast behavior. Removed slots are quarantined until the outermost call returns, preserving callback reentrancy without entry objects. Reuse free slots only after releasing both references.

M3ViewerArrays.filter retains exactly one ordered select call per input slot. Capture the reference before each callback; never reread the source to reconstruct output. Empty input returns itself; nonempty input returns a fresh Object[] even when no element survives. Exceptions propagate unchanged. Dense input is permitted a small staging array; sparse input should not allocate input-sized temporary storage.

Qualification compares the pinned predecessor to real recipe outputs, including source-byte seals, identical API descriptors, callback traces, collisions, churn, reentrant mutations, exceptions, tree-selection observations and actual SWT viewer operations. Performance measurements are separate from correctness. Public/protected APIs and JNI symbols remain unchanged. These object-callback operations execute in Java; existing SWT JNI remains the native boundary.

Recipe-generated source is the only product candidate. Historical recipe resources are immutable. Full Eclipse/Tycho/PDE and non-Linux platform qualification remain separate gates.
