/*
 * Copyright (c) 2016-2017 LabKey Corporation
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
package org.labkey.test.components.luminex.exclusionreport;

import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by iansigmon on 12/29/16.
 */
public class ExcludedSinglepointUnknownsWebpart extends BaseExclusionWebpart
{
    private static final String TITLE = "Excluded Singlepoint Unknowns";
    private static final String TABLE_NAME = "SinglepointUnknownExclusion";
    private static final String RUN_COLUMN = "DataId/Run",
                                DESCRIPTION_COLUMN = "Description",
                                DILUTION_COLUMN = "Dilution",
                                ANALYTES_COLUMN = "Analytes";

    protected ExcludedSinglepointUnknownsWebpart(WebElement componentElement, WebDriver driver)
    {
        super(componentElement, driver);
    }

    public static ExcludedSinglepointUnknownsWebpartFinder ExcludedSinglepointUnknownsWebpart(WebDriver driver)
    {
        return new ExcludedSinglepointUnknownsWebpartFinder(driver).withTitle(TITLE);
    }

    public static class ExcludedSinglepointUnknownsWebpartFinder extends WebPartFinder<ExcludedSinglepointUnknownsWebpart, ExcludedSinglepointUnknownsWebpartFinder>
    {
        public ExcludedSinglepointUnknownsWebpartFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected ExcludedSinglepointUnknownsWebpart construct(WebElement el, WebDriver driver)
        {
            return new ExcludedSinglepointUnknownsWebpart(el, driver);
        }
    }

    public void assertExclusionNotPresent(String runName, String description, String dilution, String[] analytes)
    {
        DataRegionTable table = getTable();
        table.setFilter(DILUTION_COLUMN, "Equals", dilution);
        table.setFilter(DESCRIPTION_COLUMN, "Equals", description);
        assertEquals("Exclusion was found", 0, table.getDataRowCount());

        table.clearFilter(DILUTION_COLUMN);
        table.clearFilter(DESCRIPTION_COLUMN);
    }

    public void assertExclusionPresent(String runName, String description, String dilution, String[] analytes)
    {
        DataRegionTable table = getTable();
        table.setFilter(DILUTION_COLUMN, "Equals", dilution);
        table.setFilter(DESCRIPTION_COLUMN, "Equals", description);
        assertEquals("Exclusion was found", 1, table.getDataRowCount());

        assertEquals("Exclusion doesn't match criteria",
                Arrays.asList(runName, description, dilution),
                table.getRowDataAsText(0, RUN_COLUMN, DESCRIPTION_COLUMN, DILUTION_COLUMN));

        //Breaking out analyte check since order can't be trusted
        String analyteValue = table.getDataAsText(0, ANALYTES_COLUMN);
        List<String> analyteValues = Arrays.asList(analyteValue.split(", "));
        analyteValues.sort(String.CASE_INSENSITIVE_ORDER);

        List<String> expectedAnalytes = Arrays.asList(analytes);
        expectedAnalytes.sort(String.CASE_INSENSITIVE_ORDER);

        assertEquals("Exclusion analytes not as expected", expectedAnalytes, analyteValues);

        table.clearFilter(DILUTION_COLUMN);
        table.clearFilter(DESCRIPTION_COLUMN);
    }

    @Override
    protected String getTableName()
    {
        return TABLE_NAME;
    }
}
