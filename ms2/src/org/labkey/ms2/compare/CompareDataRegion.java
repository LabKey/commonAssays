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

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.api.util.DOM.Renderable;
import org.labkey.api.util.HtmlString;
import org.labkey.api.writer.HtmlWriter;
import org.labkey.ms2.MS2Manager;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.labkey.api.util.DOM.Attribute.align;
import static org.labkey.api.util.DOM.Attribute.colspan;
import static org.labkey.api.util.DOM.Attribute.style;
import static org.labkey.api.util.DOM.TD;
import static org.labkey.api.util.DOM.TR;
import static org.labkey.api.util.DOM.at;

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
    protected void renderGridHeaderColumns(RenderContext ctx, HtmlWriter out, boolean showRecordSelectors, List<DisplayColumn> renderers)
            throws IOException, SQLException
    {
        // Add an extra row and render the multi-column captions
        TR(
            showRecordSelectors ? TD() : null,
            _offset > 0 ? TD(at(colspan, _offset, style, "text-align: center; vertical-align: bottom;"), _columnHeader) : null,
            (Renderable) ret -> {
                final MutableBoolean shade = new MutableBoolean(false);
                final MutableInt columnIndex = new MutableInt(0);
                for (int i = 0; i < _offset; i++)
                {
                    if (shade.booleanValue())
                    {
                        renderers.get(columnIndex.getValue()).addDisplayClass("labkey-alternate-row");
                    }
                    shade.setValue(!shade.getValue());
                    columnIndex.increment();
                }

                if (_offset > 0)
                {
                    TD(
                        at(colspan, _offset, style, "text-align: center; vertical-align: bottom;"),
                        _columnHeader
                    ).appendTo(out);
                }

                for (String caption : _multiColumnCaptions)
                {
                    TD(
                        at(align, "center", colspan, _colSpan).cl(shade.isTrue(), "labkey-alternate-row"),
                        (Renderable) rend -> {
                            if (shade.isTrue())
                            {
                                for (int i = 0; i < _colSpan; i++)
                                {
                                    renderers.get(columnIndex.getAndIncrement()).addDisplayClass("labkey-alternate-row");
                                }
                            }
                            else
                            {
                                columnIndex.add(_colSpan);
                            }
                            return rend;
                        },
                        caption
                    ).appendTo(out);

                    shade.setValue(!shade.getValue());
                }

                return ret;
            },
            _colSpan * _multiColumnCaptions.size() + _offset < renderers.size() ? TD(at(colspan, renderers.size() - _colSpan * _multiColumnCaptions.size() + _offset), HtmlString.NBSP) : null
        ).appendTo(out);

        super.renderGridHeaderColumns(ctx, out, showRecordSelectors, renderers);
    }

    @Override
    public boolean getAllowHeaderLock()
    {
        return false;
    }
}
