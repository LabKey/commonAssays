/*
 * Copyright (c) 2006-2008 LabKey Corporation
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

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DbSchema;
import org.labkey.api.exp.ExperimentRunFilter;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.module.SpringModule;
import org.labkey.api.ms1.MS1Service;
import org.labkey.api.query.QueryView;
import org.labkey.api.reports.ReportService;
import org.labkey.api.security.User;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.SystemMaintenance;
import org.labkey.api.view.*;
import org.labkey.ms1.maintenance.PurgeTask;
import org.labkey.ms1.model.PepSearchModel;
import org.labkey.ms1.model.SimilarSearchModel;
import org.labkey.ms1.pipeline.MSInspectFeaturesDataHandler;
import org.labkey.ms1.pipeline.PeaksFileDataHandler;
import org.labkey.ms1.query.MS1Schema;
import org.labkey.ms1.report.FeaturesRReport;
import org.labkey.ms1.report.MS1ReportUIProvider;
import org.labkey.ms1.report.PeaksRReport;

import java.beans.PropertyChangeEvent;
import java.util.Collection;
import java.util.Set;
import java.util.ArrayList;
import java.sql.SQLException;


/**
 * Main module class for MS1. Allows the module to register the services
 * it provides with CPAS.
 */

public class MS1Module extends SpringModule implements ContainerManager.ContainerListener
{
    private static final Logger _log = Logger.getLogger(MS1Module.class);
    public static final String NAME = "MS1";
    public static final String CONTROLLER_NAME = "ms1";
    public static final String WEBPART_MS1_RUNS = "MS1 Runs";
    public static final String WEBPART_PEP_SEARCH = "Peptide Search";
    public static final String WEBPART_FEATURE_SEARCH = "MS1 Feature Search";
    public static final String PROTOCOL_MS1 = "msInspect Feature Finding Analysis";
    public static final ExperimentRunFilter EXP_RUN_FILTER = new MS1ExperimentRunFilter();

    public MS1Module()
    {
        super(NAME, 8.20, "/org/labkey/ms1", true,
                new BaseWebPartFactory(WEBPART_MS1_RUNS)
                {
                    public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart)
                    {
                        QueryView view = ExperimentService.get().createExperimentRunWebPart(new ViewContext(portalCtx), MS1Module.EXP_RUN_FILTER, true, true);
                        view.setTitle("MS1 Runs");
                        ActionURL url = portalCtx.getActionURL().clone();
                        url.setPageFlow(CONTROLLER_NAME);
                        url.setAction("begin");
                        view.setTitleHref(url.getLocalURIString());
                        return view;
                    }
                },
                new BaseWebPartFactory(WEBPART_PEP_SEARCH)
                {
                    public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart)
                    {
                        PepSearchModel model = new PepSearchModel(portalCtx.getContainer());
                        JspView<PepSearchModel> view = new JspView<PepSearchModel>("/org/labkey/ms1/view/PepSearchView.jsp", model);
                        view.setTitle(WEBPART_PEP_SEARCH);
                        return view;
                    }
                },
                new BaseWebPartFactory(WEBPART_FEATURE_SEARCH)
                {
                    public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart)
                    {
                        SimilarSearchModel searchModel = new SimilarSearchModel(portalCtx.getContainer(), false);
                        JspView<SimilarSearchModel> searchView = new JspView<SimilarSearchModel>("/org/labkey/ms1/view/SimilarSearchView.jsp", searchModel);
                        searchView.setTitle(WEBPART_FEATURE_SEARCH);
                        return searchView;
                    }
                }
        );
        addController(CONTROLLER_NAME, MS1Controller.class);

        MS1Schema.register();

        MS1Service.register(new MS1ServiceImpl());
        
        //add the MS1 purge task to the list of system maintenance tasks
        SystemMaintenance.addTask(new PurgeTask());
    }

    @Override
    protected ContextType getContextType()
    {
        return ContextType.config;
    }

    public void containerCreated(Container c)
    {
    }

    public void containerDeleted(Container c, User user)
    {
        // If the module starts loading data into its own database tables,
        // it needs to clean up the relevant rows when a container
        // that holds MS1 data is deleted
    }

    public Collection<String> getSummary(Container c)
    {
        Collection<String> ret = null;
        try
        {
            ret = MS1Manager.get().getContainerSummary(c);
        }
        catch(SQLException e)
        {
            ret = new ArrayList<String>();
            ret.add(e.getMessage());
        }
        
        return ret;
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
    }


    @Override
    public void startup(ModuleContext moduleContext)
    {
        super.startup(moduleContext);
        // add a container listener so we'll know when our container is deleted:
        ContainerManager.addContainerListener(this);

        // Tell the pipeline that we know how to handle msInspect files
        ExperimentService.get().registerExperimentDataHandler(new MSInspectFeaturesDataHandler());
        ExperimentService.get().registerExperimentDataHandler(new PeaksFileDataHandler());

        ExperimentService.get().registerExperimentRunFilter(EXP_RUN_FILTER);

        //register the MS1 folder type
        ModuleLoader.getInstance().registerFolderType(new MS1FolderType(this));
        MS1Controller.registerAdminConsoleLinks();

        ReportService.get().registerReport(new FeaturesRReport());
        ReportService.get().registerReport(new PeaksRReport());
        ReportService.get().addUIProvider(new MS1ReportUIProvider());
    }


    @Override
    public Set<String> getSchemaNames()
    {
        return PageFlowUtil.set(MS1Manager.get().getSchemaName());
    }


    @Override
    public Set<DbSchema> getSchemasToTest()
    {
        return PageFlowUtil.set(MS1Manager.get().getSchema());
    }
}
