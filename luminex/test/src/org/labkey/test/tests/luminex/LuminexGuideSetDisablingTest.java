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

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
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
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.luminex.LuminexGuideSetHelper;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@Category({Daily.class, Assays.class})
@BaseWebDriverTest.ClassTimeout(minutes = 13)
public final class LuminexGuideSetDisablingTest extends LuminexTest
{
    private final LuminexGuideSetHelper _guideSetHelper = new LuminexGuideSetHelper(this);
    private static final File[] GUIDE_SET_FILES = {
            TestFileUtils.getSampleData("luminex/01-11A12-IgA-Biotin.xls"),
            TestFileUtils.getSampleData("luminex/02-14A22-IgA-Biotin.xls"),
            TestFileUtils.getSampleData("luminex/03-31A82-IgA-Biotin.xls"),
            TestFileUtils.getSampleData("luminex/04-17A32-IgA-Biotin.xls")
    };
    private static final String GUIDE_SET_WINDOW_NAME = "Guide Set Parameter Details";
    private static final String RUN_BASED_ANALYTE = "ENV1";
    private static final String VALUE_BASED_ANALYTE = "ENV2";
    private static final String[] NETWORKS = new String[]{"NETWORK2", "NETWORK3", "NETWORK4", "NETWORK1"};
    private static final String RUN_BASED_COMMENT = "LuminexGuideSetDisablingTest "+RUN_BASED_ANALYTE;
    private static final String VALUE_BASED_COMMENT = "LuminexGuideSetDisablingTest "+VALUE_BASED_ANALYTE;
    private static final String CONTROL_NAME = "Standard1";
    private static final Locator.XPathLocator TABLE_HEADER_LOCATOR = Locator.xpath("//table").withClass("gsDetails");
    private static final Locator.XPathLocator TABLE_METRICS_LOCATOR = Locator.xpath("//table").withClass("gsMetricDetails");
    private static final Locator SAVE_BTN = Ext4Helper.Locators.ext4Button("Save");

    public LuminexGuideSetDisablingTest()
    {
        // setup the testDate variable
        _guideSetHelper.TESTDATE.add(Calendar.DATE, -GUIDE_SET_FILES.length);
    }

    @BeforeClass // note: this runs after LuminexTest.initTest
    public static void initer()
    {
        ((LuminexGuideSetDisablingTest)getCurrentTest()).init();
    }

    public void init()
    {
        // add the R transform script to the assay
        goToTestAssayHome();
        ReactAssayDesignerPage assayDesigner = _assayHelper.clickEditAssayDesign();
        assayDesigner.addTransformScript(RTRANSFORM_SCRIPT_FILE_LABKEY);
        assayDesigner.expandFieldsPanel("Batch")
            .addField(new FieldDefinition("CustomProtocol", FieldDefinition.ColumnType.String).setLabel("CustomProtocol"));
        assayDesigner.clickFinish();

        for (int i = 0; i < GUIDE_SET_FILES.length; i++)
            _guideSetHelper.importGuideSetRun(TEST_ASSAY_LUM, GUIDE_SET_FILES[i]);
    }

    @Before
    public void ensureGuideSetsCreated()
    {
        _guideSetHelper.goToManageGuideSetsPage(TEST_ASSAY_LUM);
        boolean needsRunBased = !isTextPresent(RUN_BASED_COMMENT);
        boolean needsValueBased = !isTextPresent(VALUE_BASED_COMMENT);

        if (needsRunBased)
            createRunBasedGuideSet();
        if (needsValueBased)
            createValueBasedGuideSet();
    }

    @LogMethod
    private void createRunBasedGuideSet()
    {
        _guideSetHelper.goToLeveyJenningsGraphPage(TEST_ASSAY_LUM, CONTROL_NAME);
        _guideSetHelper.setUpLeveyJenningsGraphParams(RUN_BASED_ANALYTE);
        _guideSetHelper.createGuideSet(true);

        _guideSetHelper.editRunBasedGuideSet(new String[]{"allRunsRow_1", "allRunsRow_0"}, RUN_BASED_COMMENT, true);

        // apply the new guide set to a run
        _guideSetHelper.applyGuideSetToRun(NETWORKS, RUN_BASED_COMMENT, true);
    }

