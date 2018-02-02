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

package org.labkey.test.ms2;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Disabled;
import org.labkey.test.util.DataRegionExportHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.TextSearcher;

import java.io.File;

@Category({Disabled.class/*, MS2.class, Sequest.class*/})
public class SequestTest extends AbstractMS2SearchEngineTest
{
    protected static final String PEPTIDE = "K.VFHFVR.Q";
    protected static final String PEPTIDE2 = "K.GRLLIQMLK.Q";
    protected static final String PEPTIDE3 = "K.KLYNEELK.A";
    protected static final String PEPTIDE4 = "K.ARKAPQYSK.R";
    protected static final String PEPTIDE5 = "K.DSPRISIAGR.L";
    protected static final String PROTEIN = "gi|34558016|50S_RIBOSOMAL_PRO";
    protected static final String SEARCH = "gi|15677167|30S_ribosomal_pro";
    protected static final String SEARCH_FIND = "Neisseria meningitidis";
    protected static final String PROTOCOL = "Sequest analysis";
    protected static final String SEARCH_TYPE = "sequest";
    protected static final String SEARCH_BUTTON = "Sequest";
    protected static final String SEARCH_NAME = "SEQUEST";

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        cleanPipe(SEARCH_TYPE);
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @Test
    public void testSteps()
    {

        beginAt("/admin/showCustomizeSite.view");
        if (null == getAttribute(Locator.name("sequestServer"), "value") || "".equals(getAttribute(Locator.name("sequestServer"), "value")))
        {
            log("Your sequest settings are not configured.  Skipping sequest test.");
            return;
        }

        log("Testing your Sequest settings");
        addUrlParameter("testInPage=true");
        pushLocation();
        clickAndWait(Locator.linkWithText("Test Sequest settings"));
        assertTextPresent("test passed.");
        popLocation();

        String altSequestServer = "bogus.domain";
        log("Testing wrong Sequest server via " + altSequestServer);
        setFormElement(Locator.name("sequestServer"), altSequestServer);
        pushLocation();
        clickAndWait(Locator.linkWithText("Test Sequest settings"));
        assertTextPresent("Test failed.", "Failed to interact with SequestQueue application");
        log("Return to customize page.");
        popLocation();

        log("Verifying that pipeline files were cleaned up properly");
        File test2 = new File(PIPELINE_PATH + "/bov_sample/" + SEARCH_TYPE + "/test2");
        Assert.assertFalse("Pipeline files were not cleaned up; test2(" + test2.toString() + ") directory still exists", test2.exists());

        basicMS2Check();
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
        DataRegionTable list = new DataRegionTable("query", this);
        File expFile = new DataRegionExportHelper(list).exportText(DataRegionExportHelper.TextSeparator.TAB);
        TextSearcher tsvSearcher = new TextSearcher(expFile);
        assertTextPresent(tsvSearcher, PEPTIDE);
        assertTextPresentInThisOrder(tsvSearcher, PEPTIDE2, PEPTIDE3);
        assertTextPresent(tsvSearcher, PROTEIN);

        log("Test Comparing Peptides");
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));
        click(Locator.name(".toggle"));
        _ext4Helper.clickExt4MenuButton(true, Locator.lkButton("Compare"), false, "Peptide");
        selectOptionByText(Locator.name("viewParams"), VIEW);
        clickButton("Compare");
        assertTextPresent("(Mass > 1000)");

        DataRegionTable compareRegion = new DataRegionTable("MS2Compare", this);
        compareRegion.setSort("Peptide", SortDirection.DESC);
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
        assertElementPresent(Locator.linkContainingText(SAMPLE_BASE_NAME + " (test2)"));
        assertTextPresent(SEARCH_FIND);

        setFormElement(Locator.name("minimumProbability"), "2.0");
        clickButton("Search");
        clickAndWait(Locator.id("expandCollapse-ProteinSearchProteinMatches"), 0);
        assertTextPresent(SEARCH_FIND);
        assertElementNotPresent(Locator.linkWithText(SAMPLE_BASE_NAME + " (test2)"));

        setFormElement(Locator.name("identifier"), "GarbageProteinName");
        setFormElement(Locator.name("minimumProbability"), "");
        clickButton("Search");
        clickAndWait(Locator.id("expandCollapse-ProteinSearchProteinMatches"), 0);
        assertTextNotPresent(SEARCH_FIND);
        assertTextPresent("No data to show");
    }
}
