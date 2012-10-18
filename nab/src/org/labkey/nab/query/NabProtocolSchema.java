package org.labkey.nab.query;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.AssayProvider;

/**
 * User: kevink
 * Date: 10/13/12
 */
public class NabProtocolSchema extends AssayProtocolSchema
{
    /*package*/ static final String DATA_ROW_TABLE_NAME = "Data";

    public NabProtocolSchema(User user, Container container, @NotNull ExpProtocol protocol, @NotNull AssayProvider provider, @Nullable Container targetStudy)
    {
        super(user, container, protocol, targetStudy);
    }

    @Override
    public NabRunDataTable createResultsTable()
    {
        return new NabRunDataTable(this, getProtocol());
    }
}
