package cpas.ms1;

import org.fhcrc.cpas.module.DefaultModule;
import org.fhcrc.cpas.module.ModuleContext;
import org.fhcrc.cpas.data.*;
import org.fhcrc.cpas.util.PageFlowUtil;
import org.fhcrc.cpas.pipeline.PipelineService;
import org.fhcrc.cpas.exp.ExperimentDataHandler;
import org.fhcrc.cpas.exp.ExperimentRunFilter;
import org.apache.log4j.Logger;

import java.beans.PropertyChangeEvent;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

import cpas.ms1.pipeline.MSInspectPipelineProvider;
import ms1.MS1Controller;

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