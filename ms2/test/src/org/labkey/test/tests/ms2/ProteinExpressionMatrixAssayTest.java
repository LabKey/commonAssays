/*
 * Copyright (c) 2018-2019 LabKey Corporation
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
package org.labkey.test.tests.ms2;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.util.DataRegionTable;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@Category({DailyB.class})
@BaseWebDriverTest.ClassTimeout(minutes = 7)
public class ProteinExpressionMatrixAssayTest extends BaseWebDriverTest
{
    private static final String ASSAY_NAME = "Test Protein Expression Matrix";
    private static final String FOLDER_TYPE_MS2 = "MS2";
    private static final String FOLDER_NAME = "ProteinExpressionMatrixFolder";

    private static final File FASTA_FILE = TestFileUtils.getSampleData("xarfiles/ms2pipe/databases/Bovine_mini1.fasta");

    private static final List<String> SAMPLE_IDS = Arrays.asList("Condition B", "Condition D", "Condition E", "Condition F", "Condition G", "Condition H");


    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);

        //delete record of the import
        beginAt("ms2/showProteinAdmin.view?"); //go to Protein Database Admin

        final DataRegionTable annotInsertions = new DataRegionTable("AnnotInsertions", getDriver());

        annotInsertions.setFilter("FileName", "Contains", FASTA_FILE.getName());
        List<String> fileNames = annotInsertions.getColumnDataAsText("FileName");
        if(fileNames.size() > 0)
        {
            annotInsertions.checkAll();
            doAndWaitForPageToLoad(() ->
            {
                annotInsertions.clickHeaderButtonByText("Delete");
                acceptAlert();
            });
        }
    }

    @BeforeClass
    public static void setupProject()
    {
        ProteinExpressionMatrixAssayTest init = (ProteinExpressionMatrixAssayTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), FOLDER_TYPE_MS2);

        //load sample protein database
        beginAt("ms2/showProteinAdmin.view?");

        clickButton("Import Data");
        setFormElement(Locator.id("fname"), FASTA_FILE);
        selectOptionByText(Locator.name("fileType"), "fasta");
        clickButton("Load Annotations");

        //create a folder of type MS2
        goToProjectHome();
        _containerHelper.createSubfolder(getProjectName(), FOLDER_NAME);
        goToProjectHome(getProjectName());
        clickFolder(FOLDER_NAME);
        _containerHelper.setFolderType(FOLDER_TYPE_MS2);

        //create New Assay Design of type 'Protein Expression Matrix'
        _assayHelper.createAssayDesignWithDefaults("Protein Expression Matrix", ASSAY_NAME);
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testTSVImport()
    {
        importProteinExpressionData("testingTSV", TestFileUtils.getSampleData("ms2/matrix/MatchesFasta.tsv"));
    }

    @Test
    public void testExcelImport()
    {
        importProteinExpressionData("testingExcel", TestFileUtils.getSampleData("ms2/matrix/MatchesFasta.xlsx"));
    }

    @Test
    public void testBadDataInTSV()
    {
        importProteinExpressionWithBadData(TestFileUtils.getSampleData("ms2/matrix/DoesNotMatchFasta.tsv"));
    }

    @Test
    public void testBadDataInExcel()
    {
        importProteinExpressionWithBadData(TestFileUtils.getSampleData("ms2/matrix/DoesNotMatchFasta.xlsx"));
    }

    @Test
    public void testDuplicatesInTSV()
    {
        importProteinExpressionWithDuplicates(TestFileUtils.getSampleData("ms2/matrix/Duplicates.tsv"));
    }

    @Test
    public void testDuplicatesInExcel()
    {
        importProteinExpressionWithDuplicates(TestFileUtils.getSampleData("ms2/matrix/Duplicates.xlsx"));
    }

    @Test
    public void testCorrectLookupType()
    {
        goToProjectHome();
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        ReactAssayDesignerPage assayDesignerPage = _assayHelper.clickEditAssayDesign();
        assayDesignerPage.expandFieldProperties("Run");
        assertElementPresent(Locator.tagWithClass("div", "domain-field-details").containing("protein > FastaFiles"));
        assayDesignerPage.clickFinish();
    }

    private void importProteinExpressionData(String assayId, File exprDataFile)
    {
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        clickAndWait(Locator.linkWithSpan("Import Data"));
        setFormElement(Locator.name("name"), assayId);
        selectOptionByText(Locator.name("fastaFormatProteinSequences"), FASTA_FILE.getPath());
        setFormElement(Locator.name("__primaryFile__"), exprDataFile);
        clickAndWait(Locator.linkWithSpan("Save and Finish"));

        testLinks(assayId);
        ensureEmptyConditionValuesHandled(assayId); //test empty conditions are handled properly
    }

    private void importProteinExpressionWithBadData(File exprBadDataFile)
    {
        pushLocation();

        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        clickAndWait(Locator.linkWithSpan("Import Data"));
        selectOptionByText(Locator.name("fastaFormatProteinSequences"), FASTA_FILE.getPath());
        setFormElement(Locator.name("__primaryFile__"), exprBadDataFile);
        clickAndWait(Locator.linkWithSpan("Save and Finish"));

        assertTextPresent("Unable to find protein '30S_ribosomal_pro_NotInFasta' in the selected FASTA file.");

        popLocation();
    }

    private void importProteinExpressionWithDuplicates(File exprDataWithDuplicates)
    {
        pushLocation();

        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        clickAndWait(Locator.linkWithSpan("Import Data"));
        selectOptionByText(Locator.name("fastaFormatProteinSequences"), FASTA_FILE.getPath());
        setFormElement(Locator.name("__primaryFile__"), exprDataWithDuplicates);
        clickAndWait(Locator.linkWithSpan("Save and Finish"));

        assertTextPresent("SQLException"); //TODO: Issue 23232: Provide a useful and user readable error message while importing protein expression data with duplicate IDs.

        popLocation();
    }

    private void testLinks(String assayId)
    {
        clickAndWait(Locator.linkWithText(assayId));

        String currentRelativeURL1 = getCurrentRelativeURL();
        pushLocation();
        clickAndWait(Locator.linkWithText(assayId));
        String currentRelativeURL2 = getCurrentRelativeURL();
        assertNotEquals(assayId + " in Run column is not a link.", currentRelativeURL1, currentRelativeURL2);
        assertTextPresent("Standard Properties");
        popLocation();

        currentRelativeURL1 = getCurrentRelativeURL();
        pushLocation();
        clickAndWait(Locator.linkWithText("gi|18071135|gb|AAL58190.1|"));
        currentRelativeURL2 = getCurrentRelativeURL();
        assertNotEquals("'gi|18071135|gb|AAL58190.1|' in Protein column is not a link.", currentRelativeURL1, currentRelativeURL2);
        assertTextPresent("Protein Sequence");
        popLocation();

        currentRelativeURL1 = getCurrentRelativeURL();
        pushLocation();
        clickAndWait(Locator.linkWithText("Condition G"));
        currentRelativeURL2 = getCurrentRelativeURL();
        assertNotEquals("'Condition G' in Sample Id column is not a link.", currentRelativeURL1, currentRelativeURL2);
        assertTextPresent("Sample Condition G");
        popLocation();
    }

    private void ensureEmptyConditionValuesHandled(String assayId)
    {
        final DataRegionTable proteinCol = new DataRegionTable("Data", getDriver());
        proteinCol.setFilter("SeqId", "Contains", "gi|628074|pir||JP0060");
        proteinCol.setSort("SampleId", SortDirection.ASC);
        List<String> columnDataAsText = proteinCol.getColumnDataAsText("Sample Id");
        assertEquals("Condition labels in Sample Id column does not match.", columnDataAsText, SAMPLE_IDS);
        proteinCol.clearFilter("SeqId");
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "ProteinExpresssionMatrixAssayTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("ms2");
    }

}