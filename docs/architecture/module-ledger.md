# Project and OSGi module ledger

Source revision: `b8874e6b63390d65e47612574686cb8cf97b1fe8`.

This ledger accounts for 154 descriptor-backed project/build units and 143 OSGi bundles/fragments. A project is evidenced by a POM (including alternate filenames), Eclipse .project, feature.xml, bundle manifest, Ant build.xml or pomless build.properties; a directory without such evidence is not promoted to a project. Nested test fixture manifests remain separate fixture modules. The [package ledger](package-ledger.md) maps source occurrences to their nearest such owner.

All entries are included. Documentation status is **partial**, including entries with a lifecycle guide: descriptor extraction is not a full implementation contract. No entry is reviewed-complete and no build/test run is implied. Each entry still needs a complete public/exported surface review, configuration/defaults and side-effect/error/lifecycle contracts, artifact/build membership validation and source-linked test evidence at this revision. Exact headers, versions, optionality and friend/internal restrictions are retained at the linked manifest; the abbreviated dependency names below are navigation, not a replacement manifest.


The detailed records are split into linked pages for readable, bounded document sizes. The headings below preserve navigation from other architecture pages.

## Reading the boundaries

[Read the full record](module-ledger-01.md#reading-the-boundaries)

## repository-root

[Read the full record](module-ledger-01.md#repository-root)

## .m3/openrewrite-recipes

[Read the full record](module-ledger-01.md#m3openrewrite-recipes)

## bundles

[Read the full record](module-ledger-01.md#bundles)

## bundles/org.eclipse.core.commands

[Read the full record](module-ledger-01.md#bundlesorgeclipsecorecommands)

## bundles/org.eclipse.core.databinding

[Read the full record](module-ledger-01.md#bundlesorgeclipsecoredatabinding)

## bundles/org.eclipse.core.databinding.beans

[Read the full record](module-ledger-01.md#bundlesorgeclipsecoredatabindingbeans)

## bundles/org.eclipse.core.databinding.observable

[Read the full record](module-ledger-01.md#bundlesorgeclipsecoredatabindingobservable)

## bundles/org.eclipse.core.databinding.property

[Read the full record](module-ledger-01.md#bundlesorgeclipsecoredatabindingproperty)

## bundles/org.eclipse.core.filebuffers

[Read the full record](module-ledger-01.md#bundlesorgeclipsecorefilebuffers)

## bundles/org.eclipse.e4.core.commands

[Read the full record](module-ledger-01.md#bundlesorgeclipsee4corecommands)

## bundles/org.eclipse.e4.emf.xpath

[Read the full record](module-ledger-01.md#bundlesorgeclipsee4emfxpath)

## bundles/org.eclipse.e4.ui.bindings

[Read the full record](module-ledger-01.md#bundlesorgeclipsee4uibindings)

## bundles/org.eclipse.e4.ui.css.core

[Read the full record](module-ledger-01.md#bundlesorgeclipsee4uicsscore)

## bundles/org.eclipse.e4.ui.css.swt

[Read the full record](module-ledger-01.md#bundlesorgeclipsee4uicssswt)

## bundles/org.eclipse.e4.ui.css.swt.theme

[Read the full record](module-ledger-01.md#bundlesorgeclipsee4uicssswttheme)

## bundles/org.eclipse.e4.ui.di

[Read the full record](module-ledger-01.md#bundlesorgeclipsee4uidi)

## bundles/org.eclipse.e4.ui.dialogs

[Read the full record](module-ledger-01.md#bundlesorgeclipsee4uidialogs)

## bundles/org.eclipse.e4.ui.ide

[Read the full record](module-ledger-01.md#bundlesorgeclipsee4uiide)

## bundles/org.eclipse.e4.ui.model.workbench

[Read the full record](module-ledger-01.md#bundlesorgeclipsee4uimodelworkbench)

## bundles/org.eclipse.e4.ui.progress

[Read the full record](module-ledger-01.md#bundlesorgeclipsee4uiprogress)

## bundles/org.eclipse.e4.ui.services

[Read the full record](module-ledger-01.md#bundlesorgeclipsee4uiservices)

## bundles/org.eclipse.e4.ui.widgets

[Read the full record](module-ledger-01.md#bundlesorgeclipsee4uiwidgets)

## bundles/org.eclipse.e4.ui.workbench

[Read the full record](module-ledger-01.md#bundlesorgeclipsee4uiworkbench)

## bundles/org.eclipse.e4.ui.workbench.addons.swt

[Read the full record](module-ledger-01.md#bundlesorgeclipsee4uiworkbenchaddonsswt)

## bundles/org.eclipse.e4.ui.workbench.renderers.swt

[Read the full record](module-ledger-01.md#bundlesorgeclipsee4uiworkbenchrenderersswt)

## bundles/org.eclipse.e4.ui.workbench.renderers.swt.cocoa

[Read the full record](module-ledger-01.md#bundlesorgeclipsee4uiworkbenchrenderersswtcocoa)

## bundles/org.eclipse.e4.ui.workbench.swt

[Read the full record](module-ledger-01.md#bundlesorgeclipsee4uiworkbenchswt)

## bundles/org.eclipse.e4.ui.workbench3

[Read the full record](module-ledger-01.md#bundlesorgeclipsee4uiworkbench3)

## bundles/org.eclipse.jface

[Read the full record](module-ledger-01.md#bundlesorgeclipsejface)

## bundles/org.eclipse.jface.databinding

[Read the full record](module-ledger-01.md#bundlesorgeclipsejfacedatabinding)

## bundles/org.eclipse.jface.notifications

[Read the full record](module-ledger-01.md#bundlesorgeclipsejfacenotifications)

## bundles/org.eclipse.jface.text

[Read the full record](module-ledger-01.md#bundlesorgeclipsejfacetext)

## bundles/org.eclipse.ltk.core.refactoring

[Read the full record](module-ledger-01.md#bundlesorgeclipseltkcorerefactoring)

## bundles/org.eclipse.ltk.ui.refactoring

[Read the full record](module-ledger-01.md#bundlesorgeclipseltkuirefactoring)

## bundles/org.eclipse.search

[Read the full record](module-ledger-01.md#bundlesorgeclipsesearch)

## bundles/org.eclipse.search.core

[Read the full record](module-ledger-01.md#bundlesorgeclipsesearchcore)

## bundles/org.eclipse.text

[Read the full record](module-ledger-02.md#bundlesorgeclipsetext)

## bundles/org.eclipse.text.quicksearch

[Read the full record](module-ledger-02.md#bundlesorgeclipsetextquicksearch)

## bundles/org.eclipse.ui

[Read the full record](module-ledger-02.md#bundlesorgeclipseui)

## bundles/org.eclipse.ui.browser

[Read the full record](module-ledger-02.md#bundlesorgeclipseuibrowser)

## bundles/org.eclipse.ui.cocoa

[Read the full record](module-ledger-02.md#bundlesorgeclipseuicocoa)

## bundles/org.eclipse.ui.editors

[Read the full record](module-ledger-02.md#bundlesorgeclipseuieditors)

## bundles/org.eclipse.ui.forms

[Read the full record](module-ledger-02.md#bundlesorgeclipseuiforms)

## bundles/org.eclipse.ui.genericeditor

[Read the full record](module-ledger-02.md#bundlesorgeclipseuigenericeditor)

## bundles/org.eclipse.ui.ide

[Read the full record](module-ledger-02.md#bundlesorgeclipseuiide)

## bundles/org.eclipse.ui.ide.application

[Read the full record](module-ledger-02.md#bundlesorgeclipseuiideapplication)

## bundles/org.eclipse.ui.monitoring

[Read the full record](module-ledger-02.md#bundlesorgeclipseuimonitoring)

## bundles/org.eclipse.ui.navigator

[Read the full record](module-ledger-02.md#bundlesorgeclipseuinavigator)

## bundles/org.eclipse.ui.navigator.resources

[Read the full record](module-ledger-02.md#bundlesorgeclipseuinavigatorresources)

## bundles/org.eclipse.ui.themes

[Read the full record](module-ledger-02.md#bundlesorgeclipseuithemes)

## bundles/org.eclipse.ui.views

[Read the full record](module-ledger-02.md#bundlesorgeclipseuiviews)

## bundles/org.eclipse.ui.views.log

[Read the full record](module-ledger-02.md#bundlesorgeclipseuiviewslog)

## bundles/org.eclipse.ui.views.properties.tabbed

[Read the full record](module-ledger-02.md#bundlesorgeclipseuiviewspropertiestabbed)

## bundles/org.eclipse.ui.win32

[Read the full record](module-ledger-02.md#bundlesorgeclipseuiwin32)

## bundles/org.eclipse.ui.workbench

[Read the full record](module-ledger-02.md#bundlesorgeclipseuiworkbench)

## bundles/org.eclipse.ui.workbench.texteditor

[Read the full record](module-ledger-02.md#bundlesorgeclipseuiworkbenchtexteditor)

## bundles/org.eclipse.urischeme

[Read the full record](module-ledger-02.md#bundlesorgeclipseurischeme)

## docs

[Read the full record](module-ledger-02.md#docs)

## examples

[Read the full record](module-ledger-02.md#examples)

## examples/org.eclipse.e4.demo.cssbridge

[Read the full record](module-ledger-02.md#examplesorgeclipsee4democssbridge)

## examples/org.eclipse.e4.ui.examples.e4editor

[Read the full record](module-ledger-02.md#examplesorgeclipsee4uiexamplese4editor)

## examples/org.eclipse.e4.ui.examples.job

[Read the full record](module-ledger-02.md#examplesorgeclipsee4uiexamplesjob)

## examples/org.eclipse.jface.examples.databinding

[Read the full record](module-ledger-02.md#examplesorgeclipsejfaceexamplesdatabinding)

## examples/org.eclipse.jface.snippets

[Read the full record](module-ledger-02.md#examplesorgeclipsejfacesnippets)

## examples/org.eclipse.jface.text.examples

[Read the full record](module-ledger-02.md#examplesorgeclipsejfacetextexamples)

## examples/org.eclipse.ui.examples.adapterservice

[Read the full record](module-ledger-02.md#examplesorgeclipseuiexamplesadapterservice)

## examples/org.eclipse.ui.examples.contributions

[Read the full record](module-ledger-02.md#examplesorgeclipseuiexamplescontributions)

## examples/org.eclipse.ui.examples.fieldassist

[Read the full record](module-ledger-02.md#examplesorgeclipseuiexamplesfieldassist)

## examples/org.eclipse.ui.examples.filter

[Read the full record](module-ledger-02.md#examplesorgeclipseuiexamplesfilter)

## examples/org.eclipse.ui.examples.iconpack

[Read the full record](module-ledger-02.md#examplesorgeclipseuiexamplesiconpack)

## examples/org.eclipse.ui.examples.javaeditor

[Read the full record](module-ledger-02.md#examplesorgeclipseuiexamplesjavaeditor)

## examples/org.eclipse.ui.examples.job

[Read the full record](module-ledger-03.md#examplesorgeclipseuiexamplesjob)

## examples/org.eclipse.ui.examples.markerHelp

[Read the full record](module-ledger-03.md#examplesorgeclipseuiexamplesmarkerhelp)

## examples/org.eclipse.ui.examples.multipageeditor

[Read the full record](module-ledger-03.md#examplesorgeclipseuiexamplesmultipageeditor)

## examples/org.eclipse.ui.examples.navigator

[Read the full record](module-ledger-03.md#examplesorgeclipseuiexamplesnavigator)

## examples/org.eclipse.ui.examples.perspective

[Read the full record](module-ledger-03.md#examplesorgeclipseuiexamplesperspective)

## examples/org.eclipse.ui.examples.propertysheet

[Read the full record](module-ledger-03.md#examplesorgeclipseuiexamplespropertysheet)

## examples/org.eclipse.ui.examples.readmetool

[Read the full record](module-ledger-03.md#examplesorgeclipseuiexamplesreadmetool)

## examples/org.eclipse.ui.examples.undo

[Read the full record](module-ledger-03.md#examplesorgeclipseuiexamplesundo)

## examples/org.eclipse.ui.examples.uriSchemeHandler

[Read the full record](module-ledger-03.md#examplesorgeclipseuiexamplesurischemehandler)

## examples/org.eclipse.ui.examples.views.properties.tabbed.article

[Read the full record](module-ledger-03.md#examplesorgeclipseuiexamplesviewspropertiestabbedarticle)

## examples/org.eclipse.ui.forms.examples

[Read the full record](module-ledger-03.md#examplesorgeclipseuiformsexamples)

## examples/org.eclipse.ui.genericeditor.examples

[Read the full record](module-ledger-03.md#examplesorgeclipseuigenericeditorexamples)

## features

[Read the full record](module-ledger-03.md#features)

## features/org.eclipse.e4.rcp

[Read the full record](module-ledger-03.md#featuresorgeclipsee4rcp)

## releng/org.eclipse.ui.releng

[Read the full record](module-ledger-03.md#relengorgeclipseuireleng)

## tests

[Read the full record](module-ledger-03.md#tests)

## tests/org.eclipse.core.filebuffers.tests

[Read the full record](module-ledger-03.md#testsorgeclipsecorefilebufferstests)

## tests/org.eclipse.e4.core.commands.tests

[Read the full record](module-ledger-03.md#testsorgeclipsee4corecommandstests)

## tests/org.eclipse.e4.emf.xpath.test

[Read the full record](module-ledger-03.md#testsorgeclipsee4emfxpathtest)

## tests/org.eclipse.e4.ui.bindings.tests

[Read the full record](module-ledger-03.md#testsorgeclipsee4uibindingstests)

## tests/org.eclipse.e4.ui.tests

[Read the full record](module-ledger-03.md#testsorgeclipsee4uitests)

## tests/org.eclipse.e4.ui.tests.css.core

[Read the full record](module-ledger-03.md#testsorgeclipsee4uitestscsscore)

## tests/org.eclipse.e4.ui.tests.css.swt

[Read the full record](module-ledger-03.md#testsorgeclipsee4uitestscssswt)

## tests/org.eclipse.e4.ui.tests/data/org.eclipse.extensionsSortTests/tests.extensions.intermediate

[Read the full record](module-ledger-03.md#testsorgeclipsee4uitestsdataorgeclipseextensionssortteststestsextensionsintermediate)

## tests/org.eclipse.e4.ui.tests/data/org.eclipse.extensionsSortTests/tests.extensions.leaf

[Read the full record](module-ledger-03.md#testsorgeclipsee4uitestsdataorgeclipseextensionssortteststestsextensionsleaf)

## tests/org.eclipse.e4.ui.tests/data/org.eclipse.extensionsSortTests/tests.extensions.root

[Read the full record](module-ledger-03.md#testsorgeclipsee4uitestsdataorgeclipseextensionssortteststestsextensionsroot)

## tests/org.eclipse.e4.ui.workbench.addons.swt.test

[Read the full record](module-ledger-03.md#testsorgeclipsee4uiworkbenchaddonsswttest)

## tests/org.eclipse.jface.tests

[Read the full record](module-ledger-03.md#testsorgeclipsejfacetests)

## tests/org.eclipse.jface.tests.databinding

[Read the full record](module-ledger-03.md#testsorgeclipsejfacetestsdatabinding)

## tests/org.eclipse.jface.tests.databinding.conformance

[Read the full record](module-ledger-03.md#testsorgeclipsejfacetestsdatabindingconformance)

## tests/org.eclipse.jface.tests.notifications

[Read the full record](module-ledger-03.md#testsorgeclipsejfacetestsnotifications)

## tests/org.eclipse.jface.text.tests

[Read the full record](module-ledger-03.md#testsorgeclipsejfacetexttests)

## tests/org.eclipse.ltk.core.refactoring.tests

[Read the full record](module-ledger-03.md#testsorgeclipseltkcorerefactoringtests)

## tests/org.eclipse.ltk.ui.refactoring.tests

[Read the full record](module-ledger-03.md#testsorgeclipseltkuirefactoringtests)

## tests/org.eclipse.search.tests

[Read the full record](module-ledger-03.md#testsorgeclipsesearchtests)

## tests/org.eclipse.tests.urischeme

[Read the full record](module-ledger-03.md#testsorgeclipsetestsurischeme)

## tests/org.eclipse.text.quicksearch.tests

[Read the full record](module-ledger-04.md#testsorgeclipsetextquicksearchtests)

## tests/org.eclipse.text.tests

[Read the full record](module-ledger-04.md#testsorgeclipsetexttests)

## tests/org.eclipse.ui.editors.tests

[Read the full record](module-ledger-04.md#testsorgeclipseuieditorstests)

## tests/org.eclipse.ui.genericeditor.tests

[Read the full record](module-ledger-04.md#testsorgeclipseuigenericeditortests)

## tests/org.eclipse.ui.ide.application.tests

[Read the full record](module-ledger-04.md#testsorgeclipseuiideapplicationtests)

## tests/org.eclipse.ui.monitoring.tests

[Read the full record](module-ledger-04.md#testsorgeclipseuimonitoringtests)

## tests/org.eclipse.ui.tests

[Read the full record](module-ledger-04.md#testsorgeclipseuitests)

## tests/org.eclipse.ui.tests.browser

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsbrowser)

## tests/org.eclipse.ui.tests.forms

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsforms)

## tests/org.eclipse.ui.tests.harness

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsharness)

## tests/org.eclipse.ui.tests.navigator

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsnavigator)

## tests/org.eclipse.ui.tests.performance

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsperformance)

## tests/org.eclipse.ui.tests.pluginchecks

[Read the full record](module-ledger-04.md#testsorgeclipseuitestspluginchecks)

## tests/org.eclipse.ui.tests.rcp

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsrcp)

## tests/org.eclipse.ui.tests.views.properties.tabbed

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsviewspropertiestabbed)

## tests/org.eclipse.ui.tests/data/dynamic_classes

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsdatadynamic_classes)

## tests/org.eclipse.ui.tests/data/org.eclipse.acceleratorConfigurationsExtensionDynamicTest

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsdataorgeclipseacceleratorconfigurationsextensiondynamictest)

## tests/org.eclipse.ui.tests/data/org.eclipse.acceleratorScopesExtensionDynamicTest

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsdataorgeclipseacceleratorscopesextensiondynamictest)

## tests/org.eclipse.ui.tests/data/org.eclipse.actionDefinitionsExtensionDynamicTest

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsdataorgeclipseactiondefinitionsextensiondynamictest)

## tests/org.eclipse.ui.tests/data/org.eclipse.bindingsExtensionDynamicTest

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsdataorgeclipsebindingsextensiondynamictest)

## tests/org.eclipse.ui.tests/data/org.eclipse.commandsExtensionDynamicTest

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsdataorgeclipsecommandsextensiondynamictest)

## tests/org.eclipse.ui.tests/data/org.eclipse.contextsExtensionDynamicTest

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsdataorgeclipsecontextsextensiondynamictest)

## tests/org.eclipse.ui.tests/data/org.eclipse.handlersExtensionDynamicTest

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsdataorgeclipsehandlersextensiondynamictest)

## tests/org.eclipse.ui.tests/data/org.eclipse.newActionSet1

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsdataorgeclipsenewactionset1)

## tests/org.eclipse.ui.tests/data/org.eclipse.newActivitySupport1

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsdataorgeclipsenewactivitysupport1)

## tests/org.eclipse.ui.tests/data/org.eclipse.newBrowser1

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsdataorgeclipsenewbrowser1)

## tests/org.eclipse.ui.tests/data/org.eclipse.newDecorator1

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsdataorgeclipsenewdecorator1)

## tests/org.eclipse.ui.tests/data/org.eclipse.newEditor1

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsdataorgeclipseneweditor1)

## tests/org.eclipse.ui.tests/data/org.eclipse.newEncoding1

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsdataorgeclipsenewencoding1)

## tests/org.eclipse.ui.tests/data/org.eclipse.newHelpSupport1

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsdataorgeclipsenewhelpsupport1)

## tests/org.eclipse.ui.tests/data/org.eclipse.newIntro1

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsdataorgeclipsenewintro1)

## tests/org.eclipse.ui.tests/data/org.eclipse.newInvalidMenuContribution1

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsdataorgeclipsenewinvalidmenucontribution1)

## tests/org.eclipse.ui.tests/data/org.eclipse.newKeyword1

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsdataorgeclipsenewkeyword1)

## tests/org.eclipse.ui.tests/data/org.eclipse.newMarkerSupport

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsdataorgeclipsenewmarkersupport)

## tests/org.eclipse.ui.tests/data/org.eclipse.newNewWizard1

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsdataorgeclipsenewnewwizard1)

## tests/org.eclipse.ui.tests/data/org.eclipse.newOC1

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsdataorgeclipsenewoc1)

## tests/org.eclipse.ui.tests/data/org.eclipse.newPerspective1

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsdataorgeclipsenewperspective1)

## tests/org.eclipse.ui.tests/data/org.eclipse.newPerspectiveExtension1

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsdataorgeclipsenewperspectiveextension1)

## tests/org.eclipse.ui.tests/data/org.eclipse.newPreferencePage1

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsdataorgeclipsenewpreferencepage1)

## tests/org.eclipse.ui.tests/data/org.eclipse.newPropertyPage1

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsdataorgeclipsenewpropertypage1)

## tests/org.eclipse.ui.tests/data/org.eclipse.newStartup1

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsdataorgeclipsenewstartup1)

## tests/org.eclipse.ui.tests/data/org.eclipse.newStatusHandler1

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsdataorgeclipsenewstatushandler1)

## tests/org.eclipse.ui.tests/data/org.eclipse.newView1

[Read the full record](module-ledger-04.md#testsorgeclipseuitestsdataorgeclipsenewview1)

## tests/org.eclipse.ui.tests/data/org.eclipse.newWorkingSet1

[Read the full record](module-ledger-05.md#testsorgeclipseuitestsdataorgeclipsenewworkingset1)

## tests/org.eclipse.ui.workbench.texteditor.tests

[Read the full record](module-ledger-05.md#testsorgeclipseuiworkbenchtexteditortests)

## .m3/analysis-pom.xml

[Read the full record](module-ledger-05.md#m3analysis-pomxml)

## tests/org.eclipse.ui.tests/data/org.eclipse.newMultiEditor

[Read the full record](module-ledger-05.md#testsorgeclipseuitestsdataorgeclipsenewmultieditor)

## Additional dynamic-fixture build route

[Read the full record](module-ledger-05.md#additional-dynamic-fixture-build-route)

