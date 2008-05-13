/*
 * Copyright (c) 2006-2008 LabKey Corporation
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

package org.labkey.ms2.compare;

import org.labkey.api.data.DataRegion;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.DisplayColumn;
import org.labkey.ms2.MS2Manager;

import java.util.List;
import java.io.Writer;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.ResultSet;

public class CompareDataRegion extends DataRegion
{
    List<String> _multiColumnCaptions;
    int _offset = 0;
    int _colSpan;
    private final ResultSet _rs;
    private final String _columnHeader;

    public CompareDataRegion(ResultSet rs)
    {
        this(rs, "&nbsp;");
    }

    public CompareDataRegion(ResultSet rs, String columnHeader)
    {
        _rs = rs;
        _columnHeader = columnHeader;
        setName(MS2Manager.getDataRegionNameCompare());
        setShadeAlternatingRows(true);
        setShowColumnSeparators(true);
    }
    
    public ResultSet getResultSet()
    {
        return _rs;
    }

    public void setMultiColumnCaptions(List<String> multiColumnCaptions)
    {
        _multiColumnCaptions = multiColumnCaptions;
    }

    public void setColSpan(int colSpan)
    {
        _colSpan = colSpan;
    }

    public void setOffset(int offset)
    {
        _offset = offset;
    }

    @Override
    protected void renderGridHeaders(RenderContext ctx, Writer out, List<DisplayColumn> renderers) throws SQLException, IOException
    {
        // Add an extra row and render the multi-column captions
        out.write("<tr>");

        if (_showRecordSelectors)
            out.write("<td></td>");

        boolean shade = false;
        int columnIndex = 0;
        for (int i = 0; i < _offset; i++)
        {
            if (shade)
            {
                renderers.get(columnIndex).setBackgroundColor("#EEEEEE");
            }
            shade = !shade;
            columnIndex++;
        }
        if (_offset > 0)
        {
            out.write("<td colspan=\"");
            out.write(Integer.toString(_offset));
            out.write("\" style=\"text-align: center; vertical-align: bottom; border-bottom: 1px solid rgb(170, 170, 170);");
            out.write("border-right: 1px solid rgb(170, 170, 170);");
            out.write("\">");
            out.write(_columnHeader);
            out.write("</td>");
        }

        for (String caption : _multiColumnCaptions)
        {
            out.write("<td align=\"center\" colspan=\"" + _colSpan + "\" style=\"" + COLUMN_SEPARATOR_STYLE_ARRIBS + "; border-bottom: 1px solid rgb(170, 170, 170);\"");
            if (shade)
            {
                out.write(" bgcolor=\"#EEEEEE\"");
                for (int i = 0; i < _colSpan; i++)
                {
                    renderers.get(columnIndex++).setBackgroundColor("#EEEEEE");
                }
            }
            else
            {
                columnIndex += _colSpan;
            }

            out.write(">" + caption + "</td>");
            shade = !shade;
        }
        if (_colSpan * _multiColumnCaptions.size() + _offset < renderers.size())
        {
            out.write("<td colspan=\"");
            out.write(Integer.toString(renderers.size() - _colSpan * _multiColumnCaptions.size() + _offset));
            out.write("\" style=\"border-right: 1px solid rgb(170, 170, 170); border-bottom: 1px solid rgb(170, 170, 170)\">&nbsp;</td>");
        }
        out.write("</tr>\n");

        super.renderGridHeaders(ctx, out, renderers);
    }
}
