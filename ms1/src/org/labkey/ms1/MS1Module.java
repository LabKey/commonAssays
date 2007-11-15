package org.labkey.ms1;

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DbSchema;
import org.labkey.api.exp.ExperimentRunFilter;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.ms1.MS1Service;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.User;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.SystemMaintenance;
import org.labkey.api.view.*;
import org.labkey.ms1.maintenance.PurgeTask;
import org.labkey.ms1.pipeline.MSInspectPipelineProvider;
import org.labkey.ms1.pipeline.MSInspectFeaturesDataHandler;
import org.labkey.ms1.pipeline.PeaksFileDataHandler;
import org.labkey.ms1.query.MS1Schema;

import java.beans.PropertyChangeEvent;
import java.util.Set;


/**
 * Main module class for MS1. Allows the module to register the services
 * it provides with CPAS.
 */

public class MS1Module extends DefaultModule implements ContainerManager.ContainerListener
{
    private static final Logger _log = Logger.getLogger(MS1Module.class);
    public static final String NAME = "MS1";
    public static final String CONTROLLER_NAME = "ms1";
    public static final String WEBPART_MS1_RUNS = "MS1 Runs";
    public static final ExperimentRunFilter EXP_RUN_FILTER = new ExperimentRunFilter("msInspect Feature Finding", MS1Schema.SCHEMA_NAME, MS1Schema.TABLE_FEATURE_RUNS);

    public MS1Module()
    {
        super(NAME, 2.29, "/org/labkey/ms1", true,
                new WebPartFactory(WEBPART_MS1_RUNS)
                {
                    public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart)
                    {
                        QueryView view = ExperimentService.get().createExperimentRunWebPart(new ViewContext(portalCtx), MS1Module.EXP_RUN_FILTER, true);
                        view.setTitle("MS1 Runs");
                        ViewURLHelper url = portalCtx.getViewURLHelper().clone();
                        url.setPageFlow(CONTROLLER_NAME);
                        url.setAction("begin");
                        view.setTitleHref(url.getLocalURIString());
                        return view;
                    }
                }
        );
        addController(CONTROLLER_NAME, MS1Controller.class);

        MS1Schema.register();

        MS1Service.register(new MS1ServiceImpl());
        
        //add the MS1 purge task to the list of system maintenance tasks
        SystemMaintenance.addTask(new PurgeTask());
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

    public void propertyChange(PropertyChangeEvent evt)
    {
    }


    @Override
    public void startup(ModuleContext moduleContext)
    {
        super.startup(moduleContext);
        // add a container listener so we'll know when our container is deleted:
        ContainerManager.addContainerListener(this);
        PipelineService.get().registerPipelineProvider(new MSInspectPipelineProvider());

        // Tell the pipeline that we know how to handle msInspect files
        ExperimentService.get().registerExperimentDataHandler(new MSInspectFeaturesDataHandler());
        ExperimentService.get().registerExperimentDataHandler(new PeaksFileDataHandler());

        ExperimentService.get().registerExperimentRunFilter(EXP_RUN_FILTER);

        //register the MS1 folder type
        ModuleLoader.getInstance().registerFolderType(new MS1FolderType(this));
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