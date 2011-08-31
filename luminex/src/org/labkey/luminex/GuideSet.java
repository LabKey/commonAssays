/*
 * Copyright (c) 2011 LabKey Corporation
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
package org.labkey.luminex;

/**
 * User: jeckels
 * Date: Aug 26, 2011
 */
public class GuideSet
{
    private int _rowId;
    private int _protocolId;
    private String _analyteName;
    private String _conjugate;
    private String _isotype;
    private boolean _currentGuideSet;
    private Double _maxFIAverage;
    private Double _maxFIStdDev;

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
}
