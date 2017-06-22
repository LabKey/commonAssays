/*
 * Copyright (c) 2014-2017 LabKey Corporation
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

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.util.JunitUtil;
import org.labkey.api.util.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlateParserTests
{
    private final double DELTA = 1e-8;
    private final Mockery _context;

    public PlateParserTests()
    {
        _context = new Mockery();
    }

    public double[][] parse(File file, PlateTemplate template) throws ExperimentException
    {
        SinglePlateNabDataHandler handler = new SinglePlateNabDataHandler();
        return handler.getCellValues(file, template);
    }

    public void assertCells(String nabFile, double[][] expected, File file, PlateTemplate template) throws ExperimentException
    {
        double[][] actual = parse(file, template);
        if (actual == null)
            Assert.fail(nabFile + ": failed to parse file");

        Assert.assertEquals(nabFile + ": expected and actual rows differ", expected.length, actual.length);

        for (int i = 0; i < expected.length; i++)
        {
            Assert.assertArrayEquals(nabFile + ": expected and actual cells differ on row " + i, expected[i], actual[i], DELTA);
        }
    }

    public double[][] parseExpected(File file) throws IOException
    {
        List<double[]> values = new ArrayList<>(8);
        for (String line : Files.readAllLines(file.toPath(), Charset.defaultCharset()))
        {
            String[] s = line.split("\\t");
            double[] cells = new double[s.length];
            for (int i = 0; i < s.length; i++)
            {
                cells[i] = Double.parseDouble(s[i]);
            }
            values.add(cells);
        }
        return values.toArray(new double[values.size()][]);
    }

    // Create a plate template that has enough information to parse the data file
    public PlateTemplate template(String name, final int rows, final int columns)
    {
        final PlateTemplate template = _context.mock(PlateTemplate.class, name);
        _context.checking(new Expectations() {{
            allowing(template).getRows();
            will(returnValue(rows));
            allowing(template).getColumns();
            will(returnValue(columns));
        }});
        return template;
    }

    private static List<Pair<String, String>> singlePlateTests = Arrays.asList(
            // The ".expected.tsv" files is a simple grid of numbers and are used to check the plate reader formats
            // We also check that the .expected.tsv validates against itself.
            Pair.of("Nab/m0902051;3997.expected.tsv", "Nab/m0902051;3997.expected.tsv"),

            Pair.of("Nab/m0902051;3997.xls", "Nab/m0902051;3997.expected.tsv"),
            Pair.of("Nab/m0902055;4001.xlsx", "Nab/m0902055;4001.expected.tsv"),
            Pair.of("Nab/Luc5Samples02NotLocked1.xls", "Nab/Luc5Samples02NotLocked1.expected.tsv"),
            Pair.of("Nab/16AUG11 KK CD3-1-1.8.xls", "Nab/16AUG11 KK CD3-1-1.8.expected.tsv"),
            // Issue 22153: detect xls file without extension
            Pair.of("Nab/seaman/MS010407", "Nab/seaman/MS010407.expected.tsv"),
            // TODO: contains multiple plates
            //Pair.of("Nab/seaman/RC121306.xls", "Nab/seaman/RC121306.expected.tsv")
            Pair.of("Nab/SpectraMax/20140612_0588.txt", "Nab/SpectraMax/20140612_0588.expected.tsv"),
            Pair.of("Nab/SpectraMax/20140102_140152 0063_PLATE.002.txt", "Nab/SpectraMax/20140102_140152 0063_PLATE.002.expected.tsv"),
            Pair.of("Nab/EnVision/4 plate data set _001.csv", "Nab/EnVision/4 plate data set _001.expected.tsv"),
            Pair.of("Nab/sheet2row6/ID50.xlsx", "Nab/sheet2row6/ID50.expected.tsv")
    );

    @Test
    public void parseSinglePlates() throws Exception
    {
        for (Pair<String, String> test : singlePlateTests)
        {
            File nabFile = JunitUtil.getSampleData(null, test.first);
            File expectedFile = JunitUtil.getSampleData(null, test.second);

            final double[][] expected = parseExpected(expectedFile);
            PlateTemplate template = template(nabFile.getName(), expected.length, expected[0].length);
            assertCells(test.first, expected, nabFile, template);
        }
    }

    private static List<Pair<String, String>> multiPlateTests = Arrays.asList(
            Pair.of("Nab/seaman/RC121306.xls", "Nab/seaman/RC121306.expected.tsv")
    );

    @Test
    public void parseMultiPlate() throws Exception
    {
        // TODO:
    }
}
