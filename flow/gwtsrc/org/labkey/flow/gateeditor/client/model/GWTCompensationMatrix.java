/*
 * Copyright (c) 2007 LabKey Corporation
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

package org.labkey.flow.gateeditor.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

public class GWTCompensationMatrix implements IsSerializable, Serializable
{
    public static String PREFIX = "<";
    public static String SUFFIX = ">";

    int compId;
    String name;
    String label;
    String[] parameterNames;


    public String[] getParameterNames()
    {
        return parameterNames;
    }

    public void setParameterNames(String[] parameterNames)
    {
        this.parameterNames = parameterNames;
    }

    public int getCompId()
    {
        return compId;
    }

    public void setCompId(int compId)
    {
        this.compId = compId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof GWTCompensationMatrix)) return false;

        GWTCompensationMatrix that = (GWTCompensationMatrix) o;

        if (compId != that.compId) return false;

        return true;
    }

    public int hashCode()
    {
        return compId;
    }


}
