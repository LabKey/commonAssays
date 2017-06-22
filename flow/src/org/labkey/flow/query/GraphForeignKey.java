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
import org.labkey.api.data.Container;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.query.ExprColumn;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.SubsetSpec;
import org.labkey.flow.data.AttributeType;
import org.labkey.flow.persist.AttributeCache;
import org.labkey.flow.view.GraphColumn;

import java.util.Collection;

public class GraphForeignKey extends AttributeForeignKey<GraphSpec>
{
    FlowPropertySet _fps;

    public GraphForeignKey(Container c, FlowPropertySet fps)
    {
        super(c);
        _fps = fps;
    }

    @Override
    protected AttributeType type()
    {
        return AttributeType.graph;
    }

    protected Collection<AttributeCache.GraphEntry> getAttributes()
    {
        return _fps.getGraphProperties();
    }

    protected GraphSpec attributeFromString(String field)
    {
        try
        {
            return new GraphSpec(field);
        }
        catch (IllegalArgumentException e)
        {
            return null;
        }
    }

    protected void initColumn(final GraphSpec spec, String preferredName, ColumnInfo column)
    {
        column.setSqlTypeName("VARCHAR");
        SubsetSpec subset = _fps.simplifySubset(spec.getSubset());
        GraphSpec captionSpec = new GraphSpec(subset, spec.getParameters());
        column.setLabel(captionSpec.toString());
        column.setDisplayColumnFactory(GraphColumn::new);
        column.setMeasure(false);
        column.setDimension(false);
    }

    // Select the string concatenated value of objectId+'~~~'+graphSpec
    // When rendering the image URL, we will split the values apart again.
    protected SQLFragment sqlValue(ColumnInfo objectIdColumn, GraphSpec attrName, int attrId)
    {
        final SqlDialect dialect = objectIdColumn.getSqlDialect();
        final SQLFragment sepAndGraphSpec =
            dialect.concatenate(new SQLFragment("'" + GraphColumn.SEP + "'"), new SQLFragment("?").add(attrName.toString()));

        SQLFragment objectIdSql = objectIdColumn.getValueSql(ExprColumn.STR_TABLE_ALIAS);
        if (dialect.isSqlServer())
        {
            objectIdSql = new SQLFragment("CAST(").append(objectIdSql).append(" AS NVARCHAR(100))");
        }

        SQLFragment sql = new SQLFragment("(SELECT CASE WHEN COUNT(flow.Graph.ObjectId) = 1");
        sql.append("\nTHEN ");
        sql.append(dialect.concatenate(objectIdSql, sepAndGraphSpec));
        sql.append("\nELSE ");
        sql.append(sepAndGraphSpec);
        sql.append(" END");
        sql.append("\nFROM flow.Graph WHERE flow.Graph.GraphId = ");
        sql.append(attrId);
        sql.append("\nAND flow.Graph.ObjectId = ");
        sql.append(objectIdColumn.getValueSql(ExprColumn.STR_TABLE_ALIAS));
        sql.append(")");

        return sql;
    }
}
