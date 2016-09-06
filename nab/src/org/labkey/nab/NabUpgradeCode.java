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
import org.jetbrains.annotations.Nullable;
import org.labkey.api.assay.dilution.DilutionDataHandler;
import org.labkey.api.assay.dilution.DilutionManager;
import org.labkey.api.assay.nab.NabSpecimen;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.DeferredUpgrade;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpObject;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineUrls;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.LimitedUser;
import org.labkey.api.security.User;
import org.labkey.api.security.UserManager;
import org.labkey.api.security.roles.RoleManager;
import org.labkey.api.security.roles.SiteAdminRole;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.nab.multiplate.CrossPlateDilutionNabAssayProvider;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
                    populateWellData(getUser(), protocol, (NabAssayProvider)provider, this);
                }
            }
            info("Total runs processed: " + runCount + "; Total protocols: " + protocolCount);
            setStatus(TaskStatus.complete, "Job finished at: " + DateUtil.nowISO());
        }
    }

    private static void populateWellData(User user, ExpProtocol protocol, NabAssayProvider provider, @Nullable PipelineJob job)
    {
        int runCount = 0;
        DilutionDataHandler dilutionDataHandler = provider.getDataHandler();
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
                                "'. Run details will not be available. Continuing upgrade for other runs.", job);
                    }
                    else
                    {
                        dilutionDataHandler.populateWellData(protocol, run, user, cutoffFormats, wellGroupNameToNabSpecimen);
                    }
                }
                catch (DilutionDataHandler.MissingDataFileException e)
                {
                    warn(dilutionDataHandler.getResourceName(run) + " data file could not be found for run " + run.getRowId() + " (" +
                            run.getName() + ") in container '" + run.getContainer().getPath() +
                            "'. Deleted from file system? Run details will not be available. Continuing upgrade for other runs.", job);
                }
                catch (ExperimentException e)
                {
                    warn("Run " + run.getRowId() + " (" + run.getName() + ") in container '" +
                            run.getContainer().getPath() + "' failed to upgrade due to exception: " +
                            e.getMessage() + ". Continuing upgrade for other runs.", job);
                    for (StackTraceElement stackTraceElement : e.getStackTrace())
                    {
                        warn("\t\t" + stackTraceElement.toString(), job);
                    }
                    warn("", job);
                }
                catch (SQLException e)
                {
                    throw new RuntimeException(e);
                }


                if ((runCount % 500) == 0)
                {
                    if (job != null)
                        job.info("Runs processed: " + runCount);
                    else
                        _log.info("Runs processed: " + runCount);
                }
            }
        }
    }

    private static void warn(String msg, @Nullable PipelineJob job)
    {
        if (job != null)
            job.warn(msg);
        else
            _log.warn(msg);

    }

    @SuppressWarnings({"UnusedDeclaration"})
    @DeferredUpgrade
    public void repairCrossPlateDilutionData(final ModuleContext context)
    {
        if (!context.isNewInstall())
        {
            try
            {
                Container c = ContainerManager.getSharedContainer();
                ViewBackgroundInfo info = new ViewBackgroundInfo(c, context.getUpgradeUser(), PageFlowUtil.urlProvider(PipelineUrls.class).urlBegin(c));
                NabCrossPlateDilutionRepairJob job = new NabCrossPlateDilutionRepairJob("NAb Upgrade Provider", info, PipelineService.get().findPipelineRoot(c));

                PipelineService.get().queueJob(job);
            }
            catch (Exception e)
            {
                _log.error("Cross Plate dilution data repair failed", e);
            }
        }
    }

    private static class NabCrossPlateDilutionRepairJob extends PipelineJob
    {
        public static final String PROCESSING_STATUS = "Processing";

        public NabCrossPlateDilutionRepairJob(String provider, ViewBackgroundInfo info, PipeRoot root) throws IOException, SQLException
        {
            super(provider, info, root);

            File logFile = File.createTempFile("nabCrossPlateDilutionRepair", ".log", root.getRootPath());
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
            return "Repairing NAb cross plate dilution runs.";
        }

        public void run()
        {
            setStatus(PROCESSING_STATUS, "Job started at: " + DateUtil.nowISO());
            info("Starting repair of NAb cross plate dilution runs.");
            Set<ExpProtocol> protocols = new HashSet<>();   // protocols may be accessible by more than one container
            for (Container container : ContainerManager.getAllChildren(ContainerManager.getRoot()))
            {
                if (null != container)
                {
                    for (ExpProtocol protocol : AssayService.get().getAssayProtocols(container))
                        protocols.add(protocol);
                }
            }

            int protocolCount = 0;
            int runCount = 0;
            User upgradeUser = new LimitedUser(UserManager.getGuestUser(), new int[0], Collections.singleton(RoleManager.getRole(SiteAdminRole.class)), false);
            for (ExpProtocol protocol : protocols)
            {
                AssayProvider provider = AssayService.get().getProvider(protocol);
                if (provider instanceof CrossPlateDilutionNabAssayProvider)
                {
                    info("Attempting to repair runs for protocol: " + protocol.getName());
                    protocolCount++;
                    try (DbScope.Transaction transaction = DilutionManager.getSchema().getScope().ensureTransaction())
                    {
                        // delete the old dilution and well data, then regenerate with the fixed algorithm
                        Set<Integer> runIds = protocol.getExpRuns().stream().map((Function<ExpRun, Integer>) ExpObject::getRowId).collect(Collectors.toSet());
                        runCount += runIds.size();
                        SimpleFilter runFilter = new SimpleFilter(new SimpleFilter.InClause(FieldKey.fromString("RunId"), runIds));
                        Table.delete(DilutionManager.getTableInfoWellData(), runFilter);
                        Table.delete(DilutionManager.getTableInfoDilutionData(), runFilter);

                        populateWellData(upgradeUser, protocol, (NabAssayProvider)provider, this);
                        transaction.commit();
                    }
                }
            }
            info("Completed repair of NAb cross plate dilution runs. Protocols processed : " + protocolCount + " runs processed : " + runCount);
            setStatus(TaskStatus.complete, "Job finished at: " + DateUtil.nowISO());
        }
    }
}
