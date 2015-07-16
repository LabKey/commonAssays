package org.labkey.nab.query;

import org.labkey.api.assay.dilution.DilutionManager;
import org.labkey.api.exp.api.ExpProtocol;

/**
 * Created by davebradlee on 7/10/15.
 *
 */
public class NabDilutionDataTable extends NabBaseTable
{
    public NabDilutionDataTable(final NabProtocolSchema schema, ExpProtocol protocol)
    {
        super(schema, DilutionManager.getTableInfoDilutionData(), protocol);
        wrapAllColumns(true);
    }
}
