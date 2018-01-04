/*
 * Copyright (c) 2007-2016 LabKey Corporation
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

package org.labkey.ms2.query;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.ms1.MS1Service;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.QueryException;
import org.labkey.api.query.UserSchema;
import org.labkey.api.services.ServiceRegistry;
import org.labkey.api.util.Pair;
import org.labkey.api.util.StringExpressionFactory;
import org.labkey.api.view.ActionURL;
import org.labkey.ms2.DeltaScanColumn;
import org.labkey.ms2.HydrophobicityColumn;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2RunType;
import org.labkey.ms2.peptideview.ProteinDisplayColumnFactory;
import org.labkey.ms2.protein.ProteinManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: jeckels
 * Date: Apr 6, 2007
 */
public class PeptidesTableInfo extends FilteredTable<MS2Schema>
{
    public static final String DUMMY_SCORE_COLUMN_NAME = "NullScore";

    private final MS2RunType[] _runTypes;

    public PeptidesTableInfo(MS2Schema schema)
    {
        this(schema, new ActionURL(MS2Controller.BeginAction.class, schema.getContainer()), true, ContainerFilter.CURRENT, MS2RunType.values(), false);
    }

    public PeptidesTableInfo(MS2Schema schema, boolean includeFeatureFk, ContainerFilter containerFilter, MS2RunType[] runTypes)
    {
        this(schema, new ActionURL(MS2Controller.BeginAction.class, schema.getContainer()), includeFeatureFk, containerFilter, runTypes, false);
    }

