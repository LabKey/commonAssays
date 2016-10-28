/*
 * Copyright (c) 2008-2016 LabKey Corporation
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
import org.labkey.api.data.statistics.MathStat;
import org.labkey.api.data.statistics.StatsService;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.services.ServiceRegistry;
import org.labkey.api.study.AbstractPlateTypeHandler;
import org.labkey.api.study.Plate;
import org.labkey.api.study.PlateService;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.Position;
import org.labkey.api.study.Well;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.WellGroupTemplate;
import org.labkey.api.study.assay.plate.PlateReader;
import org.labkey.api.util.Pair;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: Karl Lum
 * Date: Jan 14, 2008
 */
public class ElispotPlateTypeHandler extends AbstractPlateTypeHandler
{
    public static final String BLANK_PLATE = "blank";
    public static final String DEFAULT_PLATE = "default";
    public static final String BACKGROUND_WELL_GROUP = "Background Wells";

    public static final String MEAN_STAT = "mean";
    public static final String MEDIAN_STAT = "median";

    public String getAssayType()
    {
        return "ELISpot";
    }

    public List<String> getTemplateTypes(Pair<Integer, Integer> size)
    {
        List<String> names = new ArrayList<>();
        names.add(BLANK_PLATE);
        names.add(DEFAULT_PLATE);
        return names;
    }

    @Override
    public List<Pair<Integer, Integer>> getSupportedPlateSizes()
    {
        return Collections.singletonList(new Pair<>(8, 12));
    }

    public PlateTemplate createPlate(String templateTypeName, Container container, int rowCount, int colCount) throws SQLException
    {
        PlateTemplate template = PlateService.get().createPlateTemplate(container, getAssayType(), rowCount, colCount);

        // for the default elispot plate, we pre-populate it with specimen and antigen groups
        if (templateTypeName != null && templateTypeName.equals(DEFAULT_PLATE))
        {
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
                List<Position> position1 = new ArrayList<>();
                List<Position> position2 = new ArrayList<>();

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
        }
        return template;
    }

    public List<WellGroup.Type> getWellGroupTypes()
    {
        return Arrays.asList(WellGroup.Type.SPECIMEN, WellGroup.Type.ANTIGEN, WellGroup.Type.CONTROL);
    }

    @Override
    public void validate(Container container, User user, PlateTemplate template) throws ValidationException
    {
        boolean hasBackgroundWell = false;

        for (WellGroupTemplate group : template.getWellGroups())
        {
            if (group.getType() == WellGroup.Type.CONTROL)
            {
                // look for background well groups

                if (BACKGROUND_WELL_GROUP.equals(group.getName()))
                {
                    if (hasBackgroundWell)
                        throw new ValidationException("Only one Background Well group is permitted");
                    else
                        hasBackgroundWell = true;
                }
            }
        }
    }

    /**
     * Calculates the background value on a per specimen basis and returns a map of specimen
     * wellgroup name to background value.
     *
     * Background values are calculated from the median of all background wells that exist within a
     * specimen wellgroup. Background wells are specified on the plate template control type using the
     * background well wellgroup.
     */
    public static Map<String, Map<String, Double>> getBackgroundValues(Container container, Plate plate, PlateReader reader)
    {
        Map<String, Map<String, Double>> backgroundMap = new HashMap<>();
        WellGroup backgroundGroup = null;

        for (WellGroup group : plate.getWellGroups(WellGroup.Type.CONTROL))
        {
            if (BACKGROUND_WELL_GROUP.equals(group.getName()));
            {
                backgroundGroup = group;
                break;
            }
        }

        if (backgroundGroup != null)
        {
            // for each specimen group, find the background wells
            for (WellGroup group : plate.getWellGroups(WellGroup.Type.SPECIMEN))
            {
                List<Position> positions = group.getPositions();
                double[] statsData = new double[positions.size()];
                int i = 0;

                for (Position pos : positions)
                {
                    if (backgroundGroup.contains(pos))
                    {
                        Well well = plate.getWell(pos.getRow(), pos.getColumn());
                        if (reader.isWellValueValid(well.getValue()))
                            statsData[i++] = well.getValue();
                    }
                }

                if (i > 0)
                {
                    statsData = Arrays.copyOf(statsData, i);
                    StatsService service = ServiceRegistry.get().getService(StatsService.class);
                    MathStat stats = service.getStats(statsData);

                    Map<String, Double> values = new HashMap<>();

                    if (!Double.isNaN(stats.getMedian()))
                        values.put(MEDIAN_STAT, stats.getMedian());

                    if (!Double.isNaN(stats.getMean()))
                        values.put(MEAN_STAT, stats.getMean());

                    backgroundMap.put(group.getName(), values);
                }
            }
        }
        return backgroundMap;
    }

    @Override
    public Map<String, List<String>> getDefaultGroupsForTypes()
    {
        Map<String, List<String>> groupMap = new HashMap<>();

        groupMap.put(WellGroup.Type.CONTROL.name(), Collections.singletonList(BACKGROUND_WELL_GROUP));

        return groupMap;
    }

    @Override
    public boolean showEditorWarningPanel()
    {
        return false;
    }
}

