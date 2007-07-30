package org.labkey.luminex;

/**
 * User: jeckels
 * Date: Jun 26, 2007
 */
public class LuminexDataRow
{
    private int _analyteId;

    private String _type;
    private String _well;
    private boolean _outlier;
    private String _description;
    private Double _fi;
    private Double _fiBackground;
    private Double _stdDev;
    private Double _percentCV;
    private String _obsConcString;
    private Double _obsConc;
    private String _obsConcOORIndicator;
    private Double _expConc;
    private Double _obsOverExp;
    private String _concInRangeString;
    private Double _concInRange;
    private String _concInRangeOORIndicator;
    private int _dataId;

    public LuminexDataRow()
    {
    }

    public int getAnalyteId()
    {
        return _analyteId;
    }

    public void setAnalyteId(int analyteId)
    {
        _analyteId = analyteId;
    }

    public String getType()
    {
        return _type;
    }

    public void setType(String type)
    {
        _type = type;
    }

    public String getWell()
    {
        return _well;
    }

    public void setWell(String well)
    {
        _well = well;
    }

    public boolean isOutlier()
    {
        return _outlier;
    }

    public void setOutlier(boolean outlier)
    {
        _outlier = outlier;
    }

    public String getDescription()
    {
        return _description;
    }

    public void setDescription(String description)
    {
        _description = description;
    }

    public Double getFi()
    {
        return _fi;
    }

    public void setFi(Double fi)
    {
        _fi = fi;
    }

    public Double getFiBackground()
    {
        return _fiBackground;
    }

    public void setFiBackground(Double fiBackground)
    {
        _fiBackground = fiBackground;
    }

    public Double getStdDev()
    {
        return _stdDev;
    }

    public void setStdDev(Double stdDev)
    {
        _stdDev = stdDev;
    }

    public Double getPercentCV()
    {
        return _percentCV;
    }

    public void setPercentCV(Double percentCV)
    {
        _percentCV = percentCV;
    }

    public String getObsConcString()
    {
        return _obsConcString;
    }

    public void setObsConcString(String obsConcString)
    {
        _obsConcString = obsConcString;
    }

    public Double getObsConc()
    {
        return _obsConc;
    }

    public void setObsConc(Double obsConc)
    {
        _obsConc = obsConc;
    }

    public String getObsConcOORIndicator()
    {
        return _obsConcOORIndicator;
    }

    public void setObsConcOORIndicator(String obsConcOORIndicator)
    {
        _obsConcOORIndicator = obsConcOORIndicator;
    }

    public Double getExpConc()
    {
        return _expConc;
    }

    public void setExpConc(Double expConc)
    {
        _expConc = expConc;
    }

    public Double getObsOverExp()
    {
        return _obsOverExp;
    }

    public void setObsOverExp(Double obsOverExp)
    {
        _obsOverExp = obsOverExp;
    }

    public String getConcInRangeString()
    {
        return _concInRangeString;
    }

    public void setConcInRangeString(String concInRangeString)
    {
        _concInRangeString = concInRangeString;
    }

    public Double getConcInRange()
    {
        return _concInRange;
    }

    public void setConcInRange(Double concInRange)
    {
        _concInRange = concInRange;
    }

    public String getConcInRangeOORIndicator()
    {
        return _concInRangeOORIndicator;
    }

    public void setConcInRangeOORIndicator(String concInRangeOORIndicator)
    {
        _concInRangeOORIndicator = concInRangeOORIndicator;
    }

    public void setDataId(int dataId)
    {
        _dataId = dataId;
    }

    public int getDataId()
    {
        return _dataId;
    }
}
