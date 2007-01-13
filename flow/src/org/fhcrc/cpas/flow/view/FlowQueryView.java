package org.fhcrc.cpas.flow.view;

import org.labkey.api.view.*;
import org.fhcrc.cpas.flow.data.*;
import org.fhcrc.cpas.flow.query.FlowSchema;
import org.fhcrc.cpas.flow.query.FlowQueryForm;
import org.fhcrc.cpas.flow.query.FlowQuerySettings;
import org.fhcrc.cpas.flow.util.PFUtil;
import org.labkey.api.util.PageFlowUtil;
import org.fhcrc.cpas.util.Pair;
import org.labkey.api.data.*;
import org.labkey.api.query.*;
import org.labkey.api.security.User;
import org.labkey.api.security.ACL;
import org.labkey.api.jsp.JspLoader;
import org.labkey.api.exp.api.ExpRunTable;
import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.util.*;

import Flow.FlowParam;
import Flow.FlowController;
import Flow.FlowModule;

public class FlowQueryView extends QueryView
{
    List<DisplayColumn> _displayColumns;
    boolean __hasGraphs;
    public FlowQueryView(FlowQueryForm form)
    {
        this(form.getContext(), (FlowSchema) form.getSchema(), (FlowQuerySettings) form.getQuerySettings());
    }

    public FlowQueryView(ViewContext context, FlowSchema schema, FlowQuerySettings settings)
    {
        super(context, schema, settings);
    }

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

    protected DataView createDataView()
    {
        DataView ret = super.createDataView();
        if (hasGraphs() && showGraphs())
        {
            ret = new GraphView(ret);
        }
        return ret;
    }

    private static Logger _log = Logger.getLogger(FlowQueryView.class);

    protected User getUser()
    {
        return getViewContext().getUser();
    }

    protected void renderCustomizeLinks(PrintWriter out) throws Exception
    {
        super.renderCustomizeLinks(out);
        if (hasGraphs())
        {
            if (showGraphs())
            {
                ViewURLHelper urlHide = getViewContext().cloneViewURLHelper();
                urlHide.deleteParameter(param("showGraphs"));
                out.write(PageFlowUtil.buttonLink("Hide Graphs", urlHide));
                ((HttpView) HttpView.currentView()).include(
                        new JspView(JspLoader.createPage(getViewContext().getRequest(), FlowQueryView.class, "setGraphSize.jsp")), out);
            }
            else
            {
                ViewURLHelper urlShow = getViewContext().cloneViewURLHelper();
                urlShow.addParameter(param("showGraphs"), "true");
                out.write(PageFlowUtil.buttonLink("Show Graphs", urlShow));
                out.write("&nbsp;");
            }
        }
    }

    public FlowQuerySettings getSettings()
    {
        return (FlowQuerySettings) super.getSettings();
    }

    protected boolean showGraphs()
    {
        return getSettings().getShowGraphs();
    }

    protected ViewURLHelper urlChangeView()
    {
        ViewURLHelper ret = super.urlChangeView();
        ret.deleteParameter(FlowParam.experimentId.toString());
        return ret;
    }

    public FlowSchema getSchema()
    {
        return (FlowSchema) super.getSchema();
    }

    protected List<QueryPicker> getChangeViewPickers()
    {
        List<QueryPicker> ret = super.getChangeViewPickers();
        if (getSchema().getRun() == null)
        {
            FlowExperiment[] experiments = FlowExperiment.getAnalyses(getContainer());
            Map<Object, String> availableExperiments = new LinkedHashMap();
            availableExperiments.put(0, "");
            for (FlowExperiment experiment : experiments)
            {
                availableExperiments.put(experiment.getExperimentId(), experiment.getName());
            }
            FlowExperiment current = getSchema().getExperiment();
            int currentId = current == null ? 0 : current.getExperimentId();

            if (!availableExperiments.containsKey(currentId))
            {
                availableExperiments.put(current.getExperimentId(), current.getName());
            }
            ret.add(new QueryPicker("Analysis:", FlowParam.experimentId.toString(), currentId, availableExperiments));
        }
        return ret;
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
        List<Pair> children = new ArrayList();
        children.add(new Pair(FlowModule.getShortProductName(), PFUtil.urlFor(FlowController.Action.begin, getContainer())));
        if (run != null)
        {
            FlowExperiment experiment = run.getExperiment();
            if (experiment != null)
            {
                children.add(new Pair(experiment.getLabel(), experiment.urlShow()));
            }
            children.add(new Pair(run.getLabel(), run.urlShow()));
            ntc.setExtraChildren(children.toArray(new Pair[0]));
        }
        else if (schema.getExperiment() != null)
        {
            children.add(new Pair(schema.getExperiment().getLabel(), schema.getExperiment().urlShow()));
            ntc.setExtraChildren(children.toArray(new Pair[0]));
        }
        return ntc;
    }
}