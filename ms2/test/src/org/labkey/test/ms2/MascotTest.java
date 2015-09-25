/*
 * Copyright (c) 2006-2015 LabKey Corporation
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

package org.labkey.test.ms2;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.InDevelopment;
import org.labkey.test.categories.Mascot;

import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;

import static org.junit.Assert.*;

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
@Category({/*MS2.class, */Mascot.class, InDevelopment.class})
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

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        try {
            deleteViews(VIEW); } catch (Throwable t) {}
        try {deleteRuns(); } catch (Throwable t) {}
        cleanPipe(SEARCH_TYPE);
        deleteProject(getProjectName(), afterTest);
    }

    @Test
    public void testSteps()
    {
        log("Verifying that pipeline files were cleaned up properly");
        File test2 = new File(PIPELINE_PATH + "/bov_sample/" + SEARCH_TYPE + "/test2");
        Assert.assertFalse("Pipeline files were not cleaned up; test2(" + test2.toString() + ") directory still exists", test2.exists());

        //cheehong:
        //  starting with v1.7, "Has Mascot server" checkbox removed
        //  Mascot is considered configured if there is a Mascot URL available
        //  TODO: write code to check on the respond of setting check
        beginAt("/admin/showCustomizeSite.view");
        if (null == getAttribute(Locator.name("mascotServer"), "value") || "".equals(getAttribute(Locator.name("mascotServer"), "value")))
        {
            log("Your mascot settings are not configured.  Skipping mascot test.");
            return;
        }

        String mascotServerURL = getAttribute(Locator.name("mascotServer"), "value");
        String mascotUserAccount = "";
        String mascotUserPassword = "";
        String mascotHTTPProxyURL = "mascotHTTPProxy";

        boolean testAuthentication = !("".equals(mascotUserAccount) && "".equals(mascotUserPassword));

        // Case 1: default setting has to pass as it is configured by the administrator initially
        log("Testing your Mascot settings");
        addUrlParameter("testInPage=true");
        pushLocation();
        clickAndWait(Locator.linkWithText("Test Mascot settings"));
        assertTextPresent("Test passed.");
        log("Return to customize page.");
        popLocation();

        if (testAuthentication)
        {
            // Case 2: correct server, wrong user id
            log("Testing non-existent Mascot user via " + mascotServerURL);
            setFormElement(Locator.name("mascotUserAccount"), "nonexistent");
            setFormElement(Locator.name("mascotUserPassword"), mascotUserPassword);
            pushLocation();
            clickAndWait(Locator.linkWithText("Test Mascot settings"));
            assertTextPresent("Test failed.");
            log("Return to customize page.");
            popLocation();
        }
        else
        {
            log("No authentication information, skip testing non-existent Mascot user via " + mascotServerURL);
        }

        if (testAuthentication)
        {
            // Case 3: correct server, wrong user password
            log("Testing wrong password fo Mascot user " + mascotUserAccount + " via " + mascotServerURL);
            setFormElement(Locator.name("mascotUserAccount"), mascotUserAccount);
            setFormElement(Locator.name("mascotUserPassword"), "wrongpassword");
            pushLocation();
            clickAndWait(Locator.linkWithText("Test Mascot settings"));
            assertTextPresent("Test failed.");
            log("Return to customize page.");
            popLocation();
        }
        else
        {
            log("No authentication information, skip testing wrong password fo Mascot user " + mascotUserAccount + " via " + mascotServerURL);
        }

        String altMascotServer = "";
        try
        {
            URL url = new URL((mascotServerURL.startsWith("http://") ? "" : "http://") + mascotServerURL);
            StringBuffer alternativeLink = new StringBuffer("http://");
            alternativeLink.append(url.getHost());
            if (80 != url.getPort() && -1 != url.getPort())
            {
                alternativeLink.append(":").append(url.getPort());
            }
            alternativeLink.append("/");
            if ("".equals(url.getPath()))
                alternativeLink.append("alternativefolder/");
            altMascotServer = alternativeLink.toString();
        }
        catch (MalformedURLException x)
        {
            //wch: this will not happen as we passed Case#1
        }

        // Case 4: use auto-detection setting
//        log("Testing Mascot settings detection via " + altMascotServer);
//        setFormElement("mascotServer", altMascotServer);
//        setFormElement("mascotUserAccount", mascotUserAccount);
//        setFormElement("mascotUserPassword", mascotUserPassword);
//        pushLocation();
//        clickAndWait(Locator.linkWithText("Test Mascot settings"));
//        assertTextPresent("Test passed.");
//        log("Return to customize page.");
//        popLocation();

        if (testAuthentication)
        {
            // Case 5: auto-detect server, wrong user id
            log("Testing non-existent Mascot user and server auto-detection via " + altMascotServer);
            setFormElement(Locator.name("mascotServer"), altMascotServer);
            setFormElement(Locator.name("mascotUserAccount"), "nonexistent");
            setFormElement(Locator.name("mascotUserPassword"), mascotUserPassword);
            pushLocation();
            clickAndWait(Locator.linkWithText("Test Mascot settings"));
            assertTextPresent("Test failed.");
            log("Return to customize page.");
            popLocation();
        }
        else
        {
            log("No authentication information, skip testing non-existent Mascot user and server auto-detection via " + altMascotServer);
        }

        if (testAuthentication)
        {
            // Case 6: auto-detect server, wrong user password
            log("Testing wrong password fo Mascot user " + mascotUserAccount + "  and server auto-detection via " + mascotServerURL);
            setFormElement(Locator.name("mascotServer"), altMascotServer);
            setFormElement(Locator.name("mascotUserAccount"), mascotUserAccount);
            setFormElement(Locator.name("mascotUserPassword"), "wrongpassword");
            pushLocation();
            clickAndWait(Locator.linkWithText("Test Mascot settings"));
            assertTextPresent("Test failed.");
            log("Return to customize page.");
            popLocation();
        }
        else
        {
            log("No authentication information, skip testing wrong password fo Mascot user " + mascotUserAccount + "  and server auto-detection via " + mascotServerURL);
        }

        // Case 7: wrong server
        altMascotServer = "http://bogus.domain/";
        log("Testing wrong Mascot server via " + altMascotServer);
        setFormElement(Locator.name("mascotServer"), altMascotServer);
        setFormElement(Locator.name("mascotUserAccount"), mascotUserAccount);
        setFormElement(Locator.name("mascotUserPassword"), mascotUserPassword);
        pushLocation();
        clickAndWait(Locator.linkWithText("Test Mascot settings"));
        assertTextPresent("Test failed.", "Failed to interact with Mascot Server");
        log("Return to customize page.");
        popLocation();

        // Do normal MS2 test
        basicMS2Check();

        // test import of .dat file
        log("Upload existing Mascot .dat result file.");
        clickFolder(FOLDER_NAME);
        clickButton("Process and Import Data");
        _fileBrowserHelper.importFile("bov_sample/" + SEARCH_TYPE + "/test3/", "Import Results");

        log("Verify upload started.");
        String mascotDatLabel = SAMPLE_BASE_NAME + ".dat (none)";
        assertElementPresent(Locator.linkWithText(mascotDatLabel));

        pushLocation();

        clickFolder(FOLDER_NAME);

/*
        Commented out because .dat is not loaded as part of an experiment, so it won't appear in the dashboard

        assertElementPresent(Locator.linkWithText(mascotDatLabel));
*/

        assertElementPresent(Locator.linkWithText("MS2 Runs"));
        log("Navigate to MS2 runs.");
        clickAndWait(Locator.linkWithText("MS2 Runs"));

        log("Navigate to Pipeline status.");
        //was: clickTab("Pipeline");
        clickFolder(FOLDER_NAME);
        clickAndWait(Locator.linkWithText("All"));
        int sec = 300;
        while (!isElementPresent(Locator.linkWithText("COMPLETE").index(1)) && sec-- > 0)
        {
            log("Waiting for load to complete");
            sleep(1000);
            refresh();
        }
        if (!isElementPresent(Locator.linkWithText("COMPLETE").index(1)))
            fail("Mascot .dat import did not complete.");

        popLocation();

        log("Spot check results loaded from .dat file");
        clickAndWait(Locator.linkWithText("CAexample_mini.dat (none)"));
        assertTextPresent(
                "sampledata/xarfiles/ms2pipe/databases/Bovine_mini.fasta",
                "MASCOT",
                "CAexample_mini.dat",
                "sampledata/xarfiles/ms2pipe/databases/Bovine_mini.fasta",
                "K.VEHLDKDLFR.R",
                "gi|23335713|hypothetical_prot",
                "1374.3173",
                "R.VWGACVLCLLGPLPIVLGHVHPECDVITQLR.E",
                "gi|4689022|ribosomal_protein_");
    }

    protected void setupEngine()
    {
        log("Analyze " + SEARCH_NAME + " sample data.");
        clickButton(SEARCH_BUTTON + " Peptide Search", defaultWaitForPage);
    }

    protected void basicChecks()
    {
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));
        clickAndWait(Locator.linkWithImage(WebTestHelper.getContextPath() + "/MS2/images/runIcon.gif"));

        // Make sure we're not using a custom default view for the current user
        selectOptionByText(Locator.name("viewParams"), "<Standard View>");
        clickButton("Go");

        log("Test filtering and sorting");
        setFilter("MS2Peptides", "Mass", "Is Greater Than", "1000");
        assertTextNotPresent(PEPTIDE);
        setSort("MS2Peptides", "Scan", SortDirection.DESC);
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
        pushLocation();
        addUrlParameter("exportAsWebPage=true");
        clickButton("Export All", 0);
        clickAndWait(Locator.linkWithText("TSV"));
        assertTextNotPresent(PEPTIDE);
        assertTextBefore(PEPTIDE2, PEPTIDE3);
        assertTextPresent(PROTEIN);
        popLocation();

        log("Test Comparing Peptides");
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));
        click(Locator.name(".toggle"));
        _ext4Helper.clickExt4MenuButton(true, Locator.lkButton("Compare"), false, "Peptide");
        selectOptionByText(Locator.name("viewParams"), VIEW);
        clickButton("Go");
        assertTextPresent("(Mass > 1000)");

        //Put in once bug with filters in postgres is fixed
        //assertTextNotPresent(PEPTIDE);

        setSort("MS2Compare", "Peptide", SortDirection.DESC);
        assertTextBefore(PEPTIDE5, PEPTIDE4);

        log("Navigate to folder Portal");
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));

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
