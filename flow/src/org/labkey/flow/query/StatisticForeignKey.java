package org.labkey.flow.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.SQLFragment;
import org.labkey.flow.analysis.web.SubsetSpec;
import org.labkey.flow.analysis.web.StatisticSpec;

import java.util.Collection;

public class StatisticForeignKey extends AttributeForeignKey<StatisticSpec>
{
    FlowPropertySet _fps;
    public StatisticForeignKey(FlowPropertySet fps)
    {
        super();
        _fps = fps;
    }

    protected Collection<StatisticSpec> getAttributes()
    {
        return _fps.getStatistics().keySet();
    }

    protected StatisticSpec attributeFromString(String field)
    {
        try
        {
            return new StatisticSpec(field);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    protected void initColumn(StatisticSpec stat, ColumnInfo column)
    {
        SubsetSpec subset = _fps.simplifySubset(stat.getSubset());
        stat = new StatisticSpec(subset, stat.getStatistic(), stat.getParameter());
        column.setCaption(stat.toShortString());
        column.setSqlTypeName("DOUBLE");
        column.setFormatString("#,##0.###");
    }

    protected SQLFragment sqlValue(ColumnInfo objectIdColumn, StatisticSpec attrName, int attrId)
    {
        SQLFragment ret = new SQLFragment("(SELECT flow.Statistic.Value FROM flow.Statistic WHERE flow.Statistic.ObjectId = ");
        ret.append(objectIdColumn.getValueSql());
        ret.append(" AND flow.Statistic.StatisticId = ");
        ret.append(attrId);
        ret.append(")");
        return ret;
    }
}
