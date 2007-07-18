package org.labkey.luminex;

import org.labkey.api.study.DefaultAssayProvider;
import org.labkey.api.study.AssayDataCollector;
import org.labkey.api.study.FileUploadDataCollector;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.data.Container;

/**
 * User: jeckels
 * Date: Jul 13, 2007
 */
public class LuminexAssayProvider extends DefaultAssayProvider
{
    public LuminexAssayProvider()
    {
        super("LuminexAssayProtocol", "LuminexAssayRun", LuminexExcelDataHandler.LUMINEX_DATA_LSID_PREFIX);
    }

    public String getName()
    {
        return "Luminex";
    }

    public AssayDataCollector[] getDataCollectors()
    {
        return new AssayDataCollector[] { new FileUploadDataCollector() };
    }


    protected Domain createDataDomain(Container c)
    {
        Domain result = super.createDataDomain(c);

        DomainProperty firstProp = result.addProperty();
        firstProp.setLabel("First property");
        firstProp.setName("LP1");
        firstProp.setType(PropertyService.get().getType(c, PropertyType.STRING.getXmlName()));

        DomainProperty secondProp = result.addProperty();
        secondProp.setLabel("Second property");
        secondProp.setName("LP2");
        secondProp.setType(PropertyService.get().getType(c, PropertyType.BOOLEAN.getXmlName()));

        return result;
    }
}
