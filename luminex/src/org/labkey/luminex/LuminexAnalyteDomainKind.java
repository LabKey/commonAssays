package org.labkey.luminex;

import org.labkey.api.exp.property.AssayDomainKind;
import org.labkey.api.exp.property.Domain;

import java.util.Set;

/**
 * User: jeckels
 * Date: Jan 27, 2012
 */
public class LuminexAnalyteDomainKind extends AssayDomainKind
{
    public LuminexAnalyteDomainKind()
    {
        super(LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE);
    }

    @Override
    public String getKindName()
    {
        return "Luminex Analytes";
    }

    @Override
    public Set<String> getReservedPropertyNames(Domain domain)
    {
        Set<String> result = getAssayReservedPropertyNames();
        result.add("Name");
        result.add("FitProb");
        result.add("Fit Prob");
        result.add("RegressionType");
        result.add("Regression Type");
        result.add("ResVar");
        result.add("Res Var");
        result.add("StdCurve");
        result.add("Std Curve");
        result.add("MinStandardRecovery");
        result.add("Min Standard Recovery");
        result.add("MaxStandardRecovery");
        result.add("Max Standard Recovery");
        return result;
    }
}
