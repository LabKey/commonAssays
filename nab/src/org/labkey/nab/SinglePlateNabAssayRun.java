/*
 * Copyright (c) 2010-2015 LabKey Corporation
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
package org.labkey.nab;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.assay.dilution.DilutionDataHandler;
import org.labkey.api.assay.dilution.DilutionManager;
import org.labkey.api.assay.dilution.DilutionSummary;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.data.statistics.StatsService;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.study.Plate;
import org.labkey.api.study.PlateService;
import org.labkey.api.study.Position;
import org.labkey.api.study.WellGroup;
import org.labkey.api.assay.dilution.DilutionAssayProvider;
import org.labkey.api.study.assay.AbstractPlateBasedAssayProvider;
import org.labkey.api.study.assay.AssayProtocolSchema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: brittp
 * Date: Sep 2, 2010 11:43:49 AM
 */
public class SinglePlateNabAssayRun extends NabAssayRun
{
    protected Plate _plate;
    private DilutionSummary[] _dilutionSummaries;
    public static final String SAMPLE_WELL_GROUP_NAME = "SampleWellGroupName";

    private Map<Position, WellGroup> _positionToVirusMap = new HashMap<>();
    private Map<String, Map<WellGroup, WellGroup>> _virusGroupToControlMap = new HashMap<>();

    public SinglePlateNabAssayRun(DilutionAssayProvider provider, ExpRun run, Plate plate,
                                  User user, List<Integer> cutoffs, StatsService.CurveFitType renderCurveFitType)
    {
        super(provider, run, user, cutoffs, renderCurveFitType);
        _plate = plate;
        List<? extends WellGroup> specimenGroups = _plate.getWellGroups(WellGroup.Type.SPECIMEN);
        _dilutionSummaries = getDilutionSumariesForWellGroups(specimenGroups);
    }

    @Override
    public DilutionSummary[] getSummaries()
    {
        return _dilutionSummaries;
    }

    @Override
    public List<Plate> getPlates()
    {
        return Collections.singletonList(_plate);
    }

    @Override
    protected DilutionSummary[] getDilutionSumariesForWellGroups(List<? extends WellGroup> specimenGroups)
    {
        List<WellGroup> specimenVirusGroups = new ArrayList<>();
        for (WellGroup specimenGroup : specimenGroups)
        {
            // for the multi virus case, we need to produce dilution summaries over the intersection of the
            // sample and virus well groups
            Set<WellGroup> virusGroups = specimenGroup.getOverlappingGroups(WellGroup.Type.VIRUS);
            if (!virusGroups.isEmpty())
            {
                for (WellGroup virusGroup : virusGroups)
                {
                    List<Position> specimenVirusPos = new ArrayList<>();
                    for (Position position : virusGroup.getPositions())
                    {
                        if (specimenGroup.contains(position))
                            specimenVirusPos.add(position);
                    }
                    WellGroup specimenVirusGroup = PlateService.get().createWellGroup(_plate,
                            DilutionDataHandler.getSpecimenVirusWellgroupName(specimenGroup, virusGroup),
                            WellGroup.Type.SPECIMEN, specimenVirusPos);

                    // transfer the sample properties to the new sample/virus well group so dilutions can be computed
                    for (String propName : specimenGroup.getPropertyNames())
                        specimenVirusGroup.setProperty(propName, specimenGroup.getProperty(propName));
                    specimenVirusGroup.setProperty(SAMPLE_WELL_GROUP_NAME, specimenGroup.getName());
                    specimenVirusGroup.setProperty(AbstractPlateBasedAssayProvider.VIRUS_WELL_GROUP_NAME, virusGroup.getName());

                    specimenVirusGroups.add(specimenVirusGroup);
                }
            }
            else
                specimenVirusGroups.add(specimenGroup);
        }

        return super.getDilutionSumariesForWellGroups(specimenVirusGroups);
    }


