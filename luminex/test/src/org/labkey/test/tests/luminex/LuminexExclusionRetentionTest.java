package org.labkey.test.tests.luminex;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.pages.AssayDesignerPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PipelineStatusTable;

import java.io.File;

/**
 * Created by iansigmon on 10/31/16.
 */
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

    private void cleanupPipelineJobs()
    {
        //Cleanup pipeline jobs
        goToModule("Pipeline");
        PipelineStatusTable table = new PipelineStatusTable(this);

        if(table.getDataRowCount() > 0)
        {
            table.checkAll();
            clickButton("Delete");
            clickButton("Confirm Delete");
        }
    }

    private void cleanupRuns()
    {
        goToTestAssayHome();
        DataRegionTable runTable = new DataRegionTable("Runs", this.getWrappedDriver());

        if(runTable.getDataRowCount() > 0)
        {
            runTable.checkAll();
            clickButton("Delete");
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
        excludeWell("A1", "X1", "111", wellMatchComment, matchingAnalyte); //should be retained after import
        clickButton("No", 0);

        //Add well exclusion that partially matches
        String partialMatchComment = "partial well match";
        excludeWell("A2", "X2", "112", partialMatchComment, matchingAnalyte, changedAnalyte);   //Should be kept, but only have one analyte after import
        clickButton("No", 0);

        //Add well exclusion that does not match Analyte
        String wellNonMatchAnalyteComment = "well Analyte non-match";
        excludeWell("A3", "X3", "113", wellNonMatchAnalyteComment, changedAnalyte); //Analyte changed in re-import file (new value ENV66)
        clickButton("No", 0);

        //Add well exclusion that does not match type
        String wellNonMatchTypeComment = "well Type non-match";
        excludeWell("A4", "X4", "114", wellNonMatchTypeComment, matchingAnalyte); //Well changed in re-import file (new value X2)
        clickButton("No", 0);

        //Add well exclusion that does not match type
        String wellNonMatchDescriptionComment = "well description non-match";
        excludeWell("G2", "S2", "Standard1", wellNonMatchDescriptionComment, matchingAnalyte); //Description changed in re-import file (new value Standard1a)
        clickButton("No", 0);

        //Re-import
        reimportAndReplaceRunFile(BASE_RUN_FILE, REIMPORT_FILE, "Standard1a");

        //verify exclusion page
        clickAndWait(Locator.linkContainingText(RUN_NAME));

        // Check out the exclusion report
        clickAndWait(Locator.linkWithText("view excluded data"));
        assertTextPresent(wellMatchComment);
        assertTextPresent(partialMatchComment);
        assertTextPresent(matchingAnalyte, 2);  //Once each for: match, partial-match

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
        reimportAndReplaceRunFile(BASE_RUN_FILE, BASE_RUN_FILE, "Standard1");

        //verify exclusion page
        clickAndWait(Locator.linkContainingText(RUN_NAME));
        clickAndWait(Locator.linkWithText("view excluded data"));
        assertTextPresent(titrationMatchComment);
        assertTextPresent(matchingAnalyte);
        assertTextPresent("No data to show.", 3);

        goToTestAssayHome();
        clickAndWait(Locator.linkContainingText(RUN_NAME));

        //Re-import
        reimportAndReplaceRunFile(BASE_RUN_FILE, REIMPORT_FILE, "Standard1a");

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
        reimportAndReplaceRunFile(BASE_RUN_FILE, REIMPORT_FILE, "Standard1a");

        //verify exclusion page
        clickAndWait(Locator.linkContainingText(RUN_NAME));
        // Check out the exclusion report
        clickAndWait(Locator.linkWithText("view excluded data"));
        assertTextNotPresent(analyteNonMatchComment);
        assertTextPresent("No data to show.", 4);

        goToTestAssayHome();
        clickAndWait(Locator.linkContainingText(RUN_NAME));

        //Add analyte exclusion that matches
        String analyteMatchComment = "analyte match";
        excludeAnalyteForRun(matchingAnalyte, true, analyteMatchComment);
        verifyExclusionPipelineJobComplete(2, "INSERT analyte exclusion", RUN_NAME, analyteMatchComment, 1, 2);

        reimportAndReplaceRunFile(REIMPORT_FILE, BASE_RUN_FILE, "Standard1");
        //verify retained exclusion
        clickAndWait(Locator.linkContainingText(RUN_NAME));
        // Check out the exclusion report
        clickAndWait(Locator.linkWithText("view excluded data"));
        assertTextPresent(analyteMatchComment);
        assertTextNotPresent(analyteNonMatchComment);
        assertTextPresent("No data to show.", 3);
    }

    private void reimportAndReplaceRunFile(File replacedFile, File newFile, String titrationName)
    {
        DataRegionTable data = new DataRegionTable("Data", this.getWrappedDriver());
        data.clickHeaderButtonByText("Re-import run");
        clickButton("Next"); // batch
        replaceFileInAssayRun(replacedFile, newFile);
        clickButton("Next"); // run
        uncheckCheckbox(Locator.tagWithName("input", "_titrationRole_standard_" + titrationName));
        checkCheckbox(Locator.tagWithName("input", "_titrationRole_qccontrol_" + titrationName));
        waitForText("Save and Finish");
        //TODO: verify warning message
        clickButton("Save and Finish");
    }
}
