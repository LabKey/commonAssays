package org.labkey.flow.webparts;

import org.labkey.api.data.DataColumn;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.RenderContext;
import org.labkey.flow.data.FlowScript;
import org.labkey.flow.data.FlowProtocolStep;

public class AnalysisScriptTypeColumn extends DataColumn
{
    public AnalysisScriptTypeColumn(ColumnInfo column)
    {
        super(column);
    }

    public boolean isSortable()
    {
        return false;
    }

    public boolean isFilterable()
    {
        return false;
    }

    public String getFormattedValue(RenderContext ctx)
    {
        Object value = getBoundColumn().getValue(ctx);
        if (!(value instanceof Number))
        {
            return "#ERROR#";
        }
        int id = ((Number) value).intValue();
        FlowScript script = FlowScript.fromScriptId(id);
        if (script == null)
        {
            return "#NOT FOUND#";
        }
        String ret = "";
        String and = "";
        if (script.hasStep(FlowProtocolStep.calculateCompensation))
        {
            ret += and;
            ret += "Compensation";
            and = " and ";
        }
        if (script.hasStep(FlowProtocolStep.analysis))
        {
            ret += and;
            ret += "Analysis";
            and = " and ";
        }
        return ret;
    }
}
