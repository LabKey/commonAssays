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

import org.labkey.api.pipeline.*;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.FileType;

import java.io.*;
import java.sql.SQLException;
import java.util.Map;

/**
 * <code>XarGeneratorTask</code>
 */
public class XarGeneratorTask extends PipelineJob.Task
{
    public static final FileType FT_SEARCH_XAR = new FileType(".search.xar.xml");

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

    public static class Factory extends AbstractTaskFactory
    {
        public Factory()
        {
            super(XarGeneratorTask.class);
        }

        public PipelineJob.Task createTask(PipelineJob job)
        {
            return new XarGeneratorTask(job);
        }

        public FileType getInputType()
        {
            return null;
        }

        public String getStatusName()
        {
            return "SAVE EXPERIMENT";
        }

        public boolean isJobComplete(PipelineJob job) throws IOException, SQLException
        {
            JobSupport support = (JobSupport) job;
            String baseName = support.getBaseName();
            File dirAnalysis = support.getAnalysisDirectory();

            return NetworkDrive.exists(FT_SEARCH_XAR.newFile(dirAnalysis, baseName));
        }
    }

    protected XarGeneratorTask(PipelineJob job)
    {
        super(job);
    }

    public JobSupport getJobSupport()
    {
        return (JobSupport) getJob();
    }

    public void run()
    {
        try
        {
            WorkDirFactory factory = PipelineJobService.get().getWorkDirFactory();
            WorkDirectory wd = factory.createWorkDirectory(getJob().getJobGUID(), getJobSupport());

            File fileExperimentXML = wd.newFile(FT_SEARCH_XAR);

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

            wd.outputFile(fileExperimentXML);
            wd.remove();
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
