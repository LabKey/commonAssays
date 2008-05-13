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

public class GWTPopulationSet implements IsSerializable, Serializable
{
    static private final GWTPopulation[] EMPTY_LIST = new GWTPopulation[0];
    GWTPopulation[] populations;

    public GWTPopulationSet()
    {
        populations = EMPTY_LIST;
    }

    public GWTPopulation[] getPopulations()
    {
        return populations;
    }

    public void setPopulations(GWTPopulation[] populations)
    {
        this.populations = populations;
    }

    public GWTPopulation getPopulation(String name)
    {
        for (int i = 0; i < populations.length; i ++)
        {
            if (name.equals(populations[i]))
            {
                return populations[i];
            }
        }
        return null;
    }

    public GWTPopulation findPopulation(String fullName)
    {
        for (int i = 0; i < populations.length; i ++)
        {
            GWTPopulation ret = populations[i].findPopulation(fullName);
            if (ret != null)
                return ret;
        }
        return null;
    }

    public GWTPopulation[] clonePopulations()
    {
        GWTPopulation[] ret = new GWTPopulation[populations.length];
        for (int i = 0; i < populations.length; i ++)
        {
            ret[i] = populations[i].duplicate();
        }
        return ret;
    }
}
