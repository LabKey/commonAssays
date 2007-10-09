package org.labkey.ms1;

import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.FieldKey;
import org.labkey.api.exp.api.ExpSchema;
import org.labkey.api.data.Container;
import org.labkey.api.data.SQLFragment;

import java.util.ArrayList;

/**
 * Represents a user schema table info over the peaks data
 *
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Oct 5, 2007
 * Time: 11:10:38 AM
 */
public class PeaksTableInfo extends FilteredTable
{
    //Localizable Strings
    public static final String CAPTION_PEAKS_FILE = "Peaks File";
    public static final String CAPTION_EXP_DATA_FILE = "Experiment Data File";

    public PeaksTableInfo(ExpSchema expSchema, Container container)
    {
        super(MS1Manager.get().getTable(MS1Manager.TABLE_PEAKS));
        _expSchema = expSchema;

        //wrap all the columns
        wrapAllColumns(true);

        //but only display a subset by default
        ArrayList<FieldKey> visibleColumns = new ArrayList<FieldKey>(getDefaultVisibleColumns());
        visibleColumns.remove(FieldKey.fromParts("PeakID"));
        visibleColumns.remove(FieldKey.fromParts("PeaksFileID"));
        visibleColumns.remove(FieldKey.fromParts("Scan"));
        setDefaultVisibleColumns(visibleColumns);

        //mark the PeakID column as hidden
        getColumn("PeakID").setIsHidden(true);

        //rename the PeaksFileID to something nicer
        getColumn("PeaksFileID").setCaption(CAPTION_PEAKS_FILE);
        
        //add a condition that limits the peaks returned to just those existing in the
        //current container. The FilteredTable class supports this automatically only if
        //the underlying table contains a column named "Container," which our Peaks table
        //does not, so we need to use a SQL fragment here that uses a sub-select.
        SQLFragment sf = new SQLFragment("PeaksFileID IN (SELECT PeaksFileID FROM ms1.PeaksFiles AS f INNER JOIN Exp.Data AS d ON (f.ExpDataFileID=d.RowId) WHERE d.Container=?)",
                                            container.getId());
        addCondition(sf, "PeaksFileID");
    }

    public void addScanCondition(int runId, int scan)
    {
        SQLFragment sf = new SQLFragment("PeaksFileID IN (SELECT PeaksFileID FROM ms1.FeaturesFiles AS f INNER JOIN Exp.Data AS d ON (f.ExpDataFileID=d.RowId) INNER JOIN Exp.ExperimentRun AS r ON (d.RunId=r.RowId) WHERE r.RowId=?)",
                                            runId);
        addCondition(sf, "PeaksFileID");
        addCondition(getRealTable().getColumn("Scan"), scan);
    }

    private ExpSchema _expSchema;
} //class PeaksTableInfo
