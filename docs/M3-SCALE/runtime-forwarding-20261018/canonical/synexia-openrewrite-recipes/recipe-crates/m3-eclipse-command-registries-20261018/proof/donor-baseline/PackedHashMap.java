// SPDX-License-Identifier: Apache-2.0
package com.synexia.common.collections.jdk;

import java.util.Arrays;
import java.util.Objects;

/**
 * Open-addressed hash index over stable payload slots and primitive encounter links.
 * Null keys/values are supported. Expected O(1) access; O(n) collision worst case.
 * Reversed and sequenced views share storage. No stored Entry or Node objects.
 * Backing arrays are allocated on first insertion; empty owners share zero-length lanes.
 */
public final class PackedHashMap<K, V> extends OrderedSlotMap<K, V> {
    private final Table table;
    private final boolean reversed;
    /** Insertion-order map. */
    public PackedHashMap() { this(false); }
    /** Select insertion order or access order, as with LinkedHashMap. */
    public PackedHashMap(boolean accessOrder) { this(new Table(accessOrder, false), false); }
    PackedHashMap(boolean accessOrder, boolean keyOnly) { this(new Table(accessOrder, keyOnly), false); }
    private PackedHashMap(Table table, boolean reversed) { this.table = table; this.reversed = reversed; }
    @Override int firstSlot() { return reversed ? table.last : table.first; }
    @Override int lastSlot() { return reversed ? table.first : table.last; }
    @Override int nextSlot(int slot) { return reversed ? table.before[slot] : table.after[slot]; }
    @Override int revision() { return table.modifications; }
    @Override int findSlot(Object key) { return table.find(key, SlotArrays.hash(key)); }
    @Override K keyAt(int slot) { return SlotArrays.get(table.keys, slot); }
    @Override V valueAt(int slot) { return table.keyOnly ? null : SlotArrays.get(table.values, slot); }
    @Override V writeAt(int slot, V value) {
        V old = valueAt(slot); if (!table.keyOnly) { table.values[slot] = value; } return old;
    }
    @Override V eraseAt(int slot) { V old = valueAt(slot); table.erase(slot); return old; }
    @Override void accessed(int slot) { if (table.accessOrder) { table.move(slot, false); } }
    @Override public int size() { return table.size; }
    @Override public V put(K key, V value) {
        int hash = SlotArrays.hash(key), s = table.find(key, hash);
        if (s < 0) { table.insert(key, value, hash, false); return null; }
        V old = writeAt(s, value); accessed(s); return old;
    }
    private V positioned(K key, V value, boolean first) {
        int hash = SlotArrays.hash(key), s = table.find(key, hash);
        if (s < 0) { table.insert(key, value, hash, first); return null; }
        V old = writeAt(s, value); table.move(s, first); return old;
    }
    @Override public V putFirst(K key, V value) { return positioned(key, value, !reversed); }
    @Override public V putLast(K key, V value) { return positioned(key, value, reversed); }
    @Override public Entry<K, V> firstEntry() { return snapshot(firstSlot()); }
    @Override public Entry<K, V> lastEntry() { return snapshot(lastSlot()); }
    private Entry<K, V> poll(int s) { Entry<K, V> e = snapshot(s); if (s >= 0) { eraseAt(s); } return e; }
    @Override public Entry<K, V> pollFirstEntry() { return poll(firstSlot()); }
    @Override public Entry<K, V> pollLastEntry() { return poll(lastSlot()); }
    @Override public PackedHashMap<K, V> reversed() { return new PackedHashMap<>(table, !reversed); }
    @Override public void clear() { table.clear(); }
    /** Validate index membership, encounter links and free-slot ownership. */
    public void verifyInvariants() { table.verify(); }

