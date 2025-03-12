/*
 * Copyright (c) 2014 LabKey Corporation
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
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnGroup;
import org.labkey.api.data.RenderContext;
import org.labkey.api.util.DOM;
import org.labkey.api.util.element.Input;
import org.labkey.api.view.HttpView;
import org.labkey.api.writer.HtmlWriter;
import org.labkey.luminex.LuminexDataHandler;

import java.util.List;

import static org.labkey.api.util.DOM.TD;

public class NegativeBeadDisplayColumnGroup extends DisplayColumnGroup
{
    private final String _inputName;

    public NegativeBeadDisplayColumnGroup(List<DisplayColumn> columns, String inputName)
    {
        super(columns, LuminexDataHandler.NEGATIVE_BEAD_COLUMN_NAME, true);
        _inputName = inputName;
    }

    @Override
    public void writeSameCheckboxCell(RenderContext ctx, HtmlWriter out)
    {
        TD(
            isCopyable() ? (DOM.Renderable) ret -> {
                String inputName = ColumnInfo.propNameFromName(_inputName);
                String id = inputName + "CheckBox";
                new Input.InputBuilder().type("checkbox").name(id).id(id).appendTo(out);
                StringBuilder onChange = new StringBuilder("b = this.checked;\n");

                getColumns().forEach(col -> {
                    if (col.getColumnInfo() != null)
                    {
                        onChange.append("s = document.getElementsByName('")
                            .append(col.getFormFieldName(ctx))
                            .append("')[0].options.length;\n")
                            .append("document.getElementsByName('")
                            .append(col.getFormFieldName(ctx))
                            .append("')[0].style.display = b || s == 0 ? 'none' : 'block';\n");
                    }
                });

                onChange.append(" if (b) { ")
                    .append(inputName)
                    .append("Updated(); }");
                HttpView.currentPageConfig().addHandler(id, "change", onChange.toString());

                return ret;
            } :
            null
        ).appendTo(out);
    }
}
