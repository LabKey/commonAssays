/*
 * Copyright (c) 2012-2013 LabKey Corporation
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
package org.labkey.flow.analysis.model;

import org.labkey.flow.persist.AttributeSet;

import java.util.List;
import java.util.Set;

/**
 * User: kevink
 * Date: 11/15/12
 *
 * CONSIDER: Include FCS files.
 */
public interface IWorkspace
{
    public String getPath();

    public String getName();

    /** Get a display string that indicates the workspace type, e.g. "FlowJo Mac workspace" or "Analysis Archive".) */
    public String getKindName();

    /**
     * Warnings generated during loading of the workspace.
     * @return
     */
    public List<String> getWarnings();

    /**
     * Get union of all keywords found in all samples.
     * @return
     */
    public Set<String> getKeywords();

    /**
     * Get a list of all parameter names.
     * @return
     */
    public List<String> getParameterNames();

    public List<ParameterInfo> getParameters();

    /**
     * Get all internal sample ids.
     * @return
     */
    public List<String> getSampleIds();

    /**
     * Get all sample labels.
     * @return
     */
    public List<String> getSampleLabels();

    public List<? extends ISampleInfo> getSamples();

    /**
     * Get sample by either sample id or label.
     * @param sampleIdOrLabel
     * @return
     */
    public ISampleInfo getSample(String sampleIdOrLabel);

    /**
     * @return true if the workspace has an analysis definition (
     */
    public boolean hasAnalysis();

    public Analysis getSampleAnalysis(ISampleInfo sample);

    public AttributeSet getSampleAnalysisResults(ISampleInfo sample);

    public CompensationMatrix getSampleCompensationMatrix(ISampleInfo sample);

    public List<CompensationMatrix> getCompensationMatrices();

}
