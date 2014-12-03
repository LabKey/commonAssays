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

import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.LuminexAll;
import org.labkey.test.pages.AssayDomainEditor;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LuminexGuideSetHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by aaronr on 11/24/14.
 */

// TODO: follow cory's example with filter vs iterate over DRT... (?)

// NOTE: luminex tests overwrite each others project...

@Category({DailyA.class, LuminexAll.class, Assays.class})
public final class LuminexGuideSetDisablingTest extends LuminexTest
{
    private LuminexGuideSetHelper _guideSetHelper = new LuminexGuideSetHelper(this);
    private static final File[] GUIDE_SET_FILES = {
            TestFileUtils.getSampleData("Luminex/01-11A12-IgA-Biotin.xls"),
            TestFileUtils.getSampleData("Luminex/02-14A22-IgA-Biotin.xls"),
            TestFileUtils.getSampleData("Luminex/03-31A82-IgA-Biotin.xls"),
            TestFileUtils.getSampleData("Luminex/04-17A32-IgA-Biotin.xls")
    };
    private static final String GUIDE_SET_WINDOW_NAME = "Guide Set Parameter Details";
    private static final String RUN_BASED_ANALYTE = "ENV1 (31)";
    private static final String VALUE_BASED_ANALYTE = "ENV2 (41)";
    private static final String[] NETWORKS = new String[]{"NETWORK2", "NETWORK3", "NETWORK4", "NETWORK1"};
    private static final String RUN_BASED_COMMENT = "LuminexGuideSetDisablingTest "+RUN_BASED_ANALYTE;
    private static final String VALUE_BASED_COMMENT = "LuminexGuideSetDisablingTest "+VALUE_BASED_ANALYTE;
    private static final String CONTROL_NAME = "Standard1";
    private static final Locator.XPathLocator TABLE_LOCATOR = Locator.xpath("//table").withClass("gsDetails");
    private static final Locator SAVE_BTN = Ext4Helper.Locators.ext4Button("Save");

    public LuminexGuideSetDisablingTest()
    {
        // setup the testDate variable
        _guideSetHelper.TESTDATE.add(Calendar.DATE, -GUIDE_SET_FILES.length);
    }

    @BeforeClass // note: this runs after LuminexTest.initTest
    public static void initer()
    {
        ((LuminexGuideSetDisablingTest)getCurrentTest()).init();
    }

    public void init()
    {
        // add the R transform script to the assay
        goToTestAssayHome();
        _assayHelper.clickEditAssayDesign();
        AssayDomainEditor assayDesigner = new AssayDomainEditor(this);
        assayDesigner.addTransformScript(RTRANSFORM_SCRIPT_FILE_LABKEY);
        _listHelper.addField(TEST_ASSAY_LUM + " Batch Fields", "CustomProtocol", "Protocol", ListHelper.ListColumnType.String);
        // save changes to assay design
        assayDesigner.saveAndClose();

        for (int i = 0; i < GUIDE_SET_FILES.length; i++)
            _guideSetHelper.importGuideSetRun(TEST_ASSAY_LUM, GUIDE_SET_FILES[i]);

        createRunBasedGuideSet();
        createValueBasedGuideSet();
    }

    private void createRunBasedGuideSet()
    {
        _guideSetHelper.goToLeveyJenningsGraphPage(TEST_ASSAY_LUM, CONTROL_NAME);
        _guideSetHelper.setUpLeveyJenningsGraphParams(RUN_BASED_ANALYTE);
        _guideSetHelper.createGuideSet(true);

        _guideSetHelper.editRunBasedGuideSet(new String[]{"allRunsRow_1", "allRunsRow_0"}, RUN_BASED_COMMENT, true);

        // apply the new guide set to a run
        _guideSetHelper.applyGuideSetToRun(NETWORKS, RUN_BASED_COMMENT, true);
    }

    // borrowing from LuminexValueBasedGuideSetTest
    private void createValueBasedGuideSet()
    {
        Map<String, Double> metricInputs = new TreeMap<>();
        _guideSetHelper.goToLeveyJenningsGraphPage(TEST_ASSAY_LUM, CONTROL_NAME);
        _guideSetHelper.setUpLeveyJenningsGraphParams(VALUE_BASED_ANALYTE);
        _guideSetHelper.createGuideSet(true);

        metricInputs.put("EC504PLAverage", 42158.22);
        metricInputs.put("EC504PLStdDev", 4833.76);
        metricInputs.put("EC505PLAverage", 40987.31);
        metricInputs.put("EC505PLStdDev", 4280.84);
        metricInputs.put("AUCAverage", 85268.04);
        metricInputs.put("AUCStdDev", 738.55);
        metricInputs.put("MaxFIAverage", 32507.27);
        metricInputs.put("MaxFIStdDev", 189.83);

        _guideSetHelper.editValueBasedGuideSet(metricInputs, VALUE_BASED_COMMENT, true);
        _guideSetHelper.applyGuideSetToRun(NETWORKS, VALUE_BASED_COMMENT, true);
    }

