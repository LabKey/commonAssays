/*
 * Copyright (c) 2006-2008 LabKey Corporation
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

import org.labkey.api.exp.XarContext;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineStatusFile;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.view.ActionURL;
import org.labkey.ms2.MS2Importer;
import org.labkey.ms2.MS2Manager;

import java.io.File;
import java.sql.SQLException;

/**
 * User: jeckels
 * Date: Mar 9, 2006
 */
public class MS2ImportPipelineJob extends PipelineJob
{
    private final File _file;
    private final String _description;
    private MS2Importer.RunInfo _runInfo;

    public MS2ImportPipelineJob(ViewBackgroundInfo info, File file, String description,
                                MS2Importer.RunInfo runInfo, boolean appendLog) throws SQLException
    {
        super(MS2PipelineProvider.name, info);
        _file = file;
        _description = description;
        _runInfo = runInfo;

        String basename = FileUtil.getBaseName(_file, 2);
        setLogFile(FT_LOG.newFile(_file.getParentFile(), basename), appendLog);

        // If there is a .status file created by the Perl pipeline, then connect this job
        // to it.
        File fileStatus = FT_CLUSTER_STATUS.newFile(_file.getParentFile(), basename);
        if (NetworkDrive.exists(fileStatus))
            setStatusFile(fileStatus);

        // If there is an existing status file, make sure this job does not
        // overwrite the original provider.  Both Comet and X!Tandem legacy
        // pipeline create these jobs.
        PipelineStatusFile sf = PipelineService.get().getStatusFile(getStatusFile().getAbsolutePath());
        if (sf != null)
            setProvider(sf.getProvider());
    }

    public ActionURL getStatusHref()
    {
        return null;
    }

    public String getDescription()
    {
        return _description;
    }

    public void run()
    {
        setStatus("LOADING");

        boolean completeStatus = false;
        try
        {
            MS2Manager.importRun(getInfo(), getLogger(), _file, _runInfo, new XarContext(getDescription(), getContainer(), getUser()));
            setStatus(PipelineJob.COMPLETE_STATUS);
            completeStatus = true;
        }
        catch (Exception e)
        {
            getLogger().error("MS2 import failed", e);
        }
        finally
        {
            if (!completeStatus)
            {
                setStatus(PipelineJob.ERROR_STATUS);
            }
        }
    }
}
