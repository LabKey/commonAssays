/*
 * Copyright (c) 2012-2018 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
    String getPath();

    String getName();

    /** Get a display string that indicates the workspace type, e.g. "FlowJo Mac workspace" or "Analysis Archive".) */
    String getKindName();

    /**
     * Warnings generated during loading of the workspace.
     */
    List<String> getWarnings();

    /**
     * Get union of all keywords found in all samples.
     */
    Set<String> getKeywords();

    /**
     * Get a list of all parameter names.
     */
    List<String> getParameterNames();

    List<ParameterInfo> getParameters();

    /**
     * Get all internal sample ids.  Each id is guaranteed to be unique.
     */
    List<String> getSampleIds();

    /**
     * Get all sample labels.  More than one sample may have the same label.
     */
    List<String> getSampleLabels();

    List<? extends ISampleInfo> getSamples();

    /**
     * Get SampleInfo by workspace sample ID.
     */
    @Nullable
    ISampleInfo getSampleById(String sampleId);

    /**
     * Get SampleInfos by sample label (typically the FCS filename, $FIL keyword)
     */
    @NotNull
    List<? extends ISampleInfo> getSampleByLabel(String label);

    /**
     * List of duplicate sample lables (FCS filenames) found in the workspace that have distinct IDs.
     */
    @NotNull
    List<String> getDuplicateSampleLabels();

    /**
     * @return true if the workspace has an analysis definition
     */
    boolean hasAnalysis();

    Analysis getSampleAnalysis(ISampleInfo sample);

    AttributeSet getSampleAnalysisResults(ISampleInfo sample);

    CompensationMatrix getSampleCompensationMatrix(ISampleInfo sample);

    List<CompensationMatrix> getCompensationMatrices();

    /**
     * Create backwards compatibility aliases for boolean populations.
     */
    default void createBooleanAliases()
    {
    }
}
