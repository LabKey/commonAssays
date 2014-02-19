/*
 * Copyright (c) 2013-2014 LabKey Corporation
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
package org.labkey.nab.multiplate;

import org.labkey.api.assay.dilution.DilutionAssayProvider;
import org.labkey.api.assay.dilution.DilutionMaterialKey;
import org.labkey.api.assay.dilution.DilutionSummary;
import org.labkey.api.assay.nab.Luc5Assay;
import org.labkey.api.data.statistics.StatsService;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.security.User;
import org.labkey.api.study.Plate;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.nab.NabAssayProvider;
import org.labkey.nab.NabAssayRun;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * User: klum
 * Date: 2/24/13
 */
public class SinglePlateDilutionNabAssayRun extends NabAssayRun
{
    protected List<Plate> _plates;
    private DilutionSummary[] _dilutionSummaries;

    public SinglePlateDilutionNabAssayRun(DilutionAssayProvider provider, ExpRun run, List<Plate> plates,
                                  User user, List<Integer> cutoffs, StatsService.CurveFitType renderCurveFitType)
    {
        super(provider, run, user, cutoffs, renderCurveFitType);
        _plates = plates;

        Map<String, List<WellGroup>> sampleGroups = new LinkedHashMap<>();
        for (Plate plate : plates)
        {
            for (WellGroup sample : plate.getWellGroups(WellGroup.Type.SPECIMEN))
            {
                String virusName = plate.getProperty(NabAssayProvider.VIRUS_NAME_PROPERTY_NAME).toString();
                String key = SinglePlateDilutionSamplePropertyHelper.getKey(virusName, sample.getName());
                List<WellGroup> groups = sampleGroups.get(key);
                if (groups == null)
                {
                    groups = new ArrayList<>();
                    sampleGroups.put(key, groups);
                }
                groups.add(sample);
            }
        }
        List<DilutionSummary> dilutionSummaries = new ArrayList<>();

        for (Map.Entry<String, List<WellGroup>> sample : sampleGroups.entrySet())
            dilutionSummaries.add(new MultiVirusDilutionSummary(this, sample.getValue(), null, _renderedCurveFitType));

        _dilutionSummaries = dilutionSummaries.toArray(new DilutionSummary[dilutionSummaries.size()]);
    }

    @Override
    public DilutionSummary[] getSummaries()
    {
        return _dilutionSummaries;
    }

    @Override
    public List<Plate> getPlates()
    {
        return Collections.unmodifiableList(_plates);
    }

    /**
     * Generate a key for the sample level property map
     * @param material
     * @return
     */
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

    /**
     * Generate a key for the sample level property map
     * @param summary
     * @return
     */
    @Override
    protected String getSampleKey(DilutionSummary summary)
    {
        String virusName = (String) summary.getFirstWellGroup().getProperty(NabAssayProvider.VIRUS_NAME_PROPERTY_NAME);
        String wellgroup = summary.getFirstWellGroup().getName();

        return SinglePlateDilutionSamplePropertyHelper.getKey(virusName, wellgroup);
    }

    private static class MultiVirusDilutionSummary extends DilutionSummary
    {
        public MultiVirusDilutionSummary(Luc5Assay assay, List<WellGroup> sampleGroups, String lsid, StatsService.CurveFitType curveFitType)
        {
            super(assay, sampleGroups, lsid, curveFitType);
        }
    }
}
