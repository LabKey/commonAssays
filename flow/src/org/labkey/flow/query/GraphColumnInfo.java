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

/*package org.labkey.flow.query;

import org.labkey.api.query.WrappedColumn;
import org.labkey.api.query.QueryColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.exp.PropertyType;
import org.labkey.flow.data.GraphProperty;
import org.labkey.flow.view.GraphColumn;
import org.labkey.flow.analysis.web.GraphSpec;

/**
 * ColumnInfo with enough info render a graph.
 * The bound column is the objectId, and the display column value is null if the graph does not exist,
 * otherwise it is the name of the graph.
 */
/*
public class GraphColumnInfo extends WrappedColumn
{
    public GraphColumnInfo(FieldKey key, QueryColumn colObjectId, GraphSpec graphSpec)
    {
        super(key, colObjectId);
        setDisplayField(new GraphValueColumn(this, graphSpec));
        setRenderClass(GraphColumn.class);
    }

    static private class GraphValueColumn extends ExpPropertyColumn
    {
        private GraphSpec _graph;
        public GraphValueColumn(GraphColumnInfo graphColumn, GraphSpec graphSpec)
        {
            super(graphColumn, new FieldKey(graphColumn.getKey().toTableKey(), "graph"), PropertyType.STRING,
                    GraphProperty.of(graphSpec).getPropertyDescriptor(), null);
            _graph = graphSpec;
        }

        public String getValueColumn()
        {
            return "TypeTag";
        }

        public SQLFragment getValueSql()
        {
            SQLFragment ret = new SQLFragment("CASE WHEN (");
            ret.append(super.getValueSql());
            ret.append(") IS NULL THEN ");
            ret.appendStringLiteral(_graph.toString());
            ret.append(" END ");
            return ret;
        }
    }
}
*/