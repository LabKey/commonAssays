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
package org.labkey.test.tests.luminex;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.assay.GetProtocolCommand;
import org.labkey.remoteapi.assay.Protocol;
import org.labkey.remoteapi.assay.ProtocolResponse;
import org.labkey.remoteapi.assay.SaveProtocolCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.pages.luminex.LeveyJenningsPlotWindow;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.luminex.LuminexGuideSetHelper;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({Daily.class, Assays.class})
@BaseWebDriverTest.ClassTimeout(minutes = 8)
public class LuminexSinglePointTest extends LuminexTest
{
    private final LuminexGuideSetHelper _guideSetHelper = new LuminexGuideSetHelper(this);

    private static final String file1 = "01-11A12-IgA-Biotin.xls";
    private static final String file2 = "02-14A22-IgA-Biotin.xls";
    private static final String file3 = "03-31A82-IgA-Biotin.xls";
    private static final String file4 = "04-17A32-IgA-Biotin.xls";

    @BeforeClass
    public static void configurePerl()
    {
        LuminexTest init = (LuminexTest)getCurrentTest();
        init.goToTestAssayHome();
        ReactAssayDesignerPage assayDesigner = init._assayHelper.clickEditAssayDesign();
        assayDesigner.setBackgroundImport(true);
        assayDesigner.clickFinish();
    }

    @Test
    public void testAssayOverAPI() throws Exception
    {
        String assayName = "testLuminexAssayOverRemoteAPI";

        Connection connection = createDefaultConnection(true);
        GetProtocolCommand getProtocolCommand = new GetProtocolCommand("Luminex");
        ProtocolResponse getProtocolResponse = getProtocolCommand.execute(connection, getCurrentContainerPath());

        String autoLinkContainer = getContainerId();

        Protocol newAssayProtocol = getProtocolResponse.getProtocol();
        newAssayProtocol.setName(assayName)
                .setSaveScriptFiles(true)
                .setBackgroundUpload(true)
                .setEditableRuns(true)
                .setAutoCopyTargetContainerId(autoLinkContainer);
        SaveProtocolCommand saveProtocolCommand = new SaveProtocolCommand(newAssayProtocol);
        ProtocolResponse saveProtocolResponse = saveProtocolCommand.execute(connection, getCurrentContainerPath());

        assertTrue(saveProtocolResponse.getProtocol().getSaveScriptFiles());
        assertTrue(saveProtocolResponse.getProtocol().getBackgroundUpload());
        assertTrue(saveProtocolResponse.getProtocol().getEditableRuns());
        assertTrue(saveProtocolResponse.getProtocol().getAllowTransformationScript());
        assertEquals(autoLinkContainer, saveProtocolResponse.getProtocol().getAutoCopyTargetContainerId());
    }

    @Test
    public void testSinglePoint()
    {
        importRun(file1, 1);
        importRun(file2, 2);
        assertTextPresent(TEST_ASSAY_LUM + " Upload Jobs");
        waitForPipelineJobsToComplete(3, false);
        goToTestAssayHome();
        waitForElement(Locator.tagWithText("a", "view qc report"));
        BootstrapMenu.find(getDriver(), "view qc report").clickSubMenu(true,"view single point control qc report");
        waitForText("Average Fi Bkgd");

        DataRegionTable tbl = new DataRegionTable("AnalyteSinglePointControl", getDriver());
        tbl.setFilter("Analyte", "Equals", "ENV1");
        tbl.setSort("SinglePointControl/Run/Name", SortDirection.ASC);
        assertEquals("27.0", tbl.getDataAsText(0, "Average Fi Bkgd"));
        assertEquals("30.0", tbl.getDataAsText(1, "Average Fi Bkgd"));

        LeveyJenningsPlotWindow ljp = new LeveyJenningsPlotWindow(this);

        // check LJ plots column
        tbl.link(0, 2).click();
        ljp.waitTillReady();
        assertEquals(ljp.getXTickTagElementText(), "Notebook1");
        assertEquals(Arrays.asList("Notebook1", "Notebook2"), ljp.getXAxis());
        ljp.closeWindow();

        addUrlParameter("_testLJQueryLimit=0");
        tbl = new DataRegionTable("AnalyteSinglePointControl", getDriver());
        tbl.link(0, 2).click();
        ljp.waitTillReady();
        assertEquals("Notebook1", ljp.getXTickTagElementText());
        assertEquals(Arrays.asList("Notebook1"), ljp.getXAxis());
        ljp.closeWindow();

        clickAndWait(Locator.linkContainingText("graph"));
        assertTextNotPresent("ERROR");

        _guideSetHelper.createGuideSet(true);
        _guideSetHelper.editRunBasedGuideSet(new String[]{"allRunsRow_1", "allRunsRow_0"}, "Single Point Control Guide Set 1", true);

        importRun(file3, 3);
        waitForPipelineJobsToComplete(4, false);

        goToLeviJennings();
        waitForElement(Locator.tagWithText("a", "CTRL"));

        importRun(file4, 4);
        waitForPipelineJobsToComplete(5, false);

        goToLeviJennings();
        waitForElement(Locator.linkWithText(file3));
        assertTextNotPresent(file4);

        verifyValueBasedGuideSet();
    }

