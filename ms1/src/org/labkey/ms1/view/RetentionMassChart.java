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
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBubbleRenderer;
import org.jfree.data.xy.AbstractXYZDataset;
import org.labkey.api.data.TableResultSet;
import org.labkey.ms1.MS1Manager;
import org.labkey.ms1.model.MinMaxScanInfo;

import java.awt.*;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Produces the retention time vs. m/z chart for the feature details view
 *
 * User: Dave
 * Date: Oct 15, 2007
 * Time: 12:44:30 PM
 */
public class RetentionMassChart extends FeatureChart
{
    public RetentionMassChart(int runId, double mzLow, double mzHigh, int scanFirst, int scanLast, int scanCur)
    {
        _runId = runId;
        _mzLow = mzLow;
        _mzHigh = mzHigh;
        _scanFirst = scanFirst;
        _scanLast = scanLast;
        _scanCur = scanCur;
    }


    protected TableResultSet getChartData()
    {
        return MS1Manager.get().getPeakData(_runId, _mzLow, _mzHigh, _scanFirst, _scanLast);
    }

    protected JFreeChart makeChart(TableResultSet rs) throws SQLException
    {
        IntensityXYZSeries seriesCurScan = new IntensityXYZSeries("Current Scan (" + _scanCur + ")");
        IntensityXYZSeries seriesRest = new IntensityXYZSeries("Other Scans");

        //get the actual min/max scan info for the requested scan range
        //we need this to set the range of the Y axes properly
        MinMaxScanInfo mmsi = MS1Manager.get().getMinMaxScanRT(_runId, _scanFirst, _scanLast);

        Double x,y,z;
        while(rs.next())
        {
            x = rs.getDouble("MZ");
            if(rs.wasNull())
                x = null;
            y = rs.getDouble("Scan");
            if(rs.wasNull())
                y = null;
            z = rs.getDouble("Intensity");
            if(rs.wasNull())
                z = null;

            if(rs.getInt("Scan") == _scanCur)
                seriesCurScan.addPoint(x, y, z);
            else
                seriesRest.addPoint(x, y, z);
        }

        IntensityXYZDataset dataset = new IntensityXYZDataset(seriesCurScan, seriesRest);
        JFreeChart chart = ChartFactory.createBubbleChart("Intensities for Scans " + mmsi.getMinScan() + " through " + mmsi.getMaxScan(),
                                                            "m/z", "Scan", dataset, PlotOrientation.VERTICAL,
                                                            true, false, false);

        XYPlot plot = chart.getXYPlot();

        //manually set the range of the m/z axis to our m/z window
        plot.getDomainAxis().setRangeWithMargins(_mzLow, _mzHigh);

        NumberAxis scanAxis = plot.getRangeAxis(0) instanceof NumberAxis ? (NumberAxis)plot.getRangeAxis(0) : null;
        if(null != scanAxis)
        {
            scanAxis.setNumberFormatOverride(new DecimalFormat("0"));
            scanAxis.setRangeWithMargins(mmsi.getMinScan(), mmsi.getMaxScan());
            dataset.setMinY(mmsi.getMinScan());
            dataset.setMaxY(mmsi.getMaxScan());

            //create a secondary axis for the retention times
            NumberAxis rtAxis = new NumberAxis("Retention Time");
            rtAxis.setAutoRangeIncludesZero(false);
            rtAxis.setRangeWithMargins(mmsi.getMinRetentionTime(), mmsi. getMaxRetentionTime());

            //add the scan axis
            plot.setRangeAxis(1, rtAxis);
        }

        //use our custom renderer to get color-mapped bubbles
        plot.setRenderer(new IntensityBubbleRenderer());

        return chart;

    }

    private static class IntensityBubbleRenderer extends XYBubbleRenderer
    {
        public IntensityBubbleRenderer()
        {
            super(SCALE_ON_RANGE_AXIS);
        }

