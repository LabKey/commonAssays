/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

package org.labkey.flow.webparts;

import org.labkey.flow.view.FlowQueryView;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.query.FlowQuerySettings;
import org.labkey.flow.query.FlowTableType;
import org.labkey.flow.controllers.editscript.ScriptController;
import org.labkey.api.view.*;
import org.labkey.api.data.*;
import org.labkey.api.query.AliasedColumn;
import org.labkey.api.security.ACL;
import org.labkey.api.util.PageFlowUtil;

import java.util.List;
import java.util.ArrayList;

public class AnalysisScriptsWebPart extends FlowQueryView
{
    static public final WebPartFactory FACTORY = new Factory();

    static class Factory extends WebPartFactory
    {
        Factory()
        {
            super("Flow Scripts");
            addLegacyNames("Flow Analysis Scripts");
        }

        public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart wp) throws Exception
        {
            FlowSchema schema = new FlowSchema(portalCtx.getUser(), portalCtx.getContainer());
            FlowQuerySettings settings = (FlowQuerySettings)schema.getSettings(wp, portalCtx);
            settings.setAllowChooseQuery(false);
            settings.setAllowChooseView(false);
            settings.setQueryName(FlowTableType.AnalysisScripts.toString());
            return new AnalysisScriptsWebPart(new ViewContext(portalCtx), schema, settings);
        }
    }

    public AnalysisScriptsWebPart(ViewContext context, FlowSchema schema, FlowQuerySettings settings)
    {
        super(context, schema, settings);
        setTitle("Flow Scripts");
        setButtonBarPosition(DataRegion.ButtonBarPosition.BOTTOM);
    }

    public List<DisplayColumn> getDisplayColumns()
    {
        List<DisplayColumn> ret = new ArrayList<DisplayColumn>();
        TableInfo table = getTable();
        ret.addAll(getQueryDef().getDisplayColumns(null, table));
        ColumnInfo colScriptType = new AliasedColumn("Type", table.getColumn("RowId"));
        ret.add(new AnalysisScriptTypeColumn(colScriptType));
        return ret;
    }

    protected void populateButtonBar(DataView view, ButtonBar bar)
    {
        if (!getViewContext().hasPermission(ACL.PERM_UPDATE))
            return;
        ActionButton btnNewScript = new ActionButton("Create New Analysis Script", PageFlowUtil.urlFor(ScriptController.Action.newProtocol, getContainer()));
        bar.add(btnNewScript);
    }

    protected DataRegion createDataRegion()
    {
        DataRegion ret = super.createDataRegion();
        ret.setFixedWidthColumns(false);
        return ret;
    }
}
