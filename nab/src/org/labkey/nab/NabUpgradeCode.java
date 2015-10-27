/*
 * Copyright (c) 2015 LabKey Corporation
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
package org.labkey.nab;

import org.apache.log4j.Logger;
import org.labkey.api.assay.dilution.DilutionDataHandler;
import org.labkey.api.assay.dilution.DilutionManager;
import org.labkey.api.assay.nab.NabSpecimen;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DeferredUpgrade;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineUrls;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.query.FieldKey;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ViewBackgroundInfo;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by davebradlee on 8/17/15.
 *
 */
public class NabUpgradeCode implements UpgradeCode
{
    private static final Logger _log = Logger.getLogger(NabUpgradeCode.class);

    // Invoked by nab-15.21-15.22.sql
    @SuppressWarnings({"UnusedDeclaration"})
    @DeferredUpgrade
    public void upgradeDilutionAssayWithNewTables(final ModuleContext context)
    {
        if (!context.isNewInstall())
        {
            try
            {
                Container c = ContainerManager.getSharedContainer();
                ViewBackgroundInfo info = new ViewBackgroundInfo(c, context.getUpgradeUser(), PageFlowUtil.urlProvider(PipelineUrls.class).urlBegin(c));
                NabDilutionUpgradeJob job = new NabDilutionUpgradeJob("NAb Upgrade Provider", info, PipelineService.get().findPipelineRoot(c));

                PipelineService.get().queueJob(job);
            }
            catch (Exception e)
            {
                _log.error("NAb dilution table upgrade failed", e);
            }
        }
    }

    private static class NabDilutionUpgradeJob extends PipelineJob
    {
        public static final String PROCESSING_STATUS = "Processing";

        public NabDilutionUpgradeJob(String provider, ViewBackgroundInfo info, PipeRoot root) throws IOException, SQLException
        {
            super(provider, info, root);

            File logFile = File.createTempFile("nabDilutionUpgrade", ".log", root.getRootPath());
            setLogFile(logFile);
        }

        @Override
        public URLHelper getStatusHref()
        {
            return null;
        }

        @Override
        public String getDescription()
        {
            return "NAb upgrade dilution data to new tables.";
        }

        public void run()
        {
            int runCount = 0;
            int protocolCount = 0;
            Set<ExpProtocol> protocols = new HashSet<>();   // protocols may be accessible by more than one container
            Set<Container> allContainers = ContainerManager.getAllChildren(ContainerManager.getRoot());
            for (Container container : allContainers)
            {
                if (null != container)
                    for (ExpProtocol protocol : AssayService.get().getAssayProtocols(container))
                        protocols.add(protocol);
            }

            setStatus(PROCESSING_STATUS, "Job started at: " + DateUtil.nowISO());
            info("Starting NAb dilution table upgrade for " + protocols.size() + " assay instances.");

            for (ExpProtocol protocol : protocols)
            {
                AssayProvider provider = AssayService.get().getProvider(protocol);
                if (provider instanceof NabAssayProvider)
                {
                    protocolCount += 1;
                    DilutionDataHandler dilutionDataHandler = ((NabAssayProvider) provider).getDataHandler();
                    for (ExpRun run : protocol.getExpRuns())
                    {
                        if (!DilutionDataHandler.isWellDataPopulated(run))
                        {
                            runCount += 1;
                            Map<Integer, String> cutoffFormats = DilutionDataHandler.getCutoffFormats(protocol, run);
                            final Map<String, Pair<Integer, String>> wellGroupNameToNabSpecimen = new HashMap<>();
                            TableInfo tableInfo = DilutionManager.getTableInfoNAbSpecimen();
                            SimpleFilter filter = new SimpleFilter(FieldKey.fromString("RunId"), run.getRowId());
                            new TableSelector(tableInfo, filter, null).forEach((NabSpecimen nabSpecimen) ->
                            {
                                wellGroupNameToNabSpecimen.put(nabSpecimen.getWellgroupName(), new Pair<>(nabSpecimen.getRowId(), nabSpecimen.getSpecimenLsid()));
                            }, NabSpecimen.class);

                            try
                            {
                                if (wellGroupNameToNabSpecimen.isEmpty())
                                {
                                    warn(dilutionDataHandler.getResourceName(run) + " run data could not be found for run " + run.getRowId() + " (" +
                                            run.getName() + ") in container '" + run.getContainer().getPath() +
                                            "'. Run details will not be available. Continuing upgrade for other runs.");
                                }
                                else
                                {
                                    dilutionDataHandler.populateWellData(protocol, run, getUser(), cutoffFormats, wellGroupNameToNabSpecimen);
                                }
                            }
                            catch (DilutionDataHandler.MissingDataFileException e)
                            {
                                warn(dilutionDataHandler.getResourceName(run) + " data file could not be found for run " + run.getRowId() + " (" +
                                        run.getName() + ") in container '" + run.getContainer().getPath() +
                                        "'. Deleted from file system? Run details will not be available. Continuing upgrade for other runs.");
                            }
                            catch (ExperimentException | SQLException e)
                            {
                                warn("Run " + run.getRowId() + " (" + run.getName() + ") in container '" +
                                        run.getContainer().getPath() + "' failed to upgrade due to exception: " +
                                        e.getMessage() + ". Continuing upgrade for other runs.");
                                for (StackTraceElement stackTraceElement : e.getStackTrace())
                                {
                                    warn("\t\t" + stackTraceElement.toString());
                                }
                                warn("");
                            }

                            if ((runCount % 500) == 0)
                                info("Runs processed: " + runCount);
                        }

                    }
                }
            }
            info("Total runs processed: " + runCount + "; Total protocols: " + protocolCount);
            setStatus(TaskStatus.complete, "Job finished at: " + DateUtil.nowISO());
        }
    }
}
