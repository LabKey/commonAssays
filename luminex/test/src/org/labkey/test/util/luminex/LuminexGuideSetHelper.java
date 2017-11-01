/*
 * Copyright (c) 2014-2017 LabKey Corporation
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
package org.labkey.test.util.luminex;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.tests.luminex.LuminexTest;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.LogMethod;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LuminexGuideSetHelper
{
    public static final String[] GUIDE_SET_ANALYTE_NAMES = {"GS Analyte A", "GS Analyte B"};
    private static Map<Integer, String> timestamps = new HashMap<>();
    LuminexTest _test;
    
    public Calendar TESTDATE = Calendar.getInstance();
    private int _runNumber;
    
    public LuminexGuideSetHelper(LuminexTest test)
    {
        _test = test;
        _runNumber = 1;
    }

    @LogMethod
    public int importGuideSetRun(String assayName, File guideSetFile)
    {
        _test.goToTestAssayHome(assayName);
        _test.clickButton("Import Data");
        _test.setFormElement(Locator.name("network"), "NETWORK" + (_runNumber));
        if (_test.isElementPresent(Locator.name("customProtocol")))
            _test.setFormElement(Locator.name("customProtocol"), "PROTOCOL" + (_runNumber));
        _test.clickButton("Next");

        TESTDATE.add(Calendar.DATE, 1);
        _test.importLuminexRunPageTwo("Guide Set plate " + (_runNumber), LuminexTest.isotype, LuminexTest.conjugate, "", "", "Notebook" + (_runNumber),
                "Experimental", "TECH" + (_runNumber), LuminexTest.df.format(TESTDATE.getTime()), guideSetFile, 0);
        _test.uncheckCheckbox(Locator.name("_titrationRole_standard_Standard1"));
        _test.checkCheckbox(Locator.name("_titrationRole_qccontrol_Standard1"));
        _test.clickButton("Save and Finish");
        List<String> errors = _test.getTexts(Locators.labkeyError.withText().findElements(_test.getDriver()));
        assertTrue("Unexpected error(s): " + errors, errors.isEmpty());
        return _runNumber++;
    }

    @LogMethod
    public void verifyGuideSetsNotApplied(String assayName)
    {
        _test.goToSchemaBrowser();
        _test.selectQuery("assay.Luminex." + assayName, "AnalyteTitration");
        _test.waitForText("view data");
        _test.clickAndWait(Locator.linkContainingText("view data"));
        DataRegionTable table = new DataRegionTable("query", _test.getDriver());
        table.setFilter("GuideSet/Created", "Is Not Blank", "");
        // check that the table contains one row that reads "No data to show."
        assertEquals("Expected no guide set assignments", 0, table.getDataRowCount());
        table.clearFilter("GuideSet/Created");
    }

    public void checkManageGuideSetHeader(boolean creating)
    {
        if (creating)
        {
            _test.waitForText("Create Guide Set...");
            _test.waitForText("Guide Set Id:");
            _test.assertTextPresent("TBD", 2);
        }
        else
        {
            _test.waitForText("Manage Guide Set...");
            _test.waitForText("Guide Set Id:");
            Integer id = Integer.parseInt(Locator.id("guideSetIdLabel").waitForElement(_test.shortWait()).getText());
            _test.assertTextPresentInThisOrder("Created:", timestamps.get(id));
        }
    }

    @LogMethod
    public void editRunBasedGuideSet(String[] rows, String comment, boolean creating)
    {
        checkManageGuideSetHeader(creating);

        addRemoveGuideSetRuns(rows);

        _test.setFormElement(Locator.name("commentTextField"), comment);
        saveGuideSet(creating);

        checkLeveyJenningsGuideSetHeader(comment, "Run-based");
    }

    @LogMethod
    public void editValueBasedGuideSet(Map<String, Double> metricInputs, String comment, boolean creating)
    {
        checkManageGuideSetHeader(creating);

        if (creating)
            _test.checkRadioButton(Locator.radioButtonByNameAndValue("ValueBased", "true"));
        setValueBasedMetricForm(metricInputs);

        _test.setFormElement(Locator.name("commentTextField"), comment);
        saveGuideSet(creating);

        checkLeveyJenningsGuideSetHeader(comment, "Value-based");
    }

    private void saveGuideSet(boolean creating)
    {
        if (creating)
        {
            _test.assertElementNotPresent(Locator.button("Save"));
            _test.assertElementPresent(Locator.button("Create"));
            _test.clickButton("Create", 0);
            waitForGuideSetExtMaskToDisappear();
            Integer id = Integer.parseInt(Locator.css(".guideset-tbl").waitForElement(_test.shortWait()).getAttribute("guide-set-id"));
            timestamps.put(id, LuminexTest.df.format(Calendar.getInstance().getTime()));
        }
        else
        {
            _test.assertElementNotPresent(Locator.button("Create"));
            _test.assertElementPresent(Locator.button("Save"));
            _test.clickButton("Save", 0);
            waitForGuideSetExtMaskToDisappear();
        }
    }

    private void checkLeveyJenningsGuideSetHeader(String comment, String guideSetType)
    {
        Integer id = Integer.parseInt(Locator.css(".guideset-tbl").waitForElement(_test.shortWait()).getAttribute("guide-set-id"));
        _test.waitForElement(Locator.css(".guideset-tbl td").withText(timestamps.get(id)), 2 * _test.defaultWaitForPage);
        _test.waitForElement(Locator.css(".guideset-tbl td").withText(comment));
        _test.assertElementPresent(Locator.css(".guideset-tbl td").withText(guideSetType));
    }

    private void setValueBasedMetricForm(Map<String, Double> metricInputs)
    {
        for (Map.Entry<String, Double> metricEntry : metricInputs.entrySet())
        {
            String strVal = metricEntry.getValue() != null ? metricEntry.getValue().toString() : null;
            _test.setFormElement(Locator.name(metricEntry.getKey()), strVal);
        }
    }

    public void waitForGuideSetExtMaskToDisappear()
    {
        _test._ext4Helper.waitForMaskToDisappear();
        _test._extHelper.waitForExt3MaskToDisappear(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        waitForLeveyJenningsTrendPlot();
    }

    public void goToLeveyJenningsGraphPage(String assayName, String titrationName)
    {
        _test.goToQCAnalysisPage(assayName, "view levey-jennings reports");
        _test.waitAndClick(Locator.linkWithText(titrationName));

        // Make sure we have the expected help text
        _test.waitForText("To begin, choose an Antigen, Isotype, and Conjugate from the panel to the left and click the Apply button.");
    }

    public void goToManageGuideSetsPage(String assayName)
    {
        _test.goToQCAnalysisPage(assayName, "view guide sets");
        _test.waitForElement(Locator.pageHeader("Manage Guide Sets"));
    }

    @LogMethod
    public void applyGuideSetToRun(String network, String comment, boolean useCurrent)
    {
        applyGuideSetToRun(new String[]{network}, comment, useCurrent);
    }

    @LogMethod
    public void applyGuideSetToRun(String[] networks, String comment, boolean useCurrent)
    {
        for (String network : networks)
            _test.click(ExtHelper.locateGridRowCheckbox(network));

        _test.doAndWaitForPageSignal(() -> _test.clickButton("Apply Guide Set", 0), "guideSetSelectionChange");

        if(!useCurrent)
            _test.doAndWaitForPageSignal(() -> _test.click(ExtHelper.locateGridRowCheckbox(comment)), "guideSetSelectionChange");

        _test.clickButton("Apply Thresholds", 0);
        _test._extHelper.waitForExt3MaskToDisappear(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        // verify that the plot is reloaded
        waitForLeveyJenningsTrendPlot();
    }

    public void waitForLeveyJenningsTrendPlot()
    {
        _test.waitForTextToDisappear("Loading plot...", BaseWebDriverTest.WAIT_FOR_PAGE * 2);
        _test.waitForElement(Locator.tagWithClass("div", "ljTrendPlot").withDescendant(Locator.xpath("//*[local-name() = 'svg']")));
    }

    public void createGuideSet(boolean initialGuideSet)
    {
        if (initialGuideSet)
            _test.waitForText("No current guide set for the selected graph parameters");
        else
            waitForLeveyJenningsTrendPlot();
        _test.clickButtonContainingText("New", 0);
        if (!initialGuideSet)
        {
            _test.waitForText("Creating a new guide set will cause the current guide set to be uneditable. Would you like to proceed?");
            _test.clickButton("Yes", 0);
        }
    }

    private void addRemoveGuideSetRuns(String[] rows)
    {
        for(String row: rows)
        {
            _test.waitForElement(Locator.id(row));
            _test.click(Locator.tagWithId("span", row));
        }
    }

    public void setUpLeveyJenningsGraphParams(String analyte)
    {
        _test.log("Setting Levey-Jennings Report graph parameters for Analyte " + analyte);
        _test.waitForText(analyte);
        _test.click(Locator.tagContainingText("span", analyte));

        _test._extHelper.selectComboBoxItem("Isotype:", LuminexTest.isotype);
        _test._extHelper.selectComboBoxItem("Conjugate:", LuminexTest.conjugate);
        _test.click(Locator.extButton("Apply"));

        // wait for the test headers in the guide set and tracking data regions
        _test.waitForText(analyte + " - " + LuminexTest.isotype + " " + LuminexTest.conjugate);
        _test.waitForText("Standard1 Tracking Data for " + analyte + " - " + LuminexTest.isotype + " " + LuminexTest.conjugate);
        waitForLeveyJenningsTrendPlot();
    }

    @LogMethod
    public void verifyGuideSetsApplied(String assayName, Map<String, Integer> guideSetIds, String[] analytes, int expectedRunCount)
    {
        // see if the 3 uploaded runs got the correct 'current' guide set applied
        _test.goToSchemaBrowser();
        _test.selectQuery("assay.Luminex." + assayName, "AnalyteTitration");
        _test.waitForText("view data");
        _test.clickAndWait(Locator.linkContainingText("view data"));
        _test._customizeViewsHelper.openCustomizeViewPanel();
        _test._customizeViewsHelper.showHiddenItems();
        _test._customizeViewsHelper.addColumn("Analyte/RowId");
        _test._customizeViewsHelper.addColumn("Titration/RowId");
        _test._customizeViewsHelper.addColumn("GuideSet/RowId");
        _test._customizeViewsHelper.applyCustomView();
        DataRegionTable table = new DataRegionTable("query", _test.getDriver());
        for (String analyte : analytes)
        {
            table.setFilter("GuideSet/RowId", "Equals", guideSetIds.get(analyte).toString());
            assertEquals("Expected guide set to be assigned to " + expectedRunCount + " records", expectedRunCount, table.getDataRowCount());
            table.clearFilter("GuideSet/RowId");
        }
    }

    public Map<String, Integer> getGuideSetIdMap(String assayName)
    {
        _test.goToSchemaBrowser();
        _test.selectQuery("assay.Luminex." + assayName, "GuideSet");
        _test.waitForText("view data");
        _test.clickAndWait(Locator.linkContainingText("view data"));
        Map<String, Integer> guideSetIds = new HashMap<>();
        _test._customizeViewsHelper.openCustomizeViewPanel();
        _test._customizeViewsHelper.showHiddenItems();
        _test._customizeViewsHelper.addColumn("RowId");
        _test._customizeViewsHelper.applyCustomView();
        DataRegionTable table = new DataRegionTable("query", _test.getDriver());
        table.setFilter("CurrentGuideSet", "Equals", "true");
        guideSetIds.put(table.getDataAsText(0, "Analyte Name"), Integer.parseInt(table.getDataAsText(0, "Row Id")));
        guideSetIds.put(table.getDataAsText(1, "Analyte Name"), Integer.parseInt(table.getDataAsText(1, "Row Id")));

        return guideSetIds;
    }
}
