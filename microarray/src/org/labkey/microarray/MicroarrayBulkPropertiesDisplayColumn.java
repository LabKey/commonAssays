/*
 * Copyright (c) 2009 LabKey Corporation
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
package org.labkey.microarray;

import org.labkey.api.data.SimpleDisplayColumn;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.RenderContext;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.JspView;

import java.io.Writer;
import java.io.IOException;

/**
 * User: jeckels
 * Date: Jan 30, 2009
 */
public class MicroarrayBulkPropertiesDisplayColumn extends SimpleDisplayColumn
{
    private MicroarrayRunUploadForm _form;
    private ColumnInfo _col;
    public static final String PROPERTIES_FIELD_NAME = "__bulkProperties";
    public static final String ENABLED_FIELD_NAME = "__enableBulkProperties";

    public MicroarrayBulkPropertiesDisplayColumn(MicroarrayRunUploadForm form)
    {
        _form = form;
        setCaption("Bulk Properties");

        _col = new ColumnInfo("Bulk Properties");
        _col.setInputType("file");
        setWidth("100%");
    }

    public boolean isEditable()
    {
        return true;
    }

    public ColumnInfo getColumnInfo()
    {
        return _col;
    }

    public void renderInputHtml(RenderContext ctx, Writer out, Object value) throws IOException
    {
        JspView<MicroarrayRunUploadForm> view = new JspView<MicroarrayRunUploadForm>("/org/labkey/microarray/bulkPropertiesInput.jsp", _form);
        try
        {
            view.render(ctx.getRequest(), ctx.getViewContext().getResponse());
        }
        catch (Exception e)
        {
            throw (IOException)new IOException().initCause(e);
        }
    }
}