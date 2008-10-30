/*
 * Copyright (c) 2006-2008 LabKey Corporation
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

package org.labkey.flow.view;

import org.apache.log4j.Logger;
import org.labkey.api.data.*;
import org.labkey.api.exp.api.ExpRunTable;
import org.labkey.api.jsp.JspLoader;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.portal.ProjectUrls;
import org.labkey.api.query.QueryView;
import org.labkey.api.reports.ReportService;
import org.labkey.api.reports.report.ChartQueryReport;
import org.labkey.api.reports.report.RReport;
import org.labkey.api.security.ACL;
import org.labkey.api.security.User;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.*;
import org.labkey.flow.controllers.FlowController;
import org.labkey.flow.FlowModule;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.query.FlowQueryForm;
import org.labkey.flow.query.FlowQuerySettings;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.query.SubtractBackgroundQuery;
import org.labkey.flow.webparts.FlowFolderType;

import java.io.PrintWriter;
import java.util.*;

public class FlowQueryView extends QueryView
{
    List<DisplayColumn> _displayColumns;
    boolean __hasGraphs;
    private SubtractBackgroundQuery _subtractQuery;

    public FlowQueryView(FlowQueryForm form)
    {
        this(form.getViewContext(), (FlowSchema) form.getSchema(), (FlowQuerySettings) form.getQuerySettings());
    }

    public FlowQueryView(ViewContext context, FlowSchema schema, FlowQuerySettings settings)
    {
        super(schema, settings);
        setShadeAlternatingRows(true);
        setShowBorders(true);
        setViewItemFilter(new ReportService.ItemFilter() {
            public boolean accept(String type, String label)
            {
                if (RReport.TYPE.equals(type)) return true;
                if (ChartQueryReport.TYPE.equals(type)) return true;
                return false;
            }
        });
    }

//    protected MenuButton createExportMenuButton(boolean exportAsWebPage)
//    {
//        MenuButton button = super.createExportMenuButton(exportAsWebPage);
//
//        // XXX: only add menu item for queries based on FCSAnalyses
//        ActionURL url = getViewContext().cloneActionURL();
//        url.setAction(RunController.ExportToSpiceAction.class);
//        button.addMenuItem("Export All to Spice (.spd)", url);
//        return button;
//    }

    protected boolean showRecordSelectors()
    {
        if (!getViewContext().hasPermission(ACL.PERM_DELETE))
            return false;
        TableInfo mainTable = getQueryDef().getMainTable();
        if (mainTable instanceof ExpRunTable)
        {
            return true;
        }
        return false;
    }

    public DataView createDataView()
    {
        DataView ret = super.createDataView();

        if (hasGraphs() && showGraphs())
        {
            ret = new GraphView(ret);
        }
        ret.getDataRegion().setShowPaginationCount(false);
        return ret;
    }

    private static Logger _log = Logger.getLogger(FlowQueryView.class);

    protected User getUser()
    {
        return getViewContext().getUser();
    }

    protected void renderView(Object model, PrintWriter out) throws Exception
    {
        if (!isPrintView())
        {
            if (hasGraphs())
            {
                if (showGraphs())
                {
                    ActionURL urlHide = getViewContext().cloneActionURL();
                    urlHide.deleteParameter(param("showGraphs"));
                    out.write(textLink("Hide Graphs", urlHide));
                    JspView view = new JspView(JspLoader.createPage(getViewContext().getRequest(), FlowQueryView.class, "setGraphSize.jsp"));
                    view.setFrame(FrameType.NONE);
                    HttpView.currentView().include(view, out);
                }
                else
                {
                    ActionURL urlShow = getViewContext().cloneActionURL();
                    urlShow.addParameter(param("showGraphs"), "true");
                    out.write(textLink("Show Graphs", urlShow));
                }
                out.write("<br>");
            }
        }
        super.renderView(model, out);
    }

    protected Object renderErrors(PrintWriter out, String message, List<String> errors)
    {
        out.print("<p class=\"labkey-error\" style=\"padding-left:1em; text-indent:-1em;\">");
        out.print(PageFlowUtil.filter(message));
        for (String e : errors)
        {
            out.append("<br>").append(PageFlowUtil.filter(e)).append("</span>");
        }
        out.print("</p>");
        return null;
    }

    public FlowQuerySettings getSettings()
    {
        return (FlowQuerySettings) super.getSettings();
    }

    protected boolean showGraphs()
    {
        return getSettings().getShowGraphs();
    }

    protected boolean subtractBackground()
    {
        return getSettings().getSubtractBackground();
    }

    protected ActionURL urlChangeView()
    {
        ActionURL ret = super.urlChangeView();
        ret.deleteParameter(FlowParam.experimentId.toString());
        return ret;
    }

    public FlowSchema getSchema()
    {
        return (FlowSchema) super.getSchema();
    }

    protected void populateButtonBar(DataView view, ButtonBar bar,  boolean exportAsWebPage)
    {
        if (getSchema().getRun() == null)
        {
            FlowExperiment[] experiments = FlowExperiment.getAnalysesAndWorkspace(getContainer());
            ActionURL target = urlChangeView();
            MenuButton button = new MenuButton("Analysis Folder");

            Map<Integer, String> availableExperiments = new LinkedHashMap();
            availableExperiments.put(0, "All Analysis Folders");

            for (FlowExperiment experiment : experiments)
                availableExperiments.put(experiment.getExperimentId(), experiment.getName());

            FlowExperiment current = getSchema().getExperiment();
            int currentId = current == null ? 0 : current.getExperimentId();

            if (!availableExperiments.containsKey(currentId))
                availableExperiments.put(current.getExperimentId(), current.getName());

            for (Map.Entry<Integer, String> entry : availableExperiments.entrySet())
            {
                button.addMenuItem(entry.getValue(),
                        target.clone().replaceParameter(FlowParam.experimentId, String.valueOf(entry.getKey())).toString(),
                        null,
                        currentId == entry.getKey());
            }
            bar.add(button);
        }
        super.populateButtonBar(view, bar, exportAsWebPage);
    }

    public List<DisplayColumn> getDisplayColumns()
    {
        if (_displayColumns != null)
            return _displayColumns;
        _displayColumns = super.getDisplayColumns();
        __hasGraphs = false;
        boolean showGraphs = showGraphs();
        for (Iterator<DisplayColumn> it = _displayColumns.iterator(); it.hasNext();)
        {
            DisplayColumn dc = it.next();
            if (dc instanceof GraphColumn)
            {
                __hasGraphs = true;
                if (showGraphs)
                {
                    return _displayColumns;
                }
                it.remove();
            }
        }
        return _displayColumns;
    }

    protected boolean hasGraphs()
    {
        getDisplayColumns();
        return __hasGraphs;
    }

    public NavTrailConfig getNavTrailConfig()
    {
        NavTrailConfig ntc = super.getNavTrailConfig();
        FlowSchema schema = getSchema();
        FlowRun run = schema.getRun();
        List<NavTree> children = new ArrayList<NavTree>();
        if (getContainer().getFolderType() instanceof FlowFolderType)
        {
            children.add(0, new NavTree("Flow Dashboard", PageFlowUtil.urlProvider(ProjectUrls.class).getStartURL(getContainer())));
        }
        else
        {
            children.add(0, new NavTree(FlowModule.getShortProductName(), PageFlowUtil.urlFor(FlowController.Action.begin, getContainer())));
        }
        if (run != null)
        {
            FlowExperiment experiment = run.getExperiment();
            if (experiment != null)
            {
                children.add(new NavTree(experiment.getLabel(), experiment.urlShow()));
            }
            children.add(new NavTree(run.getLabel(), run.urlShow()));
            ntc.setExtraChildren(children.toArray(new NavTree[0]));
        }
        else if (schema.getExperiment() != null)
        {
            children.add(new NavTree(schema.getExperiment().getLabel(), schema.getExperiment().urlShow()));
            ntc.setExtraChildren(children.toArray(new NavTree[0]));
        }
        ntc.setModuleOwner(ModuleLoader.getInstance().getModule(FlowModule.NAME));
        return ntc;
    }

    protected TSVGridWriter.ColumnHeaderType getColumnHeaderType()
    {
        return TSVGridWriter.ColumnHeaderType.queryColumnName;
    }
}