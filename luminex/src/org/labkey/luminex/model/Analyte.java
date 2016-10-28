/*
 * Copyright (c) 2007-2016 LabKey Corporation
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


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * User: jeckels
* Date: Jul 26, 2007
*/
public class Analyte
{
    private int _dataId;
    private String _lsid;
    private String _name;
    private String _beadNumber;
    private String _sheetName;
    private Double _fitProb;
    private Double _resVar;
    private String _regressionType;
    private String _stdCurve;
    private int _rowId;
    private int _minStandardRecovery;
    private int _maxStandardRecovery;
    private Integer _positivityThreshold;
    private String _negativeBead;

    public Analyte()
    {
    }

    public Analyte(String name, String beadNumber)
    {
        _name = name;
        _beadNumber = beadNumber;
        _sheetName = name;
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

    public void setFitProb(Double fitProb)
    {
        _fitProb = fitProb;
    }

    public void setResVar(Double resVar)
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

    public Double getFitProb()
    {
        return _fitProb;
    }

    public Double getResVar()
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

    public Integer getPositivityThreshold()
    {
        return _positivityThreshold;
    }

    public void setPositivityThreshold(Integer positivityThreshold)
    {
        _positivityThreshold = positivityThreshold;
    }

    public String getNegativeBead()
    {
        return _negativeBead;
    }

    public void setNegativeBead(String negativeBead)
    {
        _negativeBead = negativeBead;
    }

    public int getMaxStandardRecovery()
    {
        return _maxStandardRecovery;
    }

    public void setMaxStandardRecovery(int maxStandardRecovery)
    {
        _maxStandardRecovery = maxStandardRecovery;
    }
    
    public String getLsid()
    {
        return _lsid;
    }

    public void setLsid(String lsid)
    {
        _lsid = lsid;
    }

    public String getBeadNumber()
    {
        return _beadNumber;
    }

    public void setBeadNumber(String beadNumber)
    {
        _beadNumber = beadNumber;
    }

    public LuminexWellGroup buildWellGroup(List<LuminexDataRow> dataRows)
    {
        List<LuminexWell> wells = new ArrayList<>();
        for (LuminexDataRow dataRow : dataRows)
        {
            // look for wells with a matching analyteId, and skip excluded wells
            if (!dataRow.isExcluded() && Objects.equals(dataRow.getAnalyte(), getRowId()))
            {
                wells.add(new LuminexWell(dataRow));
            }
        }
        return new LuminexWellGroup(wells);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Analyte)
        {
            if (_name != null)
                return _name.equals(((Analyte)obj).getName());
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode()
    {
        if (_name != null)
            return _name.hashCode();

        return super.hashCode();
    }

    /**
     * @return if fresh from the parser, the name of the sheet in the Excel file. May have been truncated from the full
     * name, which is available by calling getName()
     */
    public String getSheetName()
    {
        return _sheetName;
    }
}
