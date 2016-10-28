/*
 * Copyright (c) 2006-2016 LabKey Corporation
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

package org.labkey.ms2.pipeline.mascot;

import org.apache.commons.io.FileUtils;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.util.FileUtil;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.ms2.MS2Importer;
import org.labkey.ms2.pipeline.MS2ImportPipelineJob;

import java.io.File;
import java.io.IOException;

/**
 * User: jeckels
 * Date: Mar 9, 2006
 */
public class MascotImportPipelineJob extends MS2ImportPipelineJob
{
    private final File _file;

    public MascotImportPipelineJob(ViewBackgroundInfo info, File file, String description,
                                   MS2Importer.RunInfo runInfo, PipeRoot root)
    {
        super(info, file, description, runInfo, root);
        _file = file;
    }

    public void run()
    {
        // Clear out any previous errors
        setErrors(0);
        if (!setStatus("INITIALIZING"))
        {
            return;
        }

        String _dirAnalysis = _file.getParent();
        String _baseName = FileUtil.getBaseName(_file);
        File dirWork = new File(_dirAnalysis, _baseName + ".import.work");
        File workFile = new File(dirWork.getAbsolutePath(), _file.getName());

        boolean completeStatus = false;
        try
        {
            if (!dirWork.exists() && !dirWork.mkdir())
            {
                getLogger().error("Failed create working folder "+dirWork.getAbsolutePath()+".");
                return;
            }

            try
            {
                FileUtils.copyFile(_file, workFile);
            }
            catch (IOException x)
            {
                getLogger().error("Failed to move Mascot result file to working folder as "+workFile.getAbsolutePath(), x);
                return;
            }

            // let's import the .dat file
            super.run();
            if (getErrors() == 0)
            {

                if (!workFile.delete())
                {
                    getLogger().error("Failed to delete " + workFile.getAbsolutePath());
                    return;
                }
                else if (!dirWork.delete())
                {
                    getLogger().error("Failed to delete " + dirWork.getAbsolutePath());
                    return;
                }
                else
                {
                    setStatus(TaskStatus.complete);
                }
                completeStatus = true;
            }
        }
        catch (Exception e)
        {
            getLogger().error("MS2 import failed", e);
        }
        finally
        {
            if (!completeStatus)
            {
                setStatus(TaskStatus.error);
            }
            if (workFile.exists())
            {
                workFile.delete();
            }
        }
    }
}
