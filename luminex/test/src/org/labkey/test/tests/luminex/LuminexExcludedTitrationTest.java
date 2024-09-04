/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
package org.labkey.test.tests.luminex;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.Daily;
import org.labkey.test.util.DataRegionTable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({Daily.class, Assays.class})
@BaseWebDriverTest.ClassTimeout(minutes = 8)
public final class LuminexExcludedTitrationTest extends LuminexTest
{
    /**
     * test of titration exclusion- the ability to exclude certain titrations and add a comment as to why
     * preconditions: LUMINEX project and assay list exist.  Having the Multiple Curve data will speed up execution
     * but is not required
     * postconditions:  multiple curve data will be present, wells for the given titration will be marked excluded
     */
    @Test
    public void testTitrationExclusion()
    {
        int jobCount = executeSelectRowCommand("pipeline", "Job").getRows().size();

        ensureMultipleCurveDataPresent(TEST_ASSAY_LUM);

        clickAndWait(Locator.linkContainingText(MULTIPLE_CURVE_ASSAY_RUN_NAME));
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("ExclusionComment");
        _customizeViewsHelper.applyCustomView();

        String titration = "Sample 1";
        String exclusionMessage =  "excluding all analytes for titration " + titration;
        excludeTitration(titration, exclusionMessage, MULTIPLE_CURVE_ASSAY_RUN_NAME, ++jobCount, 1, 1);
        verifyTitrationExclusion(titration, exclusionMessage, 70);

        titration = "Sample 2";
        String analyte = "ENV6";
        exclusionMessage =  "excluding " + analyte + " analyte for titration " + titration;
        excludeTitration(titration, exclusionMessage, MULTIPLE_CURVE_ASSAY_RUN_NAME, ++jobCount, 1, 1, analyte);
        verifyTitrationAnalyteExclusion(titration, analyte, exclusionMessage, 12);
    }

    @Test
    public void testCrossPlateTitration()
    {
        int jobCount = executeSelectRowCommand("pipeline", "Job").getRows().size();

        List<File> files = new ArrayList<>();
        files.add(TEST_ASSAY_LUM_FILE5);
        files.add(TEST_ASSAY_LUM_FILE6);
        files.add(TEST_ASSAY_LUM_FILE7);
        files.add(TEST_ASSAY_LUM_FILE8);
        files.add(TEST_ASSAY_LUM_FILE9);

        ensureMultipleCurveDataPresent(TEST_ASSAY_LUM);

        goToProjectHome();
        clickAndWait(Locator.linkWithText(TEST_ASSAY_LUM));

        DataRegionTable table = new DataRegionTable("Runs", getDriver());
        table.clickHeaderButton("Import Data");
        clickButton("Next");

        String runName = "Cross Plate titration";
        waitForElement(Locators.panelWebpartTitle.withText("Run Properties"));
        setFormElement(Locator.name("name"), runName);

        uploadAssayFiles(files);
        clickButton("Next");

        String titration = "Standard1-CrossplateTitration";
        assertTextPresent(titration);
        clickButton("Save and Finish");

        // Issue 51084: verify exclusions for a cross plate titration (exclusions applied to all DataIds for the titration)
        clickAndWait(Locator.linkContainingText(runName));
        String exclusionMessage =  "excluding all analytes for titration " + titration;
        excludeTitration(titration, exclusionMessage, runName, ++jobCount, 2, 1);

        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("ExclusionComment");
        _customizeViewsHelper.applyCustomView();
        verifyTitrationExclusion(titration, exclusionMessage, 12);

        titration = "Standard1";
        exclusionMessage =  "excluding one analyte for titration " + titration;
        excludeTitration(titration, exclusionMessage, runName, ++jobCount, 5, 2, "GS Analyte B");
        verifyTitrationAnalyteExclusion(titration, "GS Analyte B", exclusionMessage, 50);

        clickAndWait(Locator.linkWithText("view excluded data"));
        DataRegionTable region = new DataRegionTable("TitrationExclusion", this);
        region.setFilter("Description", "Equals", "Standard1-CrossplateTitration");
        assertEquals("Expected 2 rows for titration", 2, region.getDataRowCount());
        region.setFilter("Analytes", "Contains", "GS Analyte A");
        assertEquals("Expected 2 rows for analyte A", 2, region.getDataRowCount());
        region.setFilter("Analytes", "Contains", "GS Analyte B");
        assertEquals("Expected 2 rows for analyte B", 2, region.getDataRowCount());
        region.clearFilter("Analytes");
        region.setFilter("Description", "Equals", "Standard1");
        assertEquals("Expected 5 rows for titration", 5, region.getDataRowCount());
        region.setFilter("Analytes", "Contains", "GS Analyte A");
        assertEquals("Expected 0 rows for analyte B", 0, region.getDataRowCount());
        region.setFilter("Analytes", "Contains", "GS Analyte B");
        assertEquals("Expected 5 rows for analyte B", 5, region.getDataRowCount());
    }

