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
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.util.DataRegionTable;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({Daily.class, Assays.class})
@BaseWebDriverTest.ClassTimeout(minutes = 7)
public class LuminexEC50Test extends LuminexTest
{
    private final String EC50_RUN_NAME = "EC50";
    private final String drc4 = "Four Parameter";
    private final String trapezoidal = "Trapezoidal";

    @BeforeClass
    public static void updateAssayDefinition()
    {
        LuminexEC50Test init = (LuminexEC50Test)getCurrentTest();
        init.goToTestAssayHome();
        ReactAssayDesignerPage assayDesigner = init._assayHelper.clickEditAssayDesign();

        assayDesigner.addTransformScript(RTRANSFORM_SCRIPT_FILE_LABKEY);
        assayDesigner.addTransformScript(RTRANSFORM_SCRIPT_FILE_LAB);
        assayDesigner.clickFinish();
    }

    @Test
    public void testEC50()
    {
        LuminexRTransformTest rTransformTest = new LuminexRTransformTest();
        rTransformTest.uploadRun();

        createNewAssayRun(TEST_ASSAY_LUM, EC50_RUN_NAME);
        uploadMultipleCurveData();
        clickButton("Save and Finish", longWaitForPage);

        //add transform script
        goToSchemaBrowser();
        viewQueryData("assay.Luminex." + TEST_ASSAY_LUM, "CurveFit");

        checkEC50dataAndFailureFlag();
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

        log("Write this");
        for(int i=0; i<formula.size(); i++)
        {
            if(formula.get(i).equals(drc4))
            {
                //ec50=populated=inflectionPoint
                assertEquals(ec50.get(i), inflectionPoint.get(i));
                //auc=unpopulated
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
