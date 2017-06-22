/*
 * Copyright (c) 2012-2017 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.flow.persist.AttributeSet;

import java.util.Map;

/**
 * User: kevink
 * Date: 11/16/12
 */
public interface ISampleInfo
{
    /**
     * Returns true if the sample is marked as deleted in the workspace.
     */
    boolean isDeleted();

    /**
     * Internal sample id used by the workspace or analysis archive.
     * Not a stable identifier.
     * @return
     */
    public String getSampleId();

    /**
     * The sample name (usually the same as the $FIL keyword, but may be renamed in the workspace.)
     * @return
     */
    public String getSampleName();

    /**
     * The $FIL keyword value.
     * @return
     */
    public String getFilename();

    /**
     * Get human readable display name (may be sample name, $FIL, or sample id).
     * @return
     */
    public String getLabel();

    /**
     * A case-insensitive map of keyword names to values.
     * @return
     */
    public Map<String, String> getKeywords();

    /**
     * The analysis definition which may be null for an analysis archive.
     * @return
     */
    @Nullable
    public Analysis getAnalysis();

    /**
     * The calculated statistics and graphs.
     * @return
     */
    public AttributeSet getAnalysisResults();

    /**
     * The compensation matrix used for analysis.
     * @return
     */
    public CompensationMatrix getCompensationMatrix();
}
