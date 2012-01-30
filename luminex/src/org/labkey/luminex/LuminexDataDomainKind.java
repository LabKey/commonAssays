package org.labkey.luminex;

import org.labkey.api.exp.property.AssayDomainKind;
import org.labkey.api.exp.property.Domain;

import java.util.Set;

/**
 * User: jeckels
 * Date: Jan 27, 2012
 */
public class LuminexDataDomainKind extends AssayDomainKind
{
    public LuminexDataDomainKind()
    {
        super(LuminexAssayProvider.ASSAY_DOMAIN_CUSTOM_DATA);
    }

    @Override
    public String getKindName()
    {
        return "Luminex Results";
    }

    @Override
    public Set<String> getReservedPropertyNames(Domain domain)
    {
        Set<String> result = getAssayReservedPropertyNames();
        result.addAll(LuminexSchema.getTableInfoDataRow().getColumnNameSet());
        return result;
    }
}
