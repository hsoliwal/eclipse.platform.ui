/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lars Vogel <Lars.Vogel@vogella.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.tests.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.handlers.ClosePartHandler;
import org.eclipse.ui.internal.handlers.MaximizePartHandler;
import org.eclipse.ui.internal.handlers.MinimizePartHandler;
import org.eclipse.ui.internal.handlers.NewEditorHandler;
import org.eclipse.ui.internal.handlers.OpenInNewWindowHandler;
import org.eclipse.ui.tests.api.MockEditorPart;
import org.eclipse.ui.tests.harness.util.CloseTestWindowsExtension;
import org.eclipse.ui.tests.harness.util.FileUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Executes the part related workbench commands, which are handled by POJO
 * handlers.
 */
@ExtendWith(CloseTestWindowsExtension.class)
public class PartHandlersTest {

	private static final String VIEW_ID = "org.eclipse.ui.views.ProblemView"; //$NON-NLS-1$

	private IWorkbench workbench;

	private IWorkbenchPage page;

	private IHandlerService handlerService;

	private IProject project;

	@BeforeEach
	public void setUp() throws Exception {
		workbench = PlatformUI.getWorkbench();
		page = workbench.getActiveWorkbenchWindow().getActivePage();
		handlerService = workbench.getService(IHandlerService.class);
		project = FileUtil.createProject("PartHandlersTest"); //$NON-NLS-1$
	}

	@AfterEach
	public void tearDown() throws Exception {
		IViewPart view = page.findView(VIEW_ID);
		if (view != null) {
			page.setPartState(page.getReference(view), IWorkbenchPage.STATE_RESTORED);
			page.hideView(view);
		}
		page.closeAllEditors(false);
		if (project != null) {
			project.delete(true, true, null);
		}
	}

	@Test
	public void closePartHidesActiveView() throws Exception {
		page.showView(VIEW_ID);

		handlerService.executeCommand(IWorkbenchCommandConstants.WINDOW_CLOSE_PART, null);

		assertNull(page.findView(VIEW_ID));
	}

	@Test
	public void closePartClosesActiveEditor() throws Exception {
		openEditor();

		handlerService.executeCommand(IWorkbenchCommandConstants.WINDOW_CLOSE_PART, null);

		assertEquals(0, page.getEditorReferences().length);
	}

	@Test
	public void closePartInContextSnapshotClosesThePartOfTheSnapshot() throws Exception {
		page.showView(VIEW_ID);
		IEvaluationContext snapshot = handlerService.createContextSnapshot(true);
		openEditor();

		handlerService.executeCommandInContext(closePartCommand(), null, snapshot);

		assertNull(page.findView(VIEW_ID));
		assertEquals(1, page.getEditorReferences().length);
	}

	@Test
	public void closePartInContextSnapshotWithoutPartKeepsTheActivePart() throws Exception {
		openEditor();
		IEvaluationContext snapshot = new EvaluationContext(null, IEvaluationContext.UNDEFINED_VARIABLE);
		snapshot.addVariable(ISources.ACTIVE_WORKBENCH_WINDOW_NAME, workbench.getActiveWorkbenchWindow());

		handlerService.executeCommandInContext(closePartCommand(), null, snapshot);

		assertEquals(1, page.getEditorReferences().length,
				"a part missing from the snapshot must not resolve to the live one"); //$NON-NLS-1$
	}

	@Test
	public void maximizePartTogglesZoom() throws Exception {
		IWorkbenchPartReference reference = page.getReference(page.showView(VIEW_ID));

		handlerService.executeCommand(IWorkbenchCommandConstants.WINDOW_MAXIMIZE_ACTIVE_VIEW_OR_EDITOR, null);
		assertEquals(IWorkbenchPage.STATE_MAXIMIZED, page.getPartState(reference));

		handlerService.executeCommand(IWorkbenchCommandConstants.WINDOW_MAXIMIZE_ACTIVE_VIEW_OR_EDITOR, null);
		assertEquals(IWorkbenchPage.STATE_RESTORED, page.getPartState(reference));
	}

	@Test
	public void minimizePartMinimizesActivePart() throws Exception {
		IWorkbenchPartReference reference = page.getReference(page.showView(VIEW_ID));

		handlerService.executeCommand(IWorkbenchCommandConstants.WINDOW_MINIMIZE_ACTIVE_VIEW_OR_EDITOR, null);

		assertEquals(IWorkbenchPage.STATE_MINIMIZED, page.getPartState(reference));
	}

	@Test
	public void newEditorOpensActiveEditorInputAgain() throws Exception {
		IEditorPart editor = openEditor();

		handlerService.executeCommand(IWorkbenchCommandConstants.WINDOW_NEW_EDITOR, null);

		assertEquals(2, page.getEditorReferences().length);
		IEditorPart newEditor = page.getActiveEditor();
		assertNotSame(editor, newEditor);
		assertEquals(editor.getEditorInput(), newEditor.getEditorInput());
	}

