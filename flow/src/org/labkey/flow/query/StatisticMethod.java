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

package org.labkey.flow.query;

import org.labkey.api.data.*;
import org.labkey.api.query.AbstractMethodInfo;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.snapshot.AbstractTableMethodInfo;

import java.sql.Types;

public class StatisticMethod extends AbstractTableMethodInfo
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

    public SQLFragment getSQL(String tableAlias, DbSchema schema, SQLFragment[] arguments)
    {
        if (arguments.length != 1)
        {
            throw new IllegalArgumentException("The statistic method requires 1 argument");
        }
        SQLFragment ret = new SQLFragment("(SELECT flow.statistic.value FROM flow.statistic" +
                "\nINNER JOIN flow.attribute ON flow.statistic.statisticid = flow.attribute.rowid AND flow.attribute.name = ");
        ret.append(arguments[0]);
        ret.append("\nWHERE flow.statistic.objectId = " + tableAlias + ".Statistic");
//        ret.append(_objectIdColumn.getValueSql(tableAlias));
        ret.append(")");
        return ret;
    }
}
