/*
 * Copyright (c) 2008-2010 LabKey Corporation
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

package org.labkey.elispot;

import org.labkey.api.data.Container;
import org.labkey.api.study.*;
import org.labkey.api.util.Pair;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: Karl Lum
 * Date: Jan 14, 2008
 */
public class ElispotPlateTypeHandler implements PlateTypeHandler
{
    public String getAssayType()
    {
        return "ELISpot";
    }

    public List<String> getTemplateTypes()
    {
        List<String> names = new ArrayList<String>();
        return names;
    }

    @Override
    public List<Pair<Integer, Integer>> getSupportedPlateSizes()
    {
        return Collections.singletonList(new Pair<Integer, Integer>(8, 12));
    }

    public PlateTemplate createPlate(String templateTypeName, Container container, int rowCount, int colCount) throws SQLException
    {
        PlateTemplate template = PlateService.get().createPlateTemplate(container, getAssayType(), rowCount, colCount);

        for (int sample = 0; sample < 4; sample++)
        {
            int row = sample * 2;
            // create the overall specimen group, consisting of two adjacent rows:
            template.addWellGroup("Specimen " + (sample + 1), WellGroup.Type.SPECIMEN,
                    PlateService.get().createPosition(container, row, 0),
                    PlateService.get().createPosition(container, row+1, template.getColumns() - 1));
        }

        // populate the antigen groups
        for (int antigen = 0; antigen < 4; antigen++)
        {
            List<Position> position1 = new ArrayList<Position>();
            List<Position> position2 = new ArrayList<Position>();

            for (int sample = 0; sample < 4; sample++)
            {
                int row = sample * 2;
                int col = antigen * 3;

                position1.add(template.getPosition(row, col));
                position1.add(template.getPosition(row, col + 1));
                position1.add(template.getPosition(row, col + 2));

                position2.add(template.getPosition(row + 1, col));
                position2.add(template.getPosition(row + 1, col + 1));
                position2.add(template.getPosition(row + 1, col + 2));
            }
            template.addWellGroup("Antigen " + (antigen*2 + 1), WellGroup.Type.ANTIGEN, position1);
            template.addWellGroup("Antigen " + (antigen*2 + 2), WellGroup.Type.ANTIGEN, position2);
        }
        return template;
    }

    public WellGroup.Type[] getWellGroupTypes()
    {
        return new WellGroup.Type[]{
                WellGroup.Type.CONTROL, WellGroup.Type.SPECIMEN,
                WellGroup.Type.REPLICATE, WellGroup.Type.ANTIGEN};
    }
}

