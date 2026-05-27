/*
 * Copyright (c) 2024-2026 LabKey Corporation
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
package org.labkey.api.protein.search;

import org.labkey.api.action.QueryViewAction;

public abstract class ProteinSearchForm extends QueryViewAction.QueryExportForm
{
    private String _identifier;
    private String _peptideFilterType = "none";
    private Float _peptideProphetProbability;
    private boolean _includeSubfolders;
    private boolean _exactMatch;
    private boolean _restrictProteins;
    protected String _defaultCustomView;
    private boolean _showMatchingProteins = true;
    private boolean _showProteinGroups = true;
    private int[] _seqIds;
    private String _location;

    public String getDefaultCustomView()
    {
        return _defaultCustomView;
    }

    public void setDefaultCustomView(String defaultCustomView)
    {
        _defaultCustomView = defaultCustomView;
    }

    public boolean isExactMatch()
    {
        return _exactMatch;
    }

    public void setExactMatch(boolean exactMatch)
    {
        _exactMatch = exactMatch;
    }

    public String getPeptideFilterType()
    {
        return _peptideFilterType;
    }

    public void setPeptideFilterType(String peptideFilterType)
    {
        _peptideFilterType = peptideFilterType;
    }

    public Float getPeptideProphetProbability()
    {
        return _peptideProphetProbability;
    }

    public void setPeptideProphetProbability(Float peptideProphetProbability)
    {
        _peptideProphetProbability = peptideProphetProbability;
    }

    public String getIdentifier()
    {
        return _identifier;
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

    public boolean isRestrictProteins()
    {
        return _restrictProteins;
    }

    public void setRestrictProteins(boolean restrictProteins)
    {
        _restrictProteins = restrictProteins;
    }

    public abstract int[] getSeqId();

    public boolean isShowMatchingProteins()
    {
        return _showMatchingProteins;
    }

    public void setShowMatchingProteins(boolean showMatchingProteins)
    {
        _showMatchingProteins = showMatchingProteins;
    }

    public boolean isShowProteinGroups()
    {
        return _showProteinGroups;
    }

    public void setShowProteinGroups(boolean showProteinGroups)
    {
        _showProteinGroups = showProteinGroups;
    }

    public String getLocation()
    {
        return _location;
    }

    public void setLocation(String location)
    {
        _location = location;
    }
}
