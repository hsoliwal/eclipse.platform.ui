/*******************************************************************************
 * Copyright (c) 20020 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 ******************************************************************************/
package org.eclipse.e4.ui.workbench.swt;

import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

/**
 * @since 0.16
 */
public final class DisplayUISynchronize extends UISynchronize {

	private final Display display;

	public DisplayUISynchronize(Display display) {
		this.display = display;
	}

	@Override
	public void syncExec(Runnable runnable) {
		if (display != null && !display.isDisposed()) {
			display.syncExec(runnable);
		}
	}

	@Override
	public void asyncExec(Runnable runnable) {
		if (display != null && !display.isDisposed()) {
			display.asyncExec(runnable);
		}
	}

	/**
	 * Attempts an asynchronous UI submission and reports whether the underlying
	 * SWT Display accepted the runnable. Existing {@link #asyncExec(Runnable)}
	 * retains its original void-return and exception behavior.
	 *
	 * <p>The admission check is made again by Display.asyncExec under SWT's
	 * Device lock. Disposal between our initial predicate and admission is
	 * reported as a refusal, not as an accepted command that will never run.</p>
	 *
	 * @param runnable the UI callback (null retains SWT wake semantics)
	 * @return true if accepted by Display, false if there is no live Display
	 * @since 0.19
	 */
	public boolean tryAsyncExec(Runnable runnable) {
		if (display == null || display.isDisposed()) {
			return false;
		}
		try {
			display.asyncExec(runnable);
			return true;
		} catch (SWTException disposed) {
			if (disposed.code == SWT.ERROR_DEVICE_DISPOSED) {
				return false;
			}
			throw disposed;
		}
	}

	@Override
	public boolean isUIThread(Thread thread) {
		return Display.findDisplay(thread) != null;
	}

	@Override
	protected void showBusyWhile(Runnable runnable) {
		BusyIndicator.showWhile(display, runnable);
	}

	@Override
	protected boolean dispatchEvents() {
		if (display != null && !display.isDisposed()) {
			return display.readAndDispatch();
		}
		return false;
	}
}