    @LogMethod
    // borrowing from LuminexValueBasedGuideSetTest
    private void createValueBasedGuideSet()
    {
        Map<String, Double> metricInputs = new TreeMap<>();
        _guideSetHelper.goToLeveyJenningsGraphPage(TEST_ASSAY_LUM, CONTROL_NAME);
        _guideSetHelper.setUpLeveyJenningsGraphParams(VALUE_BASED_ANALYTE);
        _guideSetHelper.createGuideSet(true);

        metricInputs.put("EC504PLAverage", 42158.22);
        metricInputs.put("EC504PLStdDev", 4833.76);
        metricInputs.put("AUCAverage", 85268.04);
        metricInputs.put("AUCStdDev", 738.55);
        metricInputs.put("MaxFIAverage", 32507.27);
        metricInputs.put("MaxFIStdDev", 189.83);

        _guideSetHelper.editValueBasedGuideSet(metricInputs, VALUE_BASED_COMMENT, true);
        _guideSetHelper.applyGuideSetToRun(NETWORKS, VALUE_BASED_COMMENT, true);
    }

    @Test
    public void verifyGuideSetsPresent()
    {
        Set<String> expectedComments = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(RUN_BASED_COMMENT, VALUE_BASED_COMMENT)));

        _guideSetHelper.goToManageGuideSetsPage(TEST_ASSAY_LUM);
        DataRegionTable drt = new DataRegionTable("GuideSet", this);
        Set<String> actualComments = new HashSet<>(drt.getColumnDataAsText("Comment"));

        Assert.assertEquals("It appear that there are missing guide sets.", expectedComments, actualComments.stream()
                .filter(expectedComments::contains).collect(Collectors.toCollection(HashSet::new)));
        // note consider failing on extras too?

        // check guide sets are set in LJ reports
        _guideSetHelper.goToLeveyJenningsGraphPage(TEST_ASSAY_LUM, CONTROL_NAME);
        _guideSetHelper.setUpLeveyJenningsGraphParams(RUN_BASED_ANALYTE);
        assertTextPresent(RUN_BASED_COMMENT);

        _guideSetHelper.setUpLeveyJenningsGraphParams(VALUE_BASED_ANALYTE);
        assertTextPresent(VALUE_BASED_COMMENT);
    }

    @Test
    public void verifyQCFlagsDisabledOnImport()
    {
        String analyte = "ENV5";
        String comment = "verifyQCFlagsDisabledOnImport";

        // first create NEW guide set
        _guideSetHelper.goToLeveyJenningsGraphPage(TEST_ASSAY_LUM, CONTROL_NAME);
        _guideSetHelper.setUpLeveyJenningsGraphParams(analyte);
        _guideSetHelper.createGuideSet(true);
        _guideSetHelper.editRunBasedGuideSet(new String[]{"allRunsRow_2"}, comment, true);

        // NOTE: consider validating now that the GuideSet was set properly...
//        like: validateRedText(true, "8.08", "61889.88", "2.66", "64608.73");

        // Getting a js error: unexpected character at line 1 column 1 of the JSON data appears to be timing related.
        // Going to stop check for client side errors for this section.
        pauseJsErrorChecker();
        clickButtonContainingText("Details", 0);
        waitForElement(Locator.checkboxByName("EC504PLCheckBox"));
        click(Locator.checkboxByName("EC504PLCheckBox"));
        click(Locator.checkboxByName("MFICheckBox"));
        click(Locator.checkboxByName("AUCCheckBox"));
        click(SAVE_BTN);
        waitForElementToDisappear(Locator.checkboxByName("EC504PLCheckBox"), 500);
        resumeJsErrorChecker();

        _guideSetHelper.importGuideSetRun(TEST_ASSAY_LUM, TestFileUtils.getSampleData("luminex/plate 3_IgA-Biot (Standard1).xls"));

        // go back to LJ plot
        _guideSetHelper.goToLeveyJenningsGraphPage(TEST_ASSAY_LUM, CONTROL_NAME);
        _guideSetHelper.setUpLeveyJenningsGraphParams(analyte);
        _guideSetHelper.applyGuideSetToRun(NETWORKS, comment, true);

        validateFlaggedForQC();

        // no clean-up needed as test shouldn't collide with others...
    }

    @Test
    public void verifyQCFlagToggling()
    {
        // note: this uses the run based guide set

        // similar to verifyHighlightUpdatesAfterQCFlagChange (but not quite the same... not sure how this other place works)
        _guideSetHelper.goToLeveyJenningsGraphPage(TEST_ASSAY_LUM, CONTROL_NAME);
        _guideSetHelper.setUpLeveyJenningsGraphParams(RUN_BASED_ANALYTE);
        final String plate1_AUC = "64608.734";
        final String plate2_AUC = "61889.88";

        validateFlaggedForQC(plate1_AUC, plate2_AUC);

        clickButtonContainingText("Details", 0);
        // toggle off the AUC QC flag and then validate errors
        waitForElement(Locator.checkboxByName("AUCCheckBox"));
        click(Locator.checkboxByName("AUCCheckBox"));
        saveGuideSetParameters();

        // toggle off the rest of the QC flags and then validate errors
        clickButtonContainingText("Details", 0);
        waitForElement(Locator.checkboxByName("EC504PLCheckBox"));
        click(Locator.checkboxByName("EC504PLCheckBox"));
        click(Locator.checkboxByName("MFICheckBox"));
        saveGuideSetParameters();

        validateFlaggedForQC();

        // clean-up/revert
        clickButtonContainingText("Details", 0);
        waitForElement(Locator.checkboxByName("EC504PLCheckBox"));
        click(Locator.checkboxByName("EC504PLCheckBox"));
        click(Locator.checkboxByName("MFICheckBox"));
        click(Locator.checkboxByName("AUCCheckBox"));
        saveGuideSetParameters();

        validateFlaggedForQC(plate2_AUC, plate1_AUC);
    }

    private void saveGuideSetParameters()
    {
        click(Ext4Helper.Locators.windowButton(GUIDE_SET_WINDOW_NAME, "Save"));
        _guideSetHelper.waitForGuideSetExtMaskToDisappear();
    }

    private void validateFlaggedForQC(String... texts)
    {
        Locator redCellLoc = Locator.tagWithClass("table", "labkey-data-region").append(Locator.xpath("//span[contains(@style,'red')]"));
        List<WebElement> qcFlaggedCells = isElementPresent(redCellLoc) ? redCellLoc.findElements(getDriver()) : Collections.emptyList();

        List<String> expectedQcFlaggedValues = new ArrayList<>(Arrays.asList(texts));
        List<String> qcFlaggedValues = getTexts(qcFlaggedCells);

        Collections.sort(expectedQcFlaggedValues);
        Collections.sort(qcFlaggedValues);

        Assert.assertEquals("Wrong values flagged for QC", expectedQcFlaggedValues, qcFlaggedValues);
    }

    @Test
    public void verifyGuideSetParameterDetailsWindow()
    {
        Locator cancelBtn = Ext4Helper.Locators.ext4Button("Cancel");
        // validates both a run-based and a value-based GuideSet
        _guideSetHelper.goToManageGuideSetsPage(TEST_ASSAY_LUM);

        clickGuideSetDetailsByComment(RUN_BASED_COMMENT);
        validateGuideSetRunDetails("Run-based", "Titration");
        validateGuideSetMetricsDetails(new String[][]{
            {"5.284", "0.323", "2" }, // EC50 4PL
            {"32086.500", "331.633", "2" }, // MFI
            {"63299.346", "149.318", "2" }, // AUC
        });
        // validate checkboxes are not displayed
        assertTextPresent("# Runs");
        assertElementPresent(TABLE_METRICS_LOCATOR.append("//input[@type='checkbox']"));
        // this gets a mis-fire and selenium raises error that element is not the expected click element.
        click(cancelBtn);

        clickGuideSetDetailsByComment(VALUE_BASED_COMMENT);
        validateGuideSetRunDetails("Value-based", "Titration");
        validateGuideSetMetricsDetails(new String[][]{
            {"42158.220", "4833.760"}, // EC50 4PL
            {"32507.270", "189.830"}, // MFI
            {"85268.040", "738.550"} // AUC
        });
        // validate checkboxes are not displayed
        assertTextNotPresent("# Runs");
        assertElementNotPresent(TABLE_METRICS_LOCATOR.append("//input[@type='checkbox']"));
        click(cancelBtn);
    }

    private void clickGuideSetDetailsByComment(String comment)
    {
        DataRegionTable drt = new DataRegionTable("GuideSet", getDriver());
        int row = drt.getRowIndex("Comment", comment);
        Assert.assertTrue("There is no GuideSet with the comment: " + comment, row >= 0);
        drt.link(row, 1).click();
        waitForElement(TABLE_HEADER_LOCATOR);
    }

    private void validateGuideSetRunDetails(String type, String controlType)
    {
        String analyte, comment;
        if (type.equals("Run-based"))
        {
            analyte = RUN_BASED_ANALYTE;
            comment = RUN_BASED_COMMENT;
        }
        else
        {
            analyte = VALUE_BASED_ANALYTE;
            comment = VALUE_BASED_COMMENT;
        }

        // check guide set details at top of window
        Locator.XPathLocator table = TABLE_HEADER_LOCATOR;
        assertNotEquals("", getTableCellText(table, 0, 1)); // find better check
        assertNotEquals("", getTableCellText(table, 1, 3)); // find better check...
        assertEquals(CONTROL_NAME, getTableCellText(table, 1, 1));
        assertEquals(controlType, getTableCellText(table, 2, 1));
        assertEquals(analyte, getTableCellText(table, 3, 1));
        assertEquals(comment, getTableCellText(table, 4, 1));
        assertEquals(type, getTableCellText(table,0, 3));
        assertEquals(isotype, getTableCellText(table,2, 3));
        assertEquals(conjugate, getTableCellText(table,3, 3));
    }

    private void validateGuideSetMetricsDetails(String[][] metricsData) {
        for(int i=1; i < 4; i++) // iterate rows
        {
            assertEquals(metricsData[i - 1][0], getTableCellText(TABLE_METRICS_LOCATOR, i, 1));
            assertEquals(metricsData[i - 1][1], getTableCellText(TABLE_METRICS_LOCATOR, i, 2));
        }

        // check if counts included and validate if so (e.g. run-based)
        if (metricsData[0].length == 3)
            for(int i=1; i < 4; i++) // iterate rows
                assertEquals(metricsData[i-1][2], getTableCellText(TABLE_METRICS_LOCATOR, i, 3));
    }

    @Test
    public void verifyDeleteGuideSet()
    {
        clickDeleteOnGuideSetWithComment(RUN_BASED_COMMENT);

        assertTextPresent("LuminexGuideSetDisablingTest ENV1",
                "Guide Set plate 1", "Guide Set plate 2", "Guide Set plate 3", "Guide Set plate 4");

        clickButton("Confirm Delete");

        clickDeleteOnGuideSetWithComment(VALUE_BASED_COMMENT);

        assertTextPresent("LuminexGuideSetDisablingTest ENV2",
                "Guide Set plate 1", "Guide Set plate 2", "Guide Set plate 3", "Guide Set plate 4");

        clickButton("Confirm Delete");
    }

    private void clickDeleteOnGuideSetWithComment(String comment)
    {
        _guideSetHelper.goToManageGuideSetsPage(TEST_ASSAY_LUM);
        DataRegionTable drt = new DataRegionTable("GuideSet", this);
        // check checkbox by given guideset
        for (int i=0; i < drt.getDataRowCount(); i++)
            if (drt.getDataAsText(i, "Comment").equals(comment))
                drt.checkCheckbox(i);
        drt.clickHeaderButton("Delete");
    }
}
