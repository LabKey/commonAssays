package org.labkey.ms1.query;

import org.labkey.api.data.*;
import org.labkey.api.ms2.MS2Service;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.view.ActionURL;
import org.labkey.api.ms1.MS1Service;
import org.labkey.ms1.MS1Controller;
import org.labkey.ms1.MS1Manager;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides a filtered table implementation for the Features table, allowing clients
 * to add more conditions (e.g., filtering for features from a specific run)
 *
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Oct 3, 2007
 * Time: 11:00:43 AM
 */
public class FeaturesTableInfo extends FilteredTable
{
    public static final String COLUMN_PEPTIDE_INFO = "RelatedPeptide";
    public static final String COLUMN_FIND_SIMILAR_LINK = "FindSimilarLink";

    //Data Members
    private MS1Schema _schema;
    private boolean _includePepFk = true;
    private List<FeaturesFilter> _filters = null;
    private boolean _includeDeleted = false;

    public FeaturesTableInfo(MS1Schema schema)
    {
        this(schema, true);
    }

    public FeaturesTableInfo(MS1Schema schema, boolean includePepFk)
    {
        this(schema, includePepFk, null);
    }

    public FeaturesTableInfo(MS1Schema schema, boolean includePepFk, Boolean peaksAvailable)
    {
        super(MS1Manager.get().getTable(MS1Manager.TABLE_FEATURES));

        _schema = schema;
        _includePepFk = includePepFk;

        //wrap all the columns
        wrapAllColumns(true);

        //tell query that FileId is an FK to the Files user table info
        getColumn("FileId").setFk(new LookupForeignKey("FileId")
        {
            public TableInfo getLookupTableInfo()
            {
                return _schema.getFilesTableInfo();
            }
        });

        //URL and display factory for the scan and peptide columns
        String urlPep = new ActionURL(MS1Controller.ShowMS2PeptideAction.class, schema.getContainer()).getLocalURIString()
                + "featureId=${FeatureId}";

        DisplayColumnFactory dcfPep = new DisplayColumnFactory()
        {
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                DataColumn dataColumn = new DataColumn(colInfo);
                dataColumn.setLinkTarget("peptide");
                return dataColumn;
            }
        };

        if(includePepFk)
        {
            ColumnInfo ciPepId = addColumn(new ExprColumn(this, COLUMN_PEPTIDE_INFO,
                    new SQLFragment(COLUMN_PEPTIDE_INFO), java.sql.Types.INTEGER));

            //tell query that this new column is an FK to the peptides data table
            ciPepId.setFk(new LookupForeignKey("RowId", "Peptide")
            {
                public TableInfo getLookupTableInfo()
                {
                    return MS2Service.get().createPeptidesTableInfo(_schema.getUser(), _schema.getContainer(), 
                            false, _schema.isRestrictContainer(), null, null);
                }
            });
            
            ciPepId.setURL(urlPep);
            ciPepId.setDisplayColumnFactory(dcfPep);
        } //if(includePepFk)

        //make the ms2 scan a hyperlink to showPeptide view
        ColumnInfo ciMS2Scan = getColumn("MS2Scan");
        ciMS2Scan.setURL(urlPep);
        ciMS2Scan.setDisplayColumnFactory(dcfPep);

        //add new columns for the peaks and details links
        if(null != peaksAvailable)
            addColumn(new PeaksAvailableColumnInfo(this, peaksAvailable.booleanValue()));
        else
            addColumn(new PeaksAvailableColumnInfo(this));

