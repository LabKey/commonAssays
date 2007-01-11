package org.fhcrc.cpas.flow.query;

import org.fhcrc.cpas.data.*;
import org.fhcrc.cpas.query.api.AbstractMethodInfo;

import java.sql.Types;

public class KeywordMethod extends AbstractMethodInfo
{
    ColumnInfo _objectIdColumn;
    public KeywordMethod(ColumnInfo objectIdColumn)
    {
        super(Types.VARCHAR);
        _objectIdColumn = objectIdColumn;
    }

    public SQLFragment getSQL(DbSchema schema, SQLFragment[] arguments)
    {
        if (arguments.length != 1)
        {
            throw new IllegalArgumentException("The keyword method requires 1 argument");
        }
        SQLFragment ret = new SQLFragment("(SELECT flow.keyword.value FROM flow.keyword" +
                "\nINNER JOIN flow.attribute ON flow.statistic.keywordid = flow.attribute.rowid AND flow.attribute.name = ");
        ret.append(arguments[0]);
        ret.append("\nWHERE flow.keyword.objectId = ");
        ret.append(_objectIdColumn.getValueSql());
        ret.append(")");
        return ret;
    }
}