        /**
         * Returns the Paint to use for the item fill color
         * @param row the series index
         * @param column the item in the series
         * @return The paint
         */
        public Paint getItemPaint(int row, int column)
        {
            IntensityXYZDataset dataset = null;
            if(getPlot().getDataset() instanceof IntensityXYZDataset)
                dataset = (IntensityXYZDataset)(getPlot().getDataset());

            if(null == dataset)
                return super.getItemPaint(row, column);

            Double z = dataset.getRealZ(row, column);
            if(null == z)
                return super.getItemPaint(row, column);

            //get the real max z
            double zMax = dataset.getMaxRealZ(row);
            
            //get the series default color
            Color defColor;
            if(getSeriesPaint(row) instanceof Color)
                defColor = (Color)getSeriesPaint(row);
            else
                defColor = row == 0 ? Color.RED : Color.BLUE;

            //return a color interpolated between white and the series default color
            double percent = z / zMax;
            double r = Color.WHITE.getRed() + ((defColor.getRed() - Color.WHITE.getRed()) * percent);
            double g = Color.WHITE.getGreen() + ((defColor.getGreen() - Color.WHITE.getGreen()) * percent);
            double b = Color.WHITE.getBlue() + ((defColor.getBlue() - Color.WHITE.getBlue()) * percent);

            return new Color((int)r, (int)g, (int)b);
        }


        public Paint getItemOutlinePaint(int row, int column)
        {
            return super.getItemOutlinePaint(row, column);    //To change body of overridden methods use File | Settings | File Templates.
        }
    }

    /**
     * Represents a single data item containing x, y, and z coordinates
     */
    private static class XYZDataItem
    {
        public XYZDataItem(Double x, Double y, Double z)
        {
            _x = x;
            _y = y;
            _z = z;
        }
        public final Double getX()
        {
            return _x;
        }

        public final Double getY()
        {
            return _y;
        }

        public final Double getZ()
        {
            return _z;
        }

        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            XYZDataItem that = (XYZDataItem) o;

            if (_x != null ? !_x.equals(that._x) : that._x != null) return false;
            if (_y != null ? !_y.equals(that._y) : that._y != null) return false;
            if (_z != null ? !_z.equals(that._z) : that._z != null) return false;

            return true;
        }

        public int hashCode()
        {
            int result;
            result = (_x != null ? _x.hashCode() : 0);
            result = 31 * result + (_y != null ? _y.hashCode() : 0);
            result = 31 * result + (_z != null ? _z.hashCode() : 0);
            return result;
        }

