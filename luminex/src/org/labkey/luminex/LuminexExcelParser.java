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
package org.labkey.luminex;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
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
import org.labkey.api.util.GUID;
import org.labkey.api.util.JunitUtil;
import org.labkey.api.util.NumberUtilsLabKey;
import org.labkey.api.util.Pair;
import org.labkey.luminex.model.Analyte;
import org.labkey.luminex.model.LuminexDataRow;
import org.labkey.luminex.model.SinglePointControl;
import org.labkey.luminex.model.Titration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Parses the Excel Luminex files with one analyte per sheet, each sheet has header, footer, and a grid of data
 * User: jeckels
 * Date: Jun 8, 2011
*/
public class LuminexExcelParser
{
    private Collection<File> _dataFiles;
    @Nullable
    private Domain _excelRunDomain;
    private Map<Analyte, List<LuminexDataRow>> _sheets = new LinkedHashMap<>();
    private Map<File, Map<DomainProperty, String>> _excelRunProps = new HashMap<>();
    private Map<String, Titration> _titrations = new TreeMap<>();
    private Map<String, SinglePointControl> _singlePointControls = new TreeMap<>();
    private boolean _parsed;
    private boolean _imported;

    public LuminexExcelParser(ExpProtocol protocol, Collection<File> dataFiles)
    {
        this(LuminexAssayProvider.getExcelRunDomain(protocol), dataFiles);
    }

    private LuminexExcelParser(Domain excelRunDomain, Collection<File> dataFiles)
    {
        _excelRunDomain = excelRunDomain;
        _dataFiles = dataFiles;
    }

