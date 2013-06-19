/*
 * Copyright (c) 2006-2013 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.flow.data;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class SampleKey
{
    List<Object> _values;

    public SampleKey()
    {
        _values = new ArrayList<>();
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
