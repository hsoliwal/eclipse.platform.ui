<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Frozen gate and text-owner contract

The UI preimage is `16e944f83eec18d4cf609afd9ad9a937711211ce`; the SWT
receiver is `422fb369629ba47fc5c5c6ddf01794b6d7c79f42`. Exact source and template
digests are recorded in `SOURCE_PINS.json` and the Java/text manifests.

The receiver recipe emits three Java files and two native-format metadata files.
Two Java files use the existing typed Java owner. The pre-existing Java 25
fixture uses the exact crate/path/before/after bootstrap admitted by the existing
text owner. All other sources remain unchanged.

The native method is
`testExpandedStateDelegatesToSwtWithoutWalkingColdVirtualSiblings`. Its required
gate resolves the eight real SWT forwarding methods using public method handles,
then invokes the existing behavioral test. The selected child has one real child
before viewer input, making it expandable under SWT's established leaf contract.
Every expansion, identity, and lazy-realization assertion remains unchanged.

The shared text owner admits only the additional exact metadata paths and the
sealed Java 25 fixture tuple. It refuses Git internals ignoring case and rejects
target path aliases during both scan and visit, including POSIX literal-backslash
aliases. Unrelated sources are preserved. Duplicate, absent, drifted and occupied
targets remain fail-closed.

The final 15 SDK checks pass. The genuine GTK3 method passes once with no failure,
error or skip; the unchanged report verifier accepts its original Surefire XML.
The original native failure, typed Java 21 parser refusal, and alias regression
failure remain recorded as failures. Production SWT/JFace classes are identical
between the failed leaf-fixture run and the corrected passing run.

There is no JUnit stub, widget stub, synthetic native backend, report adapter or
runtime fallback. A skipped native method cannot qualify the composition. The
recipe and read-only verifier use Java 21. In the recorded native proof, SWT
sources compile with javac 21 at release 21; JFace sources compile with javac 25
at release 21. The native test hierarchy and corrected fixture compile with
javac 25 at release 25. Full Tycho/PDE SDK and actual initialized 12-module
composition acceptance require their own successful executions.
