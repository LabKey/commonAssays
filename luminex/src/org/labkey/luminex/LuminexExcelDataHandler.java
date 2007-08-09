package org.labkey.luminex;

import org.labkey.api.exp.api.*;
import org.labkey.api.exp.*;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.util.URLHelper;
import org.labkey.api.data.Container;
import org.labkey.api.data.Table;
import org.labkey.api.security.User;
import org.labkey.api.study.AssayProvider;
import org.labkey.api.study.AssayService;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.sql.SQLException;

import jxl.Workbook;
import jxl.Sheet;
import jxl.read.biff.BiffException;

/**
 * User: jeckels
 * Date: Jul 16, 2007
 */
public class LuminexExcelDataHandler extends AbstractExperimentDataHandler
{
    static final String LUMINEX_DATA_LSID_PREFIX = "LuminexDataFile";

    public void importFile(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        if (!dataFile.exists())
        {
            log.warn("Could not find file " + dataFile.getAbsolutePath() + " on disk for data with LSID " + data.getLSID());
            return;
        }
        ExpRun expRun = data.getRun();
        if (expRun == null)
        {
            throw new ExperimentException("Could not load Luminex file " + dataFile.getAbsolutePath() + " because it is not owned by an experiment run");
        }
        try
        {
            ExpProtocol expProtocol = expRun.getProtocol();
            Protocol protocol = ExperimentService.get().getProtocol(expProtocol.getRowId());
            Domain dataDomain = null;
            Domain analyteDomain = null;
            Domain excelRunDomain = null;
            for (String uri : protocol.retrieveObjectProperties().keySet())
            {
                Lsid lsid = new Lsid(uri);
                if (lsid.getNamespacePrefix() != null && lsid.getNamespacePrefix().startsWith(Protocol.ASSAY_DOMAIN_DATA))
                {
                    dataDomain = PropertyService.get().getDomain(info.getContainer(), uri);
                }
                else if (lsid.getNamespacePrefix() != null && lsid.getNamespacePrefix().startsWith(LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE))
                {
                    analyteDomain = PropertyService.get().getDomain(info.getContainer(), uri);
                }
                else if (lsid.getNamespacePrefix() != null && lsid.getNamespacePrefix().startsWith(LuminexAssayProvider.ASSAY_DOMAIN_EXCEL_RUN))
                {
                    excelRunDomain = PropertyService.get().getDomain(info.getContainer(), uri);
                }
            }
            if (dataDomain == null)
            {
                throw new ExperimentException("Could not find data domain for protocol with LSID " + protocol.getLSID());
            }
            if (analyteDomain == null)
            {
                throw new ExperimentException("Could not find analyte domain for protocol with LSID " + protocol.getLSID());
            }
            if (excelRunDomain == null)
            {
                throw new ExperimentException("Could not find Excel run domain for protocol with LSID " + protocol.getLSID());
            }

            FileInputStream fIn = new FileInputStream(dataFile);
            Workbook workbook = Workbook.getWorkbook(fIn);

            Integer id = OntologyManager.ensureObject(info.getContainer().getId(), data.getLSID());

            PropertyDescriptor[] dataColumns = OntologyManager.getPropertiesForType(dataDomain.getTypeURI(), info.getContainer());
            PropertyDescriptor[] analyteColumns = OntologyManager.getPropertiesForType(analyteDomain.getTypeURI(), info.getContainer());
            PropertyDescriptor[] excelRunColumns = OntologyManager.getPropertiesForType(excelRunDomain.getTypeURI(), info.getContainer());
            parseFile(dataColumns, analyteColumns, excelRunColumns, workbook, expRun, info.getContainer(), data, info.getUser());
        }
        catch (IOException e)
        {
            throw new ExperimentException("Failed to read from data file " + dataFile.getAbsolutePath(), e);
        }
        catch (SQLException e)
        {
            throw new ExperimentException("Failed to load from data file " + dataFile.getAbsolutePath(), e);
        }
        catch (BiffException e)
        {
            throw new XarFormatException("Failed to parse Excel file " + dataFile.getAbsolutePath(), e);
        }
    }

    private String getPropertyDescriptorURI(String name, PropertyDescriptor[] props)
    {
        for (PropertyDescriptor prop : props)
        {
            if (prop.getName().equalsIgnoreCase(name))
            {
                return prop.getPropertyURI();
            }
        }
        return null;
    }

