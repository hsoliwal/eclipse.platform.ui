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

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.views.markers.MarkerField;
import org.eclipse.ui.views.markers.MarkerItem;
import org.eclipse.ui.views.markers.internal.MarkerGroup;
import org.eclipse.ui.views.markers.internal.MarkerGroupingEntry;

/**
 * The one model of a markers view in M3 mode: every shown marker is a slot in
 * a set of lanes (id, marker handle, severity, interned message, location,
 * resource name and path, line, creation time, category). Each category keeps
 * its slots in sort order ({@link M3SlotOrder}); counts per severity are lane
 * counters. A {@link MarkerEntry} exists only for a slot an API asks for (the
 * rows the viewer shows, a selection, Quick Fix) and is parent-owned by its
 * category.
 * <p>
 * Built once by a clean rebuild (first input, filter, grouping or sort
 * changes), then patched by marker deltas on the UI thread
 * ({@link #apply(List)}). Design: {@code docs/m3/markers-view/DESIGN.md} and
 * {@code FALLBACKS_TO_DELTAS.md}.
 * </p>
 */
final class M3MarkerLanes {

	/**
	 * One prepared change: a marker leaves the view ({@code keys == null}) or is
	 * (re)placed with its keys and grouping. A clean rebuild reuses one instance
	 * for every marker.
	 */
	static final class Change {
		long id;
		IMarker marker;
		M3MarkerKeys keys;
		MarkerGroupingEntry group;
		/** The entry the filters saw; it names a category the first time one is needed. */
		MarkerItem item;
		/** Whether the view's search text keeps the row (true without a search text). */
		boolean textHit = true;
		/** The lanes' text generation the hit was computed for. */
		int textGeneration;

		Change set(long markerId, IMarker m, M3MarkerKeys k, MarkerGroupingEntry g, MarkerItem i) {
			id = markerId;
			marker = m;
			keys = k;
			group = g;
			item = i;
			textHit = true;
			textGeneration = 0;
			return this;
		}
	}

	/** One category: its grouping key, its slots in order, its counters and its facade. */
	static final class Category {
		/** Stable id: the value of the category lane (positions shift, ids do not). */
		final int id;
		final MarkerGroupingEntry key;
		final M3MarkerCategory facade;
		M3SlotOrder order;
		/** Counts by severity bucket (errors, warnings, infos, others) and of severities above error. */
		final int[] buckets = new int[4];
		int aboveError;
		/* slots collected by a clean rebuild, sorted once by finishBuild */
		int[] pending;
		int pendingSize;

		Category(int id, MarkerGroupingEntry key, M3MarkerCategory facade, M3SlotOrder order) {
			this.id = id;
			this.key = key;
			this.facade = facade;
			this.order = order;
		}
	}

	private static final Collator COLLATOR = Collator.getInstance();

	private final Markers owner;
	private final Comparator<MarkerGroupingEntry> categoryOrder;
	private final MarkerField categoryField;
	private final M3LabelTable labels = new M3LabelTable(COLLATOR);
	private byte[] kinds;
	private boolean[] descending;
	private boolean needType;

	/* lanes, by slot */
	private byte[] state = new byte[64];
	private long[] ids = new long[64];
	private IMarker[] markers = new IMarker[64];
	private int[] category = new int[64];
	private int[] severity = new int[64];
	private int[] message = new int[64];
	private int[] location = new int[64];
	private int[] line = new int[64];
	private int[] resource = new int[64];
	private int[] path = new int[64];
	private long[] created = new long[64];
	private int[] type = new int[64];
	private MarkerEntry[] facades = new MarkerEntry[64];
	/* the view's search text (MarkerPatternFilter): one hit bit per slot, meaningful while textFilter != null */
	private long[] textHits = new long[1];
	private MarkerPatternFilter textFilter;
	private int textGeneration;
	private int slots;
	private int[] free = new int[16];
	private int freeCount;
	private int live;
	/* marker id -> slot + 1, open addressing */
	private int[] table = new int[128];

