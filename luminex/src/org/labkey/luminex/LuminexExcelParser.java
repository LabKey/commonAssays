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

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.Container;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.XarFormatException;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.reader.ExcelFactory;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.util.GUID;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
    private ExpProtocol _protocol;
    private Map<Analyte, List<LuminexDataRow>> _sheets = new LinkedHashMap<Analyte, List<LuminexDataRow>>();
    private Map<File, Map<DomainProperty, String>> _excelRunProps = new HashMap<File, Map<DomainProperty, String>>();
    private Map<String, Titration> _titrations = new TreeMap<String, Titration>();
    private boolean _parsed;
    private boolean _imported;

    public LuminexExcelParser(ExpProtocol protocol, Collection<File> dataFiles)
    {
        _protocol = protocol;
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

                Container container = _protocol.getContainer();
                String excelRunDomainURI = AbstractAssayProvider.getDomainURIForPrefix(_protocol, LuminexAssayProvider.ASSAY_DOMAIN_EXCEL_RUN);
                Domain excelRunDomain = PropertyService.get().getDomain(container, excelRunDomainURI);

                for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++)
                {
                    Sheet sheet = workbook.getSheetAt(sheetIndex);

                    if (sheet.getPhysicalNumberOfRows() == 0 || "Row #".equals(ExcelFactory.getCellContentsAt(sheet, 0, 0)))
                    {
                        continue;
                    }

                    String analyteName = sheet.getSheetName();
                    Analyte analyte = null;
                    List<LuminexDataRow> dataRows = null;
                    // Might need to merge data rows for analytes across files
                    for (Map.Entry<Analyte, List<LuminexDataRow>> entry : _sheets.entrySet())
                    {
                        // Need to check both the name of the sheet (which might have been truncated) and the full
                        // name of all analytes already parsed to see if we have a match
                        if (analyteName.equals(entry.getKey().getName()) || analyteName.equals(entry.getKey().getSheetName()))
                        {
                            analyte = entry.getKey();
                            dataRows = entry.getValue();
                        }
                    }
                    if (analyte == null)
                    {
                        analyte = new Analyte(analyteName);
                        dataRows = new ArrayList<LuminexDataRow>();
                        _sheets.put(analyte, dataRows);
                    }

                    int row = handleHeaderOrFooterRow(sheet, 0, analyte, excelRunDomain, dataFile);

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
                        do
                        {
                            LuminexDataRow dataRow = createDataRow(sheet, colNames, row, dataFile);

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

                            dataRows.add(dataRow);
                        }
                        while (++row <= sheet.getLastRowNum() && !"".equals(ExcelFactory.getCellContentsAt(sheet, 0, row)));

                        // Skip over the blank line
                        row++;
                    }
                    while (row <= sheet.getLastRowNum())
                    {
                        row = handleHeaderOrFooterRow(sheet, row, analyte, excelRunDomain, dataFile);
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
        _parsed = true;
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

    private int handleHeaderOrFooterRow(Sheet analyteSheet, int row, Analyte analyte, Domain excelRunDomain, File dataFile)
    {
        if (row > analyteSheet.getLastRowNum())
        {
            return row;
        }

        Map<String, DomainProperty> excelProps = excelRunDomain.createImportMap(true);
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
                    assert analyte.getName().startsWith(value);
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
                if ("FI".equalsIgnoreCase(columnName))
                {
                    dataRow.setFiString(value);
                    dataRow.setFi(LuminexDataHandler.determineOutOfRange(value).getValue(value));
                }
                else if ("FI - Bkgd".equalsIgnoreCase(columnName))
                {
                    dataRow.setFiBackgroundString(value);
                    dataRow.setFiBackground(LuminexDataHandler.determineOutOfRange(value).getValue(value));
                }
                else if ("Type".equalsIgnoreCase(columnName))
                {
                    dataRow.setType(value);
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
                    dataRow.setWell(value);
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
                    dataRow.setDescription(value);
                }
                else if ("Std Dev".equalsIgnoreCase(columnName))
                {
                    dataRow.setStdDevString(value);
                    dataRow.setStdDev(LuminexDataHandler.determineOutOfRange(value).getValue(value));
                }
                else if ("Exp Conc".equalsIgnoreCase(columnName))
                {
                    dataRow.setExpConc(parseDouble(value));
                }
                else if ("Obs Conc".equalsIgnoreCase(columnName))
                {
                    dataRow.setObsConcString(value);
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
                    dataRow.setConcInRangeString(value);
                    dataRow.setConcInRange(LuminexDataHandler.determineOutOfRange(value).getValue(value));
                }
                else if ("Ratio".equalsIgnoreCase(columnName))
                {
                    dataRow.setRatio(value);
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
                    dataRow.setDilution(parseDouble(dilutionValue));
                }
                else if ("Group".equalsIgnoreCase(columnName))
                {
                    dataRow.setDataRowGroup(value);
                }
                else if ("Sampling Errors".equalsIgnoreCase(columnName))
                {
                    dataRow.setSamplingErrors(value);
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
}
