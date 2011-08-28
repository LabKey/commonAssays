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
    private Double _ec50;
    private Double _auc;
    private Double _minAsymptote;
    private Double _maxAsymptote;
    private Double _asymmetry;
    private Double _slope;
    private Double _inflection;

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

    public Double getEC50()
    {
        return _ec50;
    }

    public void setEC50(Double ec50)
    {
        _ec50 = ec50;
    }

    public Double getAUC()
    {
        return _auc;
    }

    public void setAUC(Double auc)
    {
        _auc = auc;
    }

    public Double getMinAsymptote()
    {
        return _minAsymptote;
    }

    public void setMinAsymptote(Double minAsymptote)
    {
        _minAsymptote = minAsymptote;
    }

    public Double getMaxAsymptote()
    {
        return _maxAsymptote;
    }

    public void setMaxAsymptote(Double maxAsymptote)
    {
        _maxAsymptote = maxAsymptote;
    }

    public Double getAsymmetry()
    {
        return _asymmetry;
    }

    public void setAsymmetry(Double asymmetry)
    {
        _asymmetry = asymmetry;
    }

    public Double getSlope()
    {
        return _slope;
    }

    public void setSlope(Double slope)
    {
        _slope = slope;
    }

    public Double getInflection()
    {
        return _inflection;
    }

    public void setInflection(Double inflection)
    {
        _inflection = inflection;
    }
}
