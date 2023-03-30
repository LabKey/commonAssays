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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LabKeyExpectedConditions;
import org.labkey.test.util.LogMethod;
import org.openqa.selenium.WebElement;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Category({Daily.class, Assays.class})
@BaseWebDriverTest.ClassTimeout(minutes = 12)
public final class LuminexRTransformTest extends LuminexTest
{
    private static final String TEST_ANALYTE_LOT_NUMBER = "ABC 123";
    private static final String ANALYTE1 = "MyAnalyte";
    private static final String ANALYTE2 = "MyAnalyte B";
    private static final String ANALYTE3 = "Blank";
    private static final String ANALYTE4 = "MyNegative";

    @BeforeClass
    public static void updateAssayDefinition()
    {
        LuminexRTransformTest init = (LuminexRTransformTest)getCurrentTest();

        // add the R transform script to the assay
        init.goToTestAssayHome();
        ReactAssayDesignerPage assayDesigner = init._assayHelper.clickEditAssayDesign();

        assayDesigner.addTransformScript(RTRANSFORM_SCRIPT_FILE_LABKEY);
        assayDesigner.addTransformScript(RTRANSFORM_SCRIPT_FILE_LAB);
        assayDesigner.clickFinish();
    }

    //requires drc and xtable packages installed in R
    @Test
    public void testRTransform()
    {
        log("Uploading Luminex run with a R transform script");
        uploadRun();
        verifyPDFsGenerated();
        verifyScriptVersions();
        verifyLotNumber();
        verifyAnalyteProperties(new String[]{ANALYTE4, ANALYTE4, " ", " "});
    }

    private void verifyAnalyteProperties(String[] expectedNegBead)
    {
        goToSchemaBrowser();
        viewQueryData("assay.Luminex." + TEST_ASSAY_LUM, "Analyte");
        DataRegionTable table = new DataRegionTable("query", this);
        for (int i = 0; i < table.getDataRowCount(); i++)
        {
            assertEquals(expectedNegBead[i], table.getDataAsText(i, "Negative Bead"));
        }
    }

    private void verifyLotNumber()
    {
        clickAndWait(Locator.linkWithText("r script transformed assayId"));
        DataRegionTable table;
        table = new DataRegionTable("Data", this);
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("Analyte/Properties/LotNumber");
        _customizeViewsHelper.applyCustomView();
        table.setFilter("Analyte/Properties/LotNumber", "Equals", TEST_ANALYTE_LOT_NUMBER);
        waitForElement(Locator.paginationText(1, 40, 40));
        table.clearFilter("Analyte/Properties/LotNumber");
    }

    private void verifyScriptVersions()
    {
        assertTextPresent(TEST_ASSAY_LUM + " Runs");
        DataRegionTable table = new DataRegionTable("Runs", this);
        assertEquals("Unexpected Transform Script Version number", "11.0.20230206", table.getDataAsText(0, "Transform Script Version"));
        assertEquals("Unexpected Lab Transform Script Version number", "3.1.20180903", table.getDataAsText(0, "Lab Transform Script Version"));
        assertNotNull(table.getDataAsText(0, "R Version"));
    }

    private void verifyPDFsGenerated()
    {
        DataRegionTable.DataRegion(getDriver()).find(); // Make sure page is loaded
        WebElement curvePng = Locator.tagWithAttribute("img", "src", WebTestHelper.getContextPath() + "/_images/sigmoidal_curve.png").findElement(getDriver());
        shortWait().until(LabKeyExpectedConditions.animationIsDone(curvePng));
        File curvePdf = clickAndWaitForDownload(curvePng);
        assertEquals("Curve PDF has wrong name: " + curvePdf.getName(), "WithAltNegativeBead.Standard1_Control_Curves_4PL.pdf", curvePdf.getName());
    }

