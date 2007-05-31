package org.labkey.flow.gateeditor.client;

import org.labkey.api.gwt.client.util.PropertyUtil;

public class FlowUtil
{
    static public String flowResource(String filename)
    {
        String contextPath = PropertyUtil.getContextPath();
        if (contextPath == null)
        {
            contextPath = "";
        }
        return contextPath + "/Flow/" + filename;
    }
    static public String _gif()
    {
        String contextPath = PropertyUtil.getContextPath();
        if (contextPath == null)
        {
            contextPath = "";
        }
        return contextPath + "/_.gif";

    }
}