    @Test
    public void verifyGuideSetsPresent()
    {
        Set<String> expectedComments = ImmutableSet.of(RUN_BASED_COMMENT, VALUE_BASED_COMMENT);

        _guideSetHelper.goToManageGuideSetsPage(TEST_ASSAY_LUM);
        DataRegionTable drt = new DataRegionTable("GuideSet", this);
        Set<String> actualComments = new HashSet<>(drt.getColumnDataAsText("Comment"));

        if (!actualComments.containsAll(expectedComments))
            Assert.fail("It appear that there are missing guide sets.");
        // note consider failing on extras too?

        // check guide sets are set in LJ reports
        _guideSetHelper.goToLeveyJenningsGraphPage(TEST_ASSAY_LUM, CONTROL_NAME);
        _guideSetHelper.setUpLeveyJenningsGraphParams(RUN_BASED_ANALYTE);
        assertTextPresent(RUN_BASED_COMMENT);

        _guideSetHelper.setUpLeveyJenningsGraphParams(VALUE_BASED_ANALYTE);
        assertTextPresent(VALUE_BASED_COMMENT);
    }

    @Test
    public void verifyQCFlagsDisabledOnImport()
    {
        String analyte = "ENV5 (58)";
        String comment = "verifyQCFlagsDisabledOnImport";

        // first create NEW guide set
        _guideSetHelper.goToLeveyJenningsGraphPage(TEST_ASSAY_LUM, CONTROL_NAME);
        _guideSetHelper.setUpLeveyJenningsGraphParams(analyte);
        _guideSetHelper.createGuideSet(true);
        _guideSetHelper.editRunBasedGuideSet(new String[]{"allRunsRow_2"}, comment, true);

        // NOTE: consider validating now that the GuideSet was set properly...
//        like: validateRedText(true, "8.08", "61889.88", "2.66", "64608.73");

        clickButtonContainingText("Details", 0);
        click(Locator.checkboxByName("EC504PLCheckBox"));
        click(Locator.checkboxByName("EC505PLCheckBox"));
        click(Locator.checkboxByName("MFICheckBox"));
        click(Locator.checkboxByName("AUCCheckBox"));
        click(SAVE_BTN);

        _guideSetHelper.importGuideSetRun(TEST_ASSAY_LUM, TestFileUtils.getSampleData("Luminex/plate 3_IgA-Biot (Standard1).xls"));

        // go back to LJ plot
        _guideSetHelper.goToLeveyJenningsGraphPage(TEST_ASSAY_LUM, CONTROL_NAME);
        _guideSetHelper.setUpLeveyJenningsGraphParams(analyte);
        _guideSetHelper.applyGuideSetToRun(NETWORKS, comment, true);

        validateRedText(false, "22798.96", "29916.07", "75573.07", "31984.00");

        // no clean-up needed as test shouldn't collide with others...
    }

    @Test
    public void verifyQCFlagToggling()
    {
        // note: this uses the run based guide set

        // simular to verifyHighlightUpdatesAfterQCFlagChange (but not quite the same... not sure how this other place works)
        _guideSetHelper.goToLeveyJenningsGraphPage(TEST_ASSAY_LUM, CONTROL_NAME);
        _guideSetHelper.setUpLeveyJenningsGraphParams(RUN_BASED_ANALYTE);
        validateRedText(true, "8.08", "61889.88", "2.66", "64608.73");

        clickButtonContainingText("Details", 0);
        // toggle off the AUC QC flag and then validate errors
        waitForElement(Locator.checkboxByName("AUCCheckBox"));
        click(Locator.checkboxByName("AUCCheckBox"));
        click(SAVE_BTN);
        clickAndWait(Ext4Helper.ext4WindowButton(GUIDE_SET_WINDOW_NAME, "Save"), WAIT_FOR_EXT_MASK_TO_DISSAPEAR);

        validateRedText(true, "8.08", "2.66");
        validateRedText(false, "61889.88", "64608.73");

        // toggle off the rest of the QC flags and then validate errors
        clickButtonContainingText("Details", 0);
        waitForElement(Locator.checkboxByName("EC504PLCheckBox"));
        click(Locator.checkboxByName("EC504PLCheckBox"));
        click(Locator.checkboxByName("EC505PLCheckBox"));
        click(Locator.checkboxByName("MFICheckBox"));
        clickAndWait(Ext4Helper.ext4WindowButton(GUIDE_SET_WINDOW_NAME, "Save"), WAIT_FOR_EXT_MASK_TO_DISSAPEAR);

        validateRedText(false, "8.08", "61889.88", "2.66", "64608.73");

        // clean-up/revert
        clickButtonContainingText("Details", 0);
        waitForElement(Locator.checkboxByName("EC504PLCheckBox"));
        click(Locator.checkboxByName("EC504PLCheckBox"));
        click(Locator.checkboxByName("EC505PLCheckBox"));
        click(Locator.checkboxByName("MFICheckBox"));
        click(Locator.checkboxByName("AUCCheckBox"));
        clickAndWait(Ext4Helper.ext4WindowButton(GUIDE_SET_WINDOW_NAME, "Save"), WAIT_FOR_EXT_MASK_TO_DISSAPEAR);

        validateRedText(true, "8.08", "61889.88", "2.66", "64608.73");
    }

