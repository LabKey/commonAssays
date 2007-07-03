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

