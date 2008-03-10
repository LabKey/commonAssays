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

        if (_runs != null && !_runs.isEmpty())
        {
            StringBuilder inClause = new StringBuilder();
            inClause.append("SELECT pg.RowId FROM\n");
            inClause.append(MS2Manager.getTableInfoProteinGroups());
            inClause.append(" pg, ");
            inClause.append(MS2Manager.getTableInfoProteinProphetFiles());
            inClause.append(" ppf WHERE ppf.RowId = pg.ProteinProphetFileId AND ppf.Run IN (");

            inClause.append(_runs.get(0).getRun());
            for (int i = 1; i < _runs.size(); i++)
            {
                inClause.append(", ");
                inClause.append(_runs.get(i).getRun());
            }

            inClause.append(")");
            addGroupsToList(inClause, result);
        }
        else
        {
            ResultSet rs = context.getResultSet();
            int originalRow = rs.getRow();

            Set<Integer> requestedGroupIds = new HashSet<Integer>();
            Set<Integer> newGroupIds = new HashSet<Integer>();
            String separator = "";
            StringBuilder inClause = new StringBuilder();

            while (rs.next())
            {
                Integer groupId = rs.getInt(columnName);
                if (!requestedGroupIds.contains(groupId) && !newGroupIds.contains(groupId))
                {
                    inClause.append(separator);
                    inClause.append(groupId);
                    separator = ", ";
                    newGroupIds.add(groupId);

                    // Do this in batches so our IN clause doesn't blow past the database limit
                    if (newGroupIds.size() == 250)
                    {
                        addGroupsToList(inClause, result);
                        requestedGroupIds.addAll(newGroupIds);
                        newGroupIds = new HashSet<Integer>();
                        separator = "";
                        inClause = new StringBuilder();
                    }
                }
            }

            if (newGroupIds.size() > 0)
            {
                addGroupsToList(inClause, result);
            }

            rs.absolute(originalRow);
        }
        return result;
    }

    private void addGroupsToList(StringBuilder inClause, Map<Integer, List<ProteinSummary>> result)
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
                "   AND pg.RowId IN (" + inClause + ")" +
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
