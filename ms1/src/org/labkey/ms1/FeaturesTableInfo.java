package org.labkey.ms1;

import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpSchema;
import org.labkey.api.view.ViewURLHelper;

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
        SQLFragment sf = new SQLFragment("FileId IN (SELECT FileId FROM ms1.Files AS f INNER JOIN Exp.Data AS d ON (f.ExpDataFileId=d.RowId) WHERE d.Container=?)",
                                            container.getId());
        addCondition(sf, "FileId");
    } //c-tor

    public void addRunIdCondition(int runId, Container container) throws SQLException
    {
        SQLFragment sf = new SQLFragment("FileId IN (SELECT FileId FROM ms1.Files AS f INNER JOIN Exp.Data AS d ON (f.ExpDataFileId=d.RowId) WHERE d.RunId=?)",
                                            runId);
        getFilter().deleteConditions("FileId");
        addCondition(sf, "FileId");

        //when limited to a run, we can make the scan number a link to the peaks view
        //provided peak data is available
        if(MS1Manager.get().isPeakDataAvailable(runId))
        {
            assert null != getColumn("Scan") : "Scan column not present in Features table info!";
            ViewURLHelper url = new ViewURLHelper(MS1Module.CONTROLLER_NAME, "showPeaks", container);
            String surl = url.getLocalURIString() + "runId=" + runId + "&scan=${scan}";
            getColumn("Scan").setURL(surl);
        }
    } //addRunIdCondition()

    // Protected Data Members
    protected ExpSchema _expSchema;
} //class FeaturesTableInfo