	private final List<Category> categories = new ArrayList<>();
	private Category[] byId = new Category[8];
	private int nextCategoryId;
	private M3MarkerCategory[] categoryArray;
	private final int[] counts = new int[4];
	private Integer[] countsArray;
	private int generation;

	private M3MarkerLanes(Markers owner, Comparator<MarkerGroupingEntry> categoryOrder, MarkerField categoryField) {
		this.owner = owner;
		this.categoryOrder = categoryOrder;
		this.categoryField = categoryField;
	}

	/**
	 * @param owner      the model of the view
	 * @param comparator the view's sort
	 * @return an empty lane model, or {@code null} when the view uses a field
	 *         or a presentation the lanes do not answer (flat presentation, a
	 *         field of another class): the stock model is used then
	 */
	static M3MarkerLanes create(Markers owner, MarkerComparator comparator) {
		CachedMarkerBuilder builder = owner.getBuilder();
		if (!M3MarkerDeltas.ENABLED) {
			return null;
		}
		MarkerGroup group = builder.getCategoryGroup();
		// flat presentation (no grouping): one implicit category without a tree item (FALLBACKS_TO_DELTAS.md)
		M3MarkerLanes lanes = group == null ? new M3MarkerLanes(owner, null, null)
				: new M3MarkerLanes(owner, group.getEntriesComparator(), group.getMarkerField());
		return lanes.compile(comparator) ? lanes : null;
	}

	/** @return whether the view is flat: the rows are the tree's top-level items */
	boolean isFlat() {
		return categoryOrder == null;
	}

	/** @return the implicit category of the flat presentation, or {@code null} before the first row */
	Category flatCategory() {
		return categories.isEmpty() ? null : categories.get(0);
	}

	/**
	 * The shown rows of the flat presentation.
	 *
	 * @param limit the marker limit, or a negative value for none
	 * @return the entries of the window, in order
	 */
	MarkerEntry[] flatWindow(int limit) {
		Category c = flatCategory();
		return c == null ? new MarkerEntry[0] : window(c, limit);
	}

	/**
	 * Markers.getElements: the categories, or every row of the flat presentation
	 * (an API asks for all of them).
	 *
	 * @return the elements
	 */
	MarkerSupportItem[] elements() {
		if (!isFlat()) {
			return categories();
		}
		Category c = flatCategory();
		return c == null ? new MarkerEntry[0] : children(c);
	}

	/* the comparator's fields as lane kinds; false when one is unknown */
	private boolean compile(MarkerComparator comparator) {
		MarkerField[] fields = comparator.getFields();
		byte[] k = new byte[fields.length];
		boolean[] d = new boolean[fields.length];
		boolean type = false;
		for (int i = 0; i < fields.length; i++) {
			k[i] = M3MarkerKeys.kindOf(fields[i]);
			if (k[i] == M3MarkerKeys.UNKNOWN) {
				return false;
			}
			type |= k[i] == M3MarkerKeys.TYPE;
			d[i] = comparator.descendingFields.contains(fields[i]);
		}
		kinds = k;
		descending = d;
		needType = type;
		return true;
	}

	/** @return whether the sort keys include the type label */
	boolean needsType() {
		return needType;
	}

	/* ---------------------------------------------------------------- build */

	/**
	 * Adds a marker during a clean rebuild; {@link #finishBuild()} sorts.
	 *
	 * @param change the marker, its keys and category
	 */
	void add(Change change) {
		int existing = find(change.id);
		if (existing >= 0) {
			if (!sameIdentity(markers[existing], change.marker)) {
				throw new IllegalStateException("marker ID reused across resources");
			}
			return; // the same marker found through two scopes
		}
		int slot = allocate(change.id);
		write(slot, change);
		Category c = categoryFor(change);
		category[slot] = c.id;
		count(slot, c, 1);
		if (c.pending == null) {
			c.pending = new int[64];
		} else if (c.pendingSize == c.pending.length) {
			c.pending = Arrays.copyOf(c.pending, c.pendingSize << 1);
		}
		c.pending[c.pendingSize++] = slot;
	}

