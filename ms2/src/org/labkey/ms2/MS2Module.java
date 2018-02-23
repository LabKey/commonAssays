/*
 * Copyright (c) 2005-2017 Fred Hutchinson Cancer Research Center
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.exp.ExperimentRunType;
import org.labkey.api.exp.Handler;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.files.FileContentService;
import org.labkey.api.files.TableUpdaterFileListener;
import org.labkey.api.module.FolderTypeManager;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.SpringModule;
import org.labkey.api.ms2.MS2Service;
import org.labkey.api.ms2.MS2Urls;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.protein.ProteinService;
import org.labkey.api.protein.ProteomicsModule;
import org.labkey.api.query.QueryView;
import org.labkey.api.reports.ReportService;
import org.labkey.api.search.SearchService;
import org.labkey.api.security.User;
import org.labkey.api.services.ServiceRegistry;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.BaseWebPartFactory;
import org.labkey.api.view.Portal;
import org.labkey.api.view.ProteomicsWebPartFactory;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartFactory;
import org.labkey.api.view.WebPartView;
import org.labkey.ms2.compare.MS2ReportUIProvider;
import org.labkey.ms2.compare.SpectraCountRReport;
import org.labkey.ms2.matrix.ProteinExpressionMatrixAssayProvider;
import org.labkey.ms2.matrix.ProteinExpressionMatrixDataHandler;
import org.labkey.ms2.matrix.ProteinExpressionMatrixExperimentListener;
import org.labkey.ms2.metadata.MassSpecMetadataAssayProvider;
import org.labkey.ms2.metadata.MassSpecMetadataController;
import org.labkey.ms2.peptideview.SingleMS2RunRReport;
import org.labkey.ms2.pipeline.MS2PipelineProvider;
import org.labkey.ms2.pipeline.PipelineController;
import org.labkey.ms2.pipeline.ProteinProphetPipelineProvider;
import org.labkey.ms2.pipeline.TPPTask;
import org.labkey.ms2.pipeline.comet.Comet2014ParamsBuilder;
import org.labkey.ms2.pipeline.comet.Comet2015ParamsBuilder;
import org.labkey.ms2.pipeline.comet.CometPipelineProvider;
import org.labkey.ms2.pipeline.comet.LegacyCometPipelineProvider;
import org.labkey.ms2.pipeline.mascot.MascotCPipelineProvider;
import org.labkey.ms2.pipeline.rollup.FractionRollupPipelineProvider;
import org.labkey.ms2.pipeline.sequest.BooleanParamsValidator;
import org.labkey.ms2.pipeline.sequest.ListParamsValidator;
import org.labkey.ms2.pipeline.sequest.MultipleDoubleParamsValidator;
import org.labkey.ms2.pipeline.sequest.MultipleIntegerParamsValidator;
import org.labkey.ms2.pipeline.sequest.NaturalNumberParamsValidator;
import org.labkey.ms2.pipeline.sequest.NonNegativeIntegerParamsValidator;
import org.labkey.ms2.pipeline.sequest.PositiveDoubleParamsValidator;
import org.labkey.ms2.pipeline.sequest.RealNumberParamsValidator;
import org.labkey.ms2.pipeline.sequest.SequestPipelineProvider;
import org.labkey.ms2.pipeline.sequest.SequestSearchTask;
import org.labkey.ms2.pipeline.sequest.ThermoSequestParamsBuilder;
import org.labkey.ms2.pipeline.sequest.UWSequestParamsBuilder;
import org.labkey.ms2.pipeline.sequest.UWSequestSearchTask;
import org.labkey.ms2.pipeline.tandem.XTandemPipelineProvider;
import org.labkey.ms2.protein.CustomAnnotationSet;
import org.labkey.ms2.protein.CustomProteinListView;
import org.labkey.ms2.protein.ProteinAnnotationPipelineProvider;
import org.labkey.ms2.protein.ProteinController;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.protein.ProteinServiceImpl;
import org.labkey.ms2.protein.query.CustomAnnotationSchema;
import org.labkey.ms2.protein.query.ProteinUserSchema;
import org.labkey.ms2.query.MS2Schema;
import org.labkey.ms2.reader.DatDocumentParser;
import org.labkey.ms2.reader.MGFDocumentParser;
import org.labkey.ms2.reader.MzMLDocumentParser;
import org.labkey.ms2.reader.MzXMLDocumentParser;
import org.labkey.ms2.reader.PeptideProphetSummary;
import org.labkey.ms2.reader.SequestLogDocumentParser;
import org.labkey.ms2.search.MSSearchWebpart;
import org.labkey.ms2.search.ProteinSearchWebPart;

import java.beans.PropertyChangeEvent;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * User: migra
 * Date: Jul 18, 2005
 * Time: 3:25:52 PM
 */
