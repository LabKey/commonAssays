/*
 * Copyright (c) 2011-2015 LabKey Corporation
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
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableSelector;
import org.labkey.api.data.statistics.StatsService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.luminex.LuminexDataHandler;
import org.labkey.luminex.query.GuideSetTable;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * User: jeckels
 * Date: Aug 26, 2011
 */
public class GuideSet
{
    private int _rowId;
    private Timestamp _created;
    private Timestamp _modified;
    private Integer _createdBy;
    private Integer _modifiedBy;

    // uneditable properties
    private int _protocolId;
    private boolean _valueBased;
    private boolean _isTitration;
    private String _controlName;
    private String _analyteName;
    private String _conjugate;
    private String _isotype;

    // editable non-qc related properties
    private boolean _currentGuideSet;
    private String _comment;

    // editable qc related properties
    private Double _ec504plAverage;
    private Double _ec504plStdDev;
    private Double _ec505plAverage;
    private Double _ec505plStdDev;
    private Double _aucAverage;
    private Double _aucStdDev;
    private Double _maxFIAverage;
    private Double _maxFIStdDev;
    private boolean _ec504plEnabled = true;
    private boolean _ec505plEnabled = true;
    private boolean _aucEnabled = true;
    private boolean _maxFIEnabled = true;

    public int getRowId()
    {
        return _rowId;
    }

    public void setRowId(int rowId)
    {
        _rowId = rowId;
    }

    public int getProtocolId()
    {
        return _protocolId;
    }

    public void setProtocolId(int protocolId)
    {
        _protocolId = protocolId;
    }

    public String getAnalyteName()
    {
        return _analyteName;
    }

    public void setAnalyteName(String analyteName)
    {
        _analyteName = analyteName;
    }

    public String getConjugate()
    {
        return _conjugate;
    }

    public void setConjugate(String conjugate)
    {
        _conjugate = conjugate;
    }

    public String getIsotype()
    {
        return _isotype;
    }

    public void setIsotype(String isotype)
    {
        _isotype = isotype;
    }

    public Double getMaxFIAverage()
    {
        return _maxFIAverage;
    }

    public void setMaxFIAverage(Double maxFIAverage)
    {
        _maxFIAverage = maxFIAverage;
    }

    public Double getMaxFIStdDev()
    {
        return _maxFIStdDev;
    }

    public void setMaxFIStdDev(Double maxFIStdDev)
    {
        _maxFIStdDev = maxFIStdDev;
    }

    public boolean isCurrentGuideSet()
    {
        return _currentGuideSet;
    }

    public void setCurrentGuideSet(boolean currentGuideSet)
    {
        _currentGuideSet = currentGuideSet;
    }

    public String getControlName()
    {
        return _controlName;
    }

    public void setControlName(String controlName)
    {
        _controlName = controlName;
    }

    public String getComment()
    {
        return _comment;
    }

    public void setComment(String comment)
    {
        _comment = comment;
    }

    public Timestamp getCreated()
    {
        return _created;
    }

    public void setCreated(Timestamp created)
    {
        _created = created;
    }

    public Timestamp getModified()
    {
        return _modified;
    }

    public void setModified(Timestamp modified)
    {
        _modified = modified;
    }

    public Integer getCreatedBy()
    {
        return _createdBy;
    }

    public void setCreatedBy(Integer createdBy)
    {
        _createdBy = createdBy;
    }

    public Integer getModifiedBy()
    {
        return _modifiedBy;
    }

    public void setModifiedBy(Integer modifiedBy)
    {
        _modifiedBy = modifiedBy;
    }

    public boolean isValueBased()
    {
        return _valueBased;
    }

    public void setValueBased(boolean valueBased)
    {
        _valueBased = valueBased;
    }

    public Double getEc504plAverage()
    {
        return _ec504plAverage;
    }

    public void setEc504plAverage(Double ec504plAverage)
    {
        _ec504plAverage = ec504plAverage;
    }

    public Double getEc504plStdDev()
    {
        return _ec504plStdDev;
    }

    public void setEc504plStdDev(Double ec504plStdDev)
    {
        _ec504plStdDev = ec504plStdDev;
    }

    public Double getEc505plAverage()
    {
        return _ec505plAverage;
    }

    public void setEc505plAverage(Double ec505plAverage)
    {
        _ec505plAverage = ec505plAverage;
    }

    public Double getEc505plStdDev()
    {
        return _ec505plStdDev;
    }

    public void setEc505plStdDev(Double ec505plStdDev)
    {
        _ec505plStdDev = ec505plStdDev;
    }

    public Double getAucAverage()
    {
        return _aucAverage;
    }

    public void setAucAverage(Double aucAverage)
    {
        _aucAverage = aucAverage;
    }

    public Double getAucStdDev()
    {
        return _aucStdDev;
    }

