/*
 * Copyright (c) 2007 LabKey Software Foundation
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

import org.labkey.api.pipeline.ParamParser;

import java.io.File;
import java.util.Map;

/**
 * Interface for support required from the PipelineJob to run a search task,
 * beyond the base PipelineJob methods.
 */
public interface MS2SearchJobSupport extends MS2PipelineJobSupport
{
    /**
     * Returns a parameter parser object for writing parameters to a file.
     */
    ParamParser createParamParser();
    
    /**
     * Returns name-value map of the BioML parameters.
     */
    Map<String, String> getParameters();
    
    /**
     * Returns the parameters input file used to drive the pipeline.
     */
    File getParametersFile();

    /**
     * Returns the native output file for the search.
     */
    File getSearchNativeOutputFile();

    /**
     * Returns the mzXML file containing the spectra used in the search.
     */
    File getSearchSpectraFile();

    /**
     * Returns native spectra file converted from the standard format,
     * or null if the standard format was used.
     */
    File getSearchNativeSpectraFile();

    /**
     * Returns an array of all spectra files analyzed.
     */
    File[] getSpectraFiles();
}
