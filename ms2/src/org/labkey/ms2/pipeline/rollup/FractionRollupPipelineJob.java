/*
 * Copyright (c) 2005-2015 LabKey Corporation
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
package org.labkey.ms2.pipeline.rollup;

import org.apache.log4j.Logger;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.TaskId;
import org.labkey.api.pipeline.file.AbstractFileAnalysisJob;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.ms2.pipeline.AbstractMS2SearchPipelineJob;
import org.labkey.ms2.pipeline.tandem.XTandemSearchTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Job for rolling up multiple search engine results (after the search has already been completed) into a combined
 * set of analysis results. Currently only works for .xtan.xml files - XTandem's native output file format.
 * @author jeckels
 */
public class FractionRollupPipelineJob extends AbstractMS2SearchPipelineJob
{
    private static final Logger LOG = getJobLogger(FractionRollupPipelineJob.class);
    protected static final TaskId TASK_ID = new TaskId(FractionRollupPipelineJob.class);

    @Override
    public Logger getClassLogger()
    {
        return LOG;
    }

    public FractionRollupPipelineJob(FractionRollupProtocol protocol,
                                     ViewBackgroundInfo info,
                                     PipeRoot root,
                                     String name,
                                     List<File> filesXtanXML,
                                     File fileInputXML) throws IOException
    {
        super(protocol, FractionRollupPipelineProvider.NAME, info, root, name, null, fileInputXML, filesXtanXML);

        _fractions = true;

        header("Fraction rollup analysis " + getBaseName());
        writeInputFilesToLog();
    }

    public FractionRollupPipelineJob(FractionRollupPipelineJob job, File fileFraction)
    {
        super(job, fileFraction);
    }

    @Override
    public List<File> getInteractInputFiles()
    {
        List<File> files = new ArrayList<>();
        for (File xtanXMLFile : getInputFiles())
        {
            files.add(getPepXMLConvertFile(getAnalysisDirectory(),
                    XTandemSearchTask.getNativeFileType(getGZPreference()).getBaseName(xtanXMLFile),
                    getGZPreference()));
        }
        return files;
    }

    @Override
    public TaskId getTaskPipelineId()
    {
        return TASK_ID;
    }

    @Override
    public AbstractFileAnalysisJob createSingleFileJob(File file)
    {
        return new FractionRollupPipelineJob(this, file);
    }

    @Override
    public File getSearchNativeOutputFile()
    {
        throw new UnsupportedOperationException();
    }
}