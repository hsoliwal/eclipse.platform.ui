/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 * Copyright (c) 2026 Hitesh Soliwal and Synexia contributors.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Peter Shipton - original hashtable implementation
 *     Nick Edgar - added element comparer support
 *     Hitesh Soliwal - M3 packed map forwarding, 2026
 *******************************************************************************/

package org.eclipse.jface.viewers;

import org.eclipse.jface.internal.m3.M3ViewerMap;

/** JFace compatibility facade for the contract-specific M3 packed viewer map. */
/* package */final class CustomHashtable {
	public static final int DEFAULT_CAPACITY = 13;
	private final M3ViewerMap storage;

	public CustomHashtable() { this(DEFAULT_CAPACITY); }
	public CustomHashtable(int capacity) { this(capacity, null); }
	public CustomHashtable(IElementComparer comparer) { this(DEFAULT_CAPACITY, comparer); }
	public CustomHashtable(int capacity, IElementComparer comparer) {
		storage = new M3ViewerMap(capacity, comparer);
	}
	public CustomHashtable(CustomHashtable table, IElementComparer comparer) {
		storage = new M3ViewerMap(table.storage, comparer);
	}
	public IElementComparer getComparer() { return storage.getComparer(); }
	public boolean containsKey(Object key) { return storage.containsKey(key); }
	public Object get(Object key) { return storage.get(key); }
	public Object put(Object key, Object value) { return storage.put(key, value); }
	public Object remove(Object key) { return storage.remove(key); }
	public Object remove(Object key, int hash) { return storage.remove(key, hash); }
	public int size() { return storage.size(); }
	@Override
	public String toString() { return storage.toString(); }
}
