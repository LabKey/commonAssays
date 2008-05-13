/*
 * Copyright (c) 2005-2008 LabKey Corporation
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
