/*
 * Copyright (c) 2007-2014 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.ms1.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.Container;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.ms1.MS1Manager;

import java.util.ArrayList;

/**
 * Represents a user schema table info over the peaks data
 *
 * User: Dave
 * Date: Oct 5, 2007
 * Time: 11:10:38 AM
 */
public class PeaksTableInfo extends FilteredTable<MS1Schema>
{
    public PeaksTableInfo(MS1Schema schema)
    {
        super(MS1Manager.get().getTable(MS1Manager.TABLE_PEAKS), schema);

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

        //tell query to use our user schema for scans
        ColumnInfo scanCol = getColumn("ScanId");
        scanCol.setFk(new LookupForeignKey("ScanId")
        {
            public TableInfo getLookupTableInfo()
            {
                setPrefixColumnCaption(false);
                return _userSchema.getScansTableInfo();
            }
        });

        //display only a subset by default
        ArrayList<FieldKey> visibleColumns = new ArrayList<>(getDefaultVisibleColumns());
        visibleColumns.remove(FieldKey.fromParts("PeakId"));
        visibleColumns.remove(FieldKey.fromParts("ScanId"));
        visibleColumns.add(0, FieldKey.fromParts("ScanId", "Scan"));
        visibleColumns.add(1, FieldKey.fromParts("PeakFamilies", "PeakFamilyId", "MZMono"));
        visibleColumns.add(2, FieldKey.fromParts("PeakFamilies", "PeakFamilyId", "Charge"));
        setDefaultVisibleColumns(visibleColumns);
        
        //mark the PeakId column as hidden
        getColumn("PeakId").setHidden(true);

        //add a condition that limits the features returned to just those existing in the
        //current container. The FilteredTable class supports this automatically only if
        //the underlying table contains a column named "Container," which our Peaks table
        //does not, so we need to use a SQL fragment here that uses a sub-select.
        SQLFragment sf = new SQLFragment("ScanId IN (SELECT ScanId FROM ms1.Scans as s INNER JOIN ms1.Files AS f ON (s.FileId=f.FileId) INNER JOIN Exp.Data AS d ON (f.ExpDataFileId=d.RowId) WHERE d.Container=? AND f.Imported=? AND f.Deleted=?)",
                                            _userSchema.getContainer().getId(), true, false);
        addCondition(sf, FieldKey.fromParts("ScanId"));

    }

    public void addScanRangeCondition(int runId, int scanFirst, int scanLast, Container container)
    {
        SQLFragment sf = new SQLFragment("ScanId IN (SELECT ScanId FROM ms1.Scans as s INNER JOIN ms1.Files AS f ON (s.FileId=f.FileId) INNER JOIN Exp.Data AS d ON (f.ExpDataFileId=d.RowId) WHERE d.Container=? AND f.Imported=? AND f.Deleted=? AND d.RunId=? AND s.Scan BETWEEN ? AND ?)",
                                            container.getId(), true, false, runId, scanFirst, scanLast);

        getFilter().deleteConditions(FieldKey.fromParts("ScanId"));
        addCondition(sf, FieldKey.fromParts("ScanId"));
    }
} //class PeaksTableInfo
