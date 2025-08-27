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
package org.labkey.test.tests.signaldata;

import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.signaldata.SignalDataAssayBeginPage;
import org.labkey.test.pages.signaldata.SignalDataRunViewerPage;
import org.labkey.test.pages.signaldata.SignalDataUploadPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.PostgresOnlyTest;
import org.labkey.test.util.signaldata.SignalDataInitializer;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@Category({Daily.class})
public class SignalDataRawTest extends BaseWebDriverTest implements PostgresOnlyTest
{
    private static final String PROJECT_NAME = "SignalDataRawTest";
    private static final String DEFAULT_RUN = "TestRun001";
    private static final String ASSAY_DATA_LOC = "SignalDataAssayData/" + DEFAULT_RUN;
    private static final String RESULT_FILENAME_1 = "LGC12392.TXT";
    private static final String RESULT_FILENAME_2 = "LGC14332.TXT";
    private static final String RESULT_FILENAME_3 = "MPP82113.TXT";
    private static final String RESULT_FILENAME_4 = "TD789-12.TXT";
    private static final String RESULT_FILENAME_5 = "TD789-25.TXT";
    private static final String RESULT_FILENAME_6 = "QD123-11.TXT";
    private static final String RESULT_FILENAME_7 = "QD123-24.TXT";

    @Nullable
    @Override
    protected final String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Collections.singletonList("SignalData");
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @BeforeClass
    public static void doSetup() throws Exception
    {
        SignalDataRawTest test = getCurrentTest();
        SignalDataInitializer _initializer = new SignalDataInitializer(test, test.getProjectName());
        _initializer.setupProject();
    }

    @Before
    public void preTest()
    {
        // Reset to the original run/data file set created in the initialize
        navigateToAssayLandingPage().resetUploadedData(DEFAULT_RUN);
    }

    @Test
    public void testRunsSearch()
    {
        SignalDataAssayBeginPage beginPage = navigateToAssayLandingPage();

        //Test search by file
        beginPage.setSearchBox(RESULT_FILENAME_1);
        assertEquals("Test file not found in search", 1, beginPage.getRowCount());

        //Test search by Run Identifier
        beginPage.setSearchBox(DEFAULT_RUN);
        assertEquals("Incorrect number of rows for run identifier " + DEFAULT_RUN,
                getFile(ASSAY_DATA_LOC).list().length, beginPage.getRowCount());
    }

    @Test
    public void testRunViewer()
    {
        // TODO: Test the run viewer. See FormulationsTest.qualityControlHPLCData for guidance.
        SignalDataAssayBeginPage beginPage = navigateToAssayLandingPage();

        beginPage.selectData(RESULT_FILENAME_1, DEFAULT_RUN);
        beginPage.selectData(RESULT_FILENAME_2, DEFAULT_RUN);
        SignalDataRunViewerPage runsPage = beginPage.viewRuns();

        runsPage.checkRunViewerCheckbox(resultNameFromFilename(RESULT_FILENAME_1));
        runsPage.checkRunViewerCheckbox(resultNameFromFilename(RESULT_FILENAME_2));
        WebElement plotEl = runsPage.showPlot();
        List<WebElement> plotLines = Locator.tagWithClass("path", "line").findElements(plotEl);
        assertEquals("Wrong number of lines in plot", 2, plotLines.size());
    }

    private String resultNameFromFilename(String filename)
    {
        //Trim extension
        return filename.substring(0,filename.length()-4);
    }

