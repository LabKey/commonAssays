/*
 * Copyright (c) 2007-2019 LabKey Corporation
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

package org.labkey.ms2;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.*;
import org.labkey.api.util.HtmlString;
import org.labkey.api.util.HtmlStringBuilder;
import org.labkey.api.view.ActionURL;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;

import java.util.*;

/**
 * User: jeckels
 * Date: Feb 15, 2006
 */
public class GroupNumberDisplayColumn extends DataColumn
{
    private String _groupNumber;
    private String _collectionId;
    private boolean _fromQuery;
    private Container _container;
    private ColumnInfo _collectionIdColumn;

    public GroupNumberDisplayColumn(ColumnInfo col, Container c)
    {
        super(col);
        _fromQuery = true;
        _container = c;
    }

    public GroupNumberDisplayColumn(ColumnInfo col, ActionURL url, String groupNumber, String collectionId)
    {
        super(col);
        setName("ProteinGroup");
        _groupNumber = groupNumber;
        _collectionId = collectionId;
        ActionURL actionURL = url.clone();
        actionURL.setAction(MS2Controller.ShowProteinGroupAction.class);
        setURL(actionURL.addParameter("groupNumber", "${" + _groupNumber + "}").addParameter("indistinguishableCollectionId", "${" + _collectionId + "}"));
        setLinkTarget("prot");
        setWidth("50");
    }

    @Override
    public Object getDisplayValue(RenderContext ctx)
    {
        return getFormattedHtml(ctx);
    }

    @Override
    public Class getDisplayValueClass()
    {
        return String.class;
    }

    @Override @NotNull
    public HtmlString getFormattedHtml(RenderContext ctx)
    {
        Map<String, Object> row = ctx.getRow();
        long collectionId;
        long groupNumber;
        if (_fromQuery)
        {
            if (_collectionIdColumn == null || getColumnInfo() == null)
            {
                StringBuilder sb = new StringBuilder();
                if (_collectionIdColumn == null)
                {
                    sb.append("Could not resolve IndistinguishableCollectionId column, please be sure that it is included in any custom queries. ");
                }
                if (getColumnInfo() == null)
                {
                    sb.append("Could not resolve RowId column, please be sure that it is included in any custom queries. ");
                }

                return HtmlString.of(sb);
            }
            else
            {
                if (row.get(_collectionIdColumn.getAlias()) == null)
                {
                    return HtmlString.EMPTY_STRING;
                }
                Number collectionIdObject = (Number) row.get(_collectionIdColumn.getAlias());
                if (collectionIdObject == null)
                {
                    return HtmlString.EMPTY_STRING;
                }
                collectionId = collectionIdObject.longValue();
                Number groupNumberObject = (Number) row.get(getColumnInfo().getAlias());
                if (groupNumberObject == null)
                {
                    return HtmlString.EMPTY_STRING;
                }
                groupNumber = groupNumberObject.longValue();
            }
        }
        else
        {
            if (row.get(_collectionId) == null)
            {
                return HtmlString.EMPTY_STRING;
            }
            collectionId = ((Number)row.get(_collectionId)).longValue();
            groupNumber = ((Number)row.get(_groupNumber)).longValue();
        }

        HtmlStringBuilder sb = HtmlStringBuilder.of();
        sb.append(groupNumber);
        if (collectionId != 0)
        {
            sb.append("-");
            sb.append(collectionId);
        }
        return sb.getHtmlString();
    }

    @Override
    public void addQueryColumns(Set<ColumnInfo> columns)
    {
        super.addQueryColumns(columns);
        if (_fromQuery)
        {
            FieldKey thisFieldKey = FieldKey.fromString(getColumnInfo().getName());
            List<FieldKey> keys = new ArrayList<>();

            FieldKey idiKey = new FieldKey(thisFieldKey.getTable(), "IndistinguishableCollectionId");
            keys.add(idiKey);
            FieldKey groupIdKey = new FieldKey(thisFieldKey.getTable(), "RowId");
            keys.add(groupIdKey);
            Map<FieldKey, ColumnInfo> cols = QueryService.get().getColumns(getColumnInfo().getParentTable(), keys);

            _collectionIdColumn = cols.get(idiKey);
            if (_collectionIdColumn != null)
            {
                columns.add(_collectionIdColumn);
            }

            ColumnInfo groupIdCol = cols.get(groupIdKey);
            if (groupIdCol != null)
            {
                columns.add(groupIdCol);
            }

            ActionURL url = new ActionURL(MS2Controller.ShowProteinGroupAction.class, _container)
                .addParameter("grouping", "proteinprophet")
                .addParameter("proteinGroupId", "${" + groupIdKey + "}");
            setURL(url);
            setLinkTarget("prot");
        }
    }
}
