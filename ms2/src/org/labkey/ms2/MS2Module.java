/*
 * Copyright (c) 2005-2009 Fred Hutchinson Cancer Research Center
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
package org.labkey.ms2;

import junit.framework.TestCase;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.exp.ExperimentRunType;
import org.labkey.api.exp.ExperimentRunTypeSource;
import org.labkey.api.exp.Handler;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.module.SpringModule;
import org.labkey.api.ms2.MS2Service;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.query.QueryView;
import org.labkey.api.reports.ReportService;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.*;
import org.labkey.ms2.compare.MS2ReportUIProvider;
import org.labkey.ms2.compare.SpectraCountRReport;
import org.labkey.ms2.metadata.MassSpecMetadataAssayProvider;
import org.labkey.ms2.metadata.MassSpecMetadataController;
import org.labkey.ms2.metadata.MassSpecMetadataPipelineProvider;
import org.labkey.ms2.peptideview.SingleMS2RunRReport;
import org.labkey.ms2.pipeline.MS2PipelineProvider;
import org.labkey.ms2.pipeline.PipelineController;
import org.labkey.ms2.pipeline.ProteinProphetPipelineProvider;
import org.labkey.ms2.pipeline.comet.CometCPipelineProvider;
import org.labkey.ms2.pipeline.mascot.MascotCPipelineProvider;
import org.labkey.ms2.pipeline.sequest.*;
import org.labkey.ms2.pipeline.tandem.XTandemCPipelineProvider;
import org.labkey.ms2.protein.CustomAnnotationSet;
import org.labkey.ms2.protein.CustomProteinListView;
import org.labkey.ms2.protein.ProteinController;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.protein.query.CustomAnnotationSchema;
import org.labkey.ms2.query.MS2Schema;
import org.labkey.ms2.scoring.ScoringController;
import org.labkey.ms2.search.ProteinSearchWebPart;

import java.beans.PropertyChangeEvent;
import java.sql.SQLException;
import java.util.*;

/**
 * User: migra
 * Date: Jul 18, 2005
 * Time: 3:25:52 PM
 */
public class MS2Module extends SpringModule implements ContainerManager.ContainerListener
{
    public static final MS2SearchExperimentRunType _ms2SearchRunFilter = new MS2SearchExperimentRunType("MS2 Searches", MS2Schema.TableType.MS2SearchRuns.toString(), Handler.Priority.MEDIUM, MS2Schema.XTANDEM_PROTOCOL_OBJECT_PREFIX, MS2Schema.SEQUEST_PROTOCOL_OBJECT_PREFIX, MS2Schema.MASCOT_PROTOCOL_OBJECT_PREFIX, MS2Schema.IMPORTED_SEARCH_PROTOCOL_OBJECT_PREFIX);
    private static ExperimentRunType _samplePrepRunType = new ExperimentRunType("MS2 Sample Preparation", MS2Schema.SCHEMA_NAME, MS2Schema.TableType.SamplePrepRuns.toString())
    {
        public Priority getPriority(ExpProtocol protocol)
        {
            Lsid lsid = new Lsid(protocol.getLSID());
            String objectId = lsid.getObjectId();
            if (objectId.startsWith(MS2Schema.SAMPLE_PREP_PROTOCOL_OBJECT_PREFIX) || lsid.getNamespacePrefix().startsWith(MassSpecMetadataAssayProvider.PROTOCOL_LSID_NAMESPACE_PREFIX))
            {
                return Priority.HIGH;
            }
            return null;
        }
    };

    public static final String MS2_SAMPLE_PREPARATION_RUNS_NAME = "MS2 Sample Preparation Runs";
    public static final String MS2_EXPERIMENT_RUNS_NAME = "MS2 Experiment Runs";
    public static final String MS2_RUNS_ENHANCED_LEGACY_NAME = "MS2 Runs (Enhanced)";
    private static final String MS2_RUNS_DEPRECATED_NAME = "MS2 Runs (Deprecated)";

    public String getName()
    {
        return "MS2";
    }

    public double getVersion()
    {
        return 9.22;
    }