    public void setAucStdDev(Double aucStdDev)
    {
        _aucStdDev = aucStdDev;
    }

    public boolean isEc504plEnabled()
    {
        return _ec504plEnabled;
    }

    public void setEc504plEnabled(boolean ec504plEnabled)
    {
        _ec504plEnabled = ec504plEnabled;
    }

    public boolean isEc505plEnabled()
    {
        return _ec505plEnabled;
    }

    public void setEc505plEnabled(boolean ec505plEnabled)
    {
        _ec505plEnabled = ec505plEnabled;
    }

    public boolean isAucEnabled()
    {
        return _aucEnabled;
    }

    public void setAucEnabled(boolean aucEnabled)
    {
        _aucEnabled = aucEnabled;
    }

    public boolean isMaxFIEnabled()
    {
        return _maxFIEnabled;
    }

    public void setMaxFIEnabled(boolean maxFIEnabled)
    {
        _maxFIEnabled = maxFIEnabled;
    }

    public boolean hasMetricValues()
    {
        return (getEc504plAverage() != null || getEc504plStdDev() != null
                || getEc505plAverage() != null || getEc505plStdDev() != null
                || getAucAverage() != null || getAucStdDev() != null
                || getMaxFIAverage() != null || getMaxFIStdDev() != null);
    }

    public String getOutOfRangeTypeForEC504PL(Double value, GuideSetTable guideSetTable)
    {
        // get the run-based guide set values and set them for this object
        if (!isValueBased() && getEc504plAverage() == null)
        {
            FieldKey averageRunFK = FieldKey.fromParts(StatsService.CurveFitType.FOUR_PARAMETER.getLabel() + "CurveFit", "EC50Average");
            FieldKey stdDevRunFK = FieldKey.fromParts(StatsService.CurveFitType.FOUR_PARAMETER.getLabel() + "CurveFit", "EC50StdDev");
            Map<FieldKey, ColumnInfo> colMap = QueryService.get().getColumns(guideSetTable, PageFlowUtil.set(averageRunFK, stdDevRunFK));

            setEc504plAverage(getRunBasedGuideSetValue(guideSetTable, colMap.get(averageRunFK)));
            setEc504plStdDev(getRunBasedGuideSetValue(guideSetTable, colMap.get(stdDevRunFK)));
        }

        return getOutOfRangeType(value, getEc504plAverage(), getEc504plStdDev());
    }

    public String getOutOfRangeTypeForEC505PL(Double value, GuideSetTable guideSetTable)
    {
        // get the run-based guide set values and set them for this object
        if (!isValueBased() && getEc505plAverage() == null)
        {
            FieldKey averageRunFK = FieldKey.fromParts(StatsService.CurveFitType.FIVE_PARAMETER.getLabel() + "CurveFit", "EC50Average");
            FieldKey stdDevRunFK = FieldKey.fromParts(StatsService.CurveFitType.FIVE_PARAMETER.getLabel() + "CurveFit", "EC50StdDev");
            Map<FieldKey, ColumnInfo> colMap = QueryService.get().getColumns(guideSetTable, PageFlowUtil.set(averageRunFK, stdDevRunFK));

            setEc505plAverage(getRunBasedGuideSetValue(guideSetTable, colMap.get(averageRunFK)));
            setEc505plStdDev(getRunBasedGuideSetValue(guideSetTable, colMap.get(stdDevRunFK)));
        }

        return getOutOfRangeType(value, getEc505plAverage(), getEc505plStdDev());
    }

    public String getOutOfRangeTypeForAUC(Double value, GuideSetTable guideSetTable)
    {
        // get the run-based guide set values and set them for this object
        if (!isValueBased() && getAucAverage() == null)
        {
            FieldKey averageRunFK = FieldKey.fromParts("TrapezoidalCurveFit", "AUCAverage");
            FieldKey stdDevRunFK = FieldKey.fromParts("TrapezoidalCurveFit", "AUCStdDev");
            Map<FieldKey, ColumnInfo> colMap = QueryService.get().getColumns(guideSetTable, PageFlowUtil.set(averageRunFK, stdDevRunFK));

            setAucAverage(getRunBasedGuideSetValue(guideSetTable, colMap.get(averageRunFK)));
            setAucStdDev(getRunBasedGuideSetValue(guideSetTable, colMap.get(stdDevRunFK)));
        }

        return getOutOfRangeType(value, getAucAverage(), getAucStdDev());
    }

