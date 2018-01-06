/*
 * Copyright (c) 2005-2015 Fred Hutchinson Cancer Research Center
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

package org.labkey.ms2.protein.tools;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.plot.PiePlot;
import org.labkey.api.data.Container;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.view.ActionURL;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.protein.ProteinManager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * User: tholzman
 * Date: Oct 28, 2005
 * Time: 3:44:19 PM
 */
public class PieJChartHelper extends JChartHelper<ProteinPieDataset, PiePlot>
{
    private final Container _container;

    private void init()
    {
        this.setDataset(new ProteinPieDataset());
        this.setChart(
                ChartFactory.createPieChart(
                        this.getChartTitle(),
                        this.getDataset(),
                        false, // legend?
                        true,  // tooltips?
                        true  // URLs?
                )
        );
        this.plot = (PiePlot) chart.getPlot();
        //Default visuals
        plot.setLabelGenerator(new GOPieSectionLabelGenerator());
        plot.setLabelFont(plot.getLabelFont().deriveFont((float) 14.0));
        plot.setLabelBackgroundPaint(null);
        plot.setLabelOutlineStroke(null);
        plot.setLabelShadowPaint(null);
        plot.setShadowPaint(null);
        plot.setToolTipGenerator(new GOPieToolTipGenerator());
        plot.setURLGenerator(new GOPieURLGenerator(new ActionURL(MS2Controller.PieSliceSectionAction.class, _container)));
    }

    public PieJChartHelper(String title, Container container)
    {
        _container = container;
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
    public static PieJChartHelper prepareGOPie(String title, SQLFragment distinctSeqIdsSql, ProteinDictionaryHelpers.GoTypes goChartType, Container c) throws SQLException
    {
        SQLFragment sql = new SQLFragment();

        sql.append("SELECT SeqId, LocId, Acc, Name FROM ");
        sql.append(ProteinManager.getTableInfoGoTerm());
        sql.append(" INNER JOIN\n(\n   SELECT SeqId, ThirdLevelId, LocId FROM\n   (\n");
        sql.append("      SELECT pa.SeqId, Term2Id AS LocId, CASE WHEN Term1Id IS NULL THEN gt.Id ELSE Term1Id END AS ThirdLevelId FROM ");
        sql.append(ProteinManager.getTableInfoAnnotations(), "pa");
        sql.append(" INNER JOIN\n      ");
        sql.append(ProteinManager.getTableInfoGoTerm(), "gt");
        sql.append(" ON gt.acc = ");
        sql.append(ProteinManager.getSqlDialect().getSubstringFunction("pa.AnnotVal", "1", "10"));
        sql.append(" AND pa.");
        sql.append(ProteinDictionaryHelpers.getAnnotTypeWhereClause(goChartType));
        sql.append(" AND pa.SeqId IN (");
        sql.append(distinctSeqIdsSql);
        sql.append(") LEFT OUTER JOIN\n      ");
        sql.append(ProteinManager.getTableInfoGoGraphPath(), "ggp");
        sql.append(" ON ggp.term2Id = gt.id AND ggp.term1Id IN (SELECT Term2Id FROM ");
        sql.append(ProteinManager.getTableInfoGoGraphPath());
        sql.append(" WHERE Term1Id = 1 AND Distance = 3)\n   ) x\n");
        sql.append(") y ON ThirdLevelId = Id\nGROUP BY SeqId, ThirdLevelId, LocId, Acc, Name\n");
        sql.append("ORDER BY SeqId, LocId, ThirdLevelId");

        PieJChartHelper retVal = new PieJChartHelper(title, c);
        Map<String, Integer> thirdLevTallies = new HashMap<>();
        Map<String, Set<Integer>> extra = new HashMap<>();

        try (ResultSet rs = new SqlSelector(ProteinManager.getSchema(), sql).getResultSet())
        {
            int prevSeqId = -1;
            int prevLocId = -1;

            while (rs.next())
            {
                int seqId = rs.getInt(1);
                int locId = rs.getInt(2);

                // We are simulating SELECT MIN(ThirdLevelId) ... GROUP BY SeqId, LocId in Java because SQL Server 2000
                // explodes if we add one more GROUP BY clause to this query, see issue #1664
                //
                // TODO: we should consider removing this and creating the chart with all matching ThirdLevelIds instead
                if (seqId == prevSeqId && locId == prevLocId)
                    continue;

                prevSeqId = seqId;
                prevLocId = locId;

                String thirdLevelAcc = rs.getString(3);
                String thirdLevelName = rs.getString(4);

                String key = thirdLevelAcc + " " + thirdLevelName;

                thirdLevTallies.merge(key, 1, (a, b) -> a + b);

                Set<Integer> sqids = extra.computeIfAbsent(key, k -> new HashSet<>());
                //store a copy of each seqid for each 3rd level GO accn.  sqids gets
                //is in "extra" which goes into the ProteinPieDataset
                sqids.add(seqId);
            }
        }

        retVal.getDataset().setExtraInfo(extra);

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
                int n = thirdLevTallies.get(k);
                if (n >= i) tallyGtOtherMin++;
            }
            if (tallyGtOtherMin <= PIESLICE_MAX) break;
        }
        retVal.setOtherMin(i);

        // Now we create the pie chart dataset.
        for (String k : list)
        {
            int n = thirdLevTallies.get(k);
            if (n >= retVal.getOtherMin())
            {
                retVal.getDataset().setValue(k, n);
            }
            else
            {
                otherCount += n;
            }
        }
        // Here we go through the hell necessary to put seqids associated with
        // the "Other" category into the appropriate parts of the ProteinPieDataset
        // for the image map
        if (otherCount > 0)
        {
            retVal.getDataset().setValue("Other", otherCount);
            HashSet<Integer> otherSet = new HashSet<>();
            extra.put("Other", otherSet);
            for (String k : list)
            {
                if (!k.equalsIgnoreCase("Other"))
                {
                    int ocount = thirdLevTallies.get(k);
                    if (ocount < retVal.getOtherMin())
                    {
                        otherSet.addAll(retVal.getDataset().getExtraInfo().get(k));
                    }
                }
            }
        }

        return retVal;
    }
}