	/** Sorts every category once, at the end of a clean rebuild. */
	void finishBuild() {
		M3SlotOrder.SlotComparator order = comparator(kinds, descending);
		for (Category c : categories) {
			M3SlotOrder o = new M3SlotOrder(order);
			if (c.pending != null) {
				o.sortAndFill(c.pending, c.pendingSize);
			}
			c.pending = null;
			c.pendingSize = 0;
			c.order = o;
		}
		changed();
	}

	/**
	 * Sorts again with the view's current sort (the user changed it). Builds
	 * the new orders aside and swaps them in.
	 *
	 * @param comparator the view's sort
	 * @return {@code false} when the new sort uses a field the lanes do not
	 *         answer
	 */
	synchronized boolean resort(MarkerComparator comparator) {
		if (!compile(comparator)) {
			return false;
		}
		M3SlotOrder.SlotComparator order = comparator(kinds, descending);
		M3SlotOrder[] next = new M3SlotOrder[categories.size()];
		for (int i = 0; i < next.length; i++) {
			M3SlotOrder old = categories.get(i).order;
			int[] members = new int[Math.max(1, old.size())];
			int n = old.head(old.size(), members);
			next[i] = new M3SlotOrder(order);
			next[i].sortAndFill(members, n);
		}
		for (int i = 0; i < next.length; i++) {
			categories.get(i).order = next[i];
		}
		Arrays.fill(facades, null);
		changed();
		return true;
	}

	/* ---------------------------------------------------------------- delta */

	/**
	 * Applies prepared changes on the UI thread.
	 *
	 * @param changes the changes, merged per marker
	 * @return the categories that were touched, created or emptied (an emptied
	 *         category has an empty order and is no longer in
	 *         {@link #categories()})
	 */
	synchronized List<Category> apply(List<Change> changes) {
		List<Category> touched = new ArrayList<>();
		for (Change change : changes) {
			int slot = find(change.id);
			if (slot >= 0 && change.marker != null
					&& !sameIdentity(markers[slot], change.marker)) {
				throw new IllegalStateException("delta would overwrite another resource's marker");
			}
			if (slot >= 0) {
				Category before = byId[category[slot]];
				before.order.remove(slot);
				count(slot, before, -1);
				mark(touched, before);
				release(slot);
				if (change.keys == null) {
					freeSlot(slot);
					continue;
				}
			} else if (change.keys == null) {
				continue;
			} else {
				slot = allocate(change.id);
			}
			write(slot, change);
			Category c = categoryFor(change);
			category[slot] = c.id;
			c.order.insert(slot);
			count(slot, c, 1);
			mark(touched, c);
		}
		dropEmpty();
		changed();
		return touched;
	}

	private static void mark(List<Category> touched, Category c) {
		for (Category t : touched) {
			if (t == c) {
				return;
			}
		}
		touched.add(c);
	}

	/* remove categories that lost their last slot */
	private void dropEmpty() {
		if (isFlat()) {
			return; // the implicit category stays, empty or not
		}
		for (int i = categories.size() - 1; i >= 0; i--) {
			Category c = categories.get(i);
			if (c.order.size() == 0) {
				categories.remove(i);
				byId[c.id] = null;
			}
		}
	}

	/* the category of a change: found by the group's comparator (TreeMap equality), else created in order */
	private Category categoryFor(Change change) {
		if (isFlat()) {
			return categories.isEmpty() ? newCategory(0, null, null) : categories.get(0);
		}
		int lo = 0;
		int hi = categories.size() - 1;
		while (lo <= hi) {
			int mid = (lo + hi) >>> 1;
			int c = categoryOrder.compare(categories.get(mid).key, change.group);
			if (c < 0) {
				lo = mid + 1;
			} else if (c > 0) {
				hi = mid - 1;
			} else {
				return categories.get(mid);
			}
		}
		// stock names a category after its first entry (Markers.groupIntoCategories)
		return newCategory(lo, change.group, categoryField.getValue(change.item));
	}

