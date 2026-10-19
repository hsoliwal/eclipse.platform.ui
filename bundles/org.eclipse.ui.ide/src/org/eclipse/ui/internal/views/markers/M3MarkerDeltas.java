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
import java.util.Comparator;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.views.markers.MarkerItem;
import org.eclipse.ui.views.markers.internal.MarkerGroup;
import org.eclipse.ui.views.markers.internal.MarkerGroupingEntry;

/**
 * The delta path of the markers views ({@code -Dorg.eclipse.ui.ide.markers.mode=viewport}).
 * <p>
 * The original view answers every marker change with a clean rebuild: a new
 * {@link MarkerEntry} for every marker in scope, every filter evaluated again,
 * every category regrouped and sorted, and the whole tree refreshed. This class
 * consumes the {@link IMarkerDelta}s of the resource change event instead:
 * </p>
 * <ol>
 * <li>{@link #offer} records the applicable deltas by marker id (lanes, merged
 * per id) and schedules one debounced job; a newer clean rebuild supersedes
 * them (generation counter);</li>
 * <li>the job (worker thread) creates entries only for added or changed
 * markers, evaluates the filters for them only and finds their category;</li>
 * <li>{@link #apply} (UI thread) patches the model with one merge pass per
 * category (references only, no attribute reads for unchanged markers) and
 * patches the tree: rows inside a category's shown window are removed,
 * inserted or relabelled; outside the window only the category label (its
 * count) and the title counts change.</li>
 * </ol>
 * Anything the delta path does not cover falls back to the original clean
 * rebuild: flat (ungrouped) presentation, an active search text, filters with
 * a resource scope, more than {@link #MAX_PENDING} pending deltas, or an error.
 * Design: {@code docs/m3/markers-view/DESIGN.md}.
 */
final class M3MarkerDeltas {

	/** The property that turns the delta path on. */
	static final String PROPERTY = "org.eclipse.ui.ide.markers.mode"; //$NON-NLS-1$

	/** Whether the delta path is on: {@code -Dorg.eclipse.ui.ide.markers.mode=viewport}. */
	static final boolean ENABLED = "viewport".equals(System.getProperty(PROPERTY)); //$NON-NLS-1$

	/** Above this many pending deltas a clean rebuild is cheaper. */
	static final int MAX_PENDING = 50_000;

	/** The debounce of the original update scheduler (MarkerUpdateScheduler.SHORT_DELAY), kept for the delta job. */
	static final long DEBOUNCE_MILLIS = 150;

	private static final Logger LOG = Logger.getLogger(M3MarkerDeltas.class.getName());
	private static final Map<CachedMarkerBuilder, M3MarkerDeltas> BY_BUILDER = new WeakHashMap<>();
	private static final byte NONE = 0;

	private final MarkersChangeListener listener;
	private final CachedMarkerBuilder builder;
	private final Job job;

