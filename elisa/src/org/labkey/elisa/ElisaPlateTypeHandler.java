/*
 * Copyright (c) 2012-2018 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.api.assay.plate.WellGroup;
import org.labkey.api.data.Container;
import org.labkey.api.assay.plate.AbstractPlateTypeHandler;
import org.labkey.api.assay.plate.PlateService;
import org.labkey.api.assay.plate.Plate;
import org.labkey.api.settings.ExperimentalFeatureService;
import org.labkey.api.util.Pair;

import java.util.Arrays;
import java.util.List;

import static org.labkey.elisa.ElisaModule.EXPERIMENTAL_MULTI_PLATE_SUPPORT;

/**
 * User: klum
 * Date: 10/7/12
 */
public class ElisaPlateTypeHandler extends AbstractPlateTypeHandler
{
    public static final String DEFAULT_PLATE = "default";
    public static final String UNDILUTED_PLATE = "undiluted";
    public static final String HIGH_THROUGHPUT_PLATE = "high-throughput (multi plate)";
    public static final String STANDARDS_CONTROL_SAMPLE = "Standards";

    @Override
    public String getAssayType()
    {
        return ElisaAssayProvider.NAME;
    }

    @Override
    public List<String> getTemplateTypes(Pair<Integer, Integer> size)
    {
        if (size.first == 8)
            return Arrays.asList(DEFAULT_PLATE, UNDILUTED_PLATE);
        else
            return Arrays.asList(HIGH_THROUGHPUT_PLATE);
    }

    @Override
    public Plate createTemplate(@Nullable String templateTypeName, Container container, int rowCount, int colCount)
    {
        Plate template = PlateService.get().createPlateTemplate(container, getAssayType(), rowCount, colCount);

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
        else if (HIGH_THROUGHPUT_PLATE.equals(templateTypeName))
        {
            template = createHighThroughputPlate(container, rowCount, colCount);
        }
        return template;
    }

    private Plate createHighThroughputPlate(Container container, int rowCount, int colCount)
    {
        Plate template = PlateService.get().createPlateTemplate(container, getAssayType(), rowCount, colCount);

        // control well groups
        template.addWellGroup(STANDARDS_CONTROL_SAMPLE, WellGroup.Type.CONTROL,
                PlateService.get().createPosition(container, 0, 0),
                PlateService.get().createPosition(container, 7, 2));

        for (int row = 8; row < rowCount; row++)
        {
            String wellGroupName = "Control " + (row-7);
            template.addWellGroup(wellGroupName, WellGroup.Type.CONTROL,
                    PlateService.get().createPosition(container, row, 0),
                    PlateService.get().createPosition(container, row, 2));
        }

        // control replicates
        for (int row = 0; row < rowCount; row++)
        {
            String wellGroupName = (row <= 7)
                    ? "Standard, Replicate " + (row + 1)
                    : "Control, Replicate " + (row - 7);

            template.addWellGroup(wellGroupName, WellGroup.Type.REPLICATE,
                    PlateService.get().createPosition(container, row, 0),
                    PlateService.get().createPosition(container, row, 2));
        }

        // sample well groups
        for (int col = 3; col < colCount; col++)
        {
            String wellGroupName = "Sample " + (col-2);
            template.addWellGroup(wellGroupName, WellGroup.Type.SPECIMEN,
                    PlateService.get().createPosition(container, 0, col),
                    PlateService.get().createPosition(container, rowCount-1, col));

            // sample replicates
            for (int row = 0; row < rowCount; row += 2)
            {
                String replicateGroupName = wellGroupName + ", Replicate " + ((row/2) + 1);
                template.addWellGroup(replicateGroupName, WellGroup.Type.REPLICATE,
                        PlateService.get().createPosition(container, row, col),
                        PlateService.get().createPosition(container, row+1, col));
            }
        }

        // replicate well groups
        return template;
    }

    @Override
    public List<Pair<Integer, Integer>> getSupportedPlateSizes()
    {
        if (ExperimentalFeatureService.get().isFeatureEnabled(EXPERIMENTAL_MULTI_PLATE_SUPPORT))
            return List.of(new Pair<>(8, 12), new Pair<>(16, 24));
        else
            return List.of(new Pair<>(8, 12));
    }

    @Override
    public List<WellGroup.Type> getWellGroupTypes()
    {
        return Arrays.asList(WellGroup.Type.CONTROL, WellGroup.Type.SPECIMEN, WellGroup.Type.REPLICATE);
    }
}
