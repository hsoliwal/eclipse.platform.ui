# Module Ledger detail 1

[Return to the complete ledger index](module-ledger.md)

## Reading the boundaries

- OSGi bundles/fragments are the module units counted here; automatic module names are recorded separately
- Maven/pomless aggregators, Eclipse-only projects and feature projects are project units even when they have no runtime module
- A fixture bundle under tests/data is not a deployable production component merely because it has a manifest
- Physical containment establishes the ledger owner, not proof of active compilation; build selection and profiles still apply
- Source roots are copied from source.. in build.properties when present; SWT custom/platform source selection is documented in [lifecycle contracts](lifecycle-contracts.md)

## repository-root

- Identity: `eclipse.platform.ui`; production/build; eclipse.platform.ui
- Discovery: [pom.xml](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/pom.xml)
- Build identity: `eclipse.platform.ui`; packaging `pom`; declared module references (including profile declarations): `features`, `bundles`, `tests`, `examples`
- Source roots: no source.. value read here; follow the descriptor/custom build route; 0 Java source-directory occurrences
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## .m3/openrewrite-recipes

- Identity: `m3-recipe-first-bootstrap`; tooling; m3-recipe-first-bootstrap
- Discovery: [openrewrite-recipes/pom.xml](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/.m3/openrewrite-recipes/pom.xml)
- Build identity: `m3-recipe-first-bootstrap`; packaging `(default jar)`
- Source roots: no source.. value read here; follow the descriptor/custom build route; 2 Java source-directory occurrences in [the package ledger](package-ledger.md#m3openrewrite-recipes)
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## bundles

- Identity: `ui-bundles`; pomless-build; ui-bundles
- Discovery: [bundles/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/build.properties)
- Build identity: `ui-bundles`
- Source roots: no source.. value read here; follow the descriptor/custom build route; 0 Java source-directory occurrences
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## bundles/org.eclipse.core.commands

- Identity: `org.eclipse.core.commands`; production/build; Commands
- Discovery: [org.eclipse.core.commands/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.core.commands/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.core.commands/META-INF/MANIFEST.MF); [org.eclipse.core.commands/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.core.commands/build.properties)
- Module: version `3.13.100.qualifier`; execution environment `JavaSE-17`; automatic module name `org.eclipse.core.commands`
- Dependency direction: requires `org.eclipse.equinox.common`; 0 imported package clauses
- Surface: 7 exported package clauses; inspect manifest for visibility, version, split-package and friend directives
- Source roots: `src/`; 7 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipsecorecommands)
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## bundles/org.eclipse.core.databinding

- Identity: `org.eclipse.core.databinding`; production/build; JFace Data Binding
- Discovery: [org.eclipse.core.databinding/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.core.databinding/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.core.databinding/META-INF/MANIFEST.MF); [org.eclipse.core.databinding/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.core.databinding/build.properties)
- Module: version `1.13.900.qualifier`; execution environment `JavaSE-17`; automatic module name `org.eclipse.core.databinding`
- Dependency direction: requires `org.eclipse.equinox.common`, [org.eclipse.core.databinding.observable](module-ledger.md#bundlesorgeclipsecoredatabindingobservable), [org.eclipse.core.databinding.property](module-ledger.md#bundlesorgeclipsecoredatabindingproperty); 3 imported package clauses
- Surface: 9 exported package clauses; inspect manifest for visibility, version, split-package and friend directives; activator `org.eclipse.core.internal.databinding.Activator`; activation `lazy`
- Source roots: `src/`; 10 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipsecoredatabinding)
- Coverage: partial. Selected APIs are explained in [lifecycle contracts](lifecycle-contracts.md); other exports remain unreviewed.

## bundles/org.eclipse.core.databinding.beans

- Identity: `org.eclipse.core.databinding.beans`; production/build; JFace Data Binding for JavaBeans
- Discovery: [org.eclipse.core.databinding.beans/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.core.databinding.beans/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.core.databinding.beans/META-INF/MANIFEST.MF); [org.eclipse.core.databinding.beans/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.core.databinding.beans/build.properties)
- Module: version `1.10.600.qualifier`; execution environment `JavaSE-17`; automatic module name `org.eclipse.core.databinding.beans`
- Dependency direction: requires `org.eclipse.equinox.common`, [org.eclipse.core.databinding.observable](module-ledger.md#bundlesorgeclipsecoredatabindingobservable), [org.eclipse.core.databinding.property](module-ledger.md#bundlesorgeclipsecoredatabindingproperty); 0 imported package clauses
- Surface: 3 exported package clauses; inspect manifest for visibility, version, split-package and friend directives
- Source roots: `src/`; 3 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipsecoredatabindingbeans)
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## bundles/org.eclipse.core.databinding.observable

