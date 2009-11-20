/*
 * Copyright (c) 2007-2009 LabKey Corporation
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

import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;
import org.labkey.api.data.Container;
import org.labkey.api.data.ObjectFactory;
import org.labkey.api.exp.*;
import org.labkey.api.exp.api.*;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.qc.TransformDataHandler;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.security.User;
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
    public static final DataType LUMINEX_DATA_TYPE = new DataType("LuminexDataFile");

    public Map<DataType, List<Map<String, Object>>> loadFileData(ExpProtocol expProtocol, Domain dataDomain, File dataFile) throws IOException, ExperimentException
    {
        ExpProtocol protocol = ExperimentService.get().getExpProtocol(expProtocol.getRowId());
        LuminexDataFileParser parser = getDataFileParser(protocol, dataFile);

        Map<DataType, List<Map<String, Object>>> datas = new HashMap<DataType, List<Map<String, Object>>>();
        List<Map<String, Object>> dataRows = new ArrayList<Map<String, Object>>();

        for (Map.Entry<Analyte, List<LuminexDataRow>> entry : parser.getSheets().entrySet())
        {
            for (LuminexDataRow dataRow : entry.getValue())
            {
                dataRows.add(serializeDataRow(entry.getKey(), dataRow));
            }
        }
        datas.put(LUMINEX_DATA_TYPE, dataRows);
        return datas;
    }

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
                dataRows.add(serializeDataRow(entry.getKey(), dataRow));
            }
        }
        datas.put(LUMINEX_TRANSFORMED_DATA_TYPE, dataRows);
        return datas;
    }

    public void importTransformDataMap(ExpData data, User user, ExpRun run, ExpProtocol protocol, AssayProvider provider, List<Map<String, Object>> dataMap) throws ExperimentException
    {
        ObjectFactory<Analyte> analyteFactory = ObjectFactory.Registry.getFactory(Analyte.class);
        if (null == analyteFactory)
            throw new ExperimentException("Cound not find a matching object factory.");

        ObjectFactory<LuminexDataRow> rowFactory = ObjectFactory.Registry.getFactory(LuminexDataRow.class);
        if (null == rowFactory)
            throw new ExperimentException("Cound not find a matching object factory.");

        Map<Analyte, List<LuminexDataRow>> sheets = new LinkedHashMap<Analyte, List<LuminexDataRow>>();
        for (Map<String, Object> row : dataMap)
        {
            Analyte analyte = analyteFactory.fromMap(row);
            LuminexDataRow dataRow = rowFactory.fromMap(row);

            if (!sheets.containsKey(analyte))
            {
                sheets.put(analyte, new ArrayList<LuminexDataRow>());
            }
            sheets.get(analyte).add(dataRow);
        }

        Map<String, Object> excelProps = Collections.emptyMap();
        importData(data, run, user, null, sheets, excelProps);
    }

    protected Map<String, Object> serializeDataRow(Analyte analyte, LuminexDataRow dataRow)
    {
        Map<String, Object> row = new HashMap<String, Object>();

        ObjectFactory<Analyte> af = ObjectFactory.Registry.getFactory(Analyte.class);
        if (null == af)
            throw new IllegalArgumentException("Cound not find a matching object factory.");
        row.putAll(af.toMap(analyte, null));

        ObjectFactory<LuminexDataRow> f = ObjectFactory.Registry.getFactory(LuminexDataRow.class);
        if (null == f)
            throw new IllegalArgumentException("Cound not find a matching object factory.");
        row.putAll(f.toMap(dataRow, null));

        return row;
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

            FileInputStream fIn = null;
            try {
                fIn = new FileInputStream(dataFile);
                WorkbookSettings settings = new WorkbookSettings();
                settings.setGCDisabled(true);
                Workbook workbook = Workbook.getWorkbook(fIn, settings);

                Container container = _protocol.getContainer();
                String analyteDomainURI = AbstractAssayProvider.getDomainURIForPrefix(_protocol, LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE);
                String excelRunDomainURI = AbstractAssayProvider.getDomainURIForPrefix(_protocol, LuminexAssayProvider.ASSAY_DOMAIN_EXCEL_RUN);
                Domain analyteDomain = PropertyService.get().getDomain(container, analyteDomainURI);
                Domain excelRunDomain = PropertyService.get().getDomain(container, excelRunDomainURI);

                for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++)
                {
                    Sheet sheet = workbook.getSheet(sheetIndex);

                    if (sheet.getRows() == 0 || sheet.getColumns() == 0 || "Row #".equals(sheet.getCell(0, 0).getContents()))
                    {
                        continue;
                    }

                    Analyte analyte = new Analyte(sheet.getName(), 0);

                    Map<String, Object> analyteProps = new HashMap<String, Object>();

                    int row = handleHeaderOrFooterRow(sheet, 0, analyte, analyteDomain, analyteProps, excelRunDomain, _excelRunProps);

                    // Skip over the blank line
                    row++;

                    List<String> colNames = new ArrayList<String>();
                    if (row < sheet.getRows())
                    {
                        for (int col = 0; col < sheet.getColumns(); col++)
                        {
                            colNames.add(sheet.getCell(col, row).getContents());
                        }
                        row++;
                    }

                    List<LuminexDataRow> dataRows = new ArrayList<LuminexDataRow>();
                    _sheets.put(analyte, dataRows);

                    if (row < sheet.getRows())
                    {
                        do
                        {
                            LuminexDataRow dataRow = createDataRow(sheet, colNames, row);
                            dataRows.add(dataRow);
                        }
                        while (++row < sheet.getRows() && !"".equals(sheet.getCell(0, row).getContents()));

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
            catch (BiffException e)
            {
                throw new XarFormatException("Failed to parse Excel file " + dataFile.getAbsolutePath(), e);
            }
            finally
            {
                if (fIn != null)
                {
                    try { fIn.close(); } catch (IOException e) {}
                }
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
            if (row >= analyteSheet.getRows())
            {
                return row;
            }

            Map<String, DomainProperty> analyteMap = analyteDomain.createImportMap(true);
            Map<String, DomainProperty> excelMap = excelRunDomain.createImportMap(true);

            do
            {
                String cellContents = analyteSheet.getCell(0, row).getContents();
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
            while (++row < analyteSheet.getRows() && !"".equals(analyteSheet.getCell(0, row).getContents()));
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

        private LuminexDataRow createDataRow(Sheet sheet, List<String> colNames, int row)
        {
            LuminexDataRow dataRow = new LuminexDataRow();
            //dataRow.setDataId(data.getRowId());
            for (int col = 0; col < sheet.getColumns(); col++)
            {
                String columnName = colNames.get(col);

                String value = sheet.getCell(col, row).getContents().trim();
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
                    int outlier = 0;
                    if (value != null && !"".equals(value.trim()))
                    {
                        outlier = Integer.parseInt(value.trim());
                    }
                    dataRow.setOutlier(outlier);
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
    }
}
