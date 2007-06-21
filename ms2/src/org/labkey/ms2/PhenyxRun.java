package org.labkey.ms2;

import java.util.Map;

/**
 * User: adam
 * Date: Jun 11, 2007
 * Time: 2:14:26 PM
 */
public class PhenyxRun extends MS2Run
{
    @Override
    public void adjustScores(Map<String, String> map)
    {
        if (null == map.get("bogus"))
            map.put("bogus", "87");  // TODO: Get rid of this
    }

    public MS2RunType getRunType()
    {
        return MS2RunType.Phenyx;
    }

    public String getParamsFileName()
    {
        return "phenyx.xml";
    }

    public String getChargeFilterColumnName()
    {
        return "ZScore";
    }

    public String getChargeFilterParamName()
    {
        return "zScore";
    }

    public String getDiscriminateExpressions()
    {
        return null;
    }

    protected String getPepXmlScoreNames()
    {
        return "origScore, bogus, zscore";
    }

    public String[] getGZFileExtensions()
    {
        return new String[0];
    }
}