- Identity: `org.eclipse.core.databinding.observable`; production/build; JFace Data Binding Observables
- Discovery: [org.eclipse.core.databinding.observable/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.core.databinding.observable/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.core.databinding.observable/META-INF/MANIFEST.MF); [org.eclipse.core.databinding.observable/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.core.databinding.observable/build.properties)
- Module: version `1.13.600.qualifier`; execution environment `JavaSE-17`; automatic module name `org.eclipse.core.databinding.observable`
- Dependency direction: requires `org.eclipse.equinox.common`; 0 imported package clauses
- Surface: 12 exported package clauses; inspect manifest for visibility, version, split-package and friend directives
- Source roots: `src/`; 12 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipsecoredatabindingobservable)
- Coverage: partial. Selected APIs are explained in [lifecycle contracts](lifecycle-contracts.md); other exports remain unreviewed.

## bundles/org.eclipse.core.databinding.property

- Identity: `org.eclipse.core.databinding.property`; production/build; JFace Data Binding Properties
- Discovery: [org.eclipse.core.databinding.property/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.core.databinding.property/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.core.databinding.property/META-INF/MANIFEST.MF); [org.eclipse.core.databinding.property/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.core.databinding.property/build.properties)
- Module: version `1.10.600.qualifier`; execution environment `JavaSE-17`; automatic module name `org.eclipse.core.databinding.property`
- Dependency direction: requires `org.eclipse.equinox.common`, [org.eclipse.core.databinding.observable](module-ledger.md#bundlesorgeclipsecoredatabindingobservable); 0 imported package clauses
- Surface: 10 exported package clauses; inspect manifest for visibility, version, split-package and friend directives; activation `lazy`
- Source roots: `src/`; 10 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipsecoredatabindingproperty)
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## bundles/org.eclipse.core.filebuffers

- Identity: `org.eclipse.core.filebuffers`; production/build; File Buffers
- Discovery: [org.eclipse.core.filebuffers/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.core.filebuffers/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.core.filebuffers/META-INF/MANIFEST.MF); [org.eclipse.core.filebuffers/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.core.filebuffers/build.properties)
- Module: version `3.8.700.qualifier`; execution environment `JavaSE-17`; automatic module name `org.eclipse.core.filebuffers`
- Dependency direction: requires `org.eclipse.core.runtime`, `org.eclipse.core.resources`, [org.eclipse.text](module-ledger.md#bundlesorgeclipsetext), `org.eclipse.core.filesystem`; 0 imported package clauses
- Surface: 3 exported package clauses; inspect manifest for visibility, version, split-package and friend directives
- Source roots: `src/`; 3 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipsecorefilebuffers)
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## bundles/org.eclipse.e4.core.commands

- Identity: `org.eclipse.e4.core.commands`; production/build; Eclipse e4 core commands
- Discovery: [org.eclipse.e4.core.commands/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.core.commands/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.core.commands/META-INF/MANIFEST.MF); [org.eclipse.e4.core.commands/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.core.commands/build.properties)
- Module: version `1.1.900.qualifier`; execution environment `JavaSE-17`; automatic module name `org.eclipse.e4.core.commands`
- Dependency direction: requires `org.eclipse.equinox.common`, `org.eclipse.e4.core.di`; 7 imported package clauses
- Surface: 2 exported package clauses; inspect manifest for visibility, version, split-package and friend directives; activation `lazy`
- Source roots: `src/`; 2 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipsee4corecommands)
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## bundles/org.eclipse.e4.emf.xpath

- Identity: `org.eclipse.e4.emf.xpath`; production/build; %Bundle-Name
- Discovery: [org.eclipse.e4.emf.xpath/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.emf.xpath/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.emf.xpath/META-INF/MANIFEST.MF); [org.eclipse.e4.emf.xpath/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.emf.xpath/build.properties)
- Module: version `0.6.100.qualifier`; execution environment `JavaSE-17`; automatic module name `org.eclipse.e4.emf.xpath`
- Dependency direction: requires `org.eclipse.emf.ecore`, `org.eclipse.core.runtime`, `org.eclipse.emf.ecore.xmi`; 0 imported package clauses
- Surface: 1 exported package clauses; inspect manifest for visibility, version, split-package and friend directives
- Source roots: `src/`; 2 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipsee4emfxpath)
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## bundles/org.eclipse.e4.ui.bindings

- Identity: `org.eclipse.e4.ui.bindings`; production/build; Eclipse Bindings Support
- Discovery: [org.eclipse.e4.ui.bindings/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.bindings/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.bindings/META-INF/MANIFEST.MF); [org.eclipse.e4.ui.bindings/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.bindings/build.properties)
- Module: version `0.15.300.qualifier`; execution environment `JavaSE-21`; automatic module name `org.eclipse.e4.ui.bindings`
- Dependency direction: requires `org.eclipse.swt`, [org.eclipse.core.commands](module-ledger.md#bundlesorgeclipsecorecommands), `org.eclipse.e4.core.contexts`, `org.eclipse.e4.core.di`, [org.eclipse.e4.ui.services](module-ledger.md#bundlesorgeclipsee4uiservices), `org.eclipse.e4.core.services`, [org.eclipse.e4.ui.model.workbench](module-ledger.md#bundlesorgeclipsee4uimodelworkbench); 9 imported package clauses
- Surface: 3 exported package clauses; inspect manifest for visibility, version, split-package and friend directives; activation `lazy`
- Source roots: `src/`; 3 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipsee4uibindings)
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## bundles/org.eclipse.e4.ui.css.core

