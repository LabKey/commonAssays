/*
 * Copyright (c) 2011 LabKey Corporation
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
package org.labkey.luminex;

import junit.framework.Assert;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Test;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.XarFormatException;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.reader.ExcelFactory;
import org.labkey.api.settings.AppProps;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.util.GUID;
import org.labkey.api.util.Pair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
* User: jeckels
* Date: Jun 8, 2011
*/
public class LuminexExcelParser
{
    private Collection<File> _dataFiles;
    private Domain _excelRunDomain;
    private Map<Analyte, List<LuminexDataRow>> _sheets = new LinkedHashMap<Analyte, List<LuminexDataRow>>();
    private Map<File, Map<DomainProperty, String>> _excelRunProps = new HashMap<File, Map<DomainProperty, String>>();
    private Map<String, Titration> _titrations = new TreeMap<String, Titration>();
    private boolean _parsed;
    private boolean _imported;

    public LuminexExcelParser(ExpProtocol protocol, Collection<File> dataFiles)
    {
        this(AbstractAssayProvider.getDomainByPrefix(protocol, LuminexAssayProvider.ASSAY_DOMAIN_EXCEL_RUN), dataFiles);
    }

    public LuminexExcelParser(Domain excelRunDomain, Collection<File> dataFiles)
    {
        _excelRunDomain = excelRunDomain;
        _dataFiles = dataFiles;
    }

    private void parseFile() throws ExperimentException
    {
        if (_parsed) return;

        for (File dataFile : _dataFiles)
        {
            try
            {
                Workbook workbook = ExcelFactory.create(dataFile);

                for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++)
                {
                    Sheet sheet = workbook.getSheetAt(sheetIndex);

                    if (sheet.getPhysicalNumberOfRows() == 0 || "Row #".equals(ExcelFactory.getCellContentsAt(sheet, 0, 0)))
                    {
                        continue;
                    }

                    String analyteName = sheet.getSheetName();
                    Map.Entry<Analyte, List<LuminexDataRow>> analyteEntry = ensureAnalyte(new Analyte(analyteName), _sheets);
                    Analyte analyte = analyteEntry.getKey();
                    List<LuminexDataRow> dataRows = analyteEntry.getValue();

                    int row = handleHeaderOrFooterRow(sheet, 0, analyte, dataFile);

                    // Skip over the blank line
                    row++;

                    List<String> colNames = new ArrayList<String>();
                    if (row <= sheet.getLastRowNum())
                    {
                        Row r = sheet.getRow(row);
                        if (r != null)
                        {
                            for (Cell cell : r)
                                colNames.add(ExcelFactory.getCellStringValue(cell));
                        }
                        row++;
                    }

                    Map<String, Integer> potentialTitrationCounts = new CaseInsensitiveHashMap<Integer>();
                    Map<String, Titration> potentialTitrations = new CaseInsensitiveHashMap<Titration>();

                    if (row <= sheet.getLastRowNum())
                    {
                        boolean hasMoreRows;
                        do
                        {
                            LuminexDataRow dataRow = createDataRow(sheet, colNames, row, dataFile);

                            if (dataRow.getDescription() != null)
                            {
                                Integer count = potentialTitrationCounts.get(dataRow.getDescription());
                                potentialTitrationCounts.put(dataRow.getDescription(), count == null ? 1 : count.intValue() + 1);

                                if (!potentialTitrations.containsKey(dataRow.getDescription()))
                                {
                                    Titration newTitration = new Titration();
                                    newTitration.setName(dataRow.getDescription());
                                    if (dataRow.getType() != null)
                                    {
                                        newTitration.setStandard(dataRow.getType().toUpperCase().startsWith("S") || dataRow.getType().toUpperCase().startsWith("ES"));
                                        newTitration.setQcControl(dataRow.getType().toUpperCase().startsWith("C"));
                                        newTitration.setUnknown(dataRow.getType().toUpperCase().startsWith("X"));
                                    }

                                    potentialTitrations.put(dataRow.getDescription(), newTitration);
                                }
                            }

                            dataRows.add(dataRow);
                            Pair<Boolean, Integer> nextRow = findNextDataRow(sheet, row);
                            hasMoreRows = nextRow.getKey();
                            row = nextRow.getValue();
                        }
                        while (hasMoreRows);

                        // Skip over the blank line
                        row++;
                    }
                    while (row <= sheet.getLastRowNum())
                    {
                        row = handleHeaderOrFooterRow(sheet, row, analyte, dataFile);
                    }

                    // Check if we've accumulated enough instances to consider it to be a titration
                    for (Map.Entry<String, Integer> entry : potentialTitrationCounts.entrySet())
                    {
                        if (entry.getValue().intValue() >= LuminexDataHandler.MINIMUM_TITRATION_COUNT)
                        {
                            _titrations.put(entry.getKey(), potentialTitrations.get(entry.getKey()));
                        }
                    }
                }
            }
            catch (IOException e)
            {
                throw new ExperimentException("Failed to read from data file " + dataFile.getName(), e);
            }
            catch (InvalidFormatException e)
            {
                throw new XarFormatException("Failed to parse file as Excel: " + dataFile.getName(), e);
            }
        }

