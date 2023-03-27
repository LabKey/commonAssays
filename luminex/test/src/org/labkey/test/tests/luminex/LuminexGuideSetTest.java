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
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.WikiHelper;
import org.labkey.test.util.luminex.LuminexGuideSetHelper;

import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({Daily.class, Assays.class})
@BaseWebDriverTest.ClassTimeout(minutes = 28)
public final class LuminexGuideSetTest extends LuminexTest
{
    public LuminexGuideSetHelper _guideSetHelper = new LuminexGuideSetHelper(this);
    public static final File[] GUIDE_SET_FILES = {TEST_ASSAY_LUM_FILE5, TEST_ASSAY_LUM_FILE6, TEST_ASSAY_LUM_FILE7, TEST_ASSAY_LUM_FILE8, TEST_ASSAY_LUM_FILE9};
    public static final String[] INITIAL_EXPECTED_FLAGS = {"AUC, EC50-4, HMFI, PCV", "AUC, EC50-4, HMFI", "HMFI", "", "PCV"};

    private final String GUIDE_SET_5_COMMENT = "analyte 2 guide set run removed";

    public LuminexGuideSetTest()
    {
        // setup the testDate variable
        _guideSetHelper.TESTDATE.add(Calendar.DATE, -GUIDE_SET_FILES.length);
    }

    //requires drc, rlabkey and xtable packages installed in R
    @Test
    public void testGuideSet()
    {
        log("Uploading Luminex run with a R transform script for Guide Set test");

        // add the R transform script to the assay
        goToTestAssayHome();
        ReactAssayDesignerPage assayDesigner =_assayHelper.clickEditAssayDesign();
        assayDesigner.addTransformScript(RTRANSFORM_SCRIPT_FILE_LABKEY);
        assayDesigner.expandFieldsPanel("Batch")
            .addField(new FieldDefinition("CustomProtocol", FieldDefinition.ColumnType.String).setLabel("Protocol"));
        assayDesigner.clickFinish();

        // upload the first set of files (2 runs)
        for (int i = 0; i < 2; i++)
        {
            int runNumber = _guideSetHelper.importGuideSetRun(TEST_ASSAY_LUM, GUIDE_SET_FILES[i]);
            verifyRunFileAssociations(runNumber);
        }

        //verify that the uploaded runs do not have associated guide sets
        _guideSetHelper.verifyGuideSetsNotApplied(TEST_ASSAY_LUM);

        //create initial guide sets for the 2 analytes
        _guideSetHelper.goToLeveyJenningsGraphPage(TEST_ASSAY_LUM, "Standard1");
        createInitialGuideSets();

        // check guide set IDs and make sure appropriate runs are associated to created guide sets
        Map<String, Integer> guideSetIds = _guideSetHelper.getGuideSetIdMap(TEST_ASSAY_LUM);
        _guideSetHelper.verifyGuideSetsApplied(TEST_ASSAY_LUM, guideSetIds, LuminexGuideSetHelper.GUIDE_SET_ANALYTE_NAMES, 2);

        //nav trail check
        assertElementPresent(Locator.tagWithClass("ol", "breadcrumb").append(Locator.linkWithText("assay.Luminex." + TEST_ASSAY_LUM + " Schema")));

        // verify the guide set threshold values for the first set of runs
        int[] rowCounts = {2, 2};
        String[] ec504plAverages = {"179.60", "43426.10"};
        String[] ec504plStdDevs = {"22.48", "794.96"};
        verifyGuideSetThresholds(guideSetIds, LuminexGuideSetHelper.GUIDE_SET_ANALYTE_NAMES, rowCounts, ec504plAverages, ec504plStdDevs, "Four Parameter", "EC50Average", "EC50Std Dev");
        String[] aucAverages = {"8701.37", "80851.74"};
        String[] aucStdDevs = {"466.82", "6523.05"};
        verifyGuideSetThresholds(guideSetIds, LuminexGuideSetHelper.GUIDE_SET_ANALYTE_NAMES, rowCounts, aucAverages, aucStdDevs, "Trapezoidal", "AUCAverage", "AUCStd Dev");

        // upload the final set of runs (3 runs)
        for (int i = 2; i < GUIDE_SET_FILES.length; i++)
        {
            int runNumber = _guideSetHelper.importGuideSetRun(TEST_ASSAY_LUM, GUIDE_SET_FILES[i]);
            verifyRunFileAssociations(runNumber);
        }

        // verify that the newly uploaded runs got the correct guide set applied to them
        _guideSetHelper.verifyGuideSetsApplied(TEST_ASSAY_LUM, guideSetIds, LuminexGuideSetHelper.GUIDE_SET_ANALYTE_NAMES, 5);

        //verify Levey-Jennings report plots are displayed without errors
        verifyLeveyJenningsPlots();

        verifyQCFlags(TEST_ASSAY_LUM, LuminexGuideSetHelper.GUIDE_SET_ANALYTE_NAMES[0], INITIAL_EXPECTED_FLAGS);
        verifyQCReport();

        verifyExcludingRuns(guideSetIds, LuminexGuideSetHelper.GUIDE_SET_ANALYTE_NAMES);

        // test the start and end date filter for the report
        _guideSetHelper.goToLeveyJenningsGraphPage(TEST_ASSAY_LUM, "Standard1");
        applyStartAndEndDateFilter();

        // test the network and customProtocol filters for the report
        _guideSetHelper.goToLeveyJenningsGraphPage(TEST_ASSAY_LUM, "Standard1");
        applyNetworkProtocolFilter();

        excludableWellsWithTransformTest();
        applyLogYAxisScale();
        guideSetApiTest();
        verifyQCFlagUpdatesAfterWellChange();
        verifyLeveyJenningsPermissions();
        verifyHighlightUpdatesAfterQCFlagChange();
    }

