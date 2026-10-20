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
package org.eclipse.ui.internal.views.markers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.m3.M3ViewerDelta;
import org.eclipse.jface.viewers.m3.M3Viewers;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.views.markers.internal.MarkerGroup;
import org.eclipse.ui.views.markers.internal.MarkerGroupingEntry;

/**
 * The delta path of the markers views ({@code -Dorg.eclipse.ui.ide.markers.mode=viewport}).
 * <p>
 * The original view answers every marker change with a clean rebuild: a new
 * {@link MarkerEntry} for every marker in scope, every filter evaluated again,
 * every category regrouped and sorted, and the whole tree refreshed. In M3 mode
 * the model is {@link M3MarkerLanes} and this class feeds it the
 * {@link IMarkerDelta}s of each resource change event:
 * </p>
 * <ol>
 * <li>{@link #offer} records the applicable deltas by marker id (lanes, merged
 * per id) and schedules one debounced job; a clean rebuild supersedes them
 * (generation counter);</li>
 * <li>the job (worker thread) reads keys, filters and categories of the changed
 * markers only (one attribute lookup per marker);</li>
 * <li>one {@code asyncExec} applies them on the UI thread: each changed marker
 * leaves and re-enters its category's order (O(log n) compares, at most one
 * block of slots moved), the counters follow, and the tree is patched inside
 * the shown windows only, through one {@link M3ViewerDelta}. Outside a window
 * only the category label (its count) and the title change.</li>
 * </ol>
 * The original clean rebuild still runs for the first input and for changes of
 * filters, grouping, sorting or scope, and when the view is in a state the lanes
 * do not cover (flat presentation, an active search text, a resource scope,
 * more than {@link #MAX_PENDING} pending deltas, an error). Design:
 * {@code docs/m3/markers-view/DESIGN.md}, {@code FALLBACKS_TO_DELTAS.md}.
 */
final class M3MarkerDeltas {

	/** The property that turns the delta path on. */
	static final String PROPERTY = "org.eclipse.ui.ide.markers.mode"; //$NON-NLS-1$

	/** Whether the delta path is on: {@code -Dorg.eclipse.ui.ide.markers.mode=viewport}. */
	static final boolean ENABLED = "viewport".equals(System.getProperty(PROPERTY)); //$NON-NLS-1$

	/** Above this many pending deltas a clean rebuild runs instead (to be measured, FALLBACKS_TO_DELTAS.md). */
	static final int MAX_PENDING = 50_000;

	/** The debounce of the original update scheduler (MarkerUpdateScheduler.SHORT_DELAY), kept for the delta job. */
	static final long DEBOUNCE_MILLIS = 150;

	private static final Logger LOG = Logger.getLogger(M3MarkerDeltas.class.getName());
	private static final Map<CachedMarkerBuilder, M3MarkerDeltas> BY_BUILDER = new WeakHashMap<>();
	private static final byte NONE = 0;

	/** Counters for the differential and the benchmark: see {@link #counters()}. */
	private static final int[] COUNTERS = new int[3];
	private static final int APPLIED = 0;
	private static final int FALLBACKS = 1;
	private static final int BUILDS = 2;

	private final MarkersChangeListener listener;
	private final CachedMarkerBuilder builder;
	private final Job job;

	/* pending deltas: lanes indexed by slot, plus an open-addressing id -> slot+1 table */
	private long[] ids = new long[16];
	private byte[] kinds = new byte[16];
	private IMarker[] markers = new IMarker[16];
	private int size;
	private int[] table = new int[32];
	private long generation;

	private M3MarkerDeltas(MarkersChangeListener listener, CachedMarkerBuilder builder) {
		this.listener = listener;
		this.builder = builder;
		job = Job.create("M3 marker deltas", this::prepare); //$NON-NLS-1$
		job.setSystem(true);
		job.setPriority(Job.SHORT);
	}

