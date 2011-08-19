package org.labkey.luminex;

/**
 * User: jeckels
 * Date: Aug 17, 2011
 */
public class CurveFit
{
    private int _rowId;
    private int _titrationId;
    private int _analyteId;
    private String _curveType;
    private double _maxFI;
    private double _ec50;
    private double _auc;

    public int getRowId()
    {
        return _rowId;
    }

    public void setRowId(int rowId)
    {
        _rowId = rowId;
    }

    public int getTitrationId()
    {
        return _titrationId;
    }

    public void setTitrationId(int titrationId)
    {
        _titrationId = titrationId;
    }

    public int getAnalyteId()
    {
        return _analyteId;
    }

    public void setAnalyteId(int analyteId)
    {
        _analyteId = analyteId;
    }

    public String getCurveType()
    {
        return _curveType;
    }

    public void setCurveType(String curveType)
    {
        _curveType = curveType;
    }

    public double getMaxFI()
    {
        return _maxFI;
    }

    public void setMaxFI(double maxFI)
    {
        _maxFI = maxFI;
    }

    public double getEC50()
    {
        return _ec50;
    }

    public void setEC50(double ec50)
    {
        _ec50 = ec50;
    }

    public double getAUC()
    {
        return _auc;
    }

    public void setAUC(double auc)
    {
        _auc = auc;
    }
}