    private static Double parseDouble(String value)
    {
        if (value == null || "".equals(value))
        {
            return null;
        }
        else return Double.parseDouble(value);
    }

    private enum OORIndicator
    {
        IN_RANGE
        {
            public String getOORIndicator(String value, List<LuminexDataRow> dataRows, Getter getter)
            {
                return null;
            }
            public Double getValue(String value)
            {
                return parseDouble(value);
            }
            public Double getValue(String value, List<LuminexDataRow> dataRows, Getter getter, Analyte analyte)
            {
                return getValue(value);
            }
        },
        NOT_AVAILABLE
        {
            public String getOORIndicator(String value, List<LuminexDataRow> dataRows, Getter getter)
            {
                return "***";
            }
            public Double getValue(String value)
            {
                return null;
            }
            public Double getValue(String value, List<LuminexDataRow> dataRows, Getter getter, Analyte analyte)
            {
                return null;
            }
        },
        OUT_OF_RANGE_ABOVE
        {
            public String getOORIndicator(String value, List<LuminexDataRow> dataRows, Getter getter)
            {
                return ">>";
            }
            public Double getValue(String value)
            {
                return null;
            }
            public Double getValue(String value, List<LuminexDataRow> dataRows, Getter getter, Analyte analyte)
            {
                return calcOORValue(dataRows, getter, false, analyte);
            }
        },
        OUT_OF_RANGE_BELOW
        {
            public String getOORIndicator(String value, List<LuminexDataRow> dataRows, Getter getter)
            {
                return "<<";
            }
            public Double getValue(String value)
            {
                return null;
            }
            public Double getValue(String value, List<LuminexDataRow> dataRows, Getter getter, Analyte analyte)
            {
                return calcOORValue(dataRows, getter, true, analyte);
            }
        },
        BEYOND_RANGE
        {
            public String getOORIndicator(String value, List<LuminexDataRow> dataRows, Getter getter)
            {
                int lowerCount = 0;
                int higherCount = 0;
                double thisValue = Double.parseDouble(value.substring(1));
                for (LuminexDataRow dataRow : dataRows)
                {
                    Double otherValue = getter.getValue(dataRow);
                    if (otherValue != null)
                    {
                        if (otherValue.doubleValue() < thisValue)
                        {
                            lowerCount++;
                        }
                        else if (otherValue.doubleValue() > thisValue)
                        {
                            higherCount++;
                        }
                    }
                }
                if (lowerCount > higherCount)
                {
                    return ">";
                }
                else if (lowerCount < higherCount)
                {
                    return "<";
                }
                else
                {
                    return "?";
                }
            }
            public Double getValue(String value)
            {
                return null;
            }
            public Double getValue(String value, List<LuminexDataRow> dataRows, Getter getter, Analyte analyte)
            {
                String oorIndicator = getOORIndicator(value, dataRows, getter);
                if ("<".equals(oorIndicator))
                {
                    return OUT_OF_RANGE_BELOW.getValue(value, dataRows, getter, analyte);
                }
                else if (">".equals(oorIndicator))
                {
                    return OUT_OF_RANGE_ABOVE.getValue(value, dataRows, getter, analyte);
                }
                else
                {
                    return null;
                }
            }
        },
        ERROR
        {
            public String getOORIndicator(String value, List<LuminexDataRow> dataRows, Getter getter)
            {
                return "ParseError";
            }
            public Double getValue(String value)
            {
                return null;
            }
            public Double getValue(String value, List<LuminexDataRow> dataRows, Getter getter, Analyte analyte)
            {
                return null;
            }
        },
        OUTLIER
        {
            public String getOORIndicator(String value, List<LuminexDataRow> dataRows, Getter getter)
            {
                return "---";
            }
            public Double getValue(String value)
            {
                return null;
            }
            public Double getValue(String value, List<LuminexDataRow> dataRows, Getter getter, Analyte analyte)
            {
                return null;
            }
        };


        public abstract String getOORIndicator(String value, List<LuminexDataRow> dataRows, Getter getter);
        public abstract Double getValue(String value);
        public abstract Double getValue(String value, List<LuminexDataRow> dataRows, Getter getter, Analyte analyte);

