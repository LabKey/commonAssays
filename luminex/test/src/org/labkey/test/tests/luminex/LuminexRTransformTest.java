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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.pages.AssayDesignerPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Category({DailyA.class, Assays.class})
public final class LuminexRTransformTest extends LuminexTest
{
    private static final String TEST_ANALYTE_LOT_NUMBER = "ABC 123";
    private static final String ANALYTE1 = "MyAnalyte";
    private static final String ANALYTE2 = "MyAnalyte B";
    private static final String ANALYTE3 = "Blank";
    private static final String ANALYTE4 = "MyNegative";

    private static final String[] RTRANS_FIBKGDNEG_VALUES = {"-50.5", "-70.0", "25031.5", "25584.5", "391.5", "336.5", "263.8", "290.8",
            "35.2", "35.2", "63.0", "71.0", "-34.0", "-33.0", "-29.8", "-19.8", "-639.8", "-640.2", "26430.8", "26556.2", "-216.2", "-204.2", "-158.5",
            "-208.0", "-4.0", "-4.0", "194.2", "198.8", "-261.2", "-265.2", "-211.5", "-213.0"};
    private static final String[] RTRANS_ESTLOGCONC_VALUES_5PL = {"-6.9", "-6.9", "4.3", "4.3", "0.4", "0.4", "-0.0", "-0.0", "-6.9", "-6.9",
            "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "4.2", "4.2", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9",
            "-6.9", "-0.6", "-0.6", "-6.9", "-6.9", "-6.9", "-6.9"};

    private static final String[] RTRANS_ESTLOGCONC_VALUES_4PL = {"-6.9", "-6.9", "5.0", "5.0", "0.4", "0.4", "0.1", "0.1", "-6.9", "-6.9",
            "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "5.5", "5.5", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9", "-6.9",
            "-0.8", "-0.8", "-6.9", "-6.9", "-6.9", "-6.9"};

    @BeforeClass
    public static void updateAssayDefinition()
    {
        LuminexRTransformTest init = (LuminexRTransformTest)getCurrentTest();

        // add the R transform script to the assay
        init.goToTestAssayHome();
        AssayDesignerPage assayDesigner = init._assayHelper.clickEditAssayDesign();

        assayDesigner.addTransformScript(RTRANSFORM_SCRIPT_FILE_LABKEY);
        assayDesigner.addTransformScript(RTRANSFORM_SCRIPT_FILE_LAB);
        assayDesigner.saveAndClose();
    }