    public PeptidesTableInfo(MS2Schema schema, ActionURL url, boolean includeFeatureFk, ContainerFilter containerFilter, MS2RunType[] runTypes, boolean highestScore)
    {
        super(MS2Manager.getTableInfoPeptidesData(), schema);
        setName(MS2Schema.TableType.Peptides.name());
        if (runTypes != null && runTypes.length == 1)
        {
            setName(runTypes[0].getPeptideTableName());
        }
        _runTypes = runTypes;
        setContainerFilter(containerFilter);
        if (highestScore)
        {
            addHighestScoreFilter();
        }

        // Stick EndScan column just after Scan column
        ColumnInfo scanColumn = getRealTable().getColumn("Scan");
        ColumnInfo endScanColumn = getRealTable().getColumn("EndScan");
        List<ColumnInfo> columns = new ArrayList<>(getRealTable().getColumns());
        columns.remove(endScanColumn);
        int i = columns.indexOf(scanColumn);
        columns.add(i + 1, endScanColumn);

        for (ColumnInfo col : columns)
        {
            if (!col.getName().toLowerCase().startsWith("score"))
            {
                addWrapColumn(col);
            }
        }
        SqlDialect dialect = MS2Manager.getSqlDialect();

        addCalculatedColumns(this);

        addMassColumns(dialect);

        SQLFragment mzSQL = new SQLFragment("CASE WHEN " + ExprColumn.STR_TABLE_ALIAS + ".Charge = 0.0 THEN 0.0 ELSE (" + ExprColumn.STR_TABLE_ALIAS + ".Mass + " + ExprColumn.STR_TABLE_ALIAS + ".DeltaMass + (" + ExprColumn.STR_TABLE_ALIAS + ".Charge - 1.0) * 1.007276) / " + ExprColumn.STR_TABLE_ALIAS + ".Charge END");
        ColumnInfo mz = new ExprColumn(this, "MZ", mzSQL, JdbcType.REAL);
        mz.setFormat("0.0000");
        mz.setWidth("55");
        mz.setLabel("ObsMZ");
        addColumn(mz);

        SQLFragment strippedPeptideSQL = new SQLFragment("LTRIM(RTRIM(" + dialect.concatenate(ExprColumn.STR_TABLE_ALIAS + ".PrevAA", ExprColumn.STR_TABLE_ALIAS + ".TrimmedPeptide", ExprColumn.STR_TABLE_ALIAS + ".NextAA") + "))");
        ColumnInfo strippedPeptide = new ExprColumn(this, "StrippedPeptide", strippedPeptideSQL, JdbcType.VARCHAR);
        strippedPeptide.setWidth("320");
        addColumn(strippedPeptide);

        TableInfo info = getRealTable();

        ColumnInfo quantitation = wrapColumn("Quantitation", info.getColumn("RowId"));
        quantitation.setIsUnselectable(true);
        quantitation.setFk(new LookupForeignKey("PeptideId")
        {
            public TableInfo getLookupTableInfo()
            {
                FilteredTable result = new FilteredTable<>(MS2Manager.getTableInfoQuantitation(), getUserSchema());
                result.wrapAllColumns(true);
                result.getColumn("PeptideId").setHidden(true);
                result.getColumn("QuantId").setHidden(true);
                return result;
            }
        });
        quantitation.setKeyField(false);
        addColumn(quantitation);

        ColumnInfo iTraqQuantitation = wrapColumn("iTRAQQuantitation", info.getColumn("RowId"));
        iTraqQuantitation.setLabel("iTRAQ Quantitation");
        iTraqQuantitation.setIsUnselectable(true);
        iTraqQuantitation.setFk(new LookupForeignKey("PeptideId")
        {
            public TableInfo getLookupTableInfo()
            {
                FilteredTable result = new FilteredTable<>(MS2Manager.getTableInfoITraqPeptideQuantitation(), getUserSchema());
                result.wrapAllColumns(true);
                result.getColumn("PeptideId").setHidden(true);

                SQLFragment sumSQL = new SQLFragment(
                        "(COALESCE (" + ExprColumn.STR_TABLE_ALIAS + ".AbsoluteIntensity1, 0) + " +
                        "COALESCE (" + ExprColumn.STR_TABLE_ALIAS + ".AbsoluteIntensity2, 0) + " +
                        "COALESCE (" + ExprColumn.STR_TABLE_ALIAS + ".AbsoluteIntensity3, 0) + " +
                        "COALESCE (" + ExprColumn.STR_TABLE_ALIAS + ".AbsoluteIntensity4, 0) + " +
                        "COALESCE (" + ExprColumn.STR_TABLE_ALIAS + ".AbsoluteIntensity5, 0) + " +
                        "COALESCE (" + ExprColumn.STR_TABLE_ALIAS + ".AbsoluteIntensity6, 0) + " +
                        "COALESCE (" + ExprColumn.STR_TABLE_ALIAS + ".AbsoluteIntensity7, 0) + " +
                        "COALESCE (" + ExprColumn.STR_TABLE_ALIAS + ".AbsoluteIntensity8, 0) + " +
                        "COALESCE (" + ExprColumn.STR_TABLE_ALIAS + ".AbsoluteIntensity9, 0) + " +
                        "COALESCE (" + ExprColumn.STR_TABLE_ALIAS + ".AbsoluteIntensity10, 0))");
                for (int i = 1; i <= 10; i++)
                {
                    SQLFragment sql = new SQLFragment("CASE WHEN (");
                    sql.append(sumSQL);
                    sql.append(") = 0 THEN NULL ELSE ");
                    sql.append(ExprColumn.STR_TABLE_ALIAS);
                    sql.append(".AbsoluteIntensity");
                    sql.append(i);
                    sql.append(" / ");
                    sql.append(sumSQL);
                    sql.append(" END");
                    ExprColumn nonNormalizedRatio = new ExprColumn(result, "NonNormalized" + i, sql, JdbcType.REAL);
                    nonNormalizedRatio.setLabel("Non-normalized " + i);
                    nonNormalizedRatio.setFormat("0.00");
                    result.addColumn(nonNormalizedRatio);
                }

                return result;
            }
        });
        iTraqQuantitation.setKeyField(false);
        addColumn(iTraqQuantitation);

        ColumnInfo proteinGroup = wrapColumn("ProteinProphetData", info.getColumn("RowId"));
        proteinGroup.setIsUnselectable(true);
        proteinGroup.setFk(new LookupForeignKey("PeptideId")
        {
            public TableInfo getLookupTableInfo()
            {
                return _userSchema.createPeptideMembershipsTable();
            }
        });
        proteinGroup.setKeyField(false);
        addColumn(proteinGroup);

        ColumnInfo peptideProphetData = wrapColumn("PeptideProphetDetails", info.getColumn("RowId"));
        peptideProphetData.setIsUnselectable(true);
        peptideProphetData.setFk(new LookupForeignKey("PeptideId")
        {
            public TableInfo getLookupTableInfo()
            {
                FilteredTable table = new FilteredTable<>(MS2Manager.getTableInfoPeptideProphetData(), getUserSchema());
                table.wrapAllColumns(true);
                table.getColumn("PeptideId").setHidden(true);
                return table;
            }
        });
        peptideProphetData.setKeyField(false);
        addColumn(peptideProphetData);

        ActionURL showProteinURL = url.clone();
        showProteinURL.setAction(MS2Controller.ShowProteinAction.class);
        showProteinURL.deleteParameter("seqId");
        showProteinURL.deleteParameter("protein");
        final String showProteinURLString = showProteinURL.getLocalURIString() + "&seqId=${SeqId}&protein=${Protein}";

        setupProteinColumns(showProteinURLString);

        ActionURL showPeptideURL = url.clone();
        showPeptideURL.setAction(MS2Controller.ShowPeptideAction.class);
        showPeptideURL.deleteParameter("peptideId");
        String showPeptideURLString = showPeptideURL.getLocalURIString() + "&peptideId=${RowId}";
        DisplayColumnFactory factory = new DisplayColumnFactory()
        {
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                DataColumn dataColumn = new DataColumn(colInfo);
                dataColumn.setLinkTarget("peptide");
                return dataColumn;
            }
        };
        getColumn("Scan").setURL(StringExpressionFactory.createURL(showPeptideURLString));
        getColumn("Scan").setDisplayColumnFactory(factory);
        getColumn("Peptide").setURL(StringExpressionFactory.createURL(showPeptideURLString));
        getColumn("Peptide").setDisplayColumnFactory(factory);

