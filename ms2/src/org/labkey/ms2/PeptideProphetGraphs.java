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

import org.jfree.chart.ChartColor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.labkey.api.data.Container;
import org.labkey.api.data.SqlSelector;
import org.labkey.ms2.reader.PeptideProphetSummary;
import org.labkey.ms2.reader.SensitivitySummary;

import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * User: arauch
 * Date: Mar 2, 2006
 * Time: 3:16:38 PM
 */
public class PeptideProphetGraphs
{
    public static void renderSensitivityGraph(HttpServletResponse response, SensitivitySummary summary) throws IOException
    {
        XYSeriesCollection collection = new XYSeriesCollection();

        collection.addSeries(toXYSeries("Sensitivity", summary.getMinProb(), summary.getSensitivity()));
        collection.addSeries(toXYSeries("Error", summary.getMinProb(), summary.getError()));

        JFreeChart chart = ChartFactory.createXYLineChart("Sensitivity and Error",
                "Min Probability",
                "Sensitivity and Error",
                collection,
                PlotOrientation.VERTICAL,
                true,
                true,
                false);

        outputChart(response, chart);
    }

    private static void setupColors(JFreeChart chart)
    {
        XYPlot plot = chart.getXYPlot();
        XYItemRenderer renderer = plot.getRenderer();
        renderer.setStroke(new BasicStroke(2.0f));

        if (plot.getSeriesCount() > 0)
        {
            renderer.setSeriesPaint(0, ChartColor.RED);
        }
        if (plot.getSeriesCount() > 1)
        {
            renderer.setSeriesPaint(1, ChartColor.BLUE);
        }
        if (plot.getSeriesCount() > 2)
        {
            renderer.setSeriesPaint(2, ChartColor.DARK_GREEN);
        }
        if (plot.getSeriesCount() > 3)
        {
            renderer.setSeriesPaint(3, ChartColor.DARK_YELLOW);
        }
    }

    public static void renderDistribution(HttpServletResponse response, PeptideProphetSummary summary, int charge, boolean cumulative) throws IOException
    {
        XYSeriesCollection collection = new XYSeriesCollection();
        float[] fval = summary.getFval();
        float[] obs = (cumulative ? convertToCumulative(summary.getObs(charge)) : summary.getObs(charge));
        float[] total = (cumulative ? convertToCumulative(summary.getModelTotal(charge)) : summary.getModelTotal(charge));

        collection.addSeries(toXYSeries("observed", fval, obs));

        if (!cumulative)
        {
            collection.addSeries(toXYSeries("model pos", fval, summary.getModelPos(charge)));
            collection.addSeries(toXYSeries("model neg", fval, summary.getModelNeg(charge)));
        }

        collection.addSeries(toXYSeries("model total", fval, total));
        
        JFreeChart chart = ChartFactory.createXYLineChart("Charge " + charge + "+ " + (cumulative ? "Cumulative" : "") + " Distribution",
                "fval",
                "distribution",
                collection,
                PlotOrientation.VERTICAL,
                true,
                true,
                false);

        outputChart(response, chart);
    }


    public static void renderObservedVsModel(HttpServletResponse response, PeptideProphetSummary summary, int charge, boolean cumulative) throws IOException
    {
        XYSeriesCollection collection = new XYSeriesCollection();

        float[] obs = (cumulative ? convertToCumulative(summary.getObs(charge)) : summary.getObs(charge));
        float[] total = (cumulative ? convertToCumulative(summary.getModelTotal(charge)) : summary.getModelTotal(charge));

        collection.addSeries(toXYSeries("xy", obs, total));

        JFreeChart chart = ChartFactory.createXYLineChart("Charge " + charge + "+ " + (cumulative ? "Cumulative" : "") + " Observed vs. Model",
                "observed",
                "model",
                collection,
                PlotOrientation.VERTICAL,
                false,
                true,
                false);

        outputChart(response, chart);
    }

    public static void renderObservedVsPPScore(HttpServletResponse response, Container c, int runId, int charge, boolean cumulative) throws IOException, SQLException
    {
        String chargeSQL = "SELECT count(*) " +
                        "FROM " + MS2Manager.getTableInfoPeptides().getSelectName() + " " +
                        "WHERE Run = ? AND Charge = ?";
        String negHitPrefix = MS2Manager.NEGATIVE_HIT_PREFIX;

        int total = new SqlSelector(MS2Manager.getSchema(),
                        chargeSQL,
                        runId, charge).getObject(Integer.class);
        int zeroScores = new SqlSelector(MS2Manager.getSchema(),
                        chargeSQL + " AND PeptideProphet = 0",
                        runId, charge).getObject(Integer.class);
        int zeroScoresNegative = new SqlSelector(MS2Manager.getSchema(),
                        chargeSQL + " AND PeptideProphet = 0 AND Protein LIKE ?",
                        runId, charge, negHitPrefix + '%').getObject(Integer.class);
        int zeroScoresPositive = zeroScores - zeroScoresNegative;

        float ratioRandom = (float) zeroScoresPositive / (float) zeroScoresNegative;

        XYSeries series = new XYSeries("xy");
        int increment = total / 250;
        if (increment < 50)
            increment = 50;
        int negative = 0;
        int count = 0;
        float score = 0.0f;

        ResultSet rs = new SqlSelector(MS2Manager.getSchema(),
                "SELECT Protein, PeptideProphet " +
                "FROM " + MS2Manager.getTableInfoPeptides().getSelectName() + " " +
                "WHERE Run = ? AND Charge = ? " +
                "ORDER BY PeptideProphet",
                runId, charge).getResultSet();

        while (rs.next())
        {
            if (rs.getString(1).startsWith(negHitPrefix))
                negative++;
            count++;
            score += rs.getFloat(2);

            if (count == increment)
            {
                negative += negative * ratioRandom; // add back random positives
                if (negative > count)
                    negative = count;
                score /= (float) count; // mean score
                series.add((float) (count - negative) / (float) count, score);

                count = 0;
                negative = 0;
                score = 0;
            }
        }

        XYSeriesCollection collection = new XYSeriesCollection();
        collection.addSeries(series);
        JFreeChart chart = ChartFactory.createXYLineChart("Charge " + charge + "+ " + (cumulative ? "Cumulative" : "") + " Observed vs. Prophet",
                "observed",
                "prophet",
                collection,
                PlotOrientation.VERTICAL,
                false,
                true,
                false);

        outputChart(response, chart);
    }

    private static void outputChart(HttpServletResponse response, JFreeChart chart) throws IOException
    {
        response.setContentType("image/png");
        setupColors(chart);
        ChartUtilities.writeChartAsPNG(response.getOutputStream(), chart, 450, 300);
    }


    private static XYSeries toXYSeries(Comparable key, float[] x, float[] y)
    {
        XYSeries xy = new XYSeries(key);

        for (int i=0; i<x.length; i++)
            xy.add(x[i], y[i]);

        return xy;
    }


    private static float[] convertToCumulative(float[] x)
    {
        float[] y = new float[x.length];

        if (x.length > 0)
        {
            y[0] = x[0];

            for(int i=1; i<x.length; i++)
                y[i] = y[i-1] + x[i];
        }

        return y;
    }
}
