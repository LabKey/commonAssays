/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.awt.*;

public class ElutionGraph
{
    public static final int WIDTH = 700;
    public static final int HEIGHT = 200;

    private DefaultTableXYDataset _dataset = new DefaultTableXYDataset();
    private JFreeChart _chart = ChartFactory.createXYBarChart(
                null,
                "Scan",
                false,
                "Intensity",
                _dataset,
                PlotOrientation.VERTICAL,
                false,
                false,
                false
        );

    public void addInfo(List<Quantitation.ScanInfo> scanInfos, int firstSelectedScan, int lastSelectedScan, int minScan, int maxScan, Color color)
    {
        XYSeries selectedSeries = new XYSeries("Selected", false, true);
        XYSeries surroundingSeries = new XYSeries("Surrounding", false, true);

        float area = 0;
        boolean addedMinScan = false;
        boolean addedMaxScan = false;
        for (Quantitation.ScanInfo scanInfo : scanInfos)
        {
            if (scanInfo.getScan() == minScan)
            {
                addedMinScan = true;
            }
            else if (scanInfo.getScan() == maxScan)
            {
                addedMaxScan = true;
            }
            if (scanInfo.getScan() >= firstSelectedScan && scanInfo.getScan() <= lastSelectedScan)
            {
                selectedSeries.add(scanInfo.getScan(), scanInfo.getIntensity());
                area += scanInfo.getIntensity();
            }
            else
            {
                surroundingSeries.add(scanInfo.getScan(), scanInfo.getIntensity());
            }
        }
        if (!addedMinScan)
        {
            surroundingSeries.add(minScan, 0);
        }
        if (!addedMaxScan)
        {
            surroundingSeries.add(maxScan, 0);
        }
        _dataset.addSeries(selectedSeries);
        _chart.getXYPlot().getRenderer().setSeriesPaint(_dataset.getSeriesCount() - 1, color);
        _dataset.addSeries(surroundingSeries);
        _chart.getXYPlot().getRangeAxis().setLowerBound(0);
        _chart.getXYPlot().getRenderer().setSeriesPaint(_dataset.getSeriesCount() - 1, Color.LIGHT_GRAY);
    }

    public void render(OutputStream outputStream) throws IOException
    {
        _chart.getXYPlot().setForegroundAlpha(0.70f);
        ChartUtilities.writeChartAsPNG(outputStream, _chart, WIDTH, HEIGHT);
    }
}
