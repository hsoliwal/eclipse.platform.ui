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
package org.eclipse.ui.tests.markers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.eclipse.ui.internal.views.markers.M3LabelTable;
import org.eclipse.ui.internal.views.markers.M3SlotOrder;
import org.junit.jupiter.api.Test;

/**
 * Headless tests of the two algorithms under the markers lane model: the
 * blocked order-statistic list of slots ({@link M3SlotOrder}) and the interned
 * label table ({@link M3LabelTable}), each checked against a plain reference
 * (a sorted list, a hash map) over random operation sequences.
 */
public class M3MarkerLanesAlgorithmsTest {

	/* slots ordered by a key lane, ties by slot: a total order like the lane model's */
	private static int[] keys(int n, Random random) {
		int[] key = new int[n];
		for (int i = 0; i < n; i++) {
			key[i] = random.nextInt(n / 4 + 1);
		}
		return key;
	}

	private static M3SlotOrder.SlotComparator order(int[] key) {
		return (a, b) -> {
			int c = Integer.compare(key[a], key[b]);
			return c != 0 ? c : Integer.compare(a, b);
		};
	}

	private static void sortedInsert(List<Integer> ref, int slot, M3SlotOrder.SlotComparator order) {
		int lo = 0;
		int hi = ref.size();
		while (lo < hi) {
			int mid = (lo + hi) >>> 1;
			if (order.compare(ref.get(mid), slot) < 0) {
				lo = mid + 1;
			} else {
				hi = mid;
			}
		}
		ref.add(lo, slot);
	}

	private static void check(M3SlotOrder list, List<Integer> ref) {
		assertEquals(ref.size(), list.size());
		int[] head = new int[ref.size()];
		assertEquals(ref.size(), list.head(Integer.MAX_VALUE, head));
		for (int i = 0; i < ref.size(); i++) {
			assertEquals(ref.get(i).intValue(), head[i]);
		}
		for (int i = 0; i < ref.size(); i += 97) {
			assertEquals(ref.get(i).intValue(), list.get(i));
			assertEquals(i, list.indexOf(ref.get(i)));
		}
	}

	@Test
	public void randomInsertsAndRemovesMatchASortedList() {
		Random random = new Random(7);
		int n = 6000;
		int[] key = keys(n, random);
		M3SlotOrder.SlotComparator order = order(key);
		M3SlotOrder list = new M3SlotOrder(order);
		List<Integer> ref = new ArrayList<>();
		boolean[] in = new boolean[n];
		for (int step = 0; step < 30_000; step++) {
			int slot = random.nextInt(n);
			if (in[slot]) {
				int at = ref.indexOf(slot);
				assertEquals(at, list.remove(slot));
				ref.remove(at);
			} else {
				int at = list.insert(slot);
				sortedInsert(ref, slot, order);
				assertEquals(ref.indexOf(slot), at);
			}
			in[slot] = !in[slot];
			if (step % 5000 == 0) {
				check(list, ref);
			}
		}
		check(list, ref);
		// drain completely: blocks are dropped
		for (int slot = 0; slot < n; slot++) {
			if (in[slot]) {
				list.remove(slot);
			}
		}
		assertEquals(0, list.size());
		list.insert(3);
		assertEquals(0, list.indexOf(3));
	}

	@Test
	public void ascendingAndDescendingRunsSplitBlocks() {
		int n = 5000;
		int[] key = new int[n];
		for (int i = 0; i < n; i++) {
			key[i] = i;
		}
		M3SlotOrder up = new M3SlotOrder(order(key));
		M3SlotOrder down = new M3SlotOrder(order(key));
		for (int i = 0; i < n; i++) {
			up.insert(i);
			down.insert(n - 1 - i);
		}
		for (int i = 0; i < n; i += 499) {
			assertEquals(i, up.get(i));
			assertEquals(i, down.get(i));
			assertEquals(i, down.indexOf(i));
		}
		int[] five = new int[5];
		assertEquals(5, up.head(5, five));
		assertEquals(4, five[4]);
	}

	@Test
	public void fillAndSortMatchASort() {
		Random random = new Random(11);
		int n = 4000;
		int[] key = keys(n, random);
		M3SlotOrder.SlotComparator order = order(key);
		int[] slots = new int[n];
		List<Integer> ref = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			slots[i] = n - 1 - i;
			sortedInsert(ref, i, order);
		}
		M3SlotOrder list = new M3SlotOrder(order);
		list.sortAndFill(slots, n);
		check(list, ref);
		// still a valid list after the bulk fill: inserts and removes keep working
		assertEquals(ref.indexOf(17), list.remove(17));
		ref.remove(Integer.valueOf(17));
		check(list, ref);
		M3SlotOrder empty = new M3SlotOrder(order);
		empty.sortAndFill(new int[1], 0);
		assertEquals(0, empty.size());
		assertEquals(-1, empty.indexOf(0));
		assertEquals(-1, empty.remove(0));
	}

	@Test
	public void misuseIsReported() {
		M3SlotOrder list = new M3SlotOrder(order(new int[] { 1, 2, 3 }));
		list.insert(1);
		assertThrows(IllegalArgumentException.class, () -> list.insert(1));
		assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));
		assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
		assertEquals(-1, list.indexOf(2));
		assertEquals(-1, list.remove(0));
		assertEquals(-1, list.remove(2));
	}

	@Test
	public void labelsInternCountAndFree() {
		M3LabelTable labels = new M3LabelTable(Collator.getInstance());
		Random random = new Random(5);
		Map<String, Integer> refs = new HashMap<>();
		Map<String, Integer> ids = new HashMap<>();
		for (int step = 0; step < 20_000; step++) {
			String s = "m" + random.nextInt(500);
			int count = refs.getOrDefault(s, 0);
			if (count > 0 && random.nextBoolean()) {
				labels.release(ids.get(s));
				refs.put(s, count - 1);
				if (count == 1) {
					ids.remove(s);
				}
			} else {
				int id = labels.acquire(s);
				Integer before = ids.putIfAbsent(s, id);
				if (before != null) {
					assertEquals(before.intValue(), id);
				}
				refs.put(s, count + 1);
				assertEquals(count + 1, labels.references(id));
				assertEquals(s, labels.text(id));
			}
		}
		assertEquals(ids.size(), labels.size());
		for (Map.Entry<String, Integer> e : ids.entrySet()) {
			assertEquals(e.getKey(), labels.text(e.getValue()));
		}
	}

	@Test
	public void labelsCompareLikeTheStockFields() {
		Collator collator = Collator.getInstance();
		M3LabelTable labels = new M3LabelTable(collator);
		int a = labels.acquire("apple");
		int b = labels.acquire("Banana");
		int e = labels.acquire("");
		assertEquals(Integer.signum(collator.getCollationKey("apple").compareTo(collator.getCollationKey("Banana"))),
				Integer.signum(labels.collate(a, b)));
		assertEquals(Integer.signum("apple".compareTo("Banana")), Integer.signum(labels.compare(a, b)));
		assertEquals(0, labels.collate(a, a));
		assertEquals(0, labels.compare(b, b));
		assertNotEquals(0, labels.collate(e, a));
		// a freed id is reused
		labels.release(b);
		assertEquals(b, labels.acquire("cherry"));
	}
}
