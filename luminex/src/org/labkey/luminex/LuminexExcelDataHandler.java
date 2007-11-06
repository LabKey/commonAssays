package org.labkey.luminex;

import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;
import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.Table;
import org.labkey.api.exp.*;
import org.labkey.api.exp.api.*;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.security.User;
import org.labkey.api.study.ParticipantVisit;
import org.labkey.api.study.assay.*;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ViewBackgroundInfo;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

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
        FileInputStream fIn = null;
        try
        {
            ExpProtocol expProtocol = expRun.getProtocol();
            ExpProtocol protocol = ExperimentService.get().getExpProtocol(expProtocol.getRowId());
            String analyteDomainURI = AbstractAssayProvider.getDomainURIForPrefix(expProtocol, LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE);
            String excelRunDomainURI = AbstractAssayProvider.getDomainURIForPrefix(expProtocol, LuminexAssayProvider.ASSAY_DOMAIN_EXCEL_RUN);
            Domain analyteDomain = PropertyService.get().getDomain(info.getContainer(), analyteDomainURI);
            Domain excelRunDomain = PropertyService.get().getDomain(info.getContainer(), excelRunDomainURI);

            if (analyteDomain == null)
            {
                throw new ExperimentException("Could not find analyte domain for protocol with LSID " + protocol.getLSID());
            }
            if (excelRunDomain == null)
            {
                throw new ExperimentException("Could not find Excel run domain for protocol with LSID " + protocol.getLSID());
            }

            fIn = new FileInputStream(dataFile);
            WorkbookSettings settings = new WorkbookSettings();
            settings.setGCDisabled(true);
            Workbook workbook = Workbook.getWorkbook(fIn, settings);

            PropertyDescriptor[] analyteColumns = OntologyManager.getPropertiesForType(analyteDomain.getTypeURI(), info.getContainer());
            PropertyDescriptor[] excelRunColumns = OntologyManager.getPropertiesForType(excelRunDomain.getTypeURI(), info.getContainer());
            parseFile(analyteColumns, excelRunColumns, workbook, expRun, info.getContainer(), data, info.getUser());
        }
        catch (IOException e)
        {
            log.error("Failed to read from data file " + dataFile.getAbsolutePath(), e);
            throw new ExperimentException("Failed to read from data file " + dataFile.getAbsolutePath(), e);
        }
        catch (SQLException e)
        {
            log.error("Failed to load from data file " + dataFile.getAbsolutePath(), e);
            throw new ExperimentException("Failed to load from data file " + dataFile.getAbsolutePath() + "(" + e.toString() + ")", e);
        }
        catch (BiffException e)
        {
            log.error("Failed to parse Excel data file" + dataFile.getAbsolutePath(), e);
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
                return getValidStandard(dataRows, getter, false, analyte);
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
                return getValidStandard(dataRows, getter, true, analyte);
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
                double thisValue = Double.parseDouble(value.substring(1));
                if ("<".equals(oorIndicator))
                {
                    Double standardValue = OUT_OF_RANGE_BELOW.getValue(value, dataRows, getter, analyte);
                    if (standardValue != null && standardValue.doubleValue() > thisValue)
                    {
                        return standardValue;
                    }
                    else
                    {
                        return thisValue;
                    }
                }
                else if (">".equals(oorIndicator))
                {
                    Double standardValue = OUT_OF_RANGE_ABOVE.getValue(value, dataRows, getter, analyte);
                    if (standardValue != null && standardValue.doubleValue() < thisValue)
                    {
                        return standardValue;
                    }
                    else
                    {
                        return thisValue;
                    }
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
    }

    private static Double getValidStandard(List<LuminexDataRow> dataRows, Getter getter, boolean min, Analyte analyte)
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
            return OORIndicator.OUT_OF_RANGE_BELOW;
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

    private void parseFile(PropertyDescriptor[] analyteColumns, PropertyDescriptor[] excelRunColumns, Workbook workbook, final ExpRun expRun, Container container, ExpData data, User user) throws SQLException, ExperimentException
    {
        List<Map<String, Object>> dataRowsProps = new ArrayList<Map<String, Object>>();
        
        Map<String, Object> excelRunProps = new HashMap<String, Object>();

        ParticipantVisitResolver resolver = null;
        AssayProvider provider = AssayService.get().getProvider(expRun.getProtocol());
        if (provider instanceof LuminexAssayProvider)
        {
            LuminexAssayProvider luminexProvider = (LuminexAssayProvider) provider;
            Container targetStudy = luminexProvider.getTargetStudy(expRun);
            if (targetStudy != null)
            {
                SpecimenIDLookupResolverType resolverType = new SpecimenIDLookupResolverType();
                resolver = resolverType.createResolver(expRun, targetStudy, user);
            }
        }

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
                LuminexDataRow dataRow = createDataRow(data, sheet, colNames, row, resolver);
                dataRows.add(dataRow);
                dataRowsProps.add(dataRowProps);
            }
            while (row++ < sheet.getRows() && !"".equals(sheet.getCell(0, row).getContents()));

            // Skip over the blank line
            row++;

            row = handleHeaderOrFooterRow(sheet, row, analyte, analyteColumns, analyteProps, excelRunColumns, excelRunProps);

            analyte.setLsid(new Lsid("LuminexAnalyte", "Data-" + data.getRowId() + "." + analyte.getName()).toString());

/*            if (analyte.getMaxStandardRecovery() == 0 && analyte.getMinStandardRecovery() == 0)
            {
                throw new ExperimentException("Unable to find max and min standard recovery values for analyte " + analyte.getName()); 
            }
*/
            analyte = Table.insert(user, LuminexSchema.getTableInfoAnalytes(), analyte);

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

            Double minStandardFI = getValidStandard(dataRows, fiGetter, true, analyte);
            Double maxStandardFI = getValidStandard(dataRows, fiGetter, false, analyte);
            Double minStandardObsConc = getValidStandard(dataRows, obsConcGetter, true, analyte);
            Double maxStandardObsConc = getValidStandard(dataRows, obsConcGetter, false, analyte);

            for (LuminexDataRow dataRow : dataRows)
            {
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
                Double obsConc;
                switch (obsConcOORType)
                {
                    case IN_RANGE:
                        obsConc = parseDouble(dataRow.getObsConcString());
                        break;
                    case OUT_OF_RANGE_ABOVE:
                        if (dataRow.getDilution() != null && maxStandardObsConc != null)
                        {
                            obsConc = dataRow.getDilution().doubleValue() * maxStandardObsConc.doubleValue();
                        }
                        else
                        {
                            obsConc = null;
                        }
                        break;
                    case OUT_OF_RANGE_BELOW:
                        if (dataRow.getDilution() != null && minStandardObsConc != null)
                        {
                            obsConc = dataRow.getDilution().doubleValue() * minStandardObsConc.doubleValue();
                        }
                        else
                        {
                            obsConc = null;
                        }
                        break;
                    case ERROR:
                    case OUTLIER:
                    case NOT_AVAILABLE:
                        obsConc = null;
                        break;
                    case BEYOND_RANGE:
                        if (dataRow.getFi() != null)
                        {
                            if (minStandardFI != null && dataRow.getFi().doubleValue() < minStandardFI.doubleValue())
                            {
                                obsConc = dataRow.getDilution().doubleValue() * minStandardObsConc.doubleValue();
                            }
                            else if (maxStandardFI != null && dataRow.getFi().doubleValue() > maxStandardFI.doubleValue())
                            {
                                obsConc = dataRow.getDilution().doubleValue() * maxStandardObsConc.doubleValue();
                            }
                            else
                            {
                                obsConc = null;
                            }
                        }
                        else
                        {
                            obsConc = null;
                        }
                        break;
                    default:
                        throw new IllegalArgumentException(obsConcOORType.toString());
                }
                dataRow.setObsConc(obsConc);

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

    private LuminexDataRow createDataRow(ExpData data, Sheet sheet, List<String> colNames, int row, ParticipantVisitResolver resolver)
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
                if (resolver != null)
                {
                    ParticipantVisit match = resolver.resolve(value, null, null, null);
                    dataRow.setPtid(match.getParticipantID());
                    dataRow.setVisitID(match.getVisitID());
                }
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
            ExpProtocol p = ExperimentService.get().getExpProtocol(protocol.getRowId());
            return AssayService.get().getAssayDataURL(container, p, run.getRowId());
        }
        return null;
    }


    public void beforeDeleteData(List<ExpData> data) throws ExperimentException
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

    public void deleteData(ExpData data, Container container, User user) throws ExperimentException
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

    public void runMoved(ExpData newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user, int oldDataRowID) throws ExperimentException
    {
        throw new UnsupportedOperationException();
    }

    public Priority getPriority(ExpData data)
    {
        Lsid lsid = new Lsid(data.getLSID());
        if (LUMINEX_DATA_LSID_PREFIX.equals(lsid.getNamespacePrefix()))
        {
            return Priority.HIGH;
        }
        return null;
    }
}
