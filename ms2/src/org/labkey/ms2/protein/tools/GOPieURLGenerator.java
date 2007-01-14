package org.labkey.ms2.protein.tools;

import org.jfree.chart.urls.StandardPieURLGenerator;
import org.jfree.data.general.PieDataset;
import org.labkey.api.util.PageFlowUtil;

import java.util.HashMap;
import java.util.HashSet;

/**
 * User: tholzman
 * Date: Oct 31, 2005
 * Time: 4:52:16 PM
 */
public class GOPieURLGenerator extends StandardPieURLGenerator
{
    public String getUrlPrefix()
    {
        return urlPrefix;
    }

    public void setUrlPrefix(String urlPrefix)
    {
        this.urlPrefix = urlPrefix;
    }

    protected String urlPrefix;

    public GOPieURLGenerator(String urlPrefix)
    {
        this.urlPrefix = urlPrefix;
    }

    private GOPieURLGenerator()
    {
        super();
    }

    public String generateURL(
            PieDataset dataset,
            Comparable key,
            int pieIndex
    )
    {
        HashMap<String, HashSet<Integer>> extra = ((ProteinPieDataset) dataset).getExtraInfo();
        if (extra == null) return null;
        HashSet<Integer> sqids = extra.get(key);
        if (sqids == null) return null;
        String sqdstr = "";
        for (Integer I : sqids)
        {
            sqdstr += I.toString() + ",";
        }
        if (sqdstr.endsWith(",")) sqdstr = sqdstr.substring(0, sqdstr.length() - 1);
        String retVal = urlPrefix + "?" + "sliceTitle=" + key.toString().replace(' ', '+') + "&sqids=" + sqdstr;
        return PageFlowUtil.filter(retVal);
    }
}

