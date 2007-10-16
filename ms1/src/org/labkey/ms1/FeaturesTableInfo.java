package org.labkey.ms1;

import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.data.*;
import org.labkey.api.exp.api.ExpSchema;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.util.StringExpressionFactory;

import java.util.ArrayList;
import java.sql.SQLException;

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
    //Localizable Strings
    public static final String CAPTION_FEATURES_FILE = "Features File";
    public static final String CAPTION_EXP_DATA_FILE = "Experiment Data File";

    public FeaturesTableInfo(ExpSchema expSchema, Container container)
    {
        super(MS1Manager.get().getTable(MS1Manager.TABLE_FEATURES));

        _expSchema = expSchema;
        _container = container;

        //wrap all the columns
        wrapAllColumns(true);

        //but only display a subset by default
        ArrayList<FieldKey> visibleColumns = new ArrayList<FieldKey>(getDefaultVisibleColumns());
        visibleColumns.remove(FieldKey.fromParts("FeatureId"));
        visibleColumns.remove(FieldKey.fromParts("FileId"));
        visibleColumns.remove(FieldKey.fromParts("Description"));
        setDefaultVisibleColumns(visibleColumns);

        //mark the FeatureId column as hidden
        getColumn("FeatureId").setIsHidden(true);

        //rename the FileId to something nicer
        getColumn("FileId").setCaption(CAPTION_FEATURES_FILE);

        //tell it that ms1.Files.FileId is a foreign key to exp.Data.RowId
        TableInfo fftinfo = getColumn("FileId").getFkTableInfo();
        fftinfo.getColumn("FileId").setIsHidden(true);
        ColumnInfo ffid = fftinfo.getColumn("ExpDataFileId");
        ffid.setCaption(CAPTION_EXP_DATA_FILE);
        ffid.setFk(new LookupForeignKey("RowId")
        {
            public TableInfo getLookupTableInfo()
            {
                return _expSchema.createDatasTable(null);
            }
        });

        //add a condition that limits the features returned to just those existing in the
        //current container. The FilteredTable class supports this automatically only if
        //the underlying table contains a column named "Container," which our Features table
        //does not, so we need to use a SQL fragment here that uses a sub-select.
        SQLFragment sf = new SQLFragment("FileId IN (SELECT FileId FROM ms1.Files AS f INNER JOIN Exp.Data AS d ON (f.ExpDataFileId=d.RowId) WHERE d.Container=? AND f.Imported=?)",
                                            container.getId(), true);
        addCondition(sf, "FileId");
    } //c-tor

    public void addRunIdCondition(int runId, Container container, boolean peaksAvailable)
    {
        _container = container;
        _runId = runId;

        SQLFragment sf = new SQLFragment("FileId IN (SELECT FileId FROM ms1.Files AS f INNER JOIN Exp.Data AS d ON (f.ExpDataFileId=d.RowId) WHERE d.Container=? AND f.Imported=? AND d.RunId=?)",
                                            container.getId(), true, runId);
        getFilter().deleteConditions("FileId");
        addCondition(sf, "FileId");

        //if peak data is available...
        if(peaksAvailable)
        {
            //add a new column info for the feature details link
            ColumnInfo cinfo = new ColumnInfo("Details Link");
            cinfo.setDescription("Link to details about the Feature");
            cinfo.setDisplayColumnFactory(new DisplayColumnFactory()
            {
                public DisplayColumn createRenderer(ColumnInfo colInfo)
                {
                    ViewURLHelper url = new ViewURLHelper(MS1Module.CONTROLLER_NAME, "showFeatureDetails.view", _container);
                    url.addParameter("runId", _runId);
                    return new UrlColumn(StringExpressionFactory.create(url.getLocalURIString() + "&featureId=${FeatureId}"), "details");
                }
            });
            addColumn(cinfo);

            //add a new column info for the peaks link
            cinfo = new ColumnInfo("Peaks Link");
            cinfo.setDescription("Link to detailed Peak data for the Feature");
            cinfo.setDisplayColumnFactory(new DisplayColumnFactory()
            {
                public DisplayColumn createRenderer(ColumnInfo colInfo)
                {
                    ViewURLHelper url = new ViewURLHelper(MS1Module.CONTROLLER_NAME, "showPeaks.view", _container);
                    url.addParameter("runId", _runId);
                    return new UrlColumn(StringExpressionFactory.create(url.getLocalURIString() + "&scan=${scan}"), "peaks");
                }
            });
            addColumn(cinfo);


            //move it to the front of the visible column set
            ArrayList<FieldKey> visibleColumns = new ArrayList<FieldKey>(getDefaultVisibleColumns());
            visibleColumns.remove(FieldKey.fromParts("Details Link"));
            visibleColumns.remove(FieldKey.fromParts("Peaks Link"));
            visibleColumns.add(0, FieldKey.fromParts("Details Link"));
            visibleColumns.add(1, FieldKey.fromParts("Peaks Link"));
            setDefaultVisibleColumns(visibleColumns);
        }

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

    } //addRunIdCondition()

    // Protected Data Members
    protected ExpSchema _expSchema;
    protected Container _container;
    protected int _runId = 0;
} //class FeaturesTableInfo
