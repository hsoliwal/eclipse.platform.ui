/*******************************************************************************
 * Copyright (c) 2006, 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ui.internal.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import jakarta.inject.Named;

/**
 * Provide a Handler for the Close Part command. This can then be bound to
 * whatever keybinding the user prefers.
 *
 * @since 3.3
 */
public class ClosePartHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPart part = HandlerUtil.getActivePartChecked(event);
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		if (part instanceof IEditorPart) {
			window.getActivePage().closeEditor((IEditorPart) part, true);
		} else if (part instanceof IViewPart) {
			window.getActivePage().hideView((IViewPart) part);
		}

		return null;
	}

	@Execute
	public void execute(@Named(ISources.ACTIVE_PART_NAME) IWorkbenchPart part,
			@Named(ISources.ACTIVE_WORKBENCH_WINDOW_NAME) IWorkbenchWindow window) {
		if (part instanceof IEditorPart) {
			window.getActivePage().closeEditor((IEditorPart) part, true);
		} else if (part instanceof IViewPart) {
			window.getActivePage().hideView((IViewPart) part);
		}
	}
}
