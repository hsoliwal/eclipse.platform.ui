/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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

package org.eclipse.ui.internal.activities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionConverter;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionDelta;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.registry.IWorkbenchRegistryConstants;
import org.eclipse.ui.internal.util.ConfigurationElementMemento;
import org.eclipse.ui.statushandlers.StatusManager;

final class ExtensionActivityRegistry extends AbstractActivityRegistry {

	/**
	 * Prefix for all activity preferences
	 */
	private static final String PREFIX = "UIActivities."; //$NON-NLS-1$

	private List<ActivityRequirementBindingDefinition> extensionActivityRequirementBindingDefinitions;

	private List<ActivityDefinition> extensionActivityDefinitions;

	private List<ActivityPatternBindingDefinition> extensionActivityPatternBindingDefinitions;

	private List<CategoryActivityBindingDefinition> extensionCategoryActivityBindingDefinitions;

	private List<CategoryDefinition> extensionCategoryDefinitions;

	private List<String> extensionDefaultEnabledActivities;

	private final IExtensionRegistry extensionRegistry;

	ExtensionActivityRegistry(IExtensionRegistry extensionRegistry) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		if (extensionRegistry == null) {
			throw new NullPointerException();
		}

		this.extensionRegistry = extensionRegistry;

		this.extensionRegistry.addRegistryChangeListener(registryChangeEvent -> {
			IExtensionDelta[] extensionDeltas = registryChangeEvent.getExtensionDeltas(Persistence.PACKAGE_PREFIX,
					Persistence.PACKAGE_BASE);

			if (extensionDeltas.length != 0) {
				load();
			}
		});

