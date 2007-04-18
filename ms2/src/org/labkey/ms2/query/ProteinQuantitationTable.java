package org.labkey.ms2.query;

import org.labkey.api.query.FilteredTable;
import org.labkey.ms2.MS2Manager;

/**
 * User: jeckels
 * Date: Apr 17, 2007
 */
public class ProteinQuantitationTable extends FilteredTable
{
    public ProteinQuantitationTable()
    {
        super(MS2Manager.getTableInfoProteinQuantitation());
        wrapAllColumns(true);
    }
}
