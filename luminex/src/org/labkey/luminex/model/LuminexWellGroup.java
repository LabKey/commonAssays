/*
 * Copyright (c) 2011-2014 LabKey Corporation
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
package org.labkey.luminex.model;

import org.labkey.api.data.Container;
import org.labkey.api.data.ConvertHelper;
import org.labkey.api.study.Plate;
import org.labkey.api.study.Position;
import org.labkey.api.study.PositionImpl;
import org.labkey.api.study.WellGroup;
import org.labkey.luminex.LuminexReplicate;
import org.labkey.luminex.query.LuminexDataTable;

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
    private boolean _containsSummaryData = false;
    private boolean _containsRawData = false;

    public LuminexWellGroup(List<LuminexWell> wells)
    {
        _wells = wells;

        for (LuminexWell well : wells)
        {
            if (well.getDataRow().isSummary())
                setContainsSummaryData(true);
            else if (!well.getDataRow().isSummary())
                setContainsRawData(true);
        }
    }

    @Override
    public List<LuminexWell> getWellData(boolean combineReplicates)
    {
        if (!combineReplicates)
        {
            return _wells;
        }
        Map<LuminexReplicate, List<LuminexWell>> allReplicates = new HashMap<>();
        for (LuminexWell well : _wells)
        {
            // if there is both raw and summary data for this wellgroup, only use the raw data
            if (hasRawData() && hasSummaryData() && well.getDataRow().isSummary())
                continue;

            LuminexReplicate replicate = new LuminexReplicate(well);
            List<LuminexWell> wells = allReplicates.get(replicate);
            if (wells == null)
            {
                wells = new ArrayList<>();
                allReplicates.put(replicate, wells);
            }
            wells.add(well);
        }

        List<LuminexWell> result = new ArrayList<>();
        for (Map.Entry<LuminexReplicate, List<LuminexWell>> entry : allReplicates.entrySet())
        {
            double sumFi = 0;
            int countFi = 0;
            double sumFiBackground = 0;
            int countFiBackground = 0;
            double sumFiBackgroundNegative = 0;
            int countFiBackgroundNegative = 0;
            Double cv = null;
            String wellRole = null;
            boolean excluded = false;

            for (LuminexWell well : entry.getValue())
            {
                Double value = well.getDataRow().getFi();
                if (value != null)
                {
                    sumFi += value.doubleValue();
                    countFi++;
                }

                value = well.getDataRow().getFiBackground();
                if (value != null)
                {
                    sumFiBackground += value.doubleValue();
                    countFiBackground++;
                }

                Object fbb = well.getDataRow()._getExtraProperties().get("FIBackgroundNegative");
                if (fbb != null)
                {
                    value = Double.parseDouble(fbb.toString());
                    sumFiBackgroundNegative += value.doubleValue();
                    countFiBackgroundNegative++;
                }

                cv = well.getDataRow().getCv();
                wellRole = well.getDataRow().getWellRole();

                if (well.getDataRow()._getExtraProperties().get(LuminexDataTable.FLAGGED_AS_EXCLUDED_COLUMN_NAME) != null)
                    excluded = ConvertHelper.convert(well.getDataRow()._getExtraProperties().get(LuminexDataTable.FLAGGED_AS_EXCLUDED_COLUMN_NAME), Boolean.class);
            }
            LuminexDataRow fakeDataRow = new LuminexDataRow();
            fakeDataRow.setExpConc(entry.getKey().getExpConc());
            fakeDataRow.setDilution(entry.getKey().getDilution());
            fakeDataRow.setData(entry.getKey().getDataId());
            fakeDataRow.setDescription(entry.getKey().getDescription());
            if (countFi > 0)
                fakeDataRow.setFi(sumFi / countFi);
            if (countFiBackground > 0)
                fakeDataRow.setFiBackground(sumFiBackground / countFiBackground);
            if (countFiBackgroundNegative > 0)
                fakeDataRow.setFiBackgroundNegative(sumFiBackgroundNegative / countFiBackgroundNegative);
            fakeDataRow.setType(entry.getKey().getType());
            fakeDataRow.setExcluded(excluded);
            fakeDataRow.setCv(cv);
            fakeDataRow.setWellRole(wellRole);
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
        List<Position> result = new ArrayList<>();
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
        double result = Double.NEGATIVE_INFINITY;
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

    public boolean hasSummaryData()
    {
        return _containsSummaryData;
    }

    public void setContainsSummaryData(boolean containsSummaryData)
    {
        _containsSummaryData = containsSummaryData;
    }

    public boolean hasRawData()
    {
        return _containsRawData;
    }

    public void setContainsRawData(boolean containsRawData)
    {
        _containsRawData = containsRawData;
    }
}