        private final Double _x;
        private final Double _y;
        private final Double _z;
    }

    /**
     * Holds XYZ data values, but can also return the Z value logarithmically
     * scaled to the corresponding Y value. This is useful for bubble charts
     * where the bubble size is always scaled to the Y axis. In our use above
     * the real Z value is then used for the bubble color.
     */
    private static class IntensityXYZSeries
    {
        private ArrayList<XYZDataItem> _vals = new ArrayList<>();
        private final LogarithmicAxis logaxis = new LogarithmicAxis("temp");
        private double _minY = Double.MAX_VALUE;
        private double _maxY = Double.MIN_VALUE;
        private double _maxZ = Double.MIN_VALUE;
        private double _maxLogZ = Double.MIN_VALUE;
        private String _key;
        private IntensityXYZDataset _dataset;           //parent dataset

        public IntensityXYZSeries(String key)
        {
            _key = key;
        }

        public String getKey()
        {
            return _key;
        }

        public void addPoint(Double x, Double y, Double z)
        {
            addPoint(new XYZDataItem(x,y,z));
        }

        public void addPoint(XYZDataItem item)
        {
            if(null != item.getY())
            {
                _minY = Math.min(_minY, item.getY().doubleValue());
                _maxY = Math.max(_maxY, item.getY().doubleValue());
            }
            if(null != item.getZ())
            {
                _maxZ = Math.max(_maxZ, item.getZ().doubleValue());
                _maxLogZ = Math.max(_maxLogZ, logaxis.adjustedLog10(item.getZ().doubleValue()));
            }

            _vals.add(item);
        }

        public int getItemCount()
        {
            return _vals.size();
        }

        public Number getX(int item)
        {
            return _vals.get(item).getX();
        }

        public Number getY(int item)
        {
            return _vals.get(item).getY();
        }

        /**
         * Returns the z value logarithmically scaled to its
         * corresponding y value so that the bubble size will be
         * reasonable within the chart.
         * @param item  the index of the XYZ data item
         * @return  the scaled number
         */
        public Number getZ(int item)
        {
            //scale the Z values so that they will be a reasonable size
            //comapred to the Y axis, which is what the chart uses to
            //calculate the bubble diameter
            Double z = _vals.get(item).getZ();
            IntensityXYZDataset dataset = getDataset();
            return null == z || null == dataset ? null : 
                            (logaxis.adjustedLog10(z.doubleValue()) / dataset.getMaxLogZ())
                                        * (dataset.getMaxY() - dataset.getMinY()) * .07;
        }

        /**
         * Returns the actual z value for the data item
         * @param item the XYZ data item
         * @return the actual z value
         */
        public Double getRealZ(int item)
        {
            return _vals.get(item).getZ();
        }

        /**
         * Returns the maximum actual z value for the given series
         * @return the maximum actual z value for the series
         */
        public double getMaxRealZ()
        {
            return _maxZ;
        }

        public double getMaxLogZ()
        {
            return _maxLogZ;
        }

        public double getMaxY()
        {
            return _maxY;
        }

        public double getMinY()
        {
            return _minY;
        }

        public double getMaxZ()
        {
            return _maxZ;
        }

        public IntensityXYZDataset getDataset()
        {
            return _dataset;
        }

        public void setDataset(IntensityXYZDataset dataset)
        {
            _dataset = dataset;
        }
    } //class IntensityXYZSeries

    /**
     * Provides an XYZ dataset for JFreeChart containing a number
     * of IntensityXYZSeries
     */
    private static class IntensityXYZDataset extends AbstractXYZDataset
    {
        private ArrayList<IntensityXYZSeries> _series = new ArrayList<>();

        public IntensityXYZDataset(IntensityXYZSeries... series)
        {
            for(IntensityXYZSeries s : series)
            {
                _series.add(s);
                s.setDataset(this);
                _minY = Math.min(_minY, s.getMinY());
                _maxY = Math.max(_maxY, s.getMaxY());
                _maxLogZ = Math.max(_maxLogZ, s.getMaxLogZ());
            }
        }

        public void addSeries(IntensityXYZSeries series)
        {
            _series.add(series);
        }

        public int getSeriesCount()
        {
            return _series.size();
        }

        public Comparable getSeriesKey(int series)
        {
            return _series.get(series).getKey();
        }

        public int getItemCount(int series)
        {
            return _series.get(series).getItemCount();
        }

        public Number getX(int series, int item)
        {
            return _series.get(series).getX(item);
        }

        public Number getY(int series, int item)
        {
            return _series.get(series).getY(item);
        }

        /**
         * Returns the z value logarithmically scaled to its
         * corresponding y value so that the bubble size will be
         * reasonable within the chart.
         * @param series the series
         * @param item  the index of the XYZ data item
         * @return  the scaled number
         */
        public Number getZ(int series, int item)
        {
            return _series.get(series).getZ(item);
        }

        /**
         * Returns the actual z value for the data item
         * @param series the series
         * @param item the XYZ data item
         * @return the actual z value
         */
        public Double getRealZ(int series, int item)
        {
            return _series.get(series).getRealZ(item);
        }

        /**
         * Returns the maximum actual z value for the given series
         * @param series the series
         * @return the maximum actual z value for the series
         */
        public double getMaxRealZ(int series)
        {
            return _series.get(series).getMaxRealZ();
        }

        public double getMaxLogZ()
        {
            return _maxLogZ;
        }

        public double getMaxY()
        {
            return _maxY;
        }

        public double getMinY()
        {
            return _minY;
        }

        public void setMinY(double minY)
        {
            _minY = minY;
        }

        public void setMaxY(double maxY)
        {
            _maxY = maxY;
        }

        private double _minY = Double.MAX_VALUE;
        private double _maxY = Double.MIN_VALUE;
        private double _maxLogZ = Double.MIN_VALUE;
    }

    private int _runId = -1;
    private double _mzLow = 0;
    private double _mzHigh = 0;
    private int _scanFirst = 0;
    private int _scanLast = 0;
    private int _scanCur = 0;

}
