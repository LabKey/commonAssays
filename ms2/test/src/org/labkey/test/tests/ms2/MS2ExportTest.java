/*
 * Copyright (c) 2013-2017 LabKey Corporation
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
package org.labkey.test.tests.ms2;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Assert;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.MS2;
import org.labkey.test.util.DataRegionExportHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExcelHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.TextSearcher;

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
        navigateToFolder(FOLDER_NAME);
        DataRegionTable runsTable = new DataRegionTable(REGION_NAME_SEARCH_RUNS, this);
        runsTable.checkAllOnPage();
        runsTable.clickHeaderMenu("Compare", "Peptide");
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
        DataRegionExportHelper exportHelper = new DataRegionExportHelper(new DataRegionTable("query", this));
        return exportHelper.exportExcel(DataRegionExportHelper.ExcelFileType.XLSX);
    }

    private void validateBulkExport()
    {
        log("Test export 2 runs together");
        DataRegionTable searchRunsTable = new DataRegionTable(REGION_NAME_SEARCH_RUNS, this);
        searchRunsTable.checkAllOnPage();
        searchRunsTable.clickHeaderButton("MS2 Export");

        assertTextPresent("BiblioSpec");
        Assert.assertEquals("Wrong number of rows in exported Excel file", 116, getBulkExcelExportRowCount());

        Runnable tsvPeptideValidator = () ->
        {
            File exportFile = doAndWaitForDownload(() -> clickButton("Export", 0));
            TextSearcher exportFileSearcher = new TextSearcher(exportFile);
            assertTextPresent(exportFileSearcher, "Scan", "Protein", "gi|5002198|AF143203_1_interle", "1386.6970", "gi|6049221|AF144467_1_nonstru");
            assertTextPresentInThisOrder(exportFileSearcher, "K.QLDSIHVTILHK.E", "R.GRRNGPRPVHPTSHNR.Q");
            assertTextPresentInThisOrder(exportFileSearcher, "R.EADKVLVQMPSGK.Q", "K.E^TSSKNFDASVDVAIRLGVDPR.K");
            assertTextPresent(exportFileSearcher, "\n", 86);
        };
        validateExport("TSV", LEGACY_PEPTIDES_SCAN_6_100_VIEW_NAME, tsvPeptideValidator);
        validateExport("TSV", QUERY_PEPTIDES_VIEW_NAME, tsvPeptideValidator);

        Runnable amtPeptideValidator = () ->
        {
            File exportFile = doAndWaitForDownload(() -> clickButton("Export", 0));
            TextSearcher exportFileSearcher = new TextSearcher(exportFile);
            assertTextPresent(exportFileSearcher, "Run", "Peptide", "-.MELFSNELLYK.T", "1386.6970");
            assertTextPresentInThisOrder(exportFileSearcher, "K.QLDSIHVTILHK.E", "R.GRRNGPRPVHPTSHNR.Q");
            assertTextPresentInThisOrder(exportFileSearcher, "R.EADKVLVQMPSGK.Q", "K.E^TSSKNFDASVDVAIRLGVDPR.K");
            assertTextPresent(exportFileSearcher, "\n", 89);
        };
        validateExport("AMT", LEGACY_PEPTIDES_SCAN_6_100_VIEW_NAME, amtPeptideValidator);
        validateExport("AMT", QUERY_PEPTIDES_VIEW_NAME, amtPeptideValidator);

        Runnable pklPeptideValidator = () ->
        {
            File exportFile = doAndWaitForDownload(() -> clickButton("Export", 0));
            TextSearcher exportFileSearcher = new TextSearcher(exportFile);
            assertTextPresent(exportFileSearcher, "515.9 1684.0");
            assertTextNotPresent(exportFileSearcher, "717.4 4043.0");
            assertTextPresent(exportFileSearcher, "\n", 4271);
        };
        validateExport("PKL", LEGACY_PEPTIDES_SCAN_6_100_VIEW_NAME, pklPeptideValidator);
        validateExport("PKL", QUERY_PEPTIDES_VIEW_NAME, pklPeptideValidator);

        Runnable tsvProteinProphetValidator = () ->
        {
            File exportFile = doAndWaitForDownload(() -> clickButton("Export", 0));
            TextSearcher exportFileSearcher = new TextSearcher(exportFile);
            assertTextPresent(exportFileSearcher, "gi|16078254|similar_to_riboso", "20925.0", "gi|13470573|30S_ribosomal_pro, gi|16125519|ribosomal_protein");
            assertTextPresent(exportFileSearcher, "\n", 7);
        };
        validateExport("TSV", QUERY_PROTEINPROPHET_VIEW_NAME, tsvProteinProphetValidator);
        validateExport("TSV", LEGACY_PROTEIN_PROPHET_VIEW_NAME, tsvProteinProphetValidator);

        Runnable pklProteinProphetValidator = () ->
        {
            File exportFile = doAndWaitForDownload(() -> clickButton("Export", 0));
            TextSearcher exportFileSearcher = new TextSearcher(exportFile);
            assertTextPresent(exportFileSearcher, "426.9465 1 3", "174.8 2400.0");
            assertTextPresent(exportFileSearcher,"\n", 245);
        };
        validateExport("PKL", QUERY_PROTEINPROPHET_VIEW_NAME, pklProteinProphetValidator);
        validateExport("PKL", LEGACY_PROTEIN_PROPHET_VIEW_NAME, pklProteinProphetValidator);
    }

    private void validateExport(String exportType, String viewName, Runnable validator)
    {
        checkRadioButton(Locator.radioButtonByNameAndValue("exportFormat", exportType));
        selectOptionByText(Locator.name("viewParams"), viewName);

        validator.run();
    }
}
