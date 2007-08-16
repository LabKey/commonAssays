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
package org.labkey.ms2;

import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.exp.*;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.AbstractExperimentDataHandler;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.security.User;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URI;
import java.sql.SQLException;
import java.util.Collections;

/**
 * User: jeckels
 * Date: Sep 26, 2005
 */
public class PepXmlExperimentDataHandler extends AbstractExperimentDataHandler
{
    public void importFile(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        ExpRun expRun = data.getRun();
        // We need to no-op if this file is one of the intermediate pep.xml files
        // that are produced in the fraction case.
        if (!PepXmlImporter.isFractionsFile(dataFile))
        {
            File parentDir = dataFile.getParentFile();
            File[] combinedFile = parentDir.listFiles(new FileFilter()
            {
                public boolean accept(File f)
                {
                    return PepXmlImporter.isFractionsFile(f);
                }
            });
            if (combinedFile.length > 0)
            {
                return;
            }

        }

        try
        {
            boolean restart = false;

            MS2Run existingRun = MS2Manager.getRunByFileName(dataFile.getParent(), dataFile.getName(), info.getContainer());
            if (existingRun != null)
            {
                if (existingRun.getExperimentRunLSID() != null && !existingRun.getExperimentRunLSID().equals(expRun.getLSID()))
                {
                    ExperimentRun associatedRun = ExperimentService.get().getExperimentRun(existingRun.getExperimentRunLSID());
                    if (associatedRun != null)
                    {
                        throw new ExperimentException("The MS2 data '" +
                                dataFile.getPath() + "' is already associated with an experiment run in the folder " +
                                ContainerManager.getForId(associatedRun.getContainer()).getPath() + " (LSID= '" + existingRun.getExperimentRunLSID() + "')");
                    }
                }

                // If the run failed the first time, then restart it.
                if (existingRun.statusId != MS2Importer.STATUS_SUCCESS)
                    restart = true;

                existingRun.setExperimentRunLSID(expRun.getLSID());
                MS2Manager.updateRun(existingRun, null);
            }
            if (existingRun != null && !restart)
            {
                // Don't try to load it again if it's already in the system
                return;
            }

            int runId = MS2Manager.addRun(info, log, dataFile, restart, context);

            MS2Run run = MS2Manager.getRun(runId);

            if (run == null || run.statusId != MS2Importer.STATUS_SUCCESS)
            {
                throw new ExperimentException("Failed to load MS2 data");
            }

            run.setExperimentRunLSID(expRun.getLSID());
            MS2Manager.updateRun(run, info.getUser());
        }
        catch (SQLException e)
        {
            throw new ExperimentException(e);
        }
        catch (IOException e)
        {
            throw new ExperimentException(e);
        }
        catch (XMLStreamException e)
        {
            throw new ExperimentException(e);
        }
    }

    private MS2Run getMS2Run(File dataFile, Container c)
    {
        return MS2Manager.getRunByFileName(dataFile.getParent(), dataFile.getName(), c);
    }

    public URLHelper getContentURL(HttpServletRequest request, Container container, ExpData data) throws ExperimentException
    {
        File dataFile = data.getDataFile();
        MS2Run run = getMS2Run(dataFile, container);
        if (run == null)
        {
            return null;
        }
        return new ViewURLHelper(request, "MS2", "showRun", container.getPath()).addParameter("run", Integer.toString(run.getRun()));
    }

    public void deleteData(Data data, Container container, User user) throws ExperimentException
    {
        try
        {
            MS2Run run = getMS2Run(new File(new URI(data.getDataFileUrl())), container);
            if (run != null)
            {
                run.setExperimentRunLSID(null);
                MS2Manager.updateRun(run, null);
                MS2Manager.markAsDeleted( new Integer[]{ run.getRun() }, container, user);
            }
        }
        catch (URISyntaxException e)
        {
            throw new ExperimentException(e);
        }
        catch (SQLException e)
        {
            throw new ExperimentException(e);
        }
    }

    public void runMoved(Data newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user) throws ExperimentException
    {
        try
        {
            File f = newData.getFile();
            if (f != null)
            {
                MS2Run run = getMS2Run(f, container);
                // Run might be null if it's already been moved, possibly because
                // the pep.xml file is referenced multiple times in the same experiment run.
                if (run != null)
                {
                    MS2Manager.moveRuns(user, Collections.singletonList(run), targetContainer);
                    run = getMS2Run(f, targetContainer);
                    run.setExperimentRunLSID(newRunLSID);
                    MS2Manager.updateRun(run, user);
                }
            }
        }
        catch (UnauthorizedException e)
        {
            throw new ExperimentException(e);
        }
        catch (SQLException e)
        {
            throw new ExperimentException(e);
        }
    }

    public Priority getPriority(Data data)
    {
        File f = data.getFile();
        if (f != null && f.getName().toLowerCase().endsWith(".pep.xml"))
        {
            return Priority.HIGH;
        }
        return null;
    }
}
