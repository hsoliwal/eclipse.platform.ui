# Supporting sources and non-Java boundaries

Source revision: `b8874e6b63390d65e47612574686cb8cf97b1fe8`.

All 14219 Git tree entries were enumerated with no truncated subtree response. The [project/module ledger](module-ledger.md) and [package ledger](package-ledger.md) account for Java and descriptor-backed units. This supplement keeps non-Java native sources/scripts, checked-in artifacts and schema/resource roles visible. It is structural accounting, not evidence that every script or native function has been semantically reviewed.

## Top-level ownership

| Tree area | Entries below it | Current accounting and remaining work |
| --- | --- | --- |
| [.github](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/.github) | 10 | Workflow definitions and templates; build/test routes were inspected where linked, workflow actions were not executed |
| [.m3](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/.m3) | 24 | Checked-in analysis/recipe/evidence material; source/build units remain included, historical receipts are not fresh validation |
| [.mvn](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/.mvn) | 2 | Maven/Tycho extension and invocation defaults; declarations read, dependency resolution not run |
| [bundles](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles) | 9277 | Production/runtime and tooling modules; manifest and package ownership in the linked ledgers |
| [docs](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/docs) | 163 | Existing contributor/API/design material and source-backed documentation; historical claims require revision-specific requalification |
| [examples](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/examples) | 1202 | Executable examples and snippets; each descriptor/source occurrence remains included, no example execution |
| [features](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/features) | 11 | Feature packaging descriptors; no p2/product installation or feature-resolution qualification |
| [releng](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/releng) | 7 | Workspace/release-engineering setup; inspect environment-dependent setup before use |
| [tests](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/tests) | 3503 | Test bundles, fixtures, archives, image resources and manual/performance material; presence is not a passing test |

## Supporting native/script sources

The following 2 files have native/source/script extensions. Each is included with a blob identity. The native bridge and CollectSources contracts identified in [lifecycle contracts](lifecycle-contracts.md) are selected inspected exceptions; all other script/function semantics, inputs, output effects and tests remain **missing** in this pass.

| Path | Role boundary | Blob SHA |
| --- | --- | --- |
| [.m3/atomize-patternize/bootstrap.sh](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/.m3/atomize-patternize/bootstrap.sh) | analysis/benchmark/tooling; not a fresh execution receipt | `b221d21a3ff9083f374beb9d306636e68ae2a34d` |
| [.m3/atomize-patternize/run.sh](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/.m3/atomize-patternize/run.sh) | analysis/benchmark/tooling; not a fresh execution receipt | `7a90e23bb1bfce9fd9737f5f885e3ffefae21c5b` |

## Model, extension and opaque artifact accounting

- 142 model/extension descriptors (.ecore, .genmodel, .e4xmi, .exsd, .xmi) were enumerated. They are contained in the owning project/tree above; schema fields, generated-source correspondence, extension-point contracts and lifecycle consumers remain to be reviewed
- 41 archive/binary/compiled artifacts (.jar, .zip, .tar, .gz, .dll, .so, .jnilib, .dylib, .pyc) were enumerated. Their payloads were not unpacked or executed; embedded packages or external provenance are unresolved and are not silently counted as reviewed source packages
- Other images, fonts, translations, HTML and configuration files remain owned by their project/tree. An asset directory is not automatically a language package. Feature, export-only and resource-only bundles remain in the module ledger even when they have no Java source occurrence
- These are included-but-unreviewed boundaries, not exclusions. Full repository completion requires resolving embedded/generated/vendor/tooling scope and all outstanding semantics

