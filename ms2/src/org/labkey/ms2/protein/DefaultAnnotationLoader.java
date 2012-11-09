/*
 * Copyright (c) 2006-2012 LabKey Corporation
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

package org.labkey.ms2.protein;

import org.labkey.api.data.ContainerManager;
import org.labkey.api.pipeline.CancelledException;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ViewBackgroundInfo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

/**
 * User: brittp
 * Date: Dec 23, 2005
 * Time: 4:00:42 PM
 */
public abstract class DefaultAnnotationLoader extends PipelineJob
{
    protected File _file;
    protected String _comment = null;
    protected int currentInsertId = 0;

    private static final String FORMAT_STRING = "yyyy-MM-dd-HH-mm-ss";


    public DefaultAnnotationLoader(File file, ViewBackgroundInfo info, PipeRoot pipeRoot) throws IOException
    {
        super(ProteinAnnotationPipelineProvider.NAME, info, pipeRoot);
        _file = FileUtil.getAbsoluteCaseSensitiveFile(file);
        PipeRoot pipelineRoot = PipelineService.get().findPipelineRoot(ContainerManager.getSharedContainer());
        if (pipelineRoot == null)
        {
            throw new IOException("No pipeline root configured for the /Shared project");
        }
        File logDir = pipelineRoot.resolvePath("proteinAnnotationImport");
        logDir.mkdir();
        if (!logDir.isDirectory())
        {
            throw new IOException("Could not create directory for log file: " + logDir);
        }
        setLogFile(new File(logDir, file.getName() + "." + DateUtil.formatDateTime(new Date(), FORMAT_STRING) + ".log"));
    }

    @Override
    public URLHelper getStatusHref()
    {
        return null;
    }

    public void validate() throws IOException
    {
        if (!NetworkDrive.exists(_file))
        {
            throw new FileNotFoundException("Can't open file '" + _file + "'");
        }
        if (!_file.isFile())
        {
            throw new FileNotFoundException(_file + " is available, but is not a file");
        }
    }

    public void handleThreadStateChangeRequests()
    {
        if (checkInterrupted())
        {
            throw new CancelledException();
        }
    }
    
    public void handleThreadStateChangeRequests(String message)
    {
        info(message);
        handleThreadStateChangeRequests();
    }

    public void setComment(String c)
    {
        this._comment = c;
    }

    public String getComment()
    {
        return _comment;
    }
}
