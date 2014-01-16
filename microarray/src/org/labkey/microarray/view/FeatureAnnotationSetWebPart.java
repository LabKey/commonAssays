/*
 * Copyright (c) 2013-2014 LabKey Corporation
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

import org.labkey.api.data.ActionButton;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DataRegion;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.ViewContext;
import org.labkey.microarray.controllers.FeatureAnnotationSetController;
import org.labkey.microarray.query.MicroarrayUserSchema;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

/**
 * User: tgaluhn
 * Date: 4/29/13
 */
public class FeatureAnnotationSetWebPart extends QueryView
{
    private String _featureAnnotationSetError;

    public FeatureAnnotationSetWebPart(ViewContext viewContext)
    {
        super(new MicroarrayUserSchema(viewContext.getUser(), viewContext.getContainer()));

        setSettings(createQuerySettings(viewContext, "FeatureAnnotationSet"));
        setTitle("Feature Annotation Sets");
        setTitleHref(new ActionURL(FeatureAnnotationSetController.ManageAction.class, viewContext.getContainer()));

        setShowDetailsColumn(false);
        setShowDeleteButton(false);
        setShowImportDataButton(false);
        setShowInsertNewButton(false);
        setShowUpdateColumn(false);

        setShowExportButtons(false);
        setShowBorders(true);
        setShadeAlternatingRows(true);

        setAllowableContainerFilterTypes(ContainerFilter.Type.Current, ContainerFilter.Type.CurrentPlusProjectAndShared);
    }

    private QuerySettings createQuerySettings(ViewContext portalCtx, String dataRegionName)
    {
        UserSchema schema = getSchema();
        QuerySettings settings = schema.getSettings(portalCtx, dataRegionName, MicroarrayUserSchema.TABLE_FEATURE_ANNOTATION_SET);
        if (settings.getContainerFilterName() == null)
        {
            settings.setContainerFilterName(ContainerFilter.CurrentPlusProjectAndShared.class.getSimpleName());
        }
        settings.getBaseSort().insertSortColumn(FieldKey.fromParts("Name"));
        return settings;
    }

    @Override
    protected void populateButtonBar(DataView view, ButtonBar bar, boolean exportAsWebPage)
    {
        super.populateButtonBar(view, bar, exportAsWebPage);
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


    @Override
    protected void renderView(Object model, HttpServletRequest request, HttpServletResponse response) throws Exception
    {
        PrintWriter out = response.getWriter();
        if (_featureAnnotationSetError != null)
        {
            out.write("<font class=\"labkey-error\">" + PageFlowUtil.filter(_featureAnnotationSetError) + "</font><br>");
        }
        super.renderView(model, request, response);
    }

    public void setFeatureAnnotationSetError(String featureAnnotationSetError)
    {
        _featureAnnotationSetError = featureAnnotationSetError;
    }
}