- Identity: `org.eclipse.e4.ui.css.core`; production/build; Eclipse CSS Core Support
- Discovery: [org.eclipse.e4.ui.css.core/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.css.core/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.css.core/META-INF/MANIFEST.MF); [org.eclipse.e4.ui.css.core/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.css.core/build.properties)
- Module: version `0.14.900.qualifier`; execution environment `JavaSE-17`; automatic module name `org.eclipse.e4.ui.css.core`
- Dependency direction: requires `org.eclipse.equinox.common`, `org.eclipse.core.runtime`; 0 imported package clauses
- Surface: 17 exported package clauses; inspect manifest for visibility, version, split-package and friend directives
- Source roots: `src/`; 17 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipsee4uicsscore)
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## bundles/org.eclipse.e4.ui.css.swt

- Identity: `org.eclipse.e4.ui.css.swt`; production/build; Eclipse CSS SWT Support
- Discovery: [org.eclipse.e4.ui.css.swt/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.css.swt/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.css.swt/META-INF/MANIFEST.MF); [org.eclipse.e4.ui.css.swt/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.css.swt/build.properties)
- Module: version `0.17.200.qualifier`; execution environment `JavaSE-21`; automatic module name `org.eclipse.e4.ui.css.swt`
- Dependency direction: requires [org.eclipse.e4.ui.css.core](module-ledger.md#bundlesorgeclipsee4uicsscore), `org.eclipse.swt`, `org.eclipse.core.runtime`; 1 imported package clauses
- Surface: 16 exported package clauses; inspect manifest for visibility, version, split-package and friend directives; activation `lazy`
- Source roots: `src/`; 16 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipsee4uicssswt)
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## bundles/org.eclipse.e4.ui.css.swt.theme

- Identity: `org.eclipse.e4.ui.css.swt.theme`; production/build; Eclipse CSS SWT Theme Support
- Discovery: [org.eclipse.e4.ui.css.swt.theme/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.css.swt.theme/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.css.swt.theme/META-INF/MANIFEST.MF); [org.eclipse.e4.ui.css.swt.theme/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.css.swt.theme/build.properties)
- Module: version `0.15.300.qualifier`; execution environment `JavaSE-21`; automatic module name `org.eclipse.e4.ui.css.swt.theme`
- Dependency direction: requires `org.eclipse.swt`, [org.eclipse.e4.ui.css.swt](module-ledger.md#bundlesorgeclipsee4uicssswt), [org.eclipse.e4.ui.css.core](module-ledger.md#bundlesorgeclipsee4uicsscore), `org.eclipse.core.runtime`; 3 imported package clauses
- Surface: 2 exported package clauses; inspect manifest for visibility, version, split-package and friend directives; activation `lazy`
- Source roots: `src/`; 2 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipsee4uicssswttheme)
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## bundles/org.eclipse.e4.ui.di

- Identity: `org.eclipse.e4.ui.di`; production/build; Eclipse UI Dependency Injection
- Discovery: [org.eclipse.e4.ui.di/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.di/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.di/META-INF/MANIFEST.MF); [org.eclipse.e4.ui.di/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.di/build.properties)
- Module: version `1.5.800.qualifier`; execution environment `JavaSE-17`; automatic module name `org.eclipse.e4.ui.di`
- Dependency direction: requires none declared in Require-Bundle; 6 imported package clauses
- Surface: 2 exported package clauses; inspect manifest for visibility, version, split-package and friend directives; activation `lazy`
- Source roots: `src/`; 2 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipsee4uidi)
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## bundles/org.eclipse.e4.ui.dialogs

- Identity: `org.eclipse.e4.ui.dialogs`; production/build; Eclipse e4 dialogs
- Discovery: [org.eclipse.e4.ui.dialogs/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.dialogs/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.dialogs/META-INF/MANIFEST.MF); [org.eclipse.e4.ui.dialogs/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.dialogs/build.properties)
- Module: version `1.7.300.qualifier`; execution environment `JavaSE-21`; automatic module name `org.eclipse.e4.ui.dialogs`
- Dependency direction: requires [org.eclipse.jface](module-ledger.md#bundlesorgeclipsejface), `org.eclipse.core.runtime`; 0 imported package clauses
- Surface: 2 exported package clauses; inspect manifest for visibility, version, split-package and friend directives
- Source roots: `src/`; 2 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipsee4uidialogs)
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## bundles/org.eclipse.e4.ui.ide

- Identity: `org.eclipse.e4.ui.ide`; production/build; %Bundle-Name
- Discovery: [org.eclipse.e4.ui.ide/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.ide/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.ide/META-INF/MANIFEST.MF); [org.eclipse.e4.ui.ide/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.ide/build.properties)
- Module: version `3.18.100.qualifier`; execution environment `JavaSE-21`; automatic module name `org.eclipse.e4.ui.ide`
- Dependency direction: requires `org.eclipse.core.runtime`, `org.eclipse.core.resources`, `org.eclipse.swt`; 0 imported package clauses
- Surface: 1 exported package clauses; inspect manifest for visibility, version, split-package and friend directives
- Source roots: `src/`; 1 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipsee4uiide)
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## bundles/org.eclipse.e4.ui.model.workbench

