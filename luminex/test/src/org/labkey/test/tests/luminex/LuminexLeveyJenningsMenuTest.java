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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.luminex.LeveyJenningsMenuPage;
import org.labkey.test.util.LogMethod;

import java.io.File;
import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category({Daily.class, Assays.class})
public final class LuminexLeveyJenningsMenuTest extends LuminexTest
{
    private static final String ISOTYPE_A = isotype;
    private static final String ISOTYPE_B = "IgA";
    private static final String CONJUGATE_A = conjugate;
    private static final String CONJUGATE_B = "Biotin";

    private static final String STANDARD1 = "Standard1";
    private static final String STANDARD2 = "Standard2";
    private static final String SINGLE_POINT_CONTROL = "IH5672";

    private static final List<String> ALL_ANTIGENS = List.of("Blank", "ENV1", "ENV2", "ENV3", "ENV4", "ENV5", "ENV6", "ENV7", "GS Analyte A", "GS Analyte B");

    private static final Locator ISOTYPE_COMBO = Locator.id("isotype-combo-box");
    private static final Locator CONJUGATE_COMBO = Locator.id("conjugate-combo-box");

    @BeforeClass
    public static void importRuns()
    {
        ((LuminexLeveyJenningsMenuTest) getCurrentTest()).doImportRuns();
    }

    private void doImportRuns()
    {
        // Standard1 and single point control IH5672 with Blank and ENV1-ENV5
        importRun(1, TestFileUtils.getSampleData("luminex/01-11A12-IgA-Biotin.xls"), ISOTYPE_A, CONJUGATE_A, true);
        // same titration and antigens as run 1 with a second isotype
        importRun(2, TestFileUtils.getSampleData("luminex/02-14A22-IgA-Biotin.xls"), ISOTYPE_B, CONJUGATE_A, true);
        // Standard2 (plus Sample 1, which is neither a standard nor a QC control) with Blank and ENV4-ENV7
        importRun(3, TEST_ASSAY_MULTIPLE_STANDARDS_1, ISOTYPE_A, CONJUGATE_B, false);
        // Standard1 (plus Standard1-CrossplateTitration, which is neither) with GS Analyte A and B
        importRun(4, TEST_ASSAY_LUM_FILE5, ISOTYPE_A, CONJUGATE_A, false);
    }

    @Test
    public void testAntigenList()
    {
        LeveyJenningsMenuPage menuPage = goToMenuPage();

        assertEquals("Wrong antigens", ALL_ANTIGENS, menuPage.getAntigens());
        assertEquals("Antigens should wrap into a second column after 8", 2, menuPage.getAntigenColumnCount());
        assertEquals("Wrong titrations", List.of(STANDARD1, STANDARD2), menuPage.getTitrations());
        assertEquals("Wrong single point controls", List.of(SINGLE_POINT_CONTROL), menuPage.getSinglePointControls());
        assertFalse("Report links should not include an antigen", menuPage.getTitrationHref(STANDARD1).contains("analyte="));
    }

    @Test
    public void testAntigenFilter()
    {
        LeveyJenningsMenuPage menuPage = goToMenuPage().selectAntigen("ENV1");
        assertEquals("Wrong selection", "Selected: ENV1", menuPage.getAntigensText());
        assertEquals("Wrong titrations for ENV1", List.of(STANDARD1), menuPage.getTitrations());
        assertEquals("Wrong single point controls for ENV1", List.of(SINGLE_POINT_CONTROL), menuPage.getSinglePointControls());
        assertTrue("Report link should include the antigen", menuPage.getTitrationHref(STANDARD1).contains("analyte=ENV1"));

        menuPage = menuPage.clearAntigen();
        assertEquals("Clearing the selection should restore the antigen list", ALL_ANTIGENS, menuPage.getAntigens());

        menuPage = menuPage.selectAntigen("ENV6");
        assertEquals("Wrong titrations for ENV6", List.of(STANDARD2), menuPage.getTitrations());
        assertEquals("Wrong single point controls for ENV6", "No single point controls", menuPage.getSinglePointControlsText());

        menuPage = menuPage.clearAntigen().selectAntigen("GS Analyte A");
        assertEquals("Wrong titrations for GS Analyte A", List.of(STANDARD1), menuPage.getTitrations());
        assertEquals("Wrong single point controls for GS Analyte A", "No single point controls", menuPage.getSinglePointControlsText());

        menuPage = LeveyJenningsMenuPage.beginAt(this, getProjectName(), getUrlParam("rowId"), "NotAnAntigen");
        assertEquals("Wrong selection", "Selected: NotAnAntigen", menuPage.getAntigensText());
        assertEquals("Wrong titrations for unknown antigen", "No titrations", menuPage.getTitrationsText());
        assertEquals("Wrong single point controls for unknown antigen", "No single point controls", menuPage.getSinglePointControlsText());
    }

    @Test
    public void testReportPreSelectsSingleOptions()
    {
        // ENV6 with Standard2 is only in run 3, so the isotype and conjugate are pre-selected
        goToMenuPage().selectAntigen("ENV6").clickTitration(STANDARD2);
        waitForAnalyteSelected("ENV6");
        waitForFormElementToEqual(ISOTYPE_COMBO, ISOTYPE_A);
        waitForFormElementToEqual(CONJUGATE_COMBO, CONJUGATE_B);
        assertElementPresent(Locator.extButtonEnabled("Apply"));

        // ENV1 with Standard1 has two isotypes (runs 1 and 2), so nothing is pre-selected until the user picks one
        goToMenuPage().selectAntigen("ENV1").clickTitration(STANDARD1);
        waitForAnalyteSelected("ENV1");
        assertEquals("Isotype should not be pre-selected", "", getFormElement(ISOTYPE_COMBO));
        assertEquals("Conjugate should not be pre-selected", "", getFormElement(CONJUGATE_COMBO));
        assertElementNotPresent(Locator.extButtonEnabled("Apply"));

        _extHelper.selectComboBoxItem("Isotype:", ISOTYPE_B);
        waitForFormElementToEqual(CONJUGATE_COMBO, CONJUGATE_A);
        assertElementPresent(Locator.extButtonEnabled("Apply"));
    }

    private LeveyJenningsMenuPage goToMenuPage()
    {
        goToQCAnalysisPage(TEST_ASSAY_LUM, "view levey-jennings reports");
        return new LeveyJenningsMenuPage(getDriver());
    }

    private void waitForAnalyteSelected(String analyte)
    {
        waitForElement(Locator.tagWithId("div", "analyte-grid-panel")
                .descendant(Locator.byClass("x-grid3-row-selected").withDescendant(Locator.tagWithText("span", analyte))));
    }

    @LogMethod
    private void importRun(int runNumber, File file, String isotype, String conjugate, boolean includeSinglePointControl)
    {
        goToTestAssayHome();
        clickButton("Import Data");
        setFormElement(Locator.name("Network"), "NETWORK" + runNumber);
        clickButton("Next");

        Calendar testDate = Calendar.getInstance();
        testDate.add(Calendar.DATE, runNumber - 10);
        importLuminexRunPageTwo("LJ Menu Run " + runNumber, isotype, conjugate, "", "", "Notebook" + runNumber,
                "Experimental", "TECH", df.format(testDate.getTime()), file, 0);

        if (includeSinglePointControl)
            checkCheckbox(Locator.name("_singlePointControl_" + SINGLE_POINT_CONTROL));

        clickButton("Save and Finish");
    }
}
