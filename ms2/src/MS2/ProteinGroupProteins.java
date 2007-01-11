package MS2;

import org.labkey.api.protein.ProteinManager;
import org.labkey.api.ms2.MS2Manager;
import org.labkey.api.ms2.MS2Run;
import org.labkey.api.data.Table;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.sql.SQLException;

/**
 * User: jeckels
 * Date: May 5, 2006
 */
public class ProteinGroupProteins
{
    private Map<Integer, List<ProteinSummary>> _summaries;
    private final MS2Run[] _runs;

    public ProteinGroupProteins(MS2Run... runs)
    {
        _runs = runs;
    }

    private Map<Integer, List<ProteinSummary>> calculateSummaries() throws SQLException
    {
        Map<Integer, List<ProteinSummary>> result = new HashMap<Integer, List<ProteinSummary>>();

        if (_runs.length > 0)
        {
            StringBuilder inClause = new StringBuilder("?");
            for (int i = 1; i < _runs.length; i++)
            {
                inClause.append(", ?");
            }

            String sql = "SELECT pg.RowId, protseq.SeqId, proteinseq.LookupString AS Protein, protseq.Description, protseq.BestGeneName, protSeq.BestName, protseq.Mass " +
                    "FROM " + ProteinManager.getTableInfoSequences() + " protseq, " +
                    "   " + MS2Manager.getTableInfoProteinGroupMemberships() + " pgm, " +
                    "   " + MS2Manager.getTableInfoProteinGroups() + " pg, " +
                    "   " + ProteinManager.getTableInfoFastaSequences() + " proteinseq, " +
                    "   " + MS2Manager.getTableInfoRuns() + " r, " +
                    "   " + MS2Manager.getTableInfoProteinProphetFiles() + " ppf\n" +
                    "WHERE pgm.ProteinGroupId = pg.RowId " +
                    "   AND pgm.SeqId = protseq.SeqId " +
                    "   AND ppf.Run IN (" + inClause + ")" +
                    "   AND pg.ProteinProphetFileId = ppf.RowId" +
                    "   AND ppf.Run = r.Run" +
                    "   AND r.FastaId = proteinseq.FastaId" +
                    "   AND proteinseq.SeqId = pgm.SeqId" +
                    "\nORDER BY pg.GroupNumber, pg.IndistinguishableCollectionId, protseq.Length, proteinseq.LookupString";
            Object[] ids = new Object[_runs.length];
            for (int i = 0; i < _runs.length; i++)
            {
                ids[i] = _runs[i].getRun();
            }
            Map<String, Object>[] rows = Table.executeQuery(MS2Manager.getSchema(), sql, ids, Map.class);

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
        return result;
    }

    public List<ProteinSummary> getSummaries(int proteinGroupId) throws SQLException
    {
        if (_summaries == null)
        {
            _summaries = calculateSummaries();
        }
        return _summaries.get(proteinGroupId);
    }
}
