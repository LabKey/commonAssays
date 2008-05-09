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
