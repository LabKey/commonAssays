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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.pages.AssayDesignerPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.RReportHelper;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({DailyA.class, Assays.class})
public class LuminexEC50Test extends LuminexTest
{
    private final String EC50_RUN_NAME = "EC50";
    private final String rum4 = "Four Parameter";
    private final String rum5 = "Five Parameter";
    private final String trapezoidal = "Trapezoidal";

    @BeforeClass
    public static void updateAssayDefinition()
    {
        LuminexEC50Test init = (LuminexEC50Test)getCurrentTest();
        init.goToTestAssayHome();
        AssayDesignerPage assayDesigner = init._assayHelper.clickEditAssayDesign();

        assayDesigner.addTransformScript(RTRANSFORM_SCRIPT_FILE_LABKEY);
        assayDesigner.addTransformScript(RTRANSFORM_SCRIPT_FILE_LAB);
        assayDesigner.saveAndClose();
    }

    @Test
    public void testEC50()
    {
        LuminexRTransformTest rTransformTest = new LuminexRTransformTest();
        rTransformTest.uploadRunWithoutRumiCalc();
        rTransformTest.reImportRunWithRumiCalc();

        createNewAssayRun(TEST_ASSAY_LUM, EC50_RUN_NAME);
        checkCheckbox(Locator.name("curveFitLogTransform"));
        uploadMultipleCurveData();
        clickButton("Save and Finish", longWaitForPage);

        //add transform script
        goToSchemaBrowser();
        viewQueryData("assay.Luminex." + TEST_ASSAY_LUM, "CurveFit");

        checkEC50dataAndFailureFlag();
    }

    private boolean checkRversion() throws IOException
    {
        // quick check to see if we are using 32-bit or 64-bit R
        log("Checking R 32-bit vs 64-bit");
        RReportHelper _rReportHelper = new RReportHelper(this);
        return _rReportHelper.getRScriptOutput(".Machine$sizeof.pointer").contains("[1] 8");
    }

    private void checkEC50dataAndFailureFlag()
    {
        // expect to already be viewing CurveFit query
        assertTextPresent("CurveFit");

        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("TitrationId/Name");
        _customizeViewsHelper.applyCustomView();

        DataRegionTable table = new DataRegionTable("query", getDriver());
        table.setFilter("TitrationId/Name", "Equals One Of (example usage: a;b;c)", "Standard1;Standard2");

        List<String> analyte = table.getColumnDataAsText("Analyte");
        List<String> formula = table.getColumnDataAsText("Curve Type");
        List<String> ec50 = table.getColumnDataAsText("EC50");
        List<String> auc= table.getColumnDataAsText("AUC");
        List<String> inflectionPoint = table.getColumnDataAsText("Inflection");
        int rum5ec50count = 0;

        log("Write this");
        for(int i=0; i<formula.size(); i++)
        {
            if(formula.get(i).equals(rum4))
            {
                //ec50=populated=inflectionPoint
                assertEquals(ec50.get(i), inflectionPoint.get(i));
                //auc=unpopulated
                assertEquals(" ", auc.get(i));
            }
            else if(formula.get(i).equals(rum5))
            {
                // ec50 will be populated for well formed curves (i.e. not expected for every row, so we'll keep a count and check at the end of the loop)
                if (!ec50.get(i).equals(" ") && ec50.get(i).length() > 0)
                    rum5ec50count++;

                // auc should not be populated
                assertEquals(" ", auc.get(i));
            }
            else if(formula.get(i).equals(trapezoidal))
            {
                //ec50 should not be populated
                assertEquals(" ", ec50.get(i));
                //auc=populated (for all non-blank analytes)
                if (!analyte.get(i).startsWith("Blank"))
                    assertTrue( "AUC was unpopulated for row " + i, auc.get(i).length()>0);
            }
        }
        assertEquals("Unexpected number of Five Parameter EC50 values (expected 10 of 14).", 10, rum5ec50count);

        // check that the 5PL parameters are within the expected ranges (note: exact values can change based on R 32-bit vs R 64-bit)
        // NOTE: the first two EC50s will be significantly different on Mac due to machine episolon. The test is adjusted for this, as these are "blanks" and thus provide the noisiest answers.
        Double[] FiveParameterEC50mins = {107.64, 460.75, 36465.56, 21075.08, 7826.89, 32211.66, 44972.77, 107.64, 0.4199,  0.03962};
        Double[] FiveParameterEC50maxs = {112.85, 486.5,  36469.5,  21075.29, 7826.90, 32211.67, 45012.09, 112.85, 0.43771, 0.03967};
        table.setFilter("CurveType", "Equals", "Five Parameter");
        table.setFilter("EC50", "Is Not Blank", "");
        table.setSort("EC50", SortDirection.ASC);
        table.setSort("AnalyteId", SortDirection.ASC);
        table.setSort("TitrationId", SortDirection.ASC);
        ec50 = table.getColumnDataAsText("EC50");
        assertEquals("Unexpected number of Five Parameter EC50 values (expected " + FiveParameterEC50maxs.length + ")", FiveParameterEC50maxs.length, ec50.size());
        for (int i = 0; i < ec50.size(); i++)
        {
            Double val = Double.parseDouble(ec50.get(i));
            Double min = FiveParameterEC50mins[i];
            Double max = FiveParameterEC50maxs[i];
            Double expected = (max + min) / 2;
            Double delta = (max - min) / 2;
            assertEquals(String.format("Unexpected 5PL EC50 value for %s - %s", table.getDataAsText(i, "Titration"), table.getDataAsText(i, "Analyte")), expected, val, delta);
        }
        table.clearFilter("EC50");
        table.clearFilter("CurveType");

        // expect to already be viewing CurveFit query
        assertTextPresent("CurveFit");

        table = new DataRegionTable("query", getDriver());
        table.setFilter("FailureFlag", "Equals", "true");

        // expect one 4PL curve fit failure (for Standard1 - ENV6 (97))
        table.setFilter("CurveType", "Equals", "Four Parameter");
        assertEquals("Expected one Four Parameter curve fit failure flag", 1, table.getDataRowCount());
        List<String> values = table.getColumnDataAsText("Analyte");
        assertTrue("Unexpected analyte for Four Parameter curve fit failure", values.size() == 1 && values.get(0).equals("ENV6"));
        table.clearFilter("CurveType");

        // expect four 5PL curve fit failures
        table.setFilter("CurveType", "Equals", "Five Parameter");
        assertEquals("Unexpected number of Five Parameter curve fit failure flags", 4, table.getDataRowCount());
        table.clearFilter("CurveType");

        table.clearFilter("FailureFlag");
    }
}
