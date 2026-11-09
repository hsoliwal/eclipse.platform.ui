# Release readiness for M3JDK21 (Java 21): eclipse.platform.ui

Measured on 2026-11-09 against fork master `0b550d6e3d94fc2dc1a01970d5a665bcdcac64f1` (upstream master `6c7a59310117`).
Tools and raw evidence: [`docs/m3/jdk21/release-20261109`](../jdk21/release-20261109/). Every gate ran locally on CPU; fork CI runs are not counted
(startup failures and queue starvation on this account, 2026-11).

| # | Gate | Verdict | Evidence |
| --- | --- | --- | --- |
| G1 | No Java 22+ requirement on master: BREE above JavaSE-21 (0), build/IDE pins above 21 (0), unnamed `_` (0), `import module` (0) | PASS | CENSUS.tsv (code view; hits inside strings, text blocks and comments: CENSUS_MASKED.tsv) |
| G2 | Java-21 compile gate: JDK 25 javac `--release 21` minus `--release 25` over 43 bundles (2883 files, 47 roots); 613 baseline rows cancel | PASS (residue 0) | GATE_RESIDUE.tsv, GATE_SUMMARY.tsv |
| G2b | Parse gate: every tracked Java source (7535 files, test bundles included, test-input folders excluded), JDK 25 javac `--release 21` stopped after parsing | PASS | PARSE_GATE.tsv |
| G3 | Recipe fixed point (`apply_jdk21.sh` + `apply_jdk21_pins.sh` change nothing on master) | PASS | RECIPE_CHANGES.tsv |
| G4 | Metadata consistent: Tycho bundle/feature versions match their pom, no EE above 21 in a Java-21 bundle | PASS | META.tsv |
| G5 | Full Tycho build of this repository on JDK 21 (`mvn -B clean verify -DskipTests`, fork parent + SDK target from the fork aggregator, other components from the upstream 4.42 I-build repository) | FAIL | BUILD FAILURE in 250 s, modules 45/46 ok, 1 failed, 23 [ERROR] rows (BUILD_G5.tsv, BUILD_G5_ERRORS.tsv, BUILD_G5.log.gz) |
| G6 | Eclipse SDK product build and launch on JDK 21 | PENDING | aggregator verdict, eclipse.platform.releng.aggregator docs/m3/release/RELEASE_READINESS.md |

## What this PR changes

Documentation only. Fork master `0b550d6e3d94` passes G1 to G4 as it is: the 2026-10-20 delta (43 bundles at JavaSE-21) is on
master by content, upstream syncs since then added no Java 22+ requirement, construct or API to these bundles.

## G5 (Tycho build on JDK 21.0.9)

NLS pre-check first (`nls_precheck.sh`, ECJ 3.47 over the 38 bundles that compile NLS problems as errors): 17 missing
`//$NON-NLS-n$` tags in fork-added M3 classes (M3SparseExtents 11, M3HeapGrowth 2, M3MarkerDeltas 2, M3MarkerLanes 2)
fixed before attempt 1 (`fix_nls_tags.sh`); attempt 1 had 0 NLS errors.

| Attempt | Result |
| --- | --- |
| 1 | BUILD FAILURE: 45/46 modules ok, reactor stopped at org.eclipse.ui.workbench: `M3PartVisibility` uses SWT fork API (`SWT.PartVisible`, `Control.isPartVisible()`) that the upstream 4.42 I-build SWT does not have (4 rows). Not a Java-level problem: platform.ui master needs the SWT fork |

## Re-run

```bash
git clone --depth 1 https://github.com/hsoliwal/eclipse.platform.ui.git && git -C eclipse.platform.ui remote add upstream <upstream>
git -C eclipse.platform.ui fetch --depth 1 --filter=blob:none upstream master:refs/remotes/upstream/master
bash docs/m3/jdk21/release-20261109/gate_master.sh eclipse.platform.ui    # paths at the top of the script; JAVAC = JDK 25 javac
bash docs/m3/jdk21/release-20261109/meta_check.sh eclipse.platform.ui > META.tsv
```

The gate compiles every bundle that upstream master declares JavaSE-22 or newer, plus the 2026-10-20 list, against the
newest jar of every bundle in a p2 pool. Errors in both runs are classpath noise (APIs newer than the pool); what remains
after `--release 21` is the Java 22+ residue and must be empty.
