/*
 * Copyright (c) 2009 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.Table;
import org.labkey.api.exp.*;
import org.labkey.api.exp.api.*;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.study.ParticipantVisit;
import org.labkey.api.study.assay.*;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: May 14, 2009
 */
public abstract class LuminexDataHandler extends AbstractExperimentDataHandler
{
    public interface LuminexDataFileParser
    {
        Map<Analyte, List<LuminexDataRow>> getSheets() throws ExperimentException;
        Map<String, Object> getExcelRunProps() throws ExperimentException;
    }

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

            PropertyDescriptor[] analyteColumns = OntologyManager.getPropertiesForType(analyteDomain.getTypeURI(), info.getContainer());
            PropertyDescriptor[] excelRunColumns = OntologyManager.getPropertiesForType(excelRunDomain.getTypeURI(), info.getContainer());
            importData(analyteColumns, excelRunColumns, expRun, info.getContainer(), data, info.getUser(), dataFile);
        }
        catch (SQLException e)
        {
            log.error("Failed to load from data file " + dataFile.getAbsolutePath(), e);
            throw new ExperimentException("Failed to load from data file " + dataFile.getAbsolutePath() + "(" + e.toString() + ")", e);
        }
    }

    protected abstract LuminexDataFileParser getDataFileParser(ExpProtocol protocol, File dataFile);

    private static Double parseDouble(String value)
    {
        if (value == null || "".equals(value))
        {
            return null;
        }
        else return Double.parseDouble(value);
    }

    public enum OORIndicator
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

    public static OORIndicator determineOutOfRange(String value)
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

    public static List<String> getAnalyteNames(File dataFile) throws ExperimentException
    {
        List<String> analytes = new ArrayList<String>();
        try {
            FileInputStream fIn = new FileInputStream(dataFile);
            WorkbookSettings settings = new WorkbookSettings();
            settings.setGCDisabled(true);
            Workbook workbook = Workbook.getWorkbook(fIn, settings);

            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++)
            {
                Sheet sheet = workbook.getSheet(sheetIndex);

                if (sheet.getRows() == 0 || sheet.getColumns() == 0 || "Row #".equals(sheet.getCell(0, 0).getContents()))
                {
                    continue;
                }
                analytes.add(sheet.getName());
            }
            return analytes;
        }
        catch (Exception e)
        {
            throw new ExperimentException(e);
        }
    }

    /**
     * Handles persisting of uploaded run data into the database
     * @param analyteColumns
     * @param excelRunColumns
     * @param expRun
     * @param container
     * @param data
     * @param user
     * @param dataFile
     * @throws SQLException
     * @throws ExperimentException
     */
    private void importData(PropertyDescriptor[] analyteColumns, PropertyDescriptor[] excelRunColumns, final ExpRun expRun, Container container, ExpData data, User user, File dataFile) throws SQLException, ExperimentException
    {
        boolean ownTransaction = !ExperimentService.get().isTransactionActive();
        if (ownTransaction)
        {
            ExperimentService.get().beginTransaction();
        }
        try
        {
            ExpProtocol protocol = expRun.getProtocol();
            ParticipantVisitResolver resolver = null;
            AssayProvider provider = AssayService.get().getProvider(protocol);
            Map<String, ObjectProperty> mergedProperties = new HashMap<String, ObjectProperty>();
            Set<ExpMaterial> inputMaterials = new LinkedHashSet<ExpMaterial>();
            mergedProperties.putAll(expRun.getObjectProperties());
            ExpExperiment batch = AssayService.get().findBatch(expRun);
            if (batch != null)
            {
                mergedProperties.putAll(batch.getObjectProperties());
            }
            for (ObjectProperty objectProperty : mergedProperties.values())
            {
                if (AbstractAssayProvider.PARTICIPANT_VISIT_RESOLVER_PROPERTY_NAME.equals(objectProperty.getName()))
                {
                    ParticipantVisitResolverType resolverType = AbstractAssayProvider.findType(objectProperty.getStringValue(), provider.getParticipantVisitResolverTypes());
                    Container targetStudy = null;
                    if (provider instanceof LuminexAssayProvider)
                    {
                        LuminexAssayProvider luminexProvider = (LuminexAssayProvider) provider;
                        targetStudy = luminexProvider.getTargetStudy(expRun);
                    }
                    try
                    {
                        resolver = resolverType.createResolver(expRun, targetStudy, user);
                    }
                    catch (IOException e)
                    {
                        throw new ExperimentException(e);
                    }
                }
            }

            LuminexDataFileParser parser = getDataFileParser(protocol, dataFile);

            for (Map.Entry<Analyte, List<LuminexDataRow>> sheet : parser.getSheets().entrySet())
            {
                Analyte analyte = sheet.getKey();

                analyte.setDataId(data.getRowId());
                analyte.setLsid(new Lsid("LuminexAnalyte", "Data-" + data.getRowId() + "." + analyte.getName()).toString());

                analyte = Table.insert(user, LuminexSchema.getTableInfoAnalytes(), analyte);

                List<LuminexDataRow> dataRows = sheet.getValue();
                performOOR(dataRows, analyte);

                for (LuminexDataRow dataRow : dataRows)
                {
                    handleParticipantResolver(dataRow, resolver, inputMaterials);
                    dataRow.setAnalyteId(analyte.getRowId());
                    dataRow.setDataId(data.getRowId());
                    Table.insert(user, LuminexSchema.getTableInfoDataRow(), dataRow);
                }
            }
            if (inputMaterials.isEmpty())
            {
                throw new ExperimentException("Could not find any input samples in the data");
            }

            AbstractAssayProvider.addInputMaterials(expRun, user, inputMaterials);

            // Clear out the values - this is necessary if this is a XAR import where the run properties would
            // have been loaded as part of the ExperimentRun itself.
            Integer objectId = OntologyManager.ensureObject(container, expRun.getLSID());
            for (PropertyDescriptor excelRunColumn : excelRunColumns)
            {
                OntologyManager.deleteProperty(expRun.getLSID(), excelRunColumn.getPropertyURI(), container, protocol.getContainer());
            }

            Map<String, Object> excelRunProps = parser.getExcelRunProps();
            List<Map<String, Object>> excelRunPropsList = new ArrayList<Map<String, Object>>();
            excelRunPropsList.add(excelRunProps);
            OntologyManager.insertTabDelimited(container, objectId, new OntologyManager.ImportHelper()
            {
                public String beforeImportObject(Map<String, Object> map) throws SQLException
                {
                    return expRun.getLSID();
                }

                public void afterImportObject(String lsid, ObjectProperty[] props) throws SQLException
                {
                }
            }, excelRunColumns, excelRunPropsList, true);

            if (ownTransaction)
            {
                ExperimentService.get().commitTransaction();
                ownTransaction = false;
            }
        }
        catch (ValidationException ve)
        {
            throw new ExperimentException(ve.toString(), ve);
        }
        finally
        {
            if (ownTransaction)
            {
                ExperimentService.get().rollbackTransaction();
            }
        }
    }

    protected void performOOR(List<LuminexDataRow> dataRows, Analyte analyte)
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
        }
    }

    protected void handleParticipantResolver(LuminexDataRow dataRow, ParticipantVisitResolver resolver, Set<ExpMaterial> materialInputs)
    {
        String value = dataRow.getDescription();
        if (resolver != null && value != null)
        {
            ParticipantVisit match = resolver.resolve(value, null, null, null);
            String participantId;
            Double visitId;
            Date date;
            String extraSpecimenInfo = null;
            String[] parts = value == null ? new String[0] : value.split(",");
            if (match.getParticipantID() == null && match.getVisitID() == null && match.getDate() == null && parts.length >= 3)
            {
                // First part is participant id
                participantId = parts[0].trim();
                try
                {
                    // Second part is visit id, possibly prefixed with "visit"
                    String visitString = parts[1].trim();
                    if (visitString.toLowerCase().startsWith("visit"))
                    {
                        visitString = visitString.substring("visit".length()).trim();
                    }
                    visitId = new Double(visitString);
                }
                catch (NumberFormatException e)
                {
                    visitId = null;
                }
                try
                {
                    date = (Date) ConvertUtils.convert(parts[2].trim(), Date.class);
                }
                catch (ConversionException e)
                {
                    date = null;
                }
                if (parts.length > 3)
                {
                    StringBuilder sb = new StringBuilder(parts[3]);
                    for (int i = 4; i < parts.length; i++)
                    {
                        sb.append(", ");
                        sb.append(parts[i].trim());
                    }
                    extraSpecimenInfo = sb.toString();
                }
                match = resolver.resolve(null, participantId, visitId, date);
            }
            else
            {
                participantId = match.getParticipantID();
                visitId = match.getVisitID();
                date = match.getDate();
            }
            dataRow.setPtid(participantId);
            dataRow.setVisitID(visitId);
            dataRow.setDate(date);
            dataRow.setExtraSpecimenInfo(extraSpecimenInfo);
            materialInputs.add(match.getMaterial());
        }
    }

    private LuminexDataRow createDataRow(ExpData data, Sheet sheet, List<String> colNames, int row, ParticipantVisitResolver resolver, Set<ExpMaterial> materialInputs)
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
                    String participantId;
                    Double visitId;
                    Date date;
                    String extraSpecimenInfo = null;
                    String[] parts = value == null ? new String[0] : value.split(",");
                    if (match.getParticipantID() == null && match.getVisitID() == null && match.getDate() == null && parts.length >= 3)
                    {
                        // First part is participant id
                        participantId = parts[0].trim();
                        try
                        {
                            // Second part is visit id, possibly prefixed with "visit"
                            String visitString = parts[1].trim();
                            if (visitString.toLowerCase().startsWith("visit"))
                            {
                                visitString = visitString.substring("visit".length()).trim();
                            }
                            visitId = new Double(visitString);
                        }
                        catch (NumberFormatException e)
                        {
                            visitId = null;
                        }
                        try
                        {
                            date = (Date)ConvertUtils.convert(parts[2].trim(), Date.class);
                        }
                        catch (ConversionException e)
                        {
                            date = null;
                        }
                        if (parts.length > 3)
                        {
                            StringBuilder sb = new StringBuilder(parts[3]);
                            for (int i = 4; i < parts.length; i++)
                            {
                                sb.append(", ");
                                sb.append(parts[i].trim());
                            }
                            extraSpecimenInfo = sb.toString();
                        }
                        match = resolver.resolve(null, participantId, visitId, date);
                    }
                    else
                    {
                        participantId = match.getParticipantID();
                        visitId = match.getVisitID();
                        date = match.getDate();
                    }
                    dataRow.setPtid(participantId);
                    dataRow.setVisitID(visitId);
                    dataRow.setDate(date);
                    dataRow.setExtraSpecimenInfo(extraSpecimenInfo);
                    materialInputs.add(match.getMaterial());
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
        if (row >= analyteSheet.getRows())
        {
            return row;
        }

        Map<String,PropertyDescriptor> analyteMap = OntologyManager.createImportPropertyMap(analyteColumns);
        Map<String,PropertyDescriptor> excelMap = OntologyManager.createImportPropertyMap(excelRunColumns);

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

    private void storePropertyValue(String propName, String value, Map<String,PropertyDescriptor> columns, Map<String, Object> props)
    {
        PropertyDescriptor pd = columns.get(propName);
        if (pd != null)
        {
            props.put(pd.getPropertyURI(), value);
        }
    }

    public ActionURL getContentURL(Container container, ExpData data)
    {
        ExpRun run = data.getRun();
        if (run != null)
        {
            ExpProtocol protocol = run.getProtocol();
            ExpProtocol p = ExperimentService.get().getExpProtocol(protocol.getRowId());
            return PageFlowUtil.urlProvider(AssayUrls.class).getAssayResultsURL(container, p, run.getRowId());
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

    public void deleteData(ExpData data, Container container, User user)
    {
        try
        {
            OntologyManager.deleteOntologyObjects(container, data.getLSID());
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    public void runMoved(ExpData newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user, int oldDataRowID) throws ExperimentException
    {
        throw new UnsupportedOperationException();
    }
}
