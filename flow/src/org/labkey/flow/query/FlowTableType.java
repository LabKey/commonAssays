package org.labkey.flow.query;

import org.labkey.flow.util.PFUtil;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.view.FlowQueryView;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.query.QueryAction;
import org.labkey.api.query.QueryDefinition;
import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.util.PageFlowUtil;

import java.util.Map;
import java.util.Set;

public enum FlowTableType
{
    Runs("Used whenever a list of runs is displayed.", false),
    CompensationMatrices("Shows compensation matrix values", false),
    FCSFiles("Shows FCS data files", false),
    FCSAnalyses("Shows analyses of FCS Files", false),
    CompensationControls("Shows statistics of compensation controls", false),
    AnalysisScripts("Shows analysis scripts", true),
    Analyses("Shows analyses", true),
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

    public ViewURLHelper urlFor(Container container, QueryAction action)
    {
        FlowSchema schema = new FlowSchema(null, container);
        QueryDefinition queryDef = schema.getQueryDefForTable(name());
        return schema.urlFor(action, queryDef);
    }

    public ViewURLHelper urlFor(Container container, SimpleFilter filter)
    {
        ViewURLHelper ret = urlFor(container, QueryAction.executeQuery);
        String strQuery = filter.toQueryString(FlowQueryView.DATAREGIONNAME_DEFAULT);
        for (Map.Entry<String, String> entry : PageFlowUtil.fromQueryString(strQuery))
        {
            ret.addParameter(entry.getKey(), entry.getValue());
        }
        return ret;
    }

    public ViewURLHelper urlFor(Container container, String colName, Object value)
    {
        SimpleFilter filter = new SimpleFilter(colName, value);
        return urlFor(container, filter);
    }
}
