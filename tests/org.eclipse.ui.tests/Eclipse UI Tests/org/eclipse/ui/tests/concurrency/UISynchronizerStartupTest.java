/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and Contributors.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.ui.tests.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Synchronizer;
import org.eclipse.ui.internal.StartupThreading.StartupRunnable;
import org.eclipse.ui.internal.UILockListener;
import org.eclipse.ui.internal.UISynchronizer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs in the UI harness with either upstream SWT or the fork's priority API. */
public class UISynchronizerStartupTest {
	private static final int INPUT = 0, DEFAULT = 2;
	private Display display;
	private Synchronizer previous;
	private StartupSynchronizer synchronizer;
	private Boolean startupThread, overrideThread;

	private static final class StartupSynchronizer extends UISynchronizer {
		StartupSynchronizer(Display display) {
			super(display, new UILockListener(display));
		}

		void legacy(Runnable runnable) {
			super.asyncExec(runnable);
		}

		synchronized int pending() {
			return pendingStartup == null ? 0 : pendingStartup.size();
		}

		synchronized boolean starting() {
			return isStarting;
		}
	}

	@BeforeEach
	public void setUp() {
		display = Display.getCurrent();
		assertNotNull(display, "The concurrency suite must run on its Display thread");
		startupThread = UISynchronizer.startupThread.get();
		overrideThread = UISynchronizer.overrideThread.get();
		UISynchronizer.overrideThread.set(Boolean.FALSE);
		UISynchronizer.startupThread.set(Boolean.FALSE);
		previous = display.getSynchronizer();
		synchronizer = new StartupSynchronizer(display);
		display.setSynchronizer(synchronizer);
	}

	@AfterEach
	public void tearDown() {
		try {
			if (synchronizer != null && !display.isDisposed()) {
				if (synchronizer.starting()) {
					synchronizer.started();
				}
				display.setSynchronizer(previous);
				drain();
			}
		} finally {
			if (startupThread != null) {
				UISynchronizer.startupThread.set(Boolean.FALSE);
				UISynchronizer.overrideThread.set(overrideThread);
				UISynchronizer.startupThread.set(startupThread);
			}
		}
	}

	@Test
	public void defersEveryAsyncOverloadUntilStartupCompletes() {
		List<String> events = new ArrayList<>();
		synchronizer.legacy(() -> events.add("legacy"));
		synchronizer.asyncExec("keyed", () -> events.add("keyed"));
		synchronizer.asyncExec(INPUT, "priority", () -> events.add("priority"));
		drain();
		assertTrue(events.isEmpty());
		assertEquals(3, synchronizer.pending());
		synchronizer.started();
		drain();
		assertEquals(priorityAvailable() ? List.of("priority", "legacy", "keyed")
				: List.of("legacy", "keyed", "priority"), events);
	}

	@Test
	public void retainsPriorityAndKeyWhenDeferredWorkIsReleased() {
		List<String> events = new ArrayList<>();
		synchronizer.asyncExec(INPUT, "same", () -> events.add("old input"));
		synchronizer.asyncExec(DEFAULT, "same", () -> events.add("default"));
		synchronizer.asyncExec(INPUT, "same", () -> events.add("new input"));
		drain();
		assertTrue(events.isEmpty());
		synchronizer.started();
		drain();
		assertEquals(priorityAvailable() ? List.of("new input", "default")
				: List.of("old input", "default", "new input"), events);
	}

	@Test
	public void originalStartupRunnableRemainsEligible() {
		List<String> events = new ArrayList<>();
		synchronizer.asyncExec(INPUT, "deferred", () -> events.add("deferred"));
		StartupRunnable startup = new StartupRunnable() {
			@Override
			public void runWithException() {
				events.add("startup");
			}
		};
		synchronizer.asyncExec(INPUT, "startup", startup);
		drain();
		assertEquals(List.of("startup"), events);
		assertEquals(1, synchronizer.pending());
	}

	@Test
	public void explicitStartupOverrideRemainsEligible() {
		List<String> events = new ArrayList<>();
		UISynchronizer.overrideThread.set(Boolean.TRUE);
		try {
			synchronizer.asyncExec("keyed", () -> events.add("keyed"));
			synchronizer.asyncExec(INPUT, "priority", () -> events.add("priority"));
		} finally {
			UISynchronizer.overrideThread.set(Boolean.FALSE);
		}
		drain();
		assertEquals(priorityAvailable() ? List.of("priority", "keyed") : List.of("keyed", "priority"), events);
		assertEquals(0, synchronizer.pending());
	}

	@Test
	public void invalidPrioritySubmissionsFailBeforeDeferral() {
		assertThrows(IllegalArgumentException.class, () -> synchronizer.asyncExec("key", null));
		assertThrows(IllegalArgumentException.class, () -> synchronizer.asyncExec(INPUT, "key", null));
		assertThrows(IllegalArgumentException.class, () -> synchronizer.asyncExec(-1, "key", () -> { }));
		assertThrows(IllegalArgumentException.class, () -> synchronizer.asyncExec(5, "key", () -> { }));
		assertEquals(0, synchronizer.pending());
	}

	@Test
	public void backendComputesOffThreadAndPresentationWaitsForStartup() throws InterruptedException {
		AtomicReference<Thread> computedOn = new AtomicReference<>(), presentedOn = new AtomicReference<>();
		AtomicReference<Integer> result = new AtomicReference<>();
		Thread backend = new Thread(() -> {
			computedOn.set(Thread.currentThread());
			int prepared = 6 * 7;
			synchronizer.asyncExec("viewport", () -> {
				presentedOn.set(Thread.currentThread());
				result.set(prepared);
			});
		}, "viewport-backend-admission-test");
		backend.start();
		backend.join(5_000);
		assertFalse(backend.isAlive());
		assertNotNull(computedOn.get());
		assertNotSame(display.getThread(), computedOn.get());
		drain();
		assertNull(result.get());
		synchronizer.started();
		drain();
		assertSame(display.getThread(), presentedOn.get());
		assertEquals(42, result.get());
	}

	@Test
	public void ordinaryNullWakeIsNotDeferred() {
		synchronizer.legacy(null);
		assertEquals(0, synchronizer.pending());
	}

	@Test
	public void submissionsAfterStartupReachSuperclassWithoutRecursion() {
		List<String> events = new ArrayList<>();
		synchronizer.started();
		synchronizer.asyncExec("key", () -> events.add("old"));
		synchronizer.asyncExec("key", () -> events.add("new"));
		drain();
		assertEquals(priorityAvailable() ? List.of("new") : List.of("old", "new"), events);
	}

	private void drain() {
		for (int i = 0; i < 1_000 && display.readAndDispatch(); i++) {
			// Bounded event pumping also exercises callbacks before started().
		}
	}

	private static boolean priorityAvailable() {
		try {
			Synchronizer.class.getMethod("asyncExec", int.class, Object.class, Runnable.class);
			return true;
		} catch (NoSuchMethodException e) {
			return false;
		}
	}
}
