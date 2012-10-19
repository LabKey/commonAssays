package org.labkey.elisa;

import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.elisa.query.ElisaResultsTable;

/**
 * User: jeckels
 * Date: 10/19/12
 */
public class ElisaProtocolSchema extends AssayProtocolSchema
{
    public ElisaProtocolSchema(User user, Container container, ExpProtocol protocol, Container targetStudy)
    {
        super(user, container, protocol, targetStudy);
    }

    @Override
    public FilteredTable createDataTable(boolean includeCopiedToStudyColumns)
    {
        return new ElisaResultsTable(this, includeCopiedToStudyColumns);
    }
}