    private void excludableWellsWithTransformTest()
    {
        goToProjectHome();
        clickAndWait(Locator.linkContainingText(TEST_ASSAY_LUM));
        excludeReplicateGroupFromRun("Guide Set plate 5", "A6,B6", 2, 1);
        _guideSetHelper.goToLeveyJenningsGraphPage(TEST_ASSAY_LUM, "Standard1");
        _guideSetHelper.setUpLeveyJenningsGraphParams("GS Analyte B");
        assertTextPresent("28040.512");
    }

    private void excludeReplicateGroupFromRun(String run, String wells, int jobCount, int jobInfoCount)
    {
        clickAndWait(Locator.linkContainingText(run));

        log("Exclude well from run");
        clickExclusionMenuIconForWell(wells, false);
        clickButton("Save", 0);
        verifyExclusionPipelineJobComplete(jobCount, "INSERT replicate group exclusion", run, "", 1, jobInfoCount);
    }

    //re-include an excluded well
    private void includeReplicateGroupFromRun(String run, String wells, int jobCount)
    {
        clickAndWait(Locator.linkContainingText(run));

        log("Exclude well from from run");
        clickExclusionMenuIconForWell(wells, false);
        click(Locator.radioButtonById("excludeselected"));
        clickButton("Save", 0);
        _extHelper.clickExtButton("Yes", 0);
        verifyExclusionPipelineJobComplete(jobCount, "DELETE replicate group exclusion", run, "");
    }


    @LogMethod
    private void guideSetApiTest()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        WikiHelper wikiHelper = new WikiHelper(this);

        goToProjectHome();
        assertTextNotPresent("GS Analyte");

        String wikiName = "LuminexGuideSetTestWiki";
        portalHelper.addWebPart("Wiki");
        wikiHelper.createNewWikiPage("HTML");
        setFormElement(Locator.name("name"), wikiName);
        wikiHelper.setWikiBody("Placeholder text.");
        wikiHelper.saveWikiPage();
        File guideSetWiki = TestFileUtils.getSampleData("luminex/views/LuminexGuideSet.html");
        wikiHelper.setSourceFromFile(guideSetWiki, wikiName);

        waitAndClick(Locator.id("button_loadqwps"));
        waitForText("Done loading QWPs");
        assertTextNotPresent("Unexpected Error:");

