/*
 * Copyright (c) 2007-2013 LabKey Corporation
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

package org.labkey.ms1.view;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.labkey.api.data.TableResultSet;
import org.labkey.ms1.MS1Manager;

import java.awt.*;
import java.sql.SQLException;

/**
 * Can produce a spectrum chart for a given feature/scan
 *
 * User: Dave
 * Date: Oct 15, 2007
 * Time: 9:41:38 AM
 */
public class SpectrumChart extends FeatureChart
{
    public SpectrumChart(int runId, int scan, double mzLow, double mzHigh)
    {
        _runId = runId;
        _scan = scan;
        _mzHigh = mzHigh;
        _mzLow = mzLow;
    }

    protected TableResultSet getChartData()
    {
        return MS1Manager.get().getPeakData(_runId, _scan, _mzLow, _mzHigh);
    }

    protected JFreeChart makeChart(TableResultSet rs) throws SQLException
    {
        XYSeries series = new XYSeries("Spectrum");

        while(rs.next())
            series.add(rs.getDouble("MZ"), rs.getDouble("Intensity"));

        XYSeriesCollection dataset = new XYSeriesCollection(series);
        dataset.setIntervalWidth(0); //this controls the width of the bars, which we want to be very thin
        
        JFreeChart chart = ChartFactory.createXYBarChart("Intensity Spectrum for Scan " + _scan, "m/z", false, null,
                                                        dataset, PlotOrientation.VERTICAL,
                                                        false, false, false);

        chart.getXYPlot().getRenderer().setStroke(new BasicStroke(1.5f));
        chart.getXYPlot().getDomainAxis().setRangeWithMargins(_mzLow, _mzHigh);
        chart.getXYPlot().setRangeAxis(1, chart.getXYPlot().getRangeAxis());
        return chart;

    }
    
    private int _runId = -1;
    private int _scan = 0;
    private double _mzLow = 0;
    private double _mzHigh = 0;
}
