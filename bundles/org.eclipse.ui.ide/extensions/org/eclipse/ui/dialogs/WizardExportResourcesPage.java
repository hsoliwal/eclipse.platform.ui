/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     Mickael Istria (Red Hat Inc.) - Bug 486901
 *******************************************************************************/
package org.eclipse.ui.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.dialogs.ResourceTreeAndListGroup;
import org.eclipse.ui.internal.ide.DialogUtil;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Abstract superclass for a typical export wizard's main page.
 * <p>
 * Clients may subclass this page to inherit its common destination resource
 * selection facilities.
 * </p>
 * <p>
 * Subclasses must implement
 * </p>
 * <ul>
 * <li><code>createDestinationGroup</code></li>
 * </ul>
 * <p>
 * Subclasses may override
 * </p>
 * <ul>
 * <li><code>allowNewContainerName</code></li>
 * </ul>
 * <p>
 * Subclasses may extend
 * </p>
 * <ul>
 * <li><code>handleEvent</code></li>
 * <li><code>internalSaveWidgetValues</code></li>
 * <li><code>updateWidgetEnablements</code></li>
 * </ul>
 */
public abstract class WizardExportResourcesPage extends WizardDataTransferPage {
	private final IStructuredSelection initialResourceSelection;

	private List<Object> selectedTypes = new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW

	// widgets
	private ResourceTreeAndListGroup resourceGroup;

	private boolean showLinkedResources;

	private static final String SELECT_TYPES_TITLE = IDEWorkbenchMessages.WizardTransferPage_selectTypes;

	private static final String SELECT_ALL_TITLE = IDEWorkbenchMessages.WizardTransferPage_selectAll;

	private static final String DESELECT_ALL_TITLE = IDEWorkbenchMessages.WizardTransferPage_deselectAll;

	/**
	 * Creates an export wizard page. If the current resource selection is not empty
	 * then it will be used as the initial collection of resources selected for
	 * export.
	 *
	 * @param pageName  the name of the page
	 * @param selection {@link IStructuredSelection} of {@link IResource}
	 * @see IDE#computeSelectedResources(IStructuredSelection)
	 */
	protected WizardExportResourcesPage(String pageName, IStructuredSelection selection) {
		super(pageName);
		this.initialResourceSelection = selection;
	}

	/**
	 * The <code>addToHierarchyToCheckedStore</code> implementation of this
	 * <code>WizardDataTransferPage</code> method returns <code>false</code>.
	 * Subclasses may override this method.
	 */
	@Override
	protected boolean allowNewContainerName() {
		return false;
	}

	/**
	 * Creates a new button with the given id.
	 * <p>
	 * The <code>Dialog</code> implementation of this framework method creates a
	 * standard push button, registers for selection events including button presses
	 * and registers default buttons with its shell. The button id is stored as the
	 * buttons client data. Note that the parent's layout is assumed to be a
	 * GridLayout and the number of columns in this layout is incremented.
	 * Subclasses may override.
	 * </p>
	 *
	 * @param parent        the parent composite
	 * @param id            the id of the button (see
	 *                      <code>IDialogConstants.*_ID</code> constants for
	 *                      standard dialog button ids)
	 * @param label         the label from the button
	 * @param defaultButton <code>true</code> if the button is to be the default
	 *                      button, and <code>false</code> otherwise
	 */
	protected Button createButton(Composite parent, int id, String label, boolean defaultButton) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		// increment the number of columns in the button bar
		((GridLayout) parent.getLayout()).numColumns++;

		Button button = new Button(parent, SWT.PUSH);

		GridData buttonData = new GridData(GridData.FILL_HORIZONTAL);
		button.setLayoutData(buttonData);

		button.setData(id);
		button.setText(label);
		button.setFont(parent.getFont());

