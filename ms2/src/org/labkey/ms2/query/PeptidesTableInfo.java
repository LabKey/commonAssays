package org.labkey.ms2.query;

import org.labkey.api.query.*;
import org.labkey.api.data.*;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.peptideview.ProteinDisplayColumnFactory;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.ArrayList;
import java.sql.Types;

/**
 * User: jeckels
 * Date: Apr 6, 2007
 */
public class PeptidesTableInfo extends FilteredTable
{
    private MS2Schema _schema;

    public PeptidesTableInfo(MS2Schema schema)
    {
        this(schema, null, new ViewURLHelper("MS2", "someAction.view", schema.getContainer()));
    }

    public PeptidesTableInfo(MS2Schema schema, MS2Run[] runs, ViewURLHelper url)
    {
        super(MS2Manager.getTableInfoPeptidesData());
        _schema = schema;

        for (ColumnInfo col : getRealTable().getColumns())
        {
            if (!col.getName().toLowerCase().startsWith("score"))
            {
                addWrapColumn(col);
            }
        }
        SqlDialect dialect = MS2Manager.getSqlDialect();

        addMassColumns(dialect);

        SQLFragment mzSQL = new SQLFragment("CASE WHEN " + ExprColumn.STR_TABLE_ALIAS + ".Charge = 0.0 THEN 0.0 ELSE (" + ExprColumn.STR_TABLE_ALIAS + ".Mass + " + ExprColumn.STR_TABLE_ALIAS + ".DeltaMass + (" + ExprColumn.STR_TABLE_ALIAS + ".Charge - 1.0) * 1.007276) / " + ExprColumn.STR_TABLE_ALIAS + ".Charge END");
        ColumnInfo mz = new ExprColumn(this, "MZ", mzSQL, Types.REAL);
        mz.setFormatString("0.0000");
        mz.setWidth("55");
        mz.setCaption("ObsMZ");
        addColumn(mz);

        SQLFragment strippedPeptideSQL = new SQLFragment("LTRIM(RTRIM(" + ExprColumn.STR_TABLE_ALIAS + ".PrevAA " + dialect.getConcatenationOperator() + " " + ExprColumn.STR_TABLE_ALIAS + ".TrimmedPeptide " + dialect.getConcatenationOperator() + " " + ExprColumn.STR_TABLE_ALIAS + ".NextAA))");
        ColumnInfo strippedPeptide = new ExprColumn(this, "StrippedPeptide", strippedPeptideSQL, Types.VARCHAR);
        strippedPeptide.setWidth("320");
        addColumn(strippedPeptide);

        TableInfo info = getRealTable();

        ColumnInfo quantitation = wrapColumn("Quantitation", info.getColumn("RowId"));
        quantitation.setFk(new LookupForeignKey("PeptideId")
        {
            public TableInfo getLookupTableInfo()
            {
                return MS2Manager.getTableInfoQuantitation();
            }
        });
        quantitation.setKeyField(false);
        addColumn(quantitation);

        ColumnInfo proteinGroup = wrapColumn("ProteinProphetData", info.getColumn("RowId"));
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
        peptideProphetData.setFk(new LookupForeignKey("PeptideId")
        {
            public TableInfo getLookupTableInfo()
            {
                return MS2Manager.getTableInfoPeptideProphetData();
            }
        });
        peptideProphetData.setKeyField(false);
        addColumn(peptideProphetData);

        ViewURLHelper showProteinURL = url.clone();
        showProteinURL.setAction("showProtein.view");
        showProteinURL.deleteParameter("seqId");
        showProteinURL.deleteParameter("protein");
        final String showProteinURLString = showProteinURL.getLocalURIString() + "&seqId=${SeqId}&protein=${Protein}";

        setupProteinColumns(showProteinURLString);

        ViewURLHelper showPeptideURL = url.clone();
        showPeptideURL.setAction("showPeptide.view");
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
        getColumn("Scan").setURL(showPeptideURLString);
        getColumn("Scan").setDisplayColumnFactory(factory);
        getColumn("Peptide").setURL(showPeptideURLString);
        getColumn("Peptide").setDisplayColumnFactory(factory);

        addScoreColumns(info);

        addColumn(wrapColumn("Ion", info.getColumn("score1")));
        addColumn(wrapColumn("Identity", info.getColumn("score2")));
        addColumn(wrapColumn("Homology", info.getColumn("score3")));

        getColumn("Fraction").setFk(new LookupForeignKey("Fraction")
        {
            public TableInfo getLookupTableInfo()
            {
                return _schema.createFractionsTable();
            }
        });

        SQLFragment sql = new SQLFragment();
        sql.append("Fraction IN (SELECT Fraction FROM ");
        sql.append(MS2Manager.getTableInfoFractions());
        sql.append(" WHERE Run IN (SELECT Run FROM ");
        sql.append(MS2Manager.getTableInfoRuns());
        sql.append(" WHERE Container = ? AND Deleted = ?");
        sql.add(_schema.getContainer().getId());
        sql.add(Boolean.FALSE);
        if (runs != null)
        {
            List<Integer> params = new ArrayList<Integer>(runs.length);
            for (MS2Run run : runs)
            {
                params.add(run.getRun());
            }

            assert runs.length > 0 : "Doesn't make sense to filter to no runs";
            sql.append(" AND Run IN (?");
            sql.append(StringUtils.repeat(", ?", params.size() - 1));
            sql.append(")");
            sql.addAll(params);
        }
        sql.append("))");
        addCondition(sql);
    }

