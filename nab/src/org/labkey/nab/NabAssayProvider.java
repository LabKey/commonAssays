/*
 * Copyright (c) 2007-2012 LabKey Corporation
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

package org.labkey.nab;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.MenuButton;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SimpleDisplayColumn;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.LsidManager;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.OntologyObject;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.Lookup;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.qc.DataExchangeHandler;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.assay.AbstractPlateBasedAssayProvider;
import org.labkey.api.study.assay.AssayDataCollector;
import org.labkey.api.study.assay.AssayDataLinkDisplayColumn;
import org.labkey.api.study.assay.AssayDataType;
import org.labkey.api.study.assay.AssayPipelineProvider;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.AssayTableMetadata;
import org.labkey.api.study.assay.AssayUrls;
import org.labkey.api.study.assay.ParticipantVisitResolverType;
import org.labkey.api.study.assay.RunListDetailsQueryView;
import org.labkey.api.study.assay.ThawListResolverType;
import org.labkey.api.study.query.ResultsQueryView;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.ViewContext;
import org.labkey.nab.query.NabRunDataTable;
import org.labkey.nab.query.NabSchema;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: brittp
 * Date: Sep 21, 2007
 * Time: 2:33:52 PM
 */
public class NabAssayProvider extends AbstractPlateBasedAssayProvider
{
    public static final String CUSTOM_DETAILS_VIEW_NAME = "CustomDetailsView";
    public static final String[] CUTOFF_PROPERTIES = { "Cutoff1", "Cutoff2", "Cutoff3" };
    public static final String SAMPLE_METHOD_PROPERTY_NAME = "Method";
    public static final String SAMPLE_METHOD_PROPERTY_CAPTION = "Method";
    public static final String SAMPLE_INITIAL_DILUTION_PROPERTY_NAME = "InitialDilution";
    public static final String SAMPLE_INITIAL_DILUTION_PROPERTY_CAPTION = "Initial Dilution";
    public static final String SAMPLE_DILUTION_FACTOR_PROPERTY_NAME = "Factor";
    public static final String SAMPLE_DILUTION_FACTOR_PROPERTY_CAPTION = "Dilution Factor";
    public static final String SAMPLE_DESCRIPTION_PROPERTY_NAME = "SampleDescription";
    public static final String SAMPLE_DESCRIPTION_PROPERTY_CAPTION = "Sample Description";
    public static final String CURVE_FIT_METHOD_PROPERTY_NAME = "CurveFitMethod";
    public static final String CURVE_FIT_METHOD_PROPERTY_CAPTION = "Curve Fit Method";
    public static final String LOCK_AXES_PROPERTY_NAME = "LockYAxis";
    public static final String LOCK_AXES_PROPERTY_CAPTION = "Lock Graph Y-Axis";
    private static final String NAB_RUN_LSID_PREFIX = "NabAssayRun";
    private static final String NAB_ASSAY_PROTOCOL = "NabAssayProtocol";
    public static final String VIRUS_NAME_PROPERTY_NAME = "VirusName";
    public static final String VIRUS_ID_PROPERTY_NAME = "VirusID";
    public static final String HOST_CELL_PROPERTY_NAME = "HostCell";
    public static final String STUDY_NAME_PROPERTY_NAME = "StudyName";
    public static final String EXPERIMENT_PERFORMER_PROPERTY_NAME = "ExperimentPerformer";
    public static final String EXPERIMENT_ID_PROPERTY_NAME = "ExperimentID";
    public static final String INCUBATION_TIME_PROPERTY_NAME = "IncubationTime";
    public static final String PLATE_NUMBER_PROPERTY_NAME = "PlateNumber";
    public static final String EXPERIMENT_DATE_PROPERTY_NAME = "ExperimentDate";
    public static final String FILE_ID_PROPERTY_NAME = "FileID";

    public NabAssayProvider()
    {
        this(NAB_ASSAY_PROTOCOL, NAB_RUN_LSID_PREFIX, SinglePlateNabDataHandler.NAB_DATA_TYPE);
    }

    public NabAssayProvider(String protocolLSIDPrefix, String runLSIDPrefix, AssayDataType dataType)
    {
        super(protocolLSIDPrefix, runLSIDPrefix, dataType);
    }