	/* a category at position lo; the flat presentation's implicit category has no facade (rows have no parent) */
	private Category newCategory(int lo, MarkerGroupingEntry key, String name) {
		int id = nextCategoryId++;
		M3MarkerCategory facade = name == null ? null : new M3MarkerCategory(owner, this, name);
		Category created = new Category(id, key, facade, new M3SlotOrder(comparator(kinds, descending)));
		if (facade != null) {
			facade.lane = created;
		}
		categories.add(lo, created);
		if (id == byId.length) {
			byId = Arrays.copyOf(byId, id << 1);
		}
		byId[id] = created;
		return created;
	}

	/* ---------------------------------------------------------------- lanes */

	private int allocate(long id) {
		int slot;
		if (freeCount > 0) {
			slot = free[--freeCount];
		} else {
			slot = slots++;
			if (slot == ids.length) {
				grow(slot << 1);
			}
		}
		ids[slot] = id;
		state[slot] = 1;
		live++;
		if (live << 1 > table.length) {
			rehash(table.length << 1);
		} else {
			put(id, slot);
		}
		return slot;
	}

	private void freeSlot(int slot) {
		unput(ids[slot]);
		state[slot] = 0;
		markers[slot] = null;
		category[slot] = -1;
		live--;
		if (freeCount == free.length) {
			free = Arrays.copyOf(free, freeCount << 1);
		}
		free[freeCount++] = slot;
	}

	private void write(int slot, Change change) {
		M3MarkerKeys k = change.keys;
		markers[slot] = change.marker;
		severity[slot] = k.severity;
		message[slot] = labels.acquire(k.message);
		location[slot] = labels.acquire(k.location);
		line[slot] = k.line;
		resource[slot] = labels.acquire(k.resource);
		path[slot] = labels.acquire(k.path);
		created[slot] = k.created;
		type[slot] = k.type == null ? -1 : labels.acquire(k.type);
		setTextHit(slot, change.textHit);
		facades[slot] = null;
	}

	/* the labels of a slot that leaves or changes */
	private void release(int slot) {
		labels.release(message[slot]);
		labels.release(location[slot]);
		labels.release(resource[slot]);
		labels.release(path[slot]);
		if (type[slot] >= 0) {
			labels.release(type[slot]);
		}
		facades[slot] = null;
	}

	private void count(int slot, Category c, int delta) {
		int b = bucket(severity[slot]);
		counts[b] += delta;
		c.buckets[b] += delta;
		if (severity[slot] > IMarker.SEVERITY_ERROR) {
			c.aboveError += delta;
		}
	}

	/** Severity to the index of {@link Markers#getMarkerCounts()}: errors, warnings, infos, others. */
	static int bucket(int severity) {
		return switch (severity) {
		case IMarker.SEVERITY_ERROR -> 0;
		case IMarker.SEVERITY_WARNING -> 1;
		case IMarker.SEVERITY_INFO -> 2;
		default -> 3;
		};
	}

	private void grow(int capacity) {
		state = Arrays.copyOf(state, capacity);
		ids = Arrays.copyOf(ids, capacity);
		markers = Arrays.copyOf(markers, capacity);
		category = Arrays.copyOf(category, capacity);
		severity = Arrays.copyOf(severity, capacity);
		message = Arrays.copyOf(message, capacity);
		location = Arrays.copyOf(location, capacity);
		line = Arrays.copyOf(line, capacity);
		resource = Arrays.copyOf(resource, capacity);
		path = Arrays.copyOf(path, capacity);
		created = Arrays.copyOf(created, capacity);
		type = Arrays.copyOf(type, capacity);
		facades = Arrays.copyOf(facades, capacity);
	}

