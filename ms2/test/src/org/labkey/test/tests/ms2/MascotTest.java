/*
 * Copyright (c) 2006-2017 LabKey Corporation
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

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestCredentials;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
import org.labkey.test.categories.Mascot;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.credentials.Login;
import org.labkey.test.ms2.AbstractMS2SearchEngineTest;
import org.labkey.test.pages.ms2.MascotConfigPage;
import org.labkey.test.pages.ms2.MascotTestPage;
import org.labkey.test.util.DataRegionExportHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.TextSearcher;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the fields added to the Customize Site form for the MS2 modules.
 *
 * WCH: Please take note on how you should set up the sequence database
 *      Bovine_mini.fasta Mascot server.  You should copy it to
 *      <Mascot dir>/sequence/Bovine_mini.fasta/current/Bovine_mini.fasta
 *      Its name MUST BE "Bovine_mini.fasta" (excluding the quotes)
 *      Its Path "<Mascot dir>/sequence/Bovine_mini.fasta/current/Bovine_mini*.fasta" (excluding the quotes, and note the *)
 *      Its Rule to parse accession string from Fasta file: MUST BE
 *          Rule 4 ">\([^ ]*\)"      (the rule number can be different, but regex must be the same or equivalent)
 *      Its Rule to Rule to parse description string from Fasta file: MUST BE
 *          Rule 5 ">[^ ]* \(.*\)"   (the rule number can be different, but regex must be the same or equivalent)
 *
 */
@Category({MS2.class, Mascot.class, DailyB.class})
public class MascotTest extends AbstractMS2SearchEngineTest
{
    protected static final String PEPTIDE = "R.RLPVGADR.G";
    protected static final String PEPTIDE2 = "R.SREVYIVATGYK.G";
    protected static final String PEPTIDE3 = "K.ENEPFEAALRR.F";
    protected static final String PEPTIDE4 = "-.MDIGAVKFGAFK.L";
    protected static final String PEPTIDE5 = "K.ASTVERLVTALHTLLQDMVAAPASR.L";
    protected static final String PROTEIN = "gi|23335713|hypothetical_prot";
    protected static final String SEARCH = "gi|23335713|hypothetical_prot";
    protected static final String SEARCH_FIND = "BIFIDOBACTERIUM LONGUM";
    protected static final String SEARCH_FIND_ALT = "Bifidobacterium longum";
    protected static final String PROTOCOL = "Mascot analysis";
    protected static final String SEARCH_TYPE = "mascot";
    protected static final String SEARCH_BUTTON = "Mascot";
    protected static final String SEARCH_NAME = "MASCOT";

