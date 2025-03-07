/*
 * Copyright (c) 2007-2019 LabKey Corporation
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
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.ms2.MS2Manager;

import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

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
        setShowPagination(false);
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
    protected void renderGridHeaderColumns(RenderContext ctx, Writer oldWriter, boolean showRecordSelectors, List<DisplayColumn> renderers)
            throws IOException, SQLException
    {
        // Add an extra row and render the multi-column captions
        oldWriter.write("<tr>");

        if (showRecordSelectors)
            oldWriter.write("<td></td>");

        boolean shade = false;
        int columnIndex = 0;
        for (int i = 0; i < _offset; i++)
        {
            if (shade)
            {
                renderers.get(columnIndex).addDisplayClass("labkey-alternate-row");
            }
            shade = !shade;
            columnIndex++;
        }
        if (_offset > 0)
        {
            oldWriter.write("<td colspan=\"");
            oldWriter.write(Integer.toString(_offset));
            oldWriter.write("\" style=\"text-align: center; vertical-align: bottom;\"");
            oldWriter.write("\">");
            oldWriter.write(_columnHeader);
            oldWriter.write("</td>");
        }

        for (String caption : _multiColumnCaptions)
        {
            oldWriter.write("<td align=\"center\" colspan=\"" + _colSpan + "\"");
            if (shade)
            {
                oldWriter.write(" class=\"labkey-alternate-row\"");
                for (int i = 0; i < _colSpan; i++)
                {
                    renderers.get(columnIndex++).addDisplayClass("labkey-alternate-row");
                }
            }
            else
            {
                columnIndex += _colSpan;
            }

            oldWriter.write(">" + caption + "</td>");
            shade = !shade;
        }
        if (_colSpan * _multiColumnCaptions.size() + _offset < renderers.size())
        {
            oldWriter.write("<td colspan=\"");
            oldWriter.write(Integer.toString(renderers.size() - _colSpan * _multiColumnCaptions.size() + _offset));
            oldWriter.write("\">&nbsp;</td>");
        }
        oldWriter.write("</tr>\n");

        super.renderGridHeaderColumns(ctx, oldWriter, showRecordSelectors, renderers);
    }

    @Override
    public boolean getAllowHeaderLock()
    {
        return false;
    }
}
