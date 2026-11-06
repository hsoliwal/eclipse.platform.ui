/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.core.tests.internal.databinding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.eclipse.core.internal.databinding.observable.Queue;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class QueueArrayStorageTest {

	@Test
	public void emptyDequeueRetainsItsExceptionAndLeavesQueueReusable() {
		Queue queue = new Queue();
		for (int i = 0; i < 100; i++) {
			assertTrue(queue.isEmpty());
			IllegalStateException exception = assertThrows(IllegalStateException.class, queue::dequeue);
			assertSame(IllegalStateException.class, exception.getClass());
			assertNull(exception.getMessage());
			Object value = new Object();
			queue.enqueue(value);
			assertFalse(queue.isEmpty());
			assertSame(value, queue.dequeue());
		}
	}

	@Test
	public void nullAndRepeatedPayloadsKeepTheirIdentityAndOrder() {
		Queue queue = new Queue();
		Object identity = new Object() {
			@Override
			public boolean equals(Object other) {
				throw new AssertionError("Queue must not compare payloads");
			}

			@Override
			public int hashCode() {
				throw new AssertionError("Queue must not hash payloads");
			}
		};
		queue.enqueue(null);
		queue.enqueue(identity);
		queue.enqueue(identity);
		queue.enqueue(null);
		queue.enqueue(queue);
		assertFalse(queue.isEmpty());
		assertNull(queue.dequeue());
		assertSame(identity, queue.dequeue());
		assertSame(identity, queue.dequeue());
		assertNull(queue.dequeue());
		assertSame(queue, queue.dequeue());
		assertTrue(queue.isEmpty());
	}

	@Test
	public void wrappedBacklogsGrowWithoutLosingOrReorderingPayloads() {
		Queue queue = new Queue();
		LinkedList<Object> expected = new LinkedList<>();
		for (int round = 0; round < 14; round++) {
			int count = 1 << round;
			for (int i = 0; i < count; i++) {
				Object value = i % 11 == 0 ? null : new Object();
				expected.addLast(value);
				queue.enqueue(value);
			}
			for (int i = 0; i < count / 2; i++) {
				assertSame(expected.removeFirst(), queue.dequeue());
			}
		}
		while (!expected.isEmpty()) {
			assertSame(expected.removeFirst(), queue.dequeue());
		}
		assertTrue(queue.isEmpty());
	}

	@Test
	public void mixedOperationsMatchAnIndependentFifoModel() {
		Queue queue = new Queue();
		LinkedList<Object> expected = new LinkedList<>();
		Random random = new Random(0x5eed_0248);
		Object repeated = new Object();
		for (int i = 0; i < 50_000; i++) {
			if (expected.isEmpty() || random.nextInt(5) < 3) {
				int choice = random.nextInt(4);
				Object value = choice == 0 ? null : choice == 1 ? repeated : new Object();
				expected.addLast(value);
				queue.enqueue(value);
			} else {
				assertSame(expected.removeFirst(), queue.dequeue());
			}
			assertEquals(expected.isEmpty(), queue.isEmpty());
		}
		while (!expected.isEmpty()) {
			assertSame(expected.removeFirst(), queue.dequeue());
		}
		assertTrue(queue.isEmpty());
	}

	@Test
	public void emptiedQueueCanWrapAndBeReusedWithoutStaleValues() {
		Queue queue = new Queue();
		Object first = new Object();
		Object second = new Object();
		for (int round = 0; round < 10_000; round++) {
			queue.enqueue(first);
			queue.enqueue(null);
			queue.enqueue(second);
			assertSame(first, queue.dequeue());
			assertNull(queue.dequeue());
			assertSame(second, queue.dequeue());
			assertTrue(queue.isEmpty());
		}
	}

	@Test
	public void methodsKeepTheirOriginalOverrideDispatchBoundaries() {
		CountingQueue queue = new CountingQueue();
		Object value = new Object();
		queue.enqueue(value);
		assertEquals(1, queue.enqueues);
		assertEquals(0, queue.dequeues);
		assertEquals(0, queue.emptyChecks);
		assertSame(value, queue.dequeue());
		assertThrows(IllegalStateException.class, queue::dequeue);
		assertEquals(1, queue.enqueues);
		assertEquals(2, queue.dequeues);
		assertEquals(0, queue.emptyChecks);
		assertTrue(queue.isEmpty());
		assertEquals(1, queue.emptyChecks);
	}

	@Test
	public void dequeueDoesNotConsultAnOverriddenIsEmpty() {
		Queue queue = new Queue() {
			@Override
			public boolean isEmpty() {
				throw new AssertionError("Internal storage operations must not invoke this override");
			}
		};
		Object value = new Object();
		queue.enqueue(value);
		assertSame(value, queue.dequeue());
		assertThrows(IllegalStateException.class, queue::dequeue);
	}

	@Test
	public void removedPayloadsAreNotReachableFromTheQueue() throws Exception {
		Queue queue = new Queue();
		Set<Object> payloads = Collections.newSetFromMap(new IdentityHashMap<>());
		Object[] values = new Object[513];
		for (int i = 0; i < values.length; i++) {
			values[i] = new Object();
			payloads.add(values[i]);
			queue.enqueue(values[i]);
		}
		for (int i = 0; i < 256; i++) {
			assertSame(values[i], queue.dequeue());
		}
		Set<Object> reachable = retainedGraph(queue, payloads);
		for (int i = 0; i < values.length; i++) {
			assertEquals(i >= 256, reachable.contains(values[i]), "Retained payload " + i);
		}
		while (!queue.isEmpty()) {
			queue.dequeue();
		}
		reachable = retainedGraph(queue, payloads);
		for (Object value : values) {
			assertFalse(reachable.contains(value));
		}
	}

	@Test
	@Tag("array-storage")
	public void retainedStorageDoesNotCreateOneHolderPerPayload() throws Exception {
		Queue queue = new Queue();
		Set<Object> payloads = Collections.newSetFromMap(new IdentityHashMap<>());
		for (int i = 0; i < 4096; i++) {
			Object payload = new Object();
			payloads.add(payload);
			queue.enqueue(payload);
		}
		Set<Object> retained = retainedGraph(queue, payloads);
		retained.removeAll(payloads);
		retained.remove(queue);
		assertEquals(1, retained.size(), "Queue should retain one backing array, not a holder per payload");
		assertInstanceOf(Object[].class, retained.iterator().next());
	}

	@Test
	@Tag("array-storage")
	public void capacityGrowthSaturatesBeforeIntegerOverflow() throws Exception {
		Method capacity = Queue.class.getDeclaredMethod("nextCapacity", int.class);
		capacity.setAccessible(true);
		assertEquals(16, capacity.invoke(null, 0));
		assertEquals(32, capacity.invoke(null, 16));
		assertEquals(2_147_483_646, capacity.invoke(null, 1_073_741_823));
		assertEquals(Integer.MAX_VALUE, capacity.invoke(null, 1_073_741_824));
		assertEquals(Integer.MAX_VALUE, capacity.invoke(null, Integer.MAX_VALUE - 1));
		InvocationTargetException failure = assertThrows(InvocationTargetException.class,
				() -> capacity.invoke(null, Integer.MAX_VALUE));
		assertInstanceOf(OutOfMemoryError.class, failure.getCause());
	}

	private static Set<Object> retainedGraph(Queue queue, Set<Object> payloads) throws Exception {
		Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
		List<Object> pending = new ArrayList<>();
		pending.add(queue);
		while (!pending.isEmpty()) {
			Object value = pending.remove(pending.size() - 1);
			if (value == null || !seen.add(value) || payloads.contains(value)) {
				continue;
			}
			if (value instanceof Object[] array) {
				Collections.addAll(pending, array);
			} else {
				for (Class<?> type = value.getClass(); type != Object.class; type = type.getSuperclass()) {
					for (Field field : type.getDeclaredFields()) {
						if (!Modifier.isStatic(field.getModifiers()) && !field.getType().isPrimitive()) {
							field.setAccessible(true);
							pending.add(field.get(value));
						}
					}
				}
			}
		}
		return seen;
	}

	private static class CountingQueue extends Queue {
		int enqueues;
		int dequeues;
		int emptyChecks;

		@Override
		public void enqueue(Object value) {
			enqueues++;
			super.enqueue(value);
		}

		@Override
		public Object dequeue() {
			dequeues++;
			return super.dequeue();
		}

		@Override
		public boolean isEmpty() {
			emptyChecks++;
			return super.isEmpty();
		}
	}
}