    @Override
    public ExpMaterial getMaterial(WellGroup wellgroup)
    {
        ExpMaterial material = super.getMaterial(wellgroup);

        if (material == null)
        {
            String sampleWellgroupName = wellgroup.getProperty(SAMPLE_WELL_GROUP_NAME).toString();
            if (sampleWellgroupName != null)
            {
                for (Map.Entry<WellGroup, ExpMaterial> entry : _wellGroupMaterialMapping.entrySet())
                {
                    if (entry.getKey().getName().equals(sampleWellgroupName))
                        return entry.getValue();
                }
            }
        }
        assert (material != null) : "Unable to find the ExpMaterial associated with the wellgroup : " + wellgroup.getName();

        return material;
    }

    @Override
    protected String getSampleKey(DilutionSummary summary)
    {
        String key = (String) summary.getFirstWellGroup().getProperty(SAMPLE_WELL_GROUP_NAME);

        if (key == null)
            key = super.getSampleKey(summary);
        assert (key != null) : "Unable to find the Sample key associated with the wellgroup : " + summary.getFirstWellGroup().getName();

        return key;
    }

/*
    @Override
    protected String getSampleKey(ExpMaterial material)
    {
        String virusName = "";
        String wellgroup = getWellGroupName(material);
        for (Map.Entry<PropertyDescriptor, Object> entry : material.getPropertyValues().entrySet())
        {
            if (entry.getKey().getName().equals(NabAssayProvider.VIRUS_NAME_PROPERTY_NAME))
                virusName = entry.getValue().toString();
        }
        return SinglePlateDilutionSamplePropertyHelper.getKey(virusName, wellgroup);
    }
*/

    @Nullable
    @Override
    public WellGroup getCellControlWells(Plate plate, @Nullable List<Position> dataPositions)
    {
        if (dataPositions != null)
        {
            WellGroup virusGroup = getVirusGroupForPositions(plate, dataPositions);
            if (virusGroup != null)
            {
                WellGroup controlGroup = getControlWellsForVirus(plate, virusGroup, DilutionManager.CELL_CONTROL_SAMPLE);
                if (controlGroup != null)
                    return controlGroup;
            }
        }
        return plate.getWellGroup(WellGroup.Type.CONTROL, DilutionManager.CELL_CONTROL_SAMPLE);
    }

    @Nullable
    @Override
    public WellGroup getVirusControlWells(Plate plate, @Nullable List<Position> dataPositions)
    {
        if (dataPositions != null)
        {
            WellGroup virusGroup = getVirusGroupForPositions(plate, dataPositions);
            if (virusGroup != null)
            {
                WellGroup controlGroup = getControlWellsForVirus(plate, virusGroup, DilutionManager.VIRUS_CONTROL_SAMPLE);
                if (controlGroup != null)
                    return controlGroup;
            }
        }
        return plate.getWellGroup(WellGroup.Type.CONTROL, DilutionManager.VIRUS_CONTROL_SAMPLE);
    }

    /**
     * Returns the virus well group that contains the specified positions.
     * @param plate
     * @param positions
     * @return
     */
    @Nullable
    private WellGroup getVirusGroupForPositions(Plate plate, List<Position> positions)
    {
        // save the map of positions to virus well groups (if any)
        if (_positionToVirusMap.isEmpty())
        {
            for (WellGroup group : plate.getWellGroups(WellGroup.Type.VIRUS))
            {
                for (Position position : group.getPositions())
                    _positionToVirusMap.put(position, group);
            }
        }

        WellGroup virusGroup = null;
        if (!_positionToVirusMap.isEmpty())
        {
            for (Position position : positions)
            {
                if (_positionToVirusMap.containsKey(position))
                {
                    WellGroup group = _positionToVirusMap.get(position);

                    // ensure that positions don't span multiple virus groups
                    if (virusGroup != null)
                        assert virusGroup.equals(group);
                    else
                        virusGroup = group;
                }
            }
        }

        return virusGroup;
    }

