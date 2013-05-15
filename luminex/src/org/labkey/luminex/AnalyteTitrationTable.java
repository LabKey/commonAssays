/*
 * Copyright (c) 2011-2013 LabKey Corporation
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

import org.labkey.api.assay.dilution.DilutionCurve;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.ObjectProperty;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.AbstractBeanQueryUpdateService;
import org.labkey.api.query.BatchValidationException;
import org.labkey.api.query.DuplicateKeyException;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.InvalidKeyException;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.util.Pair;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * User: jeckels
 * Date: 7/8/11
 */
public class AnalyteTitrationTable extends AbstractCurveFitPivotTable
{
    public AnalyteTitrationTable(LuminexProtocolSchema schema, boolean filter)
    {
        super(LuminexProtocolSchema.getTableInfoAnalyteTitration(), schema, filter, "AnalyteId");
        setName(LuminexProtocolSchema.ANALYTE_TITRATION_TABLE_NAME);

        ColumnInfo analyteCol = addColumn(wrapColumn("Analyte", getRealTable().getColumn("AnalyteId")));
        analyteCol.setFk(new LookupForeignKey("RowId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return _userSchema.createAnalyteTable(false);
            }
        });
        setTitleColumn(analyteCol.getName());
        ColumnInfo titrationCol = addColumn(wrapColumn("Titration", getRealTable().getColumn("TitrationId")));
        LookupForeignKey titrationFk = new LookupForeignKey("RowId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return _userSchema.createTitrationTable(false);
            }
        };
        titrationFk.setPrefixColumnCaption(false);
        titrationCol.setFk(titrationFk);

        ColumnInfo maxFiCol = wrapColumn(getRealTable().getColumn("MaxFI"));
        maxFiCol.setLabel("High MFI");
        maxFiCol.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            @Override
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new QCFlagHighlightDisplayColumn(colInfo, "MaxFIQCFlagsEnabled");
            }
        });
        addColumn(maxFiCol);
        SQLFragment maxFiFlagEnabledSQL = createQCFlagEnabledSQLFragment(this.getSqlDialect(), LuminexDataHandler.QC_FLAG_HIGH_MFI_FLAG_TYPE, null);
        ExprColumn maxFiFlagEnabledColumn = new ExprColumn(this, "MaxFIQCFlagsEnabled", maxFiFlagEnabledSQL, JdbcType.VARCHAR);
        maxFiFlagEnabledColumn.setLabel("High MFI QC Flags Enabled State");
        maxFiFlagEnabledColumn.setHidden(true);
        addColumn(maxFiFlagEnabledColumn);

        ColumnInfo guideSetCol = addColumn(wrapColumn("GuideSet", getRealTable().getColumn("GuideSetId")));
        guideSetCol.setFk(new LookupForeignKey("RowId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return _userSchema.createGuideSetTable(false);
            }
        });

        addColumn(wrapColumn(getRealTable().getColumn("IncludeInGuideSetCalculation")));

        addCurveTypeColumns();

        // set the default columns for this table to be those used for the QC Report
        List<FieldKey> defaultCols = new ArrayList<FieldKey>();
        defaultCols.add(FieldKey.fromParts("Titration", "Run", "Name"));
        defaultCols.add(FieldKey.fromParts("Titration"));
        defaultCols.add(FieldKey.fromParts("Titration", "Standard"));
        defaultCols.add(FieldKey.fromParts("Titration", "QCControl"));
        defaultCols.add(FieldKey.fromParts("Titration", "Run", "Batch", "Network"));
        defaultCols.add(FieldKey.fromParts("Titration", "Run", "Batch", "CustomProtocol"));
        defaultCols.add(FieldKey.fromParts("Titration", "Run", "Folder"));
        defaultCols.add(FieldKey.fromParts("Titration", "Run", "NotebookNo"));
        defaultCols.add(FieldKey.fromParts("Titration", "Run", "AssayType"));
        defaultCols.add(FieldKey.fromParts("Titration", "Run", "ExpPerformer"));
        defaultCols.add(FieldKey.fromParts("Analyte", "Data", "AcquisitionDate"));
        defaultCols.add(FieldKey.fromParts("Analyte"));
        defaultCols.add(FieldKey.fromParts("Titration", "Run", "Isotype"));
        defaultCols.add(FieldKey.fromParts("Titration", "Run", "Conjugate"));
        defaultCols.add(FieldKey.fromParts("Analyte", "Properties", "LotNumber"));
        defaultCols.add(FieldKey.fromParts("GuideSet", "Created"));
        defaultCols.add(FieldKey.fromParts(DilutionCurve.FitType.FOUR_PARAMETER.getLabel() + "CurveFit", "EC50"));
        defaultCols.add(FieldKey.fromParts(DilutionCurve.FitType.FIVE_PARAMETER.getLabel() + "CurveFit", "EC50"));
        defaultCols.add(FieldKey.fromParts("MaxFI"));
        defaultCols.add(FieldKey.fromParts("TrapezoidalCurveFit", "AUC"));
        setDefaultVisibleColumns(defaultCols);
    }

    protected LookupForeignKey createCurveFitFK(final String curveType)
    {
        LookupForeignKey fk = new LookupForeignKey("AnalyteId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                CurveFitTable result = _userSchema.createCurveFitTable(false);
                result.addCondition(result.getRealTable().getColumn("CurveType"), curveType);
                return result;
            }
        };
        fk.addJoin(FieldKey.fromParts("Titration"), "TitrationId", false);
        return fk;
    }

    @Override
    protected SQLFragment createContainerFilterSQL(ContainerFilter filter, Container container)
    {
        SQLFragment sql = new SQLFragment("AnalyteId IN (SELECT RowId FROM ");
        sql.append(LuminexProtocolSchema.getTableInfoAnalytes(), "a");
        sql.append(" WHERE DataId IN (SELECT RowId FROM ");
        sql.append(ExperimentService.get().getTinfoData(), "d");
        sql.append(" WHERE ");
        sql.append(filter.getSQLFragment(getSchema(), "Container", container));
        sql.append("))");
        return sql;
    }

    @Override
    public boolean hasPermission(UserPrincipal user, Class<? extends Permission> perm)
    {
        return (perm.equals(UpdatePermission.class) || perm.equals(ReadPermission.class))
                && _userSchema.getContainer().hasPermission(user, perm);
    }

    @Override
    public QueryUpdateService getUpdateService()
    {
        // Pair<Integer, Integer> is analyteid/titrationid combo
        return new AbstractBeanQueryUpdateService<AnalyteTitration, Pair<Integer, Integer>>(this)
        {
            @Override
            protected Pair<Integer, Integer> keyFromMap(Map<String, Object> map) throws InvalidKeyException
            {
                Integer analyteId = getInteger(map, map.containsKey("analyte") ? "analyte" : "analyteid");
                Integer titrationId = getInteger(map, map.containsKey("titration") ? "titration" : "titrationid");
                return new Pair<Integer, Integer>(analyteId, titrationId);
            }

            @Override
            protected AnalyteTitration get(User user, Container container, Pair<Integer, Integer> key) throws QueryUpdateServiceException
            {
                SimpleFilter filter = new SimpleFilter("AnalyteId", key.getKey());
                filter.addCondition("TitrationId", key.getValue());
                return new TableSelector(LuminexProtocolSchema.getTableInfoAnalyteTitration(), filter, null).getObject(AnalyteTitration.class);
            }

            protected Analyte getAnalyte(int rowId)
            {
                Analyte analyte = new TableSelector(LuminexProtocolSchema.getTableInfoAnalytes()).getObject(rowId, Analyte.class);
                if (analyte == null)
                {
                    throw new IllegalStateException("Unable to find referenced analyte: " + rowId);
                }
                return analyte;
            }

            protected Titration getTitration(int rowId)
            {
                Titration titration = new TableSelector(LuminexProtocolSchema.getTableInfoTitration()).getObject(rowId, Titration.class);
                if (titration == null)
                {
                    throw new IllegalStateException("Unable to find referenced titration: " + rowId);
                }
                return titration;
            }

            protected ExpRun getRun(int rowId)
            {
                ExpRun run = ExperimentService.get().getExpRun(rowId);
                if (run == null)
                {
                    throw new IllegalStateException("Unable to find referenced run: " + rowId);
                }
                return run;
            }

            protected Map<String, String> getIsotypeAndConjugate(ExpRun run)
            {
                Map<String, String> isotypeConjugate = new HashMap<String, String>();
                isotypeConjugate.put("Isotype", null);
                isotypeConjugate.put("Conjugate", null);
                Map<String, ObjectProperty> runProps = run.getObjectProperties();
                for (ObjectProperty property : runProps.values())
                {
                    if (property.getName().equalsIgnoreCase("Isotype"))
                    {
                        isotypeConjugate.put("Isotype", property.getStringValue());
                    }
                    if (property.getName().equalsIgnoreCase("Conjugate"))
                    {
                        isotypeConjugate.put("Conjugate", property.getStringValue());
                    }
                }
                return isotypeConjugate;
            }

            protected void updateAnalyteTitrationQCFlags(User user, AnalyteTitration bean) throws SQLException
            {
                // get the run, isotype, conjugate, and analtye/titration curvefit information in order to update QC Flags
                Analyte analyte = getAnalyte(bean.getAnalyteId());
                Titration titration = getTitration(bean.getTitrationId());
                ExpRun run = getRun(titration.getRunId());
                Map<String, String> runIsotypeConjugate = getIsotypeAndConjugate(run);

                SimpleFilter curveFitFilter = new SimpleFilter("AnalyteId", bean.getAnalyteId());
                curveFitFilter.addCondition("TitrationId", bean.getTitrationId());
                CurveFit[] curveFits = Table.select(LuminexProtocolSchema.getTableInfoCurveFit(), Table.ALL_COLUMNS, curveFitFilter, null, CurveFit.class);
                
                LuminexDataHandler.insertOrUpdateAnalyteTitrationQCFlags(user, run, _userSchema.getProtocol(), bean, analyte, titration, runIsotypeConjugate.get("Isotype"), runIsotypeConjugate.get("Conjugate"), Arrays.asList(curveFits));
            }

            @Override
            public List<Map<String, Object>> updateRows(User user, Container container, List<Map<String, Object>> rows, List<Map<String, Object>> oldKeys, Map<String, Object> extraScriptContext) throws InvalidKeyException, BatchValidationException, QueryUpdateServiceException, SQLException
            {
                List<Map<String, Object>> results = super.updateRows(user, container, rows, oldKeys, extraScriptContext);

                // If any of the updated rows includes a change to the guide set calculation (i.e. has IncludeInGuideSetCalculation as an updated value)
                // then we need to update the AnalyteTitration QC Flags for all AnalyteTitrations that are associated with the given guide set(s)
                Set<Integer> guideSetIds = new HashSet<Integer>();
                Set<AnalyteTitration> analyteTitrationsForUpdate = new HashSet<AnalyteTitration>();
                for (Map<String, Object> row : rows)
                {
                    // add the current row to the set of AnalyteTitrations that need to have their QC Flags updated
                    AnalyteTitration analyteTitration = get(user, container, keyFromMap(row));
                    analyteTitrationsForUpdate.add(analyteTitration);
                    if (row.containsKey("IncludeInGuideSetCalculation"))
                    {
                        guideSetIds.add(analyteTitration.getGuideSetId());
                    }
                }

                // Add all AnalyteTitrations to the update set for the guide sets that have changed
                for (Integer guideSetId : guideSetIds)
                {
                    SimpleFilter guideSetFilter = new SimpleFilter("GuideSetId", guideSetId);
                    AnalyteTitration[] guideSetAnalyteTitrations = Table.select(LuminexProtocolSchema.getTableInfoAnalyteTitration(), Table.ALL_COLUMNS, guideSetFilter, null, AnalyteTitration.class);
                    analyteTitrationsForUpdate.addAll(Arrays.asList(guideSetAnalyteTitrations));
                }

                for (AnalyteTitration analyteTitration : analyteTitrationsForUpdate)
                {
                    updateAnalyteTitrationQCFlags(user, analyteTitration);
                }

                return results;
            }

            @Override
            protected AnalyteTitration update(User user, Container container, AnalyteTitration bean, Pair<Integer, Integer> oldKey) throws ValidationException, QueryUpdateServiceException, SQLException
            {
                Integer newGuideSetId = bean.getGuideSetId();

                if (newGuideSetId != null)
                {
                    GuideSet guideSet = new TableSelector(LuminexProtocolSchema.getTableInfoGuideSet()).getObject(newGuideSetId.intValue(), GuideSet.class);
                    if (guideSet == null)
                    {
                        throw new ValidationException("No such guideSetId: " + newGuideSetId);
                    }
                    if (guideSet.getProtocolId() != _userSchema.getProtocol().getRowId())
                    {
                        throw new ValidationException("Can't set guideSetId to point to a guide set from another assay definition: " + newGuideSetId);
                    }

                    Analyte analyte = getAnalyte(bean.getAnalyteId());
                    Titration titration = getTitration(bean.getTitrationId());
                    if (!Objects.equals(analyte.getName(), guideSet.getAnalyteName()))
                    {
                        throw new ValidationException("GuideSet is for analyte " + guideSet.getAnalyteName(), " but this row is mapped to analyte " + analyte.getName());
                    }
                    if (!Objects.equals(titration.getName(), guideSet.getTitrationName()))
                    {
                        throw new ValidationException("GuideSet is for titration " + guideSet.getTitrationName(), " but this row is mapped to titration " + titration.getName());
                    }
                }

                Object[] keys = new Object[2];

                boolean analyteFirst = LuminexProtocolSchema.getTableInfoAnalyteTitration().getPkColumnNames().get(0).equalsIgnoreCase("AnalyteId");
                keys[0] = analyteFirst ? oldKey.getKey() : oldKey.getValue();
                keys[1] = analyteFirst ? oldKey.getValue() : oldKey.getKey();

                return Table.update(user, LuminexProtocolSchema.getTableInfoAnalyteTitration(), bean, keys);
            }

            @Override
            protected void delete(User user, Container container, Pair<Integer, Integer> key) throws QueryUpdateServiceException, SQLException
            {
                throw new UnsupportedOperationException();
            }

            @Override
            protected AnalyteTitration createNewBean()
            {
                return new AnalyteTitration();
            }

            @Override
            protected AnalyteTitration insert(User user, Container container, AnalyteTitration bean) throws ValidationException, DuplicateKeyException, QueryUpdateServiceException, SQLException
            {
                throw new UnsupportedOperationException();
            }
        };
    }
}
