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
package org.labkey.ms2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.ExperimentRunType;
import org.labkey.api.exp.Handler;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.files.FileContentService;
import org.labkey.api.files.TableUpdaterFileListener;
import org.labkey.api.module.FolderTypeManager;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.SpringModule;
import org.labkey.api.ms2.MS2Service;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.protein.ProteinManager;
import org.labkey.api.protein.ProteinSchema;
import org.labkey.api.protein.ProteinService;
import org.labkey.api.protein.ProteomicsModule;
import org.labkey.api.protein.query.SequencesTableInfo;
import org.labkey.api.protein.search.PepSearchModel;
import org.labkey.api.protein.search.ProbabilityProteinSearchForm;
import org.labkey.api.protein.search.ProteinSearchWebPart;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryView;
import org.labkey.api.reports.ReportService;
import org.labkey.api.search.SearchService;
import org.labkey.api.security.User;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.BaseWebPartFactory;
import org.labkey.api.view.JspView;
import org.labkey.api.view.Portal;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartFactory;
import org.labkey.api.view.WebPartView;
import org.labkey.ms2.MS2Controller.PeptidesViewProvider;
import org.labkey.ms2.MS2Controller.ProteinSearchGroupViewProvider;
import org.labkey.ms2.MS2Controller.ProteinSearchViewProvider;
import org.labkey.ms2.compare.MS2ReportUIProvider;
import org.labkey.ms2.compare.SpectraCountRReport;
import org.labkey.ms2.peptideview.SingleMS2RunRReport;
import org.labkey.ms2.pipeline.MS2PipelineProvider;
import org.labkey.ms2.pipeline.PipelineController;
import org.labkey.ms2.pipeline.ProteinProphetPipelineProvider;
import org.labkey.ms2.pipeline.TPPTask;
import org.labkey.ms2.pipeline.comet.Comet2014ParamsBuilder;
import org.labkey.ms2.pipeline.comet.Comet2015ParamsBuilder;
import org.labkey.ms2.pipeline.comet.CometPipelineProvider;
import org.labkey.ms2.pipeline.mascot.MascotCPipelineProvider;
import org.labkey.ms2.pipeline.mascot.MascotClientImpl;
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
import org.labkey.ms2.pipeline.tandem.XTandemPipelineProvider;
import org.labkey.ms2.protein.Protein;
import org.labkey.ms2.query.MS2Schema;
import org.labkey.ms2.reader.DatDocumentParser;
import org.labkey.ms2.reader.MGFDocumentParser;
import org.labkey.ms2.reader.MzMLDocumentParser;
import org.labkey.ms2.reader.MzXMLDocumentParser;
import org.labkey.ms2.reader.PeptideProphetSummary;
import org.labkey.ms2.reader.RandomAccessJrapMzxmlIterator;
import org.labkey.ms2.reader.SequestLogDocumentParser;
import org.labkey.ms2.search.MSSearchWebpart;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Module that supports mass-spectrometry based protein database searches. Can convert raw instrument files to mzXML
 * and then analyze using a variety of search engines like XTandem or Comet, load the results, and show reports.
 */
public class MS2Module extends SpringModule implements ProteomicsModule
{
    public static final String WEBPART_PEP_SEARCH = "Peptide Search";
    public static final MS2SearchExperimentRunType SEARCH_RUN_TYPE = new MS2SearchExperimentRunType("MS2 Searches", MS2Schema.TableType.MS2SearchRuns.toString(), Handler.Priority.MEDIUM, MS2Schema.XTANDEM_PROTOCOL_OBJECT_PREFIX, MS2Schema.SEQUEST_PROTOCOL_OBJECT_PREFIX, MS2Schema.MASCOT_PROTOCOL_OBJECT_PREFIX, MS2Schema.COMET_PROTOCOL_OBJECT_PREFIX, MS2Schema.IMPORTED_SEARCH_PROTOCOL_OBJECT_PREFIX, MS2Schema.FRACTION_ROLLUP_PROTOCOL_OBJECT_PREFIX);
    public static final String MS2_RUNS_NAME = "MS2 Runs";
    public static final String MS2_MODULE_NAME = "MS2";

    @Override
    public String getName()
    {
        return MS2_MODULE_NAME;
    }

    @Override
    public @Nullable Double getSchemaVersion()
    {
        return 24.000;
    }

