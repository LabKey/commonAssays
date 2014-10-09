/*
 * Copyright (c) 2007-2014 LabKey Corporation
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
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.pages.AssayDomainEditor;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.QCAssayScriptHelper;
import org.labkey.test.util.RReportHelper;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.labkey.test.util.ListHelper.ListColumnType;

import static org.junit.Assert.*;

public abstract class LuminexTest extends BaseWebDriverTest
{
    protected final static String TEST_ASSAY_PRJ_LUMINEX = "LuminexTest Project";            //project for luminex test

    public static final String TEST_ASSAY_LUM =  "&TestAssayLuminex></% 1";// put back TRICKY_CHARACTERS_NO_QUOTES when issue 20061 is resolved
    protected static final String TEST_ASSAY_LUM_DESC = "Description for Luminex assay";

    protected static final String TEST_ASSAY_XAR_NAME = "TestLuminexAssay";
    public static final File TEST_ASSAY_XAR_FILE = TestFileUtils.getSampleData("Luminex/" + TEST_ASSAY_XAR_NAME + ".xar");

    protected static final String TEST_ASSAY_LUM_SET_PROP_SPECIES = "testSpecies1";
    public static final File TEST_ASSAY_LUM_FILE1 = TestFileUtils.getSampleData("Luminex/10JAN07_plate_1.xls");
    public static final File TEST_ASSAY_LUM_FILE2 = TestFileUtils.getSampleData("Luminex/pnLINCO20070302A.xlsx");
    public static final File TEST_ASSAY_LUM_FILE3 = TestFileUtils.getSampleData("Luminex/WithIndices.xls");
    public static final File TEST_ASSAY_LUM_FILE4 = TestFileUtils.getSampleData("Luminex/WithAltNegativeBead.xls");
    public static final File TEST_ASSAY_LUM_FILE5 = TestFileUtils.getSampleData("Luminex/Guide Set plate 1.xls");
    public static final File TEST_ASSAY_LUM_FILE6 = TestFileUtils.getSampleData("Luminex/Guide Set plate 2.xls");
    public static final File TEST_ASSAY_LUM_FILE7 = TestFileUtils.getSampleData("Luminex/Guide Set plate 3.xls");
    public static final File TEST_ASSAY_LUM_FILE8 = TestFileUtils.getSampleData("Luminex/Guide Set plate 4.xls");
    public static final File TEST_ASSAY_LUM_FILE9 = TestFileUtils.getSampleData("Luminex/Guide Set plate 5.xls");
    public static final File TEST_ASSAY_LUM_FILE10 = TestFileUtils.getSampleData("Luminex/RawAndSummary.xlsx");
    public static final File TEST_ASSAY_LUM_FILE11 = TestFileUtils.getSampleData("Luminex/PositivityWithBaseline.xls");
    public static final File TEST_ASSAY_LUM_FILE12 = TestFileUtils.getSampleData("Luminex/PositivityWithoutBaseline.xls");
    public static final File TEST_ASSAY_LUM_FILE13 = TestFileUtils.getSampleData("Luminex/PositivityThreshold.xls");

    public static final File TEST_ASSAY_MULTIPLE_STANDARDS_1 = TestFileUtils.getSampleData("Luminex/plate 1_IgA-Biot (Standard2).xls");
    public static final File TEST_ASSAY_MULTIPLE_STANDARDS_2 = TestFileUtils.getSampleData("Luminex/plate 2_IgA-Biot (Standard2).xls");
    public static final File TEST_ASSAY_MULTIPLE_STANDARDS_3 = TestFileUtils.getSampleData("Luminex/plate 3_IgA-Biot (Standard1).xls");

    public static final File RTRANSFORM_SCRIPT_FILE_LABKEY = new File(TestFileUtils.getLabKeyRoot(), "server/modules/luminex/resources/transformscripts/labkey_luminex_transform.R");
    public static final File RTRANSFORM_SCRIPT_FILE_LAB =  new File(TestFileUtils.getLabKeyRoot(), "server/modules/luminex/resources/transformscripts/tomaras_luminex_transform.R");

    public static final String ASSAY_ID_FIELD  = "name";
    public static final String ASSAY_DATA_FILE_LOCATION_MULTIPLE_FIELD = "__primaryFile__";

    public static final String DATA_TABLE_NAME = "Data";
    protected static final String EXCLUDE_COMMENT_FIELD = "comment";
    protected static final String MULTIPLE_CURVE_ASSAY_RUN_NAME = "multipleCurvesTestRun";
    protected static final String SAVE_CHANGES_BUTTON = "Save";

    public static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    public static final String isotype = "IgG ></% 1";// put back TRICKY_CHARACTERS_NO_QUOTES when issue 20061 is resolved
    public static final String conjugate = "PE ></% 1";// put back TRICKY_CHARACTERS_NO_QUOTES when issue 20061 is resolved


    public List<String> getAssociatedModules()
    {
        return Arrays.asList("luminex");
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return TEST_ASSAY_PRJ_LUMINEX;
    }

    protected boolean useXarImport()
    {
        return true;
    }

    @BeforeClass
    public static void initTest()
    {
        LuminexTest init = (LuminexTest)getCurrentTest();

        init.doInit();
    }

    @LogMethod
    private void doInit()
    {
        // setup a scripting engine to run a java transform script
        QCAssayScriptHelper javaEngine = new QCAssayScriptHelper(this);
        javaEngine.ensureEngineConfig();
        javaEngine.createNetrcFile();

        // fail fast if R is not configured
        RReportHelper _rReportHelper = new RReportHelper(this);
        _rReportHelper.ensureRConfig();

        log("Testing Luminex Assay Designer");
        //create a new test project
        _containerHelper.createProject(getProjectName(), "Study");
        createDefaultStudy();

        //create a study within this project to which we will publish
        goToProjectHome();
        PortalHelper portalHelper = new PortalHelper(this);
        //add the Assay List web part so we can create a new luminex assay
        portalHelper.addWebPart("Assay List");


        if (useXarImport())
        {
            // import the assay design from the XAR file
            _assayHelper.uploadXarFileAsAssayDesign(TEST_ASSAY_XAR_FILE, 1);
            // since we want to test special characters in the assay name, copy the assay design to rename
            goToManageAssays();
            clickAndWait(Locator.linkWithText(TEST_ASSAY_XAR_NAME));
            _assayHelper.copyAssayDesign();
            AssayDomainEditor assayDesigner = new AssayDomainEditor(this);
            assayDesigner.setName(TEST_ASSAY_LUM);
            assayDesigner.setDescription(TEST_ASSAY_LUM_DESC);
            assayDesigner.saveAndClose();
        }
        else
        {
            //create a new luminex assay
            clickButton("Manage Assays");
            clickButton("New Assay Design");

            checkCheckbox(Locator.radioButtonByNameAndValue("providerName", "Luminex"));
            clickButton("Next");

            log("Setting up Luminex assay");

            AssayDomainEditor assayDesigner = new AssayDomainEditor(this);
            assayDesigner.setName(TEST_ASSAY_LUM);
            assayDesigner.setDescription(TEST_ASSAY_LUM_DESC);

            // add batch properties for transform and Ruminex version numbers
            _listHelper.addField("Batch Fields", 5, "Network", "Network", ListColumnType.String);
            _listHelper.addField("Batch Fields", 6, "TransformVersion", "Transform Script Version", ListColumnType.String);
            _listHelper.addField("Batch Fields", 7, "LabTransformVersion", "Lab Transform Script Version", ListColumnType.String);
            _listHelper.addField("Batch Fields", 8, "RuminexVersion", "Ruminex Version", ListColumnType.String);
            _listHelper.addField("Batch Fields", 9, "RVersion", "R Version", ListColumnType.String);

            // add run properties for designation of which field to use for curve fit calc in transform
            _listHelper.addField("Run Fields", 8, "SubtNegativeFromAll", "Subtract Negative Bead from All Wells", ListColumnType.Boolean);
            _listHelper.addField("Run Fields", 9, "StndCurveFitInput", "Input Var for Curve Fit Calc of Standards", ListColumnType.String);
            _listHelper.addField("Run Fields", 10, "UnkCurveFitInput", "Input Var for Curve Fit Calc of Unknowns", ListColumnType.String);
            _listHelper.addField("Run Fields", 11, "CurveFitLogTransform", "Curve Fit Log Transform", ListColumnType.Boolean);
            _listHelper.addField("Run Fields", 12, "SkipRumiCalculation", "Skip Ruminex Calculations", ListColumnType.Boolean);

            // add run properties for use with the Guide Set test
            _listHelper.addField("Run Fields", 13, "NotebookNo", "Notebook Number", ListColumnType.String);
            _listHelper.addField("Run Fields", 14, "AssayType", "Assay Type", ListColumnType.String);
            _listHelper.addField("Run Fields", 15, "ExpPerformer", "Experiment Performer", ListColumnType.String);

            // add run properties for use with Calculating Positivity
            _listHelper.addField("Run Fields", 16, "CalculatePositivity", "Calculate Positivity", ListColumnType.Boolean);
            _listHelper.addField("Run Fields", 17, "BaseVisit", "Baseline Visit", ListColumnType.Double);
            _listHelper.addField("Run Fields", 18, "PositivityFoldChange", "Positivity Fold Change", ListColumnType.Integer);

            // add analyte property for tracking lot number
            _listHelper.addField("Analyte Properties", 6, "LotNumber", "Lot Number", ListColumnType.String);
            _listHelper.addField("Analyte Properties", 7, "NegativeControl", "Negative Control", ListColumnType.Boolean);

            // add the data properties for the calculated columns
            _listHelper.addField("Data Fields", 0, "FIBackgroundNegative", "FI-Bkgd-Neg", ListColumnType.Double);
            _listHelper.addField("Data Fields", 1, "Standard", "Stnd for Calc", ListColumnType.String);
            _listHelper.addField("Data Fields", 2, "EstLogConc_5pl", "Est Log Conc Rumi 5 PL", ListColumnType.Double);
            _listHelper.addField("Data Fields", 3, "EstConc_5pl", "Est Conc Rumi 5 PL", ListColumnType.Double);
            _listHelper.addField("Data Fields", 4, "SE_5pl", "SE Rumi 5 PL", ListColumnType.Double);
            _listHelper.addField("Data Fields", 5, "EstLogConc_4pl", "Est Log Conc Rumi 4 PL", ListColumnType.Double);
            _listHelper.addField("Data Fields", 6, "EstConc_4pl", "Est Conc Rumi 4 PL", ListColumnType.Double);
            _listHelper.addField("Data Fields", 7, "SE_4pl", "SE Rumi 4 PL", ListColumnType.Double);
            _listHelper.addField("Data Fields", 8, "Slope_4pl", "Slope_4pl", ListColumnType.Double);
            _listHelper.addField("Data Fields", 9, "Lower_4pl", "Lower_4pl", ListColumnType.Double);
            _listHelper.addField("Data Fields", 10, "Upper_4pl", "Upper_4pl", ListColumnType.Double);
            _listHelper.addField("Data Fields", 11, "Inflection_4pl", "Inflection_4pl", ListColumnType.Double);
            _listHelper.addField("Data Fields", 12, "Slope_5pl", "Slope_5pl", ListColumnType.Double);
            _listHelper.addField("Data Fields", 13, "Lower_5pl", "Lower_5pl", ListColumnType.Double);
            _listHelper.addField("Data Fields", 14, "Upper_5pl", "Upper_5pl", ListColumnType.Double);
            _listHelper.addField("Data Fields", 15, "Inflection_5pl", "Inflection_5pl", ListColumnType.Double);
            _listHelper.addField("Data Fields", 16, "Asymmetry_5pl", "Asymmetry_5pl", ListColumnType.Double);
            _listHelper.addField("Data Fields", 17, "Positivity", "Positivity", ListColumnType.String);


            // set format to two decimal place for easier testing later
            setFormat("Data Fields", 0, "0.0");
            setFormat("Data Fields", 2, "0.0");
            setFormat("Data Fields", 3, "0.0");
            setFormat("Data Fields", 4, "0.0");
            setFormat("Data Fields", 5, "0.0");
            setFormat("Data Fields", 6, "0.0");
            setFormat("Data Fields", 7, "0.0");
            setFormat("Data Fields", 8, "0.0");
            setFormat("Data Fields", 9, "0.0");
            setFormat("Data Fields", 10, "0.0");
            setFormat("Data Fields", 11, "0.0");
            setFormat("Data Fields", 12, "0.0");
            setFormat("Data Fields", 13, "0.0");
            setFormat("Data Fields", 14, "0.0");
            setFormat("Data Fields", 15, "0.0");
            setFormat("Data Fields", 16, "0.0");

            assayDesigner.saveAndClose();

            // remove the SpecimenID field from the results grid to speed up the test
//            clickAndWait(Locator.linkWithText("Assay List"));
//            clickAndWait(Locator.linkWithText(TEST_ASSAY_LUM));
//            clickAndWait(Locator.linkWithText("view results"));
//            _customizeViewsHelper.openCustomizeViewPanel();
//            _customizeViewsHelper.removeCustomizeViewColumn("SpecimenID");
//            _customizeViewsHelper.saveDefaultView();
        }
    }

    public void excludeAnalyteForRun(String analyte, boolean exclude, String comment)
    {
        clickButtonContainingText("Exclude Analytes", 0);
        _extHelper.waitForExtDialog("Exclude Analytes from Analysis");
        if (!exclude)
            waitForText("Uncheck analytes to remove exclusions");

        clickExcludeAnalyteCheckBox(analyte);
        setFormElement(Locator.id(EXCLUDE_COMMENT_FIELD), comment);
        waitForElement(Locator.xpath("//table[@id='saveBtn' and not(contains(@class, 'disabled'))]"), WAIT_FOR_JAVASCRIPT);

        if (!exclude)
        {
            clickButton(SAVE_CHANGES_BUTTON, 0);
            _extHelper.waitForExtDialog("Warning");
            _extHelper.clickExtButton("Warning", "Yes", 2 * defaultWaitForPage);
        }
        else
        {
            clickButton(SAVE_CHANGES_BUTTON, 2 * defaultWaitForPage);
        }
    }

    /**
     * click on the exclusion icon associated with the particular well
     * preconditions:  at Test Result page
     * postconditions: at Test Result Page with exclude Replicate Group From Analysis window up
     * @param wellName
     */
    protected void clickExclusionMenuIconForWell(String wellName)
    {
        waitAndClick(Locator.id("__changeExclusions__" + wellName));
        _extHelper.waitForExtDialog("Exclude Replicate Group from Analysis");
        waitForElement(Locator.xpath("//table[@id='saveBtn' and not(contains(@class, 'disabled'))]"), WAIT_FOR_JAVASCRIPT);
    }

    protected void clickExcludeAnalyteCheckBox(String analyte)
    {
        Locator l = ExtHelper.locateGridRowCheckbox(analyte);
        waitAndClick(l);
    }

    protected String[] getListOfAnalytesMultipleCurveData()
    {
        //TODO:  make this a dynamic list, acquired from the current data set, rather than hardcoded
        return new String[] {"ENV6 (97)", "ENV7 (93)", "ENV4 (26)",
                        "ENV5 (58)", "Blank (53)"};
    }


    protected String startCreateMultipleCurveAssayRun()
    {
        log("Creating test run with multiple standard curves");
        String name = MULTIPLE_CURVE_ASSAY_RUN_NAME;

        createNewAssayRun(TEST_ASSAY_LUM, name);

        uploadMultipleCurveData();

        return name;

    }

    protected void compareColumnValuesAgainstExpected(String column1, String column2, Map<String, Set<String>> column1toColumn2)
    {
        Set<String> set = new HashSet<>();
        set.add(column2);
        column1toColumn2.put(column1, set); //column headers

        List<List<String>> columnVals = getColumnValues(DATA_TABLE_NAME, column1, column2);

        assertStandardsMatchExpected(columnVals, column1toColumn2);
    }

    /**
     *
     * @param columnVals two lists of equal length, with corresponding names of analytes and the standards applied to them
     * @param col1to2Map map of analyte names to the standards that should be applied to them.
     */
    private void assertStandardsMatchExpected( List<List<String>> columnVals, Map<String, Set<String>> col1to2Map)
    {
        String column1Val;
        String column2Val;
        while(columnVals.get(0).size()>0)
        {
            column1Val = columnVals.get(0).remove(0);
            column2Val = columnVals.get(1).remove(0);
            assertStandardsMatchExpected(column1Val, column2Val, col1to2Map);
        }
    }

    /**
     *
     * @param column1Val name of analyte
     * @param column2Val standard applied to analyte on server
     * @param colum1toColumn2Map map of all analytes to the appropriate standards
     */
    private void assertStandardsMatchExpected(String column1Val, String column2Val, Map<String, Set<String>> colum1toColumn2Map)
    {
        String[] splitCol2Val = column2Val.split(",");
        Set<String> expectedCol2Vals = colum1toColumn2Map.get(column1Val);
        if(expectedCol2Vals!=null)
        {
            try
            {
                assertEquals(splitCol2Val.length, expectedCol2Vals.size());

                for(String s: splitCol2Val)
                {
                    s = s.trim();
                    assertTrue("Expected " + expectedCol2Vals + " to contain" + s, expectedCol2Vals.contains(s));
                }
            }
            catch (Exception rethrow)
            {
                log("Column1: " + column1Val);
                log("Expected Column2: " + expectedCol2Vals);
                log("Column2: " + column2Val);

                throw rethrow;
            }
        }
    }

    /**
     * upload the three files used for the multiple curve data test
     * preconditions:  at assay run data import page
     * postconditions: at data import: analyte properties page
     */
    protected void uploadMultipleCurveData()
    {
        addFilesToAssayRun(TEST_ASSAY_MULTIPLE_STANDARDS_1, TEST_ASSAY_MULTIPLE_STANDARDS_2, TEST_ASSAY_MULTIPLE_STANDARDS_3);
        clickButton("Next");
        setNegativeBeads("Blank ");
    }

    private void setNegativeBeads(String negBeadPrefix)
    {
        String[] analytes = getListOfAnalytesMultipleCurveData();
        String blankAnalyte = null;
        for (String analyte : analytes)
        {
            if (analyte.startsWith(negBeadPrefix))
            {
                blankAnalyte = analyte;
                break;
            }
        }
        checkCheckbox(Locator.name("_analyte_" + blankAnalyte + "_NegativeControl"));
        for (String analyte : analytes)
        {
            if (!analyte.equals(blankAnalyte))
                selectOptionByText(Locator.name("_analyte_" + analyte + "_NegativeBead"), blankAnalyte);
        }
    }

    /**several tests use this data.  Rather that clean and import for each
     * or take an unnecessary dependency of one to the other, this function
     * checks if the data is already present and, if it is not, adds it
     * preconditions:  Project TEST_ASSAY_PRJ_LUMINEX with Assay  TEST_ASSAY_LUM exists
     * postconditions:  assay run
     */
    protected void ensureMultipleCurveDataPresent(String assayName)
    {
        goToTestAssayHome(assayName);

        if(!isTextPresent(MULTIPLE_CURVE_ASSAY_RUN_NAME)) //right now this is a good enough check.  May have to be
        // more rigorous if tests start substantially altering data
        {
            log("multiple curve data not present, adding now");
            startCreateMultipleCurveAssayRun();
            clickButton("Save and Finish");
        }
    }

    protected void addFilesToAssayRun(File firstFile, File... additionalFiles)
    {
        setFormElement(Locator.name(ASSAY_DATA_FILE_LOCATION_MULTIPLE_FIELD), firstFile);

        int index = 1;
        for (File additionalFile : additionalFiles)
        {
            sleep(500);
            click(Locator.xpath("//a[contains(@class, 'labkey-file-add-icon-enabled')]"));

            String fieldName = ASSAY_DATA_FILE_LOCATION_MULTIPLE_FIELD + (index++);
            setFormElement(Locator.name(fieldName), additionalFile);
        }
    }

    /**
     * preconditions:  can see Project Folder, assay already exists
     * postconditions: at data import screen for new test run
     */
    protected void createNewAssayRun(String assayName, String runId)
    {
        goToTestAssayHome(assayName);
        clickButtonContainingText("Import Data");
        checkCheckbox(Locator.radioButtonByNameAndValue("participantVisitResolver", "SampleInfo"));
        clickButtonContainingText("Next");
        setFormElement(Locator.name(ASSAY_ID_FIELD), runId);
    }

    public void goToQCAnalysisPage(String assayName, String submenuText)
    {
        goToTestAssayHome(assayName);

        _extHelper.clickExtMenuButton(true, Locator.xpath("//a[text() = 'view qc report']"), submenuText);
    }

    /**
     * Cleanup entry point.
     * @param afterTest
     */
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);

        QCAssayScriptHelper javaEngine = new QCAssayScriptHelper(this);
        javaEngine.deleteEngine();
    } //doCleanup()

    //helper function to go to test assay home from anywhere the project link is visible
    public void goToTestAssayHome(String assayName)
    {
        if (!isTextPresent(assayName + " Runs"))
        {
            goToProjectHome();
            clickAndWait(Locator.linkWithText(assayName));
        }
    }

    public void goToTestAssayHome()
    {
        goToTestAssayHome(TEST_ASSAY_LUM);
    }

    private void setFormat(String where, int index, String formatStr)
    {
        String prefix = getPropertyXPath(where);
        _listHelper.clickRow(prefix, index);
        click(Locator.xpath(prefix + "//span[contains(@class,'x-tab-strip-text') and text()='Format']"));
        setFormElement(Locator.id("propertyFormat"), formatStr);
    }

    public void importLuminexRunPageTwo(String name, String isotype, String conjugate, String stndCurveFitInput,
                                           String unkCurveFitInput, String notebookNo, String assayType, String expPerformer,
                                           String testDate, File file, int i)
    {
        importLuminexRunPageTwo(name, isotype, conjugate, stndCurveFitInput, unkCurveFitInput, notebookNo, assayType, expPerformer, testDate, file, i, false);
    }

    protected void importLuminexRunPageTwo(String runId, String isotype, String conjugate, String stndCurveFitInput,
                                           String unkCurveFitInput, String notebookNo, String assayType, String expPerformer,
                                           String testDate, File file, int i, boolean expectDuplicateFile)
    {
            setFormElement(Locator.name("name"), runId);
            setFormElement(Locator.name("isotype"), isotype);
            setFormElement(Locator.name("conjugate"), conjugate);
            setFormElement(Locator.name("stndCurveFitInput"), stndCurveFitInput);
            setFormElement(Locator.name("unkCurveFitInput"), unkCurveFitInput);
            uncheckCheckbox(Locator.name("curveFitLogTransform"));
            setFormElement(Locator.name("notebookNo"), notebookNo);
            setFormElement(Locator.name("assayType"), assayType);
            setFormElement(Locator.name("expPerformer"), expPerformer);
            setFormElement(Locator.name("testDate"), testDate);
            setFormElement(Locator.name("__primaryFile__"), file);

            if (expectDuplicateFile)
                waitForText("A file with name '" + file.getName() + "' already exists");

            clickButton("Next", 60000);
    }

    @LogMethod
    protected void createAndImpersonateUser(String user, String perms)
    {
        goToHome();
        createUser(user, null, false);
        goToProjectHome();
        _permissionsHelper.setUserPermissions(user, perms);
        impersonate(user);
    }


    @LogMethod(quiet = true)
    public void uploadPositivityFile(String assayName, @LoggedParam String assayRunId, @LoggedParam File file, String baseVisit, String foldChange, boolean isBackgroundUpload, boolean expectDuplicateFile)
    {
        createNewAssayRun(assayName, assayRunId);
        checkCheckbox(Locator.name("calculatePositivity"));
        setFormElement(Locator.name("baseVisit"), baseVisit);
        setFormElement(Locator.name("positivityFoldChange"), foldChange);
        selectPositivityFile(file, expectDuplicateFile);
        setAnalytePropertyValues();
        finishUploadPositivityFile(assayRunId, isBackgroundUpload);
    }

    public void finishUploadPositivityFile(String assayRunId, boolean isBackgroundUpload)
    {
        clickButton("Save and Finish");
        if (!isBackgroundUpload && !isElementPresent(Locator.css(".labkey-error").containing("Error: ")))
            clickAndWait(Locator.linkWithText(assayRunId), 2 * WAIT_FOR_PAGE);
    }

    public void selectPositivityFile(File file, boolean expectDuplicateFile)
    {
        assertTrue("Positivity Data absent: " + file.toString(), file.exists());
        setFormElement(Locator.name("__primaryFile__"), file);
        if (expectDuplicateFile)
            waitForText("A file with name '" + file.getName() + "' already exists");
        clickButton("Next");
    }

    protected void setAnalytePropertyValues()
    {
        // no op, currently used by LuminexPositivityTest
    }

    protected void verifyQCFlags(String assayName, String analyteName, String[] expectedFlags)
    {
        goToTestAssayHome(assayName);

        //add QC flag colum
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("QCFlags");
        _customizeViewsHelper.saveCustomView();

        //verify expected values in column
        List<String> var = getColumnValues("Runs", "QC Flags").get(0);
        String[] flags = var.toArray(new String[var.size()]);
        for(int i=0; i<flags.length; i++)
        {
            assertEquals(expectedFlags[i], flags[i].trim());
        }

        verifyQCFlagLink(analyteName, expectedFlags[0]);
    }

    @LogMethod
    private void verifyQCFlagLink(String analyteName, String expectedFlag)
    {
        click(Locator.linkContainingText(expectedFlag, 0));
        _extHelper.waitForExt3Mask(WAIT_FOR_JAVASCRIPT);
        sleep(1500);
        assertTextPresent("CV", 4); // 3 occurances of PCV and 1 of %CV

        //verify text is in expected form
        waitForText("Standard1 " + analyteName + " - " + isotype + " " + conjugate + " under threshold for AUC");

        //verify unchecking a box  removes the flag
        Locator aucCheckBox = Locator.xpath("//div[text()='AUC']/../../td/div/div[contains(@class, 'check')]");
        click(aucCheckBox);
        clickButton("Save", 0);
        _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);

        Locator strikeoutAUC = Locator.xpath("//span[contains(@style, 'line-through') and  text()='AUC']");
        waitForElement(strikeoutAUC);

        //verify rechecking a box adds the flag back
        click(strikeoutAUC);
        _extHelper.waitForExt3Mask(WAIT_FOR_JAVASCRIPT);
        waitAndClick(aucCheckBox);
        clickButton("Save", 0);
        _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
        waitForText(expectedFlag);
        assertElementNotPresent(strikeoutAUC);
    }

    @LogMethod
    protected void verifyQCReport()
    {
        goToQCAnalysisPage(TEST_ASSAY_LUM, "view titration qc report");

        //make sure all the columns we want are viable
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.showHiddenItems();
        _customizeViewsHelper.addCustomizeViewColumn("Five ParameterCurveFit/FailureFlag");
        _customizeViewsHelper.addCustomizeViewColumn("Four ParameterCurveFit/FailureFlag");
        _customizeViewsHelper.addCustomizeViewColumn("Five ParameterCurveFit/EC50");
        _customizeViewsHelper.saveCustomView();

        assertTextPresent("Titration QC Report");
        DataRegionTable drt = new DataRegionTable("AnalyteTitration", this);
        String isotype = drt.getDataAsText(0, "Isotype");
        if(isotype.length()==0)
            isotype = "[None]";
        String conjugate = drt.getDataAsText(0, "Conjugate");
        if(conjugate.length()==0)
            conjugate =  "[None]";

        log("verify the calculation failure flag");
        List<String> fourParamFlag = drt.getColumnDataAsText("Four Parameter Curve Fit Failure Flag");
        for(String flag: fourParamFlag)
        {
            assertEquals(" ", flag);
        }

        List<String> fiveParamFlag = drt.getColumnDataAsText("Five Parameter Curve Fit Failure Flag");
        List<String> fiveParamData = drt.getColumnDataAsText("Five Parameter Curve Fit EC50");

        for(int i=0; i<fiveParamData.size(); i++)
        {
            assertTrue("Row " + i + " was flagged as 5PL failure but had EC50 data", ((fiveParamFlag.get(i).equals(" ")) ^ (fiveParamData.get(i).equals(" "))));
        }


        //verify link to Levey-Jennings plot
        clickAndWait(Locator.linkWithText("graph", 0));
        waitForText(" - " + isotype + " " + conjugate);
        assertTextPresent("Levey-Jennings Report", "Standard1");
    }
}