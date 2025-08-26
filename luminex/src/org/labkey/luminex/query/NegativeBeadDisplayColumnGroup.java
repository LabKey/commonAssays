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
import org.labkey.api.util.InputBuilder;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.template.PageConfig;
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
                // DOM ids and JS function names can't have spaces
                String inputName = PageConfig.makeIdFromName(_inputName);
                String id = inputName + "CheckBox";
                InputBuilder.checkbox().name(id).id(id).appendTo(out);
                StringBuilder onChange = new StringBuilder("b = this.checked;\n");

                // Index starts at 1 -- always leave the first column visible (Issue 53620)
                for (int i = 1; i < getColumns().size(); i++)
                {
                    DisplayColumn col = getColumns().get(i);
                    if (col.getColumnInfo() != null)
                    {
                        // Issue 53620: instead of hiding the input, set it "disabled" via CSS (but not actually disabled so it will still submit)
                        onChange.append("document.getElementsByName('").append(col.getFormFieldName(ctx)).append("')[0].style.opacity = b ? 0.6 : 1;\n");
                        onChange.append("document.getElementsByName('").append(col.getFormFieldName(ctx)).append("')[0].style.pointerEvents = b ? 'none' : 'all';\n");
                    }
                }

                onChange.append(" if (b) { ")
                    .append(inputName)
                    .append("Updated(); }\n");
                HttpView.currentPageConfig().addHandler(id, "change", onChange.toString());

                return ret;
            } :
            null
        ).appendTo(out);
    }
}
