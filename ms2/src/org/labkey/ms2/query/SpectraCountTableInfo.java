/*
 * Copyright (c) 2008-2016 LabKey Corporation
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
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.VirtualTable;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.query.AliasManager;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.protein.ProteinManager;

import java.util.ArrayList;
import java.util.List;

/**
 * User: jeckels
 * Date: Jan 16, 2008
 */
public class SpectraCountTableInfo extends VirtualTable
{
    private final MS2Schema _ms2Schema;

    private final SpectraCountConfiguration _config;
    private final ViewContext _context;

    private List<PeptideAggregate> _aggregates = new ArrayList<>();
    private MS2Controller.SpectraCountForm _form;

    private class PeptideAggregate
    {
        private final FieldKey _key;
        private boolean _max;
        private boolean _min;
        private boolean _avg;
        private boolean _stdDev;
        private boolean _sum;

        private PeptideAggregate(FieldKey key, boolean max, boolean min, boolean avg, boolean stdDev, boolean sum)
        {
            _key = key;
            _max = max;
            _min = min;
            _avg = avg;
            _stdDev = stdDev;
            _sum = sum;
        }

        private void addSelect(SQLFragment sql, String function, String prefix)
        {
            sql.append(", ");
            sql.append(function);
            sql.append("(pd.");
            sql.append(AliasManager.makeLegalName(_key.toString(), getSqlDialect()));
            sql.append(") AS ");
            sql.append(prefix);
            sql.append(_key.getName());
        }

        public void addSelect(SQLFragment sql)
        {
            if (_max) { addSelect(sql, "MAX", "Max"); }
            if (_min) { addSelect(sql, "MIN", "Min"); }
            if (_avg) { addSelect(sql, "AVG", "Avg"); }
            if (_stdDev) { addSelect(sql, _ms2Schema.getDbSchema().getSqlDialect().getStdDevFunction(), "StdDev"); }
            if (_sum) { addSelect(sql, "SUM", "Sum"); }
            sql.append("\n");
        }

