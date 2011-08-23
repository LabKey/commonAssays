/*
 * Copyright (c) 2009-2011 LabKey Corporation
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

import junit.framework.Assert;
import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.labkey.api.assay.dilution.DilutionCurve;
import org.labkey.api.assay.dilution.ParameterCurveImpl;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.Container;
import org.labkey.api.data.ObjectFactory;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.exp.*;
import org.labkey.api.exp.api.*;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.qc.TransformDataHandler;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.study.ParticipantVisit;
import org.labkey.api.study.assay.*;
import org.labkey.api.util.FileType;
import org.labkey.api.util.GUID;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: klum
 * Date: May 14, 2009
 */
public class LuminexDataHandler extends AbstractExperimentDataHandler implements TransformDataHandler
{
    public static final DataType LUMINEX_TRANSFORMED_DATA_TYPE = new DataType("LuminexTransformedDataFile");  // marker data type
    public static final AssayDataType LUMINEX_DATA_TYPE = new AssayDataType("LuminexDataFile", new FileType(Arrays.asList(".xls", ".xlsx"), ".xls"));

    private static final Logger LOGGER = Logger.getLogger(LuminexDataHandler.class);

    public static final int MINIMUM_TITRATION_COUNT = 5;

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

        LuminexExcelParser parser;
        LuminexRunUploadForm form = null;
        if (context instanceof AssayUploadXarContext && ((AssayUploadXarContext)context).getContext() instanceof LuminexRunUploadForm)
        {
            form = (LuminexRunUploadForm)((AssayUploadXarContext)context).getContext();
            parser = form.getParser();
        }
        else
        {
            parser = new LuminexExcelParser(expRun.getProtocol(), Collections.singleton(dataFile));
        }
        // The parser has already collapsed the data from multiple files into a single set of data,
        // so don't bother importing it twice if it came from separate files. This can happen if you aren't using a
        // transform script, so the assay framework attempts to import each file individually. We don't want to reparse
        // the Excel files, so we get a cached parser, which has the data for all of the files.
        if (!parser.isImported())
        {
            importData(data, expRun, info.getUser(), log, parser.getSheets(), parser, form);
            parser.setImported(true);
        }
    }

    public void importData(ExpData data, ExpRun run, User user, Logger log, Map<Analyte, List<LuminexDataRow>> sheets, LuminexExcelParser parser, LuminexRunUploadForm form) throws ExperimentException
    {
        try
        {
            ExpProtocol expProtocol = run.getProtocol();
            ExpProtocol protocol = ExperimentService.get().getExpProtocol(expProtocol.getRowId());
            String analyteDomainURI = AbstractAssayProvider.getDomainURIForPrefix(expProtocol, LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE);
            String excelRunDomainURI = AbstractAssayProvider.getDomainURIForPrefix(expProtocol, LuminexAssayProvider.ASSAY_DOMAIN_EXCEL_RUN);
            Domain analyteDomain = PropertyService.get().getDomain(run.getContainer(), analyteDomainURI);
            Domain excelRunDomain = PropertyService.get().getDomain(run.getContainer(), excelRunDomainURI);

            if (analyteDomain == null)
            {
                throw new ExperimentException("Could not find analyte domain for protocol with LSID " + protocol.getLSID());
            }
            if (excelRunDomain == null)
            {
                throw new ExperimentException("Could not find Excel run domain for protocol with LSID " + protocol.getLSID());
            }

            PropertyDescriptor[] excelRunColumns = OntologyManager.getPropertiesForType(excelRunDomain.getTypeURI(), run.getContainer());
            insertData(excelRunColumns, run, run.getContainer(), data, user, sheets, parser, form);
        }
        catch (SQLException e)
        {
            if (log == null)
            {
                log = Logger.getLogger(LuminexDataHandler.class);
            }
            log.error("Failed to load from data file " + data.getFile().getAbsolutePath(), e);
            throw new ExperimentException("Failed to load from data file " + data.getFile().getAbsolutePath() + "(" + e.toString() + ")", e);
        }
    }

    public static Double parseDouble(String value)
    {
        if (value == null || "".equals(value))
        {
            return null;
        }
        else return Double.parseDouble(value);
    }

    public static Double getValidStandard(List<LuminexDataRow> dataRows, Getter getter, boolean min, Analyte analyte)
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
                    double obsOverExp = dataRow.getObsOverExp();
                    if (obsOverExp >= analyte.getMinStandardRecovery() && obsOverExp <= analyte.getMaxStandardRecovery())
                    {
                        if (min)
                        {
                            result = Math.min(result, rowValue);
                        }
                        else
                        {
                            result = Math.max(result, rowValue);
                        }
                    }
                }
            }
        }
        return result == startValue ? null : result;
    }

    public interface Getter
    {
        public Double getValue(LuminexDataRow dataRow);
    }

    public static LuminexOORIndicator determineOutOfRange(String value)
    {
        if (value == null || "".equals(value))
        {
            return LuminexOORIndicator.IN_RANGE;
        }
        if ("***".equals(value))
        {
            return LuminexOORIndicator.NOT_AVAILABLE;
        }
        if ("---".equals(value))
        {
            return LuminexOORIndicator.OUTLIER;
        }
        if (value.startsWith("*"))
        {
            return LuminexOORIndicator.BEYOND_RANGE;
        }
        if (value.toLowerCase().contains("oor") && value.contains(">"))
        {
            return LuminexOORIndicator.OUT_OF_RANGE_ABOVE;
        }
        if (value.toLowerCase().contains("oor") && value.contains("<"))
        {
            return LuminexOORIndicator.OUT_OF_RANGE_BELOW;
        }

        try
        {
            parseDouble(value);
            return LuminexOORIndicator.IN_RANGE;
        }
        catch (NumberFormatException e)
        {
            return LuminexOORIndicator.ERROR;
        }
    }

    private static final String NUMBER_REGEX = "-??[0-9]+(?:\\.[0-9]+)?";

    // FI = 0.441049 + (30395.4 - 0.441049) / ((1 + (Conc / 5.04206)^-11.8884))^0.0999998
    // Captures 6 groups. In the example above: 0.441049, 30395.4, 0.441049, 5.04206, -11.8884, and 0.0999998
    private static final String CURVE_REGEX = "\\s*FI\\s*=\\s*" +  // 'FI = '
            "(" + NUMBER_REGEX + ")\\s*\\+\\s*\\((" + NUMBER_REGEX + ")\\s*[\\+\\-]\\s*(" + NUMBER_REGEX + ")\\)" + // '0.441049 + (30395.4 - 0.441049)'
            "\\s*/\\s*\\(\\(1\\s*\\+\\s*\\(Conc\\s*/\\s*" + // ' / ((1 + (Conc / '
            "(" + NUMBER_REGEX + ")\\)\\s*\\^\\s*(" + NUMBER_REGEX + ")\\s*\\)\\s*\\)\\s*" + // '^-11.8884))'
            "\\^\\s*(" + NUMBER_REGEX + ")\\s*";

    private static final Pattern CURVE_PATTERN = Pattern.compile(CURVE_REGEX);

    /**
     * Handles persisting of uploaded run data into the database
     */
    private void insertData(PropertyDescriptor[] excelRunColumns, final ExpRun expRun, Container container, ExpData data, User user, Map<Analyte, List<LuminexDataRow>> sheets, LuminexExcelParser parser, LuminexRunUploadForm form) throws SQLException, ExperimentException
    {
        try
        {
            ExperimentService.get().ensureTransaction();
            ExpProtocol protocol = form.getProtocol(false);
            String dataFileName = data.getFile().getName();
            LuminexAssayProvider provider = form.getProvider();
            Set<ExpMaterial> inputMaterials = new LinkedHashSet<ExpMaterial>();
            ParticipantVisitResolver resolver = findParticipantVisitResolver(expRun, user, provider);

            // Name -> Titration
            Map<String, Titration> titrations = insertTitrations(expRun, user, form.getTitrations());

            List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();

            Set<ExpData> sourceFiles = new HashSet<ExpData>();

            for (Map.Entry<Analyte, List<LuminexDataRow>> sheet : sheets.entrySet())
            {
                Analyte analyte = sheet.getKey();

                analyte.setDataId(data.getRowId());
                analyte.setLsid(new Lsid("LuminexAnalyte", "Data-" + data.getRowId() + "." + analyte.getName()).toString());

                analyte = Table.insert(user, LuminexSchema.getTableInfoAnalytes(), analyte);

                for (String titrationName : form.getTitrationsForAnalyte(analyte.getName()))
                {
                    Titration titration = titrations.get(titrationName);
                    Map<String, Object> analyteTitration = new HashMap<String, Object>();
                    analyteTitration.put("analyteId", analyte.getRowId());
                    analyteTitration.put("titrationId", titration.getRowId());
                    Table.insert(user, LuminexSchema.getTableInfoAnalyteTitration(), analyteTitration);

                    List<LuminexWell> wells = new ArrayList<LuminexWell>();
                    LuminexDataRow firstDataRow = null;
                    for (LuminexDataRow dataRow : sheet.getValue())
                    {
                        if (PageFlowUtil.nullSafeEquals(dataRow.getDescription(), titration.getName()))
                        {
                            wells.add(new LuminexWell(dataRow));
                            if (firstDataRow == null)
                            {
                                firstDataRow = dataRow;
                            }
                        }
                    }
                    LuminexWellGroup wellGroup = new LuminexWellGroup(wells);

                    try
                    {
                        String stdCurve = analyte.getStdCurve();
                        if (stdCurve != null)
                        {
                            ParameterCurveImpl.FitParameters fitParams = parseBioPlexStdCurve(stdCurve);
                            if (fitParams != null)
                            {
                                insertCurveFit(wellGroup, user, titration, analyte, fitParams, DilutionCurve.FitType.FIVE_PARAMETER, "BioPlex");
                            }
                            else
                            {
                                LOGGER.warn("Could not parse standard curve: " + stdCurve);
                            }
                        }
                        if (firstDataRow != null)
                        {
                            importRumiCurveFit(DilutionCurve.FitType.FIVE_PARAMETER, firstDataRow, wellGroup, user, titration, analyte);
                            importRumiCurveFit(DilutionCurve.FitType.FOUR_PARAMETER, firstDataRow, wellGroup, user, titration, analyte);
                        }
                    }
                    catch (DilutionCurve.FitFailedException e)
                    {
                        throw new ExperimentException(e);
                    }
                }

                List<LuminexDataRow> dataRows = sheet.getValue();
                performOOR(dataRows, analyte);

                for (LuminexDataRow dataRow : dataRows)
                {
                    handleParticipantResolver(dataRow, resolver, inputMaterials);
                    dataRow.setProtocol(protocol.getRowId());
                    dataRow.setContainer(container);
                    Titration titration = titrations.get(dataRow.getDescription());
                    if (titration != null)
                    {
                        dataRow.setTitration(titration.getRowId());
                        StringBuilder sb = new StringBuilder();
                        if (titration.isStandard())
                        {
                            sb.append("Standard");
                        }
                        if (titration.isQcControl())
                        {
                            if (sb.length() > 0)
                            {
                                sb.append(", ");
                            }
                            sb.append("QC Control");
                        }
                        if (sb.length() > 0)
                        {
                            dataRow.setWellRole(sb.toString());
                        }
                    }
                    dataRow.setAnalyte(analyte.getRowId());
                    ExpData sourceDataForRow = data;

                    // If we've run a transform script, wire up data rows to the original data file(s) instead of the
                    // result TSV so that we can match up Excel run properties correctly
                    if (!dataFileName.equalsIgnoreCase(dataRow.getDataFile()))
                    {
                        for (ExpData potentialSourceData : expRun.getDataOutputs())
                        {
                            if (potentialSourceData.getFile() != null && potentialSourceData.getFile().getName().equalsIgnoreCase(dataRow.getDataFile()))
                            {
                                sourceDataForRow = potentialSourceData;
                                break;
                            }
                        }
                    }
                    sourceFiles.add(sourceDataForRow);
                    dataRow.setData(sourceDataForRow.getRowId());
                    rows.add(dataRow.toMap(analyte));
                }
            }

            for (ExpData sourceFile : sourceFiles)
            {
                insertExcelProperties(excelRunColumns, sourceFile, parser, user, protocol);
            }

            LuminexDataTable tableInfo = provider.createDataTable(AssayService.get().createSchema(user, container), protocol, false);
            LuminexImportHelper helper = new LuminexImportHelper();
            OntologyManager.insertTabDelimited(tableInfo, container, user, helper, rows, Logger.getLogger(LuminexDataHandler.class));

            if (inputMaterials.isEmpty())
            {
                throw new ExperimentException("Could not find any input samples in the data");
            }

            AbstractAssayProvider.addInputMaterials(expRun, user, inputMaterials);

            ExperimentService.get().commitTransaction();
        }
        catch (ValidationException ve)
        {
            throw new ExperimentException(ve.toString(), ve);
        }
        finally
        {
            ExperimentService.get().closeTransaction();
        }
    }

    private void importRumiCurveFit(DilutionCurve.FitType fitType, LuminexDataRow dataRow, LuminexWellGroup wellGroup, User user, Titration titration, Analyte analyte) throws DilutionCurve.FitFailedException, SQLException
    {
        if (fitType != DilutionCurve.FitType.FIVE_PARAMETER && fitType != DilutionCurve.FitType.FOUR_PARAMETER)
        {
            throw new IllegalArgumentException("Unsupported fit type: " + fitType);
        }
        String suffix = fitType == DilutionCurve.FitType.FIVE_PARAMETER ? "_5pl" : "_4pl";
        Number slope = (Number)dataRow.getExtraProperties().get("Slope" + suffix);
        Number upper = (Number)dataRow.getExtraProperties().get("Upper" + suffix);
        Number lower = (Number)dataRow.getExtraProperties().get("Lower" + suffix);
        Number inflection = (Number)dataRow.getExtraProperties().get("Inflection" + suffix);
        Number asymmetry = (Number)dataRow.getExtraProperties().get("Asymmtery" + suffix);

        if (slope != null && upper != null && lower != null && inflection != null)
        {
            ParameterCurveImpl.FitParameters params = new ParameterCurveImpl.FitParameters();
            params.min = lower.doubleValue();
            params.slope = slope.doubleValue();
            params.inflection = inflection.doubleValue();
            params.max = upper.doubleValue();
            params.asymmetry = asymmetry == null ? 1 : asymmetry.doubleValue();

            insertCurveFit(wellGroup, user, titration, analyte, params, fitType, "Rumi");
        }
        
    }

    public static ParameterCurveImpl.FitParameters parseBioPlexStdCurve(String stdCurve)
    {
        Matcher matcher = CURVE_PATTERN.matcher(stdCurve);
        if (matcher.matches())
        {
            ParameterCurveImpl.FitParameters params = new ParameterCurveImpl.FitParameters();
            params.min = Double.parseDouble(matcher.group(1));
            params.max = Double.parseDouble(matcher.group(2)) - Double.parseDouble(matcher.group(1));
            params.inflection = Double.parseDouble(matcher.group(4));
            params.slope = Double.parseDouble(matcher.group(5));
            params.asymmetry = Double.parseDouble(matcher.group(6));

            return params;
        }
        return null;
    }

    public static class TestCase extends Assert
    {
        @Test
        public void testBioPlexCurveParsingNegativeMinimum()
        {
            ParameterCurveImpl.FitParameters params = parseBioPlexStdCurve("FI = -2.08995 + (29934.1 + 2.08995) / ((1 + (Conc / 2.49287)^-4.99651))^0.215266");
            assertNotNull("Couldn't parse standard curve", params);
            assertEquals(params.asymmetry, 0.215266);
            assertEquals(params.min, -2.08995);
            assertEquals(params.max, 29936.18995);
            assertEquals(params.inflection, 2.49287);
            assertEquals(params.slope, -4.99651);
        }

        @Test
        public void testBioPlexCurveParsingPositiveMinimum()
        {
            ParameterCurveImpl.FitParameters params = parseBioPlexStdCurve("FI = 0.441049 + (30395.4 - 0.441049) / ((1 + (Conc / 5.04206)^-11.8884))^0.0999998");
            assertNotNull("Couldn't parse standard curve", params);
            assertEquals(0.0999998, params.asymmetry);
            assertEquals(.441049, params.min);
            assertEquals(30394.958951, params.max);
            assertEquals(5.04206, params.inflection);
            assertEquals(-11.8884, params.slope);
        }
    }

    private void insertCurveFit(LuminexWellGroup wellGroup, User user, Titration titration, Analyte analyte, ParameterCurveImpl.FitParameters params, DilutionCurve.FitType fitType, String source)
            throws DilutionCurve.FitFailedException, SQLException
    {
        ParameterCurveImpl.FiveParameterCurve curveImpl = new ParameterCurveImpl.FiveParameterCurve(Collections.singletonList(wellGroup), false, params);

        double ec50 = curveImpl.getCutoffDilution(.5);
        double auc = curveImpl.calculateAUC(DilutionCurve.AUCType.NORMAL);
        double maxFI = wellGroup.getMax();

        CurveFit fit = new CurveFit();
        fit.setAnalyteId(analyte.getRowId());
        fit.setTitrationId(titration.getRowId());
        fit.setAUC(Double.isNaN(Double.NaN) ? 0 : auc);
        fit.setEC50(Double.isInfinite(ec50) || ec50 > 5000 ? 5000 : ec50);
        fit.setMaxFI(maxFI);
        fit.setCurveType(source + " " + fitType.getLabel());

        Table.insert(user, LuminexSchema.getTableInfoCurveFit(), fit);
    }

    private ParticipantVisitResolver findParticipantVisitResolver(ExpRun expRun, User user, LuminexAssayProvider provider)
            throws ExperimentException
    {
        Map<String, ObjectProperty> mergedProperties = new HashMap<String, ObjectProperty>();
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
                Container targetStudy = provider.getTargetStudy(expRun);
                try
                {
                    return resolverType.createResolver(expRun, targetStudy, user);
                }
                catch (IOException e)
                {
                    throw new ExperimentException(e);
                }
            }
        }
        return null;
    }

    /** @return Name->Titration */
    private Map<String, Titration> insertTitrations(ExpRun expRun, User user, List<Titration> titrations)
            throws ExperimentException, SQLException
    {
        Map<String, Titration> result = new CaseInsensitiveHashMap<Titration>();

        // Insert the titrations first
        for (Titration titration : titrations)
        {
            SimpleFilter filter = new SimpleFilter("Name", titration.getName());
            filter.addCondition("RunId", expRun.getRowId());
            Titration[] exitingTitrations = Table.select(LuminexSchema.getTableInfoTitration(), Table.ALL_COLUMNS, filter, null, Titration.class);
            assert exitingTitrations.length <= 1;

            if (exitingTitrations.length > 0)
            {
                titration = exitingTitrations[0];
            }
            else
            {
                titration.setRunId(expRun.getRowId());

                titration = Table.insert(user, LuminexSchema.getTableInfoTitration(), titration);
            }
            result.put(titration.getName(), titration);
        }
        return result;
    }

    private void insertExcelProperties(PropertyDescriptor[] excelRunColumns, final ExpData data, LuminexExcelParser parser, User user, ExpProtocol protocol) throws SQLException, ValidationException, ExperimentException
    {
        Container container = data.getContainer();
        // Clear out the values - this is necessary if this is a XAR import where the run properties would
        // have been loaded as part of the ExperimentRun itself.
        Integer objectId = OntologyManager.ensureObject(container, data.getLSID());
        for (PropertyDescriptor excelRunColumn : excelRunColumns)
        {
            OntologyManager.deleteProperty(data.getLSID(), excelRunColumn.getPropertyURI(), data.getContainer(), protocol.getContainer());
        }

        List<Map<String, Object>> excelRunPropsList = new ArrayList<Map<String, Object>>();
        Map<String, Object> excelRunPropsByProperyId = new HashMap<String, Object>();
        for (Map.Entry<DomainProperty, String> entry : parser.getExcelRunProps(data.getFile()).entrySet())
        {
            excelRunPropsByProperyId.put(entry.getKey().getPropertyURI(), entry.getValue());
        }
        excelRunPropsList.add(excelRunPropsByProperyId);
        OntologyManager.insertTabDelimited(container, user, objectId, new OntologyManager.ImportHelper()
        {
            public String beforeImportObject(Map<String, Object> map) throws SQLException
            {
                return data.getLSID();
            }

            public void afterBatchInsert(int currentRow) throws SQLException
            {
            }

            public void updateStatistics(int currentRow) throws SQLException
            {
            }
        }, excelRunColumns, excelRunPropsList, true);
    }

    protected void performOOR(List<LuminexDataRow> dataRows, Analyte analyte)
    {
        Getter fiGetter = new Getter()
        {
            public Double getValue(LuminexDataRow dataRow)
            {
                if (determineOutOfRange(dataRow.getFiString()) == LuminexOORIndicator.IN_RANGE)
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
                if (determineOutOfRange(dataRow.getFiBackgroundString()) == LuminexOORIndicator.IN_RANGE)
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
                if (determineOutOfRange(dataRow.getStdDevString()) == LuminexOORIndicator.IN_RANGE)
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
                if (determineOutOfRange(dataRow.getObsConcString()) == LuminexOORIndicator.IN_RANGE)
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
                if (determineOutOfRange(dataRow.getConcInRangeString()) == LuminexOORIndicator.IN_RANGE)
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
            LuminexOORIndicator fiOORType = determineOutOfRange(dataRow.getFiString());
            dataRow.setFiOORIndicator(fiOORType.getOORIndicator(dataRow.getFiString(), dataRows, fiGetter));
            dataRow.setFi(fiOORType.getValue(dataRow.getFiString(), dataRows, fiGetter, analyte));

            LuminexOORIndicator fiBackgroundOORType = determineOutOfRange(dataRow.getFiBackgroundString());
            dataRow.setFiBackgroundOORIndicator(fiBackgroundOORType.getOORIndicator(dataRow.getFiBackgroundString(), dataRows, fiBackgroundGetter));
            dataRow.setFiBackground(fiBackgroundOORType.getValue(dataRow.getFiBackgroundString(), dataRows, fiBackgroundGetter, analyte));

            LuminexOORIndicator stdDevOORType = determineOutOfRange(dataRow.getStdDevString());
            dataRow.setStdDevOORIndicator(stdDevOORType.getOORIndicator(dataRow.getStdDevString(), dataRows, stdDevGetter));
            dataRow.setStdDev(stdDevOORType.getValue(dataRow.getStdDevString(), dataRows, stdDevGetter, analyte));

            LuminexOORIndicator obsConcOORType = determineOutOfRange(dataRow.getObsConcString());
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
                        obsConc = dataRow.getDilution() * maxStandardObsConc;
                    }
                    else
                    {
                        obsConc = null;
                    }
                    break;
                case OUT_OF_RANGE_BELOW:
                    if (dataRow.getDilution() != null && minStandardObsConc != null)
                    {
                        obsConc = dataRow.getDilution() * minStandardObsConc;
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
                        if (minStandardFI != null && dataRow.getFi() < minStandardFI && minStandardObsConc != null)
                        {
                            obsConc = dataRow.getDilution() * minStandardObsConc;
                        }
                        else if (maxStandardFI != null && dataRow.getFi() > maxStandardFI && maxStandardObsConc != null)
                        {
                            obsConc = dataRow.getDilution() * maxStandardObsConc;
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

            LuminexOORIndicator concInRangeOORType = determineOutOfRange(dataRow.getConcInRangeString());
            dataRow.setConcInRangeOORIndicator(concInRangeOORType.getOORIndicator(dataRow.getConcInRangeString(), dataRows, concInRangeGetter));
            dataRow.setConcInRange(concInRangeOORType.getValue(dataRow.getConcInRangeString(), dataRows, concInRangeGetter, analyte));
        }
    }

    protected void handleParticipantResolver(LuminexDataRow dataRow, ParticipantVisitResolver resolver, Set<ExpMaterial> materialInputs)
    {
        String value = dataRow.getDescription();
        if (resolver != null && value != null)
        {
            value = value.trim();
            String specimenID = null;
            if (value.indexOf(",") == -1)
            {
                specimenID = value;
            }
            // First try resolving the whole description column as a specimen id
            ParticipantVisit match = resolver.resolve(specimenID, null, null, null, null);
            String extraSpecimenInfo = null;
            if (!isResolved(match))
            {
                // If that doesn't work, check if we have a specimen ID followed by a : or ; and possibly other text
                int index = value.indexOf(';');
                if (index == -1)
                {
                    // No ';', might have a ':'
                    index = value.indexOf(':');
                }
                else
                {
                    int index2 = value.indexOf(':');
                    if (index2 != -1)
                    {
                        // We have both, use the first one
                        index = Math.min(index, index2);
                    }
                }

                if (index != -1)
                {
                    specimenID = value.substring(0, index);
                    match = resolver.resolve(specimenID, null, null, null, null);
                }

                // If that doesn't work either, try to parse as "<PTID>, Visit <VisitNumber>, <Date>, <ExtraInfo>"
                if (!isResolved(match))
                {
                    String valueToSplit = index == -1 ? value : value.substring(index + 1);
                    String[] parts = valueToSplit.split(",");
                    if (parts.length >= 3)
                    {
                        match = resolveParticipantVisitInfo(resolver, specimenID, parts);

                        StringBuilder sb = new StringBuilder();
                        String separator = "";
                        for (int i = 3; i < parts.length; i++)
                        {
                            sb.append(separator);
                            separator = ", ";
                            sb.append(parts[i].trim());
                        }
                        if (sb.length() > 0)
                        {
                            extraSpecimenInfo = sb.toString();
                        }
                    }
                }
            }

            dataRow.setParticipantID(match.getParticipantID());
            dataRow.setVisitID(match.getVisitID());
            dataRow.setDate(match.getDate());
            dataRow.setSpecimenID(specimenID);
            dataRow.setExtraSpecimenInfo(extraSpecimenInfo == null ? null : extraSpecimenInfo.trim());
            materialInputs.add(match.getMaterial());
        }
    }

    private boolean isResolved(ParticipantVisit match)
    {
        return match.getParticipantID() != null || match.getVisitID() != null || match.getDate() != null;
    }

    private ParticipantVisit resolveParticipantVisitInfo(ParticipantVisitResolver resolver, String specimenID, String[] parts)
    {
        // First part is participant id
        String participantId = parts[0].trim();
        Double visitId;
        Date date;
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
        return resolver.resolve(specimenID, participantId, visitId, date, null);
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
        List<Integer> ids = new ArrayList<Integer>();
        for (ExpData expData : data)
        {
            ids.add(expData.getRowId());

            if (ids.size() > 200)
            {
                deleteDatas(ids);
                ids.clear();
            }
        }
        if (!ids.isEmpty())
        {
            deleteDatas(ids);
        }
    }

    private void deleteDatas(List<Integer> ids)
    {
        try
        {
            Object[] params = ids.toArray(new Integer[ids.size()]);
            String idSQL = StringUtils.repeat("?", ", ", ids.size());
            // Clean up data row properties
            Table.execute(LuminexSchema.getSchema(),
                    "DELETE FROM " + OntologyManager.getTinfoObjectProperty() + " WHERE ObjectId IN (SELECT o.ObjectID FROM " +
                    LuminexSchema.getTableInfoDataRow() + " dr, " + OntologyManager.getTinfoObject() +
                    " o WHERE o.ObjectURI = dr.LSID AND dr.DataId IN (" + idSQL + "))", params);
            Table.execute(LuminexSchema.getSchema(),
                    "DELETE FROM " + OntologyManager.getTinfoObject() + " WHERE ObjectURI IN (SELECT LSID FROM " +
                    LuminexSchema.getTableInfoDataRow() + " WHERE DataId IN (" + idSQL + "))", params);

            // Clean up analyte properties
            Table.execute(LuminexSchema.getSchema(),
                    "DELETE FROM " + OntologyManager.getTinfoObjectProperty() + " WHERE ObjectId IN (SELECT o.ObjectID FROM " +
                    LuminexSchema.getTableInfoDataRow() + " dr, " + OntologyManager.getTinfoObject() +
                    " o, " + LuminexSchema.getTableInfoAnalytes() + " a WHERE a.RowId = dr.AnalyteId AND o.ObjectURI = " +
                    "a.LSID AND dr.DataId IN (" + idSQL + "))", params);
            Table.execute(LuminexSchema.getSchema(),
                    "DELETE FROM " + OntologyManager.getTinfoObject() + " WHERE ObjectURI IN (SELECT a.LSID FROM " +
                    LuminexSchema.getTableInfoDataRow() + " dr, " + LuminexSchema.getTableInfoAnalytes() +
                    " a WHERE dr.AnalyteId = a.RowId AND dr.DataId IN (" + idSQL + "))", params);

            Table.execute(LuminexSchema.getSchema(), "DELETE FROM " + LuminexSchema.getTableInfoDataRow() +
                    " WHERE DataId IN (" + idSQL + ")", params);

            // Clean up exclusions
            Table.execute(LuminexSchema.getSchema(), "DELETE FROM " + LuminexSchema.getTableInfoWellExclusionAnalyte() +
                    " WHERE WellExclusionId IN (SELECT RowId FROM " + LuminexSchema.getTableInfoWellExclusion() +
                    " WHERE DataId IN (" + idSQL + "))", params);
            Table.execute(LuminexSchema.getSchema(), "DELETE FROM " + LuminexSchema.getTableInfoWellExclusion() +
                    " WHERE DataId IN (" + idSQL + ")", params);
            Table.execute(LuminexSchema.getSchema(), "DELETE FROM " + LuminexSchema.getTableInfoRunExclusionAnalyte() +
                    " WHERE RunId IN (SELECT RunId FROM " + ExperimentService.get().getTinfoProtocolApplication() +
                    " WHERE RowId IN (SELECT SourceApplicationId FROM " + ExperimentService.get().getTinfoData() +
                    " WHERE RowId IN (" + idSQL + ")))", params);
            Table.execute(LuminexSchema.getSchema(), "DELETE FROM " + LuminexSchema.getTableInfoRunExclusion() +
                    " WHERE RunId IN (SELECT RunId FROM " + ExperimentService.get().getTinfoProtocolApplication() +
                    " WHERE RowId IN (SELECT SourceApplicationId FROM " + ExperimentService.get().getTinfoData() +
                    " WHERE RowId IN (" + idSQL + ")))", params);

            // Clean up curve fits
            Table.execute(LuminexSchema.getSchema(), "DELETE FROM " + LuminexSchema.getTableInfoCurveFit() +
                    " WHERE AnalyteId IN (SELECT RowId FROM " + LuminexSchema.getTableInfoAnalytes() +
                    " WHERE DataId IN (" + idSQL + "))", params);

            // Clean up analytes and titrations
            Table.execute(LuminexSchema.getSchema(), "DELETE FROM " + LuminexSchema.getTableInfoAnalyteTitration() +
                    " WHERE AnalyteId IN (SELECT RowId FROM " + LuminexSchema.getTableInfoAnalytes() +
                    " WHERE DataId IN (" + idSQL + "))", params);
            Table.execute(LuminexSchema.getSchema(), "DELETE FROM " + LuminexSchema.getTableInfoAnalytes() +
                    " WHERE DataId IN (" + idSQL + ")", params);
            Table.execute(LuminexSchema.getSchema(), "DELETE FROM " + LuminexSchema.getTableInfoTitration() +
                    " WHERE RunId IN (SELECT pa.RunId FROM " + ExperimentService.get().getTinfoProtocolApplication() +
                    " pa, " + ExperimentService.get().getTinfoData() +
                    " d WHERE pa.RowId = d.SourceApplicationId AND d.RowId IN (" + idSQL + "))", params);
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    public void deleteData(ExpData data, Container container, User user)
    {
        deleteDatas(Collections.singletonList(data.getRowId()));
    }

    public void runMoved(ExpData newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user, int oldDataRowID) throws ExperimentException
    {
        throw new UnsupportedOperationException();
    }

    public Map<DataType, List<Map<String, Object>>> getValidationDataMap(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        ExpProtocol protocol = data.getRun().getProtocol();
        LuminexExcelParser parser = new LuminexExcelParser(protocol, Collections.singleton(dataFile));

        Map<DataType, List<Map<String, Object>>> datas = new HashMap<DataType, List<Map<String, Object>>>();
        List<Map<String, Object>> dataRows = new ArrayList<Map<String, Object>>();

        Set<String> titrations = parser.getTitrations();

        for (Map.Entry<Analyte, List<LuminexDataRow>> entry : parser.getSheets().entrySet())
        {
            for (LuminexDataRow dataRow : entry.getValue())
            {
                Map<String, Object> dataMap = dataRow.toMap(entry.getKey());
                dataMap.put("titration", dataRow.getDescription() != null && titrations.contains(dataRow.getDescription()));
                dataMap.remove("data");
                dataRows.add(dataMap);
            }
        }
        datas.put(LUMINEX_TRANSFORMED_DATA_TYPE, dataRows);
        return datas;
    }

    public void importTransformDataMap(ExpData data, AssayRunUploadContext context, ExpRun run, List<Map<String, Object>> dataMap) throws ExperimentException
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
            row.remove("titration");
            LuminexDataRow dataRow = rowFactory.fromMap(row);
            dataRow.setExtraProperties(row);

            // since a transform script can generate new records for analytes with > 1 standard selected, set lsids for new records
            if (dataRow.getLsid() == null)
            {
                dataRow.setLsid(new Lsid(LuminexAssayProvider.LUMINEX_DATA_ROW_LSID_PREFIX, GUID.makeGUID()).toString());
            }

            Map.Entry<Analyte, List<LuminexDataRow>> entry = LuminexExcelParser.ensureAnalyte(analyte, sheets);
            entry.getValue().add(dataRow);
        }

        LuminexRunUploadForm form = (LuminexRunUploadForm)context;

        importData(data, run, context.getUser(), null, sheets, form.getParser(), form);
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
}
