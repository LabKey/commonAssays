package org.labkey.microarray;

import org.labkey.api.exp.ExperimentRunFilter;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.microarray.assay.MicroarrayAssayProvider;

/**
 * User: jeckels
* Date: Feb 7, 2008
*/
class MicroarrayRunFilter extends ExperimentRunFilter
{
    public static final MicroarrayRunFilter INSTANCE = new MicroarrayRunFilter();

    private MicroarrayRunFilter()
    {
        super("Microarray", MicroarraySchema.SCHEMA_NAME, MicroarraySchema.TABLE_RUNS);
    }

    public Priority getPriority(ExpProtocol protocol)
    {
        if (MicroarrayAssayProvider.PROTOCOL_PREFIX.equals(new Lsid(protocol.getLSID()).getNamespacePrefix()))
        {
            return Priority.HIGH;
        }
        return null;
    }
}