        //add a column for the find similar link
        ColumnInfo similarLinkCol = addColumn(wrapColumn(COLUMN_FIND_SIMILAR_LINK, getRealTable().getColumn("FeatureId")));
        similarLinkCol.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new SimilarLinkDisplayColumn(colInfo);
            }
        });

        //only display a subset of the columns by by default
        ArrayList<FieldKey> visibleColumns = new ArrayList<FieldKey>(getDefaultVisibleColumns());
        visibleColumns.remove(FieldKey.fromParts("FeatureId"));
        visibleColumns.remove(FieldKey.fromParts("FileId"));
        visibleColumns.remove(FieldKey.fromParts("Description"));
        visibleColumns.remove(FieldKey.fromParts("Background"));
        visibleColumns.remove(FieldKey.fromParts("Median"));
        visibleColumns.remove(FieldKey.fromParts("KL"));
        visibleColumns.remove(FieldKey.fromParts("ScanCount"));
        visibleColumns.remove(FieldKey.fromParts("ChargeStates"));

        //move peak and detail links column to first position
        visibleColumns.remove(FieldKey.fromParts(PeaksAvailableColumnInfo.COLUMN_NAME));
        visibleColumns.add(0, FieldKey.fromParts(PeaksAvailableColumnInfo.COLUMN_NAME));

        //move find similar link column to second position
        visibleColumns.remove(FieldKey.fromParts(COLUMN_FIND_SIMILAR_LINK));
        visibleColumns.add(1, FieldKey.fromParts(COLUMN_FIND_SIMILAR_LINK));

        setDefaultVisibleColumns(visibleColumns);
    } //c-tor

    public List<FeaturesFilter> getBaseFilters()
    {
        return _filters;
    }

    public void setBaseFilters(List<FeaturesFilter> filters)
    {
        _filters = filters;
    }

    public boolean includeDeleted()
    {
        return _includeDeleted;
    }

    public void setIncludeDeleted(boolean includeDeleted)
    {
        _includeDeleted = includeDeleted;
    }

    public SQLFragment getFromSQL(String alias)
    {
        SQLFragment sql = new SQLFragment("(SELECT\n");
        String sep = "";

        //all base-table columns
        for(ColumnInfo col : getRealTable().getColumns())
        {
            sql.append(sep);
            sql.append("fe.");
            sql.append(col.getName());
            sql.append(" AS ");
            sql.append(col.getAlias());
            sep = ",\n";
        }

        //peptide row id
        if(_includePepFk)
        {
            sql.append(sep);
            sql.append("pd.RowId AS ");
            sql.append(COLUMN_PEPTIDE_INFO);
        }

        //from clause
        sql.append("\nFROM ");
        sql.append(MS1Service.Tables.Features.getFullName());
        sql.append(" AS fe");

        if(!includeDeleted())
        {
            sql.append("\nINNER JOIN ");
            sql.append(MS1Service.Tables.Files.getFullName());
            sql.append(" AS fi ON (fe.FileId=fi.FileId)");
        }

        if(_includePepFk || null != _filters)
        {
            sql.append("\nINNER JOIN exp.Data AS d ON (fi.ExpDataFileId=d.RowId)");
            sql.append("\nLEFT OUTER JOIN ms2.Fractions AS fr ON (fi.MzXmlUrl=fr.MzXmlUrl)");
            sql.append("\nLEFT OUTER JOIN ms2.PeptidesData AS pd ON (pd.Fraction=fr.Fraction AND pd.Scan=fe.MS2Scan AND pd.Charge=fe.MS2Charge)");
            sql.append("\nLEFT OUTER JOIN ms2.Runs AS r ON (fr.Run=r.Run)");

            //set a base filter condition to exclude deleted and unimported runs
            //and only runs from the correct containers
            sql.append("\nWHERE r.Container IN (");
            sql.append(_schema.getContainerInList());
            sql.append(")");
            sql.append(new SQLFragment("\nAND r.Deleted=?", false)); //filter out deleted MS2 runs
            if(!includeDeleted())
                sql.append(new SQLFragment("\nAND fi.Imported=? AND fi.Deleted=?", true, false));

            if(null != _filters)
            {
                Map<String,String> aliasMap = getAliasMap();
                for(FeaturesFilter filter : _filters)
                {
                    sql.append("\nAND (");
                    sql.append(filter.getWhereClause(aliasMap, getSqlDialect()));
                    sql.append(")");
                }
            }
        }
        else
        {
            if(!includeDeleted())
                sql.append(new SQLFragment("\nWHERE fi.Imported=? AND fi.Deleted=?", true, false));
        }

        //alias
        sql.append(") AS ");
        sql.append(alias);

        return sql;
    }

    public Map<String,String> getAliasMap()
    {
        HashMap<String,String> aliasMap = new HashMap<String,String>();
        aliasMap.put(MS1Service.Tables.Features.getFullName(), "fe");
        aliasMap.put(MS1Service.Tables.Files.getFullName(), "fi");
        aliasMap.put("exp.Data", "d");
        aliasMap.put("ms2.Fractions", "fr");
        aliasMap.put("ms2.PeptidesData", "pd");
        aliasMap.put("ms2.Runs", "r");
        return aliasMap;
    }
} //class FeaturesTableInfo
