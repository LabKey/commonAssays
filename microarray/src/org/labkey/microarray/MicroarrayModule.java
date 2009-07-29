/*
 * Copyright (c) 2008-2009 LabKey Corporation
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

package org.labkey.microarray;

import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.ExperimentRunTypeSource;
import org.labkey.api.exp.ExperimentRunType;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.query.QueryView;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.*;
import org.labkey.microarray.assay.MageMLDataHandler;
import org.labkey.microarray.assay.MicroarrayAssayProvider;
import org.labkey.microarray.pipeline.MicroarrayPipelineProvider;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class MicroarrayModule extends DefaultModule
{
    public static final String NAME = "Microarray";
    public static final String WEBPART_MICROARRAY_RUNS = "Microarray Runs";
    public static final String WEBPART_MICROARRAY_STATISTICS = "Microarray Summary";
    public static final String WEBPART_PENDING_FILES = "Pending MAGE-ML Files";
    private static final String CONTROLLER_NAME = "microarray";

    public static final DataType MAGE_ML_DATA_TYPE = new DataType("MicroarrayAssayData");
    public static final DataType QC_REPORT_DATA_TYPE = new DataType("MicroarrayQCData");
    public static final DataType IMAGE_DATA_TYPE = new DataType("MicroarrayImageData");
    public static final DataType FEATURES_DATA_TYPE = new DataType("MicroarrayFeaturesData");
    public static final DataType GRID_DATA_TYPE = new DataType("MicroarrayGridData");

    public String getName()
    {
        return "Microarray";
    }

    public double getVersion()
    {
        return 9.20;
    }

    protected void init()
    {
        addController(CONTROLLER_NAME, MicroarrayController.class);
        MicroarraySchema.register();
    }

    protected Collection<? extends WebPartFactory> createWebPartFactories()
    {
        return Arrays.asList(
            new BaseWebPartFactory(WEBPART_MICROARRAY_RUNS)
            {
                public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart)
                {
                    QueryView view = ExperimentService.get().createExperimentRunWebPart(new ViewContext(portalCtx), MicroarrayRunType.INSTANCE, true, false);
                    view.setTitle(WEBPART_MICROARRAY_RUNS);
                    view.setTitleHref(MicroarrayController.getRunsURL(portalCtx.getContainer()));
                    return view;
                }
            },
            new BaseWebPartFactory(WEBPART_PENDING_FILES)
            {
                public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart)
                {
                    QueryView view = new PendingMageMLFilesView(portalCtx);
                    view.setTitle("Pending MageML Files");
                    view.setTitleHref(MicroarrayController.getPendingMageMLFilesURL(portalCtx.getContainer()));
                    return view;
                }
            },
            new BaseWebPartFactory(WEBPART_MICROARRAY_STATISTICS, WebPartFactory.LOCATION_RIGHT)
            {
                public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart)
                {
                    WebPartView view = new MicroarrayStatisticsView(portalCtx);
                    view.setTitle(WEBPART_MICROARRAY_STATISTICS);
                    view.setTitleHref(MicroarrayController.getRunsURL(portalCtx.getContainer()));
                    return view;
                }
            }
        );
    }

    public boolean hasScripts()
    {
        return true;
    }

    public Collection<String> getSummary(Container c)
    {
        return Collections.emptyList();
    }

    public void startup(ModuleContext moduleContext)
    {
        ModuleLoader.getInstance().registerFolderType(new MicroarrayFolderType(this));
        AssayService.get().registerAssayProvider(new MicroarrayAssayProvider());
        ExperimentService.get().registerExperimentDataHandler(new MageMLDataHandler());
        ExperimentService.get().registerExperimentRunTypeSource(new ExperimentRunTypeSource()
        {
            public Set<ExperimentRunType> getExperimentRunTypes(Container container)
            {
                if (container.getActiveModules().contains(MicroarrayModule.this))
                {
                    return Collections.<ExperimentRunType>singleton(MicroarrayRunType.INSTANCE);
                }
                return Collections.emptySet();
            }
        });
        PipelineService.get().registerPipelineProvider(new MicroarrayPipelineProvider());
    }

    public Set<String> getSchemaNames()
    {
        return Collections.singleton(CONTROLLER_NAME);
    }

    public Set<DbSchema> getSchemasToTest()
    {
        return PageFlowUtil.set(MicroarraySchema.getSchema());
    }

}