	/**
	 * A marker's stable logical key is (marker id, resource equals).
	 * While the M3 table remains id-only, mismatches refuse its fast path.
	 */
	static boolean sameIdentity(IMarker first, IMarker second) {
		if (first == null || second == null || first.getId() != second.getId()) {
			return false;
		}
		if (first.getResource() == null || second.getResource() == null) {
			return false; // ambiguous resource handle: use original Eclipse rebuild
		}
		return first.getResource().equals(second.getResource());
	}

	/* id table */

	/**
	 * @param id a marker id
	 * @return its slot, or -1
	 */
	int find(long id) {
		int mask = table.length - 1;
		for (int i = hash(id) & mask; table[i] != 0; i = (i + 1) & mask) {
			if (ids[table[i] - 1] == id) {
				return table[i] - 1;
			}
		}
		return -1;
	}

	private void put(long id, int slot) {
		int mask = table.length - 1;
		int i = hash(id) & mask;
		while (table[i] != 0) {
			i = (i + 1) & mask;
		}
		table[i] = slot + 1;
	}

	private void unput(long id) {
		int mask = table.length - 1;
		int i = hash(id) & mask;
		while (ids[table[i] - 1] != id) {
			i = (i + 1) & mask;
		}
		table[i] = 0;
		for (int j = (i + 1) & mask; table[j] != 0; j = (j + 1) & mask) {
			int slot = table[j] - 1;
			table[j] = 0;
			put(ids[slot], slot);
		}
	}

	private void rehash(int capacity) {
		table = new int[capacity];
		for (int s = 0; s < slots; s++) {
			if (state[s] != 0) {
				put(ids[s], s);
			}
		}
	}

	private static int hash(long id) {
		return (int) ((id ^ (id >>> 32)) * 0x9E3779B97F4A7C15L >>> 32);
	}

	/* ---------------------------------------------------------------- order */

	private M3SlotOrder.SlotComparator comparator(byte[] k, boolean[] d) {
		return (a, b) -> compare(a, b, k, d);
	}

	/* MarkerComparator.compareFields over the lanes; ties end by marker id, then slot (a total order) */
	private int compare(int a, int b, byte[] k, boolean[] d) {
		if (a == b) {
			return 0;
		}
		for (int i = 0; i < k.length; i++) {
			int c = compareField(k[i], a, b);
			if (c != 0) {
				return d[i] ? -c : c;
			}
		}
		int c = Long.compare(ids[a], ids[b]);
		return c != 0 ? c : Integer.compare(a, b);
	}

	private int compareField(byte kind, int a, int b) {
		switch (kind) {
		case M3MarkerKeys.SEVERITY_DESCRIPTION:
			int c = Integer.compare(severity[b], severity[a]);
			return c != 0 ? c : labels.collate(message[a], message[b]);
		case M3MarkerKeys.DESCRIPTION:
			return labels.collate(message[a], message[b]);
		case M3MarkerKeys.RESOURCE:
			return labels.compare(resource[a], resource[b]);
		case M3MarkerKeys.PATH:
			return labels.compare(path[a], path[b]);
		case M3MarkerKeys.LOCATION:
			String la = labels.text(location[a]);
			String lb = labels.text(location[b]);
			int e = Boolean.compare(la.isEmpty(), lb.isEmpty());
			if (e != 0) {
				return e;
			}
			e = labels.compare(location[a], location[b]);
			return e != 0 ? e : Integer.compare(line[a], line[b]);
		case M3MarkerKeys.ID:
			return Long.compare(ids[a], ids[b]);
		case M3MarkerKeys.CREATED:
			return Long.compare(created[a], created[b]);
		case M3MarkerKeys.TYPE:
			return labels.compare(type[a], type[b]);
		default:
			throw new IllegalStateException("unknown field kind " + kind); //$NON-NLS-1$
		}
	}

	/* ---------------------------------------------------------------- answers */

	private void changed() {
		generation++;
		categoryArray = null;
		countsArray = null;
	}

	/** @return the generation, increased by every change of the model */
	int generation() {
		return generation;
	}