    /**
     * Returns the control well group that corresponds to the specified virus group. Plate templates that support
     * multiple viruses can indicate separate control well groups for each virus by overlaying the cell and virus control
     * wells with the virus well group that it is associated with. For an example see the default NAb multi-virus
     * plate template.
     */
    @Nullable
    private WellGroup getControlWellsForVirus(Plate plate, WellGroup virusGroup, String controlGroupName)
    {
        // build up a map of virus group to control well group for each type of control group (virus and cell)
        //
        if (!_virusGroupToControlMap.containsKey(controlGroupName))
        {
            WellGroup control = plate.getWellGroup(WellGroup.Type.CONTROL, controlGroupName);
            List<? extends  WellGroup> virusGroups = plate.getWellGroups(WellGroup.Type.VIRUS);
            Map<WellGroup, List<Position>> virusToControlPositions = new HashMap<>();

            for (Position pos : control.getPositions())
            {
                for (WellGroup virus : virusGroups)
                {
                    if (virus.contains(pos))
                    {
                        if (!virusToControlPositions.containsKey(virus))
                            virusToControlPositions.put(virus, new ArrayList<Position>());

                        virusToControlPositions.get(virus).add(pos);
                        break;
                    }
                }
            }

            Map<WellGroup, WellGroup> virusToControl = new HashMap<>();
            for (Map.Entry<WellGroup, List<Position>> entry : virusToControlPositions.entrySet())
            {
                String name = DilutionDataHandler.getWellgroupNameVirusNameCombo(controlGroupName, entry.getKey().getName());
                WellGroup controlGroup = PlateService.get().createWellGroup(plate, name,
                                            WellGroup.Type.CONTROL, entry.getValue());
                virusToControl.put(entry.getKey(), controlGroup);
            }
            _virusGroupToControlMap.put(controlGroupName, virusToControl);
        }

        Map<WellGroup, WellGroup> virusToControl = _virusGroupToControlMap.get(controlGroupName);
        if (virusToControl != null)
            return virusToControl.get(virusGroup);

        return null;
    }

    @Override
    public Map<String, Object> getVirusNames()
    {
        if (_virusNames == null)
        {
            List<? extends WellGroup> virusWellGroups = _plate.getWellGroups(WellGroup.Type.VIRUS);
            _virusNames = new HashMap<>();
            if (!virusWellGroups.isEmpty())
            {
                for (WellGroup virusWellGroup : virusWellGroups)
                {
                    String virusWellGroupName = virusWellGroup.getName();
                    _virusNames.put(virusWellGroupName, getVirusName(virusWellGroupName));
                }
            }
            else
            {
                String virusName = getVirusName(AbstractPlateBasedAssayProvider.VIRUS_NAME_PROPERTY_NAME);
                if (null != virusName)
                    _virusNames.put("Virus", virusName);
            }
        }
        return _virusNames;
    }

    @Override
    protected String getVirusName(String virusWellGroupName)
    {
        List<? extends ExpData> outputDatas = _run.getOutputDatas(null);
        if (outputDatas.size() > 0)
        {
            Lsid virusLsid = DilutionDataHandler.createVirusWellGroupLsid(outputDatas.get(0), virusWellGroupName);
            AssayProtocolSchema schema = _provider.createProtocolSchema(_user, _run.getContainer(), _protocol, null);
            TableInfo virusTable = schema.createTable(DilutionManager.VIRUS_TABLE_NAME);
            if (null != virusTable)
            {
                ColumnInfo columnInfo = virusTable.getColumn(AbstractPlateBasedAssayProvider.VIRUS_NAME_PROPERTY_NAME);
                SimpleFilter filter = new SimpleFilter(FieldKey.fromString("VirusLsid"), virusLsid.toString());
                List<String> results = new TableSelector(columnInfo, filter, null).getArrayList(String.class);
                if (!results.isEmpty())
                    return results.get(0);
            }
        }
        return null;
    }

    protected WellGroup getWellGroupForVirus(Plate plate, String virusWellGroupName)
    {
        for (WellGroup group : plate.getWellGroups(WellGroup.Type.VIRUS))
            if (virusWellGroupName.equalsIgnoreCase(group.getName()))
                return group;
        return null;
    }

