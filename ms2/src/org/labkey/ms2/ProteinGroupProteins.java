/*
 * Copyright (c) 2006-2016 LabKey Corporation
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

import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlSelector;
import org.labkey.ms2.protein.ProteinManager;
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
    private List<MS2Run> _runs;
    private Map<ResultSet, Map<Integer, List<ProteinSummary>>> _summaries = new WeakHashMap<>();

    public ProteinGroupProteins()
    {
        this(null);
    }

    public ProteinGroupProteins(List<MS2Run> runs)
    {
        _runs = runs;
    }

    private Map<Integer, List<ProteinSummary>> calculateSummaries(ResultSet rs, String columnName)
    {
        Map<Integer, List<ProteinSummary>> result = new HashMap<>();

        int firstGroupId = Integer.MAX_VALUE;
        int lastGroupId = Integer.MIN_VALUE;
        try
        {
            if (rs.getType() == ResultSet.TYPE_FORWARD_ONLY)
            {
                firstGroupId = rs.getInt(columnName);
                lastGroupId = firstGroupId;
            }
            else
            {
                int originalRow = rs.getRow();
                rs.beforeFirst();

                while (rs.next())
                {
                    int groupId = rs.getInt(columnName);
                    firstGroupId = Math.min(firstGroupId, groupId);
                    lastGroupId = Math.max(lastGroupId, groupId);
                }

                rs.absolute(originalRow);
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }

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
    {
        String sql = "SELECT pg.RowId, protseq.SeqId, proteinseq.LookupString AS Protein, protseq.Description, protseq.BestGeneName, protSeq.BestName, protseq.Mass " +
                "FROM " + ProteinManager.getTableInfoSequences() + " protseq, " +
                "   " + MS2Manager.getTableInfoProteinGroupMemberships() + " pgm, " +
                "   " + MS2Manager.getTableInfoProteinGroups() + " pg, " +
                "   " + ProteinManager.getTableInfoFastaSequences() + " proteinseq, " +
                "   " + MS2Manager.getTableInfoFastaRunMapping() + " frm, " +
                "   " + MS2Manager.getTableInfoRuns() + " r, " +
                "   " + MS2Manager.getTableInfoProteinProphetFiles() + " ppf\n" +
                "WHERE pgm.ProteinGroupId = pg.RowId " +
                "   AND pgm.SeqId = protseq.SeqId " +
                "   AND " + extraWhereClause +
                "   AND pg.ProteinProphetFileId = ppf.RowId" +
                "   AND ppf.Run = r.Run" +
                "   AND r.Run = frm.Run" +
                "   AND frm.FastaId = proteinseq.FastaId" +
                "   AND proteinseq.SeqId = pgm.SeqId" +
                "\nORDER BY pg.GroupNumber, pg.IndistinguishableCollectionId, protseq.Length, proteinseq.LookupString";

        Map<String, Object>[] rows = new SqlSelector(MS2Manager.getSchema(), new SQLFragment(sql)).getMapArray();

        for (Map<String, Object> row : rows)
        {
            Integer rowId = ((Number)row.get("RowId")).intValue();
            String lookupString = (String)row.get("Protein");
            int seqId = ((Number)row.get("SeqId")).intValue();
            String description = (String)row.get("Description");
            String bestName = (String)row.get("BestName");
            String bestGeneName = (String)row.get("BestGeneName");
            double sequenceMass = ((Number)row.get("Mass")).doubleValue();
            ProteinSummary summary = new ProteinSummary(lookupString, seqId, description, bestName, bestGeneName, sequenceMass);
            List<ProteinSummary> summaries = result.get(rowId);
            if (summaries == null)
            {
                summaries = new ArrayList<>();
                result.put(rowId, summaries);
            }
            summaries.add(summary);
        }
    }

    public List<ProteinSummary> getSummaries(int proteinGroupId, RenderContext context, String columnName)
    {
        ResultSet rs = context.getResults();
        Map<Integer, List<ProteinSummary>> summaries = _summaries.get(rs);
        if (summaries == null)
        {
            summaries = calculateSummaries(rs, columnName);
            _summaries.put(rs, summaries);
        }
        List<ProteinSummary> result = summaries.get(proteinGroupId);
        if (result == null)
        {
            // We don't have cached results, so requery. We may not have been able to do a single query to get all
            // of the protein summaries, so we might still be able to get it for this individual row
            summaries.putAll(calculateSummaries(rs, columnName));
            result = summaries.get(proteinGroupId);
        }
        return result;
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
            return _runs == runs || new HashSet<>(runs).equals(new HashSet<>(_runs));
        }
        return true;
    }
}
