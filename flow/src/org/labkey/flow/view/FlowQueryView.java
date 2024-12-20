/*
 * Copyright (c) 2007-2019 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ActionButton;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.ColumnHeaderType;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.MenuButton;
import org.labkey.api.data.PanelButton;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentUrls;
import org.labkey.api.portal.ProjectUrls;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.study.StudyUrls;
import org.labkey.api.study.publish.StudyPublishService;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTrailConfig;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.PopupMenu;
import org.labkey.flow.FlowModule;
import org.labkey.flow.controllers.FlowController;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.controllers.well.WellController;
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.query.FlowQueryForm;
import org.labkey.flow.query.FlowQuerySettings;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.query.FlowTableType;
import org.labkey.flow.webparts.FlowFolderType;
import org.springframework.validation.Errors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FlowQueryView extends QueryView
{
    List<DisplayColumn> _displayColumns;
    boolean __hasGraphs;

    public FlowQueryView(FlowQueryForm form)
    {
        this((FlowSchema) form.getSchema(), (FlowQuerySettings) form.getQuerySettings(), null);
    }

    public FlowQueryView(FlowSchema schema, FlowQuerySettings settings, Errors errors)
    {
        super(schema, settings, errors);
        setShadeAlternatingRows(true);
        setShowBorders(true);

        // CONSIDER: Only show selectors if user can export, delete, or publish/link to study
        setShowRecordSelectors(true);
    }

    @Override
    protected boolean canInsert()
    {
        return false;
    }

    @Override
    protected boolean canUpdate()
    {
        return false;
    }

    @Override
    protected DataRegion createDataRegion()
    {
        if (hasGraphs() && showGraphs() == FlowQuerySettings.ShowGraphs.Inline)
        {
            DataRegion rgn = new GraphDataRegion();
            configureDataRegion(rgn);
            return rgn;
        }
        else
        {
            return super.createDataRegion();
        }
    }

    @Override
    protected void configureDataRegion(DataRegion rgn)
    {
        super.configureDataRegion(rgn);
        rgn.setShowPaginationCount(false);
    }

    @Override
    public User getUser()
    {
        return getViewContext().getUser();
    }

    @Override
    protected void renderView(Object model, HttpServletRequest request, HttpServletResponse response) throws Exception
    {
        if (!isPrintView())
        {
            PrintWriter out = response.getWriter();
            if (hasGraphs())
            {
                FlowQuerySettings.ShowGraphs showGraphs = showGraphs();
                ActionURL urlShow = getViewContext().cloneActionURL();

                NavTree showNone = new NavTree("None", urlShow.clone().deleteParameter(param("showGraphs")));
                showNone.setSelected(showGraphs == null || showGraphs == FlowQuerySettings.ShowGraphs.None);

                NavTree showHover = new NavTree("Thumbnail", urlShow.clone().replaceParameter(param("showGraphs"), FlowQuerySettings.ShowGraphs.Thumbnail.name()));
                showHover.setSelected(showGraphs == FlowQuerySettings.ShowGraphs.Thumbnail);

                NavTree showInline = new NavTree("Inline", urlShow.clone().replaceParameter(param("showGraphs"), FlowQuerySettings.ShowGraphs.Inline.name()));
                showInline.setSelected(showGraphs == FlowQuerySettings.ShowGraphs.Inline);

                NavTree navtree = new NavTree("Show Graphs", (ActionURL)null);
                navtree.addChild(showNone);
                navtree.addChild(showHover);
                navtree.addChild(showInline);
                PopupMenu menu = new PopupMenu(navtree, PopupMenu.Align.LEFT, PopupMenu.ButtonStyle.TEXT);
                menu.render(out);

                if (showGraphs == FlowQuerySettings.ShowGraphs.Inline)
                {
                    JspView view = new SetGraphSizeView();
                    view.setFrame(FrameType.NONE);
                    HttpView.currentView().include(view, out);
                }
                out.write("<br>");
            }
        }
        super.renderView(model, request, response);
    }

    @Override
    public FlowQuerySettings getSettings()
    {
        return (FlowQuerySettings) super.getSettings();
    }

    protected FlowQuerySettings.ShowGraphs showGraphs()
    {
        return getSettings().getShowGraphs();
    }

    protected boolean showAnalysisFolderButton()
    {
        // Don't show the "Analysis Folder" menu for some grids (flow.Analyses, flow.Statistics, flow.Keywords, etc.) but do show it for custom queries.
        String queryName = getSettings().getQueryName();
        if (queryName == null)
            return false;

        try
        {
            FlowTableType ftt = FlowTableType.valueOf(queryName);
            return (ftt == FlowTableType.CompensationControls ||
                    ftt == FlowTableType.CompensationMatrices ||
                    ftt == FlowTableType.FCSAnalyses ||
                    ftt == FlowTableType.FCSFiles ||
                    ftt == FlowTableType.Runs);
        }
        catch (IllegalArgumentException ex)
        {
            // ok, custom query
            return true;
        }
    }

    @Override
    public boolean showDeleteButton()
    {
        // Don't allow delete for individual FCS files or for wsp samples (which we represent as FCSAnalyses)
        return !(getSettings().getQueryName().equalsIgnoreCase(FlowTableType.FCSFiles.name()) || getSettings().getQueryName().equalsIgnoreCase(FlowTableType.FCSAnalyses.name()));
    }

    @Override
    protected URLHelper urlChangeView()
    {
        URLHelper ret = super.urlChangeView();
        ret.deleteParameter(FlowParam.experimentId.toString());
        return ret;
    }

    @Override
    public FlowSchema getSchema()
    {
        return (FlowSchema) super.getSchema();
    }

    @Override
    public boolean showDeleteButtonConfirmationText()
    {
        return !getSettings().getQueryName().equalsIgnoreCase(FlowTableType.Runs.name());
    }

    @Override
    public ActionButton createDeleteButton(boolean showConfirmation)
    {
        return createDeleteButton(showConfirmation, true);
    }

    protected ActionButton createDeleteButton(boolean showConfirmation, boolean useExpRunsURL)
    {
        // The Analysis "folder" table needs to use the getDeleteProtocolURL, the FCSRuns and FCSAnalysis 'views' need the getDeleteSelectedExpRunsURL
        if (useExpRunsURL)
            setDeleteURL(ExperimentUrls.get().getDeleteSelectedExpRunsURL(getContainer(), getReturnURL()).toContainerRelativeURL());
        return super.createDeleteButton(showConfirmation);
    }

    @Override
    protected void populateButtonBar(DataView view, ButtonBar bar)
    {
        if (showAnalysisFolderButton() && getSchema().getRun() == null /*&& getSchema().getScript() == null*/)
        {
            FlowExperiment[] experiments = FlowExperiment.getAnalysesAndWorkspace(getContainer());
            URLHelper target = urlChangeView();
            MenuButton button = new MenuButton("Analysis Folder");

            Map<Integer, String> availableExperiments = new LinkedHashMap<>();
            availableExperiments.put(0, "All Analysis Folders");

            for (FlowExperiment experiment : experiments)
                availableExperiments.put(experiment.getExperimentId(), experiment.getName());

            FlowExperiment current = getSchema().getExperiment();
            int currentId = current == null ? 0 : current.getExperimentId();

            if (!availableExperiments.containsKey(currentId))
                availableExperiments.put(current.getExperimentId(), current.getName());

            for (Map.Entry<Integer, String> entry : availableExperiments.entrySet())
            {
                URLHelper url = target.clone();
                if (entry.getKey().intValue() != 0)
                    url.replaceParameter(FlowParam.experimentId.name(), entry.getKey());
                button.addMenuItem(entry.getValue(), url, null, currentId == entry.getKey());
            }
            bar.add(button);
        }

        super.populateButtonBar(view, bar);

        // NOTE: Only add "Link to Study" to FCSAnlayses wells. This isn't a reliable way to check if the wells are FCSAnalysis wells.
        StudyPublishService aps = StudyPublishService.get();
        String queryName = getSettings().getQueryName();
        if (null != aps && queryName.equals(FlowTableType.FCSAnalyses.toString()))
        {
            // UNDONE: refactor ResultsQueryView create "Link to Study" button code so it can be re-used here
            FlowProtocol protocol = FlowProtocol.getForContainer(getContainer());
            if (protocol != null && !aps.getValidPublishTargets(getUser(), InsertPermission.class).isEmpty())
            {
                ExpProtocol expProtocol = protocol.getProtocol();
                ActionURL publishURL = PageFlowUtil.urlProvider(StudyUrls.class).getLinkToStudyURL(getContainer(), expProtocol);

                if (getTable().getContainerFilter() != null && getTable().getContainerFilter().getType() != null)
                    publishURL.addParameter("containerFilterName", getTable().getContainerFilter().getType().name());

                ActionButton publishButton = new ActionButton(publishURL,
                        "Link to Study", ActionButton.Action.POST);
                publishButton.setDisplayPermission(InsertPermission.class);
                publishButton.setRequiresSelection(true);

                bar.add(publishButton);
            }
        }

        if (queryName.equals(FlowTableType.FCSFiles.toString()))
        {
            ActionURL editWellsURL = new ActionURL(WellController.EditWellAction.class, getContainer());
            URLHelper returnURL = getReturnURL();
            editWellsURL.addReturnURL(returnURL);
            editWellsURL.addParameter("editWellReturnUrl", getReturnURL().toString());
            editWellsURL.addParameter("ff_isBulkEdit", true);
            editWellsURL.addParameter("isUpdate", false);

            ActionButton editKeyordsButton = new ActionButton("Edit Keywords");
            editKeyordsButton.setRequiresSelection(true, 1, null);
            editKeyordsButton.setURL(editWellsURL);
            editKeyordsButton.setActionType(ActionButton.Action.POST);
            editKeyordsButton.setDisplayPermission(UpdatePermission.class);
            bar.add(editKeyordsButton);
        }
    }

    @Override
    @NotNull
    public PanelButton createExportButton(@Nullable List<String> recordSelectorColumns)
    {
        PanelButton panelButton = super.createExportButton(recordSelectorColumns);

        User user = getUser();
        if (user != null && !user.isGuest() && getSettings() != null)
        {
            String queryName = getSettings().getQueryName();
            if (queryName.equals(FlowTableType.Runs.toString()))
            {
                ExportAnalysisForm form = new ExportAnalysisForm();
                form.setIncludeKeywords(false);
                form.setIncludeGraphs(true);
                form.setIncludeCompensation(true);
                form.setIncludeStatistics(true);
                form.setIncludeFCSFiles(false);
                form.setSelectionType("runs");
                HttpView analysisExportView = new JspView<>("/org/labkey/flow/view/exportAnalysis.jsp", form);
                panelButton.addSubPanel("Analysis", analysisExportView);
            }
            else if (queryName.equals(FlowTableType.FCSFiles.toString()) || queryName.equals(FlowTableType.FCSAnalyses.toString()) || queryName.equals(FlowTableType.CompensationControls.toString()))
            {
                ExportAnalysisForm form = new ExportAnalysisForm();
                form.setIncludeKeywords(true);
                form.setIncludeGraphs(false);
                form.setIncludeCompensation(false);
                form.setIncludeStatistics(false);
                form.setIncludeFCSFiles(true);
                form.setSelectionType("wells");
                HttpView analysisExportView = new JspView<>("/org/labkey/flow/view/exportAnalysis.jsp", form);
                panelButton.addSubPanel("Analysis", analysisExportView);
            }
        }

        return panelButton;
    }

    @Override
    public List<DisplayColumn> getDisplayColumns()
    {
        if (_displayColumns != null)
            return _displayColumns;
        _displayColumns = super.getDisplayColumns();
        __hasGraphs = false;
        FlowQuerySettings.ShowGraphs showGraphs = showGraphs();
        for (Iterator<DisplayColumn> it = _displayColumns.iterator(); it.hasNext();)
        {
            DisplayColumn dc = it.next();
            if (dc instanceof GraphColumn)
            {
                __hasGraphs = true;
                if (showGraphs == FlowQuerySettings.ShowGraphs.Thumbnail || showGraphs == FlowQuerySettings.ShowGraphs.Inline)
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

    @Override
    public NavTrailConfig getNavTrailConfig()
    {
        NavTrailConfig ntc = super.getNavTrailConfig();
        FlowSchema schema = getSchema();
        FlowRun run = schema.getRun();
        List<NavTree> children = new ArrayList<>();
        if (getContainer().getFolderType() instanceof FlowFolderType)
        {
            children.add(0, new NavTree("Flow Dashboard", PageFlowUtil.urlProvider(ProjectUrls.class).getStartURL(getContainer())));
        }
        else
        {
            children.add(0, new NavTree(FlowModule.getShortProductName(), new ActionURL(FlowController.BeginAction.class, getContainer())));
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
        return ntc;
    }

    @Override
    protected ColumnHeaderType getColumnHeaderType()
    {
        return ColumnHeaderType.FieldKey;
    }
}
