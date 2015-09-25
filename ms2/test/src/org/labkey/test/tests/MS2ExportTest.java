/*
 * Copyright (c) 2013-2015 LabKey Corporation
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

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Assert;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.MS2;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExcelHelper;
import org.labkey.test.util.LogMethod;

import java.io.File;
import java.io.IOException;

@Category({MS2.class, DailyA.class})
public class MS2ExportTest extends AbstractMS2ImportTest
{
    @Override
    @LogMethod
    protected void setupMS2()
    {
        super.setupMS2();

        importMS2Run("DRT2", 2);
    }

    @Override
    @LogMethod
    protected void verifyMS2()
    {
        validateBulkExport();
        validatePeptideComparisonExport();
    }

    private void validatePeptideComparisonExport()
    {
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));
        clickAndWait(Locator.linkWithText("MS2 Runs"));
        DataRegionTable RunsTable = new DataRegionTable("MS2SearchRuns", this);
        RunsTable.checkAllOnPage();
        _ext4Helper.clickExt4MenuButton(true, Locator.lkButton("Compare"), false, "Peptide");
        clickButton("Compare");
        Assert.assertEquals("Wrong number of rows in exported Excel file", 111, getCompExcelExportRowCount());
    }

    private int getCompExcelExportRowCount()
    {
        File export = compExcelExport();
        try
        {
            Workbook wb = ExcelHelper.create(export);
            Sheet sheet = wb.getSheetAt(0);
            return sheet.getLastRowNum() - 1; // +1 for zero-based, -2 for header rows
        }
        catch (IOException | InvalidFormatException fail)
        {
            throw new RuntimeException("Error reading exported grid file", fail);
        }
    }

    private int getBulkExcelExportRowCount()
    {
        File export = bulkExcelExport();
        try
        {
            Workbook wb = ExcelHelper.create(export);
            Sheet sheet = wb.getSheetAt(0);
            return sheet.getLastRowNum(); // +1 for zero-based, -1 for header row
        }
        catch (IOException | InvalidFormatException fail)
        {
            throw new RuntimeException("Error reading exported grid file", fail);
        }
    }

    private File bulkExcelExport()
    {
        checkRadioButton(Locator.radioButtonByNameAndValue("exportFormat", "Excel"));
        selectOptionByText(Locator.name("viewParams"), "<Standard View>");
        return clickAndWaitForDownload(findButton("Export"));
    }

    private File compExcelExport()
    {
        clickButton("Export", 0);
        _extHelper.clickSideTab("Excel");
        return clickAndWaitForDownload(Locator.lkButton("Export to Excel"), 1)[0];
    }

    private void validateBulkExport()
    {
        log("Test export 2 runs together");
        DataRegionTable searchRunsTable = new DataRegionTable("MS2SearchRuns", this);
        searchRunsTable.checkAllOnPage();
        clickButton("MS2 Export");

        assertTextPresent("BiblioSpec");
        Assert.assertEquals("Wrong number of rows in exported Excel file", 116, getBulkExcelExportRowCount());

        Runnable tsvPeptideValidator = new Runnable()
        {
            @Override
            public void run()
            {
                assertTextPresent("Scan", "Protein", "gi|5002198|AF143203_1_interle", "1386.6970", "gi|6049221|AF144467_1_nonstru");
                assertTextBefore("K.QLDSIHVTILHK.E", "R.GRRNGPRPVHPTSHNR.Q");
                assertTextBefore("R.EADKVLVQMPSGK.Q", "K.E^TSSKNFDASVDVAIRLGVDPR.K");
                assertTextPresent("\n", 86);
            }
        };
        validateExport("TSV", LEGACY_PEPTIDES_SCAN_6_100_VIEW_NAME, tsvPeptideValidator);
        validateExport("TSV", QUERY_PEPTIDES_VIEW_NAME, tsvPeptideValidator);

        Runnable amtPeptideValidator = new Runnable()
        {
            @Override
            public void run()
            {
                assertTextPresent("Run", "Peptide", "-.MELFSNELLYK.T", "1386.6970");
                assertTextBefore("K.QLDSIHVTILHK.E", "R.GRRNGPRPVHPTSHNR.Q");
                assertTextBefore("R.EADKVLVQMPSGK.Q", "K.E^TSSKNFDASVDVAIRLGVDPR.K");
                assertTextPresent("\n", 89);
            }
        };
        validateExport("AMT", LEGACY_PEPTIDES_SCAN_6_100_VIEW_NAME, amtPeptideValidator);
        validateExport("AMT", QUERY_PEPTIDES_VIEW_NAME, amtPeptideValidator);

        Runnable pklPeptideValidator = new Runnable()
        {
            @Override
            public void run()
            {
                assertTextPresent("515.9 1684.0");
                assertTextNotPresent("717.4 4043.0");
                assertTextPresent("\n", 4271);
            }
        };
        validateExport("PKL", LEGACY_PEPTIDES_SCAN_6_100_VIEW_NAME, pklPeptideValidator);
        validateExport("PKL", QUERY_PEPTIDES_VIEW_NAME, pklPeptideValidator);

        Runnable tsvProteinProphetValidator = new Runnable()
        {
            @Override
            public void run()
            {
                assertTextPresent("gi|16078254|similar_to_riboso", "20925.0", "gi|13470573|30S_ribosomal_pro, gi|16125519|ribosomal_protein");
                assertTextPresent("\n", 7);
            }
        };
        validateExport("TSV", QUERY_PROTEINPROPHET_VIEW_NAME, tsvProteinProphetValidator);
        validateExport("TSV", LEGACY_PROTEIN_PROPHET_VIEW_NAME, tsvProteinProphetValidator);

        Runnable pklProteinProphetValidator = new Runnable()
        {
            @Override
            public void run()
            {
                assertTextPresent("426.9465 1 3", "174.8 2400.0");
                assertTextPresent("\n", 245);
            }
        };
        validateExport("PKL", QUERY_PROTEINPROPHET_VIEW_NAME, pklProteinProphetValidator);
        validateExport("PKL", LEGACY_PROTEIN_PROPHET_VIEW_NAME, pklProteinProphetValidator);
    }

    private void validateExport(String exportType, String viewName, Runnable validator)
    {
        checkRadioButton(Locator.radioButtonByNameAndValue("exportFormat", exportType));
        selectOptionByText(Locator.name("viewParams"), viewName);
        clickButton("Export");
        validator.run();
        goBack();
    }
}
