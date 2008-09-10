/*
 * Copyright (c) 2008 LabKey Corporation
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

package org.labkey.microarray;

import org.labkey.api.gwt.server.BaseRemoteService;
import org.labkey.api.view.ViewContext;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.security.ACL;
import org.labkey.microarray.sampleset.client.SampleSetService;
import org.labkey.microarray.sampleset.client.model.GWTSampleSet;
import org.labkey.microarray.sampleset.client.model.GWTMaterial;

import java.util.List;
import java.util.ArrayList;

/**
 * User: jeckels
 * Date: Feb 8, 2008
 */
public class SampleSetServiceImpl extends BaseRemoteService implements SampleSetService
{
    public SampleSetServiceImpl(ViewContext context)
    {
        super(context);
    }

    public GWTSampleSet[] getSampleSets()
    {
        ExpSampleSet[] sets = ExperimentService.get().getSampleSets(getContainer(), true);
        GWTSampleSet[] result = new GWTSampleSet[sets.length];
        for (int i = 0; i < sets.length; i++)
        {
            ExpSampleSet set = sets[i];
            GWTSampleSet gwtSet = new GWTSampleSet(set.getName(), set.getLSID());
            gwtSet.setRowId(set.getRowId());
            List<String> columnNames = new ArrayList<String>();
            for (PropertyDescriptor propertyDescriptor : set.getPropertiesForType())
            {
                columnNames.add(propertyDescriptor.getName());
            }
            gwtSet.setColumnNames(columnNames);
            result[i] = gwtSet;
        }
        return result;
    }

    public GWTMaterial[] getMaterials(GWTSampleSet gwtSet)
    {
        ExpSampleSet set = ExperimentService.get().getSampleSet(gwtSet.getRowId());
        if (set == null)
        {
            return null;
        }

        if (!set.getContainer().hasPermission(getUser(), ACL.PERM_READ))
        {
            return null;
        }

        ExpMaterial[] materials = set.getSamples();
        GWTMaterial[] result = new GWTMaterial[materials.length];
        for (int i = 0; i < materials.length; i++)
        {
            ExpMaterial material = materials[i];
            GWTMaterial gwtMaterial = new GWTMaterial();
            result[i] = gwtMaterial;
            gwtMaterial.setLsid(material.getLSID());
            gwtMaterial.setRowId(material.getRowId());
            gwtMaterial.setName(material.getName());
        }
        return result;
    }
}
