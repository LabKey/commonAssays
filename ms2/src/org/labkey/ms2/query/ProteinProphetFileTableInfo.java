package org.labkey.ms2.query;

import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.ms2.MS2Manager;

/**
 * User: jeckels
 * Date: Feb 8, 2007
 */
public class ProteinProphetFileTableInfo extends FilteredTable
{
    private final MS2Schema _schema;

    public ProteinProphetFileTableInfo(MS2Schema schema)
    {
        super(MS2Manager.getTableInfoProteinProphetFiles());
        _schema = schema;
        wrapAllColumns(true);

        ViewURLHelper url = new ViewURLHelper("MS2", "showRun.view", schema.getContainer());
        getColumn("Run").setFk(new LookupForeignKey(url, "run", "Run", "Description")
        {
            public TableInfo getLookupTableInfo()
            {
                return new RunTableInfo(_schema);
            }
        });
    }
}
