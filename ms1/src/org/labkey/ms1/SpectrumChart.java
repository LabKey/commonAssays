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
    public SpectrumChart(int featureId, int runId, int scan, double mzLow, double mzHigh)
    {
        super(featureId, runId, scan, mzLow, mzHigh);
    }

    protected JFreeChart makeChart(Table.TableResultSet rs) throws SQLException
    {
        XYSeries series = new XYSeries("Spectrum");

        while(rs.next())
            series.add(rs.getDouble("MZ"), rs.getDouble("Intensity"));

        XYSeriesCollection dataset = new XYSeriesCollection(series);
        dataset.setIntervalWidth(0); //this controls the width of the bars, which we want to be very thin
        
        return ChartFactory.createXYBarChart("Spectrum for Scan " + getScan(), "m/z", false, "Intensity",
                                                        dataset, PlotOrientation.VERTICAL,
                                                        false, false, false);

    }
}
