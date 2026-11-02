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
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package org.eclipse.jface.viewers.m3;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jface.viewers.AbstractListViewer;
import org.eclipse.jface.viewers.AbstractTableViewer;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.StructuredViewerInternals;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

/**
 * Entry points that apply {@link M3ViewerDelta}s to structured viewers: the
 * event stream of a view becomes deltas, never a whole-viewer refresh.
 * <ul>
 * <li>{@link #apply} applies a delta now, on the UI thread, with consecutive
 * operations of one kind batched into one viewer call;</li>
 * <li>{@link #post(StructuredViewer, M3ViewerDelta, int)} queues a delta from any
 * thread. Deltas posted in a burst are merged and applied once, after
 * {@code delayMillis} without a newer post
 * (rescheduled debounce), and at the latest after four initial delays;</li>
 * <li>{@link #flush} applies a queued delta at once. The viewers call it
 * before every {@code refresh}, so a refresh never runs ahead of queued
 * deltas.</li>
 * </ul>
 * Supported viewers: {@link AbstractTreeViewer}, {@link AbstractTableViewer},
 * {@link AbstractListViewer}. Any other structured viewer is refreshed at the
 * first operation (the stock behaviour).
 *
 * @since 3.43 (M3 enhanced API)
 */
public final class M3Viewers {

	private static final Logger LOG = Logger.getLogger(M3Viewers.class.getName());
	private static final String KEY = "org.eclipse.jface.viewers.m3.M3Viewers.queue"; //$NON-NLS-1$

	/** Batches of at least this many operations run with redraw off. */
	static final int REDRAW_BATCH = 64;

	/** Whether any delta was ever posted; until then {@link #flush} returns at once. */
	private static volatile boolean posted;

	private M3Viewers() {
	}