	/** @return the categories in order (cached until the next change) */
	M3MarkerCategory[] categories() {
		M3MarkerCategory[] a = categoryArray;
		if (a == null) {
			a = new M3MarkerCategory[isFlat() ? 0 : categories.size()];
			for (int i = 0; i < a.length; i++) {
				a[i] = categories.get(i).facade;
			}
			categoryArray = a;
		}
		return a;
	}

	/** @return the counts: errors, warnings, infos, others (O(1), lane counters) */
	Integer[] counts() {
		Integer[] a = countsArray;
		if (a == null) {
			a = new Integer[] { Integer.valueOf(counts[0]), Integer.valueOf(counts[1]), Integer.valueOf(counts[2]),
					Integer.valueOf(counts[3]) };
			countsArray = a;
		}
		return a;
	}

	/** @return the number of markers in the categories */
	int size() {
		return live;
	}

	/**
	 * A consistent snapshot of the shown marker ids (any thread; the UI thread
	 * applies changes under the same lock).
	 *
	 * @return the ids, sorted ascending
	 */
	synchronized long[] liveIds() {
		long[] out = new long[live];
		int k = 0;
		for (int s = 0; s < slots; s++) {
			if (state[s] != 0) {
				out[k++] = ids[s];
			}
		}
		Arrays.sort(out);
		return out;
	}

	/** @return the number of distinct labels (heap evidence) */
	int labelCount() {
		return labels.size();
	}

	/**
	 * @param c a category
	 * @return its number of markers
	 */
	static int size(Category c) {
		return c.order.size();
	}

	/**
	 * The facade of a slot, created on first request and kept while the slot
	 * stays in a shown window.
	 *
	 * @param slot a slot
	 * @return its entry
	 */
	MarkerEntry facade(int slot) {
		MarkerEntry e = facades[slot];
		if (e == null) {
			e = new MarkerEntry(markers[slot]);
			e.setCategory(byId[category[slot]].facade);
			facades[slot] = e;
		}
		return e;
	}

	/**
	 * Drops the facade of a slot that left every shown window.
	 *
	 * @param slot a slot
	 */
	void releaseFacade(int slot) {
		facades[slot] = null;
	}

	/**
	 * The first rows of a category: the shown window.
	 *
	 * @param c     a category
	 * @param limit the marker limit, or a negative value for none
	 * @return the entries of the window, in order
	 */
	MarkerEntry[] window(Category c, int limit) {
		int[] head = windowSlots(c, limit);
		MarkerEntry[] out = new MarkerEntry[head.length];
		for (int i = 0; i < head.length; i++) {
			out[i] = facade(head[i]);
		}
		return out;
	}

	/**
	 * The slots of a category's window.
	 *
	 * @param c     a category
	 * @param limit the marker limit, or a negative value for none
	 * @return the slots, in order
	 */
	int[] windowSlots(Category c, int limit) {
		int size = c.order.size();
		if (textFilter == null) {
			int n = limit < 0 ? size : Math.min(limit, size);
			int[] head = new int[n];
			c.order.head(n, head);
			return head;
		}
		// a search text: the matching rows first, then the limit (MarkerViewerContentProvider.getLimitedChildren)
		int max = limit < 0 ? size : Math.min(limit, size);
		int[] head = new int[max];
		int n = 0;
		for (int i = 0; i < size && n < max; i++) {
			int slot = c.order.get(i);
			if (textHit(slot)) {
				head[n++] = slot;
			}
		}
		return n == max ? head : Arrays.copyOf(head, n);
	}

	/**
	 * The search text of the view as a lane: a hit bit per slot. Called on the
	 * UI thread when the text changes (MarkerPatternFilter.setPattern, before
	 * the viewer refresh that follows typing) and by a clean rebuild after
	 * {@link #finishBuild()}.
	 *
	 * @param filter the active search filter, or {@code null} for none
	 */
	void applyText(MarkerPatternFilter filter) {
		textGeneration++;
		textFilter = filter;
		if (filter == null) {
			return;
		}
		for (int s = 0; s < slots; s++) {
			if (state[s] != 0) {
				MarkerEntry e = facades[s];
				if (e == null) {
					e = new MarkerEntry(markers[s]);
					e.setCategory(byId[category[s]].facade);
				}
				setTextHit(s, filter.matches(e));
			}
		}
	}

