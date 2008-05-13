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

package org.labkey.flow.gateeditor.client.ui;

import com.google.gwt.user.client.ui.TreeItem;
import org.labkey.flow.gateeditor.client.model.GWTPopulation;

public class PopulationItem extends TreeItem
{
    public GWTPopulation getPopulation()
    {
        return population;
    }

    public void setPopulation(GWTPopulation population)
    {
        this.population = population;
    }

    GWTPopulation population;
    public PopulationItem(GWTPopulation population)
    {
        super(population.getName());
        this.population = population;
        for (int i = 0; i < this.population.getPopulations().length; i++)
        {
            addItem(new PopulationItem(this.population.getPopulations()[i]));
        }
    }

    public PopulationItem findPopulationItem(GWTPopulation population)
    {
        if (population == this.population)
            return this;
        for (int i = 0; i < getChildCount(); i++)
        {
            TreeItem child = getChild(i);
            if (!(child instanceof PopulationItem))
                continue;
            return ((PopulationItem) child).findPopulationItem(population);
        }
        return null;
    }
}
