/*
 * Copyright (c) 2006-2016 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.*;
import org.labkey.api.query.AliasManager;
import org.labkey.flow.data.AttributeType;
import org.labkey.flow.persist.AttributeCache;
import org.labkey.flow.persist.FlowManager;
import org.labkey.api.util.StringExpression;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;

import java.util.Collection;

abstract public class AttributeForeignKey<T extends Comparable<T>> extends AbstractForeignKey
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
        VirtualTable ret = new VirtualTable(FlowManager.get().getSchema(), null)
        {
            protected boolean isCaseSensitive()
            {
                return true;
            }
        };

        AliasManager am = new AliasManager(ret.getSchema());
        for (AttributeCache.Entry<T, ? extends AttributeCache.Entry> entry : getAttributes())
        {
            T attrName = entry.getAttribute();
            AttributeCache.Entry preferred = entry.getAliasedEntry();

            ColumnInfo column = new ColumnInfo(new FieldKey(null, attrName.toString()), ret);
            String alias = am.decideAlias(StringUtils.defaultString(preferred==null?null:preferred.getName(), attrName.toString()));
            column.setAlias(alias);
            initColumn(attrName, preferred, column);
            ret.addColumn(column);
        }
        return ret;
    }

    private void initColumn(T attrName, AttributeCache.Entry preferred, ColumnInfo column)
    {
        initColumn(attrName, preferred != null ? preferred.getName() : null, column);

        if (preferred != null)
        {
            column.setDescription("Alias for '" + preferred.getName() + "'");
            column.setHidden(true);
        }
    }

    public ColumnInfo createLookupColumn(ColumnInfo parent, String displayField)
    {
        if (displayField == null)
            return null;

        T attr = attributeFromString(displayField);
        if (attr == null)
            return null;

        AttributeCache cache = AttributeCache.forType(type());
        AttributeCache.Entry entry = cache.byAttribute(_container, attr);
        AttributeCache.Entry preferred = entry == null ? null : entry.getAliasedEntry();
        int rowId = entry == null ? 0 : entry.getRowId();

        SQLFragment sql = sqlValue(parent, attr, preferred != null ? preferred.getRowId() : rowId);
        ExprColumn ret = new ExprColumn(parent.getParentTable(), new FieldKey(parent.getFieldKey(), displayField), sql, JdbcType.NULL, parent);
        initColumn(attr, preferred, ret);

        return ret;
    }

    abstract protected AttributeType type();
    abstract protected Collection<? extends AttributeCache.Entry<T, ? extends AttributeCache.Entry>> getAttributes();
    abstract protected SQLFragment sqlValue(ColumnInfo objectIdColumn, T attrName, int attrId);
    abstract protected void initColumn(T attrName, String preferredName, ColumnInfo column);
    abstract protected T attributeFromString(String field);
}
