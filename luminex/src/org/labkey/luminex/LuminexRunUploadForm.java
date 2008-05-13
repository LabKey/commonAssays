/*
 * Copyright (c) 2007 LabKey Corporation
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

package org.labkey.luminex;

import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.PropertyType;

import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Arrays;

/**
 * User: jeckels
 * Date: Aug 8, 2007
 */
public class LuminexRunUploadForm extends AssayRunUploadForm
{
    private int _dataId;

    public int getDataId()
    {
        return _dataId;
    }

    public void setDataId(int dataId)
    {
        _dataId = dataId;
    }


    protected Map<PropertyDescriptor, String> getAnalytePropertyMapFromRequest(List<PropertyDescriptor> columns, int analyteId)
    {
        Map<PropertyDescriptor, String> properties = new LinkedHashMap<PropertyDescriptor, String>();
        for (PropertyDescriptor pd : columns)
        {
            String propName = getFormElementName(pd);
            String value = getRequest().getParameter("_analyte_" + analyteId + "_" + propName);
            if (pd.isRequired() && pd.getPropertyType() == PropertyType.BOOLEAN &&
                    (value == null || value.length() == 0))
                value = Boolean.FALSE.toString();
            properties.put(pd, value);
        }
        return properties;
    }

    public Map<PropertyDescriptor, String> getAnalyteProperties(int analyteId)
    {
        List<PropertyDescriptor> propertyDescriptors = Arrays.asList(AbstractAssayProvider.getPropertiesForDomainPrefix(getProtocol(), LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE));
        return getAnalytePropertyMapFromRequest(propertyDescriptors, analyteId);
    }
}