    private void parseFile() throws ExperimentException
    {
        if (_parsed) return;
        Set<String> analyteNames = new HashSet<>();

        for (File dataFile : _dataFiles)
        {
            analyteNames.clear();
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

                    String analyteName = analyteNameFromName(sheet.getSheetName());
                    String beadNumber = beadNumberFromName(sheet.getSheetName());

                    // verify the each tab has a unique analyte name for the workbook
                    if (!analyteNames.contains(analyteName))
                        analyteNames.add(analyteName);
                    else
                        throw new ExperimentException("All sheets in the data file must have a unique analyte name in the tab label. The analyte name excludes the bead number portion of the label.");

                    Map.Entry<Analyte, List<LuminexDataRow>> analyteEntry = ensureAnalyte(new Analyte(analyteName, beadNumber), _sheets);
                    Analyte analyte = analyteEntry.getKey();
                    List<LuminexDataRow> dataRows = analyteEntry.getValue();

                    int row = handleHeaderOrFooterRow(sheet, 0, analyte, dataFile);

                    // Skip over the blank line
                    row++;

                    List<String> colNames = new ArrayList<>();
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

                    Map<String, Integer> potentialTitrationRawCounts = new CaseInsensitiveHashMap<>();
                    Map<String, Integer> potentialTitrationSummaryCounts = new CaseInsensitiveHashMap<>();
                    Map<String, Titration> potentialTitrations = new CaseInsensitiveHashMap<>();

                    if (row <= sheet.getLastRowNum())
                    {
                        boolean hasMoreRows;
                        do
                        {
                            LuminexDataRow dataRow = createDataRow(analyte, sheet, colNames, row, dataFile);

                            if (dataRow.getDescription() != null)
                            {
                                // track the number of rows per sample for summary and raw separately
                                if (dataRow.isSummary())
                                {
                                    Integer count = potentialTitrationSummaryCounts.get(dataRow.getDescription());
                                    potentialTitrationSummaryCounts.put(dataRow.getDescription(), count == null ? 1 : count + 1);
                                }
                                else
                                {
                                    Integer count = potentialTitrationRawCounts.get(dataRow.getDescription());
                                    potentialTitrationRawCounts.put(dataRow.getDescription(), count == null ? 1 : count + 1);
                                }

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
                    for (String desc : potentialTitrations.keySet())
                    {
                        if ((potentialTitrationSummaryCounts.get(desc) != null && potentialTitrationSummaryCounts.get(desc) >= LuminexDataHandler.MINIMUM_TITRATION_SUMMARY_COUNT) ||
                            (potentialTitrationRawCounts.get(desc) != null && potentialTitrationRawCounts.get(desc) >= LuminexDataHandler.MINIMUM_TITRATION_RAW_COUNT))
                        {
                            _titrations.put(desc, potentialTitrations.get(desc));
                        }
                        // detect potential single point controls
                        if (potentialTitrations.get(desc) != null && potentialTitrations.get(desc).isQcControl()) // Type starts with 'C'
                        {
                            if ((potentialTitrationSummaryCounts.get(desc) != null && potentialTitrationSummaryCounts.get(desc) == LuminexDataHandler.SINGLE_POINT_CONTROL_SUMMARY_COUNT) ||
                                    (potentialTitrationRawCounts.get(desc) != null && potentialTitrationRawCounts.get(desc) <= LuminexDataHandler.SINGLE_POINT_CONTROL_RAW_COUNT))
                            {
                                _singlePointControls.put(desc, new SinglePointControl(potentialTitrations.get(desc)));
                            }
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
                return new Pair<>(true, peekRow + 1);
            }

            hasNext = false;
        }
        else
        {
            hasNext = true;
        }

        return new Pair<>(hasNext, row);
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
            if (analyte.getName().equals(entry.getKey().getName()) || analyte.getName().equals(analyteNameFromName(entry.getKey().getSheetName())))
            {
                matchingAnalyte = entry.getKey();
                dataRows = entry.getValue();
            }
        }
        if (matchingAnalyte == null)
        {
            matchingAnalyte = analyte;
            dataRows = new ArrayList<>();
            sheets.put(matchingAnalyte, dataRows);
        }

        return new Pair<>(matchingAnalyte, dataRows);
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

    public Set<String> getSinglePointControls() throws ExperimentException
    {
        parseFile();
        return _singlePointControls.keySet();
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
        Map<DomainProperty, String> result = _excelRunProps.get(file);
        if (result == null)
        {
            throw new IllegalArgumentException("No properties found for file: " + file);
        }
        return result;
    }

    private int handleHeaderOrFooterRow(Sheet analyteSheet, int row, Analyte analyte, File dataFile) throws ExperimentException
    {
        if (row > analyteSheet.getLastRowNum())
        {
            return row;
        }

        Map<String, DomainProperty> excelProps = _excelRunDomain.createImportMap(true);
        Map<DomainProperty, String> excelValues = _excelRunProps.get(dataFile);
        if (excelValues == null)
        {
            excelValues = new HashMap<>();
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
                if (value.isEmpty())
                {
                    value = null;
                }

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
                    analyte.setName(analyteNameFromName(value));
                    analyte.setBeadNumber(beadNumberFromName(value));
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
                    if (fitProbValue != null && !NumberUtilsLabKey.isNumber(fitProbValue))
                        throw new ExperimentException("FitProb. value is not numeric: " + fitProbValue);

                    analyte.setFitProb(parseDouble(fitProbValue));
                }
                startIndex = cellContents.lastIndexOf("=");
                if (startIndex >= 0)
                {
                    String resVarValue = cellContents.substring(startIndex + 1).trim();
                    if (resVarValue != null && !NumberUtilsLabKey.isNumber(resVarValue))
                        throw new ExperimentException("ResVar. value is not numeric: " + resVarValue);

                    analyte.setResVar(parseDouble(resVarValue));
                }
            }
        }
        while (++row <= analyteSheet.getLastRowNum() && !"".equals(ExcelFactory.getCellContentsAt(analyteSheet, 0, row)));
        return row;
    }

    private LuminexDataRow createDataRow(Analyte analyte, Sheet sheet, List<String> colNames, int rowIdx, File dataFile) throws ExperimentException
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
                if (cell == null)
                {
                    continue;
                }
                if (colNames.size() <= col)
                {
                    throw new ExperimentException("Unable to find header for cell " + ExcelFactory.getCellLocationDescription(col, row.getRowNum(), analyteNameFromName(sheet.getSheetName())) + ". This is likely not a supported Luminex file format.");
                }
                String columnName = colNames.get(col);

                String value = ExcelFactory.getCellStringValue(cell).trim();

                try
                {
                    if ("FI".equalsIgnoreCase(columnName))
                    {
                        dataRow.setFiString(StringUtils.trimToNull(value));
                        dataRow.setFi(getCellDoubleValue(cell, value));
                    }
                    else if ("FI - Bkgd".equalsIgnoreCase(columnName))
                    {
                        dataRow.setFiBackgroundString(StringUtils.trimToNull(value));
                        dataRow.setFiBackground(getCellDoubleValue(cell, value));
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
                        if (trimmedValue != null)
                        {
                            trimmedValue = trimmedValue.replaceAll("\\s+", ",");
                            dataRow.setWell(trimmedValue);
                            boolean summary = trimmedValue != null && trimmedValue.contains(",");
                            dataRow.setSummary(summary);
                        }
                    }
                    else if ("%CV".equalsIgnoreCase(columnName))
                    {
                        Double doubleValue = getCellDoubleValue(cell, value);
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
                        dataRow.setStdDev(getCellDoubleValue(cell, value));
                    }
                    else if ("Exp Conc".equalsIgnoreCase(columnName))
                    {
                        dataRow.setExpConc(getCellDoubleValue(cell, value));
                    }
                    else if ("Obs Conc".equalsIgnoreCase(columnName))
                    {
                        dataRow.setObsConcString(StringUtils.trimToNull(value));
                        dataRow.setObsConc(getCellDoubleValue(cell, value));
                    }
                    else if ("(Obs/Exp) * 100".equalsIgnoreCase(columnName))
                    {
                        if (!value.equals("***"))
                        {
                            dataRow.setObsOverExp(getCellDoubleValue(cell, value));
                        }
                    }
                    else if ("Conc in Range".equalsIgnoreCase(columnName))
                    {
                        dataRow.setConcInRangeString(StringUtils.trimToNull(value));
                        dataRow.setConcInRange(getCellDoubleValue(cell, value));
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
                        Double dilution = getCellDoubleValue(cell, dilutionValue);
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
                    else if ("Analyte".equalsIgnoreCase(columnName))
                    {
                        // validate that all analyte names match the property value for this sheet
                        String analyteName = analyteNameFromName(StringUtils.trimToNull(value));
                        if (!analyte.getName().equals(analyteName))
                        {
                            throw new ExperimentException("The analyte name for this data row : '" + analyteName + "' did not match the expected name of: '" + analyte.getName() + "'");
                        }
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

    private Double getCellDoubleValue(Cell cell, String value)
    {
        // Issue 27424: if the value is not blank and a valid number, use cell.getNumericCellValue()
        LuminexOORIndicator oorIndicator = LuminexDataHandler.determineOutOfRange(value);
        if (oorIndicator == LuminexOORIndicator.IN_RANGE && ExcelFactory.isCellNumeric(cell))
        {
            return cell.getNumericCellValue();
        }
        else
        {
            return oorIndicator.getValue(value);
        }
    }

    private Double parseDouble(String value)
    {
        if (value == null || "".equals(value) || !NumberUtilsLabKey.isNumber(value))
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

    /**
     * Parses out the analyte name from the name/bead number conjugate
     */
    private static String analyteNameFromName(String name)
    {
        if (name != null)
        {
            int idx1 = name.lastIndexOf('(');
            int idx2 = name.lastIndexOf(')');

            if (idx1 != -1 && idx2 != -1 && (idx1 < idx2))
            {
                return name.substring(0, idx1).trim();
            }
        }
        return name;
    }

    /**
     * Parses out the bead number from the analyte name/bead number conjugate
     */
    private static String beadNumberFromName(String name)
    {
        if (name != null)
        {
            int idx1 = name.lastIndexOf('(');
            int idx2 = name.lastIndexOf(')');

            if (idx1 != -1 && idx2 != -1 && (idx1 < idx2))
            {
                return name.substring(idx1+1, idx2).trim();
            }
        }
        return null;
    }

    public static class TestCase extends Assert
    {
        @Test
        public void testRaw() throws ExperimentException, IOException
        {
            LuminexExcelParser parser = createParser("plate 1_IgA-Biot (Standard2).xls");
            Map<Analyte, List<LuminexDataRow>> m = parser.getSheets();
            assertEquals("Wrong number of analytes", 5, m.size());
            validateAnalyte(m.keySet(), "ENV6", "FI = 0.582906 + (167.081 - 0.582906) / ((1 + (Conc / 0.531813)^-5.30023))^0.1", .4790, .8266);
            for (Map.Entry<Analyte, List<LuminexDataRow>> entry : m.entrySet())
            {
                assertEquals("Wrong number of data rows", 36, entry.getValue().size());
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
        public void testSummary() throws ExperimentException, IOException
        {
            LuminexExcelParser parser = createParser("Guide Set plate 2.xls");

            Map<Analyte, List<LuminexDataRow>> m = parser.getSheets();
            assertEquals("Wrong number of analytes", 2, m.size());
            validateAnalyte(m.keySet(), "GS Analyte A", null, null, null);
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
        public void testSummaryAndRaw() throws ExperimentException, IOException
        {
            LuminexExcelParser parser = createParser("RawAndSummary.xlsx");

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

        private LuminexExcelParser createParser(String fileName) throws IOException
        {
            File luminexDir = JunitUtil.getSampleData(null, "Luminex");
            assertTrue("Couldn't find " + luminexDir, null != luminexDir && luminexDir.isDirectory());

            Domain dummyDomain = PropertyService.get().createDomain(ContainerManager.getRoot(), "fakeURI", "dummyDomain");
            return new LuminexExcelParser(dummyDomain, Arrays.asList(new File(luminexDir, fileName)));
        }
    }
}