public class MS2Module extends SpringModule implements ContainerManager.ContainerListener, SearchService.DocumentProvider, ProteomicsModule
{
    public static final MS2SearchExperimentRunType SEARCH_RUN_TYPE = new MS2SearchExperimentRunType("MS2 Searches", MS2Schema.TableType.MS2SearchRuns.toString(), Handler.Priority.MEDIUM, MS2Schema.XTANDEM_PROTOCOL_OBJECT_PREFIX, MS2Schema.SEQUEST_PROTOCOL_OBJECT_PREFIX, MS2Schema.MASCOT_PROTOCOL_OBJECT_PREFIX, MS2Schema.COMET_PROTOCOL_OBJECT_PREFIX, MS2Schema.IMPORTED_SEARCH_PROTOCOL_OBJECT_PREFIX, MS2Schema.FRACTION_ROLLUP_PROTOCOL_OBJECT_PREFIX);
    private static final ExperimentRunType SAMPLE_PREP_RUN_TYPE = new ExperimentRunType("MS2 Sample Preparation", MS2Schema.SCHEMA_NAME, MS2Schema.TableType.SamplePrepRuns.toString())
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
    public static final String MS2_RUNS_NAME = "MS2 Runs";
    public static final String MS2_MODULE_NAME = "MS2";

    public String getName()
    {
        return MS2_MODULE_NAME;
    }

    public double getVersion()
    {
        return 18.10;
    }

    @NotNull
    protected Collection<WebPartFactory> createWebPartFactories()
    {
        BaseWebPartFactory runsFactory = new BaseWebPartFactory(MS2_RUNS_NAME)
        {
            public WebPartView getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
            {
                QueryView result = ExperimentService.get().createExperimentRunWebPart(new ViewContext(portalCtx), SEARCH_RUN_TYPE);
                result.setTitle(MS2_RUNS_NAME);
                result.setTitleHref(PageFlowUtil.urlProvider(MS2Urls.class).getShowListUrl(portalCtx.getContainer()));
                return result;
            }
        };
        runsFactory.addLegacyNames("MS2 Runs (Enhanced)", "MS2 Runs (Deprecated)", "MS2 Experiment Runs");

        return new ArrayList<>(Arrays.asList(
                runsFactory,
                new BaseWebPartFactory(MS2_SAMPLE_PREPARATION_RUNS_NAME)
                {
                    public WebPartView getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                    {
                        WebPartView result = ExperimentService.get().createExperimentRunWebPart(new ViewContext(portalCtx), SAMPLE_PREP_RUN_TYPE);
                        result.setTitle(MS2_SAMPLE_PREPARATION_RUNS_NAME);
                        return result;
                    }
                },
                new BaseWebPartFactory("MS2 Statistics", WebPartFactory.LOCATION_RIGHT)
                {
                    public WebPartView getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                    {
                        return new MS2StatsWebPart();
                    }
                },
                new ProteomicsWebPartFactory(ProteinSearchWebPart.NAME, WebPartFactory.LOCATION_BODY, WebPartFactory.LOCATION_RIGHT)
                {
                    public WebPartView getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                    {
                        return new ProteinSearchWebPart(!WebPartFactory.LOCATION_RIGHT.equalsIgnoreCase(webPart.getLocation()), MS2Controller.ProbabilityProteinSearchForm.createDefault());
                    }
                },
                new BaseWebPartFactory(CustomProteinListView.NAME)
                {
                    public WebPartView getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                    {
                        CustomProteinListView result = new CustomProteinListView(portalCtx, false);
                        result.setFrame(WebPartView.FrameType.PORTAL);
                        result.setTitle(CustomProteinListView.NAME);
                        result.setTitleHref(ProteinController.getBeginURL(portalCtx.getContainer()));
                        return result;
                    }
                },
                new ProteomicsWebPartFactory(MSSearchWebpart.NAME)
                {
                    public WebPartView getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                    {
                        return new MSSearchWebpart();
                    }
                }
        ));
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

        MS2Schema.register(this);
        ProteinUserSchema.register(this);
        CustomAnnotationSchema.register(this);

        MS2Service.register(new MS2ServiceImpl());

        ServiceRegistry.get().registerService(ProteinService.class, new ProteinServiceImpl());
    }

