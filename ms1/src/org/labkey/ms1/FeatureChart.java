package org.labkey.ms1;

import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.labkey.api.data.Table;
import org.labkey.api.util.ResultSetUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;

/**
 * This class can create a feature chart of the requested type for the requested feature Id
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Oct 12, 2007
 * Time: 12:00:16 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class FeatureChart
{
    public static final String TYPE_MASS = "mass";

    public FeatureChart(int featureId, int runId, int scan, double mzLow, double mzHigh)
    {
        assert featureId >= 0 : "The featureId " + featureId + " passed to FeatureChart was invalid!";

        _featureId = featureId;
        _runId = runId;
        _scan = scan;
        _mzLow = mzLow;
        _mzHigh = mzHigh;
    }

    public int getFeatureId()
    {
        return _featureId;
    }

    public int getScan()
    {
        return _scan;
    }

    public double getMzLow()
    {
        return _mzLow;
    }

    public double getMzHigh()
    {
        return _mzHigh;
    }

    public void render(OutputStream out) throws SQLException, IOException
    {
        Table.TableResultSet rs = null;
        try
        {
            //get the scan/peak data for this feature
            rs = getChartData();

            //construct the chart
            JFreeChart chart = null;
            if(null != rs)
                chart = makeChart(rs);

            //render
            if(null != chart)
                ChartUtilities.writeChartAsPNG(out, chart, 425, 300);
        }
        finally
        {
            ResultSetUtil.close(rs);
        }
    }

    /**
     * Derived classes may override this to fetch a different dataset
     * @return The data to use for the chart
     * @throws SQLException Thrown if database error
     */
    protected Table.TableResultSet getChartData() throws SQLException
    {
        return MS1Manager.get().getPeakData(_runId, _scan, _mzLow, _mzHigh);
    }

    /**
     * Derived classes should override this to actually construct the chart. The calling
     * method will then write it out to the browser.
     *
     * @param rs The data for the chart
     * @return A constructed and populated JFreeChart
     * @throws SQLException Thrown if database error
     */
    protected abstract JFreeChart makeChart(Table.TableResultSet rs) throws SQLException;

    private int _featureId = -1;
    private int _runId = -1;
    private int _scan = 0;
    private double _mzLow = 0;
    private double _mzHigh = 0;
}
