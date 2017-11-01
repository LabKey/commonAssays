/*
 * Copyright (c) 2006-2017 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.snapshot.AbstractTableMethodInfo;

public class StatisticMethod extends AbstractTableMethodInfo
{
    Container _container;
    ColumnInfo _objectIdColumn;

    public StatisticMethod(@NotNull Container c, ColumnInfo objectIdColumn)
    {
        super(JdbcType.DOUBLE);
        _container = c;
        _objectIdColumn = objectIdColumn;
    }

    public ColumnInfo createColumnInfo(TableInfo parentTable, ColumnInfo[] arguments, String alias)
    {
        ColumnInfo ret = super.createColumnInfo(parentTable, arguments, alias);
        ret.setFormat("#,##0.###");
        return ret;
    }

    public SQLFragment getSQL(String tableAlias, DbSchema schema, SQLFragment[] arguments)
    {
        if (arguments.length != 1)
        {
            throw new IllegalArgumentException("The statistic method requires 1 argument");
        }
        SQLFragment ret = new SQLFragment("(SELECT flow.statistic.value FROM flow.statistic");
        ret.append("\nINNER JOIN flow.StatisticAttr ON flow.statistic.statisticid = flow.StatisticAttr.id AND flow.StatisticAttr.name = ");
        ret.append(arguments[0]);
        // The objectId column will be aliased as "Statistic" when used in the FCSAnalysis table and as "Value" when used in the CompensationMatrices table
        ret.append("\nWHERE flow.statistic.objectId = ").append(tableAlias).append(".").append(_objectIdColumn.getColumnName());
        ret.append(" AND flow.StatisticAttr.container = ?");
        ret.add(_container.getId());
        ret.append(")");
        return ret;
    }
}
