/*
 * Copyright (c) 2008-2016 LabKey Corporation
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

package org.labkey.microarray;

import org.labkey.api.data.ActionButton;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.MenuButton;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExperimentUrls;
import org.labkey.api.exp.query.ExpDataTable;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineUrls;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.reports.ReportService;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.AssayUrls;
import org.labkey.api.study.permissions.DesignAssayPermission;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.SimpleTextDisplayElement;
import org.labkey.api.view.ViewContext;
import org.labkey.microarray.assay.MicroarrayAssayProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: jeckels
 * Date: Jan 9, 2008
 */
public class PendingMageMLFilesView extends QueryView
{
    public PendingMageMLFilesView(ViewContext context)
    {
        super(new ExpSchema(context.getUser(), context.getContainer()));
        setSettings(createSettings(context));
        setShadeAlternatingRows(true);
        setShowBorders(true);
        setShowExportButtons(false);
        setShowDetailsColumn(false);
        setViewItemFilter(ReportService.EMPTY_ITEM_LIST);
    }

    private QuerySettings createSettings(ViewContext context)
    {
        return getSchema().getSettings(context, "PendingMageMLFiles", ExpSchema.TableType.Data.name());
    }

    protected void populateButtonBar(DataView view, ButtonBar bar)
    {
//        super.populateButtonBar(view, bar);

        List<ExpProtocol> microarrayProtocols = new ArrayList<>();
        for (ExpProtocol protocol : AssayService.get().getAssayProtocols(getContainer()))
        {
            if (AssayService.get().getProvider(protocol) instanceof MicroarrayAssayProvider)
            {
                microarrayProtocols.add(protocol);
            }
        }
        Collections.sort(microarrayProtocols);

        PipeRoot root = PipelineService.get().findPipelineRoot(getContainer());

        ActionURL deleteURL = PageFlowUtil.urlProvider(ExperimentUrls.class).getDeleteDatasURL(view.getViewContext().getContainer(), getReturnURL());
        ActionButton deleteButton = new ActionButton(deleteURL, "Delete");
        deleteButton.setRequiresSelection(true, null, null, null, null, view.getDataRegion().getJavascriptFormReference());
        deleteButton.setActionType(ActionButton.Action.POST);
        deleteButton.setDisplayPermission(DeletePermission.class);
        bar.add(deleteButton);

        if (root == null)
        {
            SimpleTextDisplayElement element = new SimpleTextDisplayElement("Unable to import because pipeline has not been configured. " + PageFlowUtil.textLink("setup pipeline", PageFlowUtil.urlProvider(PipelineUrls.class).urlSetup(getContainer())), true);
            element.setDisplayPermission(InsertPermission.class);
            bar.add(element);
        }
        else
        {
            if (microarrayProtocols.size() == 0)
            {
                ActionURL url = PageFlowUtil.urlProvider(AssayUrls.class).getDesignerURL(getContainer(), MicroarrayAssayProvider.NAME, getViewContext().getActionURL());
                ActionButton button = new ActionButton(url, "Create New Microarray Assay Design");
                button.setActionType(ActionButton.Action.LINK);
                button.setDisplayPermission(DesignAssayPermission.class);
                bar.add(button);
            }
            else
            {
                if (microarrayProtocols.size() == 1)
                {
                    ExpProtocol protocol = microarrayProtocols.get(0);
                    ActionURL url = PageFlowUtil.urlProvider(AssayUrls.class).getImportURL(getContainer(), protocol, null, null);
                    ActionButton button = new ActionButton(url, "Import using " + protocol.getName());
                    button.setRequiresSelection(true, null, null, null, null, view.getDataRegion().getJavascriptFormReference());
                    button.setActionType(ActionButton.Action.POST);
                    button.setDisplayPermission(InsertPermission.class);
                    bar.add(button);
                }
                else
                {
                    MenuButton menu = new MenuButton("Import selected using...");
                    menu.setRequiresSelection(true);
                    menu.setDisplayPermission(InsertPermission.class);
                    bar.add(menu);
                    for (ExpProtocol protocol : microarrayProtocols)
                    {
                        ActionURL url = PageFlowUtil.urlProvider(AssayUrls.class).getImportURL(getContainer(), protocol, null, null);
                        menu.addMenuItem("Import using " + protocol.getName(), "javascript: if (verifySelected(" + view.getDataRegion().getJavascriptFormReference() + ", '" + url.getLocalURIString() + "', 'POST', 'files')) { " + view.getDataRegion().getJavascriptFormReference() + ".submit(); }");
                    }
                }
                ActionURL browseURL = PageFlowUtil.urlProvider(PipelineUrls.class).urlBrowse(getContainer(), getReturnURL());
                ActionButton browseButton = new ActionButton(browseURL, "Browse for MageML Files");
                browseButton.setDisplayPermission(InsertPermission.class);
                bar.add(browseButton);
            }
        }
    }

    protected TableInfo createTable()
    {
        ExpDataTable table = ExperimentService.get().createDataTable("pendingFile", getSchema());
        table.setRun(null);
        table.setDataType(MicroarrayModule.MAGE_ML_INPUT_TYPE);
        table.populate();
        return table;
    }

    public ExpSchema getSchema()
    {
        return (ExpSchema)super.getSchema();
    }
}
