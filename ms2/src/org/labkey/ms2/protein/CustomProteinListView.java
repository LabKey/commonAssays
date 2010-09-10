/*
 * Copyright (c) 2008-2010 LabKey Corporation
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

import org.labkey.api.data.ActionButton;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.Sort;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.view.*;
import org.springframework.validation.BindException;

import java.util.Collections;

/**
 * User: jeckels
 * Date: Dec 3, 2008
 */
public class CustomProteinListView extends VBox
{
    public static final String NAME = "Custom Protein Lists";

    public CustomProteinListView(ViewContext context, boolean includeButtons)
    {
        QuerySettings settings = new QuerySettings(context, NAME);

        DataRegion rgn = new DataRegion();
        rgn.setSettings(settings);
        rgn.setColumns(ProteinManager.getTableInfoCustomAnnotationSet().getColumns("Name, Created, CreatedBy, CustomAnnotationSetId"));
        rgn.getDisplayColumn("Name").setURLExpression(new DetailsURL(new ActionURL(ProteinController.ShowAnnotationSetAction.class, context.getContainer()), Collections.singletonMap("CustomAnnotation.queryName", "Name")));
        rgn.getDisplayColumn("CustomAnnotationSetId").setVisible(false);
        GridView gridView = new GridView(rgn, (BindException)null);
        rgn.setShowRecordSelectors((context.getContainer().hasPermission(context.getUser(), InsertPermission.class) || context.getContainer().hasPermission(context.getUser(), DeletePermission.class)) && includeButtons);
        gridView.setSort(new Sort("Name"));

        ButtonBar buttonBar = new ButtonBar();

        if (includeButtons)
        {
            ActionURL deleteURL = new ActionURL(ProteinController.DeleteCustomAnnotationSetsAction.class, context.getContainer());
            ActionButton deleteButton = new ActionButton(deleteURL, "Delete");
            deleteButton.setRequiresSelection(true);
            deleteButton.setActionType(ActionButton.Action.POST);
            deleteButton.setDisplayPermission(DeletePermission.class);
            buttonBar.add(deleteButton);

            ActionButton addButton = new ActionButton(new ActionURL(ProteinController.UploadCustomProteinAnnotations.class, context.getContainer()), "Import Custom Protein List");
            addButton.setDisplayPermission(InsertPermission.class);
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
