/*******************************************************************************
 * Copyright (c) 2000, 2026 IBM Corporation and others.
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
 *     Hitesh Soliwal and Contributors - priority dispatch startup admission
 *******************************************************************************/
package org.eclipse.ui.internal;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Synchronizer;
import org.eclipse.ui.internal.StartupThreading.StartupRunnable;

public class UISynchronizer extends Synchronizer {
	/* Values of SWT's optional priority dispatch API; no static linkage to fork-only fields. */
	private static final int FIRST_PRIORITY = 0, DEFAULT_PRIORITY = 2, LAST_PRIORITY = 4;
	private static final MethodHandle PRIORITY_ASYNC_EXEC = priorityAsyncExec();

	/** Metadata is needed only while a priority submission is deferred during startup. */
	private record PendingAsync(int priority, Object key, Runnable runnable) implements Runnable {
		@Override
		public void run() {
			runnable.run();
		}
	}

	protected UILockListener lockListener;

	/**
	 * Indicates that the UI is in startup mode and that no non-workbench runnables
	 * should be invoked.
	 */
	protected boolean isStarting = true;

	/**
	 * List of non-workbench Runnables that need executing at some point in the
	 * future
	 */
	protected List<Runnable> pendingStartup = new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW

	/**
	 * Setting this variable to the value {@link Boolean#TRUE} will allow a thread
	 * to execute code during the startup sequence.
	 */
	public static final ThreadLocal<Boolean> startupThread = new ThreadLocal<>() {

		@Override
		protected Boolean initialValue() {
			return Boolean.FALSE;
		}

		@Override
		public void set(Boolean value) {
			if (value != Boolean.TRUE && value != Boolean.FALSE) {
				throw new IllegalArgumentException();
			}
			super.set(value);
		}
	};

	public static final ThreadLocal<Boolean> overrideThread = new ThreadLocal<>() {
		@Override
		protected Boolean initialValue() {
			return Boolean.FALSE;
		}

		@Override
		public void set(Boolean value) {
			if (value != Boolean.TRUE && value != Boolean.FALSE) {
				throw new IllegalArgumentException();
			}
			if (value == Boolean.TRUE && startupThread.get().booleanValue()) {
				throw new IllegalStateException();
			}
			super.set(value);
		}
	};

	public UISynchronizer(Display display, UILockListener lock) {
		super(display);
		this.lockListener = lock;
	}

	public void started() { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		synchronized (this) {
			if (!isStarting) {
				throw new IllegalStateException();
			}
			isStarting = false;
			for (Iterator<Runnable> i = pendingStartup.iterator(); i.hasNext();) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				Runnable runnable = i.next();
				try {
					// queue up all pending asyncs
					if (runnable instanceof PendingAsync pending) {
						enqueuePriority(pending.priority(), pending.key(), pending.runnable());
					} else {
						super.asyncExec(runnable); //FLLDCIM: MARK — T08 UI_THREAD_SYNCHRONIZER CALL
					}
				} catch (RuntimeException e) {
					// do nothing
				}
			}
			pendingStartup = null;
			// wake up all pending syncExecs
			this.notifyAll();
		}
	}

	@Override
	protected void asyncExec(Runnable runnable) {
		if (runnable != null) {
			synchronized (this) {
				if (deferDuringStartup(runnable)) {

					// don't run it now, add it to the list of deferred runnables
					pendingStartup.add(runnable);

					return;
				}
			}
		}
		super.asyncExec(runnable); //FLLDCIM: MARK — T08 UI_THREAD_SYNCHRONIZER CALL
	}

	/**
	 * Preserves startup admission for SWT's optional keyed dispatch API. On an SWT
	 * without priority dispatch, callbacks use the ordinary asynchronous queue.
	 * This overload intentionally compiles against both SWT API shapes.
	 */
	public void asyncExec(Object key, Runnable runnable) {
		asyncExec(DEFAULT_PRIORITY, key, runnable);
	}

	/**
	 * Preserves the original callback's startup eligibility and retains its priority
	 * and coalescing key until startup finishes. Only the eventual UI presentation
	 * callback belongs here; backend computation remains on its worker thread.
	 * This overload intentionally compiles against both SWT API shapes.
	 */
	public void asyncExec(int priority, Object key, Runnable runnable) {
		if (runnable == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		if (priority < FIRST_PRIORITY || priority > LAST_PRIORITY) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		synchronized (this) {
			if (deferDuringStartup(runnable)) {
				pendingStartup.add(new PendingAsync(priority, key, runnable));
				return;
			}
		}
		enqueuePriority(priority, key, runnable);
	}

	/** Called while holding this synchronizer's startup monitor. */
	private boolean deferDuringStartup(Runnable runnable) {
		return isStarting && !(runnable instanceof StartupRunnable) && overrideThread.get() == Boolean.FALSE;
	}

	private static MethodHandle priorityAsyncExec() {
		try {
			return MethodHandles.lookup().findSpecial(Synchronizer.class, "asyncExec", //$NON-NLS-1$
					MethodType.methodType(void.class, int.class, Object.class, Runnable.class), UISynchronizer.class);
		} catch (NoSuchMethodException e) {
			// Upstream SWT still supplies the original asynchronous dispatch contract.
			return null;
		} catch (IllegalAccessException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private void enqueuePriority(int priority, Object key, Runnable runnable) {
		if (PRIORITY_ASYNC_EXEC == null) {
			super.asyncExec(runnable);
			return;
		}
		try {
			// Invoke the superclass implementation without redispatching to this overload.
			PRIORITY_ASYNC_EXEC.invokeExact(this, priority, key, runnable);
		} catch (RuntimeException | Error e) {
			throw e;
		} catch (Throwable e) {
			throw new IllegalStateException(e);
		}
	}

	@Override //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
	public void syncExec(Runnable runnable) {

		synchronized (this) {
			if (isStarting && startupThread.get() == Boolean.FALSE
					&& overrideThread.get() == Boolean.FALSE) {
				do { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
					try {
						this.wait();
					} catch (InterruptedException e) {
					}
				} while (isStarting);
			}
		}

		// if this thread is the UI or this thread does not own any locks, just do the
		// syncExec
		if ((runnable == null) || lockListener.isUI() || !lockListener.isLockOwner()) {
			super.syncExec(runnable); //FLLDCIM: MARK — T08 UI_THREAD_SYNCHRONIZER CALL
			return;
		}
		PendingSyncExec work = new PendingSyncExec(runnable);
		work.setOperationThread(Thread.currentThread());
		lockListener.addPendingWork(work);
		asyncExec(lockListener::doPendingWork); //FLLDCIM: MARK — T08 UI_THREAD_SYNCHRONIZER CALL

		try {
			work.waitUntilExecuted(lockListener);
		} catch (InterruptedException e) {
		}
	}
}
