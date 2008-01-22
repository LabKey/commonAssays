package org.labkey.nab.query;

import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.study.query.PlateBasedAssayRunDataTable;
import org.labkey.nab.NabDataHandler;

import java.sql.SQLException;

/**
 * User: brittp
 * Date: Jul 6, 2007
 * Time: 5:35:15 PM
 */
public class NabRunDataTable extends PlateBasedAssayRunDataTable
{
    public NabRunDataTable(final QuerySchema schema, String alias, final ExpProtocol protocol)
    {
        super(schema, alias, protocol);
    }

    public PropertyDescriptor[] getExistingDataProperties(ExpProtocol protocol) throws SQLException
    {
        return NabSchema.getExistingDataProperties(protocol);
    }

    public String getInputMaterialPropertyName()
    {
        return NabDataHandler.NAB_INPUT_MATERIAL_DATA_PROPERTY;
    }

    public String getDataRowLsidPrefix()
    {
        return NabDataHandler.NAB_DATA_ROW_LSID_PREFIX;
    }
}
