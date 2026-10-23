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
 *     Serge Beauchamp (Freescale Semiconductor) - [252996] Resource filters
 *******************************************************************************/
package org.eclipse.ui.internal.ide;

/**
 * Help context ids for the workbench.
 * <p>
 * This interface contains constants only; it is not intended to be implemented
 * or extended.
 * </p>
 */
public interface IIDEHelpContextIds {
	String PREFIX = IDEWorkbenchPlugin.IDE_WORKBENCH + "."; //$NON-NLS-1$ //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT

	// Actions
	String ADD_BOOKMARK_ACTION = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "add_bookmark_action_context"; //$NON-NLS-1$

	String ADD_TASK_ACTION = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "add_task_action_context"; //$NON-NLS-1$

	String INCREMENTAL_BUILD_ACTION = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "incremental_build_action_context"; //$NON-NLS-1$

	String FULL_BUILD_ACTION = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "full_build_action_context"; //$NON-NLS-1$

	String CLOSE_RESOURCE_ACTION = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "close_resource_action_context"; //$NON-NLS-1$

	String CLOSE_UNRELATED_PROJECTS_ACTION = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
	+ "close_unrelated_projects_action_context"; //$NON-NLS-1$

	String OPEN_RESOURCE_ACTION = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "open_resource_action_context"; //$NON-NLS-1$

	String OPEN_FILE_ACTION = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "open_file_action_context"; //$NON-NLS-1$

	String OPEN_LOCAL_FILE_ACTION = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
	+ "open_local_file_action_context"; //$NON-NLS-1$

	String OPEN_SYSTEM_EDITOR_ACTION = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "open_system_editor_action_context"; //$NON-NLS-1$

	String REFRESH_ACTION = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "refresh_action_context"; //$NON-NLS-1$

	String MOVE_RESOURCE_ACTION = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "move_resource_action_context"; //$NON-NLS-1$

	String COPY_RESOURCE_ACTION = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "copy_resource_action_context"; //$NON-NLS-1$

	String MOVE_PROJECT_ACTION = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "move_project_action_context"; //$NON-NLS-1$

	String COPY_PROJECT_ACTION = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "copy_project_action_context"; //$NON-NLS-1$

	String RENAME_RESOURCE_ACTION = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "rename_resource_action_context"; //$NON-NLS-1$

	String DELETE_RESOURCE_ACTION = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "delete_resource_action_context"; //$NON-NLS-1$

	String PROJECT_PROPERTY_DIALOG_ACTION = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "project_property_dialog_action_context"; //$NON-NLS-1$

	String CREATE_FOLDER_ACTION = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "create_folder_action_context"; //$NON-NLS-1$

	String CREATE_FILE_ACTION = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "create_file_action_context"; //$NON-NLS-1$

	String SCRUB_LOCAL_ACTION = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "scrub_local_action_context"; //$NON-NLS-1$

	String GLOBAL_INCREMENTAL_BUILD_ACTION = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "global_incremental_build_action_context"; //$NON-NLS-1$

	String GLOBAL_FULL_BUILD_ACTION = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "global_full_build_action_context"; //$NON-NLS-1$

	String QUICK_START_ACTION = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "quick_start_action_context"; //$NON-NLS-1$

	String TIPS_AND_TRICKS_ACTION = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "tips_and_tricks_action_context"; //$NON-NLS-1$

	String TEXT_CUT_ACTION = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "text_cut_action_context"; //$NON-NLS-1$

	String TEXT_COPY_ACTION = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "text_copy_action_context"; //$NON-NLS-1$

	String TEXT_PASTE_ACTION = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "text_paste_action_context"; //$NON-NLS-1$

	String TEXT_DELETE_ACTION = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "text_delete_action_context"; //$NON-NLS-1$

	String TEXT_SELECT_ALL_ACTION = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "text_select_all_action_context"; //$NON-NLS-1$

	String OPEN_WORKSPACE_FILE_ACTION = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "open_workspace_file_action_context"; //$NON-NLS-1$

	// Dialogs
	String PROJECT_LOCATION_SELECTION_DIALOG = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "project_location_selection_dialog_context"; //$NON-NLS-1$

	String CONTAINER_SELECTION_DIALOG = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "container_selection_dialog_context"; //$NON-NLS-1$

	String FILE_SELECTION_DIALOG = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "file_selection_dialog_context"; //$NON-NLS-1$

