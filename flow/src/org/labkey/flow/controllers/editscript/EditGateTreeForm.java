/*
 * Copyright (c) 2008-2011 LabKey Corporation
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

package org.labkey.flow.controllers.editscript;

import org.labkey.api.util.UnexpectedException;
import org.labkey.flow.analysis.model.Population;
import org.labkey.flow.analysis.web.SubsetSpec;

import java.util.Map;

/**
 * User: kevink
* Date: Nov 27, 2008 11:28:44 AM
*/
public class EditGateTreeForm extends EditScriptForm
{
    public SubsetSpec[] subsets;
    public String[] populationNames;

    @Override
    public void reset()
    {
        super.reset();
        try
        {
            Map<SubsetSpec,Population> pops = getPopulations();
            Map.Entry<SubsetSpec,Population>[] entries = pops.entrySet().toArray(new Map.Entry[0]);
            populationNames = new String[entries.length];
            subsets = new SubsetSpec[entries.length];
            for (int i = 0; i < entries.length; i ++)
            {
                populationNames[i] = entries[i].getValue().getName().getRawName();
                subsets[i] = entries[i].getKey();
            }
        }
        catch (Exception e)
        {
            UnexpectedException.rethrow(e);
        }
    }
    public String[] getPopulationNames()
    {
        return populationNames;
    }
}
