/*
 * Copyright (c) 2008 LabKey Software Foundation
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
package org.labkey.ms1.pipeline;

import org.labkey.api.pipeline.file.AbstractFileAnalysisJob;
import org.labkey.api.pipeline.file.FileAnalysisXarGeneratorSupport;
import org.labkey.api.util.FileType;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * <code>MSInspectXarGeneratorSupport</code>
 */
public class MSInspectXarGeneratorSupport implements FileAnalysisXarGeneratorSupport
{
    public String getXarTemplateResource(AbstractFileAnalysisJob job)
    {
        return "org/labkey/ms1/pipeline/templates/msInspectFeatureFinding.xml";
    }

    public Map<String, String> getXarTemplateReplacements(AbstractFileAnalysisJob job) throws IOException
    {
        Map<String, String> replaceMap = new HashMap<String, String>();

        String baseName = job.getBaseName();

        replaceMap.put("SEARCH_NAME", "Run for " + job.getDescription());

        // Ideally the FileTypes objects used below would be defined somewhere else,
        // But since all this will soon be replaced by experiments generated during
        // pipeline execution, this small hack is the least of its problems.
        File fileMzXML = job.findInputFile(new FileType(".mzXML").getName(baseName));

        replaceMap.put("MZXML_STARTING_INPUTS", job.getStartingInputDataSnippet(fileMzXML));
        replaceMap.put("MZXML_PATH", job.getXarPath(fileMzXML));
        replaceMap.put("MSINSPECT_XML_FILE_PATH", job.getXarPath(job.getParametersFile()));

        File fileFeatures = job.findInputFile(new FileType(".features.tsv").getName(baseName));

        replaceMap.put("FEATURES_FILE_PATH", job.getXarPath(fileFeatures));

        File filePeaks = job.findInputFile(new FileType(".peaks.xml").getName(baseName));

        replaceMap.put("PEAKS_FILE_PATH", job.getXarPath(filePeaks));

        replaceMap.put("RUN-UNIQUIFIER", job.getExperimentRunUniquifier());        
        return replaceMap;
    }
}
