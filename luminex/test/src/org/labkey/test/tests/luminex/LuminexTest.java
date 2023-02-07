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

import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.assay.GetProtocolCommand;
import org.labkey.remoteapi.assay.Protocol;
import org.labkey.remoteapi.assay.ProtocolResponse;
import org.labkey.remoteapi.assay.SaveProtocolCommand;
import org.labkey.remoteapi.domain.Domain;
import org.labkey.remoteapi.domain.PropertyDescriptor;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.Row;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.pages.luminex.LuminexImportWizard;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.PipelineStatusTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.PostgresOnlyTest;
import org.labkey.test.util.QCAssayScriptHelper;
import org.labkey.test.util.RReportHelper;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@BaseWebDriverTest.ClassTimeout(minutes = 40)
public abstract class LuminexTest extends BaseWebDriverTest implements PostgresOnlyTest
{
    protected final static String TEST_ASSAY_PRJ_LUMINEX = "LuminexTest Project";            //project for luminex test

    public static final String TEST_ASSAY_LUM =  "&TestAssayLuminex></% 1";// put back TRICKY_CHARACTERS_NO_QUOTES when issue 20061 is resolved
    protected static final String TEST_ASSAY_LUM_DESC = "Description for Luminex assay";

    protected static final String TEST_ASSAY_XAR_NAME = "TestLuminexAssay";
    public static final File TEST_ASSAY_XAR_FILE = TestFileUtils.getSampleData("luminex/" + TEST_ASSAY_XAR_NAME + ".xar");

    protected static final String TEST_ASSAY_LUM_SET_PROP_SPECIES = "testSpecies1";
    public static final File TEST_ASSAY_LUM_FILE1 = TestFileUtils.getSampleData("luminex/10JAN07_plate_1.xls");
    public static final File TEST_ASSAY_LUM_FILE2 = TestFileUtils.getSampleData("luminex/pnLINCO20070302A.xlsx");
    public static final File TEST_ASSAY_LUM_FILE3 = TestFileUtils.getSampleData("luminex/WithIndices.xls");
    public static final File TEST_ASSAY_LUM_FILE4 = TestFileUtils.getSampleData("luminex/WithAltNegativeBead.xls");
    public static final File TEST_ASSAY_LUM_FILE5 = TestFileUtils.getSampleData("luminex/Guide Set plate 1.xls");
    public static final File TEST_ASSAY_LUM_FILE6 = TestFileUtils.getSampleData("luminex/Guide Set plate 2.xls");
    public static final File TEST_ASSAY_LUM_FILE7 = TestFileUtils.getSampleData("luminex/Guide Set plate 3.xls");
    public static final File TEST_ASSAY_LUM_FILE8 = TestFileUtils.getSampleData("luminex/Guide Set plate 4.xls");
    public static final File TEST_ASSAY_LUM_FILE9 = TestFileUtils.getSampleData("luminex/Guide Set plate 5.xls");
    public static final File TEST_ASSAY_LUM_FILE10 = TestFileUtils.getSampleData("luminex/RawAndSummary.xlsx");
    public static final File TEST_ASSAY_LUM_FILE11 = TestFileUtils.getSampleData("luminex/PositivityWithBaseline.xls");
    public static final File TEST_ASSAY_LUM_FILE12 = TestFileUtils.getSampleData("luminex/PositivityWithoutBaseline.xls");
    public static final File TEST_ASSAY_LUM_FILE13 = TestFileUtils.getSampleData("luminex/PositivityThreshold.xls");

    public static final File TEST_ASSAY_MULTIPLE_STANDARDS_1 = TestFileUtils.getSampleData("luminex/plate 1_IgA-Biot (Standard2).xls");
    public static final File TEST_ASSAY_MULTIPLE_STANDARDS_2 = TestFileUtils.getSampleData("luminex/plate 2_IgA-Biot (Standard2).xls");
    public static final File TEST_ASSAY_MULTIPLE_STANDARDS_3 = TestFileUtils.getSampleData("luminex/plate 3_IgA-Biot (Standard1).xls");

