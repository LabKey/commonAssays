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

package org.labkey.ms2.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.*;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.ms1.MS1Service;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.api.view.ActionURL;
import org.labkey.api.services.ServiceRegistry;
import org.labkey.api.util.Pair;
import org.labkey.api.util.StringExpressionFactory;
import org.labkey.ms2.*;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.peptideview.ProteinDisplayColumnFactory;

import java.sql.Types;
import java.util.*;

/**
 * User: jeckels
 * Date: Apr 6, 2007
 */
public class PeptidesTableInfo extends FilteredTable
{
    private MS2Schema _schema;

    public PeptidesTableInfo(MS2Schema schema)
    {
        this(schema, new ActionURL(MS2Controller.BeginAction.class, schema.getContainer()), true, ContainerFilter.CURRENT);
    }

    public PeptidesTableInfo(MS2Schema schema, boolean includeFeatureFk, ContainerFilter containerFilter)
    {
        this(schema, new ActionURL(MS2Controller.BeginAction.class, schema.getContainer()), includeFeatureFk, containerFilter);
    }

    public PeptidesTableInfo(MS2Schema schema, ActionURL url, boolean includeFeatureFk, ContainerFilter containerFilter)
    {
        super(MS2Manager.getTableInfoPeptidesData());
        _schema = schema;
        setContainerFilter(containerFilter);

        // Stick EndScan column just after Scan column
        ColumnInfo scanColumn = getRealTable().getColumn("Scan");
        ColumnInfo endScanColumn = getRealTable().getColumn("EndScan");
        List<ColumnInfo> columns = new ArrayList<ColumnInfo>(getRealTable().getColumns());
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

        ColumnInfo hColumn = wrapColumn("H", getRealTable().getColumn("Peptide"));
        hColumn.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new HydrophobicityColumn(colInfo);
            }
        });
        addColumn(hColumn);

        ColumnInfo deltaScanColumn = wrapColumn("DeltaScan", getRealTable().getColumn("Fraction"));
        deltaScanColumn.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new DeltaScanColumn(colInfo);
            }
        });
        deltaScanColumn.setFk(null);
        addColumn(deltaScanColumn);

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
                FilteredTable result = new FilteredTable(MS2Manager.getTableInfoQuantitation());
                result.wrapAllColumns(true);
                result.getColumn("PeptideId").setHidden(true);
                result.getColumn("QuantId").setHidden(true);
                return result;
            }
        });
        quantitation.setKeyField(false);
        addColumn(quantitation);

        ColumnInfo proteinGroup = wrapColumn("ProteinProphetData", info.getColumn("RowId"));
        proteinGroup.setIsUnselectable(true);
        proteinGroup.setFk(new LookupForeignKey("PeptideId")
        {
            public TableInfo getLookupTableInfo()
            {
                return _schema.createPeptideMembershipsTable();
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
                FilteredTable table = new FilteredTable(MS2Manager.getTableInfoPeptideProphetData());
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

        addScoreColumns(info);

        getColumn("Fraction").setFk(new LookupForeignKey("Fraction")
        {
            public TableInfo getLookupTableInfo()
            {
                return _schema.createFractionsTable();
            }
        });

        SQLFragment trypticSQL = new SQLFragment();
        trypticSQL.append("((CASE WHEN (");
        trypticSQL.append(strippedPeptideSQL);
        trypticSQL.append(" " + dialect.getCharClassLikeOperator() + " '[KR][^P]%' OR ");
        trypticSQL.append(strippedPeptideSQL);
        trypticSQL.append(" " + dialect.getCharClassLikeOperator() + " '-%') THEN 1 ELSE 0 END) + ");
        trypticSQL.append("(CASE WHEN (");
        trypticSQL.append(strippedPeptideSQL);
        trypticSQL.append(" " + dialect.getCharClassLikeOperator() + " '%[KR][^P]' OR ");
        trypticSQL.append(strippedPeptideSQL);
        trypticSQL.append(" " + dialect.getCharClassLikeOperator() + " '%-') THEN 1 ELSE 0 END))");
        ExprColumn trypricEndsColumn = new ExprColumn(this, "TrypticEnds", trypticSQL, JdbcType.INTEGER);
        addColumn(trypricEndsColumn);

        if (!MS2Manager.getSqlDialect().isSqlServer() || !DbScope.getLabkeyScope().getDatabaseProductVersion().startsWith("08."))
        {
            // Add this column if we're not on SQLServer 2000, which doesn't support BLOBs in a subselect like this
            // This is an ugly way to conditionalize the column - remove the check as soon as we drop support for SQL Server 2000
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
        }

        if(includeFeatureFk)
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
        getFilter().deleteConditions("Fraction/Run");

        SQLFragment sql = new SQLFragment();
        sql.append("Fraction IN (SELECT Fraction FROM ");
        sql.append(MS2Manager.getTableInfoFractions());
        sql.append(" WHERE Run IN (SELECT Run FROM ");
        sql.append(MS2Manager.getTableInfoRuns());
        sql.append(" WHERE Deleted = ?");
        sql.add(Boolean.FALSE);
        Collection<String> ids = getContainerFilter().getIds(_schema.getContainer());
        if(ids != null)
        {
            sql.append(" AND Container IN (");
            String separator = "";
            for (String containerId : ids)
            {
                sql.append(separator);
                separator = ", ";
                sql.append("'");
                sql.append(containerId);
                sql.append("'");
            }
            sql.append(")");
        }
        if (_schema.getRuns() != null)
        {
            sql.append(" AND Run IN ");
            _schema.appendRunInClause(sql);
        }
        sql.append("))");
        addCondition(sql, "Fraction/Run");
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
                return null == ms1svc ? null : ms1svc.createFeaturesTableInfo(_schema.getUser(), _schema.getContainer(), false);
            }
        });
    }

    private void setupProteinColumns(final String showProteinURLString)
    {
        LookupForeignKey fk = new LookupForeignKey("SeqId")
        {
            public TableInfo getLookupTableInfo()
            {
                SequencesTableInfo sequenceTable = new SequencesTableInfo(ProteinManager.getTableInfoSequences().getName(), _schema);
                SQLFragment fastaNameSQL = new SQLFragment(getName() + ".Protein");
                ExprColumn fastaNameColumn = new ExprColumn(PeptidesTableInfo.this, "Database Sequence Name", fastaNameSQL, JdbcType.VARCHAR);
                sequenceTable.addColumn(fastaNameColumn);

                fastaNameColumn.setDisplayColumnFactory(new ProteinDisplayColumnFactory(showProteinURLString));
                fastaNameColumn.setURL(StringExpressionFactory.createURL(showProteinURLString));

                sequenceTable.addPeptideAggregationColumns();

                return sequenceTable;
            }
        };
        fk.setPrefixColumnCaption(false);
        getColumn("SeqId").setFk(fk);

        getColumn("SeqId").setURL(StringExpressionFactory.createURL(showProteinURLString));
        getColumn("SeqId").setDisplayColumnFactory(new ProteinDisplayColumnFactory());
        getColumn("SeqId").setLabel("Search Engine Protein");
        getColumn("Protein").setURL(StringExpressionFactory.createURL(showProteinURLString));
        getColumn("Protein").setDisplayColumnFactory(new ProteinDisplayColumnFactory());
    }

    private void addScoreColumns(TableInfo info)
    {
        Map<String, List<Pair<MS2RunType, Integer>>> columnMap = new HashMap<String, List<Pair<MS2RunType, Integer>>>();
        List<MS2Run> runs = _schema.getRuns();

        Collection<MS2RunType> runTypes;
        if (runs != null && runs.size() > 0)
        {
            runTypes = new HashSet<MS2RunType>();
            for (MS2Run run : runs)
            {
                runTypes.add(run.getRunType());
            }
        }
        else
        {
            runTypes = Arrays.asList(MS2RunType.values());
        }
        for (MS2RunType runType : runTypes)
        {
            int index = 1;
            // Since some search engines have the same names for different scores, build a list of all of the
            // possible intepretations for a given score name based on the run type
            for (String name : runType.getScoreColumnList())
            {
                List<Pair<MS2RunType, Integer>> l = columnMap.get(name);
                if (l == null)
                {
                    l = new ArrayList<Pair<MS2RunType, Integer>>();
                    columnMap.put(name, l);
                }
                l.add(new Pair<MS2RunType, Integer>(runType, index++));
            }
        }

        ColumnInfo realScoreCol = MS2Manager.getTableInfoPeptidesData().getColumn("Score2");

        for (Map.Entry<String, List<Pair<MS2RunType, Integer>>> entry : columnMap.entrySet())
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

        List<FieldKey> result = new ArrayList<FieldKey>();
        result.add(FieldKey.fromParts("Scan"));
        result.add(FieldKey.fromParts("Charge"));
        List<MS2Run> runs = _schema.getRuns();
        if (runs != null && runs.size() > 0)
        {
            for (String name : runs.get(0).getRunType().getScoreColumnList())
            {
                result.add(FieldKey.fromParts(name));
            }
        }
        else
        {
            Set<String> scoreCols = new CaseInsensitiveHashSet();
            for (MS2RunType runType : MS2RunType.values())
            {
                for (String name : runType.getScoreColumnList())
                {
                    if (scoreCols.add(name))
                    {
                        result.add(FieldKey.fromParts(name));
                    }
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
        return MS2Schema.TableType.Peptides.toString();
    }
}
