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
