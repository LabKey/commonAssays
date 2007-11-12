package org.labkey.nab;

import org.labkey.api.study.assay.*;
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.TimepointType;
import org.labkey.api.exp.*;
import org.labkey.api.exp.api.*;
import org.labkey.api.exp.list.ListDefinition;
import org.labkey.api.exp.list.ListService;
import org.labkey.api.exp.list.ListItem;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.Lookup;
import org.labkey.api.data.*;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryViewCustomizer;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.DataView;
import org.labkey.api.security.User;
import org.labkey.nab.query.NabSchema;
import org.labkey.nab.query.NabRunDataTable;
import org.labkey.common.util.Pair;

import javax.servlet.ServletException;
import java.util.*;
import java.sql.SQLException;
import java.io.IOException;
import java.io.Writer;

/**
 * User: brittp
 * Date: Sep 21, 2007
 * Time: 2:33:52 PM
 */
public class NabAssayProvider extends PlateBasedAssayProvider
{
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
        super("NabAssayProtocol", "NabAssayRun", NabDataHandler.NAB_DATA_LSID_PREFIX);
    }

    protected void registerLsidHandler()
    {
        LsidManager.get().registerHandler(_runLSIDPrefix, new LsidManager.LsidHandler()
        {
            public ExpRun getObject(Lsid lsid)
            {
                return ExperimentService.get().getExpRun(lsid.toString());
            }

            public String getDisplayURL(Lsid lsid)
            {
                ExpRun run = ExperimentService.get().getExpRun(lsid.toString());
                if (run == null)
                    return null;
                ExpProtocol protocol = run.getProtocol();
                if (protocol == null)
                    return null;
                ViewURLHelper dataURL = new ViewURLHelper("NabAssay", "details", run.getContainer()).addParameter("rowId", run.getRowId());
                return dataURL.getLocalURIString();
            }
        });
    }

    private ListDefinition createSimpleList(Container lookupContainer, User user, String listName, String displayColumn,
                                            String displayColumnDescription, String... values)
    {
        Map<String, ListDefinition> lists = ListService.get().getLists(lookupContainer);
        ListDefinition sampleMethodList = lists.get(listName);
        if (sampleMethodList == null)
        {
            sampleMethodList = ListService.get().createList(lookupContainer, listName);
            DomainProperty nameProperty = addProperty(sampleMethodList.getDomain(), displayColumn, PropertyType.STRING);
            nameProperty.setPropertyURI(sampleMethodList.getDomain().getTypeURI() + "#" + displayColumn);
            sampleMethodList.setKeyName(nameProperty.getName());
            sampleMethodList.setKeyType(ListDefinition.KeyType.Varchar);
            sampleMethodList.setDescription(displayColumnDescription);
            sampleMethodList.setTitleColumn(displayColumn);
            try
            {
                sampleMethodList.save(user);
                for (String value : values)
                {
                    ListItem concentration = sampleMethodList.createListItem();
                    concentration.setKey(value);
                    concentration.setProperty(nameProperty, value);
                    concentration.save(user);
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
        return sampleMethodList;
    }

    protected Domain createRunDomain(Container c, User user)
    {
        Domain runDomain = super.createRunDomain(c, user);
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
        ListDefinition curveFitMethodList = createSimpleList(lookupContainer, user, "NabCurveFitMethod", "FitMethod",
                "Method of curve fitting that will be applied to the neutralization data for each sample.", "Four Parameter", "Five Parameter");
        DomainProperty method = addProperty(runDomain, CURVE_FIT_METHOD_PROPERTY_NAME, CURVE_FIT_METHOD_PROPERTY_CAPTION, PropertyType.STRING);
        method.setLookup(new Lookup(lookupContainer, "lists", curveFitMethodList.getName()));
        method.setRequired(true);
        return runDomain;
    }

    protected Domain createUploadSetDomain(Container c, User user)
    {
        return super.createUploadSetDomain(c, user);
    }

    protected Domain createSampleWellGroupDomain(Container c, User user)
    {
        Domain sampleWellGroupDomain = super.createSampleWellGroupDomain(c, user);
        Container lookupContainer = c.getProject();
        ListDefinition sampleMethodList = createSimpleList(lookupContainer, user, "NabSamplePreparationMethods", "Method",
                "Method of preparation for a sample in a NAb well group.", SampleInfo.Method.Dilution.toString(), SampleInfo.Method.Concentration.toString());
        addProperty(sampleWellGroupDomain, SPECIMENID_PROPERTY_NAME, SPECIMENID_PROPERTY_CAPTION, PropertyType.STRING);
        addProperty(sampleWellGroupDomain, PARTICIPANTID_PROPERTY_NAME, PARTICIPANTID_PROPERTY_CAPTION, PropertyType.STRING);
        addProperty(sampleWellGroupDomain, VISITID_PROPERTY_NAME, VISITID_PROPERTY_CAPTION, PropertyType.DOUBLE);
        addProperty(sampleWellGroupDomain, DATE_PROPERTY_NAME, DATE_PROPERTY_CAPTION, PropertyType.DATE_TIME);
        addProperty(sampleWellGroupDomain, SAMPLE_DESCRIPTION_PROPERTY_NAME, SAMPLE_DESCRIPTION_PROPERTY_CAPTION, PropertyType.STRING);
        addProperty(sampleWellGroupDomain, SAMPLE_INITIAL_DILUTION_PROPERTY_NAME, SAMPLE_INITIAL_DILUTION_PROPERTY_CAPTION, PropertyType.DOUBLE).setRequired(true);
        addProperty(sampleWellGroupDomain, SAMPLE_DILUTION_FACTOR_PROPERTY_NAME, SAMPLE_DILUTION_FACTOR_PROPERTY_CAPTION, PropertyType.DOUBLE).setRequired(true);
        DomainProperty method = addProperty(sampleWellGroupDomain, SAMPLE_METHOD_PROPERTY_NAME, SAMPLE_METHOD_PROPERTY_CAPTION, PropertyType.STRING);
        method.setLookup(new Lookup(lookupContainer, "lists", sampleMethodList.getName()));
        method.setRequired(true);
        return sampleWellGroupDomain;
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
        return "Neutralizing Antibodies (NAb)";
    }

    public HttpView getDataDescriptionView(AssayRunUploadForm form)
    {
        return new HtmlView("The NAb data file is a specially formatted Excel 1997-2003 file with a .xls extension.");
    }

    public TableInfo createDataTable(QuerySchema schema, String alias, ExpProtocol protocol)
    {
        return NabSchema.getDataRowTable(schema.getUser(), protocol, alias);
    }

    public FieldKey getParticipantIDFieldKey()
    {
        return FieldKey.fromParts("Properties",
                NabDataHandler.NAB_INPUT_MATERIAL_DATA_PROPERTY, "Property", PARTICIPANTID_PROPERTY_NAME);
    }

    public FieldKey getVisitIDFieldKey(Container container)
    {
        if (AssayPublishService.get().getTimepointType(container) == TimepointType.VISIT)
            return FieldKey.fromParts("Properties",
                    NabDataHandler.NAB_INPUT_MATERIAL_DATA_PROPERTY, "Property", VISITID_PROPERTY_NAME);
        else
            return FieldKey.fromParts("Properties",
                    NabDataHandler.NAB_INPUT_MATERIAL_DATA_PROPERTY, "Property", DATE_PROPERTY_NAME);

    }

    public FieldKey getRunIdFieldKeyFromDataRow()
    {
        return FieldKey.fromParts("Run", "RowId");
    }

    public FieldKey getDataRowIdFieldKey()
    {
        return FieldKey.fromParts("ObjectId");
    }

    public FieldKey getSpecimenIDFieldKey()
    {
        return FieldKey.fromParts("Properties",
                NabDataHandler.NAB_INPUT_MATERIAL_DATA_PROPERTY, "Property", SPECIMENID_PROPERTY_NAME);
    }

    protected PropertyType getDataRowIdType()
    {
        return PropertyType.INTEGER;
    }
    
    public ViewURLHelper publish(User user, ExpProtocol protocol, Container study, Set<AssayPublishKey> dataKeys, List<String> errors)
    {
        try
        {
            int rowIndex = 0;

            TimepointType studyType = AssayPublishService.get().getTimepointType(study);

            Set<PropertyDescriptor> typeSet = new LinkedHashSet<PropertyDescriptor>();

            Map<Integer, ExpRun> runCache = new HashMap<Integer, ExpRun>();
            Map<Integer, Map<String, Object>> runPropertyCache = new HashMap<Integer, Map<String, Object>>();

            typeSet.add(createPublishPropertyDescriptor(study, getDataRowIdFieldKey().toString(), getDataRowIdType()));
            typeSet.add(createPublishPropertyDescriptor(study, "SourceLSID", getDataRowIdType()));

            PropertyDescriptor[] samplePDs = getSampleWellGroupColumns(protocol);
            PropertyDescriptor[] dataPDs = NabSchema.getExistingDataProperties(protocol);
            List<PropertyDescriptor> runPDs = new ArrayList<PropertyDescriptor>();
            runPDs.addAll(Arrays.asList(getRunPropertyColumns(protocol)));
            runPDs.addAll(Arrays.asList(getUploadSetColumns(protocol)));

            SimpleFilter filter = new SimpleFilter();
            List<Object> ids = new ArrayList<Object>();
            Map<Object, AssayPublishKey> dataIdToPublishKey = new HashMap<Object, AssayPublishKey>();
            for (AssayPublishKey dataKey : dataKeys)
            {
                ids.add(dataKey.getDataId());
                dataIdToPublishKey.put(dataKey.getDataId(), dataKey);
            }
            filter.addInClause(getDataRowIdFieldKey().toString(), ids);

            OntologyObject[] dataRows = Table.select(OntologyManager.getTinfoObject(), Table.ALL_COLUMNS, filter,
                    new Sort(getDataRowIdFieldKey().toString()), OntologyObject.class);

            Map<String, Object>[] dataMaps = new HashMap[dataRows.length];

            for (OntologyObject row : dataRows)
            {
                Map<String, Object> dataMap = new HashMap<String, Object>();
                Map<String, Object> rowProperties = OntologyManager.getProperties(row.getContainer(), row.getObjectURI());
                String materialLsid = null;
                for (PropertyDescriptor pd : dataPDs)
                {
                    Object value = rowProperties.get(pd.getPropertyURI());
                    if (!NabDataHandler.NAB_INPUT_MATERIAL_DATA_PROPERTY.equals(pd.getName()))
                        addProperty(pd, value, dataMap, typeSet);
                    else
                        materialLsid = (String) value;
                }

                ExpMaterial material = ExperimentService.get().getExpMaterial(materialLsid);
                if (material != null)
                {
                    for (PropertyDescriptor pd : samplePDs)
                    {
                        if (!PARTICIPANTID_PROPERTY_NAME.equals(pd.getName()) &&
                                !VISITID_PROPERTY_NAME.equals(pd.getName()) &&
                                !DATE_PROPERTY_NAME.equals(pd.getName()))
                        {
                            addProperty(pd, material.getProperty(pd), dataMap, typeSet);
                        }
                    }
                }

                ExpRun run = runCache.get(row.getOwnerObjectId());
                if (run == null)
                {
                    OntologyObject dataRowParent = OntologyManager.getOntologyObject(row.getOwnerObjectId());
                    ExpData data = ExperimentService.get().getExpData(dataRowParent.getObjectURI());
                    run = data.getRun();
                    runCache.put(row.getOwnerObjectId(), run);
                }

                Map<String, Object> runProperties = runPropertyCache.get(run.getRowId());
                if (runProperties == null)
                {
                    runProperties = OntologyManager.getProperties(run.getContainer().getId(), run.getLSID());
                    runPropertyCache.put(run.getRowId(), runProperties);
                }

                for (PropertyDescriptor pd : runPDs)
                {
                    if (!TARGET_STUDY_PROPERTY_NAME.equals(pd.getName()) &&
                            !PARTICIPANT_VISIT_RESOLVER_PROPERTY_NAME.equals(pd.getName()))
                    {
                        PropertyDescriptor publishPd = pd.clone();
                        publishPd.setName("Run " + pd.getName());
                        addProperty(publishPd, runProperties.get(pd.getPropertyURI()), dataMap, typeSet);
                    }
                }

                AssayPublishKey publishKey = dataIdToPublishKey.get(row.getObjectId());
                dataMap.put("ParticipantID", publishKey.getParticipantId());
                dataMap.put("SequenceNum", publishKey.getVisitId());
                if (TimepointType.DATE == studyType)
                {
                    dataMap.put("Date", publishKey.getDate());
                }
                dataMap.put("SourceLSID", run.getLSID());
                dataMap.put(getDataRowIdFieldKey().toString(), publishKey.getDataId());
                addProperty(study, "Run Name", run.getName(), dataMap, typeSet);
                addProperty(study, "Run Comments", run.getComments(), dataMap, typeSet);
                addProperty(study, "Run CreatedOn", run.getCreated(), dataMap, typeSet);
                User createdBy = run.getCreatedBy();
                addProperty(study, "Run CreatedBy", createdBy == null ? null : createdBy.getDisplayName(), dataMap, typeSet);
                dataMaps[rowIndex++] = dataMap;
            }
            return AssayPublishService.get().publishAssayData(user, study, protocol.getName(), protocol, 
                    dataMaps, new ArrayList<PropertyDescriptor>(typeSet), getDataRowIdFieldKey().toString(), errors);
        }
        catch (SQLException e)
        {
            errors.add(e.getMessage());
            return null;
        }
        catch (IOException e)
        {
            errors.add(e.getMessage());
            return null;
        }
        catch (ServletException e)
        {
            errors.add(e.getMessage());
            return null;
        }
    }

    public ViewURLHelper getUploadWizardURL(Container container, ExpProtocol protocol)
    {
        ViewURLHelper url = new ViewURLHelper("NabAssay", "nabUploadWizard.view", container);
        url.addParameter("rowId", protocol.getRowId());
        return url;
    }
    
    public List<ParticipantVisitResolverType> getParticipantVisitResolverTypes()
    {
        return Arrays.asList(new ParticipantVisitLookupResolverType(), new SpecimenIDLookupResolverType(), new ParticipantDateLookupResolverType(), new ThawListResolverType());
    }

    public static class NabQueryViewCustomizer implements QueryViewCustomizer
    {
        private String _idColumn;

        public NabQueryViewCustomizer(String idColumn)
        {
            _idColumn = idColumn;
        }

        public void customize(DataView view)
        {
            DataRegion rgn = view.getDataRegion();
            rgn.addColumn(0, new SimpleDisplayColumn()
            {
                public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
                {
                    Object runId = ctx.getRow().get(_idColumn);
                    if (runId != null)
                    {
                        ViewURLHelper url = new ViewURLHelper("NabAssay", "details", ctx.getContainer()).addParameter("rowId", "" + runId);
                        out.write("[<a href=\"" + url.getLocalURIString() + "\" title=\"View run details\">run&nbsp;details</a>]");
                    }
                }
            });
        }
    }

    public QueryViewCustomizer getRunsViewCustomizer(Container container, ExpProtocol protocol)
    {
        return new NabQueryViewCustomizer(ExpRunTable.Column.RowId.toString());
    }

    public QueryViewCustomizer getDataViewCustomizer(final Container container, final ExpProtocol protocol)
    {
        return new NabQueryViewCustomizer(NabRunDataTable.RUN_ID_COLUMN_NAME)
        {
            public void customize(DataView view)
            {
                super.customize(view);
                view.getDataRegion().setRecordSelectorValueColumns("ObjectId");
                view.getDataRegion().addHiddenFormField("protocolId", "" + protocol.getRowId());
                ButtonBar bbar = view.getDataRegion().getButtonBar(DataRegion.MODE_GRID);
                ViewURLHelper graphSelectedURL = new ViewURLHelper("NabAssay", "graphSelected", container);
                ActionButton graphSelectedButton = new ActionButton("button", "Graph Selected");
                graphSelectedButton.setScript("return verifySelected(this.form, \"" + graphSelectedURL.getLocalURIString() + "\", \"get\", \"rows\")");
                graphSelectedButton.setActionType(ActionButton.Action.GET);
                bbar.add(graphSelectedButton);
            }
        };
    }

    public Pair<ExpProtocol, List<Domain>> getAssayTemplate(User user, Container targetContainer, ExpProtocol toCopy)
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

    public Pair<ExpProtocol, List<Domain>> getAssayTemplate(User user, Container targetContainer)
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
}
