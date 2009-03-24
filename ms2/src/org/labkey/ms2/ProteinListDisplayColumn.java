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

package org.labkey.ms2;

import org.labkey.api.data.SimpleDisplayColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.view.ActionURL;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.CaseInsensitiveHashMap;
import org.labkey.api.query.FieldKey;

import java.io.Writer;
import java.io.IOException;
import java.util.*;
import java.sql.SQLException;
import java.text.DecimalFormat;

/**
 * User: jeckels
 * Date: Feb 15, 2006
 */
public class ProteinListDisplayColumn extends SimpleDisplayColumn
{
    private String _sequenceColumn;
    private final ProteinGroupProteins _proteins;

    private static final DecimalFormat MASS_FORMAT = new DecimalFormat("0.0000");
    private ColumnInfo _columnInfo;
    private String _columnName = "ProteinGroupId";

    public static final List<String> ALL_SEQUENCE_COLUMNS = Collections.unmodifiableList(Arrays.asList("Protein", "BestName", "BestGeneName", "SequenceMass", "Description"));
    private static final Map<String, String> ALL_SEQUENCE_COLUMNS_MAP;

    static
    {
        Map<String, String> values = new CaseInsensitiveHashMap<String>();
        for (String s : ALL_SEQUENCE_COLUMNS)
        {
            values.put(s, s);
        }
        ALL_SEQUENCE_COLUMNS_MAP = Collections.unmodifiableMap(values);
    }

    public ProteinListDisplayColumn(String sequenceColumn, ProteinGroupProteins proteins)
    {
        _sequenceColumn = ALL_SEQUENCE_COLUMNS_MAP.get(FieldKey.fromString(sequenceColumn).getName());
        _proteins = proteins;
        setNoWrap(true);
        setCaption(_sequenceColumn);
    }

    public ColumnInfo getColumnInfo()
    {
        return _columnInfo;
    }

    public Object getValue(RenderContext ctx)
    {
        Map row = ctx.getRow();
        String columnName = _columnName;
        Number id = (Number)row.get(columnName);
        if (id == null)
        {
            columnName = "RowId";
            id = (Number)row.get(columnName);
        }
        try
        {
            List<ProteinSummary> summaryList = _proteins.getSummaries(id.intValue(), ctx, columnName);
            StringBuilder sb = new StringBuilder();
            if (summaryList == null)
            {
                sb.append("ERROR - No matching proteins found");
            }
            else
            {
                String proteinSeparator = "";
                for (ProteinSummary summary : summaryList)
                {
                    sb.append(proteinSeparator);
                    proteinSeparator = ", ";

                    if (_sequenceColumn.equalsIgnoreCase("Protein"))
                    {
                        sb.append(summary.getName());
                    }
                    else if (_sequenceColumn.equalsIgnoreCase("Description"))
                    {
                        sb.append(summary.getDescription());
                    }
                    else if (_sequenceColumn.equalsIgnoreCase("BestName"))
                    {
                        sb.append(summary.getBestName());
                    }
                    else if (_sequenceColumn.equalsIgnoreCase("BestGeneName"))
                    {
                        String geneName = summary.getBestGeneName();
                        if (geneName != null)
                        {
                            sb.append(geneName);
                        }
                    }
                    else if (_sequenceColumn.equalsIgnoreCase("SequenceMass"))
                    {
                        sb.append(summary.getSequenceMass());
                    }
                }
            }
            return sb.toString();
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
    {
        Map row = ctx.getRow();
        try
        {
            Object groupIdObject = row.get(_columnName);
            if (!(groupIdObject instanceof Number))
            {
                out.write("ProteinGroupId not present in ResultSet");
                return;
            }
            int groupId = ((Number) groupIdObject).intValue();
            List<ProteinSummary> summaryList = _proteins.getSummaries(groupId, ctx, _columnName);

            ActionURL url = ctx.getViewContext().cloneActionURL();
            url.setAction("showProtein.view");

            if (summaryList != null)
            {
                for (ProteinSummary summary : summaryList)
                {
                    writeInfo(summary, out, url, groupId);
                }
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }


    public void addQueryColumns(Set<ColumnInfo> set)
    {
        set.add(_columnInfo);
    }

    private void writeInfo(ProteinSummary summary, Writer out, ActionURL url, int groupId) throws IOException
    {
        if (_sequenceColumn.equalsIgnoreCase("Protein"))
        {
                url.replaceParameter("proteinGroupId", Integer.toString(groupId));
            url.replaceParameter("seqId", Integer.toString(summary.getSeqId()));
            out.write("<a href=\"");
            out.write(url.toString());
            out.write("\" target=\"prot\">");
            out.write(PageFlowUtil.filter(summary.getName()));
            out.write("</a>");
        }
        else if (_sequenceColumn.equalsIgnoreCase("Description"))
        {
            out.write(PageFlowUtil.filter(summary.getDescription()));
        }
        else if (_sequenceColumn.equalsIgnoreCase("BestName"))
        {
            out.write(PageFlowUtil.filter(summary.getBestName()));
        }
        else if (_sequenceColumn.equalsIgnoreCase("BestGeneName"))
        {
            String geneName = summary.getBestGeneName();
            if (geneName != null)
            {
                out.write(PageFlowUtil.filter(geneName));
            }
        }
        else if (_sequenceColumn.equalsIgnoreCase("SequenceMass"))
        {
            out.write(PageFlowUtil.filter(MASS_FORMAT.format(summary.getSequenceMass())));
        }
        out.write("<br/>");
    }

    public void setColumnInfo(ColumnInfo colInfo)
    {
        _columnInfo = colInfo;
        _columnName = colInfo.getAlias();
    }
}
