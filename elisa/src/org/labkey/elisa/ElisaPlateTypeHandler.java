/*
 * Copyright (c) 2012-2016 LabKey Corporation
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * User: klum
 * Date: 10/7/12
 */
public class ElisaPlateTypeHandler extends AbstractPlateTypeHandler
{
    public static final String DEFAULT_PLATE = "default";
    public static final String UNDILUTED_PLATE = "undiluted";
    public static final String STANDARDS_CONTROL_SAMPLE = "Standards";

    @Override
    public String getAssayType()
    {
        return ElisaAssayProvider.NAME;
    }

    @Override
    public List<String> getTemplateTypes(Pair<Integer, Integer> size)
    {
        return Arrays.asList(DEFAULT_PLATE, UNDILUTED_PLATE);
    }

    @Override
    public PlateTemplate createPlate(String templateTypeName, Container container, int rowCount, int colCount) throws SQLException
    {
        PlateTemplate template = PlateService.get().createPlateTemplate(container, getAssayType(), rowCount, colCount);

        template.addWellGroup(STANDARDS_CONTROL_SAMPLE, WellGroup.Type.CONTROL,
                PlateService.get().createPosition(container, 0, 0),
                PlateService.get().createPosition(container, template.getRows() - 3, 1));

        if (DEFAULT_PLATE.equals(templateTypeName))
        {
            for (int sample = 0; sample < (template.getColumns())/2; sample++)
            {
                int firstCol = (sample * 2);

                if (firstCol > 0)
                {
                    // create the overall specimen group, consisting of two adjacent columns:
                    template.addWellGroup("Specimen " + sample, WellGroup.Type.SPECIMEN,
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
        }
        else if (UNDILUTED_PLATE.equals(templateTypeName))
        {
            int specimen = 1;
            for (int column = 0; column < (template.getColumns())/2; column++)
            {
                int firstCol = (column * 2);

                for (int row = 0; row < template.getRows(); row++)
                {
                    // column group 1 through rows 6 are the control well groups
                    String wellName;

                    if (firstCol == 0 && row <= 5)
                        wellName = "Standard, Replicate " + (row + 1);
                    else
                    {
                        template.addWellGroup("Specimen " + specimen, WellGroup.Type.SPECIMEN,
                                PlateService.get().createPosition(container, row, firstCol),
                                PlateService.get().createPosition(container, row, firstCol + 1));

                        wellName = "Specimen " + (specimen++) + ", Replicate 1";
                    }
                    template.addWellGroup(wellName, WellGroup.Type.REPLICATE,
                            PlateService.get().createPosition(container, row, firstCol),
                            PlateService.get().createPosition(container, row, firstCol + 1));
                }
            }
        }
        return template;
    }

    @Override
    public List<Pair<Integer, Integer>> getSupportedPlateSizes()
    {
        return Collections.singletonList(new Pair<>(8, 12));
    }

    @Override
    public List<WellGroup.Type> getWellGroupTypes()
    {
        return Arrays.asList(WellGroup.Type.CONTROL, WellGroup.Type.SPECIMEN, WellGroup.Type.REPLICATE);
    }
}
