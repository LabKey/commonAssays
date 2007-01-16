package org.labkey.flow.analysis.chart;

import org.jfree.data.xy.AbstractXYDataset;
import org.labkey.flow.analysis.model.DataFrame;

import java.util.BitSet;

/**
 */
public class DataFrameXYDataset extends AbstractXYDataset
    {
    DataFrame _data;
    int _xAxis;
    int _yAxis;

    public DataFrameXYDataset(DataFrame data, int xAxis, int yAxis)
        {
        _data = data;
        this._xAxis = xAxis;
        this._yAxis = yAxis;
        }

    public int getSeriesCount()
        {
        return 1;
        }

    public String getSeriesKey(int series)
        {
        assert series == 0;
        return "First series";
        }

    public int getItemCount(int series)
        {
        assert series == 0;
        return _data.getRowCount();
        }

    public Number getX(int series, int x)
        {
        assert series == 0;
        return new Double(_data.getColumn(_xAxis).getDouble(x));
        }

    public Number getY(int series, int y)
        {
        assert series == 0;
        return new Double(_data.getColumn(_yAxis).getDouble(y));
        }
    }
