/*
 * Copyright (c) 2014 LabKey Corporation
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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.LuminexAll;
import org.labkey.test.pages.AssayDesignerPage;
import org.labkey.test.util.LuminexGuideSetHelper;

import java.util.Map;
import java.util.TreeMap;

/**
 * Created by cnathe on 4/25/14.
 *
 * This test is meant to mimic the LuminexGuideSetTest but use value-based guide sets instead of run-based guide sets.
 */
@Category({DailyA.class, LuminexAll.class, Assays.class})
public final class LuminexValueBasedGuideSetTest extends LuminexTest
{
    private final LuminexGuideSetHelper _guideSetHelper = new LuminexGuideSetHelper(this);

    private final String[] UPDATED_EXPECTED_FLAGS = {"AUC, EC50-5, PCV", "", "", "", "PCV"};

    @BeforeClass
    public static void updateAssayDefinition()
    {
        LuminexTest init = (LuminexTest)getCurrentTest();
        init.goToTestAssayHome();
        AssayDesignerPage assayDesigner = init._assayHelper.clickEditAssayDesign();

        assayDesigner.addTransformScript(RTRANSFORM_SCRIPT_FILE_LABKEY);
        assayDesigner.saveAndClose();
    }

    @Test
    public void testValueBasedGuideSets()
    {
        _guideSetHelper.importGuideSetRun(TEST_ASSAY_LUM, LuminexGuideSetTest.GUIDE_SET_FILES[0]);
        _guideSetHelper.importGuideSetRun(TEST_ASSAY_LUM, LuminexGuideSetTest.GUIDE_SET_FILES[1]);
        _guideSetHelper.verifyGuideSetsNotApplied(TEST_ASSAY_LUM);
        createInitialGuideSets();
        _guideSetHelper.importGuideSetRun(TEST_ASSAY_LUM, LuminexGuideSetTest.GUIDE_SET_FILES[2]);
        _guideSetHelper.importGuideSetRun(TEST_ASSAY_LUM, LuminexGuideSetTest.GUIDE_SET_FILES[3]);
        _guideSetHelper.importGuideSetRun(TEST_ASSAY_LUM, LuminexGuideSetTest.GUIDE_SET_FILES[4]);
        Map<String, Integer> guideSetIds = _guideSetHelper.getGuideSetIdMap(TEST_ASSAY_LUM);
        _guideSetHelper.verifyGuideSetsApplied(TEST_ASSAY_LUM, guideSetIds, LuminexGuideSetHelper.GUIDE_SET_ANALYTE_NAMES, 5);
        verifyQCFlags(TEST_ASSAY_LUM, LuminexGuideSetHelper.GUIDE_SET_ANALYTE_NAMES[0], LuminexGuideSetTest.INITIAL_EXPECTED_FLAGS);
        verifyQCReport();
        udpateGuideSets();
        verifyQCFlags(TEST_ASSAY_LUM, LuminexGuideSetHelper.GUIDE_SET_ANALYTE_NAMES[1], UPDATED_EXPECTED_FLAGS);
        verifyGuideSetCurveFitEmpty(TEST_ASSAY_LUM);
    }

    private void createInitialGuideSets()
    {
        _guideSetHelper.goToLeveyJenningsGraphPage(TEST_ASSAY_LUM, "Standard1");

        Map<String, Double> metricInputs = new TreeMap<>();

        _guideSetHelper.setUpLeveyJenningsGraphParams(LuminexGuideSetHelper.GUIDE_SET_ANALYTE_NAMES[0]);
        _guideSetHelper.createGuideSet(true);
        metricInputs.put("EC504PLAverage", 179.78);
        metricInputs.put("EC504PLStdDev", 22.21);
        metricInputs.put("EC505PLAverage", 253.25);
        metricInputs.put("EC505PLStdDev", 16.98);
        metricInputs.put("AUCAverage", 8701.38);
        metricInputs.put("AUCStdDev", 466.81);
        metricInputs.put("MaxFIAverage", 11457.15);
        metricInputs.put("MaxFIStdDev", 549.21);
        String guideSetComment1 = "Analyte 1";
        _guideSetHelper.editValueBasedGuideSet(metricInputs, guideSetComment1, true);
        _guideSetHelper.applyGuideSetToRun(new String[]{"NETWORK1", "NETWORK2"}, guideSetComment1, true);

        _guideSetHelper.setUpLeveyJenningsGraphParams(LuminexGuideSetHelper.GUIDE_SET_ANALYTE_NAMES[1]);
        _guideSetHelper.createGuideSet(true);
        metricInputs.put("EC504PLAverage", 43426.10);
        metricInputs.put("EC504PLStdDev", 794.95);
        metricInputs.put("EC505PLAverage", 40349.13);
        metricInputs.put("EC505PLStdDev", 3084.91);
        metricInputs.put("AUCAverage", 80851.83);
        metricInputs.put("AUCStdDev", 6523.08);
        metricInputs.put("MaxFIAverage", 30992.25);
        metricInputs.put("MaxFIStdDev", 2083.49);
        String guideSetComment2 = "Analyte 2";
        _guideSetHelper.editValueBasedGuideSet(metricInputs, guideSetComment2, true);
        _guideSetHelper.applyGuideSetToRun(new String[]{"NETWORK1", "NETWORK2"}, guideSetComment2, true);
    }

    public void verifyGuideSetCurveFitEmpty(String assayName)
    {
        goToSchemaBrowser();
        selectQuery("assay.Luminex." + assayName, "GuideSetCurveFit");
        waitForText("view data");
        clickAndWait(Locator.linkContainingText("view data"));
        waitForText("No data to show");
    }

    private void udpateGuideSets()
    {
        Map<String, Double> metricInputs = new TreeMap<>();

        _guideSetHelper.goToLeveyJenningsGraphPage(TEST_ASSAY_LUM, "Standard1");

        _guideSetHelper.setUpLeveyJenningsGraphParams(LuminexGuideSetHelper.GUIDE_SET_ANALYTE_NAMES[0]);
        clickButtonContainingText("Edit", 0);
        metricInputs.put("EC505PLAverage", 325.0);
        metricInputs.put("EC505PLStdDev", 35.0);
        metricInputs.put("AUCAverage", 7800.0);
        metricInputs.put("MaxFIAverage", 10200.0);
        _guideSetHelper.editValueBasedGuideSet(metricInputs, "Analyte 1", false);

        _guideSetHelper.setUpLeveyJenningsGraphParams(LuminexGuideSetHelper.GUIDE_SET_ANALYTE_NAMES[1]);
        _guideSetHelper.createGuideSet(false);
        metricInputs.put("EC504PLAverage", 42158.22);
        metricInputs.put("EC504PLStdDev", 4833.76);
        metricInputs.put("EC505PLAverage", 40987.31);
        metricInputs.put("EC505PLStdDev", 4280.84);
        metricInputs.put("AUCAverage", 85268.04);
        metricInputs.put("AUCStdDev", 738.55);
        metricInputs.put("MaxFIAverage", 32507.27);
        metricInputs.put("MaxFIStdDev", 189.83);
        String guideSetComment = "New Analyte 2";
        _guideSetHelper.editValueBasedGuideSet(metricInputs, guideSetComment, true);
        _guideSetHelper.applyGuideSetToRun(new String[]{"NETWORK2", "NETWORK3", "NETWORK4", "NETWORK5"}, guideSetComment, true);
    }
}
