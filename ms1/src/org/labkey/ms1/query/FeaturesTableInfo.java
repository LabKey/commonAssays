package org.labkey.ms1.query;

import org.labkey.api.data.*;
import org.labkey.api.ms2.MS2Service;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.util.StringExpressionFactory;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.ms1.MS1Manager;
import org.labkey.ms1.MS1Module;

import java.util.ArrayList;

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
    public static final String COLUMN_DETAILS_LINK = "DetailsLink";
    public static final String COLUMN_PEAKS_LINK = "PeaksLink";
    public static final String COLUMN_PEPTIDE_INFO = "Related Peptide";

    public FeaturesTableInfo(MS1Schema schema, Container container)
    {
        this(schema, container, true);
    }

    public FeaturesTableInfo(MS1Schema schema, Container container, boolean includePepFk)
    {
        super(MS1Manager.get().getTable(MS1Manager.TABLE_FEATURES));

        _schema = schema;
        _container = container;

        //wrap all the columns
        wrapAllColumns(true);

        //mark the FeatureId column as hidden
        getColumn("FeatureId").setIsHidden(true);

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
                    " INNER JOIN exp.Data AS d ON (r.Container=d.Container)" +
                    " INNER JOIN ms1.Files AS fi ON (fi.MzXmlUrl=fr.MzXmlUrl AND fi.ExpDataFileId=d.RowId)" +
                    " INNER JOIN ms1.Features AS fe ON (fe.FileId=fi.FileId AND pd.scan=fe.MS2Scan)" +
                    " WHERE fe.FeatureId=" + ExprColumn.STR_TABLE_ALIAS + ".FeatureId)");

            ColumnInfo ciPepId = addColumn(new ExprColumn(this, COLUMN_PEPTIDE_INFO, sqlPepJoin, java.sql.Types.INTEGER, getColumn("FeatureId")));
            ciPepId.setIsUnselectable(true);

            //tell query that this new column is an FK to the peptides data table
            ciPepId.setFk(new LookupForeignKey("RowId")
            {
                public TableInfo getLookupTableInfo()
                {
                    return MS2Service.get().createPeptidesTableInfo(_schema.getUser(), _schema.getContainer(), false);
                }
            });
        } //if(includePepFk)

        //create the details and peaks links columns
        //note that the URL for these is dependent upon the current container
        //so that will be set in addRunIdCondition()
        ColumnInfo ciLink = addColumn(new ColumnInfo(COLUMN_DETAILS_LINK));
        ciLink.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                SimpleDisplayColumn dc = new SimpleDisplayColumn("(no details)");
                dc.setName(COLUMN_DETAILS_LINK);
                return dc;
            }
        });
        ciLink = addColumn(new ColumnInfo(COLUMN_PEAKS_LINK));
        ciLink.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                SimpleDisplayColumn dc = new SimpleDisplayColumn("(no peaks)");
                dc.setName(COLUMN_PEAKS_LINK);
                return dc;
            }
        });

        //add a condition that limits the features returned to just those existing in the
        //current container. The FilteredTable class supports this automatically only if
        //the underlying table contains a column named "Container," which our Features table
        //does not, so we need to use a SQL fragment here that uses a sub-select.
        SQLFragment sf = new SQLFragment("FileId IN (SELECT FileId FROM ms1.Files AS f INNER JOIN Exp.Data AS d ON (f.ExpDataFileId=d.RowId) WHERE d.Container=? AND f.Imported=? AND f.Deleted=?)",
                                            container.getId(), true, false);
        addCondition(sf, "FileId");

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
        visibleColumns.remove(FieldKey.fromParts(COLUMN_DETAILS_LINK));
        visibleColumns.remove(FieldKey.fromParts(COLUMN_PEAKS_LINK));
        visibleColumns.add(0, FieldKey.fromParts(COLUMN_DETAILS_LINK));
        visibleColumns.add(1, FieldKey.fromParts(COLUMN_PEAKS_LINK));
        visibleColumns.add(FieldKey.fromParts(COLUMN_PEPTIDE_INFO, "Peptide"));
        setDefaultVisibleColumns(visibleColumns);
    } //c-tor

    public void addRunIdCondition(int runId, Container container, ViewURLHelper urlBase, boolean peaksAvailable, boolean forExport)
    {
        _container = container;
        _runId = runId;
        _urlBase = urlBase;

        SQLFragment sf = new SQLFragment("FileId IN (SELECT FileId FROM ms1.Files AS f INNER JOIN Exp.Data AS d ON (f.ExpDataFileId=d.RowId) WHERE d.Container=? AND f.Imported=? AND d.RunId=?)",
                                            container.getId(), true, runId);
        getFilter().deleteConditions("FileId");
        addCondition(sf, "FileId");

        //if peak data is available...
        if(peaksAvailable)
        {
            //set the display column factory for the details and peaks links columns
            getColumn(COLUMN_DETAILS_LINK).setDisplayColumnFactory(new DisplayColumnFactory()
            {
                public DisplayColumn createRenderer(ColumnInfo colInfo)
                {
                    ViewURLHelper url = _urlBase.clone();
                    url.setAction("showFeatureDetails.view");
                    UrlColumn uc = new UrlColumn(StringExpressionFactory.create(url.getLocalURIString() + "&featureId=${FeatureId}"), "details");
                    uc.setName(COLUMN_DETAILS_LINK);
                    return uc;
                }
            });

            getColumn(COLUMN_PEAKS_LINK).setDisplayColumnFactory(new DisplayColumnFactory()
            {
                public DisplayColumn createRenderer(ColumnInfo colInfo)
                {
                    ViewURLHelper url = new ViewURLHelper(MS1Module.CONTROLLER_NAME, "showPeaks.view", _container);
                    url.addParameter("runId", _runId);
                    UrlColumn uc = new UrlColumn(StringExpressionFactory.create(url.getLocalURIString() + "&featureId=${FeatureId}"), "peaks");
                    uc.setName(COLUMN_PEAKS_LINK);
                    return uc;
                }
            });
        }
        else
        {
            //remove the details and peaks links from the default set
            ArrayList<FieldKey> visibleColumns = new ArrayList<FieldKey>(getDefaultVisibleColumns());
            visibleColumns.remove(FieldKey.fromParts(COLUMN_DETAILS_LINK));
            visibleColumns.remove(FieldKey.fromParts(COLUMN_PEAKS_LINK));
            setDefaultVisibleColumns(visibleColumns);
        }

        if(!forExport)
        {
            //make the ms2 scan a hyperlink to showPeptide view
            ViewURLHelper urlPep = new ViewURLHelper(MS1Module.CONTROLLER_NAME, "showMS2Peptide", container);
            DisplayColumnFactory factory = new DisplayColumnFactory()
            {
                public DisplayColumn createRenderer(ColumnInfo colInfo)
                {
                    DataColumn dataColumn = new DataColumn(colInfo);
                    dataColumn.setLinkTarget("peptide");
                    return dataColumn;
                }
            };

            ColumnInfo ciMS2Scan = getColumn("MS2Scan");
            ciMS2Scan.setURL(urlPep.getLocalURIString() + "featureId=${FeatureId}");
            ciMS2Scan.setDisplayColumnFactory(factory);
        }
    } //addRunIdCondition()

    // Protected Data Members
    protected MS1Schema _schema;
    protected Container _container;
    protected ViewURLHelper _urlBase;
    protected int _runId = 0;
} //class FeaturesTableInfo
