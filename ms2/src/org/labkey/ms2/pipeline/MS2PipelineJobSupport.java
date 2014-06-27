/*
 * Copyright (c) 2007-2014 LabKey Corporation
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

import org.labkey.api.pipeline.file.FileAnalysisJobSupport;

import java.io.File;
import java.io.IOException;

/**
 * <code>MS2PipelineJobSupport</code> Interface for providing MS2 search support to
 * PipelineJob tasks.
 */
public interface MS2PipelineJobSupport extends FileAnalysisJobSupport
{
    /**
     * Returns the root sequence file directory.
     */
    File getSequenceRootDirectory();
    
    /**
     * Returns a list of FASTA files to be searched.
     */
    File[] getSequenceFiles() throws IOException;

    /**
     * Returns true if the job should perform a combined analysis of
     * all the spectra files.
     */
    boolean isFractions();

    /**
     * Returns true if the job should be treated as a single sample,
     * with full TPP analysis, and upload to web site.
     */
    boolean isSamples() throws IOException;
}
