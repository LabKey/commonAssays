package org.labkey.ms1;

import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.exp.api.ExpSchema;
import org.labkey.api.data.*;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.util.StringExpressionFactory;

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

        //tell query that Peaks joins to PeaksToFamilies so we can add the PeakFamily columns
        ColumnInfo peakFamCol = wrapColumn("PeakFamilies", getRealTable().getColumn("PeakId"));
        peakFamCol.setIsUnselectable(true);
        peakFamCol.setDescription("Link to the Peak Family information");
        peakFamCol.setFk(new LookupForeignKey("PeakId")
        {
            public TableInfo getLookupTableInfo()
            {
                setPrefixColumnCaption(false);
                return MS1Manager.get().getTable(MS1Manager.TABLE_PEAKS_TO_FAMILIES);
            }
        });
        addColumn(peakFamCol);

        TableInfo joinTable = peakFamCol.getFkTableInfo();
        joinTable.getColumn("PeakId").setIsHidden(true);
        joinTable.getColumn("PeakFamilyId").setCaption("Peak Family");
        TableInfo peakFamTable = joinTable.getColumn("PeakFamilyId").getFkTableInfo();
        peakFamTable.getColumn("MZMono").setCaption("MZ Monoisotopic");
        peakFamTable.getColumn("Charge").setCaption("Charge");

        //display only a subset by default
        ArrayList<FieldKey> visibleColumns = new ArrayList<FieldKey>(getDefaultVisibleColumns());
        visibleColumns.remove(FieldKey.fromParts("PeakId"));
        visibleColumns.remove(FieldKey.fromParts("ScanId"));
        visibleColumns.add(0, FieldKey.fromParts("ScanId", "Scan"));
        visibleColumns.add(1, FieldKey.fromParts("PeakFamilies", "PeakFamilyId", "MZMono"));
        visibleColumns.add(2, FieldKey.fromParts("PeakFamilies", "PeakFamilyId", "Charge"));
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

    public void addScanRangeCondition(int scanIdFirst, int scanIdLast)
    {
        SQLFragment sql = new SQLFragment("ScanId BETWEEN ? AND ?", scanIdFirst, scanIdLast);
        addCondition(sql, "ScanId");
    }
} //class PeaksTableInfo
