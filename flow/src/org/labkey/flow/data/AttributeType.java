/*
 * Copyright (c) 2006-2014 LabKey Corporation
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

package org.labkey.flow.data;

import org.labkey.api.data.TableInfo;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.persist.FlowManager;

public enum AttributeType
{
    keyword
            {
                @Override
                public Object createAttribute(String name)
                {
                    return name;
                }

                @Override
                public TableInfo getAttributeTable()
                {
                    return FlowManager.get().getTinfoKeywordAttr();
                }

                @Override
                public TableInfo getValueTable()
                {
                    return FlowManager.get().getTinfoKeyword();
                }

                @Override
                public String getValueTableAttributeIdColumn()
                {
                    return "keywordid";
                }

                @Override
                public String getValueTableOriginalAttributeIdColumn()
                {
                    return "originalkeywordid";
                }
            },

    statistic
            {
                @Override
                public Object createAttribute(String name)
                {
                    return new StatisticSpec(name);
                }

                @Override
                public TableInfo getAttributeTable()
                {
                    return FlowManager.get().getTinfoStatisticAttr();
                }

                @Override
                public TableInfo getValueTable()
                {
                    return FlowManager.get().getTinfoStatistic();
                }

                @Override
                public String getValueTableAttributeIdColumn()
                {
                    return "statisticid";
                }

                @Override
                public String getValueTableOriginalAttributeIdColumn()
                {
                    return "originalstatisticid";
                }
            },
    graph
            {
                @Override
                public Object createAttribute(String name)
                {
                    return new GraphSpec(name);
                }

                @Override
                public TableInfo getAttributeTable()
                {
                    return FlowManager.get().getTinfoGraphAttr();
                }

                @Override
                public TableInfo getValueTable()
                {
                    return FlowManager.get().getTinfoGraph();
                }

                @Override
                public String getValueTableAttributeIdColumn()
                {
                    return "graphid";
                }

                @Override
                public String getValueTableOriginalAttributeIdColumn()
                {
                    return "originalgraphid";
                }
            };

    /** Created a parsed representation of the attribute. */
    public abstract Object createAttribute(String name);

    /** The attribute table where the keyword, statistic, graph name and alias information is stored. */
    public abstract TableInfo getAttributeTable();

    /** The value table where the keyword value, statistic value, or graph bytes are stored. */
    public abstract TableInfo getValueTable();

    /** The column name of attribute id column on the value table. */
    public abstract String getValueTableAttributeIdColumn();

    /** The column name of original attribute id column on the value table. */
    public abstract String getValueTableOriginalAttributeIdColumn();

    public static AttributeType fromClass(Class c)
    {
        if (c == String.class)
            return keyword;
        if (c == StatisticSpec.class)
            return statistic;
        if (c == GraphSpec.class)
            return graph;
        throw new IllegalArgumentException();
    }

}
