/*
 * Copyright (c) 2006-2011 LabKey Corporation
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
import org.labkey.api.data.Container;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.query.ExprColumn;
import org.labkey.flow.analysis.web.SubsetSpec;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.data.AttributeType;
import org.labkey.flow.data.FlowDataType;
import org.labkey.flow.persist.AttributeCache;

import java.util.Collection;

public class StatisticForeignKey extends AttributeForeignKey<StatisticSpec>
{
    FlowPropertySet _fps;
    FlowDataType _type;

    public StatisticForeignKey(Container c, FlowPropertySet fps, FlowDataType type)
    {
        super(c);
        _fps = fps;
        _type = type;
    }

    @Override
    protected AttributeType type()
    {
        return AttributeType.statistic;
    }

    protected Collection<AttributeCache.StatisticEntry> getAttributes()
    {
        return _fps.getStatistics();
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

    protected void initColumn(StatisticSpec stat, String preferredName, ColumnInfo column)
    {
        SubsetSpec subset = _fps.simplifySubset(stat.getSubset());
        stat = new StatisticSpec(subset, stat.getStatistic(), stat.getParameter());

        // Hide spill stats by default for all tables except CompensationMatrix.
        // Hide non-spill stats from the CompensationMatrix table.
        if (_type == FlowDataType.CompensationMatrix)
            column.setHidden(stat.getStatistic() != StatisticSpec.STAT.Spill);
        else
            column.setHidden(stat.getStatistic() == StatisticSpec.STAT.Spill);
        column.setLabel(stat.toShortString());
        column.setSqlTypeName("DOUBLE");
        column.setFormat("#,##0.###");
    }

    protected SQLFragment sqlValue(ColumnInfo objectIdColumn, StatisticSpec attrName, int attrId)
    {
        SQLFragment ret = new SQLFragment("(SELECT flow.Statistic.Value FROM flow.Statistic WHERE flow.Statistic.ObjectId = ");
        ret.append(objectIdColumn.getValueSql(ExprColumn.STR_TABLE_ALIAS));
        ret.append(" AND flow.Statistic.StatisticId = ");
        ret.append(attrId);
        ret.append(")");

        //SQLFragment ret = new SQLFragment("(SELECT flow.Statistic.Value FROM flow.Statistic, flow.StatisticAttr WHERE flow.Statistic.ObjectId = ");
        //ret.append(objectIdColumn.getValueSql(ExprColumn.STR_TABLE_ALIAS));
        //ret.append(" AND flow.Statistic.StatisticId = flow.StatisticAttr.Id AND flow.StatisticAttr.Name = ?");
        //ret.add(attrName);
        //ret.append(")");

        return ret;
    }
}
