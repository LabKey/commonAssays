package org.labkey.nab;

import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.study.DilutionCurve;
import org.labkey.api.study.Plate;
import org.labkey.api.study.Position;
import org.labkey.api.study.Well;
import org.labkey.api.study.WellGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* Copyright (c) 2010 LabKey Corporation
* <p/>
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* <p/>
* http://www.apache.org/licenses/LICENSE-2.0
* <p/>
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
* <p/>
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

    public NabRunPropertyMap(NabAssayRun assay, boolean includeStats, boolean includeWells, boolean calculateNeut, boolean includeFitParameters)
    {
        put("runId", assay.getRun().getRowId());
        put("properties", new PropertyNameMap(assay.getRunProperties()));
        put("containerPath", assay.getRun().getContainer().getPath());
        put("containerId", assay.getRun().getContainer().getId());
        put("cutoffs", assay.getCutoffs());
        List<Map<String, Object>> samples = new ArrayList<Map<String, Object>>();
        for (NabAssayRun.SampleResult result : assay.getSampleResults())
        {
            Map<String, Object> sample = new HashMap<String, Object>();
            sample.put("properties", new PropertyNameMap(result.getSampleProperties()));
            DilutionSummary dilutionSummary = result.getDilutionSummary();
            sample.put("objectId", result.getObjectId());
            sample.put("wellgroupName", dilutionSummary.getWellGroup().getName());
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
                List<Map<String, Object>> replicates = new ArrayList<Map<String, Object>>();
                for (WellGroup replicate : dilutionSummary.getWellGroup().getOverlappingGroups(WellGroup.Type.REPLICATE))
                {
                    Map<String, Object> replicateProps = new HashMap<String, Object>();
                    replicateProps.put("dilution", replicate.getDilution());
                    if (calculateNeut)
                    {
                        replicateProps.put("neutPercent", dilutionSummary.getPercent(replicate));
                        replicateProps.put("neutPlusMinus", dilutionSummary.getPlusMinus(replicate));
                    }
                    addStandardWellProperties(replicate, replicateProps, includeStats, includeWells);
                    replicates.add(replicateProps);
                }
                sample.put("replicates", replicates);
            }
            catch (DilutionCurve.FitFailedException e)
            {
                throw new RuntimeException(e);
            }

            samples.add(sample);
        }
        put("samples", samples);

        Plate[] plates = assay.getPlates();
        if (plates != null)
        {
            for (int i = 0; i < plates.length; i++)
            {
                String indexSuffix = plates.length > 1 ? "" + (i + 1) : "";
                Plate plate = plates[i];
                WellGroup cellControl = plate.getWellGroup(WellGroup.Type.CONTROL, NabManager.CELL_CONTROL_SAMPLE);
                Map<String, Object> cellControlProperties = new HashMap<String, Object>();
                addStandardWellProperties(cellControl, cellControlProperties, includeStats, includeWells);
                put("cellControl" + indexSuffix, cellControlProperties);

                WellGroup virusControl = plate.getWellGroup(WellGroup.Type.CONTROL, NabManager.VIRUS_CONTROL_SAMPLE);
                Map<String, Object> virusControlProperties = new HashMap<String, Object>();
                addStandardWellProperties(virusControl, virusControlProperties, includeStats, includeWells);
                put("virusControl" + indexSuffix, virusControlProperties);
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
            List<Map<String, Object>> wellList = new ArrayList<Map<String, Object>>();
            for (Position position : group.getPositions())
            {
                Map<String, Object> wellProps = new HashMap<String, Object>();
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
