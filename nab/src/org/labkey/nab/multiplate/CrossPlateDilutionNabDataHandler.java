/*
 * Copyright (c) 2013-2017 LabKey Corporation
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
import org.labkey.api.assay.dilution.DilutionAssayRun;
import org.labkey.api.data.statistics.StatsService;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.security.User;
import org.labkey.api.study.Plate;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.assay.AssayDataType;
import org.labkey.api.util.FileType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: klum
 * Date: 2/24/13
 */
public class CrossPlateDilutionNabDataHandler extends HighThroughputNabDataHandler
{
    public static final AssayDataType NAB_HIGH_THROUGHPUT_DATA_TYPE = new AssayDataType("HighThroughputAssayRunNabData", new FileType(".csv"));

    @Override
    public DataType getDataType()
    {
        return NAB_HIGH_THROUGHPUT_DATA_TYPE;
    }

    @Override
    protected Map<ExpMaterial, List<WellGroup>> getMaterialWellGroupMapping(DilutionAssayProvider provider, List<Plate> plates, Map<ExpMaterial,String> sampleInputs) throws ExperimentException
    {
        Map<String, ExpMaterial> nameToMaterial = new HashMap<>();
        for (Map.Entry<ExpMaterial,String> e : sampleInputs.entrySet())
            nameToMaterial.put(e.getValue(), e.getKey());

        Map<ExpMaterial, List<WellGroup>> mapping = new HashMap<>();
        for (Plate plate : plates)
        {
            List<? extends WellGroup> specimenGroups = plate.getWellGroups(WellGroup.Type.SPECIMEN);
            for (WellGroup specimenGroup : specimenGroups)
            {
                String name = specimenGroup.getName();
                ExpMaterial material = nameToMaterial.get(name);
                if (material == null)
                {
                    throw new ExperimentException("Unable to find sample metadata for sample well group \"" + name +
                            "\": your sample metadata file may contain incorrect well group names, or it may not list all required samples.");
                }
                List<WellGroup> materialWellGroups = mapping.get(material);
                if (materialWellGroups == null)
                {
                    materialWellGroups = new ArrayList<>();
                    mapping.put(material, materialWellGroups);
                }
                materialWellGroups.add(specimenGroup);
            }
        }
        return mapping;
    }

    @Override
    protected DilutionAssayRun createDilutionAssayRun(DilutionAssayProvider provider, ExpRun run, List<Plate> plates, User user, List<Integer> sortedCutoffs, StatsService.CurveFitType fit)
    {
        return new CrossPlateDilutionNabAssayRun(provider, run, plates, user, sortedCutoffs, fit);
    }
}
