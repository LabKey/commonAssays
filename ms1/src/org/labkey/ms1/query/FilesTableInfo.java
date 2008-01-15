package org.labkey.ms1.query;

import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.exp.api.ExpSchema;
import org.labkey.api.data.Container;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.SQLFragment;
import org.labkey.ms1.MS1Manager;

/**
 * User schema table info for the ms1.Files table
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Oct 23, 2007
 * Time: 2:01:34 PM
 */
public class FilesTableInfo extends FilteredTable
{
    public FilesTableInfo(ExpSchema expSchema, Container container)
    {
        super(MS1Manager.get().getTable(MS1Manager.TABLE_FILES));

        _expSchema = expSchema;
        wrapAllColumns(true);

        getColumn("FileId").setIsHidden(true);
        ColumnInfo edfid = getColumn("ExpDataFileId");
        edfid.setFk(new LookupForeignKey("RowId")
        {
            public TableInfo getLookupTableInfo()
            {
                return _expSchema.createDatasTable(null);
            }
        });

        //add a condition that excludes deleted and not full-imported files
        //also limit to the passed container if not null
        SQLFragment sf;
        if(null != container)
            sf = new SQLFragment("Imported=? AND Deleted=? AND ExpDataFileId IN (SELECT RowId FROM Exp.Data WHERE Container=?)",
                                            true, false, container.getId());
        else
            sf = new SQLFragment("Imported=? AND Deleted=?", true, false);
        addCondition(sf, "FileId");

    }

    private ExpSchema _expSchema;
}
