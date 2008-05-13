/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

public class GWTPopulation extends GWTPopulationSet implements IsSerializable, Serializable
{
    String name;
    String fullName;
    GWTGate gate;

    public GWTGate getGate()
    {
        return gate;
    }

    public void setGate(GWTGate gate)
    {
        this.gate = gate;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getFullName()
    {
        return fullName;
    }

    public void setFullName(String fullName)
    {
        this.fullName = fullName;
    }

    public String getParentFullName()
    {
        int ichLastSlash = getFullName().lastIndexOf("/");
        if (ichLastSlash < 0)
            return null;
        return getFullName().substring(0, ichLastSlash);
    }


    public GWTPopulation findPopulation(String fullName)
    {
        if (this.fullName.equals(fullName))
            return this;
        return super.findPopulation(fullName);
    }

    public GWTPopulation duplicate()
    {
        GWTPopulation that = new GWTPopulation();
        that.name = this.name;
        that.fullName = this.fullName;
        that.gate = this.gate;
        that.populations = clonePopulations();
        return that;
    }

    public boolean isIncomplete()
    {
        if (gate instanceof GWTPolygonGate)
        {
            GWTPolygonGate polyGate = (GWTPolygonGate) gate;
            return polyGate.length() == 0;
        }
        return false;
    }
}
