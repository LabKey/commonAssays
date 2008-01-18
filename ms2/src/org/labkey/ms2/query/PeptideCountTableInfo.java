package org.labkey.ms2.query;

import org.labkey.api.data.*;
import org.labkey.api.query.*;
import org.labkey.api.view.ActionURL;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.protein.ProteinManager;

import javax.servlet.http.HttpServletRequest;
import java.sql.Types;
import java.util.List;
import java.util.ArrayList;

/**
 * User: jeckels
 * Date: Jan 16, 2008
 */
public class PeptideCountTableInfo extends VirtualTable
{
    private final MS2Schema _ms2Schema;

    private final MS2Schema.SpectraCountConfiguration _config;
    private final HttpServletRequest _request;
    private final String _peptideViewName;

    private List<PeptideAggregate> _aggregates = new ArrayList<PeptideAggregate>();

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

        public void addColumn(PeptideCountTableInfo table)
        {
            if (_max) { table.addColumn(new ExprColumn(table, "Max" + _key.getName(), new SQLFragment("Max" + _key.getName()), Types.FLOAT)); }
            if (_min) { table.addColumn(new ExprColumn(table, "Min" + _key.getName(), new SQLFragment("Min" + _key.getName()), Types.FLOAT)); }
            if (_avg)
            {
                ExprColumn col = new ExprColumn(table, "Avg" + _key.getName(), new SQLFragment("Avg" + _key.getName()), Types.FLOAT);
                col.setFormatString("#.#####");
                table.addColumn(col);
            }
            if (_stdDev)
            {
                ExprColumn col = new ExprColumn(table, "StdDev" + _key.getName(), new SQLFragment("StdDev" + _key.getName()), Types.FLOAT);
                col.setFormatString("#.#####");
                table.addColumn(col);
            }
            if (_sum) { table.addColumn(new ExprColumn(table, "Sum" + _key.getName(), new SQLFragment("Sum" + _key.getName()), Types.FLOAT)); }
        }

        public FieldKey getKey()
        {
            return _key;
        }
    }

    public PeptideCountTableInfo(MS2Schema ms2Schema, MS2Schema.SpectraCountConfiguration config, HttpServletRequest request, String peptideViewName)
    {
        super(MS2Manager.getSchema());
        _ms2Schema = ms2Schema;
        _config = config;

        _request = request;
        _peptideViewName = peptideViewName;

        _aggregates.add(new PeptideAggregate(FieldKey.fromParts("PeptideProphet"), true, true, true, true, false));
        _aggregates.add(new PeptideAggregate(FieldKey.fromParts("RetentionTime"), true, true, true, true, false));
        _aggregates.add(new PeptideAggregate(FieldKey.fromParts("PeptideProphetErrorRate"), true, true, true, true, false));
        _aggregates.add(new PeptideAggregate(FieldKey.fromParts("Quantitation", "LightArea"), true, true, true, true, true));
        _aggregates.add(new PeptideAggregate(FieldKey.fromParts("Quantitation", "HeavyArea"), true, true, true, true, true));
        _aggregates.add(new PeptideAggregate(FieldKey.fromParts("Quantitation", "DecimalRatio"), true, true, true, true, false));

        List<FieldKey> defaultCols = new ArrayList<FieldKey>();

        ExprColumn runColumn = new ExprColumn(this, "Run", new SQLFragment("Run"), Types.INTEGER);
        runColumn.setFk(new LookupForeignKey(new ActionURL(MS2Controller.ShowRunAction.class, _ms2Schema.getContainer()), "run", "MS2Details", "Name")
        {
            public TableInfo getLookupTableInfo()
            {
                return _ms2Schema.createRunsTable(null);
            }
        });
        addColumn(runColumn);
        defaultCols.add(FieldKey.fromParts(runColumn.getName()));

        if (_config.isGroupedByPeptide())
        {
            addColumn(new ExprColumn(this, "Peptide", new SQLFragment("Peptide"), Types.VARCHAR));
            defaultCols.add(FieldKey.fromParts("Peptide"));
            addColumn(new ExprColumn(this, "TrimmedPeptide", new SQLFragment("TrimmedPeptide"), Types.VARCHAR));
        }

        if (_config.isGroupedByProtein())
        {
            ExprColumn col = new ExprColumn(this, "Protein", new SQLFragment("SequenceId"), Types.INTEGER);
            col.setFk(new LookupForeignKey(new ActionURL(MS2Controller.ShowProteinAction.class), "seqId", "SeqId", "BestName")
            {
                public TableInfo getLookupTableInfo()
                {
                    return _ms2Schema.createSequencesTable(null);
                }
            });
            addColumn(col);
            defaultCols.add(FieldKey.fromParts(col.getName()));

            addColumn(new ExprColumn(this, "FastaName", new SQLFragment("FastaName"), Types.VARCHAR));
        }

        if (_config.isGroupedByCharge())
        {
            addColumn(new ExprColumn(this, "Charge", new SQLFragment("Charge"), Types.INTEGER));
            defaultCols.add(FieldKey.fromParts("Charge"));
        }
        else
        {
            addColumn(new ExprColumn(this, "ChargeStatesObsv", new SQLFragment("ChargeStatesObsv"), Types.INTEGER));
            defaultCols.add(FieldKey.fromParts("ChargeStatesObsv"));
        }
        
        addColumn(new ExprColumn(this, "TotalPeptideCount", new SQLFragment("TotalPeptideCount"), Types.INTEGER));
        defaultCols.add(FieldKey.fromParts("TotalPeptideCount"));

        if (_config.isUsingProteinProphet())
        {
            addColumn(new ExprColumn(this, "GroupProbability", new SQLFragment("GroupProbability"), Types.FLOAT));
            addColumn(new ExprColumn(this, "ProtErrorRate", new SQLFragment("ProtErrorRate"), Types.FLOAT));
            addColumn(new ExprColumn(this, "ProteinProphetUniquePeptides", new SQLFragment("UniquePeptidesCount"), Types.INTEGER));
            addColumn(new ExprColumn(this, "ProteinProphetTotalPeptides", new SQLFragment("TotalNumberPeptides"), Types.INTEGER));
            addColumn(new ExprColumn(this, "PctSpectrumIds", new SQLFragment("PctSpectrumIds"), Types.FLOAT));
            addColumn(new ExprColumn(this, "PercentCoverage", new SQLFragment("PercentCoverage"), Types.INTEGER));
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
        QueryDefinition queryDef = QueryService.get().createQueryDefForTable(_ms2Schema, MS2Schema.PEPTIDES_TABLE_NAME);
        SimpleFilter filter = new SimpleFilter();
        CustomView view = queryDef.getCustomView(_ms2Schema.getUser(), _request, _peptideViewName);
        if (view != null)
        {
            ActionURL url = new ActionURL();
            view.applyFilterAndSortToURL(url, "InternalName");
            filter.addUrlFilters(url, "InternalName");
        }

        TableInfo peptidesTable = _ms2Schema.createPeptidesTable("PeptidesAlias");

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

        ColumnInfo[] peptideCols = QueryService.get().getColumns(peptidesTable, peptideFieldKeys).values().toArray(new ColumnInfo[0]);

        SQLFragment peptidesSQL = Table.getSelectSQL(peptidesTable, peptideCols, filter, new Sort());
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
        sql.append("AND r.Container = ?\n");
        sql.add(_ms2Schema.getContainer().getId());
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