    //requires drc, Ruminex and xtable packages installed in R
    @Test
    public void testRTransform()
    {
        log("Uploading Luminex run with a R transform script");

        uploadRunWithoutRumiCalc();
        verifyPDFsGenerated(false);
        verifyScriptVersions();
        verifyLotNumber();
        verifyRumiCalculatedValues(false, ANALYTE3);
        verifyAnalyteProperties(new String[]{ANALYTE3, ANALYTE3, " ", " "});

        reImportRunWithRumiCalc();
        verifyPDFsGenerated(true);
        verifyScriptVersions();
        verifyLotNumber();
        verifyRumiCalculatedValues(true, ANALYTE4);
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

    private void verifyRumiCalculatedValues(boolean hasRumiCalcData, String negativeBead)
    {
        DataRegionTable table = new DataRegionTable("Data", this);
        table.setFilter("FIBackgroundNegative", "Is Not Blank", null);
        waitForElement(Locator.paginationText(1, 80, 80));
        table.setFilter("Type", "Equals", "C9"); // issue 20457
        assertEquals(4, table.getDataRowCount());
        for(int i = 0; i < table.getDataRowCount(); i++)
        {
            assertEquals(table.getDataAsText(i, "FI-Bkgd"), table.getDataAsText(i, "FI-Bkgd-Neg"));
        }
        table.clearFilter("Type");
        table.setFilter("Type", "Starts With", "X"); // filter to just the unknowns
        waitForElement(Locator.paginationText(1, 32, 32));
        assertTextPresent(negativeBead, 32);
        // check values in the fi-bkgd-neg column
        for(int i = 0; i < RTRANS_FIBKGDNEG_VALUES.length; i++)
        {
            assertEquals(RTRANS_FIBKGDNEG_VALUES[i], table.getDataAsText(i, "FI-Bkgd-Neg"));
        }
        table.clearFilter("FIBackgroundNegative");

        table.setFilter("EstLogConc_5pl", "Is Not Blank", null);
        if (!hasRumiCalcData)
        {
            waitForText("No data to show.");
            assertEquals(0, table.getDataRowCount());
        }
        else
        {
            waitForElement(Locator.paginationText(1, 32, 32));
            // check values in the est log conc 5pl column
            for(int i = 0; i < RTRANS_ESTLOGCONC_VALUES_5PL.length; i++)
            {
                assertEquals(RTRANS_ESTLOGCONC_VALUES_5PL[i], table.getDataAsText(i, "Est Log Conc Rumi 5 PL"));
            }
        }
        table.clearFilter("EstLogConc_5pl");

        table.setFilter("EstLogConc_4pl", "Is Not Blank", null);
        if (!hasRumiCalcData)
        {
            waitForText("No data to show.");
            assertEquals(0, table.getDataRowCount());
        }
        else
        {
            waitForElement(Locator.paginationText(1, 32, 32));
            // check values in the est log conc 4pl column
            for(int i = 0; i < RTRANS_ESTLOGCONC_VALUES_4PL.length; i++)
            {
                assertEquals(RTRANS_ESTLOGCONC_VALUES_4PL[i], table.getDataAsText(i, "Est Log Conc Rumi 4 PL"));
            }
        }
        table.clearFilter("EstLogConc_4pl");

        table.clearFilter("Type");
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
        assertEquals("Unexpected Transform Script Version number", "10.0.20150910", table.getDataAsText(0, "Transform Script Version"));
        assertEquals("Unexpected Lab Transform Script Version number", "3.0.20160729", table.getDataAsText(0, "Lab Transform Script Version"));
        assertEquals("Unexpected Ruminex Version number", "0.0.9", table.getDataAsText(0, "Ruminex Version"));
        assertNotNull(table.getDataAsText(0, "R Version"));
    }

    private void verifyPDFsGenerated(boolean hasStandardPDFs)
    {
        click(Locator.tagWithAttribute("img", "src", "/labkey/_images/sigmoidal_curve.png"));
        assertElementPresent(Locator.linkContainingText(".Standard1_Control_Curves_4PL.pdf"));
        assertElementPresent(Locator.linkContainingText(".Standard1_Control_Curves_5PL.pdf"));
        if (hasStandardPDFs)
        {
            assertElementPresent(Locator.linkWithText("WithAltNegativeBead.Standard1_5PL.pdf"));
            assertElementPresent(Locator.linkWithText("WithAltNegativeBead.Standard1_4PL.pdf"));
        }
    }

    @LogMethod
    public void uploadRunWithoutRumiCalc()
    {
        goToProjectHome();
        clickAndWait(Locator.linkWithText(TEST_ASSAY_LUM));

        clickButton("Import Data");
        clickButton("Next");

        setFormElement(Locator.name("name"), "r script transformed assayId");
        checkCheckbox(Locator.name("subtNegativeFromAll"));
        setFormElement(Locator.name("stndCurveFitInput"), "FI");
        setFormElement(Locator.name("unkCurveFitInput"), "FI-Bkgd-Neg");
        checkCheckbox(Locator.name("curveFitLogTransform"));
        checkCheckbox(Locator.name("skipRumiCalculation"));
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
        clickButton("Save and Finish");
    }

    @LogMethod
    public void reImportRunWithRumiCalc()
    {
        goToProjectHome();
        clickAndWait(Locator.linkWithText(TEST_ASSAY_LUM));

        // all batch, run, analyte properties should be remembered from the first upload
        DataRegionTable table = new DataRegionTable("Runs", this);
        table.checkCheckbox(0);
        clickButton("Re-import run");
        clickButton("Next");
        uncheckCheckbox(Locator.name("skipRumiCalculation"));
        clickButton("Next", defaultWaitForPage * 2);
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
