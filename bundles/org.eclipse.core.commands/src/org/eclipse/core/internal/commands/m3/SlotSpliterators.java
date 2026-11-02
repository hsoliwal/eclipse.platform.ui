// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: package donation to Eclipse core.commands and Eclipse NLS comments; storage algorithms unchanged.
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: late-binding collection adapters and comparator-preserving splits.
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.core.internal.commands.m3;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

/** Preserve encounter/comparator semantics for sequential and parallel stream boundaries. */
final class SlotSpliterators {
    private SlotSpliterators() { }
    static <E> Spliterator<E> ordered(Iterator<E> iterator, int size, boolean distinct) {
        return Spliterators.spliterator(iterator, size, Spliterator.ORDERED | (distinct ? Spliterator.DISTINCT : 0));
    }
    /** Bind iterator and size at first traversal/split/size query, as the JDK owners do. */
    static <E> Spliterator<E> ordered(Collection<E> collection, boolean distinct) {
        return Spliterators.spliterator(collection,
                Spliterator.ORDERED | (distinct ? Spliterator.DISTINCT : 0));
    }
    static <E> Spliterator<E> concurrent(Iterator<E> iterator, boolean ordered, boolean distinct) {
        int flags = Spliterator.CONCURRENT | Spliterator.NONNULL;
        if (ordered) { flags |= Spliterator.ORDERED; }
        if (distinct) { flags |= Spliterator.DISTINCT; }
        return Spliterators.spliteratorUnknownSize(iterator, flags);
    }
    static <E> Spliterator<E> sorted(Iterator<E> iterator, int size, Comparator<? super E> comparator) {
        Spliterator<E> source = new Spliterators.AbstractSpliterator<>(size, Spliterator.ORDERED | Spliterator.SORTED | Spliterator.DISTINCT) {
            @Override public boolean tryAdvance(Consumer<? super E> action) {
                Objects.requireNonNull(action); if (!iterator.hasNext()) { return false; } action.accept(iterator.next()); return true;
            }
        };
        return withComparator(source, comparator);
    }
    static <E> Spliterator<E> sorted(Collection<E> collection, Comparator<? super E> comparator) {
        return withComparator(Spliterators.spliterator(collection,
                Spliterator.ORDERED | Spliterator.SORTED | Spliterator.DISTINCT), comparator);
    }
    /** JDK iterator batching returns array splits; retain custom ordering on each child. */
    private static <E> Spliterator<E> withComparator(
            Spliterator<E> source, Comparator<? super E> comparator) {
        return new Spliterator<>() {
            @Override public boolean tryAdvance(Consumer<? super E> action) {
                return source.tryAdvance(action);
            }
            @Override public void forEachRemaining(Consumer<? super E> action) {
                source.forEachRemaining(action);
            }
            @Override public Spliterator<E> trySplit() {
                Spliterator<E> prefix = source.trySplit();
                return prefix == null ? null : withComparator(prefix, comparator);
            }
            @Override public long estimateSize() { return source.estimateSize(); }
            @Override public int characteristics() { return source.characteristics(); }
            @Override public Comparator<? super E> getComparator() { return comparator; }
        };
    }
}
