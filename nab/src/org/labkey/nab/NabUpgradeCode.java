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
import org.labkey.api.query.FieldKey;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.util.Pair;

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
            Set<ExpProtocol> protocols = new HashSet<>();   // protocols may be accessible by more than one container
            Set<Container> allContainers = ContainerManager.getAllChildren(ContainerManager.getRoot());
            for (Container container : allContainers)
            {
                if (null != container)
                    for (ExpProtocol protocol : AssayService.get().getAssayProtocols(container))
                        protocols.add(protocol);
            }

            for (ExpProtocol protocol : protocols)
            {
                AssayProvider provider = AssayService.get().getProvider(protocol);
                if (provider instanceof NabAssayProvider)
                {
                    DilutionDataHandler dilutionDataHandler = ((NabAssayProvider) provider).getDataHandler();
                    for (ExpRun run : protocol.getExpRuns())
                    {
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
                                _log.error(dilutionDataHandler.getResourceName(run) + " data file could not be found for run " +
                                        run.getName() + ".  Deleted from file system? Run details will not be available.");
                            }
                            else
                            {
                                dilutionDataHandler.populateWellData(protocol, run, context.getUpgradeUser(), cutoffFormats, wellGroupNameToNabSpecimen);
                            }
                        }
                        catch (ExperimentException | SQLException e)
                        {
                            _log.error("Run " + run.getRowId() + " failed to upgrade due to exception: " + e.getMessage());
                        }
                    }

                }
            }
        }
    }
}
