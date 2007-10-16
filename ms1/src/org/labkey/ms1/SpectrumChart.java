package org.labkey.ms1;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.labkey.api.data.Table;

import java.sql.SQLException;

/**
 * Can produce a spectrum chart for a given feature/scan
 * Created by IntelliJ IDEA.
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

    protected Table.TableResultSet getChartData() throws SQLException
    {
        return MS1Manager.get().getPeakData(_runId, _scan, _mzLow, _mzHigh);
    }

    protected JFreeChart makeChart(Table.TableResultSet rs) throws SQLException
    {
        XYSeries series = new XYSeries("Spectrum");

        while(rs.next())
            series.add(rs.getDouble("MZ"), rs.getDouble("Intensity"));

        XYSeriesCollection dataset = new XYSeriesCollection(series);
        dataset.setIntervalWidth(0); //this controls the width of the bars, which we want to be very thin
        
        return ChartFactory.createXYBarChart("Spectrum for Scan " + _scan, "m/z", false, "Intensity",
                                                        dataset, PlotOrientation.VERTICAL,
                                                        false, false, false);

    }
    
    private int _runId = -1;
    private int _scan = 0;
    private double _mzLow = 0;
    private double _mzHigh = 0;
}
