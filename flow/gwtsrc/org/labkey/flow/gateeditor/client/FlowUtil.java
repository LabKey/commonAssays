package org.labkey.flow.gateeditor.client;

import org.labkey.api.gwt.client.util.PropertyUtil;

public class FlowUtil
{
    static public String flowResource(String filename)
    {
        return PropertyUtil.getContextPath() + "/Flow/" + filename;
    }
    static public String _gif()
    {
        return PropertyUtil.getContextPath() + "/_.gif";

    }
}
