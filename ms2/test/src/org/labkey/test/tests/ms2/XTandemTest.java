/*
 * Copyright (c) 2007-2017 LabKey Corporation
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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
import org.labkey.test.categories.XTandem;
import org.labkey.test.ms2.AbstractXTandemTest;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.TextSearcher;
import org.labkey.test.utils.ms2.Ms2DataRegionExportHelper;

import java.io.File;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category({DailyB.class, MS2.class, XTandem.class})
public class XTandemTest extends AbstractXTandemTest
{
    protected static final String SEARCH_FASTA1 = "gi|4689022";
    protected static final String SEARCH_FIND_FASTA1 = "SCHIZOSACCHAROMYCES";
    protected static final String SEARCH_FIND_ALT_FASTA1 = "Schizosaccharomyces";
    protected static final String SEARCH_FASTA2 = "gi|1234567890|search_target_low_density_lipop";
    protected static final String SEARCH_FIND_FASTA2 = "search target low density lipoprotein receptor-related protein 8 [Bos taurus]";
    protected static final String SEARCH_FASTA3 = "gi|9876543210";
    protected static final String SEARCH_FIND_FASTA3 = "search target ribosomal protein S16 (BS17)";
    protected static final String PROTOCOL = "X!Tandem analysis";
    protected static final String PEPTIDE_CROSSTAB_RADIO_PROBABILITY_ID = "peptideProphetRadioButton";
    protected static final String PEPTIDE_CROSSTAB_RADIO_PROBABILITY_VALUE = "probability";
    protected static final String PEPTIDE_CROSSTAB__PROBABILITY_TEXTBOX_NAME = "peptideProphetProbability";
    protected static final String PEPTIDE_CROSSTAB_RADIO_NAME = "peptideFilterType";
    protected static final String PEPTIDE_CROSSTAB_RADIO_VALUE_NONE = "none";

    @Test
    public void testSteps()
    {
        log("Verifying that pipeline files were cleaned up properly");
        File test2 = new File(PIPELINE_PATH + "/bov_sample/" + SEARCH_TYPE + "/test2");
        if (test2.exists())
            fail("Pipeline files were not cleaned up; test2("+test2.toString()+") directory still exists");

        basicMS2Check();
    }

    protected void basicChecks()
    {
        goToSchemaBrowser();
        viewQueryData("ms2", "Fractions");
        assertTextPresent("CAexample_mini.mzXML");
        // There should be 200 scans total
        assertTextPresent("200");
        // There should be 100 MS1 scans and 100 MS2 scans
        assertTextPresent("100");

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
        setFormElement(Locator.id("name"), VIEW);
        clickButton("Save View");
        selectOptionByText(Locator.name("viewParams"), "<Standard View>");
        clickButton("Go");
        assertTextPresent(PEPTIDE);
        selectOptionByText(Locator.name("viewParams"), VIEW);
        clickButton("Go");
        assertTextNotPresent("K.VFHFVR.Q");
        assertTextBefore(PEPTIDE2, PEPTIDE3);

        verifyPeptideDetailsPage();

        log("Test exporting");
        File expFile = new Ms2DataRegionExportHelper(peptidesRegion)
                .exportText(Ms2DataRegionExportHelper.FileDownloadType.TSV, false);
        TextSearcher tsvSearcher = new TextSearcher(expFile);
        assertTextNotPresent(tsvSearcher, PEPTIDE);
        assertTextPresentInThisOrder(tsvSearcher, PEPTIDE2, PEPTIDE3);
        assertTextPresent(tsvSearcher, PROTEIN);

        log("Test Comparing Peptides");
        navigateToFolder(FOLDER_NAME);
        DataRegionTable ms2Runs = new DataRegionTable(REGION_NAME_SEARCH_RUNS, this);
        ms2Runs.checkAll();
        ms2Runs.clickHeaderMenu("Compare", "Peptide (Legacy)");
        selectOptionByText(Locator.name("viewParams"), VIEW);
        clickButton("Compare");
        assertTextPresent("(Mass > 1000.0)");

        //Put in once bug with filters in postgres is fixed
        assertTextNotPresent(PEPTIDE);

        DataRegionTable compareRegion = new DataRegionTable("MS2Compare", this);
        compareRegion.setSort("Peptide", SortDirection.DESC);
        assertTextBefore(PEPTIDE5, PEPTIDE4);

        navigateToFolder(FOLDER_NAME);
        verifyPeptideCrosstab();
        verifyComparePeptides();
    }

    private void verifyComparePeptides()
    {
        clickAndWait(Locator.linkWithText("Setup Compare Peptides"));
        checkRadioButton(Locator.radioButtonById(PEPTIDE_CROSSTAB_RADIO_PROBABILITY_ID));
        setFormElement(Locator.name(PEPTIDE_CROSSTAB__PROBABILITY_TEXTBOX_NAME), "0.25");
        clickButton("Compare");
        assertTextPresent(PEPTIDE4);
        assertTextNotPresent(PEPTIDE);

        log("Navigate to folder Portal");
        navigateToFolder(FOLDER_NAME);

        log("Verify experiment information in MS2 runs.");
        assertElementPresent(Locator.linkWithText(PROTOCOL));

        log("Test Protein Search");
        log("Search for a protein in the first fasta file.");
        setFormElement(Locator.name("identifier"), SEARCH_FASTA1);
        uncheckCheckbox(Locator.name("exactMatch"));
        clickButton("Search");
        assertElementPresent(Locator.linkContainingText(SAMPLE_BASE_NAME + " (test2)"));
        clickAndWait(Locator.id("expandCollapse-ProteinSearchProteinMatches"), 0);
        assertTrue(isTextPresent(SEARCH_FIND_FASTA1) || isTextPresent(SEARCH_FIND_ALT_FASTA1));

        setFormElement(Locator.name("minimumProbability"), "2.0");
        clickButton("Search");
        clickAndWait(Locator.id("expandCollapse-ProteinSearchProteinMatches"), 0);
        assertTrue(isTextPresent(SEARCH_FIND_FASTA1) || isTextPresent(SEARCH_FIND_ALT_FASTA1));
        assertElementNotPresent(Locator.linkContainingText(SAMPLE_BASE_NAME + " (test2)"));

        setFormElement(Locator.name("identifier"), "GarbageProteinName");
        setFormElement(Locator.name("minimumProbability"), "");
        clickButton("Search");
        clickAndWait(Locator.id("expandCollapse-ProteinSearchProteinMatches"), 0);
        assertTrue(!(isTextPresent(SEARCH_FIND_FASTA1) || isTextPresent(SEARCH_FIND_ALT_FASTA1)));
        assertTextNotPresent(SEARCH_FIND_FASTA1);
        assertTextPresent("No data to show");

        log("Search for a protein in the second fasta file.");
        navigateToFolder(FOLDER_NAME);
        setFormElement(Locator.name("identifier"), SEARCH_FASTA2);
        checkCheckbox(Locator.name("exactMatch"));
        clickButton("Search");
        clickAndWait(Locator.id("expandCollapse-ProteinSearchProteinMatches"), 0);
        assertTrue(isTextPresent(SEARCH_FIND_FASTA2));

        log("Search for a protein in the third fasta file.");
        navigateToFolder(FOLDER_NAME);
        setFormElement(Locator.name("identifier"), SEARCH_FASTA3);
        uncheckCheckbox(Locator.name("exactMatch"));
        clickButton("Search");
        clickAndWait(Locator.id("expandCollapse-ProteinSearchProteinMatches"), 0);
        assertTrue(isTextPresent(SEARCH_FIND_FASTA3));
    }

    private void verifyPeptideDetailsPage()
    {
        log("Test peptide details page");
        click(Locator.linkWithText(PEPTIDE2));
        Object[] windows = getDriver().getWindowHandles().toArray();
        getDriver().switchTo().window((String)windows[1]);
        waitForText("44.0215"); // Look for b3+ ions, populated bu JavaScript
        assertTextPresent(
                "gi|4689022|ribosomal_protein_",  // Check for protein
                "CAexample_mini.pep.xml - bov_sample/CAexample_mini (test2)", // Check for run name
                "1373.4690", // Check for mass
                "87.0357",
                "130.0499");
        getDriver().close();
        getDriver().switchTo().window((String)windows[0]);
    }

    private void verifyPeptideCrosstab()
    {
        log("Test PeptideCrosstab");
        DataRegionTable dt = new DataRegionTable("MS2SearchRuns", getDriver());
        dt.checkAllOnPage();
        dt.clickHeaderMenu("Compare", "Peptide");

        checkRadioButton(Locator.radioButtonByNameAndValue(PEPTIDE_CROSSTAB_RADIO_NAME, PEPTIDE_CROSSTAB_RADIO_VALUE_NONE));
        clickButton("Compare");
        assertTextPresent(PEPTIDE3, PEPTIDE4, PEPTIDE);
    }
}
