/*
 * Copyright (c) 2014-2019 LabKey Corporation
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
import org.labkey.api.util.DOM;
import org.labkey.api.util.DOM.Attribute;
import org.labkey.api.util.HtmlString;
import org.labkey.api.util.Link;
import org.labkey.api.util.PageFlowUtil;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import static org.labkey.api.util.DOM.Attribute.alt;
import static org.labkey.api.util.DOM.Attribute.height;
import static org.labkey.api.util.DOM.Attribute.src;
import static org.labkey.api.util.DOM.Attribute.title;
import static org.labkey.api.util.DOM.Attribute.width;
import static org.labkey.api.util.DOM.IMG;
import static org.labkey.api.util.DOM.at;
import static org.labkey.api.util.PageFlowUtil.jsString;

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
    public void renderTitle(RenderContext ctx, Writer out)
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

        boolean canEdit = _container.hasPermission(_user, UpdatePermission.class);
        Boolean excluded = (Boolean)ctx.get(getColumnInfo().getFieldKey());

        HtmlString img = excluded.booleanValue() ?
            getImgTag("excluded.png", exclusionComment, id, canEdit) :
            getImgTag("included.png", "Click to add a well or replicate group exclusion", id, canEdit);

        if (canEdit)
        {
            // add onclick handler to call the well exclusion window creation function
            String onClick = "openExclusionsWellWindow(" + _protocolId + ", " + runId + ", " + dataId + ", " +
                jsString(wellID) + ", " + (description == null ? null : jsString(description)) + ", " + jsString(type) + ");";
            new Link.LinkBuilder(img).href("#").onClick(onClick).clearClasses().appendTo(out);
        }
        else
        {
            out.write(img.toString());
        }
    }

    private HtmlString getImgTag(String png, String titleAlt, String id, boolean canEdit)
    {
        DOM._Attributes att = at(src, AppProps.getInstance().getContextPath() + "/luminex/exclusion/" + png)
            .at(height, 16)
            .at(width, 16)
            .at(Attribute.id, id);

        if (canEdit)
            att.at(title, titleAlt).at(alt, titleAlt);

        return DOM.createHtmlFragment(IMG(att));
    }
}
