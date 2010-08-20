package org.labkey.ms2;

/**
* User: jeckels
* Date: Aug 20, 2010
*/
public class ProteinQuantitation
{
    private int _proteinGroupId;
    private float _ratioMean;
    private float _ratioStandardDev;
    private int _ratioNumberPeptides;
    private float _heavy2lightRatioMean;
    private float _heavy2lightRatioStandardDev;

    public int getProteinGroupId()
    {
        return _proteinGroupId;
    }

    public void setProteinGroupId(int proteinGroupId)
    {
        _proteinGroupId = proteinGroupId;
    }

    public float getRatioMean()
    {
        return _ratioMean;
    }

    public void setRatioMean(float ratioMean)
    {
        _ratioMean = ratioMean;
    }

    public float getRatioStandardDev()
    {
        return _ratioStandardDev;
    }

    public void setRatioStandardDev(float ratioStandardDev)
    {
        _ratioStandardDev = ratioStandardDev;
    }

    public int getRatioNumberPeptides()
    {
        return _ratioNumberPeptides;
    }

    public void setRatioNumberPeptides(int ratioNumberPeptides)
    {
        _ratioNumberPeptides = ratioNumberPeptides;
    }

    public float getHeavy2lightRatioMean()
    {
        return _heavy2lightRatioMean;
    }

    public void setHeavy2lightRatioMean(float heavy2lightRatioMean)
    {
        _heavy2lightRatioMean = heavy2lightRatioMean;
    }

    public float getHeavy2lightRatioStandardDev()
    {
        return _heavy2lightRatioStandardDev;
    }

    public void setHeavy2lightRatioStandardDev(float heavy2lightRatioStandardDev)
    {
        _heavy2lightRatioStandardDev = heavy2lightRatioStandardDev;
    }
}
