/*
 * Copyright (c) 2005-2016 LabKey Corporation
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

package org.labkey.flow.analysis.web;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.io.Serializable;

public class GraphSpec implements Serializable, Comparable<GraphSpec>
{
    final SubsetSpec _subset;
    final String[] _parameters;

    public GraphSpec(SubsetSpec subset, String ... parameters)
    {
        _subset = subset;
        _parameters = parameters;
    }

    public GraphSpec(SubsetSpec subset, Collection<String> parameters)
    {
        _subset = subset;
        _parameters = parameters.toArray(new String[parameters.size()]);
    }

    public GraphSpec(@NotNull String str)
    {
        if (str == null)
            throw new IllegalArgumentException("null graph spec");
        int ichParen = str.indexOf("(");
        if (ichParen < 0)
            throw new IllegalArgumentException("missing '('");
        if (!str.endsWith(")"))
            throw new IllegalArgumentException("must end with ')");
        _subset = SubsetSpec.fromEscapedString(str.substring(0, ichParen));
        String strParameters = str.substring(ichParen + 1, str.length() - 1);
        StringTokenizer st = new StringTokenizer(strParameters, ":");
        ArrayList<String> parameters = new ArrayList();
        while (st.hasMoreTokens())
        {
            parameters.add(st.nextToken());
        }
        _parameters = parameters.toArray(new String[0]);
    }

    public String toString()
    {
        StringBuilder ret = new StringBuilder();

        if (_subset != null)
            ret.append(_subset.toString());
        ret.append("(");
        for (int i = 0; i < _parameters.length; i ++)
        {
            if (i != 0)
                ret.append(":");
            ret.append(_parameters[i]);
        }
        ret.append(")");
        return ret.toString();
    }

    public String[] getParameters()
    {
        return _parameters;
    }

    public SubsetSpec getSubset()
    {
        return _subset;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof GraphSpec))
            return false;
        return toString().equals(other.toString());
    }

    public int hashCode()
    {
        return toString().hashCode();
    }

    @Override
    public int compareTo(@NotNull GraphSpec graph)
    {
        int ret = SubsetSpec.compare(getSubset(), graph.getSubset());
        if (ret != 0)
            return ret;
        return this.toString().compareTo(graph.toString());
    }
}
