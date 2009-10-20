package org.labkey.ms2.pipeline;

import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2RunType;

/**
 * User: jeckels
 * Date: Oct 19, 2009
 */
public class UnknownMS2Run extends MS2Run
{
    public String getChargeFilterColumnName()
    {
        return null;
    }

    public String getChargeFilterParamName()
    {
        return null;
    }

    public String getDiscriminateExpressions()
    {
        return null;
    }

    public String[] getGZFileExtensions()
    {
        return new String[0];
    }

    public String getParamsFileName()
    {
        return null;
    }

    protected String[] getPepXmlScoreNames()
    {
        return new String[0];
    }

    public MS2RunType getRunType()
    {
        return MS2RunType.Unknown;
    }
}
