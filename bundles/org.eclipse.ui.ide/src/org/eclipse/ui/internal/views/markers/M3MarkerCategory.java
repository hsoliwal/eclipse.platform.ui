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
package org.eclipse.ui.internal.views.markers;

/**
 * The facade of one category of the lane model ({@link M3MarkerLanes}): it
 * answers counts and the highest severity from lane counters and materializes
 * entries only for the window the viewer shows ({@link #window(int)}) or when
 * an API asks for every child ({@link #getChildren()}). It lives as long as its
 * category: marker deltas patch it in place instead of creating a new one.
 */
final class M3MarkerCategory extends MarkerCategory {

	private final Markers owner;
	private final M3MarkerLanes lanes;
	/** The category in the lanes; set by the lanes right after construction. */
	M3MarkerLanes.Category lane;

	M3MarkerCategory(Markers owner, M3MarkerLanes lanes, String name) {
		super(owner, 0, -1, name);
		this.owner = owner;
		this.lanes = lanes;
	}

	/** Every child: an API asks for all of them (selection of the category, copy, the search box). */
	@Override
	MarkerSupportItem[] getChildren() {
		return lanes.children(lane);
	}

	@Override
	int getChildrenCount() {
		return lane.order.size();
	}

	/**
	 * The rows the viewer shows.
	 *
	 * @param limit the marker limit, or a negative value for none
	 * @return the entries of the window, in order
	 */
	MarkerEntry[] window(int limit) {
		return lanes.window(lane, limit);
	}

	@Override
	int getHighestSeverity() {
		return lanes.highestSeverity(lane);
	}

	@Override
	int getShownChildrenCount() {
		ExtendedMarkersView view = owner.getBuilder().getView();
		// the search text is a lane (M3MarkerLanes.applyText): what stock counts over every child
		return view.applyMarkerLimit(lanes.matching(lane));
	}

	@Override
	void clearCache() {
		// Counts come directly from the lanes; this facade holds no child cache.
	}
}