        click(Locator.id("button_testiud"));
        waitForText("Done testing inserts, updates, and deletes");
        assertTextNotPresent("Unexpected Error:");

        click(Locator.id("button_updateCurveFit"));
        waitForText("Done with CurveFit update");
        assertTextNotPresent("Unexpected Error:");

        click(Locator.id("button_updateGuideSetCurveFit"));
        waitForText("Done with GuideSetCurveFit update");
        assertTextNotPresent("Unexpected Error:");

        // check the QWPs again to make the inserts/updates/deletes didn't affected the expected row counts
        click(Locator.id("button_loadqwps"));
        waitForText("Done loading QWPs again");
        assertTextNotPresent("Unexpected Error:");
    }

    @LogMethod
    private void applyStartAndEndDateFilter()
    {
        _guideSetHelper.setUpLeveyJenningsGraphParams("GS Analyte B");
        // check that all 5 runs are present in the grid and plot
        DataRegionTable table = _guideSetHelper.getTrackingDataRegion();
        assertEquals("Initial grid row count not as expected", 5, table.getDataRowCount());
        assertEquals("Initial plot data point count not as expected", 5, Locator.findElements(getDriver(), Locator.tagWithClass("a", "point")).size());

        // set start and end date filter
        table.setFilter("Analyte/Data/AcquisitionDate", "Is Greater Than or Equal To", "2011-03-26", "Is Less Than or Equal To", "2011-03-28");
        _guideSetHelper.waitForLeveyJenningsTrendPlot();

        // check that only 3 runs are now present
        assertEquals("Initial grid row count not as expected", 3, table.getDataRowCount());
        assertEquals("Initial plot data point count not as expected", 3, Locator.findElements(getDriver(), Locator.tagWithClass("a", "point")).size());
        List<String> rowNetworkValues = table.getColumnDataAsText("Titration/Run/Batch/Network");
        assertEquals("Filtered grid row value not as expected", "NETWORK4", rowNetworkValues.get(0));
        assertEquals("Filtered grid row value not as expected", "NETWORK3", rowNetworkValues.get(1));
        assertEquals("Filtered grid row value not as expected", "NETWORK2", rowNetworkValues.get(2));

        // Clear the filter and check that all rows reappear
        table.clearAllFilters();
        _guideSetHelper.waitForLeveyJenningsTrendPlot();
        assertEquals("Initial grid row count not as expected", 5, table.getDataRowCount());
        assertEquals("Initial plot data point count not as expected", 5, Locator.findElements(getDriver(), Locator.tagWithClass("a", "point")).size());
    }

    @LogMethod
    private void applyNetworkProtocolFilter()
    {
        _guideSetHelper.setUpLeveyJenningsGraphParams("GS Analyte B");
        // check that all 5 runs are present in the grid and plot
        DataRegionTable table = _guideSetHelper.getTrackingDataRegion();
        assertEquals("Initial grid row count not as expected", 5, table.getDataRowCount());
        assertEquals("Initial plot data point count not as expected", 5, Locator.findElements(getDriver(), Locator.tagWithClass("a", "point")).size());

        // set network and protocol filter
        table.setFilter("Titration/Run/Batch/Network", "Equals", "NETWORK3");
        table.setFilter("Titration/Run/Batch/CustomProtocol", "Equals", "PROTOCOL3");
        _guideSetHelper.waitForLeveyJenningsTrendPlot();

        // check that only 1 run is now present
        assertEquals("Initial grid row count not as expected", 1, table.getDataRowCount());
        assertEquals("Initial plot data point count not as expected", 1, Locator.findElements(getDriver(), Locator.tagWithClass("a", "point")).size());
        assertEquals("Filtered grid row value not as expected", "NETWORK3", table.getColumnDataAsText("Titration/Run/Batch/Network").get(0));
        assertEquals("Filtered grid row value not as expected", "PROTOCOL3", table.getColumnDataAsText("Titration/Run/Batch/CustomProtocol").get(0));

        // Clear the filter and check that all rows reappear
        table.clearAllFilters();
        _guideSetHelper.waitForLeveyJenningsTrendPlot();
        assertEquals("Initial grid row count not as expected", 5, table.getDataRowCount());
        assertEquals("Initial plot data point count not as expected", 5, Locator.findElements(getDriver(), Locator.tagWithClass("a", "point")).size());
    }

    private void applyLogYAxisScale()
    {
        _guideSetHelper.setUpLeveyJenningsGraphParams("GS Analyte B");
        _extHelper.selectComboBoxItem(Locator.xpath("//input[@id='scale-combo-box']/.."), "Log");
        _guideSetHelper.waitForLeveyJenningsTrendPlot();
    }

    @LogMethod
    private boolean verifyRunFileAssociations(int index)
    {
        // verify that the PDF of curves file was generated along with the xls file and the Rout file
        DataRegionTable table = new DataRegionTable("Runs", getDriver());
        table.setFilter("Name", "Equals", "Guide Set plate " + index);
        clickAndWait(Locator.tagWithAttribute("img", "src", "/labkey/experiment/images/graphIcon.gif"));
        clickAndWait(Locator.linkWithText("Text View"));
        waitForElement(Locator.css(".labkey-protocol-applications")); // bottom section of the "Text View" tab for the run details page
        waitForElements(Locator.linkWithText("Guide Set plate " + index + ".Standard1_Control_Curves_4PL.pdf"), 3);
        assertElementNotPresent(Locator.linkWithText("Guide Set plate " + index + ".Standard1_Control_Curves_5PL.pdf"));
        assertElementPresent(Locator.linkWithText("Guide Set plate " + index + ".xls"), 4);
        assertElementPresent(Locator.linkWithText("Guide Set plate " + index + ".labkey_luminex_transform.Rout"), 3);

        return true;
    }

    private void createInitialGuideSets()
    {
        _guideSetHelper.setUpLeveyJenningsGraphParams("GS Analyte A");
        _guideSetHelper.createGuideSet(true);
        _guideSetHelper.editRunBasedGuideSet(new String[]{"allRunsRow_1", "allRunsRow_0"}, "Analyte 1", true);

        _guideSetHelper.setUpLeveyJenningsGraphParams("GS Analyte B");
        _guideSetHelper.createGuideSet(true);
        _guideSetHelper.editRunBasedGuideSet(new String[]{"allRunsRow_1"}, "Analyte 2", true);

        //edit a guide set
        log("attempt to edit guide set after creation");
        clickButtonContainingText("Edit", 0);
        _guideSetHelper.editRunBasedGuideSet(new String[]{"allRunsRow_0"}, "edited analyte 2", false);
    }

    @LogMethod
    private void verifyExcludingRuns(Map<String, Integer> guideSetIds, String[] analytes)
    {

        // remove a run from the current guide set
        _guideSetHelper.setUpLeveyJenningsGraphParams("GS Analyte B");
        clickButtonContainingText("Edit", 0);
        _guideSetHelper.editRunBasedGuideSet(new String[]{"guideRunSetRow_0"}, GUIDE_SET_5_COMMENT, false);

        // create a new guide set for the second analyte so that we can test the apply guide set
        _guideSetHelper.setUpLeveyJenningsGraphParams("GS Analyte B");
        _guideSetHelper.createGuideSet(false);
        _guideSetHelper.editRunBasedGuideSet(new String[]{"allRunsRow_1", "allRunsRow_2", "allRunsRow_3"}, "create new analyte 2 guide set with 3 runs", true);

        // apply the new guide set to a run
        verifyGuideSetToRun("NETWORK5", "create new analyte 2 guide set with 3 runs");

        // verify the threshold values for the new guide set
        guideSetIds = _guideSetHelper.getGuideSetIdMap(TEST_ASSAY_LUM);
        int[] rowCounts2 = {2, 3};
        String[] ec504plAverages2 = {"179.60", "42158.38"};
        String[] ec504plStdDevs2 = {"22.48", "4833.95"};
        verifyGuideSetThresholds(guideSetIds, analytes, rowCounts2, ec504plAverages2, ec504plStdDevs2, "Four Parameter", "EC50Average", "EC50Std Dev");
        String[] aucAverages2 = {"8701.37", "85267.93"};
        String[] aucStdDevs2 = {"466.82", "738.53"};
        verifyGuideSetThresholds(guideSetIds, analytes, rowCounts2, aucAverages2, aucStdDevs2, "Trapezoidal", "AUCAverage", "AUCStd Dev");
    }

    // NOTE: is this necessary? (What's this checking that applying the threshold wouldn't fail on already...)
    @LogMethod
    private void verifyGuideSetToRun(String network, String comment)
    {
        DataRegionTable table = _guideSetHelper.getTrackingDataRegion();
        table.checkCheckbox(table.getRowIndex("Titration/Run/Batch/Network", network));
        clickButton("Apply Guide Set", 0);
        waitForElement(ExtHelper.locateGridRowCheckbox(comment));
        sleep(1000);
        // deselect the current guide set to test error message
        click(ExtHelper.locateGridRowCheckbox(comment));
        clickButton("Apply Thresholds", 0);
        waitForText("Please select a guide set to be applied to the selected records.");
        clickButton("OK", 0);
        // reselect the current guide set and apply it
        click(ExtHelper.locateGridRowCheckbox(comment));
        clickButton("Apply Thresholds", 0);
        _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
        // verify that the plot is reloaded
        _guideSetHelper.waitForLeveyJenningsTrendPlot();
    }

    @LogMethod
    private void verifyGuideSetThresholds(Map<String, Integer> guideSetIds, String[] analytes, int[] rowCounts, String[] averages, String[] stdDevs,
                                          String curveType, String averageColName, String stdDevColName)
    {
        // go to the GuideSetCurveFit table to verify the calculated threshold values for the EC50 and AUC
        goToSchemaBrowser();
        selectQuery("assay.Luminex." + TEST_ASSAY_LUM, "GuideSetCurveFit");
        waitForText("view data");
        clickAndWait(Locator.linkContainingText("view data"));
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.showHiddenItems();
        _customizeViewsHelper.addColumn("GuideSetId/RowId");
        _customizeViewsHelper.applyCustomView();
        DataRegionTable table = new DataRegionTable("query", getDriver());
        for (int i = 0; i < analytes.length; i++)
        {
            // verify the row count, average, and standard deviation for the specified curve type's values
            table.setFilter("GuideSetId/RowId", "Equals", guideSetIds.get(analytes[i]).toString());
            table.setFilter("CurveType", "Equals", curveType);
            assertEquals("Unexpected row count for guide set " + guideSetIds.get(analytes[i]).toString(), rowCounts[i], Integer.parseInt(table.getDataAsText(0, "Run Count")));
            assertEquals("Unexpected average for guide set " + guideSetIds.get(analytes[i]).toString(), averages[i],table.getDataAsText(0, averageColName));
            assertEquals("Unexpected stddev for guide set " + guideSetIds.get(analytes[i]).toString(), stdDevs[i], table.getDataAsText(0, stdDevColName));
            table.clearFilter("CurveType");
            table.clearFilter("GuideSetId/RowId");
        }
    }

    @LogMethod
    private void verifyLeveyJenningsPlots()
    {
        _guideSetHelper.goToLeveyJenningsGraphPage(TEST_ASSAY_LUM, "Standard1");
        _guideSetHelper.setUpLeveyJenningsGraphParams("GS Analyte B");

        // check 4PL ec50 trending plot
        click(Locator.tagWithText("span", "EC50 - 4PL"));
        _guideSetHelper.waitForLeveyJenningsTrendPlot();
        assertElementPresent(Locator.id("EC504PLTrendPlotDiv"));

        // check 5PL ec50 trending plot tab not shown
        assertElementNotPresent(Locator.linkWithText("EC50 - 5PL Rumi"));
        assertTextNotPresent("EC50 - 5PL Rumi");
        assertElementNotPresent(Locator.id("EC505PLTrendPlotDiv"));

        // check auc trending plot
        click(Locator.tagWithText("span", "AUC"));
        _guideSetHelper.waitForLeveyJenningsTrendPlot();
        assertElementPresent(Locator.id("AUCTrendPlotDiv"));

        // check high mfi trending plot
        click(Locator.tagWithText("span", "High MFI"));
        _guideSetHelper.waitForLeveyJenningsTrendPlot();
        assertElementPresent(Locator.id("HighMFITrendPlotDiv"));

        // TODO: add more validation of the plot SVG

        //verify QC flags
        DataRegionTable table = _guideSetHelper.getTrackingDataRegion();
        assertTrue(table.getColumnLabels().contains("QC Flags"));
        //this locator finds an EC50 flag, then makes sure there's red text outlining
        Locator.XPathLocator l = Locator.xpath("//td/span[contains(@style,'red')]/../../td/a[contains(text(),'EC50-4')]");
        assertElementPresent(l,2);

        // Verify as much of the Curve Comparison window as we can - most of its content is in the image, so it's opaque to the test
        for (int i = 1; i <= 5; i++)
        {
            table.checkCheckbox(table.getRowIndex("Titration/Run/Batch/Network", "NETWORK" + i));
        }
        clickButton("View 4PL Curves", 0);
        waitForTextToDisappear("loading curves...", WAIT_FOR_JAVASCRIPT);
        assertTextNotPresent("Error executing command");
        assertTextPresent("Export to PDF");
        selectCurveComparisonPlotOption("curvecomparison-scale-combo", "Log");
        selectCurveComparisonPlotOption("curvecomparison-yaxis-combo", "FI-Bkgd-Neg");
        selectCurveComparisonPlotOption("curvecomparison-yaxis-combo", "FI");
        selectCurveComparisonPlotOption("curvecomparison-legend-combo", "Assay Type");
        selectCurveComparisonPlotOption("curvecomparison-legend-combo", "Experiment Performer");
        selectCurveComparisonPlotOption("curvecomparison-legend-combo", "Notebook No.");
        clickButton("Close", 0);
    }

    private void selectCurveComparisonPlotOption(String comboName, String value)
    {
        _extHelper.selectComboBoxItem(Locator.input(comboName).parent(), value);
        waitForTextToDisappear("loading curves...", WAIT_FOR_JAVASCRIPT);
        assertTextNotPresent("Error executing command");
    }

    @LogMethod
    private void verifyQCFlagUpdatesAfterWellChange()
    {
        importPlateFiveAgain();

        //add QC flag colum
        assertTextPresent(TEST_ASSAY_LUM + " Runs");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("QCFlags");
        _customizeViewsHelper.saveCustomView("QC Flags View");

        //2. exclude wells A4, B4 from plate 5a for both analytes
        //	- the EC50 for GS Analyte B is changed to be under the Guide Set range so new QC Flag inserted for that
        excludeReplicateGroupFromRun("Guide Set plate 5", "A4,B4", 3, 2);
        clickAndWait(Locator.linkContainingText("view runs"));
        DataRegionTable drt = new DataRegionTable("Runs", getDriver());
        drt.goToView("QC Flags View");
        assertTrue(drt.getDataAsText(1, "QC Flags").contains("EC50-4"));

        //3. un-exclude wells A4, B4 from plate 5a for both analytes
        //	- the EC50 QC Flag for GS Analyte B that was inserted in the previous step is removed
        includeReplicateGroupFromRun("Guide Set plate 5", "A4,B4", 4);
        clickAndWait(Locator.linkContainingText("view runs"));
        drt = new DataRegionTable("Runs", getDriver());
        drt.goToView("QC Flags View");
        assertTrue(!drt.getDataAsText(1, "QC Flags").contains("EC50-4"));

        //4. For GS Analyte B, apply the non-current guide set to plate 5a
        //	- QC Flags added for EC50 and HMFI
        _guideSetHelper.goToLeveyJenningsGraphPage(TEST_ASSAY_LUM, "Standard1");
        _guideSetHelper.setUpLeveyJenningsGraphParams("GS Analyte B");
        String newQcFlags = "AUC, EC50-4, HMFI";
        assertTextNotPresent(newQcFlags);
        _guideSetHelper.applyGuideSetToRun("NETWORK5", GUIDE_SET_5_COMMENT, false);
        //assert ec50 and HMFI red text present
        waitForText(newQcFlags);
        assertElementPresent(Locator.xpath("//span[text()='28040.512' and contains(@style,'red')]")); // EC50
        assertElementPresent(Locator.xpath("//span[text()='79121.445' and contains(@style,'red')]")); // AUC
        assertElementPresent(Locator.xpath("//span[text()='32145.8' and contains(@style,'red')]")); // High MFI
        //verify new flags present in run list
        goToTestAssayHome();
        drt = new DataRegionTable("Runs", getDriver());
        drt.goToView("QC Flags View");
        assertTextPresent("AUC, EC50-4, HMFI, PCV");

        //5. For GS Analyte B, apply the guide set for plate 5a back to the current guide set
        //	- the EC50 and HMFI QC Flags that were added in step 4 are removed
        _guideSetHelper.goToLeveyJenningsGraphPage(TEST_ASSAY_LUM, "Standard1");
        _guideSetHelper.setUpLeveyJenningsGraphParams("GS Analyte B");
        _guideSetHelper.applyGuideSetToRun("NETWORK5", GUIDE_SET_5_COMMENT, true);
        assertTextNotPresent(newQcFlags);
        assertElementPresent(Locator.xpath("//td/span[contains(@style,'red')]"),2);

        //6. Create new Guide Set for GS Analyte B that includes plate 5 (but not plate 5a)
        //	- the AUC QC Flag for plate 5 is removed for GS Analyte B but still exists for GS Analyte A
        Locator.XPathLocator aucLink =  Locator.xpath("//a[contains(text(),'AUC')]");
        waitForElement(aucLink);
        int aucCount = getElementCount(aucLink);
        _guideSetHelper.createGuideSet(false);
        _guideSetHelper.editRunBasedGuideSet(new String[]{"allRunsRow_1"}, "Guide set includes plate 5", true);
        assertEquals("Wrong count for AUC flag links", aucCount, (getElementCount(aucLink)));
        assertElementPresent(Locator.xpath("//td/span[contains(@style,'red')]"),1);

        //7. Switch to GS Analyte A, and edit the current guide set to include plate 3
        //	- the QC Flag for plate 3 (the run included) and the other plates (4, 5, and 5a) are all removed as all values are within the guide set ranges
        _guideSetHelper.setUpLeveyJenningsGraphParams("GS Analyte A");
        assertExpectedAnalyte1QCFlagsInitial();
        assertElementPresent(Locator.xpath("//td/span[contains(@style,'red')]"),7);
        clickButtonContainingText("Edit", 0);
        _guideSetHelper.editRunBasedGuideSet(new String[]{"allRunsRow_3"}, "edited analyte 1", false);
        assertExpectedAnalyte1QCFlagsUpdated();
        assertElementPresent(Locator.xpath("//td/span[contains(@style,'red')]"),0);

        //8. Edit the GS Analyte A guide set and remove plate 3
        //	- the QC Flags for plates 3, 4, 5, and 5a return (HMFI for all 4 and AUC for plates 4, 5, and 5a)
        removePlate3FromGuideSet();
        assertExpectedAnalyte1QCFlagsInitial();
        assertElementPresent(Locator.xpath("//td/span[contains(@style,'red')]"),7);
    }

    @LogMethod
    private void removePlate3FromGuideSet()
    {
        clickButtonContainingText("Edit", 0);
        _extHelper.waitForExt3Mask(WAIT_FOR_JAVASCRIPT);
        waitAndClick(Locator.id("guideRunSetRow_0"));
        clickButton("Save",0);
        _guideSetHelper.waitForGuideSetExtMaskToDisappear();
    }

    private void assertExpectedAnalyte1QCFlagsInitial()
    {
        waitForElements(Locator.xpath("//a[contains(text(),'HMFI')]"), 4);
        waitForElements(Locator.xpath("//a[contains(text(),'AUC')]"), 9);
    }

    private void assertExpectedAnalyte1QCFlagsUpdated()
    {
        for (String flag : new String[] {"HMFI", "EC50-4", "EC50-5"})
            assertElementNotPresent(Locator.xpath("//a[contains(text(),'" + flag + "')]"));
        waitForElements(Locator.xpath("//a[contains(text(),'AUC')]"), 7);
    }

    private void importPlateFiveAgain()
    {
        //1. upload plate 5 again with the same isotype and conjugate (plate 5a)
        //	- QC flags inserted for AUC for both analytes and HMFI for GS Analyte A

        goToTestAssayHome();
        clickButton("Import Data");
        setFormElement(Locator.name("network"), "NETWORK" + (10));
        clickButton("Next");

        importLuminexRunPageTwo("Reload guide set 5", isotype, conjugate, "", "", "Notebook" + 11,
                "Experimental", "TECH" + (11), "",  TEST_ASSAY_LUM_FILE9, 6, true);
        uncheckCheckbox(Locator.name("_titrationRole_standard_Standard1"));
        checkCheckbox(Locator.name("_titrationRole_qccontrol_Standard1"));
        clickButton("Save and Finish");
    }

    @LogMethod
    private void verifyLeveyJenningsPermissions()
    {
        String ljUrl = getCurrentRelativeURL();
        String editor = "editor1_luminex@luminex.test";
        String reader = "reader1_luminex@luminex.test";

        createAndImpersonateUser(editor, "Editor");

        beginAt(ljUrl);
        _guideSetHelper.setUpLeveyJenningsGraphParams("GS Analyte B");
        assertTextPresent("Apply Guide Set");
        stopImpersonating();
        _userHelper.deleteUsers(true, editor);

        createAndImpersonateUser(reader, "Reader");

        beginAt(ljUrl);
        _guideSetHelper.setUpLeveyJenningsGraphParams("GS Analyte B");
        assertTextPresent("Levey-Jennings Reports", "Standard1");
        assertTextNotPresent("Apply Guide Set");
        stopImpersonating();
        _userHelper.deleteUsers(true, reader);
    }

    @LogMethod
    private void verifyHighlightUpdatesAfterQCFlagChange()
    {
        goToTestAssayHome();
        clickAndWait(Locator.linkWithText("Guide Set plate 4"));
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.showHiddenItems();
        String[] newColumns = {"AnalyteTitration/MaxFIQCFlagsEnabled", "AnalyteTitration/MaxFI",
                "AnalyteTitration/Four ParameterCurveFit/EC50", "AnalyteTitration/Four ParameterCurveFit/AUC",
                "AnalyteTitration/Four ParameterCurveFit/EC50QCFlagsEnabled",
                "AnalyteTitration/Four ParameterCurveFit/AUCQCFlagsEnabled"};
        for(String column : newColumns)
        {
            _customizeViewsHelper.addColumn(column);
        }
        _customizeViewsHelper.saveCustomView();

        String expectedHMFI=  "9173.8";
        String expectedEC50 = "36676.645";

        assertElementPresent(Locator.xpath("//span[contains(@style, 'red') and text()=" + expectedHMFI + "]"));

        clickAndWait(Locator.linkContainingText("view runs"));
        enableDisableQCFlags("Guide Set plate 4", "AUC", "HMFI");
        clickAndWait(Locator.linkContainingText("view results"));
        //turn off flags
        assertElementPresent(Locator.xpath("//td[contains(@style, 'white-space') and text()=" + expectedHMFI + "]"));
        assertElementPresent(Locator.xpath("//td[contains(@style, 'white-space') and text()=" + expectedEC50 + "]"));
    }

    private void enableDisableQCFlags(String runName, String... flags)
    {
        DataRegionTable drt = new DataRegionTable("Runs", getDriver());
        int rowIndex = drt.getRowIndexStrict("Name", runName);
        drt.link(rowIndex, "QCFlags").click();
        _extHelper.waitForExt3Mask(WAIT_FOR_JAVASCRIPT);

        sleep(1500);
        waitForText("Run QC Flags");

        for(String flag : flags)
        {
            Locator aucCheckBox = Locator.xpath("//div[text()='" + flag + "']/../../td/div/div[contains(@class, 'check')]");
            click(aucCheckBox);
        }

        clickButton("Save");
    }
}
