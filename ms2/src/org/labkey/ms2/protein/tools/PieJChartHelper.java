package org.labkey.ms2.protein.tools;

import org.labkey.api.data.Table;
import org.labkey.api.data.SQLFragment;
import org.labkey.ms2.protein.ProteinManager;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.plot.PiePlot;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * User: tholzman
 * Date: Oct 28, 2005
 * Time: 3:44:19 PM
 */
public class PieJChartHelper extends JChartHelper
{
    private void init()
    {
        this.setDataset(new ProteinPieDataset());
        this.setChart(
                ChartFactory.createPieChart(
                        this.getChartTitle(),
                        (ProteinPieDataset) this.getDataset(),
                        false, // legend?
                        true,  // tooltips?
                        true  // URLs?
                )
        );
        this.plot = chart.getPlot();
        //Default visuals
        ((PiePlot) plot).setLabelGenerator(new GOPieSectionLabelGenerator());
        ((PiePlot) plot).setLabelFont(((PiePlot) plot).getLabelFont().deriveFont((float) 14.0));
        ((PiePlot) plot).setLabelBackgroundPaint(null);
        ((PiePlot) plot).setLabelOutlineStroke(null);
        ((PiePlot) plot).setLabelShadowPaint(null);
        ((PiePlot) plot).setShadowPaint(null);
        ((PiePlot) plot).setToolTipGenerator(new GOPieToolTipGenerator());
        ((PiePlot) plot).setURLGenerator(new GOPieURLGenerator("pieSliceSection.view"));
    }

    public PieJChartHelper()
    {
        init();
    }

    public PieJChartHelper(String title)
    {
        this.chartTitle = title;
        init();
    }

    public int getOtherMin()
    {
        return otherMin;
    }

    public void setOtherMin(int otherMin)
    {
        this.otherMin = otherMin;
    }

    protected int otherMin;

    public HashSet<Integer> getOtherClassifications()
    {
        return otherClassifications;
    }

    public void setOtherClassifications(HashSet<Integer> otherClassifications)
    {
        this.otherClassifications = otherClassifications;
    }

    protected HashSet<Integer> otherClassifications = null;

    private static final int PIESLICE_MAX = 25;