    @Override
    @NotNull
    protected Collection<WebPartFactory> createWebPartFactories()
    {
        return List.of(
            new BaseWebPartFactory(MS2_RUNS_NAME)
            {
                {
                    addLegacyNames("MS2 Runs (Enhanced)", "MS2 Runs (Deprecated)", "MS2 Experiment Runs");
                }

                @Override
                public WebPartView<?> getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                {
                    QueryView result = ExperimentService.get().createExperimentRunWebPart(new ViewContext(portalCtx), SEARCH_RUN_TYPE);
                    result.setTitle(MS2_RUNS_NAME);
                    result.setTitleHref(new ActionURL(MS2Controller.ShowListAction.class, portalCtx.getContainer()));
                    return result;
                }
            },
            new ProteomicsWebPartFactory(ProteinSearchWebPart.NAME, WebPartFactory.LOCATION_BODY, WebPartFactory.LOCATION_RIGHT)
            {
                @Override
                public WebPartView<?> getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                {
                    return new ProteinSearchWebPart(!WebPartFactory.LOCATION_RIGHT.equalsIgnoreCase(webPart.getLocation()), ProbabilityProteinSearchForm.createDefault());
                }
            },
            new ProteomicsWebPartFactory(MSSearchWebpart.NAME)
            {
                @Override
                public WebPartView<?> getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                {
                    return new MSSearchWebpart();
                }
            },
            new ProteomicsWebPartFactory(WEBPART_PEP_SEARCH)
            {
                @Override
                public WebPartView<?> getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                {
                    PepSearchModel model = new PepSearchModel(portalCtx.getContainer());
                    JspView<PepSearchModel> view = new JspView<>("/org/labkey/protein/view/PepSearchView.jsp", model);
                    view.setTitle(WEBPART_PEP_SEARCH);
                    return view;
                }
            }
        );
    }

    @Override
    public boolean hasScripts()
    {
        return true;
    }

    @Override
    protected void init()
    {
        addController("ms2", MS2Controller.class);
        addController("ms2-pipeline", PipelineController.class);

        MS2Schema.register(this);
        MS2Service.setInstance(new MS2ServiceImpl());
    }

    @Override
    protected void startupAfterSpringConfig(ModuleContext moduleContext)
    {
        PipelineService service = PipelineService.get();
        service.registerPipelineProvider(new MS2PipelineProvider(this));
        service.registerPipelineProvider(new XTandemPipelineProvider(this), "X!Tandem (Cluster)");
        service.registerPipelineProvider(new MascotCPipelineProvider(this), "Mascot (Cluster)");
        service.registerPipelineProvider(new SequestPipelineProvider(this));
        service.registerPipelineProvider(new CometPipelineProvider(this), "Comet");
        service.registerPipelineProvider(new FractionRollupPipelineProvider(this));
        service.registerPipelineProvider(new ProteinProphetPipelineProvider(this));

        final Set<ExperimentRunType> runTypes = new HashSet<>();
        runTypes.add(SEARCH_RUN_TYPE);
        runTypes.add(new MS2SearchExperimentRunType("Imported Searches", MS2Schema.TableType.ImportedSearchRuns.toString(), Handler.Priority.HIGH, MS2Schema.IMPORTED_SEARCH_PROTOCOL_OBJECT_PREFIX));
        runTypes.add(new MS2SearchExperimentRunType("X!Tandem Searches", MS2Schema.TableType.XTandemSearchRuns.toString(), Handler.Priority.HIGH, MS2Schema.XTANDEM_PROTOCOL_OBJECT_PREFIX));
        runTypes.add(new MS2SearchExperimentRunType("Comet Searches", MS2Schema.TableType.CometSearchRuns.toString(), Handler.Priority.HIGH, MS2Schema.COMET_PROTOCOL_OBJECT_PREFIX));
        runTypes.add(new MS2SearchExperimentRunType("Mascot Searches", MS2Schema.TableType.MascotSearchRuns.toString(), Handler.Priority.HIGH, MS2Schema.MASCOT_PROTOCOL_OBJECT_PREFIX));
        runTypes.add(new MS2SearchExperimentRunType("Sequest Searches", MS2Schema.TableType.SequestSearchRuns.toString(), Handler.Priority.HIGH, MS2Schema.SEQUEST_PROTOCOL_OBJECT_PREFIX));
        runTypes.add(new MS2SearchExperimentRunType("Fraction Rollups", MS2Schema.TableType.FractionRollupsRuns.toString(), Handler.Priority.HIGH, MS2Schema.FRACTION_ROLLUP_PROTOCOL_OBJECT_PREFIX));

        ExperimentService.get().registerExperimentRunTypeSource(container ->
        {
            if (container == null || container.getActiveModules(moduleContext.getUpgradeUser()).contains(MS2Module.this))
            {
                return runTypes;
            }
            return Collections.emptySet();
        });

        ExperimentService.get().registerExperimentDataHandler(new PepXmlExperimentDataHandler());
        ExperimentService.get().registerExperimentDataHandler(new ProteinProphetExperimentDataHandler());
        ExperimentService.get().registerExperimentDataHandler(new MascotDatExperimentDataHandler());

        ContainerManager.addContainerListener(new MS2ContainerListener());
        FolderTypeManager.get().registerFolderType(this, new MS2FolderType(this));

        ReportService.get().registerReport(new SpectraCountRReport());
        ReportService.get().registerReport(new SingleMS2RunRReport());
        ReportService.get().addUIProvider(new MS2ReportUIProvider());
        MS2Controller.registerAdminConsoleLinks();

        SearchService ss = SearchService.get();

        if (null != ss)
        {
            ss.addDocumentParser(new MzXMLDocumentParser());
            ss.addDocumentParser(new MzMLDocumentParser());
            ss.addDocumentParser(new DatDocumentParser());
            ss.addDocumentParser(new SequestLogDocumentParser());
            ss.addDocumentParser(new MGFDocumentParser());
        }

        FileContentService fcs = FileContentService.get();
        if (fcs != null)
        {
            fcs.addFileListener(new TableUpdaterFileListener(MS2Manager.getTableInfoRuns(), "Path", TableUpdaterFileListener.Type.filePathForwardSlash, "Run")
            {
                @Override
                public int fileMoved(@NotNull File srcFile, @NotNull File destFile, @Nullable User user, @Nullable Container container)
                {
                    int result = super.fileMoved(srcFile, destFile, user, container);
                    MS2Manager.clearRunCache();
                    return result;
                }
            });

            SQLFragment containerFrag = new SQLFragment();
            containerFrag.append("SELECT r.Container FROM ");
            containerFrag.append(MS2Manager.getTableInfoRuns(), "r");
            containerFrag.append(" WHERE r.Run = ").append(TableUpdaterFileListener.TABLE_ALIAS).append(".Run");

            fcs.addFileListener(new TableUpdaterFileListener(MS2Manager.getTableInfoFractions(), "MzXMLURL", TableUpdaterFileListener.Type.uri, "Fraction", containerFrag)
            {
                @Override
                public int fileMoved(@NotNull File srcFile, @NotNull File destFile, @Nullable User user, @Nullable Container container)
                {
                    int result = super.fileMoved(srcFile, destFile, user, container);
                    MS2Manager.clearFractionCache();
                    return result;
                }
            });
            fcs.addFileListener(new TableUpdaterFileListener(MS2Manager.getTableInfoProteinProphetFiles(), "FilePath", TableUpdaterFileListener.Type.filePath, "RowId", containerFrag));
        }

        SequencesTableInfo.setTableModifier(MS2Schema.STANDARD_MODIFIER);

        // Use ms2.FastaAdmin view and add Runs column with link to show all runs action
        ProteinManager.registerFastaAdminDataRegionModifier(rgn -> {
            rgn.setColumns(MS2Manager.getTableInfoFastaAdmin().getColumns("FileName, Loaded, FastaId, Runs"));
            ActionURL runsURL = new ActionURL(MS2Controller.ShowAllRunsAction.class, ContainerManager.getRoot())
                    .addParameter(MS2Manager.getDataRegionNameRuns() + ".FastaId~eq", "${FastaId}");
            rgn.getDisplayColumn("Runs").setURL(runsURL);
        });

        // On FASTA delete, check ms2.FastaAdmin for references to runs
        ProteinSchema.registerValidForFastaDeleteSelectorProvider(filter -> {
            filter.addCondition(FieldKey.fromString("Runs"), null, CompareType.ISBLANK);
            return new TableSelector(MS2Manager.getTableInfoFastaAdmin(), filter, null);
        });
        ProteinSchema.registerInvalidForFastaDeleteReason("are still referenced by runs");

        ProteinService.get().registerPeptideSearchView(new PeptidesViewProvider());
        ProteinService.get().registerProteinSearchView(new ProteinSearchGroupViewProvider());
        ProteinService.get().registerProteinSearchView(new ProteinSearchViewProvider());
        MS2Controller.registerPeptidePanelForSearch();
    }

