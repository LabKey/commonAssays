/*
 * Copyright (c) 2005-2008 Fred Hutchinson Cancer Research Center
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

package org.labkey.flow.controllers;

import org.apache.log4j.Logger;
import org.labkey.api.data.*;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Search;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewContext;
import org.labkey.flow.controllers.compensation.CompensationController;
import org.labkey.flow.controllers.editscript.ScriptController;
import org.labkey.flow.controllers.executescript.AnalysisScriptController;
import org.labkey.flow.controllers.log.LogController;
import org.labkey.flow.controllers.protocol.ProtocolController;
import org.labkey.flow.controllers.remote.FlowRemoteController;
import org.labkey.flow.controllers.run.RunController;
import org.labkey.flow.controllers.well.WellController;
import org.labkey.flow.data.FlowDataType;
import org.labkey.flow.data.FlowProperty;
import org.labkey.flow.data.FlowProtocolImplementation;
import org.labkey.flow.data.InputRole;
import org.labkey.flow.persist.FlowContainerListener;
import org.labkey.flow.persist.FlowDataHandler;
import org.labkey.flow.persist.FlowManager;
import org.labkey.flow.persist.ObjectType;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.script.FlowPipelineProvider;
import org.labkey.flow.webparts.AnalysesWebPart;
import org.labkey.flow.webparts.AnalysisScriptsWebPart;
import org.labkey.flow.webparts.FlowFolderType;
import org.labkey.flow.webparts.OverviewWebPart;

import javax.servlet.http.HttpServletRequest;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

public class FlowModule extends DefaultModule
{
    static final private Logger _log = Logger.getLogger(FlowModule.class);
    public static final String NAME = "Flow";

    public FlowModule()
    {
        super(NAME, 8.20, null, true,
                OverviewWebPart.FACTORY,
                AnalysesWebPart.FACTORY,
                AnalysisScriptsWebPart.FACTORY);
        DefaultSchema.registerProvider(FlowSchema.SCHEMANAME, new DefaultSchema.SchemaProvider()
        {
            public QuerySchema getSchema(DefaultSchema schema)
            {
                // don't create twice for same request
                FlowSchema fs;
                ViewContext c = HttpView.getRootContext();
                if (null != c)
                {
                    fs = (FlowSchema)c.get("org.labkey.flow.controllers.FlowModule$FlowSchema");
                    if (null != fs)
                        return fs;
                }
                fs = new FlowSchema(schema.getUser(), schema.getContainer());
                if (null != c)
                    c.put("org.labkey.flow.controllers.FlowModule$FlowSchema", fs);
                return fs;
            }
        });
        addController("flow", FlowController.class);
        addController("flow-executescript", AnalysisScriptController.class);
        addController("flow-run", RunController.class);
        addController("flow-editscript", ScriptController.class);
        addController("flow-well", WellController.class);
        addController("flow-log", LogController.class);
        addController("flow-compensation", CompensationController.class);
        addController("flow-protocol", ProtocolController.class);
        addController("flow-remote", FlowRemoteController.class);
        FlowProperty.register();
        ContainerManager.addContainerListener(new FlowContainerListener());
    }


    @Override
    public Set<String> getModuleDependencies()
    {
        Set<String> result = new HashSet<String>();
        result.add("Experiment");
        return result;
    }

    public void afterSchemaUpdate(ModuleContext moduleContext, ViewContext viewContext)
    {
        super.afterSchemaUpdate(moduleContext, viewContext);
        if (moduleContext.getInstalledVersion() >= 1.54 && moduleContext.getInstalledVersion() < 1.71)
        {
            ensureDataInputRoles(viewContext);
        }
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
        for (Module module : container.getActiveModules())
        {
            if (module instanceof FlowModule)
                return true;
        }
        return false;
    }

    @Override
    public void startup(ModuleContext moduleContext)
    {
        PipelineService.get().registerPipelineProvider(new FlowPipelineProvider());
        FlowDataType.register();
        ExperimentService.get().registerExperimentDataHandler(FlowDataHandler.instance);
        FlowProtocolImplementation.register();
        super.startup(moduleContext);
        ModuleLoader.getInstance().registerFolderType(new FlowFolderType(this));
        Search.register(new FlowManager.FCSFileSearch(null,null));
    }


    public Set<String> getSchemaNames()
    {
        return PageFlowUtil.set(FlowManager.get().getSchemaName());
    }


    public static String getShortProductName()
    {
        return "Flow";
    }

    public static String getLongProductName()
    {
        return "LabKey Flow";
    }

    /**
     * Go through all data inputs, and make sure that the PropertyId column is set to a value
     * which is appropriate for the type of input.
     */
    private void ensureDataInputRoles(ViewContext ctx)
    {
        try
        {
            DbSchema expSchema = ExperimentService.get().getSchema();
            ResultSet rs = Table.executeQuery(expSchema,
                    new SQLFragment("SELECT flow.object.typeid, exp.data.container, exp.datainput.dataid, exp.datainput.targetapplicationid" +
                    "\nFROM exp.datainput INNER JOIN exp.data ON exp.datainput.dataid = exp.data.rowid" +
                    "\nINNER JOIN flow.object ON exp.datainput.dataid = flow.object.dataid WHERE flow.object.typeid IS NOT NULL" +
                    "\nAND exp.datainput.propertyid IS NULL"
                    ));
            while (rs.next())
            {
                String containerId = rs.getString("container");
                int typeId = rs.getInt("typeid");
                int dataId = rs.getInt("dataid");
                int targetApplicationid = rs.getInt("targetApplicationId");

                Container container = ContainerManager.getForId(containerId);
                if (container == null)
                {
                    continue;
                }
                ObjectType type = ObjectType.fromTypeId(typeId);
                if (type == null)
                    continue;
                InputRole role = type.getInputRole();
                if (role == null)
                    continue;
                PropertyDescriptor pd = ExperimentService.get().ensureDataInputRole(ctx.getUser(), container, role.toString(), null);
                if (pd != null)
                {
                    Table.execute(expSchema, "UPDATE exp.datainput SET propertyid = " + pd.getPropertyId() +
                            "\nWHERE exp.datainput.dataid = " + dataId + " AND exp.datainput.targetapplicationid = " + targetApplicationid, null);
                }
            }
            rs.close();
        }
        catch (Exception e)
        {
            _log.error("Error updating flow to add propertyid's to datainput's", e);
        }
    }
}
