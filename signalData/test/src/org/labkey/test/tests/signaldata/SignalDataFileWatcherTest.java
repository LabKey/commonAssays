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
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.Filter;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Git;
import org.labkey.test.components.pipeline.PipelineTriggerWizard;
import org.labkey.test.pages.signaldata.SignalDataAssayBeginPage;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PermissionsHelper;
import org.labkey.test.util.PipelineStatusTable;
import org.labkey.test.util.PortalHelper;
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.labkey.test.util.PermissionsHelper.FOLDER_ADMIN_ROLE;
import static org.labkey.test.util.PermissionsHelper.READER_ROLE;

@Category({Git.class})
@BaseWebDriverTest.ClassTimeout(minutes = 10)
public class SignalDataFileWatcherTest extends BaseWebDriverTest
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

    private static final String METADATA_FILE_BY_NAME = "datafiles.tsv";
    private static final String METADATA_FILE_WEBDAV = "webdavDatafiles.tsv";
    private static final String METADATA_FILE_OUTSIDE_ROOT = "outsideRoot.tsv";
    private static final String METADATA_FILE_DENIED_WEBDAV = "crossProjectDeniedWebDav.tsv";
    private static final String METADATA_FILE_DENIED_SERVER_PATH = "crossProjectDeniedServerPath.tsv";
    private static final String METADATA_FILE_ALLOWED_WEBDAV = "crossProjectAllowedWebDav.tsv";
    private static final String METADATA_FILE_ALLOWED_SERVER_PATH = "crossProjectAllowedServerPath.tsv";

    // A separate project, whose pipeline root is disjoint from the test project's, holding data files that an import
    // running in the test project reaches only when its user can read that project (GitHub Issue #1391).
    private static final String FOREIGN_PROJECT = PROJECT_NAME + "Other";
    private static final String FOREIGN_REJECTED_FILENAME = "FOREIGN01.TXT";
    private static final String FOREIGN_WEBDAV_FILENAME = "FOREIGN02.TXT";
    private static final String FOREIGN_SERVER_PATH_FILENAME = "FOREIGN03.TXT";

    // Both are folder admin in the test project, which the file watcher requires of a trigger's "Run as" user; that
    // user becomes the import job's user. Only CROSS_READER_USER can read FOREIGN_PROJECT.
    private static final String LIMITED_USER = "signaldata_limited@signaldatafilewatcher.test";
    private static final String CROSS_READER_USER = "signaldata_crossreader@signaldatafilewatcher.test";

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
        assertMetadataFileNamesAreDistinct();

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

        test._containerHelper.createProject(FOREIGN_PROJECT, null);
        WebDavUploadHelper foreignUploadHelper = new WebDavUploadHelper(FOREIGN_PROJECT);
        for (String dataFile : List.of(FOREIGN_REJECTED_FILENAME, FOREIGN_WEBDAV_FILENAME, FOREIGN_SERVER_PATH_FILENAME))
        {
            foreignUploadHelper.putText(dataFile, "foreign signal data");
            assertTrue("Test requires a data file in the foreign project's file root: " + dataFile, foreignUploadHelper.fileExists(dataFile));
        }

        test._userHelper.createUser(LIMITED_USER);
        test._userHelper.createUser(CROSS_READER_USER);
        ApiPermissionsHelper permissionsHelper = new ApiPermissionsHelper(test);
        permissionsHelper.addMemberToRole(LIMITED_USER, FOLDER_ADMIN_ROLE, PermissionsHelper.MemberType.user, test.getProjectName());
        permissionsHelper.addMemberToRole(CROSS_READER_USER, FOLDER_ADMIN_ROLE, PermissionsHelper.MemberType.user, test.getProjectName());
        permissionsHelper.addMemberToRole(CROSS_READER_USER, READER_ROLE, PermissionsHelper.MemberType.user, FOREIGN_PROJECT);
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _userHelper.deleteUsers(false, LIMITED_USER, CROSS_READER_USER);
        _containerHelper.deleteProject(FOREIGN_PROJECT, afterTest);
        _containerHelper.deleteProject(getProjectName(), afterTest);
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
        File metadataFile = getFile("RunsMetadata/" + METADATA_FILE_BY_NAME);

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
        File metadataFile = TestDataUtils.writeRowsToTsv(METADATA_FILE_WEBDAV, rows);

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
        File metadataFile = TestDataUtils.writeRowsToTsv(METADATA_FILE_OUTSIDE_ROOT, rows);

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
                .waitForError(String.format("DataFile '%s' is not under a pipeline root readable by this user", outsidePath));

        // The rejection is logged as an error and surfaces in the server error log; account for it so the harness's
        // post-test error check does not fail this test.
        deleteAllPipelineJobs();
        checkExpectedErrors(1);
    }

    /**
     * GitHub Issue #1391: a data file in a project the job's user cannot read must not be pulled into this one.
     */
    @Test
    public void testWebDavDataFileFromUnreadableProjectIsRejected() throws IOException
    {
        verifyForeignDataFileRejected(METADATA_FILE_DENIED_WEBDAV, "Unreadable WebDav path trigger",
                FOREIGN_REJECTED_FILENAME, foreignWebDavPath(FOREIGN_REJECTED_FILENAME));
    }

    /**
     * GitHub Issue #1391: same as above for an absolute server-side path.
     */
    @Test
    public void testServerPathDataFileFromUnreadableProjectIsRejected() throws IOException
    {
        verifyForeignDataFileRejected(METADATA_FILE_DENIED_SERVER_PATH, "Unreadable server path trigger",
                FOREIGN_REJECTED_FILENAME, foreignFile(FOREIGN_REJECTED_FILENAME).getAbsolutePath());
    }

    /**
     * GitHub Issue #1391: resolving a data file through another project's pipeline root is intended behavior when the
     * job's user can read that project.
     */
    @Test
    public void testWebDavDataFileFromReadableProjectIsImported() throws IOException
    {
        verifyForeignDataFileImported(METADATA_FILE_ALLOWED_WEBDAV, "Readable WebDav path trigger",
                FOREIGN_WEBDAV_FILENAME, foreignWebDavPath(FOREIGN_WEBDAV_FILENAME));
    }

    /**
     * GitHub Issue #1391: same as above for an absolute server-side path.
     */
    @Test
    public void testServerPathDataFileFromReadableProjectIsImported() throws IOException
    {
        verifyForeignDataFileImported(METADATA_FILE_ALLOWED_SERVER_PATH, "Readable server path trigger",
                FOREIGN_SERVER_PATH_FILENAME, foreignFile(FOREIGN_SERVER_PATH_FILENAME).getAbsolutePath());
    }

    /**
     * Runs the cross-project import as LIMITED_USER and verifies the foreign row was rejected. The assertion matches
     * only the data file path, since either the resource ACL or the pipeline root containment check can reject it
     * depending on how the path was spelled.
     */
    private void verifyForeignDataFileRejected(String metadataFileName, String triggerName, String foreignFileName,
                                               String foreignDataFilePath) throws IOException
    {
        runForeignDataFileImport(metadataFileName, triggerName, LIMITED_USER, foreignFileName, foreignDataFilePath);

        log("Verify the import job logged the rejection of the cross-project data file");
        PipelineStatusTable statusTable = new PipelineStatusTable(getDriver());
        List<String> statuses = statusTable.getColumnDataAsText("Status");
        int errorRow = statuses.indexOf("ERROR");
        assertTrue("Expected one of the pipeline jobs to be in error, statuses were: " + statuses, errorRow >= 0);

        statusTable.clickStatusLink(errorRow)
                .waitForError(String.format("DataFile '%s'", foreignDataFilePath));

        assertEquals("A data file from an unreadable project must not be imported into this project", 0,
                foreignDataRowCount(foreignFileName));

        // The rejection is logged as an error and surfaces in the server error log; account for it so the harness's
        // post-test error check does not fail this test.
        deleteAllPipelineJobs();
        checkExpectedErrors(1);
    }

    /**
     * Runs the same cross-project import as CROSS_READER_USER, who can read the foreign project, and verifies both rows
     * imported and the foreign data file was registered in this project.
     */
    private void verifyForeignDataFileImported(String metadataFileName, String triggerName, String foreignFileName,
                                               String foreignDataFilePath) throws IOException
    {
        runForeignDataFileImport(metadataFileName, triggerName, CROSS_READER_USER, foreignFileName, foreignDataFilePath);

        log("Verify no pipeline job failed");
        List<String> statuses = new PipelineStatusTable(getDriver()).getColumnDataAsText("Status");
        assertFalse("No pipeline job should be in error, statuses were: " + statuses, statuses.contains("ERROR"));

        log("Verify the run imported both the local and the cross-project data file");
        SignalDataAssayBeginPage beginPage = navigateToAssayLandingPage(SignalDataInitializer.RAW_SignalData_ASSAY);
        beginPage.setSearchBox(getImportedRunIdentifier(beginPage));
        assertEquals("Incorrect number of rows imported by the file watcher", 2, beginPage.getRowCount());

        // Sorted, because the grid's default order is not part of what this test is asserting.
        List<String> names = new ArrayList<>(beginPage.getDataRegionTable().getColumnDataAsText("Name"));
        Collections.sort(names);
        assertEquals("Incorrect Name values for the imported run", List.of(foreignFileName, RESULT_FILENAME_1), names);

        assertEquals("The cross-project data file should be registered in this project", 1,
                foreignDataRowCount(foreignFileName));
    }

    /**
     * Drops a metadata file holding one row resolvable in this project and one row pointing at FOREIGN_PROJECT, then
     * waits for the file watcher's move and import jobs.
     */
    private void runForeignDataFileImport(String metadataFileName, String triggerName, String runAsUser,
                                          String foreignFileName, String foreignDataFilePath) throws IOException
    {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Name", "DataFile"));
        rows.add(List.of(RESULT_FILENAME_1, RESULT_FILENAME_1));
        rows.add(List.of(foreignFileName, foreignDataFilePath));
        File metadataFile = TestDataUtils.writeRowsToTsv(metadataFileName, rows);

        log("Configure a file watcher trigger for the Signal Data import pipeline, running as " + runAsUser);
        createImportTrigger(triggerName, metadataFile.getName(), _userHelper.getDisplayNameForEmail(runAsUser));

        log("Drop a metadata file referencing a data file in another project");
        goToProjectHome();
        _fileBrowserHelper.dragDropUpload(metadataFile);

        log("Wait for the file watcher import job to finish");
        goToDataPipeline();
        waitForPipelineJobsToFinish(2);
    }

    private int foreignDataRowCount(String foreignFileName) throws IOException
    {
        return executeSelectRowCommand("exp", "Data", ContainerFilter.CurrentAndSubfolders, "/" + getProjectName(),
                List.of(new Filter("Name", foreignFileName))).getRowCount().intValue();
    }

    /**
     * The server-relative WebDAV resource path for a data file at the top of FOREIGN_PROJECT's file root.
     */
    private String foreignWebDavPath(String fileName)
    {
        return String.format("/_webdav/%s/@files/%s", FOREIGN_PROJECT, fileName);
    }

    private File foreignFile(String fileName)
    {
        File file = FileUtil.appendName(TestFileUtils.getDefaultFileRoot(FOREIGN_PROJECT), fileName);
        assertTrue("Test requires the foreign data file to exist at " + file, file.exists());
        return file;
    }

    /**
     * The file watcher matches its file pattern with Matcher.find(), so a trigger watching for one metadata file also
     * picks up a leftover file whose name merely contains that pattern, importing two runs where the test expects one.
     */
    private static void assertMetadataFileNamesAreDistinct()
    {
        List<String> names = List.of(METADATA_FILE_BY_NAME, METADATA_FILE_WEBDAV, METADATA_FILE_OUTSIDE_ROOT,
                METADATA_FILE_DENIED_WEBDAV, METADATA_FILE_DENIED_SERVER_PATH, METADATA_FILE_ALLOWED_WEBDAV,
                METADATA_FILE_ALLOWED_SERVER_PATH);
        for (String pattern : names)
        {
            for (String other : names)
            {
                if (!pattern.equals(other) && other.contains(pattern))
                    fail(String.format("Metadata file name '%s' contains '%s', so the trigger watching for '%s' would also import '%s'",
                            other, pattern, pattern, other));
            }
        }
    }

    private void createImportTrigger(String name, String filePattern)
    {
        createImportTrigger(name, filePattern, null);
    }

    private void createImportTrigger(String name, String filePattern, @Nullable String runAsDisplayName)
    {
        goToProjectHome();
        goToFolderManagement().goToImportTab();
        waitAndClickAndWait(Locator.linkWithText(IMPORT_PIPELINE_TASK));

        PipelineTriggerWizard wizard = new PipelineTriggerWizard(getDriver());
        wizard.setName(name)
                .setTask(IMPORT_PIPELINE_TASK)
                .setEnabled(true);
        if (runAsDisplayName != null)
            wizard.setUsername(runAsDisplayName);
        wizard.goToConfiguration()
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
