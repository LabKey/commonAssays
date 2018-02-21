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
package org.labkey.test.tests.luminex;

import org.jetbrains.annotations.Nullable;
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
import org.labkey.test.util.DataRegionTable;

import java.io.File;

@Category({DailyA.class, Assays.class})
public final class LuminexExclusionRetentionTest extends LuminexTest
{
    private static final File BASE_RUN_FILE = TestFileUtils.getSampleData("Luminex/01-11A12-IgA-Biotin.xls");
    private static final File REIMPORT_FILE = TestFileUtils.getSampleData("Luminex/01-11A12-IgA-Biotin_reimport.xls");
    private static final String RUN_NAME = "ReimportTestRun";

    private static final String[] ANALYTE_NAMES = new String[] {"ENV1","ENV2","ENV4","ENV5","BLANK"};

    @BeforeClass
    public static void updateAssayDefinition()
    {
        LuminexExclusionRetentionTest init = (LuminexExclusionRetentionTest)getCurrentTest();
        init.goToTestAssayHome();
        AssayDesignerPage assayDesigner = init._assayHelper.clickEditAssayDesign();
        assayDesigner.addTransformScript(RTRANSFORM_SCRIPT_FILE_LABKEY);
        assayDesigner.saveAndClose();
    }

    @Before
    public void setupTestRun()
    {
        goToTestAssayHome();
        cleanupPipelineJobs();
        cleanupRuns();
        addInitialAssayRun();
        clickAndWait(Locator.linkContainingText(RUN_NAME));
    }

    private void addInitialAssayRun()
    {
        goToTestAssayHome();
        createNewAssayRun(TEST_ASSAY_LUM, RUN_NAME);
        addFilesToAssayRun(BASE_RUN_FILE);
        clickButton("Next"); // run
        uncheckCheckbox(Locator.tagWithName("input", "_titrationRole_standard_Standard1"));
        checkCheckbox(Locator.tagWithName("input", "_titrationRole_qccontrol_Standard1"));
        waitForText("Save and Finish");
        clickButton("Save and Finish");
    }


    private void cleanupRuns()
    {
        goToTestAssayHome();
        DataRegionTable runTable = new DataRegionTable("Runs", this.getWrappedDriver());

        if(runTable.getDataRowCount() > 0)
        {
            runTable.checkAll();
            runTable.clickHeaderButton("Delete");
            clickButton("Confirm Delete");
        }
    }

    @Test
    public void testWellExclusionRetention()
    {
        String matchingAnalyte = ANALYTE_NAMES[0];
        String changedAnalyte = ANALYTE_NAMES[1];

        //Add well exclusion that matches
        String wellMatchComment = "well match";
        excludeReplicateGroup("A1", "X1", "111", wellMatchComment, matchingAnalyte); //should be retained after import
        clickButton("No", 0);

        //Add well exclusion that partially matches
        String partialMatchComment = "partial well match";
        excludeReplicateGroup("A2", "X2", "112", partialMatchComment, matchingAnalyte, changedAnalyte);   //Should be kept, but only have one analyte after import
        clickButton("No", 0);

        //Add well exclusion that does not match Analyte
        String wellNonMatchAnalyteComment = "well Analyte non-match";
        excludeReplicateGroup("A3", "X3", "113", wellNonMatchAnalyteComment, changedAnalyte); //Analyte changed in re-import file (new value ENV66)
        clickButton("No", 0);

        //Add well exclusion that does not match type
        String wellNonMatchTypeComment = "well Type non-match";
        excludeReplicateGroup("A4", "X4", "114", wellNonMatchTypeComment, matchingAnalyte); //Well changed in re-import file (new value X2)
        clickButton("No", 0);

        //Add well exclusion that does not match type
        String wellNonMatchDescriptionComment = "well description non-match";
        excludeReplicateGroup("G2", "S2", "Standard1", wellNonMatchDescriptionComment, matchingAnalyte); //Description changed in re-import file (new value Standard1a)
        clickButton("No", 0);

        String singleWellRemovedFromTheReplicate = "Replicate group with single well excluded";
        String description = "116";
        String type = "X6";
        String well = "A6";
        excludeOneWellFromReplicateGroup("Unknown",well,singleWellRemovedFromTheReplicate,description);
        clickButton("No", 0);

        // wait for the last pipeline job to finish
        goToModule("Pipeline");
        DataRegionTable table = new DataRegionTable("StatusFiles", getDriver());
        table.setFilter("Info", "Equals", "INSERT well exclusion (Description: " + description + ", Type: " + type + ", Well: " + well + ")");
        waitForPipelineJobsToComplete(1, "Luminex Exclusion: " + RUN_NAME, false);

        // go to the run results grid from the pipeline job list
        clickAndWait(Locator.linkContainingText("Luminex Exclusion: " + RUN_NAME));

        //Re-import
        reimportAndReplaceRunFile(BASE_RUN_FILE, REIMPORT_FILE, "Standard1a", "3 exclusions were not matched and will be lost");

        //verify exclusion page
        clickAndWait(Locator.linkContainingText(RUN_NAME));

        // Check out the exclusion report
        clickAndWait(Locator.linkWithText("view excluded data"));
        assertTextPresent(wellMatchComment);
        assertTextPresent(partialMatchComment);
        assertTextPresent(matchingAnalyte, 3);  //Once each for: match, partial-match, and single well exclusion

        assertTextNotPresent(wellNonMatchAnalyteComment);
        assertTextNotPresent(wellNonMatchTypeComment);
        assertTextNotPresent(wellNonMatchDescriptionComment);
        assertTextNotPresent(changedAnalyte);
        assertTextPresent("No data to show.", 3);
    }

