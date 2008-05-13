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
