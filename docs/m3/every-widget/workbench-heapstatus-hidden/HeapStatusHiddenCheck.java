/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/

import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.internal.HeapStatus;

/**
 * Acceptance check of the every-widget workbench HeapStatus H leaf: a heap status nobody can see (an ancestor hidden,
 * or its shell minimized) keeps no sampling timer, and its next paint restarts sampling. Win32: the pending timers are
 * read from Display.timerList. Run with the HeapStatus class, the workbench jar and its dependencies and an SWT jar on
 * the class path: java HeapStatusHiddenCheck. Prints PASSED or throws. Use the workbench classes unsigned (an extract of
 * the jar without its META-INF signature files): the signed jar refuses a rebuilt HeapStatus in its package.
 */
public class HeapStatusHiddenCheck {

	public static void main(String[] args) {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		Composite holder = new Composite(shell, SWT.NONE);
		holder.setBounds(0, 0, 200, 40);
		PreferenceStore preferences = new PreferenceStore();
		preferences.setValue("HeapStatus.updateInterval", 100); //$NON-NLS-1$ IHeapStatusConstants.PREF_UPDATE_INTERVAL
		HeapStatus status = new HeapStatus(holder, preferences);
		status.setBounds(0, 0, 160, 24);
		shell.setSize(240, 120);
		shell.open();
		spin(display, 400);
		check(timers(display) == 1, "a shown heap status samples (" + timers(display) + " timers)");

		holder.setVisible(false);
		spin(display, 400);
		check(timers(display) == 0, "a heap status under a hidden ancestor keeps no timer (" + timers(display) + ")");

		holder.setVisible(true);
		spin(display, 400);
		check(timers(display) == 1, "the next paint restarts sampling (" + timers(display) + " timers)");

		shell.setMinimized(true);
		spin(display, 400);
		if (shell.getMinimized()) {
			check(timers(display) == 0, "a heap status in a minimized shell keeps no timer (" + timers(display) + ")");
			shell.setMinimized(false);
			spin(display, 400);
			check(timers(display) == 1, "restoring the shell restarts sampling (" + timers(display) + " timers)");
			System.out.println("minimize: checked");
		}
		shell.dispose();
		System.out.println("PASSED");
	}

	/* pending HeapStatus timers in the display's timer list (win32 Display.timerList) */
	static int timers(Display display) {
		try {
			java.lang.reflect.Field field = Display.class.getDeclaredField("timerList");
			field.setAccessible(true);
			Runnable[] list = (Runnable[]) field.get(display);
			int count = 0;
			if (list != null) {
				for (Runnable timer : list) {
					if (timer != null && timer.getClass().getName().startsWith(HeapStatus.class.getName())) {
						count++;
					}
				}
			}
			return count;
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	static void spin(Display display, int milliseconds) {
		long end = System.currentTimeMillis() + milliseconds;
		while (System.currentTimeMillis() < end) {
			if (!display.readAndDispatch()) {
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					return;
				}
			}
		}
	}

	static void check(boolean condition, String message) {
		if (!condition) {
			throw new IllegalStateException(message);
		}
	}
}
