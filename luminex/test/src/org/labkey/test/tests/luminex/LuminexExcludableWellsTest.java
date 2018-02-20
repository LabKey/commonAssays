/*
 * Copyright (c) 2011-2017 LabKey Corporation
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
    public void testReplicateGroupExclusion()
    {
        int jobCount = 0;

        ensureMultipleCurveDataPresent(TEST_ASSAY_LUM);

        clickAndWait(Locator.linkContainingText(MULTIPLE_CURVE_ASSAY_RUN_NAME));

        //ensure multiple curve data present
        //there was a bug (never filed) that showed up with multiple curve data, so best to use that.

        String[] analytes = getListOfAnalytesMultipleCurveData();

        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("ExclusionComment");
        _customizeViewsHelper.applyCustomView();

        // standard titration well group exclusion
        excludedWellDescription = "Standard2";
        excludedWellType = "S3";
        excludedWells = new HashSet<>(Arrays.asList("C3", "D3"));
        excludeAllAnalytesForReplicateGroup("Standard", "C3", false, ++jobCount);

        // QC control titration well group exclusion
        excludedWellDescription = "Standard1";
        excludedWellType = "C2";
        excludedWells = new HashSet<>(Arrays.asList("E2", "F2"));
        excludeAllAnalytesForReplicateGroup("QC Control", "E2", false, ++jobCount);

        // unknown titration well group exclusion
        excludedWellDescription = "Sample 2";
        excludedWellType = "X25";
        excludedWells = new HashSet<>(Arrays.asList("E1", "F1"));
        excludeAllAnalytesForReplicateGroup("Unknown", "E1", true, ++jobCount);
        excludeOneAnalyteForReplicateGroup("Unknown", "E1", analytes[0], (jobCount += 2));

        // single well exclusion
        excludedWellDescription = "Sample 1";
        excludedWellType = "X6";
        excludedWells = new HashSet<>(Arrays.asList("A6"));
        String comment = "Replicate group with single well excluded";
        excludeOneWellFromReplicateGroup("Unknown","A6", comment, ++jobCount);

        // analyte exclusion
        excludeAnalyteForAllWellsTest(analytes[1], ++jobCount);

        log("Verifying the highlight for the excluded row");
        clickAndWait(Locator.linkWithText("view results"));
        DataRegionTable data = new DataRegionTable("Data",getDriver());
        data.setFilter("WellRole", "Equals", "Unknown");
        data.setFilter("Analyte","Equals","ENV6");
        data.setFilter("Well","Equals","A6");
        assertEquals("Excluded well is not highlighted",1,Locator.css(".labkey-error-row").findElements(data).size());

        log("Verifying the standard deviation is not blank");
        data.setFilter("StdDev","Is Blank");
        assertEquals("Standard deviation is blank",0,data.getDataRowCount());

        log("Verification for all the operation performed");
        clickAndWait(Locator.linkWithText("view excluded data"));

        log("Verifying the data for excluded analytes");
        DataRegionTable excludedAnalytesTable = new DataRegionTable("RunExclusion",getDriver());
        assertEquals("Wrong value in comment for Excluded Analytes","Changed for all analytes",excludedAnalytesTable.getDataAsText(0,"Comment"));
        assertEquals("Wrong value in Analytes for Excluded Analytes","ENV7",excludedAnalytesTable.getDataAsText(0,"Analytes"));
        assertEquals("Mismatch in the number of row count for Excluded Analytes",1,excludedAnalytesTable.getDataRowCount());

        log("Verifying the data for excluded titrations");
        DataRegionTable excludedTitrationsTable = new DataRegionTable("TitrationExclusion",getDriver());
        assertEquals("Rows should not be present in Excluded Titrations",0,excludedTitrationsTable.getDataRowCount());

        log("Verifying the data for excluded Single Point Unknowns");
        DataRegionTable excludedSinglePointUnknownsTable = new DataRegionTable("SinglepointUnknownExclusion",getDriver());
        assertEquals("Rows should not be present in  Single point Unknown Exclusion",0,excludedSinglePointUnknownsTable.getDataRowCount());


        log("Verifying the single well excluded from the replicate group");
        DataRegionTable table = new DataRegionTable("WellExclusion",getDriver());
        assertEquals("Mismatch in the number of row count for Excluded wells",4,table.getDataRowCount());
        table.setFilter("Comment","Equals","Replicate group with single well excluded");
        assertEquals("Wrong Type excluded for well exclusion","X6",table.getDataAsText(0,"Type"));
        assertEquals("Wrong Well excluded from the replicate","A6",table.getDataAsText(0,"Well"));
        table.clearAllFilters();

        log("Verifying the exclude single analyte for replicate group");
        table.setFilter("Comment","Equals","exclude single analyte for single well");
        assertEquals("Record for excluded single analyte for single well not found",1,table.getDataRowCount());
        assertEquals("Wrong Type excluded for well exclusion","X25",table.getDataAsText(0,"Type"));
        assertEquals("Wrong value for Excluded single analyte for single well","ENV6",table.getDataAsText(0,"Analytes"));
        assertEquals("Wrong Well excluded from the replicate"," ", table.getDataAsText(0,"Well"));
        table.clearAllFilters();
    }

    @Test
    public void testSinglePointExclusions()
    {
        int jobCount = 0;
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
        verifyExclusionPipelineJobComplete(++jobCount, "MULTIPLE singlepoint unknown exclusions", runId, "", 3, 1);

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
        verifyExclusionPipelineJobComplete(++jobCount, String.format("DELETE singlepoint unknown exclusion (Description: %1$s, Dilution: %2$s)", toDelete, dilutionDecimal), runId, "");

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
        verifyExclusionPipelineJobComplete(++jobCount, String.format("UPDATE singlepoint unknown exclusion (Description: %1s, Dilution: %2s)", toUpdate, dilutionDecimal), runId, "");

        exclusionReportPage = ExclusionReportPage.beginAt(this);
        exclusionReportPage.assertSinglepointUnknownExclusion(runId, toKeep, dilutionDecimal, analytes[0]);
        exclusionReportPage.assertSinglepointUnknownExclusion(runId, toUpdate, dilutionDecimal, analytes[1]);  //Verify update
    }
    /**
     * verify that a user can exclude every analyte for a replicate group, and that this
     * successfully applies to both the original well and its replicates
     *
     * preconditions:  at run screen, wellName exists
     * postconditions: no change (exclusion is removed at end of test)
     * @param wellName name of well to excluse
     */
    private void excludeAllAnalytesForReplicateGroup(String wellRole, String wellName, boolean removeExclusion, int jobCount)
    {
        DataRegionTable table = new DataRegionTable("Data", this);
        table.setFilter("WellRole", "Equals", wellRole);
        clickExclusionMenuIconForWell(wellName, true);
        String comment = "Exclude all for single well";
        setFormElement(Locator.name(EXCLUDE_COMMENT_FIELD), comment);
        clickButton(SAVE_CHANGES_BUTTON, 0);
        String expectedInfo = "INSERT replicate group exclusion (Description: " + excludedWellDescription + ", Type: " + excludedWellType + ")";
        verifyExclusionPipelineJobComplete(jobCount, expectedInfo, MULTIPLE_CURVE_ASSAY_RUN_NAME, comment);

        verifyWellGroupExclusion("Excluded for well replicate group: " + comment, new HashSet<>(Arrays.asList(getListOfAnalytesMultipleCurveData())));

        if (removeExclusion)
        {
            table.setFilter("WellRole", "Equals", wellRole);
            clickExclusionMenuIconForWell(wellName, true);
            click(Locator.radioButtonById("excludeselected"));
            clickSaveAndAcceptConfirm("Warning");
            expectedInfo = expectedInfo.replace("INSERT", "DELETE");
            verifyExclusionPipelineJobComplete(jobCount + 1, expectedInfo, MULTIPLE_CURVE_ASSAY_RUN_NAME, comment);
        }
    }

    private void excludeOneWellFromReplicateGroup(String wellRole, String wellName, String comment, int jobCount)
    {
        DataRegionTable table = new DataRegionTable("Data", this);
        table.setFilter("WellRole", "Equals", wellRole);
        clickExclusionMenuIconForWell(wellName, false);
        setFormElement(Locator.name(EXCLUDE_COMMENT_FIELD), comment);
        clickReplicateGroupCheckBoxSelectSingleWell("Replicate Group",wellName,true);
        clickButton(SAVE_CHANGES_BUTTON,0);
        String expectedInfo = "INSERT well exclusion (Description: " + excludedWellDescription + ", Type: " + excludedWellType + ", Well: " + wellName + ")";
        verifyExclusionPipelineJobComplete(jobCount, expectedInfo, MULTIPLE_CURVE_ASSAY_RUN_NAME, comment, 1, 1);

    }
    private void clickSaveAndAcceptConfirm(String dialogTitle)
    {
        clickButton(SAVE_CHANGES_BUTTON, 0);
        _extHelper.waitForExtDialog(dialogTitle);
        clickButton("Yes", 0);
    }

    private void excludeOneAnalyteForReplicateGroup(String wellRole, String wellName, String excludedAnalyte, int jobCount)
    {
        waitForText("Well Role");
        DataRegionTable table = new DataRegionTable("Data", this);
        table.setFilter("WellRole", "Equals", wellRole);
        clickExclusionMenuIconForWell(wellName, true);
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
        waitForElement(Locator.paginationText(69)); // 4 are showing replicate group exclusion comment
        table.setFilter("Analyte", "Does Not Equal", analyte);
        waitForText("No data to show.");
        table.clearFilter("Analyte");
        table.clearFilter("ExclusionComment");
    }
}
