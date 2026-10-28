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
package org.eclipse.ui.internal.m3;

import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;

/**
 * The hidden state of a view's viewport, driven by the workbench (brief rule
 * 24, "Hidden viewports"): {@link IPartListener2#partHidden} hides it (behind
 * another tab, in a minimized or inactive stack), {@link IPartListener2#partVisible}
 * shows it.
 * <p>
 * While the part is hidden, the view keeps applying model deltas to its lanes
 * but does no tree work: it calls {@link #markDirty()} instead (O(1), the view
 * keeps its own coalesced dirty window). When the part becomes visible again,
 * the {@code onShown} callback runs once, inside the visibility event and
 * before the first paint, so the view applies ONE window patch against the
 * current model and the tree paints once. The one helper every view uses
 * (docs/m3/markers-view/REFERENCE_TREE_VIEW.md).
 * </p>
 * Must be created and used on the UI thread.
 */
public final class M3PartVisibility implements IPartListener2 { //FLLDCIM: MARK — T20  IMPLEMENTS

	private final IWorkbenchPart part;
	private final IWorkbenchPage page;
	private final Runnable onShown;
	private boolean hidden;
	private boolean dirty;
	private int patches;

	private M3PartVisibility(IWorkbenchPart part, Runnable onShown) {
		this.part = part;
		this.page = part.getSite().getPage();
		this.onShown = onShown;
		this.hidden = !page.isPartVisible(part);
	}

	/**
	 * Starts watching a part; the watch ends when the part closes.
	 *
	 * @param part    the view
	 * @param onShown applies the view's one window patch; runs only when a
	 *                change arrived while the part was hidden
	 * @return the watch
	 */
	public static M3PartVisibility watch(IWorkbenchPart part, Runnable onShown) {
		M3PartVisibility watch = new M3PartVisibility(part, onShown);
		watch.page.addPartListener(watch);
		return watch;
	}

	/** @return whether the part is hidden: deltas must not touch its tree */
	public boolean isHidden() {
		return hidden;
	}

	/** A delta changed the model while the part was hidden: patch once when it shows. */
	public void markDirty() {
		dirty = true;
	}

	/** @return whether a patch is pending for the next show */
	public boolean isDirty() {
		return dirty;
	}

	/** @return how many window patches ran on show (tests: one per hidden period with changes) */
	public int patches() {
		return patches;
	}

	/** Stops watching. */
	public void dispose() {
		page.removePartListener(this);
	}

	@Override
	public void partHidden(IWorkbenchPartReference ref) {
		if (ref.getPart(false) == part) {
			hidden = true;
		}
	}

	@Override
	public void partVisible(IWorkbenchPartReference ref) {
		if (ref.getPart(false) != part) {
			return;
		}
		hidden = false;
		if (dirty) {
			dirty = false;
			patches++;
			onShown.run();
		}
	}

	@Override
	public void partClosed(IWorkbenchPartReference ref) {
		if (ref.getPart(false) == part) {
			dispose();
		}
	}
}
