/*
 * Copyright (c) 2007-2010 LabKey Corporation
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
import org.labkey.api.data.Container;
import org.labkey.api.data.ObjectFactory;
import org.labkey.api.exp.*;
import org.labkey.api.exp.api.*;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.qc.TransformDataHandler;
import org.labkey.api.reader.ExcelFactory;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayDataType;
import org.labkey.api.util.GUID;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.security.User;
import org.labkey.api.util.FileType;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * User: jeckels
 * Date: Jul 16, 2007
 */
public class LuminexExcelDataHandler extends LuminexDataHandler implements TransformDataHandler
{
    public static final DataType LUMINEX_TRANSFORMED_DATA_TYPE = new DataType("LuminexTransformedDataFile");  // marker data type
    public static final AssayDataType LUMINEX_DATA_TYPE = new AssayDataType("LuminexDataFile", new FileType(Arrays.asList(".xls", ".xlsx"), ".xls"));

    public Map<DataType, List<Map<String, Object>>> getValidationDataMap(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        ExpProtocol protocol = data.getRun().getProtocol();
        LuminexDataFileParser parser = getDataFileParser(protocol, dataFile);

        Map<DataType, List<Map<String, Object>>> datas = new HashMap<DataType, List<Map<String, Object>>>();
        List<Map<String, Object>> dataRows = new ArrayList<Map<String, Object>>();

        for (Map.Entry<Analyte, List<LuminexDataRow>> entry : parser.getSheets().entrySet())
        {
            for (LuminexDataRow dataRow : entry.getValue())
            {
                dataRows.add(dataRow.toMap(entry.getKey()));
            }
        }
        datas.put(LUMINEX_TRANSFORMED_DATA_TYPE, dataRows);
        return datas;
    }

    public void importTransformDataMap(ExpData data, User user, ExpRun run, ExpProtocol protocol, AssayProvider provider, List<Map<String, Object>> dataMap) throws ExperimentException
    {
        ObjectFactory<Analyte> analyteFactory = ObjectFactory.Registry.getFactory(Analyte.class);
        if (null == analyteFactory)
            throw new ExperimentException("Could not find a matching object factory for " + Analyte.class);

        ObjectFactory<LuminexDataRow> rowFactory = ObjectFactory.Registry.getFactory(LuminexDataRow.class);
        if (null == rowFactory)
            throw new ExperimentException("Could not find a matching object factory for " + LuminexDataRow.class);

        Map<Analyte, List<LuminexDataRow>> sheets = new LinkedHashMap<Analyte, List<LuminexDataRow>>();
        for (Map<String, Object> row : dataMap)
        {
            Analyte analyte = analyteFactory.fromMap(row);
            LuminexDataRow dataRow = rowFactory.fromMap(row);
            dataRow.setExtraProperties(row);

            if (!sheets.containsKey(analyte))
            {
                sheets.put(analyte, new ArrayList<LuminexDataRow>());
            }
            sheets.get(analyte).add(dataRow);
        }

        Map<String, Object> excelProps = Collections.emptyMap();
        importData(data, run, user, null, sheets, excelProps);
    }

    protected LuminexDataFileParser getDataFileParser(ExpProtocol protocol, File dataFile)
    {
        return new LuminexExcelParser(protocol, dataFile);
    }

    public Priority getPriority(ExpData data)
    {
        Lsid lsid = new Lsid(data.getLSID());
        if (LUMINEX_DATA_TYPE.matches(lsid))
        {
            return Priority.HIGH;
        }
        if (LUMINEX_TRANSFORMED_DATA_TYPE.matches(lsid))
        {
            return Priority.HIGH;
        }
        return null;
    }

    public static class LuminexExcelParser implements LuminexDataFileParser
    {
        private File _dataFile;
        private ExpProtocol _protocol;
        private Map<Analyte, List<LuminexDataRow>> _sheets = new LinkedHashMap<Analyte, List<LuminexDataRow>>();
        private Map<String, Object> _excelRunProps = new HashMap<String, Object>();
        private boolean _fileParsed;

        public LuminexExcelParser(ExpProtocol protocol, File dataFile)
        {
            _protocol = protocol;
            _dataFile = dataFile;
        }

