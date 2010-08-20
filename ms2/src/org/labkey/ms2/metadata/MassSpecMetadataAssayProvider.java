/*
 * Copyright (c) 2007-2010 LabKey Corporation
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

package org.labkey.ms2.metadata;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.*;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.api.*;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.exp.query.ExpDataTable;
import org.labkey.api.exp.query.ExpMaterialTable;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.query.*;
import org.labkey.api.security.User;
import org.labkey.api.settings.AppProps;
import org.labkey.api.study.TimepointType;
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.assay.*;
import org.labkey.api.util.GUID;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Module;
import org.labkey.ms2.pipeline.MS2PipelineManager;
import org.labkey.ms2.pipeline.AbstractMS2SearchProtocol;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

/**
 * User: Peter@labkey.com
 * Date: Oct 17, 2008
 * Time: 5:54:45 PM
 */


public class MassSpecMetadataAssayProvider extends AbstractAssayProvider
{
    public static final String PROTOCOL_LSID_NAMESPACE_PREFIX = "MassSpecMetadataProtcool";
    public static final String NAME = "Mass Spec Metadata";
    public static final AssayDataType MS_ASSAY_DATA_TYPE = new AssayDataType("MZXMLData", AbstractMS2SearchProtocol.FT_MZXML);
    public static final String RUN_LSID_NAMESPACE_PREFIX = "MassSpecMetadataRun";

    public static final String FRACTION_DOMAIN_PREFIX = ExpProtocol.ASSAY_DOMAIN_PREFIX + "Fractions";
    public static final String FRACTION_SET_NAME = "Fraction Fields";
    public static final String FRACTION_SET_LABEL = "These fields are used to describe searches where one sample has been divided into multiple fractions. The fields should describe the properties that vary from fraction to fraction.";
    private static final String FRACTION_INPUT_ROLE = "Fraction";

    public MassSpecMetadataAssayProvider()
    {
        super(PROTOCOL_LSID_NAMESPACE_PREFIX, RUN_LSID_NAMESPACE_PREFIX, MS_ASSAY_DATA_TYPE, new AssayTableMetadata(
                FieldKey.fromParts("Run"),
                FieldKey.fromParts("Run"),
                FieldKey.fromParts("RowId")));
    }

    @Override
    protected Pair<Domain, Map<DomainProperty, Object>> createBatchDomain(Container c, User user)
    {
        // don't call the standard createBatchDomain because we don't want the target study or participant data resolver
        Domain domain = PropertyService.get().createDomain(c, getPresubstitutionLsid(ExpProtocol.ASSAY_DOMAIN_BATCH), "Batch Fields");
        domain.setDescription("The user is prompted for batch properties once for each set of runs they import. Batches " +
                "are a convenience to let users set properties that seldom change in one place and import many runs " +
                "using them. This is the first step of the import process.");

        return new Pair<Domain, Map<DomainProperty, Object>>(domain, Collections.<DomainProperty, Object>emptyMap());
    }

    public static final String SEARCH_COUNT_COLUMN = "MS2SearchCount";
    public static final String SEARCHES_COLUMN = "MS2Searches";

    private class LinkDisplayColumn extends IconDisplayColumn
    {
        private ColumnInfo _searchCountColumn;

        public LinkDisplayColumn(ColumnInfo runIdColumn, Container container)
        {
            super(runIdColumn, 18, 18, new ActionURL(MassSpecMetadataController.SearchLinkAction.class, container), "runId", AppProps.getInstance().getContextPath() + "/MS2/images/runIcon.gif");
        }

        @Override
        public void addQueryColumns(Set<ColumnInfo> columns)
        {
            super.addQueryColumns(columns);
            FieldKey fk = new FieldKey(getBoundColumn().getFieldKey().getParent(), SEARCH_COUNT_COLUMN);
            _searchCountColumn = QueryService.get().getColumns(getBoundColumn().getParentTable(), Collections.singletonList(fk)).get(fk);
            if (_searchCountColumn != null)
            {
                columns.add(_searchCountColumn);
            }
        }

        @Override
        public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
        {
            if (_searchCountColumn == null || ((Number)_searchCountColumn.getValue(ctx)).intValue() > 0)
            {
                super.renderGridCellContents(ctx, out);
            }
            else
            {
                out.write("&nbsp;");
            }
        }
    }

