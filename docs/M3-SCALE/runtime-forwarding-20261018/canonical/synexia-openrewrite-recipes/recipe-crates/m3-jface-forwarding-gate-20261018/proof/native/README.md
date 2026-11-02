<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Recorded native JFace forwarding proof

The selected method `org.eclipse.jface.tests.viewers.M3TreeForwardingTest#testExpandedStateDelegatesToSwtWithoutWalkingColdVirtualSiblings` passed in genuine GTK3 with source-built SWT and JFace, the actual generated Commands receiver, JUnit 5.14.4 and Surefire 3.6.0. Exactly one test ran, with zero failures, errors or skips. The unchanged `M3EclipseComposition reports` verifier accepted the original Surefire XML. This is a bounded native classpath proof; a full Tycho/PDE reactor and the other native platform configurations are outside its scope.

## Inputs and the fixture correction

`SOURCE_PINS.json` records all 546 SWT and 423 JFace production compiler inputs, their Git blobs and SHA256 values, the 15-source test closure, all copied resources and each binary dependency checksum. The 15 Java test sources produce 31 classes: 13 unchanged upstream sources and two actual SDK-emitted sources in the passing build. Production classes were compiled for Java 21; the original upstream test hierarchy uses unnamed variables and was compiled and executed with JDK 25.0.4.1.

The preserved first native execution failed because `TestElement.createModel(2, 200)` made the selected descendant a leaf. The pre-existing SWT `TreeItem.test_setExpandedZ` contract requires leaves to remain unexpanded. The corrected fixture adds exactly one real model grandchild with `child.basicAddChild()` before setting viewer input. Every existing assertion and both 200-sibling domains remain unchanged. The canonical recipe emitted this exact correction; no SWT production behavior changed. The before and after class manifests prove that only `VirtualLazyTreeViewerTest.class` changed between the native executions.

## Reproduction binding

This directory preserves the exact Maven POM and display/test driver used in the successful run. They are recorded workspace adapters, with the original layout `/tmp/eclipse-m3-20261018/native-gate` and toolchain paths retained in the receipts. Copy these two files into that native workspace layout when replaying the recorded commands. The driver starts the verified genuine Xvfb, invokes standard Maven Surefire, feeds its unmodified XML to the existing Java report verifier, and stops Xvfb in a `finally` block. It refuses existing test XML so an earlier execution cannot be mistaken for a new one.

Reconstruct the pinned public source checkouts first. The compiler commands in `../../evidence/native/compile/*-compile.json` and the two `.args` files reproduce the source-built SWT, JFace and original test closure. Copy the resources identified in `SOURCE_PINS.json` into their recorded class directories. Run the canonical five-target recipe, then compile its actual corrected fixture using `../../evidence/native/native-green/corrected-fixture-compile.json`. The checksum of that SDK output is `96f9ca262513ee4f964016d35682ac8a8641a976d9b1d957dac0968d34ad55c7`.

With those inputs in place, invoke the recorded driver:

```sh
python /tmp/eclipse-m3-20261018/native-gate/run-native-gate.py
```

The generated Commands JAR on the runtime classpath has SHA256 `4d7573b670652c41d105210deeffde877c9034e2bd2916a6fb7ec4b5cf454c1e`. Compilation of the original JFace/test API used the genuine published Commands API; the passing native runtime used this generated receiver. The POM explicitly selects the real Surefire JUnit Platform provider because the verified standalone JUnit distribution does not provide separate dependency coordinates for automatic provider discovery.

`../../evidence/native/toolchain/` preserves actual acquisition, source/native compilation, display readiness and exact toolchain checksums. The local Unix display socket required authorized execution outside the restrictive workspace sandbox; the server disables TCP listening. No JARs, class files, native libraries, images or replacement test harnesses are distributed in this packet.

## Evidence

The original failing XML, logs, input class/resource hashes and report-verifier rejection are in `../../evidence/native/native-leaf-fixture-failure/`. The passing XML, logs, verifier acceptance and sole compiled-fixture delta are in `../../evidence/native/native-green/`. The passing XML SHA256 is `bc984d3d195c38ef37619a0954b73ee35bea0b2585b587e25c734256a6a3e52a`; the failing XML SHA256 is `1ed312a4862b64ed2338dbdfffda5633612ca733222b6303d58e7f26876bfb70`.

`stage-native-packet.py` only copies those existing observations and records their source bindings. It does not execute product tests or generate replacement XML. Both actual native outcomes remain preserved.
