/*
 * Copyright (c) 2012-2017 LabKey Corporation
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

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.pages.AssayDesignerPage;
import org.labkey.test.pages.luminex.SetAnalyteDefaultValuesPage;
import org.labkey.test.util.AbstractAssayHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PerlHelper;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

@Category({DailyA.class, Assays.class})
public final class LuminexPositivityTest extends LuminexTest
{
    private static List<String> _analyteNames = Arrays.asList("MyAnalyte", "Blank");
    private int _expectedThresholdValue = 100;
    private int _newThresholdValue = 100;
    private Boolean _expectedNegativeControlValue = false;
    private Boolean _newNegativeControlValue = false;
    private static String _negControlAnalyte = _analyteNames.get(1);
    protected static final File POSITIVITY_RTRANSFORM_SCRIPT_FILE =  new File(TestFileUtils.getLabKeyRoot(), "server/modules/luminex/resources/transformscripts/description_parsing_example.pl");
    private static final String RUN_ID_BASE = "Positivity";

    @BeforeClass
    public static void updateAssayDesign()
    {
        PerlHelper perlHelper = new PerlHelper(getCurrentTest());
        perlHelper.ensurePerlConfig();

        LuminexTest init = (LuminexTest)getCurrentTest();
        init.goToTestAssayHome();
        AssayDesignerPage assayDesigner = init._assayHelper.clickEditAssayDesign();

        assayDesigner.addTransformScript(POSITIVITY_RTRANSFORM_SCRIPT_FILE);
        assayDesigner.addTransformScript(RTRANSFORM_SCRIPT_FILE_LABKEY);
        assayDesigner.addTransformScript(RTRANSFORM_SCRIPT_FILE_LAB);
        assayDesigner.saveAndClose();
    }

    @Test
    public void testPositivity()
    {
        Assume.assumeTrue("Skipping test on SQL Server: TODO 28604: Luminex positivity upload occasionally bogs down server", WebTestHelper.getDatabaseType() != WebTestHelper.DatabaseType.MicrosoftSQLServer);
        setupResultsDefaultView();
        test3xFoldChange();
        test5xFoldChange();
        testWithoutBaselineVisitOrFoldChange();
        testWithNegativeControls();
        testBaselineVisitDataFromPreviousRun();
        testDefaultAnalyteProperties();
    }

    @Test
    public void testImportAnalyteDefaults()
    {
        String assayName = "Luminex Analyte Default Test";
        File analyteDefaults = TestFileUtils.getSampleData("Luminex/LuminexDefaultValues.tsv");
        File badDefaults = TestFileUtils.getSampleData("Luminex/LuminexDefaultValues_bad.tsv");
        File noAnalytes = TestFileUtils.getSampleData("Luminex/LuminexDefaultValues_bad_only_analytes.tsv");
        Set<SetAnalyteDefaultValuesPage.AnalyteDefault> expectedDefaults = new HashSet<>();
        expectedDefaults.add(new SetAnalyteDefaultValuesPage.AnalyteDefault("Analyte1", "Blank"));
        expectedDefaults.add(new SetAnalyteDefaultValuesPage.AnalyteDefault("Blank", 110));
        expectedDefaults.add(new SetAnalyteDefaultValuesPage.AnalyteDefault("MyAnalyte", 120, "Blank"));

        goToTestAssayHome();
        AssayDesignerPage assayDesigner = _assayHelper.copyAssayDesign();
        assayDesigner.setName(assayName);
        assayDesigner.saveAndClose();

        goToTestAssayHome(assayName);
        _assayHelper.setDefaultValues(assayName, AbstractAssayHelper.AssayDefaultAreas.ANALYTE_PROPERTIES);
        SetAnalyteDefaultValuesPage analyteDefaultsPage = new SetAnalyteDefaultValuesPage(this);

        // Issue 21413: NPE when importing analyte default values that are missing expected columns
        analyteDefaultsPage.importDefaultsExpectError(noAnalytes, "The uploaded file only contains a column of analyte names without any analyte properities.");
        analyteDefaultsPage.importDefaultsExpectError(badDefaults, "The Positivity Threshold 'cat' does not appear to be an integer.");
        analyteDefaultsPage.importDefaults(analyteDefaults);

        Set<SetAnalyteDefaultValuesPage.AnalyteDefault> importedDefaults = new HashSet<>(analyteDefaultsPage.getAnalyteDefaults());
        assertEquals(expectedDefaults, importedDefaults);
        Map<String, SetAnalyteDefaultValuesPage.AnalyteDefault> analyteDefaultMap = new HashMap<>();
        for (SetAnalyteDefaultValuesPage.AnalyteDefault analyte : importedDefaults)
        {
            analyteDefaultMap.put(analyte.getAnalyteName(), analyte);
        }

        createNewAssayRun(assayName, RUN_ID_BASE + " Check Imported Defaults");
        checkCheckbox(Locator.name("calculatePositivity"));
        selectPositivityFile(TEST_ASSAY_LUM_FILE13, false);

        List<WebElement> analytePropertyRows = PortalHelper.Locators.webPart("Analyte Properties").findElement(getDriver())
                .findElements(Locator.css("tr.labkey-row, tr.labkey-alternate-row"));

        for (WebElement row : analytePropertyRows)
        {
            String analyteName = row.findElement(By.xpath("td")).getText();

            Integer positivityThreshold = Integer.parseInt(row.findElement(By.name("_analyte_" + analyteName + "_PositivityThreshold")).getAttribute("value"));
            assertEquals(analyteDefaultMap.get(analyteName).getPositivityThreshold(), positivityThreshold);

            String negativeBead = row.findElement(By.name("_analyte_" + analyteName + "_NegativeBead")).getAttribute("value");
            assertEquals(analyteDefaultMap.get(analyteName).getNegativeBead(), negativeBead);
        }
    }

    @LogMethod
    private void testDefaultAnalyteProperties()
    {
        // for issue 20549 :upload a run that unchecks the "Calucate Positivity" and then verify the default value for re-runs and new imports

        createNewAssayRun(TEST_ASSAY_LUM, RUN_ID_BASE + " No Pos Calc");
        uncheckCheckbox(Locator.name("calculatePositivity"));
        selectPositivityFile(TEST_ASSAY_LUM_FILE12, true);
        clickButton("Save and Finish");

        verifyThresholdForReImportRun(3, 100);
        verifyThresholdForReImportRun(2, 99);
        verifyThresholdForReImportRun(1, 98);

        createNewAssayRun(TEST_ASSAY_LUM, RUN_ID_BASE + " Threshold Default Test");
        checkCheckbox(Locator.name("calculatePositivity"));
        selectPositivityFile(TEST_ASSAY_LUM_FILE12, true);
        verifyAnalytePosThresholdValue(_analyteNames.get(0), 98);
        clickButton("Cancel");
    }

    private void verifyThresholdForReImportRun(int runIndex, int expectedThresholdValue)
    {
        DataRegionTable runs = new DataRegionTable("Runs", this);
        runs.checkCheckbox(runIndex);
        runs.clickHeaderButton("Re-import run");
        clickButton("Next"); // batch
        clickButton("Next"); // run
        for (String analyteName : _analyteNames)
            verifyAnalytePosThresholdValue(analyteName, expectedThresholdValue);
        clickButton("Cancel");
    }

    @LogMethod
    private void testBaselineVisitDataFromPreviousRun()
    {
        setPositivityThresholdParams(100, 100);
        setNegativeControlParams(true, false);
        uploadPositivityFile(TEST_ASSAY_LUM, RUN_ID_BASE + " Baseline Visit Previous Run Error", TEST_ASSAY_LUM_FILE12, "1", "3", false, false);
        assertTextPresent(
                "Error: Baseline visit data found in more than one prevoiusly uploaded run: Analyte=" + _analyteNames.get(0) + ", Participant=123400001, Visit=1.",
                "Error: Baseline visit data found in more than one prevoiusly uploaded run: Analyte=" + _analyteNames.get(0) + ", Participant=123400002, Visit=1.",
                "Error: Baseline visit data found in more than one prevoiusly uploaded run: Analyte=" + _analyteNames.get(0) + ", Participant=123400003, Visit=1.");
        clickButton("Cancel");

        // delete all but one run of data so we have the expected number of previous baseline visits rows
        String runToKeep = RUN_ID_BASE + " 3x Fold Change";
        DataRegionTable runs = new DataRegionTable("Runs", this);
        runs.checkCheckbox(0);
        runs.checkCheckbox(1);
        runs.checkCheckbox(2);
        runs.checkCheckbox(3);
        runs.clickHeaderButtonAndWait("Delete");
        assertEquals(4, Locator.linkContainingText("Positivity ").findElements(getDriver()).size());
        assertTextNotPresent(runToKeep);
        clickButton("Confirm Delete");

        // now we exclude the analytes in the remaining run to test that version of the baseline visit query
        waitAndClickAndWait(Locator.linkWithText(runToKeep));
        excludeAnalyteForRun(_analyteNames.get(0), true, "");
        verifyExclusionPipelineJobComplete(2, "INSERT analyte exclusion", runToKeep, "");

        setPositivityThresholdParams(100, 99);
        uploadPositivityFile(TEST_ASSAY_LUM, RUN_ID_BASE + " Baseline Visit Previous Run 1", TEST_ASSAY_LUM_FILE12, "1", "3", false, false);
        checkPositivityValues("positive", 0, new String[0]);
        checkPositivityValues("negative", 0, new String[0]);
        clickAndWait(Locator.linkWithText("view runs"));
        waitAndClickAndWait(Locator.linkWithText(runToKeep));
        excludeAnalyteForRun(_analyteNames.get(0), false, "");
        _extHelper.waitForExtDialog("Warning");
        _extHelper.clickExtButton("Warning", "Yes", 0);
        verifyExclusionPipelineJobComplete(3, "DELETE analyte exclusion", runToKeep, "");

        // now we actual test the case of getting baseline visit data from a previously uploaded run
        setPositivityThresholdParams(99, 98);
        uploadPositivityFile(TEST_ASSAY_LUM, RUN_ID_BASE + " Baseline Visit Previous Run 2", TEST_ASSAY_LUM_FILE12, "1", "3", false, true);
        String[] posWells = new String[]{"A2", "B2", "A6", "B6", "A9", "B9", "A10", "B10"};
        checkPositivityValues("positive", posWells.length, posWells);
        String[] negWells = new String[]{"A3", "B3", "A5", "B5"};
        checkPositivityValues("negative", negWells.length, negWells);
    }

    @LogMethod
    private void testWithNegativeControls()
    {
        setPositivityThresholdParams(100, 100);
        setNegativeControlParams(false, true);
        uploadPositivityFile(TEST_ASSAY_LUM, RUN_ID_BASE + " Negative Control", TEST_ASSAY_LUM_FILE11, "1", "5", false, true);
        checkPositivityValues("positive", 0, new String[0]);
        checkPositivityValues("negative", 0, new String[0]);
    }

    @LogMethod
    private void testWithoutBaselineVisitOrFoldChange()
    {
        setPositivityThresholdParams(101, 101);
        uploadPositivityFile(TEST_ASSAY_LUM, RUN_ID_BASE + " No Fold Change Error", TEST_ASSAY_LUM_FILE11, "1", "", false, true);
        assertTextPresent("Error: No value provided for 'Positivity Fold Change'.");
        clickButton("Cancel");

        // file contains the baseline visit data, which is not used in this case since we don't have a baseline visit run property set
        setPositivityThresholdParams(101, 100);
        uploadPositivityFile(TEST_ASSAY_LUM, RUN_ID_BASE + " No Base Visit 1", TEST_ASSAY_LUM_FILE11, "", "", false, true);
        String[] posWells = new String[] {"A1", "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A6", "B6", "A7", "B7", "A8", "B8", "A9", "B9", "A10", "B10"};
        checkPositivityValues("positive", posWells.length, posWells);
        String[] negWells = new String[] {"A5", "B5"};
        checkPositivityValues("negative", negWells.length, negWells);
        checkDescriptionParsing("123400001 1 2012-10-01", " ", "123400001", "1.0", "2012-10-01");
        checkDescriptionParsing("123400002,2,1/15/2012", " ", "123400002", "2.0", "2012-01-15");

        // file contains data that is only checked against thresholds (i.e. no baseline visit data)
        uploadPositivityFile(TEST_ASSAY_LUM, RUN_ID_BASE + " No Base Visit 2", TEST_ASSAY_LUM_FILE13, "", "", false, false);
        posWells = new String[] {"C1", "D1"};
        checkPositivityValues("positive", posWells.length, posWells);
        negWells = new String[] {"C2", "D2", "C3", "D3", "C4", "D4", "C5", "D5"};
        checkPositivityValues("negative", negWells.length, negWells);
        checkDescriptionParsing("P562, Wk 48, 7-27-2011", " ", "P562", "48.0", "2011-07-27");
    }

    @LogMethod
    public void test5xFoldChange()
    {
        // file contains the baseline visit data
        setPositivityThresholdParams(100, 101);
        uploadPositivityFile(TEST_ASSAY_LUM, RUN_ID_BASE + " 5x Fold Change", TEST_ASSAY_LUM_FILE11, "1", "5", false, true);
        String[] posWells = new String[] {"A9", "B9", "A10", "B10"};
        checkPositivityValues("positive", posWells.length, posWells);
        String[] negWells = new String[] {"A2", "B2", "A3", "B3", "A5", "B5", "A6", "B6"};
        checkPositivityValues("negative", negWells.length, negWells);
        checkDescriptionParsing("123400001 1 2012-10-01", " ", "123400001", "1.0", "2012-10-01");
        checkDescriptionParsing("123400002,2,1/15/2012", " ", "123400002", "2.0", "2012-01-15");
    }

    @LogMethod
    public void test3xFoldChange()
    {
        // file contains the baseline visit data
        setPositivityThresholdParams(100, 100);
        uploadPositivityFile(TEST_ASSAY_LUM, RUN_ID_BASE + " 3x Fold Change", TEST_ASSAY_LUM_FILE11, "1", "3", false, false);
        String[] posWells = new String[] {"A2", "B2", "A6", "B6", "A9", "B9", "A10", "B10"};
        checkPositivityValues("positive", posWells.length, posWells);
        String[] negWells = new String[] {"A3", "B3", "A5", "B5"};
        checkPositivityValues("negative", negWells.length, negWells);
        checkDescriptionParsing("123400001 1 2012-10-01", " ", "123400001", "1.0", "2012-10-01");
        checkDescriptionParsing("123400002,2,1/15/2012", " ", "123400002", "2.0", "2012-01-15");
    }

    private void setupResultsDefaultView()
    {
        goToTestAssayHome(TEST_ASSAY_LUM);
        clickAndWait(Locator.linkContainingText("view results"));
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("Date");
        _customizeViewsHelper.saveCustomView();
    }

    private void setPositivityThresholdParams(int expectedValue, int newValue)
    {
        _expectedThresholdValue = expectedValue;
        _newThresholdValue = newValue;
    }

    private void setNegativeControlParams(boolean expectedValue, boolean newValue)
    {
        _expectedNegativeControlValue = expectedValue;
        _newNegativeControlValue = newValue;
    }

    @Override
    protected void setAnalytePropertyValues()
    {
        // special case for analyte that should always be considered negative control
        if (_negControlAnalyte != null)
        {
            Locator negControlLoc = Locator.name("_analyte_" + _negControlAnalyte + "_NegativeControl");
            waitForElement(negControlLoc);
            checkCheckbox(negControlLoc);
        }

        for (String analyteName : _analyteNames)
        {
            Locator l = verifyAnalytePosThresholdValue(analyteName, _expectedThresholdValue);
            setFormElement(l, Integer.toString(_newThresholdValue));

            Locator negControlLoc = Locator.name("_analyte_" + analyteName + "_NegativeControl");
            waitForElement(negControlLoc);
            if (!_expectedNegativeControlValue && _newNegativeControlValue)
                checkCheckbox(negControlLoc);
            else if (_expectedNegativeControlValue && !_newNegativeControlValue)
                uncheckCheckbox(negControlLoc);

            if (!_newNegativeControlValue && !analyteName.equals(_negControlAnalyte) && _negControlAnalyte != null)
            {
                selectOptionByText(Locator.name("_analyte_" + analyteName + "_NegativeBead"), _negControlAnalyte);
            }
        }

        if (_expectedThresholdValue != _newThresholdValue)
            _expectedThresholdValue = _newThresholdValue;

        if (!_expectedNegativeControlValue.equals(_newNegativeControlValue))
            _expectedNegativeControlValue = _newNegativeControlValue;
    }

    private Locator verifyAnalytePosThresholdValue(String analyteName, int expectedValue)
    {
        String inputName = "_analyte_" + analyteName + "_PositivityThreshold";
        Locator l = Locator.xpath("//input[@type='text' and @name='" + inputName + "'][1]");
        waitForElement(l);
        assertEquals(Integer.toString(expectedValue), getFormElement(l));
        return l;
    }

    private void checkDescriptionParsing(String description, String specimenID, String participantID, String visitID, String date)
    {
        DataRegionTable drt = new DataRegionTable("Data", this);
        drt.ensureColumnsPresent("Description", "Specimen ID", "Participant ID", "Visit ID", "Date");
        int rowID = drt.getIndexWhereDataAppears(description, "Description");
        assertEquals(specimenID, drt.getDataAsText(rowID, "Specimen ID"));
        assertEquals(participantID, drt.getDataAsText(rowID, "Participant ID"));
        assertEquals(visitID, drt.getDataAsText(rowID, "Visit ID"));
        assertEquals(date, drt.getDataAsText(rowID, "Date"));
    }

    private void checkPositivityValues(String type, int numExpected, String[] positivityWells)
    {
        // verify that we are already on the Data results view
        assertElementPresent(Locator.tagWithText("span", "Exclusions"));

        assertTextPresent(type, numExpected);

        DataRegionTable drt = new DataRegionTable("Data", this);
        List<String> posivitiy = drt.getColumnDataAsText("Positivity");
        List<String> wells = drt.getColumnDataAsText("Well");

        for(String well : positivityWells)
        {
            int i = wells.indexOf(well);
            assertEquals(type, posivitiy.get(i));
        }
    }
}