- Identity: `org.eclipse.e4.ui.model.workbench`; production/build; Eclipse Workbench Model
- Discovery: [org.eclipse.e4.ui.model.workbench/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.model.workbench/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.model.workbench/META-INF/MANIFEST.MF); [org.eclipse.e4.ui.model.workbench/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.model.workbench/build.properties)
- Module: version `2.4.1000.qualifier`; execution environment `JavaSE-17`; automatic module name `org.eclipse.e4.ui.model.workbench`
- Dependency direction: requires `org.eclipse.core.runtime`, `org.eclipse.e4.core.services`, `org.eclipse.e4.core.contexts`, [org.eclipse.core.commands](module-ledger.md#bundlesorgeclipsecorecommands), `org.eclipse.e4.core.di`, `org.eclipse.emf.ecore`, [org.eclipse.e4.emf.xpath](module-ledger.md#bundlesorgeclipsee4emfxpath); 0 imported package clauses
- Surface: 26 exported package clauses; inspect manifest for visibility, version, split-package and friend directives; activation `lazy`
- Source roots: `src/`; 26 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipsee4uimodelworkbench)
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## bundles/org.eclipse.e4.ui.progress

- Identity: `org.eclipse.e4.ui.progress`; production/build; Eclipse e4 Progress View
- Discovery: [org.eclipse.e4.ui.progress/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.progress/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.progress/META-INF/MANIFEST.MF); [org.eclipse.e4.ui.progress/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.progress/build.properties)
- Module: version `0.5.200.qualifier`; execution environment `JavaSE-21`; automatic module name `org.eclipse.e4.ui.progress`
- Dependency direction: requires `org.eclipse.e4.core.di`, [org.eclipse.e4.ui.di](module-ledger.md#bundlesorgeclipsee4uidi), [org.eclipse.jface](module-ledger.md#bundlesorgeclipsejface), [org.eclipse.e4.ui.workbench](module-ledger.md#bundlesorgeclipsee4uiworkbench), [org.eclipse.e4.core.commands](module-ledger.md#bundlesorgeclipsee4corecommands), `org.eclipse.e4.core.services`, `org.eclipse.e4.core.contexts`, [org.eclipse.e4.ui.model.workbench](module-ledger.md#bundlesorgeclipsee4uimodelworkbench), [org.eclipse.e4.ui.workbench.swt](module-ledger.md#bundlesorgeclipsee4uiworkbenchswt), `org.eclipse.core.runtime`, [org.eclipse.e4.ui.services](module-ledger.md#bundlesorgeclipsee4uiservices); 2 imported package clauses
- Surface: 1 exported package clauses; inspect manifest for visibility, version, split-package and friend directives; activation `lazy`
- Source roots: `src/`; 3 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipsee4uiprogress)
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## bundles/org.eclipse.e4.ui.services

- Identity: `org.eclipse.e4.ui.services`; production/build; Eclipse UI Application Services
- Discovery: [org.eclipse.e4.ui.services/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.services/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.services/META-INF/MANIFEST.MF); [org.eclipse.e4.ui.services/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.services/build.properties)
- Module: version `1.6.800.qualifier`; execution environment `JavaSE-17`; automatic module name `org.eclipse.e4.ui.services`
- Dependency direction: requires [org.eclipse.core.commands](module-ledger.md#bundlesorgeclipsecorecommands), `org.eclipse.e4.core.services`, `org.eclipse.e4.core.contexts`, `org.eclipse.e4.core.di`, [org.eclipse.e4.ui.di](module-ledger.md#bundlesorgeclipsee4uidi); 4 imported package clauses
- Surface: 5 exported package clauses; inspect manifest for visibility, version, split-package and friend directives; activation `lazy`
- Source roots: `src/`; 5 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipsee4uiservices)
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## bundles/org.eclipse.e4.ui.widgets

- Identity: `org.eclipse.e4.ui.widgets`; production/build; Eclipse UI Custom widgets
- Discovery: [org.eclipse.e4.ui.widgets/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.widgets/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.widgets/META-INF/MANIFEST.MF); [org.eclipse.e4.ui.widgets/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.widgets/build.properties)
- Module: version `1.5.100.qualifier`; execution environment `JavaSE-21`; automatic module name `org.eclipse.e4.ui.widgets`
- Dependency direction: requires `org.eclipse.swt`; 0 imported package clauses
- Surface: 1 exported package clauses; inspect manifest for visibility, version, split-package and friend directives
- Source roots: `src/`; 1 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipsee4uiwidgets)
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## bundles/org.eclipse.e4.ui.workbench

