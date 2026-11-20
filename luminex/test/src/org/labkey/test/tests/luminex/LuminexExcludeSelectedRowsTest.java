/*
 * Copyright (c) 2026 LabKey Corporation
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
package org.labkey.test.tests.luminex;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.luminex.dialogs.SelectedRowsExclusionDialog;
import org.labkey.test.components.luminex.dialogs.SelectedRowsExclusionDialog.ExclusionRow;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.TestDataGenerator;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@Category({Daily.class, Assays.class})
public final class LuminexExcludeSelectedRowsTest extends LuminexTest
{
    private static final String RUN_NAME = "SelectedRowsRun";
    private static final String OTHER_RUN_NAME = "SelectedRowsOtherRun";
    private static final String DATA_FILE = TEST_ASSAY_LUM_FILE10.getName();
    private static final String DESCRIPTION = "Standard1";
    private static final String ANALYTE_2 = "Analyte2";
    private static final String ANALYTE_3 = "Analyte3";
    private static final String REPLICATE_GROUP = "Replicate Group";

    @BeforeClass
    public static void importRun()
    {
        LuminexExcludeSelectedRowsTest init = getCurrentTest();

        // Each exclusion save reruns the transform, so fuzz whether there is one to rerun
        if (TestDataGenerator.randomBoolean("whether to add transform script in assay design"))
        {
            init.goToTestAssayHome();
            ReactAssayDesignerPage assayDesigner = init._assayHelper.clickEditAssayDesign();
            assayDesigner.addTransformScript(RTRANSFORM_SCRIPT_FILE_LABKEY);
            assayDesigner.clickFinish();
        }

        init.importRawAndSummaryRun(RUN_NAME);
    }

    @Before
    public void preTest()
    {
        cleanupPipelineJobs();
        // Saving leaves the selection in place, so start each test without one
        goToRunResultsWithoutSelection();
    }

    @Test
    public void testExcludeSelectedRows()
    {
        DataRegionTable results = goToRunResults(RUN_NAME);
        selectRows(results, wellAnalytes("S1", "G1", ANALYTE_2, ANALYTE_3), wellAnalytes("S2", "H2", ANALYTE_2));

        SelectedRowsExclusionDialog dialog = SelectedRowsExclusionDialog.beginAt(getDriver(), false);
        assertEquals("Wrong proposed exclusions", List.of(
                row("S1", "G1", "Analyte2, Analyte3", "Insert"),
                row("S2", "H2", "Analyte2", "Insert")
        ), dialog.getRows());
        assertTextPresent("Analytes excluded for a singlepoint unknown, titration, or at the assay level will not be re-included");
        dialog.setComment("exclude selected rows");
        dialog.saveAndGoToPipeline();
        waitForPipelineJobsToComplete(1, false);

        assertWellExclusions("S1", Map.of("G1", "Analyte2, Analyte3"));
        assertWellExclusions("S2", Map.of("H2", "Analyte2"));

        log("Merge a new analyte into an existing well exclusion");
        results = goToRunResultsWithoutSelection();
        selectRows(results, wellAnalytes("S2", "H2", ANALYTE_2, ANALYTE_3));
        dialog = SelectedRowsExclusionDialog.beginAt(getDriver(), false);
        assertEquals("Only the added analyte should be listed", List.of(
                row("S2", "H2", "Analyte3", "Update")
        ), dialog.getRows());
        dialog.saveAndGoToPipeline();
        waitForPipelineJobsToComplete(2, false);
        assertWellExclusions("S2", Map.of("H2", "Analyte2, Analyte3"));

        log("Select only rows that are already excluded");
        results = goToRunResultsWithoutSelection();
        selectRows(results, wellAnalytes("S1", "G1", ANALYTE_2));
        assertEquals("The selected rows are already excluded.", SelectedRowsExclusionDialog.getSelectionError(getDriver(), false));
    }

    @Test
    public void testSummaryRowReplicateGroup()
    {
        DataRegionTable results = goToRunResults(RUN_NAME);
        selectRows(results, wellAnalytes("S3", "G3,H3", ANALYTE_2), wellAnalytes("S3", "G3", ANALYTE_2, ANALYTE_3));

        SelectedRowsExclusionDialog dialog = SelectedRowsExclusionDialog.beginAt(getDriver(), false);
        assertEquals("The summary row should exclude its replicate group, and only raw analytes it doesn't cover get a well exclusion", List.of(
                row("S3", REPLICATE_GROUP, "Analyte2", "Insert"),
                row("S3", "G3", "Analyte3", "Insert")
        ), dialog.getRows());
        dialog.saveAndGoToPipeline();
        waitForPipelineJobsToComplete(1, false);

        assertWellExclusions("S3", Map.of(REPLICATE_GROUP, "Analyte2", "G3", "Analyte3"));
    }

    @Test
    public void testRemoveSelectedExclusions()
    {
        DataRegionTable results = goToRunResults(RUN_NAME);
        selectRows(results, wellAnalytes("S4", "G4", ANALYTE_2, ANALYTE_3), wellAnalytes("S4", "H4", ANALYTE_2));
        SelectedRowsExclusionDialog dialog = SelectedRowsExclusionDialog.beginAt(getDriver(), false);
        dialog.saveAndGoToPipeline();
        waitForPipelineJobsToComplete(1, false);
        assertWellExclusions("S4", Map.of("G4", "Analyte2, Analyte3", "H4", "Analyte2"));

        results = goToRunResultsWithoutSelection();
        selectRows(results, wellAnalytes("S4", "G4", ANALYTE_2), wellAnalytes("S4", "H4", ANALYTE_2), wellAnalytes("S5", "G5", ANALYTE_2)); // S5 isn't excluded

        dialog = SelectedRowsExclusionDialog.beginAt(getDriver(), true);
        assertEquals("Wrong proposed removals", List.of(
                row("S4", "H4", "Analyte2", "Remove"),
                row("S4", "G4", "Analyte2", "Update")
        ), dialog.getRows());
        assertTextPresent("1 of the selected rows have no well or replicate group exclusion to remove.");
        dialog.saveAndGoToPipeline();
        waitForPipelineJobsToComplete(2, false);
        assertWellExclusions("S4", Map.of("G4", "Analyte3"));

        log("Select only rows that have no exclusion");
        results = goToRunResultsWithoutSelection();
        selectRows(results, wellAnalytes("S5", "G5", ANALYTE_2));
        assertEquals("None of the selected rows have a well or replicate group exclusion to remove.",
                SelectedRowsExclusionDialog.getSelectionError(getDriver(), true));
    }

    @Test
    public void testSelectionErrors()
    {
        goToRunResults(RUN_NAME);
        assertEquals("No rows are selected.", SelectedRowsExclusionDialog.getSelectionError(getDriver(), false));

        log("Select rows from two runs; the selection is kept across the runs' results grids");
        importRawAndSummaryRun(OTHER_RUN_NAME);
        selectRowsInRun(RUN_NAME, wellAnalytes("S6", "G6", ANALYTE_2));
        selectRowsInRun(OTHER_RUN_NAME, wellAnalytes("S6", "G6", ANALYTE_2));
        assertEquals("The selection includes rows from more than one run. Select rows from a single run.",
                SelectedRowsExclusionDialog.getSelectionError(getDriver(), false));
    }

    private void importRawAndSummaryRun(String runName)
    {
        createNewAssayRun(TEST_ASSAY_LUM, runName);
        addFilesToAssayRun(TEST_ASSAY_LUM_FILE10);
        clickButton("Next", longWaitForPage);
        clickButton("Save and Finish", longWaitForPage);
    }

    private DataRegionTable goToRunResults(String runName)
    {
        goToTestAssayHome();
        clickAndWait(Locator.linkWithText(runName));
        return new DataRegionTable(DATA_TABLE_NAME, getDriver());
    }

    /** Opens the run's results with the Type filter on the URL, since setting one in the grid clears the selection, and checks the rows. */
    private void selectRowsInRun(String runName, WellAnalytes selection)
    {
        goToRunResults(runName);
        beginAt(getCurrentRelativeURL() + "&" + DATA_TABLE_NAME + ".Type~eq=" + selection.type());
        checkRows(new DataRegionTable(DATA_TABLE_NAME, getDriver()), selection);
    }

    private DataRegionTable goToRunResultsWithoutSelection()
    {
        DataRegionTable results = goToRunResults(RUN_NAME);
        results.doAndWaitForUpdate(() -> results.rowSelector().clickSubMenu(false, "Select None"));
        return results;
    }

    private record WellAnalytes(String type, String well, List<String> analytes) {}

    private static WellAnalytes wellAnalytes(String type, String well, String... analytes)
    {
        return new WellAnalytes(type, well, List.of(analytes));
    }

    /** Filters the results grid to the wells' types and checks the rows for their analytes. */
    private void selectRows(DataRegionTable results, WellAnalytes... selections)
    {
        // Changing a filter clears the grid's selection, so filter once for all the wells
        String types = Arrays.stream(selections).map(WellAnalytes::type).distinct().collect(Collectors.joining(";"));
        results.setFilter("Type", "Equals One Of", types);
        checkRows(results, selections);
    }

    /** Checks the rows for each well's analytes on the grid's current page, adding to the selection. */
    private void checkRows(DataRegionTable results, WellAnalytes... selections)
    {
        List<String> rowTypes = results.getColumnDataAsText("Type");
        List<String> wells = results.getColumnDataAsText("Well");
        List<String> rowAnalytes = results.getColumnDataAsText("Analyte");
        for (int i = 0; i < wells.size(); i++)
        {
            int row = i;
            if (Arrays.stream(selections).anyMatch(s -> s.type().equals(rowTypes.get(row)) && s.well().equals(wells.get(row)) && s.analytes().contains(rowAnalytes.get(row))))
                results.checkCheckbox(i);
        }
    }

    private static ExclusionRow row(String type, String wells, String analytes, String action)
    {
        return new ExclusionRow(RUN_NAME, DATA_FILE, DESCRIPTION, type, wells, analytes, action);
    }

    /** Checks the run's well and replicate group exclusions for the type, keyed by well ("Replicate Group" for a group exclusion). */
    private void assertWellExclusions(String type, Map<String, String> expected)
    {
        goToRunResults(RUN_NAME);
        clickAndWait(Locator.linkWithText("view excluded data"));
        DataRegionTable exclusions = new DataRegionTable("WellExclusion", getDriver());
        exclusions.setFilter("Type", "Equals", type);

        List<String> wells = exclusions.getColumnDataAsText("Well");
        List<String> analytes = exclusions.getColumnDataAsText("Analytes");
        Map<String, String> actual = new TreeMap<>();
        for (int i = 0; i < wells.size(); i++)
        {
            String well = wells.get(i).isBlank() ? REPLICATE_GROUP : wells.get(i).trim();
            actual.put(well, Arrays.stream(analytes.get(i).split(",")).map(String::trim).sorted().collect(Collectors.joining(", ")));
        }
        assertEquals("Wrong exclusions for type " + type, new TreeMap<>(expected), actual);
    }
}