    private void uploadAssayFiles(List<File> guavaFiles)
    {
        setInput(Locator.name("__primaryFile__"), guavaFiles);
    }

    private void verifyTitrationAnalyteExclusion(String excludedTitration, String excludedAnalyte, String exclusionMessage, int rowCount)
    {
        DataRegionTable region = new DataRegionTable("Data", this);

        region.setFilter("Description", "Equals", excludedTitration);
        region.setFilter("Analyte", "Contains", excludedAnalyte);
        waitForElement(Locator.paginationText(1, rowCount, rowCount));
        List<List<String>> vals = region.getFullColumnValues("Well", "Description", "Type", "Exclusion Comment", "Analyte");
        List<String> wells = vals.get(0);
        List<String> descriptions = vals.get(1);
        List<String> types = vals.get(2);
        List<String> comments = vals.get(3);
        List<String> analytesPresent = vals.get(4);

        String well;
        String description;
        String type;
        String comment;
        String analyte;

        for (int i=0; i<wells.size(); i++)
        {
            well = wells.get(i);
            log("well: " + well);
            description= descriptions.get(i);
            log("description: " + description);
            type = types.get(i);
            log("type: " + type);
            comment = comments.get(i);
            log("Comment: "+ comment);
            analyte= analytesPresent.get(i);
            analyte = analyte.contains("(") ? analyte.substring(0, 4) : analyte;
            log("Analyte: " + analyte);

            if (analyte.contains(excludedAnalyte) && description.equals(excludedTitration))
            {
                assertTrue(comment.contains(exclusionMessage));
            }

            if (comment.contains(exclusionMessage))
            {
                assertTrue(analyte.contains(excludedAnalyte) && description.equals(excludedTitration));
            }
        }

        region.setFilter("Analyte", "Does Not Contain", excludedAnalyte);
        region.setFilter("ExclusionComment", "Is Not Blank");
        waitForText("No data to show.");

        region.clearFilter("ExclusionComment");
        region.clearFilter("Analyte");
        region.clearFilter("Description");
    }

    private void verifyTitrationExclusion(String excludedTitration, String exclusionMessage, int rowCount)
    {
        DataRegionTable region = new DataRegionTable("Data", this);

        region.setFilter("Description", "Equals", excludedTitration);
        waitForElement(Locator.paginationText(1, rowCount, rowCount));
        List<List<String>> vals = region.getFullColumnValues("Well", "Description", "Type", "Exclusion Comment", "Analyte");
        List<String> wells = vals.get(0);
        List<String> descriptions = vals.get(1);
        List<String> types = vals.get(2);
        List<String> comments = vals.get(3);
        List<String> analytesPresent = vals.get(4);

        String well;
        String description;
        String type;
        String comment;
        String analyte;

        for (int i=0; i < wells.size(); i++)
        {
            well = wells.get(i);
            log("well: " + well);
            description= descriptions.get(i);
            log("description: " + description);
            type = types.get(i);
            log("type: " + type);
            comment = comments.get(i);
            log("Comment: "+ comment);
            analyte= analytesPresent.get(i);
            analyte = analyte.substring(0, 4);
            log("Analyte: " + analyte);

            if (description.equals(excludedTitration))
            {
                assertTrue(comment.contains(exclusionMessage));
            }
        }

        region.setFilter("Description", "Does Not Equal", excludedTitration);
        region.setFilter("ExclusionComment", "Is Not Blank");
        waitForText("No data to show.");

        region.clearFilter("ExclusionComment");
        region.clearFilter("Description");
    }

}
