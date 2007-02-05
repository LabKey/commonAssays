package org.labkey.flow.analysis.chart;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.AbstractXYDataset;
import org.labkey.flow.analysis.model.DataFrame;
import org.labkey.flow.analysis.data.NumberArray;

/**
 */
public class DatasetFactory
{
    static public XYDataset createXYDataset(DataFrame data, int xColumn, int yColumn)
    {
        return new XYDatasetImpl(data.getColumn(xColumn), data.getColumn(yColumn));
    }

    static class XYDatasetImpl extends AbstractXYDataset
    {
        NumberArray _xValues;
        NumberArray _yValues;

        public XYDatasetImpl(NumberArray xValues, NumberArray yValues)
        {
            _xValues = xValues;
            _yValues = yValues;
        }

        public String getSeriesKey(int series)
        {
            return "First Series";
        }

        public int getSeriesCount()
        {
            return 1;
        }

        public int getItemCount(int series)
        {
            return _xValues.size();
        }

        public Number getX(int series, int item)
        {
            return new Double(_xValues.getDouble(item));
        }

        public Number getY(int series, int item)
        {
            return new Double(_yValues.getDouble(item));
        }
    }
}
