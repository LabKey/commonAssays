package org.labkey.flow.data;

import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class SampleKey
{
    List<Object> _values;

    public SampleKey()
    {
        _values = new ArrayList();
    }

    public void addValue(Object value)
    {
        if (value instanceof String)
        {
            try
            {
                value = Double.valueOf((String) value);
            }
            catch (NumberFormatException nfe)
            {
                // do nothing
            }
        }
        else if (value instanceof Number)
        {
            value = ((Number) value).doubleValue();
        }
        _values.add(value);
    }

    public void addUniqueValue()
    {
        _values.add(new Object());
    }

    public int hashCode()
    {
        return _values.hashCode();
    }

    public boolean equals(Object other)
    {
        if (other == this)
            return true;
        if (other.getClass() != getClass())
            return false;
        SampleKey that = (SampleKey) other;
        return _values.equals(that._values);
    }

    public String toString()
    {
        return _values.toString();
    }

    public String toName()
    {
        return StringUtils.join(_values.iterator(), "-");
    }
}
