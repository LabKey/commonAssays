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

import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.util.NetworkDrive;

import java.io.*;
import java.util.Map;
import java.sql.SQLException;

/**
 * <code>XarGeneratorTask</code>
 */
public class XarGeneratorTask extends PipelineJob.Task
{
    /**
     * Interface for support required from the PipelineJob to run this task,
     * beyond the base PipelineJob methods.
     */
    public interface JobSupport extends MS2SearchJobSupport
    {
        /**
         * Returns a description of the search.
         */
        String getDescription();

        /**
         * Returns a classpath-relative path to the template resource.
         */
        String getXarTemplateResource();

        /**
         * Returns a map of string replacements to be made in the template.
         */
        Map<String, String> getXarTemplateReplacements() throws IOException;
    }

    public JobSupport getJobSupport()
    {
        return (JobSupport) getJob();
    }

    public String getStatusName()
    {
        return "SAVE EXPERIMENT";
    }

    public boolean isComplete() throws IOException, SQLException
    {
        String baseName = getJobSupport().getOutputBasename();
        File dirAnalysis = getJobSupport().getAnalysisDirectory();

        return NetworkDrive.exists(MS2PipelineManager.getSearchExperimentFile(dirAnalysis, baseName));
    }

    public void run()
    {
        try
        {
            String baseName = getJobSupport().getOutputBasename();
            File dirAnalysis = getJobSupport().getAnalysisDirectory();
            File dirWork = MS2PipelineManager.createWorkingDirectory(dirAnalysis, baseName);

            File fileExperimentXML = MS2PipelineManager.getSearchExperimentFile(dirWork, baseName);

            InputStream in = getClass().getClassLoader().getResourceAsStream(getJobSupport().getXarTemplateResource());
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder sb = new StringBuilder();
            String line;
            try
            {
                while ((line = reader.readLine()) != null)
                {
                    sb.append(line);
                    sb.append("\n");
                }
            }
            finally
            {
                reader.close();
            }

            Map<String, String> replaceMap = getJobSupport().getXarTemplateReplacements();
            for (Map.Entry<String, String> entry : replaceMap.entrySet())
                replaceString(sb, entry.getKey(), entry.getValue());

            FileOutputStream fOut = new FileOutputStream(fileExperimentXML);
            PrintWriter writer = new PrintWriter(fOut);
            try
            {
                writer.write(sb.toString());
            }
            finally
            {
                writer.close();
            }

            MS2PipelineManager.moveWorkToParent(fileExperimentXML);
            MS2PipelineManager.removeWorkingDirectory(dirWork);
        }
        catch (IOException e)
        {
            getJob().error(e.getMessage(), e);
        }
    }

    protected void replaceString(StringBuilder sb, String oldString, String newString)
    {
        oldString = "@@" + oldString + "@@";
        int index = sb.indexOf(oldString);
        while (index != -1)
        {
            sb.replace(index, index + oldString.length(), newString);
            index = sb.indexOf(oldString);
        }
    }
}
