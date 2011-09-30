/*
 * Copyright (c) 2011 LabKey Corporation
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
package org.labkey.luminex;

import org.labkey.api.data.Container;
import org.labkey.api.study.Plate;
import org.labkey.api.study.Position;
import org.labkey.api.study.PositionImpl;
import org.labkey.api.study.WellGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        if (!combineReplicates)
        {
            return _wells;
        }
        Map<LuminexReplicate, List<LuminexWell>> allReplicates = new HashMap<LuminexReplicate, List<LuminexWell>>();
        for (LuminexWell well : _wells)
        {
            LuminexReplicate replicate = new LuminexReplicate(well);
            List<LuminexWell> wells = allReplicates.get(replicate);
            if (wells == null)
            {
                wells = new ArrayList<LuminexWell>();
                allReplicates.put(replicate, wells);
            }
            wells.add(well);
        }

        List<LuminexWell> result = new ArrayList<LuminexWell>();
        for (Map.Entry<LuminexReplicate, List<LuminexWell>> entry : allReplicates.entrySet())
        {
            double sum = 0;
            int count = 0;
            for (LuminexWell well : entry.getValue())
            {
                Double value = well.getValue();
                if (value != null)
                {
                    sum += value.doubleValue();
                    count++;
                }
            }
            LuminexDataRow fakeDataRow = new LuminexDataRow();
            fakeDataRow.setExpConc(entry.getKey().getExpConc());
            fakeDataRow.setDilution(entry.getKey().getDilution());
            fakeDataRow.setData(entry.getKey().getDataId());
            fakeDataRow.setDescription(entry.getKey().getDescription());
            fakeDataRow.setFiBackground(sum / count);
            fakeDataRow.setFi(sum / count);
            fakeDataRow.setType(entry.getKey().getType());
            result.add(new LuminexWell(fakeDataRow));
        }
        Collections.sort(result);
        return result;
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
