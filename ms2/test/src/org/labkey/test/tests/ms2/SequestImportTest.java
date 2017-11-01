/*
 * Copyright (c) 2013-2017 LabKey Corporation
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
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.ModulePropertyValue;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.FileBrowser;
import org.labkey.test.categories.MS2;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.FileBrowserHelper;
import org.labkey.test.util.LogMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Category({DailyB.class, MS2.class, FileBrowser.class})
public class SequestImportTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "SequestImport" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;

    private static final String[] TOTAL_PEPTIDES_FIELD_KEY = {"PeptideCounts", "TotalPeptides"};
    private static final String[] UNIQUE_PEPTIDES_FIELD_KEY = {"PeptideCounts", "DistinctPeptides"};

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Test
    public void testSteps()
    {
        setupProject();
        importSequestRun();
        verifyRunGrid();
        testFileUsageColumn();
    }

    @LogMethod
    private void verifyRunGrid()
    {
        clickFolder(PROJECT_NAME);

        // Customize the view to show the distinct and total peptide counts based on the criteria established
        // by the custom query
        CustomizeView viewsHelper = new DataRegionTable("MS2ExtensionsRunGrid", this).getCustomizeView();
        viewsHelper.openCustomizeViewPanel();
        viewsHelper.addColumn(TOTAL_PEPTIDES_FIELD_KEY);
        viewsHelper.addColumn(UNIQUE_PEPTIDES_FIELD_KEY);

        // Add a filter so that we can check the values were calculated and shown correctly
        viewsHelper.addFilter(TOTAL_PEPTIDES_FIELD_KEY, "Total Peptides", "Equals", "2");
        viewsHelper.addFilter(UNIQUE_PEPTIDES_FIELD_KEY, "Distinct Peptides", "Equals", "1");
        viewsHelper.saveDefaultView();

        // Make sure that our run is still showing
        assertTextPresent("raftflow10", "ipi.HUMAN.fasta.v2.31");

        // Do a peptide comparison, making sure that the target protein filter works
        setFormElement(Locator.name("targetProtein"), "IPI00176617");
        click(Locator.checkboxByTitle("Select/unselect all on current page"));
        clickButton("Compare Peptides");
        assertTextPresent("S.GDPEEEEEEEEELVDPLTTVR.E", "raftflow10");
        // Make sure that other peptides have been filtered our correctly based on the target protein
        assertTextNotPresent("A.ADSNPAP.S", "A.VPSGQDNIHR.F");

        // Make sure that our target protein is remembered across page views
        clickFolder(PROJECT_NAME);
        assertTextPresent("IPI00176617");
    }

    @LogMethod
    private void testFileUsageColumn()
    {
        final String[] runFiles = {"ipi.HUMAN.fasta.v2.31", "raftflow10.mzXML", "raftflow10.pep.xml"};
        final String runName = "sampledata/raftflow10 (raftflow)";
        final String runProtocol = "MS2 Import";
        final Locator.XPathLocator runLink = Locator.linkWithText(runName + " (" + runProtocol + ")");

        goToModule("Pipeline");
        clickButton("Process and Import Data");
        for (String file : runFiles)
        {
            waitForElement(FileBrowserHelper.Locators.gridRowWithNodeId(file).append(runLink));
        }
        clickAndWait(runLink.parent().parent()); /* Clicking the anchor doesn't work, but this does, shrug */
        assertElementPresent(Locator.pageHeader(runName));
        clickAndWait(Locator.linkWithText(runProtocol));

        assertElementPresent(Locator.pageHeader("Protocol: " + runProtocol));
        assertElementPresent(Locator.linkWithText(runName));
    }

    @LogMethod
    private void importSequestRun()
    {
        clickFolder(PROJECT_NAME);  // go to MS2 Dashboard
        // Import a Sequest run
        clickButton("Process and Import Data");
        _fileBrowserHelper.importFile("raftflow10.pep.xml", "Import Search Results");

        clickFolder(PROJECT_NAME);
        waitAndClick(Locator.linkWithText("Data Pipeline"));
        waitForPipelineJobsToComplete(1, "Experiment Import", false);
    }

    @LogMethod
    private void setupProject()
    {
        _containerHelper.createProject(PROJECT_NAME, "MS2 Extensions");
        setPipelineRoot(TestFileUtils.getLabKeyRoot() + "/sampledata/raftflow");
        _containerHelper.enableModule(PROJECT_NAME, "MS2Extensions");
        List<ModulePropertyValue> properties = new ArrayList<>();
        // Clear out any custom queries that might have been set by the user
        properties.add(new ModulePropertyValue("MS2Extensions", "/", "peptideCountQuery", ""));
        properties.add(new ModulePropertyValue("MS2Extensions", "/", "peptideCountSchema", ""));

        // We're being impolite by changing the site-wide default without resetting the previous value, but we
        // can live with that for now
        setModuleProperties(properties);
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("ms2");
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