    @NotNull
    @Override
    public Collection<String> getSummary(Container c)
    {
        Collection<String> list = new LinkedList<>();
        long count = MS2Manager.getRunCount(c);
        if (count > 0)
            list.add(count + " MS2 Run" + (count > 1 ? "s" : ""));
        return list;
    }

    @Override
    @NotNull
    public List<String> getSchemaNames()
    {
        return List.of(MS2Schema.SCHEMA_NAME);
    }

    @Override
    @NotNull
    public Set<Class> getIntegrationTests()
    {
        return Set.of(
            Comet2014ParamsBuilder.FullParseTestCase.class,
            Comet2015ParamsBuilder.FullParseTestCase.class,
            MascotClientImpl.TestCase.class,
            MS2Controller.TestCase.class,
            ThermoSequestParamsBuilder.TestCase.class
        );
    }

    @Override
    @NotNull
    public Set<Class> getUnitTests()
    {
        return Set.of(
            BibliospecSpectrumRenderer.TestCase.class,
            BooleanParamsValidator.TestCase.class,
            Comet2014ParamsBuilder.LimitedParseTestCase.class,
            Comet2015ParamsBuilder.LimitedParseTestCase.class,
            ListParamsValidator.TestCase.class,
            MS2Modification.MS2ModificationTest.class,
            MS2RunType.TestCase.class,
            MultipleDoubleParamsValidator.TestCase.class,
            MultipleIntegerParamsValidator.TestCase.class,
            NaturalNumberParamsValidator.TestCase.class,
            NonNegativeIntegerParamsValidator.TestCase.class,
            PeptideProphetSummary.TestCase.class,
            PositiveDoubleParamsValidator.TestCase.class,
            Protein.TestCase.class,
            ProteinCoverageMapBuilder.TestCase.class,
            RandomAccessJrapMzxmlIterator.TestCase.class,
            RealNumberParamsValidator.TestCase.class,
            SequestSearchTask.TestCase.class,
            TPPTask.TestCase.class
        );
    }
}