    @Test
    public void testTitrationExclusionRetention()
    {
        String matchingAnalyte = ANALYTE_NAMES[0];

        //Add titration exclusion that matches
        String titrationMatchComment = "titration match";
        excludeTitration("Standard1", titrationMatchComment, RUN_NAME, 1, matchingAnalyte);

        //Re-import
        reimportAndReplaceRunFile(BASE_RUN_FILE, BASE_RUN_FILE, "Standard1", null);

        //verify exclusion page
        clickAndWait(Locator.linkContainingText(RUN_NAME));
        clickAndWait(Locator.linkWithText("view excluded data"));
        assertTextPresent(titrationMatchComment);
        assertTextPresent(matchingAnalyte);
        assertTextPresent("No data to show.", 3);

        goToTestAssayHome();
        clickAndWait(Locator.linkContainingText(RUN_NAME));

        //Re-import
        reimportAndReplaceRunFile(BASE_RUN_FILE, REIMPORT_FILE, "Standard1a", "1 exclusion was not matched and will be lost");

        //verify exclusion page
        clickAndWait(Locator.linkContainingText(RUN_NAME));

        // Check out the exclusion report
        clickAndWait(Locator.linkWithText("view excluded data"));
        assertTextNotPresent(titrationMatchComment);
        assertTextNotPresent(matchingAnalyte);
        assertTextPresent("No data to show.", 4);
    }

    @Test
    public void testAnalyteExclusionRetention()
    {
        String matchingAnalyte = ANALYTE_NAMES[0];
        String changedAnalyte = ANALYTE_NAMES[1];

        //Add analyte exclusion that does not match
        String analyteNonMatchComment = "analyte non-match";
        excludeAnalyteForRun(changedAnalyte, true, analyteNonMatchComment);
        verifyExclusionPipelineJobComplete(1, "INSERT analyte exclusion", RUN_NAME, analyteNonMatchComment);

        //Re-import
        reimportAndReplaceRunFile(BASE_RUN_FILE, REIMPORT_FILE, "Standard1a", "1 exclusion was not matched and will be lost");

        //verify exclusion page
        clickAndWait(Locator.linkContainingText(RUN_NAME));
        clickAndWait(Locator.linkWithText("view excluded data"));
        assertTextNotPresent(analyteNonMatchComment);
        assertTextPresent("No data to show.", 4);

        goToTestAssayHome();
        clickAndWait(Locator.linkContainingText(RUN_NAME));

        //Add analyte exclusion that matches
        String analyteMatchComment = "analyte match";
        excludeAnalyteForRun(matchingAnalyte, true, analyteMatchComment);
        verifyExclusionPipelineJobComplete(2, "INSERT analyte exclusion", RUN_NAME, analyteMatchComment, 1, 2);

        //Re-import
        reimportAndReplaceRunFile(REIMPORT_FILE, BASE_RUN_FILE, "Standard1", null);

        //verify retained exclusion
        clickAndWait(Locator.linkContainingText(RUN_NAME));
        clickAndWait(Locator.linkWithText("view excluded data"));
        assertTextPresent(analyteMatchComment);
        assertTextNotPresent(analyteNonMatchComment);
        assertTextPresent("No data to show.", 3);
    }

