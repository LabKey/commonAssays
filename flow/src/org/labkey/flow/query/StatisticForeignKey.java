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
