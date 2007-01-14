package org.labkey.ms2.protein.tools;


import org.jfree.data.general.DefaultPieDataset;

import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

/**
 * User: tholzman
 * Date: Oct 31, 2005
 * Time: 4:30:14 PM
 */
public class ProteinPieDataset extends DefaultPieDataset
{
    public HashMap<String, HashSet<Integer>> getExtraInfo()
    {
        return extraInfo;
    }

    public void setExtraInfo(HashMap<String, HashSet<Integer>> extraInfo)
    {
        this.extraInfo = extraInfo;
    }

    protected HashMap<String, HashSet<Integer>> extraInfo = new HashMap<String, HashSet<Integer>>();

    public ProteinPieDataset()
    {
        super();
    }
}
