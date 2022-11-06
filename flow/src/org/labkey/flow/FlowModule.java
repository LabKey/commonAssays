/*
 * Copyright (c) 2005-2018 Fred Hutchinson Cancer Research Center
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.assay.AssayService;
import org.labkey.api.audit.AuditLogService;
import org.labkey.api.audit.DefaultAuditProvider;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.files.FileContentService;
import org.labkey.api.files.TableUpdaterFileListener;
import org.labkey.api.flow.api.FlowService;
import org.labkey.api.module.FolderTypeManager;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.ModuleProperty;
import org.labkey.api.module.SpringModule;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.reports.ReportService;
import org.labkey.api.search.SearchService;
import org.labkey.api.usageMetrics.UsageMetricsService;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.DefaultWebPartFactory;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartFactory;
import org.labkey.api.webdav.WebdavService;
import org.labkey.flow.analysis.model.CompensationMatrix;
import org.labkey.flow.analysis.model.FCSHeader;
import org.labkey.flow.analysis.model.FlowJoWorkspace;
import org.labkey.flow.analysis.model.PopulationName;
import org.labkey.flow.analysis.util.LogicleRangeFunction;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.analysis.web.SubsetParser;
import org.labkey.flow.analysis.web.SubsetTests;
import org.labkey.flow.controllers.FlowController;
import org.labkey.flow.controllers.ReportsController;
import org.labkey.flow.controllers.attribute.AttributeController;
import org.labkey.flow.controllers.compensation.CompensationController;
import org.labkey.flow.controllers.editscript.ScriptController;
import org.labkey.flow.controllers.executescript.AnalysisScriptController;
import org.labkey.flow.controllers.protocol.ProtocolController;
import org.labkey.flow.controllers.run.RunController;
import org.labkey.flow.controllers.well.WellController;
import org.labkey.flow.data.FlowAssayProvider;
import org.labkey.flow.data.FlowDataType;
import org.labkey.flow.data.FlowProperty;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.FlowProtocolImplementation;
import org.labkey.flow.persist.AnalysisSerializer;
import org.labkey.flow.persist.FlowContainerListener;
import org.labkey.flow.persist.FlowDataHandler;
import org.labkey.flow.persist.FlowKeywordAuditProvider;
import org.labkey.flow.persist.FlowManager;
import org.labkey.flow.persist.PersistTests;
import org.labkey.flow.query.FlowPropertySet;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.reports.ControlsQCReport;
import org.labkey.flow.reports.ControlsQCReportUIProvider;
import org.labkey.flow.reports.PositivityFlowReport;
import org.labkey.flow.reports.PositivityFlowReportUIProvider;
import org.labkey.flow.script.FlowPipelineProvider;
import org.labkey.flow.view.ExportAnalysisManifest;
import org.labkey.flow.webparts.AnalysesWebPart;
import org.labkey.flow.webparts.AnalysisScriptsWebPart;
import org.labkey.flow.webparts.FlowFolderType;
import org.labkey.flow.webparts.FlowSummaryWebPart;
import org.labkey.flow.webparts.OverviewWebPart;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class FlowModule extends SpringModule
{
    public static final String NAME = "Flow";

    private static final String EXPORT_TO_SCRIPT_PATH = "ExportToScriptPath";
    private static final String EXPORT_TO_SCRIPT_COMMAND_LINE = "ExportToScriptCommandLine";
    private static final String EXPORT_TO_SCRIPT_LOCATION = "ExportToScriptLocation";
    private static final String EXPORT_TO_SCRIPT_FORMAT = "ExportToScriptFormat";
    private static final String EXPORT_TO_SCRIPT_TIMEOUT = "ExportToScriptTimeout";
    private static final String EXPORT_TO_SCRIPT_DELETE_ON_COMPLETE = "ExportToScriptDeleteOnComplete";

    @Override
    public String getName()
    {
        return "Flow";
    }

    @Override
    public @Nullable Double getSchemaVersion()
    {
        return 22.000;
    }

    @Override
    protected void init()
    {
        DefaultSchema.registerProvider(FlowSchema.SCHEMANAME, new DefaultSchema.SchemaProvider(this)
        {
            @Override
            public boolean isAvailable(DefaultSchema schema, Module module)
            {
                // UNDONE: schema must be available for backwards compatibility, but consider hiding it instead
                return true;
            }

            @Override
            public QuerySchema createSchema(DefaultSchema schema, Module module)
            {
                if (HttpView.hasCurrentView())
                {
                    // Use the user and container of the parent schema and the URL parameters from the current context.
                    ViewContext context = HttpView.currentContext();
                    if (context != null)
                        return new FlowSchema(schema.getUser(), schema.getContainer(), context.getActionURL(), context.getRequest(), context.getContainer());
                }

                return new FlowSchema(schema.getUser(), schema.getContainer());
            }
        });
        addController("flow", FlowController.class);
        addController("flow-executescript", AnalysisScriptController.class);
        addController("flow-run", RunController.class);
        addController("flow-editscript", ScriptController.class);
        addController("flow-well", WellController.class);
        addController("flow-compensation", CompensationController.class);
        addController("flow-protocol", ProtocolController.class);
        addController("flow-reports", ReportsController.class);

        addController("flow-attribute", AttributeController.class);

        FlowProperty.register();
        ContainerManager.addContainerListener(new FlowContainerListener());

        ReportService.get().registerReport(new ControlsQCReport());
        ReportService.get().registerReport(new PositivityFlowReport());
        ReportService.get().addUIProvider(new ControlsQCReportUIProvider());
        ReportService.get().addUIProvider(new PositivityFlowReportUIProvider());

        FlowServiceImpl flowService = new FlowServiceImpl(this);
        FlowService.setInstance(flowService);

        registerModuleProperty(EXPORT_TO_SCRIPT_PATH, "Set the path of the script that will be invoked when exporting FCS files", "Export To Script - Path", ModuleProperty.InputType.text);
        registerModuleProperty(EXPORT_TO_SCRIPT_COMMAND_LINE, "Set the export to script command line with token replacements", "Export To Script - Command Line", ModuleProperty.InputType.text,
                "${scriptPath} --timeout ${timeout} --guid ${guid} --location ${location} --exportFormat ${exportFormat}");
        registerModuleProperty(EXPORT_TO_SCRIPT_LOCATION, "Set the directory location where the exported files will be saved before executing the script", "Export To Script - Location", ModuleProperty.InputType.text);
        registerModuleProperty(EXPORT_TO_SCRIPT_FORMAT, "Set the format type of the exported files - either 'zip' or 'directory'", "Export To Script - Format", ModuleProperty.InputType.text, "directory");
        registerModuleProperty(EXPORT_TO_SCRIPT_TIMEOUT, "Set timeout in seconds the export script will be allowed to run", "Export To Script - Timeout", ModuleProperty.InputType.text);

        registerModuleProperty(
                EXPORT_TO_SCRIPT_DELETE_ON_COMPLETE,
                "When unset or 'true', delete the export directory after the script completes successfully. " +
                        "If 'false', the export directory will be preserved.",
                "Export To Script - Delete on successful complete",
                ModuleProperty.InputType.text,
                "true");

        WebdavService.get().addExpDataProvider(flowService);
    }


    private ModuleProperty registerModuleProperty(String name, String description, String label, ModuleProperty.InputType type)
    {
        return registerModuleProperty(name, description, label, type, null);
    }


    private ModuleProperty registerModuleProperty(String name, String description, String label, ModuleProperty.InputType type, String defaultValue)
    {
        ModuleProperty prop = new ModuleProperty(this, name, type);
        prop.setDescription(description);
        prop.setCanSetPerContainer(true);
        prop.setLabel(label);
        if (defaultValue != null)
            prop.setDefaultValue(defaultValue);
        addModuleProperty(prop);
        return prop;
    }


    @Nullable
    public String getExportToScriptPath(Container c)
    {
        ModuleProperty prop = this.getModuleProperties().get(EXPORT_TO_SCRIPT_PATH);
        return prop.getEffectiveValue(c);
    }

    @Nullable
    public String getExportToScriptCommandLine(Container c)
    {
        ModuleProperty prop = this.getModuleProperties().get(EXPORT_TO_SCRIPT_COMMAND_LINE);
        return prop.getEffectiveValue(c);
    }

    @Nullable
    public String getExportToScriptLocation(Container c)
    {
        ModuleProperty prop = this.getModuleProperties().get(EXPORT_TO_SCRIPT_LOCATION);
        return prop.getEffectiveValue(c);
    }

    @Nullable
    public String getExportToScriptFormat(Container c)
    {
        ModuleProperty prop = this.getModuleProperties().get(EXPORT_TO_SCRIPT_FORMAT);
        return prop.getEffectiveValue(c);
    }

    @Nullable
    public String getExportToScriptTimeout(Container c)
    {
        ModuleProperty prop = this.getModuleProperties().get(EXPORT_TO_SCRIPT_TIMEOUT);
        return prop.getEffectiveValue(c);
    }

    @Nullable
    public String getExportToScriptDeleteOnComplete(Container c)
    {
        ModuleProperty prop = this.getModuleProperties().get(EXPORT_TO_SCRIPT_DELETE_ON_COMPLETE);
        return prop.getEffectiveValue(c);
    }

    @Override
    @NotNull
    protected Collection<WebPartFactory> createWebPartFactories()
    {
        return List.of(
            AnalysesWebPart.FACTORY,
            AnalysisScriptsWebPart.FACTORY,
            FlowSummaryWebPart.FACTORY,
            OverviewWebPart.FACTORY,
            new DefaultWebPartFactory("Flow Reports", ReportsController.BeginView.class)
        );
    }

    @Override
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

    @Override
    protected void startupAfterSpringConfig(ModuleContext moduleContext)
    {
        PipelineService.get().registerPipelineProvider(new FlowPipelineProvider(this));
        FlowDataType.register();
        ExperimentService.get().registerExperimentDataHandler(FlowDataHandler.instance);
        ExperimentService.get().addExperimentListener(new FlowExperimentListener());
        FlowProtocolImplementation.register();
        AssayService.get().registerAssayProvider(new FlowAssayProvider());

        FolderTypeManager.get().registerFolderType(this, new FlowFolderType(this));
        SearchService ss = SearchService.get();
        if (null != ss)
            ss.addDocumentParser(FCSHeader.documentParser);
        FlowController.registerAdminConsoleLinks();

        FileContentService fcs = FileContentService.get();
        if (null != fcs)
            fcs.addFileListener(new TableUpdaterFileListener(FlowManager.get().getTinfoObject(), "uri", TableUpdaterFileListener.Type.uri, "RowId"));

        if (null != AuditLogService.get() && AuditLogService.get().getClass() != DefaultAuditProvider.class)
        {
            AuditLogService.get().registerAuditType(new FlowKeywordAuditProvider());
        }
        UsageMetricsService svc = UsageMetricsService.get();
        if (null != svc)
        {
            FlowManager mgr = FlowManager.get();
            if (null != mgr)
            {
                svc.registerUsageMetrics(NAME, mgr::getUsageMetrics);
            }
        }

        FlowSchema.registerContainerListener();
    }


    @Override
    @NotNull
    public Set<String> getSchemaNames()
    {
        return PageFlowUtil.set(FlowManager.get().getSchemaName());
    }


    @Override
    @NotNull
    public Set<Class> getUnitTests()
    {
        return Set.of(
            AnalysisSerializer.TestCase.class,
            CompensationMatrix.TestFCS.class,
            ExportAnalysisManifest.TestCase.class,
            FlowJoWorkspace.LoadTests.class,
            PopulationName.NameTests.class,
            StatisticSpec.TestCase.class,
            SubsetParser.TestLexer.class,
            SubsetTests.class,
            LogicleRangeFunction.TestCase.class,
            FlowPropertySet.TestCase.class
        );
    }

    @NotNull
    @Override
    public Set<Class> getIntegrationTests()
    {
        return Set.of(
            FlowController.TestCase.class,
            FlowProtocol.TestCase.class,
            PersistTests.class,
            FlowManager.TestCase.class
        );
    }

    public static String getShortProductName()
    {
        return "Flow";
    }

    public static String getLongProductName()
    {
        return "LabKey Server";
    }
}