    private void setupProteinColumns(final String showProteinURLString)
    {
        getColumn("SeqId").setFk(new LookupForeignKey("SeqId", false)
        {
            public TableInfo getLookupTableInfo()
            {
                SequencesTableInfo sequenceTable = new SequencesTableInfo(null, _schema.getContainer());
                SQLFragment fastaNameSQL = new SQLFragment(getAliasName() + ".Protein");
                ExprColumn fastaNameColumn = new ExprColumn(PeptidesTableInfo.this, "Database Sequence Name", fastaNameSQL, Types.VARCHAR);
                sequenceTable.addColumn(fastaNameColumn);

                fastaNameColumn.setDisplayColumnFactory(ProteinDisplayColumnFactory.INSTANCE);
                fastaNameColumn.setURL(showProteinURLString);

                sequenceTable.addPeptideAggregationColumns();

                return sequenceTable;
            }
        });

        getColumn("SeqId").setURL(showProteinURLString);
        getColumn("SeqId").setDisplayColumnFactory(ProteinDisplayColumnFactory.INSTANCE);
        getColumn("SeqId").setCaption("Search Engine Protein");
        getColumn("Protein").setURL(showProteinURLString);
        getColumn("Protein").setDisplayColumnFactory(ProteinDisplayColumnFactory.INSTANCE);
    }

    private void addScoreColumns(TableInfo info)
    {
        addColumn(wrapColumn("RawScore", info.getColumn("score1"))).setCaption("Raw");
        addColumn(wrapColumn("DiffScore", info.getColumn("score2"))).setCaption("dScore");
        addColumn(wrapColumn("ZScore", info.getColumn("score3")));

        addColumn(wrapColumn("SpScore", info.getColumn("score1")));
        addColumn(wrapColumn("DeltaCN", info.getColumn("score2")));
        addColumn(wrapColumn("XCorr", info.getColumn("score3")));
        addColumn(wrapColumn("SpRank", info.getColumn("score4")));

        addColumn(wrapColumn("Hyper", info.getColumn("score1")));
        addColumn(wrapColumn("Next", info.getColumn("score2")));
        addColumn(wrapColumn("B", info.getColumn("score3")));
        addColumn(wrapColumn("Y", info.getColumn("score4")));
        addColumn(wrapColumn("Expect", info.getColumn("score5")));
    }

