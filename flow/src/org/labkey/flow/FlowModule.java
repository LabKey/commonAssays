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

package org.labkey.flow;

//import org.apache.log4j.Logger;
import org.labkey.api.data.*;
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
import org.labkey.api.view.WebPartFactory;
import org.labkey.api.view.SimpleWebPartFactory;
import org.labkey.flow.controllers.compensation.CompensationController;
import org.labkey.flow.controllers.editscript.ScriptController;
import org.labkey.flow.controllers.editscript.SpringScriptController;
import org.labkey.flow.controllers.executescript.AnalysisScriptController;
import org.labkey.flow.controllers.log.LogController;
import org.labkey.flow.controllers.protocol.ProtocolController;
import org.labkey.flow.controllers.remote.FlowRemoteController;
import org.labkey.flow.controllers.run.RunController;
import org.labkey.flow.controllers.well.WellController;
import org.labkey.flow.controllers.FlowController;
import org.labkey.flow.data.FlowDataType;
import org.labkey.flow.data.FlowProperty;
import org.labkey.flow.data.FlowProtocolImplementation;
import org.labkey.flow.persist.FlowContainerListener;
import org.labkey.flow.persist.FlowDataHandler;
import org.labkey.flow.persist.FlowManager;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.script.FlowPipelineProvider;
import org.labkey.flow.webparts.*;

import java.util.Set;
import java.util.Collection;
import java.util.Arrays;

public class FlowModule extends DefaultModule
{
//    static final private Logger _log = Logger.getLogger(FlowModule.class);
    public static final String NAME = "Flow";

    public String getName()
    {
        return "Flow";
    }

    public double getVersion()
    {
        return 8.30;
    }

    protected void init()
    {
        DefaultSchema.registerProvider(FlowSchema.SCHEMANAME, new DefaultSchema.SchemaProvider()
        {
            public QuerySchema getSchema(DefaultSchema schema)
            {
                // don't create twice for same request
                FlowSchema fs;
                ViewContext c = HttpView.getRootContext();
                if (null != c)
                {
                    fs = (FlowSchema)c.get("org.labkey.flow.FlowModule$FlowSchema");
                    if (null != fs)
                        return fs;
                }
                fs = new FlowSchema(schema.getUser(), schema.getContainer());
                if (null != c)
                    c.put("org.labkey.flow.FlowModule$FlowSchema", fs);
                return fs;
            }
        });
        addController("flow", FlowController.class);
        addController("flow-executescript", AnalysisScriptController.class);
        addController("flow-run", RunController.class);
        addController("flow-editscript", SpringScriptController.class);
        addController("flow-well", WellController.class);
        addController("flow-log", LogController.class);
        addController("flow-compensation", CompensationController.class);
        addController("flow-protocol", ProtocolController.class);
        addController("flow-remote", FlowRemoteController.class);
        FlowProperty.register();
        ContainerManager.addContainerListener(new FlowContainerListener());
    }

    protected Collection<? extends WebPartFactory> createWebPartFactories()
    {
        return Arrays.asList(OverviewWebPart.FACTORY,
                AnalysesWebPart.FACTORY,
                AnalysisScriptsWebPart.FACTORY
//                ,FlowFrontPage.FACTORY,FlowFiles.FACTORY
                );
    }

    public boolean hasScripts()
    {
        return true;
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

    public void startup(ModuleContext moduleContext)
    {
        PipelineService.get().registerPipelineProvider(new FlowPipelineProvider());
        FlowDataType.register();
        ExperimentService.get().registerExperimentDataHandler(FlowDataHandler.instance);
        FlowProtocolImplementation.register();
        ModuleLoader.getInstance().registerFolderType(new FlowFolderType(this));
        Search.register(new FlowManager.FCSFileSearch(null,null));
        FlowController.registerAdminConsoleLinks();
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
}