    /*

    Get the third (or higher) level GO accession number(s) and name(s) associated with each of the specified SeqIds.

    distinctSeqIdsSql is SQL that specifies the SeqIds to use.  It should ensure a distinct list of SeqIds and must
    be compatible with an IN clause.  Valid examples:

        "2851"
        "1786, 2109, 3328"
        "SELECT DISTINCT SeqId FROM ms2.MS2Peptides WHERE Run=19 AND PeptideProphet > 0.9"

    This is a summary of the steps used to generate the third-level GO information:

    1. Select each SeqId along with all the associated GO ids of the specified type (e.g., Cellular Location,
       Molecular Function, Metabolic Process).  Each SeqId will have 1 - n associated GO ids and each GO id may
       appear multiple times, since many SeqIds will map to the same GO id.
    2. For each GO id, find a path to one of the GO ids on the third level, using the GoGraphPath table.  If more
       than one exists, pick the lowest third-level GO id.  If none exists, then our GO id must be above the third
       level, so treat it as the third-level GO id.  Each SeqId will now have 1 - n associated third-level GO ids.
    3. Join the third-level GO ids to their accession numbers and names, and collapse any duplicates within a
       single SeqId.

    */
    public static PieJChartHelper prepareGOPie(String title, SQLFragment distinctSeqIdsSql, ProteinDictionaryHelpers.GoTypes goChartType) throws SQLException
    {
        SQLFragment sql = new SQLFragment();

        sql.append("SELECT SeqId, Acc, Name FROM ");
        sql.append(ProteinManager.getTableInfoGoTerm());
        sql.append(" INNER JOIN\n(\n   SELECT SeqId, MIN(ThirdLevelId) AS ThirdLevelId FROM\n   (\n");
        sql.append("      SELECT pa.SeqId, Term2Id AS LocId, CASE WHEN Term1Id IS NULL THEN gt.Id ELSE Term1Id END AS ThirdLevelId FROM ");
        sql.append(ProteinManager.getTableInfoAnnotations());
        sql.append(" pa INNER JOIN\n      ");
        sql.append(ProteinManager.getTableInfoGoTerm());
        sql.append(" gt ON gt.acc = ");
        sql.append(ProteinManager.getSqlDialect().getSubstringFunction("pa.AnnotVal", 1, 10));
        sql.append(" AND pa.");
        sql.append(ProteinDictionaryHelpers.getAnnotTypeWhereClause(goChartType));
        sql.append(" AND pa.SeqId IN (");
        sql.append(distinctSeqIdsSql);
        sql.append(") LEFT OUTER JOIN\n      ");
        sql.append(ProteinManager.getTableInfoGoGraphPath());
        sql.append(" ggp ON ggp.term2Id = gt.id AND ggp.term1Id IN (SELECT Term2Id FROM ");
        sql.append(ProteinManager.getTableInfoGoGraphPath());
        sql.append(" WHERE Term1Id = 1 AND Distance = 3)\n   ) x\n   GROUP BY SeqId, LocId\n");
        sql.append(") y ON ThirdLevelId = Id\nGROUP BY SeqId, Acc, Name");

        PieJChartHelper retVal = new PieJChartHelper(title);
        HashMap<String, Integer> thirdLevTallies = new HashMap<String, Integer>();
        HashMap<String, HashSet<Integer>> extra = new HashMap<String, HashSet<Integer>>();

        ResultSet rs = null;

        try
        {
            rs = Table.executeQuery(ProteinManager.getSchema(), sql.getSQL(), sql.getParams().toArray());

            while(rs.next())
            {
                Integer seqId = rs.getInt(1);
                String thirdLevelAcc = rs.getString(2);
                String thirdLevelName = rs.getString(3);

                String key = thirdLevelAcc + " " + thirdLevelName;
                Integer val = thirdLevTallies.get(key);

                if (val == null)
                    thirdLevTallies.put(key, 1);
                else
                    thirdLevTallies.put(key, thirdLevTallies.get(key) + 1);

                HashSet<Integer> sqids = extra.get(key);
                if (sqids == null)
                {
                    sqids = new HashSet<Integer>();
                    extra.put(key, sqids);
                }
                //store a copy of each seqid for each 3rd level GO accn.  sqids gets
                //is in "extra" which goes into the ProteinPieDataset
                sqids.add(seqId);
            }
        }
        finally
        {
            if (null != rs)
                rs.close();
        }

        ((ProteinPieDataset) retVal.getDataset()).setExtraInfo(extra);

        // This section looks for pie-slices with too few members.  PIESLICE_MAX is
        // an approximation of the number of slices which will look good on a
        // page.  If a category (3rd lev GO category) has too few members, the
        // members are shoved into the "Other" slice.  The minimum number of
        // members before it gets so shoved is in "otherMin".
        Set<String> list = thirdLevTallies.keySet();
        int otherCount = 0;
        retVal.setOtherMin(0);
        int i;

        // This for loop iterates a hypothetical otherMin from 1 (no Other cat.)
        //  to 10.  As soon as the number of categories <= PIESLICE_MAX, the
        //  loop breaks, giving us a minimum otherMin.
        for (i = 1; i <= 10; i++)
        {
            int tallyGtOtherMin = 0;
            for (String k : list)
            {
                int n = thirdLevTallies.get(k).intValue();
                if (n >= i) tallyGtOtherMin++;
            }
            if (tallyGtOtherMin <= PIESLICE_MAX) break;
        }
        retVal.setOtherMin(i);

        // Now we create the pie chart dataset.
        for (String k : list)
        {
            int n = thirdLevTallies.get(k).intValue();
            if (n >= retVal.getOtherMin())
            {
                ((ProteinPieDataset) retVal.getDataset()).setValue(k, n);
            }
            else
            {
                otherCount += n;
            }
        }
        // Here we go through the hell necessary to put seqids associated with
        // the "Other" category into the appropriate parts of the CPASPieDataSet
        // for the image map
        if (otherCount > 0)
        {
            ((ProteinPieDataset) retVal.getDataset()).setValue("Other", otherCount);
            HashSet<Integer> otherSet = new HashSet<Integer>();
            extra.put("Other", otherSet);
            for (String k : list)
            {
                if (!k.equalsIgnoreCase("Other"))
                {
                    int ocount = thirdLevTallies.get(k).intValue();
                    if (ocount < retVal.getOtherMin())
                    {
                        otherSet.addAll(((ProteinPieDataset) retVal.getDataset()).getExtraInfo().get(k));
                    }
                }
            }
        }

        return retVal;
    }
}
