/*
 * Copyright (c) 2008-2012 LabKey Corporation
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

package org.labkey.ms2.pipeline.client.mascot;

import org.labkey.ms2.pipeline.client.EnzymeComposite;

/**
 * User: billnelson@uky.edu
 * Date: Apr 29, 2008
 */

/**
 * <code>MascotEnzymeComposite</code>
 */
public class MascotEnzymeComposite extends EnzymeComposite
{
    public String setSelectedEnzyme(String enzymeSignature)
    {
        if(enzymeSignature == null) return "Cut site is equal to null.";
        int numEnz = enzymeListBox.getItemCount();
        boolean found = false;
        for(int i = 0; i < numEnz; i++)
        {
            if(enzymeSignature.equals(enzymeListBox.getValue(i)))
            {
                enzymeListBox.setSelectedIndex(i);
                found = true;
                break;
            }
        }
        if(found) return "";
        return "The enzyme '" + enzymeSignature + "' was not found.";
    }
}
