package org.labkey.ms1;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.Container;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;

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
    public PeaksTableInfo(Container container)
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

        //add a condition that limits the features returned to just those existing in the
        //current container. The FilteredTable class supports this automatically only if
        //the underlying table contains a column named "Container," which our Peaks table
        //does not, so we need to use a SQL fragment here that uses a sub-select.
        SQLFragment sf = new SQLFragment("ScanId IN (SELECT ScanId FROM ms1.Scans as s INNER JOIN ms1.Files AS f ON (s.FileId=f.FileId) INNER JOIN Exp.Data AS d ON (f.ExpDataFileId=d.RowId) WHERE d.Container=? AND f.Imported=?)",
                                            container.getId(), true);
        addCondition(sf, "ScanId");

    }

    public void addScanRangeCondition(int runId, int scanFirst, int scanLast, Container container)
    {
        SQLFragment sf = new SQLFragment("ScanId IN (SELECT ScanId FROM ms1.Scans as s INNER JOIN ms1.Files AS f ON (s.FileId=f.FileId) INNER JOIN Exp.Data AS d ON (f.ExpDataFileId=d.RowId) WHERE d.Container=? AND f.Imported=? AND d.RunId=? AND s.Scan BETWEEN ? AND ?)",
                                            container.getId(), true, runId, scanFirst, scanLast);

        getFilter().deleteConditions("ScanId");
        addCondition(sf, "ScanId");
    }
} //class PeaksTableInfo
