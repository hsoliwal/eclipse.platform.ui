<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Required JFace native forwarding gate

The existing Eclipse UI fork forwards virtual-tree expansion operations to the
SWT fork. Its upstream-compatible test can skip when those SWT methods are
unavailable. This recipe adds an opt-in acceptance gate that requires the methods
and executes the real native lazy-tree behavioral contract.

The existing Java and text snapshot owners emit exactly five reviewed results:

| Receiver | Change |
| --- | --- |
| `M3TreeForwardingTest.java` | Require all eight SWT forwarding methods, then execute the native lazy-tree contract |
| `AllViewersTests.java` | Register the required gate in the existing viewer suite |
| UI root `pom.xml` | Add the `m3-forwarding` profile and its forked-VM system property |
| JFace test `MANIFEST.MF` | Import the actual Jupiter condition package for OSGi resolution |
| `VirtualLazyTreeViewerTest.java` | Give the selected child one real child so the existing expansion assertions exercise an expandable node |

The required property is `org.eclipse.jface.tests.m3Required=true`. The gate uses
ordinary SWT `Tree` and `TreeItem` instances and the existing
`VirtualLazyTreeViewerTest` hierarchy.

## Native finding and correction

The first genuine GTK3 run failed: the existing fixture selected a leaf and
expected it to be expanded. SWT's existing `TreeItem.test_setExpandedZ` contract
correctly keeps a leaf collapsed. The generated correction adds
`child.basicAddChild()` before setting viewer input. It preserves every existing
assertion, both 200-sibling levels, and all SetData budgets. The model gains one
node, from 40,201 to 40,202. No SWT or JFace production class changed between the
sealed failing run and the passing run.

The corrected SDK output passed the exact required method with one test, zero
failures, errors or skips. The existing composition report verifier accepted the
genuine Surefire XML and exited zero; it rejected the genuine earlier failure.
The passing XML has SHA256
`bc984d3d195c38ef37619a0954b73ee35bea0b2585b587e25c734256a6a3e52a`.

The native proof uses source-built SWT and JFace at their pinned fork commits,
real GTK3 JNI libraries, real Xvfb, the generated M3 Commands bundle, and actual
JUnit execution. Fifteen Java test sources produce 31 test classes. Exact inputs,
commands, hashes, and unedited RED/GREEN receipts are under `proof/native/` and
`evidence/native/`. This is a bounded native gate, not a complete Tycho/PDE SDK run.

## Canonical shared text owner

The final shared text owner has SHA256
`58c94f4b87a6e4a53268808bf0c635fe7bd56e2864cb8fe46483c85efba771d0`
and Git blob `cc5f86ab579d2f4b1b9bc4c26b4592d2d8142244`.

It admits exact root `.gitmodules`, the exact JFace test manifest, and the two
Commands project metadata paths needed by the Java 21 donor. It refuses
case-insensitive Git-internal path segments and noncanonical target aliases
during scanning and again before application. On POSIX a literal backslash in a
filename cannot alias a path separator; native Windows separators remain valid.

The pinned JFace fixture contains valid Java 25 unnamed-lambda syntax that the
current Java 21 recipe parser refuses. The real failed typed-parser attempt is
preserved under `evidence/fixture-*`. This one bootstrap is admitted by an exact
crate, path, preimage hash and postimage hash tuple in the existing text owner.
A different crate is refused. The actual generated Java file is compiled with
Java 25 and used in the passing native test. This does not admit arbitrary Java
sources to the text recipe.

Ten actual SDK tests establish the final admission and refusal contracts. The
original and intermediate owner bytes under `proof/kernel/` remain historical
proof inputs. Their recorded failures and earlier passes retain their original
source pins and do not replace qualification of the final owner.

## Reproduce recipe generation

From this canonical crate with Java 21 selected:

```sh
mvn -B -ntp test -Dmaterialize.dir=/absolute/empty/output
```

The POM resolves the four canonical recipe source owners through its relative
default. The tests load the named declarative recipe and materialize only actual
OpenRewrite results. They verify all five exact postimages, fixed point, replay,
missing or drifted inputs, duplicate targets, occupied additions, unrelated
source, and the explicitly sealed Java 25 fixture bootstrap.
`SOURCE_PINS.json` binds the real Eclipse preimages, outputs, and shared owners.

The final canonical-path run records 15 tests: ten metadata-owner tests and five
receiver tests, with zero failures, errors or skips. See `evidence/final-sdk.json`,
the original Maven log, and the two original `final-TEST-*.xml` files. Earlier
13-test, parser-refusal, and backslash-alias RED runs are separate historical
stages and remain unedited.

## Acceptance boundary

The recipe, native method, composition verifier tests, actual 12-module source
composition, and full Tycho/PDE SDK build are separate gates. The first two pass
for the exact inputs recorded here. This crate does not claim that the complete
Eclipse framework has moved into M3 or that the full SDK has passed.
