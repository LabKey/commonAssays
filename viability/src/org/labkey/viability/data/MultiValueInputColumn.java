/*
 * Copyright (c) 2009-2017 LabKey Corporation
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
import org.labkey.api.util.PageFlowUtil;

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

    @Override
    public void renderInputHtml(RenderContext ctx, Writer out, Object value) throws IOException
    {
        String formFieldName = ctx.getForm().getFormFieldName(getColumnInfo());
        String id = getInputPrefix() + formFieldName;

        out.write("<div id=\"" + PageFlowUtil.filter(id) + "\" class=\"extContainer\"></div>");
        out.write("<script text=\"text/javascript\">\n");
        out.write("LABKEY.requiresScript('viability/MultiValueInput', function(){\n");
        out.write("new MultiValueInput('");
        out.write(PageFlowUtil.filter(id));
        out.write("'");

        // XXX: hack. ignore the value in the render context. take the value as passed in during view creation.
        if (_values != null && _values.size() > 0)
        {
            out.write(", [");
            for (int i = 0; i < _values.size(); i++)
            {
                out.write("'");
                out.write(PageFlowUtil.filter(_values.get(i)));
                out.write("'");
                if (i < _values.size() - 1)
                    out.write(", ");
            }
            out.write("]");
        }

        out.write(");\n});\n");
        out.write("</script>\n");
    }

    @Override
    protected Object getInputValue(RenderContext ctx)
    {
        // HACK:
        return _values;
    }
}
