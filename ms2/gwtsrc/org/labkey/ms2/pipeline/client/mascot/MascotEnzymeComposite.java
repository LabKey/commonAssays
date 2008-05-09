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
