/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.Path;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.MS2;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.ms2.AbstractMS2SearchEngineTest;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.TextSearcher;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Imports a minimal PEAKS exported pepXML file and verifies its contents. Files adapted from public examples at:
 * <a href="https://groups.google.com/g/spctools-discuss/c/VptRGKWbkvM">...</a>
 */
@Category({MS2.class, Daily.class})
public class PeaksTest extends AbstractMS2SearchEngineTest
{
    protected static final String PEPTIDE = "GDGDSGR";
    protected static final String PROTEIN = "P58107|EPIPL_HUMAN";
    protected static final String SEARCH_TYPE = "peaks";

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        cleanPipe(SEARCH_TYPE);

        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @BeforeClass
    public static void setupProject() throws Exception
    {
        PeaksTest init = getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        createProjectAndFolder();
        navigateToFolder(FOLDER_NAME);
    }

    @Override
    protected void basicChecks()
    {

    }

    @Test
    public void testPeaksImport()
    {
        log("Upload existing PEAKS pepXML result file.");
        clickButton("Process and Import Data");
        _fileBrowserHelper.importFile("peaks/peaks.pep.xml", "Import Search Results");

        String runLabel = "peaks (peaks)";
        waitForRunningPipelineJobs(MAX_WAIT_SECONDS * 1000);
        waitForElement(Locator.linkWithText(runLabel));

        log("Spot check results loaded from pepXML file");
        clickAndWait(Locator.linkWithText(runLabel));
        String overviewText = new BodyWebPart<>(getDriver(), "Run Overview").getComponentElement().getText();
        assertTextPresent(new TextSearcher(overviewText),
                "Trypsin",
                "PEAKS_DB",
                "peaks.pep.xml",
                "peaksMinimal.fasta");

        DataRegionTable peptidesTable = new DataRegionTable(REGION_NAME_PEPTIDES, this);

        assertEquals("Wrong number of peptides found", 5, peptidesTable.getDataRowCount());
        List<String> peptideRow = peptidesTable.getRowDataAsText(0);
        List<String> expectedPeptideRow = new ArrayList<>(Arrays.asList(
                "1950",                 // Scan
                "2+",                   // Z
                "31.218",               // -10lgP
                "0%",                   // Ion%
                "1.0073",               // CalcMH+
                "+1000000.0000",        // dMass
                PEPTIDE,                // Peptide
                "1",                    // SeqHits
                PROTEIN)); // Protein
        expectedPeptideRow.removeAll(peptideRow);
        assertTrue("Missing values from first peptide row: [" + String.join(",", expectedPeptideRow) + "]", expectedPeptideRow.isEmpty());
        String value = peptidesTable.getDataAsText(0, "-10lg P");
        assertEquals("Wrong value for '-10lg P' in first row",  31.218, Double.parseDouble(value), 0.01);
        value = peptidesTable.getDataAsText(0, "CalcMH+");
        assertEquals("Wrong value for 'CalcMH+' in first row", 1.0073, Double.parseDouble(value), 0.01);
    }

    @Override
    protected void setupEngine()
    {
    }

    @Override
    protected void cleanPipe(String search_type)
    {
        super.cleanPipe(search_type);

        File rootDir = new File(PIPELINE_PATH);
        delete(FileUtil.appendPath(rootDir, Path.parse("peaks/peaks.log")));
    }
}