        private static Double calcOORValue(List<LuminexDataRow> dataRows, Getter getter, boolean min, Analyte analyte)
        {
            double startValue = min ? Double.MAX_VALUE : Double.MIN_VALUE;
            double result = startValue;
            for (LuminexDataRow dataRow : dataRows)
            {
                if (dataRow.getType() != null)
                {
                    String type = dataRow.getType().trim().toLowerCase();
                    Double rowValue = getter.getValue(dataRow);
                    if ((type.startsWith("s") || type.startsWith("es")) && dataRow.getObsOverExp() != null && rowValue != null)
                    {
                        double obsOverExp = dataRow.getObsOverExp().doubleValue();
                        if (obsOverExp >= analyte.getMinStandardRecovery() && obsOverExp <= analyte.getMaxStandardRecovery())
                        {
                            if (min)
                            {
                                result = Math.min(result, rowValue.doubleValue());
                            }
                            else
                            {
                                result = Math.max(result, rowValue.doubleValue());
                            }
                        }
                    }
                }
            }
            return result == startValue ? null : result;
        }
    }

    private interface Getter
    {
        public Double getValue(LuminexDataRow dataRow);
    }

    private OORIndicator determineOutOfRange(String value)
    {
        if (value == null || "".equals(value))
        {
            return OORIndicator.IN_RANGE;
        }
        if ("***".equals(value))
        {
            return OORIndicator.NOT_AVAILABLE;
        }
        if ("---".equals(value))
        {
            return OORIndicator.OUTLIER;
        }
        if (value.startsWith("*"))
        {
            return OORIndicator.BEYOND_RANGE;
        }
        if (value.toLowerCase().contains("oor") && value.contains(">"))
        {
            return OORIndicator.OUT_OF_RANGE_ABOVE;
        }
        if (value.toLowerCase().contains("oor") && value.contains("<"))
        {
            return OORIndicator.OUT_OF_RANGE_ABOVE;
        }

        try
        {
            parseDouble(value);
            return OORIndicator.IN_RANGE;
        }
        catch (NumberFormatException e)
        {
            return OORIndicator.ERROR;
        }
    }

