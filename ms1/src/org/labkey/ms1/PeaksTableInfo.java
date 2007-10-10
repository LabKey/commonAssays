package org.labkey.ms1;

import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.FieldKey;
import org.labkey.api.exp.api.ExpSchema;
import org.labkey.api.data.Container;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.TableInfo;

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
    public PeaksTableInfo()
    {
        super(MS1Manager.get().getTable(MS1Manager.TABLE_PEAKS));

        //wrap all the columns
        wrapAllColumns(true);

        //but only display a subset by default
        ArrayList<FieldKey> visibleColumns = new ArrayList<FieldKey>(getDefaultVisibleColumns());
        visibleColumns.remove(FieldKey.fromParts("PeakId"));
        visibleColumns.remove(FieldKey.fromParts("ScanId"));
        setDefaultVisibleColumns(visibleColumns);
        
        //mark the PeakId column as hidden
        getColumn("PeakId").setIsHidden(true);

        //rename ScanId to something nicer
        //hide ScanId on Scans and rename FileId to something nicer
        ColumnInfo ciScan = getColumn("ScanId");
        ciScan.setCaption("Scan");
        TableInfo tiScans = ciScan.getFkTableInfo();
        tiScans.getColumn("FileId").setCaption("Data File");
        tiScans.getColumn("ScanId").setIsHidden(true);
    }

    public void addScanCondition(int scanId)
    {
        addCondition(getRealTable().getColumn("ScanId"), scanId);
    }

} //class PeaksTableInfo
