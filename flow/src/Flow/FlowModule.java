/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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

package Flow;

import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.view.ViewContext;
import org.labkey.api.pipeline.PipelineService;
import org.fhcrc.cpas.flow.script.FlowPipelineProvider;
import org.fhcrc.cpas.flow.data.FlowDataType;
import org.fhcrc.cpas.flow.data.FlowProperty;
import org.fhcrc.cpas.flow.data.FlowProtocolImplementation;
import org.fhcrc.cpas.flow.query.FlowSchema;
import org.fhcrc.cpas.flow.persist.FlowDataHandler;
import org.labkey.api.query.api.DefaultSchema;
import org.labkey.api.query.api.QuerySchema;
import org.labkey.api.data.*;
import org.labkey.api.exp.ExperimentDataHandler;
import org.apache.log4j.Logger;
import Flow.Run.RunController;
import Flow.ExecuteScript.AnalysisScriptController;
import Flow.EditScript.ScriptController;
import Flow.Well.WellController;
import Flow.Log.LogController;

import java.sql.SQLException;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

public class FlowModule extends DefaultModule
{
    static final private Logger _log = Logger.getLogger(FlowModule.class);
    public static final String NAME = "Flow";

    private Set<ExperimentDataHandler> _handlers;

    public FlowModule()
    {
        super(NAME, 1.70, "/Flow");
        PipelineService.get().registerPipelineProvider(new FlowPipelineProvider());
        DefaultSchema.registerProvider(FlowSchema.SCHEMANAME, new DefaultSchema.SchemaProvider()
        {
        public QuerySchema getSchema(DefaultSchema schema)
        {
            if (!isActive(schema.getContainer()))
                return null;
            return new FlowSchema(schema.getUser(), schema.getContainer());
        }
        });
        addController("Flow", FlowController.class);
        addController("Flow-ExecuteScript", AnalysisScriptController.class);
        addController("Flow-Run", RunController.class);
        addController("Flow-EditScript", ScriptController.class);
        addController("Flow-Well", WellController.class);
        addController("Flow-Log", LogController.class);
        FlowDataType.register();
        FlowProperty.register();
        _handlers = Collections.singleton((ExperimentDataHandler) new FlowDataHandler());
    }


    @Override
    public Set<String> getModuleDependencies()
    {
        Set<String> result = new HashSet<String>();
        result.add("Pipeline");
        result.add("Experiment");
        return result;
    }

    @Override
    public void beforeSchemaUpdate(ModuleContext moduleContext, ViewContext viewContext)
    {
        if (moduleContext.getInstalledVersion() >= .62 && moduleContext.getInstalledVersion() < 1.54)
        {
            try
            {
                DbSchema schema = DbSchema.createFromMetaData("flow");
                if (schema != null)
                {
                    schema.getSqlDialect().dropSchema(schema, "flow");
                }
            }
            catch (Exception e)
            {
                _log.debug("Exception trying to drop schema flow " + e);
            }
        }
        super.beforeSchemaUpdate(moduleContext, viewContext);
    }

    static public boolean isActive(Container container)
    {
        try
        {
            for (Module module : container.getActiveModules())
            {
                if (module instanceof FlowModule)
                    return true;
            }
            return false;
        }
        catch (SQLException e)
        {
            return false;
        }
    }

    @Override
    public Set<ExperimentDataHandler> getDataHandlers()
    {
        return _handlers;
    }

    @Override
    public void startup(ModuleContext moduleContext)
    {
        FlowProtocolImplementation.register();
        super.startup(moduleContext);
    }

    public static String getShortProductName()
    {
        return "Flow";
    }

    public static String getLongProductName()
    {
        return "LabKey Flow";
    }
}
