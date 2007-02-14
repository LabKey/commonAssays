package org.labkey.ms2.query;

import org.labkey.api.data.TableInfo;
import org.labkey.api.data.CoreSchema;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.exp.api.ExpSchema;
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

                ColumnInfo erLSIDColumn = result.getColumn("ExperimentRunLSID");
                erLSIDColumn.setCaption("Experiment Run");
                erLSIDColumn.setFk(new LookupForeignKey("LSID")
                {
                    public TableInfo getLookupTableInfo()
                    {
                        ExpSchema schema = new ExpSchema(_schema.getUser(), _schema.getContainer());
                        return schema.createRunsTable(null);
                    }
                });

                return result;
            }
        });
    }
}
