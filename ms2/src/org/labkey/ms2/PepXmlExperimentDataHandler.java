/*
 * Copyright (c) 2005-2016 LabKey Corporation
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

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.Container;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.AbstractExperimentDataHandler;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.security.User;
import org.labkey.api.util.PepXMLFileType;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.api.view.ViewBackgroundInfo;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;

/**
 * User: jeckels
 * Date: Sep 26, 2005
 */
public class PepXmlExperimentDataHandler extends AbstractExperimentDataHandler
{
    public static final String IMPORT_PROPHET_RESULTS = "pipeline, import prophet results";

    public DataType getDataType()
    {
        return null;
    }

    protected boolean shouldImport(ExpData data, XarContext context)
    {
        return context.getJob() == null || !"false".equalsIgnoreCase(context.getJob().getParameters().get(IMPORT_PROPHET_RESULTS));
    }

    public void importFile(@NotNull ExpData data, File dataFile, @NotNull ViewBackgroundInfo info, @NotNull Logger log, @NotNull XarContext context) throws ExperimentException
    {
        if (!shouldImport(data, context))
        {
            log.info("Skipping import of file " + dataFile);
            return;
        }

        ExpRun expRun = data.getRun();
        // We need to no-op if this file is one of the intermediate pep.xml files
        // that are produced in the fraction case.
        // HACK: The combined name used to always be simply "all", but we've changed the
        // HACK: default combined/joined name to the protocol name.
        // HACK: However, we don't have the protocol available here -- luckily the the parent directory name is the protocol name.
        final String joinedBaseName = dataFile.getParentFile().getName();
        if (!PepXmlImporter.isFractionsFile(dataFile, joinedBaseName))
        {
            File parentDir = dataFile.getParentFile();
            File[] combinedFile = parentDir.listFiles(new FileFilter()
            {
                public boolean accept(File f)
                {
                    return PepXmlImporter.isFractionsFile(f, joinedBaseName);
                }
            });
            if (combinedFile.length > 0)
            {
                return;
            }
        }

        if (expRun != null && !expRun.getDataOutputs().contains(data))
        {
            // If this is an intermediate file, don't bother loading it
            return;
        }

        try
        {
            boolean restart = false;

            MS2Run existingMS2Run = MS2Manager.getRunByFileName(dataFile.getParent(), dataFile.getName(), info.getContainer());
            if (existingMS2Run != null)
            {
                if (existingMS2Run.getExperimentRunLSID() != null && expRun != null && !existingMS2Run.getExperimentRunLSID().equals(expRun.getLSID()))
                {
                    ExpRun associatedRun = ExperimentService.get().getExpRun(existingMS2Run.getExperimentRunLSID());
                    if (associatedRun != null)
                    {
                        throw new ExperimentException("The MS2 data '" +
                                dataFile.getPath() + "' is already associated with an experiment run in the folder " +
                                associatedRun.getContainer().getPath() + " (LSID= '" + existingMS2Run.getExperimentRunLSID() + "')");
                    }
                }

                // If the run failed the first time, then restart it.
                if (existingMS2Run.statusId != MS2Importer.STATUS_SUCCESS)
                    restart = true;

                if (expRun != null)
                {
                    existingMS2Run.setExperimentRunLSID(expRun.getLSID());
                    MS2Manager.updateRun(existingMS2Run, null);
                }
            }
            if (existingMS2Run != null && !restart)
            {
                // Don't try to load it again if it's already in the system
                return;
            }

            MS2Run run = MS2Manager.addRun(info, log, dataFile, restart, context);

            if (run == null || run.statusId != MS2Importer.STATUS_SUCCESS)
            {
                throw new ExperimentException("Failed to load MS2 data");
            }

            if (expRun != null)
            {
                run.setExperimentRunLSID(expRun.getLSID());
                MS2Manager.updateRun(run, info.getUser());
            }
        }
        catch (SQLException | IOException | XMLStreamException e)
        {
            throw new ExperimentException(e);
        }
    }

    private MS2Run getMS2Run(File dataFile, Container c)
    {
        return MS2Manager.getRunByFileName(dataFile.getParent(), dataFile.getName(), c);
    }

    public ActionURL getContentURL(ExpData data)
    {
        File dataFile = data.getFile();
        MS2Run run = getMS2Run(dataFile, data.getContainer());
        if (run == null)
        {
            return null;
        }
        return MS2Controller.MS2UrlsImpl.get().getShowRunUrl(null, run);
    }

    public void deleteData(ExpData data, Container container, User user)
    {
        File file = data.getFile();
        if (file != null)
        {
            MS2Run run = getMS2Run(file, container);
            if (run != null)
            {
                MS2Manager.markDeleted(Arrays.asList(run.getRun()), container);
            }
        }
    }

    public void runMoved(ExpData newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user, int oldDataRowID) throws ExperimentException
    {
        updateRunLSID(newData, container, targetContainer, newRunLSID, user);
    }

    private void updateRunLSID(ExpData data, Container originalContainer, Container targetContainer, String lsid, User user)
            throws ExperimentException
    {
        try
        {
            File f = data.getFile();
            if (f != null)
            {
                MS2Run run = getMS2Run(f, originalContainer);
                // Run might be null if it's already been moved, possibly because
                // the pep.xml file is referenced multiple times in the same experiment run.
                if (run != null)
                {
                    MS2Manager.moveRuns(user, Collections.singletonList(run), targetContainer);
                    run = getMS2Run(f, targetContainer);
                    run.setExperimentRunLSID(lsid);
                    MS2Manager.updateRun(run, user);
                }
            }
        }
        catch (UnauthorizedException e)
        {
            throw new ExperimentException(e);
        }
    }

    public Priority getPriority(ExpData data)
    {
        PepXMLFileType ft = new PepXMLFileType();
        if (ft.isType(data.getFile()))
        {
            return Priority.HIGH;
        }
        return null;
    }

    public void beforeMove(ExpData oldData, Container container, User user) throws ExperimentException
    {
        updateRunLSID(oldData, container, container, null, user);
    }
}