	@Test
	public void newWindowOpensWorkbenchWindow() throws Exception {
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		List<IWorkbenchWindow> windowsBefore = List.of(workbench.getWorkbenchWindows());

		handlerService.executeCommand(IWorkbenchCommandConstants.WINDOW_NEW_WINDOW, null);

		List<IWorkbenchWindow> newWindows = Arrays.stream(workbench.getWorkbenchWindows())
				.filter(w -> !windowsBefore.contains(w)).toList();
		assertEquals(1, newWindows.size());
		IWorkbenchWindow newWindow = newWindows.get(0);
		assertEquals(window.getActivePage().getPerspective().getId(),
				newWindow.getActivePage().getPerspective().getId());
	}


	@Test
	public void partHandlersRetainTheOriginalHandlerHierarchy() {
		List<AbstractHandler> handlers = List.of(new ClosePartHandler(), new MaximizePartHandler(),
				new MinimizePartHandler(), new NewEditorHandler(), new OpenInNewWindowHandler());
		for (AbstractHandler handler : handlers) {
			assertTrue(handler.isEnabled());
			assertTrue(handler.isHandled());
			handler.dispose();
		}
	}

	@Test
	public void legacyClosePartUsesTheExplicitSnapshotPart() throws Exception {
		IViewPart view = page.showView(VIEW_ID);
		IEvaluationContext snapshot = handlerService.createContextSnapshot(true);
		openEditor();

		assertNull(new ClosePartHandler().execute(legacyEvent(snapshot)));

		assertNull(page.findView(VIEW_ID));
		assertEquals(1, page.getEditorReferences().length);
		assertEquals(view, snapshot.getVariable(ISources.ACTIVE_PART_NAME));
	}

	@Test
	public void legacyClosePartRetainsCheckedMissingPartFailure() throws Exception {
		openEditor();
		IEvaluationContext snapshot = new EvaluationContext(null, IEvaluationContext.UNDEFINED_VARIABLE);
		snapshot.addVariable(ISources.ACTIVE_WORKBENCH_WINDOW_NAME, workbench.getActiveWorkbenchWindow());

		assertThrows(ExecutionException.class, () -> new ClosePartHandler().execute(legacyEvent(snapshot)));

		assertEquals(1, page.getEditorReferences().length);
	}

	@Test
	public void legacyOptionalWindowHandlersRetainMissingWindowNoOp() throws Exception {
		IEvaluationContext empty = new EvaluationContext(null, IEvaluationContext.UNDEFINED_VARIABLE);
		ExecutionEvent event = legacyEvent(empty);

		assertNull(new MaximizePartHandler().execute(event));
		assertNull(new MinimizePartHandler().execute(event));
		assertNull(new OpenInNewWindowHandler().execute(event));
	}

	@Test
	public void legacyNewEditorOpensTheActiveEditorInputAgain() throws Exception {
		IEditorPart editor = openEditor();

		assertNull(new NewEditorHandler().execute(legacyEvent(handlerService.createContextSnapshot(true))));

		assertEquals(2, page.getEditorReferences().length);
		assertNotSame(editor, page.getActiveEditor());
		assertEquals(editor.getEditorInput(), page.getActiveEditor().getEditorInput());
	}

	@Test
	public void legacyNewWindowRetainsTheActivePerspective() throws Exception {
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		List<IWorkbenchWindow> before = List.of(workbench.getWorkbenchWindows());

		assertNull(new OpenInNewWindowHandler().execute(legacyEvent(handlerService.createContextSnapshot(true))));

		List<IWorkbenchWindow> added = Arrays.stream(workbench.getWorkbenchWindows())
				.filter(w -> !before.contains(w)).toList();
		assertEquals(1, added.size());
		assertEquals(window.getActivePage().getPerspective().getId(),
				added.get(0).getActivePage().getPerspective().getId());
	}

	private ExecutionEvent legacyEvent(IEvaluationContext snapshot) {
		return new ExecutionEvent(closePartCommand().getCommand(), Map.of(), null, snapshot);
	}

	private ParameterizedCommand closePartCommand() {
		return ParameterizedCommand.generateCommand(
				workbench.getService(ICommandService.class).getCommand(IWorkbenchCommandConstants.WINDOW_CLOSE_PART),
				null);
	}

	private IEditorPart openEditor() throws Exception {
		IFile file = FileUtil.createFile("test.mock1", project); //$NON-NLS-1$
		return IDE.openEditor(page, file, MockEditorPart.ID1);
	}
}
