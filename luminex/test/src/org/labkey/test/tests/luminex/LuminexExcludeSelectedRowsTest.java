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
import org.labkey.test.Locator;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.luminex.dialogs.SelectedRowsExclusionDialog;
import org.labkey.test.components.luminex.dialogs.SelectedRowsExclusionDialog.ExclusionRow;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.TestDataGenerator;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@Category({Daily.class, Assays.class})
public final class LuminexExcludeSelectedRowsTest extends LuminexTest
{
    private static final TestRun SUMMARY_RUN = new TestRun("SelectedRowsRun", TEST_ASSAY_LUM_FILE10, "Standard1");
    private static final TestRun RAW_RUN = new TestRun("SelectedRowsRawRun", TEST_ASSAY_MULTIPLE_STANDARDS_1_XLSX, "Standard2");
    private static final String OTHER_RUN_NAME = "SelectedRowsOtherRun";
    private static final String ANALYTE_2 = "Analyte2";
    private static final String ANALYTE_3 = "Analyte3";
    private static final String ENV6 = "ENV6";
    private static final String ENV7 = "ENV7";
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

        init.importRun(SUMMARY_RUN.name(), SUMMARY_RUN.file());
        init.importRun(RAW_RUN.name(), RAW_RUN.file());
    }

    @Before
    public void preTest()
    {
        cleanupPipelineJobs();
        // Saving leaves the selection in place, so start each test without one
        goToRunResultsWithoutSelection(SUMMARY_RUN.name());
    }

    @Test
    public void testExcludeSelectedRows()
    {
        DataRegionTable results = goToRunResults(RAW_RUN.name());
        selectRows(results, wellAnalytes("S1", "C1", ENV6, ENV7), wellAnalytes("S2", "D2", ENV6));

        SelectedRowsExclusionDialog dialog = SelectedRowsExclusionDialog.beginAt(getDriver(), false);
        assertEquals("Wrong proposed exclusions", List.of(
                row(RAW_RUN, "S1", "C1", "ENV6, ENV7", "Add"),
                row(RAW_RUN, "S2", "D2", "ENV6", "Add")
        ), dialog.getRows());
        assertTextPresent("Analytes excluded for a singlepoint unknown, titration, or at the assay level will not be re-included");
        dialog.setComment("exclude selected rows");
        dialog.saveAndGoToPipeline();
        waitForPipelineJobsToComplete(1, false);

        assertWellExclusions(RAW_RUN, "S1", Map.of("C1", "ENV6, ENV7"));
        assertWellExclusions(RAW_RUN, "S2", Map.of("D2", "ENV6"));

        log("Merge a new analyte into an existing well exclusion");
        results = goToRunResultsWithoutSelection(RAW_RUN.name());
        selectRows(results, wellAnalytes("S2", "D2", ENV6, ENV7));
        dialog = SelectedRowsExclusionDialog.beginAt(getDriver(), false);
        assertEquals("Only the added analyte should be listed", List.of(
                row(RAW_RUN, "S2", "D2", "ENV7", "Update")
        ), dialog.getRows());
        dialog.saveAndGoToPipeline();
        waitForPipelineJobsToComplete(2, false);
        assertWellExclusions(RAW_RUN, "S2", Map.of("D2", "ENV6, ENV7"));
        assertEquals("Saving with a blank comment should keep the updated exclusion's comment", List.of("exclude selected rows"),
                new DataRegionTable("WellExclusion", getDriver()).getColumnDataAsText("Comment"));

        log("Select only rows that are already excluded");
        results = goToRunResultsWithoutSelection(RAW_RUN.name());
        selectRows(results, wellAnalytes("S1", "C1", ENV6));
        assertEquals("The selected rows are already excluded.", SelectedRowsExclusionDialog.getSelectionError(getDriver(), false));
    }

    @Test
    public void testSummaryRowReplicateGroup()
    {
        DataRegionTable results = goToRunResults(SUMMARY_RUN.name());
        selectRows(results, wellAnalytes("S1", "G1", ANALYTE_2), wellAnalytes("S1", "H1", ANALYTE_3),
                wellAnalytes("S3", "G3,H3", ANALYTE_2), wellAnalytes("S3", "G3", ANALYTE_3));

        SelectedRowsExclusionDialog dialog = SelectedRowsExclusionDialog.beginAt(getDriver(), false);
        assertEquals("Rows in a replicate group with summary rows should exclude the whole group, as the well exclusion dialog does", List.of(
                row(SUMMARY_RUN, "S1", "G1,H1", "Analyte2, Analyte3", "Add"),
                row(SUMMARY_RUN, "S3", "G3,H3", "Analyte2, Analyte3", "Add")
        ), dialog.getRows());
        dialog.saveAndGoToPipeline();
        waitForPipelineJobsToComplete(1, false);

        assertWellExclusions(SUMMARY_RUN, "S1", Map.of(REPLICATE_GROUP, "Analyte2, Analyte3"));
        assertWellExclusions(SUMMARY_RUN, "S3", Map.of(REPLICATE_GROUP, "Analyte2, Analyte3"));

        log("Remove a raw row's analyte from its replicate group exclusion");
        results = goToRunResultsWithoutSelection(SUMMARY_RUN.name());
        selectRows(results, wellAnalytes("S1", "H1", ANALYTE_2));
        dialog = SelectedRowsExclusionDialog.beginAt(getDriver(), true);
        assertEquals("Wrong proposed removals", List.of(
                row(SUMMARY_RUN, "S1", "G1,H1", "Analyte2", "Update")
        ), dialog.getRows());
        dialog.saveAndGoToPipeline();
        waitForPipelineJobsToComplete(2, false);
        assertWellExclusions(SUMMARY_RUN, "S1", Map.of(REPLICATE_GROUP, "Analyte3"));
    }

    @Test
    public void testRemoveSelectedExclusions()
    {
        DataRegionTable results = goToRunResults(RAW_RUN.name());
        selectRows(results, wellAnalytes("S4", "C4", ENV6, ENV7), wellAnalytes("S4", "D4", ENV6));
        SelectedRowsExclusionDialog dialog = SelectedRowsExclusionDialog.beginAt(getDriver(), false);
        dialog.saveAndGoToPipeline();
        waitForPipelineJobsToComplete(1, false);
        assertWellExclusions(RAW_RUN, "S4", Map.of("C4", "ENV6, ENV7", "D4", "ENV6"));

        results = goToRunResultsWithoutSelection(RAW_RUN.name());
        selectRows(results, wellAnalytes("S4", "C4", ENV6), wellAnalytes("S4", "D4", ENV6), wellAnalytes("S5", "C5", ENV6)); // S5 isn't excluded

        dialog = SelectedRowsExclusionDialog.beginAt(getDriver(), true);
        assertEquals("Wrong proposed removals", List.of(
                row(RAW_RUN, "S4", "D4", "ENV6", "Remove"),
                row(RAW_RUN, "S4", "C4", "ENV6", "Update")
        ), dialog.getRows());
        assertTextPresent("1 of the selected rows have no well or replicate group exclusion to remove.");
        dialog.saveAndGoToPipeline();
        waitForPipelineJobsToComplete(2, false);
        assertWellExclusions(RAW_RUN, "S4", Map.of("C4", "ENV7"));

        log("Select only rows that have no exclusion");
        results = goToRunResultsWithoutSelection(RAW_RUN.name());
        selectRows(results, wellAnalytes("S5", "C5", ENV6));
        assertEquals("None of the selected rows have a well or replicate group exclusion to remove.",
                SelectedRowsExclusionDialog.getSelectionError(getDriver(), true));
    }

    @Test
    public void testSelectionErrors()
    {
        goToRunResults(SUMMARY_RUN.name());
        assertEquals("No rows are selected.", SelectedRowsExclusionDialog.getSelectionError(getDriver(), false));

        log("Select rows from two runs; the selection is kept across the runs' results grids");
        importRun(OTHER_RUN_NAME, SUMMARY_RUN.file());
        selectRowsInRun(SUMMARY_RUN.name(), wellAnalytes("S6", "G6", ANALYTE_2));
        selectRowsInRun(OTHER_RUN_NAME, wellAnalytes("S6", "G6", ANALYTE_2));
        assertEquals("The selection includes rows from more than one run. Select rows from a single run.",
                SelectedRowsExclusionDialog.getSelectionError(getDriver(), false));
    }

    private void importRun(String runName, File file)
    {
        createNewAssayRun(TEST_ASSAY_LUM, runName);
        addFilesToAssayRun(file);
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

    private DataRegionTable goToRunResultsWithoutSelection(String runName)
    {
        DataRegionTable results = goToRunResults(runName);
        results.doAndWaitForUpdate(() -> results.rowSelector().clickSubMenu(false, "Select None"));
        return results;
    }

    private record TestRun(String name, File file, String description) {}

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

    private static ExclusionRow row(TestRun run, String type, String wells, String analytes, String exclusionType)
    {
        return new ExclusionRow(run.name(), run.file().getName(), run.description(), type, wells, analytes, exclusionType);
    }

    /** Checks the run's well and replicate group exclusions for the type, keyed by well ("Replicate Group" for a group exclusion). */
    private void assertWellExclusions(TestRun run, String type, Map<String, String> expected)
    {
        goToRunResults(run.name());
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
