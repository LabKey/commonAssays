package org.labkey.ms2.pipeline.client;

import org.labkey.ms2.pipeline.client.mascot.MascotInputXmlComposite;
import org.labkey.ms2.pipeline.client.sequest.SequestInputXmlComposite;
import org.labkey.ms2.pipeline.client.tandem.XtandemInputXmlComposite;
import org.labkey.ms2.pipeline.client.tandem.XtandemSequenceDbComposite;

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

    public SequenceDbComposite getSequenceDbComposite()
    {
        if(searchEngine.equals(XTANDEM))
            return new XtandemSequenceDbComposite();
        else
            return new SequenceDbComposite(); 
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
}
