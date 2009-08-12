/*
 * Copyright (c) 2006-2009 LabKey Corporation
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
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.snapshot.AbstractTableMethodInfo;
import org.labkey.flow.data.ICSMetadata;

import java.sql.Types;

public class BackgroundMethod extends AbstractTableMethodInfo
{
    FlowSchema _schema;
    ColumnInfo _objectIdColumn;

    public BackgroundMethod(FlowSchema schema, ColumnInfo objectIdColumn)
    {
        super(Types.DOUBLE);
        _schema = schema;
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
            throw new IllegalArgumentException("The statistic method requires 1 argument");

        ICSMetadata ics = _schema.getProtocol().getICSMetadata();
        if (ics == null)
            return new SQLFragment("NULL");

        String junctionTable = _schema.getBackgroundJunctionTableName(_schema.getContainer());
        if (junctionTable == null)
            return new SQLFragment("NULL");
        
        SQLFragment ret = new SQLFragment(
                "(SELECT AVG(flow.Statistic.Value) " +
                "FROM flow.Statistic INNER JOIN " + junctionTable + " J ON flow.Statistic.ObjectId = J.bg " +
                "INNER JOIN flow.attribute ON flow.statistic.statisticid = flow.attribute.rowid AND flow.attribute.name = " + arguments[0].getSQL() + " " +
                "WHERE J.fg = " + tableAlias + ".Background)"); 
                        // + _objectIdColumn.getValueSql(tableAlias) + ")");
        return ret;
    }
}