    @Test
    public void testFileImport()
    {
        File metadataFile = getFile("RunsMetadata/datafiles.tsv");
        Map<String, List<String>> expectedData = Map.of("StringValue", List.of("StringOne", "StringTwo", "StringThree"),
                "IntegerValue", List.of("1", "2", "3"));
        SignalDataAssayBeginPage beginPage = importRun("importTest1",
                metadataFile,
                List.of(getFile(String.join("/", ASSAY_DATA_LOC, "BLANK235.TXT"))),
                List.of(
                        getFile(String.join("/", ASSAY_DATA_LOC, RESULT_FILENAME_1)),
                        getFile(String.join("/", ASSAY_DATA_LOC, RESULT_FILENAME_2)),
                        getFile(String.join("/", ASSAY_DATA_LOC, RESULT_FILENAME_3))
                ),
                expectedData, 3);

        ///////////  Check clearing run  ///////////
        log("Check clearing a run during import");
        //Create new run
        SignalDataUploadPage uploadPage = beginPage.navigateToImportPage();
        uploadPage.uploadMetadataFile(metadataFile);
        uploadPage.setRunIDField("cleared run");
        assertElementPresent(Ext4Helper.Locators.getGridRow()); //Check grid has elements
        uploadPage.clearRun();
        navigateToAssayLandingPage();  //Should not cause unload warning

        // test upload of metadata file with full data file paths
        importRun("importTest2",
            getFile("RunsMetadata/datafiles2.tsv"),
            Collections.emptyList(),
            List.of(
                    getFile(String.join("/", ASSAY_DATA_LOC, RESULT_FILENAME_4)),
                    getFile(String.join("/", ASSAY_DATA_LOC, RESULT_FILENAME_5)),
                    getFile(String.join("/", ASSAY_DATA_LOC, RESULT_FILENAME_6)),
                    getFile(String.join("/", ASSAY_DATA_LOC, RESULT_FILENAME_7))
            ), Collections.EMPTY_MAP, 4);

        // test import of files with a subset of the metadata files
        importRun("importTest3",
                getFile("RunsMetadata/datafiles.tsv"),
                List.of(getFile(String.join("/", ASSAY_DATA_LOC, "BLANK235.TXT"))),
                List.of(
                        getFile(String.join("/", ASSAY_DATA_LOC, RESULT_FILENAME_1)),
                        getFile(String.join("/", ASSAY_DATA_LOC, RESULT_FILENAME_3))
                ),
                expectedData, 3);

        importRun("importTest4",
                getFile("RunsMetadata/datafiles2.tsv"),
                Collections.emptyList(),
                List.of(
                        getFile(String.join("/", ASSAY_DATA_LOC, RESULT_FILENAME_6)),
                        getFile(String.join("/", ASSAY_DATA_LOC, RESULT_FILENAME_7))
                ), Collections.EMPTY_MAP, 4);
    }

    private SignalDataAssayBeginPage importRun(
        String runName,
        File metadataFile,
        List<File> unspecifiedDataFiles,
        List<File> dataFiles,
        Map<String, List<String>> expectedData,
        int expectedResultRows
    )
    {
        SignalDataAssayBeginPage beginPage = navigateToAssayLandingPage();
        SignalDataUploadPage uploadPage = beginPage.navigateToImportPage();

        log("Uploading metadata file");
        uploadPage.uploadMetadataFile(metadataFile);

        for (File dataFile : unspecifiedDataFiles)
        {
            log("Attempting to upload a data file not specified in metadata");
            uploadPage.uploadIncorrectFile(dataFile);
        }

        log("Uploading data files");
        int uploadCount = dataFiles.size();
        uploadPage.uploadFile(dataFiles);
        uploadPage.waitForProgressBars(uploadCount);

        uploadPage.setRunIDField(runName);
        uploadPage.saveRun();
        beginPage.waitForPageLoad();
        log("Verifying run was added");
        beginPage.waitForGridValue(runName, expectedResultRows);

        // verify the uploaded files in the run
        beginPage.setSearchBox(runName);
        assertEquals("Incorrect number of rows for imported run " + runName, expectedResultRows, beginPage.getRowCount());

        // verify any data row values
        DataRegionTable table = beginPage.getDataRegionTable();
        for (Map.Entry<String, List<String>> entry : expectedData.entrySet())
        {
            List<String> values = table.getColumnDataAsText(entry.getKey());
            assertArrayEquals(values.toArray(), entry.getValue().toArray());
        }
        return beginPage;
    }

    private File getFile(String relativePath)
    {
        File file = new File(SignalDataInitializer.RAW_SignalData_SAMPLE_DATA, relativePath);
        if (!file.exists())
            throw new RuntimeException("Can't find path: " + file.getAbsolutePath());
        return file;
    }

    private SignalDataAssayBeginPage navigateToAssayLandingPage()
    {
        //Navigate to Landing Page
        goToProjectHome();
        clickAndWait(Locator.linkWithText(SignalDataInitializer.RAW_SignalData_ASSAY));
        SignalDataAssayBeginPage page = new SignalDataAssayBeginPage(this);
        page.waitForPageLoad();
        return page;
    }
}
