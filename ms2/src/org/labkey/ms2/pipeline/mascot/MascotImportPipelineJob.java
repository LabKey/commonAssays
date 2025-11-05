/*
 * Copyright (c) 2008-2019 LabKey Corporation
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.util.FileUtil;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.ms2.MS2Importer;
import org.labkey.ms2.pipeline.MS2ImportPipelineJob;
import org.labkey.vfs.FileLike;

import java.io.IOException;

/**
 * User: jeckels
 * Date: Mar 9, 2006
 */
public class MascotImportPipelineJob extends MS2ImportPipelineJob
{
    @JsonCreator
    protected MascotImportPipelineJob(
            @JsonProperty("_file") FileLike file,
            @JsonProperty("_description") String description,
            @JsonProperty("_runInfo") MS2Importer.RunInfo runInfo)
    {
        super(file, description, runInfo);
    }

    public MascotImportPipelineJob(ViewBackgroundInfo info, FileLike file, String description,
                                   MS2Importer.RunInfo runInfo, PipeRoot root)
    {
        super(info, file, description, runInfo, root);
    }

    @Override
    public void run()
    {
        // Clear out any previous errors
        setErrors(0);
        if (!setStatus("INITIALIZING"))
        {
            return;
        }

        FileLike dirAnalysis = _file.getParent();
        String baseName = FileUtil.getBaseName(_file);
        FileLike dirWork = dirAnalysis.resolveChild(baseName + ".import.work");
        FileLike workFile = dirWork.resolveChild(_file.getName());

        boolean completeStatus = false;
        try
        {
            if (!dirWork.exists() && !FileUtil.mkdir(dirWork))
            {
                getLogger().error("Failed create working folder "+dirWork+".");
                return;
            }

            try
            {
                FileUtil.copyFile(_file, workFile);
            }
            catch (IOException x)
            {
                getLogger().error("Failed to move Mascot result file to working folder as "+workFile, x);
                return;
            }

            // let's import the .dat file
            super.run();
            if (getErrors() == 0)
            {

                if (!workFile.delete())
                {
                    getLogger().error("Failed to delete " + workFile);
                    return;
                }
                else if (!dirWork.delete())
                {
                    getLogger().error("Failed to delete " + dirWork);
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
                try
                {
                    workFile.delete();
                }
                catch (IOException e)
                {
                    getLogger().error("Failed to delete " + workFile, e);
                }
            }
        }
    }
}