- Identity: `org.eclipse.e4.ui.workbench`; production/build; Eclipse e4 Workbench
- Discovery: [org.eclipse.e4.ui.workbench/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.workbench/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.workbench/META-INF/MANIFEST.MF); [org.eclipse.e4.ui.workbench/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.workbench/build.properties)
- Module: version `1.18.400.qualifier`; execution environment `JavaSE-17`; automatic module name `org.eclipse.e4.ui.workbench`
- Dependency direction: requires [org.eclipse.e4.ui.model.workbench](module-ledger.md#bundlesorgeclipsee4uimodelworkbench), [org.eclipse.e4.ui.services](module-ledger.md#bundlesorgeclipsee4uiservices), `org.eclipse.e4.core.services`, `org.eclipse.e4.core.contexts`, `org.eclipse.e4.core.di`, [org.eclipse.core.commands](module-ledger.md#bundlesorgeclipsecorecommands), [org.eclipse.e4.core.commands](module-ledger.md#bundlesorgeclipsee4corecommands), `org.eclipse.emf.ecore.change`, `org.eclipse.core.expressions`, [org.eclipse.e4.ui.di](module-ledger.md#bundlesorgeclipsee4uidi), `org.eclipse.emf.ecore.xmi`, `org.eclipse.e4.core.di.extensions`, `org.eclipse.core.runtime`; 4 imported package clauses
- Surface: 5 exported package clauses; inspect manifest for visibility, version, split-package and friend directives; activator `org.eclipse.e4.ui.internal.workbench.Activator`; activation `lazy`
- Source roots: `src/`; 6 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipsee4uiworkbench)
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## bundles/org.eclipse.e4.ui.workbench.addons.swt

- Identity: `org.eclipse.e4.ui.workbench.addons.swt`; production/build; Eclipse e4 Workbench Add-ons
- Discovery: [org.eclipse.e4.ui.workbench.addons.swt/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.workbench.addons.swt/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.workbench.addons.swt/META-INF/MANIFEST.MF); [org.eclipse.e4.ui.workbench.addons.swt/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.workbench.addons.swt/build.properties)
- Module: version `1.6.200.qualifier`; execution environment `JavaSE-21`; automatic module name `org.eclipse.e4.ui.workbench.addons.swt`
- Dependency direction: requires [org.eclipse.e4.ui.model.workbench](module-ledger.md#bundlesorgeclipsee4uimodelworkbench), `org.eclipse.e4.core.contexts`, `org.eclipse.e4.core.di`, [org.eclipse.e4.ui.workbench](module-ledger.md#bundlesorgeclipsee4uiworkbench), `org.eclipse.e4.core.services`, [org.eclipse.e4.ui.workbench.renderers.swt](module-ledger.md#bundlesorgeclipsee4uiworkbenchrenderersswt), [org.eclipse.e4.ui.widgets](module-ledger.md#bundlesorgeclipsee4uiwidgets), `org.eclipse.swt`, [org.eclipse.jface](module-ledger.md#bundlesorgeclipsejface), `org.eclipse.core.runtime`, [org.eclipse.e4.ui.di](module-ledger.md#bundlesorgeclipsee4uidi), [org.eclipse.e4.ui.services](module-ledger.md#bundlesorgeclipsee4uiservices), `org.eclipse.emf.ecore.xmi`; 5 imported package clauses
- Surface: 5 exported package clauses; inspect manifest for visibility, version, split-package and friend directives; activation `lazy`
- Source roots: `src/`; 5 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipsee4uiworkbenchaddonsswt)
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## bundles/org.eclipse.e4.ui.workbench.renderers.swt

- Identity: `org.eclipse.e4.ui.workbench.renderers.swt`; production/build; Eclipse e4 Workbench SWT Renderer
- Discovery: [org.eclipse.e4.ui.workbench.renderers.swt/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.workbench.renderers.swt/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.workbench.renderers.swt/META-INF/MANIFEST.MF); [org.eclipse.e4.ui.workbench.renderers.swt/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.workbench.renderers.swt/build.properties)
- Module: version `0.18.200.qualifier`; execution environment `JavaSE-21`; automatic module name `org.eclipse.e4.ui.workbench.renderers.swt`
- Dependency direction: requires [org.eclipse.e4.ui.workbench](module-ledger.md#bundlesorgeclipsee4uiworkbench), `org.eclipse.e4.core.services`, `org.eclipse.e4.core.contexts`, `org.eclipse.e4.core.di`, [org.eclipse.e4.ui.services](module-ledger.md#bundlesorgeclipsee4uiservices), `org.eclipse.swt`, [org.eclipse.jface](module-ledger.md#bundlesorgeclipsejface), [org.eclipse.e4.ui.workbench.swt](module-ledger.md#bundlesorgeclipsee4uiworkbenchswt), [org.eclipse.e4.ui.model.workbench](module-ledger.md#bundlesorgeclipsee4uimodelworkbench), [org.eclipse.e4.core.commands](module-ledger.md#bundlesorgeclipsee4corecommands), [org.eclipse.e4.ui.bindings](module-ledger.md#bundlesorgeclipsee4uibindings), `org.eclipse.core.expressions`, [org.eclipse.e4.ui.css.core](module-ledger.md#bundlesorgeclipsee4uicsscore), [org.eclipse.e4.ui.di](module-ledger.md#bundlesorgeclipsee4uidi), `org.eclipse.emf.ecore`, [org.eclipse.e4.ui.css.swt](module-ledger.md#bundlesorgeclipsee4uicssswt), `org.eclipse.e4.core.di.extensions`, `org.eclipse.core.runtime`; 3 imported package clauses
- Surface: 2 exported package clauses; inspect manifest for visibility, version, split-package and friend directives; activation `lazy`
- Source roots: `src/`; 2 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipsee4uiworkbenchrenderersswt)
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## bundles/org.eclipse.e4.ui.workbench.renderers.swt.cocoa

