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

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FieldKey;
import org.labkey.flow.analysis.web.SubsetSpec;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.analysis.model.ScriptSettings;
import org.labkey.flow.persist.ObjectType;
import org.labkey.flow.data.ICSMetadata;

import java.util.*;

public class BackgroundForeignKey extends AttributeForeignKey<StatisticSpec>
{
    FlowSchema _schema;
    FlowPropertySet _fps;

    public BackgroundForeignKey(FlowSchema schema, FlowPropertySet fps)
    {
        super();
        _schema = schema;
        _fps = fps;
    }

    protected Collection<StatisticSpec> getAttributes()
    {
        Set<StatisticSpec> all = _fps.getStatistics().keySet();
        Set<StatisticSpec> pct = new TreeSet<StatisticSpec>();
        for (StatisticSpec s : all)
        {
            if (s.getStatistic() == StatisticSpec.STAT.Freq_Of_Parent)
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
        
        SQLFragment ret = new SQLFragment(
                "(SELECT AVG(flow.Statistic.Value)\n" +
                "FROM flow.Statistic INNER JOIN " + junctionTable + " J ON flow.Statistic.ObjectId = J.bg\n" +
                "WHERE J.fg = " + objectIdColumn.getValueSql() + " AND flow.Statistic.StatisticId = " + attrId + ")");
        return ret;
    }
}