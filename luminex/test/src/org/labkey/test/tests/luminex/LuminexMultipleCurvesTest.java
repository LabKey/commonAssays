/*
 * Copyright (c) 2011-2017 LabKey Corporation
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

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.openqa.selenium.WebElement;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertTrue;

@Category({DailyA.class, Assays.class})
public final class LuminexMultipleCurvesTest extends LuminexTest
{
    /**
     * Test our ability to upload multiple files and set multiple standards
     *
     */
    @Test
    public void testMultipleCurve() throws Exception
    {
        String name = startCreateMultipleCurveAssayRun();

        String[] standardsNames = {"Standard1", "Standard2"};
        checkStandardsCheckBoxesExist(standardsNames);
        checkQCControlCheckBoxesExist(standardsNames);
        checkOtherControlCheckboxesExist(standardsNames);

        WellRole[] std1Roles = {WellRole.OTHER_CONTROL};
        //selectRoleCheckboxesForStandard("Standard1", std1Roles);

        String[] possibleAnalytes = getListOfAnalytesMultipleCurveData();
        String[] possibleStandards = new String[]{"Standard2", "Standard1"};
        String[] possibleRoles = new String[]{"Standard", "QC Control", "Other Control"};

        Map<String, Set<String>> analytesAndStandardsConfig = generateAnalytesAndStandardsConfig(possibleAnalytes, possibleStandards);
        configureStandardsForAnalytes(analytesAndStandardsConfig, possibleStandards);

        clickButton("Save and Finish", longWaitForPage);
        WebElement runLink = Locator.linkWithText(name).findElement(getDriver());
        int runId = Integer.parseInt(WebTestHelper.parseUrlQuery(new URL(runLink.getAttribute("href"))).get("runId"));
        clickAndWait(runLink);

        //edit view to show Analyte Standard
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("Analyte/Standard");
        _customizeViewsHelper.addColumn("Analyte/StdCurve");
        _customizeViewsHelper.addColumn("Analyte/FitProb");
        _customizeViewsHelper.addColumn("Analyte/ResVar");
        _customizeViewsHelper.saveCustomView();
        waitForText("Std Curve");

        // We're OK with grabbing the footer curve fit from any of the files, under normal usage they should all share
        // the same curve fits
        assertTrue("BioPlex curve fit for ENV6 (97) in plate 1, 2, or 3",
                isTextPresent("FI = 0.465914 + (1.5417E+006 - 0.465914) / ((1 + (Conc / 122.733)^-0.173373))^7.64039") ||
                        isTextPresent("FI = 0.582906 + (167.081 - 0.582906) / ((1 + (Conc / 0.531813)^-5.30023))^0.1"));
        assertTrue("BioPlex FitProb for ENV6 (97) in plate 1, 2, or 3", isTextPresent("0.9667") || isTextPresent("0.4790"));
        assertTrue("BioPlex ResVar for ENV6 (97) in plate 1, 2, 3", isTextPresent("0.1895") || isTextPresent("0.8266"));

        assertAnalytesHaveCorrectStandards(TEST_ASSAY_LUM, runId, analytesAndStandardsConfig);

        // Go to the schema browser to check out the parsed curve fits
        goToSchemaBrowser();
        selectQuery("assay.Luminex." + TEST_ASSAY_LUM, "CurveFit");
        waitForText("view data");
        clickAndWait(Locator.linkContainingText("view data"));

        // We're OK with grabbing the footer curve fit from any of the files, under normal usage they should all share
        // the same curve fits
        assertTrue("BioPlex curve fit parameter for ENV6 (97) in plate 1, 2, or 3", isTextPresent("0.465914") || isTextPresent("0.582906"));
        assertTrue("BioPlex curve fit parameter for ENV6 (97) in plate 1, 2, or 3", isTextPresent("7.64039") || isTextPresent("0.1"));


        //test with one standard, and one other control
        Map<String, WellRole[]> testRoles = new HashMap<>();
        testRoles.put("Standard1", new WellRole[]{WellRole.STANDARD});
        testRoles.put("Standard2", new WellRole[]{WellRole.OTHER_CONTROL});
        reImportData(testRoles);

        goToQCAnalysisPage(TEST_ASSAY_LUM, "view titration qc report");
        //standard role should be present in qc report
        assertTextPresent("Standard1");
        //other role should not be present in qc report
        assertTextNotPresent("Standard2");

        //test with multiple roles, both should show up on QC report since they have role other than 'other'
        testRoles.clear();
        testRoles.put("Standard1", new WellRole[]{WellRole.STANDARD, WellRole.OTHER_CONTROL});
        testRoles.put("Standard2", new WellRole[]{WellRole.QC_CONTROL, WellRole.OTHER_CONTROL});
        reImportData(testRoles);
        goToQCAnalysisPage(TEST_ASSAY_LUM, "view titration qc report");
        //both roles should be present in qc report
        assertTextPresent("Standard1", "Standard2");

    }

    /**
     * Verify that the "set this  as standard" checkboxes exist and can be checked for the given standard names
     * preconditions:  at analyte properties page
     * postconditions:  unchanged
     * @param standardsNames
     */
    private void checkStandardsCheckBoxesExist(String[] standardsNames)
    {
        for(int i=0; i<standardsNames.length; i++)
        {
            String s = standardsNames[i];
            Locator l = Locator.checkboxByName("_titrationRole_standard_"+s);
            checkCheckbox(l);
            assertChecked(l);
        }
    }

    @LogMethod
    private void reImportData(Map<String, WellRole[]> wellRoleMap)
    {
        goToTestAssayHome();
        click(Locator.linkContainingText(MULTIPLE_CURVE_ASSAY_RUN_NAME));
            clickButtonContainingText("Re-import run");
            checkCheckbox(Locator.radioButtonByNameAndValue("participantVisitResolver", "SampleInfo"));
            clickButtonContainingText("Next");
            setFormElement(Locator.name(ASSAY_ID_FIELD), MULTIPLE_CURVE_ASSAY_RUN_NAME);
            clickButtonContainingText("Next");
        for(String desc : wellRoleMap.keySet())
        {
            selectRoleCheckboxesForStandard(desc, wellRoleMap.get(desc));
        }
        clickButtonContainingText("Save and Finish");
    }

    private void checkQCControlCheckBoxesExist(String[] standardsNames)
    {
        for(int i=0; i<standardsNames.length; i++)
        {
            String s = standardsNames[i];
            Locator l = Locator.checkboxByName("_titrationRole_qccontrol_"+s);
            checkCheckbox(l);
            assertChecked(l);
            uncheckCheckbox(l);
        }
    }

    private void checkOtherControlCheckboxesExist(String[] standardsNames)
    {
        for(int i=0; i<standardsNames.length; i++)
        {
            String s = standardsNames[i];
            Locator l = Locator.checkboxByName("_titrationRole_othercontrol_"+s);
            checkCheckbox(l);
            assertChecked(l);
            uncheckCheckbox(l);
        }
    }

    private void selectRoleCheckboxesForStandard(String desc, WellRole[] selectedRoles)
    {
        WellRole[] unselectedRoles = validRoles;
        for(WellRole role : selectedRoles)
        {
            unselectedRoles = ArrayUtils.removeElement(unselectedRoles, role);
        }
        for (WellRole role : validRoles)
        {
            Locator loc = getWellRoleCheckboxLoc(desc, role);
            if(ArrayUtils.contains(unselectedRoles, role))
                //unselect if undesired role is selected
                uncheckCheckbox(loc);
            else
                //is desired role, select if unselected
                checkCheckbox(loc);
        }
    }

    /**
     * using the list of analytes and standards, select one or more standards for each analyte.  Expects at least three
     * analytes and exactly two standards, and returns two analytes using different standards and the rest using both.
     * This is based on the test data we're currently using, can be changed to accommodate future changes
     *
     * preconditions: none, does not interact with server
     *
     * @param possibleAnalytes list of possible analytes
     * @param possibleStandards list of possible standards
     * @return map.  Key is the name of the analyte, value is a set of standards to be used for that analyte
     */
    private Map<String,Set<String>> generateAnalytesAndStandardsConfig(String[] possibleAnalytes, String[] possibleStandards)
    {
        Map<String, Set<String>> analytesAndStandardsConfig =  new HashMap<>();

        //based on the assumption that there are five analytes and two possible standards:  update this if you need to test for more
        Set<String> firstStandard = new HashSet<>(); firstStandard.add(possibleStandards[0]);
        Set<String> secondStandard = new HashSet<>(); secondStandard.add(possibleStandards[1]);
        Set<String> bothStandard = new HashSet<>();
        bothStandard.add(possibleStandards[0]);
        bothStandard.add(possibleStandards[1]);

        analytesAndStandardsConfig.put(possibleAnalytes[0], bothStandard);
        analytesAndStandardsConfig.put(possibleAnalytes[1], firstStandard);
        analytesAndStandardsConfig.put(possibleAnalytes[2], firstStandard);
        analytesAndStandardsConfig.put(possibleAnalytes[3], secondStandard);
        analytesAndStandardsConfig.put(possibleAnalytes[4], secondStandard);

        return analytesAndStandardsConfig;
    }

    /**
     * based on the instructions endcoded in the map, select the specified standards for each analyte
     *
     * preconditions:  on multiple curve data page.  analytes and standards must exist
     * postconditions: given check boxes checked and unchecked
     *
     * @param analytesAndTheirStandards map, where the keys are the analyte names and the values are sets of standard names,
     *      corresponding to the standards that should be used for the analyte.
     * @param standardsList list of all possible standards.  Important so that we know which boxes to uncheck when
     *      configuring which standard dto use
     */
    private void configureStandardsForAnalytes(Map<String, Set<String>> analytesAndTheirStandards, String[] standardsList)
    {
        Set<String> analytes = analytesAndTheirStandards.keySet();

        for(String analyte : analytes)
        {
            Set<String> analyteStandards = analytesAndTheirStandards.get(analyte);
            for(String standard: standardsList)
            {
                if(analyteStandards.contains(standard))
                    setAnalyteAndStandardCheckBox(analyte, standard, true);
                else
                    setAnalyteAndStandardCheckBox(analyte, standard, false);
            }
        }
    }

    /**
     * check or uncheck the checkbox for the given standard and analyte
     */
    @LogMethod (quiet = true)
    private void setAnalyteAndStandardCheckBox(@LoggedParam String analyte, @LoggedParam String standard, boolean check)
    {
        Locator.XPathLocator checkBoxLocator = Locator.checkboxByName("titration_" + analyte + "_" + standard);
        WebElement checkbox = checkBoxLocator.findElement(getDriver());

        if(check)
        {
            checkCheckbox(checkbox);
        }
        else
        {
            if(checkbox.isDisplayed())
            {
                //not all checkboxes are visible for all plates
                uncheckCheckbox(checkbox);
            }
        }
    }

    //check to ensure that either an EC50 value exists, or failure flag is set
    private void sanityCheckEC50Values()
    {

    }

    private enum WellRole {STANDARD,QC_CONTROL,OTHER_CONTROL}

    private WellRole[] validRoles = {WellRole.STANDARD, WellRole.QC_CONTROL, WellRole.OTHER_CONTROL};

    private Locator getWellRoleCheckboxLoc(String description, WellRole role)
    {
        Locator loc = null;
        switch(role)
        {
            case STANDARD:
                loc = Locator.checkboxByName("_titrationRole_standard_"+ description);
                break;
            case QC_CONTROL:
                loc = Locator.checkboxByName("_titrationRole_qccontrol_"+ description);
                break;
            case OTHER_CONTROL:
                loc = Locator.checkboxByName("_titrationRole_othercontrol_"+description);
                break;
        }
        return loc;
    }

    private Set<Locator> getAllWellRoleLocForDesc(String description)
    {
        Set<Locator> locs = new HashSet<>();
        locs.add(getWellRoleCheckboxLoc(description, WellRole.OTHER_CONTROL));
        locs.add(getWellRoleCheckboxLoc(description, WellRole.QC_CONTROL));
        locs.add(getWellRoleCheckboxLoc(description, WellRole.STANDARD));
        return locs;
    }
}
