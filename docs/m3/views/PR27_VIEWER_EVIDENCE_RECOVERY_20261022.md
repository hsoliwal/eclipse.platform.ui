# PR #27 viewer-evidence additive recovery

This change restores six previously absent documentation/qualification artifacts from [Eclipse Platform UI PR #27](https://github.com/hsoliwal/eclipse.platform.ui/pull/27), donor head `1d1fdc5c3e6b9dca47cf4eb9eeea726db878cabf`.

The active JFace/SWT implementations in `master` have overlapping edits and cannot safely be overwritten with the original PR head. This recovery therefore makes **no runtime code changes**. It preserves the original six new documentation blobs under their original paths and carries their test/coverage observations forward for a later reviewable runtime consolidation.

Exact donor blob IDs:
- ALL_VIEWERS_PROGRAMME_20261021.md: `aa5e29d87cf7e67bfaf920dc88c3021c9021a52c`
- JFACE_QUEUE_REPAIR_20261021.md: `b5e54465119dc46e25061fbb70101788441dd5bc`
- SHARED_VIEWER_REPAIR_20261021.md: `b8bcfff1e7bac0a52bdf493b7a587d0982917c77`
- QUALIFICATION.json: `95bdacbbb8582e282a3dbefed70f5f23da76601d`
- UPSTREAM_RECONCILIATION.json: `ded2abd8685016c58eda5e3a3a3a9369a341a751`
- VIEWER_COVERAGE.tsv: `96407537834fa770079823e32f06562426d828ac`

No Java, SWT, JFace, build, or JNI code is modified. The historical qualification is evidence only, **not a claim that current Eclipse tests passed**. Future runtime reconciliation must be source- and API-compatible and execute actual viewer tests. An ancestry-only merge would not prove this source integration.
