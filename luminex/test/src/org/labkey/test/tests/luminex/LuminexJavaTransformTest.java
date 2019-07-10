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
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.pages.AssayDesignerPage;
import org.labkey.test.util.DataRegionTable;

import static org.junit.Assert.assertEquals;

@Category({DailyA.class, Assays.class})
@BaseWebDriverTest.ClassTimeout(minutes = 7)
public final class LuminexJavaTransformTest extends LuminexTest
{
    @Override
    protected boolean useXarImport()
    {
        return false;
    }

    @BeforeClass
    public static void updateAssayDefinition()
    {
        LuminexJavaTransformTest init = (LuminexJavaTransformTest)getCurrentTest();
        init.goToTestAssayHome();
        AssayDesignerPage assayDesigner = init._assayHelper.clickEditAssayDesign();

        assayDesigner.addTransformScript(TestFileUtils.getSampleData("qc/transform.jar"));
        assayDesigner.saveAndClose();
    }

    @Test
    public void testJavaTransform()
    {
        log("Uploading Luminex Runs with a transform script");

        goToTestAssayHome();
        clickButton("Import Data");
        setFormElement(Locator.name("species"), TEST_ASSAY_LUM_SET_PROP_SPECIES);
        clickButton("Next");
        setFormElement(Locator.name("name"), "transformed assayId");
        setFormElement(Locator.name("__primaryFile__"), TEST_ASSAY_LUM_FILE1);
        clickButton("Next", 60000);
        clickButton("Save and Finish");

        // verify the description error was generated by the transform script
        clickAndWait(Locator.linkWithText("transformed assayId"), longWaitForPage);
        DataRegionTable table = new DataRegionTable("Data", this);
        for(int i = 1; i <= 40; i++)
        {
            assertEquals("Transformed", table.getDataAsText(i, "Description"));
        }
    }

    @Test
    public void testFileUpload()
    {
        String[] assayRunIds = {"Test Assay 1", "Test Assay 2", "Test Assay 3"};
        String ERROR_TEXT = "already exists.";

        log("Testing file upload conflict error messages and archive on delete");

        goToTestAssayHome();

        // Create a run that imports 1 file
        createNewAssayRun(TEST_ASSAY_LUM, assayRunIds[0]);

        addFilesToAssayRun(TEST_ASSAY_MULTIPLE_STANDARDS_1);
        clickButton("Next");
        clickButton("Save and Finish", longWaitForPage);

        // Create a second run that imports the file again and check for error message
        createNewAssayRun(TEST_ASSAY_LUM, assayRunIds[1]);
        addFilesToAssayRun(TEST_ASSAY_MULTIPLE_STANDARDS_1, TEST_ASSAY_MULTIPLE_STANDARDS_2, TEST_ASSAY_MULTIPLE_STANDARDS_3);
        // verify that conflict error message is present
        waitForText(WAIT_FOR_JAVASCRIPT, ERROR_TEXT);
        clickButton("Next");
        clickButton("Cancel", longWaitForPage);

        // Delete the first run, files should be archived
        DataRegionTable runs = new DataRegionTable("Runs", getDriver());
        runs.checkCheckbox(0);
        runs.clickHeaderButton("Delete");
        waitForText(WAIT_FOR_JAVASCRIPT, "Confirm Deletion");
        clickButton("Confirm Delete");
        waitForText(WAIT_FOR_JAVASCRIPT, "Description for Luminex assay");

        // Create a run with a duplicate file within the set of files
        createNewAssayRun(TEST_ASSAY_LUM, assayRunIds[2]);
        addFilesToAssayRun(TEST_ASSAY_MULTIPLE_STANDARDS_1, TEST_ASSAY_MULTIPLE_STANDARDS_2, TEST_ASSAY_MULTIPLE_STANDARDS_3, TEST_ASSAY_MULTIPLE_STANDARDS_3);
        // verify that the error message for duplicate entries pops up, and that the first remove button is enabled (checks prior bug)
        waitForText(WAIT_FOR_JAVASCRIPT, "duplicate");
        clickButton("OK", 0);
    }
}
