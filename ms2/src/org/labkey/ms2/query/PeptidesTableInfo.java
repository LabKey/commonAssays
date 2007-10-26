package org.labkey.ms2.query;

import org.labkey.api.query.*;
import org.labkey.api.data.*;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.ms1.MS1Service;
import org.labkey.ms2.*;
import org.labkey.ms2.peptideview.ProteinDisplayColumnFactory;
import org.labkey.common.util.Pair;
import org.apache.commons.lang.StringUtils;

import java.util.*;
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
        this(schema, null, new ViewURLHelper("MS2", "someAction.view", schema.getContainer()), true);
    }

    public PeptidesTableInfo(MS2Schema schema, boolean includeFeatureFk)
    {
        this(schema, null, new ViewURLHelper("MS2", "someAction.view", schema.getContainer()), includeFeatureFk);
    }

    public PeptidesTableInfo(MS2Schema schema, final MS2Run[] runs, ViewURLHelper url, boolean includeFeatureFk)
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
        quantitation.setIsUnselectable(true);
        quantitation.setFk(new LookupForeignKey("PeptideId")
        {
            public TableInfo getLookupTableInfo()
            {
                FilteredTable result = new FilteredTable(MS2Manager.getTableInfoQuantitation());
                result.wrapAllColumns(true);
                result.getColumn("PeptideId").setIsHidden(true);
                result.getColumn("QuantId").setIsHidden(true);
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
                return _schema.createPeptideMembershipsTable(runs);
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
                table.getColumn("PeptideId").setIsHidden(true);
                return table;
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

        getColumn("Fraction").setFk(new LookupForeignKey("Fraction")
        {
            public TableInfo getLookupTableInfo()
            {
                return _schema.createFractionsTable();
            }
        });

        SQLFragment spectrumSQL = new SQLFragment();
        spectrumSQL.append("(SELECT Spectrum FROM ");
        spectrumSQL.append(MS2Manager.getTableInfoSpectraData());
        spectrumSQL.append(" sd WHERE sd.Fraction = ");
        spectrumSQL.append(ExprColumn.STR_TABLE_ALIAS);
        spectrumSQL.append(".fraction AND sd.Scan = ");
        spectrumSQL.append(ExprColumn.STR_TABLE_ALIAS);
        spectrumSQL.append(".Scan)");
        ExprColumn spectrumColumn = new ExprColumn(this, "Spectrum", spectrumSQL, Types.BLOB);
        spectrumColumn.setIsHidden(true);
        addColumn(spectrumColumn);

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

        if(includeFeatureFk)
            addFeatureInfoColumn();
    }

    private void addFeatureInfoColumn()
    {
        //add an expression column that finds the corresponding ms1 feature id based on
        //the mzXmlUrl and MS2Scan (but not charge since, according to Roland, it may not always be correct)
        //Since we're not matching on charge, we could get multiple rows back, so use MIN to
        //select just the first matching one.
        SQLFragment sqlFeatureJoin = new SQLFragment("(SELECT MIN(fe.FeatureId) as FeatureId FROM ms1.Features AS fe\n" +
                "INNER JOIN ms1.Files AS fi ON (fe.FileId=fi.FileId)\n" +
                "INNER JOIN ms2.Fractions AS fr ON (fr.MzXmlUrl=fi.MzXmlUrl)\n" +
                "INNER JOIN ms2.PeptidesData AS pd ON (pd.Fraction=fr.Fraction AND pd.scan=fe.MS2Scan)\n" +
                "WHERE pd.RowId=" + ExprColumn.STR_TABLE_ALIAS + ".RowId)");

        ColumnInfo ciFeatureId = addColumn(new ExprColumn(this, "MS1 Feature", sqlFeatureJoin, java.sql.Types.INTEGER, getColumn("RowId")));

        //tell query that this new column is an FK to the features table info
        ciFeatureId.setFk(new LookupForeignKey("FeatureId")
        {
            public TableInfo getLookupTableInfo()
            {
                return MS1Service.get().createFeaturesTableInfo(_schema.getUser(), _schema.getContainer(), false);
            }
        });
    }

    private void setupProteinColumns(final String showProteinURLString)
    {
        LookupForeignKey fk = new LookupForeignKey("SeqId")
        {
            public TableInfo getLookupTableInfo()
            {
                SequencesTableInfo sequenceTable = new SequencesTableInfo(null, _schema);
                SQLFragment fastaNameSQL = new SQLFragment(getAliasName() + ".Protein");
                ExprColumn fastaNameColumn = new ExprColumn(PeptidesTableInfo.this, "Database Sequence Name", fastaNameSQL, Types.VARCHAR);
                sequenceTable.addColumn(fastaNameColumn);

                fastaNameColumn.setDisplayColumnFactory(ProteinDisplayColumnFactory.INSTANCE);
                fastaNameColumn.setURL(showProteinURLString);

                sequenceTable.addPeptideAggregationColumns();

                return sequenceTable;
            }
        };
        fk.setPrefixColumnCaption(false);
        getColumn("SeqId").setFk(fk);

        getColumn("SeqId").setURL(showProteinURLString);
        getColumn("SeqId").setDisplayColumnFactory(ProteinDisplayColumnFactory.INSTANCE);
        getColumn("SeqId").setCaption("Search Engine Protein");
        getColumn("Protein").setURL(showProteinURLString);
        getColumn("Protein").setDisplayColumnFactory(ProteinDisplayColumnFactory.INSTANCE);
    }

    private void addScoreColumns(TableInfo info)
    {
        Map<String, List<Pair<MS2RunType,Integer>>> columnMap = new HashMap<String, List<Pair<MS2RunType, Integer>>>();
        MS2Run[] runs = _schema.getRuns();

        Collection<MS2RunType> runTypes;
        if (runs != null && runs.length > 0)
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
            sql.append(MS2Manager.getTableInfoRuns());
            sql.append(" r, ");
            sql.append(MS2Manager.getTableInfoFractions());
            sql.append(" f WHERE r.Run = f.Run AND f.Fraction = ");
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

            ColumnInfo newCol = addColumn(new ExprColumn(this, entry.getKey(), sql, Types.DOUBLE));
            newCol.setFormatString(realScoreCol.getFormatString());
            newCol.setWidth(realScoreCol.getWidth());
        }
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
        MS2Run[] runs = _schema.getRuns();
        if (runs != null && runs.length > 0)
        {
            for (String name : runs[0].getRunType().getScoreColumnList())
            {
                result.add(FieldKey.fromParts(name));
            }
        }
        else
        {
            result.add(FieldKey.fromParts("RawScore"));
            result.add(FieldKey.fromParts("DiffScore"));
            result.add(FieldKey.fromParts("Expect"));
        }
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
