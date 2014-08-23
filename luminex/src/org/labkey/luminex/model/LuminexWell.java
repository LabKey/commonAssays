/*
 * Copyright (c) 2011-2014 LabKey Corporation
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
package org.labkey.luminex.model;

import org.labkey.api.study.Plate;
import org.labkey.api.study.WellData;

/**
 * User: jeckels
 * Date: Aug 17, 2011
 */
public class LuminexWell implements WellData, Comparable<LuminexWell>
{
    public LuminexDataRow _dataRow;

    public LuminexWell(LuminexDataRow dataRow)
    {
        _dataRow = dataRow;
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
        return _dataRow.getDilution();
    }

    public Double getDose()
    {
        // for standards, use the expected conc values for the curve fit
        // for non-standard titrations, use the dilution values for the curve fit
        if (_dataRow.getType() != null)
        {
            String type = _dataRow.getType().trim().toLowerCase();
            if (type.startsWith("s") || type.startsWith("es"))
                return _dataRow.getExpConc();
            else
                return _dataRow.getDilution();
        }
        else
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

    public Double getAucValue(String curveFitInput)
    {
        // return the FI column based on the run property for which curve fit input to use for AUC calc (default is FI-Bkgd)
        if (curveFitInput != null)
        {
            if (curveFitInput.equals("FI"))
                return _dataRow.getFi();
            else if (curveFitInput.equals("FI-Bkgd-Blank") || curveFitInput.equals("FI-Bkgd-Neg"))
                return _dataRow.getFiBackgroundNegative();
            else
                return _dataRow.getFiBackground();
        }
        else
            return getValue();
    }

    @Override
    public double getMax()
    {
        Double result = getValue();
        return result == null ? Double.NEGATIVE_INFINITY : result.doubleValue();
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
        if (getDose() == null && w.getDose() == null)
        {
            return 0;
        }
        if (getDose() == null)
        {
            return -1;
        }
        if (w.getDose() == null)
        {
            return 1;
        }
        return getDose().compareTo(w.getDose());
    }
}