    private WellGroup getControlWellGroupForVirus(Plate plate, String virusWellGroupName, String controlGroupName)
    {
        WellGroup virusWellGroup = getWellGroupForVirus(plate, virusWellGroupName);
        if (null != virusWellGroup)
            return getControlWellsForVirus(plate, virusWellGroup, controlGroupName);
        else
            return plate.getWellGroup(WellGroup.Type.CONTROL, controlGroupName);
    }

    private CaseInsensitiveHashMap<WellGroup> _cellControls = new CaseInsensitiveHashMap<>();
    private CaseInsensitiveHashMap<WellGroup> _virusControls = new CaseInsensitiveHashMap<>();

    private boolean ensureCellControl(Plate plate, String virusWellGroupName)
    {
        if (null == _cellControls.get(virusWellGroupName))
            _cellControls.put(virusWellGroupName, getControlWellGroupForVirus(plate, virusWellGroupName, DilutionManager.CELL_CONTROL_SAMPLE));
        return null != _cellControls.get(virusWellGroupName);
    }

    private boolean ensureVirusControl(Plate plate, String virusWellGroupName)
    {
        if (null == _virusControls.get(virusWellGroupName))
            _virusControls.put(virusWellGroupName, getControlWellGroupForVirus(plate, virusWellGroupName, DilutionManager.VIRUS_CONTROL_SAMPLE));
        return null != _virusControls.get(virusWellGroupName);
    }

    @Override
    public WellGroup getCellControlWellGroup(Plate plate, String virusWellGroupName)
    {
        if (null == virusWellGroupName)
            return super.getCellControlWellGroup(plate, null);
        if (ensureCellControl(plate, virusWellGroupName))
            return _cellControls.get(virusWellGroupName);
        return null;
    }

    @Override
    public WellGroup getVirusControlWellGroup(Plate plate, String virusWellGroupName)
    {
        if (null == virusWellGroupName)
            return super.getVirusControlWellGroup(plate, null);
        if (ensureVirusControl(plate, virusWellGroupName))
            return _virusControls.get(virusWellGroupName);
        return null;
    }

    @Override
    public double getControlRange(Plate plate, String virusWellGroupName)
    {
        if (null == virusWellGroupName)
            return super.getControlRange(plate, null);
        if (ensureCellControl(plate, virusWellGroupName) && ensureVirusControl(plate, virusWellGroupName))
            return _virusControls.get(virusWellGroupName).getMean() - _cellControls.get(virusWellGroupName).getMean();
        return 0.0;
    }

    @Override
    public double getVirusControlMean(Plate plate, String virusWellGroupName)
    {
        if (null == virusWellGroupName)
            return super.getVirusControlMean(plate, null);
        if (ensureVirusControl(plate, virusWellGroupName))
            return _virusControls.get(virusWellGroupName).getMean();
        return super.getVirusControlMean(plate, null);
    }

    @Override
    public double getCellControlMean(Plate plate, String virusWellGroupName)
    {
        if (null == virusWellGroupName)
            return super.getCellControlMean(plate, null);
        if (ensureCellControl(plate, virusWellGroupName))
            return _cellControls.get(virusWellGroupName).getMean();
        return super.getCellControlMean(plate, null);
    }

    @Override
    public double getVirusControlPlusMinus(Plate plate, String virusWellGroupName)
    {
        if (null == virusWellGroupName)
            return super.getVirusControlPlusMinus(plate, null);
        if (ensureVirusControl(plate, virusWellGroupName))
        {
            double virusControlMean = _virusControls.get(virusWellGroupName).getMean();
            double virusControlStdDev = _virusControls.get(virusWellGroupName).getStdDev();
            return virusControlStdDev / virusControlMean;
        }
        return 0.0;
    }

    @Override
    public double getCellControlPlusMinus(Plate plate, String virusWellGroupName)
    {
        if (null == virusWellGroupName)
            return super.getCellControlPlusMinus(plate, null);
        if (ensureCellControl(plate, virusWellGroupName))
        {
            double cellControlMean = _cellControls.get(virusWellGroupName).getMean();
            double cellControlStdDev = _cellControls.get(virusWellGroupName).getStdDev();
            return cellControlStdDev / cellControlMean;
        }
        return 0.0;
    }
}
