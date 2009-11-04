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

package org.labkey.viability.data;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.RenderContext;

import java.io.Writer;
import java.io.IOException;
import java.util.List;

/**
 * User: kevink
 * Date: Sep 19, 2009
 */
public class MultiValueInputColumn extends DataColumn
{
    private List<String> _values;

    public MultiValueInputColumn(ColumnInfo col, List<String> values)
    {
        super(col);
        _values = values;
    }

    private void renderRequiresScript(RenderContext ctx, Writer out) throws IOException
    {
        if (ctx.get("renderedRequiresMultiValueInputScript") == null)
        {
            out.write("<script type='text/javascript'>LABKEY.requiresScript('viability/MultiValueInput.js');</script>\n");
            ctx.put("renderedRequiresMultiValueInputScript", true);
        }
    }

    @Override
    public void renderInputHtml(RenderContext ctx, Writer out, Object value) throws IOException
    {
        boolean disabledInput = isDisabledInput();
        String formFieldName = ctx.getForm().getFormFieldName(getColumnInfo());
        String id = getInputPrefix() + formFieldName;

        renderRequiresScript(ctx, out);

        out.write("<div id='");
        out.write(id);
        out.write("' class='extContainer'></div>");

        out.write("<script text='text/javascript'>\n");
        out.write("new MultiValueInput('");
        out.write(id);
        out.write("'");

        // XXX: hack. ignore the value in the render context. take the value as pased in during view creation.
        if (_values != null && _values.size() > 0)
        {
            out.write(", [");
            for (String s : _values)
            {
                out.write("'");
                out.write(s);
                out.write("', ");
            }
            out.write("]");
        }

        out.write(");\n");
        out.write("</script>\n");
    }

    @Override
    protected Object getInputValue(RenderContext ctx)
    {
        // HACK:
        return _values;
    }
}
