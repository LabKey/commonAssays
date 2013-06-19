/*
 * Copyright (c) 2006-2013 LabKey Corporation
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
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.view.ActionURL;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.query.FieldKey;

import java.io.Writer;
import java.io.IOException;
import java.util.*;
import java.text.DecimalFormat;

/**
 * User: jeckels
 * Date: Feb 15, 2006
 */
public class ProteinListDisplayColumn extends SimpleDisplayColumn
{
    private final SequenceColumnType _sequenceColumn;
    private final ProteinGroupProteins _proteins;

    private static final DecimalFormat MASS_FORMAT = new DecimalFormat("0.0000");
    private ColumnInfo _columnInfo;
    private String _columnName = "ProteinGroupId";

    public static final List<String> SEQUENCE_COLUMN_NAMES;
    /** Case insensitive map from name to enum type */
    private static final Map<String, SequenceColumnType> ALL_SEQUENCE_COLUMNS_MAP;

    static
    {
        Map<String, SequenceColumnType> values = new CaseInsensitiveHashMap<>();
        for (SequenceColumnType sequenceColumnType : SequenceColumnType.values())
        {
            values.put(sequenceColumnType.toString(), sequenceColumnType);
        }

        SEQUENCE_COLUMN_NAMES = Collections.unmodifiableList(new ArrayList<>(values.keySet()));
        ALL_SEQUENCE_COLUMNS_MAP = Collections.unmodifiableMap(values);
    }

    public enum SequenceColumnType
    {
        Protein
        {
            @Override
            public Object getValue(ProteinSummary summary)
            {
                return summary.getName();
            }

            @Override
            public void writeInfo(ProteinSummary summary, Writer out, ActionURL url, int groupId) throws IOException
            {
                url.replaceParameter("proteinGroupId", Integer.toString(groupId));
                url.replaceParameter("seqId", Integer.toString(summary.getSeqId()));
                out.write("<a href=\"");
                out.write(url.toString());
                out.write("\" target=\"prot\">");
                out.write(PageFlowUtil.filter(summary.getName()));
                out.write("</a>");
            }
        },
        Description
        {
            public Object getValue(ProteinSummary summary)
            {
                return summary.getDescription();
            }

            @Override
            public void writeInfo(ProteinSummary summary, Writer out, ActionURL url, int groupId) throws IOException
            {
                out.write(PageFlowUtil.filter(summary.getDescription()));
            }
        },
        BestName
        {
            @Override
            public Object getValue(ProteinSummary summary)
            {
                return summary.getBestName();
            }

            @Override
            public void writeInfo(ProteinSummary summary, Writer out, ActionURL url, int groupId) throws IOException
            {
                out.write(PageFlowUtil.filter(summary.getBestName()));
            }
        },
        BestGeneName
        {
            @Override
            public Object getValue(ProteinSummary summary)
            {
                return summary.getBestGeneName();
            }

            @Override
            public void writeInfo(ProteinSummary summary, Writer out, ActionURL url, int groupId) throws IOException
            {
                String geneName = summary.getBestGeneName();
                if (geneName != null)
                {
                    out.write(PageFlowUtil.filter(geneName));
                }
            }
        },
        SequenceMass
        {
            @Override
            public Object getValue(ProteinSummary summary)
            {
                return summary.getSequenceMass();
            }

            @Override
            public void writeInfo(ProteinSummary summary, Writer out, ActionURL url, int groupId) throws IOException
            {
                out.write(PageFlowUtil.filter(MASS_FORMAT.format(summary.getSequenceMass())));
            }

            @Override
            public String getTextAlign()
            {
                return "right";
            }
        };

        public abstract Object getValue(ProteinSummary summary);

        public abstract void writeInfo(ProteinSummary summary, Writer out, ActionURL url, int groupId) throws IOException;

        public String getTextAlign()
        {
            return "left";
        }
    }

    public ProteinListDisplayColumn(String sequenceColumn, ProteinGroupProteins proteins)
    {
        _sequenceColumn = ALL_SEQUENCE_COLUMNS_MAP.get(FieldKey.fromString(sequenceColumn).getName());
        if (_sequenceColumn == null)
        {
            throw new IllegalArgumentException("Could not find sequence column for " + sequenceColumn);
        }
        _proteins = proteins;
        setNoWrap(true);
        setCaption(_sequenceColumn.toString());
        setTextAlign(_sequenceColumn.getTextAlign());
    }

    public ColumnInfo getColumnInfo()
    {
        return _columnInfo;
    }

    public Object getValue(RenderContext ctx)
    {
        Map row = ctx.getRow();
        String columnName = _columnName;
        Number id;
        if (!row.containsKey(columnName))
        {
            columnName = "RowId";
            id = (Number)row.get(columnName);
        }
        else
        {
            id = (Number)row.get(columnName);
        }
        if (id == null)
        {
            return "";
        }

        List<ProteinSummary> summaryList = _proteins.getSummaries(id.intValue(), ctx, columnName);
        StringBuilder sb = new StringBuilder();
        if (summaryList == null)
        {
            sb.append("ERROR - No matching proteins found for RowId ");
            sb.append(id);
        }
        else
        {
            String separator = "";
            for (ProteinSummary summary : summaryList)
            {
                Object value = _sequenceColumn.getValue(summary);
                if (value != null)
                {
                    sb.append(separator);
                    separator = ", ";
                    sb.append(value);
                }
            }
        }
        return sb.toString();
    }

    public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
    {
        Map row = ctx.getRow();

        if (!row.containsKey(_columnName))
        {
            out.write("ProteinGroupId not present in ResultSet");
            return;
        }
        Object groupIdObject = row.get(_columnName);
        if (groupIdObject == null)
        {
            return;
        }
        if (!(groupIdObject instanceof Number))
        {
            out.write("ProteinGroupId is of unexpected type: " + groupIdObject.getClass());
            return;
        }
        int groupId = ((Number) groupIdObject).intValue();
        List<ProteinSummary> summaryList = _proteins.getSummaries(groupId, ctx, _columnName);

        ActionURL url = ctx.getViewContext().cloneActionURL();
        url.setAction(MS2Controller.ShowProteinAction.class);

        if (summaryList != null)
        {
            for (ProteinSummary summary : summaryList)
            {
                writeInfo(summary, out, url, groupId);
            }
        }
    }


    public void addQueryColumns(Set<ColumnInfo> set)
    {
        set.add(_columnInfo);
    }

    private void writeInfo(ProteinSummary summary, Writer out, ActionURL url, int groupId) throws IOException
    {
        _sequenceColumn.writeInfo(summary, out, url, groupId);
        out.write("<br/>");
    }

    public void setColumnInfo(ColumnInfo colInfo)
    {
        _columnInfo = colInfo;
        _columnName = colInfo.getAlias();
    }
}
