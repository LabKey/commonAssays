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

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.pages.AssayDesignerPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;

import java.io.File;
import java.util.Calendar;

@Category({DailyA.class, Assays.class})
public final class LuminexAsyncImportTest extends LuminexTest
{
    @BeforeClass
    public static void updateAssayDefinition()
    {
        LuminexTest init = (LuminexTest)getCurrentTest();
        init.goToTestAssayHome();
        AssayDesignerPage assayDesigner = init._assayHelper.clickEditAssayDesign();

        assayDesigner.addTransformScript(RTRANSFORM_SCRIPT_FILE_LABKEY);
        assayDesigner.addTransformScript(RTRANSFORM_SCRIPT_FILE_LAB);
        assayDesigner.setBackgroundImport(true);
        assayDesigner.saveAndClose();
    }

    @Test
    public void testAsyncImport()
    {
        importFirstRun();
        importSecondRun(1, Calendar.getInstance(), TEST_ASSAY_LUM_FILE5);
        reimportFirstRun(0, Calendar.getInstance(), TEST_ASSAY_LUM_FILE5);
        importBackgroundFailure();
        importBackgroundWarning();
    }

    private void importFirstRun() {
        // IMPORT 1ST RUN
        // First file to be import which will subsequently be re-imported
        importRunForTestLuminexConfig(TEST_ASSAY_LUM_FILE5, Calendar.getInstance(), 0);
        assertTextPresent(TEST_ASSAY_LUM + " Upload Jobs");
        waitForPipelineJobsToFinish(2);
        assertElementNotPresent(Locator.linkWithText("ERROR"));
        clickAndWait(Locator.linkWithText("COMPLETE").index(0));
        assertLuminexLogInfoPresent();
        assertElementNotPresent(Locator.linkWithText("ERROR")); //Issue 14082
        assertTextPresent("Starting assay upload", "Finished assay upload");
        clickButton("Data"); // data button links to the run results
        assertTextPresent(TEST_ASSAY_LUM + " Results");
    }

    private void importBackgroundFailure()
    {
        uploadPositivityFile(TEST_ASSAY_LUM, "No Fold Change", TEST_ASSAY_LUM_FILE11, "1", "", true, false);
        assertTextPresent(TEST_ASSAY_LUM + " Upload Jobs");
        waitForPipelineJobsToFinish(5);
        clickAndWait(Locator.linkWithText("ERROR"));
        assertTextPresent("Error: No value provided for 'Positivity Fold Change'.", 3);
        checkExpectedErrors(2);
    }

    private void importBackgroundWarning()
    {
        uploadPositivityFile(TEST_ASSAY_LUM, "No Baseline Data", TEST_ASSAY_LUM_FILE12, "1", "3", true, false);
        assertTextPresent(TEST_ASSAY_LUM + " Upload Jobs");
        waitForPipelineJobsToFinish(6);
        DataRegionTable status = new DataRegionTable("StatusFiles", this);
        status.setFilter("Description", "Equals", "No Baseline Data");
        if (isElementPresent(Locator.linkWithText("ERROR")))
        {
            clickAndWait(Locator.linkWithText("ERROR"));
            Assert.fail("Unexpected error during positivity upload. See screenshot for more detail");
        }
        clickAndWait(Locator.linkWithText("COMPLETE").index(0));
        assertTextPresent("Warning: No baseline visit data found", 6);
    }

    private void importSecondRun(int index, Calendar testDate, File file) {
        // add a second run with different run values
        int i = index;
        goToTestAssayHome();
        clickButton("Import Data");
        setFormElement(Locator.name("network"), "NEWNET" + (i + 1));
        clickButton("Next");
        testDate.add(Calendar.DATE, 1);
        importLuminexRunPageTwo("Guide Set plate " + (i+1), "new"+isotype, "new"+conjugate, "", "", "NewNote" + (i+1),
                "new Experimental", "NewTECH" + (i+1), df.format(testDate.getTime()), file, i, true);
        uncheckCheckbox(Locator.name("_titrationRole_standard_Standard1"));
        checkCheckbox(Locator.name("_titrationRole_qccontrol_Standard1"));
        clickButton("Save and Finish");
        waitForPipelineJobsToFinish(3);
        assertElementNotPresent(Locator.linkWithText("ERROR"));
    }