        addScoreColumns();

        getColumn("Fraction").setFk(new LookupForeignKey("Fraction")
        {
            public TableInfo getLookupTableInfo()
            {
                return _userSchema.createFractionsTable();
            }
        });

        SQLFragment trypticSQL = new SQLFragment();
        trypticSQL.append("((CASE WHEN (");
        trypticSQL.append(strippedPeptideSQL);
        trypticSQL.append(" ").append(dialect.getCharClassLikeOperator()).append(" '[KR][^P]%' OR ");
        trypticSQL.append(strippedPeptideSQL);
        trypticSQL.append(" ").append(dialect.getCharClassLikeOperator()).append(" '-%') THEN 1 ELSE 0 END) + ");
        trypticSQL.append("(CASE WHEN (");
        trypticSQL.append(strippedPeptideSQL);
        trypticSQL.append(" ").append(dialect.getCharClassLikeOperator()).append(" '%[KR][^P]' OR ");
        trypticSQL.append(strippedPeptideSQL);
        trypticSQL.append(" ").append(dialect.getCharClassLikeOperator()).append(" '%-') THEN 1 ELSE 0 END))");
        ExprColumn trypricEndsColumn = new ExprColumn(this, "TrypticEnds", trypticSQL, JdbcType.INTEGER);
        addColumn(trypricEndsColumn);

        SQLFragment spectrumSQL = new SQLFragment();
        spectrumSQL.append("(SELECT Spectrum FROM ");
        spectrumSQL.append(MS2Manager.getTableInfoSpectraData(), "sd");
        spectrumSQL.append(" WHERE sd.Fraction = ");
        spectrumSQL.append(ExprColumn.STR_TABLE_ALIAS);
        spectrumSQL.append(".fraction AND sd.Scan = ");
        spectrumSQL.append(ExprColumn.STR_TABLE_ALIAS);
        spectrumSQL.append(".Scan)");
        ExprColumn spectrumColumn = new ExprColumn(this, "Spectrum", spectrumSQL, JdbcType.LONGVARBINARY);
        spectrumColumn.setHidden(true);
        addColumn(spectrumColumn);

