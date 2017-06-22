/*
 * Copyright (c) 2008-2017 LabKey Corporation
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

package org.labkey.ms2.metadata;

import org.labkey.api.data.Container;
import org.labkey.api.exp.*;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.exp.property.Domain;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.io.File;

/**
 * User: peter@labkey.com
 * Date: Oct 3, 2007
 */
public class MsFractionPropertyHelper extends SamplePropertyHelper<File>
{
    private List<String> _names;
    private List<File>_files;
    private final @NotNull ExpSampleSet _sampleSet;

    public MsFractionPropertyHelper(@NotNull ExpSampleSet sampleSet, List<File> files, Container c)
    {
        super(getProperties(sampleSet, c));
        _sampleSet = sampleSet;
        _files = files;
        _names = new ArrayList<>();
        for (File file : files)
        {
            String fName = file.getName();
            _names.add(fName.substring(0, fName.lastIndexOf('.')));
        }
    }

    public List<String> getSampleNames()
    {
        return _names;
    }

    protected File getObject(int index, Map<DomainProperty, String> sampleProperties) throws DuplicateMaterialException
    {
        return _files.get(index);
    }

    protected boolean isCopyable(DomainProperty pd)
    {
        return !getNamePDs().contains(pd);
    }

    public static List<? extends DomainProperty> getProperties(@NotNull ExpSampleSet sampleSet, Container c)
    {
        if (sampleSet.getType() != null)
        {
            return sampleSet.getType().getProperties();
        }
        else
        {
            Domain d = PropertyService.get().createDomain(c, sampleSet.getLSID(), sampleSet.getName());
            DomainProperty namePD = d.addProperty();
            namePD.setName("Name");
            namePD.setLabel("Name");
            namePD.setType(PropertyService.get().getType(c, PropertyType.STRING.getXmlName()));
            namePD.setRequired(true);
            return Collections.singletonList(namePD);
        }
    }

    public List<DomainProperty> getNamePDs()
    {
        return _sampleSet.getIdCols();
    }

}
