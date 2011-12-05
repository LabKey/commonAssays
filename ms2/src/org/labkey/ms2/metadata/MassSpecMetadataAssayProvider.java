/*
 * Copyright (c) 2007-2011 LabKey Corporation
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
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.IconDisplayColumn;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.exp.query.ExpDataTable;
import org.labkey.api.exp.query.ExpMaterialTable;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.security.User;
import org.labkey.api.settings.AppProps;
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AssayDataCollector;
import org.labkey.api.study.assay.AssayDataType;
import org.labkey.api.study.assay.AssayPipelineProvider;
import org.labkey.api.study.assay.AssayRunCreator;
import org.labkey.api.study.assay.AssayRunUploadContext;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.AssayTableMetadata;
import org.labkey.api.study.assay.AssayUrls;
import org.labkey.api.study.assay.ParticipantVisitResolverType;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Module;
import org.labkey.ms2.pipeline.AbstractMS2SearchProtocol;
import org.labkey.ms2.pipeline.MS2PipelineManager;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public AssayRunCreator getRunCreator()
    {
        return new MassSpecRunCreator(this);
    }

    @Override
    public ExpRunTable createRunTable(final AssaySchema schema, ExpProtocol protocol)
    {
        ExpRunTable result = super.createRunTable(schema, protocol);
        SQLFragment searchCountSQL = new SQLFragment();
        searchCountSQL.append(getSearchRunSQL(schema.getContainer(), result.getContainerFilter(), ExprColumn.STR_TABLE_ALIAS + ".RowId", "COUNT(DISTINCT(er.RowId))"));
        ExprColumn searchCountCol = new ExprColumn(result, SEARCH_COUNT_COLUMN, searchCountSQL, JdbcType.INTEGER);
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
        searchCountSQL.append(") AND ");
        searchCountSQL.append(containerFilter.getSQLFragment(ExperimentService.get().getSchema(), "er.Container", container));

        searchCountSQL.append(")");
        return searchCountSQL;
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

    public ExpData getDataForDataRow(Object dataRowId, ExpProtocol protocol)
    {
        return ExperimentService.get().getExpData(((Number)dataRowId).intValue());
    }

    public Domain getResultsDomain(ExpProtocol protocol)
    {
        return null;
    }
    
    public Domain getFractionDomain(ExpProtocol protocol)
    {
        return getDomainByPrefix(protocol, FRACTION_DOMAIN_PREFIX);
    }

    public ExpDataTable createDataTable(final AssaySchema schema, final ExpProtocol protocol, boolean includeCopiedToStudyColumns)
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
        Domain fractionDomain = getFractionDomain(protocol);
        if (fractionDomain != null)
        {
            for (DomainProperty fractionProperty : fractionDomain.getProperties())
            {
                cols.add(getDataFractionPropertyFieldKey(fractionProperty));
            }
        }
        for (DomainProperty runProperty : getRunDomain(protocol).getProperties())
        {
            cols.add(FieldKey.fromParts(ExpDataTable.Column.Run.toString(), runProperty.getName()));
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
                        MassSpecRunCreator.FRACTION_INPUT_ROLE,
                        ExpMaterialTable.Column.Property.toString(),
                        fractionProperty.getName());
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

    public ActionURL getImportURL(Container container, ExpProtocol protocol)
    {
        return PageFlowUtil.urlProvider(AssayUrls.class).getProtocolURL(container, protocol, MassSpecMetadataUploadAction.class);
    }

    @NotNull
    public static ExpSampleSet getFractionSampleSet(AssayRunUploadContext context) throws ExperimentException
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