    private void addMassColumns(SqlDialect dialect)
    {
        SQLFragment precursorMassSQL = new SQLFragment(getAliasName() + ".mass + " + getAliasName() + ".deltamass");
        ColumnInfo precursorMass = new ExprColumn(this, "PrecursorMass", precursorMassSQL, Types.REAL);
        precursorMass.setFormatString("0.0000");
        precursorMass.setWidth("65");
        precursorMass.setCaption("ObsMH+");
        addColumn(precursorMass);

        SQLFragment fractionalDeltaMassSQL = new SQLFragment("ABS(" + getAliasName() + ".deltamass - " + dialect.getRoundFunction(getAliasName() + ".deltamass") + ")");
        ColumnInfo fractionalDeltaMass = new ExprColumn(this, "FractionalDeltaMass", fractionalDeltaMassSQL, Types.REAL);
        fractionalDeltaMass.setFormatString("0.0000");
        fractionalDeltaMass.setWidth("55");
        fractionalDeltaMass.setCaption("fdMass");
        addColumn(fractionalDeltaMass);

        SQLFragment fractionalSQL = new SQLFragment("CASE\n" +
            "            WHEN " + ExprColumn.STR_TABLE_ALIAS + ".mass = 0.0 THEN 0.0\n" +
            "            ELSE abs(1000000.0 * abs(" + ExprColumn.STR_TABLE_ALIAS + ".deltamass - " + dialect.getRoundFunction(ExprColumn.STR_TABLE_ALIAS + ".deltamass") + ") / (" + ExprColumn.STR_TABLE_ALIAS + ".mass + ((" + ExprColumn.STR_TABLE_ALIAS + ".charge - 1.0) * 1.007276)))\n" +
            "        END");
        ColumnInfo fractionalDeltaMassPPM = new ExprColumn(this, "FractionalDeltaMassPPM", fractionalSQL, Types.REAL);
        fractionalDeltaMassPPM.setFormatString("0.0");
        fractionalDeltaMassPPM.setWidth("80");
        fractionalDeltaMassPPM.setCaption("fdMassPPM");
        addColumn(fractionalDeltaMassPPM);

        SQLFragment deltaSQL = new SQLFragment("CASE\n" +
            "            WHEN " + ExprColumn.STR_TABLE_ALIAS + ".mass = 0.0 THEN 0.0\n" +
            "            ELSE abs(1000000.0 * " + ExprColumn.STR_TABLE_ALIAS + ".deltamass / (" + ExprColumn.STR_TABLE_ALIAS + ".mass + ((" + ExprColumn.STR_TABLE_ALIAS + ".charge - 1.0) * 1.007276)))\n" +
            "        END");
        ColumnInfo deltaMassPPM = new ExprColumn(this, "DeltaMassPPM", deltaSQL, Types.REAL);
        deltaMassPPM.setFormatString("0.0");
        deltaMassPPM.setWidth("75");
        deltaMassPPM.setCaption("dMassPPM");
        addColumn(deltaMassPPM);
    }

    public List<FieldKey> getDefaultVisibleColumns()
    {
        List<FieldKey> result = new ArrayList<FieldKey>();
        result.add(FieldKey.fromParts("Scan"));
        result.add(FieldKey.fromParts("Charge"));
        result.add(FieldKey.fromParts("RawScore"));
        result.add(FieldKey.fromParts("DiffScore"));
        result.add(FieldKey.fromParts("Expect"));
        result.add(FieldKey.fromParts("IonPercent"));
        result.add(FieldKey.fromParts("Mass"));
        result.add(FieldKey.fromParts("DeltaMass"));
        result.add(FieldKey.fromParts("PeptideProphet"));
        result.add(FieldKey.fromParts("Peptide"));
        result.add(FieldKey.fromParts("ProteinHits"));
        result.add(FieldKey.fromParts("Protein"));
        return result;
    }

}