- Identity: `org.eclipse.e4.ui.workbench.renderers.swt.cocoa`; production/build; Eclipse UI MacOS X Enhancements
- Discovery: [org.eclipse.e4.ui.workbench.renderers.swt.cocoa/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.workbench.renderers.swt.cocoa/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.workbench.renderers.swt.cocoa/META-INF/MANIFEST.MF); [org.eclipse.e4.ui.workbench.renderers.swt.cocoa/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.workbench.renderers.swt.cocoa/build.properties)
- Module: version `0.14.800.qualifier`; execution environment `JavaSE-17`; automatic module name `org.eclipse.e4.ui.workbench.renderers.swt.cocoa`
- Dependency direction: requires none declared in Require-Bundle; 0 imported package clauses; fragment host `org.eclipse.e4.ui.workbench.renderers.swt;bundle-version="[0.10.0,1.0.0)"`
- Surface: 1 exported package clauses; inspect manifest for visibility, version, split-package and friend directives
- Platform selection: `(osgi.ws=cocoa)`
- Source roots: `src/`; 1 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipsee4uiworkbenchrenderersswtcocoa)
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## bundles/org.eclipse.e4.ui.workbench.swt

- Identity: `org.eclipse.e4.ui.workbench.swt`; production/build; Eclipse e4 Workbench SWT
- Discovery: [org.eclipse.e4.ui.workbench.swt/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.workbench.swt/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.workbench.swt/META-INF/MANIFEST.MF); [org.eclipse.e4.ui.workbench.swt/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.workbench.swt/build.properties)
- Module: version `0.18.300.qualifier`; execution environment `JavaSE-21`; automatic module name `org.eclipse.e4.ui.workbench.swt`
- Dependency direction: requires [org.eclipse.e4.ui.workbench](module-ledger.md#bundlesorgeclipsee4uiworkbench), `org.eclipse.e4.core.services`, [org.eclipse.e4.ui.services](module-ledger.md#bundlesorgeclipsee4uiservices), [org.eclipse.jface](module-ledger.md#bundlesorgeclipsejface), [org.eclipse.e4.ui.dialogs](module-ledger.md#bundlesorgeclipsee4uidialogs), [org.eclipse.core.databinding](module-ledger.md#bundlesorgeclipsecoredatabinding), [org.eclipse.jface.databinding](module-ledger.md#bundlesorgeclipsejfacedatabinding), [org.eclipse.e4.ui.css.core](module-ledger.md#bundlesorgeclipsee4uicsscore), [org.eclipse.e4.ui.css.swt](module-ledger.md#bundlesorgeclipsee4uicssswt), [org.eclipse.e4.ui.bindings](module-ledger.md#bundlesorgeclipsee4uibindings), `org.eclipse.e4.core.contexts`, [org.eclipse.e4.ui.workbench3](module-ledger.md#bundlesorgeclipsee4uiworkbench3), `org.eclipse.e4.core.di`, [org.eclipse.e4.ui.css.swt.theme](module-ledger.md#bundlesorgeclipsee4uicssswttheme), `org.eclipse.core.expressions`, `org.eclipse.core.runtime`, [org.eclipse.e4.core.commands](module-ledger.md#bundlesorgeclipsee4corecommands), [org.eclipse.e4.ui.widgets](module-ledger.md#bundlesorgeclipsee4uiwidgets), [org.eclipse.e4.ui.di](module-ledger.md#bundlesorgeclipsee4uidi), `org.eclipse.emf.ecore.xmi`, [org.eclipse.e4.ui.model.workbench](module-ledger.md#bundlesorgeclipsee4uimodelworkbench), `org.eclipse.e4.core.di.extensions`, [org.eclipse.urischeme](module-ledger.md#bundlesorgeclipseurischeme); 7 imported package clauses
- Surface: 6 exported package clauses; inspect manifest for visibility, version, split-package and friend directives; activator `org.eclipse.e4.ui.internal.workbench.swt.WorkbenchSWTActivator`; activation `lazy`
- Source roots: `src/`; 6 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipsee4uiworkbenchswt)
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## bundles/org.eclipse.e4.ui.workbench3

- Identity: `org.eclipse.e4.ui.workbench3`; production/build; Bundle for Workbench APIs available in e4
- Discovery: [org.eclipse.e4.ui.workbench3/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.workbench3/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.workbench3/META-INF/MANIFEST.MF); [org.eclipse.e4.ui.workbench3/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.e4.ui.workbench3/build.properties)
- Module: version `0.18.100.qualifier`; execution environment `JavaSE-21`; automatic module name `org.eclipse.e4.ui.workbench3`
- Dependency direction: requires `org.eclipse.equinox.common`, `org.eclipse.swt`, `org.eclipse.equinox.registry`, `org.eclipse.core.runtime`; 0 imported package clauses
- Surface: 2 exported package clauses; inspect manifest for visibility, version, split-package and friend directives
- Source roots: `src/`; 2 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipsee4uiworkbench3)
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## bundles/org.eclipse.jface

