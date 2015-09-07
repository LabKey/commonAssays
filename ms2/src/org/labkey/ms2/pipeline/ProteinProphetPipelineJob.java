/*
 * Copyright (c) 2006-2015 LabKey Corporation
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

import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.ms2.*;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.XarContext;

import javax.xml.stream.XMLStreamException;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Imports an existing ProteinProphet .prot.xml output file.
 * User: jeckels
 * Date: Feb 17, 2006
 */
public class ProteinProphetPipelineJob extends PipelineJob
{
    private final File _file;

    public ProteinProphetPipelineJob(ViewBackgroundInfo info, File file, PipeRoot root) throws SQLException
    {
        super(ProteinProphetPipelineProvider.NAME, info, root);
        _file = file;

        setLogFile(new File(_file.getParentFile(), _file.getName() + ".log"));
    }

    public ActionURL getStatusHref()
    {
        return null;
    }

    public String getDescription()
    {
        return _file.getName();
    }

    public void run()
    {
        if (!setStatus("LOADING"))
        {
            return;
        }
        boolean completeStatus = false;
        try
        {
            ProteinProphetImporter importer = new ProteinProphetImporter(_file, null, new XarContext(null, getContainer(), getUser()));
            MS2Run run = importer.importFile(getInfo(), getLogger());
            MS2Manager.ensureWrapped(run, getUser());
            setStatus(TaskStatus.complete);
            completeStatus = true;
        }
        catch (SQLException | IOException | XMLStreamException | ExperimentException | RuntimeException e)
        {
            getLogger().error("ProteinProphet load failed", e);
        }
        finally
        {
            if (!completeStatus)
            {
                setStatus(TaskStatus.error);
            }
        }

    }
}
