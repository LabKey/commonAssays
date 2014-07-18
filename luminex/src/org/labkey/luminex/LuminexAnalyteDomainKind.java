/*
 * Copyright (c) 2012-2014 LabKey Corporation
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
        result.add(LuminexDataHandler.POSITIVITY_THRESHOLD_COLUMN_NAME);
        result.add(LuminexDataHandler.POSITIVITY_THRESHOLD_DISPLAY_NAME);
        result.add(LuminexDataHandler.NEGATIVE_BEAD_COLUMN_NAME);
        result.add(LuminexDataHandler.NEGATIVE_BEAD_DISPLAY_NAME);
        return result;
    }
}
