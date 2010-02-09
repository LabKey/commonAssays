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

package org.labkey.ms2.peptideview;

import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.GroupedResultSet;
import org.labkey.api.data.TSVGridWriter;
import org.labkey.api.data.RenderContext;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.List;
import java.io.PrintWriter;

/**
 * User: adam
 * Date: Sep 6, 2006
 */
public abstract class ProteinTSVGridWriter extends TSVGridWriter
{
    protected boolean _expanded = false;
    protected TSVGridWriter _nestedTSVGridWriter = null;
    protected GroupedResultSet _groupedRS = null;
    protected List<DisplayColumn> _peptideDisplayColumns;   // Only used to generate the full set of column headers... needs to match columns set in the peptide grid

    private static Logger _log = Logger.getLogger(ProteinTSVGridWriter.class);

    public ProteinTSVGridWriter(List<DisplayColumn> proteinDisplayColumns, List<DisplayColumn> peptideDisplayColumns)
    {
        super(proteinDisplayColumns);
        _peptideDisplayColumns = peptideDisplayColumns;
    }

    public void setExpanded(boolean expanded)
    {
        _expanded = expanded;
    }

    public boolean getExpanded()
    {
        return _expanded;
    }

    public void setTSVGridWriter(TSVGridWriter nestedTSVGridWriter)
    {
        _nestedTSVGridWriter = nestedTSVGridWriter;
    }

    public void setGroupedResultSet(GroupedResultSet groupedRS) throws SQLException
    {
        _groupedRS = groupedRS;
    }

    protected StringBuilder getHeader(RenderContext ctx, List<DisplayColumn> proteinDisplayColumns)
    {
        StringBuilder header = super.getHeader(ctx, proteinDisplayColumns);

        if (null != _peptideDisplayColumns && !_peptideDisplayColumns.isEmpty())
        {
            if (header.length() > 0)
                header.append(_chDelimiter);

            header.append(super.getHeader(ctx, _peptideDisplayColumns));
        }

        return header;
    }

    protected abstract void addCalculatedValues(RenderContext ctx, ResultSet nestedRS) throws SQLException;

    protected void writeRow(PrintWriter out, RenderContext ctx, List<DisplayColumn> displayColumns)
    {
        try
        {
            if (_expanded)
            {
                ResultSet rs = _groupedRS.getNextResultSet();
                try
                {
                    writeExpandedRow(out, ctx, displayColumns, rs);
                }
                finally
                {
                    if (rs != null) { try { rs.close(); } catch (SQLException e) {} }
                }
            }
            else
                writeCollapsedRow(out, ctx, displayColumns);
        }
        catch(SQLException e)
        {
            _log.error("writeRow", e);
        }
    }


    protected void writeExpandedRow(PrintWriter out, RenderContext ctx, List<DisplayColumn> displayColumns, ResultSet nestedRS) throws SQLException
    {
        addCalculatedValues(ctx, nestedRS);
        nestedRS.beforeFirst();

        // Generate the protein information and store in the context; it will be pre-pended to each peptide row
        StringBuilder proteinRow = getRow(ctx, displayColumns);

        // Append a tab if there's data in the protein columns (there won't be in the case of AMT)
        if (proteinRow.length() > 0)
            proteinRow.append(_chDelimiter);

        RenderContext peptideCtx = new RenderContext(ctx.getViewContext());
        peptideCtx.put("ProteinRow", proteinRow);
        _nestedTSVGridWriter.writeResultSet(peptideCtx, nestedRS);
    }


    protected void writeCollapsedRow(PrintWriter out, RenderContext ctx, List<DisplayColumn> displayColumns) throws SQLException
    {
        ResultSet nestedRS = _groupedRS.getNextResultSet();
        addCalculatedValues(ctx, nestedRS);
        nestedRS.close();

        super.writeRow(out, ctx, displayColumns);
    }
}
