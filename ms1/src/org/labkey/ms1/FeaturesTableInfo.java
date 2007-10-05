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
        visibleColumns.remove(FieldKey.fromParts("FeatureID"));
        visibleColumns.remove(FieldKey.fromParts("FeaturesFileID"));
        visibleColumns.remove(FieldKey.fromParts("Description"));
        setDefaultVisibleColumns(visibleColumns);

        //mark the FeatureID column as hidden
        getColumn("FeatureID").setIsHidden(true);

        //rename the FeaturesFileID to something nicer
        getColumn("FeaturesFileID").setCaption(CAPTION_FEATURES_FILE);

        //tell it that ms1.FeaturesFiles.FeaturesFileID is a foreign key to exp.Data.RowId
        TableInfo fftinfo = getColumn("FeaturesFileID").getFkTableInfo();
        ColumnInfo ffid = fftinfo.getColumn("ExpDataFileID");
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
        SQLFragment sf = new SQLFragment("FeaturesFileID IN (SELECT FeaturesFileID FROM ms1.FeaturesFiles AS f INNER JOIN Exp.Data AS d ON (f.ExpDataFileID=d.RowId) WHERE d.Container=?)",
                                            container.getId());
        addCondition(sf, "FeaturesFileID");
    } //c-tor

    public void addRunIdCondition(int runId, Container container)
    {
        SQLFragment sf = new SQLFragment("FeaturesFileID IN (SELECT FeaturesFileID FROM ms1.FeaturesFiles AS f INNER JOIN Exp.Data AS d ON (f.ExpDataFileID=d.RowId) INNER JOIN Exp.ExperimentRun AS r ON (d.RunId=r.RowId) WHERE r.RowId=?)",
                                            runId);
        addCondition(sf, "FeaturesFileID");

        //when limited to a run, we can make the scan number a link to the peaks view
        assert null != getColumn("Scan") : "Scan column not present in Features table info!";
        ViewURLHelper url = new ViewURLHelper(MS1Module.CONTROLLER_NAME, "showPeaks", container);
        String surl = url.getLocalURIString() + "runId=" + runId + "&scan=${Scan}";
        getColumn("Scan").setURL(surl);

    } //addRunIdCondition()

    // Protected Data Members
    protected ExpSchema _expSchema;
} //class FeaturesTableInfo
