/*
 * Copyright (c) 2008-2014 LabKey Corporation
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

package org.labkey.elispot;

import org.labkey.api.study.WellGroupTemplate;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.exp.SamplePropertyHelper;
import org.labkey.api.exp.property.DomainProperty;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * User: Karl Lum
 * Date: Jan 14, 2008
 */
public class PlateAntigenPropertyHelper extends SamplePropertyHelper<String>
{
    private List<String> _antigenNames;
    private final PlateTemplate _template;

    public PlateAntigenPropertyHelper(List<? extends DomainProperty> antigenDomainProperties, PlateTemplate template)
    {
        super(antigenDomainProperties);
        _template = template;
        _antigenNames = new ArrayList<>();

        if (template != null)
        {
            for (WellGroupTemplate wellgroup : template.getWellGroups())
            {
                if (wellgroup.getType() == WellGroup.Type.ANTIGEN)
                {
                    _antigenNames.add(wellgroup.getName());
                }
            }
        }
    }

    protected String getObject(int index, Map<DomainProperty, String> sampleProperties)
    {
        int i = 0;
        for (WellGroupTemplate wellgroup : _template.getWellGroups())
        {
            if (wellgroup.getType() == WellGroup.Type.ANTIGEN)
            {
                if (i == index)
                {
                    return wellgroup.getName();
                }
                i++;
            }
        }
        throw new IndexOutOfBoundsException("Requested #" + index + " but there were only " + i + " well group templates");
    }

    protected boolean isCopyable(DomainProperty pd)
    {
        return !AbstractAssayProvider.SPECIMENID_PROPERTY_NAME.equals(pd.getName()) && !AbstractAssayProvider.PARTICIPANTID_PROPERTY_NAME.equals(pd.getName());
    }


    public List<String> getSampleNames()
    {
        return _antigenNames;
    }

}
