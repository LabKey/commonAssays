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
import org.labkey.api.util.JavaScriptFragment;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.writer.HtmlWriter;

import java.util.List;

import static org.labkey.api.util.DOM.DIV;
import static org.labkey.api.util.DOM.SCRIPT;
import static org.labkey.api.util.DOM.id;

public class MultiValueInputColumn extends DataColumn
{
    private final List<String> _values;

    public MultiValueInputColumn(ColumnInfo col, List<String> values)
    {
        super(col);
        _values = values;
    }

    @Override
    public void renderInputHtml(RenderContext ctx, HtmlWriter out, Object value)
    {
        String colId = ctx.getForm().getFormFieldName(getColumnInfo());

        DIV(
            id(colId).cl("extContainer")
        ).appendTo(out);

        StringBuilder script = new StringBuilder("LABKEY.requiresScript('viability/MultiValueInput', function(){");
        script.append("new MultiValueInput(");
        script.append(PageFlowUtil.jsString(colId));

        // XXX: hack. ignore the value in the render context. take the value as passed in during view creation.
        if (_values != null && !_values.isEmpty())
        {
            script.append(", [");
            for (int i = 0; i < _values.size(); i++)
            {
                script.append(PageFlowUtil.jsString(_values.get(i)));
                if (i < _values.size() - 1)
                    script.append(", ");
            }
            script.append("]");
        }

        script.append(");\n});\n");

        SCRIPT(
            JavaScriptFragment.unsafe(script.toString())
        ).appendTo(out);
    }

    @Override
    protected Object getInputValue(RenderContext ctx)
    {
        // HACK:
        return _values;
    }
}
