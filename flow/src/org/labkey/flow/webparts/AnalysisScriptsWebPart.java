/*
 * Copyright (c) 2007-2009 LabKey Corporation
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
import org.labkey.flow.controllers.executescript.AnalysisScriptController;
import org.labkey.flow.data.FlowScript;
import org.labkey.flow.data.FlowProtocolStep;
import org.labkey.api.view.*;
import org.labkey.api.data.*;
import org.labkey.api.query.AliasedColumn;
import org.labkey.api.security.ACL;
import org.labkey.api.util.PageFlowUtil;
import org.springframework.web.servlet.mvc.Controller;

import java.util.List;
import java.util.ArrayList;
import java.io.Writer;
import java.io.IOException;


public class AnalysisScriptsWebPart extends FlowQueryView
{
    static public final SimpleWebPartFactory FACTORY = new SimpleWebPartFactory("Flow Scripts", AnalysisScriptsWebPart.class);
    static
    {
        FACTORY.addLegacyNames("Flow Analysis Scripts");
    }

    public AnalysisScriptsWebPart(ViewContext portalCtx, Portal.WebPart wp) throws Exception
    {
        super(portalCtx, new FlowSchema(portalCtx.getUser(), portalCtx.getContainer()), null);
        FlowQuerySettings settings = (FlowQuerySettings)getSchema().getSettings(wp, portalCtx);
        settings.setAllowChooseQuery(false);
        settings.setAllowChooseView(false);
        settings.setQueryName(FlowTableType.AnalysisScripts.toString());
        setSettings(settings);
        
        setTitle("Flow Scripts");
        setButtonBarPosition(DataRegion.ButtonBarPosition.BOTTOM);
    }

    public List<DisplayColumn> getDisplayColumns()
    {
        List<DisplayColumn> ret = new ArrayList<DisplayColumn>();
        TableInfo table = getTable();
        ret.addAll(getQueryDef().getDisplayColumns(null, table));

        ColumnInfo colRowId = new AliasedColumn("RowId", table.getColumn("RowId"));
        ret.add(new PerformAnalysisColumn(colRowId));
        ret.add(new ScriptActionColumn("Copy", ScriptController.CopyAction.class, colRowId));
        ret.add(new ScriptActionColumn("Delete", ScriptController.DeleteAction.class, colRowId));

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

    public class ScriptActionColumn extends DataColumn
    {
        String _actionName;

        public ScriptActionColumn(String actionName, Class<? extends Controller> action, ColumnInfo col)
        {
            super(col);
            _actionName = actionName;
            ActionURL actionURL = new ActionURL(action, AnalysisScriptsWebPart.this.getContainer());
            setURL(actionURL + "scriptId=${RowId}");
            setCaption("");
            setWidth("40");
        }

        @Override
        public String getFormattedValue(RenderContext ctx)
        {
            return _actionName;
        }
    }

    public class PerformAnalysisColumn extends DataColumn
    {
        public PerformAnalysisColumn(ColumnInfo col)
        {
            super(col);
            setCaption("Execute Script");
            setNoWrap(true);
            setWidth("auto");
        }

        public FlowScript getScript(RenderContext ctx)
        {
            Object value = getBoundColumn().getValue(ctx);
            if (!(value instanceof Number))
                return null;
            int id = ((Number) value).intValue();
            return FlowScript.fromScriptId(id);
        }

        @Override
        public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
        {
            FlowScript script = getScript(ctx);
            if (script != null)
            {
                String and = "";

                if (script.hasStep(FlowProtocolStep.calculateCompensation))
                {
                    ActionURL url = script.urlFor(AnalysisScriptController.Action.chooseRunsToAnalyze, FlowProtocolStep.calculateCompensation);
                    out.write("<a href='" + PageFlowUtil.filter(url) + "'>Compensation</a>");
                    and = "<br>";
                }

                if (script.hasStep(FlowProtocolStep.analysis))
                {
                    ActionURL url = script.urlFor(AnalysisScriptController.Action.chooseRunsToAnalyze, FlowProtocolStep.analysis);
                    out.write(and);
                    out.write("<a href='" + PageFlowUtil.filter(url) + "'>Statistics and Graphs</a>");
                }

            }
            else
            {
                out.write("&nbsp;");
            }
        }
    }

}
