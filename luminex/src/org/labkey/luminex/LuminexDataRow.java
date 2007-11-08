package org.labkey.luminex;

import java.util.Date;

/**
 * User: jeckels
 * Date: Jun 26, 2007
 */
public class LuminexDataRow
{
    private int _analyteId;
    private int _rowId;

    private String _type;
    private String _well;
    private boolean _outlier;
    private String _description;
    private String _ptid;
    private Double _visitID;
    private Date _date;
    private String _fiString;
    private Double _fi;
    private String _fiOORIndicator;
    private String _fiBackgroundString;
    private Double _fiBackground;
    private String _fiBackgroundOORIndicator;
    private String _stdDevString;
    private Double _stdDev;
    private String _stdDevOORIndicator;
    private String _obsConcString;
    private Double _obsConc;
    private String _obsConcOORIndicator;
    private Double _expConc;
    private Double _obsOverExp;
    private String _concInRangeString;
    private Double _concInRange;
    private String _concInRangeOORIndicator;
    private int _dataId;
    private Double _dilution;
    private String _dataRowGroup;
    private String _ratio;
    private String _samplingErrors;

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

    public int getRowId()
    {
        return _rowId;
    }

    public void setRowId(int rowId)
    {
        _rowId = rowId;
    }
    
    public String getFiString()
    {
        return _fiString;
    }

    public void setFiString(String fiString)
    {
        _fiString = fiString;
    }

    public String getFiOORIndicator()
    {
        return _fiOORIndicator;
    }

    public void setFiOORIndicator(String fiOORIndicator)
    {
        _fiOORIndicator = fiOORIndicator;
    }

    public String getFiBackgroundString()
    {
        return _fiBackgroundString;
    }

    public void setFiBackgroundString(String fiBackgroundString)
    {
        _fiBackgroundString = fiBackgroundString;
    }

    public String getFiBackgroundOORIndicator()
    {
        return _fiBackgroundOORIndicator;
    }

    public void setFiBackgroundOORIndicator(String fiBackgroundOORIndicator)
    {
        _fiBackgroundOORIndicator = fiBackgroundOORIndicator;
    }

    public String getStdDevString()
    {
        return _stdDevString;
    }

    public void setStdDevString(String stdDevString)
    {
        _stdDevString = stdDevString;
    }

    public String getStdDevOORIndicator()
    {
        return _stdDevOORIndicator;
    }

    public void setStdDevOORIndicator(String stdDevOORIndicator)
    {
        _stdDevOORIndicator = stdDevOORIndicator;
    }

    public Double getDilution()
    {
        return _dilution;
    }

    public void setDilution(Double dilution)
    {
        _dilution = dilution;
    }

    public String getDataRowGroup()
    {
        return _dataRowGroup;
    }

    public void setDataRowGroup(String dataRowGroup)
    {
        _dataRowGroup = dataRowGroup;
    }

    public String getRatio()
    {
        return _ratio;
    }

    public void setRatio(String ratio)
    {
        _ratio = ratio;
    }

    public String getSamplingErrors()
    {
        return _samplingErrors;
    }

    public void setSamplingErrors(String samplingErrors)
    {
        _samplingErrors = samplingErrors;
    }

    public String getPtid()
    {
        return _ptid;
    }

    public void setPtid(String ptid)
    {
        _ptid = ptid;
    }

    public Double getVisitID()
    {
        return _visitID;
    }

    public void setVisitID(Double visitID)
    {
        _visitID = visitID;
    }

    public Date getDate()
    {
        return _date;
    }

    public void setDate(Date date)
    {
        _date = date;
    }
}