    protected void registerLsidHandler()
    {
        LsidManager.get().registerHandler(_runLSIDPrefix, new LsidManager.ExpRunLsidHandler()
        {
            @Override
            protected ActionURL getDisplayURL(Container c, ExpProtocol protocol, ExpRun run)
            {
                return new ActionURL(NabAssayController.DetailsAction.class, run.getContainer()).addParameter("rowId", run.getRowId());
            }

            @Override
            public boolean hasPermission(Lsid lsid, @NotNull User user, @NotNull Class<? extends Permission> perm)
            {
                // defer permission checking until user attempts to view the details page
                return true;

//                ExpRun run = getObject(lsid);
//                if (run == null)
//                    return false;
//                Container c = run.getContainer();
//                return c.hasPermission(user, perm, RunDataSetContextualRoles.getContextualRolesForRun(c, user, run));
            }
        });
    }

    @Override
    public AssayTableMetadata getTableMetadata(ExpProtocol protocol)
    {
        return new AssayTableMetadata(
                this,
                protocol,
                FieldKey.fromParts("Properties", SinglePlateNabDataHandler.NAB_INPUT_MATERIAL_DATA_PROPERTY, "Property"),
                FieldKey.fromParts("Run"),
                FieldKey.fromParts("ObjectId"));
    }

