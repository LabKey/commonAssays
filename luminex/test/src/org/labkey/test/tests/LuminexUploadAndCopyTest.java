/*
 * Copyright (c) 2011-2014 LabKey Corporation
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
package org.labkey.test.tests;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.Luminex;
import org.labkey.test.categories.LuminexAll;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;

import java.io.File;

import static org.junit.Assert.assertEquals;

@Category({DailyA.class, LuminexAll.class, Assays.class, Luminex.class})
public final class LuminexUploadAndCopyTest extends LuminexTest
{
    private static final String THAW_LIST_NAME = "LuminexThawList";
    private static final String TEST_ASSAY_LUM_SET_PROP_SPECIES2 = "testSpecies2";
    private static final String TEST_ASSAY_LUM_RUN_NAME = "testRunName1";
    private static final String TEST_ASSAY_LUM_RUN_NAME2 = "testRunName2";
    private static final String TEST_ASSAY_LUM_RUN_NAME3 = "WithIndices.xls";
    private static final String TEST_ASSAY_LUM_RUN_NAME4 = "testRunName4";

    @Override
    protected boolean useXarImport()
    {
        return false;
    }

    @Test
    public void testUploadAndCopy()
    {
        _listHelper.importListArchive(getProjectName(), new File(TestFileUtils.getSampledataPath(), "/Luminex/UploadAndCopy.lists.zip"));

        clickProject(TEST_ASSAY_PRJ_LUMINEX);

        clickAndWait(Locator.linkWithText("Assay List"));
        clickAndWait(Locator.linkWithText(TEST_ASSAY_LUM));
        // Make sure we have the expected help text
        assertTextPresent("No runs to show. To add new runs, use the Import Data button.");
        log("Uploading Luminex Runs");
        clickButton("Import Data");
        setFormElement(Locator.name("species"), TEST_ASSAY_LUM_SET_PROP_SPECIES);
        clickButton("Next");
        setFormElement(Locator.name("name"), TEST_ASSAY_LUM_RUN_NAME);
        setFormElement(Locator.name("__primaryFile__"), TEST_ASSAY_LUM_FILE1);
        clickButton("Next", 60000);
        clickButton("Save and Import Another Run");
        clickAndWait(Locator.linkWithText(TEST_ASSAY_LUM));

        clickButton("Import Data");
        assertEquals(TEST_ASSAY_LUM_SET_PROP_SPECIES, getFormElement(Locator.name("species")));
        setFormElement(Locator.name("species"), TEST_ASSAY_LUM_SET_PROP_SPECIES2);
        clickButton("Next");
        setFormElement(Locator.name("name"), TEST_ASSAY_LUM_RUN_NAME2);
        setFormElement(Locator.name("__primaryFile__"), TEST_ASSAY_LUM_FILE2);
        clickButton("Next", 60000);
        setFormElement(Locator.xpath("//input[@type='text' and contains(@name, '_analyte_')][1]"), "StandardName1b");
        setFormElement(Locator.xpath("//input[@type='text' and contains(@name, '_analyte_')][1]/../../../tr[4]//input[@type='text']"), "StandardName2");
        setFormElement(Locator.xpath("//input[@type='text' and contains(@name, '_analyte_')][1]/../../../tr[5]//input[@type='text']"), "StandardName4");
        clickButton("Save and Finish");

        // Upload another run using a thaw list pasted in as a TSV
        clickButton("Import Data");
        assertEquals(TEST_ASSAY_LUM_SET_PROP_SPECIES2, getFormElement(Locator.name("species")));
        checkCheckbox(Locator.radioButtonByNameAndValue("participantVisitResolver", "Lookup"));
        checkCheckbox(Locator.radioButtonByNameAndValue("ThawListType", "Text"));
        setFormElement(Locator.id("ThawListTextArea"), "Index\tSpecimenID\tParticipantID\tVisitID\n" +
                "1\tSpecimenID1\tParticipantID1\t1.1\n" +
                "2\tSpecimenID2\tParticipantID2\t1.2\n" +
                "3\tSpecimenID3\tParticipantID3\t1.3\n" +
                "4\tSpecimenID4\tParticipantID4\t1.4");
        clickButton("Next");
        assertElementPresent(Locator.tagContainingText("font", "Pasted TSV is missing required column"));

        setFormElement(Locator.id("ThawListTextArea"), "Index\tParticipantID\tVisitID\tSpecimenID\tDate\n" +
                "1\tListParticipant1\t1001.1\t\t\n" +
                "2\tListParticipant2\t1001.2\t\t\n" +
                "3\tListParticipant3\t1001.3\t\t\n" +
                "4\tListParticipant4\t1001.4\t\t\n" +
                "5\tListParticipant5\t1001.5\t\t\n" +
                "6\tListParticipant6\t1001.6\t\t\n" +
                "7\tListParticipant7\t1001.7\t\t\n" +
                "8\tListParticipant8\t1001.8\t\t\n" +
                "9\tListParticipant9\t1001.9\t\t\n" +
                "10\tListParticipant10\t1001.10\t\t\n" +
                "11\tListParticipant11\t1001.11\t\t\n" +
                "C1\tListParticipantC1\t1001.001\t\t\n" +
                "C2\tListParticipantC2\t1001.002\t\t\n" +
                "275001\tListParticipant275001\t1001.275001\t\t\n" +
                "275005\tListParticipant275005\t1001.275005\t\t\n" +
                "275010\tListParticipant275010\t1001.275010\t\t\n" +
                "275-12M\tListParticipant275-12M\t1001.27512\t\t\n" +
                "335001\tListParticipant335001\t1001.335001\t\t\n" +
                "335006\tListParticipant335006\t1001.335006\t\t\n" +
                "335-12M\tListParticipant335-12M\t1001.33512\t\t\n" +
                "9016-05\tListParticipant9016-05\t1001.901605\t\t\n" +
                "9016-06\tListParticipant9016-06\t1001.901606\t\t\n" +
                "9016-07\tListParticipant9016-07\t1001.901607\t\t\n" +
                "9016-08\tListParticipant9016-08\t1001.901608\t\t\n" +
                "9016-09\tListParticipant9016-09\t1001.901609\t\t\n" +
                "9016-10\tListParticipant9016-10\t1001.901610\t\t\n" +
                "9021-01\tListParticipant9021-01\t1001.902101\t\t\n" +
                "9021-02\tListParticipant9021-02\t1001.902102\t\t\n" +
                "9021-03\tListParticipant9021-03\t1001.902103\t\t\n" +
                "9021-04\tListParticipant9021-04\t1001.902104\t\t\n" +
                "9021-05\tListParticipant9021-05\t1001.902105\t\t\n" +
                "9021-06\tListParticipant9021-06\t1001.902106\t\t\n" +
                "9021-07\tListParticipant9021-07\t1001.902107\t\t\n" +
                "9021-08\tListParticipant9021-08\t1001.902108\t\t\n" +
                "9021-09\tListParticipant9021-09\t1001.902109\t\t\n" +
                "9021-10\tListParticipant9021-10\t1001.902110\t\t\n" +
                "9021-11\tListParticipant9021-11\t1001.902111\t\t\n" +
                "9021-12\tListParticipant9021-12\t1001.902112\t\t\n" +
                "9021-13\tListParticipant9021-13\t1001.902113\t\t\n" +
                "9021-14\tListParticipant9021-14\t1001.902114\t\t\n" +
                "9021-15\tListParticipant9021-15\t1001.902115\t\t\n" +
                "9021-16\tListParticipant9021-16\t1001.902116\t\t\n" +
                "9021-17\tListParticipant9021-17\t1001.902117\t\t\n" +
                "9011-01\tListParticipant9011-01\t1001.901101\t\t\n" +
                "9011-02\tListParticipant9011-02\t1001.901102\t\t\n" +
                "9011-03\tListParticipant9011-03\t1001.901103\t\t\n" +
                "9011-04\tListParticipant9011-04\t1001.901104\t\t\n" +
                "9016-01\tListParticipant9016-01\t1001.901601\n" +
                "9016-02\tListParticipant9016-02\t1001.901602\n" +
                "9016-03\tListParticipant9016-03\t1001.901603\n" +
                "9016-04\tListParticipant9016-04\t1001.901604\n" +
                "Standard1\tListParticipantStandard1\t1001.0001\n" +
                "FakeSample57\tListParticipantFakeSample57\t1001.0057\n" +
                "FakeSample1\tListParticipantFakeSample1\t1001.0001\n");
        clickButton("Next");
        setFormElement(Locator.name("__primaryFile__"), TEST_ASSAY_LUM_FILE3);
        clickButton("Next", 60000);
        assertEquals("StandardName1b", getFormElement(Locator.xpath("//input[@type='text' and contains(@name, '_analyte_')][1]")));
        assertEquals("StandardName4", getFormElement(Locator.xpath("//input[@type='text' and contains(@name, '_analyte_')][1]/../../../tr[4]//input[@type='text'][1]")));
        clickButton("Save and Finish");

        // Upload another run using a thaw list that pointed at the list we uploaded earlier
        clickButton("Import Data");
        assertEquals(TEST_ASSAY_LUM_SET_PROP_SPECIES2, getFormElement(Locator.name("species")));
        assertRadioButtonSelected(Locator.radioButtonByNameAndValue("participantVisitResolver", "Lookup"));
        assertRadioButtonSelected(Locator.radioButtonByNameAndValue("ThawListType", "Text"));

        checkCheckbox(Locator.radioButtonByNameAndValue("ThawListType", "List"));
        waitForElement(Locator.css(".schema-loaded-marker"));
        _ext4Helper.selectComboBoxItem(Locator.id("thawListSchemaName"), "lists");
        waitForElement(Locator.css(".query-loaded-marker"));
        _ext4Helper.selectComboBoxItem(Locator.id("thawListQueryName"), THAW_LIST_NAME);

        clickButton("Next");
        setFormElement(Locator.name("name"), TEST_ASSAY_LUM_RUN_NAME4);
        setFormElement(Locator.name("__primaryFile__"), TEST_ASSAY_LUM_FILE3);
        waitForText("A file with name '" + TEST_ASSAY_LUM_FILE3.getName() + "' already exists");
        clickButton("Next", 60000);
        assertEquals("StandardName1b", getFormElement(Locator.xpath("//input[@type='text' and contains(@name, '_analyte_')][1]")));
        assertEquals("StandardName4", getFormElement(Locator.xpath("//input[@type='text' and contains(@name, '_analyte_')][1]/../../../tr[4]//input[@type='text'][1]")));
        clickButton("Save and Finish");

        log("Check that upload worked");
        clickAndWait(Locator.linkWithText(TEST_ASSAY_LUM_RUN_NAME), 2 * WAIT_FOR_PAGE);
        assertTextPresent("Hu IL-1b (32)");

        clickAndWait(Locator.linkWithText(TEST_ASSAY_LUM + " Runs"));
        clickAndWait(Locator.linkWithText(TEST_ASSAY_LUM_RUN_NAME3));
        assertTextPresent("IL-1b (1)");
        assertTextPresent("ListParticipant1");
        assertTextPresent("ListParticipant2");
        assertTextPresent("ListParticipant3");
        setFilter("Data", "ParticipantID", "Equals", "ListParticipant1");
        assertTextPresent("1.1");
        setFilter("Data", "ParticipantID", "Equals", "ListParticipant2");
        assertTextPresent("1001.2");

        clickAndWait(Locator.linkWithText(TEST_ASSAY_LUM + " Runs"));
        clickAndWait(Locator.linkWithText(TEST_ASSAY_LUM_RUN_NAME4));
        assertTextPresent("IL-1b (1)");
        assertTextPresent("ListParticipant1");
        assertTextPresent("ListParticipant2");
        assertTextPresent("ListParticipant3");
        assertTextPresent("ListParticipant4");
        setFilter("Data", "ParticipantID", "Equals", "ListParticipant1");
        assertTextPresent("1001.1");
        setFilter("Data", "ParticipantID", "Equals", "ListParticipant2");
        assertTextPresent("1001.2");

        clickAndWait(Locator.linkWithText(TEST_ASSAY_LUM + " Runs"));
        clickAndWait(Locator.linkWithText(TEST_ASSAY_LUM_RUN_NAME2));
        assertTextPresent("IL-1b (1)");
        assertTextPresent("9011-04");

        setFilter("Data", "FI", "Equals", "20");
        click(Locator.name(".toggle"));
        clickButton("Copy to Study");
        selectOptionByText(Locator.name("targetStudy"), "/" + TEST_ASSAY_PRJ_LUMINEX + " (" + TEST_ASSAY_PRJ_LUMINEX + " Study)");
        clickButton("Next");
        setFormElement(Locator.name("participantId"), "ParticipantID");
        setFormElement(Locator.name("visitId"), "100.1");
        clickButton("Copy to Study");

        log("Verify that the data was published");
        assertTextPresent("ParticipantID");
        assertTextPresent("100.1");
        assertTextPresent(TEST_ASSAY_LUM_RUN_NAME2);
        assertTextPresent("LX10005314302");

        // Upload another run that has both Raw and Summary data in the same excel file
        clickProject(TEST_ASSAY_PRJ_LUMINEX);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_LUM));
        clickButton("Import Data");
        clickButton("Next");
        setFormElement(Locator.name("name"), "raw and summary");
        setFormElement(Locator.name("__primaryFile__"), TEST_ASSAY_LUM_FILE10);
        clickButton("Next", 60000);
        clickButton("Save and Finish");

        clickAndWait(Locator.linkWithText("raw and summary"), 2 * WAIT_FOR_PAGE);
        // make sure the Summary, StdDev, and DV columns are visible
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("Summary");
        _customizeViewsHelper.addCustomizeViewColumn("StdDev");
        _customizeViewsHelper.addCustomizeViewColumn("CV");
        _customizeViewsHelper.applyCustomView();
        // show all rows (> 100 in full data file)
        clickButton("Page Size", 0);
        clickAndWait(Locator.linkWithText("Show All"));

        // check that both the raw and summary data were uploaded together
        DataRegionTable table = new DataRegionTable("Data", this);
        assertEquals("Unexpected number of data rows for both raw and summary data", 108, table.getDataRowCount());
        // check the number of rows of summary data
        table.setFilter("Summary", "Equals", "true");
        assertEquals("Unexpected number of data rows for summary data", 36, table.getDataRowCount());
        table.clearFilter("Summary");
        // check the number of rows of raw data
        table.setFilter("Summary", "Equals", "false");
        assertEquals("Unexpected number of data rows for raw data", 72, table.getDataRowCount());
        table.clearFilter("Summary");
        // check the row count at the analyte level
        table.setFilter("Analyte", "Equals", "Analyte1");
        assertEquals("Unexpected number of data rows for Analyte1", 36, table.getDataRowCount());

        // check the StdDev and % CV for a few samples
        checkStdDevAndCV("Analyte1", "S10", 3, "0.35", "9.43%");
        checkStdDevAndCV("Analyte2", "S4", 3, "3.18", "4.80%");
        checkStdDevAndCV("Analyte3", "S8", 3, "1.77", "18.13%");
    }

    private void checkStdDevAndCV(String analyte, String type, int rowCount, String stddev, String cv)
    {
        DataRegionTable table = new DataRegionTable("Data", this);
        table.setFilter("Analyte", "Equals", analyte);
        table.setFilter("Type", "Equals", type);
        assertEquals("Unexpected number of data rows for " + analyte + "/" + type, rowCount, table.getDataRowCount());
        for (int i = 0; i < rowCount; i++)
        {
            assertEquals("Wrong StdDev", stddev, table.getDataAsText(i, "StdDev"));
            assertEquals("Wrong %CV", cv, table.getDataAsText(i, "CV"));
        }
        table.clearFilter("Type");
        table.clearFilter("Analyte");
    }
}
