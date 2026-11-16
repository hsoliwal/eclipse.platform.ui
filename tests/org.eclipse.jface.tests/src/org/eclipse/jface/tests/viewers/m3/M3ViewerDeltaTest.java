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
package org.eclipse.jface.tests.viewers.m3;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.jface.viewers.m3.M3ViewerDelta;
import org.junit.jupiter.api.Test;

/** Headless tests of the {@link M3ViewerDelta} lanes. */
public class M3ViewerDeltaTest {

	@Test
	public void operationsKeepTheirOrderAndFields() {
		String[] props = { "label" };
		M3ViewerDelta d = new M3ViewerDelta().add("root", "a", 2).add("root", "b", -1).remove("c").update("d", props)
				.refresh(null).refresh("e");
		assertEquals(6, d.size());
		assertFalse(d.isEmpty());
		assertEquals(M3ViewerDelta.ADD, d.kind(0));
		assertEquals("root", d.parent(0));
		assertEquals("a", d.element(0));
		assertEquals(2, d.index(0));
		assertEquals(-1, d.index(1));
		assertEquals(M3ViewerDelta.REMOVE, d.kind(2));
		assertNull(d.parent(2));
		assertEquals(M3ViewerDelta.UPDATE, d.kind(3));
		assertArrayEquals(props, d.properties(3));
		assertEquals(M3ViewerDelta.REFRESH, d.kind(4));
		assertNull(d.element(4));
		assertEquals("e", d.element(5));
		assertEquals("M3ViewerDelta[add a, add b, remove c, update d, refresh null, refresh e]", d.toString());
	}

	@Test
	public void lanesGrowAndAppendMerges() {
		M3ViewerDelta a = new M3ViewerDelta();
		for (int i = 0; i < 100; i++) {
			a.remove(Integer.valueOf(i));
		}
		M3ViewerDelta b = new M3ViewerDelta().update("x", null);
		a.append(b);
		assertEquals(101, a.size());
		assertEquals(Integer.valueOf(99), a.element(99));
		assertEquals("x", a.element(100));
		assertTrue(new M3ViewerDelta().isEmpty());
	}