    private static String MASCOT_HOST;
    private static Login MASCOT_USER_LOGIN;
    private static String MASCOT_PROXY;

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        cleanPipe(SEARCH_TYPE);

        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @BeforeClass
    public static void setupProject() throws Exception
    {
        MascotTest init = (MascotTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup() throws IOException
    {
        if (TestCredentials.hasCredentials(SEARCH_TYPE))
        {
            MASCOT_HOST = TestCredentials.getServer(SEARCH_TYPE).getHost();
            MASCOT_USER_LOGIN = TestCredentials.getServer(SEARCH_TYPE).getLogins().get(0);
            MASCOT_PROXY = (String)TestCredentials.getServer(SEARCH_TYPE).getExtraValues().get("proxy");
        }

        createProjectAndFolder();
        PortalHelper _portalHelper = new PortalHelper(this);
        navigateToFolder(FOLDER_NAME);
        _portalHelper.addWebPart("MS2 Runs Browser");
    }

    @Test
    public void testMascotAuthentication()
    {
        Assume.assumeTrue("Add Mascot Server info to test.credentials.json to test Mascot authentication",
                TestCredentials.hasCredentials(SEARCH_TYPE));

        String mascotServerURL = MASCOT_HOST;
        String mascotUserAccount = MASCOT_USER_LOGIN.getUsername();
        String mascotUserPassword = MASCOT_USER_LOGIN.getPassword();
        String mascotHTTPProxyURL = MASCOT_PROXY;
        MascotConfigPage configPage;
        MascotTestPage testPage;

        // Case 1: default setting has to pass as it is configured by the administrator initially
        log("Testing Mascot settings");
        configPage = MascotConfigPage.beginAt(this);
        configPage.
                setMascotServer(mascotServerURL).
                setMascotUser(mascotUserAccount).
                setMascotPassword(mascotUserPassword);
        testPage = configPage.testMascotSettings();
        assertTextPresent("Test passed.");
        configPage = testPage.close();

        // Case 2: correct server, wrong user id
        log("Testing non-existent Mascot user via " + mascotServerURL);
        configPage.
                setMascotServer(mascotServerURL).
                setMascotUser("nonexistent").
                setMascotPassword(mascotUserPassword);
        testPage = configPage.testMascotSettings();
        assertTextPresent("Test failed.");
        configPage = testPage.close();

        // Case 3: correct server, wrong user password
        log("Testing wrong password fo Mascot user " + mascotUserAccount + " via " + mascotServerURL);
        configPage.
                setMascotServer(mascotServerURL).
                setMascotUser(mascotUserAccount).
                setMascotPassword("wrongpassword");
        testPage = configPage.testMascotSettings();
        assertTextPresent("Test failed.");
        testPage.close();
    }

    @Test
    public void testAlternateMascotAuthentication() throws Exception
    {
        Assume.assumeTrue("Add Mascot Server info to test.credentials.json to test Mascot authentication",
                TestCredentials.hasCredentials(SEARCH_TYPE));

        URL url = new URL((MASCOT_HOST.startsWith("http://") ? "" : "http://") + MASCOT_HOST);
        StringBuilder alternativeLink = new StringBuilder("http://");
        alternativeLink.append(url.getHost());
        if (80 != url.getPort() && -1 != url.getPort())
        {
            alternativeLink.append(":").append(url.getPort());
        }
        alternativeLink.append("/");
        if ("".equals(url.getPath()))
            alternativeLink.append("alternativefolder/");

        String mascotServerURL = alternativeLink.toString();
        String mascotUserAccount = MASCOT_USER_LOGIN.getUsername();
        String mascotUserPassword = MASCOT_USER_LOGIN.getPassword();
        String mascotHTTPProxyURL = MASCOT_PROXY;
        MascotConfigPage configPage;
        MascotTestPage testPage;

        // Case 1: default setting has to pass as it is configured by the administrator initially
        log("Testing Mascot settings");
        configPage = MascotConfigPage.beginAt(this);
        configPage.
                setMascotServer(mascotServerURL).
                setMascotUser(mascotUserAccount).
                setMascotPassword(mascotUserPassword);
        testPage = configPage.testMascotSettings();
        assertTextPresent("Test passed.");
        configPage = testPage.close();

        // Case 2: correct server, wrong user id
        log("Testing non-existent Mascot user via " + mascotServerURL);
        configPage.
                setMascotServer(mascotServerURL).
                setMascotUser("nonexistent").
                setMascotPassword(mascotUserPassword);
        testPage = configPage.testMascotSettings();
        assertTextPresent("Test failed.");
        configPage = testPage.close();

        // Case 3: correct server, wrong user password
        log("Testing wrong password fo Mascot user " + mascotUserAccount + " via " + mascotServerURL);
        configPage.
                setMascotServer(mascotServerURL).
                setMascotUser(mascotUserAccount).
                setMascotPassword("wrongpassword");
        testPage = configPage.testMascotSettings();
        assertTextPresent("Test failed.");
        testPage.close();
    }

    @Test
    public void testDatImport()
    {
        // test import of .dat file
        log("Upload existing Mascot .dat result file.");
        navigateToFolder(FOLDER_NAME);
        clickButton("Process and Import Data");
        _fileBrowserHelper.importFile("bov_sample/" + SEARCH_TYPE + "/test3/", "Import Search Results");

        String mascotDatLabel = SAMPLE_BASE_NAME + ".dat";
        waitForRunningPipelineJobs(MAX_WAIT_SECONDS * 1000);
        waitForElement(Locator.linkWithText(mascotDatLabel));

        log("Spot check results loaded from .dat file");
        clickAndWait(Locator.linkWithText(mascotDatLabel));
        String overviewText = new BodyWebPart(getDriver(), "Run Overview").getComponentElement().getText();
        assertTextPresent(new TextSearcher(overviewText),
                "Trypsin", // N.B. when importing via XML, this becomes lower case ("trypsin")
                "MASCOT",
                "CAexample_mini.dat",
                "sampledata/xarfiles/ms2pipe/bov_sample/mascot/test3",
                "Bovine_mini1.fasta");

        DataRegionTable peptidesTable = new DataRegionTable(REGION_NAME_PEPTIDES, this);

        // really 466 peptides in the .dat import, but only first 100 show in default view
        assertEquals("Wrong number of peptides found", 100, peptidesTable.getDataRowCount());
        List<String> peptideRow = peptidesTable.getRowDataAsText(0);
        List<String> expectedPeptideRow = new ArrayList<>(Arrays.asList(
                "4",                // Scan
                "3+",               // Z
                "15.100",           // Ion
                "29.770",           // Identity
                "24.030",           // Homology
                "8%",               // Ion%
                "-0.8489",          // dMass
                "K.VEHLDKDLFR.R",   // Peptide
                "1",                // SeqHits
                "gi|23335713|hypothetical_prot")); // Protein
        expectedPeptideRow.removeAll(peptideRow);
        assertTrue("Missing values from first peptide row: [" + String.join(",", expectedPeptideRow) + "]", expectedPeptideRow.isEmpty());
        String value = peptidesTable.getDataAsText(0, "Expect");
        assertEquals("Wrong value for 'Expect' in first row", 1.47, Double.parseDouble(value), 0.01);
        value = peptidesTable.getDataAsText(0, "CalcMH+");
        assertEquals("Wrong value for 'CalcMH+' in first row", 1272.43, Double.parseDouble(value), 0.01);
        navigateToFolder(FOLDER_NAME);
        waitForElement(Locator.id("filter-engine"));
        setFormElement(Locator.id("filter-engine"), "MASCOT");
        setFormElement(Locator.id("filter-fasta"), "Bovine_mini1.fasta");
        _extHelper.selectExtGridItem("path", "/MS2VerifyProject/ms2folder", -1, "x-grid-panel", false);
        click(Locator.button("Show Matching MS2 Runs"));
        waitAndClick(Locator.linkWithText("CAexample_mini.dat"));
        waitForText("Peptides");
        assertTextPresent("CAexample_mini.mgf");
        click(Locator.linkWithText("K.GTPAAGDP.-"));

        switchToWindow(1);
        peptidesTable = new DataRegionTable(REGION_NAME_PEPTIDES, this);
        List<String> peptides = peptidesTable.getColumnDataAsText("Peptide");
        assertEquals("peptide grid not filtered on the current fraction/scan/charge", Arrays.asList("K.GTPAAGDP.-", "K.GAQAPLK.G", "K.VVKVLK.A"), peptides);
        getDriver().close();
        switchToMainWindow();
    }


    @Test
    public void testDatImportWithDecoys()
    {
        // test import of .dat file
        log("Upload existing Mascot .dat result file.");
        navigateToFolder(FOLDER_NAME);
        clickButton("Process and Import Data");
        _fileBrowserHelper.importFile("bov_sample/" + SEARCH_TYPE + "/test4/", "Import Search Results");

        String mascotDatLabel = SAMPLE_BASE_NAME + "_decoy.dat";
        waitForRunningPipelineJobs(MAX_WAIT_SECONDS * 1000);
        waitForElement(Locator.linkWithText(mascotDatLabel));

        log("Spot check results loaded from .dat file");
        clickAndWait(Locator.linkWithText(mascotDatLabel));
        String overviewText = new BodyWebPart(getDriver(), "Run Overview").getComponentElement().getText();
        assertTextPresent(new TextSearcher(overviewText),
                "Trypsin", // N.B. when importing via XML, this becomes lower case ("trypsin")
                "MASCOT",
                "CAexample_mini_decoy.dat",
                "sampledata/xarfiles/ms2pipe/bov_sample/mascot/test4",
                "Bovine_mini1.fasta");

        DataRegionTable peptidesTable = new DataRegionTable(REGION_NAME_PEPTIDES, this);
        peptidesTable.ensureColumnsPresent("HitRank", "QueryNumber", "Decoy");
        peptidesTable = new DataRegionTable(REGION_NAME_PEPTIDES, this);

        assertEquals("Wrong number of peptides found", 67, peptidesTable.getDataRowCount());
        List<String> peptideRow = peptidesTable.getRowDataAsText(0);
        List<String> expectedPeptideRow = new ArrayList<>(Arrays.asList(
                "20",             // Scan
                "1+",               // Z
                "23.260",            // Ion
                "26.590",           // Identity
                "25.340",           // Homology
                "29%",              // Ion%
                "-0.0695",          // dMass
                "K.GTPAAGDP.-",     // Peptide
                "1",                // SeqHits
                "gi|5002198|AF143203_1_interle"));     // Protein
        expectedPeptideRow.removeAll(peptideRow);
        assertTrue("Missing values from first peptide row: [" + String.join(",", expectedPeptideRow) + "]", expectedPeptideRow.isEmpty());
        String value = peptidesTable.getDataAsText(0, "Expect");
        assertEquals("Wrong value for 'Expect' in first row", 0.110, Double.parseDouble(value), 0.01);
        value = peptidesTable.getDataAsText(0, "CalcMH+");
        assertEquals("Wrong value for 'CalcMH+' in first row", 685.7027, Double.parseDouble(value), 0.01);
        value = peptidesTable.getDataAsText(0, "QueryNumber");
        assertEquals("Wrong value for 'QueryNumber' in first row", 12, Integer.parseInt(value));
        value = peptidesTable.getDataAsText(0, "HitRank");
        assertEquals("Wrong value for 'HitRank' in first row", 1, Integer.parseInt(value));
        value = peptidesTable.getDataAsText(0, "Decoy");
        assertEquals("Wrong value for 'Decoy' in first row", "false", value);
        peptidesTable.setFilter("Decoy", "Equals", "true");
        assertEquals("Wrong number of decoy peptides", 6, peptidesTable.getDataRowCount());
        peptidesTable.setFilter("QueryNumber", "Equals", "12");
        assertEquals("Should not have a decoy peptide for query 12", 0, peptidesTable.getDataRowCount());
        peptidesTable.clearAllFilters("QueryNumber");
        peptidesTable.clearAllFilters("Decoy");
        peptidesTable.setFilter("Decoy", "Equals", "false");
        assertEquals("Wrong number of non-decoy peptides", 61, peptidesTable.getDataRowCount());
        peptidesTable.setFilter("QueryNumber", "Equals", "2");
        assertEquals("Should not have a non-decoy peptide for query 2", 0, peptidesTable.getDataRowCount());
        peptidesTable.clearAllFilters("QueryNumber");
        peptidesTable.clearAllFilters("Decoy");
        peptidesTable.setFilter("HitRank", "Is Greater Than", "1");
        peptidesTable.setSort("HitRank", SortDirection.DESC);
        assertEquals("Wrong number of peptides with hit rank > 1", 55, peptidesTable.getDataRowCount());
        value = peptidesTable.getDataAsText(0, "HitRank");
        assertEquals("Wrong value for maximum 'HitRank'", "10", value);
        value = peptidesTable.getDataAsText(0, "QueryNumber");
        assertEquals("Wrong value for 'QueryNumber' in first row with max hit rank", "5", value);
        peptidesTable.clearSort("HitRank");
        peptidesTable.clearAllFilters("HitRank");
        _customizeViewsHelper.revertUnsavedViewGridClosed();
        navigateToFolder(FOLDER_NAME);
        waitForElement(Locator.id("filter-engine"));
        setFormElement(Locator.id("filter-engine"), "MASCOT");
        setFormElement(Locator.id("filter-fasta"), "Bovine_mini1.fasta");
        _extHelper.selectExtGridItem("path", "/MS2VerifyProject/ms2folder", -1, "x-grid-panel", false);
        click(Locator.button("Show Matching MS2 Runs"));
        clickAndWait(Locator.linkWithText("CAexample_mini_decoy.dat"));

        waitForText("Decoy Summary");
        //get and check values in Decoy Summary table
        List<WebElement> headers = getDriver().findElements(Locator.xpath("//table[tbody/tr/th/span[text()='Decoy Summary']]//table//td[@class='labkey-form-label']"));
        List<WebElement> values = getDriver().findElements(Locator.xpath("//table[tbody/tr/th/span[text()='Decoy Summary']]//table//td[not(@class='labkey-form-label')]"));
        Map<String,String> decoySummary = new LinkedHashMap<>();
        for(int i = 0; i < headers.size(); i++)
        {
            decoySummary.put(headers.get(i).getText(), values.get(i).getText());
        }
        assertEquals("Incorrect Identity Threshold in Decoy Summary", "13.1", getDriver().findElement(Locator.id("ionThresholdValue")).getText());
        assertEquals("Incorrect In Target count in Decoy Summary", "3", getDriver().findElement(Locator.id("inTargetValue")).getText());
        assertEquals("Incorrect In Decoy count in Decoy Summary", "0", getDriver().findElement(Locator.id("inDecoyValue")).getText());
        assertEquals("Incorrect FDR % in Decoy Summary", "0.00%", getDriver().findElement(Locator.id("fdrValue")).getText());

        // TODO: Would be good to test the functionality of the "Adjust FDR To" dropdown, but we may still have further tweaking on this UI to do pending client feedback.

        DataRegionTable drt = new DataRegionTable(REGION_NAME_PEPTIDES, this);
        drt.setFilter("Peptide", "Equals", "R.KPLAPK.K");
        drt.setSort("Ion", SortDirection.DESC);
        assertTrue("Not enough results to test filtering", drt.getDataRowCount() > 1);

        String greatest = drt.getDataAsText(0, "Ion");

        //Apply filter
        checkCheckbox(Locator.checkboxByName("highestScore"));
        clickAndWait(Locator.id("AddHighestScoreFilterButton"));
        assertEquals("Too many results ", 1, drt.getDataRowCount());

        String filteredIonValue = drt.getDataAsText(0, "Ion");
        assertEquals("Highest ion value not shown", greatest, filteredIonValue);

        checkCheckbox(Locator.checkboxById("isIonCutoff"));
        waitForElement(Locator.tagContainingText("span","Ion >= 13.1"));
        assertEquals("All results should be filtered", 0, drt.getDataRowCount());

        selectOptionByText(Locator.tagWithName("select", "desiredFdr"),"1.0%");
        assertChecked(Locator.checkboxById("isIonCutoff"));
        assertEquals("IonCutoff incorrectly applied", 1, drt.getDataRowCount());
        drt.clearFilter("Peptide");
        assertEquals("Unexpected number of results for IonCutoff filter", 20, drt.getDataRowCount());
        drt.clearFilter("Ion");  //reset filter
        drt.clearSort("Ion");
    }

    protected void setupEngine()
    {
        log("Analyze " + SEARCH_NAME + " sample data.");
        _fileBrowserHelper.selectImportDataAction(SEARCH_BUTTON + " Peptide Search");
    }

    protected void basicChecks()
    {
        navigateToFolder(FOLDER_NAME);
        clickAndWait(Locator.linkWithImage(WebTestHelper.getContextPath() + "/MS2/images/runIcon.gif"));

        // Make sure we're not using a custom default view for the current user
        selectOptionByText(Locator.name("viewParams"), "<Standard View>");
        clickButton("Go");

        log("Test filtering and sorting");
        DataRegionTable peptidesRegion = new DataRegionTable(REGION_NAME_PEPTIDES, this);
        peptidesRegion.setFilter("Mass", "Is Greater Than", "1000");
        assertTextNotPresent(PEPTIDE);
        peptidesRegion.setSort("Scan", SortDirection.DESC);
        assertTextBefore(PEPTIDE2, PEPTIDE3);

        log("Test Save View");
        clickButton("Save View");
        setFormElement(Locator.name("name"), VIEW);
        clickButton("Save View");
        selectOptionByText(Locator.name("viewParams"), "<Standard View>");
        clickButton("Go");
        assertTextPresent(PEPTIDE);
        selectOptionByText(Locator.name("viewParams"), VIEW);
        clickButton("Go");
        assertTextNotPresent(PEPTIDE);
        assertTextBefore(PEPTIDE2, PEPTIDE3);

        log("Test exporting");
        File expFile = new DataRegionExportHelper(new DataRegionTable("query", this))
               .exportText(DataRegionExportHelper.TextSeparator.COMMA);
        TextSearcher tsvSearcher = new TextSearcher(expFile);

        assertTextNotPresent(tsvSearcher, PEPTIDE);
        assertTextPresentInThisOrder(tsvSearcher, PEPTIDE2, PEPTIDE3);
        assertTextPresent(tsvSearcher, PROTEIN);

        log("Test Comparing Peptides");
        navigateToFolder(FOLDER_NAME);
        DataRegionTable MS2SearchRunsTable = new DataRegionTable("MS2SearchRuns", this);
        MS2SearchRunsTable.checkAllOnPage();
        MS2SearchRunsTable.clickHeaderMenu("Compare", "Peptide");
        selectOptionByText(Locator.name("viewParams"), VIEW);
        clickButton("Go");
        assertTextPresent("(Mass > 1000)");

        DataRegionTable compareRegion = new DataRegionTable("MS2Compare", this);
        compareRegion.setSort("Peptide", SortDirection.DESC);
        assertTextBefore(PEPTIDE5, PEPTIDE4);

        navigateToFolder(FOLDER_NAME);

        log("Verify experiment information in MS2 runs.");
        assertElementPresent(Locator.linkWithText(PROTOCOL));

        log("Test Protein Search");
        setFormElement(Locator.name("identifier"), SEARCH);
        click(Locator.name("exactMatch"));
        clickButton("Search");
        assertElementPresent(Locator.linkContainingText(SAMPLE_BASE_NAME + " (test2)"));
        clickAndWait(Locator.id("expandCollapse-ProteinSearchProteinMatches"), 0);
        assertTrue(isTextPresent(SEARCH_FIND) || isTextPresent(SEARCH_FIND_ALT));

        setFormElement(Locator.name("minimumProbability"), "2.0");
        clickButton("Search");
        clickAndWait(Locator.id("expandCollapse-ProteinSearchProteinMatches"), 0);
        assertTrue(isTextPresent(SEARCH_FIND) || isTextPresent(SEARCH_FIND_ALT));
        assertElementNotPresent(Locator.linkWithText(SAMPLE_BASE_NAME + " (test2)"));

        setFormElement(Locator.name("identifier"), "GarbageProteinName");
        setFormElement(Locator.name("minimumProbability"), "");
        clickButton("Search");
        clickAndWait(Locator.id("expandCollapse-ProteinSearchProteinMatches"), 0);
        assertTextNotPresent(SEARCH_FIND, SEARCH_FIND_ALT);
        assertTextPresent("No data to show");
    }

    protected void cleanPipe(String search_type)
    {
        super.cleanPipe(search_type);

        if (PIPELINE_PATH == null)
            return;

        File rootDir = new File(PIPELINE_PATH);
        delete(new File(rootDir, "databases/mascot"));
    }
}