    @Test
    public void testSinglepointExclusionRetention()
    {
        SinglepointExclusionDialog dialog = SinglepointExclusionDialog.beginAt(this.getDriver());

        String matchingAnalyte = ANALYTE_NAMES[0];
        String changedAnalyte = ANALYTE_NAMES[1];

        String  toKeep = "120",
                partialAnalyteMatch = "121",
                analyteNotMatched = "122",
                dilutionNotMatched = "123";

        String dilution = "200";
        String dilutionDecimal = "200.0";

        //Check dialog Exclusion info field
        //Add matching exclusion
        assertTextNotPresent("1 analyte excluded");
        dialog.selectDilution(toKeep, dilution);
        dialog.checkAnalyte(matchingAnalyte);
        //Add partial match
        dialog.selectDilution(partialAnalyteMatch, dilution);    //Exclusion info not set until singlepoint is deselected
        dialog.checkAnalyte(matchingAnalyte);
        dialog.checkAnalyte(changedAnalyte);

        //Add analyte not matched
        dialog.selectDilution(analyteNotMatched, dilution);    //Exclusion info not set until singlepoint is deselected
        dialog.checkAnalyte(changedAnalyte);
        dialog.selectDilution(dilutionNotMatched, dilution);    //Exclusion info not set until singlepoint is deselected
        dialog.checkAnalyte(matchingAnalyte);
        dialog.selectDilution(toKeep, dilution);
        assertTextPresent("1 analyte excluded", 3);
        assertTextPresent("2 analytes excluded");

        //Save Exclusion
        _extHelper.clickExtButton("Save", 0);
        _extHelper.clickExtButton("Yes", 0);
        verifyExclusionPipelineJobComplete(1, "MULTIPLE singlepoint unknown exclusions", RUN_NAME, "", 4, 1);

        //Verify exclusions retained
        ExclusionReportPage exclusionReportPage = ExclusionReportPage.beginAt(this);
        exclusionReportPage.assertSinglepointUnknownExclusion(RUN_NAME, toKeep, dilutionDecimal, matchingAnalyte);
        exclusionReportPage.assertSinglepointUnknownExclusion(RUN_NAME, partialAnalyteMatch, dilutionDecimal, matchingAnalyte, changedAnalyte);
        exclusionReportPage.assertSinglepointUnknownExclusion(RUN_NAME, analyteNotMatched, dilutionDecimal, changedAnalyte);
        exclusionReportPage.assertSinglepointUnknownExclusion(RUN_NAME, dilutionNotMatched, dilutionDecimal, matchingAnalyte);

        //Re-import
        goToTestAssayHome();
        clickAndWait(Locator.linkWithText(RUN_NAME));
        reimportAndReplaceRunFile(BASE_RUN_FILE, REIMPORT_FILE, "Standard1a", "2 exclusions were not matched and will be lost");

        //Verify exclusion retained
        exclusionReportPage = ExclusionReportPage.beginAt(this);
        exclusionReportPage.assertSinglepointUnknownExclusion(RUN_NAME, toKeep, dilutionDecimal, matchingAnalyte);

        //verify partial analyte exclusion retained
        exclusionReportPage.assertSinglepointUnknownExclusion(RUN_NAME, partialAnalyteMatch, dilutionDecimal, matchingAnalyte);

        //Verify changes not retained
        exclusionReportPage.assertSinglepointUnknownExclusionNotPresent(RUN_NAME, analyteNotMatched, dilutionDecimal, changedAnalyte);
        exclusionReportPage.assertSinglepointUnknownExclusionNotPresent(RUN_NAME, dilutionNotMatched, dilutionDecimal, matchingAnalyte);
    }

    private void excludeOneWellFromReplicateGroup(String wellRole,String wellName,String comment,String description)
    {
        DataRegionTable table = new DataRegionTable("Data", this);
        table.clearAllFilters("Type");
        table.setFilter("WellRole", "Equals", wellRole);
        table.setFilter("Description","Equals",description);
        clickExclusionMenuIconForWell(wellName, false);
        setFormElement(Locator.name(EXCLUDE_COMMENT_FIELD), comment);
        clickReplicateGroupCheckBoxSelectSingleWell("Replicate Group",wellName,true);
        clickButton(SAVE_CHANGES_BUTTON,0);

    }

    private void reimportAndReplaceRunFile(File replacedFile, File newFile, String titrationName, @Nullable String retentionWarningMsg)
    {
        DataRegionTable data = new DataRegionTable("Data", this.getWrappedDriver());
        data.clickHeaderButton("Re-import run");
        clickButton("Next"); // batch
        replaceFileInAssayRun(replacedFile, newFile);
        clickButton("Next"); // run
        uncheckCheckbox(Locator.tagWithName("input", "_titrationRole_standard_" + titrationName));
        checkCheckbox(Locator.tagWithName("input", "_titrationRole_qccontrol_" + titrationName));
        waitForText("Save and Finish");

        if (retentionWarningMsg != null)
            assertElementPresent(Locator.tagWithText("span", retentionWarningMsg));
        else
            assertElementNotPresent(Locator.tagWithClass("span", "labkey-error"));

        clickButton("Save and Finish");
    }
}
