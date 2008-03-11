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
import org.labkey.api.util.NetworkDrive;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * <code>MSInspectXarGeneratorSupport</code>
 */
abstract public class MSInspectXarGeneratorSupport implements FileAnalysisXarGeneratorSupport
{
    public static class Features extends MSInspectXarGeneratorSupport
    {
        public String getXarTemplateResource(AbstractFileAnalysisJob job)
        {
            if (hasFileType(job, PeaksFileDataHandler.FT_PEAKS))
                return "org/labkey/ms1/pipeline/templates/msInspectFeaturesPeaks.xml";
            else
                return "org/labkey/ms1/pipeline/templates/msInspectFeatures.xml";
        }
    }

    public static class Peptides extends MSInspectXarGeneratorSupport
    {
        public String getXarTemplateResource(AbstractFileAnalysisJob job)
        {
            if (hasFileType(job, PeaksFileDataHandler.FT_PEAKS))
                return "org/labkey/ms1/pipeline/templates/msInspectPeptidesPeaks.xml";
            else
                return "org/labkey/ms1/pipeline/templates/msInspectPeptides.xml";
        }
    }

    public boolean hasFileType(AbstractFileAnalysisJob job, FileType ft)
    {
        File file = job.findInputFile(ft.getName(job.getBaseName()));
        return NetworkDrive.exists(file);

    }

    public Map<String, String> getXarTemplateReplacements(AbstractFileAnalysisJob job) throws IOException
    {
        Map<String, String> replaceMap = new HashMap<String, String>();

        String baseName = job.getBaseName();

        replaceMap.put("SEARCH_NAME", job.getDescription());

        // Ideally the FileTypes objects used below would be defined somewhere else,
        // But since all this will soon be replaced by experiments generated during
        // pipeline execution, this small hack is the least of its problems.
        File fileMzXML = job.findInputFile(new FileType(".mzXML").getName(baseName));
        replaceMap.put("MZXML_STARTING_INPUT", job.getStartingInputDataSnippet(fileMzXML));
        replaceMap.put("MZXML_PATH", job.getXarPath(fileMzXML));
        File filePepXML = job.findInputFile(new FileType(".pep.xml").getName(baseName));
        replaceMap.put("PEPXML_STARTING_INPUT", job.getStartingInputDataSnippet(filePepXML));
        replaceMap.put("PEPXML_PATH", job.getXarPath(filePepXML));
        replaceMap.put("INPUT_XML_FILE_PATH", job.getXarPath(job.getParametersFile()));

        // Not all pipelines will contain all three files below, but add them just in case.
        File filePeaks = job.findInputFile(PeaksFileDataHandler.FT_PEAKS.getName(baseName));
        replaceMap.put("PEAKS_FILE_PATH", job.getXarPath(filePeaks));
        File fileFeatures = job.findInputFile(MSInspectFeaturesDataHandler.FT_PEPTIDES.getName(baseName));
        replaceMap.put("FEATURES_FILE_PATH", job.getXarPath(fileFeatures));
        File filePeptides = job.findInputFile(MSInspectFeaturesDataHandler.FT_PEPMATCH.getName(baseName));
        replaceMap.put("PEPTIDES_FILE_PATH", job.getXarPath(filePeptides));

        replaceMap.put("RUN-UNIQUIFIER", job.getExperimentRunUniquifier());        
        return replaceMap;
    }
}
