/*
 * Copyright (c) 2007-2017 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.labkey.api.stats.ColumnAnalyticsProvider;
import org.labkey.api.data.ActionButton;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.AliasedColumn;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.Portal;
import org.labkey.api.view.SimpleWebPartFactory;
import org.labkey.api.view.ViewContext;
import org.labkey.flow.controllers.editscript.ScriptController;
import org.labkey.flow.controllers.executescript.AnalysisScriptController;
import org.labkey.flow.data.FlowProtocolStep;
import org.labkey.flow.data.FlowScript;
import org.labkey.flow.query.FlowQuerySettings;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.query.FlowTableType;
import org.labkey.flow.view.FlowQueryView;
import org.springframework.web.servlet.mvc.Controller;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class AnalysisScriptsWebPart extends FlowQueryView
{
    static public final SimpleWebPartFactory FACTORY = new SimpleWebPartFactory("Flow Scripts", AnalysisScriptsWebPart.class);
    static
    {
        FACTORY.addLegacyNames("Flow Analysis Scripts");
    }

    public AnalysisScriptsWebPart(ViewContext portalCtx, Portal.WebPart wp)
    {
        super(new FlowSchema(portalCtx.getUser(), portalCtx.getContainer()), null, null);
        FlowQuerySettings settings = (FlowQuerySettings)getSchema().getSettings(wp, portalCtx);
        settings.setAllowChooseQuery(false);
        settings.setAllowChooseView(false);
        settings.setSchemaName(FlowSchema.SCHEMANAME);
        settings.setQueryName(FlowTableType.AnalysisScripts.toString());
        setSettings(settings);
        
        setTitle("Flow Scripts");
        setShowExportButtons(false);
        setShowPagination(false);
        setButtonBarPosition(DataRegion.ButtonBarPosition.TOP);
    }

    public List<DisplayColumn> getDisplayColumns()
    {
        List<DisplayColumn> ret = new ArrayList<>();
        TableInfo table = getTable();
        ret.addAll(getQueryDef().getDisplayColumns(null, table));

        ColumnInfo colRowId = new AliasedColumn("RowId", table.getColumn("RowId"));
        if (getContainer().hasPermission(getUser(), UpdatePermission.class))
        {
            // PerformAnalsysisColumn is too expensive
            //ret.add(new PerformAnalysisColumn(colRowId));
            ret.add(new ScriptActionColumn("Copy", ScriptController.CopyAction.class, colRowId));
            ret.add(new ScriptActionColumn("Delete", ScriptController.DeleteAction.class, colRowId));
        }

        return ret;
    }

    protected void populateButtonBar(DataView view, ButtonBar bar)
    {
        ActionButton btnNewScript = new ActionButton("Create Analysis Script", new ActionURL(ScriptController.NewProtocolAction.class, getContainer()));
        btnNewScript.setDisplayPermission(UpdatePermission.class);
        bar.add(btnNewScript);
    }

    public class ScriptActionColumn extends DataColumn
    {
        String _actionName;

        public ScriptActionColumn(@NotNull String actionName, @NotNull Class<? extends Controller> action, @NotNull ColumnInfo col)
        {
            super(col);
            _actionName = actionName;
            ActionURL actionURL = new ActionURL(action, AnalysisScriptsWebPart.this.getContainer());
            setURL(actionURL + "scriptId=${RowId}");
            setCaption("");
            setWidth("40");
        }

        @Override @NotNull
        public String getFormattedValue(RenderContext ctx)
        {
            return _actionName;
        }

        @NotNull
        @Override
        public List<ColumnAnalyticsProvider> getAnalyticsProviders()
        {
            return Collections.emptyList();
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
                    ActionURL url = script.urlFor(AnalysisScriptController.ChooseRunsToAnalyzeAction.class, FlowProtocolStep.calculateCompensation);
                    out.write("<a href='" + PageFlowUtil.filter(url) + "'>Compensation</a>");
                    and = "<br>";
                }

                if (script.hasStep(FlowProtocolStep.analysis))
                {
                    ActionURL url = script.urlFor(AnalysisScriptController.ChooseRunsToAnalyzeAction.class, FlowProtocolStep.analysis);
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
