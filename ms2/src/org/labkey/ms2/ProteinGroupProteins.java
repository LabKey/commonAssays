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

import org.labkey.ms2.protein.ProteinManager;
import org.labkey.api.data.Table;
import org.labkey.api.data.RenderContext;

import java.util.*;
import java.sql.SQLException;
import java.sql.ResultSet;

/**
 * User: jeckels
 * Date: May 5, 2006
 */
public class ProteinGroupProteins
{
    private Map<Integer, List<ProteinSummary>> _summaries;
    private List<MS2Run> _runs;

    public ProteinGroupProteins()
    {
        this(null);
    }

    public ProteinGroupProteins(List<MS2Run> runs)
    {
        _runs = runs;
    }

    private Map<Integer, List<ProteinSummary>> calculateSummaries(RenderContext context, String columnName) throws SQLException
    {
        Map<Integer, List<ProteinSummary>> result = new HashMap<Integer, List<ProteinSummary>>();

        ResultSet rs = context.getResultSet();
        int originalRow = rs.getRow();

        int firstGroupId = Integer.MAX_VALUE;
        int lastGroupId = Integer.MIN_VALUE;

        while (rs.next())
        {
            int groupId = rs.getInt(columnName);
            firstGroupId = Math.min(firstGroupId, groupId);
            lastGroupId = Math.max(lastGroupId, groupId);
        }

        rs.absolute(originalRow);

        StringBuilder whereClause = new StringBuilder();

        whereClause.append(" pg.RowId >= ");
        whereClause.append(firstGroupId);
        whereClause.append(" AND pg.RowId <= ");
        whereClause.append(lastGroupId);

        if (_runs != null && !_runs.isEmpty())
        {
            whereClause.append(" AND ppf.Run IN (");
            whereClause.append(_runs.get(0).getRun());
            for (int i = 1; i < _runs.size(); i++)
            {
                whereClause.append(", ");
                whereClause.append(_runs.get(i).getRun());
            }

            whereClause.append(")");
        }

        addGroupsToList(whereClause, result);
        
        return result;
    }

    private void addGroupsToList(StringBuilder extraWhereClause, Map<Integer, List<ProteinSummary>> result)
        throws SQLException
    {
        String sql = "SELECT pg.RowId, protseq.SeqId, proteinseq.LookupString AS Protein, protseq.Description, protseq.BestGeneName, protSeq.BestName, protseq.Mass " +
                "FROM " + ProteinManager.getTableInfoSequences() + " protseq, " +
                "   " + MS2Manager.getTableInfoProteinGroupMemberships() + " pgm, " +
                "   " + MS2Manager.getTableInfoProteinGroups() + " pg, " +
                "   " + ProteinManager.getTableInfoFastaSequences() + " proteinseq, " +
                "   " + MS2Manager.getTableInfoRuns() + " r, " +
                "   " + MS2Manager.getTableInfoProteinProphetFiles() + " ppf\n" +
                "WHERE pgm.ProteinGroupId = pg.RowId " +
                "   AND pgm.SeqId = protseq.SeqId " +
                "   AND " + extraWhereClause +
                "   AND pg.ProteinProphetFileId = ppf.RowId" +
                "   AND ppf.Run = r.Run" +
                "   AND r.FastaId = proteinseq.FastaId" +
                "   AND proteinseq.SeqId = pgm.SeqId" +
                "\nORDER BY pg.GroupNumber, pg.IndistinguishableCollectionId, protseq.Length, proteinseq.LookupString";

        Map<String, Object>[] rows = Table.executeQuery(MS2Manager.getSchema(), sql, new Object[0], Map.class);

        for (Map<String, Object> row : rows)
        {
            Integer rowId = (Integer)row.get("RowId");
            String lookupString = (String)row.get("Protein");
            int seqId = ((Integer)row.get("SeqId")).intValue();
            String description = (String)row.get("Description");
            String bestName = (String)row.get("BestName");
            String bestGeneName = (String)row.get("BestGeneName");
            double sequenceMass = ((Double)row.get("Mass")).doubleValue();
            ProteinSummary summary = new ProteinSummary(lookupString, seqId, description, bestName, bestGeneName, sequenceMass);
            List<ProteinSummary> summaries = result.get(rowId);
            if (summaries == null)
            {
                summaries = new ArrayList<ProteinSummary>();
                result.put(rowId, summaries);
            }
            summaries.add(summary);
        }
    }

    public List<ProteinSummary> getSummaries(int proteinGroupId, RenderContext context, String columnName) throws SQLException
    {
        if (_summaries == null)
        {
            _summaries = calculateSummaries(context, columnName);
        }
        return _summaries.get(proteinGroupId);
    }


    public void setRuns(List<MS2Run> runs)
    {
        assert runsMatch(runs);
        _runs = runs;
    }

    private boolean runsMatch(List<MS2Run> runs)
    {
        if (_runs != null && _runs.size() != 0)
        {
            return _runs == runs || new HashSet<MS2Run>(runs).equals(new HashSet<MS2Run>(_runs));
        }
        return true;
    }
}
