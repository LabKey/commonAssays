package org.labkey.luminex;

import org.labkey.api.data.Container;
import org.labkey.api.study.Plate;
import org.labkey.api.study.Position;
import org.labkey.api.study.PositionImpl;
import org.labkey.api.study.WellData;
import org.labkey.api.study.WellGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * User: jeckels
 * Date: Aug 17, 2011
 */
public class LuminexWellGroup implements WellGroup
{
    private List<LuminexWell> _wells;

    public LuminexWellGroup(List<LuminexWell> wells)
    {
        _wells = wells;
    }

    @Override
    public List<LuminexWell> getWellData(boolean combineReplicates)
    {
        return _wells;
    }

    @Override
    public Type getType()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(Position position)
    {
        return getPositions().contains(position);
    }

    @Override
    public Set<WellGroup> getOverlappingGroups()
    {
        return Collections.emptySet();
    }

    @Override
    public Set<WellGroup> getOverlappingGroups(Type type)
    {
        return Collections.emptySet();
    }

    @Override
    public List<Position> getPositions()
    {
        List<Position> result = new ArrayList<Position>();
        for (LuminexWell well : _wells)
        {
            String wellNames = well.getDataRow().getWell();
            for (String wellName : wellNames.split(","))
            {
                result.add(new PositionImpl(well.getDataRow().getContainer(), wellName));
            }
        }
        return result;
    }

    @Override
    public Double getMinDilution()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Double getMaxDilution()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integer getRowId()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName()
    {
        return _wells.get(0)._dataRow.getDescription();
    }

    @Override
    public String getPositionDescription()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getPropertyNames()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProperty(String name, Object value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getProperty(String name)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Container getContainer()
    {
        return _wells.get(0)._dataRow.getContainer();
    }

    @Override
    public String getLSID()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Plate getPlate()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getStdDev()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getMax()
    {
        double result = Double.MIN_VALUE;
        for (LuminexWell well : _wells)
        {
            if (well.getMax() > result)
            {
                result = well.getMax();
            }
        }
        return result;
    }

    @Override
    public double getMin()
    {
        double result = Double.MAX_VALUE;
        for (LuminexWell well : _wells)
        {
            if (well.getMin() < result)
            {
                result = well.getMin();
            }
        }
        return result;
    }

    @Override
    public double getMean()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Double getDilution()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDilution(Double dilution)
    {
        throw new UnsupportedOperationException();
    }
}
