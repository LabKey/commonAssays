package org.labkey.ms2.pipeline;

import java.util.Collection;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.TreeSet;

/**
 * User: billnelson@uky.edu
 * Date: Oct 26, 2006
 * Time: 10:06:56 AM
 */
public abstract class Params
{
    TreeSet<Param> _params = new TreeSet<Param>();

    abstract void initProperties();

    public Collection<Param> getParams()
    {
        return _params;
    }

    public Param getParam(String name)
    {
        for (Param prop : _params)
        {
            String propName = prop.getName();
            if (name.equals(propName))
            {
                return prop;
            }
        }
        return null;
    }

    public Param startsWith(String prefix)
    {
        for (Param prop : _params)
        {
            if (prop.getName().startsWith(prefix))
            {
                return prop;
            }
        }
        return null;
    }

    public Collection<String> getInputXmlLabels()
    {
        HashSet<String> labels = new HashSet<String>();
        for (Param prop : _params)
        {
            labels.addAll(prop.getInputXmlLabels());
        }
        return labels;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        for (Param prop : getParams())
        {
            sb.append(prop.convert());
            sb.append(" ");
        }
        return sb.toString();
    }

}
