/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
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
 *******************************************************************************/

package org.eclipse.ui.internal.views.markers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.views.markers.MarkerFieldFilter;
import org.eclipse.ui.views.markers.MarkerItem;
import org.eclipse.ui.views.markers.internal.MarkerFilter;
import org.eclipse.ui.views.markers.internal.MarkerType;
import org.eclipse.ui.views.markers.internal.MarkerTypesModel;
import org.eclipse.ui.views.markers.internal.ProblemFilter;

/**
 * MarkerTypeFieldFilter is the field filter for filtering on types.
 *
 * @since 3.4
 */
public class MarkerTypeFieldFilter extends CompatibilityFieldFilter {

	private static final String TAG_TYPES_DELIMITER = ":"; //$NON-NLS-1$
	private static final String TAG_SELECTED_TYPES = "selectedTypes"; //$NON-NLS-1$
	Collection<MarkerType> selectedTypes = new HashSet<>(); //FLLDCIM: MARK — M03 M3_SET_LANES NEW
	private MarkerContentGenerator contentGenerator;


	@Override
	public boolean select(MarkerItem item) {
		IMarker marker = item.getMarker();
		// OK if all are selected
		if (marker == null) {
			return contentGenerator.allTypesSelected(selectedTypes);
		}
		try {
			return selectedTypes.contains(MarkerTypesModel.getInstance().getType(marker.getType()));
		} catch (CoreException e) {
			return false;
		}
	}

	/**
	 * Set the selected types in the receiver based on the values in
	 * contentGenerator.
	 *
	 * @param markerTypes
	 *            Collection of MarkerType
	 * @param generator
	 *            {@link MarkerContentGenerator}
	 */
	void setSelectedTypes(Collection<MarkerType> markerTypes, MarkerContentGenerator generator) {
		setContentGenerator(generator);
		selectedTypes = markerTypes;
	}

	/**
	 * Return the selectedTypes.
	 *
	 * @return Collection of MarkerType
	 */
	Collection<MarkerType> getSelectedTypes() {
		return selectedTypes;
	}

	@Override
	public void saveSettings(IMemento memento) {
		if (selectedTypes.containsAll(contentGenerator.getMarkerTypes())) {
			return;
		}

		Iterator<MarkerType> selected = selectedTypes.iterator();
		StringBuilder settings = new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		while (selected.hasNext()) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			MarkerType markerType = selected.next();
			settings.append(markerType.getId());
			settings.append(TAG_TYPES_DELIMITER);
		}
		memento.putString(TAG_SELECTED_TYPES, settings.toString());
	}

	@Override //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
	public void loadSettings(IMemento memento) {
		String types = memento.getString(TAG_SELECTED_TYPES);
		if (types == null) {
			return;
		}

		selectedTypes.clear();
		int start = 0;
		int nextSpace = types.indexOf(TAG_TYPES_DELIMITER, 0);
		while (nextSpace > 0) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			String typeId = types.substring(start, nextSpace);
			start = nextSpace + 1;
			nextSpace = types.indexOf(TAG_TYPES_DELIMITER, start);

			MarkerType type = contentGenerator.getType(typeId);
			if (type != null) {
				selectedTypes.add(type);
			}
		}
	}

	@Override //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
	void loadLegacySettings(IMemento memento, MarkerContentGenerator generator) {
		setContentGenerator(generator);
		// new selection list attribute
		// format is "id:(true|false):"
		String setting = memento.getString(MarkerFilter.TAG_SELECTION_STATUS);

		if (setting != null) {
			selectedTypes.clear();

			// get the complete list of types
			List<MarkerType> newTypes = new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
			StringTokenizer stringTokenizer = new StringTokenizer(setting);
			while (stringTokenizer.hasMoreTokens()) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				String id = stringTokenizer.nextToken(TAG_TYPES_DELIMITER);
				String status = null;
				if (stringTokenizer.hasMoreTokens()) {
					status = stringTokenizer.nextToken(TAG_TYPES_DELIMITER);
				}

				MarkerType type = contentGenerator.getType(id);
				if (type != null) {
					newTypes.remove(type);

					// add the type to the selected list
					if (!MarkerFilter.SELECTED_FALSE.equals(status) && !selectedTypes.contains(type)) {
						selectedTypes.add(type);
					}
				}
			}
		}
	}

	@Override
	public void initialize(ProblemFilter problemFilter) {
		selectedTypes.clear();
		selectedTypes.addAll(problemFilter.getSelectedTypes());
	}

	/**
	 * Set the content generator that is being configured.
	 */
	void setContentGenerator(MarkerContentGenerator generator) {
		contentGenerator = generator;
		// Set the initial selection to be everything
		selectedTypes = new HashSet<>(); //FLLDCIM: MARK — M03 M3_SET_LANES NEW
		selectedTypes.addAll(generator.getMarkerTypes());
	}

	@Override
	public void populateWorkingCopy(MarkerFieldFilter copy) {
		super.populateWorkingCopy(copy);
		((MarkerTypeFieldFilter) copy).selectedTypes = new HashSet<>(selectedTypes); //FLLDCIM: MARK — M03 M3_SET_LANES NEW
		((MarkerTypeFieldFilter) copy).contentGenerator = contentGenerator;
	}
}
