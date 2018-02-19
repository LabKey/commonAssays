/*
 * Copyright (c) 2013-2017 LabKey Corporation
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
import org.labkey.test.Locator;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.DataRegionTable;

import java.util.List;

import static org.junit.Assert.assertTrue;

@Category({DailyA.class, Assays.class})
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
        ensureMultipleCurveDataPresent(TEST_ASSAY_LUM);

        clickAndWait(Locator.linkContainingText(MULTIPLE_CURVE_ASSAY_RUN_NAME));
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("ExclusionComment");
        _customizeViewsHelper.applyCustomView();

        String titration = "Sample 1";
        String exclusionMessage =  "excluding all analytes for titration " + titration;
        excludeTitration(titration, exclusionMessage, MULTIPLE_CURVE_ASSAY_RUN_NAME, 2);
        verifyTitrationExclusion(titration, exclusionMessage);

        titration = "Sample 2";
        String analyte = "ENV6";
        exclusionMessage =  "excluding " + analyte + " analyte for titration " + titration;
        excludeTitration(titration, exclusionMessage, MULTIPLE_CURVE_ASSAY_RUN_NAME, 3, analyte);
        verifyTitrationAnalyteExclusion(titration, analyte, exclusionMessage);
    }

    private void verifyTitrationAnalyteExclusion(String excludedTitration, String excludedAnalyte, String exclusionMessage)
    {
        DataRegionTable region = new DataRegionTable("Data", this);

        region.setFilter("Description", "Equals", excludedTitration);
        region.setFilter("Analyte", "Contains", excludedAnalyte);
        waitForElement(Locator.paginationText(1, 12, 12));
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
            analyte = analyte.substring(0, 4);
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

    private void verifyTitrationExclusion(String excludedTitration, String exclusionMessage)
    {
        DataRegionTable region = new DataRegionTable("Data", this);

        region.setFilter("Description", "Equals", excludedTitration);
        waitForElement(Locator.paginationText(1, 70, 70));
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
