/*
 * Copyright (c) 2014-2026 LabKey Corporation
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

import org.apache.commons.beanutils.ConvertUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.util.DOM;
import org.labkey.api.util.HtmlString;
import org.labkey.api.util.HtmlStringBuilder;
import org.labkey.api.util.JavaScriptFragment;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.SelectBuilder;
import org.labkey.api.writer.HtmlWriter;
import org.labkey.luminex.LuminexDataHandler;

import java.util.Set;

import static org.labkey.api.util.DOM.SCRIPT;
import static org.labkey.api.util.DOM.TD;
import static org.labkey.api.util.DOM.cl;

public class NegativeBeadDisplayColumnFactory implements DisplayColumnFactory
{
    private final String _analyteName;
    private final String _inputName;
    private final String _displayName;
    private final Set<String> _initNegativeControlAnalytes;

    public NegativeBeadDisplayColumnFactory(String analyteName, String inputName, Set<String> initNegativeControlAnalytes)
    {
        _analyteName = analyteName;
        _inputName = inputName;
        _displayName = LuminexDataHandler.NEGATIVE_BEAD_DISPLAY_NAME;
        _initNegativeControlAnalytes = initNegativeControlAnalytes;
    }

    @Override
    public DisplayColumn createRenderer(ColumnInfo colInfo)
    {
        return new DataColumn(colInfo)
        {
            @Override
            public String getFormFieldName(RenderContext ctx)
            {
                return _inputName;
            }

            @Override
            public @NotNull HtmlString getTitle(RenderContext ctx)
            {
                String script = """
                    LABKEY.requiresExt4Sandbox(function() {
                        LABKEY.requiresScript('luminex/NegativeBeadPopulation.js');
                    });
                    """;

                return HtmlStringBuilder.of()
                    .append(DOM.createHtml(SCRIPT(JavaScriptFragment.unsafe(script))))
                    .append(_displayName).getHtmlString();
            }

            @Override
            public void renderDetailsCaptionCell(RenderContext ctx, HtmlWriter out, @Nullable String cls)
            {
                HtmlStringBuilder builder = HtmlStringBuilder.of("""
                    The analyte to use in the FI-Bkgd-Neg transform script calculation. Available options are \
                    those selected as Negative Control analytes.
                    """)
                    .append("Type: ")
                    .append(getBoundColumn().getFriendlyTypeName())
                    .append("\n");

                TD(
                    cl("control-header-label"),
                    getTitle(ctx),
                    PageFlowUtil.popupHelp(builder, _displayName)

                ).appendTo(out);
            }

            @Override
            public void renderInputHtml(RenderContext ctx, HtmlWriter out, Object value)
            {
                String strValue = ConvertUtils.convert(value);
                boolean hidden = _initNegativeControlAnalytes.contains(_analyteName);

                SelectBuilder builder = new SelectBuilder()
                    .name(_inputName)
                    .className("form-control negative-bead-input") // used by NegativeBeadPopulation.js
                    .addStyle("width:200px;" + (hidden ? "display:none;" : "display:inline-block;"))
                    .addDataAttribute("analytename", _analyteName); // used by NegativeBeadPopulation.js

                if (!hidden)
                    builder
                        .addOption("")
                        .addOptions(_initNegativeControlAnalytes)
                        .selected(strValue);

                builder.appendTo(out);
            }
        };
    }
}