        SQLFragment retentionTimeMinutesSQL = new SQLFragment("RetentionTime / 60.0");
        ExprColumn retentionTimeMinutesColumn = new ExprColumn(this, "RetentionTimeMinutes", retentionTimeMinutesSQL, JdbcType.DECIMAL);
        retentionTimeMinutesColumn.setFormat("#.##");
        retentionTimeMinutesColumn.setSortFieldKeys(Collections.singletonList(FieldKey.fromParts("RetentionTime")));
        addColumn(retentionTimeMinutesColumn);

        if (includeFeatureFk)
            addFeatureInfoColumn();
    }

    @Override
    public void setContainerFilter(@NotNull ContainerFilter filter)
    {
        super.setContainerFilter(filter);
        addRunFilter();
    }

    private void addRunFilter()
    {
        FieldKey fractionRunFieldKey = FieldKey.fromParts("Fraction", "Run");
        getFilter().deleteConditions(fractionRunFieldKey);

        SQLFragment sql = new SQLFragment();
        sql.append("Fraction IN (SELECT Fraction FROM ");
        sql.append(MS2Manager.getTableInfoFractions(), "f");
        sql.append(" WHERE Run IN (SELECT Run FROM ");
        sql.append(MS2Manager.getTableInfoRuns(), "r");
        sql.append(" WHERE Deleted = ? ");
        sql.add(Boolean.FALSE);

        Collection<MS2RunType> runTypes = getRunTypes();
        if (runTypes.size() != MS2RunType.values().length)
        {
            if (runTypes.isEmpty())
            {
                sql.append("AND 1 = 0");
            }
            else
            {
                sql.append("AND Type IN (");
                String separator = "";
                for (MS2RunType runType : runTypes)
                {
                    sql.append(separator);
                    separator = ", ";
                    sql.append("'");
                    sql.append(runType.toString());
                    sql.append("'");
                }
                sql.append(")");
            }
        }
        sql.append(" AND ");

        sql.append(getContainerFilter().getSQLFragment(getSchema(), new SQLFragment("Container"), _userSchema.getContainer(), true));
        if (_userSchema.getRuns() != null)
        {
            sql.append(" AND Run IN ");
            _userSchema.appendRunInClause(sql);
        }
        sql.append("))");
        addCondition(sql, fractionRunFieldKey);
    }

    private void addHighestScoreFilter()
    {
        List<MS2Run> runs = _userSchema.getRuns();

        if (runs != null)
        {
            // check to make sure runs are all the same type, in case future code breaks this assumption

            MS2RunType runType = null;
            for (MS2Run run : runs)
            {
                if(run.getRunType() == null)
                {
                    throw new IllegalStateException("Run type should never be null.");
                }
                else
                {
                    if(runType == null)
                    {
                        runType = run.getRunType();
                    }
                    else
                    {
                        if (!run.getRunType().equals(runType))
                            throw new IllegalStateException("Runs have different types when looking at a single run.");
                    }
                }
            }


            // now find index of charge column name for query
            MS2Run run = runs.get(0);  // all run types are the same, so just get the first one
            String chargeColumnName = run.getChargeFilterColumnName();
            int index = runType.getScoreColumnList().indexOf(FieldKey.fromParts(chargeColumnName));  // all run types are the same, so re-use runType
            String databaseScoreColumn = "Score" + String.valueOf(index + 1);  // db columns are 1-indexed

            SQLFragment sql = new SQLFragment();
            sql.append("RowId IN (SELECT RowId FROM \n");
            sql.append(" (SELECT RowId, row_number() OVER(PARTITION BY Peptide ORDER BY " + databaseScoreColumn + " DESC) as RowNum FROM ");
            sql.append(MS2Manager.getTableInfoPeptidesData(), "pep");
            sql.append(" JOIN ");
            sql.append(MS2Manager.getTableInfoFractions(), "fra");
            sql.append(" ON pep.Fraction = fra.Fraction WHERE fra.Run IN ");
            _userSchema.appendRunInClause(sql);
            sql.append(" )x\n");
            sql.append(" WHERE RowNum = 1)");

            FieldKey fractionRunFieldKey = FieldKey.fromParts("Fraction", "Run");
            addCondition(sql, fractionRunFieldKey);
        }
    }

    private void addFeatureInfoColumn()
    {
        //add an expression column that finds the corresponding ms1 feature id based on
        //the mzXmlUrl and MS2Scan (but not charge since, according to Roland, it may not always be correct)
        //Since we're not matching on charge, we could get multiple rows back, so use MIN to
        //select just the first matching one.
        SQLFragment sqlFeatureJoin = new SQLFragment("(SELECT MIN(fe.FeatureId) as FeatureId FROM ms1.Features AS fe\n" +
                "INNER JOIN ms1.Files AS fi ON (fe.FileId=fi.FileId)\n" +
                "INNER JOIN exp.Data AS d ON (fi.ExpDataFileId=d.RowId)\n" +
                "INNER JOIN ms2.Runs AS r ON (r.Container=d.Container)\n" +
                "INNER JOIN ms2.Fractions AS fr ON (fr.Run=r.Run AND fr.MzXmlUrl=fi.MzXmlUrl)\n" +
                "INNER JOIN ms2.PeptidesData AS pd ON (pd.Fraction=fr.Fraction AND pd.scan=fe.MS2Scan AND pd.Charge=fe.MS2Charge)\n" +
                "WHERE pd.RowId=" + ExprColumn.STR_TABLE_ALIAS + ".RowId)");

        ColumnInfo ciFeatureId = addColumn(new ExprColumn(this, "MS1 Feature", sqlFeatureJoin, JdbcType.INTEGER, getColumn("RowId")));

        //tell query that this new column is an FK to the features table info
        ciFeatureId.setFk(new LookupForeignKey("FeatureId")
        {
            public TableInfo getLookupTableInfo()
            {
                MS1Service ms1svc = ServiceRegistry.get().getService(MS1Service.class);
                return null == ms1svc ? null : ms1svc.createFeaturesTableInfo(_userSchema.getUser(), _userSchema.getContainer(), false);
            }
        });
    }

    private void setupProteinColumns(final String showProteinURLString)
    {
        LookupForeignKey fk = new LookupForeignKey("SeqId")
        {
            public TableInfo getLookupTableInfo()
            {
                SequencesTableInfo sequenceTable = new SequencesTableInfo(ProteinManager.getTableInfoSequences().getName(), _userSchema);
                SQLFragment fastaNameSQL = new SQLFragment(getName() + ".Protein");
                ExprColumn fastaNameColumn = new ExprColumn(sequenceTable, "Database Sequence Name", fastaNameSQL, JdbcType.VARCHAR);
                sequenceTable.addColumn(fastaNameColumn);

                fastaNameColumn.setDisplayColumnFactory(new ProteinDisplayColumnFactory(_userSchema.getContainer(), showProteinURLString));
                fastaNameColumn.setURL(StringExpressionFactory.createURL(showProteinURLString));

                sequenceTable.addPeptideAggregationColumns();

                return sequenceTable;
            }
        };
        fk.setPrefixColumnCaption(false);
        getColumn("SeqId").setFk(fk);

        getColumn("SeqId").setURL(StringExpressionFactory.createURL(showProteinURLString));
        getColumn("SeqId").setDisplayColumnFactory(new ProteinDisplayColumnFactory(_userSchema.getContainer()));
        getColumn("SeqId").setLabel("Search Engine Protein");
        getColumn("Protein").setURL(StringExpressionFactory.createURL(showProteinURLString));
        getColumn("Protein").setDisplayColumnFactory(new ProteinDisplayColumnFactory(_userSchema.getContainer()));
    }

    private void addScoreColumns()
    {
        Map<FieldKey, List<Pair<MS2RunType, Integer>>> columnMap = new HashMap<>();
        for (MS2RunType runType : getRunTypes())
        {
            int index = 1;
            // Since some search engines have the same names for different scores, build a list of all of the
            // possible interpretations for a given score name based on the run type
            for (FieldKey name : runType.getScoreColumnList())
            {
                List<Pair<MS2RunType, Integer>> l = columnMap.get(name);
                if (l == null)
                {
                    l = new ArrayList<>();
                    columnMap.put(name, l);
                }
                l.add(new Pair<>(runType, index++));
            }
        }

        ColumnInfo realScoreCol = MS2Manager.getTableInfoPeptidesData().getColumn("Score2");

        for (Map.Entry<FieldKey, List<Pair<MS2RunType, Integer>>> entry : columnMap.entrySet())
        {
            SQLFragment sql = new SQLFragment("CASE (SELECT r.Type FROM ");
            sql.append(MS2Manager.getTableInfoRuns(), "r");
            sql.append(", ");
            sql.append(MS2Manager.getTableInfoFractions(), "f");
            sql.append(" WHERE r.Run = f.Run AND f.Fraction = ");
            sql.append(ExprColumn.STR_TABLE_ALIAS);
            sql.append(".Fraction) ");
            for (Pair<MS2RunType, Integer> typeInfo : entry.getValue())
            {
                sql.append(" WHEN '");
                sql.append(typeInfo.getKey().toString());
                sql.append("' THEN ");
                sql.append(ExprColumn.STR_TABLE_ALIAS);
                sql.append(".score");
                sql.append(typeInfo.getValue());
            }
            sql.append(" ELSE NULL END");

            ColumnInfo newCol = addColumn(new ExprColumn(this, entry.getKey(), sql, JdbcType.DOUBLE));
            newCol.setFormat(realScoreCol.getFormat());
            newCol.setWidth(realScoreCol.getWidth());
        }
    }

    /**
     * Looks at the potential set of run types and further filters by including only those that are used
     * by the set of runs, if known 
     */
    private Collection<MS2RunType> getRunTypes()
    {
        // If we're already targeting a single kind of run, just use that
        if (_runTypes.length == 1)
        {
            return Collections.singleton(_runTypes[0]);
        }

        List<MS2Run> runs = _userSchema.getRuns();

        Collection<MS2RunType> runTypes = new HashSet<>(Arrays.asList(_runTypes));
        if (runs != null && runs.size() > 0)
        {
            Set<MS2RunType> usedRunTypes = new HashSet<>();
            for (MS2Run run : runs)
            {
                usedRunTypes.add(run.getRunType());
            }
            runTypes = CollectionUtils.intersection(runTypes, usedRunTypes);
        }
        return runTypes;
    }

    private void addMassColumns(SqlDialect dialect)
    {
        SQLFragment precursorMassSQL = new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".mass + " + ExprColumn.STR_TABLE_ALIAS + ".deltamass");
        ColumnInfo precursorMass = new ExprColumn(this, "PrecursorMass", precursorMassSQL, JdbcType.REAL);
        precursorMass.setFormat("0.0000");
        precursorMass.setWidth("65");
        precursorMass.setLabel("ObsMH+");
        addColumn(precursorMass);

        SQLFragment fractionalDeltaMassSQL = new SQLFragment("ABS(" + ExprColumn.STR_TABLE_ALIAS + ".deltamass - " + dialect.getRoundFunction(ExprColumn.STR_TABLE_ALIAS + ".deltamass") + ")");
        ColumnInfo fractionalDeltaMass = new ExprColumn(this, "FractionalDeltaMass", fractionalDeltaMassSQL, JdbcType.REAL);
        fractionalDeltaMass.setFormat("0.0000");
        fractionalDeltaMass.setWidth("55");
        fractionalDeltaMass.setLabel("fdMass");
        addColumn(fractionalDeltaMass);

        SQLFragment fractionalSQL = new SQLFragment("CASE\n" +
            "            WHEN " + ExprColumn.STR_TABLE_ALIAS + ".mass = 0.0 THEN 0.0\n" +
            "            ELSE abs(1000000.0 * abs(" + ExprColumn.STR_TABLE_ALIAS + ".deltamass - " + dialect.getRoundFunction(ExprColumn.STR_TABLE_ALIAS + ".deltamass") + ") / (" + ExprColumn.STR_TABLE_ALIAS + ".mass + ((" + ExprColumn.STR_TABLE_ALIAS + ".charge - 1.0) * 1.007276)))\n" +
            "        END");
        ColumnInfo fractionalDeltaMassPPM = new ExprColumn(this, "FractionalDeltaMassPPM", fractionalSQL, JdbcType.REAL);
        fractionalDeltaMassPPM.setFormat("0.0");
        fractionalDeltaMassPPM.setWidth("80");
        fractionalDeltaMassPPM.setLabel("fdMassPPM");
        addColumn(fractionalDeltaMassPPM);

        SQLFragment deltaSQL = new SQLFragment("CASE\n" +
            "            WHEN " + ExprColumn.STR_TABLE_ALIAS + ".mass = 0.0 THEN 0.0\n" +
            "            ELSE abs(1000000.0 * " + ExprColumn.STR_TABLE_ALIAS + ".deltamass / (" + ExprColumn.STR_TABLE_ALIAS + ".mass + ((" + ExprColumn.STR_TABLE_ALIAS + ".charge - 1.0) * 1.007276)))\n" +
            "        END");
        ColumnInfo deltaMassPPM = new ExprColumn(this, "DeltaMassPPM", deltaSQL, JdbcType.REAL);
        deltaMassPPM.setFormat("0.0");
        deltaMassPPM.setWidth("75");
        deltaMassPPM.setLabel("dMassPPM");
        addColumn(deltaMassPPM);
    }

    public List<FieldKey> getDefaultVisibleColumns()
    {
        if(null != _defaultVisibleColumns)
            return super.getDefaultVisibleColumns();

        List<FieldKey> result = new ArrayList<>();
        result.add(FieldKey.fromParts("Scan"));
        result.add(FieldKey.fromParts("Charge"));
        for (MS2RunType runType : getRunTypes())
        {
            Set<FieldKey> scoreCols = new HashSet<>();
            for (FieldKey name : runType.getScoreColumnList())
            {
                if (!DUMMY_SCORE_COLUMN_NAME.equalsIgnoreCase(name.getName()) && scoreCols.add(name))
                {
                    result.add(name);
                }
            }
        }
        result.add(FieldKey.fromParts("IonPercent"));
        result.add(FieldKey.fromParts("Mass"));
        result.add(FieldKey.fromParts("DeltaMass"));
        result.add(FieldKey.fromParts("PeptideProphet"));
        result.add(FieldKey.fromParts("Peptide"));
        result.add(FieldKey.fromParts("ProteinHits"));
        result.add(FieldKey.fromParts("Protein"));
        return Collections.unmodifiableList(result);
    }

    public String getPublicName()
    {
        return _runTypes.length > 1 ? MS2Schema.TableType.Peptides.toString() : _runTypes[0].getPeptideTableName();
    }

    public static void addCalculatedColumns(FilteredTable table)
    {
        ColumnInfo hColumn = table.wrapColumn("H", table.getRealTable().getColumn("Peptide"));
        hColumn.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new HydrophobicityColumn(colInfo);
            }
        });
        table.addColumn(hColumn);

        ColumnInfo deltaScanColumn = table.wrapColumn("DeltaScan", table.getRealTable().getColumn("Fraction"));
        deltaScanColumn.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new DeltaScanColumn(colInfo);
            }
        });
        deltaScanColumn.setFk(null);
        table.addColumn(deltaScanColumn);
    }

    @Override
    public void overlayMetadata(String tableName, UserSchema schema, Collection<QueryException> errors)
    {
        if (!MS2Schema.TableType.Peptides.toString().equalsIgnoreCase(tableName))
        {
            super.overlayMetadata(MS2Schema.TableType.Peptides.toString(), schema, errors);
        }

        super.overlayMetadata(tableName, schema, errors);
    }
}
