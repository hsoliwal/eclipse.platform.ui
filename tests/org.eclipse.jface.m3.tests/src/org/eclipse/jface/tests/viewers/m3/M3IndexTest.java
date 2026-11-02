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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.eclipse.jface.internal.m3.M3FirstIndex;
import org.eclipse.jface.internal.m3.M3IdentityIndex;
import org.eclipse.jface.internal.m3.M3ViewerMode;
import org.eclipse.jface.viewers.IElementComparer;
import org.junit.jupiter.api.Test;

/**
 * Headless tests of the JFace viewport index lanes: every answer is checked
 * against the linear scan the viewers use in the original code.
 */
public class M3IndexTest {

	/** Equality by id, as in models with value equality. */
	record Key(int id, int copy) {
		@Override
		public boolean equals(Object o) {
			return o instanceof Key k && k.id == id;
		}

		@Override
		public int hashCode() {
			return id;
		}
	}

	/** A comparer that compares and hashes on the copy instead of the id. */
	static final IElementComparer BY_COPY = new IElementComparer() {
		@Override
		public boolean equals(Object a, Object b) {
			return ((Key) a).copy == ((Key) b).copy;
		}

		@Override
		public int hashCode(Object element) {
			return ((Key) element).copy;
		}
	};

	static int scan(Object[] rows, Object key, IElementComparer comparer) {
		for (int i = 0; i < rows.length; i++) {
			if (rows[i] != null && (comparer == null ? key.equals(rows[i]) : comparer.equals(key, rows[i]))) {
				return i;
			}
		}
		return -1;
	}

	@Test
	public void firstIndexAnswersLikeTheScan() {
		Random random = new Random(7);
		for (int round = 0; round < 50; round++) {
			int n = random.nextInt(300);
			Object[] rows = new Object[n];
			for (int i = 0; i < n; i++) {
				rows[i] = random.nextInt(10) == 0 ? null : new Key(random.nextInt(60), random.nextInt(5));
			}
			for (IElementComparer comparer : new IElementComparer[] { null, BY_COPY }) {
				M3FirstIndex index = new M3FirstIndex(n, comparer);
				for (int i = 0; i < n; i++) {
					index.add(rows[i], i);
				}
				for (int q = 0; q < 100; q++) {
					Key key = new Key(random.nextInt(70), random.nextInt(6));
					assertEquals(scan(rows, key, comparer), index.first(key), "round " + round + " key " + key);
				}
			}
		}
	}

	@Test
	public void firstIndexKeepsTheFirstOfEqualElements() {
		M3FirstIndex index = new M3FirstIndex(4, null);
		Key a0 = new Key(1, 0);
		Key a1 = new Key(1, 1);
		index.add(a0, 3);
		index.add(a1, 5);
		assertEquals(3, index.first(a1));
		assertEquals(-1, index.first(new Key(2, 0)));
	}

	@Test
	public void firstIndexAcceptsZeroAndOneElement() {
		M3FirstIndex empty = new M3FirstIndex(0, null);
		assertEquals(-1, empty.first("x"));
		M3FirstIndex one = new M3FirstIndex(1, null);
		one.add("x", 0);
		assertEquals(0, one.first("x"));
		assertEquals(-1, one.first("y"));
	}

	@Test
	public void identityIndexAnswersPositionsByIdentity() {
		Random random = new Random(11);
		for (int round = 0; round < 30; round++) {
			int n = random.nextInt(200);
			Object[] items = new Object[n];
			List<Object> pool = new ArrayList<>();
			for (int i = 0; i < n; i++) {
				if (random.nextInt(8) == 0) {
					items[i] = null;
				} else if (!pool.isEmpty() && random.nextInt(4) == 0) {
					items[i] = pool.get(random.nextInt(pool.size()));
				} else {
					items[i] = new Key(i, 0);
					pool.add(items[i]);
				}
			}
			M3IdentityIndex index = new M3IdentityIndex(items);
			Map<Object, Integer> first = new HashMap<>();
			for (int i = 0; i < n; i++) {
				if (items[i] != null) {
					first.putIfAbsent(new IdentityKey(items[i]), Integer.valueOf(i));
				}
			}
			for (Object item : pool) {
				assertEquals(first.get(new IdentityKey(item)).intValue(), index.positionOf(item));
			}
			assertEquals(-1, index.positionOf(new Key(0, 0)));
			assertEquals(-1, index.positionOf(null));
		}
		assertEquals(-1, new M3IdentityIndex(new Object[0]).positionOf("x"));
	}

	record IdentityKey(Object o) {
		@Override
		public boolean equals(Object other) {
			return other instanceof IdentityKey k && k.o == o;
		}

		@Override
		public int hashCode() {
			return System.identityHashCode(o);
		}
	}

	@Test
	public void modeReadsTheProperties() {
		assertEquals("original", M3ViewerMode.ORIGINAL);
		assertEquals("viewport", M3ViewerMode.VIEWPORT);
		String expected = System.getProperty(M3ViewerMode.PROPERTY, M3ViewerMode.ORIGINAL).trim();
		assertEquals(M3ViewerMode.VIEWPORT.equals(expected), M3ViewerMode.ENABLED);
		assertTrue(!M3ViewerMode.ENABLED || M3ViewerMode.ANY);
		System.setProperty(M3ViewerMode.PROPERTY + ".M3IndexTestViewer", M3ViewerMode.VIEWPORT);
		try {
			assertTrue(M3ViewerMode.enabled("M3IndexTestViewer"));
			assertEquals(M3ViewerMode.ENABLED, M3ViewerMode.enabled("NoSuchViewer"));
		} finally {
			System.clearProperty(M3ViewerMode.PROPERTY + ".M3IndexTestViewer");
		}
		System.setProperty(M3ViewerMode.PROPERTY + ".M3IndexTestViewer", " ");
		try {
			assertEquals(M3ViewerMode.ENABLED, M3ViewerMode.enabled("M3IndexTestViewer"));
		} finally {
			System.clearProperty(M3ViewerMode.PROPERTY + ".M3IndexTestViewer");
		}
		assertFalse(M3ViewerMode.enabled("M3IndexTestViewer") && !M3ViewerMode.ENABLED);
	}
}
