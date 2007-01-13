package org.fhcrc.cpas.flow.query;

import org.fhcrc.cpas.flow.util.PFUtil;
import org.fhcrc.cpas.flow.query.FlowSchema;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.query.QueryAction;
import org.labkey.api.query.QueryDefinition;
import org.labkey.api.data.Container;

public enum FlowTableType
{
    Runs("Used whenever a list of runs is displayed."),
    CompensationMatrices("Shows compensation matrix values"),
    FCSFiles("Shows FCS data files"),
    FCSAnalyses("Shows analyses of FCS Files"),
    CompensationControls("Shows statistics of compensation controls"),
    AnalysisScripts("Shows analysis scripts")
    ;
    final String _description;
    FlowTableType(String description)
    {
        _description = description;
    }

    public boolean isHidden()
    {
        return name().startsWith("_");
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
}