        boolean foundRealRow = false;
        for (List<LuminexDataRow> rows : _sheets.values())
        {
            for (LuminexDataRow row : rows)
            {
                // Look for a row that was actually populated with at least some data
                if (row.getBeadCount() != null || row.getWell() != null || row.getFi() != null || row.getType() != null)
                {
                    foundRealRow = true;
                    break;
                }
            }
        }

        // Show an error if we didn't find any real Luminex data
        if (!foundRealRow)
        {
            throw new ExperimentException("No data rows found. Most likely not a supported Luminex file.");
        }
        _parsed = true;
    }

    private Pair<Boolean, Integer> findNextDataRow(Sheet sheet, int row)
    {
        row++;
        boolean hasNext;
        if (row == sheet.getLastRowNum())
        {
            // We've run out of rows
            hasNext = false;
        }
        else if ("".equals(ExcelFactory.getCellContentsAt(sheet, 0, row)))
        {
            // Blank row - check if there are more data rows afterwards
            int peekRow = row;
            // Burn any additional blank rows
            while (peekRow < sheet.getLastRowNum() && "".equals(ExcelFactory.getCellContentsAt(sheet, 0, peekRow)))
            {
                peekRow++;
            }

            Cell cell = sheet.getRow(peekRow).getCell(0);
            if ("Analyte".equals(ExcelFactory.getCellStringValue(cell)) && sheet.getLastRowNum() > peekRow)
            {
                return new Pair<Boolean, Integer>(true, peekRow + 1);
            }

            hasNext = false;
        }
        else
        {
            hasNext = true;
        }

        return new Pair<Boolean, Integer>(hasNext, row);
    }

    public static Map.Entry<Analyte, List<LuminexDataRow>> ensureAnalyte(Analyte analyte, Map<Analyte, List<LuminexDataRow>> sheets)
    {
        List<LuminexDataRow> dataRows = null;

        Analyte matchingAnalyte = null;
        // Might need to merge data rows for analytes across files
        for (Map.Entry<Analyte, List<LuminexDataRow>> entry : sheets.entrySet())
        {
            // Need to check both the name of the sheet (which might have been truncated) and the full
            // name of all analytes already parsed to see if we have a match
            if (analyte.getName().equals(entry.getKey().getName()) || analyte.getName().equals(entry.getKey().getSheetName()))
            {
                matchingAnalyte = entry.getKey();
                dataRows = entry.getValue();
            }
        }
        if (matchingAnalyte == null)
        {
            matchingAnalyte = analyte;
            dataRows = new ArrayList<LuminexDataRow>();
            sheets.put(matchingAnalyte, dataRows);
        }

        return new Pair<Analyte, List<LuminexDataRow>>(matchingAnalyte, dataRows);
    }

    public Set<String> getTitrations() throws ExperimentException
     {
         parseFile();
        return _titrations.keySet();
    }

    public Map<String, Titration> getTitrationsWithTypes() throws ExperimentException
    {
        parseFile();
         return _titrations;
     }

    public int getStandardTitrationCount() throws ExperimentException
    {
        parseFile();

        int count = 0;
        for (Map.Entry<String, Titration> titrationEntry : getTitrationsWithTypes().entrySet())
        {
            if (titrationEntry.getValue().isStandard())
            {
                count++;
            }
        }
        return count;
    }

    public Map<Analyte, List<LuminexDataRow>> getSheets() throws ExperimentException
    {
        parseFile();
        return _sheets;
    }

    public Map<DomainProperty, String> getExcelRunProps(File file) throws ExperimentException
    {
        parseFile();
        return _excelRunProps.get(file);
    }

    private int handleHeaderOrFooterRow(Sheet analyteSheet, int row, Analyte analyte, File dataFile)
    {
        if (row > analyteSheet.getLastRowNum())
        {
            return row;
        }

        Map<String, DomainProperty> excelProps = _excelRunDomain.createImportMap(true);
        Map<DomainProperty, String> excelValues = _excelRunProps.get(dataFile);
        if (excelValues == null)
        {
            excelValues = new HashMap<DomainProperty, String>();
            _excelRunProps.put(dataFile, excelValues);
        }

        do
        {
            String cellContents = ExcelFactory.getCellContentsAt(analyteSheet, 0, row);
            int index = cellContents.indexOf(":");
            if (index != -1)
            {
                String propName = cellContents.substring(0, index);
                String value = cellContents.substring((propName + ":").length()).trim();

                if ("Regression Type".equalsIgnoreCase(propName))
                {
                    analyte.setRegressionType(value);
                }
                else if ("Std. Curve".equalsIgnoreCase(propName))
                {
                    analyte.setStdCurve(value);
                }
                else if ("Analyte".equalsIgnoreCase(propName))
                {
                    // Sheet names may have been truncated, so set the name based on the full value within the sheet
                    analyte.setName(value);
                }

                DomainProperty pd = excelProps.get(propName);
                if (pd != null)
                {
                    excelValues.put(pd, value);
                }
            }

            String recoveryPrefix = "conc in range = unknown sample concentrations within range where standards recovery is ";
            if (cellContents.toLowerCase().startsWith(recoveryPrefix))
            {
                String recoveryString = cellContents.substring(recoveryPrefix.length()).trim();
                int charIndex = 0;

                StringBuilder minSB = new StringBuilder();
                while (charIndex < recoveryString.length() && Character.isDigit(recoveryString.charAt(charIndex)))
                {
                    minSB.append(recoveryString.charAt(charIndex));
                    charIndex++;
                }

                while (charIndex < recoveryString.length() && !Character.isDigit(recoveryString.charAt(charIndex)))
                {
                    charIndex++;
                }

                StringBuilder maxSB = new StringBuilder();
                while (charIndex < recoveryString.length() && Character.isDigit(recoveryString.charAt(charIndex)))
                {
                    maxSB.append(recoveryString.charAt(charIndex));
                    charIndex++;
                }

                analyte.setMinStandardRecovery(Integer.parseInt(minSB.toString()));
                analyte.setMaxStandardRecovery(Integer.parseInt(maxSB.toString()));
            }

            if (cellContents.toLowerCase().startsWith("fitprob. "))
            {
                int startIndex = cellContents.indexOf("=");
                int endIndex = cellContents.indexOf(",");
                if (startIndex >= 0 && endIndex >= 0 && endIndex > startIndex)
                {
                    String fitProbValue = cellContents.substring(startIndex + 1, endIndex).trim();
                    analyte.setFitProb(Double.parseDouble(fitProbValue));
                }
                startIndex = cellContents.lastIndexOf("=");
                if (startIndex >= 0)
                {
                    String resVarValue = cellContents.substring(startIndex + 1).trim();
                    analyte.setResVar(Double.parseDouble(resVarValue));
                }
            }
        }
        while (++row <= analyteSheet.getLastRowNum() && !"".equals(ExcelFactory.getCellContentsAt(analyteSheet, 0, row)));
        return row;
    }

    private LuminexDataRow createDataRow(Sheet sheet, List<String> colNames, int rowIdx, File dataFile) throws ExperimentException
    {
        LuminexDataRow dataRow = new LuminexDataRow();
        dataRow.setLsid(new Lsid(LuminexAssayProvider.LUMINEX_DATA_ROW_LSID_PREFIX, GUID.makeGUID()).toString());
        dataRow.setDataFile(dataFile.getName());
        Row row = sheet.getRow(rowIdx);
        if (row != null)
        {
            for (int col=0; col < row.getLastCellNum(); col++)
            {
                Cell cell = row.getCell(col);
                if (colNames.size() <= col)
                {
                    throw new ExperimentException("Unable to find header for column index " + col + ". This is likely not a supported Luminex file format.");
                }
                String columnName = colNames.get(col);

                String value = ExcelFactory.getCellStringValue(cell).trim();

                try
                {
                    if ("FI".equalsIgnoreCase(columnName))
                    {
                        dataRow.setFiString(StringUtils.trimToNull(value));
                        dataRow.setFi(LuminexDataHandler.determineOutOfRange(value).getValue(value));
                    }
                    else if ("FI - Bkgd".equalsIgnoreCase(columnName))
                    {
                        dataRow.setFiBackgroundString(StringUtils.trimToNull(value));
                        dataRow.setFiBackground(LuminexDataHandler.determineOutOfRange(value).getValue(value));
                    }
                    else if ("Type".equalsIgnoreCase(columnName))
                    {
                        dataRow.setType(StringUtils.trimToNull(value));
                        if (value != null)
                        {
                            String upper = value.toUpperCase();
                            if (upper.startsWith("S") || upper.startsWith("ES"))
                            {
                                dataRow.setWellRole("Standard");
                            }
                            if (upper.startsWith("C"))
                            {
                                dataRow.setWellRole("Control");
                            }
                            if (upper.startsWith("U") || upper.startsWith("X"))
                            {
                                dataRow.setWellRole("Unknown");
                            }
                            if (upper.startsWith("B"))
                            {
                                dataRow.setWellRole("Background");
                            }
                        }
                    }
                    else if ("Well".equalsIgnoreCase(columnName))
                    {
                        String trimmedValue = StringUtils.trimToNull(value);
                        dataRow.setWell(trimmedValue);
                        boolean summary = trimmedValue != null && trimmedValue.contains(",");
                        dataRow.setSummary(summary);
                    }
                    else if ("%CV".equalsIgnoreCase(columnName))
                    {
                        Double doubleValue = LuminexDataHandler.determineOutOfRange(value).getValue(value);
                        if (doubleValue != null)
                        {
                            // We store the values as 1 == 100%, so translate from the Excel file's values
                            doubleValue = doubleValue.doubleValue() / 100.0;
                        }
                        dataRow.setCv(doubleValue);
                    }
                    else if ("Outlier".equalsIgnoreCase(columnName))
                    {
                        double outlier = 0;
                        if (value != null && !"".equals(value.trim()))
                        {
                            outlier = cell.getNumericCellValue();
                        }
                        dataRow.setOutlier((int)outlier);
                    }
                    else if ("Description".equalsIgnoreCase(columnName))
                    {
                        dataRow.setDescription(StringUtils.trimToNull(value));
                    }
                    else if ("Std Dev".equalsIgnoreCase(columnName))
                    {
                        dataRow.setStdDevString(StringUtils.trimToNull(value));
                        dataRow.setStdDev(LuminexDataHandler.determineOutOfRange(value).getValue(value));
                    }
                    else if ("Exp Conc".equalsIgnoreCase(columnName))
                    {
                        dataRow.setExpConc(LuminexDataHandler.determineOutOfRange(value).getValue(value));
                    }
                    else if ("Obs Conc".equalsIgnoreCase(columnName))
                    {
                        dataRow.setObsConcString(StringUtils.trimToNull(value));
                        dataRow.setObsConc(LuminexDataHandler.determineOutOfRange(value).getValue(value));
                    }
                    else if ("(Obs/Exp) * 100".equalsIgnoreCase(columnName))
                    {
                        if (!value.equals("***"))
                        {
                            dataRow.setObsOverExp(parseDouble(value));
                        }
                    }
                    else if ("Conc in Range".equalsIgnoreCase(columnName))
                    {
                        dataRow.setConcInRangeString(StringUtils.trimToNull(value));
                        dataRow.setConcInRange(LuminexDataHandler.determineOutOfRange(value).getValue(value));
                    }
                    else if ("Ratio".equalsIgnoreCase(columnName))
                    {
                        dataRow.setRatio(StringUtils.trimToNull(value));
                    }
                    else if ("Bead Count".equalsIgnoreCase(columnName) || "BeadCount".equalsIgnoreCase(columnName))
                    {
                        Double beadCount = parseDouble(value);
                        dataRow.setBeadCount(beadCount == null ? null : beadCount.intValue());
                    }
                    else if ("Dilution".equalsIgnoreCase(columnName))
                    {
                        String dilutionValue = value;
                        if (dilutionValue != null && dilutionValue.startsWith("1:"))
                        {
                            dilutionValue = dilutionValue.substring("1:".length());
                        }
                        Double dilution = parseDouble(dilutionValue);
                        if (dilution != null && dilution.doubleValue() == 0.0)
                        {
                            throw new ExperimentException("Dilution values must not be zero");
                        }
                        dataRow.setDilution(dilution);
                    }
                    else if ("Group".equalsIgnoreCase(columnName))
                    {
                        dataRow.setDataRowGroup(StringUtils.trimToNull(value));
                    }
                    else if ("Sampling Errors".equalsIgnoreCase(columnName))
                    {
                        dataRow.setSamplingErrors(StringUtils.trimToNull(value));
                    }
                }
                catch (NumberFormatException e)
                {
                    throw new ExperimentException("Unable to parse " + columnName + " value as a number: " + value);
                }
            }
        }
        return dataRow;
    }

    private Double parseDouble(String value)
    {
        if (value == null || "".equals(value))
        {
            return null;
        }
        else return Double.parseDouble(value);
    }

    public boolean isImported()
    {
        return _imported;
    }

    public void setImported(boolean imported)
    {
        _imported = imported;
    }

    public static class TestCase extends Assert
    {
        @Test
        public void testRaw() throws ExperimentException
        {
            LuminexExcelParser parser = createParser("plate 1_IgA-Biot (b12 IgA std).xls");
            if (parser == null) return;
            Map<Analyte, List<LuminexDataRow>> m = parser.getSheets();
            assertEquals("Wrong number of analytes", 5, m.size());
            validateAnalyte(m.keySet(), "VRC A 5304 gp140 (62)", "FI = 0.582906 + (167.081 - 0.582906) / ((1 + (Conc / 0.531813)^-5.30023))^0.1", .4790, .8266);
            for (Map.Entry<Analyte, List<LuminexDataRow>> entry : m.entrySet())
            {
                assertEquals("Wrong number of data rows", 34, entry.getValue().size());
                for (LuminexDataRow dataRow : entry.getValue())
                {
                    assertFalse("Shouldn't be summary", dataRow.isSummary());
                }
            }
        }

        private void validateAnalyte(Set<Analyte> analytes, String name, String curveFit, Double fitProb, Double resVar)
        {
            for (Analyte analyte : analytes)
            {
                if (name.equals(analyte.getName()))
                {
                    assertEquals("Curve fit", curveFit, analyte.getStdCurve());
                    assertEquals("Fit prob", fitProb, analyte.getFitProb());
                    assertEquals("Res var", resVar, analyte.getResVar());

                    // Found it, so return
                    return;
                }
            }
            fail("Analyte " + name + " was not found");
        }

        @Test
        public void testSummary() throws ExperimentException
        {
            LuminexExcelParser parser = createParser("Guide Set plate 2.xls");
            if (parser == null) return;
            
            Map<Analyte, List<LuminexDataRow>> m = parser.getSheets();
            assertEquals("Wrong number of analytes", 2, m.size());
            validateAnalyte(m.keySet(), "GS Analyte (1)", null, null, null);
            for (Map.Entry<Analyte, List<LuminexDataRow>> entry : m.entrySet())
            {
                assertEquals("Wrong number of data rows", 11, entry.getValue().size());
                for (LuminexDataRow dataRow : entry.getValue())
                {
                    assertTrue("Should be summary", dataRow.isSummary());
                }
            }
        }

        @Test
        public void testSummaryAndRaw() throws ExperimentException
        {
            LuminexExcelParser parser = createParser("RawAndSummary.xlsx");
            if (parser == null) return;

            Map<Analyte, List<LuminexDataRow>> m = parser.getSheets();
            assertEquals("Wrong number of analytes", 3, m.size());
            validateAnalyte(m.keySet(), "Analyte2", "FI = -1.29301 + (490671 + 1.29301) / ((1 + (Conc / 9511.48)^-3.75411))^0.291452", .0136, 2.8665);
            for (Map.Entry<Analyte, List<LuminexDataRow>> entry : m.entrySet())
            {
                assertEquals("Wrong number of data rows", 36, entry.getValue().size());
                int summaryCount = 0;
                int rawCount = 0;
                for (LuminexDataRow dataRow : entry.getValue())
                {
                    if (dataRow.isSummary())
                    {
                        summaryCount++;
                    }
                    else
                    {
                        rawCount++;
                    }
                }
                assertEquals("Wrong number of raw data rows", 24, rawCount);
                assertEquals("Wrong number of summary data rows", 12, summaryCount);
            }
        }

        private LuminexExcelParser createParser(String fileName)
        {
            AppProps props = AppProps.getInstance();
            if (!props.isDevMode()) // We can only run the excel tests if we're in dev mode and have access to our samples
                return null;

            String projectRootPath = props.getProjectRoot();
            File projectRoot = new File(projectRootPath);
            File luminexDir = new File(projectRoot, "sampledata/Luminex/");
            assertTrue("Couldn't find " + luminexDir, luminexDir.isDirectory());

            Domain dummyDomain = PropertyService.get().createDomain(ContainerManager.getRoot(), "fakeURI", "dummyDomain");
            return new LuminexExcelParser(dummyDomain, Arrays.asList(new File(luminexDir, fileName)));
        }
    }
}
