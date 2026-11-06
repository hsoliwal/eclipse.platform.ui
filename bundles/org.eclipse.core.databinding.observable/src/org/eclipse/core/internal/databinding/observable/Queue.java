/*******************************************************************************
 * Copyright (c) 2007, 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.databinding.observable;

/**
 * Created to avoid a dependency on java.util.LinkedList, see bug 205224.
 *
 * @since 1.1
 */
public class Queue {

	private Object[] elements;
	private int head;
	private int size;

	/**
	 * Adds the given object to the end of the queue.
	 */
	public void enqueue(Object o) {
		if (elements == null || size == elements.length) {
			grow();
		}
		int remaining = elements.length - head;
		int tail = size < remaining ? head + size : size - remaining;
		elements[tail] = o;
		size++;
	}

	private void grow() {
		int capacity = elements == null ? 0 : elements.length;
		Object[] newElements = new Object[nextCapacity(capacity)];
		if (capacity != 0) {
			int remaining = capacity - head;
			System.arraycopy(elements, head, newElements, 0, remaining);
			System.arraycopy(elements, 0, newElements, remaining, head);
		}
		elements = newElements;
		head = 0;
	}

	private static int nextCapacity(int capacity) {
		if (capacity == 0) {
			return 16;
		}
		if (capacity == Integer.MAX_VALUE) {
			throw new OutOfMemoryError();
		}
		return capacity <= Integer.MAX_VALUE / 2 ? capacity * 2 : Integer.MAX_VALUE;
	}

	/**
	 * Returns the first object in the queue. The queue must not be empty.
	 *
	 * @return the first object
	 */
	public Object dequeue() {
		if (size == 0) {
			throw new IllegalStateException();
		}
		Object object = elements[head];
		elements[head] = null;
		head++;
		if (head == elements.length) {
			head = 0;
		}
		size--;
		return object;
	}

	/**
	 * Returns <code>true</code> if the list is empty.
	 *
	 * @return <code>true</code> if the list is empty
	 */
	public boolean isEmpty() {
		return size == 0;
	}
}
