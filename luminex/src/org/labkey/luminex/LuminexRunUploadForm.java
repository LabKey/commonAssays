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
