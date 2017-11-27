/*
 * Copyright (c) 2014-2017 LabKey Corporation
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
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.luminex.LuminexDataHandler;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

public class NegativeBeadDisplayColumnFactory implements DisplayColumnFactory
{
    private String _analyteName;
    private String _inputName;
    private String _displayName;
    private Set<String> _initNegativeControlAnalytes;

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
            public void renderTitle(RenderContext ctx, Writer out) throws IOException
            {
                out.write("<script type=\"text/javascript\">\n" +
                        "LABKEY.requiresExt4Sandbox(function() {\n" +
                            "LABKEY.requiresScript('luminex/NegativeBeadPopulation.js');\n" +
                        "});\n" +
                    "</script>\n");

                out.write(PageFlowUtil.filter(_displayName));
            }

            @Override
            public void renderDetailsCaptionCell(RenderContext ctx, Writer out, @Nullable String cls) throws IOException
            {
                out.write("<td class=\"control-header-label\">");

                renderTitle(ctx, out);
                StringBuilder sb = new StringBuilder();
                sb.append("The analyte to use in the FI-Bkgd-Neg transform script calculation. Available options are " +
                        "those selected as Negative Control analytes.\n\n");
                sb.append("Type: ").append(getBoundColumn().getFriendlyTypeName()).append("\n");
                out.write(PageFlowUtil.helpPopup(_displayName, sb.toString()));

                out.write("</td>");
            }

            @Override
            public void renderInputHtml(RenderContext ctx, Writer out, Object value) throws IOException
            {
                String strValue = ConvertUtils.convert(value);
                boolean hidden = _initNegativeControlAnalytes.contains(_analyteName);

                out.write("<select name=\"" + PageFlowUtil.filter(_inputName) + "\" " +
                        "class=\"form-control negative-bead-input\" " + // used by NegativeBeadPopulation.js
                        "analytename=\"" + PageFlowUtil.filter(_analyteName) + "\" " + // used by NegativeBeadPopulation.js
                        "width=\"200\" style=\"width:200px;" +
                        (hidden ? "display:none;" : "display:inline-block;") + "\">");

                if (!hidden)
                {
                    out.write("<option value=\"\"></option>");
                    for (String negControlAnalyte : _initNegativeControlAnalytes)
                    {
                        out.write("<option value=\"" + PageFlowUtil.filter(negControlAnalyte) + "\"");
                        if (strValue != null && strValue.equals(negControlAnalyte))
                        {
                            out.write(" SELECTED");
                        }
                        out.write(">");
                        out.write(PageFlowUtil.filter(negControlAnalyte));
                        out.write("</option>");
                    }
                }
                out.write("</select>");
            }
        };
    }
}
