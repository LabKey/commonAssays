package org.labkey.ms1;

import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.data.*;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.exp.ExperimentDataHandler;
import org.labkey.api.exp.ExperimentRunFilter;
import org.labkey.ms1.pipeline.MSInspectPipelineProvider;
import org.apache.log4j.Logger;

import java.beans.PropertyChangeEvent;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;


/**
 * Main module class for MS1. Allows the module to register the services
 * it provides with CPAS.
 */

public class MS1Module extends DefaultModule implements ContainerManager.ContainerListener
{
    private static final Logger _log = Logger.getLogger(MS1Module.class);
    public static final String NAME = "MS1";

    private Set<ExperimentDataHandler> _dataHandlers;
    private Set<ExperimentRunFilter> _filters;

    public MS1Module()
    {
        super(NAME, 0.01, "/ms1");
        addController("ms1", MS1Controller.class);

        // Tell the pipeline that we know how to handle msInspect files
        PipelineService service = PipelineService.get();
        service.registerPipelineProvider(new MSInspectPipelineProvider());

        Set<ExperimentDataHandler> dataHandlers = new HashSet<ExperimentDataHandler>();
        dataHandlers.add(new MSInspectFeaturesDataHandler());
        _dataHandlers = Collections.unmodifiableSet(dataHandlers);

        MS1Schema.register();
    }

    public void containerCreated(Container c)
    {
    }

    public void containerDeleted(Container c)
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
    }


    @Override
    public Set<DbSchema> getSchemasToTest()
    {
        return PageFlowUtil.set(MS1Manager.get().getSchema());
    }


    @Override
    public Set<ExperimentDataHandler> getDataHandlers()
    {
        return _dataHandlers;
    }


    @Override
    public synchronized Set<ExperimentRunFilter> getExperimentRunFilters()
    {
        if (_filters == null)
        {
            Set<ExperimentRunFilter> filters = new HashSet<ExperimentRunFilter>();

            filters.add(new ExperimentRunFilter("msInspect Feature Finding", MS1Schema.SCHEMA_NAME, MS1Schema.MSINSPECT_FEATURE_EXPERIMENT_RUNS_TABLE_NAME));
            _filters = Collections.unmodifiableSet(filters);
        }
        return _filters;
    }
}