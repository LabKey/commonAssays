/*
 * Copyright (c) 2007-2013 LabKey Corporation
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

package org.labkey.nab.query;

import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SimpleDisplayColumn;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.nab.NabAssayController;

import java.io.Writer;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * User: jeckels
 * Date: Jul 23, 2007
 */
public class NabDataLinkDisplayColumn extends SimpleDisplayColumn
{
    public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
    {
        Object runId = ctx.getRow().get(ExpRunTable.Column.RowId.toString());
        if (runId != null)
        {
            ActionURL url = new ActionURL(NabAssayController.DetailsAction.class, ctx.getContainer()).addParameter("rowId", "" + runId);
            Map<String, String> title = new HashMap<>();
            title.put("title", "View run details");
            out.write(PageFlowUtil.textLink("details", url, "", "", title)); 
        }
    }
}