	/**
	 * The counters the differential and the benchmark read: deltas applied
	 * through the lanes, fallbacks to a clean rebuild, clean rebuilds started.
	 *
	 * @return {@code "applied=<n> fallbacks=<n> builds=<n>"}
	 */
	static synchronized String counters() {
		return "applied=" + COUNTERS[APPLIED] + " fallbacks=" + COUNTERS[FALLBACKS] + " builds=" + COUNTERS[BUILDS]; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	private static synchronized void count(int counter) {
		COUNTERS[counter]++;
	}

	/**
	 * Takes the marker deltas of {@code event} instead of a clean rebuild.
	 *
	 * @param listener the view's change listener
	 * @param event    the resource change event
	 * @param types    the marker types the view listens to
	 * @return {@code true} when the deltas were taken; {@code false} to run the
	 *         original clean rebuild
	 */
	static boolean offer(MarkersChangeListener listener, IResourceChangeEvent event, Set<String> types) {
		if (!ENABLED) {
			return false;
		}
		M3MarkerDeltas deltas;
		synchronized (BY_BUILDER) {
			deltas = BY_BUILDER.computeIfAbsent(listener.getBuilder(), b -> new M3MarkerDeltas(listener, b));
		}
		return deltas.accept(event, types);
	}

	/**
	 * A clean rebuild is starting: pending and prepared deltas are dropped, as the
	 * rebuild reads the markers afresh.
	 *
	 * @param builder the builder that starts a clean rebuild
	 */
	static void supersede(CachedMarkerBuilder builder) {
		if (!ENABLED) {
			return;
		}
		count(BUILDS);
		M3MarkerDeltas deltas;
		synchronized (BY_BUILDER) {
			deltas = BY_BUILDER.get(builder);
		}
		if (deltas != null) {
			deltas.clear();
		}
	}

	/**
	 * Builds the lane model of a clean rebuild from the gathered entries (the
	 * entries the filters selected); they are not kept.
	 *
	 * @param model   the view's model
	 * @param entries the selected entries
	 * @param monitor the rebuild's monitor
	 * @return the lanes, or {@code null} when the view is in a state the lanes
	 *         do not cover or the rebuild was cancelled
	 */
	static M3MarkerLanes build(Markers model, Collection<MarkerEntry> entries, IProgressMonitor monitor) {
		CachedMarkerBuilder builder = model.getBuilder();
		M3MarkerLanes lanes = M3MarkerLanes.create(model, builder.getComparator());
		if (lanes == null) {
			return null;
		}
		MarkerGroup group = builder.getCategoryGroup();
		M3MarkerLanes.Change change = new M3MarkerLanes.Change();
		M3MarkerKeys keys = null;
		int i = 0;
		for (MarkerEntry entry : entries) {
			if ((++i & 4095) == 0 && monitor.isCanceled()) {
				return null;
			}
			IMarker marker = entry.getMarker();
			if (marker == null) {
				continue;
			}
			try {
				MarkerGroupingEntry key = group.findGroupValue(marker.getType(), marker);
				keys = M3MarkerKeys.read(marker, lanes.needsType(), keys);
				lanes.add(change.set(keys.id, marker, keys, key, entry));
			} catch (CoreException e) {
				entry.checkIfMarkerStale(); // stock skips a stale marker when grouping (Markers.groupMarkerEntries)
			}
		}
		lanes.finishBuild();
		return lanes;
	}

	private synchronized boolean accept(IResourceChangeEvent event, Set<String> types) {
		if (!ready()) {
			clear();
			return false;
		}
		for (IMarkerDelta delta : event.findMarkerDeltas(null, true)) {
			if (types.contains(delta.getType())) {
				add(delta.getId(), (byte) delta.getKind(), delta.getMarker());
			}
		}
		if (size > MAX_PENDING) {
			clear();
			count(FALLBACKS);
			return false;
		}
		job.schedule(DEBOUNCE_MILLIS);
		return true;
	}

	/* the lane model is built and the view state is one the delta path covers */
	private boolean ready() {
		if (builder.isBuilding() || !builder.isShowingHierarchy() || builder.getGenerator() == null
				|| builder.getMarkers().m3 == null) {
			return false;
		}
		ExtendedMarkersView view = listener.getView();
		if (view == null || view.isDisposed() || view.getSearchFilter() != null) {
			return false;
		}
		for (MarkerFieldFilterGroup group : builder.getGenerator().getEnabledFilters()) {
			if (group.getScope() != MarkerFieldFilterGroup.ON_ANY) {
				return false;
			}
		}
		return true;
	}

	private void add(long id, byte kind, IMarker marker) {
		int slot = find(id);
		if (slot < 0) {
			grow();
			slot = size++;
			ids[slot] = id;
			kinds[slot] = kind;
			markers[slot] = marker;
			index(id, slot);
			return;
		}
		byte before = kinds[slot];
		if (before == IResourceDelta.ADDED) {
			kinds[slot] = kind == IResourceDelta.REMOVED ? NONE : IResourceDelta.ADDED;
		} else if (before == NONE) {
			kinds[slot] = kind;
		} else {
			kinds[slot] = (byte) (kind == IResourceDelta.REMOVED ? IResourceDelta.REMOVED : IResourceDelta.CHANGED);
		}
		markers[slot] = marker;
	}

	private int find(long id) {
		int mask = table.length - 1;
		for (int i = hash(id) & mask; table[i] != 0; i = (i + 1) & mask) {
			if (ids[table[i] - 1] == id) {
				return table[i] - 1;
			}
		}
		return -1;
	}

	private void index(long id, int slot) {
		if ((size << 1) > table.length) {
			table = new int[table.length << 1];
			for (int s = 0; s < size; s++) {
				put(ids[s], s);
			}
			return;
		}
		put(id, slot);
	}

	private void put(long id, int slot) {
		int mask = table.length - 1;
		int i = hash(id) & mask;
		while (table[i] != 0) {
			i = (i + 1) & mask;
		}
		table[i] = slot + 1;
	}

	private static int hash(long id) {
		return (int) ((id ^ (id >>> 32)) * 0x9E3779B97F4A7C15L >>> 32);
	}

	private void grow() {
		if (size == ids.length) {
			int capacity = size << 1;
			ids = Arrays.copyOf(ids, capacity);
			kinds = Arrays.copyOf(kinds, capacity);
			markers = Arrays.copyOf(markers, capacity);
		}
	}

	private synchronized void clear() {
		generation++;
		Arrays.fill(markers, 0, size, null);
		Arrays.fill(table, 0);
		size = 0;
	}

	/* worker thread: keys, filters and categories for the changed markers only */
	private IStatus prepare(IProgressMonitor monitor) {
		long gen;
		long[] batchIds;
		byte[] batchKinds;
		IMarker[] batchMarkers;
		int n;
		synchronized (this) {
			gen = generation;
			n = size;
			batchIds = Arrays.copyOf(ids, n);
			batchKinds = Arrays.copyOf(kinds, n);
			batchMarkers = Arrays.copyOf(markers, n);
			Arrays.fill(markers, 0, size, null);
			Arrays.fill(table, 0);
			size = 0;
		}
		List<M3MarkerLanes.Change> changes = new ArrayList<>(n);
		try {
			MarkerContentGenerator generator = builder.getGenerator();
			M3MarkerLanes lanes = builder.getMarkers().m3;
			if (generator == null || lanes == null) {
				return Status.OK_STATUS;
			}
			IResource[] selected = generator.getSelectedResources();
			Collection<MarkerFieldFilterGroup> filters = generator.getEnabledFilters();
			MarkerGroup group = builder.getCategoryGroup();
			for (int i = 0; i < n; i++) {
				if ((i & 4095) == 4095 && stale(gen)) {
					return Status.OK_STATUS;
				}
				if (batchKinds[i] != NONE) {
					changes.add(prepare(batchIds[i], batchKinds[i], batchMarkers[i], lanes, selected, filters,
							group));
				}
			}
		} catch (CoreException | RuntimeException e) {
			fallback(e);
			return Status.OK_STATUS;
		}
		Display display = Display.getDefault();
		display.asyncExec(() -> {
			try {
				apply(gen, changes);
			} catch (RuntimeException e) {
				fallback(e);
			}
		});
		return Status.OK_STATUS;
	}

	/* one changed marker: its keys and category when it is (still) selected by the filters, else it leaves */
	private static M3MarkerLanes.Change prepare(long id, byte kind, IMarker marker, M3MarkerLanes lanes,
			IResource[] selected, Collection<MarkerFieldFilterGroup> filters, MarkerGroup group)
			throws CoreException {
		M3MarkerLanes.Change change = new M3MarkerLanes.Change().set(id, marker, null, null, null);
		if (kind == IResourceDelta.REMOVED || !marker.exists()) {
			return change;
		}
		MarkerEntry candidate = new MarkerEntry(marker);
		if (!MarkerContentGenerator.select(candidate, selected, filters)) {
			return change;
		}
		MarkerGroupingEntry key = group.findGroupValue(marker.getType(), marker);
		candidate.clearCache();
		return change.set(id, marker, M3MarkerKeys.read(marker, lanes.needsType(), null), key, candidate);
	}

	private synchronized boolean stale(long gen) {
		return gen != generation;
	}

	private void fallback(Exception e) {
		LOG.log(Level.WARNING, "marker delta path failed, rebuilding the view", e); //$NON-NLS-1$
		count(FALLBACKS);
		clear();
		builder.getUpdateScheduler().scheduleUpdate();
	}

	/* UI thread: patch the lanes, then the tree rows inside the shown windows */
	private void apply(long gen, List<M3MarkerLanes.Change> changes) {
		if (stale(gen)) {
			return;
		}
		ExtendedMarkersView view = listener.getView();
		if (view == null || view.isDisposed() || builder.isBuilding() || !ready()) {
			return;
		}
		M3MarkerLanes lanes = builder.getMarkers().m3;
		M3LongSet changed = new M3LongSet(changes.size());
		for (M3MarkerLanes.Change c : changes) {
			changed.add(c.id);
		}
		List<M3MarkerLanes.Category> touched = lanes.apply(changes);
		count(APPLIED);
		if (view.getActiveViewerInputClone().m3 == lanes) {
			patchTree(view, lanes, touched, changed);
		} // else a rebuilt model waits for its UI update, which refreshes the tree from these lanes
		// UIUpdateJob: with no saved state, a single category is recorded for expansion
		if (view.getCategoriesToExpand().isEmpty()) {
			M3MarkerCategory[] categories = lanes.categories();
			if (categories.length == 1) {
				view.getCategoriesToExpand().add(categories[0].getDescription());
			}
		}
		view.reexpandCategories();
		view.updateTitle();
		view.updateStatusLine(view.getViewer().getStructuredSelection());
	}

	/* the tree patch: categories removed and added; per touched category its label and the rows of its window */
	private static void patchTree(ExtendedMarkersView view, M3MarkerLanes lanes,
			List<M3MarkerLanes.Category> touched, M3LongSet changed) {
		TreeViewer viewer = view.getViewer();
		Object input = viewer.getInput();
		M3ViewerDelta delta = new M3ViewerDelta();
		for (M3MarkerLanes.Category c : touched) {
			if (lanes.indexOf(c) < 0 && viewer.testFindItem(c.facade) != null) {
				delta.remove(c.facade);
			}
		}
		M3MarkerCategory[] categories = lanes.categories();
		List<M3MarkerCategory> expand = new ArrayList<>();
		for (int i = 0; i < categories.length; i++) {
			M3MarkerCategory category = categories[i];
			if (contains(touched, category) && viewer.testFindItem(category) == null) {
				delta.add(input, category, i);
				if (view.getCategoriesToExpand().contains(category.getName())) {
					expand.add(category);
				}
			}
		}
		int limit = limit(view);
		for (M3MarkerLanes.Category c : touched) {
			if (lanes.indexOf(c) < 0) {
				continue;
			}
			Widget widget = viewer.testFindItem(c.facade);
			if (widget instanceof TreeItem item) {
				delta.update(c.facade, null);
				patchWindow(delta, lanes, c, item, limit, changed);
			}
		}
		if (delta.isEmpty()) {
			return;
		}
		((MarkersTreeViewer) viewer).m3PreservingSelection(() -> M3Viewers.apply(viewer, delta));
		for (M3MarkerCategory category : expand) {
			viewer.setExpandedState(category, true);
		}
	}

	private static boolean contains(List<M3MarkerLanes.Category> touched, M3MarkerCategory category) {
		for (M3MarkerLanes.Category c : touched) {
			if (c.facade == category) {
				return true;
			}
		}
		return false;
	}

	/**
	 * MarkerViewerContentProvider.getLimitedChildren: the marker limit.
	 *
	 * @param view the view
	 * @return the limit, or -1 for none
	 */
	static int limit(ExtendedMarkersView view) {
		MarkerContentGenerator generator = view.getGenerator();
		int limit = generator.getMarkerLimits();
		return generator.isMarkerLimitsEnabled() && limit > 0 ? limit : -1;
	}

	/*
	 * Rows of one category whose items exist (expanded once): rows that left the
	 * window or changed are removed, rows that entered it (or changed) are inserted
	 * at their window index, in increasing order.
	 */
	private static void patchWindow(M3ViewerDelta delta, M3MarkerLanes lanes, M3MarkerLanes.Category c,
			TreeItem item, int limit, M3LongSet changed) {
		Item[] rows = item.getItems();
		if (rows.length == 0 || rows.length == 1 && rows[0].getData() == null) {
			return; // never expanded: the content provider gives the window on expansion
		}
		int[] next = lanes.windowSlots(c, limit);
		M3LongSet nextIds = new M3LongSet(next.length);
		for (int slot : next) {
			nextIds.add(lanes.id(slot));
		}
		M3LongSet kept = new M3LongSet(rows.length);
		for (Item row : rows) {
			if (row.getData() instanceof MarkerEntry entry && entry.getMarker() != null) {
				long id = entry.getMarker().getId();
				if (changed.contains(id) || !nextIds.contains(id)) {
					delta.remove(entry);
					int slot = lanes.find(id);
					if (slot >= 0 && !nextIds.contains(id)) {
						lanes.releaseFacade(slot);
					}
				} else {
					kept.add(id);
				}
			}
		}
		for (int j = 0; j < next.length; j++) {
			if (!kept.contains(lanes.id(next[j]))) {
				delta.add(c.facade, lanes.facade(next[j]), j);
			}
		}
	}
}
