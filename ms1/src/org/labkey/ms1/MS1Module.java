/*
 * Copyright (c) 2006-2017 LabKey Corporation
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

package org.labkey.ms1;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.exp.ExperimentRunType;
import org.labkey.api.exp.ExperimentRunTypeSource;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.files.FileContentService;
import org.labkey.api.files.TableUpdaterFileListener;
import org.labkey.api.module.FolderTypeManager;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.SpringModule;
import org.labkey.api.ms1.MS1Service;
import org.labkey.api.ms1.MS1Urls;
import org.labkey.api.protein.ProteomicsModule;
import org.labkey.api.query.QueryView;
import org.labkey.api.reports.ReportService;
import org.labkey.api.services.ServiceRegistry;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.SystemMaintenance;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.BaseWebPartFactory;
import org.labkey.api.view.JspView;
import org.labkey.api.view.Portal;
import org.labkey.api.view.ProteomicsWebPartFactory;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartFactory;
import org.labkey.api.view.WebPartView;
import org.labkey.ms1.maintenance.PurgeTask;
import org.labkey.ms1.model.PepSearchModel;
import org.labkey.ms1.model.SimilarSearchModel;
import org.labkey.ms1.pipeline.MSInspectFeaturesDataHandler;
import org.labkey.ms1.pipeline.PeaksFileDataHandler;
import org.labkey.ms1.query.MS1Schema;
import org.labkey.ms1.report.FeaturesRReport;
import org.labkey.ms1.report.MS1ReportUIProvider;
import org.labkey.ms1.report.PeaksRReport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;


/**
 * Main module class for MS1. Allows the module to register the services
 * it provides with LabKey Server.
 */

public class MS1Module extends SpringModule implements ProteomicsModule
{
    public static final String NAME = "MS1";
    public static final String CONTROLLER_NAME = "ms1";
    public static final String WEBPART_MS1_RUNS = "MS1 Runs";
    public static final String WEBPART_PEP_SEARCH = "Peptide Search";
    public static final String WEBPART_FEATURE_SEARCH = "MS1 Feature Search";
    public static final String PROTOCOL_MS1 = "msInspect Feature Finding Analysis";
    public static final ExperimentRunType EXP_RUN_TYPE = new MS1ExperimentRunType();

    public String getName()
    {
        return NAME;
    }

    public double getVersion()
    {
        return 18.10;
    }

    protected void init()
    {
        addController(CONTROLLER_NAME, MS1Controller.class);

        MS1Schema.register(this);

        ServiceRegistry svcReg = ServiceRegistry.get();
        svcReg.registerService(MS1Service.class, new MS1ServiceImpl());
        svcReg.registerService(MS1Urls.class, new MS1Controller.MS1UrlsImpl());

        //add the MS1 purge task to the list of system maintenance tasks
        SystemMaintenance.addTask(new PurgeTask());
    }

    @NotNull
    protected Collection<WebPartFactory> createWebPartFactories()
    {
        return new ArrayList<>(Arrays.asList(new BaseWebPartFactory(WEBPART_MS1_RUNS)
            {
                public WebPartView getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                {
                    QueryView view = ExperimentService.get().createExperimentRunWebPart(new ViewContext(portalCtx), MS1Module.EXP_RUN_TYPE);
                    view.setTitle("MS1 Runs");
                    ActionURL url = portalCtx.getActionURL().clone();
                    url.setAction(MS1Controller.BeginAction.class);
                    view.setTitleHref(url);
                    return view;
                }
            },
            new ProteomicsWebPartFactory(WEBPART_PEP_SEARCH)
            {
                public WebPartView getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                {
                    PepSearchModel model = new PepSearchModel(portalCtx.getContainer());
                    JspView<PepSearchModel> view = new JspView<>("/org/labkey/ms1/view/PepSearchView.jsp", model);
                    view.setTitle(WEBPART_PEP_SEARCH);
                    return view;
                }
            },
            new BaseWebPartFactory(WEBPART_FEATURE_SEARCH)
            {
                public WebPartView getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                {
                    SimilarSearchModel searchModel = new SimilarSearchModel(portalCtx.getContainer(), false);
                    JspView<SimilarSearchModel> searchView = new JspView<>("/org/labkey/ms1/view/SimilarSearchView.jsp", searchModel);
                    searchView.setTitle(WEBPART_FEATURE_SEARCH);
                    return searchView;
                }
            }
        ));
    }

    public boolean hasScripts()
    {
        return true;
    }

    @NotNull
    @Override
    public Collection<String> getSummary(Container c)
    {
        return MS1Manager.get().getContainerSummary(c);
    }

    @Override
    protected void startupAfterSpringConfig(ModuleContext moduleContext)
    {
        // Tell the pipeline that we know how to handle msInspect files
        ExperimentService.get().registerExperimentDataHandler(new MSInspectFeaturesDataHandler());
        ExperimentService.get().registerExperimentDataHandler(new PeaksFileDataHandler());

        ExperimentService.get().registerExperimentRunTypeSource(new ExperimentRunTypeSource()
        {
            @NotNull
            public Set<ExperimentRunType> getExperimentRunTypes(@Nullable Container container)
            {
                if (container == null || container.getActiveModules().contains(MS1Module.this))
                {
                    return Collections.singleton(EXP_RUN_TYPE);
                }
                return Collections.emptySet();
            }
        });

        //register the MS1 folder type
        FolderTypeManager.get().registerFolderType(this, new MS1FolderType(this));
        MS1Controller.registerAdminConsoleLinks();

        ReportService.get().registerReport(new FeaturesRReport());
        ReportService.get().registerReport(new PeaksRReport());
        ReportService.get().addUIProvider(new MS1ReportUIProvider());


        SQLFragment containerFrag = new SQLFragment();
        containerFrag.append("SELECT d.Container FROM exp.Data d ");
        containerFrag.append("WHERE d.RowId = ").append(TableUpdaterFileListener.TABLE_ALIAS).append(".ExpDataFileId");

        FileContentService.get().addFileListener(new TableUpdaterFileListener(MS1Manager.get().getTable(MS1Manager.TABLE_FILES), "MzXMLURL", TableUpdaterFileListener.Type.uri, "FileId", containerFrag));
    }


    @Override
    @NotNull
    public Set<String> getSchemaNames()
    {
        return PageFlowUtil.set(MS1Manager.get().getSchemaName());
    }
}
