package org.labkey.ms2.query;

import org.labkey.api.data.TableInfo;
import org.labkey.api.data.CoreSchema;
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
    public ProteinProphetFileTableInfo()
    {
        super(MS2Manager.getTableInfoProteinProphetFiles());
        wrapAllColumns(true);

        ViewURLHelper url = new ViewURLHelper("MS2", "showRun.view", "");
        getColumn("Run").setFk(new LookupForeignKey(url, "run", "Run", "Description")
        {
            public TableInfo getLookupTableInfo()
            {
                FilteredTable result = new FilteredTable(MS2Manager.getTableInfoRuns());
                result.wrapAllColumns(true);
                ViewURLHelper url = new ViewURLHelper("core", "containerRedirect", "");
                url.addParameter("action", "showList.view");
                url.addParameter("pageflow", "MS2");
                result.getColumn("Container").setFk(new LookupForeignKey(url, "containerId", "EntityId", "Name")
                {
                    public TableInfo getLookupTableInfo()
                    {
                        return CoreSchema.getInstance().getTableInfoContainers();
                    }
                });
                return result;
            }
        });
    }
}
