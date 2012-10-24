/*
 * Copyright (c) 2012 LabKey Corporation
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

package org.labkey.elisa;

import org.labkey.api.data.Container;
import org.labkey.api.study.AbstractPlateTypeHandler;
import org.labkey.api.study.PlateService;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.WellGroup;
import org.labkey.api.util.Pair;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: 10/7/12
 */
public class ElisaPlateTypeHandler extends AbstractPlateTypeHandler
{
    public static final String DEFAULT_PLATE = "default";
    public static final String STANDARDS_CONTROL_SAMPLE = "Standards";

    @Override
    public String getAssayType()
    {
        return ElisaAssayProvider.NAME;
    }

    @Override
    public List<String> getTemplateTypes()
    {
        return Collections.singletonList(DEFAULT_PLATE);
    }

    @Override
    public PlateTemplate createPlate(String templateTypeName, Container container, int rowCount, int colCount) throws SQLException
    {
        PlateTemplate template = PlateService.get().createPlateTemplate(container, getAssayType(), rowCount, colCount);

        template.addWellGroup(STANDARDS_CONTROL_SAMPLE, WellGroup.Type.CONTROL,
                PlateService.get().createPosition(container, 0, 0),
                PlateService.get().createPosition(container, template.getRows() - 1, 1));

        for (int sample = 0; sample < (template.getColumns())/2; sample++)
        {
            int firstCol = (sample * 2);

            if (firstCol > 0)
            {
            // create the overall specimen group, consisting of two adjacent columns:
            template.addWellGroup("Specimen " + (sample + 1), WellGroup.Type.SPECIMEN,
                    PlateService.get().createPosition(container, 0, firstCol),
                    PlateService.get().createPosition(container, template.getRows() - 1, firstCol + 1));
            }

            for (int replicate = 0; replicate < template.getRows(); replicate++)
            {
                String specimenName = firstCol == 0 ? "Standard" : ("Specimen " + sample + 1);

                template.addWellGroup(specimenName + ", Replicate " + (replicate + 1), WellGroup.Type.REPLICATE,
                        PlateService.get().createPosition(container, replicate, firstCol),
                        PlateService.get().createPosition(container, replicate, firstCol + 1));
            }
        }
        return template;
    }

    @Override
    public List<Pair<Integer, Integer>> getSupportedPlateSizes()
    {
        return Collections.singletonList(new Pair<Integer, Integer>(8, 12));
    }

    @Override
    public WellGroup.Type[] getWellGroupTypes()
    {
        return new WellGroup.Type[]{
                WellGroup.Type.CONTROL, WellGroup.Type.SPECIMEN,
                WellGroup.Type.REPLICATE};
    }
}