	/**
	 * Applies {@code delta} to {@code viewer} now. Must run on the viewer's UI
	 * thread. ComboViewer UPDATE runs preserve their logical selection, including
	 * explicit selection requests made by callbacks.
	 *
	 * @param viewer the viewer
	 * @param delta  the delta; it is not changed
	 */
	public static void apply(StructuredViewer viewer, M3ViewerDelta delta) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		Control control = viewer.getControl();
		if (control == null || control.isDisposed() || delta.isEmpty()) {
			return;
		}
		if (!(viewer instanceof AbstractTreeViewer || viewer instanceof AbstractTableViewer
				|| viewer instanceof AbstractListViewer)) {
			viewer.refresh(); //FLLDCIM: MARK — M21 FULL_REBUILD_TO_DELTA CALL
			return;
		}
		boolean bulk = delta.size() >= REDRAW_BATCH;
		if (bulk) {
			control.setRedraw(false);
		}
		try {
			for (int i = 0; i < delta.size();) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				i = run(viewer, delta, i);
			}
		} finally {
			if (bulk && !control.isDisposed()) {
				control.setRedraw(true);
			}
		}
	}

	/**
	 * Applies {@code delta} now, preserving the viewer's selection across the
	 * complete sequence. A selected element may be removed and reinserted by the
	 * patch without exposing an intermediate selection change. An explicit
	 * selection set by a callback takes precedence over restoring the old one.
	 * Nested calls join the current selection boundary.
	 * <p>
	 * Checkbox viewers use their existing check-state preservation, including
	 * the authority of an installed check-state provider. This method does not
	 * save or restore a tree's expansion state. Exceptions propagate as for
	 * {@link #apply}; a partially applied delta is not rolled back.
	 * </p>
	 * <p>
	 * Must run on the viewer's UI thread. This entry point preserves selection
	 * across the complete delta. Ordinary {@link #apply} and
	 * {@link #post(StructuredViewer, M3ViewerDelta, int)} preserve ComboViewer
	 * selection within each UPDATE run.
	 * </p>
	 *
	 * @param viewer the viewer
	 * @param delta  the delta; it is not changed
	 * @since 3.43 (M3 enhanced API)
	 */
	public static void applyPreservingSelection(StructuredViewer viewer, M3ViewerDelta delta) {
		Control control = viewer.getControl();
		if (control == null || control.isDisposed() || delta.isEmpty()) {
			return;
		}
		StructuredViewerInternals.preservingSelection(viewer, () -> apply(viewer, delta));
	}

	/* applies the run of operations starting at i; returns the index after it */
	private static int run(StructuredViewer viewer, M3ViewerDelta delta, int i) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		byte kind = delta.kind(i);
		int end = i + 1;
		switch (kind) {
		case M3ViewerDelta.ADD -> {
			if (delta.index(i) >= 0) {
				insert(viewer, delta.parent(i), delta.element(i), delta.index(i));
				return end;
			}
			while (end < delta.size() && delta.kind(end) == M3ViewerDelta.ADD && delta.index(end) < 0 //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
					&& delta.parent(end) == delta.parent(i)) {
				end++;
			}
			add(viewer, delta.parent(i), elements(delta, i, end));
		}
		case M3ViewerDelta.REMOVE -> {
			while (end < delta.size() && delta.kind(end) == M3ViewerDelta.REMOVE) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				end++;
			}
			remove(viewer, elements(delta, i, end));
		}
		case M3ViewerDelta.UPDATE -> {
			while (end < delta.size() && delta.kind(end) == M3ViewerDelta.UPDATE //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
					&& Arrays.equals(delta.properties(end), delta.properties(i))) {
				end++;
			}
			Object[] updated = elements(delta, i, end);
			String[] properties = delta.properties(i);
			if (viewer instanceof ComboViewer) {
				StructuredViewerInternals.preservingSelection(viewer, () -> viewer.update(updated, properties));
			} else {
				viewer.update(updated, properties);
			}
		}
		default -> {
			if (delta.element(i) == null) {
				viewer.refresh(); //FLLDCIM: MARK — M21 FULL_REBUILD_TO_DELTA CALL
			} else {
				viewer.refresh(delta.element(i)); //FLLDCIM: MARK — M21 FULL_REBUILD_TO_DELTA CALL
			}
		}
		}
		return end;
	}

	private static Object[] elements(M3ViewerDelta delta, int from, int to) {
		Object[] result = new Object[to - from];
		for (int k = from; k < to; k++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			result[k - from] = delta.element(k);
		}
		return result;
	}

	private static void insert(StructuredViewer viewer, Object parent, Object element, int index) {
		if (viewer instanceof AbstractTreeViewer tree) {
			// the model computed the position and applied the filters: the stock insert would append under a filter
			StructuredViewerInternals.insertAt(tree, parent, element, index);
		} else if (viewer instanceof AbstractTableViewer table) {
			table.insert(element, index);
		} else {
			((AbstractListViewer) viewer).insert(element, index);
		}
	}

	private static void add(StructuredViewer viewer, Object parent, Object[] elements) {
		if (viewer instanceof AbstractTreeViewer tree) {
			tree.add(parent, elements);
		} else if (viewer instanceof AbstractTableViewer table) {
			table.add(elements);
		} else {
			((AbstractListViewer) viewer).add(elements);
		}
	}

	private static void remove(StructuredViewer viewer, Object[] elements) {
		if (viewer instanceof AbstractTreeViewer tree) {
			tree.remove(elements);
		} else if (viewer instanceof AbstractTableViewer table) {
			table.remove(elements);
		} else {
			((AbstractListViewer) viewer).remove(elements);
		}
	}

	/** Nanoseconds per millisecond, shared by elapsed-time and timer conversions. */
	private static final long NANOS_PER_MILLISECOND = 1_000_000L;

	/** One parent-owned batch; only its admission metadata is shared by threads. */
	private static final class Queue implements Runnable {
		final StructuredViewer viewer;
		final Display display;
		final Runnable timer;
		final Runnable activation;
		M3ViewerDelta pending;
		long firstPostNanos;
		long maximumWaitNanos;
		long dueNanos;
		boolean lastRefreshLater;
		boolean activationQueued;
		boolean draining;
		boolean disposed;

		Queue(StructuredViewer viewer, Display display) {
			this.viewer = viewer;
			this.display = display;
			timer = () -> wake(this);
			activation = () -> activate(this);
		}

		@Override
		public void run() {
			dispose(this);
		}
	}

	/**
	 * Queues {@code delta} for {@code viewer}; it may be called from any thread.
	 * Deltas posted within {@code delayMillis} of each other are merged; the
	 * merged delta is applied on the UI thread after {@code delayMillis} without
	 * a newer post, and at the latest after four times the burst's first delay.
	 * UI and worker posts enter the same batch in admission order. Widget
	 * delivery is asynchronous unless an explicit flush or refresh drains it.
	 * A worker does not read viewer controls or dispatch display events. A
	 * custom viewer without a published owning display must use
	 * {@link #post(Display, StructuredViewer, M3ViewerDelta, int)} from a worker.
	 *
	 * @param viewer      the viewer
	 * @param delta       the delta; the caller must not change it afterwards
	 * @param delayMillis the debounce delay, 0 or more
	 * @throws IllegalStateException if a worker posts to a custom viewer whose
	 *         owning display is not known
	 */
	public static void post(StructuredViewer viewer, M3ViewerDelta delta, int delayMillis) {
		post(viewer, delta, delayMillis, false);
	}

	/**
	 * Posts using the explicitly supplied owning display. This also supports a
	 * custom viewer with an already-created control that has not published its
	 * display through the control hook.
	 *
	 * @param display the viewer's owning display
	 * @param viewer the target viewer
	 * @param delta the operations; do not modify after posting
	 * @param delayMillis the debounce delay, zero or greater
	 * @throws IllegalArgumentException if the display differs from the known owner
	 * @since 3.43
	 */
	public static void post(Display display, StructuredViewer viewer, M3ViewerDelta delta, int delayMillis) {
		post(display, viewer, delta, delayMillis, false);
	}

	private static void post(StructuredViewer viewer, M3ViewerDelta delta, int delayMillis, boolean refreshLater) {
		if (delta.isEmpty()) {
			return;
		}
		Display display = StructuredViewerInternals.getM3Display(viewer);
		if (display == null) {
			Runnable state = StructuredViewerInternals.getM3DeltaQueue(viewer);
			if (state instanceof Queue queue) {
				display = queue.display;
			} else {
				if (Display.getCurrent() == null) {
					throw new IllegalStateException("worker post requires the viewer's owning display"); //$NON-NLS-1$
				}
				Control control = viewer.getControl();
				if (control == null || control.isDisposed()) {
					return;
				}
				display = control.getDisplay();
			}
		}
		post(display, viewer, delta, delayMillis, refreshLater);
	}

	private static void post(Display display, StructuredViewer viewer, M3ViewerDelta delta,
			int delayMillis, boolean refreshLater) {
		if (delta.isEmpty() || display.isDisposed()) {
			return;
		}
		Display owner = StructuredViewerInternals.getM3Display(viewer);
		if (owner != null && owner != display) {
			throw new IllegalArgumentException("display does not own the viewer"); //$NON-NLS-1$
		}
		Runnable state = StructuredViewerInternals.getM3DeltaQueue(viewer);
		if (state == null) {
			state = StructuredViewerInternals.installM3DeltaQueue(viewer, new Queue(viewer, display));
		}
		if (state == null) {
			return;
		}
		Queue queue = (Queue) state;
		if (queue.display != display) {
			throw new IllegalArgumentException("display differs from the viewer queue owner"); //$NON-NLS-1$
		}
		boolean activate;
		synchronized (queue) {
			if (queue.disposed) {
				return;
			}
			long now = System.nanoTime();
			long delayNanos = Math.max(0, delayMillis) * NANOS_PER_MILLISECOND;
			if (queue.pending == null) {
				queue.pending = new M3ViewerDelta().append(delta);
				queue.firstPostNanos = now;
				queue.maximumWaitNanos = 4L * delayNanos;
			} else if (!refreshLater || !queue.lastRefreshLater) {
				queue.pending.append(delta);
			}
			queue.lastRefreshLater = refreshLater;
			long remaining = queue.maximumWaitNanos - (now - queue.firstPostNanos);
			queue.dueNanos = now + Math.max(0L, Math.min(delayNanos, remaining));
			activate = !queue.activationQueued;
			queue.activationQueued = true;
			posted = true;
		}
		if (activate) {
			try {
				display.asyncExec(queue.activation);
			} catch (SWTException failure) {
				// A rejected activation must not retain an unreachable pending batch.
				dispose(queue);
				throw failure;
			}
		}
	}

	/* UI thread: aliases the parent-owned queue to its original control lifetime. */
	private static boolean bind(Queue queue) {
		if (StructuredViewerInternals.getM3DeltaQueue(queue.viewer) != queue) {
			dispose(queue);
			return false;
		}
		Control control = queue.viewer.getControl();
		if (control == null || control.isDisposed() || control.getDisplay() != queue.display) {
			dispose(queue);
			return false;
		}
		if (control.getData(KEY) != queue) {
			control.setData(KEY, queue);
			control.addDisposeListener(event -> dispose(queue));
		}
		return true;
	}

	private static void activate(Queue queue) {
		synchronized (queue) {
			queue.activationQueued = false;
			if (queue.disposed) {
				return;
			}
		}
		if (bind(queue)) {
			schedule(queue);
		}
	}

	/* Reusing the runnable replaces its previous SWT timer schedule. */
	private static void schedule(Queue queue) {
		long remaining;
		synchronized (queue) {
			if (queue.disposed || queue.pending == null || queue.draining) {
				return;
			}
			remaining = Math.max(0L, queue.dueNanos - System.nanoTime());
		}
		long millis = (remaining + NANOS_PER_MILLISECOND - 1) / NANOS_PER_MILLISECOND;
		queue.display.timerExec((int) Math.min(Integer.MAX_VALUE, millis), queue.timer);
	}

	/* A stale invocation checks the current batch's deadline before doing work. */
	private static void wake(Queue queue) {
		boolean due;
		synchronized (queue) {
			if (queue.disposed || queue.pending == null || queue.draining) {
				return;
			}
			due = queue.dueNanos - System.nanoTime() <= 0;
		}
		if (due) {
			drain(queue, true);
		} else {
			schedule(queue);
		}
	}

	private static void cancelTimer(Queue queue) {
		if (!queue.display.isDisposed()) {
			queue.display.timerExec(-1, queue.timer);
		}
	}

	/* Disposal also closes posts admitted before the first UI activation. */
	private static void dispose(Queue queue) {
		synchronized (queue) {
			queue.disposed = true;
			queue.pending = null;
			queue.lastRefreshLater = false;
			queue.activationQueued = false;
		}
		if (Display.getCurrent() == queue.display) {
			cancelTimer(queue);
		}
	}

	private static void drain(Queue queue, boolean onlyIfDue) {
		M3ViewerDelta delta;
		synchronized (queue) {
			if (queue.draining || queue.disposed || queue.pending == null) {
				return;
			}
			// A worker may have extended the deadline since wake observed it.
			if (onlyIfDue && queue.dueNanos - System.nanoTime() > 0) {
				return;
			}
			delta = queue.pending;
			queue.pending = null;
			queue.lastRefreshLater = false;
			queue.draining = true;
		}
		try {
			cancelTimer(queue);
			apply(queue.viewer, delta);
		} catch (RuntimeException e) {
			LOG.log(Level.WARNING, "viewer delta failed; refreshing the viewer", e); //$NON-NLS-1$
			Control control = queue.viewer.getControl();
			if (control != null && !control.isDisposed()) {
				queue.viewer.refresh();
			}
		} finally {
			synchronized (queue) {
				queue.draining = false;
			}
			schedule(queue);
		}
	}

	/**
	 * Applies every delta admitted before this flush, if any, now. The viewers
	 * call this before refresh. Off-thread flush is a no-op. A reentrant flush
	 * leaves later posts for the next batch and never dispatches display events.
	 *
	 * @param viewer the viewer
	 */
	public static void flush(StructuredViewer viewer) {
		if (!posted) {
			return;
		}
		Display current = Display.getCurrent();
		if (current == null) {
			return;
		}
		Runnable state = StructuredViewerInternals.getM3DeltaQueue(viewer);
		if (state instanceof Queue queue && queue.display == current && bind(queue)) {
			drain(queue, false);
		}
	}

	/**
	 * Schedules a refresh of {@code viewer} through the same debounce: adjacent
	 * calls refresh once. An intervening explicit
	 * {@link #post(StructuredViewer, M3ViewerDelta, int)} keeps its place
	 * in the operation sequence and starts a new refresh group.
	 *
	 * @param viewer      the viewer
	 * @param delayMillis the debounce delay
	 */
	public static void refreshLater(StructuredViewer viewer, int delayMillis) {
		post(viewer, new M3ViewerDelta().refresh(null), delayMillis, true);
	}
}
		// preserved predecessor annotation: //FLLDCIM: MARK — T08 UI_THREAD_SYNCHRONIZER CALL
	// preserved predecessor annotation: //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		// preserved predecessor annotation: //FLLDCIM: MARK — T08 UI_THREAD_SYNCHRONIZER CALL
				// preserved predecessor annotation: //FLLDCIM: MARK — M21 FULL_REBUILD_TO_DELTA CALL
	// preserved predecessor annotation: //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
