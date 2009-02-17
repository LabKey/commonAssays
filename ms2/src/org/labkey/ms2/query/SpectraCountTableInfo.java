/*
 * Copyright (c) 2008-2009 LabKey Corporation
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

import org.labkey.api.data.*;
import org.labkey.api.query.*;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewContext;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.protein.ProteinManager;

import java.sql.Types;
import java.util.List;
import java.util.ArrayList;

/**
 * User: jeckels
 * Date: Jan 16, 2008
 */
public class SpectraCountTableInfo extends VirtualTable
{
    private final MS2Schema _ms2Schema;

    private final SpectraCountConfiguration _config;
    private final ViewContext _context;

    private List<PeptideAggregate> _aggregates = new ArrayList<PeptideAggregate>();
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
            sql.append(_key.toString().replace('/', '_'));
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
            if (_max) { table.addColumn(new ExprColumn(table, "Max" + _key.getName(), new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".Max" + _key.getName()), Types.FLOAT)); }
            if (_min) { table.addColumn(new ExprColumn(table, "Min" + _key.getName(), new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".Min" + _key.getName()), Types.FLOAT)); }
            if (_avg)
            {
                ExprColumn col = new ExprColumn(table, "Avg" + _key.getName(), new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".Avg" + _key.getName()), Types.FLOAT);
                col.setFormatString("#.#####");
                table.addColumn(col);
            }
            if (_stdDev)
            {
                ExprColumn col = new ExprColumn(table, "StdDev" + _key.getName(), new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".StdDev" + _key.getName()), Types.FLOAT);
                col.setFormatString("#.#####");
                table.addColumn(col);
            }
            if (_sum) { table.addColumn(new ExprColumn(table, "Sum" + _key.getName(), new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".Sum" + _key.getName()), Types.FLOAT)); }
        }

        public FieldKey getKey()
        {
            return _key;
        }
    }

    public SpectraCountTableInfo(MS2Schema ms2Schema, SpectraCountConfiguration config, ViewContext context, MS2Controller.SpectraCountForm form)
    {
        super(MS2Manager.getSchema());
        _ms2Schema = ms2Schema;
        _config = config;

        _context = context;
        _form = form;

        _aggregates.add(new PeptideAggregate(FieldKey.fromParts("PeptideProphet"), true, true, true, true, false));
        _aggregates.add(new PeptideAggregate(FieldKey.fromParts("RetentionTime"), true, true, true, true, false));
        _aggregates.add(new PeptideAggregate(FieldKey.fromParts("PeptideProphetErrorRate"), true, true, true, true, false));
        _aggregates.add(new PeptideAggregate(FieldKey.fromParts("Quantitation", "LightArea"), true, true, true, true, true));
        _aggregates.add(new PeptideAggregate(FieldKey.fromParts("Quantitation", "HeavyArea"), true, true, true, true, true));
        _aggregates.add(new PeptideAggregate(FieldKey.fromParts("Quantitation", "DecimalRatio"), true, true, true, true, false));

        List<FieldKey> defaultCols = new ArrayList<FieldKey>();

        ExprColumn runColumn = new ExprColumn(this, "Run", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".Run"), Types.INTEGER);
        runColumn.setFk(new LookupForeignKey(new ActionURL(MS2Controller.ShowRunAction.class, _ms2Schema.getContainer()), "run", "MS2Details", "Name")
        {
            public TableInfo getLookupTableInfo()
            {
                ExpRunTable result = (ExpRunTable)MS2Schema.TableType.MS2SearchRuns.createTable(null, _ms2Schema);
                result.setContainerFilter(ContainerFilter.EVERYTHING);
                return result;
            }
        });
        addColumn(runColumn);
        defaultCols.add(FieldKey.fromParts(runColumn.getName()));

        if (_config.isGroupedByPeptide())
        {
            addColumn(new ExprColumn(this, "Peptide", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".Peptide"), Types.VARCHAR));
            defaultCols.add(FieldKey.fromParts("Peptide"));
            addColumn(new ExprColumn(this, "TrimmedPeptide", new SQLFragment("TrimmedPeptide"), Types.VARCHAR));
        }

        if (_config.isGroupedByCharge())
        {
            addColumn(new ExprColumn(this, "Charge", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".Charge"), Types.INTEGER));
            defaultCols.add(FieldKey.fromParts("Charge"));
        }

        if (_config.isGroupedByProtein())
        {
            ExprColumn col = new ExprColumn(this, "Protein", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".SequenceId"), Types.INTEGER);
            col.setFk(new LookupForeignKey(new ActionURL(MS2Controller.ShowProteinAction.class, ContainerManager.getRoot()), "seqId", "SeqId", "BestName")
            {
                public TableInfo getLookupTableInfo()
                {
                    return _ms2Schema.createSequencesTable(null);
                }
            });
            addColumn(col);
            defaultCols.add(FieldKey.fromParts(col.getName()));

            addColumn(new ExprColumn(this, "FastaName", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".FastaName"), Types.VARCHAR));
        }

        if (!_config.isGroupedByCharge())
        {
            addColumn(new ExprColumn(this, "ChargeStatesObsv", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".ChargeStatesObsv"), Types.INTEGER));
            defaultCols.add(FieldKey.fromParts("ChargeStatesObsv"));
        }
        
        addColumn(new ExprColumn(this, "TotalPeptideCount", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".TotalPeptideCount"), Types.INTEGER));
        defaultCols.add(FieldKey.fromParts("TotalPeptideCount"));

        if (_config.isUsingProteinProphet())
        {
            addColumn(new ExprColumn(this, "GroupProbability", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".GroupProbability"), Types.FLOAT));
            addColumn(new ExprColumn(this, "ProtErrorRate", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".ProtErrorRate"), Types.FLOAT));
            addColumn(new ExprColumn(this, "ProteinProphetUniquePeptides", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".UniquePeptidesCount"), Types.INTEGER));
            addColumn(new ExprColumn(this, "ProteinProphetTotalPeptides", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".TotalNumberPeptides"), Types.INTEGER));
            addColumn(new ExprColumn(this, "PctSpectrumIds", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".PctSpectrumIds"), Types.FLOAT));
            addColumn(new ExprColumn(this, "PercentCoverage", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".PercentCoverage"), Types.INTEGER));
        }

        for (PeptideAggregate aggregate : _aggregates)
        {
            aggregate.addColumn(this);
        }
        defaultCols.add(FieldKey.fromParts("MaxPeptideProphet"));
        defaultCols.add(FieldKey.fromParts("AvgRetentionTime"));

        setDefaultVisibleColumns(defaultCols);
    }

    @Override
    public SQLFragment getFromSQL(String alias)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("(SELECT\n");
        sql.append("f.run\n"); // FK
        if (_config.isGroupedByPeptide())
        {
            sql.append(", pd.peptide\n");
            sql.append(", MIN(pd.trimmedpeptide) as TrimmedPeptide\n");
        }

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
            sql.append(", MIN(fs.LookupString) AS FastaName");
        }

        sql.append(", COUNT(distinct pd.charge) AS ChargeStatesObsv\n");
        sql.append(", COUNT(distinct pd.rowid) AS TotalPeptideCount\n");
        // all sensible aggreigates of peptide-level data
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

        sql.append("FROM " + MS2Manager.getTableInfoRuns() + " r\n");
        sql.append("INNER JOIN " + MS2Manager.getTableInfoFractions() + " f ON (r.run = f.run)\n");

        sql.append("INNER JOIN (");

        List<FieldKey> peptideFieldKeys = new ArrayList<FieldKey>();
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
        if (_form.isCustomViewPeptideFilter())
        {
            peptidesSQL = _ms2Schema.getPeptideSelectSQL(_context.getRequest(), _form.getCustomViewName(_context), peptideFieldKeys);
        }
        else
        {
            SimpleFilter filter = new SimpleFilter();
            if (_form.isPeptideProphetFilter() && _form.getPeptideProphetProbability() != null)
            {
                filter.addClause(new CompareType.CompareClause("PeptideProphet", CompareType.GTE, _form.getPeptideProphetProbability()));
            }
            peptidesSQL = _ms2Schema.getPeptideSelectSQL(filter, peptideFieldKeys);
        }
        
        sql.append(peptidesSQL);
        sql.append(") pd ON (f.fraction = pd.fraction)\n");

        if (_config.isGroupedByProtein())
        {
            if (_config.isUsingProteinProphet())
            {
                sql.append("INNER JOIN " + MS2Manager.getTableInfoPeptideMemberships()+ " pm ON (pd.rowId = pm.peptideid)\n");
                sql.append("INNER JOIN " + MS2Manager.getTableInfoProteinGroups() + " pg ON (pm.proteinGroupId = pg.rowid)\n");
                sql.append("INNER JOIN " + MS2Manager.getTableInfoProteinGroupMemberships() + " pgm ON (pgm.ProteinGroupId = pg.rowId)\n");
                sql.append("INNER JOIN " + ProteinManager.getTableInfoFastaSequences() + " fs ON (fs.fastaid = r.fastaid AND pgm.seqid = fs.seqid)\n");
            }
            else
            {
                sql.append("INNER JOIN " + ProteinManager.getTableInfoFastaSequences() + " fs ON (fs.fastaid = r.fastaid AND pd.seqid = fs.seqid)\n");
            }
        }

        if (_ms2Schema.getRuns() != null)
        {
            sql.append("AND r.Run IN (");
            String separator = "";
            for (MS2Run ms2Run : _ms2Schema.getRuns())
            {
                sql.append(separator);
                separator = ", ";
                sql.append(ms2Run.getRun());
            }
            sql.append(")\n");
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
        }
        sql.append(") AS ");
        sql.append(alias);
        return sql;
    }
}
