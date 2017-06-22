/*
 * Copyright (c) 2008-2017 LabKey Corporation
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
import org.labkey.api.data.Container;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.RenderContext;
import org.labkey.api.settings.AppProps;
import org.labkey.api.view.ActionURL;
import org.labkey.ms1.MS1Controller;

import java.io.Writer;
import java.io.IOException;

/**
 * Renders the features link column
 *
 * User: Dave
 * Date: Feb 28, 2008
 * Time: 1:19:34 PM
 */
public class FeaturesLinkDisplayColumn  extends DataColumn
{
    private ActionURL _featuresUrl = null;

    public FeaturesLinkDisplayColumn(ColumnInfo column, Container container)
    {
        super(column);
        setCaption("");
        setWidth("18");
        _featuresUrl = new ActionURL(MS1Controller.ShowFeaturesAction.class, container);
    }

    public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
    {
        Object runId = getValue(ctx);
        if(null == runId)
            return;
        
        _featuresUrl.replaceParameter(MS1Controller.ShowFeaturesForm.ParamNames.runId.name(), runId.toString());
        out.write("<a href=\"");
        out.write(_featuresUrl.getLocalURIString());
        out.write("\" title=\"Features Link\"><img src=\"");
        out.write(AppProps.getInstance().getContextPath());
        out.write("/ms1/images/features.png\" height=\"18\" width=\"18\" alt=\"features\"/></a>");
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
