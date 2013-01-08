/*
 * Copyright (c) 2011-2012 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.elispot.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.elispot.ElispotDataHandler;
import org.labkey.elispot.ElispotProtocolSchema;

/**
 * User: klum
 * Date: Jan 27, 2011
 * Time: 1:45:11 PM
 */
public class ElispotRunAntigenTable extends ElispotRunDataTable
{
    public ElispotRunAntigenTable(final AssaySchema schema, final ExpProtocol protocol)
    {
        super(schema, protocol);
        setDescription("Contains one row per well for the \"" + protocol.getName() + "\" ELISpot assay design.");
    }

    public PropertyDescriptor[] getExistingDataProperties(ExpProtocol protocol)
    {
        return ElispotProtocolSchema.getExistingDataProperties(protocol, ElispotDataHandler.ELISPOT_ANTIGEN_PROPERTY_LSID_PREFIX);
    }

    public String getInputMaterialPropertyName()
    {
        return ElispotDataHandler.ELISPOT_INPUT_MATERIAL_DATA_PROPERTY;
    }

    public String getDataRowLsidPrefix()
    {
        return ElispotDataHandler.ELISPOT_ANTIGEN_ROW_LSID_PREFIX;  
    }

    @Override
    protected ColumnInfo resolveColumn(String name)
    {
        ColumnInfo result = super.resolveColumn(name);

        if ("Properties".equalsIgnoreCase(name))
        {
            // Hook up a column that joins back to this table so that the columns formerly under the Properties
            // node can still be queried there.
            result = wrapColumn("Properties", getRealTable().getColumn("ObjectId"));
            result.setIsUnselectable(true);
            LookupForeignKey fk = new LookupForeignKey("ObjectId")
            {
                @Override
                public TableInfo getLookupTableInfo()
                {
                    return new ElispotRunAntigenTable(_userSchema, _protocol);
                }
            };
            fk.setPrefixColumnCaption(false);
            result.setFk(fk);
        }

        return result;
    }
}
