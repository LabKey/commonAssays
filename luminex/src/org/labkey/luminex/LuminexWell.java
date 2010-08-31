/*
 * Copyright (c) 2007-2010 LabKey Corporation
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

package org.labkey.luminex;

import org.labkey.api.study.Well;
import org.labkey.api.study.Plate;

/**
 * User: jeckels
 * Date: Jun 27, 2007
 */
public class LuminexWell implements Well
{
    private int _row;
    private int _column;
    
    public LuminexWell(int row, int column)
    {
        _row = row;
        _column = column;
    }

    public double getValue()
    {
        throw new UnsupportedOperationException();
    }

    public Plate getPlate()
    {
        throw new UnsupportedOperationException();
    }

    public double getStdDev()
    {
        throw new UnsupportedOperationException();
    }

    public double getMax()
    {
        throw new UnsupportedOperationException();
    }

    public double getMin()
    {
        throw new UnsupportedOperationException();
    }

    public double getMean()
    {
        throw new UnsupportedOperationException();
    }

    public Double getDilution()
    {
        throw new UnsupportedOperationException();
    }

    public void setDilution(Double dilution)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDescription()
    {
        throw new UnsupportedOperationException();
    }

    public int getColumn()
    {
        return _column;
    }

    public int getRow()
    {
        return _row;
    }
    
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LuminexWell that = (LuminexWell) o;

        if (_column != that._column) return false;
        if (_row != that._row) return false;

        return true;
    }

    public int hashCode()
    {
        int result;
        result = _row;
        result = 31 * result + _column;
        return result;
    }
}

