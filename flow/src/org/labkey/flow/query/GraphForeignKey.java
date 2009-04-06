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

import org.labkey.api.query.ExprColumn;
import org.labkey.api.data.*;
import org.labkey.api.util.StringExpressionFactory;
import org.labkey.flow.view.GraphColumn;

import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.SubsetSpec;

import java.sql.Types;
import java.util.Collection;

public class GraphForeignKey extends AttributeForeignKey<GraphSpec>
{
    FlowPropertySet _fps;

    public GraphForeignKey(FlowPropertySet fps)
    {
        super();
        _fps = fps;
    }

    protected Collection<GraphSpec> getAttributes()
    {
        return _fps.getGraphProperties().keySet();
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

    protected void initColumn(final GraphSpec spec, ColumnInfo column)
    {
        column.setSqlTypeName("INTEGER");
        SubsetSpec subset = _fps.simplifySubset(spec.getSubset());
        GraphSpec captionSpec = new GraphSpec(subset, spec.getParameters());
        column.setCaption(captionSpec.toString());
        column.setFk(new AbstractForeignKey() {
            public ColumnInfo createLookupColumn(ColumnInfo parent, String displayField)
            {
                if (displayField != null)
                    return null;
                SQLFragment sqlExpr = new SQLFragment();
                sqlExpr.appendStringLiteral(spec.toString());
                ColumnInfo ret = new ExprColumn(parent.getParentTable(), parent.getName() + "$", sqlExpr, Types.VARCHAR);
                return ret;
            }

            public TableInfo getLookupTableInfo()
            {
                return null;
            }

            public StringExpressionFactory.StringExpression getURL(ColumnInfo parent)
            {
                return null;
            }
        });
        column.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new GraphColumn(colInfo);
            }
        });
    }

    protected SQLFragment sqlValue(ColumnInfo objectIdColumn, GraphSpec attrName, int attrId)
    {
        SQLFragment sql = new SQLFragment("(SELECT CASE WHEN flow.Graph.RowId IS NOT NULL THEN ");
        sql.append(objectIdColumn.getValueSql());
        sql.append(" END");
        sql.append("\nFROM flow.Graph WHERE flow.Graph.GraphId = ");
        sql.append(attrId);
        sql.append("\nAND flow.Graph.ObjectId = ");
        sql.append(objectIdColumn.getValueSql());
        sql.append(")");
        return sql;
    }
}