    public static final File RTRANSFORM_SCRIPT_FILE_LABKEY = new File(TestFileUtils.getLabKeyRoot(), "server/modules/commonAssays/luminex/resources/transformscripts/labkey_luminex_transform.R");
    public static final File RTRANSFORM_SCRIPT_FILE_LAB =  new File(TestFileUtils.getLabKeyRoot(), "server/modules/commonAssays/luminex/resources/transformscripts/tomaras_luminex_transform.R");

    public static final String ASSAY_ID_FIELD  = "name";
    public static final String ASSAY_DATA_FILE_LOCATION_MULTIPLE_FIELD = "__primaryFile__";

    public static final String DATA_TABLE_NAME = "Data";
    protected static final String EXCLUDE_COMMENT_FIELD = "comment";
    protected static final String MULTIPLE_CURVE_ASSAY_RUN_NAME = "multipleCurvesTestRun";
    protected static final String SAVE_CHANGES_BUTTON = "Save";

    //TODO: move to control
    protected static final String EXCLUDE_ALL_BUTTON = "excludeall";
    protected static final String EXCLUDE_SELECTED_BUTTON = "excludeselected";
    private static final Locator AVAILABLE_ANALYTES_CHECKBOX = Locator.xpath("//div[@class='x-grid3-hd-inner x-grid3-hd-checker']/div[@class='x-grid3-hd-checker']");
    private static final Locator COMMENT_LOCATOR = Locator.xpath("//input[@id='comment']") ;

    public static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    public static final String isotype = "IgG ></% 1";// put back TRICKY_CHARACTERS_NO_QUOTES when issue 20061 is resolved
    public static final String conjugate = "PE ></% 1";// put back TRICKY_CHARACTERS_NO_QUOTES when issue 20061 is resolved

    public LuminexTest()
    {
        setDefaultWaitForPage(60000);
    }

    @Override
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

    protected boolean renameTargetStudy()
    {
        return false;
    }

    protected boolean requiresStudy()
    {
        return false;
    }

    @BeforeClass
    public static void initTest() throws Exception
    {
        ((LuminexTest)getCurrentTest()).doInit();
    }