    private void reimportFirstRun(int index, Calendar testDate, File file)
    {
        // test Luminex re-run import, check for identical values
        int i = index;
        goToTestAssayHome();
        new DataRegionTable("Runs", getDriver()).checkCheckbox(1);
        clickButton("Re-import run");
        Assert.assertEquals("Form did not remember values from first import", "NETWORK1", getFormElement(Locator.name("network")));
        clickButton("Next");
        testDate.add(Calendar.DATE, 1);
        reimportLuminexRunPageTwo("Guide Set plate " + (i+1), isotype, conjugate, "", "", "Notebook" + (i+1),
                "Experimental", "TECH" + (i+1), df.format(testDate.getTime()), file, i);
        uncheckCheckbox(Locator.name("_titrationRole_standard_Standard1"));
        checkCheckbox(Locator.name("_titrationRole_qccontrol_Standard1"));
        clickButton("Save and Finish");
        waitForPipelineJobsToFinish(4);
        assertElementNotPresent(Locator.linkWithText("ERROR"));
    }

    private void reimportLuminexRunPageTwo(String runId, String isotype, String conjugate, String stndCurveFitInput,
                                             String unkCurveFitInput, String notebookNo, String assayType, String expPerformer,
                                             String testDate, File file, int i)
    {
        // verify that all old values from the first imported run are present
        assertFormElementEquals(Locator.name("name"), runId);
        assertFormElementEquals(Locator.name("isotype"), isotype);
        assertFormElementEquals(Locator.name("conjugate"), conjugate);
        assertFormElementEquals(Locator.name("stndCurveFitInput"), stndCurveFitInput);
        assertFormElementEquals(Locator.name("unkCurveFitInput"), unkCurveFitInput);
        uncheckCheckbox(Locator.name("curveFitLogTransform"));
        assertFormElementEquals(Locator.name("notebookNo"), notebookNo);
        assertFormElementEquals(Locator.name("assayType"), assayType);
        assertFormElementEquals(Locator.name("expPerformer"), expPerformer);
        assertFormElementEquals(Locator.name("testDate"), testDate);
        click(Locator.xpath("//a[contains(@class, 'labkey-file-add-icon-enabled')]"));
        setFormElement(Locator.name("__primaryFile__"), file);
        waitForText("A file with name '" + file.getName() + "' already exists");
        clickButton("Next", 60000);
    }

    private void assertLuminexLogInfoPresent()
    {
        waitForText("Finished assay upload");

        //Check for Analyte Properties
        assertTextPresentInThisOrder("----- Start Analyte Properties -----", "----- Stop Analyte Properties -----");
        assertTextPresent("Properties for GS Analyte B", "Properties for GS Analyte A", "*LotNumber:",
                "*NegativeControl:", "*PositivityThreshold:");

        //Check for Well Role Properties
        assertTextPresentInThisOrder("----- Start Well Role Properties -----", "----- Stop Well Role Properties -----");
        assertTextPresent("Standard", "QC Control", "Unknown");

        //Check for Run Properties
        assertTextPresentInThisOrder("----- Start Run Properties -----", "----- End Run Properties -----");
        assertTextPresent("Uploaded Files", "Assay ID", "Isotype", "Conjugate", "Test Date", "Replaces Previous File", "Date file was modified",
                "Specimen Type", "Additive", "Derivative", "Subtract Negative Bead", "Calc of Standards", "Calc of Unknown",
                "Curve Fit Log", "Notebook Number", "Assay Type", "Experiment Performer", "Calculate Positivity",
                "Baseline Visit", "Positivity Fold Change");

        //Check for Batch Properties
        assertTextPresentInThisOrder("----- Start Batch Properties -----", "----- End Batch Properties -----");
        assertTextPresent("Participant Visit", "Target Study", "Species", "Lab ID", "Analysis", "Network", "Transform Script Version", "Ruminex Version");
    }

    @LogMethod
    private void importRunForTestLuminexConfig(File file, Calendar testDate, int i)
    {
        goToTestAssayHome();
        clickButton("Import Data");
        setFormElement(Locator.name("network"), "NETWORK" + (i + 1));
        clickButton("Next");

        testDate.add(Calendar.DATE, 1);
        importLuminexRunPageTwo("Guide Set plate " + (i+1), isotype, conjugate, "", "", "Notebook" + (i+1),
                "Experimental", "TECH" + (i+1), df.format(testDate.getTime()), file, i);
        uncheckCheckbox(Locator.name("_titrationRole_standard_Standard1"));
        checkCheckbox(Locator.name("_titrationRole_qccontrol_Standard1"));
        clickButton("Save and Finish");
    }
}