    private void verifyValueBasedGuideSet()
    {
        String guideSetComment = "Single Point Control Guide Set 2";
        Locator ctrlFlag = Locator.tagWithText("a", "CTRL");

        Map<String, Double> metricInputs = new TreeMap<>();
        metricInputs.put("MaxFIAverage", 33.0);
        metricInputs.put("MaxFIStdDev", 1.25);
        _guideSetHelper.createGuideSet(false);
        _guideSetHelper.editValueBasedGuideSet(metricInputs, guideSetComment, true);

        _guideSetHelper.applyGuideSetToRun("NETWORK3", guideSetComment, true);
        waitForElementToDisappear(ctrlFlag);

        metricInputs.put("MaxFIStdDev", 0.25);
        clickButtonContainingText("Edit", 0);
        _guideSetHelper.editValueBasedGuideSet(metricInputs, guideSetComment, false);
        waitForElement(ctrlFlag);

        metricInputs.put("MaxFIAverage", 35.0);
        metricInputs.put("MaxFIStdDev", null);
        clickButtonContainingText("Edit", 0);
        _guideSetHelper.editValueBasedGuideSet(metricInputs, guideSetComment, false);
        waitForElementToDisappear(ctrlFlag);

        _guideSetHelper.applyGuideSetToRun("NETWORK3", "Single Point Control Guide Set 1", false);
        waitForElement(ctrlFlag);
    }

    private void goToLeviJennings()
    {
        goToTestAssayHome();
        BootstrapMenu.find(getDriver(), "view qc report").clickSubMenu(true, "view single point control qc report");
        DataRegionTable table = new DataRegionTable("AnalyteSinglePointControl", getDriver());
        table.setFilter("Analyte", "Equals", "ENV1");
        clickAndWait(Locator.linkContainingText("graph"));
    }

    private void importRun(String filename, int runNumber) {

        goToTestAssayHome();
        clickButton("Import Data");
        setFormElement(Locator.name("network"), "NETWORK" + runNumber);
        clickButton("Next");

        Calendar testDate = Calendar.getInstance();
        testDate.add(Calendar.DATE, 1);

        importLuminexRunPageTwo(filename, isotype, conjugate, "", "", "Notebook"+runNumber,
                "Experimental", "TECH", df.format(testDate.getTime()),
                TestFileUtils.getSampleData("luminex/" + filename), 1);

         switch(runNumber){
             case 1 :
                 checkCheckbox(Locator.name("_singlePointControl_IH5672"));
                 break;
             case 2 :
                 assertChecked(Locator.name("_singlePointControl_IH5672"));
                 break;
             case 4 :
                 uncheckCheckbox(Locator.name("_singlePointControl_IH5672"));
                 break;
             default :
                 break;
         }

        clickButton("Save and Finish");

    }
}
