/*
 * Copyright (c) 2007 LabKey Corporation
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

package org.labkey.ms2.search;

/**
 * User: jeckels
 * Date: Feb 26, 2007
 */
public class ProteinSearchBean
{
    private boolean _includeSubfolders = true;
    private boolean _exactMatch = true;
    private boolean _restrictProteins = true;
    private Float _minProbability;
    private Float _maxErrorRate;
    private String _identifier = "";
    private boolean _horizontal;

    public ProteinSearchBean(boolean horizontal)
    {
        _horizontal = horizontal;
    }

    public boolean isHorizontal()
    {
        return _horizontal;
    }

    public String getIdentifier()
    {
        return _identifier;
    }

    public boolean isExactMatch()
    {
        return _exactMatch;
    }

    public void setExactMatch(boolean exactMatch)
    {
        _exactMatch = exactMatch;
    }

    public void setIdentifier(String identifier)
    {
        _identifier = identifier;
    }

    public boolean isIncludeSubfolders()
    {
        return _includeSubfolders;
    }

    public void setIncludeSubfolders(boolean includeSubfolders)
    {
        _includeSubfolders = includeSubfolders;
    }

    public Float getMaxErrorRate()
    {
        return _maxErrorRate;
    }

    public void setMaxErrorRate(Float maxErrorRate)
    {
        _maxErrorRate = maxErrorRate;
    }

    public Float getMinProbability()
    {
        return _minProbability;
    }

    public void setMinProbability(Float minProbability)
    {
        _minProbability = minProbability;
    }

    public boolean isRestrictProteins()
    {
        return _restrictProteins;
    }

    public void setRestrictProteins(boolean restrictProteins)
    {
        _restrictProteins = restrictProteins;
    }
}