    @Override
    public ExpRunTable createRunTable(AssaySchema schema, ExpProtocol protocol)
    {
        final ExpRunTable runTable = super.createRunTable(schema, protocol);
        ColumnInfo nameColumn = runTable.getColumn(ExpRunTable.Column.Name);
        // NAb has two detail type views of a run - the filtered results/data grid, and the run details page that
        // shows the graph. Set the run's name to be a link to the grid instead of the default details page.
        nameColumn.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new AssayDataLinkDisplayColumn(colInfo, runTable.getContainerFilter());
            }
        });
        return runTable;
    }

    protected Pair<Domain, Map<DomainProperty, Object>> createRunDomain(Container c, User user)
    {
        Pair<Domain, Map<DomainProperty, Object>> result = super.createRunDomain(c, user);
        Domain runDomain = result.getKey();
        boolean first = true;
        for (int i = 0; i < CUTOFF_PROPERTIES.length; i++)
        {
            DomainProperty cutoff = addProperty(runDomain, CUTOFF_PROPERTIES[i], "Cutoff Percentage (" + (i + 1) + ")",
                    PropertyType.INTEGER);
            if (first)
            {
                cutoff.setRequired(true);
                first = false;
            }
            cutoff.setShownInUpdateView(false);
            cutoff.setFormat("0.0##");
        }

        addPassThroughRunProperties(runDomain);

        Container lookupContainer = c.getProject();
        DomainProperty method = addProperty(runDomain, CURVE_FIT_METHOD_PROPERTY_NAME, CURVE_FIT_METHOD_PROPERTY_CAPTION, PropertyType.STRING);
        method.setLookup(new Lookup(lookupContainer, NabSchema.SCHEMA_NAME, NabSchema.CURVE_FIT_METHOD_TABLE_NAME));
        method.setRequired(true);
        method.setShownInUpdateView(false);
        return result;
    }

    @Override
    public List<AssayDataCollector> getDataCollectors(Map<String, File> uploadedFiles, AssayRunUploadForm context)
    {
        return super.getDataCollectors(uploadedFiles, context, false);
    }

    protected void addPassThroughRunProperties(Domain runDomain)
    {
        addProperty(runDomain, VIRUS_NAME_PROPERTY_NAME, "Virus Name", PropertyType.STRING);
        addProperty(runDomain, VIRUS_ID_PROPERTY_NAME, "Virus ID", PropertyType.STRING);
        addProperty(runDomain, HOST_CELL_PROPERTY_NAME, "Host Cell", PropertyType.STRING);
        addProperty(runDomain, STUDY_NAME_PROPERTY_NAME, "Study Name", PropertyType.STRING);
        addProperty(runDomain, EXPERIMENT_PERFORMER_PROPERTY_NAME, "Experiment Performer", PropertyType.STRING);
        addProperty(runDomain, EXPERIMENT_ID_PROPERTY_NAME, "Experiment ID", PropertyType.STRING);
        addProperty(runDomain, INCUBATION_TIME_PROPERTY_NAME, "Incubation Time", PropertyType.STRING);
        addProperty(runDomain, PLATE_NUMBER_PROPERTY_NAME, "Plate Number", PropertyType.STRING);
        addProperty(runDomain, EXPERIMENT_DATE_PROPERTY_NAME, "Experiment Date", PropertyType.DATE_TIME);
        addProperty(runDomain, FILE_ID_PROPERTY_NAME, "File ID", PropertyType.STRING);
        addProperty(runDomain, LOCK_AXES_PROPERTY_NAME, LOCK_AXES_PROPERTY_CAPTION, PropertyType.BOOLEAN);
    }

    protected Pair<Domain, Map<DomainProperty, Object>> createSampleWellGroupDomain(Container c, User user)
    {
        Pair<Domain, Map<DomainProperty, Object>> result = super.createSampleWellGroupDomain(c, user);
        Domain sampleWellGroupDomain = result.getKey();
        Container lookupContainer = c.getProject();
        addProperty(sampleWellGroupDomain, SPECIMENID_PROPERTY_NAME, SPECIMENID_PROPERTY_CAPTION, PropertyType.STRING);
        addProperty(sampleWellGroupDomain, PARTICIPANTID_PROPERTY_NAME, PARTICIPANTID_PROPERTY_CAPTION, PropertyType.STRING);
        addProperty(sampleWellGroupDomain, VISITID_PROPERTY_NAME, VISITID_PROPERTY_CAPTION, PropertyType.DOUBLE);
        addProperty(sampleWellGroupDomain, DATE_PROPERTY_NAME, DATE_PROPERTY_CAPTION, PropertyType.DATE_TIME);
        addProperty(sampleWellGroupDomain, SAMPLE_DESCRIPTION_PROPERTY_NAME, SAMPLE_DESCRIPTION_PROPERTY_CAPTION, PropertyType.STRING);
        addProperty(sampleWellGroupDomain, SAMPLE_INITIAL_DILUTION_PROPERTY_NAME, SAMPLE_INITIAL_DILUTION_PROPERTY_CAPTION, PropertyType.DOUBLE).setRequired(true);
        addProperty(sampleWellGroupDomain, SAMPLE_DILUTION_FACTOR_PROPERTY_NAME, SAMPLE_DILUTION_FACTOR_PROPERTY_CAPTION, PropertyType.DOUBLE).setRequired(true);
        DomainProperty method = addProperty(sampleWellGroupDomain, SAMPLE_METHOD_PROPERTY_NAME, SAMPLE_METHOD_PROPERTY_CAPTION, PropertyType.STRING);
        method.setLookup(new Lookup(lookupContainer, NabSchema.SCHEMA_NAME, NabSchema.SAMPLE_PREPARATION_METHOD_TABLE_NAME));
        method.setRequired(true);
        return result;
    }

    protected Map<String, Set<String>> getRequiredDomainProperties()
    {
        Map<String, Set<String>> domainMap = super.getRequiredDomainProperties();
        Set<String> sampleProperties = domainMap.get(ASSAY_DOMAIN_SAMPLE_WELLGROUP);
        if (sampleProperties == null)
        {
            sampleProperties = new HashSet<String>();
            domainMap.put(ASSAY_DOMAIN_SAMPLE_WELLGROUP, sampleProperties);
        }
        sampleProperties.add(SPECIMENID_PROPERTY_NAME);
        sampleProperties.add(PARTICIPANTID_PROPERTY_NAME);
        sampleProperties.add(VISITID_PROPERTY_NAME);
        sampleProperties.add(DATE_PROPERTY_NAME);
        sampleProperties.add(SAMPLE_INITIAL_DILUTION_PROPERTY_NAME);
        sampleProperties.add(SAMPLE_DILUTION_FACTOR_PROPERTY_NAME);
        sampleProperties.add(SAMPLE_METHOD_PROPERTY_NAME);

        Set<String> runProperties = domainMap.get(ExpProtocol.ASSAY_DOMAIN_RUN);
        if (runProperties == null)
        {
            runProperties = new HashSet<String>();
            domainMap.put(ExpProtocol.ASSAY_DOMAIN_RUN, runProperties);
        }
        runProperties.add(CURVE_FIT_METHOD_PROPERTY_NAME);
        runProperties.add(CUTOFF_PROPERTIES[0]);

        return domainMap;
    }

    public Domain getResultsDomain(ExpProtocol protocol)
    {
        return null;
    }

    public ExpData getDataForDataRow(Object dataRowId, ExpProtocol protocol)
    {
        if (!(dataRowId instanceof Integer))
            return null;
        OntologyObject dataRow = OntologyManager.getOntologyObject((Integer) dataRowId);
        if (dataRow == null)
            return null;
        OntologyObject dataRowParent = OntologyManager.getOntologyObject(dataRow.getOwnerObjectId());
        if (dataRowParent == null)
            return null;
        return ExperimentService.get().getExpData(dataRowParent.getObjectURI());
    }

    public String getName()
    {
        return "TZM-bl Neutralization (NAb)";
    }

    public HttpView getDataDescriptionView(AssayRunUploadForm form)
    {
        return new HtmlView("The NAb data file is a specially formatted Excel 1997-2003 file with a .xls extension.");
    }

    public NabRunDataTable createDataTable(AssaySchema schema, ExpProtocol protocol, boolean includeCopiedToStudyColumns)
    {
        NabSchema nabSchema = new NabSchema(schema.getUser(), schema.getContainer());
        nabSchema.setTargetStudy(schema.getTargetStudy());
        NabRunDataTable table = nabSchema.createDataRowTable(protocol);
        if (includeCopiedToStudyColumns)
        {
            addCopiedToStudyColumns(table, protocol, schema.getUser(), true);
        }
        return table;
    }

    public ActionURL getImportURL(Container container, ExpProtocol protocol)
    {
        return PageFlowUtil.urlProvider(AssayUrls.class).getProtocolURL(container, protocol, NabUploadWizardAction.class);
    }

    public List<ParticipantVisitResolverType> getParticipantVisitResolverTypes()
    {
        return Arrays.asList(new ParticipantVisitLookupResolverType(), new SpecimenIDLookupResolverType(), new ParticipantDateLookupResolverType(), new ThawListResolverType());
    }

    public NabDataHandler getDataHandler()
    {
        return new SinglePlateNabDataHandler();
    }

    public static class NabResultsQueryView extends ResultsQueryView
    {
        public NabResultsQueryView(ExpProtocol protocol, ViewContext context, AssayProvider provider)
        {
            super(protocol, context, getDefaultSettings(protocol, context));
        }

        private static QuerySettings getDefaultSettings(ExpProtocol protocol, ViewContext context)
        {
            UserSchema schema = AssayService.get().createSchema(context.getUser(), context.getContainer());
            String name = AssayService.get().getResultsTableName(protocol);
            return schema.getSettings(context, name, name);
        }

        private void addGraphSubItems(NavTree parent, Domain domain, String dataRegionName, Set<String> excluded)
        {
            ActionURL graphSelectedURL = new ActionURL(NabAssayController.GraphSelectedAction.class, getContainer());
            for (DomainProperty prop : domain.getProperties())
            {
                if (!excluded.contains(prop.getName()))
                {
                    NavTree menuItem = new NavTree(prop.getLabel(), "#");
                    menuItem.setScript("document.forms['" + dataRegionName + "'].action = '" + graphSelectedURL.getLocalURIString() + "';\n" +
                            "document.forms['" + dataRegionName + "'].captionColumn.value = '" + prop.getName() + "';\n" +
                            "document.forms['" + dataRegionName + "'].chartTitle.value = 'Neutralization by " + prop.getLabel() + "';\n" +
                            "document.forms['" + dataRegionName + "'].method = 'GET';\n" +
                            "document.forms['" + dataRegionName + "'].submit(); return false;");
                    parent.addChild(menuItem);
                }
            }
        }

        public DataView createDataView()
        {
            DataView view = super.createDataView();
            DataRegion rgn = view.getDataRegion();
            rgn.setRecordSelectorValueColumns("ObjectId");
            rgn.addHiddenFormField("protocolId", "" + _protocol.getRowId());
            ButtonBar bbar = view.getDataRegion().getButtonBar(DataRegion.MODE_GRID);

            ActionURL graphSelectedURL = new ActionURL(NabAssayController.GraphSelectedAction.class, getContainer());
            MenuButton graphSelectedButton = new MenuButton("Graph");
            rgn.addHiddenFormField("captionColumn", "");
            rgn.addHiddenFormField("chartTitle", "");

            graphSelectedButton.addMenuItem("Default Graph", "#",
                    "document.forms['" + rgn.getName() + "'].action = '" + graphSelectedURL.getLocalURIString() + "';\n" +
                    "document.forms['" + rgn.getName() + "'].method = 'GET';\n" +
                    "document.forms['" + rgn.getName() + "'].submit(); return false;");

            Domain sampleDomain = ((NabAssayProvider) _provider).getSampleWellGroupDomain(_protocol);
            NavTree sampleSubMenu = new NavTree("Custom Caption (Sample)");
            Set<String> excluded = new HashSet<String>();
            excluded.add(SAMPLE_METHOD_PROPERTY_NAME);
            excluded.add(SAMPLE_INITIAL_DILUTION_PROPERTY_NAME);
            excluded.add(SAMPLE_DILUTION_FACTOR_PROPERTY_NAME);
            addGraphSubItems(sampleSubMenu, sampleDomain, rgn.getName(), excluded);
            graphSelectedButton.addMenuItem(sampleSubMenu);

            Domain runDomain = _provider.getRunDomain(_protocol);
            NavTree runSubMenu = new NavTree("Custom Caption (Run)");
            excluded.clear();
            excluded.add(CURVE_FIT_METHOD_PROPERTY_NAME);
            excluded.add(LOCK_AXES_PROPERTY_NAME);
            excluded.addAll(Arrays.asList(CUTOFF_PROPERTIES));
            addGraphSubItems(runSubMenu, runDomain, rgn.getName(), excluded);
            graphSelectedButton.addMenuItem(runSubMenu);
            graphSelectedButton.setRequiresSelection(true);
            bbar.add(graphSelectedButton);

            rgn.addDisplayColumn(0, new SimpleDisplayColumn()
            {
                public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
                {
                    Object runId = ctx.getRow().get(NabRunDataTable.RUN_ID_COLUMN_NAME);
                    if (runId != null)
                    {
                        ActionURL url = new ActionURL(NabAssayController.DetailsAction.class, ctx.getContainer()).addParameter("rowId", "" + runId);
                        Map<String, String> title = new HashMap<String, String>();
                        title.put("title", "View run details");
                        out.write(PageFlowUtil.textLink("run details", url, "", "", title));
                    }
                }

                @Override
                public void addQueryColumns(Set<ColumnInfo> set)
                {
                    super.addQueryColumns(set);
                    ColumnInfo runIdColumn = getTable().getColumn(NabRunDataTable.RUN_ID_COLUMN_NAME);
                    if (runIdColumn != null)
                        set.add(runIdColumn);
                }
            });
            return view;
        }
    }

    public NabResultsQueryView createResultsQueryView(ViewContext context, ExpProtocol protocol)
    {
        return new NabResultsQueryView(protocol, context, this);
    }

    public static class NabRunListQueryView extends RunListDetailsQueryView
    {
        public NabRunListQueryView(ExpProtocol protocol, ViewContext context)
        {
            super(protocol, context, NabAssayController.DetailsAction.class, "rowId", ExpRunTable.Column.RowId.toString());
        }
    }

    public NabRunListQueryView createRunQueryView(ViewContext context, ExpProtocol protocol)
    {
        return new NabRunListQueryView(protocol, context);
    }

    public boolean hasUsefulDetailsPage()
    {
        return true;
    }

    public String getDescription()
    {
        return "Imports a specially formatted Excel 1997-2003 file (.xls). " +
                "Measures neutralization in TZM-bl cells as a function of a reduction in Tat-induced luciferase (Luc) " +
                "reporter gene expression after a single round of infection. Montefiori, D.C. 2004" +
                PageFlowUtil.helpPopup("NAb", "<a href=\"http://www.ncbi.nlm.nih.gov/pubmed/18432938\">" +
                        "Evaluating neutralizing antibodies against HIV, SIV and SHIV in luciferase " +
                        "reporter gene assays</a>.  Current Protocols in Immunology, (Coligan, J.E., " +
                        "A.M. Kruisbeek, D.H. Margulies, E.M. Shevach, W. Strober, and R. Coico, eds.), John Wiley & Sons, 12.11.1-12.11.15.", true);
    }

    public Pair<ExpProtocol, List<Pair<Domain, Map<DomainProperty, Object>>>> getAssayTemplate(User user, Container targetContainer, ExpProtocol toCopy)
    {
        try
        {
            NabManager.get().ensurePlateTemplate(targetContainer, user);
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
        return super.getAssayTemplate(user, targetContainer, toCopy);
    }

    public Pair<ExpProtocol, List<Pair<Domain, Map<DomainProperty, Object>>>> getAssayTemplate(User user, Container targetContainer)
    {
        try
        {
            NabManager.get().ensurePlateTemplate(targetContainer, user);
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
        return super.getAssayTemplate(user, targetContainer);
    }

    @Override
    public DataExchangeHandler createDataExchangeHandler()
    {
        return new NabDataExchangeHandler();
    }

    public PipelineProvider getPipelineProvider()
    {
        return new AssayPipelineProvider(NabModule.class,
                new PipelineProvider.FileTypesEntryFilter(getDataType().getFileType()), this, "Import NAb");
    }

    @Override
    public boolean supportsReRun()
    {
        return true;
    }
}