	/* pending deltas: lanes indexed by slot, plus an open-addressing id -> slot+1 table */
	private long[] ids = new long[16];
	private byte[] kinds = new byte[16];
	private int[] oldSeverities = new int[16];
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
		M3MarkerDeltas deltas;
		synchronized (BY_BUILDER) {
			deltas = BY_BUILDER.get(builder);
		}
		if (deltas != null) {
			deltas.clear();
		}
	}

	/**
	 * Whether categories are sorted completely, not only their shown window: the
	 * delta path inserts by binary search.
	 *
	 * @return {@code true} when the delta path is on
	 */
	static boolean fullSort() {
		return ENABLED;
	}

	private synchronized boolean accept(IResourceChangeEvent event, Set<String> types) {
		if (!ready()) {
			clear();
			return false;
		}
		for (IMarkerDelta delta : event.findMarkerDeltas(null, true)) {
			if (types.contains(delta.getType())) {
				add(delta.getId(), (byte) delta.getKind(), delta.getMarker(),
						delta.getKind() == IResourceDelta.ADDED ? -2 : delta.getAttribute(IMarker.SEVERITY, -1));
			}
		}
		if (size > MAX_PENDING) {
			clear();
			return false;
		}
		job.schedule(DEBOUNCE_MILLIS);
		return true;
	}

	/* the model is built and the view state is one the delta path covers */
	private boolean ready() {
		if (builder.isBuilding() || !builder.isShowingHierarchy() || builder.getGenerator() == null) {
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

	private void add(long id, byte kind, IMarker marker, int oldSeverity) {
		int slot = find(id);
		if (slot < 0) {
			grow();
			slot = size++;
			ids[slot] = id;
			kinds[slot] = kind;
			markers[slot] = marker;
			oldSeverities[slot] = oldSeverity;
			index(id, slot);
			return;
		}
		byte before = kinds[slot];
		if (before == IResourceDelta.ADDED) {
			kinds[slot] = kind == IResourceDelta.REMOVED ? NONE : IResourceDelta.ADDED;
		} else if (before == NONE) {
			kinds[slot] = kind;
			oldSeverities[slot] = oldSeverity;
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
			oldSeverities = Arrays.copyOf(oldSeverities, capacity);
		}
	}

	private synchronized void clear() {
		generation++;
		Arrays.fill(markers, 0, size, null);
		Arrays.fill(table, 0);
		size = 0;
	}

	/** One prepared change: the marker, its new entry (or null when it leaves the view) and its category. */
	private record Change(long id, byte kind, MarkerEntry entry, String category, MarkerGroupingEntry key,
			int oldSeverity) {
	}

	/* worker thread: entries, filters and categories for the changed markers only */
	private IStatus prepare(IProgressMonitor monitor) {
		long gen;
		List<Change> changes = new ArrayList<>();
		long[] batchIds;
		byte[] batchKinds;
		IMarker[] batchMarkers;
		int[] batchSeverities;
		int n;
		synchronized (this) {
			gen = generation;
			n = size;
			batchIds = Arrays.copyOf(ids, n);
			batchKinds = Arrays.copyOf(kinds, n);
			batchMarkers = Arrays.copyOf(markers, n);
			batchSeverities = Arrays.copyOf(oldSeverities, n);
			Arrays.fill(markers, 0, size, null);
			Arrays.fill(table, 0);
			size = 0;
		}
		try {
			MarkerContentGenerator generator = builder.getGenerator();
			if (generator == null) {
				return Status.OK_STATUS;
			}
			IResource[] selected = generator.getSelectedResources();
			Collection<MarkerFieldFilterGroup> filters = generator.getEnabledFilters();
			MarkerGroup group = builder.getCategoryGroup();
			for (int i = 0; i < n; i++) {
				byte kind = batchKinds[i];
				if (kind == NONE) {
					continue;
				}
				IMarker marker = batchMarkers[i];
				MarkerEntry entry = null;
				MarkerGroupingEntry key = null;
				if (kind != IResourceDelta.REMOVED && marker.exists()) {
					MarkerEntry candidate = new MarkerEntry(marker);
					if (MarkerContentGenerator.select(candidate, selected, filters)) {
						entry = candidate;
						key = group.findGroupValue(marker.getType(), marker);
					}
					candidate.clearCache();
				}
				String category = entry == null ? null
						: group.getMarkerField().getValue(entry);
				changes.add(new Change(batchIds[i], kind, entry, category, key, batchSeverities[i]));
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

	private void fallback(Exception e) {
		LOG.log(Level.WARNING, "marker delta path failed, rebuilding the view", e); //$NON-NLS-1$
		clear();
		builder.getUpdateScheduler().scheduleUpdate();
	}

	/* UI thread: patch the model with one merge pass, then the tree rows inside the shown windows */
	private void apply(long gen, List<Change> changes) {
		synchronized (this) {
			if (gen != generation) {
				return;
			}
		}
		ExtendedMarkersView view = listener.getView();
		if (view == null || view.isDisposed() || builder.isBuilding() || !ready()) {
			return;
		}
		Markers model = builder.getMarkers();
		MarkerCategory[] oldCategories;
		MarkerCategory[] newCategories;
		synchronized (model) {
			oldCategories = model.getCategories();
			Patch patch = new Patch(model, changes);
			if (!patch.build()) {
				fallback(new IllegalStateException("marker delta does not fit the model")); //$NON-NLS-1$
				return;
			}
			newCategories = patch.categories;
			model.m3Replace(patch.entries, newCategories, patch.counts);
			Markers clone = builder.getClonedMarkers();
			if (clone != model) {
				clone.m3Replace(patch.entries, newCategories, patch.counts);
			}
		}
		patchTree(view, oldCategories, newCategories, changes);
		view.updateTitle();
	}

	/** The model patch: new entry array, categories and counts from the old ones and the changes. */
	private final class Patch {
		final Markers model;
		final List<Change> changes;
		MarkerEntry[] entries;
		MarkerCategory[] categories;
		Integer[] counts;

		Patch(Markers model, List<Change> changes) {
			this.model = model;
			this.changes = changes;
		}

		boolean build() {
			MarkerEntry[] old = model.getMarkerEntryArray();
			MarkerCategory[] oldCategories = model.getCategories();
			Integer[] oldCounts = model.getMarkerCounts();
			int[] counts4 = { oldCounts[0].intValue(), oldCounts[1].intValue(), oldCounts[2].intValue(),
					oldCounts[3].intValue() };
			// one pass: which old positions leave (removed or changed markers)
			M3LongSet changedIds = new M3LongSet(changes.size());
			for (Change c : changes) {
				changedIds.add(c.id);
			}
			boolean[] leaving = new boolean[old.length];
			int left = 0;
			for (int i = 0; i < old.length; i++) {
				IMarker marker = old[i].getMarker();
				if (marker != null && changedIds.contains(marker.getId())) {
					leaving[i] = true;
					left++;
				}
			}
			for (Change c : changes) {
				if (c.oldSeverity != -2 && c.kind != IResourceDelta.ADDED) {
					// counted before only when it was in the view; the scan above found it there
					if (wasShown(old, leaving, c.id)) {
						counts4[countIndex(c.oldSeverity)]--;
					}
				}
				if (c.entry != null) {
					Object severity = c.entry.getAttributeValue(IMarker.SEVERITY);
					counts4[countIndex(severity instanceof Integer s ? s.intValue() : -1)]++;
				}
			}
			// group the arriving entries by category name, each group sorted
			Comparator<MarkerItem> order = builder.getComparator().getFieldsComparator();
			List<String> names = new ArrayList<>();
			List<List<MarkerEntry>> arriving = new ArrayList<>();
			List<MarkerGroupingEntry> keys = new ArrayList<>();
			for (Change c : changes) {
				if (c.entry == null) {
					continue;
				}
				int at = names.indexOf(c.category);
				if (at < 0) {
					names.add(c.category);
					keys.add(c.key);
					arriving.add(new ArrayList<>());
					at = names.size() - 1;
				}
				arriving.get(at).add(c.entry);
			}
			for (List<MarkerEntry> list : arriving) {
				list.sort(order);
			}
			// category order: existing categories keep their order; new ones go by the group comparator
			MarkerGroup group = builder.getCategoryGroup();
			Comparator<MarkerGroupingEntry> categoryOrder = group.getEntriesComparator();
			List<String> orderNames = new ArrayList<>();
			List<MarkerGroupingEntry> orderKeys = new ArrayList<>();
			for (MarkerCategory category : oldCategories) {
				orderNames.add(category.getName());
				orderKeys.add(keyOf(old, category, leaving, keys, names));
			}
			for (int i = 0; i < names.size(); i++) {
				if (!orderNames.contains(names.get(i))) {
					MarkerGroupingEntry key = keys.get(i);
					int at = 0;
					while (at < orderKeys.size() && orderKeys.get(at) != null
							&& categoryOrder.compare(orderKeys.get(at), key) <= 0) {
						at++;
					}
					orderNames.add(at, names.get(i));
					orderKeys.add(at, key);
				}
			}
			int total = old.length - left;
			for (List<MarkerEntry> list : arriving) {
				total += list.size();
			}
			entries = new MarkerEntry[total];
			List<MarkerCategory> made = new ArrayList<>();
			int out = 0;
			for (String name : orderNames) {
				MarkerCategory before = categoryNamed(oldCategories, name);
				int at = names.indexOf(name);
				List<MarkerEntry> in = at < 0 ? List.of() : arriving.get(at);
				int start = out;
				int i = before == null ? 0 : before.start;
				int end = before == null ? -1 : before.end;
				int j = 0;
				while (i <= end || j < in.size()) {
					while (i <= end && leaving[i]) {
						i++;
					}
					if (i > end) {
						while (j < in.size()) {
							entries[out++] = in.get(j++);
						}
						break;
					}
					if (j < in.size() && order.compare(in.get(j), old[i]) < 0) {
						entries[out++] = in.get(j++);
					} else {
						entries[out++] = old[i++];
					}
				}
				if (out > start) {
					made.add(new MarkerCategory(model, start, out - 1, name));
				}
			}
			if (out != total) {
				return false;
			}
			categories = made.toArray(new MarkerCategory[0]);
			counts = new Integer[] { Integer.valueOf(counts4[0]), Integer.valueOf(counts4[1]),
					Integer.valueOf(counts4[2]), Integer.valueOf(counts4[3]) };
			return true;
		}

		private boolean wasShown(MarkerEntry[] old, boolean[] leaving, long id) {
			for (int i = 0; i < old.length; i++) {
				if (leaving[i] && old[i].getMarker() != null && old[i].getMarker().getId() == id) {
					return true;
				}
			}
			return false;
		}

		/* the grouping key of an existing category, from an entry that stays (its attributes are unchanged) */
		private MarkerGroupingEntry keyOf(MarkerEntry[] old, MarkerCategory category, boolean[] leaving,
				List<MarkerGroupingEntry> keys, List<String> names) {
			int at = names.indexOf(category.getName());
			if (at >= 0) {
				return keys.get(at);
			}
			for (int i = category.start; i <= category.end; i++) {
				IMarker marker = old[i].getMarker();
				if (!leaving[i] && marker != null) {
					try {
						return builder.getCategoryGroup().findGroupValue(marker.getType(), marker);
					} catch (CoreException e) {
						return null;
					}
				}
			}
			return null;
		}
	}

	/** Severity to the index of {@link Markers#getMarkerCounts()}: errors, warnings, infos, others. */
	static int countIndex(int severity) {
		return switch (severity) {
		case IMarker.SEVERITY_ERROR -> 0;
		case IMarker.SEVERITY_WARNING -> 1;
		case IMarker.SEVERITY_INFO -> 2;
		default -> 3;
		};
	}

	private static MarkerCategory categoryNamed(MarkerCategory[] categories, String name) {
		for (MarkerCategory category : categories) {
			if (category.getName().equals(name)) {
				return category;
			}
		}
		return null;
	}

	/* the tree patch: categories added/removed; per expanded category only the rows of its shown window */
	private void patchTree(ExtendedMarkersView view, MarkerCategory[] oldCategories, MarkerCategory[] newCategories,
			List<Change> changes) {
		TreeViewer viewer = view.getViewer();
		Object input = viewer.getInput();
		M3LongSet changed = new M3LongSet(changes.size());
		for (Change c : changes) {
			changed.add(c.id);
		}
		for (MarkerCategory before : oldCategories) {
			if (categoryNamed(newCategories, before.getName()) == null) {
				viewer.remove(before);
			}
		}
		int limit = view.getGenerator().isMarkerLimitsEnabled() ? view.getGenerator().getMarkerLimits() : -1;
		for (int c = 0; c < newCategories.length; c++) {
			MarkerCategory category = newCategories[c];
			MarkerCategory before = categoryNamed(oldCategories, category.getName());
			if (before == null) {
				viewer.insert(input, category, c);
				if (view.getCategoriesToExpand().contains(category.getName())) {
					viewer.setExpandedState(category, true);
				}
				continue;
			}
			viewer.update(category, null);
			Widget widget = viewer.testFindItem(category);
			if (!(widget instanceof TreeItem item) || !item.getExpanded()) {
				continue;
			}
			patchWindow(viewer, category, item, limit, changed);
		}
	}

	/* rows of one expanded category: remove rows that left the window, insert rows that entered it */
	private static void patchWindow(TreeViewer viewer, MarkerCategory category, TreeItem item, int limit,
			M3LongSet changed) {
		MarkerSupportItem[] all = category.getChildren();
		int shown = limit > 0 ? Math.min(limit, all.length) : all.length;
		M3LongSet next = new M3LongSet(shown);
		for (int i = 0; i < shown; i++) {
			IMarker marker = ((MarkerEntry) all[i]).getMarker();
			if (marker != null) {
				next.add(marker.getId());
			}
		}
		List<Object> gone = new ArrayList<>();
		M3LongSet kept = new M3LongSet(shown);
		for (Item child : item.getItems()) {
			if (child.getData() instanceof MarkerEntry entry && entry.getMarker() != null) {
				long id = entry.getMarker().getId();
				if (changed.contains(id) || !next.contains(id)) {
					gone.add(entry);
				} else {
					kept.add(id);
				}
			}
		}
		if (!gone.isEmpty()) {
			viewer.remove(gone.toArray());
		}
		for (int i = 0; i < shown; i++) {
			MarkerEntry entry = (MarkerEntry) all[i];
			if (entry.getMarker() == null || !kept.contains(entry.getMarker().getId())) {
				viewer.insert(category, entry, i);
			}
		}
	}
}
