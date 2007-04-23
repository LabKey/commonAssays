package org.labkey.ms2.query;

import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.CoreSchema;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.ContainerTable;
import org.labkey.api.exp.api.ExpSchema;
import org.labkey.ms2.MS2Manager;

/**
 * User: jeckels
 * Date: Feb 22, 2007
 */
public class RunTableInfo extends FilteredTable
{
    private final MS2Schema _schema;

    public RunTableInfo(MS2Schema schema)
    {
        super(MS2Manager.getTableInfoRuns());
        _schema = schema;

        wrapAllColumns(true);

        ViewURLHelper url = new ViewURLHelper("core", "containerRedirect", "");
        url.addParameter("action", "showList.view");
        url.addParameter("pageflow", "MS2");
        LookupForeignKey containerFK = new LookupForeignKey(url, "containerId", "EntityId", "Name")
        {
            public TableInfo getLookupTableInfo()
            {
                return new ContainerTable();
            }
        };
        getColumn("Container").setFk(containerFK);
        getColumn("Container").setIsHidden(true);

        ColumnInfo folderColumn = wrapColumn("Folder", getRealTable().getColumn("Container"));
        folderColumn.setIsHidden(false);
        folderColumn.setFk(containerFK);
        addColumn(folderColumn);

        ColumnInfo erLSIDColumn = getColumn("ExperimentRunLSID");
        erLSIDColumn.setCaption("Experiment Run");
        erLSIDColumn.setFk(new LookupForeignKey("LSID")
        {
            public TableInfo getLookupTableInfo()
            {
                ExpSchema schema = new ExpSchema(_schema.getUser(), _schema.getContainer());
                return schema.createRunsTable(null);
            }
        });
    }
}
