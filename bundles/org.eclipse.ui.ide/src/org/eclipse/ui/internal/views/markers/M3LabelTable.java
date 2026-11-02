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

import java.text.CollationKey;
import java.text.Collator;
import java.util.Arrays;

/**
 * Interned labels of the marker lanes: each distinct string (message,
 * location, resource name, path, type name) is stored once and named by an
 * {@code int} id; a reference count per id frees it when no marker uses it.
 * The collation key of a label is computed once, on the first comparison that
 * needs it, so a sort by description costs one key per distinct message, not
 * one per marker (stock rotates a per-update map of keys, MarkerEntry.java:76).
 * <p>
 * Lookup is an open-addressing {@code int[]} table over the ids (no map entry
 * objects). Not thread-safe.
 * </p>
 */
public final class M3LabelTable {

	private final Collator collator;
	private String[] text = new String[16];
	private CollationKey[] keys = new CollationKey[16];
	private int[] refs = new int[16];
	private int[] free = new int[16];
	private int freeCount;
	private int count;
	private int live;
	private int[] table = new int[32];

	/**
	 * @param collator the collator of the description sort (MarkerEntry's)
	 */
	public M3LabelTable(Collator collator) {
		this.collator = collator;
	}

	/**
	 * Interns a label and counts one more reference to it.
	 *
	 * @param label the label, not {@code null}
	 * @return its id
	 */
	public int acquire(String label) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		int mask = table.length - 1;
		int h = label.hashCode();
		int i = mix(h) & mask;
		while (table[i] != 0) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			int id = table[i] - 1;
			if (text[id].equals(label)) {
				refs[id]++;
				return id;
			}
			i = (i + 1) & mask;
		}
		int id = freeCount > 0 ? free[--freeCount] : count++;
		if (id == text.length) {
			int capacity = id << 1;
			text = Arrays.copyOf(text, capacity);
			keys = Arrays.copyOf(keys, capacity);
			refs = Arrays.copyOf(refs, capacity);
		}
		text[id] = label;
		keys[id] = null;
		refs[id] = 1;
		live++;
		table[i] = id + 1;
		if (live << 1 > table.length) {
			rehash(table.length << 1);
		}
		return id;
	}

	/**
	 * Counts one reference less; the label is freed at zero.
	 *
	 * @param id a label id
	 */
	public void release(int id) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		if (--refs[id] > 0) {
			return;
		}
		String label = text[id];
		text[id] = null;
		keys[id] = null;
		live--;
		if (freeCount == free.length) {
			free = Arrays.copyOf(free, freeCount << 1);
		}
		free[freeCount++] = id;
		// remove from the table and re-place the rest of the cluster (linear probing)
		int mask = table.length - 1;
		int i = mix(label.hashCode()) & mask;
		while (table[i] != id + 1) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			i = (i + 1) & mask;
		}
		table[i] = 0;
		for (int j = (i + 1) & mask; table[j] != 0; j = (j + 1) & mask) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			int other = table[j] - 1;
			table[j] = 0;
			place(other);
		}
	}

	/**
	 * @param id a label id
	 * @return the label
	 */
	public String text(int id) {
		return text[id];
	}

	/**
	 * @param id a label id
	 * @return the number of markers that use it
	 */
	public int references(int id) {
		return refs[id];
	}

	/** @return the number of distinct labels in use */
	public int size() {
		return live;
	}

	/**
	 * Compares two labels by their collation keys (the order of
	 * {@code MarkerDescriptionField}); an empty label sorts as the empty key.
	 *
	 * @param a a label id
	 * @param b a label id
	 * @return the collation order
	 */
	public int collate(int a, int b) {
		if (a == b) {
			return 0;
		}
		return key(a).compareTo(key(b));
	}

	/**
	 * Compares two labels by {@link String#compareTo(String)}.
	 *
	 * @param a a label id
	 * @param b a label id
	 * @return the code-unit order
	 */
	public int compare(int a, int b) {
		return a == b ? 0 : text[a].compareTo(text[b]);
	}

	private CollationKey key(int id) {
		CollationKey key = keys[id];
		if (key == null) {
			key = collator.getCollationKey(text[id]);
			keys[id] = key;
		}
		return key;
	}

	private void rehash(int capacity) {
		table = new int[capacity];
		for (int id = 0; id < count; id++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			if (text[id] != null) {
				place(id);
			}
		}
	}

	private void place(int id) {
		int mask = table.length - 1;
		int i = mix(text[id].hashCode()) & mask;
		while (table[i] != 0) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			i = (i + 1) & mask;
		}
		table[i] = id + 1;
	}

	private static int mix(int h) {
		return h * 0x9E3779B9 ^ (h >>> 16);
	}
}
