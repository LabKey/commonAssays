/*
 * Copyright (c) 2007-2014 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.Container;
import org.labkey.api.data.ConvertHelper;
import org.labkey.api.data.ObjectFactory;
import org.labkey.luminex.query.LuminexDataTable;

import java.util.Date;
import java.util.Map;

/**
 * User: jeckels
 * Date: Jun 26, 2007
 */
public class LuminexDataRow
{
    private int _analyte;
    private int _rowId;

    private Map<String, Object> _extraProperties = new CaseInsensitiveHashMap<>();

    private String _lsid;
    private String _type;
    private String _wellRole;
    private String _well;
    private int _outlier;
    private String _description;
    private String _specimenID;
    private String _participantID;
    private Double _visitID;
    private Date _date;
    private String _fiString;
    private Double _fi;
    private String _fiOORIndicator;
    private String _fiBackgroundString;
    private Double _fiBackground;
    private String _fiBackgroundOORIndicator;
    private Double _fiBackgroundNegative;
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
    private int _data;
    private Double _dilution;
    private String _dataRowGroup;
    private String _ratio;
    private String _samplingErrors;
    private String _extraSpecimenInfo;
    private Integer _beadCount;
    private Integer _titration;
    private Integer _singlePointControl;
    private boolean _summary;
    private Double _cv;
    private boolean _excluded;

    /** Unfortunate to have these denormalized values here, but required for acceptable query performance */
    private Container _container;
    private int _protocol;

    // Extra properties that aren't stored directly in the database
    private String _dataFile;


    /** For testing */
    public LuminexDataRow(String type, String well, double fiBackground, double expConc, double dilution)
    {
        setType(type);
        setWell(well);
        setFi(fiBackground);
        setFiBackground(fiBackground);
        setExpConc(expConc);
        setDilution(dilution);
        if (well != null && well.contains(","))
        {
            setSummary(true);
        }
    }

    /** For testing */
    public LuminexDataRow(String type, String well, double fiBackground, String standard)
    {
        setType(type);
        setWell(well);
        setFi(fiBackground);
        setFiBackground(fiBackground);

        Map<String, Object> extraProperties = new CaseInsensitiveHashMap<>();
        extraProperties.put("Standard", standard);
        _setExtraProperties(extraProperties);

        if (well != null && well.contains(","))
        {
            setSummary(true);
        }
    }

    /** General purpose and for reflection */
    public LuminexDataRow()
    {
    }

    public int getAnalyte()
    {
        return _analyte;
    }

    public void setAnalyte(int analyte)
    {
        _analyte = analyte;
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

    public int getOutlier()
    {
        return _outlier;
    }

    public void setOutlier(int outlier)
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

    public Double getFiBackgroundNegative()
    {
        return _fiBackgroundNegative;
    }

    public void setFiBackgroundNegative(Double fiBackgroundNegative)
    {
        _fiBackgroundNegative = fiBackgroundNegative;
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

    public void setData(int data)
    {
        _data = data;
    }

    public int getData()
    {
        return _data;
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

    public String getParticipantID()
    {
        return _participantID;
    }

    public void setParticipantID(String participantID)
    {
        _participantID = participantID;
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

    public void setExtraSpecimenInfo(String extraSpecimenInfo)
    {
        _extraSpecimenInfo = extraSpecimenInfo;
    }

    public String getExtraSpecimenInfo()
    {
        return _extraSpecimenInfo;
    }

    public String getSpecimenID()
    {
        return _specimenID;
    }

    public void setSpecimenID(String specimenID)
    {
        _specimenID = specimenID;
    }

    public Container getContainer()
    {
        return _container;
    }

    public void setContainer(Container container)
    {
        _container = container;
    }

    public int getProtocol()
    {
        return _protocol;
    }

    public void setProtocol(int protocol)
    {
        _protocol = protocol;
    }

    public Integer getBeadCount()
    {
        return _beadCount;
    }

    public void setBeadCount(Integer beadCount)
    {
        _beadCount = beadCount;
    }

    public String getLsid()
    {
        return _lsid;
    }

    public void setLsid(String lsid)
    {
        _lsid = lsid;
    }

    public Integer getTitration()
    {
        return _titration;
    }

    public void setTitration(Integer titration)
    {
        _titration = titration;
    }

    public Integer getSinglePointControl()
    {
        return _singlePointControl;
    }

    public void setSinglePointControl(Integer singlePointControl)
    {
        _singlePointControl = singlePointControl;
    }

    public String getDataFile()
    {
        return _dataFile;
    }

    public void setDataFile(String dataFile)
    {
        _dataFile = dataFile;
    }

    public String getWellRole()
    {
        return _wellRole;
    }

    public void setWellRole(String wellRole)
    {
        _wellRole = wellRole;
    }

    /** Remember any extra properties from the transform script so they can be included when generating map version of this row */
    public void _setExtraProperties(Map<String, Object> properties)
    {
        _extraProperties = properties;

        // look for the FlaggedAsExcluded property to set the data row exclusion state
        if (properties.containsKey(LuminexDataTable.FLAGGED_AS_EXCLUDED_COLUMN_NAME))
            setExcluded(ConvertHelper.convert(properties.get(LuminexDataTable.FLAGGED_AS_EXCLUDED_COLUMN_NAME), Boolean.class));
    }

    public Map<String, Object> toMap(Analyte analyte)
    {
        Map<String, Object> row = new CaseInsensitiveHashMap<>(_extraProperties);

        ObjectFactory<Analyte> af = ObjectFactory.Registry.getFactory(Analyte.class);
        if (null == af)
            throw new IllegalArgumentException("Could not find a matching object factory.");
        row.putAll(af.toMap(analyte, null));

        ObjectFactory<LuminexDataRow> f = ObjectFactory.Registry.getFactory(LuminexDataRow.class);
        if (null == f)
            throw new IllegalArgumentException("Could not find a matching object factory.");
        row.putAll(f.toMap(this, null));

        return row;
    }

    public @NotNull Map<String, Object> _getExtraProperties()
    {
        return _extraProperties;
    }

    public boolean isSummary()
    {
        return _summary;
    }

    public void setSummary(boolean summary)
    {
        _summary = summary;
    }

    public Double getCv()
    {
        return _cv;
    }

    public void setCv(Double cv)
    {
        _cv = cv;
    }

    public boolean isExcluded()
    {
        return _excluded;
    }

    public void setExcluded(boolean excluded)
    {
        _excluded = excluded;
    }
}
