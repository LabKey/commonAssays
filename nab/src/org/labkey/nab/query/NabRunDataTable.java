/*
 * Copyright (c) 2007-2009 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.nab.query;

import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.study.assay.AssaySchema;
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
    public NabRunDataTable(final AssaySchema schema, final ExpProtocol protocol)
    {
        super(schema, protocol);
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
