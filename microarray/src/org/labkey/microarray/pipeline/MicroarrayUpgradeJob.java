/*
 * Copyright (c) 2009-2010 LabKey Corporation
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
package org.labkey.microarray.pipeline;

import org.labkey.api.exp.api.*;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.util.DateUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.microarray.MicroarrayModule;
import org.labkey.microarray.assay.MicroarrayAssayProvider;

import java.io.*;
import java.util.*;

/**
 * User: jeckels
 * Date: Apr 7, 2010
 */
public class MicroarrayUpgradeJob extends PipelineJob implements Serializable
{
    public static final String PROCESSING_STATUS = "Processing";

    private static final String UPGRADE_RUN = "Checking microarray run: %s/%s for additional files to attach";
    private static final String UPGRADE_ATTACH = "Attaching file %s to run %s";
    private static final String UPGRADE_EXCEPTION = "An unexpected error occurred attempting to upgrade the microarray run: %s, skipping.";
    private static final String UPGRADE_STATS = "Upgrade job complete. Number of assay instances checked: %s. Number of assay runs checked: %s.";

    public MicroarrayUpgradeJob(String provider, ViewBackgroundInfo info) throws IOException
    {
        super(provider, info);
        init();
    }

    private void init() throws IOException
    {
        PipeRoot root = PipelineService.get().findPipelineRoot(getInfo().getContainer());
        if (root == null)
        {
            throw new FileNotFoundException("Could not find pipeline root on disk for container " + getInfo().getContainer().getPath());
        }
        File logFile = File.createTempFile("upgradeMicroarray", ".log", root.getRootPath());
        setLogFile(logFile);
    }

    public ActionURL getStatusHref()
    {
        return null;
    }

    public String getDescription()
    {
        return "Microarray attach files upgrade job";
    }

    @Override
    public void run()
    {
        setStatus(PROCESSING_STATUS, "Job started at: " + DateUtil.nowISO());
        int assayInstances = 0;
        int runsProcessed = 0;

        try
        {
            // get all the microarray instances
            for (ExpProtocol protocol : ExperimentService.get().getAllExpProtocols())
            {
                AssayProvider provider = AssayService.get().getProvider(protocol);
                if (provider instanceof MicroarrayAssayProvider)
                {
                    MicroarrayAssayProvider microarrayProvider = (MicroarrayAssayProvider)provider;
                    assayInstances++;
                    for (ExpRun run : protocol.getExpRuns())
                    {
                        info(String.format(UPGRADE_RUN, run.getContainer().getPath(), run.getName()));
                        try
                        {
                            File mageMLFile = null;
                            Set<File> allFiles = new HashSet<File>();
                            // Find the MageML file to figure out the base name for the files
                            for (ExpData expData : run.getDataOutputs())
                            {
                                File f = expData.getFile();
                                if (f != null && expData.isFileOnDisk())
                                {
                                    allFiles.add(f);
                                    if (MicroarrayModule.MAGE_ML_INPUT_TYPE.getFileType().isType(expData.getFile()))
                                    {
                                        mageMLFile = expData.getFile();
                                    }
                                }
                            }
                            ExpProtocolApplication outputProtocolApp = null;
                            if (mageMLFile != null)
                            {
                                Map<ExpData, String> outputDatas = new HashMap<ExpData, String>();
                                // Figure out what other files are related based on naming convention
                                microarrayProvider.addRelatedOutputDatas(run.getContainer(), outputDatas, mageMLFile, MicroarrayModule.RELATED_INPUT_TYPES);
                                ExperimentService.get().beginTransaction();
                                try
                                {
                                    for (Map.Entry<ExpData, String> entry : outputDatas.entrySet())
                                    {
                                        ExpData outputData = entry.getKey();
                                        // If it's not already attached to the run, set it up as an output
                                        if (!allFiles.contains(outputData.getFile()))
                                        {
                                            if (outputProtocolApp == null)
                                            {
                                                // Find the output step in the run
                                                for (ExpProtocolApplication protocolApplication : run.getProtocolApplications())
                                                {
                                                    if (protocolApplication.getApplicationType() == ExpProtocol.ApplicationType.ExperimentRunOutput)
                                                    {
                                                        outputProtocolApp = protocolApplication;
                                                        break;
                                                    }
                                                }
                                            }

                                            if (outputProtocolApp != null)
                                            {
                                                // Mark the file as being created by the output step, and attach it
                                                // based on its role name
                                                info(String.format(UPGRADE_ATTACH, outputData.getFile(), run.getName()));
                                                outputData.setSourceApplication(outputProtocolApp);
                                                outputData.setRun(run);
                                                outputData.save(getUser());
                                                outputProtocolApp.addDataInput(getUser(), outputData, entry.getValue());
                                            }
                                        }
                                    }
                                    ExperimentService.get().commitTransaction();
                                }
                                finally
                                {
                                    ExperimentService.get().closeTransaction();
                                }
                            }
                        }
                        catch (Exception e)
                        {
                            error(String.format(UPGRADE_EXCEPTION, run.getName()), e);
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            error("Error occurred running the microarray upgrade background job", e);
            setStatus(PipelineJob.ERROR_STATUS, "Job finished at: " + DateUtil.nowISO());
        }
        finally
        {
            info(String.format(UPGRADE_STATS, assayInstances, runsProcessed));
            setStatus(PipelineJob.COMPLETE_STATUS, "Job finished at: " + DateUtil.nowISO());
        }
    }
}
