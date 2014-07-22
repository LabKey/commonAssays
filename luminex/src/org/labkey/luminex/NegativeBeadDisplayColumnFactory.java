package org.labkey.luminex;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.util.PageFlowUtil;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

/**
 * Created by cnathe on 7/21/14.
 */
public class NegativeBeadDisplayColumnFactory implements DisplayColumnFactory
{
    private String _analyteName;
    private String _inputName;
    private String _displayName;
    private Set<String> _initNegativeControlAnalytes;

    public NegativeBeadDisplayColumnFactory(String analyteName, String inputName, String displayName, Set<String> initNegativeControlAnalytes)
    {
        _analyteName = analyteName;
        _inputName = inputName;
        _displayName = displayName;
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
                out.write("<script type='text/javascript'>"
                        + "   LABKEY.requiresScript('luminex/NegativeBeadPopulation.js');"
                        + "</script>");
                out.write(_displayName);
            }

            @Override
            public void renderDetailsCaptionCell(RenderContext ctx, Writer out) throws IOException
            {
                out.write("<td class='labkey-form-label'>");
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
                out.write("<select name=\"" + _inputName + "\" " +
                        "class=\"negative-bead-input\" " + // used by NegativeBeadPopulation.js
                        "analytename=\"" + _analyteName + "\" " + // used by NegativeBeadPopulation.js
                        "width=\"200\" style=\"width:200px;\"");
                if (_initNegativeControlAnalytes.contains(_analyteName))
                {
                    out.write(" DISABLED>");
                }
                else
                {
                    out.write(">");
                    out.write("<option value=\"\"></option>");
                    for (String negControlAnalyte : _initNegativeControlAnalytes)
                    {
                        out.write("<option value=\"" + negControlAnalyte + "\"");
                        if (value != null && value.equals(negControlAnalyte))
                        {
                            out.write(" SELECTED");
                        }
                        out.write(">");
                        out.write(negControlAnalyte);
                        out.write("</option>");
                    }
                }
                out.write("</select>");
            }
        };
    }
}