	String RESOURCE_SELECTION_DIALOG = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "resource_selection_dialog_context"; //$NON-NLS-1$

	String DELETE_PROJECT_DIALOG = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "delete_project_dialog_context"; //$NON-NLS-1$

	String MARKER_RESOLUTION_SELECTION_DIALOG = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "marker_resolution_selection_dialog_context"; //$NON-NLS-1$

	String WELCOME_PAGE_SELECTION_DIALOG = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "welcome_page_selection_dialog"; //$NON-NLS-1$

	String TIPS_AND_TRICKS_PAGE_SELECTION_DIALOG = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "tips_and_tricks_page_selection_dialog"; //$NON-NLS-1$

	String OPEN_RESOURCE_DIALOG = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "open_resource_dialog"; //$NON-NLS-1$

	String NEW_FOLDER_DIALOG = PREFIX + "new_folder_dialog"; //$NON-NLS-1$ //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT

	String PATH_VARIABLE_SELECTION_DIALOG = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "path_variable_selection_dialog"; //$NON-NLS-1$

	String IMPORT_TYPE_DIALOG = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
	+ "import_type_dialog"; //$NON-NLS-1$

	String SAVE_AS_DIALOG = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
	+ "save_as_dialog_context"; //$NON-NLS-1$


	// Editors
	String WELCOME_EDITOR = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "welcome_editor_context"; //$NON-NLS-1$

	// Preference pages
	String BUILD_ORDER_PREFERENCE_PAGE = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "build_order_preference_page_context"; //$NON-NLS-1$

	String FILE_STATES_PREFERENCE_PAGE = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "file_states_preference_page_context"; //$NON-NLS-1$

	String LINKED_RESOURCE_PREFERENCE_PAGE = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "linked_resource_preference_page_context"; //$NON-NLS-1$

	// Property pages
	String PROJECT_REFERENCE_PROPERTY_PAGE = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "project_reference_property_page_context"; //$NON-NLS-1$

	String PROJECT_NATURES_PROPERTY_PAGE = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "project_natures_property_page_context"; //$NON-NLS-1$

	String RESOURCE_FILTER_PROPERTY_PAGE = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "resource_filter_property_page_context"; //$NON-NLS-1$

	String EDIT_RESOURCE_FILTER_PROPERTY_PAGE = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "edit_resource_filter_property_page_context"; //$NON-NLS-1$

	String EDIT_RESOURCE_FILTER_DIALOG = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "edit_resource_filter_dialog_context"; //$NON-NLS-1$

	String RESOURCE_INFO_PROPERTY_PAGE = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "resource_info_property_page_context"; //$NON-NLS-1$

	String NEW_FILE_WIZARD_PAGE = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "new_file_wizard_page_context"; //$NON-NLS-1$

	// Wizard pages
	String NEW_PROJECT_WIZARD_PAGE = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "new_project_wizard_page_context"; //$NON-NLS-1$

	String NEW_PROJECT_REFERENCE_WIZARD_PAGE = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "new_project_reference_wizard_page_context"; //$NON-NLS-1$

	String NEW_FOLDER_WIZARD_PAGE = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "new_folder_wizard_page_context"; //$NON-NLS-1$

	String LINKED_RESOURCE_PAGE = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "linked_resource_page_context"; //$NON-NLS-1$

	String NEW_GROUP_WIZARD_PAGE = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "new_group_wizard_page_context"; //$NON-NLS-1$

	String NEW_LINK_WIZARD_PAGE = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "new_link_wizard_page_context"; //$NON-NLS-1$

	String WORKING_SET_RESOURCE_PAGE = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "working_set_resource_page"; //$NON-NLS-1$

	String WORKSPACE_PREFERENCE_PAGE = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "workspace_preference_page_context"; //$NON-NLS-1$

	// Wizards

	String NEW_FILE_WIZARD = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "new_file_wizard_context"; //$NON-NLS-1$

	String NEW_FOLDER_WIZARD = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "new_folder_wizard_context"; //$NON-NLS-1$

	String NEW_PROJECT_WIZARD = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			+ "new_project_wizard_context"; //$NON-NLS-1$

	String SWITCH_WORKSPACE_ACTION = PREFIX //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
	+ "switch_workspace_dialog_context"; //$NON-NLS-1$
}