    @Override
    protected void startupAfterSpringConfig(ModuleContext moduleContext)
    {
        final ModuleContext finalModuleContext = moduleContext;
//        SearchService ss = SearchService.get());
//        if (null != ss)
//        {
//            ss.addSearchCategory(ProteinManager.proteinCategory);
//            ss.addDocumentProvider(this);
//        }

        PipelineService service = PipelineService.get();
        service.registerPipelineProvider(new MS2PipelineProvider(this));
        service.registerPipelineProvider(new ProteinAnnotationPipelineProvider(this));
        service.registerPipelineProvider(new XTandemPipelineProvider(this), "X!Tandem (Cluster)");
        service.registerPipelineProvider(new MascotCPipelineProvider(this), "Mascot (Cluster)");
        service.registerPipelineProvider(new SequestPipelineProvider(this));
        service.registerPipelineProvider(new LegacyCometPipelineProvider(this), "Comet (Cluster)");
        service.registerPipelineProvider(new CometPipelineProvider(this), "Comet");
        service.registerPipelineProvider(new FractionRollupPipelineProvider(this));

        service.registerPipelineProvider(new ProteinProphetPipelineProvider(this));

        final Set<ExperimentRunType> runTypes = new HashSet<>();
        runTypes.add(SAMPLE_PREP_RUN_TYPE);
        runTypes.add(SEARCH_RUN_TYPE);
        runTypes.add(new MS2SearchExperimentRunType("Imported Searches", MS2Schema.TableType.ImportedSearchRuns.toString(), Handler.Priority.HIGH, MS2Schema.IMPORTED_SEARCH_PROTOCOL_OBJECT_PREFIX));
        runTypes.add(new MS2SearchExperimentRunType("X!Tandem Searches", MS2Schema.TableType.XTandemSearchRuns.toString(), Handler.Priority.HIGH, MS2Schema.XTANDEM_PROTOCOL_OBJECT_PREFIX));
        runTypes.add(new MS2SearchExperimentRunType("Comet Searches", MS2Schema.TableType.CometSearchRuns.toString(), Handler.Priority.HIGH, MS2Schema.COMET_PROTOCOL_OBJECT_PREFIX));
        runTypes.add(new MS2SearchExperimentRunType("Mascot Searches", MS2Schema.TableType.MascotSearchRuns.toString(), Handler.Priority.HIGH, MS2Schema.MASCOT_PROTOCOL_OBJECT_PREFIX));
        runTypes.add(new MS2SearchExperimentRunType("Sequest Searches", MS2Schema.TableType.SequestSearchRuns.toString(), Handler.Priority.HIGH, MS2Schema.SEQUEST_PROTOCOL_OBJECT_PREFIX));
        runTypes.add(new MS2SearchExperimentRunType("Fraction Rollups", MS2Schema.TableType.FractionRollupsRuns.toString(), Handler.Priority.HIGH, MS2Schema.FRACTION_ROLLUP_PROTOCOL_OBJECT_PREFIX));

        ExperimentService.get().registerExperimentRunTypeSource(container ->
        {
            if (container == null || container.getActiveModules(finalModuleContext.getUpgradeUser()).contains(MS2Module.this))
            {
                return runTypes;
            }
            return Collections.emptySet();
        });

        ExperimentService.get().registerExperimentDataHandler(new PepXmlExperimentDataHandler());
        ExperimentService.get().registerExperimentDataHandler(new ProteinProphetExperimentDataHandler());
        ExperimentService.get().registerExperimentDataHandler(new ProteinExpressionMatrixDataHandler());
        ExperimentService.get().registerExperimentDataHandler(new MascotDatExperimentDataHandler());

        ExperimentService.get().addExperimentListener(new ProteinExpressionMatrixExperimentListener());

        //We are the first creator of this...
        ContainerManager.addContainerListener(this);
        FolderTypeManager.get().registerFolderType(this, new MS2FolderType(this));

        ReportService.get().registerReport(new SpectraCountRReport());
        ReportService.get().registerReport(new SingleMS2RunRReport());
        ReportService.get().addUIProvider(new MS2ReportUIProvider());
        MS2Controller.registerAdminConsoleLinks();

        AssayService svc = AssayService.get();

        // Study module might not be present, #29772
        if (null != svc)
        {
            svc.registerAssayProvider(new MassSpecMetadataAssayProvider());
            svc.registerAssayProvider(new ProteinExpressionMatrixAssayProvider());
        }

        SearchService ss = SearchService.get();

        if (null != ss)
        {
            ss.addDocumentParser(new MzXMLDocumentParser());
            ss.addDocumentParser(new MzMLDocumentParser());
            ss.addDocumentParser(new DatDocumentParser());
            ss.addDocumentParser(new SequestLogDocumentParser());
            ss.addDocumentParser(new MGFDocumentParser());
        }

        FileContentService.get().addFileListener(new TableUpdaterFileListener(MS2Manager.getTableInfoRuns(), "Path", TableUpdaterFileListener.Type.filePathForwardSlash, "Run")
        {
            @Override
            public void fileMoved(@NotNull File srcFile, @NotNull File destFile, @Nullable User user, @Nullable Container container)
            {
                super.fileMoved(srcFile, destFile, user, container);
                MS2Manager.clearRunCache();
            }
        });

        SQLFragment containerFrag = new SQLFragment();
        containerFrag.append("SELECT r.Container FROM ");
        containerFrag.append(MS2Manager.getTableInfoRuns(), "r");
        containerFrag.append(" WHERE r.Run = ").append(TableUpdaterFileListener.TABLE_ALIAS).append(".Run");

        FileContentService.get().addFileListener(new TableUpdaterFileListener(MS2Manager.getTableInfoFractions(), "MzXMLURL", TableUpdaterFileListener.Type.uri, "Fraction", containerFrag)
        {
            @Override
            public void fileMoved(@NotNull File srcFile, @NotNull File destFile, @Nullable User user, @Nullable Container container)
            {
                super.fileMoved(srcFile, destFile, user, container);
                MS2Manager.clearFractionCache();
            }
        });
        FileContentService.get().addFileListener(new TableUpdaterFileListener(MS2Manager.getTableInfoProteinProphetFiles(), "FilePath", TableUpdaterFileListener.Type.filePath, "RowId", containerFrag));
        FileContentService.get().addFileListener(new TableUpdaterFileListener(ProteinManager.getTableInfoAnnotInsertions(), "FileName", TableUpdaterFileListener.Type.filePath, "InsertId"));
        FileContentService.get().addFileListener(new TableUpdaterFileListener(ProteinManager.getTableInfoFastaFiles(), "FileName", TableUpdaterFileListener.Type.filePath, "FastaId"));
    }

