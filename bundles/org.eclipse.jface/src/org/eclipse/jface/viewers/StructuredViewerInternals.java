/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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
 ******************************************************************************/

package org.eclipse.jface.viewers;

import org.eclipse.pde.api.tools.annotations.NoExtend;
import org.eclipse.pde.api.tools.annotations.NoImplement;
import org.eclipse.pde.api.tools.annotations.NoInstantiate;
import org.eclipse.pde.api.tools.annotations.NoReference;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Widget;

/**
 * This class is not part of the public API of JFace. See bug 267722.
 *
 * @since 3.5
 */
@NoInstantiate
@NoExtend
public class StructuredViewerInternals {

	/**
	 * Nothing to see here.
	 *
	 * @since 3.5
	 */
	@NoExtend
	@NoImplement
	protected static interface AssociateListener {

		/**
		 * Call when an element is associated with an Item
		 *
		 * @param element the element
		 * @param item    the item
		 */
		void associate(Object element, Item item);

		/**
		 * Called when an Item is no longer associated
		 *
		 * @param item the item
		 */
		void disassociate(Item item);

		/**
		 * Called when an element has been filtered out.
		 *
		 * @since 3.6
		 * @param element the filtered element
		 */
		void filteredOut(Object element);
	}

	/**
	 * Nothing to see here. Sets or resets the AssociateListener for the given
	 * Viewer.
	 *
	 * @param viewer
	 *            the viewer
	 * @param listener
	 *            the {@link AssociateListener}
	 */
	@NoReference
	protected static void setAssociateListener(StructuredViewer viewer,
			AssociateListener listener) {
		viewer.setAssociateListener(listener);
	}

	/**
	 * Nothing to see here. Returns the items for the given element.
	 *
	 * @param viewer
	 *            the viewer
	 * @param element
	 *            the element
	 * @return the Widgets corresponding to the element
	 */
	@NoReference
	protected static Widget[] getItems(StructuredViewer viewer, Object element) {
		return viewer.findItems(element);
	}

	/**
	 * Internal access for the explicit M3 delta selection boundary. The viewer
	 * remains the owner of selection and subclass check-state preservation.
	 *
	 * @param viewer the viewer
	 * @param updateCode the synchronous update
	 * @since 3.43
	 */
	@NoReference
	public static void preservingSelection(StructuredViewer viewer, Runnable updateCode) {
		viewer.m3PreservingSelection(updateCode);
	}

	/**
	 * Internal queue access without reading widgets from producer threads.
	 *
	 * @param viewer the viewer
	 * @return the display published by the viewer's control hook, or null
	 * @since 3.43
	 */
	@NoReference
	public static Display getM3Display(StructuredViewer viewer) {
		return viewer.m3Display();
	}

	/**
	 * Returns the viewer-owned queue's internal disposal callback.
	 *
	 * @param viewer the viewer
	 * @return the existing queue, or null before its first post or after disposal
	 * @since 3.43
	 */
	@NoReference
	public static Runnable getM3DeltaQueue(StructuredViewer viewer) {
		return viewer.m3DeltaQueue();
	}

	/**
	 * Installs a first queue without resurrecting a disposed viewer.
	 *
	 * @param viewer the viewer
	 * @param queue the proposed queue and its disposal callback
	 * @return the installed queue, or null if the viewer has been disposed
	 * @since 3.43
	 */
	@NoReference
	public static Runnable installM3DeltaQueue(StructuredViewer viewer, Runnable queue) {
		return viewer.m3InstallDeltaQueue(queue);
	}

	/**
	 * Internal access for the M3 viewport deltas: inserts {@code element} under
	 * {@code parent} at the position the model computed. In {@code viewport}
	 * mode and without a viewer comparator the filter check of
	 * {@link AbstractTreeViewer#insert(Object, Object, int)} is skipped (the
	 * model applied the filters; the stock check appends every row under a
	 * filter); otherwise the stock insert runs.
	 *
	 * @param viewer   the tree viewer
	 * @param parent   the parent element, or the tree path to the parent
	 * @param element  the element
	 * @param position the 0-based position among the parent's children, or -1
	 * @since 3.43
	 */
	@NoReference
	public static void insertAt(AbstractTreeViewer viewer, Object parent, Object element, int position) {
		if (M3ViewerPaths.on(viewer) && viewer.getComparator() == null) {
			viewer.insertAt(parent, element, position);
		} else {
			viewer.insert(parent, element, position);
		}
	}

}
