/*
 * Copyright (c) 2010-2014 LabKey Corporation
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

import org.labkey.api.data.DbScope;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpProtocolApplication;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.study.assay.DefaultAssayRunCreator;
import org.labkey.api.util.DateUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.ms2.MS2Module;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * User: jeckels
 * Date: Apr 7, 2010
 */
public class MSPictureUpgradeJob extends PipelineJob implements Serializable
{
    public static final String PROCESSING_STATUS = "Processing";

    private static final String UPGRADE_RUN = "Checking MS2 run: %s/%s for additional mspicture files to attach";
    private static final String UPGRADE_ATTACH = "Attaching file %s to run %s";
    private static final String UPGRADE_EXCEPTION = "An unexpected error occurred attempting to upgrade the MS2 run: %s, skipping.";
    private static final String UPGRADE_STATS = "Upgrade job complete. Number of runs checked: %s.";

    private static final DataType DATA_TYPE = new DataType("msPictureOutput");

    public MSPictureUpgradeJob(ViewBackgroundInfo info, PipeRoot root) throws IOException
    {
        super(null, info, root);

        File logFile = File.createTempFile("attachMSPicture", ".log", root.ensureSystemDirectory());
        setLogFile(logFile);
    }

    public ActionURL getStatusHref()
    {
        return null;
    }

    public String getDescription()
    {
        return "mspicture attach files upgrade job";
    }

    @Override
    public void run()
    {
        setStatus(PROCESSING_STATUS, "Job started at: " + DateUtil.nowISO());
        int runsProcessed = 0;

        try
        {
            // get all the MS2 protocol instances
            for (ExpProtocol protocol : ExperimentService.get().getAllExpProtocols())
            {
                if (MS2Module.SEARCH_RUN_TYPE.getPriority(protocol) != null)
                {
                    for (ExpRun run : protocol.getExpRuns())
                    {
                        runsProcessed++;
                        processRun(run);
                    }
                }
            }
        }
        catch (Exception e)
        {
            error("Error occurred running the mspicture upgrade background job", e);
            setStatus(TaskStatus.error, "Job finished at: " + DateUtil.nowISO());
        }
        finally
        {
            info(String.format(UPGRADE_STATS, runsProcessed));
            setStatus(TaskStatus.complete, "Job finished at: " + DateUtil.nowISO());
        }
    }

    private void processRun(ExpRun run)
    {
        info(String.format(UPGRADE_RUN, run.getContainer().getPath(), run.getName()));
        try
        {
            Map<File, String> filesToAdd = new HashMap<>();
            Set<File> existingFiles = new HashSet<>();

            // Find the mzXML file(s) to figure out the base name for the files
            findRelatedFiles(run, filesToAdd, existingFiles, run.getDataOutputs());
            findRelatedFiles(run, filesToAdd, existingFiles, run.getDataInputs().keySet());

            // Don't re-add something that's already attached to the run
            for (File existingFile : existingFiles)
            {
                filesToAdd.remove(existingFile);
            }

            if (!filesToAdd.isEmpty())
            {
                addFiles(run, filesToAdd);
            }
        }
        catch (Exception e)
        {
            error(String.format(UPGRADE_EXCEPTION, run.getName()), e);
        }
    }

    private void findRelatedFiles(ExpRun run, Map<File, String> filesToAdd, Set<File> existingFiles, Collection<ExpData> datas)
    {
        for (ExpData expData : datas)
        {
            File f = expData.getFile();
            if (f != null && expData.isFileOnDisk())
            {
                existingFiles.add(f);
                if (AbstractMS2SearchProtocol.FT_MZXML.isType(expData.getFile()))
                {
                    final String baseName = AbstractMS2SearchProtocol.FT_MZXML.getBaseName(f);
                    if (baseName != null)
                    {
                        // Grab all the files that are related based on naming convention
                        File[] relatedFiles = run.getFilePathRoot().listFiles(new FileFilter()
                        {
                            public boolean accept(File pathname)
                            {
                                String name = pathname.getName();
                                return name.startsWith(baseName) && name.endsWith(".png");
                            }
                        });
                        if (relatedFiles != null)
                        {
                            for (File relatedFile : relatedFiles)
                            {
                                filesToAdd.put(relatedFile, baseName);
                            }
                        }
                    }
                }
            }
        }
    }

    private ExpProtocolApplication findOutputProtocolApp(ExpRun run)
    {
        // Find the output step in the run
        for (ExpProtocolApplication protocolApplication : run.getProtocolApplications())
        {
            if (protocolApplication.getApplicationType() == ExpProtocol.ApplicationType.ExperimentRunOutput)
            {
                return protocolApplication;
            }
        }
        return null;
    }

    private void addFiles(ExpRun run, Map<File, String> filesToAdd)
    {
        ExpProtocolApplication outputProtocolApp = findOutputProtocolApp(run);
        if (outputProtocolApp == null)
        {
            info("Could not find output protocol application for run " + run.getName());
        }
        else
        {
            try (DbScope.Transaction transaction = ExperimentService.get().ensureTransaction())
            {
                for (Map.Entry<File, String> entry : filesToAdd.entrySet())
                {
                    File file = entry.getKey();
                    String baseName = entry.getValue();
                    // If not, make up a new type and role for it
                    String roleName = file.getName().substring(baseName.length());
                    while (roleName.length() > 0 && (roleName.startsWith(".") || roleName.startsWith("-") || roleName.startsWith("_") || roleName.startsWith(" ")))
                    {
                        roleName = roleName.substring(1);
                    }
                    if ("".equals(roleName))
                    {
                        roleName = null;
                    }
                    
                    ExpData outputData = DefaultAssayRunCreator.createData(run.getContainer(), file, file.getName(), DATA_TYPE, true);
                    if (outputData.getSourceApplication() != null)
                    {
                        info("File " + file.getName() + " is already associated with another run");
                    }
                    else
                    {
                        // Mark the file as being created by the output step, and attach it
                        // based on its role name
                        info(String.format(UPGRADE_ATTACH, outputData.getFile(), run.getName()));
                        outputData.setSourceApplication(outputProtocolApp);
                        outputData.setRun(run);
                        outputData.save(getUser());
                        outputProtocolApp.addDataInput(getUser(), outputData, roleName);
                    }
                }
                transaction.commit();
            }
        }
    }
}
