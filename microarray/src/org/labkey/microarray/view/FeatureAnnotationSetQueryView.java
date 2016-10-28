/*
 * Copyright (c) 2013-2016 LabKey Corporation
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
package org.labkey.microarray.view;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ActionButton;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DataRegion;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryForm;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.microarray.controllers.FeatureAnnotationSetController;
import org.springframework.validation.Errors;

/**
 * User: tgaluhn
 * Date: 4/29/13
 */
public class FeatureAnnotationSetQueryView extends QueryView
{
    public FeatureAnnotationSetQueryView(QueryForm form, Errors errors)
    {
        super(form, errors);
        init();
    }

    public FeatureAnnotationSetQueryView(UserSchema schema, QuerySettings settings, @Nullable Errors errors)
    {
        super(schema, settings, errors);
        init();
    }

    protected void init()
    {
        setTitle("Feature Annotation Sets");
        setTitleHref(new ActionURL(FeatureAnnotationSetController.ManageAction.class, getSchema().getContainer()));

        setShowDetailsColumn(false);
        setShowDeleteButton(false);
        setShowImportDataButton(false);
        setShowInsertNewButton(false);
        setShowUpdateColumn(false);

        setShowExportButtons(false);
        setShowBorders(true);
        setShadeAlternatingRows(true);

        setAllowableContainerFilterTypes(ContainerFilter.Type.Current, ContainerFilter.Type.CurrentPlusProjectAndShared);

        getSettings().getBaseSort().insertSortColumn(FieldKey.fromParts("Name"));
    }

    @Override
    protected void populateButtonBar(DataView view, ButtonBar bar)
    {
        super.populateButtonBar(view, bar);
        ActionButton deleteButton = new ActionButton(FeatureAnnotationSetController.DeleteAction.class, "Delete", DataRegion.MODE_GRID, ActionButton.Action.GET);
        deleteButton.setDisplayPermission(DeletePermission.class);
        ActionURL deleteURL = new ActionURL(FeatureAnnotationSetController.DeleteAction.class, getContainer());
        deleteURL.addParameter(ActionURL.Param.returnUrl, getViewContext().getActionURL().toString());
        deleteButton.setURL(deleteURL);
        deleteButton.setActionType(ActionButton.Action.POST);
        deleteButton.setRequiresSelection(true);
        bar.add(deleteButton);

        ActionButton uploadButton = new ActionButton(FeatureAnnotationSetController.UploadAction.class, "Import Feature Annotation Set", DataRegion.MODE_GRID, ActionButton.Action.LINK);
        ActionURL uploadURL = new ActionURL(FeatureAnnotationSetController.UploadAction.class, getContainer());
        uploadURL.addParameter(ActionURL.Param.returnUrl, getViewContext().getActionURL().toString());
        uploadButton.setURL(uploadURL);
        uploadButton.setDisplayPermission(UpdatePermission.class);
        bar.add(uploadButton);

        bar.add(new ActionButton(new ActionURL(FeatureAnnotationSetController.UploadAction.class, getContainer()), "Submit", DataRegion.MODE_UPDATE));
    }


}
