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

    public SequenceDbComposite getSequenceDbComposite()
    {
        if(searchEngine.equals(XTANDEM))
            return new XtandemSequenceDbComposite();
        else if(searchEngine.equals(MASCOT))
            return new MascotSequenceDbComposite();
        else if(searchEngine.equals(SEQUEST))
            return new SequestSequenceDbComposite();
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
