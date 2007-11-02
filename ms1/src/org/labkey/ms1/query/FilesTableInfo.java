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

        //add a condition that limits the files returned to just those existing in the
        //current container. The FilteredTable class supports this automatically only if
        //the underlying table contains a column named "Container," which our Files table
        //does not, so we need to use a SQL fragment here that uses a sub-select.
        SQLFragment sf = new SQLFragment("Imported=? AND Deleted=? AND ExpDataFileId IN (SELECT RowId FROM Exp.Data WHERE Container=?)",
                                            true, false, container.getId());
        addCondition(sf, "FileId");

    }

    private ExpSchema _expSchema;
}