    @LogMethod
    public void uploadRun()
    {
        goToProjectHome();
        clickAndWait(Locator.linkWithText(TEST_ASSAY_LUM));

        clickButton("Import Data");
        clickButton("Next");

        setFormElement(Locator.name("name"), "r script transformed assayId");
        checkCheckbox(Locator.name("subtNegativeFromAll"));
        setFormElement(Locator.name("stndCurveFitInput"), "FI");
        setFormElement(Locator.name("unkCurveFitInput"), "FI-Bkgd-Neg");
        setFormElement(Locator.name("__primaryFile__"), TEST_ASSAY_LUM_FILE4);
        clickButton("Next", defaultWaitForPage * 2);

        // make sure the Standard checkboxes are checked
        checkCheckbox(Locator.name("_titrationRole_standard_Standard1"));
        checkCheckbox(Locator.name("titration_" + ANALYTE1 + "_Standard1"));
        checkCheckbox(Locator.name("titration_" + ANALYTE2 + "_Standard1"));
        checkCheckbox(Locator.name("titration_" + ANALYTE3 + "_Standard1"));
        // make sure that that QC Control checkbox is checked
        checkCheckbox(Locator.name("_titrationRole_qccontrol_Standard1"));
        // set LotNumber for the first analyte
        setFormElement(Locator.xpath("//input[@type='text' and contains(@name, '_LotNumber')][1]"), TEST_ANALYTE_LOT_NUMBER);
        // set negative control and negative bead values
        checkCheckbox(Locator.name("_analyte_" + ANALYTE3 + "_NegativeControl"));
        selectOptionByText(Locator.name("_analyte_" + ANALYTE1 + "_NegativeBead"), ANALYTE3);
        selectOptionByText(Locator.name("_analyte_" + ANALYTE2 + "_NegativeBead"), ANALYTE3);
        // switch to using MyNegative bead for subtraction
        checkCheckbox(Locator.name("_analyte_" + ANALYTE4 + "_NegativeControl"));
        selectOptionByText(Locator.name("_analyte_" + ANALYTE1 + "_NegativeBead"), ANALYTE4);
        selectOptionByText(Locator.name("_analyte_" + ANALYTE2 + "_NegativeBead"), ANALYTE4);
        clickButton("Save and Finish");
    }

    @Test
    public void testNegativeBead()
    {
        goToProjectHome();
        log("Upload Luminex run for testing Negative Bead UI and calculation");
        clickAndWait(Locator.linkWithText(TEST_ASSAY_LUM));

        clickButton("Import Data");
        clickButton("Next");
        String assayRunId = "negative bead assayId";
        setFormElement(Locator.name("name"), assayRunId);
        uncheckCheckbox(Locator.name("subtNegativeFromAll"));
        setFormElement(Locator.name("__primaryFile__"), TEST_ASSAY_LUM_FILE4);
        waitForElement(Locator.id("file-upload-tbl").containing(TEST_ASSAY_LUM_FILE4.getName()));
        clickButton("Next", defaultWaitForPage * 2);

        // uncheck all of the titration well role types
        uncheckCheckbox(Locator.name("_titrationRole_standard_Standard1"));
        uncheckCheckbox(Locator.name("_titrationRole_qccontrol_Standard1"));

        // verify some UI with 2 negative controls checked
        assertElementPresent(Locator.tagWithClass("select", "negative-bead-input"), 4);
        uncheckCheckbox(Locator.name("_analyte_" + ANALYTE1 + "_NegativeControl"));
        uncheckCheckbox(Locator.name("_analyte_" + ANALYTE2 + "_NegativeControl"));
        checkCheckbox(Locator.name("_analyte_" + ANALYTE3 + "_NegativeControl"));
        checkCheckbox(Locator.name("_analyte_" + ANALYTE4 + "_NegativeControl"));
        assertElementPresent(Locator.tag("option"), 6);
        assertElementPresent(Locator.tagWithAttribute("option", "value", ANALYTE3), 2);
        assertElementPresent(Locator.tagWithAttribute("option", "value", ANALYTE4), 2);

        // change negative control and negative bead selections and verify UI
        uncheckCheckbox(Locator.name("_analyte_" + ANALYTE3 + "_NegativeControl"));
        uncheckCheckbox(Locator.name("_analyte_" + ANALYTE4 + "_NegativeControl"));
        assertElementPresent(Locator.tag("option"), 4); // all analytes have only empty option
        assertElementNotPresent(Locator.tagWithAttribute("option", "value", ANALYTE3));
        assertElementNotPresent(Locator.tagWithAttribute("option", "value", ANALYTE4));

        // subtract MyNegative bead from Blank and verify
        checkCheckbox(Locator.name("_analyte_" + ANALYTE4 + "_NegativeControl"));
        selectOptionByText(Locator.name("_analyte_" + ANALYTE3 + "_NegativeBead"), ANALYTE4);
        clickButton("Save and Finish");
        waitAndClickAndWait(Locator.linkWithText(assayRunId));
        DataRegionTable table = new DataRegionTable("Data", this);
        table.setFilter("Analyte/NegativeBead", "Equals", ANALYTE4);
        table.setFilter("Type", "Does Not Equal", "C9"); // see usage above for issue 20457
        waitForElement(Locator.paginationText(1, 38, 38));
        for (int i = 0; i < table.getDataRowCount(); i=i+2)
        {
            // since data for Blank bead and MyNegative bead are exact copies, adding the wells of a group together should result in zero
            double well1value = Double.parseDouble(table.getDataAsText(i, "FI-Bkgd-Neg"));
            double well2value = Double.parseDouble(table.getDataAsText(i+1, "FI-Bkgd-Neg"));
            assertEquals(0.0, well1value + well2value, 0);
        }
        table.clearAllFilters("Type");
    }
}
