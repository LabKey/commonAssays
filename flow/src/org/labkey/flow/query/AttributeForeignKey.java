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

import org.labkey.api.data.*;
import org.labkey.flow.persist.FlowManager;
import org.labkey.api.util.StringExpression;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;

import java.util.Collection;

abstract public class AttributeForeignKey<T> extends AbstractForeignKey
{
    public StringExpression getURL(ColumnInfo parent)
    {
        return null;
    }

    public AttributeForeignKey()
    {
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

        for (T attrName : getAttributes())
        {
            ColumnInfo column = new ColumnInfo(new FieldKey(null,attrName.toString()), ret);
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
        ExprColumn ret = new ExprColumn(parent.getParentTable(), new FieldKey(parent.getFieldKey(), displayField), sql, JdbcType.NULL, parent);
        initColumn(attrName, ret);
        return ret;
    }

    abstract protected Collection<T> getAttributes();
    abstract protected SQLFragment sqlValue(ColumnInfo objectIdColumn, T attrName, int attrId);
    abstract protected void initColumn(T attrName, ColumnInfo column);
    abstract protected T attributeFromString(String field);
}
