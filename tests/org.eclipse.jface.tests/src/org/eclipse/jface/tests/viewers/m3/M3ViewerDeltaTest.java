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
}
