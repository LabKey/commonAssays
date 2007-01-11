package org.fhcrc.cpas.flow.query;

import org.fhcrc.cpas.query.api.ExprColumn;
import org.fhcrc.cpas.data.SQLFragment;
import org.fhcrc.cpas.data.ColumnInfo;
import org.fhcrc.cpas.data.ForeignKey;
import org.fhcrc.cpas.data.TableInfo;
import org.fhcrc.cpas.util.StringExpressionFactory;
import org.fhcrc.cpas.flow.view.GraphColumn;

import com.labkey.flow.web.GraphSpec;
import com.labkey.flow.web.SubsetSpec;

import java.sql.Types;

public class GraphForeignKey extends AttributeForeignKey<GraphSpec>
{
    FlowPropertySet _fps;
    public GraphForeignKey(FlowPropertySet fps)
    {
        super(fps.getGraphProperties().keySet());
        _fps = fps;
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
        column.setFk(new ForeignKey() {
            public ColumnInfo createLookupColumn(ColumnInfo parent, String displayField)
            {
                if (displayField != null)
                    return null;
                SQLFragment sqlExpr = new SQLFragment();
                sqlExpr.appendStringLiteral(spec.toString());
                ColumnInfo ret = new ExprColumn(parent.getParentTable(), parent.getName() + "$", sqlExpr, Types.VARCHAR);
                ret.setAlias(parent.getAlias() + "$");
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
        column.setRenderClass(GraphColumn.class);
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
