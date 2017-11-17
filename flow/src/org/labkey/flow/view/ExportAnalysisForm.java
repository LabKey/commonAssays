/*
 * Copyright (c) 2011-2017 LabKey Corporation
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
package org.labkey.flow.view;

import org.labkey.api.data.DataRegionSelection;
import org.labkey.flow.persist.AnalysisSerializer;

/**
 * User: kevink
 * Date: 9/14/11
 */
public class ExportAnalysisForm implements DataRegionSelection.DataSelectionKeyForm
{
    public enum SendTo
    {
        Browser,
        PipelineZip,
        PipelineFiles,
        Script
    }

    private int[] _runId;
    private int[] _wellId;
    private boolean _includeFCSFiles = false;
    private boolean _includeKeywords = false;
    private boolean _includeStatistics = false;
    private boolean _includeGraphs = false;
    private boolean _includeCompensation = false;
    private boolean _useShortStatNames = false;
    private AnalysisSerializer.Options _exportFormat = AnalysisSerializer.Options.FormatGroupBySamplePopulation;
    private SendTo _sendTo = SendTo.Browser;
    private String _label;
    private String _fcsDirName = "FCSFiles";

    private String _dataRegionSelectionKey = null;
    private String _selectionType;

    // Non-form bound fields
    public boolean _renderForm = false;

    public int[] getRunId()
    {
        return _runId;
    }

    public void setRunId(int[] runId)
    {
        _runId = runId;
    }

    public int[] getWellId()
    {
        return _wellId;
    }

    public void setWellId(int[] wellId)
    {
        _wellId = wellId;
    }

    public boolean isIncludeFCSFiles()
    {
        return _includeFCSFiles;
    }

    public void setIncludeFCSFiles(boolean includeFCSFiles)
    {
        _includeFCSFiles = includeFCSFiles;
    }

    public boolean isIncludeKeywords()
    {
        return _includeKeywords;
    }

    public void setIncludeKeywords(boolean includeKeywords)
    {
        _includeKeywords = includeKeywords;
    }

    public boolean isIncludeStatistics()
    {
        return _includeStatistics;
    }

    public void setIncludeStatistics(boolean includeStatistics)
    {
        _includeStatistics = includeStatistics;
    }

    public boolean isIncludeGraphs()
    {
        return _includeGraphs;
    }

    public void setIncludeGraphs(boolean includeGraphs)
    {
        _includeGraphs = includeGraphs;
    }

    public boolean isIncludeCompensation()
    {
        return _includeCompensation;
    }

    public void setIncludeCompensation(boolean includeCompensation)
    {
        _includeCompensation = includeCompensation;
    }

    public boolean isUseShortStatNames()
    {
        return _useShortStatNames;
    }

    public void setUseShortStatNames(boolean useShortStatNames)
    {
        _useShortStatNames = useShortStatNames;
    }

    public AnalysisSerializer.Options getExportFormat()
    {
        return _exportFormat;
    }

    public void setExportFormat(AnalysisSerializer.Options exportFormat)
    {
        _exportFormat = exportFormat;
    }

    public SendTo getSendTo()
    {
        return _sendTo;
    }

    public void setSendTo(SendTo sendTo)
    {
        _sendTo = sendTo;
    }

    public String getLabel()
    {
        return _label;
    }

    public void setLabel(String label)
    {
        _label = label;
    }

    public String getFcsDirName()
    {
        return _fcsDirName;
    }

    public void setFcsDirName(String fcsDirName)
    {
        _fcsDirName = fcsDirName;
    }

    @Override
    public String getDataRegionSelectionKey()
    {
        return _dataRegionSelectionKey;
    }

    @Override
    public void setDataRegionSelectionKey(String dataRegionSelectionKey)
    {
        _dataRegionSelectionKey = dataRegionSelectionKey;
    }

    public String getSelectionType()
    {
        return _selectionType;
    }

    public void setSelectionType(String selectionType)
    {
        _selectionType = selectionType;
    }
}