    public String getOutOfRangeTypeForMaxFI(Double value, GuideSetTable guideSetTable, String averageColName, String stdDevColName)
    {
        // get the run-based guide set values and set them for this object
        if (!isValueBased() && getMaxFIAverage() == null)
        {
            FieldKey averageRunFK = FieldKey.fromParts(averageColName);
            FieldKey stdDevRunFK = FieldKey.fromParts(stdDevColName);
            Map<FieldKey, ColumnInfo> colMap = QueryService.get().getColumns(guideSetTable, PageFlowUtil.set(averageRunFK, stdDevRunFK));

            setMaxFIAverage(getRunBasedGuideSetValue(guideSetTable, colMap.get(averageRunFK)));
            setMaxFIStdDev(getRunBasedGuideSetValue(guideSetTable, colMap.get(stdDevRunFK)));
        }

        return getOutOfRangeType(value, getMaxFIAverage(), getMaxFIStdDev());
    }

    private Double getRunBasedGuideSetValue(GuideSetTable table, ColumnInfo col)
    {
        SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("RowId"), getRowId());
        return new TableSelector(table, Collections.singleton(col), filter, null).getObject(Double.class);
    }

    /**
     * Check if the given value is outside of the Guide Set range (i.e. average +/- 3 stdDev threshold)
     * @param value The value to be compared with the range
     * @return Return null if the value is within range and "over" or "under" if the value is not in the range
     */
    public static String getOutOfRangeType(Double value, Double average, Double stdDev)
    {
        if (null != value && null != average)
        {
            // set the stdDev to zero if there is none
            if (null == stdDev)
                stdDev = 0.0;

            // compare everything to six decimal places, Issue 16767
            int precision = 1000000;
            value = (double)Math.round(value * precision) / precision;
            double top = (double)Math.round((average + 3 * stdDev) * precision) / precision;
            double bottom = (double)Math.round((average - 3 * stdDev) * precision) / precision;

            if (value > top)
                return "over";
            else if (value < bottom)
                return "under";
        }
        return null;
    }

    public boolean getIsTitration()
    {
        return _isTitration;
    }

    public void setIsTitration(boolean isTitration)
    {
        _isTitration = isTitration;
    }

    public boolean hasValueBasedQCRelatedPropertyChanged(GuideSet orig)
    {
        // value-based guide set updates might change expected ranges so QC flags needs to be updated,
        return !Objects.equals(this.getEc504plAverage(), orig.getEc504plAverage()) || !Objects.equals(this.getEc504plStdDev(), orig.getEc504plStdDev())
                || !Objects.equals(this.getEc505plAverage(), orig.getEc505plAverage()) || !Objects.equals(this.getEc505plStdDev(), orig.getEc505plStdDev())
                || !Objects.equals(this.getAucAverage(), orig.getAucAverage()) || !Objects.equals(this.getAucStdDev(), orig.getAucStdDev())
                || !Objects.equals(this.getMaxFIAverage(), orig.getMaxFIAverage()) || !Objects.equals(this.getMaxFIStdDev(), orig.getMaxFIStdDev());
    }

    /*
     * Return the list of QC Flag Types for the run-based metric QC flag states that have changed from
     * either enabled -> disabled or from disabled -> enabled.
     */
    public @NotNull List<String> getQCFlagTypesForChanged(GuideSet orig, boolean enabled)
    {
        List<String> flagTypes = new ArrayList<>();
        if (this.isEc504plEnabled() == enabled && orig.isEc504plEnabled() != enabled)
        {
            flagTypes.add(LuminexDataHandler.QC_FLAG_EC50_4PL_FLAG_TYPE);
        }
        if (this.isEc505plEnabled() == enabled && orig.isEc505plEnabled() != enabled)
        {
            flagTypes.add(LuminexDataHandler.QC_FLAG_EC50_5PL_FLAG_TYPE);
        }
        if (this.isAucEnabled() == enabled && orig.isAucEnabled() != enabled)
        {
            flagTypes.add(LuminexDataHandler.QC_FLAG_AUC_FLAG_TYPE);
        }
        if (this.isMaxFIEnabled() == enabled && orig.isMaxFIEnabled() != enabled)
        {
            flagTypes.add(LuminexDataHandler.QC_FLAG_HIGH_MFI_FLAG_TYPE); // for Titrations
            flagTypes.add(LuminexDataHandler.QC_FLAG_SINGLE_POINT_CONTROL_FI_FLAG_TYPE); // for SinglePointControls
        }

        return flagTypes;
    }

    public String getUneditablePropertyNames()
    {
        return "protocolId, valueBased, isTitration, controlName, analyteName, conjugate, and isotype";
    }

    public boolean hasUneditablePropertyChanged(GuideSet orig)
    {
        return this.getProtocolId() != orig.getProtocolId()
                || this.isValueBased() != orig.isValueBased()
                || this.getIsTitration() != orig.getIsTitration()
                || !Objects.equals(this.getControlName(), orig.getControlName())
                || !Objects.equals(this.getAnalyteName(), orig.getAnalyteName())
                || !Objects.equals(this.getConjugate(), orig.getConjugate())
                || !Objects.equals(this.getIsotype(), orig.getIsotype());
    }
}
