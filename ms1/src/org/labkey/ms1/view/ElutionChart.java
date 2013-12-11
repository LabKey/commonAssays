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
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.labkey.api.data.TableResultSet;
import org.labkey.ms1.MS1Manager;
import org.labkey.ms1.model.MinMaxScanInfo;

import java.awt.*;
import java.sql.SQLException;
import java.text.DecimalFormat;

/**
 * Creates the elution line chart in the features detail view
 * User: Dave
 * Date: Oct 15, 2007
 * Time: 4:21:09 PM
 */
public class ElutionChart extends FeatureChart
{
    public ElutionChart(int runId, double mzLow, double mzHigh, int scanFirst, int scanLast)
    {
        _runId = runId;
        _mzLow = mzLow;
        _mzHigh = mzHigh;
        _scanFirst = scanFirst;
        _scanLast = scanLast;
    }

    protected TableResultSet getChartData()
    {
        return MS1Manager.get().getPeakData(_runId, _mzLow, _mzHigh, _scanFirst, _scanLast);
    }

    protected JFreeChart makeChart(TableResultSet rs) throws SQLException
    {
        //get the actual min/max scan info for the requested scan range
        //we need this to set the range of the Y axes properly
        MinMaxScanInfo mmsi = MS1Manager.get().getMinMaxScanRT(_runId, _scanFirst, _scanLast);

        XYSeries series = new XYSeries("Elution");
        while(rs.next())
            series.add(rs.getDouble("Scan"), rs.getDouble("Intensity"));

        XYSeriesCollection dataset = new XYSeriesCollection(series);

        JFreeChart chart = ChartFactory.createXYLineChart("Elution for Scans " + mmsi.getMinScan() + " through " + mmsi.getMaxScan(),
                                                            "Scan", "Intensity", dataset, PlotOrientation.HORIZONTAL,
                                                            false, false, false);

        XYPlot plot = chart.getXYPlot();

        //make the stroke a bit heavier than default
        plot.getRenderer().setStroke(new BasicStroke(1.5f));

        NumberAxis scanAxis = plot.getDomainAxis(0) instanceof NumberAxis ? (NumberAxis)plot.getDomainAxis(0) : null;
        if(null != scanAxis)
        {
            scanAxis.setRangeWithMargins(mmsi.getMinScan(), mmsi.getMaxScan());
            scanAxis.setNumberFormatOverride(new DecimalFormat("0"));

            //create a secondary axis for the retention times
            NumberAxis rtAxis = new NumberAxis("Retention Time");
            rtAxis.setAutoRangeIncludesZero(false);
            rtAxis.setRangeWithMargins(mmsi.getMinRetentionTime(), mmsi.getMaxRetentionTime());

            plot.setDomainAxis(1, rtAxis);
        }
        return chart;
    }

    private int _runId = -1;
    private double _mzLow = 0;
    private double _mzHigh = 0;
    private int _scanFirst = 0;
    private int _scanLast = 0;
}
