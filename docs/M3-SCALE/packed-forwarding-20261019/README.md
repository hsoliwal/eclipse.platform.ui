# Packed JFace M3 forwarding

CustomHashtable forwards to the internal M3ViewerMap parallel-array owner. TreeSelection,
StructuredViewer, AbstractTreeViewer and TableViewer retain their existing callers and APIs.
M3ViewerArrays filtering gains compact sparse output and one dense-prefix decision.

Canonical source, recipes, failure history and custody: https://github.com/hsoliwal/com.synexia/pull/12445
The exact source commit is in SYNEXIA_HANDOFF.json. The tooling directory is a pinned authoring
copy for offline target qualification, not a new canonical recipe or runtime dependency.
Synexia-wide custody fixtures/ledgers are deliberately retained only at their canonical owner.

See the mirrored crate README.md for reproduction, algorithm choices, measurements and limits.
The existing first-pass recipe remains unchanged. The three production Java files are actual
SDK outputs. Their manifest contains exact preimage/postimage SHA-256; replay is a no-op and
source drift refuses. No new package is exported; original Eclipse and SWT/JNI owners remain.

Passed: 5 target recipe tests, 1,520,395 assertions per Java variant, identical public/protected
API, real GTK normal/virtual tables and trees (538 checks / 372,235 callbacks per variant).
At 10,000 entries: 31.4% less measured structural heap, 2 structural objects instead of 10,001,
zero allocation for steady remove/reinsert. Sparse filter allocation falls sharply; dense input
allocates 80 additional bytes, and CPU results are mixed. Do not claim a universal speedup.

Draft: full Tycho/PDE/platform and current OSGi-manifest qualification remains pending.
Supporting runtime binaries are JFace 3.37 / SWT 3.130 with the pinned current selected sources.
This is one integrated JFace pass; the rest of Eclipse and M3JDK21 integration remain open.