        public void addColumn(SpectraCountTableInfo table)
        {
            if (_max) { table.addColumn(new ExprColumn(table, "Max" + _key.getName(), new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".Max" + _key.getName()), JdbcType.REAL)); }
            if (_min) { table.addColumn(new ExprColumn(table, "Min" + _key.getName(), new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".Min" + _key.getName()), JdbcType.REAL)); }
            if (_avg)
            {
                ExprColumn col = new ExprColumn(table, "Avg" + _key.getName(), new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".Avg" + _key.getName()), JdbcType.REAL);
                col.setFormat("#.#####");
                table.addColumn(col);
            }
            if (_stdDev)
            {
                ExprColumn col = new ExprColumn(table, "StdDev" + _key.getName(), new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".StdDev" + _key.getName()), JdbcType.REAL);
                col.setFormat("#.#####");
                table.addColumn(col);
            }
            if (_sum) { table.addColumn(new ExprColumn(table, "Sum" + _key.getName(), new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".Sum" + _key.getName()), JdbcType.REAL)); }
        }

        public FieldKey getKey()
        {
            return _key;
        }
    }

    public SpectraCountTableInfo(MS2Schema ms2Schema, SpectraCountConfiguration config, ViewContext context, MS2Controller.SpectraCountForm form)
    {
        super(MS2Manager.getSchema(), config.getTableName());
        _ms2Schema = ms2Schema;
        _config = config;

        _context = context;
        _form = form;

        _aggregates.add(new PeptideAggregate(FieldKey.fromParts("PeptideProphet"), true, true, true, true, false));
        _aggregates.add(new PeptideAggregate(FieldKey.fromParts("MZ"), true, true, true, true, false));
        _aggregates.add(new PeptideAggregate(FieldKey.fromParts("RetentionTime"), true, true, true, true, false));
        _aggregates.add(new PeptideAggregate(FieldKey.fromParts("PeptideProphetErrorRate"), true, true, true, true, false));
        _aggregates.add(new PeptideAggregate(FieldKey.fromParts("Quantitation", "LightArea"), true, true, true, true, true));
        _aggregates.add(new PeptideAggregate(FieldKey.fromParts("Quantitation", "HeavyArea"), true, true, true, true, true));
        _aggregates.add(new PeptideAggregate(FieldKey.fromParts("Quantitation", "DecimalRatio"), true, true, true, true, false));

        List<FieldKey> defaultCols = new ArrayList<>();

        ExprColumn runColumn = new ExprColumn(this, "Run", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".Run"), JdbcType.INTEGER);
        runColumn.setFk(new LookupForeignKey(MS2Controller.getShowRunURL(_ms2Schema.getUser(), _ms2Schema.getContainer()), "run", "MS2Details", "Name")
        {
            public TableInfo getLookupTableInfo()
            {
                ExpRunTable result = (ExpRunTable)MS2Schema.TableType.MS2SearchRuns.createTable(_ms2Schema);
                result.setContainerFilter(ContainerFilter.EVERYTHING);
                return result;
            }
        });
        addColumn(runColumn);
        defaultCols.add(FieldKey.fromParts(runColumn.getName()));

        if (_config.isGroupedByPeptide())
        {
            addColumn(new ExprColumn(this, "Peptide", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".Peptide"), JdbcType.VARCHAR));
            defaultCols.add(FieldKey.fromParts("Peptide"));
            addColumn(new ExprColumn(this, "TrimmedPeptide", new SQLFragment("TrimmedPeptide"), JdbcType.VARCHAR));
        }

        if (config.isGroupedByPeptide())
        {
            ColumnInfo indexColumn;
            if (form != null && form.hasTargetSeqIds())
            {
                SQLFragment indexSQL = new SQLFragment(getSqlDialect().getStringIndexOfFunction(new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".TrimmedPeptide"), new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".ProtSequence")));
                indexColumn = new ExprColumn(this, "PeptideIndex", indexSQL, JdbcType.INTEGER);
            }
            else
            {
                indexColumn = new ExprColumn(this, "PeptideIndex", new SQLFragment("NULL"), JdbcType.INTEGER);
            }
            addColumn(indexColumn);
            indexColumn.setDescription("Index of the peptide's sequence within the protein sequence. Only available if a grouping by protein information, or a target protein has been specified.");
        }

        if (_config.isGroupedByCharge())
        {
            addColumn(new ExprColumn(this, "Charge", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".Charge"), JdbcType.INTEGER));
            defaultCols.add(FieldKey.fromParts("Charge"));
        }

        ExprColumn proteinColumn;
        if (_config.isGroupedByProtein() || (form != null && form.hasTargetSeqIds()))
        {
            proteinColumn = new ExprColumn(this, "Protein", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + (_config.isGroupedByProtein() ? ".SequenceId" : ".SeqId")), JdbcType.INTEGER);
            defaultCols.add(FieldKey.fromParts(proteinColumn.getName()));
        }
        else
        {
            proteinColumn = new ExprColumn(this, "Protein", new SQLFragment("NULL"), JdbcType.INTEGER);
        }
        proteinColumn.setDescription("The protein associated with the peptide identification. Only available if a grouping by protein information, or a target protein has been specified.");
        addColumn(proteinColumn);
        proteinColumn.setFk(new LookupForeignKey(new ActionURL(MS2Controller.ShowProteinAction.class, ContainerManager.getRoot()), "seqId", "SeqId", "BestName")
        {
            public TableInfo getLookupTableInfo()
            {
                return _ms2Schema.createSequencesTable();
            }
        });

        if (_config.isGroupedByProtein())
        {
            addColumn(new ExprColumn(this, "FastaName", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".FastaName"), JdbcType.VARCHAR));
        }

        if (!_config.isGroupedByCharge())
        {
            addColumn(new ExprColumn(this, "ChargeStatesObsv", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".ChargeStatesObsv"), JdbcType.INTEGER));
            defaultCols.add(FieldKey.fromParts("ChargeStatesObsv"));
        }

        addColumn(new ExprColumn(this, "TotalPeptideCount", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".TotalPeptideCount"), JdbcType.INTEGER));
        defaultCols.add(FieldKey.fromParts("TotalPeptideCount"));

        if (_config.isUsingProteinProphet())
        {
            addColumn(new ExprColumn(this, "GroupProbability", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".GroupProbability"), JdbcType.REAL));
            addColumn(new ExprColumn(this, "ProtErrorRate", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".ProtErrorRate"), JdbcType.REAL));
            addColumn(new ExprColumn(this, "ProteinProphetUniquePeptides", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".UniquePeptidesCount"), JdbcType.INTEGER));
            addColumn(new ExprColumn(this, "ProteinProphetTotalPeptides", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".TotalNumberPeptides"), JdbcType.INTEGER));
            addColumn(new ExprColumn(this, "PctSpectrumIds", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".PctSpectrumIds"), JdbcType.REAL));
            addColumn(new ExprColumn(this, "PercentCoverage", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".PercentCoverage"), JdbcType.INTEGER));
        }

        for (PeptideAggregate aggregate : _aggregates)
        {
            aggregate.addColumn(this);
        }
        defaultCols.add(FieldKey.fromParts("MaxPeptideProphet"));
        defaultCols.add(FieldKey.fromParts("AvgRetentionTime"));

        setDefaultVisibleColumns(defaultCols);
    }

    @Override @NotNull
    public SQLFragment getFromSQL()
    {
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT\n");
        sql.append("f.run\n"); // FK
        if (_config.isGroupedByPeptide())
        {
            sql.append(", pd.peptide\n");
            sql.append(", MIN(pd.trimmedpeptide) as TrimmedPeptide\n");
        }

        // SQLServer can't GROUP BY a TEXT field, so convert to VARCHAR
        String protSequenceSQL = getSqlDialect().isSqlServer() ? "CAST(s.ProtSequence AS VARCHAR(MAX))" : "s.ProtSequence";

        if (_config.isGroupedByCharge())
        {
            sql.append(", pd.Charge\n");
        }

        if (_config.isGroupedByProtein())
        {
            if (_config.isUsingProteinProphet())
            {
                sql.append(", pgm.SeqId AS SequenceId");
            }
            else
            {
                sql.append(", pd.SeqId AS SequenceId");
            }
            sql.append(", ").append(protSequenceSQL).append(" AS ProtSequence");
            sql.append(", MIN(fs.LookupString) AS FastaName");
        }
        else if (_form != null && _form.hasTargetSeqIds())
        {
            sql.append(", s.SeqId\n");
            sql.append(", ").append(protSequenceSQL).append(" AS ProtSequence\n");
        }

        sql.append(", COUNT(distinct pd.charge) AS ChargeStatesObsv\n");
        sql.append(", COUNT(distinct pd.rowid) AS TotalPeptideCount\n");
        // all sensible aggregates of peptide-level data
        for (PeptideAggregate aggregate : _aggregates)
        {
            aggregate.addSelect(sql);
        }

        if (_config.isUsingProteinProphet())
        {
            // Protein group measurements (values are the same for all proteins in a group, they are not aggs)
            sql.append(", MIN(pg.groupnumber) as ProteinGroupNum\n");
            sql.append(", MIN(pg.indistinguishablecollectionid) as IndistinguishableCollectionId\n");
            sql.append(", MIN(pg.GroupProbability) AS GroupProbability\n");
            sql.append(", MIN(pg.ProteinProbability) AS ProteinProbability\n");
            sql.append(", MIN(pg.ErrorRate) AS ProtErrorRate\n");
            sql.append(", MIN(pg.UniquePeptidesCount) AS UniquePeptidesCount\n");
            sql.append(", MIN(pg.TotalNumberPeptides) AS TotalNumberPeptides\n");
            sql.append(", MIN(pg.PctSpectrumIds) AS PctSpectrumIds\n");
            sql.append(", MIN(pg.PercentCoverage) AS PercentCoverage\n");
        }

        sql.append("FROM ");
        sql.append(MS2Manager.getTableInfoRuns(), "r");
        sql.append("\nINNER JOIN ");
        sql.append(MS2Manager.getTableInfoFractions(), "f");
        sql.append(" ON (r.run = f.run)\n");

        sql.append("INNER JOIN (");

        List<FieldKey> peptideFieldKeys = new ArrayList<>();
        for (PeptideAggregate aggregate : _aggregates)
        {
            peptideFieldKeys.add(aggregate.getKey());
        }
        peptideFieldKeys.add(FieldKey.fromParts("Peptide"));
        peptideFieldKeys.add(FieldKey.fromParts("TrimmedPeptide"));
        peptideFieldKeys.add(FieldKey.fromParts("SeqId"));
        peptideFieldKeys.add(FieldKey.fromParts("Fraction"));
        peptideFieldKeys.add(FieldKey.fromParts("RowId"));
        peptideFieldKeys.add(FieldKey.fromParts("Charge"));

        SQLFragment peptidesSQL;
        if (_form != null && _form.isCustomViewPeptideFilter())
        {
            peptidesSQL = _ms2Schema.getPeptideSelectSQL(_context.getRequest(), _form.getPeptideCustomViewName(_context), peptideFieldKeys, _form.getTargetSeqIds());
        }
        else
        {
            SimpleFilter filter = new SimpleFilter();
            if (_form != null && _form.isPeptideProphetFilter() && _form.getPeptideProphetProbability() != null)
            {
                filter.addCondition(FieldKey.fromParts("PeptideProphet"), _form.getPeptideProphetProbability(), CompareType.GTE);
            }
            if (_form != null && _form.hasTargetSeqIds())
            {
                filter.addClause(ProteinManager.getSequencesFilter(_form.getTargetSeqIds()));
            }
            peptidesSQL = _ms2Schema.getPeptideSelectSQL(filter, peptideFieldKeys);
        }

        sql.append(peptidesSQL);
        sql.append(") pd ON (f.fraction = pd.fraction)\n");

        if (_config.isGroupedByProtein())
        {
            if (_config.isUsingProteinProphet())
            {
                sql.append(" INNER JOIN ");
                sql.append(MS2Manager.getTableInfoPeptideMemberships(), "pm");
                sql.append(" ON (pd.rowId = pm.peptideid)\nINNER JOIN ");
                sql.append(MS2Manager.getTableInfoProteinGroups(), "pg");
                sql.append(" ON (pm.proteinGroupId = pg.rowid)\nINNER JOIN ");
                sql.append(MS2Manager.getTableInfoProteinGroupMemberships(), "pgm");
                sql.append(" ON (pgm.ProteinGroupId = pg.rowId)\nINNER JOIN ");
                sql.append(MS2Manager.getTableInfoFastaRunMapping(), "frm");
                sql.append(" ON (frm.Run = r.Run)\nINNER JOIN ");
                sql.append(ProteinManager.getTableInfoFastaSequences(), "fs");
                sql.append(" ON (fs.fastaid = frm.fastaid AND pgm.seqid = fs.seqid)\nINNER JOIN ");
                sql.append(ProteinManager.getTableInfoSequences(), "s");
                sql.append(" ON (fs.seqId = s.seqid)\n");
            }
            else
            {
                sql.append(" INNER JOIN ");
                sql.append(MS2Manager.getTableInfoFastaRunMapping(), "frm");
                sql.append(" ON (frm.Run = r.Run)\n INNER JOIN ");
                sql.append(ProteinManager.getTableInfoFastaSequences(), "fs");
                sql.append(" ON (fs.fastaid = frm.fastaid AND pd.seqid = fs.seqid)\n INNER JOIN ");
                sql.append(ProteinManager.getTableInfoSequences(), "s");
                sql.append(" ON (s.seqid = fs.seqid)\n");
            }
        }
        else if (_form != null && _form.hasTargetSeqIds())
        {
            sql.append("INNER JOIN ");
            sql.append(ProteinManager.getTableInfoSequences(), "s");
            sql.append(" ON (s.SeqId IN ");
            _form.appendTargetSeqIdsClause(sql);
            sql.append(")\n");
        }

        if (_ms2Schema.getRuns() != null)
        {
            sql.append("AND r.Run IN ");
            _ms2Schema.appendRunInClause(sql);
            sql.append("\n");
        }
        sql.append("GROUP BY f.Run");
        if (_config.isGroupedByPeptide())
        {
            sql.append(", pd.Peptide");
        }
        if (_config.isGroupedByCharge())
        {
            sql.append(", pd.Charge");
        }
        if (_config.isGroupedByProtein())
        {
            if (_config.isUsingProteinProphet())
            {
                sql.append(", pgm.SeqId");
            }
            else
            {
                sql.append(", pd.SeqId");
            }
            sql.append(", ");
            sql.append(protSequenceSQL);
        }
        else if (_form != null && _form.hasTargetSeqIds())
        {
            sql.append(", s.SeqId");
            sql.append(", ");
            sql.append(protSequenceSQL);
        }
        return sql;
    }
}
