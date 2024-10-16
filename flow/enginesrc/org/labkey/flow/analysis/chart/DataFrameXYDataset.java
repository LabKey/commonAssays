/*
 * Copyright (c) 2011-2019 LabKey Corporation
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

package org.labkey.flow.analysis.chart;

import org.jfree.data.xy.AbstractXYDataset;
import org.labkey.flow.analysis.model.DataFrame;

import java.util.BitSet;

/**
 */
public class DataFrameXYDataset extends AbstractXYDataset
{
    private final DataFrame _data;
    private final int _xAxis;
    private final int _yAxis;

    public DataFrameXYDataset(DataFrame data, int xAxis, int yAxis)
    {
        _data = data;
        _xAxis = xAxis;
        _yAxis = yAxis;
    }

    @Override
    public int getSeriesCount()
    {
        return 1;
    }

    @Override
    public String getSeriesKey(int series)
    {
        assert series == 0;
        return "First series";
    }

    @Override
    public int getItemCount(int series)
    {
        assert series == 0;
        return _data.getRowCount();
    }

    @Override
    public Number getX(int series, int x)
    {
        assert series == 0;
        return Double.valueOf(_data.getColumn(_xAxis).getDouble(x));
    }

    @Override
    public Number getY(int series, int y)
    {
        assert series == 0;
        return Double.valueOf(_data.getColumn(_yAxis).getDouble(y));
    }
}
