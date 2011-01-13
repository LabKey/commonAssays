/*
 * Copyright (c) 2007-2011 LabKey Corporation
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

import java.io.File;

/**
 * Interface for support required from the PipelineJob to run a search task,
 * beyond the base PipelineJob methods.
 */
public interface MS2SearchJobSupport extends MS2PipelineJobSupport
{
    /**
     * Returns the native output file for the search.
     */
    File getSearchNativeOutputFile();

    /**
     * Returns native spectra file converted from the standard format,
     * or null if the standard format was used.
     */
    File getSearchNativeSpectraFile();

}
