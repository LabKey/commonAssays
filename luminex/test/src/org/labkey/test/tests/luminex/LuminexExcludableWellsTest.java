/*
 * Copyright (c) 2011-2016 LabKey Corporation
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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.pages.AssayDesignerPage;
import org.labkey.test.util.DataRegionTable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({DailyA.class, Assays.class})
public final class LuminexExcludableWellsTest extends LuminexTest
{
    private static final String EXCLUDE_SELECTED_BUTTON = "excludeselected";
    private String excludedWellDescription = null;
    private String excludedWellType = null;
    private Set<String> excludedWells = null;

    @BeforeClass
    public static void updateAssayDefinition()
    {
        LuminexExcludableWellsTest init = (LuminexExcludableWellsTest)getCurrentTest();
        init.goToTestAssayHome();
        AssayDesignerPage assayDesigner = init._assayHelper.clickEditAssayDesign();
        assayDesigner.addTransformScript(RTRANSFORM_SCRIPT_FILE_LABKEY);
        assayDesigner.saveAndClose();
    }

    /**
     * test of well exclusion- the ability to exclude certain wells or analytes and add a comment as to why
     * preconditions: LUMINEX project and assay list exist.  Having the Multiple Curve data will speed up execution
     * but is not required
     * postconditions:  multiple curve data will be present, certain wells will be marked excluded
     */
    @Test
    public void testWellExclusion()
    {
        ensureMultipleCurveDataPresent(TEST_ASSAY_LUM);

        clickAndWait(Locator.linkContainingText(MULTIPLE_CURVE_ASSAY_RUN_NAME));

        //ensure multiple curve data present
        //there was a bug (never filed) that showed up with multiple curve data, so best to use that.

        String[] analytes = getListOfAnalytesMultipleCurveData();

        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("ExclusionComment");
        _customizeViewsHelper.applyCustomView();

        // standard titration well group exclusion
        excludedWellDescription = "Standard2";
        excludedWellType = "S3";
        excludedWells = new HashSet<>(Arrays.asList("C3", "D3"));
        excludeAllAnalytesForSingleWellTest("Standard", "C3", false, 2);

        // QC control titration well group exclusion
        excludedWellDescription = "Standard1";
        excludedWellType = "C2";
        excludedWells = new HashSet<>(Arrays.asList("E2", "F2"));
        excludeAllAnalytesForSingleWellTest("QC Control", "E2", false, 3);

        // unknown titration well group exclusion
        excludedWellDescription = "Sample 2";
        excludedWellType = "X25";
        excludedWells = new HashSet<>(Arrays.asList("E1", "F1"));
        excludeAllAnalytesForSingleWellTest("Unknown", "E1", true, 4);
        excludeOneAnalyteForSingleWellTest("Unknown", "E1", analytes[0], 6);

        // analyte exclusion
        excludeAnalyteForAllWellsTest(analytes[1], 7);

        // Check out the exclusion report
        clickAndWait(Locator.linkWithText("view excluded data"));
        assertTextPresent("multipleCurvesTestRun", 4);
        assertTextPresent("Changed for all analytes");
        assertTextPresent("No data to show.", 2); // no titration or singlepoint unknown exclusions
        assertTextPresent("exclude all for single well", 2);
        assertTextPresent("exclude single analyte for single well", 1);
        assertTextPresentInThisOrder("S3", "C2", "X25");
        assertTextPresent("ENV7", 3);
        assertTextPresent("ENV6", 3);
        assertTextPresentInThisOrder("C3", "E2", "E1");
    }

    /**
     * verify that a user can exclude every analyte for a single well, and that this
     * successfully applies to both the original well and its duplicates
     *
     * preconditions:  at run screen, wellName exists
     * postconditions: no change (exclusion is removed at end of test)
     * @param wellName name of well to excluse
     */
    private void excludeAllAnalytesForSingleWellTest(String wellRole, String wellName, boolean removeExclusion, int jobCount)
    {
        DataRegionTable table = new DataRegionTable("Data", this);
        table.setFilter("WellRole", "Equals", wellRole);
        clickExclusionMenuIconForWell(wellName);
        String comment = "exclude all for single well";
        setFormElement(Locator.name(EXCLUDE_COMMENT_FIELD), comment);
        clickButton(SAVE_CHANGES_BUTTON, 0);
        String expectedInfo = "INSERT replicate group exclusion (Description: " + excludedWellDescription + ", Type: " + excludedWellType + ")";
        verifyExclusionPipelineJobComplete(jobCount, expectedInfo, MULTIPLE_CURVE_ASSAY_RUN_NAME, comment);

        verifyWellGroupExclusion("Excluded for well replicate group: " + comment, new HashSet<>(Arrays.asList(getListOfAnalytesMultipleCurveData())));

        if (removeExclusion)
        {
            table.setFilter("WellRole", "Equals", wellRole);
            clickExclusionMenuIconForWell(wellName);
            click(Locator.radioButtonById("excludeselected"));
            clickButton(SAVE_CHANGES_BUTTON, 0);
            _extHelper.waitForExtDialog("Warning");
            clickButton("Yes", 0);
            expectedInfo = expectedInfo.replace("INSERT", "DELETE");
            verifyExclusionPipelineJobComplete(jobCount + 1, expectedInfo, MULTIPLE_CURVE_ASSAY_RUN_NAME, comment);
        }
    }

    private void excludeOneAnalyteForSingleWellTest(String wellRole, String wellName, String excludedAnalyte, int jobCount)
    {
        waitForText("Well Role");
        DataRegionTable table = new DataRegionTable("Data", this);
        table.setFilter("WellRole", "Equals", wellRole);
        clickExclusionMenuIconForWell(wellName);
        String exclusionComment = "exclude single analyte for single well";
        setFormElement(Locator.name(EXCLUDE_COMMENT_FIELD), exclusionComment);
        click(Locator.radioButtonById(EXCLUDE_SELECTED_BUTTON));
        clickExcludeAnalyteCheckBox(excludedAnalyte);
        clickButton(SAVE_CHANGES_BUTTON, 0);
        String expectedInfo = "INSERT replicate group exclusion (Description: " + excludedWellDescription + ", Type: " + excludedWellType + ")";
        verifyExclusionPipelineJobComplete(jobCount, expectedInfo, MULTIPLE_CURVE_ASSAY_RUN_NAME, exclusionComment, 1, 2);

        verifyWellGroupExclusion("Excluded for well replicate group: " + exclusionComment, new HashSet<>((Arrays.asList(excludedAnalyte))));
    }

    /**
     * Go through every analyte/well row with an exclusion comment.
     * Verify that the row has the expected comment, well, description, and type values
     *
     * @param expectedComment
     * @param analytes
     */
    private void verifyWellGroupExclusion(String expectedComment, Set<String> analytes)
    {
        DataRegionTable table = new DataRegionTable(DATA_TABLE_NAME, this);
        table.setFilter("Description", "Equals", excludedWellDescription);
        table.setFilter("ExclusionComment", "Is Not Blank", null);

        List<List<String>> vals = table.getFullColumnValues("Well", "Description", "Type", "Exclusion Comment", "Analyte");
        List<String> wells = vals.get(0);
        List<String> descriptions = vals.get(1);
        List<String> types = vals.get(2);
        List<String> comments = vals.get(3);
        List<String> analytesPresent = vals.get(4);

        String well;
        String description;
        String type;
        String comment;

        for(int i=0; i<wells.size(); i++)
        {
            well = wells.get(i);
            description = descriptions.get(i);
            type = types.get(i);
            comment = comments.get(i);
            String analyteVal = analytesPresent.get(i);

            try
            {
                if(matchesWell(description, type, well) && analytes.contains(analyteVal))
                {
                    assertEquals(expectedComment,comment);
                }

                if(expectedComment.equals(comment))
                {
                    assertTrue(matchesWell(description, type, well));
                    assertTrue(analytes.contains(analyteVal));
                }
            }
            catch (Exception rethrow)
            {
                log("well: " + well);
                log("description: " + description);
                log("type: " + type);
                log("Comment: "+ comment);
                log("Analyte: " + analyteVal);

                throw rethrow;
            }
        }

        table.clearFilter("Description");
        table.clearFilter("ExclusionComment");
    }

    //verifies if description, type, and well match the hardcoded values
    private boolean matchesWell(String description, String type, String well)
    {
        return excludedWellDescription.equals(description) &&
                excludedWellType.equals(type) &&
                excludedWells.contains(well);
    }

    /**
     * verify a user can exclude a single analyte for all wells
     * preconditions:  multiple curve data imported, on assay run page
     * post conditions: specified analyte excluded from all wells, with comment "Changed for all analytes"
     * @param analyte
     */
    private void excludeAnalyteForAllWellsTest(String analyte, int jobCount)
    {
        String exclusionPrefix = "Excluded for analyte: ";
        String comment ="Changed for all analytes";
        excludeAnalyteForRun(analyte, true, comment);
        verifyExclusionPipelineJobComplete(jobCount, "INSERT analyte exclusion", MULTIPLE_CURVE_ASSAY_RUN_NAME, comment);

        DataRegionTable table = new DataRegionTable(DATA_TABLE_NAME, this);
        table.setFilter("ExclusionComment", "Equals", exclusionPrefix + comment);
        waitForElement(Locator.paginationText(68)); // 4 are showing replicate group exclusion comment
        table.setFilter("Analyte", "Does Not Equal", analyte);
        waitForText("No data to show.");
        table.clearFilter("Analyte");
        table.clearFilter("ExclusionComment");
    }
}
