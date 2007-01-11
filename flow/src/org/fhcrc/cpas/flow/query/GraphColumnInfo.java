/*package org.fhcrc.cpas.flow.query;

import org.fhcrc.cpas.query.api.WrappedColumn;
import org.fhcrc.cpas.query.api.QueryColumn;
import org.fhcrc.cpas.query.api.FieldKey;
import org.fhcrc.cpas.data.SQLFragment;
import org.fhcrc.cpas.exp.PropertyType;
import org.fhcrc.cpas.flow.data.GraphProperty;
import org.fhcrc.cpas.flow.view.GraphColumn;
import com.labkey.flow.web.GraphSpec;

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