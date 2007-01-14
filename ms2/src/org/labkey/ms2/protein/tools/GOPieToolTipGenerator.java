package org.labkey.ms2.protein.tools;

import org.jfree.chart.labels.PieToolTipGenerator;
import org.jfree.data.general.PieDataset;

/**
 * User: tholzman
 * Date: Oct 24, 2005
 * Time: 3:40:25 PM
 */
public class GOPieToolTipGenerator implements PieToolTipGenerator
{

    public String generateToolTip(PieDataset pieDataset, Comparable comparable)
    {
        String skey = (String) comparable;
        if (skey == null) return null;
        if (skey.equalsIgnoreCase("other")) return "Others: too few members for individual slices";
        if (skey.length() < 10 || !skey.startsWith("GO:")) return skey;
        String acc = skey.substring(0, 10).trim();
        String tip = null;
        try
        {
            tip = ProteinDictionaryHelpers.getGODefinitionFromAcc(acc);
        }
        catch (Exception e)
        {
        }
        if (tip == null) return "'" + skey + "' is an unknown or defunct category";
        return tip;
    }
}
