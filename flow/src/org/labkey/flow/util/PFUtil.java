package org.labkey.flow.util;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.util.AppProps;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ViewController;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.module.ModuleLoader;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class PFUtil
{
    static private final Logger _log = Logger.getLogger(PFUtil.class);

    static public String getPageFlowName(Enum e)
    {
        return getPageFlowName(e.getClass().getPackage());
    }

    static public String getPageFlowName(Class<? extends ViewController> controller)
    {
        return getPageFlowName(controller.getPackage());
    }

    static private String getPageFlowName(Package p)
    {
        return ModuleLoader.getPageFlowForPackage(p);
    }


    static public ViewURLHelper urlFor(Enum action, String containerPath)
    {
        return new ViewURLHelper(getPageFlowName(action), action.toString(), containerPath);
    }

    static public ViewURLHelper urlFor(Enum action, Container container)
    {
        return urlFor(action, container.getPath());
    }

    static public String helpTopic(Enum action)
    {
        return getPageFlowName(action) + "/" + action.toString();
    }

    static private String h(Object o)
    {
        return PageFlowUtil.filter(o);
    }

    static public <T> String strSelect(String selectName, Map<T,String> map, T current)
    {
        return strSelect(selectName, map.keySet(), map.values(), current);
    }

    static public String strSelect(String selectName, Collection<? extends Object> values, Collection<String> labels, Object current)
    {
        if (values.size() != labels.size())
            throw new IllegalArgumentException();
        StringBuilder ret = new StringBuilder();
        ret.append("<select name=\"");
        ret.append(h(selectName));
        ret.append("\">");
        boolean found = false;
        Iterator itValue;
        Iterator<String> itLabel;
        for (itValue  = values.iterator(), itLabel = labels.iterator();
             itValue.hasNext() && itLabel.hasNext();)
        {
            Object value = itValue.next();
            String label = itLabel.next();
            boolean selected = !found && ObjectUtils.equals(current, value);
            ret.append("\n<option value=\"");
            ret.append(h(value));
            ret.append("\"");
            if (selected)
            {
                ret.append(" SELECTED");
                found = true;
            }
            ret.append(">");
            ret.append(h(label));
            ret.append("</option>");
        }
        ret.append("</select>");
        return ret.toString();
    }

    /**
     * Returns a string with the html for a &lt;select> with a given label.
     * If the select would contain only 0 or 1 elements, then just outputs
     * a hidden field to remember the current value. 
     */

    static public String strChooser(String label, String name, Object current, Collection<? extends Object> values, Collection<String> labels)
    {
        if (values.size() < 2)
        {
            if (current == null)
                return "";
            return strHiddenField(name, current);
        }
        StringBuilder ret = new StringBuilder();
        ret.append(label);
        ret.append(strSelect(name, values, labels, current));
        return ret.toString();
    }

    static public String strChooser(String label, String name, Object current, Map<? extends Object, String> entries)
    {
        return strChooser(label, name, current, entries.keySet(), entries.values());
    }

    static public String strHiddenField(String name, Object value)
    {
        return "<input type=\"hidden\" name=\"" + h(name) + "\" value=\"" + h(value) + "\">";
    }

    static public void close(Closeable closeable)
    {
        if (closeable == null)
            return;
        try
        {
            closeable.close();
        }
        catch (IOException e)
        {
            _log.error("Error in close", e);
        }
    }

    static public String getResourceAsString(Class clazz, String resource)
    {
        InputStream is = null;
        try
        {
            is = clazz.getResourceAsStream(resource);
            if (is == null)
                return null;
            return PageFlowUtil.getStreamContentsAsString(is);
        }
        finally
        {
            close(is);
        }
    }

    static public String _gif()
    {
        return _gif(1, 1);
    }
    static public String _gif(int height, int width)
    {
        StringBuilder ret = new StringBuilder();
        ret.append("<img src=\"");
        ret.append(AppProps.getInstance().getContextPath());
        ret.append("/_.gif\" height=\"");
        ret.append(height);
        ret.append("\" width=\"");
        ret.append(width);
        ret.append("\">");
        return ret.toString();        
    }
}
