/*
 * Copyright (c) 2007-2017 LabKey Corporation
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

import org.labkey.api.assay.dilution.SampleProperty;
import org.labkey.api.data.Container;
import org.labkey.api.study.AbstractPlateTypeHandler;
import org.labkey.api.study.PlateService;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.Position;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.WellGroupTemplate;
import org.labkey.api.util.Pair;
import org.labkey.nab.multiplate.HighThroughputNabDataHandler;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * User: jeckels
 * Date: Apr 23, 2007
 */
public class NabPlateTypeHandler extends AbstractPlateTypeHandler
{
    public static final String SINGLE_PLATE_TYPE = "single-plate";
    public static final String HIGH_THROUGHPUT_PLATE_TYPE = "high-throughput (cross plate dilution)";
    public static final String HIGH_THROUGHPUT_SINGLEDILUTION_PLATE_TYPE = "high-throughput (single plate dilution)";
    public static final String BLANK_PLATE_TYPE = "blank";
    public static final String MULTI_VIRUS_384WELL_PLATE_TYPE = "multi-virus plate";
    public static final String SCREENING_20SAMPLE_4VIRUS_PLATE_TYPE = "screening : 20 samples, 4 virus plate";
    public static final String SCREENING_240SAMPLE_1VIRUS_PLATE_TYPE = "screening : 240 samples, 1 virus plate";

    public String getAssayType()
    {
        return "NAb";
    }

    public List<String> getTemplateTypes(Pair<Integer, Integer> size)
    {
        List<String> names = new ArrayList<>();
        names.add(BLANK_PLATE_TYPE);
        names.add(SINGLE_PLATE_TYPE);
        names.add(HIGH_THROUGHPUT_PLATE_TYPE);
        names.add(HIGH_THROUGHPUT_SINGLEDILUTION_PLATE_TYPE);
        if (16 == size.first && 24 == size.second)
        {
            names.add(MULTI_VIRUS_384WELL_PLATE_TYPE);
            names.add(SCREENING_20SAMPLE_4VIRUS_PLATE_TYPE);
            names.add(SCREENING_240SAMPLE_1VIRUS_PLATE_TYPE);
        }
        return names;
    }

    @Override
    public List<Pair<Integer, Integer>> getSupportedPlateSizes()
    {
        List<Pair<Integer, Integer>> sizes = new ArrayList<>();
        sizes.add(new Pair<>(8, 12));
        sizes.add(new Pair<>(16, 24));
        return sizes;
    }