    private void parseFile(PropertyDescriptor[] dataColumns, PropertyDescriptor[] analyteColumns, PropertyDescriptor[] excelRunColumns, Workbook workbook, final ExpRun expRun, Container container, ExpData data, User user) throws SQLException, ExperimentException
    {
        List<Map<String, Object>> dataRowsProps = new ArrayList<Map<String, Object>>();
        
        Map<String, Object> excelRunProps = new HashMap<String, Object>();

        for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++)
        {
            Sheet sheet = workbook.getSheet(sheetIndex);

            if ("Row #".equals(sheet.getCell(0, 0).getContents()))
            {
                continue;
            }

            Analyte analyte = new Analyte(sheet.getName(), data.getRowId());

            Map<String, Object> analyteProps = new HashMap<String, Object>();

            int row = handleHeaderOrFooterRow(sheet, 0, analyte, analyteColumns, analyteProps, excelRunColumns, excelRunProps);

            // Skip over the blank line
            row++;
            
            List<String> colNames = new ArrayList<String>();
            for (int col = 0; col < sheet.getColumns(); col++)
            {
                colNames.add(sheet.getCell(col, row).getContents());
            }
            row++;

            List<LuminexDataRow> dataRows = new ArrayList<LuminexDataRow>();

            do
            {
                Map<String, Object> dataRowProps = new LinkedHashMap<String, Object>();
                LuminexDataRow dataRow = createDataRow(data, sheet, colNames, row, dataColumns, dataRowProps);
                dataRows.add(dataRow);
                dataRowsProps.add(dataRowProps);
            }
            while (row++ < sheet.getRows() && !"".equals(sheet.getCell(0, row).getContents()));

            // Skip over the blank line
            row++;

            row = handleHeaderOrFooterRow(sheet, row, analyte, analyteColumns, analyteProps, excelRunColumns, excelRunProps);

            analyte.setLsid(new Lsid("LuminexAnalyte", "Data-" + data.getRowId() + "." + analyte.getName()).toString());

            if (analyte.getMaxStandardRecovery() == 0 && analyte.getMinStandardRecovery() == 0)
            {
                throw new ExperimentException("Unable to find max and min standard recovery values for analyte " + analyte.getName()); 
            }

            analyte = Table.insert(user, LuminexSchema.getTableInfoAnalytes(), analyte);

            for (LuminexDataRow dataRow : dataRows)
            {
                Getter fiGetter = new Getter()
                {
                    public Double getValue(LuminexDataRow dataRow)
                    {
                        if (determineOutOfRange(dataRow.getFiString()) == OORIndicator.IN_RANGE)
                        {
                            return dataRow.getFi();
                        }
                        return null;
                    }
                };
                Getter fiBackgroundGetter = new Getter()
                {
                    public Double getValue(LuminexDataRow dataRow)
                    {
                        if (determineOutOfRange(dataRow.getFiBackgroundString()) == OORIndicator.IN_RANGE)
                        {
                            return dataRow.getFiBackground();
                        }
                        return null;
                    }
                };
                Getter stdDevGetter = new Getter()
                {
                    public Double getValue(LuminexDataRow dataRow)
                    {
                        if (determineOutOfRange(dataRow.getStdDevString()) == OORIndicator.IN_RANGE)
                        {
                            return dataRow.getStdDev();
                        }
                        return null;
                    }
                };
                Getter obsConcGetter = new Getter()
                {
                    public Double getValue(LuminexDataRow dataRow)
                    {
                        if (determineOutOfRange(dataRow.getObsConcString()) == OORIndicator.IN_RANGE)
                        {
                            return dataRow.getObsConc();
                        }
                        return null;
                    }
                };
                Getter concInRangeGetter = new Getter()
                {
                    public Double getValue(LuminexDataRow dataRow)
                    {
                        if (determineOutOfRange(dataRow.getConcInRangeString()) == OORIndicator.IN_RANGE)
                        {
                            return dataRow.getConcInRange();
                        }
                        return null;
                    }
                };
                OORIndicator fiOORType = determineOutOfRange(dataRow.getFiString());
                dataRow.setFiOORIndicator(fiOORType.getOORIndicator(dataRow.getFiString(), dataRows, fiGetter));
                dataRow.setFi(fiOORType.getValue(dataRow.getFiString(), dataRows, fiGetter, analyte));

                OORIndicator fiBackgroundOORType = determineOutOfRange(dataRow.getFiBackgroundString());
                dataRow.setFiBackgroundOORIndicator(fiBackgroundOORType.getOORIndicator(dataRow.getFiBackgroundString(), dataRows, fiBackgroundGetter));
                dataRow.setFiBackground(fiBackgroundOORType.getValue(dataRow.getFiBackgroundString(), dataRows, fiBackgroundGetter, analyte));

                OORIndicator stdDevOORType = determineOutOfRange(dataRow.getStdDevString());
                dataRow.setStdDevOORIndicator(stdDevOORType.getOORIndicator(dataRow.getStdDevString(), dataRows, stdDevGetter));
                dataRow.setStdDev(stdDevOORType.getValue(dataRow.getStdDevString(), dataRows, stdDevGetter, analyte));

                OORIndicator obsConcOORType = determineOutOfRange(dataRow.getObsConcString());
                dataRow.setObsConcOORIndicator(obsConcOORType.getOORIndicator(dataRow.getObsConcString(), dataRows, obsConcGetter));
                dataRow.setObsConc(obsConcOORType.getValue(dataRow.getObsConcString(), dataRows, obsConcGetter, analyte));

                OORIndicator concInRangeOORType = determineOutOfRange(dataRow.getConcInRangeString());
                dataRow.setConcInRangeOORIndicator(concInRangeOORType.getOORIndicator(dataRow.getConcInRangeString(), dataRows, concInRangeGetter));
                dataRow.setConcInRange(concInRangeOORType.getValue(dataRow.getConcInRangeString(), dataRows, concInRangeGetter, analyte));

                dataRow.setAnalyteId(analyte.getRowId());

                Table.insert(user, LuminexSchema.getTableInfoDataRow(), dataRow);
            }
        }

