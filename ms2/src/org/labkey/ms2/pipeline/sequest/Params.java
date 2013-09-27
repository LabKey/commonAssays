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

package org.labkey.ms2.pipeline.sequest;

import java.util.Collection;
import java.util.HashSet;
import java.util.TreeSet;

/**
 * User: billnelson@uky.edu
 * Date: Oct 26, 2006
 * Time: 10:06:56 AM
 */
public abstract class Params
{
    TreeSet<Param> _params = new TreeSet<>();

    protected abstract void initProperties();

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
        HashSet<String> labels = new HashSet<>();
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
            sb.append(prop.convert(getCommentPrefix()));
            sb.append(" ");
        }
        return sb.toString();
    }

    protected String getCommentPrefix()
    {
        return ";";
    }

}
