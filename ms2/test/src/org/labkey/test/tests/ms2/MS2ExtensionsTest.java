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
package org.labkey.test.tests.ms2;

import org.junit.experimental.categories.Category;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.Path;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.MS2;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({MS2.class, Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 6)
public class MS2ExtensionsTest extends AbstractMS2ImportTest
{
    protected static final String MS2EXTENSIONS_WEBPART = "MS2 Runs With Peptide Counts";
    protected static final String MS2EXTENSIONS_MODULE = "MS2Extensions";
    protected static final String SEARCH_STRING = "gi|11";
    protected static final String MS2EXTENSIONS_DATAREGION_NAME = "MS2ExtensionsRunGrid";
    protected static final String MULTI_PROTEIN_MSG = "Multiple proteins match your search. Please choose the applicable proteins below";
    protected static final String NO_PROTEIN_MSG = "No proteins match. Please try another name.";
    protected static Locator.XPathLocator matchCriteriaComo = Locator.tagWithName("select", "targetProteinMatchCriteria");
    protected static Locator.XPathLocator targetProteinInput = Locator.input("targetProtein");
    protected static Locator.XPathLocator matchedProteinCheckbox = Locator.input("targetSeqIds");

    @Override
    @LogMethod
    protected void setupMS2()
    {
        cleanPipeline();
        super.setupMS2();
        importMS2Run("DRT2", 2);
    }

    @Override
    protected void verifyMS2()
    {
        verifyMultiProteinsSearch();
        verifyChargeFilterLookup();
    }

    // Issue 52659: lookups from module query XML must resolve in the run's peptide grid and on the protein page
    private void verifyChargeFilterLookup()
    {
        String chargeTwoFilter = "MS2Peptides.XTandemChgFilt%2FHyperChgFilt2~lt=999999";

        navigateToFolder(FOLDER_NAME);
        clickAndWait(Locator.linkWithText("drt/CAexample_mini (DRT2)"));
        selectOptionByText(Locator.name("viewParams"), "<Standard View>");
        clickButton("Go");
        beginAt(getCurrentRelativeURL() + "&" + chargeTwoFilter);

        assertTextNotPresent("Ignoring filter");
        DataRegionTable peptides = new DataRegionTable(REGION_NAME_PEPTIDES, this);
        assertEquals("Peptide rows filtered on a module-defined lookup", 21, peptides.getDataRowCount());
        assertEquals("Charges after filtering on HyperChgFilt2", Set.of("2+"), new HashSet<>(peptides.getColumnDataAsText("Charge")));

        beginAt(getAttribute(Locator.linkWithText("gi|15645924|ribosomal_protein"), "href") + "&allPeps=true");
        assertTextPresent("1 total, 1 distinct", "R.VKLKAMQLSNPNEIKKAR.N");
        assertTextNotPresent("K.YTELK.D");
    }

    private void verifyMultiProteinsSearch()
    {
        _containerHelper.enableModule(MS2EXTENSIONS_MODULE);
        PortalHelper _portalHelper = new PortalHelper(this);
        _portalHelper.addWebPart(MS2EXTENSIONS_WEBPART);

        log("Verify protein search with match criteria of Prefix");
        assertEquals("Default match criteria should be Prefix", "Prefix", getSelectedOptionText(matchCriteriaComo.waitForElement(shortWait())));

        compareWithCriteria("Prefix");

        waitForText(MULTI_PROTEIN_MSG);
        int proteinCounts = matchedProteinCheckbox.findElements(getDriver()).size();
        assertTrue("No matched proteins found for \"" + SEARCH_STRING + "\" with match criteria \"Prefix\"", proteinCounts > 1);
        clickButton("Continue");

        waitForText("Compare Peptides: Protein gi|11");
        DataRegionTable compDetailTable = new DataRegionTable("query", getDriver());
        assertTrue("Comparison Details shouldn't be empty for " + SEARCH_STRING + "\" with match criteria \"Prefix\"", compDetailTable.getDataRowCount() > 0);

        log("Verify protein search with match criteria of Substring");
        compareWithCriteria("Substring");
        int proteinCountsSubString = matchedProteinCheckbox.findElements(getDriver()).size();
        assertEquals("Wrong number of proteins found for \"" + SEARCH_STRING + "\" with match criteria \"Substring\"", proteinCounts, proteinCountsSubString);

        log("Verify protein search with match criteria of Suffix");
        compareWithCriteria("Suffix");
        waitForText(NO_PROTEIN_MSG);
        assertElementNotPresent("No proteins should be found for \"" + SEARCH_STRING + "\" with match criteria \"Suffix\"", matchedProteinCheckbox);
    }

    private void compareWithCriteria(String matchCriteria)
    {
        navigateToFolder(FOLDER_NAME);

        targetProteinInput.waitForElement(getDriver(), 1000);
        setFormElement(targetProteinInput, SEARCH_STRING);
        selectOptionByText(matchCriteriaComo, matchCriteria);
        DataRegionTable runsTable = new DataRegionTable(MS2EXTENSIONS_DATAREGION_NAME, getDriver());
        runsTable.checkAll();
        runsTable.clickHeaderButtonAndWait("Compare Peptides");
    }

    private void cleanPipeline()
    {
        File rootDir = new File(PIPELINE_PATH);
        delete(FileUtil.appendPath(rootDir, Path.parse(".labkey/protocols/rollup/Protocol Rollup 1.xml")));
    }
}
