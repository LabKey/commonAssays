/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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

import org.apache.log4j.Logger;
import org.labkey.api.pipeline.*;
import org.labkey.api.view.ViewBackgroundInfo;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * XTandemPipelineJob class
 * <p/>
 * Created: Oct 4, 2005
 *
 * @author bmaclean
 */
public class XTandemPipelineJob extends AbstractMS2SearchPipelineJob implements XTandemSearchTask.JobSupport
{
    enum Pipelines
    {
        sample, fraction, fractionGroup;

        public TaskId getTaskId()
        {
            return new TaskId(getClass().getEnclosingClass(), toString());
        }
    }

    private static Logger _log = getJobLogger(XTandemPipelineJob.class);

    public Logger getClassLogger()
    {
        return _log;
    }

    public XTandemPipelineJob(ViewBackgroundInfo info,
                              String name,
                              File dirSequenceRoot,
                              File filesMzXML[],
                              File fileInputXML,
                              boolean fromCluster) throws SQLException, IOException
    {
        super(XTandemCPipelineProvider.name, info, name, dirSequenceRoot, fileInputXML, filesMzXML, fromCluster);

        if (filesMzXML.length > 1)
            header("X! Tandem search for " + _dirMzXML.getName());
        else
            header("X! Tandem search for " + filesMzXML[0].getName());
    }

    public XTandemPipelineJob(XTandemPipelineJob job, File fileFraction)
    {
        super(job, fileFraction);        
    }

    public boolean isPerlClusterAware()
    {
        return true;
    }

    public String getSearchEngine()
    {
        return "x!tandem";
    }

    public TaskPipeline getTaskPipeline()
    {
        TaskPipeline pipeline = super.getTaskPipeline();
        if (pipeline != null)
            return pipeline;

        TaskPipelineRegistry registry = PipelineJobService.get();
        if (_filesMzXML.length > 1)
            return registry.getTaskPipeline(Pipelines.fractionGroup.getTaskId());
        if (!isSamples())
            return PipelineJobService.get().getTaskPipeline(Pipelines.fraction.getTaskId());

        return PipelineJobService.get().getTaskPipeline(Pipelines.sample.getTaskId());
    }

    public BioMLInputParser createInputParser()
    {
        return new XTandemInputParser();
    }

    public AbstractMS2SearchPipelineJob[] getSingleFileJobs()
    {
        if (getSpectraFiles().length == 1)
            return new AbstractMS2SearchPipelineJob[0];
        
        ArrayList<AbstractMS2SearchPipelineJob> jobs = new ArrayList<AbstractMS2SearchPipelineJob>();
        for (File fileSpectra : getSpectraFiles())
            jobs.add(new XTandemPipelineJob(this, fileSpectra));
        return jobs.toArray(new AbstractMS2SearchPipelineJob[jobs.size()]);
    }

    public boolean isProphetEnabled()
    {
        // Currently prophet analysis is only supported for the 'comet' scoring
        // algorithm.
        String paramScore = getParameters().get("scoring, algorithm");
        if (paramScore == null || paramScore.length() == 0)
            paramScore = "native";

        return ("native".equals(paramScore) ||
                "comet".equals(paramScore) ||
                "k-score".equals(paramScore));        
    }

    public File getSearchNativeOutputFile()
    {
        return XTandemSearchTask.getNativeOutputFile(getAnalysisDirectory(), getFileBasename());
    }

    public String getXarTemplateResource()
    {
        StringBuilder templateResource = new StringBuilder("org/labkey/ms2/templates/MS2Search");
        if (getSpectraFiles().length > 1)
        {
            templateResource.append("Fractions");
        }
        if ("comet".equals(getParameters().get("scoring, algorithm")))
        {
            templateResource.append("XComet");
        }
        if (isXPressQuantitation())
        {
            templateResource.append("Xpress");
        }
        templateResource.append(".xml");

        return templateResource.toString();
    }
}