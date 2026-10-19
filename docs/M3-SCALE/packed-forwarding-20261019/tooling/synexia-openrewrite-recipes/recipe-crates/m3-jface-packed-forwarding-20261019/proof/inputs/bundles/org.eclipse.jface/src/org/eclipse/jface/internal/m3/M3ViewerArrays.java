/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
 *     IBM Corporation - initial API and implementation
 *     Jeanderson Candido <http://jeandersonbc.github.io> - Bug 434153
 *     Lars Vogel - Bugs 430873 and 487940
 *     Andrey Loskutov - Bug 364735
 *     Mikael Barbero (Eclipse Foundation) - Bug 254570
 *     Hitesh Soliwal - M3 array atom extraction, 2026
 *******************************************************************************/
package org.eclipse.jface.internal.m3;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 * Contract-specific M3 array atoms used by the existing JFace facades.
 * No state registry or per-element wrappers: the caller's arrays remain authoritative.
 * This package is internal and is not exported by the JFace bundle.
 */
public final class M3ViewerArrays {
	private M3ViewerArrays() {
	}

	public static Object[] contentElements(Object inputElement) {
		if (inputElement instanceof Object[]) {
			return (Object[]) inputElement;
		}
		if (inputElement instanceof Collection<?>) {
			return ((Collection<?>) inputElement).toArray();
		}
		return new Object[0];
	}

	public static Object[] filter(ViewerFilter filter, Viewer viewer, Object parent, Object[] elements) {
		int size = elements.length;
		if (size == 0) {
			return elements;
		}
		Object[] out = new Object[size];
		int count = 0;
		for (int i = 0; i < size; ++i) {
			Object element = elements[i];
			if (filter.select(viewer, parent, element)) {
				out[count++] = element;
			}
		}
		return count == size ? out : Arrays.copyOf(out, count);
	}

	public static void sort(ViewerComparator comparator, Viewer viewer, Object[] elements) {
		Arrays.sort(elements, (a, b) -> comparator.compare(viewer, a, b));
	}

	public static Object[] validatedPath(Object[] segments) {
		Assert.isNotNull(segments);
		for (Object segment : segments) {
			Assert.isNotNull(segment);
		}
		return segments;
	}

	public static int pathHash(Object[] segments, IElementComparer comparer) {
		int result = 0;
		for (Object segment : segments) {
			if (comparer == null) {
				result += segment.hashCode();
			} else {
				result += comparer.hashCode(segment);
			}
		}
		return result;
	}

	public static boolean pathEquals(Object[] segments, Object[] otherPath, IElementComparer comparer) {
		if (otherPath == null) {
			return false;
		}
		if (segments.length != otherPath.length) {
			return false;
		}
		for (int i = 0; i < segments.length; i++) {
			if (comparer == null) {
				if (!segments[i].equals(otherPath[i])) {
					return false;
				}
			} else if (!comparer.equals(segments[i], otherPath[i])) {
				return false;
			}
		}
		return true;
	}

	public static boolean pathStartsWith(Object[] segments, Object[] treePath, IElementComparer comparer) {
		int thisSegmentCount = segments.length;
		int otherSegmentCount = treePath.length;
		if (otherSegmentCount == thisSegmentCount) {
			return pathEquals(segments, treePath, comparer);
		}
		if (otherSegmentCount > thisSegmentCount) {
			return false;
		}
		for (int i = 0; i < otherSegmentCount; i++) {
			Object otherSegment = treePath[i];
			if (comparer == null) {
				if (!otherSegment.equals(segments[i])) {
					return false;
				}
			} else if (!comparer.equals(otherSegment, segments[i])) {
				return false;
			}
		}
		return true;
	}

	public static TreePath parentPath(Object[] segments) {
		int segmentCount = segments.length;
		if (segmentCount < 1) {
			return null;
		} else if (segmentCount == 1) {
			return TreePath.EMPTY;
		}
		Object[] parentSegments = new Object[segmentCount - 1];
		System.arraycopy(segments, 0, parentSegments, 0, segmentCount - 1);
		return new TreePath(parentSegments);
	}

	public static TreePath childPath(Object[] segments, Object newSegment) {
		int segmentCount = segments.length;
		Object[] childSegments = new Object[segmentCount + 1];
		if(segmentCount>0) {
			System.arraycopy(segments, 0, childSegments, 0, segmentCount);
		}
		childSegments[segmentCount] = newSegment;
		return new TreePath(childSegments);
	}

	public static boolean selectionEquals(Object[] elements, Object[] other, IElementComparer comparer) {
		//size
		int myLen = elements.length;
		if (myLen != other.length) {
			return false;
		}

		//element comparison
		if (comparer != null) {
			for (int i = 0; i < myLen; i++) {
				if (!comparer.equals(elements[i], other[i])) {
					return false;
				}
			}
		} else {
			return Arrays.equals(elements, other);
		}

		return true;
	}

	public static int selectionHash(Object[] elements, IElementComparer comparer) {
		int r;
		if (comparer != null) {
			r = 31 + comparer.hashCode();
			for (Object e : elements) {
				r = 31 * r + (e == null ? 0 : comparer.hashCode(e));
			}
		} else {
			r = Arrays.hashCode(elements);
		}

		return r;
	}

	public static Object[] selectionArray(Object[] elements) {
		return elements == null ? new Object[0] : elements.clone();
	}

	public static List<Object> selectionList(Object[] elements) {
		return Arrays.asList(elements == null ? new Object[0] : elements);
	}
}
