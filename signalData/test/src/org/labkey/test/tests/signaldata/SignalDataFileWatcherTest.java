/*
 * Copyright (c) 2016-2026 LabKey Corporation
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
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.Path;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.pipeline.PipelineTriggerWizard;
import org.labkey.test.pages.signaldata.SignalDataAssayBeginPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PipelineStatusTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.PostgresOnlyTest;
import org.labkey.test.util.core.webdav.WebDavUploadHelper;
import org.labkey.test.util.data.TestDataUtils;
import org.labkey.test.util.query.QueryUtils;
import org.labkey.test.util.signaldata.SignalDataInitializer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 10)
public class SignalDataFileWatcherTest extends BaseWebDriverTest implements PostgresOnlyTest
{
    private static final String PROJECT_NAME = "SignalDataFileWatcherTest";
    private static final String DEFAULT_RUN = "TestRun001";
    private static final String ASSAY_DATA_LOC = "SignalDataAssayData/" + DEFAULT_RUN;

    // The pipeline description registered for the import task in signaldataContext.xml. It is the link text on the
    // folder management Import tab and the label of the trigger's task selector.
    private static final String IMPORT_PIPELINE_TASK = "Import Signal Data run from a Metadata File";

    // Data files referenced by RunsMetadata/datafiles.tsv (by bare file name).
    private static final String RESULT_FILENAME_1 = "LGC12392.TXT";
    private static final String RESULT_FILENAME_2 = "LGC14332.TXT";
    private static final String RESULT_FILENAME_3 = "MPP82113.TXT";

    // A file root subdirectory, separate from where metadata files are dropped, used to exercise data files
    // referenced by WebDAV path rather than by bare name.
    private static final String ALT_DATA_DIR = "altData";

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

    @BeforeClass
    public static void doSetup() throws Exception
    {
        SignalDataFileWatcherTest test = getCurrentTest();
        SignalDataInitializer initializer = new SignalDataInitializer(test, test.getProjectName());
        initializer.setupProject();

        // The file watcher drops files into the project file root; expose a file browser there for the upload.
        test.goToProjectHome();
        new PortalHelper(test.getDriver()).addBodyWebPart("Files");

        // Place the data files in two locations: the file root, where the metadata file will be dropped (so rows
        // that reference data files by bare name resolve relative to the metadata file's directory), and a separate
        // subdirectory referenced by WebDAV path in testWebDavDataFilePaths.
        WebDavUploadHelper uploadHelper = new WebDavUploadHelper(test.getPrimaryTestProject());
        uploadHelper.mkDir(ALT_DATA_DIR);
        for (String dataFile : List.of(RESULT_FILENAME_1, RESULT_FILENAME_2, RESULT_FILENAME_3))
        {
            File file = test.getFile(ASSAY_DATA_LOC + "/" + dataFile);
            uploadHelper.uploadFile(file, "");
            uploadHelper.uploadFile(file, ALT_DATA_DIR);
        }
    }

    @Before
    public void preTest() throws Exception
    {
        // Each test imports a new run (the import task names runs with a generated timestamp). Delete any runs from
        // a previous test, keeping only the run created during setup, so the imported run can be identified by
        // exclusion. Also clear trigger configurations and completed jobs between tests.
        log("Reset runs, trigger configurations, and completed jobs");
        navigateToAssayLandingPage(SignalDataInitializer.RAW_SignalData_ASSAY).resetUploadedData(DEFAULT_RUN);
        QueryUtils.truncateTable(getProjectName(), "pipeline", "TriggerConfigurations");
        deleteAllPipelineJobs();
    }

    @Test
    public void testMetadataFileWatcherImport()
    {
        File metadataFile = getFile("RunsMetadata/datafiles.tsv");

        log("Configure a file watcher trigger for the Signal Data import pipeline");
        createImportTrigger("Signal Data import trigger", metadataFile.getName());

        log("Drop the metadata file into the watched file root to trigger the import");
        goToProjectHome();
        _fileBrowserHelper.dragDropUpload(metadataFile);

        log("Wait for the file watcher to run the import job");
        goToDataPipeline();
        waitForPipelineJobsToFinish(2);

        log("Verify a new run was imported from the metadata file");
        SignalDataAssayBeginPage beginPage = navigateToAssayLandingPage(SignalDataInitializer.RAW_SignalData_ASSAY);
        String importedRun = getImportedRunIdentifier(beginPage);

        beginPage.setSearchBox(importedRun);
        assertEquals("Incorrect number of rows imported by the file watcher", 3, beginPage.getRowCount());

        DataRegionTable table = beginPage.getDataRegionTable();
        assertArrayEquals("Incorrect Name values for the imported run",
                new String[]{RESULT_FILENAME_1, RESULT_FILENAME_2, RESULT_FILENAME_3},
                table.getColumnDataAsText("Name").toArray());
        assertArrayEquals("Incorrect StringValue values for the imported run",
                new String[]{"StringOne", "StringTwo", "StringThree"},
                table.getColumnDataAsText("StringValue").toArray());
        assertArrayEquals("Incorrect IntegerValue values for the imported run",
                new String[]{"1", "2", "3"},
                table.getColumnDataAsText("IntegerValue").toArray());
    }

    @Test
    public void testWebDavDataFilePaths() throws IOException
    {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Name", "DataFile", "StringValue", "IntegerValue"));
        rows.add(List.of(RESULT_FILENAME_1, webDavPath(RESULT_FILENAME_1), "StringOne", "1"));
        rows.add(List.of(RESULT_FILENAME_2, webDavPath(RESULT_FILENAME_2), "StringTwo", "2"));
        rows.add(List.of(RESULT_FILENAME_3, webDavPath(RESULT_FILENAME_3), "StringThree", "3"));
        File metadataFile = TestDataUtils.writeRowsToTsv("webdavDatafiles.tsv", rows);

        log("Configure a file watcher trigger for the Signal Data import pipeline");
        createImportTrigger("WebDav paths trigger", metadataFile.getName());

        log("Drop the metadata file; its data files are referenced by WebDAV path in a separate folder");
        goToProjectHome();
        _fileBrowserHelper.dragDropUpload(metadataFile);

        log("Wait for the file watcher to run the import job");
        goToDataPipeline();
        waitForPipelineJobsToFinish(2);

        log("Verify a new run was imported from the WebDAV-referenced data files");
        SignalDataAssayBeginPage beginPage = navigateToAssayLandingPage(SignalDataInitializer.RAW_SignalData_ASSAY);
        String importedRun = getImportedRunIdentifier(beginPage);

        beginPage.setSearchBox(importedRun);
        assertEquals("Incorrect number of rows imported by the file watcher", 3, beginPage.getRowCount());

        DataRegionTable table = beginPage.getDataRegionTable();
        assertArrayEquals("Incorrect Name values for the imported run",
                new String[]{RESULT_FILENAME_1, RESULT_FILENAME_2, RESULT_FILENAME_3},
                table.getColumnDataAsText("Name").toArray());
        assertArrayEquals("Incorrect StringValue values for the imported run",
                new String[]{"StringOne", "StringTwo", "StringThree"},
                table.getColumnDataAsText("StringValue").toArray());
        assertArrayEquals("Incorrect IntegerValue values for the imported run",
                new String[]{"1", "2", "3"},
                table.getColumnDataAsText("IntegerValue").toArray());
    }

    @Test
    public void testDataFileOutsidePipelineRootIsRejected() throws IOException
    {
        String outsidePath = "/not/a/pipeline/root/OutsideRoot.TXT";
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Name", "DataFile"));
        rows.add(List.of(RESULT_FILENAME_1, RESULT_FILENAME_1));
        rows.add(List.of("OutsideRoot.TXT", outsidePath));
        File metadataFile = TestDataUtils.writeRowsToTsv("outsideRoot.tsv", rows);

        log("Configure a file watcher trigger for the Signal Data import pipeline");
        createImportTrigger("Outside root trigger", metadataFile.getName());

        log("Drop a metadata file referencing a data file outside any pipeline root");
        goToProjectHome();
        _fileBrowserHelper.dragDropUpload(metadataFile);

        log("Wait for the file watcher import job to finish");
        goToDataPipeline();
        waitForPipelineJobsToFinish(2);

        log("Verify the import job logged the pipeline-root rejection");
        PipelineStatusTable statusTable = new PipelineStatusTable(getDriver());
        List<String> statuses = statusTable.getColumnDataAsText("Status");
        int errorRow = statuses.indexOf("ERROR");
        assertTrue("Expected one of the pipeline jobs to be in error, statuses were: " + statuses, errorRow >= 0);

        statusTable.clickStatusLink(errorRow)
                .waitForError(String.format("DataFile '%s' is not under a server-managed pipeline root", outsidePath));

        // The rejection is logged as an error and surfaces in the server error log; account for it so the harness's
        // post-test error check does not fail this test.
        deleteAllPipelineJobs();
        checkExpectedErrors(1);
    }

    private void createImportTrigger(String name, String filePattern)
    {
        goToProjectHome();
        goToFolderManagement().goToImportTab();
        waitAndClickAndWait(Locator.linkWithText(IMPORT_PIPELINE_TASK));

        PipelineTriggerWizard wizard = new PipelineTriggerWizard(getDriver());
        wizard.setName(name)
                .setTask(IMPORT_PIPELINE_TASK)
                .setEnabled(true)
                .goToConfiguration()
                .setLocation(".")
                .setFilePattern(filePattern)
                // The 'protocolName' custom field declared by SignalDataImportTask's pipeline; the wizard binds
                // setAssayProtocol() to the input named "protocolName".
                .setAssayProtocol(SignalDataInitializer.RAW_SignalData_ASSAY);
        wizard.saveConfiguration();
    }

    /**
     * Returns the single run identifier present on the assay landing page other than the run created during setup.
     * The import task names the run with a generated timestamp, so it is identified by exclusion.
     */
    private String getImportedRunIdentifier(SignalDataAssayBeginPage beginPage)
    {
        List<String> importedRuns = beginPage.getDataRegionTable().getColumnDataAsText("Run Identifier").stream()
                .filter(runId -> !DEFAULT_RUN.equals(runId))
                .distinct()
                .toList();
        assertEquals("Expected exactly one newly imported run, found: " + importedRuns, 1, importedRuns.size());
        return importedRuns.get(0);
    }

    /**
     * Builds the server-relative WebDAV resource path for a data file in the alternate data directory, in the form
     * resolved by {@code WebdavService.lookup}: {@code /_webdav/<container>/@files/<dir>/<file>}. The project name
     * has no characters that require encoding.
     */
    private String webDavPath(String fileName)
    {
        return String.format("/_webdav/%s/@files/%s/%s", getProjectName(), ALT_DATA_DIR, fileName);
    }

    private File getFile(String relativePath)
    {
        File file = FileUtil.appendPath(SignalDataInitializer.RAW_SignalData_SAMPLE_DATA, Path.parse(relativePath));
        if (!file.exists())
            throw new RuntimeException("Can't find path: " + file.getAbsolutePath());
        return file;
    }

    private SignalDataAssayBeginPage navigateToAssayLandingPage(String assayName)
    {
        goToProjectHome();
        clickAndWait(Locator.linkWithText(assayName));
        SignalDataAssayBeginPage page = new SignalDataAssayBeginPage(this);
        page.waitForPageLoad();
        return page;
    }
}