- Identity: `org.eclipse.jface`; production/build; JFace
- Discovery: [org.eclipse.jface/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.jface/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.jface/META-INF/MANIFEST.MF); [org.eclipse.jface/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.jface/build.properties)
- Module: version `3.42.0.qualifier`; execution environment `JavaSE-21`; automatic module name `org.eclipse.jface`
- Dependency direction: requires `org.eclipse.swt`, [org.eclipse.core.commands](module-ledger.md#bundlesorgeclipsecorecommands), `org.eclipse.equinox.common`, `org.eclipse.equinox.bidi`; 7 imported package clauses
- Surface: 29 exported package clauses; inspect manifest for visibility, version, split-package and friend directives
- Source roots: `src/`; 22 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipsejface)
- Coverage: partial. Selected APIs are explained in [lifecycle contracts](lifecycle-contracts.md); other exports remain unreviewed.

## bundles/org.eclipse.jface.databinding

- Identity: `org.eclipse.jface.databinding`; production/build; JFace Data Binding for SWT and JFace
- Discovery: [org.eclipse.jface.databinding/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.jface.databinding/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.jface.databinding/META-INF/MANIFEST.MF); [org.eclipse.jface.databinding/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.jface.databinding/build.properties)
- Module: version `1.16.100.qualifier`; execution environment `JavaSE-21`; automatic module name `org.eclipse.jface.databinding`
- Dependency direction: requires `org.eclipse.swt`, [org.eclipse.jface](module-ledger.md#bundlesorgeclipsejface), `org.eclipse.equinox.common`, [org.eclipse.core.databinding.observable](module-ledger.md#bundlesorgeclipsecoredatabindingobservable), [org.eclipse.core.databinding.property](module-ledger.md#bundlesorgeclipsecoredatabindingproperty), [org.eclipse.core.databinding](module-ledger.md#bundlesorgeclipsecoredatabinding); 0 imported package clauses
- Surface: 14 exported package clauses; inspect manifest for visibility, version, split-package and friend directives
- Source roots: `src/`; 14 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipsejfacedatabinding)
- Coverage: partial. Selected APIs are explained in [lifecycle contracts](lifecycle-contracts.md); other exports remain unreviewed.

## bundles/org.eclipse.jface.notifications

- Identity: `org.eclipse.jface.notifications`; production/build; Notification API
- Discovery: [org.eclipse.jface.notifications/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.jface.notifications/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.jface.notifications/META-INF/MANIFEST.MF); [org.eclipse.jface.notifications/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.jface.notifications/build.properties)
- Module: version `0.8.200.qualifier`; execution environment `JavaSE-21`; automatic module name `org.eclipse.jface.notifications`
- Dependency direction: requires `org.eclipse.core.runtime`, [org.eclipse.jface](module-ledger.md#bundlesorgeclipsejface); 0 imported package clauses
- Surface: 2 exported package clauses; inspect manifest for visibility, version, split-package and friend directives
- Source roots: `src/`; 2 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipsejfacenotifications)
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## bundles/org.eclipse.jface.text

- Identity: `org.eclipse.jface.text`; production/build; JFace Text
- Discovery: [org.eclipse.jface.text/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.jface.text/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.jface.text/META-INF/MANIFEST.MF); [org.eclipse.jface.text/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.jface.text/build.properties)
- Module: version `3.32.0.qualifier`; execution environment `JavaSE-21`; automatic module name `org.eclipse.jface.text`
- Dependency direction: requires `org.eclipse.core.runtime`, [org.eclipse.text](module-ledger.md#bundlesorgeclipsetext), `org.eclipse.swt`, [org.eclipse.jface](module-ledger.md#bundlesorgeclipsejface); 0 imported package clauses
- Surface: 27 exported package clauses; inspect manifest for visibility, version, split-package and friend directives; activator `org.eclipse.jface.text.Activator`; activation `lazy`
- Source roots: `src/,projection/`; 25 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipsejfacetext)
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## bundles/org.eclipse.ltk.core.refactoring

- Identity: `org.eclipse.ltk.core.refactoring`; production/build; Refactoring Core
- Discovery: [org.eclipse.ltk.core.refactoring/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.ltk.core.refactoring/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.ltk.core.refactoring/META-INF/MANIFEST.MF); [org.eclipse.ltk.core.refactoring/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.ltk.core.refactoring/build.properties)
- Module: version `3.16.100.qualifier`; execution environment `JavaSE-17`; automatic module name `org.eclipse.ltk.core.refactoring`
- Dependency direction: requires `org.eclipse.core.runtime`, `org.eclipse.core.expressions`, `org.eclipse.core.filesystem`, [org.eclipse.core.filebuffers](module-ledger.md#bundlesorgeclipsecorefilebuffers), `org.eclipse.core.resources`, [org.eclipse.core.commands](module-ledger.md#bundlesorgeclipsecorecommands), [org.eclipse.text](module-ledger.md#bundlesorgeclipsetext); 0 imported package clauses
- Surface: 9 exported package clauses; inspect manifest for visibility, version, split-package and friend directives; activator `org.eclipse.ltk.internal.core.refactoring.RefactoringCorePlugin`; activation `lazy`
- Source roots: `src/`; 9 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipseltkcorerefactoring)
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## bundles/org.eclipse.ltk.ui.refactoring

