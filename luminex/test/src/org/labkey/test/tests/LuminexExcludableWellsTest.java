/*
 * Copyright (c) 2011-2014 LabKey Corporation
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
package org.labkey.test.tests;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.LuminexAll;
import org.labkey.test.util.DataRegionTable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({DailyA.class, LuminexAll.class, Assays.class})
public final class LuminexExcludableWellsTest extends LuminexTest
{
    private static final String EXCLUDE_SELECTED_BUTTON = "excludeselected";

    private final String excludedWellDescription = "Sample 2";
    private final String excludedWellType = "X25";
    private final Set<String> excludedWells = new HashSet<>(Arrays.asList("E1", "F1"));

    /**
     * test of well exclusion- the ability to exclude certain wells or analytes and add ac oment as to why
     * preconditions: LUMINEX project and assay list exist.  Having the Multiple Curve data will speed up execution
     * but is not required
     * postconditions:  multiple curve data will be present, certain wells will be marked excluded
     */
    @Test
    public void testWellExclusion()
    {
        ensureMultipleCurveDataPresent(TEST_ASSAY_LUM);

        clickAndWait(Locator.linkContainingText(MULTIPLE_CURVE_ASSAY_RUN_NAME));

        //ensure multiple curve data present
        //there was a bug (never filed) that showed up with multiple curve data, so best to use that.

        String[] analytes = getListOfAnalytesMultipleCurveData();

        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("ExclusionComment");
        _customizeViewsHelper.applyCustomView();

        //"all" excludes all
        String excludeAllWellName = "E1";
        excludeAllAnalytesForSingleWellTest(excludeAllWellName);

        String excludeOneWellName = "E1";
        excludeOneAnalyteForSingleWellTest(excludeOneWellName, analytes[0]);

        //excluding for one well excludes for duplicate wells
        excludeAnalyteForAllWellsTest(analytes[1]);

        // Check out the exclusion report
        clickAndWait(Locator.linkWithText("view excluded data"));
        assertTextPresent("Changed for all analytes", "exclude single analyte for single well", "ENV7 (93)", "ENV6 (97)");
        assertTextPresent("multipleCurvesTestRun", 2);
    }

    /**
     * verify that a user can exclude every analyte for a single well, and that this
     * successfully applies to both the original well and its duplicates
     *
     * preconditions:  at run screen, wellName exists
     * postconditions: no change (exclusion is removed at end of test)
     * @param wellName name of well to excluse
     */
    private void excludeAllAnalytesForSingleWellTest(String wellName)
    {
        DataRegionTable table = new DataRegionTable("Data", this);
        table.setFilter("WellRole", "Equals", "Unknown");
        clickExclusionMenuIconForWell(wellName);
        String comment = "exclude all for single well";
        setFormElement(Locator.name(EXCLUDE_COMMENT_FIELD), comment);
        clickButton(SAVE_CHANGES_BUTTON, 2 * defaultWaitForPage);
        table.clearFilter("WellRole");

        excludeForSingleWellVerify("Excluded for replicate group: " + comment, new HashSet<>(Arrays.asList(getListOfAnalytesMultipleCurveData())));

        //remove exclusions to leave in clean state
        table.setFilter("WellRole", "Equals", "Unknown");
        clickExclusionMenuIconForWell(wellName);
        click(Locator.radioButtonById("excludeselected"));
        clickButton(SAVE_CHANGES_BUTTON, 0);
        _extHelper.waitForExtDialog("Warning");
        clickButton("Yes", 2 * defaultWaitForPage);
        table.clearFilter("WellRole");
    }

    private void excludeOneAnalyteForSingleWellTest(String wellName, String excludedAnalyte)
    {
        waitForText("Well Role");
        DataRegionTable table = new DataRegionTable("Data", this);
        table.setFilter("WellRole", "Equals", "Unknown");
        clickExclusionMenuIconForWell(wellName);
        String exclusionComment = "exclude single analyte for single well";
        setFormElement(EXCLUDE_COMMENT_FIELD, exclusionComment);
        click(Locator.radioButtonById(EXCLUDE_SELECTED_BUTTON));
        clickExcludeAnalyteCheckBox(excludedAnalyte);
        clickButton(SAVE_CHANGES_BUTTON, 2 * defaultWaitForPage);
        table.clearFilter("WellRole");

        excludeForSingleWellVerify("Excluded for replicate group: " + exclusionComment, new HashSet<>((Arrays.asList(excludedAnalyte))));
    }

    /**
     * go through every well.  If they match the hardcoded well, description, and type values, and one of the analyte values given
     * verify that the row has the expected comment
     *
     * @param expectedComment
     * @param analytes
     */
    private void excludeForSingleWellVerify(String expectedComment, Set<String> analytes)
    {
        for (String analyte : analytes)
        {
            setFilter("Data", "Analyte", "Equals", analyte);

            List<List<String>> vals = getColumnValues(DATA_TABLE_NAME, "Well", "Description", "Type", "Exclusion Comment", "Analyte");
            List<String> wells = vals.get(0);
            List<String> descriptions = vals.get(1);
            List<String> types = vals.get(2);
            List<String> comments = vals.get(3);
            List<String> analytesPresent = vals.get(4);

            String well;
            String description;
            String type;
            String comment;

            for(int i=0; i<wells.size(); i++)
            {
                well = wells.get(i);
                description = descriptions.get(i);
                type = types.get(i);
                comment = comments.get(i);
                String analyteVal = analytesPresent.get(i);

                try
                {
                    if(matchesWell(description, type, well) && analytes.contains(analyteVal))
                    {
                        assertEquals(expectedComment,comment);
                    }

                    if(expectedComment.equals(comment))
                    {
                        assertTrue(matchesWell(description, type, well));
                        assertTrue(analytes.contains(analyteVal));
                    }
                }
                catch (Exception rethrow)
                {
                    log("well: " + well);
                    log("description: " + description);
                    log("type: " + type);
                    log("Comment: "+ comment);
                    log("Analyte: " + analyteVal);

                    throw rethrow;
                }
            }
        }
    }

    //verifies if description, type, and well match the hardcoded values
    private boolean matchesWell(String description, String type, String well)
    {
        return excludedWellDescription.equals(description) &&
                excludedWellType.equals(type) &&
                excludedWells.contains(well);
    }

    /**
     * verify a user can exclude a single analyte for all wells
     * preconditions:  multiple curve data imported, on assay run page
     * post conditions: specified analyte excluded from all wells, with comment "Changed for all analytes"
     * @param analyte
     */
    private void excludeAnalyteForAllWellsTest(String analyte)
    {
        String comment ="Changed for all analytes";
        excludeAnalyteForRun(analyte, true, comment);

        String exclusionPrefix = "Excluded for analyte: ";
        Map<String, Set<String>> analyteToExclusion = new HashMap<>();
        Set<String> set = new HashSet<>();
        set.add(exclusionPrefix + comment);
        analyteToExclusion.put(analyte, set);

        analyteToExclusion = createExclusionMap(set, analyte);

        compareColumnValuesAgainstExpected("Analyte", "Exclusion Comment", analyteToExclusion);
    }

    /**
     * return a map that, for each key, has value value
     * @param value
     * @param key
     * @return
     */
    private Map<String, Set<String>> createExclusionMap(Set<String> value, String... key)
    {
        Map<String, Set<String>> m  = new HashMap<>();

        for(String k: key)
        {
            m.put(k, value);
        }

        return m;
    }
}
