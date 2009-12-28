/*
 * Copyright (c) 2007-2009 LabKey Corporation
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

import org.labkey.api.data.*;
import org.labkey.api.exp.*;
import org.labkey.api.exp.api.*;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.Lookup;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.study.TimepointType;
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.assay.*;
import org.labkey.api.study.query.ResultsQueryView;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.util.FileType;
import org.labkey.api.view.*;
import org.labkey.api.qc.DataExchangeHandler;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.nab.query.NabRunDataTable;
import org.labkey.nab.query.NabSchema;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.*;

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
    public static final String ORIGINAL_DATAFILE_PROPERTY_NAME = "OriginalDataFile";
    public static final String SAMPLE_DESCRIPTION_PROPERTY_NAME = "SampleDescription";
    public static final String SAMPLE_DESCRIPTION_PROPERTY_CAPTION = "Sample Description";
    public static final String CURVE_FIT_METHOD_PROPERTY_NAME = "CurveFitMethod";
    public static final String CURVE_FIT_METHOD_PROPERTY_CAPTION = "Curve Fit Method";
    public static final String LOCK_AXES_PROPERTY_NAME = "LockYAxis";
    public static final String LOCK_AXES_PROPERTY_CAPTION = "Lock Graph Y-Axis";

    public NabAssayProvider()
    {
        super("NabAssayProtocol", "NabAssayRun", NabDataHandler.NAB_DATA_TYPE, new AssayTableMetadata(
            FieldKey.fromParts("Properties", NabDataHandler.NAB_INPUT_MATERIAL_DATA_PROPERTY, "Property"),
            FieldKey.fromParts("Run"),
            FieldKey.fromParts("ObjectId")));
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
            cutoff.setFormat("0.0##");
        }

        addProperty(runDomain, "VirusName", "Virus Name", PropertyType.STRING);
        addProperty(runDomain, "VirusID", "Virus ID", PropertyType.STRING);
        addProperty(runDomain, "HostCell", "Host Cell", PropertyType.STRING);
        addProperty(runDomain, "StudyName", "Study Name", PropertyType.STRING);
        addProperty(runDomain, "ExperimentPerformer", "Experiment Performer", PropertyType.STRING);
        addProperty(runDomain, "ExperimentID", "Experiment ID", PropertyType.STRING);
        addProperty(runDomain, "IncubationTime", "Incubation Time", PropertyType.STRING);
        addProperty(runDomain, "PlateNumber", "Plate Number", PropertyType.STRING);
        addProperty(runDomain, "ExperimentDate", "Experiment Date", PropertyType.DATE_TIME);
        addProperty(runDomain, "FileID", "File ID", PropertyType.STRING);
        addProperty(runDomain, LOCK_AXES_PROPERTY_NAME, LOCK_AXES_PROPERTY_CAPTION, PropertyType.BOOLEAN);

        Container lookupContainer = c.getProject();
        DomainProperty method = addProperty(runDomain, CURVE_FIT_METHOD_PROPERTY_NAME, CURVE_FIT_METHOD_PROPERTY_CAPTION, PropertyType.STRING);
        method.setLookup(new Lookup(lookupContainer, NabSchema.SCHEMA_NAME, NabSchema.CURVE_FIT_METHOD_TABLE_NAME));
        method.setRequired(true);
        return result;
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

    public ExpData getDataForDataRow(Object dataRowId)
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

    public TableInfo createDataTable(AssaySchema schema, ExpProtocol protocol)
    {
        NabSchema nabSchema = new NabSchema(schema.getUser(), schema.getContainer());
        nabSchema.setTargetStudy(schema.getTargetStudy());
        NabRunDataTable table = nabSchema.createDataRowTable(protocol);
        addCopiedToStudyColumns(table, protocol, schema.getUser(), "objectId", true);
        return table;
    }

    protected PropertyType getDataRowIdType()
    {
        return PropertyType.INTEGER;
    }

    public ActionURL copyToStudy(User user, ExpProtocol protocol, Container study, Map<Integer, AssayPublishKey> dataKeys, List<String> errors)
    {
        try
        {
            TimepointType studyType = AssayPublishService.get().getTimepointType(study);

            Set<PropertyDescriptor> typeSet = new LinkedHashSet<PropertyDescriptor>();

            typeSet.add(createPublishPropertyDescriptor(study, getTableMetadata().getResultRowIdFieldKey().toString(), getDataRowIdType()));
            typeSet.add(createPublishPropertyDescriptor(study, "SourceLSID", getDataRowIdType()));

            Domain sampleDomain = getSampleWellGroupDomain(protocol);
            DomainProperty[] samplePDs = sampleDomain.getProperties();

            PropertyDescriptor[] dataPDs = NabSchema.getExistingDataProperties(protocol);
            CopyToStudyContext context = new CopyToStudyContext(protocol, user);

            SimpleFilter filter = new SimpleFilter();
            filter.addInClause(getTableMetadata().getResultRowIdFieldKey().toString(), dataKeys.keySet());

            OntologyObject[] dataRows = Table.select(OntologyManager.getTinfoObject(), Table.ALL_COLUMNS, filter,
                    new Sort(getTableMetadata().getResultRowIdFieldKey().toString()), OntologyObject.class);

            List<Map<String, Object>> dataMaps = new ArrayList<Map<String, Object>>(dataRows.length);
            Container sourceContainer = null;

            // little hack here: since the property descriptors created by the 'addProperty' calls below are not in the database,
            // they have no RowId, and such are never equal to each other.  Since the loop below is run once for each row of data,
            // this will produce a types set that contains rowCount*columnCount property descriptors unless we prevent additions
            // to the map after the first row.  This is done by nulling out the 'tempTypes' object after the first iteration:
            Set<PropertyDescriptor> tempTypes = typeSet;
            for (OntologyObject row : dataRows)
            {
                Map<String, Object> dataMap = new HashMap<String, Object>();
                Map<String, Object> rowProperties = OntologyManager.getProperties(row.getContainer(), row.getObjectURI());
                String materialLsid = null;
                for (PropertyDescriptor pd : dataPDs)
                {
                    Object value = rowProperties.get(pd.getPropertyURI());
                    if (!NabDataHandler.NAB_INPUT_MATERIAL_DATA_PROPERTY.equals(pd.getName()))
                        addProperty(pd, value, dataMap, tempTypes);
                    else
                        materialLsid = (String) value;
                }

                ExpMaterial material = ExperimentService.get().getExpMaterial(materialLsid);
                if (material != null)
                {
                    for (DomainProperty pd : samplePDs)
                    {
                        if (!PARTICIPANTID_PROPERTY_NAME.equals(pd.getName()) &&
                                !VISITID_PROPERTY_NAME.equals(pd.getName()) &&
                                !DATE_PROPERTY_NAME.equals(pd.getName()))
                        {
                            addProperty(pd.getPropertyDescriptor(), material.getProperty(pd), dataMap, tempTypes);
                        }
                    }
                }

                ExpRun run = context.getRun(row);
                sourceContainer = run.getContainer();

                AssayPublishKey publishKey = dataKeys.get(row.getObjectId());
                dataMap.put("ParticipantID", publishKey.getParticipantId());
                dataMap.put("SequenceNum", publishKey.getVisitId());
                if (TimepointType.DATE == studyType)
                {
                    dataMap.put("Date", publishKey.getDate());
                }
                dataMap.put("SourceLSID", run.getLSID());
                dataMap.put(getTableMetadata().getResultRowIdFieldKey().toString(), publishKey.getDataId());
                addStandardRunPublishProperties(study, tempTypes, dataMap, run, context);
                dataMaps.add(dataMap);
                tempTypes = null;
            }
            return AssayPublishService.get().publishAssayData(user, sourceContainer, study, protocol.getName(), protocol,
                    dataMaps, new ArrayList<PropertyDescriptor>(typeSet), getTableMetadata().getResultRowIdFieldKey().toString(), errors);
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    public ActionURL getImportURL(Container container, ExpProtocol protocol)
    {
        return PageFlowUtil.urlProvider(AssayUrls.class).getProtocolURL(container, protocol, NabUploadWizardAction.class);
    }

    public List<ParticipantVisitResolverType> getParticipantVisitResolverTypes()
    {
        return Arrays.asList(new ParticipantVisitLookupResolverType(), new SpecimenIDLookupResolverType(), new ParticipantDateLookupResolverType(), new ThawListResolverType());
    }

    public static class NabResultsQueryView extends ResultsQueryView
    {
        public NabResultsQueryView(ExpProtocol protocol, ViewContext context, AssayProvider provider)
        {
            super(protocol, context, getDefaultSettings(protocol, context, provider));
        }

        private static QuerySettings getDefaultSettings(ExpProtocol protocol, ViewContext context, AssayProvider provider)
        {
            String name = AssayService.get().getResultsTableName(protocol);
            QuerySettings settings = new QuerySettings(context, name);
            settings.setSchemaName(AssaySchema.NAME);
            settings.setQueryName(name);
            return settings;
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
                        out.write("[<a href=\"" + url.getLocalURIString() + "\" title=\"View run details\">run&nbsp;details</a>]");
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
    public DataExchangeHandler getDataExchangeHandler()
    {
        return new NabDataExchangeHandler();
    }

    public PipelineProvider getPipelineProvider()
    {
        return new AssayPipelineProvider(NabModule.class,
                new PipelineProvider.FileTypesEntryFilter(getDataType().getFileType()), this, "Import NAb");
    }
}