- Identity: `org.eclipse.ltk.ui.refactoring`; production/build; Refactoring UI
- Discovery: [org.eclipse.ltk.ui.refactoring/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.ltk.ui.refactoring/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.ltk.ui.refactoring/META-INF/MANIFEST.MF); [org.eclipse.ltk.ui.refactoring/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.ltk.ui.refactoring/build.properties)
- Module: version `3.14.300.qualifier`; execution environment `JavaSE-21`; automatic module name `org.eclipse.ltk.ui.refactoring`
- Dependency direction: requires `org.eclipse.core.runtime`, `org.eclipse.core.expressions`, [org.eclipse.core.filebuffers](module-ledger.md#bundlesorgeclipsecorefilebuffers), `org.eclipse.core.filesystem`, `org.eclipse.core.resources`, [org.eclipse.ltk.core.refactoring](module-ledger.md#bundlesorgeclipseltkcorerefactoring), [org.eclipse.jface.text](module-ledger.md#bundlesorgeclipsejfacetext), [org.eclipse.ui](module-ledger.md#bundlesorgeclipseui), [org.eclipse.ui.navigator](module-ledger.md#bundlesorgeclipseuinavigator), `org.eclipse.compare`, `org.eclipse.team.core`, `org.eclipse.team.ui`; 0 imported package clauses
- Surface: 11 exported package clauses; inspect manifest for visibility, version, split-package and friend directives; activator `org.eclipse.ltk.internal.ui.refactoring.RefactoringUIPlugin`; activation `lazy`
- Source roots: `src/`; 11 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipseltkuirefactoring)
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## bundles/org.eclipse.search

- Identity: `org.eclipse.search`; production/build; Search Support
- Discovery: [org.eclipse.search/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.search/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.search/META-INF/MANIFEST.MF); [org.eclipse.search/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.search/build.properties)
- Module: version `3.19.100.qualifier`; execution environment `JavaSE-21`; automatic module name `org.eclipse.search`
- Dependency direction: requires `org.eclipse.core.runtime`, `org.eclipse.core.resources`, `org.eclipse.core.filesystem`, [org.eclipse.core.filebuffers](module-ledger.md#bundlesorgeclipsecorefilebuffers), [org.eclipse.ui](module-ledger.md#bundlesorgeclipseui), [org.eclipse.ui.ide](module-ledger.md#bundlesorgeclipseuiide), [org.eclipse.ui.workbench.texteditor](module-ledger.md#bundlesorgeclipseuiworkbenchtexteditor), [org.eclipse.jface.text](module-ledger.md#bundlesorgeclipsejfacetext), [org.eclipse.ui.forms](module-ledger.md#bundlesorgeclipseuiforms), [org.eclipse.ltk.core.refactoring](module-ledger.md#bundlesorgeclipseltkcorerefactoring), [org.eclipse.ltk.ui.refactoring](module-ledger.md#bundlesorgeclipseltkuirefactoring), [org.eclipse.search.core](module-ledger.md#bundlesorgeclipsesearchcore); 0 imported package clauses
- Surface: 10 exported package clauses; inspect manifest for visibility, version, split-package and friend directives; activator `org.eclipse.search.internal.ui.SearchPlugin`; activation `lazy`
- Source roots: `search/,newsearch/`; 10 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipsesearch)
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## bundles/org.eclipse.search.core

- Identity: `org.eclipse.search.core`; production/build; Search Support Core
- Discovery: [org.eclipse.search.core/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.search.core/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.search.core/META-INF/MANIFEST.MF); [org.eclipse.search.core/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.search.core/build.properties)
- Module: version `3.16.700.qualifier`; execution environment `JavaSE-17`; automatic module name `org.eclipse.search.core`
- Dependency direction: requires `org.eclipse.core.runtime`, `org.eclipse.core.resources`, `org.eclipse.core.filesystem`, [org.eclipse.core.filebuffers](module-ledger.md#bundlesorgeclipsecorefilebuffers), [org.eclipse.ltk.core.refactoring](module-ledger.md#bundlesorgeclipseltkcorerefactoring), [org.eclipse.text](module-ledger.md#bundlesorgeclipsetext); 0 imported package clauses
- Surface: 3 exported package clauses; inspect manifest for visibility, version, split-package and friend directives; activator `org.eclipse.search.internal.core.SearchCorePlugin`; activation `lazy`
- Source roots: `search/`; 3 Java source-directory occurrences in [the package ledger](package-ledger.md#bundlesorgeclipsesearchcore)
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

