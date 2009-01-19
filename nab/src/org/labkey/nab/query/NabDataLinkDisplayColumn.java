/*
 * Copyright (c) 2007-2008 LabKey Corporation
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
import org.labkey.api.view.ActionURL;

import java.io.Writer;
import java.io.IOException;

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
            ActionURL url = new ActionURL("NabAssay", "details", ctx.getContainer()).addParameter("rowId", "" + runId);
            out.write("[<a href=\"" + url.getLocalURIString() + "\" title=\"View run details\">details</a>]");
        }
    }
}
