package org.labkey.nab;

import org.jfree.chart.ChartColor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.labkey.api.assay.dilution.DilutionCurve;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.study.WellData;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.Pair;

import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.IOException;
import java.util.*;

/**
 * Copyright (c) 2010 LabKey Corporation
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p/>
 * <p/>
 * User: brittp
 * Date: Apr 21, 2010 12:50:55 PM
 */
public class NabGraph
{
    private static final int DEFAULT_WIDTH = 425;
    private static final int DEFAULT_HEIGHT = 300;

    private static final Color[] GRAPH_COLORS = {
            ChartColor.BLUE,
            ChartColor.RED,
            ChartColor.DARK_GREEN,
            ChartColor.DARK_YELLOW,
            ChartColor.MAGENTA
    };

    public static class Config
    {
        private int[] _cutoffs;
        private boolean _lockAxes = false;
        private String _captionColumn;
        private String _chartTitle;
        private int _height = DEFAULT_HEIGHT;
        private int _width = DEFAULT_WIDTH;
        private int _firstSample = 0;
        private int _maxSamples = -1;

        public int[] getCutoffs()
        {
            return _cutoffs;
        }

        public void setCutoffs(int[] cutoffs)
        {
            _cutoffs = cutoffs;
        }

        public boolean isLockAxes()
        {
            return _lockAxes;
        }

        public void setLockAxes(boolean lockAxes)
        {
            _lockAxes = lockAxes;
        }

        public String getCaptionColumn()
        {
            return _captionColumn;
        }

        public void setCaptionColumn(String captionColumn)
        {
            _captionColumn = captionColumn;
        }

        public String getChartTitle()
        {
            return _chartTitle;
        }

        public void setChartTitle(String chartTitle)
        {
            _chartTitle = chartTitle;
        }

        public int getHeight()
        {
            return _height;
        }

        public void setHeight(int height)
        {
            _height = height;
        }

        public int getWidth()
        {
            return _width;
        }

        public void setWidth(int width)
        {
            _width = width;
        }

        public int getFirstSample()
        {
            return _firstSample;
        }

        public void setFirstSample(int firstSample)
        {
            _firstSample = firstSample;
        }

        public int getMaxSamples()
        {
            return _maxSamples;
        }

        public void setMaxSamples(int maxSamples)
        {
            _maxSamples = maxSamples;
        }
    }

    private static String getDefaultCaption(DilutionSummary summary, boolean longForm)
    {
        String sampleId = (String) summary.getFirstWellGroup().getProperty(AbstractAssayProvider.SPECIMENID_PROPERTY_NAME);
        String participantId = (String) summary.getFirstWellGroup().getProperty(AbstractAssayProvider.PARTICIPANTID_PROPERTY_NAME);
        Double visitId = (Double) summary.getFirstWellGroup().getProperty(AbstractAssayProvider.VISITID_PROPERTY_NAME);
        Date date = (Date) summary.getFirstWellGroup().getProperty(AbstractAssayProvider.DATE_PROPERTY_NAME);
        NabMaterialKey materialKey = new NabMaterialKey(sampleId, participantId, visitId, date);
        return materialKey.getDisplayString(longForm);
    }

    private static String formatCaption(Object captionValue)
    {
        if (captionValue instanceof Date)
        {
            Date date = (Date) captionValue;
            if (date.getHours() == 0 && date.getMinutes() == 0 && date.getSeconds() == 0)
                return DateUtil.formatDate(date);
            else
                return DateUtil.formatDateTime(date);
        }
        else
            return captionValue.toString();
    }

    public static void renderChartPNG(HttpServletResponse response, Map<DilutionSummary, NabAssayRun> summaries, Config config) throws IOException, DilutionCurve.FitFailedException
    {
        boolean longCaptions = false;
        Set<String> shortCaptions = new HashSet<String>();
        for (DilutionSummary summary : summaries.keySet())
        {
            String shortCaption = getDefaultCaption(summary, false);
            if (shortCaptions.contains(shortCaption))
                longCaptions = true;
            shortCaptions.add(shortCaption);
        }
        java.util.List<Pair<String, DilutionSummary>> summaryMap = new ArrayList<Pair<String, DilutionSummary>>();
        for (Map.Entry<DilutionSummary, NabAssayRun> sampleEntry : summaries.entrySet())
        {
            String caption = null;
            DilutionSummary summary = sampleEntry.getKey();
            if (config.getCaptionColumn() != null)
            {
                Object value = summary.getFirstWellGroup().getProperty(config.getCaptionColumn());
                if (value != null)
                    caption = formatCaption(value);
                else
                {
                    Map<PropertyDescriptor, Object> runProperties = sampleEntry.getValue().getRunProperties();
                    for (Map.Entry<PropertyDescriptor, Object> runProperty : runProperties.entrySet())
                    {
                        if (config.getCaptionColumn().equals(runProperty.getKey().getName()) && runProperty.getValue() != null)
                            caption = formatCaption(runProperty.getValue());
                    }
                }
            }
            if (caption == null || caption.length() == 0)
                caption = getDefaultCaption(summary, longCaptions);
            summaryMap.add(new Pair<String, DilutionSummary>(caption, summary));
        }
        renderChartPNG(response, summaryMap, config);
    }

