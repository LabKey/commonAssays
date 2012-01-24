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

package org.labkey.ms2.pipeline.client;

import org.labkey.ms2.pipeline.client.mascot.MascotInputXmlComposite;
import org.labkey.ms2.pipeline.client.mascot.MascotSequenceDbComposite;
import org.labkey.ms2.pipeline.client.mascot.MascotEnzymeComposite;
import org.labkey.ms2.pipeline.client.mascot.MascotResidueModComposite;
import org.labkey.ms2.pipeline.client.sequest.SequestInputXmlComposite;
import org.labkey.ms2.pipeline.client.sequest.SequestSequenceDbComposite;
import org.labkey.ms2.pipeline.client.sequest.SequestResidueModComposite;
import org.labkey.ms2.pipeline.client.tandem.XtandemInputXmlComposite;
import org.labkey.ms2.pipeline.client.tandem.XtandemSequenceDbComposite;
import org.labkey.ms2.pipeline.client.tandem.XtandemResidueModComposite;

/**
 * User: billnelson@uky.edu
 * Date: Apr 17, 2008
 */

/**
 * <code>SearchFormCompositeFactory</code>
 */
public class SearchFormCompositeFactory
{
    private static final String XTANDEM = "X! Tandem";
    private static final String MASCOT = "Mascot";
    private static final String SEQUEST = "Sequest";
    private String searchEngine;

    public SearchFormCompositeFactory(String searchEngine)
    {
        this.searchEngine = searchEngine;
    }

    public SequenceDbComposite getSequenceDbComposite(Search search)
    {
        if(searchEngine.equals(XTANDEM))
            return new XtandemSequenceDbComposite(search);
        else if(searchEngine.equals(MASCOT))
            return new MascotSequenceDbComposite(search);
        else if(searchEngine.equals(SEQUEST))
            return new SequestSequenceDbComposite(search);
        return null;
    }

    public InputXmlComposite getInputXmlComposite()
    {
        if(searchEngine.equals(XTANDEM))
            return new XtandemInputXmlComposite();
        else if(searchEngine.equals(MASCOT))
            return new MascotInputXmlComposite();
        else if(searchEngine.equals(SEQUEST))
            return new SequestInputXmlComposite();
        else
            return null;
    }

    public EnzymeComposite getEnzymeComposite()
    {
        if(searchEngine.equals(XTANDEM))
            return new EnzymeComposite();
        else if(searchEngine.equals(MASCOT))
            return new MascotEnzymeComposite();
        else if(searchEngine.equals(SEQUEST))
            return new EnzymeComposite();
        else
            return null;
    }

    public ResidueModComposite getResidueModComposite(Search searchForm)
    {
        if(searchEngine.equals(XTANDEM))
            return new XtandemResidueModComposite(searchForm);
        else if(searchEngine.equals(MASCOT))
            return new MascotResidueModComposite(searchForm);
        else if(searchEngine.equals(SEQUEST))
            return new SequestResidueModComposite(searchForm);
        else
            return null;
    }
}