        private void parseFile(File dataFile) throws ExperimentException
        {
            if (_fileParsed) return;

            try {
                Workbook workbook = ExcelFactory.create(dataFile);

                Container container = _protocol.getContainer();
                String analyteDomainURI = AbstractAssayProvider.getDomainURIForPrefix(_protocol, LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE);
                String excelRunDomainURI = AbstractAssayProvider.getDomainURIForPrefix(_protocol, LuminexAssayProvider.ASSAY_DOMAIN_EXCEL_RUN);
                Domain analyteDomain = PropertyService.get().getDomain(container, analyteDomainURI);
                Domain excelRunDomain = PropertyService.get().getDomain(container, excelRunDomainURI);

                for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++)
                {
                    Sheet sheet = workbook.getSheetAt(sheetIndex);

                    if (sheet.getPhysicalNumberOfRows() == 0 || "Row #".equals(ExcelFactory.getCellContentsAt(sheet, 0, 0)))
                    {
                        continue;
                    }

                    Analyte analyte = new Analyte(sheet.getSheetName(), 0);

                    Map<String, Object> analyteProps = new HashMap<String, Object>();

                    int row = handleHeaderOrFooterRow(sheet, 0, analyte, analyteDomain, analyteProps, excelRunDomain, _excelRunProps);

                    // Skip over the blank line
                    row++;

                    List<String> colNames = new ArrayList<String>();
                    if (row < sheet.getLastRowNum())
                    {
                        Row r = sheet.getRow(row);
                        if (r != null)
                        {
                            for (Cell cell : r)
                                colNames.add(ExcelFactory.getCellStringValue(cell));
                        }
                        row++;
                    }

                    List<LuminexDataRow> dataRows = new ArrayList<LuminexDataRow>();
                    _sheets.put(analyte, dataRows);

                    if (row < sheet.getLastRowNum())
                    {
                        do
                        {
                            LuminexDataRow dataRow = createDataRow(sheet, colNames, row);
                            dataRows.add(dataRow);
                        }
                        while (++row < sheet.getLastRowNum() && !"".equals(ExcelFactory.getCellContentsAt(sheet, 0, row)));

                        // Skip over the blank line
                        row++;
                    }
                    row = handleHeaderOrFooterRow(sheet, row, analyte, analyteDomain, analyteProps, excelRunDomain, _excelRunProps);
                    //analyte.setLsid(new Lsid("LuminexAnalyte", "Data-" + rowId + "." + analyte.getName()).toString());
                }
                _fileParsed = true;
            }
            catch (IOException e)
            {
                throw new ExperimentException("Failed to read from data file " + dataFile.getAbsolutePath(), e);
            }
            catch (InvalidFormatException e)
            {
                throw new XarFormatException("Failed to parse Excel file " + dataFile.getAbsolutePath(), e);
            }
        }

        public Map<Analyte, List<LuminexDataRow>> getSheets() throws ExperimentException
        {
            parseFile(_dataFile);
            return _sheets;
        }

        public Map<String, Object> getExcelRunProps() throws ExperimentException
        {
            parseFile(_dataFile);
            return _excelRunProps;
        }

        private int handleHeaderOrFooterRow(Sheet analyteSheet, int row, Analyte analyte, Domain analyteDomain, Map<String, Object> analyteProps, Domain excelRunDomain, Map<String, Object> excelRunProps)
        {
            if (row >= analyteSheet.getLastRowNum())
            {
                return row;
            }

            Map<String, DomainProperty> analyteMap = analyteDomain.createImportMap(true);
            Map<String, DomainProperty> excelMap = excelRunDomain.createImportMap(true);

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

                    storePropertyValue(propName, value, analyteMap, analyteProps);
                    storePropertyValue(propName, value, excelMap, excelRunProps);
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
            while (++row < analyteSheet.getLastRowNum() && !"".equals(ExcelFactory.getCellContentsAt(analyteSheet, 0, row)));
            return row;
        }

        private void storePropertyValue(String propName, String value, Map<String, DomainProperty> columns, Map<String, Object> props)
        {
            DomainProperty pd = columns.get(propName);
            if (pd != null)
            {
                props.put(pd.getPropertyURI(), value);
            }
        }

        private LuminexDataRow createDataRow(Sheet sheet, List<String> colNames, int rowIdx)
        {
            LuminexDataRow dataRow = new LuminexDataRow();
            dataRow.setLsid(new Lsid(LuminexAssayProvider.LUMINEX_DATA_ROW_LSID_PREFIX, GUID.makeGUID()).toString());
            Row row = sheet.getRow(rowIdx);
            if (row != null)
            {
                for (int col=0; col < row.getLastCellNum(); col++)
                {
                    Cell cell = row.getCell(col);
                    String columnName = colNames.get(col);

                    String value = ExcelFactory.getCellStringValue(cell).trim();
                    if ("FI".equalsIgnoreCase(columnName))
                    {
                        dataRow.setFiString(value);
                        dataRow.setFi(LuminexExcelDataHandler.determineOutOfRange(value).getValue(value));
                    }
                    else if ("FI - Bkgd".equalsIgnoreCase(columnName))
                    {
                        dataRow.setFiBackgroundString(value);
                        dataRow.setFiBackground(LuminexExcelDataHandler.determineOutOfRange(value).getValue(value));
                    }
                    else if ("Type".equalsIgnoreCase(columnName))
                    {
                        dataRow.setType(value);
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
                        dataRow.setStdDev(LuminexExcelDataHandler.determineOutOfRange(value).getValue(value));
                    }
                    else if ("Exp Conc".equalsIgnoreCase(columnName))
                    {
                        dataRow.setExpConc(parseDouble(value));
                    }
                    else if ("Obs Conc".equalsIgnoreCase(columnName))
                    {
                        dataRow.setObsConcString(value);
                        dataRow.setObsConc(LuminexExcelDataHandler.determineOutOfRange(value).getValue(value));
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
                        dataRow.setConcInRange(LuminexExcelDataHandler.determineOutOfRange(value).getValue(value));
                    }
                    else if ("Ratio".equalsIgnoreCase(columnName))
                    {
                        dataRow.setRatio(value);
                    }
                    else if ("Bead Count".equalsIgnoreCase(columnName) || "BeadCount".equalsIgnoreCase(columnName))
                    {
                        dataRow.setBeadCount(parseInteger(value));
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

        private Integer parseInteger(String value)
        {
            if (value == null || "".equals(value))
            {
                return null;
            }
            else return Integer.parseInt(value);
        }
    }
}
