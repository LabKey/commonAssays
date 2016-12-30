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

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.components.luminex.dialogs.SinglepointExclusionDialog;
import org.labkey.test.pages.AssayDesignerPage;
import org.labkey.test.pages.luminex.ExclusionReportPage;
import org.labkey.test.pages.luminex.LuminexImportWizard;
import org.labkey.test.util.DataRegionTable;

import java.io.File;
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
    private static final File SINGLEPOINT_RUN_FILE = TestFileUtils.getSampleData("Luminex/01-11A12-IgA-Biotin.xls");


    @BeforeClass
    public static void updateAssayDefinition()
    {
        LuminexExcludableWellsTest init = (LuminexExcludableWellsTest)getCurrentTest();
        init.goToTestAssayHome();
        AssayDesignerPage assayDesigner = init._assayHelper.clickEditAssayDesign();
        assayDesigner.addTransformScript(RTRANSFORM_SCRIPT_FILE_LABKEY);
        assayDesigner.saveAndClose();
    }

    @Before
    public void preTest()
    {
        cleanupPipelineJobs();
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
        excludeAllAnalytesForSingleWellTest("Standard", "C3", false, 1);

        // QC control titration well group exclusion
        excludedWellDescription = "Standard1";
        excludedWellType = "C2";
        excludedWells = new HashSet<>(Arrays.asList("E2", "F2"));
        excludeAllAnalytesForSingleWellTest("QC Control", "E2", false, 2);

        // unknown titration well group exclusion
        excludedWellDescription = "Sample 2";
        excludedWellType = "X25";
        excludedWells = new HashSet<>(Arrays.asList("E1", "F1"));
        excludeAllAnalytesForSingleWellTest("Unknown", "E1", true, 3);
        excludeOneAnalyteForSingleWellTest("Unknown", "E1", analytes[0], 5);

        // analyte exclusion
        excludeAnalyteForAllWellsTest(analytes[1], 6);

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


    @Test
    public void testSinglePointExclusions()
    {
        String runId = "Singlepoint Exclusion run";
        goToTestAssayHome();

        LuminexImportWizard importWizard = new LuminexImportWizard(this);
        // Create a run that imports 1 file
        importWizard.createNewAssayRun(runId,
                null,
                (wizard) -> wizard.addFilesToAssayRun(SINGLEPOINT_RUN_FILE),
                (wizard) -> {
                    wizard.setStandardRole("Standard1", false);
                    wizard.setQCControlRole("Standard1", true);
                });

        String[] analytes = {"ENV1", "ENV2", "ENV3",
                "ENV4", "Blank"};
        clickAndWait(Locator.linkWithText(runId));

        SinglepointExclusionDialog dialog = SinglepointExclusionDialog.beginAt(this.getDriver());

        String  toDelete = "112",
                toUpdate = "113",
                toKeep = "114"
        ;

        String dilution = "200";
        String dilutionDecimal = "200.0";

        //Check dialog Exclusion info field
        assertTextNotPresent("1 analyte excluded");
        dialog.selectDilution(toDelete, dilution);
        dialog.checkAnalyte(analytes[0]);
        dialog.selectDilution(toUpdate, dilution);    //Exclusion info not set until singlepoint is deselected
        assertTextPresent("1 analyte excluded");

        dialog.selectDilution(toDelete, dilution);
        dialog.checkAnalyte(analytes[1]);
        dialog.selectDilution(toUpdate, dilution);
        assertTextNotPresent("1 analyte excluded");
        assertTextPresent("2 analytes excluded");

        dialog.selectDilution(toDelete, dilution);
        dialog.uncheckAnalyte(analytes[0]);
        dialog.uncheckAnalyte(analytes[1]);
        dialog.selectDilution(toUpdate, dilution);   //Exclusion info not set until singlepoint is deselected
        assertTextNotPresent("2 analytes excluded");

        dialog.selectDilution(toKeep, dilution);
        dialog.checkAnalyte(analytes[0]);
        dialog.selectDilution(toDelete, dilution);
        dialog.checkAnalyte(analytes[0]);
        dialog.selectDilution(toUpdate, dilution);
        dialog.checkAnalyte(analytes[0]);
        dialog.checkAnalyte(analytes[1]);
        dialog.selectDilution(toKeep, dilution);    //Exclusion info not set until singlepoint is deselected
        assertTextPresent("1 analyte excluded", 2);
        assertTextPresent("2 analytes excluded");

        //Save Exclusion
        clickSaveAndAcceptConfirm("Confirm Exclusions");
        verifyExclusionPipelineJobComplete(1, "MULTIPLE singlepoint unknown exclusions", runId, "", 3, 1);

        //Check ExclusionReport for changes
        ExclusionReportPage exclusionReportPage = ExclusionReportPage.beginAt(this);
        exclusionReportPage.assertSinglepointUnknownExclusion(runId, toKeep, dilutionDecimal, analytes[0]);
        exclusionReportPage.assertSinglepointUnknownExclusion(runId, toDelete, dilutionDecimal, analytes[0]);
        exclusionReportPage.assertSinglepointUnknownExclusion(runId, toUpdate, dilutionDecimal, analytes[0], analytes[1]);

        //Verify we can delete an exclusion
        goToTestAssayHome();
        clickAndWait(Locator.linkWithText(runId));

        dialog = SinglepointExclusionDialog.beginAt(this.getDriver());
        assertTextPresent("1 analyte excluded", 2);    //Verify exclusion retained across page loads
        assertTextPresent("2 analytes excluded");   //Verify exclusion retained across page loads
        dialog.selectDilution(toDelete, dilution);
        dialog.uncheckAnalyte(analytes[0]);
        clickSaveAndAcceptConfirm("Confirm Exclusions");
        verifyExclusionPipelineJobComplete(2, String.format("DELETE singlepoint unknown exclusion (Description: %1$s, Dilution: %2$s)", toDelete, dilutionDecimal), runId, "");

        //Check ExclusionReport for changes
        exclusionReportPage = ExclusionReportPage.beginAt(this);
        //Check deleted exclusion is absent
        exclusionReportPage.assertSinglepointUnknownExclusionNotPresent(runId, toDelete, dilutionDecimal, analytes[0], analytes[1]);
        //Check retained exclusion is still present
        exclusionReportPage.assertSinglepointUnknownExclusion(runId, toKeep, dilutionDecimal, analytes[0]);
        exclusionReportPage.assertSinglepointUnknownExclusion(runId, toUpdate, dilutionDecimal, analytes[0], analytes[1]);

        //Verify we can update an exclusion
        goToTestAssayHome();
        clickAndWait(Locator.linkWithText(runId));

        dialog = SinglepointExclusionDialog.beginAt(this.getDriver());
        assertTextPresent("1 analyte excluded");   //Verify deletion (2 occurrences -> 1)
        assertTextPresent("2 analytes excluded");   //Verify exclusion retained
        dialog.selectDilution(toUpdate, dilution);
        dialog.uncheckAnalyte(analytes[0]);
        clickSaveAndAcceptConfirm("Confirm Exclusions");
        verifyExclusionPipelineJobComplete(3, String.format("UPDATE singlepoint unknown exclusion (Description: %1s, Dilution: %2s)", toUpdate, dilutionDecimal), runId, "");

        exclusionReportPage = ExclusionReportPage.beginAt(this);
        exclusionReportPage.assertSinglepointUnknownExclusion(runId, toKeep, dilutionDecimal, analytes[0]);
        exclusionReportPage.assertSinglepointUnknownExclusion(runId, toUpdate, dilutionDecimal, analytes[1]);  //Verify update
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
            clickSaveAndAcceptConfirm("Warning");
            expectedInfo = expectedInfo.replace("INSERT", "DELETE");
            verifyExclusionPipelineJobComplete(jobCount + 1, expectedInfo, MULTIPLE_CURVE_ASSAY_RUN_NAME, comment);
        }
    }

    private void clickSaveAndAcceptConfirm(String dialogTitle)
    {
        clickButton(SAVE_CHANGES_BUTTON, 0);
        _extHelper.waitForExtDialog(dialogTitle);
        clickButton("Yes", 0);
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
