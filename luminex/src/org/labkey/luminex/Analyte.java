package org.labkey.luminex;

/**
 * User: jeckels
* Date: Jul 26, 2007
*/
public class Analyte
{
    private int _dataId;
    private String _name;
    private double _fitProb;
    private double _resVar;
    private String _regressionType;
    private String _stdCurve;
    private int _rowId;
    private int _minStandardRecovery;
    private int _maxStandardRecovery;

    public Analyte()
    {
    }

    public Analyte(String name, int dataId)
    {
        _name = name;
        _dataId = dataId;
    }

    public int getDataId()
    {
        return _dataId;
    }

    public String getName()
    {
        return _name;
    }

    public void setDataId(int dataId)
    {
        _dataId = dataId;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public void setFitProb(double fitProb)
    {
        _fitProb = fitProb;
    }

    public void setResVar(double resVar)
    {
        _resVar = resVar;
    }

    public void setRegressionType(String regressionType)
    {
        _regressionType = regressionType;
    }

    public void setStdCurve(String stdCurve)
    {
        _stdCurve = stdCurve;
    }

    public int getRowId()
    {
        return _rowId;
    }

    public double getFitProb()
    {
        return _fitProb;
    }

    public double getResVar()
    {
        return _resVar;
    }

    public String getRegressionType()
    {
        return _regressionType;
    }

    public String getStdCurve()
    {
        return _stdCurve;
    }

    public void setRowId(int rowId)
    {
        _rowId = rowId;
    }

    public int getMinStandardRecovery()
    {
        return _minStandardRecovery;
    }

    public void setMinStandardRecovery(int minStandardRecovery)
    {
        _minStandardRecovery = minStandardRecovery;
    }

    public int getMaxStandardRecovery()
    {
        return _maxStandardRecovery;
    }

    public void setMaxStandardRecovery(int maxStandardRecovery)
    {
        _maxStandardRecovery = maxStandardRecovery;
    }
}
