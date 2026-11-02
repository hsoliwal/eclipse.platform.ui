# Module Ledger detail 5

[Return to the complete ledger index](module-ledger.md)

## tests/org.eclipse.ui.tests/data/org.eclipse.newWorkingSet1

- Identity: `newWorkingSet1`; fixture; New WorkingSet One Plug-In
- Discovery: [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/tests/org.eclipse.ui.tests/data/org.eclipse.newWorkingSet1/META-INF/MANIFEST.MF)
- Module: version `3.0.0`; execution environment `not declared`; automatic module name `not declared`
- Dependency direction: requires [org.eclipse.ui](module-ledger.md#bundlesorgeclipseui), `org.eclipse.core.runtime`; 0 imported package clauses
- Surface: 2 exported package clauses; inspect manifest for visibility, version, split-package and friend directives
- Source roots: no source.. value read here; follow the descriptor/custom build route; 0 Java source-directory occurrences
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## tests/org.eclipse.ui.workbench.texteditor.tests

- Identity: `org.eclipse.ui.workbench.texteditor.tests`; test; Workbench Text Editor Test Plug-in
- Discovery: [org.eclipse.ui.workbench.texteditor.tests/.project](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/tests/org.eclipse.ui.workbench.texteditor.tests/.project); [META-INF/MANIFEST.MF](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/tests/org.eclipse.ui.workbench.texteditor.tests/META-INF/MANIFEST.MF); [org.eclipse.ui.workbench.texteditor.tests/build.properties](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/tests/org.eclipse.ui.workbench.texteditor.tests/build.properties)
- Module: version `3.15.0.qualifier`; execution environment `JavaSE-21`; automatic module name `org.eclipse.ui.workbench.texteditor.tests`
- Dependency direction: requires `org.eclipse.core.runtime`, [org.eclipse.jface.text](module-ledger.md#bundlesorgeclipsejfacetext), [org.eclipse.ui.workbench.texteditor](module-ledger.md#bundlesorgeclipseuiworkbenchtexteditor), [org.eclipse.ui](module-ledger.md#bundlesorgeclipseui), [org.eclipse.text.tests](module-ledger.md#testsorgeclipsetexttests), `org.eclipse.core.expressions`; 7 imported package clauses
- Surface: 5 exported package clauses; inspect manifest for visibility, version, split-package and friend directives
- Source roots: `src/`; 6 Java source-directory occurrences in [the package ledger](package-ledger.md#testsorgeclipseuiworkbenchtexteditortests)
- Coverage: partial. Purpose/title and structural boundaries are established by descriptors; complete behavior and API semantics remain open.

## .m3/analysis-pom.xml

- Identity: alternate Maven analysis project, distinct from the production reactor and the adjacent recipe-bootstrap project
- Discovery: [analysis-pom.xml](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/.m3/analysis-pom.xml)
- Purpose: declares a repository-root source directory and OpenRewrite analysis/task profiles with data-table export. Configured exclusions cover generated output/build directories and the tooling directory itself
- Boundary: the source root overlaps production projects for analysis; this does not transfer runtime ownership of their packages to the analysis project
- Coverage: partial. Profile/plugin declarations were read; recipe behavior, dependency availability, output completeness and any previous run claims are not qualified here. NOT_RUN

## tests/org.eclipse.ui.tests/data/org.eclipse.newMultiEditor

- Identity: test fixture Ant project discovered through [build.xml](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/tests/org.eclipse.ui.tests/data/org.eclipse.newMultiEditor/build.xml)
- Purpose: the file declares a default target depending on an empty depends target; no substantive build action is declared in this file
- Coverage: partial/stub. It remains included as a fixture project; surrounding fixture usage and any intended role still need caller/test review. NOT_RUN

## Additional dynamic-fixture build route

[dynamic_classes/build.xml](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/tests/org.eclipse.ui.tests/data/dynamic_classes/build.xml) belongs to the already counted dynamic_classes fixture module. Its default target cleans the jars output directory, packages pre-existing component .bin directories and calls an Eclipse workspace refresh task. It is not a standalone production bundle build or proof those compiled inputs exist. The file and targets were read; none were executed.


