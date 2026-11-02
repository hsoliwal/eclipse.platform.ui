/*******************************************************************************
 * Copyright (c) 2003, 2017 IBM Corporation and others.
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
package org.eclipse.ui.internal.ide;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IPluginContribution;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.activities.ITriggerPoint;
import org.eclipse.ui.activities.IWorkbenchActivitySupport;
import org.eclipse.ui.activities.WorkbenchActivityHelper;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * Utility class that manages promotion of activities in response to workspace changes.
 *
 * @since 3.0
 */
public class IDEWorkbenchActivityHelper {

	private static final String NATURE_POINT = "org.eclipse.ui.ide.natures"; //$NON-NLS-1$

	/**
	 * Resource listener that reacts to new projects (and associated natures)
	 * coming into the workspace.
	 */
	private final IResourceChangeListener listener;

	/**
	 * Mapping from composite nature ID to IPluginContribution.  Used for proper
	 * activity mapping of natures.
	 */
	private final Map<String, IPluginContribution> natureMap;

	/**
	 * Lock for the list of nature ids to be processed.
	 */
	private final IDEWorkbenchActivityHelper lock;

	/**
	 * The update job.
	 */
	private WorkbenchJob fUpdateJob;

	/**
	 * The collection of natures to process.
	 */
	private final HashSet<String> fPendingNatureUpdates = new HashSet<>(); //FLLDCIM: MARK — M03 M3_SET_LANES NEW

	/**
	 * Singleton instance.
	 */
	private static IDEWorkbenchActivityHelper singleton;

	/**
	 * Get the singleton instance of this class.
	 * @return the singleton instance of this class.
	 * @since 3.0
	 */
	public static IDEWorkbenchActivityHelper getInstance() {
		if (singleton == null) {
			singleton = new IDEWorkbenchActivityHelper();
		}
		return singleton;
	}

	/**
	 * Create a new <code>IDEWorkbenchActivityHelper</code> which will listen
	 * for workspace changes and promote activities accordingly.
	 */
	private IDEWorkbenchActivityHelper() { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		lock = this;
		natureMap = new HashMap<>(); //FLLDCIM: MARK — M01 M3_HASH_LANES NEW
		// for dynamic UI
		Platform.getExtensionRegistry().addRegistryChangeListener(
				event -> {
					if (event.getExtensionDeltas(
							"org.eclipse.core.resources", "natures").length > 0) { //$NON-NLS-1$ //$NON-NLS-2$
						loadNatures();
					}
				}, "org.eclipse.core.resources"); //$NON-NLS-1$
		loadNatures();
		listener = getChangeListener();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(listener);
		// crawl the initial projects to set up nature bindings
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		processProjects(new HashSet<>(Arrays.asList(projects))); //FLLDCIM: MARK — M03 M3_SET_LANES NEW
	}

	/**
	 * For dynamic UI.  Clears the cache of known natures and recreates it.
	 */
	public void loadNatures() { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		natureMap.clear();
		IExtensionPoint point = Platform.getExtensionRegistry()
				.getExtensionPoint("org.eclipse.core.resources.natures"); //$NON-NLS-1$
		for (IExtension extension : point.getExtensions()) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			final String localId = extension.getSimpleIdentifier();
			final String pluginId = extension.getContributor().getName();
			String natureId = extension.getUniqueIdentifier();
			natureMap.put(natureId, new IPluginContribution() {
				@Override
				public String getLocalId() {
					return localId;
				}

				@Override
				public String getPluginId() {
					return pluginId;
				}
			});
		}
	}

	/**
	 * Get a change listener for listening to resource changes.
	 *
	 * @return the resource change listeners
	 */
	private IResourceChangeListener getChangeListener() { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		return event -> { //FLLDCIM: MARK — T07  LAMBDA
			if (!WorkbenchActivityHelper.isFiltering()) {
				return;
			}
			IResourceDelta mainDelta = event.getDelta();

			if (mainDelta == null) {
				return;
			}
			//Has the root changed?
			if (mainDelta.getKind() == IResourceDelta.CHANGED
					&& mainDelta.getResource().getType() == IResource.ROOT) {

				Set<IProject> projectsToUpdate = new HashSet<>(); //FLLDCIM: MARK — M03 M3_SET_LANES NEW
				for (IResourceDelta delta : mainDelta.getAffectedChildren()) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
					if (delta.getResource().getType() == IResource.PROJECT) {
						IProject project = (IProject) delta.getResource();

						if (project.isOpen()) {
							projectsToUpdate.add(project);
						}
					}
				}

				processProjects(projectsToUpdate);
			}
		};
	}


	/**
	 * Drain the queue and consult the helper.
	 */
	protected void runPendingUpdates() { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		String[] ids = null;
		synchronized (lock) {
			ids = fPendingNatureUpdates
					.toArray(new String[fPendingNatureUpdates.size()]);
			fPendingNatureUpdates.clear();
		}
		IWorkbenchActivitySupport workbenchActivitySupport = PlatformUI
				.getWorkbench().getActivitySupport();
		for (String id : ids) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			final IPluginContribution contribution = natureMap.get(id);
			if (contribution == null) {
				continue; // bad nature ID.
			}

			final ITriggerPoint triggerPoint = workbenchActivitySupport
					.getTriggerPointManager().getTriggerPoint(NATURE_POINT);
			// consult the advisor - if the activities need enabling, they will
			// be
			WorkbenchActivityHelper.allowUseOf(triggerPoint, contribution);
		}

	}

	/**
	 * Unhooks the <code>IResourceChangeListener</code>.
	 */
	public void shutdown() {
		if (listener != null) {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(
					listener);
		}
	}

	private void processProjects(Set<IProject> projectsToUpdate) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		boolean needsUpdate = false;
		for (IProject project : projectsToUpdate) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			try {
				String[] ids = project.getDescription().getNatureIds();
				if (ids.length == 0) {
					continue;
				}

				synchronized (lock) {
					needsUpdate = fPendingNatureUpdates.addAll(Arrays.asList(ids)) || needsUpdate;
				}

			} catch (CoreException e) {
				// Do nothing if there is a CoreException
			}
		}
		if (needsUpdate) {
			if (fUpdateJob == null) {
				fUpdateJob = new WorkbenchJob(IDEWorkbenchMessages.IDEWorkbenchActivityHelper_jobName) { //FLLDCIM: MARK — T09  ANONYMOUS
					@Override
					public IStatus runInUIThread(
							IProgressMonitor monitor) {
						runPendingUpdates();
						return Status.OK_STATUS;
					}
				};
				fUpdateJob.setSystem(true);
			}
			fUpdateJob.schedule();
		}
	}
}
