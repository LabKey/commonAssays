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
package org.labkey.elisa.actions;

import org.labkey.api.exp.SamplePropertyHelper;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.Position;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.WellGroupTemplate;
import org.labkey.elisa.ElisaAssayProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * User: klum
 * Date: 10/14/12
 */
public class PlateConcentrationPropertyHelper extends SamplePropertyHelper<WellGroupTemplate>
{
    private Set<String> _controlNames;
    private final PlateTemplate _template;

    public PlateConcentrationPropertyHelper(List<? extends DomainProperty> domainProperties, PlateTemplate template)
    {
        super(domainProperties);
        _template = template;
        _controlNames = new TreeSet<>();

        if (template != null)
        {
            Map<String, Position> controls = new HashMap<>();
            for (WellGroupTemplate group : template.getWellGroups())
            {
                if (group.getType() == WellGroup.Type.CONTROL)
                {
                    for (Position position : group.getPositions())
                        controls.put(position.getDescription(), position);
                }
            }

            for (WellGroupTemplate group : template.getWellGroups())
            {
                if (group.getType() == WellGroup.Type.REPLICATE)
                {
                    for (Position position : group.getPositions())
                    {
                        if (controls.containsKey(position.getDescription()))
                            _controlNames.add(group.getPositionDescription());
                    }
                }
            }
        }
    }

    protected WellGroupTemplate getObject(int index, Map<DomainProperty, String> sampleProperties)
    {
        int i = 0;
        for (WellGroupTemplate wellgroup : _template.getWellGroups())
        {
            if (wellgroup.getType() == WellGroup.Type.CONTROL)
            {
                if (i == index)
                {
                    return wellgroup;
                }
                i++;
            }
        }
        throw new IndexOutOfBoundsException("Requested #" + index + " but there were only " + i + " well group templates");
    }

    protected boolean isCopyable(DomainProperty pd)
    {
        return false;
    }

    public List<String> getSampleNames()
    {
        return Arrays.asList(_controlNames.toArray(new String[_controlNames.size()]));
    }

    @Override
    public void setDomainProperties(List<? extends DomainProperty> domainProperties)
    {
        for (DomainProperty prop : domainProperties)
        {
            // only interested in the concentration property
            if (ElisaAssayProvider.CONCENTRATION_PROPERTY_NAME.equals(prop.getName()))
            {
                domainProperties = Collections.singletonList(prop);
                break;
            }
        }
        super.setDomainProperties(domainProperties);
    }
}