	@Test
	public void selfAppendCopiesOnlyTheOriginalOperations() {
		String[] changed = { "label" }; //$NON-NLS-1$
		M3ViewerDelta delta = new M3ViewerDelta().add("root", "a", 2).update("b", changed).remove("c"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		delta.append(delta);
		assertEquals(6, delta.size());
		for (int i = 0; i < 3; i++) {
			assertEquals(delta.kind(i), delta.kind(i + 3));
			assertEquals(delta.element(i), delta.element(i + 3));
			assertEquals(delta.parent(i), delta.parent(i + 3));
			assertEquals(delta.index(i), delta.index(i + 3));
			assertArrayEquals(delta.properties(i), delta.properties(i + 3));
		}
		assertFalse(delta.isEmpty());
	}

	@Test
	public void rejectsNullElementsAndBadIndexes() {
		M3ViewerDelta d = new M3ViewerDelta();
		assertThrows(IllegalArgumentException.class, () -> d.add("p", null, 0));
		assertThrows(IllegalArgumentException.class, () -> d.remove(null));
		assertThrows(IllegalArgumentException.class, () -> d.update(null, null));
		assertThrows(IndexOutOfBoundsException.class, () -> d.kind(0));
		assertThrows(IndexOutOfBoundsException.class, () -> d.element(-1));
	}
	@Test
	public void bulkAppendCopiesOriginalLanesExactlyOnceEvenForSelfAppend() {
		String[] properties = { "label" };
		M3ViewerDelta delta = new M3ViewerDelta().add("parent", "alpha", 7)
				.update("beta", properties).refresh(null);
		delta.append(delta);
		assertEquals(6, delta.size());
		for (int i = 0; i < 3; i++) {
			assertEquals(delta.kind(i), delta.kind(i + 3));
			assertSame(delta.element(i), delta.element(i + 3));
			assertSame(delta.parent(i), delta.parent(i + 3));
			assertEquals(delta.index(i), delta.index(i + 3));
			assertSame(delta.properties(i), delta.properties(i + 3));
		}
		M3ViewerDelta other = new M3ViewerDelta().remove("gamma");
		delta.append(other);
		assertEquals(7, delta.size());
		assertEquals("gamma", delta.element(6));
		assertEquals(1, other.size());
	}

	@Test
	public void emptySelfAppendIsAnIdentity() {
		M3ViewerDelta delta = new M3ViewerDelta();
		assertSame(delta, delta.append(delta));
		assertTrue(delta.isEmpty());
	}

	@Test
	public void selfAppendDuplicatesOriginalLanesExactlyOnce() {
		for (int count : new int[] { 1, 6, 8, 9, 17, 128 }) {
			M3ViewerDelta delta = new M3ViewerDelta();
			Object parent = new Object();
			String[] properties = { "label" };
			for (int i = 0; i < count; i++) {
				switch (i % 4) {
				case 0 -> delta.add(parent, new Object(), i);
				case 1 -> delta.remove(new Object());
				case 2 -> delta.update(new Object(), properties);
				default -> delta.refresh(null);
				}
			}
			assertSame(delta, delta.append(delta));
			assertEquals(2 * count, delta.size());
			for (int i = 0; i < count; i++) {
				assertEquals(delta.kind(i), delta.kind(count + i));
				assertSame(delta.element(i), delta.element(count + i));
				assertSame(delta.parent(i), delta.parent(count + i));
				assertEquals(delta.index(i), delta.index(count + i));
				assertSame(delta.properties(i), delta.properties(count + i));
			}
		}
	}

	/* Applies a delta to a plain list the way a viewer would: removes by element, adds at their index. */
	private static java.util.List<Object> applied(Object[] oldChildren, M3ViewerDelta delta) {
		java.util.List<Object> list = new java.util.ArrayList<>(java.util.Arrays.asList(oldChildren));
		for (int i = 0; i < delta.size(); i++) {
			if (delta.kind(i) == M3ViewerDelta.REMOVE) {
				assertTrue(list.remove(delta.element(i)), "a removed element is in the list");
			} else {
				assertEquals(M3ViewerDelta.ADD, delta.kind(i));
				list.add(delta.index(i), delta.element(i));
			}
		}
		return list;
	}

	@Test
	public void betweenAnswersRemovesThenAddsAtTheirNewPositions() {
		Object[] oldChildren = { "a", "b", "c", "d", "e" };
		Object[] newChildren = { "a", "x", "c", "y", "z", "e" };
		M3ViewerDelta d = M3ViewerDelta.between("root", oldChildren, newChildren);
		assertEquals("M3ViewerDelta[remove b, remove d, add x, add y, add z]", d.toString());
		assertEquals("root", d.parent(2));
		assertEquals(1, d.index(2));
		assertEquals(3, d.index(3));
		assertEquals(4, d.index(4));
		assertEquals(java.util.Arrays.asList(newChildren), applied(oldChildren, d));
		assertArrayEquals(new Object[] { "a", "b", "c", "d", "e" }, oldChildren, "the lists are not modified");
	}

	@Test
	public void betweenOfEqualListsIsEmptyAndEndsNeedNoTable() {
		assertTrue(M3ViewerDelta.between("root", new Object[] { "a", "b" }, new Object[] { "a", "b" }).isEmpty());
		assertTrue(M3ViewerDelta.between("root", new Object[0], new Object[0]).isEmpty());
		assertEquals("M3ViewerDelta[add c]",
				M3ViewerDelta.between("root", new Object[] { "a", "b" }, new Object[] { "a", "b", "c" }).toString());
		assertEquals("M3ViewerDelta[add z]",
				M3ViewerDelta.between("root", new Object[] { "a", "b" }, new Object[] { "z", "a", "b" }).toString());
		assertEquals("M3ViewerDelta[remove a, remove b]",
				M3ViewerDelta.between("root", new Object[] { "a", "b" }, new Object[0]).toString());
		assertEquals(0, M3ViewerDelta.between("root", new Object[0], new Object[] { "q" }).index(0));
	}

	@Test
	public void betweenFallsBackToOneRefreshWhenAddsAndRemovesCannotExpressTheChange() {
		M3ViewerDelta reordered = M3ViewerDelta.between("root", new Object[] { "a", "b", "c" },
				new Object[] { "c", "a", "b" });
		assertEquals(1, reordered.size());
		assertEquals(M3ViewerDelta.REFRESH, reordered.kind(0));
		assertEquals("root", reordered.element(0));
		assertEquals("M3ViewerDelta[refresh root]",
				M3ViewerDelta.between("root", new Object[] { "a", "b", "a" }, new Object[] { "b" }).toString(),
				"an element twice among the old children");
		assertEquals("M3ViewerDelta[refresh root]",
				M3ViewerDelta.between("root", new Object[] { "b" }, new Object[] { "x", "b", "x" }).toString(),
				"an element twice among the new children");
		assertEquals("M3ViewerDelta[refresh root]",
				M3ViewerDelta.between("root", new Object[] { "a", "b" }, new Object[] { "b", "b", "c" }).toString(),
				"a kept element twice among the new children");
	}

	@Test
	public void betweenRefusesNullListsAndElements() {
		assertThrows(IllegalArgumentException.class, () -> M3ViewerDelta.between("root", null, new Object[0]));
		assertThrows(IllegalArgumentException.class, () -> M3ViewerDelta.between("root", new Object[0], null));
		assertThrows(IllegalArgumentException.class,
				() -> M3ViewerDelta.between("root", new Object[] { "a", null }, new Object[] { "a", "b" }));
		assertThrows(IllegalArgumentException.class,
				() -> M3ViewerDelta.between("root", new Object[] { "a" }, new Object[] { "b", null, "c" }));
	}

	/* Elements that are equal without being identical, with colliding hash codes, as model objects often are. */
	private record Key(int id) {
		@Override
		public int hashCode() {
			return id & 3;
		}
	}

	@Test
	public void betweenOnRandomListsYieldsTheNewListWithTheFewestOperations() {
		java.util.Random random = new java.util.Random(20261116L);
		int refreshes = 0;
		for (int round = 0; round < 5000; round++) {
			int universe = 1 + random.nextInt(40);
			java.util.List<Object> oldList = new java.util.ArrayList<>(), newList = new java.util.ArrayList<>();
			for (int id = 0; id < universe; id++) {
				int fate = random.nextInt(4);
				if (fate != 0) {
					oldList.add(new Key(id));
				}
				if (fate != 1) {
					newList.add(new Key(id));
				}
			}
			boolean reorder = round % 10 == 0 && newList.size() > 1;
			if (reorder) {
				java.util.Collections.swap(newList, 0, newList.size() - 1);
			}
			Object[] oldChildren = oldList.toArray(), newChildren = newList.toArray();
			M3ViewerDelta d = M3ViewerDelta.between("root", oldChildren, newChildren);
			if (d.size() == 1 && d.kind(0) == M3ViewerDelta.REFRESH) {
				assertTrue(reorder, "a refresh only when kept elements changed their order (round " + round + ")");
				refreshes++;
				continue;
			}
			assertEquals(newList, applied(oldChildren, d), "round " + round);
			java.util.Set<Object> left = new java.util.HashSet<>(oldList), entered = new java.util.HashSet<>(newList);
			left.removeAll(newList);
			entered.removeAll(oldList);
			assertEquals(left.size() + entered.size(), d.size(), "one operation per element that left or entered");
		}
		assertTrue(refreshes > 100, "the reordered rounds were exercised: " + refreshes);
	}
}