		if (defaultButton) {
			Shell shell = parent.getShell();
			if (shell != null) {
				shell.setDefaultButton(button);
			}
			button.setFocus();
		}
		button.setFont(parent.getFont());
		setButtonLayoutData(button);
		return button;
	}

	/**
	 * Creates the buttons for selecting specific types or selecting all or none of
	 * the elements.
	 *
	 * @param parent the parent control
	 */
	protected final void createButtonsGroup(Composite parent) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD

		Font font = parent.getFont();

		// top level group
		Composite buttonComposite = new Composite(parent, SWT.NONE);
		buttonComposite.setFont(parent.getFont());

		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.makeColumnsEqualWidth = true;
		buttonComposite.setLayout(layout);
		buttonComposite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

		// types edit button
		Button selectTypesButton = createButton(buttonComposite, IDialogConstants.SELECT_TYPES_ID, SELECT_TYPES_TITLE,
				false);

		SelectionListener listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleTypesEditButtonPressed();
			}
		};
		selectTypesButton.addSelectionListener(listener); //FLLDCIM: MARK — T03 SELECTION_LANE CALL
		selectTypesButton.setFont(font);
		setButtonLayoutData(selectTypesButton);

		Button selectButton = createButton(buttonComposite, IDialogConstants.SELECT_ALL_ID, SELECT_ALL_TITLE, false);

		listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				resourceGroup.setAllSelections(true);
				updateWidgetEnablements();
			}
		};
		selectButton.addSelectionListener(listener); //FLLDCIM: MARK — T03 SELECTION_LANE CALL
		selectButton.setFont(font);
		setButtonLayoutData(selectButton);

		Button deselectButton = createButton(buttonComposite, IDialogConstants.DESELECT_ALL_ID, DESELECT_ALL_TITLE,
				false);

		listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				resourceGroup.setAllSelections(false);
				updateWidgetEnablements();
			}
		};
		deselectButton.addSelectionListener(listener); //FLLDCIM: MARK — T03 SELECTION_LANE CALL
		deselectButton.setFont(font);
		setButtonLayoutData(deselectButton);

	}

	@Override //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
	public void createControl(Composite parent) {

		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
		composite.setFont(parent.getFont());

		createResourcesGroup(composite);
		createButtonsGroup(composite);

		createDestinationGroup(composite);

		createOptionsGroup(composite);

		restoreResourceSpecificationWidgetValues(); // ie.- local
		restoreWidgetValues(); // ie.- subclass hook
		if (initialResourceSelection != null) {
			setupBasedOnInitialSelections();
		}

		updateWidgetEnablements();
		setPageComplete(determinePageCompletion());
		setErrorMessage(null); // should not initially have error message

		setControl(composite);
	}

	/**
	 * Creates the export destination specification visual components.
	 * <p>
	 * Subclasses must implement this method.
	 * </p>
	 *
	 * @param parent the parent control
	 */
	protected abstract void createDestinationGroup(Composite parent);

	/**
	 * Creates the checkbox tree and list for selecting resources.
	 *
	 * @param parent the parent control
	 */
	protected final void createResourcesGroup(Composite parent) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD

		// create the input element, which has the root resource
		// as its only child
		List<IProject> input = new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : projects) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			if (project.isOpen()) {
				input.add(project);
			}
		}

		showLinkedResources = getShowLinkedResources();
		this.resourceGroup = new ResourceTreeAndListGroup(parent, input,
				getResourceProvider(IResource.FOLDER | IResource.PROJECT),
				WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider(),
				getResourceProvider(IResource.FILE),
				WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider(), SWT.NONE,
				DialogUtil.inRegularFontMode(parent));

		ICheckStateListener listener = unused -> updateWidgetEnablements(); //FLLDCIM: MARK — T21  LAMBDA

		this.resourceGroup.addCheckStateListener(listener); //FLLDCIM: MARK — T21 CHECK_STATE_LANE CALL
	}

	/*
	 * @see WizardDataTransferPage.getErrorDialogTitle()
	 */
	@Override
	protected String getErrorDialogTitle() {
		return IDEWorkbenchMessages.WizardExportPage_errorDialogTitle;
	}

	/**
	 * Obsolete method. This was implemented to handle the case where ensureLocal()
	 * needed to be called but it doesn't use it any longer.
	 *
	 * @deprecated Only retained for backwards compatibility.
	 */
	@Deprecated
	protected boolean ensureResourcesLocal(@SuppressWarnings("unused") List resources) {
		return true;
	}

	/**
	 * Returns a new subcollection containing only those resources which are not
	 * local.
	 *
	 * @param originalList the original list of resources (element type:
	 *                     <code>IResource</code>)
	 * @return the new list of non-local resources (element type:
	 *         <code>IResource</code>)
	 */
	protected List extractNonLocalResources(List originalList) {
		ArrayList<IResource> result = new ArrayList<>(originalList.size()); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		Iterator<IResource> resourcesEnum = originalList.iterator();

		while (resourcesEnum.hasNext()) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			IResource currentResource = resourcesEnum.next();
			if (!currentResource.isLocal(IResource.DEPTH_ZERO)) {
				result.add(currentResource);
			}
		}

		return result;
	}

	private static class ResourceProvider extends WorkbenchContentProvider { //FLLDCIM: MARK — M22  IMPLEMENTS
		private static final Object[] EMPTY = new Object[0];
		private final int resourceType;
		private final boolean showLinkedResources;

		public ResourceProvider(int resourceType, boolean showLinkedResources) {
			super();
			this.resourceType = resourceType;
			this.showLinkedResources = showLinkedResources;
		}

		@Override //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		public Object[] getChildren(Object o) {
			if (o instanceof IContainer container) {
				if (!showLinkedResources && container.isLinked(IResource.CHECK_ANCESTORS)) {
					// just return an empty set of children
					return EMPTY;
				}
				IResource[] members = null;
				try {
					members = container.members();
				} catch (CoreException e) {
					// just return an empty set of children
					return EMPTY;
				}

				// filter out the desired resource types
				List<IResource> results = new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
				for (IResource resource : members) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
					if (!showLinkedResources && resource.isLinked()) {
						continue;
					}
					if ((resource.getType() & resourceType) > 0) {
						results.add(resource);
					}
				}
				return results.toArray();
			}
			// input element case
			if (o instanceof ArrayList) {
				return ((List<?>) o).toArray();
			}
			return EMPTY;
		}
	}

	/**
	 * Returns a content provider for <code>IResource</code>s that returns only
	 * children of the given resource type.
	 */
	private ITreeContentProvider getResourceProvider(int resourceType) {
		return new ResourceProvider(resourceType, showLinkedResources);
	}

	/**
	 * Returns {<code>true</code> if the page tree and list providers should show
	 * linked resources. Default is false.
	 *
	 * @since 3.12
	 */
	protected boolean getShowLinkedResources() {
		return showLinkedResources;
	}

	/**
	 * Updates the resources tree to show or hide linked resources
	 *
	 * @param showLinked {<code>true</code> if the page should show linked resources
	 * @since 3.12
	 */
	protected void updateContentProviders(boolean showLinked) {
		showLinkedResources = showLinked;
		resourceGroup.setTreeProviders(getResourceProvider(IResource.FOLDER | IResource.PROJECT),
				WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider());

		resourceGroup.setListProviders(getResourceProvider(IResource.FILE),
				WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider());
	}

	/**
	 * Returns this page's collection of currently-specified resources to be
	 * exported. This is the primary resource selection facility accessor for
	 * subclasses.
	 *
	 * @return a collection of resources currently selected for export (element
	 *         type: <code>IResource</code>)
	 */
	protected List getSelectedResources() {
		Iterator<?> resourcesToExportIterator = this.getSelectedResourcesIterator();
		List<Object> resourcesToExport = new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		while (resourcesToExportIterator.hasNext()) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			resourcesToExport.add(resourcesToExportIterator.next());
		}
		return resourcesToExport;
	}

	/**
	 * Returns this page's collection of currently-specified resources to be
	 * exported. This is the primary resource selection facility accessor for
	 * subclasses.
	 *
	 * @return an iterator over the collection of resources currently selected for
	 *         export (element type: <code>IResource</code>). This will include
	 *         white checked folders and individually checked files.
	 */
	protected Iterator getSelectedResourcesIterator() {
		return this.resourceGroup.getAllCheckedListItems().iterator();
	}

	/**
	 * Returns the resource extensions currently specified to be exported.
	 *
	 * @return the resource extensions currently specified to be exported (element
	 *         type: <code>String</code>)
	 */
	protected List getTypesToExport() {

		return selectedTypes;
	}

	/**
	 * Returns this page's collection of currently-specified resources to be
	 * exported. This returns both folders and files - for just the files use
	 * getSelectedResources.
	 *
	 * @return a collection of resources currently selected for export (element
	 *         type: <code>IResource</code>)
	 */
	protected List getWhiteCheckedResources() {

		return this.resourceGroup.getAllWhiteCheckedItems();
	}

	/**
	 * Queries the user for the types of resources to be exported and selects them
	 * in the checkbox group.
	 */
	protected void handleTypesEditButtonPressed() {
		Object[] newSelectedTypes = queryResourceTypesToExport();

		if (newSelectedTypes != null) { // ie.- did not press Cancel
			this.selectedTypes = new ArrayList<>(newSelectedTypes.length); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
			this.selectedTypes.addAll(Arrays.asList(newSelectedTypes));
			setupSelectionsBasedOnSelectedTypes();
		}

	}

	/**
	 * Returns whether the extension of the given resource name is an extension that
	 * has been specified for export by the user.
	 *
	 * @param resourceName the resource name
	 * @return <code>true</code> if the resource name is suitable for export based
	 *         upon its extension
	 */
	protected boolean hasExportableExtension(String resourceName) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		if (selectedTypes == null) {
			return true;
		}

		int separatorIndex = resourceName.lastIndexOf('.');
		if (separatorIndex == -1) {
			return false;
		}

		String extension = resourceName.substring(separatorIndex + 1);

		Iterator<Object> it = selectedTypes.iterator();
		while (it.hasNext()) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			if (extension.equalsIgnoreCase((String) it.next())) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Persists additional setting that are to be restored in the next instance of
	 * this page.
	 * <p>
	 * The <code>WizardImportPage</code> implementation of this method does nothing.
	 * Subclasses may extend to persist additional settings.
	 * </p>
	 */
	protected void internalSaveWidgetValues() {
	}

	/**
	 * Queries the user for the resource types that are to be exported and returns
	 * these types as an array.
	 *
	 * @return the resource types selected for export (element type:
	 *         <code>String</code>), or <code>null</code> if the user canceled the
	 *         selection
	 */
	protected Object[] queryResourceTypesToExport() {

		TypeFilteringDialog dialog = new TypeFilteringDialog(getContainer().getShell(), getTypesToExport());

		dialog.open();

		return dialog.getResult();
	}

	/**
	 * Restores resource specification control settings that were persisted in the
	 * previous instance of this page. Subclasses wishing to restore persisted
	 * values for their controls may extend.
	 */
	protected void restoreResourceSpecificationWidgetValues() {
	}

	/**
	 * Persists resource specification control setting that are to be restored in
	 * the next instance of this page. Subclasses wishing to persist additional
	 * setting for their controls should extend hook method
	 * <code>internalSaveWidgetValues</code>.
	 */
	@Override
	protected void saveWidgetValues() {

		// allow subclasses to save values
		internalSaveWidgetValues();

	}

	/**
	 * Set the initial selections in the resource group.
	 */
	protected void setupBasedOnInitialSelections() {

		for (IResource currentResource : IDE.computeSelectedResources(this.initialResourceSelection)) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			if (currentResource.getType() == IResource.FILE) {
				this.resourceGroup.initialCheckListItem(currentResource);
			} else {
				this.resourceGroup.initialCheckTreeItem(currentResource);
			}
		}
	}

	/**
	 * Update the tree to only select those elements that match the selected types
	 */
	private void setupSelectionsBasedOnSelectedTypes() { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD

		Runnable runnable = () -> {
			Map<IContainer, List<IResource>> selectionMap = new Hashtable<>();
			// Only get the white selected ones
			Iterator<?> resourceIterator = resourceGroup.getAllWhiteCheckedItems().iterator();
			while (resourceIterator.hasNext()) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				// handle the files here - white checked containers require recursion
				IResource resource = (IResource) resourceIterator.next();
				if (resource.getType() == IResource.FILE) {
					if (hasExportableExtension(resource.getName())) {
						List<IResource> resourceList = new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
						IContainer parent = resource.getParent();
						if (selectionMap.containsKey(parent)) {
							resourceList = selectionMap.get(parent);
						}
						resourceList.add(resource);
						selectionMap.put(parent, resourceList);
					}
				} else {
					setupSelectionsBasedOnSelectedTypes(selectionMap, (IContainer) resource);
				}
			}
			resourceGroup.updateSelections(selectionMap);
		};

		BusyIndicator.showWhile(getShell().getDisplay(), runnable);

	}

	/**
	 * Set up the selection values for the resources and put them in the
	 * selectionMap. If a resource is a file see if it matches one of the selected
	 * extensions. If not then check the children.
	 */
	private void setupSelectionsBasedOnSelectedTypes(Map<IContainer, List<IResource>> selectionMap, IContainer parent) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD

		List<IResource> selections = new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		IResource[] resources;
		boolean hasFiles = false;

		try {
			resources = parent.members();
		} catch (CoreException exception) {
			// Just return if we can't get any info
			return;
		}

		for (IResource resource : resources) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			if (resource.getType() == IResource.FILE) {
				if (hasExportableExtension(resource.getName())) {
					hasFiles = true;
					selections.add(resource);
				}
			} else {
				setupSelectionsBasedOnSelectedTypes(selectionMap, (IContainer) resource);
			}
		}

		// Only add it to the list if there are files in this folder
		if (hasFiles) {
			selectionMap.put(parent, selections);
		}
	}

	/**
	 * Save any editors that the user wants to save before export.
	 *
	 * @return boolean if the save was successful.
	 */
	protected boolean saveDirtyEditors() {
		return PlatformUI.getWorkbench().saveAllEditors(true);
	}

	/**
	 * Check if widgets are enabled or disabled by a change in the dialog.
	 */
	@Override
	protected void updateWidgetEnablements() {

		boolean pageComplete = determinePageCompletion();
		setPageComplete(pageComplete);
		if (pageComplete) {
			setMessage(null);
		}
		super.updateWidgetEnablements();
	}
}
