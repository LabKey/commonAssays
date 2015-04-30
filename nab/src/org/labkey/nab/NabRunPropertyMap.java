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

import org.labkey.api.assay.dilution.DilutionAssayRun;
import org.labkey.api.assay.dilution.DilutionSummary;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.statistics.FitFailedException;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.study.Plate;
import org.labkey.api.study.Position;
import org.labkey.api.study.Well;
import org.labkey.api.study.WellGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: brittp
 * Date: Mar 12, 2010 9:43:44 AM
 */
public class NabRunPropertyMap extends HashMap<String, Object>
{
    private static class PropertyNameMap extends HashMap<String, Object>
    {
        public PropertyNameMap(Map<PropertyDescriptor, Object> properties)
        {
            for (Map.Entry<PropertyDescriptor, Object> entry : properties.entrySet())
                put(entry.getKey().getName(), entry.getValue());
        }
    }

    public NabRunPropertyMap(DilutionAssayRun assay, boolean includeStats, boolean includeWells, boolean calculateNeut, boolean includeFitParameters)
    {
        this(assay, includeStats, includeWells, calculateNeut, includeFitParameters, Collections.EMPTY_MAP);
    }

    public NabRunPropertyMap(DilutionAssayRun assay, boolean includeStats, boolean includeWells, boolean calculateNeut,
                             boolean includeFitParameters, Map<Integer, Map<String, Object>> extraObjectIdProps)
    {
        put("runId", assay.getRun().getRowId());
        put("properties", new PropertyNameMap(assay.getRunProperties()));
        put("containerPath", assay.getRun().getContainer().getPath());
        put("containerId", assay.getRun().getContainer().getId());
        put("cutoffs", assay.getCutoffs());
        List<Map<String, Object>> samples = new ArrayList<>();
        for (DilutionAssayRun.SampleResult result : assay.getSampleResults())
        {
            Map<String, Object> sample = new HashMap<>();
            sample.put("properties", new PropertyNameMap(result.getSampleProperties()));
            CaseInsensitiveHashMap<Object> virusProperties = result.getVirusProperties();
            if (!virusProperties.isEmpty())
                sample.put("virusProperties", virusProperties);

            DilutionSummary dilutionSummary = result.getDilutionSummary();
            sample.put("objectId", result.getObjectId());

            // add any additional properties associated with this object id
            if (extraObjectIdProps.containsKey(result.getObjectId()))
            {
                for (Map.Entry<String, Object> entry : extraObjectIdProps.get(result.getObjectId()).entrySet())
                {
                    sample.put(entry.getKey(), entry.getValue());
                }
            }

            sample.put("wellgroupName", dilutionSummary.getFirstWellGroup().getName());
            try
            {
                if (includeStats)
                {
                    sample.put("minDilution", dilutionSummary.getMinDilution(assay.getRenderedCurveFitType()));
                    sample.put("maxDilution", dilutionSummary.getMaxDilution(assay.getRenderedCurveFitType()));
                }
                if (calculateNeut)
                {
                    sample.put("fitError", dilutionSummary.getFitError());
                    for (int cutoff : assay.getCutoffs())
                    {
                        sample.put("curveIC" + cutoff, dilutionSummary.getCutoffDilution(cutoff/100.0, assay.getRenderedCurveFitType()));
                        sample.put("pointIC" + cutoff, dilutionSummary.getInterpolatedCutoffDilution(cutoff/100.0, assay.getRenderedCurveFitType()));
                    }
                }
                if (includeFitParameters)
                {
                    sample.put("fitParameters", dilutionSummary.getCurveParameters(assay.getRenderedCurveFitType()).toMap());
                }
                List<Map<String, Object>> replicates = new ArrayList<>();
                for (WellGroup sampleGroup : dilutionSummary.getWellGroups())
                {
                    for (WellGroup replicate : sampleGroup.getOverlappingGroups(WellGroup.Type.REPLICATE))
                    {
                        Map<String, Object> replicateProps = new HashMap<>();
                        replicateProps.put("dilution", replicate.getDilution());
                        if (calculateNeut)
                        {
                            replicateProps.put("neutPercent", dilutionSummary.getPercent(replicate));
                            replicateProps.put("neutPlusMinus", dilutionSummary.getPlusMinus(replicate));
                        }
                        addStandardWellProperties(replicate, replicateProps, includeStats, includeWells);
                        replicates.add(replicateProps);
                    }
                }
                sample.put("replicates", replicates);
            }
            catch (FitFailedException e)
            {
                throw new RuntimeException(e);
            }

            samples.add(sample);
        }
        put("samples", samples);

        Map<String, Object> virusNames = assay.getVirusNames();
        List<Plate> plates = assay.getPlates();
        if (plates != null)
        {
            for (int i = 0; i < plates.size(); i++)
            {
                String indexSuffix = plates.size() > 1 ? "" + (i + 1) : "";
                Plate plate = plates.get(i);
                if (virusNames.isEmpty())
                {
                    WellGroup cellControl = plate.getWellGroup(WellGroup.Type.CONTROL, NabManager.CELL_CONTROL_SAMPLE);
                    Map<String, Object> cellControlProperties = new HashMap<>();
                    addStandardWellProperties(cellControl, cellControlProperties, includeStats, includeWells);
                    put("cellControl" + indexSuffix, cellControlProperties);

                    WellGroup virusControl = plate.getWellGroup(WellGroup.Type.CONTROL, NabManager.VIRUS_CONTROL_SAMPLE);
                    Map<String, Object> virusControlProperties = new HashMap<>();
                    addStandardWellProperties(virusControl, virusControlProperties, includeStats, includeWells);
                    put("virusControl" + indexSuffix, virusControlProperties);
                }
                else
                {
                    List<Map<String, Object>> cellControls = new ArrayList<>();
                    List<Map<String, Object>> virusControls = new ArrayList<>();
                    for (Map.Entry<String, Object> virusEntry : virusNames.entrySet())
                    {
                        Map<String, Object> cellControlProperties = new HashMap<>();
                        WellGroup cellControl = assay.getCellControlWellGroup(plate, virusEntry.getKey());
                        if (null != cellControl)
                        {
                            addStandardWellProperties(cellControl, cellControlProperties, includeStats, includeWells);
                            cellControlProperties.put("VirusName", virusEntry.getValue());
                            cellControlProperties.put("VirusLocation", virusEntry.getKey());
                            cellControls.add(cellControlProperties);
                        }

                        Map<String, Object> virusControlProperties = new HashMap<>();
                        WellGroup virusControl = assay.getVirusControlWellGroup(plate, virusEntry.getKey());
                        if (null != virusControl)
                        {
                            addStandardWellProperties(virusControl, virusControlProperties, includeStats, includeWells);
                            virusControlProperties.put("VirusName", virusEntry.getValue());
                            virusControlProperties.put("VirusLocation", virusEntry.getKey());
                            virusControls.add(virusControlProperties);
                        }
                    }
                    put("cellControls" + indexSuffix, cellControls);
                    put("virusControls" + indexSuffix, virusControls);
                }
            }
        }
    }

    private void addStandardWellProperties(WellGroup group, Map<String, Object> properties, boolean includeStats, boolean includeWells)
    {
        if (includeStats)
        {
            properties.put("min", group.getMin());
            properties.put("max", group.getMax());
            properties.put("mean", group.getMean());
            properties.put("stddev", group.getStdDev());
        }
        if (includeWells)
        {
            List<Map<String, Object>> wellList = new ArrayList<>();
            for (Position position : group.getPositions())
            {
                Map<String, Object> wellProps = new HashMap<>();
                Well well = group.getPlate().getWell(position.getRow(), position.getColumn());
                wellProps.put("row", well.getRow());
                wellProps.put("column", well.getColumn());
                wellProps.put("value", well.getValue());
                wellList.add(wellProps);
            }
            properties.put("wells", wellList);
        }
    }
}
