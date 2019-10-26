/*
 * Copyright (c) 2009-2019 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.Container;
import org.labkey.api.exp.SamplePropertyHelper;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.exp.property.DomainProperty;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @Override
    protected File getObject(int index, @NotNull Map<DomainProperty, String> sampleProperties, @NotNull Set<ExpMaterial> parentMaterials)
    {
        return _files.get(index);
    }

    protected boolean isCopyable(DomainProperty pd)
    {
        return !getNamePDs().contains(pd);
    }

    public static List<? extends DomainProperty> getProperties(@NotNull ExpSampleSet sampleSet, Container c)
    {
        return sampleSet.getDomain().getProperties();
    }

    public List<DomainProperty> getNamePDs()
    {
        return _sampleSet.getIdCols();
    }

}
