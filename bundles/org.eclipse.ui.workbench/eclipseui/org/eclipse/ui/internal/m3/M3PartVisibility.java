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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * The hidden state of a view's viewport, owned by SWT (brief rule 24, "Hidden
 * viewports"; rule 25, "own the implementation"): the viewport is hidden while
 * its control is not {@link Control#isPartVisible() part-visible}, that is while
 * the control or an ancestor is hidden (behind another tab, in a minimized
 * stack), sized to zero (a collapsed sash) or marked part-hidden, or its shell
 * is minimized. SWT reports the transitions with {@link SWT#PartHidden} and
 * {@link SWT#PartVisible}; this class keeps no second copy of that state, only
 * the view's dirty flag and the patch count.
 * <p>
 * While the viewport is hidden, the view keeps applying model deltas to its
 * lanes but does no tree work: it calls {@link #markDirty()} instead (O(1), the
 * view keeps its own coalesced dirty window). When the control becomes
 * part-visible again, the {@code onShown} callback runs once, inside the
 * PartVisible event and before the first paint, so the view applies ONE window
 * patch against the current model and the tree paints once. The one helper
 * every view uses (docs/m3/markers-view/REFERENCE_TREE_VIEW.md).
 * </p>
 * Must be created and used on the UI thread.
 */
public final class M3PartVisibility implements Listener { //FLLDCIM: MARK — T20  IMPLEMENTS

	private final Control control;
	private final Runnable onShown;
	private boolean dirty;
	private int patches;

	private M3PartVisibility(Control control, Runnable onShown) {
		this.control = control;
		this.onShown = onShown;
	}

	/**
	 * Starts watching a viewport; the watch ends when its control is disposed.
	 *
	 * @param control the view's viewport control (its tree or table)
	 * @param onShown applies the view's one window patch; runs only when a
	 *                change arrived while the control was hidden
	 * @return the watch
	 */
	public static M3PartVisibility watch(Control control, Runnable onShown) {
		M3PartVisibility watch = new M3PartVisibility(control, onShown);
		control.addListener(SWT.PartVisible, watch);
		return watch;
	}

	/**
	 * @return whether the viewport is hidden: deltas must not touch its tree. A
	 *         pending patch whose PartVisible event was not seen runs first, so
	 *         the caller's own patch lands on a current tree.
	 */
	public boolean isHidden() {
		if (control.isDisposed() || !control.isPartVisible()) {
			return true;
		}
		shown();
		return false;
	}

	/** A delta changed the model while the viewport was hidden: patch once when it shows. */
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
		if (!control.isDisposed()) {
			control.removeListener(SWT.PartVisible, this);
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (event.type == SWT.PartVisible) {
			shown();
		}
	}

	/* the one window patch of a hidden period with changes */
	private void shown() {
		if (dirty) {
			dirty = false;
			patches++;
			onShown.run();
		}
	}
}
