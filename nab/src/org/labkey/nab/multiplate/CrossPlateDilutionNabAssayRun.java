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

import org.labkey.api.data.statistics.StatsService;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.security.User;
import org.labkey.api.study.Plate;
import org.labkey.api.study.WellGroup;
import org.labkey.api.assay.dilution.DilutionAssayProvider;
import org.labkey.api.assay.dilution.DilutionSummary;
import org.labkey.nab.NabAssayRun;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * User: klum
 * Date: 2/24/13
 */
public class CrossPlateDilutionNabAssayRun extends NabAssayRun
{
    protected List<Plate> _plates;
    private DilutionSummary[] _dilutionSummaries;

    public CrossPlateDilutionNabAssayRun(DilutionAssayProvider provider, ExpRun run, List<Plate> plates,
                                         User user, List<Integer> cutoffs, StatsService.CurveFitType renderCurveFitType)
    {
        super(provider, run, user, cutoffs, renderCurveFitType);
        _plates = plates;

        int sampleCount = plates.get(0).getWellGroupCount(WellGroup.Type.SPECIMEN);
        _dilutionSummaries = new DilutionSummary[sampleCount];
        Map<String, List<WellGroup>> sampleGroups = new LinkedHashMap<>();
        for (Plate plate : plates)
        {
            for (WellGroup sample : plate.getWellGroups(WellGroup.Type.SPECIMEN))
            {
                List<WellGroup> groups = sampleGroups.get(sample.getName());
                if (groups == null)
                {
                    groups = new ArrayList<>();
                    sampleGroups.put(sample.getName(), groups);
                }
                groups.add(sample);
            }
        }
        int index = 0;
        for (Map.Entry<String, List<WellGroup>> sample : sampleGroups.entrySet())
            _dilutionSummaries[index++] = new DilutionSummary(this, sample.getValue(), null, _renderedCurveFitType);
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
}
