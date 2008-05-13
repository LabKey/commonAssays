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

package org.labkey.ms2.pipeline.client.sequest;

import org.labkey.ms2.pipeline.client.SequenceDbComposite;

import java.util.List;

import com.google.gwt.user.client.ui.ChangeListener;

/**
 * User: billnelson@uky.edu
 * Date: Apr 22, 2008
 */

/**
 * <code>SequestSequenceDbComposite</code>
 */
public class SequestSequenceDbComposite extends SequenceDbComposite
{

    public SequestSequenceDbComposite()
    {
        super();
        init();
    }

    public void setTaxonomyListBoxContents(List taxonomyList)
    {
        //No Mascot style taxonomy in Sequest
    }

    public String getSelectedTaxonomy()
    {
        //No Mascot style taxonomy in Sequest
        return null;
    }

    public String setDefaultTaxonomy(String name)
    {
        //No Mascot style taxonomy in Sequest
        return null;
    }

    public void addTaxonomyChangeListener(ChangeListener listener) {
       //No Mascot style taxonomy in Sequest
    }

    public void removeTaxonomyChangeListener(ChangeListener listener) {
        //No Mascot style taxonomy in Sequest
    }
}
