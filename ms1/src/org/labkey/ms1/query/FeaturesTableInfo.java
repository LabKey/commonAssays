package org.labkey.ms1.query;

import org.labkey.api.data.*;
import org.labkey.api.ms2.MS2Service;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.util.StringExpressionFactory;
import org.labkey.api.view.ActionURL;
import org.labkey.ms1.MS1Manager;
import org.labkey.ms1.MS1Module;
import org.labkey.ms1.MS1Controller;

import java.util.ArrayList;
import java.util.List;

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
    public static final String COLUMN_PEPTIDE_INFO = "Related Peptide";
    public static final String COLUMN_FIND_SIMILAR_LINK = "FindSimilarLink";

    //Data Members
    private MS1Schema _schema;

    public FeaturesTableInfo(MS1Schema schema)
    {
        this(schema, true);
    }

    public FeaturesTableInfo(MS1Schema schema, boolean includePepFk)
    {
        super(MS1Manager.get().getTable(MS1Manager.TABLE_FEATURES));

        _schema = schema;

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

        if(includePepFk)
        {
            //add an expression column that finds the corresponding peptide id based on
            //the mzXmlUrl and MS2Scan (but not charge since, according to Roland, it may not always be correct)
            //Since we're not matching on charge, we could get multiple rows back, so use MIN to
            //select just the first matching one.
            SQLFragment sqlPepJoin = new SQLFragment("(SELECT MIN(pd.rowid) AS PeptideId" +
                    " FROM ms2.PeptidesData AS pd" +
                    " INNER JOIN ms2.Fractions AS fr ON (fr.fraction=pd.fraction)" +
                    " INNER JOIN ms2.Runs AS r ON (fr.Run=r.Run)" +
                    " INNER JOIN ms1.Files AS fi ON (fi.MzXmlUrl=fr.MzXmlUrl)" +
                    " INNER JOIN ms1.Features AS fe ON (fe.FileId=fi.FileId AND pd.scan=fe.MS2Scan)" +
                    " WHERE fe.FeatureId=" + ExprColumn.STR_TABLE_ALIAS + ".FeatureId)");

            ColumnInfo ciPepId = addColumn(new ExprColumn(this, COLUMN_PEPTIDE_INFO, sqlPepJoin, java.sql.Types.INTEGER, getColumn("FeatureId")));
            ciPepId.setIsUnselectable(true);

            //tell query that this new column is an FK to the peptides data table
            ciPepId.setFk(new LookupForeignKey("RowId")
            {
                public TableInfo getLookupTableInfo()
                {
                    return MS2Service.get().createPeptidesTableInfo(_schema.getUser(), _schema.getContainer(), 
                            false, _schema.isRestrictContainer());
                }
            });
        } //if(includePepFk)

        //make the ms2 scan a hyperlink to showPeptide view
        ColumnInfo ciMS2Scan = getColumn("MS2Scan");
        ActionURL urlPep = new ActionURL(MS1Module.CONTROLLER_NAME, "showMS2Peptide", schema.getContainer());
        ciMS2Scan.setURL(urlPep.getLocalURIString() + "featureId=${FeatureId}");

        ciMS2Scan.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                DataColumn dataColumn = new DataColumn(colInfo);
                dataColumn.setLinkTarget("peptide");
                return dataColumn;
            }
        });

        //mandate a filter that excludes deleted and not fully-imported features
        addCondition(new SQLFragment("FileId IN (SELECT FileId FROM ms1.Files WHERE Imported=? AND Deleted=?)", true, false), "FileId");

        //add new columns for the peaks and details links
        addColumn(new PeaksAvailableColumnInfo(this));

        //add a column for the find similar link
        ColumnInfo similarLinkCol = addColumn(new ColumnInfo(COLUMN_FIND_SIMILAR_LINK, this));
        similarLinkCol.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new FindSimilarDisplayColumn();
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
        visibleColumns.add(FieldKey.fromParts(COLUMN_PEPTIDE_INFO, "Peptide"));
        
        //move peak and detail links column to first position
        visibleColumns.remove(FieldKey.fromParts(PeaksAvailableColumnInfo.COLUMN_NAME));
        visibleColumns.add(0, FieldKey.fromParts(PeaksAvailableColumnInfo.COLUMN_NAME));

        //move find similar link column to second position
        visibleColumns.remove(FieldKey.fromParts(COLUMN_FIND_SIMILAR_LINK));
        visibleColumns.add(1, FieldKey.fromParts(COLUMN_FIND_SIMILAR_LINK));

        setDefaultVisibleColumns(visibleColumns);
    } //c-tor

} //class FeaturesTableInfo
