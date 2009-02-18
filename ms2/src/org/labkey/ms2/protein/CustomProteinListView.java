/*
 * Copyright (c) 2008 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.ms2.protein;

import org.labkey.api.view.*;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.Sort;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.ActionButton;
import org.labkey.api.security.ACL;

/**
 * User: jeckels
 * Date: Dec 3, 2008
 */
public class CustomProteinListView extends VBox
{
    public static final String NAME = "Custom Protein Lists";

    public CustomProteinListView(ViewContext context, boolean includeButtons)
    {
        DataRegion rgn = new DataRegion();
        rgn.setName(NAME);
        rgn.setColumns(ProteinManager.getTableInfoCustomAnnotationSet().getColumns("Name, Created, CreatedBy, CustomAnnotationSetId"));
        rgn.getDisplayColumn("Name").setURL("showAnnotationSet.view?CustomAnnotation.queryName=${Name}");
        rgn.getDisplayColumn("CustomAnnotationSetId").setVisible(false);
        GridView gridView = new GridView(rgn);
        rgn.setShowRecordSelectors(context.getContainer().hasPermission(context.getUser(), ACL.PERM_INSERT) || context.getContainer().hasPermission(context.getUser(), ACL.PERM_DELETE));
        gridView.setSort(new Sort("Name"));

        ButtonBar buttonBar = new ButtonBar();

        if (includeButtons)
        {
            ActionButton deleteButton = new ActionButton("", "Delete");
            ActionURL deleteURL = new ActionURL(ProteinController.DeleteCustomAnnotationSetsAction.class, context.getContainer());
            deleteButton.setURL(deleteURL);
            deleteButton.setRequiresSelection(true);
            deleteButton.setActionType(ActionButton.Action.POST);
            deleteButton.setDisplayPermission(ACL.PERM_DELETE);
            buttonBar.add(deleteButton);

            ActionButton addButton = new ActionButton(new ActionURL(ProteinController.UploadCustomProteinAnnotations.class, context.getContainer()), "Import Custom Protein List");
            addButton.setDisplayPermission(ACL.PERM_INSERT);
            addButton.setActionType(ActionButton.Action.LINK);
            buttonBar.add(addButton);
        }

        rgn.setButtonBar(buttonBar);

        if (!context.getContainer().isProject())
        {
            ActionURL link = ProteinController.getBeginURL(context.getContainer().getProject());
            HtmlView noteView = new HtmlView("This list only shows protein lists that have been loaded into this folder. When constructing queries, <a href=\"" + link + "\">annotations in the project</a> are visible from all the folders in that project.");
            addView(noteView);
        }
        addView(gridView);
    }
}