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
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.labkey.api.assay.dilution.DilutionCurve;
import org.labkey.api.assay.dilution.ParameterCurveImpl;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ObjectFactory;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.exp.*;
import org.labkey.api.exp.api.*;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.qc.TransformDataHandler;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.study.ParticipantVisit;
import org.labkey.api.study.assay.*;
import org.labkey.api.util.FileType;
import org.labkey.api.util.GUID;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.Stats;
import org.labkey.api.view.ViewBackgroundInfo;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * We've made the simplifying assumption that in the case of updating an existing run, we don't have to worry about
 * deleting any data (no data rows, analytes, etc have disappeared completely).
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
        LuminexRunContext form = null;
        if (context instanceof AssayUploadXarContext && ((AssayUploadXarContext)context).getContext() instanceof LuminexRunContext)
        {
            form = (LuminexRunContext)((AssayUploadXarContext)context).getContext();
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

    private static final String NUMBER_REGEX = "-??[0-9]+(?:\\.[0-9]+)?(?:[Ee][\\+\\-][0-9]+)?";

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
    private void importData(ExpData data, ExpRun expRun, User user, @NotNull Logger log, Map<Analyte, List<LuminexDataRow>> sheets, LuminexExcelParser parser, LuminexRunContext form) throws ExperimentException
    {
        try
        {
            ExperimentService.get().ensureTransaction();
            ExpProtocol protocol = form.getProtocol();
            String dataFileName = data.getFile().getName();
            LuminexAssayProvider provider = form.getProvider();
            Set<ExpMaterial> inputMaterials = new LinkedHashSet<ExpMaterial>();
            ParticipantVisitResolver resolver = findParticipantVisitResolver(expRun, user, provider);

            Domain excelRunDomain = AbstractAssayProvider.getDomainByPrefix(protocol, LuminexAssayProvider.ASSAY_DOMAIN_EXCEL_RUN);
            if (excelRunDomain == null)
            {
                throw new ExperimentException("Could not find Excel run domain for protocol with LSID " + protocol.getLSID());
            }

            Domain runDomain = provider.getRunDomain(protocol);
            if (runDomain == null)
            {
                throw new ExperimentException("Could not find run domain for protocol with LSID " + protocol.getLSID());
            }

            // Look for isotype and conjugate as run properties
            String isotype = null;
            String conjugate = null;
            for (DomainProperty runProp : runDomain.getProperties())
            {
                if (runProp.getName().equalsIgnoreCase("Conjugate"))
                {
                    Object value = expRun.getProperty(runProp);
                    conjugate = value == null ? null : value.toString();
                }
                else if (runProp.getName().equalsIgnoreCase("Isotype"))
                {
                    Object value = expRun.getProperty(runProp);
                    isotype = value == null ? null : value.toString();
                }
            }
            
            // Name -> Titration
            Map<String, Titration> titrations = insertTitrations(expRun, user, form.getTitrations());

            // Keep these in a map so that we can easily look them up against the rows that are already in the database
            Map<DataRowKey, Map<String, Object>> rows = new LinkedHashMap<DataRowKey, Map<String, Object>>();
            Set<ExpData> sourceFiles = new HashSet<ExpData>();

            Map<String, Analyte> existingAnalytes = getExistingAnalytes(expRun);

            for (Map.Entry<Analyte, List<LuminexDataRow>> sheet : sheets.entrySet())
            {
                Analyte analyte = sheet.getKey();
                List<LuminexDataRow> dataRows = sheet.getValue();

                // Look at analyte properties to find the conjugate if we don't have one from the run properties
                if (conjugate == null)
                {
                    for (Map.Entry<DomainProperty, String> entry : form.getAnalyteProperties(analyte.getName()).entrySet())
                    {
                        if (entry.getKey().getName().equalsIgnoreCase("Conjugate"))
                        {
                            conjugate = entry.getValue();
                        }
                    }
                }
                // Look at analyte properties to find the isotype if we don't have one from the run properties
                if (isotype == null)
                {
                    for (Map.Entry<DomainProperty, String> entry : form.getAnalyteProperties(analyte.getName()).entrySet())
                    {
                        if (entry.getKey().getName().equalsIgnoreCase("Isotype"))
                        {
                            isotype = entry.getValue();
                        }
                    }
                }

                analyte.setDataId(data.getRowId());

                analyte = saveAnalyte(expRun, user, existingAnalytes, analyte);

                insertTitrationAnalyteMappings(user, form, titrations, sheet.getValue(), analyte, conjugate, isotype, protocol);

                performOOR(dataRows, analyte);

                for (LuminexDataRow dataRow : dataRows)
                {
                    handleParticipantResolver(dataRow, resolver, inputMaterials);
                    dataRow.setProtocol(protocol.getRowId());
                    dataRow.setContainer(expRun.getContainer());
                    Titration titration = titrations.get(dataRow.getDescription());
                    if (titration != null)
                    {
                        dataRow.setTitration(titration.getRowId());
                        List<String> roles = new ArrayList<String>();
                        if (titration.isStandard())
                        {
                            roles.add("Standard");
                        }
                        if (titration.isQcControl())
                        {
                            roles.add("QC Control");
                        }
                        if (!roles.isEmpty())
                        {
                            dataRow.setWellRole(StringUtils.join(roles, ", "));
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
                }

                // Now that we've made sure each data row to the appropriate data file, make sure that we
                // have %CV and StdDev. It's important to wait so that we know the scope in which to do the aggregate
                // calculations - we only want to look for replicates within the same plate.
                ensureSummaryStats(dataRows);

                // now add the dataRows to the rows list to be persisted
                for (LuminexDataRow dataRow : dataRows)
                    rows.put(new DataRowKey(dataRow), dataRow.toMap(analyte));
            }

            List<Integer> dataIds = new ArrayList<Integer>();
            for (ExpData sourceFile : sourceFiles)
            {
                insertExcelProperties(excelRunDomain, sourceFile, parser, user, protocol);
                dataIds.add(sourceFile.getRowId());
            }

            saveDataRows(expRun, user, protocol, provider, rows, dataIds);

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
        catch (SQLException e)
        {
            log.error("Failed to load from data file " + data.getFile().getAbsolutePath(), e);
            throw new ExperimentException("Failed to load from data file " + data.getFile().getAbsolutePath() + "(" + e.toString() + ")", e);
        }
        finally
        {
            ExperimentService.get().closeTransaction();
        }
    }

    /** Saves the data rows, updating if they already exist, inserting if not */
    private void saveDataRows(ExpRun expRun, User user, ExpProtocol protocol, LuminexAssayProvider provider, Map<DataRowKey, Map<String, Object>> rows, List<Integer> dataIds)
            throws SQLException, ValidationException
    {
        // Do a query to find all of the rows that have already been inserted 
        LuminexDataTable tableInfo = provider.createDataTable(AssayService.get().createSchema(user, expRun.getContainer()), protocol, false);
        SimpleFilter filter = new SimpleFilter(new SimpleFilter.InClause("Data", dataIds));
        LuminexDataRow[] databaseRows = Table.select(tableInfo, Table.ALL_COLUMNS, filter, null, LuminexDataRow.class);

        Map<DataRowKey, LuminexDataRow> existingRows = new HashMap<DataRowKey, LuminexDataRow>();
        for (LuminexDataRow existingRow : databaseRows)
        {
            existingRows.put(new DataRowKey(existingRow), existingRow);
        }

        List<Map<String, Object>> insertRows = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> updateRows = new ArrayList<Map<String, Object>>();

        // Sort them into new and existing rows
        for (Map.Entry<DataRowKey, Map<String, Object>> entry : rows.entrySet())
        {
            LuminexDataRow existingRow = existingRows.get(entry.getKey());
            if (existingRow == null)
            {
                insertRows.add(entry.getValue());
            }
            else
            {
                Map<String, Object> updateRow = entry.getValue();
                updateRow.put("RowId", existingRow.getRowId());
                updateRow.put("LSID", existingRow.getLsid());
                updateRows.add(updateRow);
            }
        }

        LuminexImportHelper helper = new LuminexImportHelper();
        OntologyManager.insertTabDelimited(tableInfo, expRun.getContainer(), user, helper, insertRows, Logger.getLogger(LuminexDataHandler.class));
        OntologyManager.updateTabDelimited(tableInfo, expRun.getContainer(), user, helper, updateRows, Logger.getLogger(LuminexDataHandler.class));
    }

    /** Inserts or updates an analyte row in the hard table */
    private Analyte saveAnalyte(ExpRun expRun, User user, Map<String, Analyte> existingAnalytes, Analyte analyte)
            throws SQLException
    {
        Analyte existingAnalyte = existingAnalytes.get(analyte.getName());
        if (existingAnalyte != null)
        {
            // Need the original rowId so that we update the existing row
            analyte.setRowId(existingAnalyte.getRowId());
            // Retain the LSID so we don't lose the custom property values
            analyte.setLsid(existingAnalyte.getLsid());
            analyte = Table.update(user, LuminexSchema.getTableInfoAnalytes(), analyte, analyte.getRowId());
        }
        else
        {
            analyte.setLsid(new Lsid("LuminexAnalyte", "Data-" + expRun.getRowId() + "." + analyte.getName()).toString());
            analyte = Table.insert(user, LuminexSchema.getTableInfoAnalytes(), analyte);
        }
        return analyte;
    }

    /** @return Name->Analyte for all of the analytes that are already associated with this run */
    private Map<String, Analyte> getExistingAnalytes(ExpRun expRun) throws SQLException
    {
        SQLFragment sql = new SQLFragment("SELECT a.* FROM ");
        sql.append(LuminexSchema.getTableInfoAnalytes(), "a");
        sql.append(", ");
        sql.append(ExperimentService.get().getTinfoData(), "d");
        sql.append(" WHERE a.DataId = d.RowId AND d.RunId = ?");
        sql.add(expRun.getRowId());
        Analyte[] databaseAnalytes = Table.executeQuery(LuminexSchema.getSchema(), sql, Analyte.class);
        Map<String, Analyte> existingAnalytes = new HashMap<String, Analyte>();
        for (Analyte databaseAnalyte : databaseAnalytes)
        {
            existingAnalytes.put(databaseAnalyte.getName(), databaseAnalyte);
        }
        return existingAnalytes;
    }

    private static class DataRowKey
    {
        private int _dataId;
        private int _analyteId;
        private String _well;

        public DataRowKey(LuminexDataRow dataRow)
        {
            _dataId = dataRow.getData();
            _analyteId = dataRow.getAnalyte();
            _well = dataRow.getWell();
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DataRowKey that = (DataRowKey) o;

            if (_analyteId != that._analyteId) return false;
            if (_dataId != that._dataId) return false;
            if (_well != null ? !_well.equals(that._well) : that._well != null) return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = _dataId;
            result = 31 * result + _analyteId;
            result = 31 * result + (_well != null ? _well.hashCode() : 0);
            return result;
        }
    }

    /** Calculate %CV and StdDev for raw data rows that don't have it already, aggregating the matching replicate wells */
    private void ensureSummaryStats(List<LuminexDataRow> dataRows)
    {
        for (LuminexDataRow dataRow : dataRows)
        {
            if (!dataRow.isSummary() && (dataRow.getCv() == null || dataRow.getStdDev() == null))
            {
                List<Double> fis = new ArrayList<Double>();
                for (LuminexDataRow statRow : dataRows)
                {
                    // Only look for replicates within the same data file (plate)
                    if (statRow.getFi() != null && !statRow.isSummary() &&
                        ObjectUtils.equals(statRow.getDilution(), dataRow.getDilution()) &&
                        ObjectUtils.equals(statRow.getExpConc(), dataRow.getExpConc()) &&
                        ObjectUtils.equals(statRow.getDescription(), dataRow.getDescription()) &&
                        ObjectUtils.equals(statRow.getType(), dataRow.getType()) &&
                        ObjectUtils.equals(statRow.getData(), dataRow.getData()) &&
                        ObjectUtils.equals(statRow.getAnalyte(), dataRow.getAnalyte()))
                    {
                        fis.add(statRow.getFi());
                    }
                }

                if (fis.size() > 1)
                {
                    Stats.DoubleStats stats = new Stats.DoubleStats(ArrayUtils.toPrimitive(fis.toArray(new Double[fis.size()])));
                    double stdDev = stats.getStdDev();
                    double mean = Math.abs(stats.getMean());
                    dataRow.setStdDev(stdDev);
                    dataRow.setCv(mean == 0.0 ? null : stdDev / mean);
                }
            }
        }
    }

    private GuideSet determineGuideSet(Analyte analyte, Titration titration, String conjugate, String isotype, ExpProtocol protocol)
    {
        GuideSet guideSet = GuideSetTable.GuideSetTableUpdateService.getMatchingCurrentGuideSet(protocol, analyte.getName(), titration.getName(), conjugate, isotype);
        if (guideSet != null)
        {
            return guideSet;
        }

        // Should we create a new one automatically here?
        return null;
    }

    private void insertTitrationAnalyteMappings(User user, LuminexRunContext form, Map<String, Titration> titrations, List<LuminexDataRow> dataRows, Analyte analyte, String conjugate, String isotype, ExpProtocol protocol)
            throws ExperimentException, SQLException
    {
        // Insert mappings for all of the titrations that aren't standards
        for (Titration titration : titrations.values())
        {
            if (!titration.isStandard())
            {
                insertAnalyteTitrationMapping(user, dataRows, analyte, titration, conjugate, isotype, protocol);
            }
        }

        // Insert mappings for all of the standard titrations that have been selected for this analyte
        for (String titrationName : form.getTitrationsForAnalyte(analyte.getName()))
        {
            Titration titration = titrations.get(titrationName);
            insertAnalyteTitrationMapping(user, dataRows, analyte, titration, conjugate, isotype, protocol);
        }
    }

    private void insertAnalyteTitrationMapping(User user, List<LuminexDataRow> dataRows, Analyte analyte, Titration titration, String conjugate, String isotype, ExpProtocol protocol)
            throws SQLException, ExperimentException
    {
        LuminexWellGroup wellGroup = titration.buildWellGroup(dataRows);

        // Insert the mapping row, which includes the Max FI
        SimpleFilter filter = new SimpleFilter("AnalyteId", analyte.getRowId());
        filter.addCondition("TitrationId", titration.getRowId());

        AnalyteTitration analyteTitration = Table.selectObject(LuminexSchema.getTableInfoAnalyteTitration(), filter, null, AnalyteTitration.class);
        boolean newRow = analyteTitration == null;
        if (analyteTitration == null)
        {
            analyteTitration = new AnalyteTitration();
            analyteTitration.setAnalyteId(analyte.getRowId());
            analyteTitration.setTitrationId(titration.getRowId());
        }

        // TODO - be sure that we respect exclusion state
        double maxFI = wellGroup.getMax();
        analyteTitration.setMaxFI(maxFI == Double.MIN_VALUE ? null : maxFI);

        if (newRow)
        {
            // Check if we have a guide set for this combo
            GuideSet currentGuideSet = determineGuideSet(analyte, titration, conjugate, isotype, protocol);
            if (currentGuideSet != null)
            {
                analyteTitration.setGuideSetId(currentGuideSet.getRowId());
            }

            Table.insert(user, LuminexSchema.getTableInfoAnalyteTitration(), analyteTitration);
        }
        else
        {
            Map<String, Object> keys = new CaseInsensitiveHashMap<Object>();
            keys.put("AnalyteId", analyte.getRowId());
            keys.put("TitrationId", titration.getRowId());
            Table.update(user, LuminexSchema.getTableInfoAnalyteTitration(), analyteTitration, keys);
        }

        // Insert the curve fit values (EC50 and AUC)
        try
        {
            String stdCurve = analyte.getStdCurve();
            if (stdCurve != null)
            {
                ParameterCurveImpl.FitParameters fitParams = parseBioPlexStdCurve(stdCurve);
                if (fitParams != null)
                {
                    insertOrUpdateCurveFit(wellGroup, user, titration, analyte, fitParams, DilutionCurve.FitType.FIVE_PARAMETER, "BioPlex");
                }
                else
                {
                    LOGGER.warn("Could not parse standard curve: " + stdCurve);
                }
            }
            if (!wellGroup.getWellData(false).isEmpty())
            {
                LuminexDataRow firstDataRow = wellGroup.getWellData(false).get(0)._dataRow;
                importRumiCurveFit(DilutionCurve.FitType.FIVE_PARAMETER, firstDataRow, wellGroup, user, titration, analyte);
                importRumiCurveFit(DilutionCurve.FitType.FOUR_PARAMETER, firstDataRow, wellGroup, user, titration, analyte);

                // Do the trapezoidal AUC calculation
                double auc = calculateTrapezoidalAUC(wellGroup);

                CurveFit fit = new CurveFit();
                fit.setAUC(auc);
                fit.setAnalyteId(analyte.getRowId());
                fit.setTitrationId(titration.getRowId());
                fit.setCurveType("Trapezoidal");

                insertOrUpdateCurveFit(user, fit);
            }
        }
        catch (DilutionCurve.FitFailedException e)
        {
            throw new ExperimentException(e);
        }
    }

    private double calculateTrapezoidalAUC(LuminexWellGroup wellGroup)
    {
        double auc = 0;
        List<LuminexWell> wells = wellGroup.getWellData(true);
        if (!wells.isEmpty())
        {
            LuminexWell previousWell = wells.get(0);
            for (int i = 1; i < wells.size(); i++)
            {
                LuminexWell well = wells.get(i);
                auc += Math.max(Math.abs(Math.log10(well.getDose()) - Math.log10(previousWell.getDose())) *
                        (Math.min(previousWell.getValue().doubleValue(), well.getValue().doubleValue()) +
                            0.5 * Math.abs(previousWell.getValue().doubleValue() - well.getValue().doubleValue())), 0);
                previousWell = well;
            }
        }
        return auc;
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
        Number asymmetry = (Number)dataRow.getExtraProperties().get("Asymmetry" + suffix);

        if (slope != null && upper != null && lower != null && inflection != null)
        {
            ParameterCurveImpl.FitParameters params = new ParameterCurveImpl.FitParameters();
            params.min = lower.doubleValue();
            params.slope = slope.doubleValue();
            params.inflection = inflection.doubleValue();
            params.max = upper.doubleValue();
            params.asymmetry = asymmetry == null ? 1 : asymmetry.doubleValue();

            insertOrUpdateCurveFit(wellGroup, user, titration, analyte, params, fitType, null);
        }
        
    }

    private ParameterCurveImpl.FitParameters parseBioPlexStdCurve(String stdCurve)
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
            ParameterCurveImpl.FitParameters params = new LuminexDataHandler().parseBioPlexStdCurve("FI = -2.08995 + (29934.1 + 2.08995) / ((1 + (Conc / 2.49287)^-4.99651))^0.215266");
            assertNotNull("Couldn't parse standard curve", params);
            assertEquals(params.asymmetry, 0.215266);
            assertEquals(params.min, -2.08995);
            assertEquals(params.max, 29936.18995);
            assertEquals(params.inflection, 2.49287);
            assertEquals(params.slope, -4.99651);
        }

        @Test
        public void testBioPlexCurveParsingBadInput()
        {
            ParameterCurveImpl.FitParameters params = new LuminexDataHandler().parseBioPlexStdCurve("FIA = -2.08995 + (29934.1 + 2.08995) / ((1 + (Conc / 2.49287)^-4.99651))^0.215266");
            assertNull("Shouldn't return a standard curve", params);
        }

        @Test
        public void testBioPlexCurveParsingScientific()
        {
            ParameterCurveImpl.FitParameters params = new LuminexDataHandler().parseBioPlexStdCurve("FI = -0.723451 + (2.48266E+006 + 0.723451) / ((1 + (Conc / 21.932)^-0.192152))^10");
            assertNotNull("Couldn't parse standard curve", params);
            assertEquals(params.asymmetry, 10.0);
            assertEquals(params.min, -0.723451);
            assertEquals(params.max, 2482660.723451);
            assertEquals(params.inflection, 21.932);
            assertEquals(params.slope, -0.192152);
        }

        @Test
        public void testBioPlexCurveParsingPositiveMinimum()
        {
            ParameterCurveImpl.FitParameters params = new LuminexDataHandler().parseBioPlexStdCurve("FI = 0.441049 + (30395.4 - 0.441049) / ((1 + (Conc / 5.04206)^-11.8884))^0.0999998");
            assertNotNull("Couldn't parse standard curve", params);
            assertEquals(0.0999998, params.asymmetry);
            assertEquals(.441049, params.min);
            assertEquals(30394.958951, params.max);
            assertEquals(5.04206, params.inflection);
            assertEquals(-11.8884, params.slope);
        }

        @Test
        public void testAUCSummaryData() throws DilutionCurve.FitFailedException
        {
            // test calculation using dilutions for a control
            List<LuminexWell> wells = new ArrayList<LuminexWell>();

            wells.add(new LuminexWell(new LuminexDataRow("C1", "A1", 30427, 1, 100)));
            wells.add(new LuminexWell(new LuminexDataRow("C2", "A2", 30139, 1, 600)));
            wells.add(new LuminexWell(new LuminexDataRow("C3", "A3", 26612.25, 1, 3600)));
            wells.add(new LuminexWell(new LuminexDataRow("C4", "A4", 4867, 1, 21600)));
            wells.add(new LuminexWell(new LuminexDataRow("C5", "A5", 571.75, 1, 129600)));
            wells.add(new LuminexWell(new LuminexDataRow("C6", "A6", 80.5, 1, 777600)));
            wells.add(new LuminexWell(new LuminexDataRow("C7", "A7", 16, 1, 4665600)));
            wells.add(new LuminexWell(new LuminexDataRow("C8", "A8", 2.5, 1, 27993600)));
            wells.add(new LuminexWell(new LuminexDataRow("C9", "A9", 2, 1, 167961600)));
            wells.add(new LuminexWell(new LuminexDataRow("C10", "A10", 1.5, 1, 1007769600)));
            LuminexWellGroup group = new LuminexWellGroup(wells);

            assertEquals("Check number of replicates found", 10, group.getWellData(true).size());
            assertEquals("Check replicate value", 30427.0, group.getWellData(true).get(0).getValue());
            assertEquals("Check number of raw wells", 10, group.getWellData(false).size());

            assertEquals("AUC", 60310.8, Math.round(new LuminexDataHandler().calculateTrapezoidalAUC(group) * 10.0) / 10.0);

            // test calculation using expected concentrations for a standard
            wells = new ArrayList<LuminexWell>();

            wells.add(new LuminexWell(new LuminexDataRow("S1", "A1,B1", 32320, 100, 1)));
            wells.add(new LuminexWell(new LuminexDataRow("S2", "A2,B2", 32189.5, 20, 1)));
            wells.add(new LuminexWell(new LuminexDataRow("S3", "A3,B3", 30695.5, 4, 1)));
            wells.add(new LuminexWell(new LuminexDataRow("S4", "A4,B4", 20215.25, 0.8, 1)));
            wells.add(new LuminexWell(new LuminexDataRow("S5", "A5,B5", 5586.5, 0.16, 1)));
            wells.add(new LuminexWell(new LuminexDataRow("S6", "A6,B6", 1204, 0.032, 1)));
            wells.add(new LuminexWell(new LuminexDataRow("S7", "A7,B7", 270.25, 0.0064, 1)));
            wells.add(new LuminexWell(new LuminexDataRow("S8", "A8,B8", 60.75, 0.00128, 1)));
            wells.add(new LuminexWell(new LuminexDataRow("S9", "A9,B9", 20.5, 0.00026, 1)));
            wells.add(new LuminexWell(new LuminexDataRow("S10", "A10,B10", 10.5, 0.00005, 1)));
            group = new LuminexWellGroup(wells);

            assertEquals("Check number of replicates found", 10, group.getWellData(true).size());
            assertEquals("Check replicate value", 10.5, group.getWellData(true).get(0).getValue());
            assertEquals("Check number of raw wells", 10, group.getWellData(false).size());

            assertEquals("AUC", 74375.6, Math.round(new LuminexDataHandler().calculateTrapezoidalAUC(group) * 10.0) / 10.0);
        }

        @Test
        public void testAUCRawData() throws DilutionCurve.FitFailedException
        {
            // test calculation using dilutions for a control
            List<LuminexWell> wells = new ArrayList<LuminexWell>();

            wells.add(new LuminexWell(new LuminexDataRow("C1", "G1", 30271, 1, 100)));
            wells.add(new LuminexWell(new LuminexDataRow("C1", "H1", 30583, 1, 100)));
            wells.add(new LuminexWell(new LuminexDataRow("C2", "G2", 30151.5, 1, 600)));
            wells.add(new LuminexWell(new LuminexDataRow("C2", "H2", 30126.5, 1, 600)));
            wells.add(new LuminexWell(new LuminexDataRow("C3", "G3", 26439, 1, 3600)));
            wells.add(new LuminexWell(new LuminexDataRow("C3", "H3", 26785.5, 1, 3600)));
            wells.add(new LuminexWell(new LuminexDataRow("C4", "G4", 4786, 1, 21600)));
            wells.add(new LuminexWell(new LuminexDataRow("C4", "H4", 4948, 1, 21600)));
            wells.add(new LuminexWell(new LuminexDataRow("C5", "G5", 553.5, 1, 129601)));
            wells.add(new LuminexWell(new LuminexDataRow("C5", "H5", 590, 1, 129601)));
            wells.add(new LuminexWell(new LuminexDataRow("C6", "G6", 77.5, 1, 777605)));
            wells.add(new LuminexWell(new LuminexDataRow("C6", "H6", 83.5, 1, 777605)));
            wells.add(new LuminexWell(new LuminexDataRow("C7", "G7", 16.5, 1, 4664179)));
            wells.add(new LuminexWell(new LuminexDataRow("C7", "H7", 15.5, 1, 4664179)));
            wells.add(new LuminexWell(new LuminexDataRow("C8", "G8", 1.5, 1, 27932961)));
            wells.add(new LuminexWell(new LuminexDataRow("C8", "H8", 3.5, 1, 27932961)));
            wells.add(new LuminexWell(new LuminexDataRow("C9", "G9", 1.5, 1, 166666667)));
            wells.add(new LuminexWell(new LuminexDataRow("C9", "H9", 2.5, 1, 166666667)));
            wells.add(new LuminexWell(new LuminexDataRow("C10", "G10", 1.5, 1, 1000000000)));
            wells.add(new LuminexWell(new LuminexDataRow("C10", "H10", 1.5, 1, 1000000000)));
            LuminexWellGroup group = new LuminexWellGroup(wells);

            assertEquals("Check number of replicates found", 10, group.getWellData(true).size());
            assertEquals("Check replicate value", 30427.0, group.getWellData(true).get(0).getValue());
            assertEquals("Check number of raw wells", 20, group.getWellData(false).size());

            assertEquals("AUC", 60310.8, Math.round(new LuminexDataHandler().calculateTrapezoidalAUC(group) * 10.0) / 10.0);

            // test calculation using expected concentrations for a standard
            wells = new ArrayList<LuminexWell>();

            wells.add(new LuminexWell(new LuminexDataRow("S1", "A1", 32298.5, 100, 1)));
            wells.add(new LuminexWell(new LuminexDataRow("S1", "B1", 32341.5, 100, 1)));
            wells.add(new LuminexWell(new LuminexDataRow("S2", "A2", 32180.5, 20, 1)));
            wells.add(new LuminexWell(new LuminexDataRow("S2", "B2", 32198.5, 20, 1)));
            wells.add(new LuminexWell(new LuminexDataRow("S3", "A3", 30774.5, 4, 1)));
            wells.add(new LuminexWell(new LuminexDataRow("S3", "B3", 30616.5, 4, 1)));
            wells.add(new LuminexWell(new LuminexDataRow("S4", "A4", 20194.5, 0.8, 1)));
            wells.add(new LuminexWell(new LuminexDataRow("S4", "B4", 20236, 0.8, 1)));
            wells.add(new LuminexWell(new LuminexDataRow("S5", "A5", 5566, 0.16, 1)));
            wells.add(new LuminexWell(new LuminexDataRow("S5", "B5", 5607, 0.16, 1)));
            wells.add(new LuminexWell(new LuminexDataRow("S6", "A6", 1174.5, 0.032, 1)));
            wells.add(new LuminexWell(new LuminexDataRow("S6", "B6", 1233.5, 0.032, 1)));
            wells.add(new LuminexWell(new LuminexDataRow("S7", "A7", 255.5, 0.0064, 1)));
            wells.add(new LuminexWell(new LuminexDataRow("S7", "B7", 285, 0.0064, 1)));
            wells.add(new LuminexWell(new LuminexDataRow("S8", "A8", 60, 0.00128, 1)));
            wells.add(new LuminexWell(new LuminexDataRow("S8", "B8", 61.5, 0.00128, 1)));
            wells.add(new LuminexWell(new LuminexDataRow("S9", "A9", 19.5, 0.00026, 1)));
            wells.add(new LuminexWell(new LuminexDataRow("S9", "B9", 21.5, 0.00026, 1)));
            wells.add(new LuminexWell(new LuminexDataRow("S10", "A10", 10.5, 0.00005, 1)));
            wells.add(new LuminexWell(new LuminexDataRow("S10", "B10", 10.5, 0.00005, 1)));
            group = new LuminexWellGroup(wells);

            assertEquals("Check number of replicates found", 10, group.getWellData(true).size());
            assertEquals("Check replicate value", 10.5, group.getWellData(true).get(0).getValue());
            assertEquals("Check number of raw wells", 20, group.getWellData(false).size());

            assertEquals("AUC", 74375.6, Math.round(new LuminexDataHandler().calculateTrapezoidalAUC(group) * 10.0) / 10.0);
        }

        @Test
        public void testRawStats()
        {
            // Test calculation of stddev and %cv
            List<LuminexDataRow> dataRows = new ArrayList<LuminexDataRow>();

            dataRows.add(new LuminexDataRow("S1", "A1", 30284.5, 500, 1));
            dataRows.add(new LuminexDataRow("S1", "B1", 30596.5, 500, 1));
            dataRows.add(new LuminexDataRow("S2", "A2", 30165, 83.33333, 1));
            dataRows.add(new LuminexDataRow("S2", "B2", 30140, 83.33333, 1));
            dataRows.add(new LuminexDataRow("S3", "A3", 26452.5, 13.88889, 1));
            dataRows.add(new LuminexDataRow("S3", "B3", 26799, 13.88889, 1));

            for (LuminexDataRow dataRow : dataRows)
            {
                assertFalse("Shouldn't be a summary row", dataRow.isSummary());
                assertNull("Shouldn't have %CV", dataRow.getCv());
                assertNull("Shouldn't have StdDev", dataRow.getStdDev());
            }

            // Add a summary row with a fake CV and StdDev to make sure it doesn't mess up our calcs over the raw data
            LuminexDataRow bogusSummaryRow = new LuminexDataRow("S2", "A2,B2", 26625.75, 13.88889, 1);
            bogusSummaryRow.setCv(5000.0);
            bogusSummaryRow.setStdDev(5000.0);
            dataRows.add(bogusSummaryRow);

            // Add a summary row that does match the expected stats for the raw data to make sure it doesn't cause problems either
            LuminexDataRow matchingSummaryRow = new LuminexDataRow("S3", "A3,B3", 26625.75, 13.88889, 1);
            matchingSummaryRow.setCv(0.0092021);
            matchingSummaryRow.setStdDev(245.0125);
            dataRows.add(matchingSummaryRow);

            new LuminexDataHandler().ensureSummaryStats(dataRows);

            assertEquals("Wrong %CV", 0.0072475, Math.round(dataRows.get(0).getCv() * 10000000) / 10000000.0);
            assertEquals("Wrong %CV", 0.0072475, Math.round(dataRows.get(1).getCv() * 10000000) / 10000000.0);
            assertEquals("Wrong %CV", 0.0005863, Math.round(dataRows.get(2).getCv() * 10000000) / 10000000.0);
            assertEquals("Wrong %CV", 0.0005863, Math.round(dataRows.get(3).getCv() * 10000000) / 10000000.0);
            assertEquals("Wrong %CV", 0.0092021, Math.round(dataRows.get(4).getCv() * 10000000) / 10000000.0);
            assertEquals("Wrong %CV", 0.0092021, Math.round(dataRows.get(5).getCv() * 10000000) / 10000000.0);

            assertEquals("Wrong %CV", 5000.0, Math.round(dataRows.get(6).getCv() * 10000000) / 10000000.0);
            assertEquals("Wrong %CV", 0.0092021, Math.round(dataRows.get(7).getCv() * 10000000) / 10000000.0);

            assertEquals("Wrong StdDev", 220.61732, Math.round(dataRows.get(0).getStdDev() * 100000) / 100000.0);
            assertEquals("Wrong StdDev", 220.61732, Math.round(dataRows.get(1).getStdDev() * 100000) / 100000.0);
            assertEquals("Wrong StdDev", 17.67767, Math.round(dataRows.get(2).getStdDev() * 100000) / 100000.0);
            assertEquals("Wrong StdDev", 17.67767, Math.round(dataRows.get(3).getStdDev() * 100000) / 100000.0);
            assertEquals("Wrong StdDev", 245.0125, Math.round(dataRows.get(4).getStdDev() * 100000) / 100000.0);
            assertEquals("Wrong StdDev", 245.0125, Math.round(dataRows.get(5).getStdDev() * 100000) / 100000.0);

            assertEquals("Wrong StdDev", 5000.0, Math.round(dataRows.get(6).getStdDev() * 100000) / 100000.0);
            assertEquals("Wrong StdDev", 245.0125, Math.round(dataRows.get(7).getStdDev() * 100000) / 100000.0);
        }
    }

    private void insertOrUpdateCurveFit(LuminexWellGroup wellGroup, User user, Titration titration, Analyte analyte, ParameterCurveImpl.FitParameters params, DilutionCurve.FitType fitType, String source)
            throws DilutionCurve.FitFailedException, SQLException
    {
        CurveFit fit = createCurveFit(wellGroup, titration, analyte, params, fitType, source);
        insertOrUpdateCurveFit(user, fit);
    }

    private void insertOrUpdateCurveFit(User user, CurveFit fit)
            throws SQLException
    {
        SimpleFilter filter = new SimpleFilter("AnalyteId", fit.getAnalyteId());
        filter.addCondition("TitrationId", fit.getTitrationId());
        filter.addCondition("CurveType", fit.getCurveType());
        CurveFit existingFit = Table.selectObject(LuminexSchema.getTableInfoCurveFit(), filter, null, CurveFit.class);
        if (existingFit != null)
        {
            fit.setRowId(existingFit.getRowId());
            Table.update(user, LuminexSchema.getTableInfoCurveFit(), fit, fit.getRowId());
        }
        else
        {
            Table.insert(user, LuminexSchema.getTableInfoCurveFit(), fit);
        }
    }

    private CurveFit createCurveFit(LuminexWellGroup wellGroup, Titration titration, Analyte analyte, ParameterCurveImpl.FitParameters params, DilutionCurve.FitType fitType, String source)
            throws DilutionCurve.FitFailedException
    {
//        ParameterCurveImpl.FiveParameterCurve curveImpl = new ParameterCurveImpl.FiveParameterCurve(Collections.singletonList(wellGroup), false, params);

        CurveFit fit = new CurveFit();
        fit.setAnalyteId(analyte.getRowId());
        fit.setTitrationId(titration.getRowId());
        fit.setMinAsymptote(params.getMin());
        fit.setMaxAsymptote(params.getMax());
        fit.setInflection(params.getInflection());
        fit.setSlope(params.getSlope());
        if (fitType == DilutionCurve.FitType.FIVE_PARAMETER)
        {
            fit.setAsymmetry(params.getAsymmetry());
        }

        Double ec50 = null;

        if (fitType == DilutionCurve.FitType.FOUR_PARAMETER)
        {
            ec50 = fit.getInflection();
        }

        // Don't calculate AUC for 4/5PL fits
//        double auc = curveImpl.calculateAUC(DilutionCurve.AUCType.NORMAL);

        String fitLabel = fitType.getLabel();
        if (source != null)
            fitLabel = source + " " + fitLabel;

        fit.setEC50(ec50);
        fit.setCurveType(fitLabel);
        return fit;
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

    private void insertExcelProperties(Domain domain, final ExpData data, LuminexExcelParser parser, User user, ExpProtocol protocol) throws SQLException, ValidationException, ExperimentException
    {
        Container container = data.getContainer();
        // Clear out the values - this is necessary if this is a XAR import where the run properties would
        // have been loaded as part of the ExperimentRun itself.
        Integer objectId = OntologyManager.ensureObject(container, data.getLSID());
        DomainProperty[] props = domain.getProperties();
        PropertyDescriptor[] pds = new PropertyDescriptor[props.length];
        for (int i = 0; i < props.length; i++)
        {
            OntologyManager.deleteProperty(data.getLSID(), props[i].getPropertyURI(), container, protocol.getContainer());
            pds[i] = props[i].getPropertyDescriptor();
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
        }, pds, excelRunPropsList, true);
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
        ExpRun run = data.getRun();
        ExpProtocol protocol = run.getProtocol();
        LuminexExcelParser parser = new LuminexExcelParser(protocol, Collections.singleton(dataFile));

        Map<DataType, List<Map<String, Object>>> datas = new HashMap<DataType, List<Map<String, Object>>>();
        List<Map<String, Object>> dataRows = new ArrayList<Map<String, Object>>();

        Set<String> titrations = parser.getTitrations();

        Set<Object> excludedWells = getExcludedWellKeys(run, protocol, info);

        for (Map.Entry<Analyte, List<LuminexDataRow>> entry : parser.getSheets().entrySet())
        {
            for (LuminexDataRow dataRow : entry.getValue())
            {
                Map<String, Object> dataMap = dataRow.toMap(entry.getKey());
                dataMap.put("titration", dataRow.getDescription() != null && titrations.contains(dataRow.getDescription()));
                dataMap.remove("data");
                // Merge in whether it's been excluded or not.
                // We write out all the wells, excluded or not, but the transform script can choose to ignore them
                dataMap.put(LuminexDataTable.FLAGGED_AS_EXCLUDED_COLUMN_NAME, excludedWells.contains(createKey(entry.getKey().getName(), dataRow.getDataFile(), dataRow.getWell())));
                dataRows.add(dataMap);
            }
        }
        datas.put(LUMINEX_TRANSFORMED_DATA_TYPE, dataRows);
        return datas;
    }

    /**
     * Check the database to see if any wells have been excluded so that we can pass the info to the transform script.
     * If the run is in the process of being inserted, nothing's been excluded, but there's no harm in looking. 
     */
    private Set<Object> getExcludedWellKeys(ExpRun run, ExpProtocol protocol, ViewBackgroundInfo info)
    {
        LuminexSchema schema = new LuminexSchema(info.getUser(), info.getContainer(), protocol);
        LuminexDataTable table = new LuminexDataTable(schema);
        AssayProvider provider = AssayService.get().getProvider(protocol);

        Set<Object> excludedWells = new HashSet<Object>();

        try
        {
            // Well, data file, and analyte are sufficient to identify which wells from the Excel file need to be
            // marked as excluded
            FieldKey wellFK = FieldKey.fromParts("Well");
            FieldKey dataNameFK = FieldKey.fromParts("Data", "Name");
            FieldKey analyteFK = FieldKey.fromParts("Analyte", "Name");

            Map<FieldKey, ColumnInfo> cols = QueryService.get().getColumns(table, Arrays.asList(wellFK, dataNameFK, analyteFK));

            SimpleFilter filter = new SimpleFilter(provider.getTableMetadata().getRunFieldKeyFromResults().toString(), run.getRowId());
            filter.addCondition(LuminexDataTable.FLAGGED_AS_EXCLUDED_COLUMN_NAME, true);
            Map<String, Object>[] rows = Table.select(table, new ArrayList<ColumnInfo>(cols.values()), filter, null, Map.class);

            String wellAlias = cols.get(wellFK).getAlias();
            String dataNameAlias = cols.get(dataNameFK).getAlias();
            String analyteNameAlias = cols.get(analyteFK).getAlias();

            for (Map<String, Object> row : rows)
            {
                String well = (String)row.get(wellAlias);
                String dataName = (String)row.get(dataNameAlias);
                String analyteName = (String)row.get(analyteNameAlias);
                excludedWells.add(createKey(analyteName, dataName, well));
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
        return excludedWells;
    }

    /** Create a simple object to use as a key, combining the three properties */
    private Object createKey(String analyteName, String dataFileName, String well)
    {
        return Arrays.asList(analyteName, dataFileName, well);
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

        LuminexRunContext form = (LuminexRunContext)context;
        importData(data, run, context.getUser(), Logger.getLogger(LuminexDataHandler.class), sheets, form.getParser(), form);
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