    private static final class Table {
        private Object[] keys = SlotArrays.EMPTY_OBJECTS, values = SlotArrays.EMPTY_OBJECTS;
        private int[] hashes = SlotArrays.EMPTY_INTS, before = SlotArrays.EMPTY_INTS,
                after = SlotArrays.EMPTY_INTS, positions = SlotArrays.EMPTY_INTS;
        private int[] index = SlotArrays.EMPTY_INTS;
        private int first = -1, last = -1, free = -1, used, occupied, size, modifications;
        private final boolean accessOrder, keyOnly;
        Table(boolean accessOrder, boolean keyOnly) {
            this.accessOrder = accessOrder; this.keyOnly = keyOnly;
        }
        int find(Object key, int hash) {
            if (index.length == 0) { return -1; }
            for (int p = hash & (index.length - 1); index[p] != 0; p = (p + 1) & (index.length - 1)) {
                int s = index[p] - 1;
                if (s >= 0 && hashes[s] == hash && Objects.equals(key, keys[s])) { return s; }
            }
            return -1;
        }
        int vacant(int hash) {
            int p = hash & (index.length - 1);
            while (index[p] > 0) { p = (p + 1) & (index.length - 1); } return p;
        }
        void rehash(int capacity) {
            int[] replacement = new int[capacity];
            for (int s = first; s >= 0; s = after[s]) {
                int p = hashes[s] & (capacity - 1);
                while (replacement[p] != 0) { p = (p + 1) & (capacity - 1); }
                replacement[p] = s + 1; positions[s] = p;
            }
            index = replacement; occupied = size;
        }
        void grow() {
            int n = SlotArrays.grow(keys.length);
            Object[] k = Arrays.copyOf(keys, n), v = keyOnly ? values : Arrays.copyOf(values, n);
            int[] h = Arrays.copyOf(hashes, n), b = Arrays.copyOf(before, n), a = Arrays.copyOf(after, n), p = Arrays.copyOf(positions, n);
            keys = k; values = v; hashes = h; before = b; after = a; positions = p;
        }
        void insert(Object key, Object value, int hash, boolean atFirst) {
            if (free < 0 && used == keys.length) { grow(); }
            if (index.length == 0) { rehash(16); }
            else if (occupied + 1 >= index.length - index.length / 3) {
                rehash(size + 1 >= index.length - index.length / 3 ? SlotArrays.doubled(index.length) : index.length);
            }
            int p = vacant(hash), s;
            if (free < 0) { s = used++; } else { s = free; free = after[s]; }
            if (index[p] == 0) { occupied++; }
            keys[s] = key; if (!keyOnly) { values[s] = value; } hashes[s] = hash;
            index[p] = s + 1; positions[s] = p; link(s, atFirst); size++; modifications++;
        }
        void link(int s, boolean atFirst) {
            before[s] = atFirst ? -1 : last; after[s] = atFirst ? first : -1;
            if (atFirst) { if (first < 0) { last = s; } else { before[first] = s; } first = s; }
            else { if (last < 0) { first = s; } else { after[last] = s; } last = s; }
        }
        void unlink(int s) {
            int b = before[s], a = after[s];
            if (b < 0) { first = a; } else { after[b] = a; }
            if (a < 0) { last = b; } else { before[a] = b; }
        }
        void move(int s, boolean atFirst) {
            if (s == (atFirst ? first : last)) { return; }
            unlink(s); link(s, atFirst); modifications++;
        }
        void erase(int s) {
            unlink(s); index[positions[s]] = -1; keys[s] = null; if (!keyOnly) { values[s] = null; }
            before[s] = -1; positions[s] = -1; after[s] = free; free = s; size--; modifications++;
        }
        void clear() {
            if (size == 0) { return; }
            Arrays.fill(keys, null); Arrays.fill(values, null); Arrays.fill(index, 0);
            first = -1; last = -1; free = -1; used = 0; occupied = 0; size = 0; modifications++;
        }
        void verify() {
            boolean[] seen = new boolean[used]; int count = 0, previous = -1, buckets = 0;
            for (int s = first; s >= 0; s = after[s]) {
                if (s >= used || seen[s] || before[s] != previous || index[positions[s]] != s + 1 || find(keys[s], hashes[s]) != s) { throw new AssertionError("hash links"); }
                seen[s] = true; previous = s; count++;
            }
            if (count != size || previous != last) { throw new AssertionError("hash size"); }
            for (int p = 0; p < index.length; p++) { if (index[p] > 0) { buckets++; if (positions[index[p] - 1] != p) { throw new AssertionError("bucket"); } } }
            if (buckets != size) { throw new AssertionError("buckets"); }
            for (int s = free; s >= 0; s = after[s]) {
                if (s >= used || seen[s] || keys[s] != null || !keyOnly && values[s] != null) { throw new AssertionError("hash free"); }
                seen[s] = true; count++;
            }
            if (count != used) { throw new AssertionError("hash ownership"); }
        }
    }
}
