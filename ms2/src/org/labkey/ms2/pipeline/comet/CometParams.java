package org.labkey.ms2.pipeline.comet;

import org.labkey.ms2.pipeline.sequest.AbstractSequestParams;

/**
 * User: jeckels
 * Date: 9/24/13
 */
public class CometParams extends AbstractSequestParams
{
    public CometParams()
    {
        super(Variant.comet);
    }

    protected String getCommentPrefix()
    {
        return "#";
    }

}