    private void validateRedText(boolean present, String... texts)
    {
        if (present)
            for (String text : texts)
                waitForElement(Locator.xpath("//div[text()='"+text+"' and contains(@style,'red')]"));
        else
            for (String text : texts)
                waitForElementToDisappear(Locator.xpath("//div[text()='" + text + "' and contains(@style,'red')]"));
    }

    @Test
    public void verifyGuideSetParameterDetailsWindow()
    {
        Locator closeBtn = Ext4Helper.Locators.ext4Button("Close");
        // validates both a run-based and a value-based GuideSet
        _guideSetHelper.goToManageGuideSetsPage(TEST_ASSAY_LUM);

        clickGuideSetDetailsByComment(RUN_BASED_COMMENT);
        validateGuideSetRunDetails("Run-based");
        validateGuideSetMetricsDetails(new String[][]{
            {"0.323", "5.284", "2" },
            {"0.540", "5.339", "2" },
            {"331.633", "32086.500", "2" },
            {"149.318", "63299.346", "2" },
        });
        // validate checkboxes are not displayed
        assertTextPresent("Num Runs");
        assertElementPresent(TABLE_LOCATOR.append("//input[@type='checkbox']"));
        // this gets a mis-fire and selenium raises error that element is not the expected click element.
        click(closeBtn);

        clickGuideSetDetailsByComment(VALUE_BASED_COMMENT);
        validateGuideSetRunDetails("Value-based");
        validateGuideSetMetricsDetails(new String[][]{
            {"4833.760", "42158.220"},
            {"4280.840", "40987.310"},
            {"189.830", "32507.270"},
            {"738.550", "85268.040"}
        });
        // validate checkboxes are not displayed
        assertTextNotPresent("Num Runs");
        assertElementNotPresent(TABLE_LOCATOR.append("//input[@type='checkbox']"));
        click(closeBtn);
    }

    private void clickGuideSetDetailsByComment(String comment)//, @Nullable DataRegionTable drt)
    {
        boolean found = false;
//        if(drt == null)
        DataRegionTable drt = new DataRegionTable("GuideSet", this);
        for (int i=0; i < drt.getDataRowCount(); i++)
            if (drt.getDataAsText(i, "Comment").equals(comment))
            {
                click(drt.link(i,-1));
                found = true;
            }

        if (!found)
            Assert.fail("There is no GuideSet with that comment. Check the value of the comment and try again.");
        else
            waitForElement(TABLE_LOCATOR);

    }

    private void validateGuideSetRunDetails(String type)
    {
        String analyte, comment;
        if (type.equals("Run-based"))
        {
            analyte = RUN_BASED_ANALYTE;
            comment = RUN_BASED_COMMENT;
        }
        else
        {
            analyte = VALUE_BASED_ANALYTE;
            comment = VALUE_BASED_COMMENT;
        }

        // check top left table
        Locator.XPathLocator table = TABLE_LOCATOR.append("[1]");
        assertNotEquals("", getTableCellText(table, 0, 1)); // find better check
        assertNotEquals("", getTableCellText(table, 1, 1)); // find better check...
        assertEquals(CONTROL_NAME, getTableCellText(table, 2, 1));
        assertEquals(analyte, getTableCellText(table, 3, 1));
        assertEquals(comment, getTableCellText(table, 4, 1));

        // check top right table
        table = TABLE_LOCATOR.append("[2]");
        assertEquals(type, getTableCellText(table,0, 1));
        assertEquals("IgG >", getTableCellText(table,1, 1));
        assertEquals("PE >", getTableCellText(table,2, 1));
    }

    private void validateGuideSetMetricsDetails(String[][] metricsData) {
        Locator.XPathLocator table = Locator.xpath("//table").withClass("gsDetails").append("[3]");

        for(int i=1; i < 4; i++) // iterate rows
        {
            assertEquals(metricsData[i - 1][0], getTableCellText(table, i, 1));
            assertEquals(metricsData[i - 1][1], getTableCellText(table, i, 2));
        }

        // check if counts included and validate if so (e.g. run-based)
        if (metricsData[0].length == 3)
            for(int i=1; i < 4; i++) // iterate rows
                assertEquals(metricsData[i-1][2], getTableCellText(table,i, 3));

    }

}
