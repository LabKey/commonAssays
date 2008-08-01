/*
 * Copyright (c) 2005-2008 LabKey Corporation
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
package org.labkey.ms2.pipeline.tandem;

import org.apache.log4j.Logger;
import org.labkey.api.pipeline.*;
import org.labkey.api.pipeline.file.AbstractFileAnalysisJob;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.ms2.pipeline.AbstractMS2SearchPipelineJob;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * XTandemPipelineJob class
 * <p/>
 * Created: Oct 4, 2005
 *
 * @author bmaclean
 */
public class XTandemPipelineJob extends AbstractMS2SearchPipelineJob implements XTandemSearchTask.JobSupport
{
    private static Logger _log = getJobLogger(XTandemPipelineJob.class);
    protected static TaskId _tid = new TaskId(XTandemPipelineJob.class);

    public Logger getClassLogger()
    {
        return _log;
    }

    public XTandemPipelineJob(XTandemSearchProtocol protocol,
                              ViewBackgroundInfo info,
                              String name,
                              File dirSequenceRoot,
                              File filesMzXML[],
                              File fileInputXML,
                              boolean fromCluster) throws SQLException, IOException
    {
        super(protocol, XTandemCPipelineProvider.name, info, name, dirSequenceRoot, fileInputXML, filesMzXML, fromCluster);

        header("X! Tandem search for " + getBaseName());
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

    public TaskId getTaskPipelineId()
    {
        TaskId tid = super.getTaskPipelineId();
        if (tid != null)
            return tid;

        return _tid;
    }

    public AbstractFileAnalysisJob createSingleFileJob(File file)
    {
        return new XTandemPipelineJob(this, file);
    }

    /** Currently prophet analysis enabled for native, comet, and k-store algorithms */
    public boolean isProphetEnabled()
    {
        String paramScore = getParameters().get("scoring, algorithm");
        if (paramScore == null || paramScore.length() == 0)
            paramScore = "native";

        return ("native".equals(paramScore) ||
                "comet".equals(paramScore) ||
                "k-score".equals(paramScore));        
    }

    public File getSearchNativeOutputFile()
    {
        return XTandemSearchTask.getNativeOutputFile(getAnalysisDirectory(), getBaseName());
    }

    public String getXarTemplateResource()
    {
        StringBuilder templateResource = new StringBuilder("org/labkey/ms2/pipeline/tandem/templates/MS2Search");
        if (getInputFiles().length > 1)
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