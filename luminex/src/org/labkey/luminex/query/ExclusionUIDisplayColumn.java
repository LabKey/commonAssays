/*
 * Copyright (c) 2011-2016 LabKey Corporation
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
package org.labkey.luminex.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.settings.AppProps;
import org.labkey.api.util.PageFlowUtil;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

/**
 * User: jeckels
 * Date: 6/29/11
 */
public class ExclusionUIDisplayColumn extends DataColumn
{
    private final FieldKey _typeFieldKey;
    private final FieldKey _descriptionFieldKey;
    private final FieldKey _dataFieldKey;
    private final FieldKey _runFieldKey;
    private final FieldKey _wellIDKey;
    private final FieldKey _exclusionCommentKey;
    private final Integer _protocolId;
    private final Container _container;
    private final User _user;

    public ExclusionUIDisplayColumn(ColumnInfo colInfo, Integer protocolId, Container container, User user)
    {
        super(colInfo);
        _container = container;
        _user = user;
        FieldKey parentFK = colInfo.getFieldKey().getParent();

        _typeFieldKey = new FieldKey(parentFK, "Type");
        _descriptionFieldKey = new FieldKey(parentFK, "Description");
        _exclusionCommentKey = new FieldKey(parentFK, LuminexDataTable.EXCLUSION_COMMENT_COLUMN_NAME);
        _dataFieldKey = new FieldKey(new FieldKey(parentFK, "Data"), "RowId");
        _runFieldKey = new FieldKey(new FieldKey(new FieldKey(parentFK, "Data"), "Run"), "RowId");
        _protocolId = protocolId;
        _wellIDKey = new FieldKey(parentFK, "well");
    }

    @Override
    public void addQueryFieldKeys(Set<FieldKey> keys)
    {
        super.addQueryFieldKeys(keys);
        keys.add(_typeFieldKey);
        keys.add(_descriptionFieldKey);
        keys.add(_exclusionCommentKey);
        keys.add(_dataFieldKey);
        keys.add(_runFieldKey);
    }

    @Override
    public void renderTitle(RenderContext ctx, Writer out) throws IOException
    {
        // Don't render a title, to keep the column narrow
    }

    @Override
    public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
    {
        String type = (String)ctx.get(_typeFieldKey);
        String description = (String)ctx.get(_descriptionFieldKey);
        String exclusionComment = (String)ctx.get(_exclusionCommentKey);
        Integer dataId = (Integer)ctx.get(_dataFieldKey);
        Integer runId = (Integer)ctx.get(_runFieldKey);
        String wellID = PageFlowUtil.filter((String)ctx.get(_wellIDKey));

        String id = "__changeExclusions__" + wellID;

        // add onclick handler to call the well exclusion window creation function
        boolean canEdit = _container.hasPermission(_user, UpdatePermission.class);
        if (canEdit)
        {
            out.write("<a onclick=\"openExclusionsWellWindow(" + _protocolId + ", " + runId + ", " + dataId + ", '" + wellID + "', "
                + (description == null ? null : "'" + description + "'") + ", '" + type + "');\">");
        }

        Boolean excluded = (Boolean)ctx.get(getColumnInfo().getFieldKey());
        if (excluded.booleanValue())
        {
            out.write("<img src=\"" + AppProps.getInstance().getContextPath() + "/luminex/exclusion/excluded.png\" height=\"16\" width=\"16\" id=\""+id+"\"");
            if (canEdit)
            {
                String tooltip = PageFlowUtil.filter(exclusionComment);
                out.write("title=\"" + tooltip + "\" alt=\"" + tooltip + "\" ");
            }
            out.write(" />");
        }
        else
        {
            out.write("<img src=\"" + AppProps.getInstance().getContextPath() + "/luminex/exclusion/included.png\" height=\"16\" width=\"16\"  id=\""+id+"\"");
            if (canEdit)
            {
                out.write("title=\"Click to add a well or replicate group exclusion\" alt=\"Click to add a well or replicate group exclusion\" ");
            }
            out.write(" />");
        }
        if (canEdit)
        {
            out.write("</a>");
        }
    }
}
