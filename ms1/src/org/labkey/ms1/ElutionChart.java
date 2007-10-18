package org.labkey.ms1;

import org.labkey.api.data.Table;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.sql.SQLException;
import java.awt.*;

/**
 * Creates the elution line chart in the features detail view
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Oct 15, 2007
 * Time: 4:21:09 PM
 * To change this template use File | Settings | File Templates.
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

    protected Table.TableResultSet getChartData() throws SQLException
    {
        return MS1Manager.get().getPeakData(_runId, _mzLow, _mzHigh, _scanFirst, _scanLast);
    }

    protected JFreeChart makeChart(Table.TableResultSet rs) throws SQLException
    {
        XYSeries series = new XYSeries("Elution");
        while(rs.next())
            series.add(rs.getDouble("RetentionTime"), rs.getDouble("Intensity"));

        XYSeriesCollection dataset = new XYSeriesCollection(series);

        JFreeChart chart = ChartFactory.createXYLineChart("Elution for Scans " + _scanFirst + " through " + _scanLast,
                                                "Retention Time", "Intensity", dataset, PlotOrientation.HORIZONTAL,
                                                        false, false, false);
        
        chart.getXYPlot().getRenderer().setStroke(new BasicStroke(1.5f));
        return chart;
    }

    private int _runId = -1;
    private double _mzLow = 0;
    private double _mzHigh = 0;
    private int _scanFirst = 0;
    private int _scanLast = 0;
}
