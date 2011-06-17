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

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.*;
import org.labkey.flow.data.AttributeType;
import org.labkey.flow.persist.FlowManager;
import org.labkey.api.util.StringExpression;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;

import java.util.Map;

abstract public class AttributeForeignKey<T> extends AbstractForeignKey
{
    public StringExpression getURL(ColumnInfo parent)
    {
        return null;
    }

    protected Container _container;

    public AttributeForeignKey(@NotNull Container c)
    {
        _container = c;
        assert _container != null;
    }

    public TableInfo getLookupTableInfo()
    {
        VirtualTable ret = new VirtualTable(FlowManager.get().getSchema())
        {
            protected boolean isCaseSensitive()
            {
                return true;
            }
        };

        for (Map.Entry<T, Integer> attr : getAttributes().entrySet())
        {
            T attrName = attr.getKey();
            Integer attrId = attr.getValue();

            FlowManager.FlowEntry preferred = FlowManager.get().getAliased(type(), attrId);
            ColumnInfo column = new ColumnInfo(new FieldKey(null, attrName.toString()), ret);
            initColumn(attrName, preferred != null ? preferred._name : null, column);
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

        int attrId = FlowManager.get().getAttributeId(_container, type(), attrName.toString());
        FlowManager.FlowEntry preferred = FlowManager.get().getAliased(type(), attrId);

        SQLFragment sql = sqlValue(parent, attrName, preferred != null ? preferred._rowId : attrId);
        ExprColumn ret = new ExprColumn(parent.getParentTable(), new FieldKey(parent.getFieldKey(), displayField), sql, JdbcType.NULL, parent);
        initColumn(attrName, preferred != null ? preferred._name : null, ret);

        return ret;
    }

    abstract protected AttributeType type();
    abstract protected Map<T, Integer> getAttributes();
    abstract protected SQLFragment sqlValue(ColumnInfo objectIdColumn, T attrName, int attrId);
    abstract protected void initColumn(T attrName, String preferredName, ColumnInfo column);
    abstract protected T attributeFromString(String field);
}
