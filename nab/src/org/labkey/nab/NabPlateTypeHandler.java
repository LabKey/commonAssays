/*
 * Copyright (c) 2007-2010 LabKey Corporation
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

import org.labkey.api.study.*;
import org.labkey.api.data.Container;
import org.labkey.api.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.sql.SQLException;


/**
 * User: jeckels
 * Date: Apr 23, 2007
 */
public class NabPlateTypeHandler extends AbstractPlateTypeHandler
{
    public static final String SINGLE_PLATE_TYPE = "single-plate";
    public static final String HIGH_THROUGHPUT_PLATE_TYPE = "high-throughput";
    public static final String BLANK_PLATE_TYPE = "blank";

    public String getAssayType()
    {
        return "NAb";
    }

    public List<String> getTemplateTypes()
    {
        List<String> names = new ArrayList<String>();
        names.add(BLANK_PLATE_TYPE);
        names.add(SINGLE_PLATE_TYPE);
        names.add(HIGH_THROUGHPUT_PLATE_TYPE);
        return names;
    }

    @Override
    public List<Pair<Integer, Integer>> getSupportedPlateSizes()
    {
        List<Pair<Integer, Integer>> sizes = new ArrayList<Pair<Integer, Integer>>();
        sizes.add(new Pair<Integer, Integer>(8, 12));
        sizes.add(new Pair<Integer, Integer>(16, 24));
        return sizes;
    }

    public PlateTemplate createPlate(String templateTypeName, Container container, int rowCount, int colCount) throws SQLException
    {
        PlateTemplate template = PlateService.get().createPlateTemplate(container, getAssayType(), rowCount, colCount);

        template.addWellGroup(NabManager.CELL_CONTROL_SAMPLE, WellGroup.Type.CONTROL,
                PlateService.get().createPosition(container, 0, 0),
                PlateService.get().createPosition(container, template.getRows() - 1, 0));
        template.addWellGroup(NabManager.VIRUS_CONTROL_SAMPLE, WellGroup.Type.CONTROL,
                PlateService.get().createPosition(container, 0, 1),
                PlateService.get().createPosition(container, template.getRows() - 1, 1));
        
        if (templateTypeName != null && templateTypeName.equalsIgnoreCase(SINGLE_PLATE_TYPE))
        {
            for (int sample = 0; sample < (template.getColumns() - 2)/2; sample++)
            {
                int firstCol = (sample * 2) + 2;
                // create the overall specimen group, consisting of two adjacent columns:
                WellGroupTemplate sampleGroup = template.addWellGroup("Specimen " + (sample + 1), WellGroup.Type.SPECIMEN,
                        PlateService.get().createPosition(container, 0, firstCol),
                        PlateService.get().createPosition(container, template.getRows() - 1, firstCol + 1));
//                sampleGroup.setProperty(prop.name(), "");
                for (NabManager.SampleProperty prop : NabManager.SampleProperty.values())
                {
                    if (prop.isTemplateProperty())
                        sampleGroup.setProperty(prop.name(), "");
                }

                for (int replicate = 0; replicate < template.getRows(); replicate++)
                {   
                    template.addWellGroup("Specimen " + (sample + 1) + ", Replicate " + (replicate + 1), WellGroup.Type.REPLICATE,
                            PlateService.get().createPosition(container, replicate, firstCol),
                            PlateService.get().createPosition(container, replicate, firstCol + 1));
                }
            }
        }
        else if (templateTypeName != null && templateTypeName.equalsIgnoreCase(HIGH_THROUGHPUT_PLATE_TYPE))
        {
            int sample = 1;
            for (int col = 2; col < (template.getColumns() - 1); col += 2)
            {
                for (int row = 0; row < template.getRows(); row++)
                {
                    int currentSampleIndex = sample++;
                    template.addWellGroup("Specimen " + currentSampleIndex, WellGroup.Type.SPECIMEN,
                            PlateService.get().createPosition(container, row, col),
                            PlateService.get().createPosition(container, row, col+1));
                    template.addWellGroup("Specimen " + currentSampleIndex, WellGroup.Type.REPLICATE,
                        PlateService.get().createPosition(container, row, col),
                        PlateService.get().createPosition(container, row, col+1));
                }
            }
        }
        return template;
    }

    public WellGroup.Type[] getWellGroupTypes()
    {
        return new WellGroup.Type[]{
                WellGroup.Type.CONTROL, WellGroup.Type.SPECIMEN,
                WellGroup.Type.REPLICATE, WellGroup.Type.OTHER};
    }
}
