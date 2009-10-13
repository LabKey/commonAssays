/*
 * Copyright (c) 2009 LabKey Corporation
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

package org.labkey.viability;

import org.labkey.api.data.*;
import org.labkey.api.query.*;
import org.labkey.api.study.assay.*;
import org.labkey.api.security.User;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.exp.query.ExpDataTable;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.PropertyDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.sql.Types;

public class ViabilityAssaySchema extends AssaySchema
{
    public enum UserTables
    {
        Results, ResultSpecimens
    }

    private final ExpProtocol _protocol;

    public ViabilityAssaySchema(User user, Container container, ExpProtocol protocol)
    {
        super(AssaySchema.NAME, user, container, getSchema());
        assert protocol != null;
        _protocol = protocol;
    }

    public static DbSchema getSchema()
    {
        return ViabilitySchema.getSchema();
    }

    public SqlDialect getSqlDialect()
    {
        return getSchema().getSqlDialect();
    }

    public Set<String> getTableNames()
    {
        return Collections.singleton(prefixTableName(UserTables.ResultSpecimens));
    }

    private String prefixTableName(UserTables table)
    {
        return _protocol.getName() + " " + table.name();
    }

    public TableInfo createTable(String name)
    {
        assert name.startsWith(_protocol.getName() + " ");
        name = name.substring((_protocol.getName() + " ").length());
//        if (name.equals(UserTables.Results.name()))
//            return createResultsTable();
        if (name.equals(UserTables.ResultSpecimens.name()))
            return createResultSpecimensTable();
        return null;
    }

    public AbstractTableInfo createResultsTable()
    {
        return new RecoveryResultsTable();
    }

    public ResultSpecimensTable createResultSpecimensTable()
    {
        return new ResultSpecimensTable(null);
    }

    public ExpDataTable createDataTable()
    {
        ExpDataTable ret = ExperimentService.get().createDataTable(ExpSchema.TableType.Datas.toString(), this);
        ret.addColumn(ExpDataTable.Column.RowId);
        ret.addColumn(ExpDataTable.Column.Name);
        ret.addColumn(ExpDataTable.Column.Flag);
        ret.addColumn(ExpDataTable.Column.Created);
        ret.setTitleColumn("Name");
        ColumnInfo protocol = ret.addColumn(ExpDataTable.Column.Protocol);
        protocol.setHidden(true);
        return ret;
    }

    class ViabilityAssayTable extends FilteredTable
    {
        ViabilityAssayProvider _provider;

        protected ViabilityAssayTable(TableInfo table)
        {
            super(table, ViabilityAssaySchema.this.getContainer());
            _provider = ViabilityManager.get().getProvider();
            _defaultVisibleColumns = new ArrayList<FieldKey>();
        }

        protected ColumnInfo addVisible(ColumnInfo col)
        {
            ColumnInfo ret = addColumn(col);
            addDefaultVisible(col.getName());
            return ret;
        }

        protected void addDefaultVisible(String... key)
        {
            addDefaultVisible(FieldKey.fromParts(key));
        }

        protected void addDefaultVisible(FieldKey fieldKey)
        {
            ((List<FieldKey>)_defaultVisibleColumns).add(fieldKey);
        }

    }

    protected static ColumnInfo copyProperties(ColumnInfo column, DomainProperty dp)
    {
        if (dp != null)
        {
            PropertyDescriptor pd = dp.getPropertyDescriptor();
            column.setLabel(pd.getLabel() == null ? column.getName() : pd.getLabel());
            column.setDescription(pd.getDescription());
            column.setFormat(pd.getFormat());
            column.setDisplayWidth(pd.getDisplayWidth());
            column.setHidden(pd.isHidden());
            column.setNullable(!pd.isRequired());
        }
        return column;
    }

    public class RecoveryResultsTable extends FilteredTable
    {

        public RecoveryResultsTable()
        {
            super(new ResultsTable());
            ResultsTable resultsTable = (ResultsTable)getRealTable();
            wrapAllColumns(true);

            ExprColumn recoveryCol = new ExprColumn(this, "Recovery",
                    new SQLFragment(
                            "(CASE WHEN OriginalCells IS NULL OR OriginalCells = 0 THEN NULL " +
                            "ELSE ViableCells / OriginalCells END)"), Types.DOUBLE);
            copyProperties(recoveryCol, resultsTable._resultsDomain.getPropertyByName(ViabilityAssayProvider.RECOVERY_PROPERTY_NAME));
            addColumn(recoveryCol);
        }

        @NotNull
        public SQLFragment getFromSQL()
        {
            // XXX: shouldn't have to call getSelectSQL() myself here
            SQLFragment fromSQL = QueryService.get().getSelectSQL(getFromTable(), getFromTable().getColumns(), getFilter(), null, 0, 0);
            return fromSQL;
        }
    }

    public class ResultsTable extends ViabilityAssayTable
    {
        protected Domain _resultsDomain;

        public ResultsTable()
        {
            super(ViabilitySchema.getTableInfoResults());

            _resultsDomain = _provider.getResultsDomain(_protocol);
            DomainProperty[] resultDomainProperties = _resultsDomain.getProperties();
            Map<String, DomainProperty> propertyMap = new LinkedHashMap<String, DomainProperty>(resultDomainProperties.length);
            for (DomainProperty property : resultDomainProperties)
                propertyMap.put(property.getName(), property);

            ColumnInfo rowId = addColumn(wrapColumn(getRealTable().getColumn("RowId")));
            rowId.setHidden(true);
            rowId.setKeyField(true);

            ColumnInfo dataColumn = addColumn(wrapColumn("Data", getRealTable().getColumn("DataId")));
            dataColumn.setHidden(true);
            dataColumn.setFk(new LookupForeignKey("RowId")
            {
                public TableInfo getLookupTableInfo()
                {
                    return ViabilityAssaySchema.this.createDataTable();
                }
            });

            SQLFragment runSql = new SQLFragment("(SELECT d.RunID FROM exp.data d WHERE d.RowID = " + ExprColumn.STR_TABLE_ALIAS + ".DataID)");
            ExprColumn runColumn = new ExprColumn(this, "Run", runSql, Types.INTEGER);
            runColumn.setFk(new LookupForeignKey("RowID")
            {
                public TableInfo getLookupTableInfo()
                {
                    ExpRunTable expRunTable = AssayService.get().createRunTable(_protocol, _provider, ViabilityAssaySchema.this.getUser(), ViabilityAssaySchema.this.getContainer());
                    expRunTable.setContainerFilter(getContainerFilter());
                    return expRunTable;
                }
            });
            addColumn(runColumn);

            addVisible(copyProperties(wrapColumn(getRealTable().getColumn("ParticipantID")), propertyMap.get(ViabilityAssayProvider.PARTICIPANTID_PROPERTY_NAME)));
            addVisible(copyProperties(wrapColumn(getRealTable().getColumn("VisitID")), propertyMap.get(ViabilityAssayProvider.VISITID_PROPERTY_NAME)));
            addVisible(copyProperties(wrapColumn(getRealTable().getColumn("Date")), propertyMap.get(ViabilityAssayProvider.DATE_PROPERTY_NAME)));

            addColumn(copyProperties(wrapColumn(getRealTable().getColumn("SampleNum")), propertyMap.get(ViabilityAssayProvider.SAMPLE_NUM_PROPERTY_NAME)));
            addVisible(copyProperties(wrapColumn(getRealTable().getColumn("PoolID")), propertyMap.get(ViabilityAssayProvider.POOL_ID_PROPERTY_NAME)));
            addVisible(copyProperties(wrapColumn(getRealTable().getColumn("TotalCells")), propertyMap.get(ViabilityAssayProvider.TOTAL_CELLS_PROPERTY_NAME)));
            addVisible(copyProperties(wrapColumn(getRealTable().getColumn("ViableCells")), propertyMap.get(ViabilityAssayProvider.VIABLE_CELLS_PROPERTY_NAME)));

            ExprColumn viabilityCol = new ExprColumn(this, "Viability", new SQLFragment("(CASE WHEN " + ExprColumn.STR_TABLE_ALIAS + ".TotalCells > 0 THEN CAST(" + ExprColumn.STR_TABLE_ALIAS + ".ViableCells AS FLOAT) / " + ExprColumn.STR_TABLE_ALIAS + ".TotalCells ELSE 0.0 END)"), Types.DOUBLE);
            copyProperties(viabilityCol, propertyMap.get(ViabilityAssayProvider.VIABILITY_PROPERTY_NAME));
            addVisible(viabilityCol);

            ExprColumn originalCells = new ExprColumn(this, "OriginalCells", new SQLFragment("VolumeSum"), Types.DOUBLE);
            copyProperties(viabilityCol, propertyMap.get(ViabilityAssayProvider.ORIGINAL_CELLS_PROPERTY_NAME));
            addVisible(originalCells);

            SQLFragment specimenIDs = new SQLFragment();
            if (getDbSchema().getSqlDialect().isSqlServer())
            {
                specimenIDs.append("(REPLACE(");
                specimenIDs.append("(SELECT rs.SpecimenID AS [data()]");
                specimenIDs.append(" FROM " + ViabilitySchema.getTableInfoResultSpecimens() + " rs");
                specimenIDs.append(" WHERE rs.ResultID = " + ExprColumn.STR_TABLE_ALIAS + ".RowID");
                specimenIDs.append(" ORDER BY rs.SpecimenIndex");
                specimenIDs.append(" FOR XML PATH ('')),' ', ','))");
            }
            else if (getDbSchema().getSqlDialect().isPostgreSQL())
            {
                specimenIDs.append("(SELECT array_to_string(viability.array_accum(rs1.SpecimenID), ',')");
                specimenIDs.append("  FROM (SELECT rs2.SpecimenID, rs2.ResultID FROM viability.ResultSpecimens rs2");
                specimenIDs.append("    WHERE rs2.ResultID = " + ExprColumn.STR_TABLE_ALIAS + ".RowID");
                specimenIDs.append("    ORDER BY rs2.SpecimenIndex) AS rs1");
                specimenIDs.append("  GROUP BY rs1.ResultID");
                specimenIDs.append(")");
            }
            else
            {
                throw new UnsupportedOperationException("SqlDialect not supported: " + getDbSchema().getSqlDialect().getClass().getSimpleName());
            }
            ExprColumn specimenIDsCol = new ExprColumn(this, "SpecimenIDs", specimenIDs, java.sql.Types.VARCHAR);
            copyProperties(specimenIDsCol, propertyMap.get(ViabilityAssayProvider.SPECIMENIDS_PROPERTY_NAME));
            addVisible(specimenIDsCol);

            ExprColumn specimenIDCount = new ExprColumn(this, "SpecimenIDCount",
                    new SQLFragment("(SELECT COUNT(RS.specimenid) FROM viability.resultspecimens RS WHERE " + ExprColumn.STR_TABLE_ALIAS + ".RowID = RS.ResultID)"), Types.INTEGER);
            addVisible(specimenIDCount);

            addResultDomainPropertiesColumn();

            addProtocolContainerFilter();
        }

        private void addResultDomainPropertiesColumn()
        {
            ColumnInfo colProperty = wrapColumn("Properties", _rootTable.getColumn("ObjectId"));
            colProperty.setIsUnselectable(true);

            DomainProperty[] userResultDPs = ViabilityAssayProvider.getResultDomainUserProperties(_resultsDomain);
            QcAwarePropertyForeignKey fk = new QcAwarePropertyForeignKey(userResultDPs, this, ViabilityAssaySchema.this);

            Set<String> hiddenCols = new HashSet<String>();
            for (PropertyDescriptor pd : fk.getDefaultHiddenProperties())
                hiddenCols.add(pd.getName());

            FieldKey dataKeyProp = new FieldKey(null, colProperty.getName());
            for (DomainProperty lookupCol : userResultDPs)
            {
                if (!lookupCol.isHidden() && !hiddenCols.contains(lookupCol.getName()))
                    addDefaultVisible(new FieldKey(dataKeyProp, lookupCol.getName()));
            }

            colProperty.setFk(fk);
            addColumn(colProperty);
        }

        protected void addProtocolContainerFilter()
        {
            SQLFragment filter = new SQLFragment(
                    "DataID IN (" +
                            "SELECT d.RowId FROM " + ExperimentService.get().getTinfoData() + " d, " + ExperimentService.get().getTinfoExperimentRun() + " r " +
                            "WHERE r.RowID = d.RunID " +
                            "AND d.Container = ? " + // XXX: or is r.Container better?
                            "AND r.ProtocolLSID = ? " +
                            ")");
            filter.add(ViabilityAssaySchema.this.getContainer().getId());
            filter.add(_protocol.getLSID());
            addCondition(filter);
        }

        private boolean _addedResultSpecimens = false;

        @NotNull
        @Override
        public SQLFragment getFromSQL()
        {
            if (!_addedResultSpecimens)
            {
                _addedResultSpecimens = true;
                
                ResultSpecimensTable rs = new ResultSpecimensTable(this);
                SimpleFilter filter = new SimpleFilter();
                filter.addCondition("SpecimenID/Specimen/VolumeUnits", "CEL");
                List<FieldKey> fields = new ArrayList<FieldKey>();
                FieldKey resultId, volume;
                fields.add(resultId = FieldKey.fromParts("ResultID"));
                fields.add(FieldKey.fromParts("SpecimenID"));
                fields.add(volume = FieldKey.fromParts("SpecimenID", "Volume"));
                fields.add(FieldKey.fromParts("SpecimenID", "Specimen", "VolumeUnits"));
                fields.add(FieldKey.fromParts("ResultID", "Run", "Batch", "BatchProperties", "TargetStudy"));
                Map<FieldKey, ColumnInfo> columnMap = QueryService.get().getColumns(rs, fields);

                SQLFragment sub = QueryService.get().getSelectSQL(rs, columnMap.values(), filter, null, 0, 0);

                SQLFragment fromSQL = new SQLFragment("SELECT * FROM (\n");
                fromSQL.append(super.getFromSQL());
                fromSQL.append("\n) AS x");
                fromSQL.append("\nLEFT OUTER JOIN (\n");

                fromSQL.append("SELECT " + columnMap.get(resultId).getAlias() + " as VolumeResultID, SUM(" + columnMap.get(volume).getAlias() + ") as VolumeSum FROM (\n");
                fromSQL.append(sub);
                fromSQL.append(") y \nGROUP BY " + columnMap.get(resultId).getAlias());
                fromSQL.append("\n) AS z");
                fromSQL.append(" ON x.RowId = z.VolumeResultId");

                return fromSQL;
            }
            else
            {
                return super.getFromSQL();
            }
        }
    }

    public class ResultSpecimensTable extends ViabilityAssayTable
    {
        public ResultSpecimensTable(final ResultsTable results)
        {
            super(ViabilitySchema.getTableInfoResultSpecimens());

            ColumnInfo resultIDCol = addVisible(wrapColumn(getRealTable().getColumn("ResultID")));
            resultIDCol.setLabel("Result");
            resultIDCol.setKeyField(true);
            resultIDCol.setFk(new LookupForeignKey("RowID")
            {
                public TableInfo getLookupTableInfo()
                {
                    if (results != null)
                        return results;
                    return new RecoveryResultsTable();
                }
            });

            ColumnInfo specimenID = addVisible(wrapColumn(getRealTable().getColumn("SpecimenID")));
            specimenID.setLabel("Specimen");
            specimenID.setKeyField(true);
            SpecimenForeignKey fk = new SpecimenForeignKey(ViabilityAssaySchema.this, _provider, _protocol, new ViabilityAssayProvider.ResultsSpecimensAssayTableMetadata());
            specimenID.setFk(fk);

            ColumnInfo indexCol = addVisible(wrapColumn(getRealTable().getColumn("SpecimenIndex")));
            indexCol.setKeyField(true);
        }
    }
}
