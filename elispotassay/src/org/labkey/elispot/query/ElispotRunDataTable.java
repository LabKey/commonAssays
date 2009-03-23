/*
 * Copyright (c) 2008 LabKey Corporation
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

package org.labkey.elispot.query;

import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.UserSchema;
import org.labkey.api.study.query.PlateBasedAssayRunDataTable;
import org.labkey.elispot.ElispotDataHandler;
import org.labkey.elispot.ElispotSchema;

import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: Karl Lum
 * Date: Jan 21, 2008
 */
public class ElispotRunDataTable extends PlateBasedAssayRunDataTable
{
    public ElispotRunDataTable(final UserSchema schema, final ExpProtocol protocol)
    {
        super(schema, protocol);
    }

    public PropertyDescriptor[] getExistingDataProperties(ExpProtocol protocol) throws SQLException
    {
        return ElispotSchema.getExistingDataProperties(protocol);
    }

    public String getInputMaterialPropertyName()
    {
        return ElispotDataHandler.ELISPOT_INPUT_MATERIAL_DATA_PROPERTY;
    }

    public String getDataRowLsidPrefix()
    {
        return ElispotDataHandler.ELISPOT_DATA_ROW_LSID_PREFIX;
    }
}