    protected Collection<? extends WebPartFactory> createWebPartFactories()
    {
        BaseWebPartFactory legacyRunsFactory = new BaseWebPartFactory(MS2_RUNS_DEPRECATED_NAME)
        {
            public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart)
            {
                MS2WebPart part = new MS2WebPart(portalCtx);
                part.setTitle(MS2_RUNS_DEPRECATED_NAME);
                part.setTitlePopupHelp(MS2_RUNS_DEPRECATED_NAME, "The MS2 Experiment Runs web part replaces this MS2 Runs web part and adds significant new functionality.");
                return part;
            }
        };
        legacyRunsFactory.addLegacyNames("MS2 Runs");

        BaseWebPartFactory runsFactory = new BaseWebPartFactory(MS2_EXPERIMENT_RUNS_NAME)
        {
            public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart)
            {
                QueryView result = ExperimentService.get().createExperimentRunWebPart(new ViewContext(portalCtx), _ms2SearchRunFilter, true, true);
                result.setTitle("MS2 Experiment Runs");
                return result;
            }
        };
        runsFactory.addLegacyNames(MS2_RUNS_ENHANCED_LEGACY_NAME);
        
        return Arrays.asList(
                legacyRunsFactory,
                runsFactory,
            new BaseWebPartFactory(MS2_SAMPLE_PREPARATION_RUNS_NAME)
            {
                public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart)
                {
                    WebPartView result = ExperimentService.get().createExperimentRunWebPart(new ViewContext(portalCtx), _samplePrepRunType, true, true);
                    result.setTitle(MS2_SAMPLE_PREPARATION_RUNS_NAME);
                    return result;
                }
            },
            new BaseWebPartFactory("MS2 Statistics", WebPartFactory.LOCATION_RIGHT)
            {
                public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart)
                {
                    return new MS2StatsWebPart();
                }
            },
            new BaseWebPartFactory(ProteinSearchWebPart.NAME, WebPartFactory.LOCATION_RIGHT)
            {
                public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart)
                {
                    return new ProteinSearchWebPart(!"right".equalsIgnoreCase(webPart.getLocation()), MS2Controller.ProteinSearchForm.createDefault());
                }
            },
            new BaseWebPartFactory(ProteinSearchWebPart.NAME)
            {
                public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart)
                {
                    return new ProteinSearchWebPart(!"right".equalsIgnoreCase(webPart.getLocation()), MS2Controller.ProteinSearchForm.createDefault());
                }
            },
            new BaseWebPartFactory(CustomProteinListView.NAME)
            {
                public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart)
                {
                    CustomProteinListView result = new CustomProteinListView(portalCtx, false);
                    result.setFrame(WebPartView.FrameType.PORTAL);
                    result.setTitle(CustomProteinListView.NAME);
                    result.setTitleHref(ProteinController.getBeginURL(portalCtx.getContainer()));
                    return result;
                }
            }
        );
    }

    public boolean hasScripts()
    {
        return true;
    }

    protected void init()
    {
        addController("ms2", MS2Controller.class);
        addController("xarassay", MassSpecMetadataController.class);
        addController("protein", ProteinController.class);
        addController("ms2-pipeline", PipelineController.class);
        addController("ms2-scoring", ScoringController.class);

        MS2Schema.register();
        CustomAnnotationSchema.register();

        MS2Service.register(new MS2ServiceImpl());
    }

    @Override
    protected ContextType getContextType()
    {
        return ContextType.config;
    }

    public void startup(ModuleContext context)
    {
        PipelineService service = PipelineService.get();
        service.registerPipelineProvider(new MS2PipelineProvider(this));
        service.registerPipelineProvider(new XTandemCPipelineProvider(this), "X!Tandem (Cluster)");
        service.registerPipelineProvider(new MascotCPipelineProvider(this), "Mascot (Cluster)");
        service.registerPipelineProvider(new SequestLocalPipelineProvider(this));
        service.registerPipelineProvider(new CometCPipelineProvider(this), "Comet (Cluster)");

        service.registerPipelineProvider(new ProteinProphetPipelineProvider(this));

        final Set<ExperimentRunType> runTypes = new HashSet<ExperimentRunType>();
        runTypes.add(_samplePrepRunType);
        runTypes.add(_ms2SearchRunFilter);
        runTypes.add(new MS2SearchExperimentRunType("Imported Searches", MS2Schema.TableType.ImportedSearchRuns.toString(), Handler.Priority.HIGH, MS2Schema.IMPORTED_SEARCH_PROTOCOL_OBJECT_PREFIX));
        runTypes.add(new MS2SearchExperimentRunType("X!Tandem Searches", MS2Schema.TableType.XTandemSearchRuns.toString(), Handler.Priority.HIGH, MS2Schema.XTANDEM_PROTOCOL_OBJECT_PREFIX));
        runTypes.add(new MS2SearchExperimentRunType("Mascot Searches", MS2Schema.TableType.MascotSearchRuns.toString(), Handler.Priority.HIGH, MS2Schema.MASCOT_PROTOCOL_OBJECT_PREFIX));
        runTypes.add(new MS2SearchExperimentRunType("Sequest Searches", MS2Schema.TableType.SequestSearchRuns.toString(), Handler.Priority.HIGH, MS2Schema.SEQUEST_PROTOCOL_OBJECT_PREFIX));

        ExperimentService.get().registerExperimentRunTypeSource(new ExperimentRunTypeSource()
        {
            public Set<ExperimentRunType> getExperimentRunTypes(Container container)
            {
                if (container.getActiveModules().contains(MS2Module.this))
                {
                    return runTypes;
                }
                return Collections.emptySet();
            }
        });

        ExperimentService.get().registerExperimentDataHandler(new PepXmlExperimentDataHandler());
        ExperimentService.get().registerExperimentDataHandler(new ProteinProphetExperimentDataHandler());

        //We are the first creator of this...
        ContainerManager.addContainerListener(this);
        ModuleLoader.getInstance().registerFolderType(new MS2FolderType(this));

        ReportService.get().registerReport(new SpectraCountRReport());
        ReportService.get().registerReport(new SingleMS2RunRReport());
        ReportService.get().addUIProvider(new MS2ReportUIProvider());
        MS2Controller.registerAdminConsoleLinks();
        
        AssayService.get().registerAssayProvider(new MassSpecMetadataAssayProvider());
        PipelineService.get().registerPipelineProvider(new MassSpecMetadataPipelineProvider(this));

        initWebApplicationContext();
    }

    @Override
    public Collection<String> getSummary(Container c)
    {
        Collection<String> list = new LinkedList<String>();
        try
        {
            long count = MS2Manager.getRunCount(c);
            if (count > 0)
                list.add("" + count + " MS2 Run" + (count > 1 ? "s" : ""));
            int customAnnotationCount = ProteinManager.getCustomAnnotationSets(c, false).size();
            if (customAnnotationCount > 0)
            {
                list.add(customAnnotationCount + " custom protein annotation set" + (customAnnotationCount > 1 ? "s" : ""));
            }
        }
        catch (SQLException x)
        {
            list.add(x.getMessage());
        }
        return list;
    }

    //
    // ContainerListener
    //
    public void containerCreated(Container c)
    {
    }


    public void containerDeleted(Container c, User user)
    {
        MS2Manager.markAsDeleted(c, user);

        for (CustomAnnotationSet set : ProteinManager.getCustomAnnotationSets(c, false).values())
        {
            ProteinManager.deleteCustomAnnotationSet(set);
        }
    }


    public void propertyChange(PropertyChangeEvent evt)
    {
    }


    @Override
    public Set<String> getSchemaNames()
    {
        return PageFlowUtil.set(MS2Schema.SCHEMA_NAME, ProteinManager.getSchemaName());
    }

    @Override
    public Set<DbSchema> getSchemasToTest()
    {
        return PageFlowUtil.set(MS2Manager.getSchema(), ProteinManager.getSchema());
    }

    @Override
    public Set<Class<? extends TestCase>> getJUnitTests()
    {
        return new HashSet<Class<? extends TestCase>>(Arrays.asList(
            SequestParamsBuilder.TestCase.class,
            PositiveDoubleParamsValidator.TestCase.class,
            NaturalNumberParamsValidator.TestCase.class,
            NaturalNumberParamsValidator.TestCase.class,
            RealNumberParamsValidator.TestCase.class,
            BooleanParamsValidator.TestCase.class,
            PositiveIntegerParamsValidator.TestCase.class,
            ListParamsValidator.TestCase.class,
            org.labkey.ms2.protein.FastaDbLoader.TestCase.class,                
            org.labkey.ms2.reader.RandomAccessMzxmlIterator.TestCase.class,
            org.labkey.ms2.protein.fasta.PeptideTestCase.class,
            MS2Modification.MS2ModificationTest.class
        ));
    }

    @Override
    public UpgradeCode getUpgradeCode()
    {
        return new MS2UpgradeCode();
    }
}
