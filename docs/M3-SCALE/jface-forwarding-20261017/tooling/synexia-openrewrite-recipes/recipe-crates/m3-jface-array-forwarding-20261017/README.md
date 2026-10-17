# JFace array operations forward to M3

This recipe creates an internal M3ViewerArrays owner and changes five existing Eclipse
JFace classes to forward 14 call sites to 13 replaceable array atoms. The current Eclipse
implementations are the licensed donors. Arrays remain authoritative; no per-element
wrappers, extra state registry, reflection dispatch or native callback trampoline is added.
Public descriptors and named Eclipse APIs remain unchanged. Lifecycle, exception logging,
hash-cache ownership and required virtual dispatch remain at their original boundaries.

This is a qualified first forwarding pass, not an implementation of the entire Eclipse
platform. The UI bundles inventory contains 5,105 Java files in 53 bundles; five facades
are changed here. COVERAGE.tsv records the remaining boundaries explicitly. The existing
MIndex viewer/session and primitive JNI owners remain available for their admitted contracts.

## Recipe and reproduction

Canonical Maven crate: synexia-openrewrite-recipes/recipe-crates/m3-jface-array-forwarding-20261017.
Named recipe: com.synexia.eclipse.JFaceArrayForwarding20261017. It composes the unchanged
M3HashPinnedJavaSnapshotRecipe. Source-pin re-admission confirmed the engine and policies
unchanged when Synexia develop advanced during qualification. No root admission policy is bypassed.

Run from a Synexia checkout or the receiver's pinned tooling mirror. Supply Java 21,
Maven 3.9.9+, the prepared offline Maven cache, Eclipse dependency directory, and the
P2 plugins directory listed in evidence/runtime-dependencies.json:

```sh
python proof/reproduce.py all \
  --work /absolute/separate/proof-work \
  --jdk /absolute/jdk-21 \
  --maven /absolute/apache-maven/bin/mvn \
  --m2 /absolute/maven-cache \
  --deps /absolute/eclipse-deps \
  --p2 /absolute/p2/plugins
```

The driver creates pinned baseline inputs, runs lint and compilation, executes the real
OpenRewrite SDK tests, writes only actual recipe results, checks generated code, compares
contracts/API/bytecode, then runs real Equinox and cost probes. Production outputs appear
under proof-work/generated. Never edit those outputs: change the recipe payload and rerun.
Missing dependencies fail explicitly; the driver does not download arbitrary replacements.
The source crate uses the four canonical engines in ../../src/main/java; the receiver keeps
an exact hash-pinned Apache-2.0 tooling copy. No runtime Synexia service dependency is added.

## Executed evidence

- Five Maven/OpenRewrite tests: generation, fixed point, missing/drift refusal,
  duplicate/occupied refusal, reversed input order, unrelated source and partial replay.
- 445,093 independent client assertions per baseline and candidate, with identical
  callback trace SHA-256 1b35c943d32e48dbc2a6548d3e6f835895d86b0f3b34f610d6ae9e82cc2fd5b5.
- Public javap descriptors identical. Bytecode contains 14 direct calls to the 13 atoms.
- Real Equinox 3.24.300 resolves the original JFace 3.42 manifest, executes the same client
  through a separate consuming bundle, and confirms the new package is not exported.
- Filtering allocations match exactly for dense, 1/16 and 1/1024 selection in two forks.
  CPU measurements vary between local runs, including slower candidate samples. No
  statistically supported speedup or universal performance claim is made.
- A separate clean-work portable reproduction completed the same ordered gates.

The live runtime uses unmodified supporting classes/resources from pinned JFace 3.40,
overlaid with the exact five current baseline/generated classes, current manifest, and
new kernel. This proves scoped OSGi linkage and behavior, not a full source-built Eclipse
release. Full Tycho reactor, PDE API tools, display/rendering and other OS/platform gates
remain pending. Keep PRs draft until those gates and broader integration review complete.

## Qualification corrections retained

Initial checks correctly stopped on a Synexia no-tabs style rule inappropriate for Eclipse,
transient synchronization files in wildcard test discovery, and signed/unsigned mixed-package
test loading. The receiver uses Eclipse tab conventions, exact suite discovery and a local
unsigned qualification overlay. The independent baseline test also exposed its own wrong
empty-selection hash expectation (Arrays.hashCode(empty)=1, Eclipse specifies 31 in code);
the baseline-grounded oracle was corrected before testing the candidate. Portable lint
dependency discovery was narrowed to pinned Checkstyle 9.3 dependencies after an ANTLR
version collision. These are qualification corrections; generated outputs were never patched.
