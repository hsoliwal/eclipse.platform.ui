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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.internal.forms.widgets.BusyIndicator;

/**
 * Acceptance check of the every-widget forms BusyIndicator H leaf: a busy indicator nobody can see (an ancestor hidden,
 * or its shell minimized) keeps no animation timer, and its next paint restarts the animation. Win32: the pending
 * timers are read from Display.timerList. Run with the forms classes, their dependencies and an SWT jar on the class
 * path: java BusyIndicatorHiddenCheck. Prints PASSED or throws.
 * <p>
 * Outside OSGi the indicator cannot load its frame images (FrameworkUtil.getBundle answers null), so the check fills
 * its frame cache with plain 16x16 images first.
 */
public class BusyIndicatorHiddenCheck {

	public static void main(String[] args) {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		Composite holder = new Composite(shell, SWT.NONE);
		holder.setBounds(0, 0, 100, 100);
		BusyIndicator indicator = new BusyIndicator(holder, SWT.NONE);
		fillFrames(indicator, display);
		indicator.setBounds(0, 0, 0, 0); // no paint (and no frame image) while it starts
		shell.setSize(200, 200);
		shell.open();
		spin(display, 200);

		indicator.setBusy(true);
		spin(display, 500);
		check(timers(display) == 1, "a busy indicator animates (" + timers(display) + " timers)");

		holder.setVisible(false);
		spin(display, 500);
		check(timers(display) == 0, "a busy indicator under a hidden ancestor keeps no timer (" + timers(display) + ")");

		holder.setVisible(true);
		indicator.setSize(16, 16);
		spin(display, 500);
		check(timers(display) == 1, "the next paint restarts the animation (" + timers(display) + " timers)");

		shell.setMinimized(true);
		spin(display, 500);
		boolean minimized = shell.getMinimized();
		if (minimized) {
			check(timers(display) == 0, "a busy indicator in a minimized shell keeps no timer (" + timers(display) + ")");
			shell.setMinimized(false);
			spin(display, 500);
			check(timers(display) == 1, "restoring the shell restarts the animation (" + timers(display) + " timers)");
			System.out.println("minimize: checked");
		}

		indicator.setBusy(false);
		spin(display, 500);
		check(timers(display) == 0, "an idle indicator keeps no timer");
		shell.dispose();
		System.out.println("PASSED");
	}

	/* the 8 animation frames, which the indicator loads from its bundle under OSGi */
	static void fillFrames(BusyIndicator indicator, Display display) {
		try {
			java.lang.reflect.Field field = BusyIndicator.class.getDeclaredField("imageCache");
			field.setAccessible(true);
			Image[] frames = new Image[8];
			for (int i = 0; i < frames.length; i++) {
				frames[i] = new Image(display, 16, 16);
			}
			field.set(indicator, frames);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	/* pending BusyIndicator timers in the display's timer list (win32 Display.timerList) */
	static int timers(Display display) {
		try {
			java.lang.reflect.Field field = Display.class.getDeclaredField("timerList");
			field.setAccessible(true);
			Runnable[] list = (Runnable[]) field.get(display);
			int count = 0;
			if (list != null) {
				for (Runnable timer : list) {
					if (timer != null && timer.getClass().getName().startsWith(BusyIndicator.class.getName())) {
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