    @LogMethod
    private void doInit() throws Exception
    {
        // setup a scripting engine to run a java transform script
        QCAssayScriptHelper javaEngine = new QCAssayScriptHelper(this);
        javaEngine.ensureEngineConfig();

        // fail fast if R is not configured
        RReportHelper _rReportHelper = new RReportHelper(this);
        _rReportHelper.ensureRConfig();

        log("Testing Luminex Assay Designer");
        //create a new test project
        _containerHelper.createProject(getProjectName(), requiresStudy() ? "Study" : "Assay");
        if (requiresStudy())
        {
            //create a study within this project to which we will publish
            goToProjectHome(getProjectName());
            createDefaultStudy();
            goToProjectHome();
        }

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
            ReactAssayDesignerPage assayDesigner = _assayHelper.copyAssayDesign();
            assayDesigner.setName(TEST_ASSAY_LUM);
            assayDesigner.setDescription(TEST_ASSAY_LUM_DESC);

            // rename TargetStudy field to avoid the expensive assay-to-study SpecimenId join
            if (requiresStudy() && renameTargetStudy())
                assayDesigner.goToBatchFields().getField(1).setName("TargetStudyTemp");

            assayDesigner.clickFinish();
        }
        else
        {
            log("Setting up Luminex assay");

            GetProtocolCommand getProtocolCommand = new GetProtocolCommand("Luminex");
            ProtocolResponse getProtocolResponse = getProtocolCommand.execute(createDefaultConnection(), getProjectName());

            Protocol assayProtocol = getProtocolResponse.getProtocol();
            assayProtocol.setName(TEST_ASSAY_LUM)
                .setDescription(TEST_ASSAY_LUM_DESC);

            // add batch properties for transform and R version numbers
            Domain batchesDomain =  assayProtocol.getDomains().stream().filter(a->a.getName().equals("Batch Fields")).findFirst()
                    .orElseThrow(()-> new IllegalStateException("The protocol template did not supply a [Batch Fields] domain"));
            List<PropertyDescriptor> batchFields = batchesDomain.getFields();   // keep the template-supplied fields, add the following
            batchFields.add(new PropertyDescriptor("Network", "Network", "string"));
            batchFields.add(new PropertyDescriptor("TransformVersion", "Transform Script Version", "string"));
            batchFields.add(new PropertyDescriptor("LabTransformVersion", "Lab Transform Script Version", "string"));
            batchFields.add(new PropertyDescriptor("RVersion", "R Version", "string"));
            setFieldsToDomain(batchFields, batchesDomain);

            // add run properties for designation of which field to use for curve fit calc in transform
            Domain runsDomain =  assayProtocol.getDomains().stream().filter(a->a.getName().equals("Run Fields")).findFirst()
                    .orElseThrow(()-> new IllegalStateException("The protocol template did not supply a [Run Fields] domain"));
            List<PropertyDescriptor> runFields = runsDomain.getFields();
            runFields.add(new PropertyDescriptor("SubtNegativeFromAll", "Subtract Negative Bead from All Wells", "boolean"));
            runFields.add(new PropertyDescriptor("StndCurveFitInput", "Input Var for CurveFit Calc of Standards", "string"));
            runFields.add(new PropertyDescriptor("UnkCurveFitInput", "Input Var for Curve Fit Calc of Unknowns", "string"));

            // add run properties for use with the Guide Set test
            runFields.add(new PropertyDescriptor("NotebookNo", "Notebook Number", "string"));
            runFields.add(new PropertyDescriptor("AssayType", "Assay Type", "string"));
            runFields.add(new PropertyDescriptor("ExpPerformer", "Experiment Performer", "string"));

            // add run properties for use with Calculating Positivity
            runFields.add(new PropertyDescriptor("CalculatePositivity", "Calculate Positivity", "boolean"));
            runFields.add(new PropertyDescriptor("BaseVisit", "Baseline Visit", "float"));
            runFields.add(new PropertyDescriptor("PositivityFoldChange", "Positivity Fold Change", "int"));
            setFieldsToDomain(runFields, runsDomain);

            // add analyte property for tracking lot number
            Domain analyteDomain = assayProtocol.getDomains().stream().filter(a->a.getName().equals("Analyte Properties")).findFirst()
                    .orElseThrow(()-> new IllegalStateException("The protocol template did not supply an [Analyte Properties] domain"));
            List<PropertyDescriptor> analyteFields = analyteDomain.getFields();
            analyteFields.add(new PropertyDescriptor("LotNumber", "Lot Number", "string"));
            analyteFields.add(new PropertyDescriptor("NegativeControl", "Negative Control", "boolean"));
            setFieldsToDomain(analyteFields, analyteDomain);

            // add the data properties for the calculated columns, set format to two decimal place for easier testing later
            Domain resultsDomain = assayProtocol.getDomains().stream().filter(a->a.getName().equals("Data Fields")).findFirst()
                    .orElseThrow(()-> new IllegalStateException("The protocol template did not supply a [Data Fields] domain"));
            List<PropertyDescriptor> resultsFields = resultsDomain.getFields();
            resultsFields.add(new PropertyDescriptor("FIBackgroundNegative", "FI-Bkgd-Neg", "float").setFormat("0.0"));
            resultsFields.add(new PropertyDescriptor("Slope_4pl", "Slope_4pl", "float").setFormat("0.0"));
            resultsFields.add(new PropertyDescriptor("Lower_4pl", "Lower_4pl", "float").setFormat("0.0"));
            resultsFields.add(new PropertyDescriptor("Upper_4pl", "Upper_4pl", "float").setFormat("0.0")); 
            resultsFields.add(new PropertyDescriptor("Inflection_4pl", "Inflection_4pl", "float").setFormat("0.0"));
            resultsFields.add(new PropertyDescriptor("Positivity", "Positivity", "string"));
            setFieldsToDomain(resultsFields, resultsDomain);

            SaveProtocolCommand saveProtocolCommand = new SaveProtocolCommand(assayProtocol);
            saveProtocolCommand.execute(createDefaultConnection(), getProjectName());
        }
    }

    private void setFieldsToDomain(List<PropertyDescriptor> fields, Domain domain)
    {
        for (PropertyDescriptor field : fields)
        {
            log("adding field ["+field.getName()+"] to domain ["+domain.getName()+"]");
        }
        domain.setFields(fields);
    }

    public void excludeAnalyteForRun(String analyte, boolean firstExclusion, String comment)
    {
        DataRegionTable table = new DataRegionTable("Data", getDriver());
        table.clickHeaderMenu("Exclusions", false, "Exclude Analytes");
        _extHelper.waitForExtDialog("Exclude Analytes from Analysis");
        if (!firstExclusion)
            waitForText("To remove an exclusion, uncheck the analyte(s).");

        clickExcludeAnalyteCheckBox(analyte);
        setFormElement(Locator.id(EXCLUDE_COMMENT_FIELD), comment);
        waitForElement(Locator.xpath("//table[@id='saveBtn' and not(contains(@class, 'disabled'))]"), WAIT_FOR_JAVASCRIPT);

        clickButton(SAVE_CHANGES_BUTTON, 0);
    }

    /**
     * From the results table of a run will set a well type/wellRole (i.e. replicate group) as excluded
     */
    protected void excludeReplicateGroup(String wellName, String type, String description, String exclusionComment, String... analytes)
    {
        DataRegionTable table = new DataRegionTable("Data", this.getWrappedDriver());
        table.setFilter("Type", "Equals", type);
        table.setFilter("Description", "Equals", description);
        clickExclusionMenuIconForWell(wellName, true);
        setFormElement(Locator.name(EXCLUDE_COMMENT_FIELD), exclusionComment);

        if (analytes == null || analytes.length == 0)
        {
            click(Locator.radioButtonById(EXCLUDE_ALL_BUTTON));
        }
        else
        {
            click(Locator.radioButtonById(EXCLUDE_SELECTED_BUTTON));

            for (String analyte : analytes)
            {
                if (StringUtils.isNotBlank(analyte))
                    clickExcludeAnalyteCheckBox(analyte);
            }
        }
        clickButton(SAVE_CHANGES_BUTTON, 0);
    }

    protected void excludeTitration(String titration, String exclusionMessage, String runName, int pipelineJobCount, String...analytes)
    {
        DataRegionTable table = new DataRegionTable("Data", getDriver());
        table.clickHeaderMenu("Exclusions", false, "Exclude Titrations");
        _extHelper.waitForExtDialog("Exclude Titrations from Analysis");
        assertElementPresent(Locator.tagWithText("div", "Analytes excluded for a well, replicate group, singlepoint unknown, "
                + "or at the assay level will not be re-included by changes in titration exclusions."));

        waitAndClick(Locator.tagWithClass("td", "x-grid3-cell-first").withText(titration));
        if (analytes == null || analytes.length == 0)
        {
            waitAndClick(AVAILABLE_ANALYTES_CHECKBOX);
        }
        else
        {
            for (String analyte : analytes)
                waitAndClick(Locator.tagWithClass("td", "x-grid3-td-1").containing(analyte));
        }
        setFormElement(COMMENT_LOCATOR, exclusionMessage);
        sleep(1000);
        clickButton("Save", 0);
        _extHelper.waitForExtDialog("Confirm Exclusions", WAIT_FOR_JAVASCRIPT);
        clickButtonContainingText("Yes", 0);
        verifyExclusionPipelineJobComplete(pipelineJobCount, "INSERT titration exclusion (Description: " + titration + ")", runName, exclusionMessage);
    }

    /**
     * click on the exclusion icon associated with the particular well
     * preconditions:  at Test Result page
     * postconditions: at Test Result Page with exclude Replicate Group From Analysis window up
     * @param wellName
     */
    protected void clickExclusionMenuIconForWell(String wellName, boolean selectReplicateGroups)
    {
        new DataRegionTable.DataRegionFinder(getDriver()).withName("Data").timeout(3 * getDefaultWaitForPage()).waitFor();
        waitAndClick(Locator.id("__changeExclusions__" + wellName));
        _extHelper.waitForExtDialog("Exclude Well or Replicate Group from Analysis");
        waitForElement(Locator.xpath("//table[@id='saveBtn' and not(contains(@class, 'disabled'))]"), WAIT_FOR_JAVASCRIPT);

        if (selectReplicateGroups)
        {
            WebElement element = Locator.checkboxByLabel("Replicate Group", false)
                    .findWhenNeeded(getDriver()).withTimeout(4000);
            checkCheckbox(element);
        }
    }

    protected void clickExcludeAnalyteCheckBox(String analyte)
    {
        Locator l = ExtHelper.locateGridRowCheckbox(analyte);
        waitAndClick(l);
    }

    protected void clickReplicateGroupCheckBoxSelectSingleWell(String labelText, String well, boolean set)
    {
/*
        WebElement element = Locator.checkboxByLabel(labelText,false)
                .findWhenNeeded(getDriver()).withTimeout(4000);
        uncheckCheckbox(element);
*/
        WebElement wellElement = Locator.checkboxByLabel(well,false)
                .findWhenNeeded(getDriver()).withTimeout(4000);
        new Checkbox(wellElement).set(set);
    }
    
    protected void verifyExclusionPipelineJobComplete(int jobCount, String expectedInfo, String runName, String exclusionComment)
    {
        verifyExclusionPipelineJobComplete(jobCount, expectedInfo, runName, exclusionComment, 1, 1);
    }

    protected void verifyExclusionPipelineJobComplete(int jobCount, String expectedInfo, String runName, String exclusionComment, int numCommands, int numMatchingJobs)
    {
        _extHelper.waitForExtDialog("Success", WAIT_FOR_JAVASCRIPT);
        clickButtonContainingText("Yes");
        waitForPipelineJobsToComplete(jobCount, false);
        DataRegionTable table = new DataRegionTable("StatusFiles", this);
        table.setFilter("Info", "Starts With", expectedInfo);
        assertElementPresent(Locator.linkWithText("COMPLETE"), numMatchingJobs);
        clickAndWait(Locator.linkWithText("COMPLETE").index(0));
        assertTextPresent(
                expectedInfo,
                "Assay Id: " + runName,
                "Comment: " + exclusionComment);
        assertTextPresent("Finished", numCommands);
        clickButtonContainingText("Data");
    }

    protected String[] getListOfAnalytesMultipleCurveData()
    {
        //TODO:  make this a dynamic list, acquired from the current data set, rather than hardcoded
        return new String[] {"ENV6", "ENV7", "ENV4", "ENV5", "Blank"};
    }


    protected String startCreateMultipleCurveAssayRun()
    {
        log("Creating test run with multiple standard curves");
        String name = MULTIPLE_CURVE_ASSAY_RUN_NAME;

        createNewAssayRun(TEST_ASSAY_LUM, name);

        uploadMultipleCurveData();

        return name;

    }

    @LogMethod(quiet = true)
    protected void assertAnalytesHaveCorrectStandards(String assayName, int runId, Map<String, Set<String>> expectedAnalyteStandards)
    {
        SelectRowsCommand command = new SelectRowsCommand("assay.Luminex." + assayName, "Data");
        command.setRequiredVersion(9.1); // Needed in order to get display values of lookup columns
        command.addFilter(new Filter("Run/RowId", runId, Filter.Operator.EQUAL));
        Connection connection = createDefaultConnection(true);
        SelectRowsResponse response;
        try
        {
            response = command.execute(connection, getProjectName());
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }

        for (Row row : response.getRowset())
        {
            String analyte = (String)row.getDisplayValue("Analyte");
            assertEquals(String.format("Wrong standards for analyte %s at row %d", analyte, row.getDisplayValue("RowId")),
                    expectedAnalyteStandards.get(analyte), splitStandards((String)row.getDisplayValue("Analyte/Standard")));
        }
    }

    private Set<String> splitStandards(String standards)
    {
        return new HashSet<>(Arrays.asList(standards.trim().split("\\s*,\\s*")));
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
        setNegativeBeads("Blank");
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

        if (!isTextPresent(MULTIPLE_CURVE_ASSAY_RUN_NAME)) //right now this is a good enough check.  May have to be
        // more rigorous if tests start substantially altering data
        {
            log("multiple curve data not present, adding now");
            startCreateMultipleCurveAssayRun();
            clickButton("Save and Finish", defaultWaitForPage * 4);
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

    /*Note: Do not use for multiple file replacement*/
    protected void replaceFileInAssayRun(File original, File newFile)
    {
        //Add spot for new file
        click(Locator.xpath("//a[contains(@class, 'labkey-file-add-icon-enabled')]"));
        //remove old file
        click(Locator.xpath("//tr[td/span/text() = '" + original.getName() + "']" +
                "/td/a[contains(@class, 'labkey-file-remove-icon-enabled')]"));

        String fieldName = ASSAY_DATA_FILE_LOCATION_MULTIPLE_FIELD;
        setFormElement(Locator.name(fieldName), newFile);
    }

    /**
     * preconditions:  can see Project Folder, assay already exists
     * postconditions: at data import screen for new test run
     */
    protected void createNewAssayRun(String assayName, String runId)
    {
        goToTestAssayHome(assayName);
        LuminexImportWizard wiz = new LuminexImportWizard(this);
        wiz.startImport();
        wiz.checkParticipantVisitResolver();
        clickButtonContainingText("Next");
        setFormElement(Locator.name(ASSAY_ID_FIELD), runId);
    }

    public void goToQCAnalysisPage(String assayName, String submenuText)
    {
        goToTestAssayHome(assayName);
        BootstrapMenu.find(getDriver(), "view qc report").clickSubMenu(true,submenuText);
    }

    /**
     * Cleanup entry point.
     * @param afterTest
     */
    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);

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
        _userHelper.createUser(user, false);
        goToProjectHome();
        _permissionsHelper.setUserPermissions(user, perms);
        impersonate(user);
    }


    @LogMethod(quiet = true)
    public DataRegionTable uploadPositivityFile(String assayName, @LoggedParam String assayRunId, @LoggedParam File file, String baseVisit, String foldChange, boolean isBackgroundUpload, boolean expectDuplicateFile)
    {
        createNewAssayRun(assayName, assayRunId);
        checkCheckbox(Locator.name("calculatePositivity"));
        setFormElement(Locator.name("baseVisit"), baseVisit);
        setFormElement(Locator.name("positivityFoldChange"), foldChange);
        selectPositivityFile(file, expectDuplicateFile);
        setAnalytePropertyValues();
        finishUploadPositivityFile(assayRunId, isBackgroundUpload);
        return new DataRegionTable("Data", this);
    }

    public void finishUploadPositivityFile(String assayRunId, boolean isBackgroundUpload)
    {
        clickButton("Save and Finish", longWaitForPage);
        if (!isBackgroundUpload && !isElementPresent(Locator.css(".labkey-error").containing("Error: ")))
            clickAndWait(Locator.linkWithText(assayRunId), longWaitForPage);
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

        //add QC flag column
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("QCFlags");
        _customizeViewsHelper.saveCustomView();

        //verify expected values in column
        List<String> var = new DataRegionTable("Runs", getDriver()).getFullColumnValues("QC Flags").get(0);
        String[] flags = var.toArray(new String[var.size()]);
        for (int i=0; i<flags.length; i++)
        {
            assertEquals(expectedFlags[i], flags[i].trim());
        }

        verifyQCFlagLink(analyteName, expectedFlags[0]);
    }

    @LogMethod
    private void verifyQCFlagLink(String analyteName, String expectedFlag)
    {
        click(Locator.linkContainingText(expectedFlag).index(0));
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
        _customizeViewsHelper.addColumn("Four ParameterCurveFit/FailureFlag");
        _customizeViewsHelper.addColumn("Four ParameterCurveFit/EC50");
        _customizeViewsHelper.saveCustomView();

        assertTextPresent("Titration QC Report");
        DataRegionTable drt = new DataRegionTable("AnalyteTitration", this);
        String isotype = drt.getDataAsText(0, "Isotype");
        if (isotype.length() == 0)
            isotype = "[None]";
        String conjugate = drt.getDataAsText(0, "Conjugate");
        if (conjugate.length() == 0)
            conjugate =  "[None]";

        log("verify the calculation failure flag");
        List<String> fourParamFlag = drt.getColumnDataAsText("Four Parameter Curve Fit Failure Flag");
        for (String flag : fourParamFlag)
        {
            assertEquals(" ", flag);
        }

        //verify link to Levey-Jennings plot
        clickAndWait(Locator.linkWithText("graph").index(0));
        waitForText(" - " + isotype + " " + conjugate);
        assertTextPresent("Levey-Jennings Report", "Standard1");
    }

    @LogMethod (quiet = true)
    protected void cleanupPipelineJobs()
    {
        goToProjectHome();
        goToModule("Pipeline");
        PipelineStatusTable table = new PipelineStatusTable(this);

        if (table.getDataRowCount() > 0)
        {
            table.checkAll();
            table.clickHeaderButton("Delete");
            clickButton("Confirm Delete");
        }
    }
}