        OntologyManager.insertTabDelimited(container, OntologyManager.ensureObject(container.getId(), expRun.getLSID()), new OntologyManager.ImportHelper()
        {
            public String beforeImportObject(Map map) throws SQLException
            {
                return expRun.getLSID();
            }

            public void afterImportObject(String lsid, ObjectProperty[] props) throws SQLException
            {
            }
        }, excelRunColumns, new Map[] { excelRunProps }, true);
    }

    private LuminexDataRow createDataRow(ExpData data, Sheet sheet, List<String> colNames, int row, PropertyDescriptor[] dataColumns, Map<String, Object> rowValues)
    {
        LuminexDataRow dataRow = new LuminexDataRow();
        dataRow.setDataId(data.getRowId());
        for (int col = 0; col < sheet.getColumns(); col++)
        {
            String columnName = colNames.get(col);

            String value = sheet.getCell(col, row).getContents().trim();
            if ("FI".equalsIgnoreCase(columnName))
            {
                dataRow.setFiString(value);
                dataRow.setFi(determineOutOfRange(value).getValue(value));
            }
            else if ("FI - Bkgd".equalsIgnoreCase(columnName))
            {
                dataRow.setFiBackgroundString(value);
                dataRow.setFiBackground(determineOutOfRange(value).getValue(value));
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
                dataRow.setOutlier(!"0".equals(value));
            }
            else if ("Description".equalsIgnoreCase(columnName))
            {
                dataRow.setDescription(value);
            }
            else if ("Std Dev".equalsIgnoreCase(columnName))
            {
                dataRow.setStdDevString(value);
                dataRow.setStdDev(determineOutOfRange(value).getValue(value));
            }
            else if ("Exp Conc".equalsIgnoreCase(columnName))
            {
                dataRow.setExpConc(parseDouble(value));
            }
            else if ("Obs Conc".equalsIgnoreCase(columnName))
            {
                dataRow.setObsConcString(value);
                dataRow.setObsConc(determineOutOfRange(value).getValue(value));
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
                dataRow.setConcInRange(determineOutOfRange(value).getValue(value));
            }
            else
            {
                storePropertyValue(columnName, value, dataColumns, rowValues);
            }
        }
        return dataRow;
    }

    private int handleHeaderOrFooterRow(Sheet analyteSheet, int row, Analyte analyte, PropertyDescriptor[] analyteColumns, Map<String, Object> analyteProps, PropertyDescriptor[] excelRunColumns, Map<String, Object> excelRunProps)
    {
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

                storePropertyValue(propName, value, analyteColumns, analyteProps);
                storePropertyValue(propName, value, excelRunColumns, excelRunProps);
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

    private void storePropertyValue(String propName, String value, PropertyDescriptor[] columns, Map<String, Object> props)
    {
        String analytePropURI = getPropertyDescriptorURI(propName, columns);
        if (analytePropURI != null)
        {
            props.put(analytePropURI, value);
        }
    }

    public URLHelper getContentURL(HttpServletRequest request, Container container, ExpData data) throws ExperimentException
    {
        ExpRun run = data.getRun();
        if (run != null)
        {
            ExpProtocol protocol = run.getProtocol();
            Protocol p = ExperimentService.get().getProtocol(protocol.getRowId());
            AssayProvider provider = AssayService.get().getProvider(p);
            if (provider != null)
            {
                return provider.getAssayDataURL(container, p, run.getRowId());
            }
        }
        return null;
    }


    public void beforeDeleteData(List<Data> data) throws ExperimentException
    {
        try
        {
            Object[] ids = new Object[data.size()];
            for (int i = 0; i < data.size(); i++)
            {
                ids[i] = data.get(0).getRowId();
            }
            Table.execute(LuminexSchema.getSchema(), "DELETE FROM " + LuminexSchema.getTableInfoDataRow() + " WHERE DataId = ?", ids);
            Table.execute(LuminexSchema.getSchema(), "DELETE FROM " + LuminexSchema.getTableInfoAnalytes() + " WHERE DataId = ?", ids);
        }
        catch (SQLException e)
        {
            throw new ExperimentException(e);
        }
    }

    public void deleteData(Data data, Container container) throws ExperimentException
    {
        try
        {
            OntologyManager.deleteOntologyObject(container.getId(), data.getLSID());
        }
        catch (SQLException e)
        {
            throw new ExperimentException(e);
        }
    }

    public void runMoved(Data newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user) throws ExperimentException
    {
        throw new UnsupportedOperationException();
    }

    public Priority getPriority(Data data)
    {
        Lsid lsid = new Lsid(data.getLSID());
        if (LUMINEX_DATA_LSID_PREFIX.equals(lsid.getNamespacePrefix()))
        {
            return Priority.HIGH;
        }
        return null;
    }
}
