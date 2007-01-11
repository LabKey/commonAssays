package org.fhcrc.cpas.flow.query;

import org.fhcrc.cpas.data.*;
import org.fhcrc.cpas.flow.persist.FlowManager;
import org.fhcrc.cpas.util.StringExpressionFactory;
import org.fhcrc.cpas.query.api.ExprColumn;
import org.fhcrc.cpas.query.api.FieldKey;
import org.fhcrc.cpas.query.api.TableKey;

import java.util.Collection;

abstract public class AttributeForeignKey<T> implements ForeignKey
{
    protected Collection<T> _attributes;

    public StringExpressionFactory.StringExpression getURL(ColumnInfo parent)
    {
        return null;
    }

    public AttributeForeignKey(Collection<T> attributes)
    {
        _attributes = attributes;
    }

    public TableInfo getLookupTableInfo()
    {
        VirtualTable ret = new VirtualTable(FlowManager.get().getSchema());
        for (T attrName : _attributes)
        {
            ColumnInfo column = new ColumnInfo(attrName.toString(), ret);
            initColumn(attrName, column);
            ret.addColumn(column);
        }
        return ret;
    }

    public ColumnInfo createLookupColumn(ColumnInfo parent, String displayField)
    {
        if (displayField == null)
            return null;

        T attrName = attributeFromString(displayField);
        if (attrName == null)
            return null;
        int attrId = FlowManager.get().getAttributeId(attrName.toString());
        SQLFragment sql = sqlValue(parent, attrName, attrId);
        ExprColumn ret = new ExprColumn(parent.getParentTable(), new FieldKey(TableKey.fromString(parent.getName()), displayField).toString(), sql, 0, parent);
        initColumn(attrName, ret);
        return ret;
    }

    abstract protected SQLFragment sqlValue(ColumnInfo objectIdColumn, T attrName, int attrId);
    abstract protected void initColumn(T attrName, ColumnInfo column);
    abstract protected T attributeFromString(String field);
}
