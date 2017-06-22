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

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.snapshot.AbstractTableMethodInfo;
import org.labkey.flow.data.ICSMetadata;

public class BackgroundMethod extends AbstractTableMethodInfo
{
    FlowSchema _schema;
    ColumnInfo _objectIdColumn;

    public BackgroundMethod(FlowSchema schema, ColumnInfo objectIdColumn)
    {
        super(JdbcType.DOUBLE);
        _schema = schema;
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
            throw new IllegalArgumentException("The statistic method requires 1 argument");

        ICSMetadata ics = _schema.getProtocol().getICSMetadata();
        if (ics == null)
            return new SQLFragment("NULL");

        SQLFragment junctionTable = _schema.getBackgroundJunctionFromSql("J",_schema.getContainer());
        if (junctionTable == null)
            return new SQLFragment("NULL");
        
        SQLFragment ret = new SQLFragment("(SELECT AVG(flow.Statistic.Value) ");
        ret.append("FROM flow.Statistic INNER JOIN ").append(junctionTable).append(" ON flow.Statistic.ObjectId = J.bg ");
        ret.append("INNER JOIN flow.StatisticAttr ON flow.statistic.statisticid = flow.StatisticAttr.id AND flow.StatisticAttr.name = ");
        ret.append(arguments[0]);
        ret.append("\nWHERE J.fg = " + tableAlias + ".Background");
        ret.append(" AND flow.StatisticAttr.container = ?");
        ret.add(_schema.getContainer().getId());
        ret.append(")");
                        // + _objectIdColumn.getValueSql(tableAlias) + ")");
        return ret;
    }
}