    @NotNull
    @Override
    public Collection<String> getSummary(Container c)
    {
        Collection<String> list = new LinkedList<>();
        long count = MS2Manager.getRunCount(c);
        if (count > 0)
            list.add("" + count + " MS2 Run" + (count > 1 ? "s" : ""));
        int customAnnotationCount = ProteinManager.getCustomAnnotationSets(c, false).size();
        if (customAnnotationCount > 0)
        {
            list.add(customAnnotationCount + " custom protein annotation set" + (customAnnotationCount > 1 ? "s" : ""));
        }
        return list;
    }

    //
    // ContainerListener
    //
    public void containerCreated(Container c, User user)
    {
    }


    public void containerDeleted(Container c, User user)
    {
        MS2Manager.markAsDeleted(c, user);
        MS2Manager.deleteExpressionData(c);

        for (CustomAnnotationSet set : ProteinManager.getCustomAnnotationSets(c, false).values())
        {
            ProteinManager.deleteCustomAnnotationSet(set);
        }
    }

    public void containerMoved(Container c, Container oldParent, User user)
    {        
    }

    @NotNull
    @Override
    public Collection<String> canMove(Container c, Container newParent, User user)
    {
        return Collections.emptyList();
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
    }


    @Override
    @NotNull
    public Set<String> getSchemaNames()
    {
        return PageFlowUtil.set(MS2Schema.SCHEMA_NAME, ProteinManager.getSchemaName());
    }