    public PlateTemplate createPlate(String templateTypeName, Container container, int rowCount, int colCount) throws SQLException
    {
        PlateTemplate template = PlateService.get().createPlateTemplate(container, getAssayType(), rowCount, colCount);

        if (templateTypeName != null && templateTypeName.equalsIgnoreCase(HIGH_THROUGHPUT_SINGLEDILUTION_PLATE_TYPE))
            return createHighThroughputSingleDilutionTemplate(template, container, rowCount, colCount);

        if (templateTypeName != null && templateTypeName.equalsIgnoreCase(MULTI_VIRUS_384WELL_PLATE_TYPE))
            return createMultiVirusTemplate(template, container, rowCount, colCount);

        if (templateTypeName != null && templateTypeName.equalsIgnoreCase(SCREENING_20SAMPLE_4VIRUS_PLATE_TYPE))
            return create20Sample4VirusScreeningTemplate(template, container, rowCount, colCount);

        if (templateTypeName != null && templateTypeName.equalsIgnoreCase(SCREENING_240SAMPLE_1VIRUS_PLATE_TYPE))
            return create240Sample1VirusScreeningTemplate(template, container, rowCount, colCount);

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
                for (SampleProperty prop : SampleProperty.values())
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

    private PlateTemplate createHighThroughputSingleDilutionTemplate(PlateTemplate template, Container c, int rowCount, int colCount)
    {
        template.addWellGroup(NabManager.VIRUS_CONTROL_SAMPLE, WellGroup.Type.CONTROL,
                PlateService.get().createPosition(c, 0, 0),
                PlateService.get().createPosition(c, rowCount-1, 1));

        template.addWellGroup(NabManager.CELL_CONTROL_SAMPLE, WellGroup.Type.CONTROL,
                PlateService.get().createPosition(c, 0, colCount-2),
                PlateService.get().createPosition(c, rowCount-1, colCount-1));

        int colsPerSample = 8;
        int sampleIndex = 1;

        for (int colGroup = 0; colGroup < 2; colGroup++)
        {
            int startCol = colGroup * colsPerSample + 2;
            for (int row = 0; row < (rowCount - 1); row += 2)
            {
                template.addWellGroup("" + sampleIndex, WellGroup.Type.SPECIMEN,
                        PlateService.get().createPosition(c, row, startCol),
                        PlateService.get().createPosition(c, row+1, startCol+colsPerSample-1));

                int replicateIndex = 1;
                for (int col = startCol; col < (startCol + colsPerSample); col++)
                {
                    template.addWellGroup("Specimen " + sampleIndex + "-" + replicateIndex++, WellGroup.Type.REPLICATE,
                        PlateService.get().createPosition(c, row, col),
                        PlateService.get().createPosition(c, row+1, col));
                }
                sampleIndex++;
            }
        }

        // four groups in 4x4 configuration in replicate
        int startCol = 2 * colsPerSample + 2;
        colsPerSample = 4;

        for (int row = 0; row < (rowCount - 1); row += 4)
        {
            int replicateGroupIndex = 1;
            template.addWellGroup("" + sampleIndex, WellGroup.Type.SPECIMEN,
                    PlateService.get().createPosition(c, row, startCol),
                    PlateService.get().createPosition(c, row+3, startCol+colsPerSample-1));

            int replicateIndex = 1;
            for (int rowGroup=0; rowGroup < 2; rowGroup++)
            {
                for (int col = startCol; col < (startCol + colsPerSample); col++)
                {
                    List<Position> positions = new ArrayList<>();

                    positions.add(PlateService.get().createPosition(c, row + (rowGroup % 2), col));
                    positions.add(PlateService.get().createPosition(c, row + (rowGroup % 2) + 2, col));

                    WellGroupTemplate wg = template.addWellGroup("Specimen " + sampleIndex + "-" + replicateIndex++, WellGroup.Type.REPLICATE, positions);

                    // add an explicit order property to override the natural ordering
                    wg.setProperty(HighThroughputNabDataHandler.REPLICATE_GROUP_ORDER_PROPERTY, String.valueOf(replicateGroupIndex++));
                }
            }
            sampleIndex++;
        }
        return template;
    }

    private PlateTemplate createMultiVirusTemplate(PlateTemplate template, Container c, int rowCount, int colCount)
    {
        assert 16 == rowCount && 24 == colCount: "Only 16x24 multi-virus supported";
        PlateService plateService = PlateService.get();

        List<Position> cellControlPositions = new ArrayList<>();
        List<Position> virusControlPositions = new ArrayList<>();
        List<Position> virus2Positions = new ArrayList<>();
        List<Position> virus1Positions = new ArrayList<>();

        for (int row = 2; row < rowCount-2; row += 1)
        {
            cellControlPositions.add(plateService.createPosition(c, row, 0));
            virus2Positions.add(plateService.createPosition(c, row, 0));
            cellControlPositions.add(plateService.createPosition(c, row, colCount-1));
            virus1Positions.add(plateService.createPosition(c, row, colCount-1));

            virusControlPositions.add(plateService.createPosition(c, row, 1));
            virus1Positions.add(plateService.createPosition(c, row, 1));
            virusControlPositions.add(plateService.createPosition(c, row, colCount-2));
            virus2Positions.add(plateService.createPosition(c, row, colCount-2));
        }

        template.addWellGroup(NabManager.CELL_CONTROL_SAMPLE, WellGroup.Type.CONTROL, cellControlPositions);
        template.addWellGroup(NabManager.VIRUS_CONTROL_SAMPLE, WellGroup.Type.CONTROL, virusControlPositions);

        for (int col = 2; col < colCount-2; col += 2)
        {
            int specimen = (col/2);
            template.addWellGroup(String.format("Specimen %02d", specimen), WellGroup.Type.SPECIMEN,
                    plateService.createPosition(c, 1, col),
                    plateService.createPosition(c, rowCount-2, col+1));

            for (int row = 1; row < rowCount-1; row += 1)
            {
                virus2Positions.add(plateService.createPosition(c, row, col));
                virus1Positions.add(plateService.createPosition(c, row, col + 1));
            }

            for (int row = 1; row < rowCount-2; row += 2)
            {
                int repl = (row/2) + 1;
                template.addWellGroup("Virus 2, Specimen " + specimen + ", Replicate " + repl, WellGroup.Type.REPLICATE,
                        plateService.createPosition(c, row, col),
                        plateService.createPosition(c, row+1, col));
                template.addWellGroup("Virus 1, Specimen " + specimen + ", Replicate " + repl, WellGroup.Type.REPLICATE,
                        plateService.createPosition(c, row, col+1),
                        plateService.createPosition(c, row+1, col+1));
            }
        }
        template.addWellGroup("Virus 2", WellGroup.Type.VIRUS, virus2Positions);
        template.addWellGroup("Virus 1", WellGroup.Type.VIRUS, virus1Positions);

        return template;
    }

    public List<WellGroup.Type> getWellGroupTypes()
    {
        return Arrays.asList(
                WellGroup.Type.CONTROL, WellGroup.Type.VIRUS, WellGroup.Type.SPECIMEN,
                WellGroup.Type.REPLICATE, WellGroup.Type.OTHER);
    }

    private PlateTemplate create20Sample4VirusScreeningTemplate(PlateTemplate template, Container c, int rowCount, int colCount)
    {
        assert 16 == rowCount && 24 == colCount: "Only 16x24 multi-virus supported";
        PlateService plateService = PlateService.get();

        List<Position> cellControlPositions = new ArrayList<>();
        List<Position> virusControlPositions = new ArrayList<>();

        for (int row = 0; row < rowCount; row++)
        {
            cellControlPositions.add(plateService.createPosition(c, row, 0));
            virusControlPositions.add(plateService.createPosition(c, row, 1));
            virusControlPositions.add(plateService.createPosition(c, row, colCount-2));
            cellControlPositions.add(plateService.createPosition(c, row, colCount-1));
        }

        for (int col = 1; col < colCount-1; col++)
        {
            virusControlPositions.add(plateService.createPosition(c, 0, col));
            virusControlPositions.add(plateService.createPosition(c, 1, col));
            virusControlPositions.add(plateService.createPosition(c, rowCount-2, col));
            virusControlPositions.add(plateService.createPosition(c, rowCount-1, col));
        }
        template.addWellGroup(NabManager.CELL_CONTROL_SAMPLE, WellGroup.Type.CONTROL, cellControlPositions);
        template.addWellGroup(NabManager.VIRUS_CONTROL_SAMPLE, WellGroup.Type.CONTROL, virusControlPositions);

        List<Position> virus1Positions = new ArrayList<>();
        List<Position> virus2Positions = new ArrayList<>();
        List<Position> virus3Positions = new ArrayList<>();
        List<Position> virus4Positions = new ArrayList<>();

        for (int row = 0; row < rowCount; row += 2)
        {
            for (int col=1; col < colCount-1; col += 2)
            {
                virus2Positions.add(plateService.createPosition(c, row, col));
                virus4Positions.add(plateService.createPosition(c, row + 1, col));
                virus1Positions.add(plateService.createPosition(c, row, col + 1));
                virus3Positions.add(plateService.createPosition(c, row + 1, col + 1));
            }
        }
        template.addWellGroup("Virus 1", WellGroup.Type.VIRUS, virus1Positions);
        template.addWellGroup("Virus 2", WellGroup.Type.VIRUS, virus2Positions);
        template.addWellGroup("Virus 3", WellGroup.Type.VIRUS, virus3Positions);
        template.addWellGroup("Virus 4", WellGroup.Type.VIRUS, virus4Positions);

        int specimenCount = 1;
        List<Position> replicate1Pos = new ArrayList<>();
        List<Position> replicate2Pos = new ArrayList<>();
        List<Position> replicate3Pos = new ArrayList<>();
        List<Position> replicate4Pos = new ArrayList<>();

        for (int col=2; col < colCount-5; col += 4)
        {
            for (int row = 2; row < rowCount-2; row += 2)
            {
                template.addWellGroup(String.format("Specimen %02d", specimenCount), WellGroup.Type.SPECIMEN,
                        plateService.createPosition(c, row, col),
                        plateService.createPosition(c, row+1, col+3));

                // replicates
                replicate1Pos.add(plateService.createPosition(c, row, col));
                replicate1Pos.add(plateService.createPosition(c, row, col+2));

                replicate2Pos.add(plateService.createPosition(c, row, col+1));
                replicate2Pos.add(plateService.createPosition(c, row, col+3));

                replicate3Pos.add(plateService.createPosition(c, row+1, col));
                replicate3Pos.add(plateService.createPosition(c, row+1, col+2));

                replicate4Pos.add(plateService.createPosition(c, row+1, col+1));
                replicate4Pos.add(plateService.createPosition(c, row+1, col+3));

                template.addWellGroup("Virus 1, Specimen " + specimenCount, WellGroup.Type.REPLICATE, replicate1Pos);
                template.addWellGroup("Virus 2, Specimen " + specimenCount, WellGroup.Type.REPLICATE, replicate2Pos);
                template.addWellGroup("Virus 3, Specimen " + specimenCount, WellGroup.Type.REPLICATE, replicate3Pos);
                template.addWellGroup("Virus 4, Specimen " + specimenCount, WellGroup.Type.REPLICATE, replicate4Pos);

                specimenCount++;
                replicate1Pos.clear();
                replicate2Pos.clear();
                replicate3Pos.clear();
                replicate4Pos.clear();
            }
        }
        return template;
    }

    private PlateTemplate create240Sample1VirusScreeningTemplate(PlateTemplate template, Container c, int rowCount, int colCount)
    {
        assert 16 == rowCount && 24 == colCount: "Only 16x24 multi-virus supported";
        PlateService plateService = PlateService.get();

        template.addWellGroup(NabManager.VIRUS_CONTROL_SAMPLE, WellGroup.Type.CONTROL,
                plateService.createPosition(c, 2, 1),
                plateService.createPosition(c, 13, 1));
        template.addWellGroup(NabManager.CELL_CONTROL_SAMPLE, WellGroup.Type.CONTROL,
                plateService.createPosition(c, 2, 22),
                plateService.createPosition(c, 13, 22));

        int specimenCount = 1;
        for (int row = 2; row < rowCount-2; row ++)
        {
            for (int col=2; col < colCount-2; col++)
            {
                template.addWellGroup(String.format("Specimen %03d", specimenCount), WellGroup.Type.SPECIMEN,
                        plateService.createPosition(c, row, col),
                        plateService.createPosition(c, row, col));

                // replicate
                template.addWellGroup(String.format("Specimen %03d", specimenCount), WellGroup.Type.REPLICATE,
                        plateService.createPosition(c, row, col),
                        plateService.createPosition(c, row, col));

                specimenCount++;
            }
        }
        return template;
    }
}
