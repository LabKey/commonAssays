package org.labkey.luminex;

import org.labkey.api.study.Plate;
import org.labkey.api.study.WellData;

/**
 * User: jeckels
 * Date: Aug 17, 2011
 */
public class LuminexWell implements WellData
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
        return _dataRow.getExpConc() == null || _dataRow.getDilution() == null ? null : _dataRow.getDilution() / _dataRow.getExpConc();
    }

    @Override
    public void setDilution(Double dilution)
    {
        throw new UnsupportedOperationException();
    }

    private Double getValue()
    {
        if (_dataRow.getExtraProperties() != null)
        {
            Object value = _dataRow.getExtraProperties().get("fiBackgroundBlank");
            if (value instanceof Number)
            {
                return ((Number)value).doubleValue();
            }
        }
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
}
