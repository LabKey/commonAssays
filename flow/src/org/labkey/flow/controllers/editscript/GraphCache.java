package org.labkey.flow.controllers.editscript;

import org.labkey.flow.gateeditor.client.model.GWTGraphOptions;
import org.labkey.flow.gateeditor.client.model.GWTGraphInfo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class GraphCache
{
    static final private String key = GraphCache.class.getName();
    transient private GWTGraphOptions graphOptions;
    transient private GWTGraphInfo graphInfo;
    transient private byte[] graphBytes;

    static public GraphCache get(HttpServletRequest request)
    {
        HttpSession session = request.getSession(true);
        Object oCache = session.getAttribute(key);
        if (oCache instanceof GraphCache)
        {
            return (GraphCache) oCache;
        }
        GraphCache ret = new GraphCache();
        session.setAttribute(key, ret);
        return ret;
    }

    public GWTGraphInfo getGraphInfo(GWTGraphOptions options)
    {
        if (!options.equals(this.graphOptions))
        {
            return null;
        }
        return graphInfo;
    }

    public byte[] getGraphBytes(GWTGraphOptions options)
    {
        if (!options.equals(this.graphOptions))
        {
            return null;
        }
        return graphBytes;
    }

    public void setGraphInfo(GWTGraphOptions options, GWTGraphInfo info, byte[] bytes)
    {
        this.graphOptions = options;
        this.graphInfo = info;
        this.graphBytes = bytes;
    }
}
