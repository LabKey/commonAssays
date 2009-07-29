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
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.ExprColumn;
import org.labkey.flow.analysis.web.SubsetSpec;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.analysis.model.ScriptSettings;
import org.labkey.flow.persist.ObjectType;
import org.labkey.flow.data.ICSMetadata;
import org.labkey.flow.data.FlowDataType;

import java.util.*;

public class BackgroundForeignKey extends AttributeForeignKey<StatisticSpec>
{
    FlowSchema _schema;
    FlowPropertySet _fps;
    FlowDataType _type;

    public BackgroundForeignKey(FlowSchema schema, FlowPropertySet fps, FlowDataType type)
    {
        super();
        _schema = schema;
        _fps = fps;
        _type = type;
    }

    protected Collection<StatisticSpec> getAttributes()
    {
        Set<StatisticSpec> all = _fps.getStatistics().keySet();
        Set<StatisticSpec> pct = new TreeSet<StatisticSpec>();
        for (StatisticSpec s : all)
        {
            pct.add(s);
        }
        return pct;
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
        // Hide spill stats be default for all tables except CompensationMatrix.
        // Hide non-spill stats from the CompensationMatrix table.
        if (_type == FlowDataType.CompensationMatrix)
            column.setIsHidden(stat.getStatistic() != StatisticSpec.STAT.Spill);
        else
            column.setIsHidden(stat.getStatistic() == StatisticSpec.STAT.Spill);
        column.setCaption("BG " + stat.toShortString());
        column.setSqlTypeName("DOUBLE");
        column.setFormatString("#,##0.###");
    }

    protected SQLFragment sqlValue(ColumnInfo objectIdColumn, StatisticSpec attrName, int attrId)
    {
        ICSMetadata ics = _schema.getProtocol().getICSMetadata();
        if (ics == null)
            return new SQLFragment("NULL");

        String junctionTable = _schema.getBackgroundJunctionTableName(_schema.getContainer());
        if (null == junctionTable)
            return new SQLFragment("NULL");
        
        SQLFragment ret = new SQLFragment(
                "(SELECT AVG(flow.Statistic.Value)\n" +
                "FROM flow.Statistic INNER JOIN " + junctionTable + " J ON flow.Statistic.ObjectId = J.bg\n" +
                "WHERE J.fg = " + objectIdColumn.getValueSql(ExprColumn.STR_TABLE_ALIAS) + " AND flow.Statistic.StatisticId = " + attrId + ")");
        return ret;
    }
}