/*
 * Copyright (c) 2008-2011 LabKey Corporation
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
package org.labkey.ms2.pipeline;

import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.WorkDirectoryTask;
import org.labkey.api.util.FileType;

/**
 * User: jeckels
 * Date: Sep 2, 2008
 */
public abstract class AbstractMS2SearchTask<FactoryType extends AbstractMS2SearchTaskFactory> extends WorkDirectoryTask<FactoryType>
{
    public static final String JOB_ANALYSIS_PARAMETERS_ROLE_NAME = "JobAnalysisParameters";
    public static final String SPECTRA_INPUT_ROLE = "Spectra";
    public static final String FASTA_INPUT_ROLE = "FASTA";

    public static final String MINIMUM_PARENT_M_H = "spectrum, minimum parent m+h";
    public static final String MAXIMUM_PARENT_M_H = "spectrum, maximum parent m+h";
    public static final String MAXIMUM_MISSED_CLEAVAGE_SITES = "scoring, maximum missed cleavage sites";


    public AbstractMS2SearchTask(FactoryType factory, PipelineJob job)
    {
        super(factory, job);
    }
}