/*
 * Copyright (c) 2009-2012 LabKey Corporation
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
package org.labkey.luminex;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.ContainerForeignKey;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.OORDisplayColumnFactory;
import org.labkey.api.data.Parameter;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.StatementUtils;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.UpdateableTableInfo;
import org.labkey.api.etl.DataIteratorBuilder;
import org.labkey.api.etl.DataIteratorContext;
import org.labkey.api.etl.TableInsertDataIterator;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.query.ExpDataTable;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.query.AliasedColumn;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.SpecimenForeignKey;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * User: jeckels
 * Date: May 22, 2009
 */
public class LuminexDataTable extends FilteredTable implements UpdateableTableInfo
{
    private final LuminexProtocolSchema _schema;
    private LuminexAssayProvider _provider;
    public static final String FLAGGED_AS_EXCLUDED_COLUMN_NAME = "FlaggedAsExcluded";

    public LuminexDataTable(LuminexProtocolSchema schema)
    {
        super(LuminexProtocolSchema.getTableInfoDataRow(), schema.getContainer());
        final ExpProtocol protocol = schema.getProtocol();

        setName(AssaySchema.getResultsTableName(protocol, false));
        setPublicSchemaName(AssaySchema.NAME);

        _provider = (LuminexAssayProvider)AssayService.get().getProvider(protocol);

        setDescription("Contains all the Luminex data rows for the " + protocol.getName() + " assay definition");
        _schema = schema;

        ColumnInfo dataColumn = addColumn(wrapColumn("Data", getRealTable().getColumn("DataId")));
        dataColumn.setFk(new LookupForeignKey("RowId")
        {
            public TableInfo getLookupTableInfo()
            {
                ExpDataTable result = _schema.createDataTable();
                result.setContainerFilter(getContainerFilter());
                return result;
            }
        });
        ColumnInfo rowIdColumn = addColumn(wrapColumn(getRealTable().getColumn("RowId")));
        rowIdColumn.setHidden(true);
        rowIdColumn.setKeyField(true);
        addColumn(wrapColumn(getRealTable().getColumn("LSID"))).setHidden(true);
        ColumnInfo protocolColumn = addColumn(wrapColumn("Protocol", getRealTable().getColumn("ProtocolId")));
        protocolColumn.setFk(new ExpSchema(_schema.getUser(), _schema.getContainer()).getProtocolForeignKey("RowId"));
        protocolColumn.setHidden(true);
        addColumn(wrapColumn(getRealTable().getColumn("WellRole")));
        addColumn(wrapColumn(getRealTable().getColumn("Type")));
        addColumn(wrapColumn(getRealTable().getColumn("Well")));
        addColumn(wrapColumn(getRealTable().getColumn("Outlier")));
        addColumn(wrapColumn(getRealTable().getColumn("Description")));
        ColumnInfo specimenColumn = wrapColumn(getRealTable().getColumn("SpecimenID"));
        specimenColumn.setFk(new SpecimenForeignKey(_schema, AssayService.get().getProvider(_schema.getProtocol()), _schema.getProtocol()));
        addColumn(specimenColumn);
        addColumn(wrapColumn(getRealTable().getColumn("ExtraSpecimenInfo")));
        addColumn(wrapColumn(getRealTable().getColumn("FIString"))).setLabel("FI String");
        OORDisplayColumnFactory.addOORColumns(this, getRealTable().getColumn("FI"), getRealTable().getColumn("FIOORIndicator"));
        addColumn(wrapColumn(getRealTable().getColumn("FIBackgroundString"))).setLabel("FI-Bkgd String");
        OORDisplayColumnFactory.addOORColumns(this, getRealTable().getColumn("FIBackground"), getRealTable().getColumn("FIBackgroundOORIndicator"), "FI-Bkgd");
        addColumn(wrapColumn(getRealTable().getColumn("StdDevString")));
        OORDisplayColumnFactory.addOORColumns(this, getRealTable().getColumn("StdDev"), getRealTable().getColumn("StdDevOORIndicator"));
        addColumn(wrapColumn(getRealTable().getColumn("ObsConcString")));
        OORDisplayColumnFactory.addOORColumns(this, getRealTable().getColumn("ObsConc"), getRealTable().getColumn("ObsConcOORIndicator")).setLabel("Obs Conc BioPlex 5PL");
        addColumn(wrapColumn(getRealTable().getColumn("ExpConc")));
        addColumn(wrapColumn(getRealTable().getColumn("ObsOverExp"))).setLabel("(Obs/Exp)*100 BioPlex 5PL");
        OORDisplayColumnFactory.addOORColumns(this, getRealTable().getColumn("ConcInRange"), getRealTable().getColumn("ConcInRangeOORIndicator"));
        addColumn(wrapColumn(getRealTable().getColumn("ConcInRangeString")));
        addColumn(wrapColumn(getRealTable().getColumn("Dilution")));
        addColumn(wrapColumn("Group", getRealTable().getColumn("DataRowGroup")));
        addColumn(wrapColumn(getRealTable().getColumn("Ratio")));
        addColumn(wrapColumn(getRealTable().getColumn("SamplingErrors")));
        addColumn(wrapColumn(getRealTable().getColumn("BeadCount")));

        ColumnInfo cvCol = wrapColumn(getRealTable().getColumn("CV"));
        cvCol.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            @Override
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new QCFlagHighlightDisplayColumn(colInfo, "CVQCFlagsEnabled");
            }
        });
        addColumn(cvCol);
        SQLFragment cvQCFlagSQL = new SQLFragment("SELECT qf.Enabled FROM ");
        cvQCFlagSQL.append(ExperimentService.get().getTinfoAssayQCFlag(), "qf");
        cvQCFlagSQL.append(" WHERE " + ExprColumn.STR_TABLE_ALIAS + ".AnalyteId = qf.IntKey1");
        cvQCFlagSQL.append("   AND " + ExprColumn.STR_TABLE_ALIAS + ".DataId = qf.IntKey2");
        cvQCFlagSQL.append("   AND " + ExprColumn.STR_TABLE_ALIAS + ".Type = qf.Key1");
        cvQCFlagSQL.append("   AND " + ExprColumn.STR_TABLE_ALIAS + ".Description = qf.Key2");
        cvQCFlagSQL.append(" ORDER BY qf.RowId");
        ExprColumn cvFlagEnabledColumn = new ExprColumn(this, "CVQCFlagsEnabled", this.getSqlDialect().getSelectConcat(cvQCFlagSQL), JdbcType.VARCHAR);
        cvFlagEnabledColumn.setLabel("CV QC Flags Enabled State");
        cvFlagEnabledColumn.setHidden(true);
        addColumn(cvFlagEnabledColumn);

        addColumn(wrapColumn(getRealTable().getColumn("Summary")));
        ColumnInfo titrationColumn = addColumn(wrapColumn("Titration", getRealTable().getColumn("TitrationId")));
        titrationColumn.setFk(new LookupForeignKey("RowId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return _schema.createTitrationTable(true);
            }
        });

        ColumnInfo analyteTitrationColumn = wrapColumn("AnalyteTitration", getRealTable().getColumn("AnalyteId"));
        analyteTitrationColumn.setIsUnselectable(true);
        LookupForeignKey fk = new LookupForeignKey()
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return _schema.createAnalyteTitrationTable(false);
            }

            @Override
            protected ColumnInfo getPkColumn(TableInfo table)
            {
                // Pretend that analyte is the sole column in the PK for this table.
                // We'll get the other key of the compound key with addJoin() below.
                return table.getColumn("Analyte");
            }
        };
        fk.addJoin(FieldKey.fromParts("Titration"), "Titration", false);
        analyteTitrationColumn.setFk(fk);
        addColumn(analyteTitrationColumn);
        addColumn(wrapColumn("Analyte", getRealTable().getColumn("AnalyteId")));

        ColumnInfo containerColumn = addColumn(wrapColumn(getRealTable().getColumn("Container")));
        containerColumn.setHidden(true);
        containerColumn.setFk(new ContainerForeignKey(_schema));

        SQLFragment repGroupCaseStatement = new SQLFragment();
        repGroupCaseStatement.append("(CASE WHEN we.Comment IS NOT NULL THEN ");
        repGroupCaseStatement.append(getSqlDialect().concatenate(new SQLFragment("': '"), new SQLFragment("we.Comment")));
        repGroupCaseStatement.append(" ELSE '' END)");

        SQLFragment analyteCaseStatement = new SQLFragment();
        analyteCaseStatement.append("(CASE WHEN re.Comment IS NOT NULL THEN ");
        analyteCaseStatement.append(getSqlDialect().concatenate(new SQLFragment("': '"), new SQLFragment("re.Comment")));
        analyteCaseStatement.append(" ELSE '' END)");

        SQLFragment exclusionUnionSQL = new SQLFragment();
        exclusionUnionSQL.append("SELECT ");
        exclusionUnionSQL.append(getSqlDialect().concatenate(new SQLFragment("'Excluded for replicate group'"), repGroupCaseStatement));
        exclusionUnionSQL.append(" AS Comment, we.Modified, we.ModifiedBy, we.Created, we.CreatedBy FROM ");
        exclusionUnionSQL.append(LuminexProtocolSchema.getTableInfoWellExclusion(), "we");
        exclusionUnionSQL.append(", ");
        exclusionUnionSQL.append(LuminexProtocolSchema.getTableInfoWellExclusionAnalyte(), "wea");
        exclusionUnionSQL.append(" WHERE we.RowId = wea.WellExclusionId AND ");
        exclusionUnionSQL.append(" (we.Description = " + ExprColumn.STR_TABLE_ALIAS + ".Description OR (we.Description IS NULL AND " + ExprColumn.STR_TABLE_ALIAS + ".Description IS NULL)) AND ");
        exclusionUnionSQL.append("(we.Type = " + ExprColumn.STR_TABLE_ALIAS + ".Type OR (we.Type IS NULL AND " + ExprColumn.STR_TABLE_ALIAS + ".Type IS NULL)) AND ");
        exclusionUnionSQL.append("(we.DataId = " + ExprColumn.STR_TABLE_ALIAS + ".DataId OR (we.DataId IS NULL AND " + ExprColumn.STR_TABLE_ALIAS + ".DataId IS NULL)) AND ");
        exclusionUnionSQL.append("(wea.AnalyteId = " + ExprColumn.STR_TABLE_ALIAS + ".AnalyteId)");
        exclusionUnionSQL.append("UNION SELECT ");
        exclusionUnionSQL.append(getSqlDialect().concatenate(new SQLFragment("'Excluded for analyte'"), analyteCaseStatement));
        exclusionUnionSQL.append(" AS Comment, re.Modified, re.ModifiedBy, re.Created, re.CreatedBy FROM ");
        exclusionUnionSQL.append(LuminexProtocolSchema.getTableInfoRunExclusion(), "re");
        exclusionUnionSQL.append(", ");
        exclusionUnionSQL.append(LuminexProtocolSchema.getTableInfoRunExclusionAnalyte(), "rea");
        exclusionUnionSQL.append(", ");
        exclusionUnionSQL.append(ExperimentService.get().getTinfoData(), "d");
        exclusionUnionSQL.append(", ");
        exclusionUnionSQL.append(ExperimentService.get().getTinfoProtocolApplication(), "pa");
        exclusionUnionSQL.append(" WHERE re.RunId = rea.RunId AND re.RunId = pa.RunId AND pa.RowId = d.SourceApplicationId AND d.RowId = " + ExprColumn.STR_TABLE_ALIAS + ".DataId AND ");
        exclusionUnionSQL.append("(rea.AnalyteId = " + ExprColumn.STR_TABLE_ALIAS + ".AnalyteId)");

        SQLFragment excludedSQL = new SQLFragment("CASE WHEN (SELECT COUNT(*) FROM (");
        excludedSQL.append(exclusionUnionSQL);
        excludedSQL.append(") x) = 0 THEN ? ELSE ? END ");
        excludedSQL.add(Boolean.FALSE);
        excludedSQL.add(Boolean.TRUE);
        ExprColumn exclusionColumn = new ExprColumn(this, FLAGGED_AS_EXCLUDED_COLUMN_NAME, excludedSQL, JdbcType.BOOLEAN);
        exclusionColumn.setFormat("yes;no;");
        addColumn(exclusionColumn);

        SQLFragment exclusionCommentSQL = new SQLFragment("(SELECT MIN(Comment) FROM (");
        exclusionCommentSQL.append(exclusionUnionSQL);
        exclusionCommentSQL.append(") x)");
        ExprColumn exclusionReasonColumn = new ExprColumn(this, "ExclusionComment", exclusionCommentSQL, JdbcType.VARCHAR);
        addColumn(exclusionReasonColumn);

        AliasedColumn exclusionUIColumn = new AliasedColumn("ExclusionToggle", exclusionColumn);
        exclusionUIColumn.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            @Override
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new ExclusionUIDisplayColumn(colInfo, protocol.getName(), _schema.getContainer(), _schema.getUser());
            }
        });
        addColumn(exclusionUIColumn);

        List<FieldKey> defaultCols = new ArrayList<FieldKey>();
        defaultCols.add(exclusionUIColumn.getFieldKey());
        defaultCols.add(FieldKey.fromParts("Analyte"));
        defaultCols.add(FieldKey.fromParts("WellRole"));
        defaultCols.add(FieldKey.fromParts("Type"));
        defaultCols.add(FieldKey.fromParts("Well"));
        defaultCols.add(FieldKey.fromParts("Description"));
        defaultCols.add(exclusionColumn.getFieldKey());
        defaultCols.add(FieldKey.fromParts("SpecimenID"));
        defaultCols.add(FieldKey.fromParts("ParticipantID"));
        defaultCols.add(FieldKey.fromParts("VisitID"));
        defaultCols.add(FieldKey.fromParts("FI"));
        defaultCols.add(FieldKey.fromParts("FIBackground"));
        defaultCols.add(FieldKey.fromParts("StdDev"));
        defaultCols.add(FieldKey.fromParts("ObsConc"));
        defaultCols.add(FieldKey.fromParts("ExpConc"));
        defaultCols.add(FieldKey.fromParts("ObsOverExp"));
        defaultCols.add(FieldKey.fromParts("ConcInRange"));
        defaultCols.add(FieldKey.fromParts("Dilution"));
        defaultCols.add(FieldKey.fromParts("BeadCount"));
        defaultCols.add(FieldKey.fromParts("Titration"));

        Domain domain = getDomain();
        for (ColumnInfo propertyCol : domain.getColumns(this, getColumn("LSID"), schema.getContainer(), schema.getUser()))
        {
            addColumn(propertyCol);
            defaultCols.add(propertyCol.getFieldKey());
        }

        addColumn(wrapColumn("ParticipantID", getRealTable().getColumn("PTID")));
        addColumn(wrapColumn(getRealTable().getColumn("VisitID")));
        addColumn(wrapColumn(getRealTable().getColumn("Date")));


        for (DomainProperty prop : _provider.getRunDomain(protocol).getProperties())
        {
            defaultCols.add(new FieldKey(_provider.getTableMetadata(protocol).getRunFieldKeyFromResults(), prop.getName()));
        }
        for (DomainProperty prop : AbstractAssayProvider.getDomainByPrefix(protocol, LuminexAssayProvider.ASSAY_DOMAIN_EXCEL_RUN).getProperties())
        {
            defaultCols.add(new FieldKey(dataColumn.getFieldKey(), prop.getName()));
        }
        for (DomainProperty prop : _provider.getBatchDomain(protocol).getProperties())
        {
            defaultCols.add(new FieldKey(new FieldKey(_provider.getTableMetadata(protocol).getRunFieldKeyFromResults(), "Batch"), prop.getName()));
        }

        setDefaultVisibleColumns(defaultCols);

        getColumn("Analyte").setFk(new LuminexProtocolSchema.AnalyteForeignKey(_schema));

        SQLFragment protocolIDFilter = new SQLFragment("ProtocolID = ?");
        protocolIDFilter.add(_schema.getProtocol().getRowId());
        addCondition(protocolIDFilter, FieldKey.fromParts("ProtocolID"));

        SQLFragment containerFilter = new SQLFragment("Container = ?");
        containerFilter.add(_schema.getContainer().getId());
        addCondition(containerFilter, FieldKey.fromParts("Container"));
    }

    @Override
    @NotNull
    public Domain getDomain()
    {
        return _provider.getResultsDomain(_schema.getProtocol());
    }

    @Override
    public boolean insertSupported()
    {
        return true;
    }

    @Override
    public boolean updateSupported()
    {
        return false;
    }

    @Override
    public boolean deleteSupported()
    {
        return false;
    }

    @Override
    public TableInfo getSchemaTableInfo()
    {
        return LuminexProtocolSchema.getTableInfoDataRow();
    }

    @Override
    public ObjectUriType getObjectUriType()
    {
        return ObjectUriType.schemaColumn;
    }

    @Override
    public String getObjectURIColumnName()
    {
        return "LSID";
    }

    @Override
    public String getObjectIdColumnName()
    {
        return null;
    }

    @Override
    public CaseInsensitiveHashMap<String> remapSchemaColumns()
    {
        CaseInsensitiveHashMap<String> result = new CaseInsensitiveHashMap<String>();
        result.put("PTID", "ParticipantID");
        result.put("DataId", "Data");
        result.put("AnalyteId", "Analyte");
        result.put("ProtocolId", "Protocol");
        return result;
    }

    @Override
    public CaseInsensitiveHashSet skipProperties()
    {
        return new CaseInsensitiveHashSet();
    }

    @Override
    public DataIteratorBuilder persistRows(DataIteratorBuilder data, DataIteratorContext context)
    {
        return TableInsertDataIterator.create(data, this, null, context);
    }

    @Override
    public Parameter.ParameterMap insertStatement(Connection conn, User user) throws SQLException
    {
        return StatementUtils.insertStatement(conn, this, getContainer(), user, false, true);
    }

    @Override
    public Parameter.ParameterMap updateStatement(Connection conn, User user, Set<String> columns) throws SQLException
    {
        return StatementUtils.updateStatement(conn, this, getContainer(), user, false, true);
    }

    @Override
    public Parameter.ParameterMap deleteStatement(Connection conn) throws SQLException
    {
        throw new UnsupportedOperationException();
    }
}