	/** The active search filter, or {@code null}. */
	MarkerPatternFilter textFilter() {
		return textFilter;
	}

	/** Increases on every {@link #applyText}. */
	int textGeneration() {
		return textGeneration;
	}

	/** Whether the search text keeps a slot; every slot without a search text. */
	boolean textHit(int slot) {
		return textFilter == null || (textHits[slot >>> 6] & (1L << (slot & 63))) != 0;
	}

	void setTextHit(int slot, boolean hit) {
		int word = slot >>> 6;
		if (word >= textHits.length) {
			textHits = Arrays.copyOf(textHits, Math.max(word + 1, textHits.length << 1));
		}
		if (hit) {
			textHits[word] |= 1L << (slot & 63);
		} else {
			textHits[word] &= ~(1L << (slot & 63));
		}
	}

	/**
	 * The rows of a category the search text keeps (all of them without a
	 * search text): what stock counts with MarkerPatternFilter over every child.
	 *
	 * @param c a category
	 * @return the matching row count
	 */
	int matching(Category c) {
		int size = c.order.size();
		if (textFilter == null) {
			return size;
		}
		int m = 0;
		for (int i = 0; i < size; i++) {
			if (textHit(c.order.get(i))) {
				m++;
			}
		}
		return m;
	}

	/**
	 * Every row of a category (an API asks for all of them: selection of a
	 * category, copy, the search box). Facades of rows outside the windows are
	 * not kept.
	 *
	 * @param c a category
	 * @return the entries in order
	 */
	MarkerEntry[] children(Category c) {
		int n = c.order.size();
		int[] all = new int[n];
		c.order.head(n, all);
		MarkerEntry[] out = new MarkerEntry[n];
		for (int i = 0; i < n; i++) {
			MarkerEntry e = facades[all[i]];
			if (e == null) {
				e = new MarkerEntry(markers[all[i]]);
				e.setCategory(c.facade);
			}
			out[i] = e;
		}
		return out;
	}

	/**
	 * MarkerCategory.getHighestSeverity from the counters: the first child's
	 * order matters only when a severity above error exists, then the rows are
	 * scanned like stock does.
	 *
	 * @param c a category
	 * @return the highest severity, at least info
	 */
	int highestSeverity(Category c) {
		if (c.aboveError == 0) {
			if (c.buckets[0] > 0) {
				return IMarker.SEVERITY_ERROR;
			}
			return c.buckets[1] > 0 ? IMarker.SEVERITY_WARNING : IMarker.SEVERITY_INFO;
		}
		int highest = 0;
		int n = c.order.size();
		for (int i = 0; i < n; i++) {
			int s = severity[c.order.get(i)];
			if (s > highest) {
				highest = s;
			}
			if (highest == IMarker.SEVERITY_ERROR) {
				return highest;
			}
		}
		return highest;
	}

	/**
	 * @param marker a marker
	 * @return its entry, or {@code null} when it is not shown
	 */
	MarkerEntry item(IMarker marker) {
		int slot = find(marker.getId());
		if (slot < 0 || !sameIdentity(marker, markers[slot])) {
			return null;
		}
		return facade(slot);
	}

	/**
	 * @param id a marker id
	 * @return the marker of the slot that holds it, or {@code null}
	 */
	IMarker marker(long id) {
		int slot = find(id);
		return slot < 0 ? null : markers[slot];
	}

	/**
	 * @param slot a slot
	 * @return its marker id
	 */
	long id(int slot) {
		return ids[slot];
	}

	/**
	 * @param c a category
	 * @return its index in {@link #categories()}, or -1 once dropped
	 */
	int indexOf(Category c) {
		return categories.indexOf(c);
	}
}
