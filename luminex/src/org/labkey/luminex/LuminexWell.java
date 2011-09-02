/*
 * Copyright (c) 2011 LabKey Corporation
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

import org.labkey.api.study.Plate;
import org.labkey.api.study.WellData;

/**
 * User: jeckels
 * Date: Aug 17, 2011
 */
public class LuminexWell implements WellData, Comparable<LuminexWell>
{
    private static final double TOMARAS_SCALE_DILUTION = 50000.0;

    public LuminexDataRow _dataRow;
    private double _scaleDilutionFactor;

    public LuminexWell(LuminexDataRow dataRow, double scaleDilutionFactor)
    {
        _dataRow = dataRow;
        _scaleDilutionFactor = scaleDilutionFactor;
    }

    public LuminexWell(LuminexDataRow dataRow)
    {
        this(dataRow, TOMARAS_SCALE_DILUTION);
    }

    @Override
    public Plate getPlate()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getStdDev()
    {
        return _dataRow.getStdDev() == null ? 0 : _dataRow.getStdDev().doubleValue();
    }

    @Override
    public Double getDilution()
    {
        if (_dataRow.getExpConc() != null && _dataRow.getDilution() != null)
        {
            return (_dataRow.getDilution() / _dataRow.getExpConc()) * _scaleDilutionFactor;
        }
        if (_dataRow.getDilution() != null)
        {
            return _dataRow.getDilution() * _scaleDilutionFactor;
        }
        if (_dataRow.getExpConc() != null && _dataRow.getExpConc() != 0.0)
        {
            return 1.0 / _dataRow.getExpConc() * _scaleDilutionFactor;
        }
        return null;
    }

    @Override
    public void setDilution(Double dilution)
    {
        throw new UnsupportedOperationException();
    }

    public Double getValue()
    {
        return _dataRow.getFiBackground();
    }

    @Override
    public double getMax()
    {
        Double result = getValue();
        return result == null ? Double.MIN_VALUE : result.doubleValue();
    }

    @Override
    public double getMin()
    {
        Double result = getValue();
        return result == null ? Double.MAX_VALUE : result.doubleValue();
    }

    @Override
    public double getMean()
    {
        return getMax();
    }

    public LuminexDataRow getDataRow()
    {
        return _dataRow;
    }

    @Override
    public int compareTo(LuminexWell w)
    {
        if (getDilution() == null && w.getDilution() == null)
        {
            return 0;
        }
        if (getDilution() == null)
        {
            return -1;
        }
        if (w.getDilution() == null)
        {
            return 1;
        }
        return getDilution().compareTo(w.getDilution());
    }
}
