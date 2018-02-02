/*
 * Copyright (c) 2011-2017 LabKey Corporation
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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.ms2.MS2TestBase;
import org.labkey.test.util.DataRegionExportHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.TextSearcher;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.labkey.test.components.WebPartPanel.WebPart;
import static org.labkey.test.util.DataRegionTable.DataRegion;

@Category({DailyB.class, MS2.class})
public class LibraTest extends MS2TestBase
{
    private static final String standardView = "Standard View";
    protected String proteinProphetView = "Protein Prophet View";
    private static final String iTRAQ_QUANTITATION_RATIO = "Ratio ";

    @Override
    protected String getProjectName()
    {
        return "LibraTest" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    }

    @BeforeClass
    public static void doSetup() throws Exception
    {
        LibraTest init = (LibraTest)getCurrentTest();
        init.configure();
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
    }


    String runName = "itraq/iTRAQ (Libra)";
    int normalizationCount = 8;

    @Test
    public void testSteps()
    {
        goToProjectHome();
        clickAndWait(Locator.linkContainingText(runName));
        waitForText("Grouping");
        selectOptionByText(Locator.id("viewTypeGrouping"), "Standard");
        clickButton("Go");
        DataRegionTable dataRegionTable = DataRegion(getDriver()).find();
        CustomizeView customizeView = dataRegionTable.openCustomizeGrid();
        addNormalizationCount(customizeView);

        customizeView.saveCustomView(standardView);

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
        TextSearcher tsvSearcher = new TextSearcher(expFile);
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
        tsvSearcher = new TextSearcher(tsvFile);
        assertTextPresent(tsvSearcher, "# Peptide filter: (Hyper > 250)", "-.MM'EILRGSPALSAFR.I", "R.TDTGEPM'GR.G");
        assertTextNotPresent(tsvSearcher, "R.AEGTFPGK.I", "R.ILEKSGSPER.I");

        // Validate that it remembers our options
        clickAndWait(Locator.linkWithText("Spectra Count Options"));
        assertRadioButtonSelected(Locator.radioButtonByNameAndValue("spectraConfig", "SpectraCountPeptideCharge"));
        assertEquals("HyperFilter", getFormElement(Locator.id("PeptidesFilter.viewName")));
    }

    private void specificProteinTest()
    {
        click(Locator.linkWithText("gi|2144275|JC5226_ubiquitin_/"));
        switchToWindow(1);
        waitForText("Protein Sequence");

        checkForITRAQNormalization();
        checkForITRAQQuantitation();

        assertTextPresent();
        getDriver().close();
        switchToMainWindow();
        //TODO:  single cell check
    }

    private void groupTest()
    {
        click(Locator.linkWithText("1"));
        switchToWindow(1);
        waitForText("Scan");

        new DataRegionTable("MS2Peptides", getDriver()).goToView(proteinProphetView);
        checkForITRAQNormalization();
        checkForITRAQQuantitation();

        List webparts = WebPart(getDriver()).withTitle("Protein Sequence").findAll();
        assertEquals("Wrong number of Protein Sequenes in group 1", 12, webparts.size());
        assertTextPresent("gi|28189228|similar_to_polyub", "gi|28189228|dbj|BAC56305.1|");
        getDriver().close();
        switchToMainWindow();
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

    private void addNormalizationCount(CustomizeView customizeView)
    {
        for (int i = 1; i <= normalizationCount; i++)
        {
            customizeView.addColumn("iTRAQQuantitation/Normalized" + i, "Normalized " + i);
        }
    }

    private void proteinProphetTest()
    {
        DataRegionTable dataRegionTable = DataRegion(getDriver()).find();
        dataRegionTable.goToView("ProteinProphet");
        CustomizeView customizeView = dataRegionTable.openCustomizeGrid();

        for (int i = 1; i <= normalizationCount; i++)
        {
            customizeView.addColumn("ProteinProphetData/ProteinGroupId/iTRAQQuantitation/Ratio" + i, "Ratio " + i);
        }
        addNormalizationCount(customizeView);
        customizeView.saveCustomView(proteinProphetView);

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
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("ms2");
    }
}
