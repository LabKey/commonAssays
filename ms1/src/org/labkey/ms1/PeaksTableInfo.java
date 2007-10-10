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
    public PeaksTableInfo(ExpSchema expSchema, Container container)
    {
        super(MS1Manager.get().getTable(MS1Manager.TABLE_PEAKS));
        _expSchema = expSchema;

        //wrap all the columns
        wrapAllColumns(true);

        //but only display a subset by default
        ArrayList<FieldKey> visibleColumns = new ArrayList<FieldKey>(getDefaultVisibleColumns());
        visibleColumns.remove(FieldKey.fromParts("PeakId"));
        visibleColumns.remove(FieldKey.fromParts("ScanId"));
        setDefaultVisibleColumns(visibleColumns);
        
        //mark the PeakId column as hidden
        getColumn("PeakId").setIsHidden(true);
        getColumn("ScanId").setIsHidden(true);
    }

    public void addScanCondition(int scanId)
    {
        addCondition(getRealTable().getColumn("ScanId"), scanId);
    }

    private ExpSchema _expSchema;
} //class PeaksTableInfo