		load();
	}

	private String getNamespace(IConfigurationElement configurationElement) {
		String namespace = null;

		if (configurationElement != null) {
			IExtension extension = configurationElement.getDeclaringExtension();

			if (extension != null) {
				namespace = extension.getContributor().getName();
			}
		}

		return namespace;
	}

	/**
	 * Returns the activity definition found at this id.
	 *
	 * @param id <code>ActivityDefinition</code> id.
	 * @return <code>ActivityDefinition</code> with given id or <code>null</code> if
	 *         not found.
	 */
	private ActivityDefinition getActivityDefinitionById(String id) {
		int size = extensionActivityDefinitions.size();
		for (int i = 0; i < size; i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			ActivityDefinition activityDef = extensionActivityDefinitions.get(i);
			if (activityDef.getId().equals(id)) {
				return activityDef;
			}
		}
		return null;
	}

	private void load() { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		if (extensionActivityRequirementBindingDefinitions == null) {
			extensionActivityRequirementBindingDefinitions = new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		} else {
			extensionActivityRequirementBindingDefinitions.clear();
		}

		if (extensionActivityDefinitions == null) {
			extensionActivityDefinitions = new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		} else {
			extensionActivityDefinitions.clear();
		}

		if (extensionActivityPatternBindingDefinitions == null) {
			extensionActivityPatternBindingDefinitions = new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		} else {
			extensionActivityPatternBindingDefinitions.clear();
		}

		if (extensionCategoryActivityBindingDefinitions == null) {
			extensionCategoryActivityBindingDefinitions = new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		} else {
			extensionCategoryActivityBindingDefinitions.clear();
		}

		if (extensionCategoryDefinitions == null) {
			extensionCategoryDefinitions = new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		} else {
			extensionCategoryDefinitions.clear();
		}

		if (extensionDefaultEnabledActivities == null) {
			extensionDefaultEnabledActivities = new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		} else {
			extensionDefaultEnabledActivities.clear();
		}

		IConfigurationElement[] configurationElements = extensionRegistry
				.getConfigurationElementsFor(Persistence.PACKAGE_FULL);

		for (IConfigurationElement configurationElement : configurationElements) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			String name = configurationElement.getName();

			switch (name) {
			case Persistence.TAG_ACTIVITY_REQUIREMENT_BINDING:
				readActivityRequirementBindingDefinition(configurationElement);
				break;
			case Persistence.TAG_ACTIVITY:
				readActivityDefinition(configurationElement);
				break;
			case Persistence.TAG_ACTIVITY_PATTERN_BINDING:
				readActivityPatternBindingDefinition(configurationElement);
				break;
			case Persistence.TAG_CATEGORY_ACTIVITY_BINDING:
				readCategoryActivityBindingDefinition(configurationElement);
				break;
			case Persistence.TAG_CATEGORY:
				readCategoryDefinition(configurationElement);
				break;
			case Persistence.TAG_DEFAULT_ENABLEMENT:
				readDefaultEnablement(configurationElement);
				break;
			default:
				break;
			}
		}

		// merge enablement overrides from plugin_customization.ini
		IPreferenceStore store = WorkbenchPlugin.getDefault().getPreferenceStore();
		for (ActivityDefinition activityDef : extensionActivityDefinitions) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			String id = activityDef.getId();
			String preferenceKey = createPreferenceKey(id);
			if ("".equals(store.getDefaultString(preferenceKey))) { //$NON-NLS-1$
				continue;
			}
			if (store.getDefaultBoolean(preferenceKey)) {
				if (!extensionDefaultEnabledActivities.contains(id) && activityDef.getEnabledWhen() == null) {
					extensionDefaultEnabledActivities.add(id);
				}
			} else {
				extensionDefaultEnabledActivities.remove(id);
			}
		}

		// Removal of all defaultEnabledActivites which target to expression
		// controlled activities.
		for (int i = 0; i < extensionDefaultEnabledActivities.size();) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			String id = extensionDefaultEnabledActivities.get(i);
			ActivityDefinition activityDef = getActivityDefinitionById(id);
			if (activityDef != null && activityDef.getEnabledWhen() != null) {
				extensionDefaultEnabledActivities.remove(i);
				StatusManager.getManager().handle(new Status(IStatus.WARNING, PlatformUI.PLUGIN_ID,
						"Default enabled activity declarations will be ignored (id: " + id + ")")); //$NON-NLS-1$ //$NON-NLS-2$ //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			} else {
				i++;
			}
		}

		// remove all requirement bindings that reference expression-bound activities
		for (Iterator<ActivityRequirementBindingDefinition> i = extensionActivityRequirementBindingDefinitions.iterator(); i //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				.hasNext();) {
			ActivityRequirementBindingDefinition bindingDef = i.next();
			ActivityDefinition activityDef = getActivityDefinitionById(bindingDef.getRequiredActivityId());
			if (activityDef != null && activityDef.getEnabledWhen() != null) {
				i.remove();
				StatusManager.getManager().handle(new Status(IStatus.WARNING, PlatformUI.PLUGIN_ID,
						"Expression activity cannot have requirements (id: " + activityDef.getId() + ")")); //$NON-NLS-1$ //$NON-NLS-2$ //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
				continue;
			}

			activityDef = getActivityDefinitionById(bindingDef.getActivityId());
			if (activityDef != null && activityDef.getEnabledWhen() != null) {
				i.remove();
				StatusManager.getManager().handle(new Status(IStatus.WARNING, PlatformUI.PLUGIN_ID,
						"Expression activity cannot be required (id: " + activityDef.getId() + ")")); //$NON-NLS-1$ //$NON-NLS-2$ //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			}
		}

		boolean activityRegistryChanged = false;

		if (!extensionActivityRequirementBindingDefinitions.equals(super.activityRequirementBindingDefinitions)) {
			activityRequirementBindingDefinitions = Collections
					.unmodifiableList(new ArrayList<>(extensionActivityRequirementBindingDefinitions)); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
			activityRegistryChanged = true;
		}

		if (!extensionActivityDefinitions.equals(super.activityDefinitions)) {
			activityDefinitions = Collections.unmodifiableList(new ArrayList<>(extensionActivityDefinitions)); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
			activityRegistryChanged = true;
		}

		if (!extensionActivityPatternBindingDefinitions.equals(super.activityPatternBindingDefinitions)) {
			activityPatternBindingDefinitions = Collections
					.unmodifiableList(new ArrayList<>(extensionActivityPatternBindingDefinitions)); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
			activityRegistryChanged = true;
		}

		if (!extensionCategoryActivityBindingDefinitions.equals(super.categoryActivityBindingDefinitions)) {
			categoryActivityBindingDefinitions = Collections
					.unmodifiableList(new ArrayList<>(extensionCategoryActivityBindingDefinitions)); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
			activityRegistryChanged = true;
		}

		if (!extensionCategoryDefinitions.equals(super.categoryDefinitions)) {
			categoryDefinitions = Collections.unmodifiableList(new ArrayList<>(extensionCategoryDefinitions)); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
			activityRegistryChanged = true;
		}

		if (!extensionDefaultEnabledActivities.equals(super.defaultEnabledActivities)) {
			defaultEnabledActivities = Collections.unmodifiableList(new ArrayList<>(extensionDefaultEnabledActivities)); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
			activityRegistryChanged = true;
		}

		if (activityRegistryChanged) {
			fireActivityRegistryChanged();
		}
	}

	/**
	 * Create the preference key for the activity.
	 *
	 * @param activityId the activity id.
	 * @return String a preference key representing the activity.
	 */
	private String createPreferenceKey(String activityId) {
		return PREFIX + activityId; //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
	}

	private void readDefaultEnablement(IConfigurationElement configurationElement) {
		String enabledActivity = Persistence
				.readDefaultEnablement(new ConfigurationElementMemento(configurationElement));

		if (enabledActivity != null) {
			extensionDefaultEnabledActivities.add(enabledActivity);
		}

	}

	private void readActivityRequirementBindingDefinition(IConfigurationElement configurationElement) {
		ActivityRequirementBindingDefinition activityRequirementBindingDefinition = Persistence
				.readActivityRequirementBindingDefinition(new ConfigurationElementMemento(configurationElement),
						getNamespace(configurationElement));

		if (activityRequirementBindingDefinition != null) {
			extensionActivityRequirementBindingDefinitions.add(activityRequirementBindingDefinition);
		}
	}

	private void readActivityDefinition(IConfigurationElement configurationElement) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		ActivityDefinition activityDefinition = Persistence.readActivityDefinition(
				new ConfigurationElementMemento(configurationElement), getNamespace(configurationElement));

		if (activityDefinition != null) {
			// this is not ideal, but core expressions takes an
			// IConfigurationElement or a w3c dom Document
			IConfigurationElement[] enabledWhen = configurationElement
					.getChildren(IWorkbenchRegistryConstants.TAG_ENABLED_WHEN);
			if (enabledWhen.length == 1) {
				IConfigurationElement[] expElement = enabledWhen[0].getChildren();
				if (expElement.length == 1) {
					try {
						Expression expression = ExpressionConverter.getDefault().perform(expElement[0]);
						activityDefinition.setEnabledWhen(expression);
					} catch (CoreException e) {
						StatusManager.getManager().handle(e, WorkbenchPlugin.PI_WORKBENCH);
					}
				}
			}
			extensionActivityDefinitions.add(activityDefinition);
		}
	}

	private void readActivityPatternBindingDefinition(IConfigurationElement configurationElement) {
		ActivityPatternBindingDefinition activityPatternBindingDefinition = Persistence
				.readActivityPatternBindingDefinition(new ConfigurationElementMemento(configurationElement),
						getNamespace(configurationElement));

		if (activityPatternBindingDefinition != null) {
			extensionActivityPatternBindingDefinitions.add(activityPatternBindingDefinition);
		}
	}

	private void readCategoryActivityBindingDefinition(IConfigurationElement configurationElement) {
		CategoryActivityBindingDefinition categoryActivityBindingDefinition = Persistence
				.readCategoryActivityBindingDefinition(new ConfigurationElementMemento(configurationElement),
						getNamespace(configurationElement));

		if (categoryActivityBindingDefinition != null) {
			extensionCategoryActivityBindingDefinitions.add(categoryActivityBindingDefinition);
		}
	}

	private void readCategoryDefinition(IConfigurationElement configurationElement) {
		CategoryDefinition categoryDefinition = Persistence.readCategoryDefinition(
				new ConfigurationElementMemento(configurationElement), getNamespace(configurationElement));

		if (categoryDefinition != null) {
			extensionCategoryDefinitions.add(categoryDefinition);
		}
	}
}
