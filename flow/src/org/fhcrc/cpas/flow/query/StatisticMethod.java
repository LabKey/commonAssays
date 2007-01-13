package org.fhcrc.cpas.flow.query;

import org.labkey.api.data.*;
import org.labkey.api.query.AbstractMethodInfo;

import java.sql.Types;

public class StatisticMethod extends AbstractMethodInfo
{
    ColumnInfo _objectIdColumn;
    public StatisticMethod(ColumnInfo objectIdColumn)
    {
        super(Types.DOUBLE);
        _objectIdColumn = objectIdColumn;
    }

    public ColumnInfo createColumnInfo(TableInfo parentTable, ColumnInfo[] arguments, String alias)
    {
        ColumnInfo ret = super.createColumnInfo(parentTable, arguments, alias);
        ret.setFormatString("#,##0.###");
        return ret;
    }

    public SQLFragment getSQL(DbSchema schema, SQLFragment[] arguments)
    {
        if (arguments.length != 1)
        {
            throw new IllegalArgumentException("The statistic method requires 1 argument");
        }
        SQLFragment ret = new SQLFragment("(SELECT flow.statistic.value FROM flow.statistic" +
                "\nINNER JOIN flow.attribute ON flow.statistic.statisticid = flow.attribute.rowid AND flow.attribute.name = ");
        ret.append(arguments[0]);
        ret.append("\nWHERE flow.statistic.objectId = ");
        ret.append(_objectIdColumn.getValueSql());
        ret.append(")");
        return ret;
    }
}