    @Override
    @NotNull
    public Set<Class> getIntegrationTests()
    {
        return new HashSet<>(Arrays.asList(
            ThermoSequestParamsBuilder.TestCase.class,
            Comet2014ParamsBuilder.FullParseTestCase.class,
            Comet2015ParamsBuilder.FullParseTestCase.class,
            MS2Controller.TestCase.class
        ));
    }

    @Override
    @NotNull
    public Set<Class> getUnitTests()
    {
        return new HashSet<>(Arrays.asList(
            PeptideProphetSummary.TestCase.class,
            MS2Modification.MS2ModificationTest.class,
            org.labkey.ms2.protein.fasta.PeptideTestCase.class,
            org.labkey.ms2.reader.RandomAccessJrapMzxmlIterator.TestCase.class,
            org.labkey.ms2.reader.RandomAccessPwizMSDataIterator.TestCase.class,
            org.labkey.ms2.protein.FastaDbLoader.TestCase.class,
            ListParamsValidator.TestCase.class,
            NonNegativeIntegerParamsValidator.TestCase.class,
            BooleanParamsValidator.TestCase.class,
            RealNumberParamsValidator.TestCase.class,
            PositiveDoubleParamsValidator.TestCase.class,
            NaturalNumberParamsValidator.TestCase.class,
            MultipleIntegerParamsValidator.TestCase.class,
            MultipleDoubleParamsValidator.TestCase.class,
            UWSequestParamsBuilder.TestCase.class,
            UWSequestSearchTask.TestCase.class,
            ProteinCoverageMapBuilder.TestCase.class,
            UWSequestSearchTask.TestCase.class,
            Comet2014ParamsBuilder.LimitedParseTestCase.class,
            Comet2015ParamsBuilder.LimitedParseTestCase.class,
            TPPTask.TestCase.class,
            Protein.TestCase.class,
            SequestSearchTask.TestCase.class,
            BibliospecSpectrumRenderer.TestCase.class,
            MS2RunType.TestCase.class
        ));
    }

    public void enumerateDocuments(@NotNull SearchService.IndexTask task, @NotNull Container c, Date modifiedSince)
    {
        if (c == ContainerManager.getSharedContainer())
        {
            ProteinManager.indexProteins(task, modifiedSince);
        }
    }

    public void indexDeleted() throws SQLException
    {
    }
}