    @Override
    public ExpRunTable createRunTable(final AssaySchema schema, ExpProtocol protocol)
    {
        ExpRunTable result = super.createRunTable(schema, protocol);
        SQLFragment searchCountSQL = new SQLFragment();
        searchCountSQL.append(getSearchRunSQL(schema.getContainer(), result.getContainerFilter(), ExprColumn.STR_TABLE_ALIAS + ".RowId", "COUNT(DISTINCT(er.RowId))"));
        ExprColumn searchCountCol = new ExprColumn(result, SEARCH_COUNT_COLUMN, searchCountSQL, Types.INTEGER);
        searchCountCol.setLabel("MS2 Search Count");
        result.addColumn(searchCountCol);

        ColumnInfo searchLinkCol = result.addColumn(SEARCHES_COLUMN, ExpRunTable.Column.RowId);
        searchLinkCol.setHidden(false);
        searchLinkCol.setLabel("MS2 Search Results");
        searchLinkCol.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new LinkDisplayColumn(colInfo, schema.getContainer());
            }
        });

        List<FieldKey> defaultCols = new ArrayList<FieldKey>(result.getDefaultVisibleColumns());
        defaultCols.add(2, FieldKey.fromParts(searchLinkCol.getName()));
        result.setDefaultVisibleColumns(defaultCols);

        return result;
    }

    public static SQLFragment getSearchRunSQL(Container container, ContainerFilter containerFilter, String runIdSQL, String selectSQL)
    {
        SQLFragment searchCountSQL = new SQLFragment("(SELECT " + selectSQL + " FROM " +
                MS2Manager.getTableInfoRuns() + " r, " +
                ExperimentService.get().getTinfoExperimentRun() + " er, " +
                ExperimentService.get().getTinfoData() + " d, " +
                ExperimentService.get().getTinfoDataInput() + " di, " +
                ExperimentService.get().getTinfoProtocolApplication() + " pa " +
                " WHERE di.TargetApplicationId = pa.RowId AND pa.RunId = er.RowId AND r.ExperimentRunLSID = er.LSID " +
                " AND r.deleted = ? AND d.RunId = " + runIdSQL + " AND d.RowId = di.DataId AND (" );
        searchCountSQL.add(Boolean.FALSE);
        String separator = "";
        for (String prefix : MS2Module.SEARCH_RUN_TYPE.getProtocolPrefixes())
        {
            searchCountSQL.append(separator);
            searchCountSQL.append("er.ProtocolLSID LIKE ");
            searchCountSQL.append(ExperimentService.get().getSchema().getSqlDialect().concatenate("'%'", "?", "'%'"));
            searchCountSQL.add(prefix);
            separator = " OR ";
        }
        searchCountSQL.append(")");
        Collection<String> ids = containerFilter.getIds(container);
        if (ids != null)
        {
            searchCountSQL.append(" AND er.Container IN (");
            separator = "";
            for (String id : ids)
            {
                searchCountSQL.append(separator);
                searchCountSQL.append("?");
                searchCountSQL.add(id);
                separator = ", ";
            }
            searchCountSQL.append(")");
        }

        searchCountSQL.append(")");
        return searchCountSQL;
    }

    @Override
    public Pair<ExpRun, ExpExperiment> saveExperimentRun(AssayRunUploadContext context, ExpExperiment batch) throws ExperimentException, ValidationException
    {
        MassSpecMetadataAssayForm form = (MassSpecMetadataAssayForm)context;
        if (form.isFractions())
        {
            // If this is a fractions search, first derive the fraction samples
            deriveFractions(form);
            Pair<ExpRun, ExpExperiment> result = null;
            // Then upload a bunch of runs
            while (!form.getSelectedDataCollector().getFileQueue(form).isEmpty())
            {
                result = super.saveExperimentRun(context, batch);
                batch = result.getValue();
                form.clearUploadedData();
                form.getSelectedDataCollector().uploadComplete(form);
            }
            return result;
        }
        else
        {
            return super.saveExperimentRun(context, batch);
        }
    }

    private ExpRun deriveFractions(MassSpecMetadataAssayForm form) throws ExperimentException
    {
        ExpSampleSet fractionSet = getFractionSampleSet(form);

        Map<File, Map<DomainProperty, String>> mapFilesToFractionProperties = form.getFractionProperties(fractionSet);

        Map<ExpMaterial, String> derivedSamples = new HashMap<ExpMaterial, String>();

        try
        {
            for (Map.Entry<File,Map<DomainProperty, String>> entry : mapFilesToFractionProperties.entrySet())
            {
                // generate unique lsids for the derived samples
                File mzxmlFile = entry.getKey();
                String fileNameBase = mzxmlFile.getName().substring(0, (mzxmlFile.getName().lastIndexOf('.')));
                Map<DomainProperty, String> properties = entry.getValue();
                Lsid derivedLsid = new Lsid(fractionSet.getMaterialLSIDPrefix() + "OBJECT");
                derivedLsid.setObjectId(GUID.makeGUID());
                int index = 0;
                while(ExperimentService.get().getExpMaterial(derivedLsid.toString()) != null)
                    derivedLsid.setObjectId(derivedLsid.getObjectId() + "-" + ++index);

                ExpMaterial derivedMaterial = ExperimentService.get().createExpMaterial(form.getContainer()
                        , derivedLsid.toString(), "Fraction - " + fileNameBase);
                derivedMaterial.setCpasType(fractionSet.getLSID());
                // could put the fraction properties on the fraction material object or on the run.  decided to do the run

                for (Map.Entry<DomainProperty, String> property : properties.entrySet())
                {
                    String value = property.getValue();
                    derivedMaterial.setProperty(form.getUser(), property.getKey().getPropertyDescriptor(), value);
                }

                derivedSamples.put(derivedMaterial, FRACTION_INPUT_ROLE);
                form.getFileFractionMap().put(mzxmlFile, derivedMaterial);
            }
            ViewBackgroundInfo info = new ViewBackgroundInfo(form.getContainer(), form.getUser(), form.getActionURL());
            Map<ExpMaterial, String> startingMaterials = form.getInputMaterials();
            ExpRun run = ExperimentService.get().deriveSamples(startingMaterials, derivedSamples, info, null);

            // Change the run's name
            StringBuilder sb = new StringBuilder("Fractionate ");
            String separator = "";
            for (ExpMaterial inputMaterial : startingMaterials.keySet())
            {
                sb.append(separator);
                sb.append(inputMaterial.getName());
                separator = ", ";
            }
            sb.append(" into ");
            sb.append(derivedSamples.size());
            sb.append(" fractions");
            run.setName(sb.toString());
            run.save(form.getUser());

            return run;
        }
        catch (ValidationException e)
        {
            throw new ExperimentException(e);
        }
    }

    protected void addInputMaterials(AssayRunUploadContext context, Map<ExpMaterial, String> inputMaterials, ParticipantVisitResolverType resolverType) throws ExperimentException
    {
        MassSpecMetadataAssayForm form = (MassSpecMetadataAssayForm)context;
        if (form.isFractions())
        {
            Map<String, File> files = form.getUploadedData();
            assert files.containsKey(AssayDataCollector.PRIMARY_FILE);
            File mzXMLFile = files.get(AssayDataCollector.PRIMARY_FILE);
            ExpMaterial sample = form.getFileFractionMap().get(mzXMLFile);
            assert sample != null;
            inputMaterials.put(sample, FRACTION_INPUT_ROLE);
        }
        else
        {
            inputMaterials.putAll(form.getInputMaterials());
        }
    }

    protected Pair<Domain, Map<DomainProperty, Object>> createFractionDomain(Container c)
    {
        String domainLsid = getPresubstitutionLsid(FRACTION_DOMAIN_PREFIX);
        Domain fractionDomain = PropertyService.get().createDomain(c, domainLsid, FRACTION_SET_NAME);
        fractionDomain.setDescription(FRACTION_SET_LABEL);
        return new Pair<Domain, Map<DomainProperty, Object>>(fractionDomain, Collections.<DomainProperty, Object>emptyMap());
    }
    
    public List<Pair<Domain, Map<DomainProperty, Object>>> createDefaultDomains(Container c, User user)
    {
        List<Pair<Domain, Map<DomainProperty, Object>>> result = super.createDefaultDomains(c, user);
        result.add(createFractionDomain(c));
        return result;
    }

    public String getName()
    {
        return NAME;
    }

    public List<AssayDataCollector> getDataCollectors(Map<String, File> uploadedFiles, AssayRunUploadForm context)
    {
        return Collections.<AssayDataCollector>singletonList(new MassSpecMetadataDataCollector());
    }

    public ExpData getDataForDataRow(Object dataRowId)
    {
        return ExperimentService.get().getExpData(((Number)dataRowId).intValue());
    }

    public Domain getFractionDomain(ExpProtocol protocol)
    {
        return getDomainByPrefix(protocol, FRACTION_DOMAIN_PREFIX);
    }

    public ExpDataTable createDataTable(final AssaySchema schema, final ExpProtocol protocol)
    {
        final ExpDataTable result = new ExpSchema(schema.getUser(), schema.getContainer()).getDatasTable();
        SQLFragment runConditionSQL = new SQLFragment("RunId IN (SELECT RowId FROM " +
                ExperimentService.get().getTinfoExperimentRun() + " WHERE ProtocolLSID = ?)");
        runConditionSQL.add(protocol.getLSID());
        result.addCondition(runConditionSQL, "RunId");
        result.getColumn(ExpDataTable.Column.Run).setFk(new LookupForeignKey("RowId")
        {
            public TableInfo getLookupTableInfo()
            {
                ExpRunTable expRunTable = AssayService.get().createRunTable(protocol, MassSpecMetadataAssayProvider.this, schema.getUser(), schema.getContainer());
                expRunTable.setContainerFilter(result.getContainerFilter());
                return expRunTable;
            }
        });

        List<FieldKey> cols = new ArrayList<FieldKey>(result.getDefaultVisibleColumns());
        cols.remove(FieldKey.fromParts(ExpDataTable.Column.DataFileUrl));
        for (DomainProperty fractionProperty : getFractionDomain(protocol).getProperties())
        {
            cols.add(getDataFractionPropertyFieldKey(fractionProperty));
        }
        for (DomainProperty runProperty : getRunDomain(protocol).getProperties())
        {
            cols.add(FieldKey.fromParts(ExpDataTable.Column.Run.toString(), "Run Properties", runProperty.getName()));
        }
        cols.add(0, FieldKey.fromParts(ExpDataTable.Column.Run.toString(), SEARCHES_COLUMN));
        result.setDefaultVisibleColumns(cols);

        return result;
    }

    private FieldKey getDataFractionPropertyFieldKey(DomainProperty fractionProperty)
    {
        return FieldKey.fromParts(
                        ExpDataTable.Column.Run.toString(),
                        ExpRunTable.Column.Input.toString(),
                        FRACTION_INPUT_ROLE,
                        ExpMaterialTable.Column.Property.toString(),
                        fractionProperty.getName());
    }

    public ActionURL copyToStudy(User user, ExpProtocol protocol, Container study, Map<Integer, AssayPublishKey> dataKeys, List<String> errors)
    {
        try
        {
            TimepointType studyType = AssayPublishService.get().getTimepointType(study);

            SimpleFilter filter = new SimpleFilter();
            filter.addInClause(getTableMetadata().getResultRowIdFieldKey().toString(), dataKeys.keySet());

            AssaySchema schema = AssayService.get().createSchema(user, protocol.getContainer());
            ExpDataTable tableInfo = createDataTable(schema, protocol);
            tableInfo.setContainerFilter(new ContainerFilter.CurrentAndSubfolders(user));

            List<FieldKey> fieldKeys = new ArrayList<FieldKey>();
            DomainProperty[] fractionProperties = getFractionDomain(protocol).getProperties();
            for (DomainProperty prop : fractionProperties)
            {
                fieldKeys.add(getDataFractionPropertyFieldKey(prop));
            }
            fieldKeys.add(getTableMetadata().getRunRowIdFieldKeyFromResults());
            fieldKeys.add(FieldKey.fromParts("DataFileUrl"));
            fieldKeys.add(getTableMetadata().getResultRowIdFieldKey());
            Map<FieldKey, ColumnInfo> columns = QueryService.get().getColumns(tableInfo, fieldKeys);

            ColumnInfo dataRowIdCol = columns.get(getTableMetadata().getResultRowIdFieldKey());

            SQLFragment selectSQL = Table.getSelectSQL(tableInfo, columns.values(), filter, new Sort(getTableMetadata().getResultRowIdFieldKey().toString()));
            Table.TableResultSet rs = Table.executeQuery(schema.getDbSchema(), selectSQL);

            List<Map<String, Object>> dataMaps = new ArrayList<Map<String, Object>>();

            CopyToStudyContext context = new CopyToStudyContext(protocol, user);

            Set<PropertyDescriptor> typeList = new LinkedHashSet<PropertyDescriptor>();
            typeList.add(createPublishPropertyDescriptor(study, getTableMetadata().getResultRowIdFieldKey().toString(), PropertyType.INTEGER));
            typeList.add(createPublishPropertyDescriptor(study, "SourceLSID", PropertyType.INTEGER));

            Container sourceContainer = null;

            // little hack here: since the property descriptors created by the 'addProperty' calls below are not in the database,
            // they have no RowId, and such are never equal to each other.  Since the loop below is run once for each row of data,
            // this will produce a types set that contains rowCount*columnCount property descriptors unless we prevent additions
            // to the map after the first row.  This is done by nulling out the 'tempTypes' object after the first iteration:
            Set<PropertyDescriptor> tempTypes = typeList;
            while (rs.next())
            {
                Map<String, Object> dataMap = new HashMap<String, Object>();

                ExpData data = context.getData(((Number)dataRowIdCol.getValue(rs)).intValue());

                PropertyDescriptor namePD = addProperty(study, "Name", data.getName(), dataMap, tempTypes);
                setStandardPropertyAttributes(tableInfo.getColumn("Name"), namePD);
                PropertyDescriptor urlPD = addProperty(study, "DataFileUrl", data.getDataFileUrl(), dataMap, tempTypes);
                setStandardPropertyAttributes(tableInfo.getColumn("DataFileUrl"), urlPD);

                for (DomainProperty prop : fractionProperties)
                {
                    PropertyDescriptor pd = prop.getPropertyDescriptor();
                    // We should skip properties that are set by the resolver: participantID,
                    // and either date or visit, depending on the type of study
                    boolean skipProperty = PARTICIPANTID_PROPERTY_NAME.equals(prop.getName());

                    if (TimepointType.DATE == studyType)
                            skipProperty = skipProperty || DATE_PROPERTY_NAME.equals(prop.getName());
                    else // it's visit-based
                        skipProperty = skipProperty || VISITID_PROPERTY_NAME.equals(prop.getName());

                    ColumnInfo col = columns.get(getDataFractionPropertyFieldKey(prop));
                    if (!skipProperty && col != null)
                    {
                        // We won't find the fraction properties if we haven't done any fraction searches yet
                        addProperty(pd, col.getValue(rs), dataMap, tempTypes);
                    }
                }

                ExpRun run = context.getRun(data);
                sourceContainer = run.getContainer();

                AssayPublishKey publishKey = dataKeys.get(data.getRowId());
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
                    dataMaps, new ArrayList<PropertyDescriptor>(typeList), getTableMetadata().getResultRowIdFieldKey().toString(), errors);
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    public String getDescription()
    {
        return "Describes metadata for mass spec data files, including mzXML";
    }

    public List<ParticipantVisitResolverType> getParticipantVisitResolverTypes()
    {
        return Collections.emptyList();
    }

    public HttpView getDataDescriptionView(AssayRunUploadForm form)
    {
        return null;
    }

    @Override
    public boolean canCopyToStudy()
    {
        return true;
    }
    
    public ActionURL getImportURL(Container container, ExpProtocol protocol)
    {
        return PageFlowUtil.urlProvider(AssayUrls.class).getProtocolURL(container, protocol, MassSpecMetadataUploadAction.class);
    }

    @NotNull
    protected ExpSampleSet getFractionSampleSet(AssayRunUploadContext context) throws ExperimentException
    {
        String domainURI = getDomainURIForPrefix(context.getProtocol(), FRACTION_DOMAIN_PREFIX);
        ExpSampleSet sampleSet=null;
        if (null != domainURI)
            sampleSet = ExperimentService.get().getSampleSet(domainURI);

        if (sampleSet == null)
        {
            sampleSet = ExperimentService.get().createSampleSet();
            sampleSet.setContainer(context.getProtocol().getContainer());
            sampleSet.setName("Fractions: " + context.getProtocol().getName());
            sampleSet.setLSID(domainURI);

            Lsid sampleSetLSID = new Lsid(domainURI);
            sampleSetLSID.setNamespacePrefix("Sample");
            sampleSetLSID.setNamespaceSuffix(context.getProtocol().getContainer().getRowId() + "." + context.getProtocol().getName());
            sampleSetLSID.setObjectId("");
            String prefix = sampleSetLSID.toString();

            sampleSet.setMaterialLSIDPrefix(prefix);
            sampleSet.save(context.getUser());
        }
        return sampleSet;
    }

    public static final PipelineProvider.FileEntryFilter FILE_FILTER = new PipelineProvider.FileEntryFilter()
    {
        public boolean accept(File f)
        {
            // TODO:  If no corresponding mzXML file, show raw files.
            return MS2PipelineManager.isMzXMLFile(f);
        }
    };

    public PipelineProvider getPipelineProvider()
    {
        return new AssayPipelineProvider(MS2Module.class, FILE_FILTER, this, "Describe Samples");
    }

}
