/*
 * Copyright (c) 2011-2016 LabKey Corporation
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

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
import org.labkey.test.ms2.MS2TestBase;
import org.labkey.test.util.DataRegionExportHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.TextSearcher;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

@Category({DailyB.class, MS2.class})
public class LibraTest extends MS2TestBase
{
    private String standardView = "Standard View";
    protected String proteinProphetView = "Protein Prophet View";
    private String iTRAQ_QUANTITATION_RATIO = "Ratio ";

    @Override
    protected String getProjectName()
    {
        return "LibraTest" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    }

    protected void configure()
    {
        _containerHelper.createProject(getProjectName(), "MS2");
        setPipelineRoot(TestFileUtils.getLabKeyRoot() + "/sampledata/xarfiles/ms2pipe/iTRAQ/");
        clickProject(getProjectName());

        clickButton("Process and Import Data");
        _fileBrowserHelper.importFile("xtandem/Libra/iTRAQ.search.xar.xml", "Import Experiment");
        goToModule("Pipeline");
        waitForPipelineJobsToComplete(1, "Experiment Import - iTRAQ.search.xar.xml", false);
        clickProject(getProjectName());
        for (int i = 0; i < 10; i++)
        {
            refresh();
            if (isElementPresent(Locator.linkContainingText(runName)))
            {
                break;
            }
            // Takes a moment for run to appear after import.
            sleep(1000);
        }

        clickAndWait(Locator.linkContainingText(runName));
    }


    String runName = "itraq/iTRAQ (Libra)";
    int normalizationCount = 8;

    @Test
    public void testSteps()
    {
        configure();
        waitForText("Grouping");
        selectOptionByText(Locator.id("viewTypeGrouping"), "Standard");
        clickButton("Go");
        _customizeViewsHelper.openCustomizeViewPanel();
        addNormalizationCount();

        _customizeViewsHelper.saveCustomView(standardView);

        checkForITRAQNormalization();

        proteinProphetTest();
        groupTest();
        specificProteinTest();

        spectraCountTest();
    }

    private void spectraCountTest()
    {
        clickProject(getProjectName());
        DataRegionTable table = new DataRegionTable(REGION_NAME_SEARCH_RUNS, this);
        table.checkAllOnPage();
        table.clickHeaderMenu("Compare", "Spectra Count");
        click(Locator.radioButtonById("SpectraCountPeptide"));
        clickButton("Compare");
        assertTextPresent("-.MM'EILRGSPALSAFR.I");
        assertElementPresent(Locator.linkWithText("itraq/iTRAQ (Libra)"), 27);

        // Try setting a target protein
        clickAndWait(Locator.linkWithText("Spectra Count Options"));
        setFormElement(Locator.name("targetProtein"), "gi|34392343");
        clickButton("Compare");
        assertElementPresent(Locator.linkWithText("itraq/iTRAQ (Libra)"), 1);
        assertTextPresent("R.TDTGEPM'GR.G");
        clickAndWait(Locator.linkContainingText("gi|34392343"));
        assertTextPresent("84,731");
        goBack();

        // Customize view to pull in other columns
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("TrimmedPeptide");
        _customizeViewsHelper.addColumn(new String[] {"Protein", "ProtSequence"});
        _customizeViewsHelper.addColumn(new String[] {"Protein", "BestName"});
        _customizeViewsHelper.addColumn(new String[] {"Protein", "Mass"});
        _customizeViewsHelper.saveDefaultView();
        assertTextPresent("84731", "MPEETQAQDQPMEEEEVETFAFQAEIAQLM");

        // Try a TSV export
        File expFile = new DataRegionExportHelper(new DataRegionTable("SpectraCount", this)).exportText(DataRegionExportHelper.TextSeparator.TAB);
        String tsv = TestFileUtils.getFileContents(expFile);
        TextSearcher tsvSearcher = new TextSearcher(() -> tsv).setSearchTransformer(t -> t);
        assertTextPresent(tsvSearcher, "# Target protein: gi|34392343", "R.TDTGEPM'GR.G", "84731", "MPEETQAQDQPMEEEEVETFAFQAEIAQLM");

        // Try filtering based on a custom view using a different grouping
        clickAndWait(Locator.linkWithText("Spectra Count Options"));
        click(Locator.linkWithText("Create or Edit View"));
        findButton("Save");
        _customizeViewsHelper.addFilter("Hyper", "Hyper", "Is Greater Than", "250");
        assertRadioButtonSelected(Locator.radioButtonByNameAndValue("spectraConfig", "SpectraCountPeptide"));
        _customizeViewsHelper.saveCustomView("HyperFilter");
        click(Locator.radioButtonById("SpectraCountPeptideCharge"));
        selectOptionByText(Locator.id("PeptidesFilter.viewName"), "HyperFilter");
        setFormElement(Locator.name("targetProtein"), "");
        clickButton("Compare");
        assertElementPresent(Locator.linkWithText("itraq/iTRAQ (Libra)"), 12);
        assertTextPresent("-.MM'EILRGSPALSAFR.I", "R.TDTGEPM'GR.G");
        assertTextNotPresent("R.AEGTFPGK.I", "R.ILEKSGSPER.I");

        // Try a TSV export
        File tsvFile = new DataRegionExportHelper(new DataRegionTable("SpectraCount", this)).exportText();
        String tsvContent = TestFileUtils.getFileContents(tsvFile);
        tsvSearcher = new TextSearcher(() -> tsvContent).setSearchTransformer(t -> t);
        assertTextPresent(tsvSearcher, "# Peptide filter: (Hyper > 250)", "-.MM'EILRGSPALSAFR.I", "R.TDTGEPM'GR.G");
        assertTextNotPresent(tsvSearcher, "R.AEGTFPGK.I", "R.ILEKSGSPER.I");

        // Validate that it remembers our options
        clickAndWait(Locator.linkWithText("Spectra Count Options"));
        assertRadioButtonSelected(Locator.radioButtonByNameAndValue("spectraConfig", "SpectraCountPeptideCharge"));
        assertEquals("HyperFilter", getFormElement(Locator.id("PeptidesFilter.viewName")));
    }

    protected void newWindowTest(String linkToClick, String verificationString, String... additionalChecks)
    {
        click(Locator.linkContainingText(linkToClick));
        Object[] windows = getDriver().getWindowHandles().toArray();
        Assert.assertTrue("Didn't find newly opened window", windows.length > 1);
        getDriver().switchTo().window((String)windows[1]);
        waitForText(verificationString);

        checkForITRAQNormalization();
        checkForITRAQQuantitation();

        assertTextPresent(additionalChecks);
        getDriver().close();
        getDriver().switchTo().window((String)windows[0]);
    }

    private void specificProteinTest()
    {
        newWindowTest("gi|2144275|JC5226_ubiquitin_", "Protein Sequence");
        //TODO:  single cell check
    }

    private void groupTest()
    {
        newWindowTest("4", "Scan", "gi|28189228|similar_to_polyub");
    }

    private void checkForITRAQNormalization()
    {
        checkForNormalizationCountofSomething("Normalized ");
    }

    private void checkForITRAQQuantitation()
    {
        checkForNormalizationCountofSomething(iTRAQ_QUANTITATION_RATIO);
    }

    protected void checkForNormalizationCountofSomething(String toCheck)
    {
        for (int i = 1; i <= normalizationCount; i++)
        {
            assertTextPresent(toCheck + i);
        }
    }

    private void addNormalizationCount()
    {
        for (int i = 1; i <= normalizationCount; i++)
        {
            _customizeViewsHelper.addColumn("iTRAQQuantitation/Normalized" + i, "Normalized " + i);
        }
    }

    private void proteinProphetTest()
    {
        DataRegionTable.findDataRegion(this).goToView("ProteinProphet");

        waitForElement(Locator.lkButton("Grid Views"), WAIT_FOR_JAVASCRIPT);
        _customizeViewsHelper.openCustomizeViewPanel();
        for (int i = 1; i <= normalizationCount; i++)
        {
            _customizeViewsHelper.addColumn("ProteinProphetData/ProteinGroupId/iTRAQQuantitation/Ratio" + i, "Ratio " + i);
        }

        addNormalizationCount();

        _customizeViewsHelper.saveCustomView(proteinProphetView);
        checkForITRAQQuantitation();

        DataRegionTable pepTable = new DataRegionTable(REGION_NAME_PEPTIDES, this);
        assertEquals("Wrong ratio for peptide", "0.71", pepTable.getDataAsText(0, "Ratio 1"));

        Locator img = Locator.xpath("//img[contains(@id,'MS2Peptides-Handle')]");
        click(img);
        checkForITRAQNormalization();
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("ms2");
    }
}