    public static void renderChartPNG(HttpServletResponse response, NabAssayRun assay, Config config) throws IOException, DilutionCurve.FitFailedException
    {
        Map<DilutionSummary, NabAssayRun> samples = new LinkedHashMap<DilutionSummary, NabAssayRun>();
        for (DilutionSummary summary : assay.getSummaries())
        {
            if (!summary.isBlank())
                samples.put(summary, assay);
        }
        if (config.getCutoffs() == null)
            config.setCutoffs(assay.getCutoffs());
        renderChartPNG(response, samples, config);
    }

    public static void renderChartPNG(HttpServletResponse response, java.util.List<Pair<String, DilutionSummary>> dilutionSummaries, Config config) throws IOException, DilutionCurve.FitFailedException
    {
        XYSeriesCollection curvesDataset = new XYSeriesCollection();
        XYSeriesCollection pointDataset = new XYSeriesCollection();
        JFreeChart chart = ChartFactory.createXYLineChart(config.getChartTitle(), null, "Percent Neutralization", curvesDataset, PlotOrientation.VERTICAL, true, true, false);
        XYPlot plot = chart.getXYPlot();
        plot.setDataset(1, pointDataset);
        plot.getRenderer(0).setStroke(new BasicStroke(1.5f));
        if (config.isLockAxes())
            plot.getRangeAxis().setRange(-20, 120);
        XYLineAndShapeRenderer pointRenderer = new XYLineAndShapeRenderer(true, true);
        plot.setRenderer(1, pointRenderer);
        pointRenderer.setStroke(new BasicStroke(
                0.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1.0f, new float[]{4.0f, 4.0f}, 0.0f));
        plot.getRenderer(0).setSeriesVisibleInLegend(false);
        pointRenderer.setShapesFilled(true);

        int count = 0;
        for (Pair<String, DilutionSummary> summaryEntry : dilutionSummaries)
        {
            count++;
            if ((config.getFirstSample() > 0 && count <= config.getFirstSample()) || // before the first sample we want to show
                (config.getMaxSamples() >= 0 && count > config.getMaxSamples() + config.getFirstSample()))// ) // after the last sample we want to show
            {
                continue;
            }

            String sampleId = summaryEntry.getKey();
            DilutionSummary summary = summaryEntry.getValue();
            XYSeries pointSeries = new XYSeries(sampleId);
            for (WellData well : summary.getWellData())
            {
                double percentage = 100 * summary.getPercent(well);
                double dilution = summary.getDilution(well);
                pointSeries.add(dilution, percentage);
            }
            pointDataset.addSeries(pointSeries);
            int pointDatasetCount = pointDataset.getSeriesCount();
            Color currentColor;
            if (pointDatasetCount <= GRAPH_COLORS.length)
            {
                currentColor = GRAPH_COLORS[pointDatasetCount - 1];
                plot.getRenderer(0).setSeriesPaint(pointDatasetCount - 1, currentColor);
            }
            else
                currentColor = (Color) plot.getRenderer(0).getSeriesPaint(pointDatasetCount - 1);

            try
            {
                DilutionCurve.DoublePoint[] curve = summary.getCurve();
                XYSeries curvedSeries = new XYSeries(sampleId);
                for (DilutionCurve.DoublePoint point : curve)
                    curvedSeries.add(point.getX(), point.getY());
                curvesDataset.addSeries(curvedSeries);
                if (currentColor != null)
                    plot.getRenderer(1).setSeriesPaint(curvesDataset.getSeriesCount() - 1, currentColor);
            }
            catch (DilutionCurve.FitFailedException e)
            {
                // fall through; we'll just graph those that can be graphed.
            }
        }

        chart.getXYPlot().setDomainAxis(new LogarithmicAxis("Dilution/Concentration"));
        chart.getXYPlot().addRangeMarker(new ValueMarker(0f, Color.DARK_GRAY, new BasicStroke()));
        for (int cutoff : config.getCutoffs())
            chart.getXYPlot().addRangeMarker(new ValueMarker(cutoff));
        response.setContentType("image/png");
        ChartUtilities.writeChartAsPNG(response.getOutputStream(), chart, config.getWidth(), config.getHeight());
    }
}
