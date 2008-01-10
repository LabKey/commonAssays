package org.labkey.flow.query;

import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.view.FlowQueryView;
import org.labkey.api.view.ActionURL;
import org.labkey.api.query.QueryAction;
import org.labkey.api.query.QueryService;
import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.CompareType;
import org.labkey.api.util.PageFlowUtil;

import java.util.Map;

public enum FlowTableType
{
    Runs("The flow 'Runs' table shows experiment runs in the three steps of analysis: read Keywords, calculate Compensation, and perform Analysis.", false),
    CompensationMatrices("The 'CompensationMatrices' table shows compensation matrices and their spill values.", false),
    FCSFiles("The 'FCSFiles' table shows FCS files and their keywords", false),
    FCSAnalyses("The 'FCSAnalyses' table shows statistics and graphs of FCS files", false),
    CompensationControls("The 'CompensationControls' table shows statistics and graphs of FCS files that were used to calculate a compensaton matrix.", false),
    AnalysisScripts("An analysis script contains the rules for calculating the compensation matrix for a run, as well as gates to apply, statistics to calculate, and graphs to draw.", true),
    Analyses("When a flow runs are analyzed, the results are grouped in an analysis.", true),
    Statistics("The 'Statistics' table shows the names of all statistics", true),
    Keywords("The 'Keywords' table show the names of all keywords", true),
    ;
    final String _description;
    final boolean _hidden;
    FlowTableType(String description, boolean hidden)
    {
        _description = description;
        _hidden = hidden;
    }

    public boolean isHidden()
    {
        return _hidden;
    }

    public String getDescription()
    {
        return _description;
    }

    public ActionURL urlFor(Container container, QueryAction action)
    {
        return QueryService.get().urlFor(container, action, FlowSchema.SCHEMANAME, toString());
    }

    public ActionURL urlFor(Container container, SimpleFilter filter)
    {
        return urlFor(container, filter, null);
    }

    public ActionURL urlFor(Container container, SimpleFilter filter, Sort sort)
    {
        ActionURL ret = urlFor(container, QueryAction.executeQuery);
        if (filter != null)
        {
            String strQuery = filter.toQueryString(FlowQueryView.DATAREGIONNAME_DEFAULT);
            for (Map.Entry<String, String> entry : PageFlowUtil.fromQueryString(strQuery))
            {
                ret.addParameter(entry.getKey(), entry.getValue());
            }
        }
        if (sort != null)
        {
            ret.addParameter(FlowQueryView.DATAREGIONNAME_DEFAULT + ".sort", sort.getSortParamValue());
        }
        return ret;
    }

    public ActionURL urlFor(Container container, String colName, Object value)
    {
        SimpleFilter filter = new SimpleFilter(colName, value);
        return urlFor(container, filter);
    }

    public ActionURL urlFor(Container container, String colName, CompareType compare, Object value)
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(colName, value, compare);
        return urlFor(container, filter);
    }
}
