// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: late-binding sequenced-view spliterators.
// SPDX-License-Identifier: Apache-2.0
package com.synexia.common.collections.jdk;

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.Set;
import java.util.Spliterator;

/** Direct-slot sequenced views shared by hash encounter order and AVL key order. */
abstract class OrderedSlotMap<K, V> extends SlotMap<K, V> implements SequencedMap<K, V> {
    abstract int lastSlot();
    @Override public abstract OrderedSlotMap<K, V> reversed();
    @Override public SequencedSet<K> keySet() { return sequencedKeySet(); }
    @Override public SequencedSet<Entry<K, V>> entrySet() { return sequencedEntrySet(); }
    @Override public SequencedCollection<V> values() { return sequencedValues(); }
    @Override public SequencedSet<K> sequencedKeySet() { return new SetView<>(0); }
    @Override public SequencedCollection<V> sequencedValues() { return new View<>(1); }
    @Override public SequencedSet<Entry<K, V>> sequencedEntrySet() { return new SetView<>(2); }
    private class View<T> extends AbstractCollection<T> implements SequencedCollection<T> {
        final int kind;
        View(int kind) { this.kind = kind; }
        @Override public int size() { return OrderedSlotMap.this.size(); }
        @Override public Spliterator<T> spliterator() { return SlotSpliterators.ordered(this, kind != 1); }
        @Override public Iterator<T> iterator() { return slots(kind); }
        @Override public void clear() { OrderedSlotMap.this.clear(); }
        @Override public boolean contains(Object value) {
            if (kind == 0) { return containsKey(value); }
            if (kind == 1) { return containsValue(value); }
            if (!(value instanceof Entry<?, ?> e)) { return false; }
            int s = findSlot(e.getKey()); return s >= 0 && Objects.equals(valueAt(s), e.getValue());
        }
        @Override public boolean remove(Object value) {
            if (kind == 0) { int s = findSlot(value); if (s < 0) { return false; } eraseAt(s); return true; }
            if (kind == 2) { return value instanceof Entry<?, ?> e && OrderedSlotMap.this.remove(e.getKey(), e.getValue()); }
            return super.remove(value);
        }
        @SuppressWarnings("unchecked") private T endpoint(boolean last, boolean remove) {
            int s = last ? lastSlot() : firstSlot(); if (s < 0) { throw new NoSuchElementException(); }
            Object value = kind == 0 ? keyAt(s) : kind == 1 ? valueAt(s) : remove ? new SimpleEntry<>(keyAt(s), valueAt(s)) : new LiveEntry(keyAt(s));
            if (remove) { eraseAt(s); } return (T) value;
        }
        @Override public T getFirst() { return endpoint(false, false); }
        @Override public T getLast() { return endpoint(true, false); }
        @Override public T removeFirst() { return endpoint(false, true); }
        @Override public T removeLast() { return endpoint(true, true); }
        @Override public void addFirst(T value) { throw new UnsupportedOperationException("map view"); }
        @Override public void addLast(T value) { throw new UnsupportedOperationException("map view"); }
        @Override public SequencedCollection<T> reversed() { return OrderedSlotMap.this.reversed().new View<>(kind); }
    }
    private final class SetView<T> extends View<T> implements SequencedSet<T> {
        SetView(int kind) { super(kind); }
        @Override public SequencedSet<T> reversed() { return OrderedSlotMap.this.reversed().new SetView<>(kind); }
        @Override public int hashCode() {
            if (kind == 2) { return OrderedSlotMap.this.hashCode(); }
            int hash = 0; for (int s = firstSlot(); s >= 0; s = nextSlot(s)) { hash += Objects.hashCode(keyAt(s)); } return hash;
        }
        @Override public boolean equals(Object o) {
            if (o == this) { return true; }
            if (!(o instanceof Set<?> set) || set.size() != size()) { return false; }
            try { return containsAll(set); } catch (ClassCastException | NullPointerException ignored) { return false; }
        }
    }
}
