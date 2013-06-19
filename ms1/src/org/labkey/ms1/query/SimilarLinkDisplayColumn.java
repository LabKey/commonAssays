/*
 * Copyright (c) 2008-2013 LabKey Corporation
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
package org.labkey.ms1.query;

import org.labkey.api.data.DataColumn;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.RenderContext;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.ms1.MS1Controller;

import java.io.Writer;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Used when displaying the similar search link
 * 
 * User: Dave
 * Date: Feb 26, 2008
 * Time: 4:41:05 PM
 */
public class SimilarLinkDisplayColumn extends DataColumn
{
    private ActionURL _similarUrl = null;

    public SimilarLinkDisplayColumn(ColumnInfo column)
    {
        super(column);
    }

    public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
    {
        if(null == _similarUrl)
            _similarUrl = new ActionURL(MS1Controller.SimilarSearchAction.class, ctx.getContainer());

        Object val = getValue(ctx);
        if(null == val)
            return;

        //val is an Integer
        assert val instanceof Integer : "Value for SimilarLinkDisplayColumn was not an Integer!";
        int featureId = ((Integer)val).intValue();
        _similarUrl.replaceParameter(MS1Controller.SimilarSearchForm.ParamNames.featureId.name(), String.valueOf(featureId));
        out.write(PageFlowUtil.textLink("similar", _similarUrl.getLocalURIString()));
    }

    public void renderTitle(RenderContext ctx, Writer out) throws IOException
    {
        //don't display a title
        out.write("&nbsp;");
    }

    public boolean isSortable()
    {
        return false;
    }

    public boolean isFilterable()
    {
        return false;
    }
}
