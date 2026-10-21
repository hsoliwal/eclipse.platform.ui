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
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

/**
 * Entry points that apply {@link M3ViewerDelta}s to structured viewers: the
 * event stream of a view becomes deltas, never a whole-viewer refresh.
 * <ul>
 * <li>{@link #apply} applies a delta now, on the UI thread, with consecutive
 * operations of one kind batched into one viewer call;</li>
 * <li>{@link #post} queues a delta from any thread. Deltas posted in a burst
 * are merged and applied once, after {@code delayMillis} without a newer post
 * (superseding-generation debounce), and at the latest after four delays;</li>
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
	 * thread.
	 *
	 * @param viewer the viewer
	 * @param delta  the delta; it is not changed
	 */
	public static void apply(StructuredViewer viewer, M3ViewerDelta delta) {
		Control control = viewer.getControl();
		if (control == null || control.isDisposed() || delta.isEmpty()) {
			return;
		}
		if (!(viewer instanceof AbstractTreeViewer || viewer instanceof AbstractTableViewer
				|| viewer instanceof AbstractListViewer)) {
			viewer.refresh();
			return;
		}
		boolean bulk = delta.size() >= REDRAW_BATCH;
		if (bulk) {
			control.setRedraw(false);
		}
		try {
			for (int i = 0; i < delta.size();) {
				i = run(viewer, delta, i);
			}
		} finally {
			if (bulk && !control.isDisposed()) {
				control.setRedraw(true);
			}
		}
	}

	/* applies the run of operations starting at i; returns the index after it */
	private static int run(StructuredViewer viewer, M3ViewerDelta delta, int i) {
		byte kind = delta.kind(i);
		int end = i + 1;
		switch (kind) {
		case M3ViewerDelta.ADD -> {
			if (delta.index(i) >= 0) {
				insert(viewer, delta.parent(i), delta.element(i), delta.index(i));
				return end;
			}
			while (end < delta.size() && delta.kind(end) == M3ViewerDelta.ADD && delta.index(end) < 0
					&& delta.parent(end) == delta.parent(i)) {
				end++;
			}
			add(viewer, delta.parent(i), elements(delta, i, end));
		}
		case M3ViewerDelta.REMOVE -> {
			while (end < delta.size() && delta.kind(end) == M3ViewerDelta.REMOVE) {
				end++;
			}
			remove(viewer, elements(delta, i, end));
		}
		case M3ViewerDelta.UPDATE -> {
			while (end < delta.size() && delta.kind(end) == M3ViewerDelta.UPDATE
					&& Arrays.equals(delta.properties(end), delta.properties(i))) {
				end++;
			}
			viewer.update(elements(delta, i, end), delta.properties(i));
		}
		default -> {
			if (delta.element(i) == null) {
				viewer.refresh();
			} else {
				viewer.refresh(delta.element(i));
			}
		}
		}
		return end;
	}

	private static Object[] elements(M3ViewerDelta delta, int from, int to) {
		Object[] result = new Object[to - from];
		for (int k = from; k < to; k++) {
			result[k - from] = delta.element(k);
		}
		return result;
	}

	private static void insert(StructuredViewer viewer, Object parent, Object element, int index) {
		if (viewer instanceof AbstractTreeViewer tree) {
			tree.insert(parent, element, index);
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

	/** The queue of one viewer: the merged pending delta and the generation of the latest post. */
	private static final class Queue {
		final StructuredViewer viewer;
		M3ViewerDelta pending;
		long generation;
		long firstPost;

		Queue(StructuredViewer viewer) {
			this.viewer = viewer;
		}
	}

	/**
	 * Queues {@code delta} for {@code viewer}; it may be called from any thread.
	 * Deltas posted within {@code delayMillis} of each other are merged; the
	 * merged delta is applied on the UI thread after {@code delayMillis} without
	 * a newer post, and at the latest {@code 4 * delayMillis} after the first.
	 *
	 * @param viewer      the viewer
	 * @param delta       the delta; the caller must not change it afterwards
	 * @param delayMillis the debounce delay, 0 or more
	 */
	public static void post(StructuredViewer viewer, M3ViewerDelta delta, int delayMillis) {
		Control control = viewer.getControl();
		if (control == null || control.isDisposed() || delta.isEmpty()) {
			return;
		}
		posted = true;
		Display display = control.getDisplay();
		display.asyncExec(() -> enqueue(viewer, delta, Math.max(0, delayMillis)));
	}

	/* UI thread */
	private static void enqueue(StructuredViewer viewer, M3ViewerDelta delta, int delayMillis) {
		Control control = viewer.getControl();
		if (control == null || control.isDisposed()) {
			return;
		}
		Queue queue = (Queue) control.getData(KEY);
		if (queue == null) {
			queue = new Queue(viewer);
			control.setData(KEY, queue);
		}
		long now = System.currentTimeMillis();
		if (queue.pending == null) {
			queue.pending = new M3ViewerDelta().append(delta);
			queue.firstPost = now;
		} else {
			queue.pending.append(delta);
		}
		long generation = ++queue.generation;
		Queue q = queue;
		long overdue = q.firstPost + 4L * delayMillis - now;
		int wait = (int) Math.max(0, Math.min(delayMillis, overdue));
		control.getDisplay().timerExec(wait, () -> {
			if (q.generation == generation || System.currentTimeMillis() >= q.firstPost + 4L * delayMillis) {
				drain(q);
			}
		});
	}

	private static void drain(Queue queue) {
		M3ViewerDelta delta = queue.pending;
		queue.pending = null;
		if (delta == null) {
			return;
		}
		try {
			apply(queue.viewer, delta);
		} catch (RuntimeException e) {
			LOG.log(Level.WARNING, "viewer delta failed; refreshing the viewer", e); //$NON-NLS-1$
			Control control = queue.viewer.getControl();
			if (control != null && !control.isDisposed()) {
				queue.viewer.refresh();
			}
		}
	}

	/**
	 * Applies the delta queued for {@code viewer}, if any, now. The viewers call
	 * this before every refresh. Must run on the UI thread.
	 *
	 * @param viewer the viewer
	 */
	public static void flush(StructuredViewer viewer) {
		if (!posted) {
			return;
		}
		Control control = viewer.getControl();
		if (control == null || control.isDisposed() || control.getDisplay().getThread() != Thread.currentThread()) {
			return;
		}
		if (control.getData(KEY) instanceof Queue queue && queue.pending != null) {
			queue.generation++;
			drain(queue);
		}
	}

	/**
	 * Schedules a refresh of {@code viewer} through the same debounce: a burst
	 * of calls refreshes once.
	 *
	 * @param viewer      the viewer
	 * @param delayMillis the debounce delay
	 */
	public static void refreshLater(StructuredViewer viewer, int delayMillis) {
		post(viewer, new M3ViewerDelta().refresh(null), delayMillis);
	}
}
