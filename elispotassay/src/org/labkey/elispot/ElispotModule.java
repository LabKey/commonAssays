/*
 * Copyright (c) 2008-2014 LabKey Corporation
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

package org.labkey.elispot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.UserSchema;
import org.labkey.api.study.PlateService;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.PlateBasedAssayProvider;
import org.labkey.api.study.assay.plate.ExcelPlateReader;
import org.labkey.api.study.assay.plate.PlateReaderService;
import org.labkey.api.study.assay.plate.TextPlateReader;
import org.labkey.api.view.WebPartFactory;
import org.labkey.elispot.pipeline.ElispotPipelineProvider;
import org.labkey.elispot.plate.AIDPlateReader;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ElispotModule extends DefaultModule
{
    public String getName()
    {
        return "ELISpotAssay";
    }

    public double getVersion()
    {
        return 14.31;
    }

    protected void init()
    {
        addController("elispot-assay", ElispotController.class);
    }

    @NotNull
    protected Collection<WebPartFactory> createWebPartFactories()
    {
        return Collections.emptyList();
    }

    public boolean hasScripts()
    {
        return true;
    }

    @NotNull
    @Override
    public Collection<String> getSchemaNames()
    {
        return Collections.singleton("elispotassay");
    }

    public void doStartup(ModuleContext moduleContext)
    {
        PlateService.get().registerPlateTypeHandler(new ElispotPlateTypeHandler());
        ExperimentService.get().registerExperimentDataHandler(new ElispotDataHandler());

        PlateBasedAssayProvider provider = new ElispotAssayProvider();
        AssayService.get().registerAssayProvider(provider);

        PlateReaderService.registerPlateReader(provider, new ExcelPlateReader());
        PlateReaderService.registerPlateReader(provider, new TextPlateReader());
        PlateReaderService.registerPlateReader(provider, new AIDPlateReader());

        PipelineService.get().registerPipelineProvider(new ElispotPipelineProvider(this));
    }

    @Nullable
    @Override
    public UpgradeCode getUpgradeCode()
    {
        return new ElispotUpgradeCode();
    }

    public static class ElispotUpgradeCode implements UpgradeCode
    {
        /**
         * Upgrade the plate reader type information. Invoked from elispot-14.30-14.31.sql
         * @param context
         */
        public void upgradePlateReaderList(ModuleContext context)
        {
            if (!context.isNewInstall())
            {
                Container root = ContainerManager.getRoot();

                for (Container child : ContainerManager.getAllChildren(root))
                {
                    upgradePlateReaderList(context, child);
                }
            }
        }

        private void upgradePlateReaderList(ModuleContext context, Container container)
        {
            UserSchema schema = QueryService.get().getUserSchema(context.getUpgradeUser(), container, "lists");
            TableInfo table = schema.getTable("ElispotPlateReader");

            List<Map<String, Object>> keys = Collections.singletonList(Collections.singletonMap(PlateReaderService.PLATE_READER_PROPERTY, (Object)"AID"));
            if (table != null)
            {
                try
                {
                    QueryUpdateService qus = table.getUpdateService();
                    if (qus != null)
                    {
                        List<Map<String, Object>> rows = qus.getRows(context.getUpgradeUser(), container, keys);
                        if (rows.size() == 1)
                        {
                            // need to change the reader types to map to the new reader instances
                            rows.get(0).put(PlateReaderService.READER_TYPE_PROPERTY, AIDPlateReader.TYPE);
                            qus.updateRows(context.getUpgradeUser(), container, rows, null, null, null);
                        }
                    }
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
