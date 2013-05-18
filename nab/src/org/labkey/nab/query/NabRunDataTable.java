/*
 * Copyright (c) 2007-2013 LabKey Corporation
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
import org.labkey.nab.SinglePlateNabDataHandler;

import java.util.*;

/**
 * User: brittp
 * Date: Jul 6, 2007
 * Time: 5:35:15 PM
 */
public class NabRunDataTable extends NabRunDataBaseTable
{
    public NabRunDataTable(final NabProtocolSchema schema, final ExpProtocol protocol)
    {
        super(schema, protocol);
        setDescription("Contains one row per data for the \"" + protocol.getName() + "\" Neutralizing Antibodies assay design.");
    }

    public PropertyDescriptor[] getExistingDataProperties(ExpProtocol protocol)
    {
        List<PropertyDescriptor>pds = NabProviderSchema.getExistingDataProperties(protocol, _schema.getCutoffValues());

        Collections.sort(pds, new Comparator<PropertyDescriptor>()
        {
            public int compare(PropertyDescriptor o1, PropertyDescriptor o2)
            {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return pds.toArray(new PropertyDescriptor[pds.size()]);        
    }

    public String getInputMaterialPropertyName()
    {
        return SinglePlateNabDataHandler.DILUTION_INPUT_MATERIAL_DATA_PROPERTY;
    }

    public String getDataRowLsidPrefix()
    {
        return SinglePlateNabDataHandler.NAB_DATA_ROW_LSID_PREFIX;
    }

    @Override
    protected Set<String> getHiddenColumns(ExpProtocol protocol)
    {
        Set<String> hiddenCols = super.getHiddenColumns(protocol);

        // hide the fit method specific values for curve IC and AUC
        for (PropertyDescriptor prop : getExistingDataProperties(protocol))
        {
            String propName = prop.getName();
            if (propName.startsWith(SinglePlateNabDataHandler.CURVE_IC_PREFIX) ||
                    propName.startsWith(SinglePlateNabDataHandler.AUC_PREFIX) ||
                    propName.startsWith(SinglePlateNabDataHandler.pAUC_PREFIX))
            {
                if (propName.indexOf('_') != -1)
                    hiddenCols.add(propName);
            }
        }
        return hiddenCols;
    }
}
