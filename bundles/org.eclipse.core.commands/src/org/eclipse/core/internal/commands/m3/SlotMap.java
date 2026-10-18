// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: package donation to Eclipse core.commands and Eclipse NLS comments; storage algorithms unchanged.
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.core.internal.commands.m3;

import java.lang.ref.Reference;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/** Shared JDK semantics over integer slots; internal traversal never constructs entries. */
abstract class SlotMap<K, V> extends AbstractMap<K, V> {
    abstract int firstSlot();
    abstract int nextSlot(int slot);
    abstract int findSlot(Object key);
    abstract K keyAt(int slot);
    abstract V valueAt(int slot);
    abstract V writeAt(int slot, V value);
    abstract V eraseAt(int slot);
    abstract int revision();
    boolean live(int slot) { return true; }
    void accessed(int slot) { }
    final void check(int expected) {
        if (revision() != expected) { throw new ConcurrentModificationException(); }
    }
    @Override public boolean containsKey(Object key) { return findSlot(key) >= 0; }
    @Override public V get(Object key) { return getOrDefault(key, null); }
    @Override public V getOrDefault(Object key, V fallback) {
        int slot = findSlot(key);
        if (slot < 0) { return fallback; }
        V value = valueAt(slot); accessed(slot); return value;
    }
    @Override public boolean containsValue(Object value) {
        for (int s = firstSlot(); s >= 0; s = nextSlot(s)) {
            if (live(s) && Objects.equals(value, valueAt(s))) { return true; }
        }
        return false;
    }
    @Override public V remove(Object key) {
        int s = findSlot(key); return s < 0 ? null : eraseAt(s);
    }
    @Override public boolean remove(Object key, Object value) {
        int s = findSlot(key);
        if (s < 0 || !Objects.equals(value, valueAt(s))) { return false; }
        eraseAt(s); return true;
    }
    @Override public void putAll(Map<? extends K, ? extends V> source) {
        if (source != this) { source.forEach(this::put); }
    }
    @Override public V putIfAbsent(K key, V value) {
        int s = findSlot(key);
        if (s < 0) { return put(key, value); }
        V old = valueAt(s);
        if (old == null) { writeAt(s, value); }
        accessed(s); return old;
    }
    @Override public V replace(K key, V value) {
        int s = findSlot(key);
        if (s < 0) { return null; }
        V old = writeAt(s, value); accessed(s); return old;
    }
    @Override public boolean replace(K key, V old, V value) {
        int s = findSlot(key);
        if (s < 0 || !Objects.equals(old, valueAt(s))) { return false; }
        writeAt(s, value); accessed(s); return true;
    }
    @Override public V computeIfAbsent(K key, Function<? super K, ? extends V> action) {
        Objects.requireNonNull(action);
        int s = findSlot(key);
        if (s >= 0 && valueAt(s) != null) { V v = valueAt(s); accessed(s); return v; }
        int expected = revision(); V v = action.apply(key); check(expected);
        if (v != null) { put(key, v); }
        return v;
    }
    @Override public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> action) {
        Objects.requireNonNull(action);
        int s = findSlot(key);
        if (s < 0 || valueAt(s) == null) { return null; }
        int expected = revision(); V v = action.apply(key, valueAt(s)); check(expected);
        if (v == null) { eraseAt(s); } else { writeAt(s, v); accessed(s); }
        return v;
    }
    @Override public V compute(K key, BiFunction<? super K, ? super V, ? extends V> action) {
        Objects.requireNonNull(action); int s = findSlot(key), expected = revision();
        V v = action.apply(key, s < 0 ? null : valueAt(s)); check(expected);
        if (v == null) { if (s >= 0) { eraseAt(s); } } else { put(key, v); }
        return v;
    }
    @Override public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> action) {
        Objects.requireNonNull(action); Objects.requireNonNull(value);
        int s = findSlot(key), expected = revision(); V old = s < 0 ? null : valueAt(s);
        V v = old == null ? value : action.apply(old, value); check(expected);
        if (v == null) { if (s >= 0) { eraseAt(s); } } else { put(key, v); }
        return v;
    }
    @Override public void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action); int s = firstSlot(), expected = revision();
        while (s >= 0) {
            K key = keyAt(s);
            if (live(s)) { action.accept(key, valueAt(s)); check(expected); }
            s = nextSlot(s);
        }
    }
    @Override public void replaceAll(BiFunction<? super K, ? super V, ? extends V> action) {
        Objects.requireNonNull(action); int s = firstSlot(), expected = revision();
        while (s >= 0) {
            K key = keyAt(s);
            if (live(s)) { V value = action.apply(key, valueAt(s)); check(expected); writeAt(s, value); }
            s = nextSlot(s);
        }
    }
    @Override public int hashCode() {
        int hash = 0;
        for (int s = firstSlot(); s >= 0; s = nextSlot(s)) {
            if (live(s)) { hash += Objects.hashCode(keyAt(s)) ^ Objects.hashCode(valueAt(s)); }
        }
        return hash;
    }
    @Override public boolean equals(Object other) {
        if (other == this) { return true; }
        if (!(other instanceof Map<?, ?> map) || map.size() != size()) { return false; }
        try {
            for (int s = firstSlot(); s >= 0; s = nextSlot(s)) {
                K k = keyAt(s); V v = valueAt(s);
                if (live(s) && (!Objects.equals(v, map.get(k)) || v == null && !map.containsKey(k))) { return false; }
            }
        } catch (ClassCastException | NullPointerException ignored) { return false; }
        return true;
    }
    @Override public String toString() {
        StringBuilder b = new StringBuilder("{"); boolean comma = false; //$NON-NLS-1$
        for (int s = firstSlot(); s >= 0; s = nextSlot(s)) {
            if (!live(s)) { continue; }
            if (comma) { b.append(", "); } comma = true; //$NON-NLS-1$
            K k = keyAt(s); V v = valueAt(s);
            b.append(k == this ? "(this Map)" : k).append('=').append(v == this ? "(this Map)" : v); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return b.append('}').toString();
    }
    final Entry<K, V> snapshot(int slot) {
        return slot < 0 ? null : new SimpleImmutableEntry<>(keyAt(slot), valueAt(slot));
    }
    final class LiveEntry implements Entry<K, V> {
        private final K key;
        LiveEntry(K key) { this.key = key; }
        @Override public K getKey() { return key; }
        @Override public V getValue() { int s = findSlot(key); return s < 0 ? null : valueAt(s); }
        @Override public V setValue(V value) {
            int s = findSlot(key);
            if (s < 0) { throw new IllegalStateException("entry no longer present"); } //$NON-NLS-1$
            return writeAt(s, value);
        }
        @Override public boolean equals(Object o) {
            return o instanceof Entry<?, ?> e && Objects.equals(key, e.getKey()) && Objects.equals(getValue(), e.getValue());
        }
        @Override public int hashCode() { return Objects.hashCode(key) ^ Objects.hashCode(getValue()); }
        @Override public String toString() { return key + "=" + getValue(); } //$NON-NLS-1$
    }
    final <T> Iterator<T> slots(int kind) {
        return new Iterator<>() {
            private int cursor = firstSlot();
            private int expected = revision();
            private int last = -1;
            private K heldKey;
            private K lastKey;
            private boolean prepared;
            @Override public boolean hasNext() {
                Reference.reachabilityFence(lastKey);
                check(expected);
                if (!prepared) {
                    while (cursor >= 0) {
                        heldKey = keyAt(cursor);
                        if (live(cursor)) { prepared = true; break; }
                        cursor = nextSlot(cursor);
                    }
                }
                return cursor >= 0;
            }
            @Override @SuppressWarnings("unchecked") public T next() {
                if (!hasNext()) { throw new NoSuchElementException(); }
                last = cursor; lastKey = heldKey; cursor = nextSlot(cursor); prepared = false;
                Object result = kind == 0 ? heldKey : kind == 1 ? valueAt(last) : new LiveEntry(heldKey);
                heldKey = null; return (T) result;
            }
            @Override public void remove() {
                check(expected);
                if (last < 0) { throw new IllegalStateException(); }
                eraseAt(last); expected = revision(); last = -1; lastKey = null;
            }
        };
    }
    @Override public Set<K> keySet() {
        return new AbstractSet<>() {
            @Override public int size() { return SlotMap.this.size(); }
            @Override public boolean contains(Object k) { return containsKey(k); }
            @Override public boolean remove(Object k) {
                int s = findSlot(k); if (s < 0) { return false; } eraseAt(s); return true;
            }
            @Override public void clear() { SlotMap.this.clear(); }
            @Override public Iterator<K> iterator() { return slots(0); }
        };
    }
    @Override public Collection<V> values() {
        return new AbstractCollection<>() {
            @Override public int size() { return SlotMap.this.size(); }
            @Override public boolean contains(Object v) { return containsValue(v); }
            @Override public void clear() { SlotMap.this.clear(); }
            @Override public Iterator<V> iterator() { return slots(1); }
        };
    }
    @Override public Set<Entry<K, V>> entrySet() {
        return new AbstractSet<>() {
            @Override public int size() { return SlotMap.this.size(); }
            @Override public void clear() { SlotMap.this.clear(); }
            @Override public Iterator<Entry<K, V>> iterator() { return slots(2); }
            @Override public boolean contains(Object o) {
                if (!(o instanceof Entry<?, ?> e)) { return false; }
                int s = findSlot(e.getKey()); return s >= 0 && Objects.equals(valueAt(s), e.getValue());
            }
            @Override public boolean remove(Object o) {
                return o instanceof Entry<?, ?> e && SlotMap.this.remove(e.getKey(), e.getValue());
            }
        };
    }
}
