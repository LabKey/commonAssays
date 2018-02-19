/*
 * Copyright (c) 2009-2017 LabKey Corporation
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

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.BeanObjectFactory;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.ObjectFactory;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.data.statistics.FitFailedException;
import org.labkey.api.data.statistics.MathStat;
import org.labkey.api.data.statistics.StatsService;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.AbstractExperimentDataHandler;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.query.ExpQCFlagTable;
import org.labkey.api.qc.DataLoaderSettings;
import org.labkey.api.qc.TransformDataHandler;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.services.ServiceRegistry;
import org.labkey.api.study.ParticipantVisit;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AssayDataType;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayRunUploadContext;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.AssayUploadXarContext;
import org.labkey.api.study.assay.AssayUrls;
import org.labkey.api.study.assay.ParticipantVisitResolver;
import org.labkey.api.study.assay.StudyParticipantVisitResolverType;
import org.labkey.api.util.FileType;
import org.labkey.api.util.GUID;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.luminex.model.AbstractLuminexControl;
import org.labkey.luminex.model.Analyte;
import org.labkey.luminex.model.AnalyteSinglePointControl;
import org.labkey.luminex.model.AnalyteSinglePointControlQCFlag;
import org.labkey.luminex.model.AnalyteTitration;
import org.labkey.luminex.model.AnalyteTitrationQCFlag;
import org.labkey.luminex.model.CVQCFlag;
import org.labkey.luminex.model.CurveFit;
import org.labkey.luminex.model.GuideSet;
import org.labkey.luminex.model.LuminexDataRow;
import org.labkey.luminex.model.LuminexWell;
import org.labkey.luminex.model.LuminexWellGroup;
import org.labkey.luminex.model.SinglePointControl;
import org.labkey.luminex.model.Titration;
import org.labkey.luminex.query.GuideSetTable;
import org.labkey.luminex.query.LuminexDataTable;
import org.labkey.luminex.query.LuminexProtocolSchema;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Import data from file, handles participant/visit resolver, insert/update data rows (titrations, analytes, QC flags,
 * guide sets, curve fits), handles deletions of data on run/container delete.
 * We've made the simplifying assumption that in the case of updating an existing run, we don't have to worry about
 * deleting any data (no data rows, analytes, etc have disappeared completely).
 * User: klum
 * Date: May 14, 2009
 */
public class LuminexDataHandler extends AbstractExperimentDataHandler implements TransformDataHandler
{
    public static final String NAMESPACE = "LuminexDataFile";
    private static final DataType LUMINEX_TRANSFORMED_DATA_TYPE = new DataType("LuminexTransformedDataFile");  // marker data type
    private static final AssayDataType LUMINEX_DATA_TYPE = new AssayDataType(NAMESPACE, new FileType(Arrays.asList(".xls", ".xlsx"), ".xls"));
    public static final String QC_FLAG_SINGLE_POINT_CONTROL_FI_FLAG_TYPE = "CTRL";
    public static final String QC_FLAG_HIGH_MFI_FLAG_TYPE = "HMFI";
    public static final String QC_FLAG_EC50_4PL_FLAG_TYPE = "EC50-4";
    public static final String QC_FLAG_EC50_5PL_FLAG_TYPE = "EC50-5";
    public static final String QC_FLAG_AUC_FLAG_TYPE = "AUC";
    public static final String QC_FLAG_CV_FLAG_TYPE = "PCV";
    public static final String QC_FLAG_TITRATION_ID = "TitrationId";  // Column name to use in createQCFlagEnabledSQLFragment
    public static final String QC_FLAG_SINGLE_POINT_CONTROL_ID = "SinglePointControlId";   // Column name to use in createQCFlagEnabledSQLFragment
    public static final String POSITIVITY_THRESHOLD_COLUMN_NAME = "PositivityThreshold";
    public static final String POSITIVITY_THRESHOLD_DISPLAY_NAME = "Positivity Threshold";
    public static final String CALCULATE_POSITIVITY_COLUMN_NAME = "calculatePositivity";
    public static final String NEGATIVE_CONTROL_COLUMN_NAME = "NegativeControl";
    public static final String NEGATIVE_BEAD_COLUMN_NAME = "NegativeBead";
    public static final String NEGATIVE_BEAD_DISPLAY_NAME = "Subtract Negative Bead";
    public static final Double MAX_CURVE_PARAM_VALUE = 10e37; // Issue 15200

    private static final Logger LOGGER = Logger.getLogger(LuminexDataHandler.class);

    public static final int MINIMUM_TITRATION_SUMMARY_COUNT = 5;
    public static final int MINIMUM_TITRATION_RAW_COUNT = 10;

    public static final int SINGLE_POINT_CONTROL_SUMMARY_COUNT = 1;
    public static final int SINGLE_POINT_CONTROL_RAW_COUNT = 2;
    private static final int DELETE_BATCH_SIZE = 200;

    @Override
    public DataType getDataType()
    {
        return LUMINEX_DATA_TYPE;
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
            if (form == null)
            {
                throw new ExperimentException("Importing a Luminex run from a XAR file is not supported");
            }
            importData(data, expRun, info.getUser(), log, parser.getSheets(), parser, form, true);
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
                    int minRecovery = analyte.getMinStandardRecovery();
                    int maxRecovery = analyte.getMaxStandardRecovery();
                    if (minRecovery == 0 && maxRecovery == 0)
                    {
                        // Use reasonable default values if the data wasn't in the Excel file 
                        minRecovery = 70;
                        maxRecovery = 130;
                    }
                    if (obsOverExp >= minRecovery && obsOverExp <= maxRecovery)
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
    private void importData(ExpData data, ExpRun expRun, User user, @NotNull Logger log, Map<Analyte, List<LuminexDataRow>> sheets, LuminexExcelParser parser, LuminexRunContext form, boolean parseDescription) throws ExperimentException
    {
        try (DbScope.Transaction transaction = ExperimentService.get().ensureTransaction())
        {
            ExpProtocol protocol = form.getProtocol();
            String dataFileName = data.getFile().getName();
            LuminexAssayProvider provider = form.getProvider();
            Map<ExpMaterial, String> inputMaterials = new LinkedHashMap<>();
            ParticipantVisitResolver resolver = findParticipantVisitResolver(expRun, user, provider);

            Domain excelRunDomain = LuminexAssayProvider.getExcelRunDomain(protocol);
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
            // and look for curve fit input properties for AUC calculation
            String isotype = null;
            String conjugate = null;
            String stndCurveFitInput = null;
            String unkCurveFitInput = null;
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
                else if (runProp.getName().equalsIgnoreCase("StndCurveFitInput"))
                {
                    Object value = expRun.getProperty(runProp);
                    stndCurveFitInput = value == null ? null : value.toString();
                }
                else if (runProp.getName().equalsIgnoreCase("UnkCurveFitInput"))
                {
                    Object value = expRun.getProperty(runProp);
                    unkCurveFitInput = value == null ? null : value.toString();
                }
            }
            
            // Name -> Titration
            Map<String, Titration> titrations = insertTitrations(expRun, user, form.getTitrations());

            // Name -> SinglePointControl
            Map<String, SinglePointControl> singlePointControls = insertSinglePointControls(expRun, user, form.getSinglePointControls());

            // Keep these in a map so that we can easily look them up against the rows that are already in the database
            Map<DataRowKey, Map<String, Object>> rows = new LinkedHashMap<>();
            Set<ExpData> sourceFiles = new HashSet<>();

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

                for (LuminexDataRow dataRow : dataRows)
                {
                    // Iterate through all of the data rows, figuring out what data file they
                    // originally came from. Additionally, set that as the analyte's source file.
                    // This way they don't end up pointing at the transform script result file instead.
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
                    analyte.setDataId(sourceDataForRow.getRowId());
                }

                Map<ColumnInfo, String> colProperties = form.getAnalyteColumnProperties(analyte.getName());
                for (ColumnInfo col : colProperties.keySet())
                {
                    if (col.getName().equals(POSITIVITY_THRESHOLD_COLUMN_NAME))
                    {
                        analyte.setPositivityThreshold(colProperties.get(col) != null ? Integer.parseInt(colProperties.get(col)) : null);
                    }
                    else if (col.getName().equals(NEGATIVE_BEAD_COLUMN_NAME))
                    {
                        analyte.setNegativeBead(colProperties.get(col));
                    }
                }

                analyte = saveAnalyte(expRun, user, existingAnalytes, analyte);

                performOOR(dataRows, analyte);

                for (LuminexDataRow dataRow : dataRows)
                {
                    handleParticipantResolver(dataRow, resolver, inputMaterials, parseDescription);
                    dataRow.setProtocol(protocol.getRowId());
                    dataRow.setContainer(expRun.getContainer());
                    Titration titration = titrations.get(dataRow.getDescription());
                    if (titration != null)
                    {
                        dataRow.setTitration(titration.getRowId());
                        List<String> roles = new ArrayList<>();
                        if (titration.isStandard())
                        {
                            roles.add("Standard");
                        }
                        if (titration.isQcControl())
                        {
                            roles.add("QC Control");
                        }
                        if (titration.isOtherControl())
                        {
                            roles.add("Other Control");
                        }
                        if (!roles.isEmpty())
                        {
                            dataRow.setWellRole(StringUtils.join(roles, ", "));
                        }
                    }
                    dataRow.setAnalyte(analyte.getRowId());

                    SinglePointControl singlePointControl = singlePointControls.get(dataRow.getDescription());
                    if (singlePointControl != null)
                        dataRow.setSinglePointControl(singlePointControl.getRowId());
                }

                insertTitrationAnalyteMappings(user, form, expRun, titrations, sheet.getValue(), analyte, conjugate, isotype, stndCurveFitInput, unkCurveFitInput, protocol);
                insertSinglePointControlAnalyteMappings(user, form, expRun, singlePointControls, sheet.getValue(), analyte, conjugate, isotype, protocol);

                // Now that we've made sure each data row to the appropriate data file, make sure that we
                // have %CV and StdDev. It's important to wait so that we know the scope in which to do the aggregate
                // calculations - we only want to look for replicates within the same plate.
                ensureSummaryStats(dataRows);

                // check if QC Flags need to be inserted for any of the %CV values for this analyte (only on initial insert of run data)
                if (!existingAnalytes.containsKey(analyte.getName()))
                    insertCVQCFlags(user, expRun, dataRows, analyte);

                // now add the dataRows to the rows list to be persisted
                for (LuminexDataRow dataRow : dataRows)
                    rows.put(new DataRowKey(dataRow), dataRow.toMap(analyte));
            }

            List<Integer> dataIds = new ArrayList<>();
            for (ExpData sourceFile : sourceFiles)
            {
                insertExcelProperties(excelRunDomain, sourceFile, parser, user, protocol);
                dataIds.add(sourceFile.getRowId());
            }

            saveDataRows(expRun, user, protocol, rows, dataIds);

            if (inputMaterials.isEmpty())
            {
                throw new ExperimentException("Could not find any input samples in the data");
            }

            AbstractAssayProvider.addInputMaterials(expRun, user, inputMaterials);

            transaction.commit();
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
    }

    /** Saves the data rows, updating if they already exist, inserting if not */
    private void saveDataRows(ExpRun expRun, User user, ExpProtocol protocol, Map<DataRowKey, Map<String, Object>> rows, List<Integer> dataIds)
            throws SQLException, ValidationException
    {
        // Do a query to find all of the rows that have already been inserted 
        LuminexDataTable tableInfo = ((LuminexProtocolSchema)AssayService.get().getProvider(protocol).createProtocolSchema(user, expRun.getContainer(), protocol, null)).createDataTable(false);
        SimpleFilter filter = new SimpleFilter(new SimpleFilter.InClause(FieldKey.fromParts("Data"), dataIds));

        Map<DataRowKey, LuminexDataRow> existingRows = new HashMap<>();
        // Pull back as a map so that we get custom properties as well
        for (Map<String, Object> databaseMap : new TableSelector(tableInfo, filter, null).getMapCollection())
        {
            LuminexDataRow existingRow = BeanObjectFactory.Registry.getFactory(LuminexDataRow.class).fromMap(databaseMap);
            // Make sure an extra properties are made available
            existingRow._setExtraProperties(new CaseInsensitiveHashMap<>(databaseMap));
            existingRows.put(new DataRowKey(existingRow), existingRow);
        }

        List<Map<String, Object>> insertRows = new ArrayList<>();
        List<Map<String, Object>> updateRows = new ArrayList<>();

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
    {
        Analyte existingAnalyte = existingAnalytes.get(analyte.getName());
        if (existingAnalyte != null)
        {
            // Need the original rowId so that we update the existing row
            analyte.setRowId(existingAnalyte.getRowId());
            // Retain the LSID so we don't lose the custom property values
            analyte.setLsid(existingAnalyte.getLsid());
            analyte = Table.update(user, LuminexProtocolSchema.getTableInfoAnalytes(), analyte, analyte.getRowId());
        }
        else
        {
            analyte.setLsid(new Lsid("LuminexAnalyte", "Data-" + expRun.getRowId() + "." + analyte.getName()).toString());
            analyte = Table.insert(user, LuminexProtocolSchema.getTableInfoAnalytes(), analyte);
        }
        return analyte;
    }

    /** @return Name->Analyte for all of the analytes that are already associated with this run */
    private Map<String, Analyte> getExistingAnalytes(ExpRun expRun)
    {
        SQLFragment sql = new SQLFragment("SELECT a.* FROM ");
        sql.append(LuminexProtocolSchema.getTableInfoAnalytes(), "a");
        sql.append(", ");
        sql.append(ExperimentService.get().getTinfoData(), "d");
        sql.append(" WHERE a.DataId = d.RowId AND d.RunId = ?");
        sql.add(expRun.getRowId());
        Analyte[] databaseAnalytes = new SqlSelector(LuminexProtocolSchema.getSchema(), sql).getArray(Analyte.class);
        Map<String, Analyte> existingAnalytes = new HashMap<>();
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
        private String _type;
        private Object _standard;

        public DataRowKey(LuminexDataRow dataRow)
        {
            _dataId = dataRow.getData();
            _analyteId = dataRow.getAnalyte();
            _well = dataRow.getWell();
            _type = dataRow.getType();
            _standard = dataRow._getExtraProperties().get("Standard");
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DataRowKey that = (DataRowKey) o;

            if (_analyteId != that._analyteId) return false;
            if (_dataId != that._dataId) return false;
            if (_standard != null ? !_standard.equals(that._standard) : that._standard != null) return false;
            if (_well != null ? !_well.equals(that._well) : that._well != null) return false;
            if (_type != null ? !_type.equals(that._type) : that._type != null) return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = _dataId;
            result = 31 * result + _analyteId;
            result = 31 * result + (_well != null ? _well.hashCode() : 0);
            result = 31 * result + (_type != null ? _type.hashCode() : 0);
            result = 31 * result + (_standard != null ? _standard.hashCode() : 0);
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
                List<Double> fis = new ArrayList<>();
                for (LuminexDataRow statRow : dataRows)
                {
                    // Only look for replicates within the same data file (plate)
                    if (statRow.getFi() != null && !statRow.isSummary() && !statRow.isExcluded() &&
                        Objects.equals(statRow.getDilution(), dataRow.getDilution()) &&
                        Objects.equals(statRow.getExpConc(), dataRow.getExpConc()) &&
                        Objects.equals(statRow.getDescription(), dataRow.getDescription()) &&
                        Objects.equals(statRow.getType(), dataRow.getType()) &&
                        Objects.equals(statRow.getData(), dataRow.getData()) &&
                        Objects.equals(statRow.getAnalyte(), dataRow.getAnalyte()) &&
                        Objects.equals(statRow._getExtraProperties().get("Standard"), dataRow._getExtraProperties().get("Standard")))
                    {
                        fis.add(statRow.getFi());
                    }
                }

                if (fis.size() > 1)
                {
                    StatsService service = ServiceRegistry.get().getService(StatsService.class);
                    MathStat stats = service.getStats(ArrayUtils.toPrimitive(fis.toArray(new Double[fis.size()])));
                    double stdDev = stats.getStdDev();
                    double mean = Math.abs(stats.getMean());
                    dataRow.setStdDev(stdDev);
                    dataRow.setCv(mean == 0.0 ? null : stdDev / mean);
                }
            }
        }
    }

    /* Insert QC Flags for %CV values that are over the threshold (Unknown > 20%, Standard/Control > 15%) */
    private void insertCVQCFlags(User user, ExpRun expRun, List<LuminexDataRow> dataRows, Analyte analyte)
    {
        LuminexWellGroup wellGroup = analyte.buildWellGroup(dataRows);
        List<LuminexWell> allReplicates = wellGroup.getWellData(true); // combine replicates and get mean MFI and %CV

        Set<CVQCFlag> newCVQCFlags = new HashSet<>();
        for (LuminexWell replicate : allReplicates)
        {
            LuminexDataRow dataRow = replicate.getDataRow();

            // only need to check %CV thresholds for potentially positive samples (i.e. mean MFI of replicates > 100)
            if (null != dataRow.getCv() && dataRow.getFi() > 100.0 && 
                ((dataRow.getWellRole().equalsIgnoreCase("Unknown") && dataRow.getCv() > 0.15) ||
                 (dataRow.getWellRole().toLowerCase().contains("standard") && dataRow.getCv() > 0.2) ||
                 (dataRow.getWellRole().toLowerCase().contains("control") && dataRow.getCv() > 0.2)))
            {
                String description = dataRow.getType() + " : " + dataRow.getDescription() + " with " + analyte.getName() + " over threshold value for %CV";
                CVQCFlag newQcFlag = new CVQCFlag(expRun.getRowId(), QC_FLAG_CV_FLAG_TYPE, description, analyte.getRowId(), dataRow.getData(), dataRow.getType(), dataRow.getDescription());
                if (!newCVQCFlags.contains(newQcFlag))
                {
                    newCVQCFlags.add(newQcFlag);
                    Table.insert(user, ExperimentService.get().getTinfoAssayQCFlag(), newQcFlag);
                }
            }
        }
    }

    private GuideSet determineGuideSet(Analyte analyte, AbstractLuminexControl control, String conjugate, String isotype, ExpProtocol protocol, Boolean isTitration)
    {
        GuideSet guideSet = GuideSetTable.GuideSetTableUpdateService.getMatchingCurrentGuideSet(protocol, analyte.getName(), control.getName(), conjugate, isotype, isTitration);
        if (guideSet != null)
        {
            return guideSet;
        }

        // Should we create a new one automatically here?
        return null;
    }

    private void insertTitrationAnalyteMappings(User user, LuminexRunContext form, ExpRun expRun, Map<String, Titration> titrations, List<LuminexDataRow> dataRows, Analyte analyte, String conjugate, String isotype, String stndCurveFitInput, String unkCurveFitInput, ExpProtocol protocol)
            throws ExperimentException
    {
        // Insert mappings for all of the titrations that aren't standards
        for (Titration titration : titrations.values())
        {
            if (!titration.isStandard() && (titration.isQcControl() || titration.isUnknown() || titration.isOtherControl()))
            {
                String curveFitInput = titration.isUnknown() ? unkCurveFitInput : stndCurveFitInput;
                insertAnalyteTitrationMapping(user, expRun, dataRows, analyte, titration, conjugate, isotype, curveFitInput, protocol);
            }
        }

        // Insert mappings for all of the standard titrations that have been selected for this analyte
        for (String titrationName : form.getTitrationsForAnalyte(analyte.getName()))
        {
            Titration titration = titrations.get(titrationName);
            if (titration != null)
            {
                insertAnalyteTitrationMapping(user, expRun, dataRows, analyte, titration, conjugate, isotype, stndCurveFitInput, protocol);
            }
        }
    }

    private void insertSinglePointControlAnalyteMappings(User user, LuminexRunContext form, ExpRun expRun, Map<String, SinglePointControl> singlePointControls, List<LuminexDataRow> dataRows, Analyte analyte, String conjugate, String isotype, ExpProtocol protocol)
            throws ExperimentException
    {
        // Insert mappings for all of the controls
        for (SinglePointControl singlePointControl : singlePointControls.values())
        {
            insertAnalyteSinglePointControlMapping(user, expRun, dataRows, analyte, singlePointControl, conjugate, isotype, protocol);
        }
    }

    private void insertAnalyteSinglePointControlMapping(User user, ExpRun expRun, List<LuminexDataRow> dataRows, Analyte analyte, SinglePointControl singlePointControl, String conjugate, String isotype, ExpProtocol protocol)
    {
        // Calculate the average FI value
        double sum = 0;
        int count = 0;
        for (LuminexDataRow dataRow : dataRows)
        {
            // Include in the sum for all non-null values (might be OOR) for the right analyte/single point control. See issue 21430
            if (dataRow.getAnalyte() == analyte.getRowId() && singlePointControl.getName().equals(dataRow.getDescription()) && dataRow.getFiBackground() != null)
            {
                sum += dataRow.getFiBackground();
                count++;
            }
        }

        // It's possible that we don't have any data rows for this particular single point control/analyte combination
        if (count > 0)
        {
            SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("AnalyteId"), analyte.getRowId());
            filter.addCondition(FieldKey.fromParts("SinglePointControlId"), singlePointControl.getRowId());

            AnalyteSinglePointControl analyteSinglePointControl = new TableSelector(LuminexProtocolSchema.getTableInfoAnalyteSinglePointControl(), filter, null).getObject(AnalyteSinglePointControl.class);

            boolean newRow = analyteSinglePointControl == null;
            if (analyteSinglePointControl == null)
            {
                analyteSinglePointControl = new AnalyteSinglePointControl(analyte, singlePointControl);
            }

            if (newRow)
            {
                // Check if we have a guide set for this combo
                GuideSet currentGuideSet = determineGuideSet(analyte, singlePointControl, conjugate, isotype, protocol, false);
                if (currentGuideSet != null)
                {
                    analyteSinglePointControl.setGuideSetId(currentGuideSet.getRowId());
                }

                Table.insert(user, LuminexProtocolSchema.getTableInfoAnalyteSinglePointControl(), analyteSinglePointControl);
            }
            else
            {
                Map<String, Object> keys = new CaseInsensitiveHashMap<>();
                keys.put("AnalyteId", analyte.getRowId());
                keys.put("SinglePointControlId", singlePointControl.getRowId());
                Table.update(user, LuminexProtocolSchema.getTableInfoAnalyteSinglePointControl(), analyteSinglePointControl, keys);
            }

            insertOrUpdateAnalyteSinglePointControlQCFlags(user, expRun, protocol, analyteSinglePointControl, analyte, singlePointControl, isotype, conjugate, sum / count);
        }
    }

    /** Insert or Update QC Flags for MFI that are out of the guide set range if this AnalyteSinglePointControl record
     * has a current GuideSet */
    public static void insertOrUpdateAnalyteSinglePointControlQCFlags(User user, ExpRun expRun, ExpProtocol protocol, AnalyteSinglePointControl analyteSinglePointControl, Analyte analyte, SinglePointControl singlePointControl, String isotype, String conjugate, double averageFI)
    {
        AssayProvider provider = AssayService.get().getProvider(protocol);
        if (!(provider instanceof LuminexAssayProvider))
            throw new NotFoundException("Luminex assay provider not found");

        LuminexProtocolSchema schema = new LuminexProtocolSchema(user, expRun.getContainer(), (LuminexAssayProvider)provider, protocol, null);

        // query the QC Flags table to get any existing analyte/titration QC Flags
        ExpQCFlagTable qcFlagTable = schema.createAnalyteSinglePointControlQCFlagTable();
        SimpleFilter analyteSinglePointControlFilter = new SimpleFilter(FieldKey.fromParts("Analyte"), analyte.getRowId());
        analyteSinglePointControlFilter.addCondition(FieldKey.fromParts("SinglePointControl"), singlePointControl.getRowId());
        List<AnalyteSinglePointControlQCFlag> existingQCFlags = new TableSelector(qcFlagTable, analyteSinglePointControlFilter, null).getArrayList(AnalyteSinglePointControlQCFlag.class);

        List<AnalyteSinglePointControlQCFlag> newQCFlags = new ArrayList<>();

        if (null != analyteSinglePointControl.getGuideSetId())
        {
            GuideSetTable guideSetTable = schema.createGuideSetTable(false);
            GuideSet guideSetRow = new TableSelector(guideSetTable).getObject(analyteSinglePointControl.getGuideSetId(), GuideSet.class);

            if (guideSetRow == null)
            {
                throw new IllegalStateException("Unable to find referenced guide set: " + analyteSinglePointControl.getGuideSetId());
            }
            else
            {
                // add QCFlag for MFI, if out of guide set range
                String outOfRangeType = guideSetRow.getOutOfRangeTypeForMaxFI(averageFI, guideSetTable, "SinglePointControlFIAverage", "SinglePointControlFIStdDev");
                if (null != outOfRangeType && guideSetRow.isMaxFIEnabled())
                {
                    String descriptionPrefix = singlePointControl.getName() + " " + analyte.getName() + " - "
                            + (isotype == null ? "[None]" : isotype) + " "
                            + (conjugate == null ? "[None]" : conjugate) + " ";

                    newQCFlags.add(new AnalyteSinglePointControlQCFlag(expRun.getRowId(), descriptionPrefix + outOfRangeType + " threshold for MFI", analyte.getRowId(), singlePointControl.getRowId()));
                }
            }
        }

        // insert new flags if a matching QC Flag does not exist (based on runId, flagType, desription, analyteId, and titrationId)
        for (AnalyteSinglePointControlQCFlag newQCFlag : newQCFlags)
        {
            if (!existingQCFlags.contains(newQCFlag))
            {
                Table.insert(user, ExperimentService.get().getTinfoAssayQCFlag(), newQCFlag);
            }
        }

        // remove existing flags that are no longer relevant based on rerunning the transform script with well exclusions
        for (AnalyteSinglePointControlQCFlag existingAnalyteTitrationQCFlag : existingQCFlags)
        {
            if (!newQCFlags.contains(existingAnalyteTitrationQCFlag))
            {
                Table.delete(ExperimentService.get().getTinfoAssayQCFlag(), existingAnalyteTitrationQCFlag.getRowId());
            }
        }
    }

    private static Double getGuideSetRangeValue(boolean isValueBased, Object runBasedVal, Object valueBasedVal)
    {
        if (isValueBased)
            return parseDouble(valueBasedVal != null ? valueBasedVal.toString() : null);
        else
            return parseDouble(runBasedVal != null ? runBasedVal.toString() : null);
    }

    private void insertAnalyteTitrationMapping(User user, ExpRun expRun, List<LuminexDataRow> dataRows, Analyte analyte, Titration titration, String conjugate, String isotype, String curveFitInput, ExpProtocol protocol)
            throws ExperimentException
    {
        LuminexWellGroup wellGroup = titration.buildWellGroup(dataRows);

        // Insert the mapping row, which includes the Max FI
        SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("AnalyteId"), analyte.getRowId());
        filter.addCondition(FieldKey.fromParts("TitrationId"), titration.getRowId());

        AnalyteTitration analyteTitration = new TableSelector(LuminexProtocolSchema.getTableInfoAnalyteTitration(), filter, null).getObject(AnalyteTitration.class);
        boolean newRow = analyteTitration == null;
        if (analyteTitration == null)
        {
            analyteTitration = new AnalyteTitration();
            analyteTitration.setAnalyteId(analyte.getRowId());
            analyteTitration.setTitrationId(titration.getRowId());
        }

        double maxFI = wellGroup.getMax();
        analyteTitration.setMaxFI(maxFI == Double.NEGATIVE_INFINITY ? null : maxFI);

        if (newRow)
        {
            // Check if we have a guide set for this combo
            GuideSet currentGuideSet = determineGuideSet(analyte, titration, conjugate, isotype, protocol, true);
            if (currentGuideSet != null)
            {
                analyteTitration.setGuideSetId(currentGuideSet.getRowId());
            }

            Table.insert(user, LuminexProtocolSchema.getTableInfoAnalyteTitration(), analyteTitration);
        }
        else
        {
            Map<String, Object> keys = new CaseInsensitiveHashMap<>();
            keys.put("AnalyteId", analyte.getRowId());
            keys.put("TitrationId", titration.getRowId());
            Table.update(user, LuminexProtocolSchema.getTableInfoAnalyteTitration(), analyteTitration, keys);
        }

        // Insert the curve fit values (EC50 and AUC)
        try
        {
            // Look up any existing curve fits that might already be in the database
            SimpleFilter curveFitFilter = new SimpleFilter(FieldKey.fromParts("AnalyteId"), analyte.getRowId());
            curveFitFilter.addCondition(FieldKey.fromParts("TitrationId"), titration.getRowId());
            CurveFit[] existingCurveFits = new TableSelector(LuminexProtocolSchema.getTableInfoCurveFit(), filter, null).getArray(CurveFit.class);

            // Keep track of the curve fits that should be part of this run
            List<CurveFit> newCurveFits = new ArrayList<>();

            // TODO this seems to be dependent on which data file the analyte record was associated with
            String stdCurve = analyte.getStdCurve();
            if (stdCurve != null)
            {
                FitParameters fitParams = parseBioPlexStdCurve(stdCurve);
                if (fitParams != null)
                {
                    CurveFit fit = insertOrUpdateCurveFit(wellGroup, user, titration, analyte, fitParams, null, null, StatsService.CurveFitType.FIVE_PARAMETER, "BioPlex", existingCurveFits);
                    if (fit != null)
                    {
                        newCurveFits.add(fit);
                    }
                }
                else
                {
                    LOGGER.warn("Could not parse standard curve: " + stdCurve);
                }
            }

            if (!wellGroup.getWellData(false).isEmpty())
            {
                LuminexDataRow firstDataRow = wellGroup.getWellData(false).get(0)._dataRow;
                CurveFit rumi5PLFit = importRumiCurveFit(StatsService.CurveFitType.FIVE_PARAMETER, firstDataRow, wellGroup, user, titration, analyte, existingCurveFits);
                if (rumi5PLFit != null)
                {
                    newCurveFits.add(rumi5PLFit);
                }
                CurveFit rumi4PLFit = importRumiCurveFit(StatsService.CurveFitType.FOUR_PARAMETER, firstDataRow, wellGroup, user, titration, analyte, existingCurveFits);
                if (rumi4PLFit != null)
                {
                    newCurveFits.add(rumi4PLFit);
                }

                // Do the trapezoidal AUC calculation
                Double auc = calculateTrapezoidalAUC(wellGroup, curveFitInput);

                // issue 15042
                if (auc != null && auc.isNaN())
                    throw new ExperimentException("Error: unable to calculate Trapezoidal AUC for " + titration.getName() + " " + analyte.getName());

                CurveFit fit = new CurveFit();
                fit.setAUC(auc);
                fit.setAnalyteId(analyte.getRowId());
                fit.setTitrationId(titration.getRowId());
                fit.setCurveType("Trapezoidal");

                fit = insertOrUpdateCurveFit(user, fit, existingCurveFits);
                newCurveFits.add(fit);
            }

            // Look through the original set of curve fits. If there are any that don't have an updated curve fit,
            // delete them.
            for (CurveFit existingCurveFit : existingCurveFits)
            {
                CurveFit newFit = findMatch(existingCurveFit, newCurveFits);
                if (newFit == null)
                {
                    Table.delete(LuminexProtocolSchema.getTableInfoCurveFit(), existingCurveFit.getRowId());
                }
            }

            if (titration.isStandard() || titration.isQcControl())
                insertOrUpdateAnalyteTitrationQCFlags(user, expRun, protocol, analyteTitration, analyte, titration, isotype, conjugate, newCurveFits);
        }
        catch (FitFailedException e)
        {
            throw new ExperimentException(e);
        }
    }

    /** Walks the newCurveFits list to find fit for the same analyte/titration/curve type combination */
    private CurveFit findMatch(CurveFit existingCurveFit, List<CurveFit> newCurveFits)
    {
        for (CurveFit newCurveFit : newCurveFits)
        {
            if (existingCurveFit.getAnalyteId() == newCurveFit.getAnalyteId() &&
                existingCurveFit.getTitrationId() == newCurveFit.getTitrationId() &&
                existingCurveFit.getCurveType().equals(newCurveFit.getCurveType()))
            {
                return newCurveFit;
            }
        }
        return null;
    }

    private Double calculateTrapezoidalAUC(LuminexWellGroup wellGroup, String curveFitInput) throws ExperimentException
    {
        double auc = 0;
        List<LuminexWell> wells = wellGroup.getWellData(true);

        // Strip out any incomplete data points that will mess up the AUC calculations
        // along with any excluded wells
        Iterator<LuminexWell> it = wells.iterator();
        while (it.hasNext())
        {
            LuminexWell well = it.next();
            if (well.getDose() == null || well.getAucValue(curveFitInput) == null || well.getDataRow().isExcluded())
            {
                it.remove();
            }
            else if (well.getDose() == 0.0)
            {
                // issue 15042
                throw new ExperimentException("Zero values not allowed in dose (i.e. ExpConc/Dilution) for Trapezoidal AUC calculation: "
                    + well.getDataRow().getDescription() + " (" + well.getDataRow().getType() + ").");
            }
        }

        if (!wells.isEmpty())
        {
            LuminexWell previousWell = wells.get(0);
            for (int i = 1; i < wells.size(); i++)
            {
                LuminexWell well = wells.get(i);
                auc += Math.max(Math.abs(Math.log10(well.getDose()) - Math.log10(previousWell.getDose())) *
                        (Math.min(previousWell.getAucValue(curveFitInput).doubleValue(), well.getAucValue(curveFitInput).doubleValue()) +
                            0.5 * Math.abs(previousWell.getAucValue(curveFitInput).doubleValue() - well.getAucValue(curveFitInput).doubleValue())), 0);
                previousWell = well;
            }
            return auc;
        }
        else
            return null;
    }

    /** @return null if we can't find matching Rumi curve fit data */
    @Nullable
    private CurveFit importRumiCurveFit(StatsService.CurveFitType fitType, LuminexDataRow dataRow, LuminexWellGroup wellGroup, User user, Titration titration, Analyte analyte, CurveFit[] existingCurveFits) throws FitFailedException
    {
        if (fitType != StatsService.CurveFitType.FIVE_PARAMETER && fitType != StatsService.CurveFitType.FOUR_PARAMETER)
        {
            throw new IllegalArgumentException("Unsupported fit type: " + fitType);
        }
        String suffix = fitType == StatsService.CurveFitType.FIVE_PARAMETER ? "_5pl" : "_4pl";
        Object value = dataRow._getExtraProperties().get("Slope" + suffix);
        Number slope = (value == null ? (Number)value : Double.parseDouble(value.toString()));
        value = dataRow._getExtraProperties().get("Upper" + suffix);
        Number upper = (value == null ? (Number)value : Double.parseDouble(value.toString()));
        value = dataRow._getExtraProperties().get("Lower" + suffix);
        Number lower = (value == null ? (Number)value : Double.parseDouble(value.toString()));
        value = dataRow._getExtraProperties().get("Inflection" + suffix);
        Number inflection = (value == null ? (Number)value : Double.parseDouble(value.toString()));
        value = dataRow._getExtraProperties().get("Asymmetry" + suffix);
        Number asymmetry = (value == null ? (Number)value : Double.parseDouble(value.toString()));
        value = dataRow._getExtraProperties().get("EC50" + suffix);
        Double ec50 = (value != null ? Double.parseDouble(value.toString()) : null);
        value = dataRow._getExtraProperties().get("Flag" + suffix);
        Boolean flag = (value != null ? Boolean.parseBoolean(value.toString()) : null);

        if ((slope != null && upper != null && lower != null && inflection != null && ec50 != null) || flag != null)
        {
            FitParameters params = new FitParameters();
            if (flag == null)
            {
                params.min = lower.doubleValue();
                params.slope = slope.doubleValue();
                params.inflection = inflection.doubleValue();
                params.max = upper.doubleValue();
                params.asymmetry = asymmetry == null ? 1 : asymmetry.doubleValue();
            }

            return insertOrUpdateCurveFit(wellGroup, user, titration, analyte, params, ec50, flag, fitType, null, existingCurveFits);
        }
        return null;
    }

    private FitParameters parseBioPlexStdCurve(String stdCurve)
    {
        Matcher matcher = CURVE_PATTERN.matcher(stdCurve);
        if (matcher.matches())
        {
            FitParameters params = new FitParameters();
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
        public static final double DELTA = 10E-10;

        @Test
        public void testBioPlexCurveParsingNegativeMinimum()
        {
            FitParameters params = new LuminexDataHandler().parseBioPlexStdCurve("FI = -2.08995 + (29934.1 + 2.08995) / ((1 + (Conc / 2.49287)^-4.99651))^0.215266");
            assertNotNull("Couldn't parse standard curve", params);
            assertEquals(params.asymmetry, 0.215266, DELTA);
            assertEquals(params.min, -2.08995, DELTA);
            assertEquals(params.max, 29936.18995, DELTA);
            assertEquals(params.inflection, 2.49287, DELTA);
            assertEquals(params.slope, -4.99651, DELTA);
        }

        @Test
        public void testBioPlexCurveParsingBadInput()
        {
            FitParameters params = new LuminexDataHandler().parseBioPlexStdCurve("FIA = -2.08995 + (29934.1 + 2.08995) / ((1 + (Conc / 2.49287)^-4.99651))^0.215266");
            assertNull("Shouldn't return a standard curve", params);
        }

        @Test
        public void testBioPlexCurveParsingScientific()
        {
            FitParameters params = new LuminexDataHandler().parseBioPlexStdCurve("FI = -0.723451 + (2.48266E+006 + 0.723451) / ((1 + (Conc / 21.932)^-0.192152))^10");
            assertNotNull("Couldn't parse standard curve", params);
            assertEquals(params.asymmetry, 10.0, DELTA);
            assertEquals(params.min, -0.723451, DELTA);
            assertEquals(params.max, 2482660.723451, DELTA);
            assertEquals(params.inflection, 21.932, DELTA);
            assertEquals(params.slope, -0.192152, DELTA);
        }

        @Test
        public void testBioPlexCurveParsingPositiveMinimum()
        {
            FitParameters params = new LuminexDataHandler().parseBioPlexStdCurve("FI = 0.441049 + (30395.4 - 0.441049) / ((1 + (Conc / 5.04206)^-11.8884))^0.0999998");
            assertNotNull("Couldn't parse standard curve", params);
            assertEquals(0.0999998, params.asymmetry, DELTA);
            assertEquals(.441049, params.min, DELTA);
            assertEquals(30394.958951, params.max, DELTA);
            assertEquals(5.04206, params.inflection, DELTA);
            assertEquals(-11.8884, params.slope, DELTA);
        }

        @Test
        public void testAUCSummaryData() throws FitFailedException, ExperimentException
        {
            // test calculation using dilutions for a control
            List<LuminexWell> wells = new ArrayList<>();

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
            assertEquals("Check replicate value", 30427.0, group.getWellData(true).get(0).getValue(), DELTA);
            assertEquals("Check number of raw wells", 10, group.getWellData(false).size());

            assertEquals("AUC", 60310.8, Math.round(new LuminexDataHandler().calculateTrapezoidalAUC(group, null) * 10.0) / 10.0, DELTA);

            // test calculation using expected concentrations for a standard
            wells = new ArrayList<>();

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
            assertEquals("Check replicate value", 10.5, group.getWellData(true).get(0).getValue(), DELTA);
            assertEquals("Check number of raw wells", 10, group.getWellData(false).size());

            assertEquals("AUC", 74375.6, Math.round(new LuminexDataHandler().calculateTrapezoidalAUC(group, null) * 10.0) / 10.0, DELTA);
        }

        @Test
        public void testAUCRawData() throws FitFailedException, ExperimentException
        {
            // test calculation using dilutions for a control
            List<LuminexWell> wells = new ArrayList<>();

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
            assertEquals("Check replicate value", 30427.0, group.getWellData(true).get(0).getValue(), DELTA);
            assertEquals("Check number of raw wells", 20, group.getWellData(false).size());

            assertEquals("AUC", 60310.8, Math.round(new LuminexDataHandler().calculateTrapezoidalAUC(group, null) * 10.0) / 10.0, DELTA);

            // test calculation using expected concentrations for a standard
            wells = new ArrayList<>();

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
            assertEquals("Check replicate value", 10.5, group.getWellData(true).get(0).getValue(), DELTA);
            assertEquals("Check number of raw wells", 20, group.getWellData(false).size());

            assertEquals("AUC", 74375.6, Math.round(new LuminexDataHandler().calculateTrapezoidalAUC(group, null) * 10.0) / 10.0, DELTA);
        }

        @Test
        public void testRawStats()
        {
            // Test calculation of stddev and %cv
            List<LuminexDataRow> dataRows = new ArrayList<>();

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

            assertEquals("Wrong %CV", 0.0072475, Math.round(dataRows.get(0).getCv() * 10000000) / 10000000.0, DELTA);
            assertEquals("Wrong %CV", 0.0072475, Math.round(dataRows.get(1).getCv() * 10000000) / 10000000.0, DELTA);
            assertEquals("Wrong %CV", 0.0005863, Math.round(dataRows.get(2).getCv() * 10000000) / 10000000.0, DELTA);
            assertEquals("Wrong %CV", 0.0005863, Math.round(dataRows.get(3).getCv() * 10000000) / 10000000.0, DELTA);
            assertEquals("Wrong %CV", 0.0092021, Math.round(dataRows.get(4).getCv() * 10000000) / 10000000.0, DELTA);
            assertEquals("Wrong %CV", 0.0092021, Math.round(dataRows.get(5).getCv() * 10000000) / 10000000.0, DELTA);

            assertEquals("Wrong %CV", 5000.0, Math.round(dataRows.get(6).getCv() * 10000000) / 10000000.0, DELTA);
            assertEquals("Wrong %CV", 0.0092021, Math.round(dataRows.get(7).getCv() * 10000000) / 10000000.0, DELTA);

            assertEquals("Wrong StdDev", 220.61732, Math.round(dataRows.get(0).getStdDev() * 100000) / 100000.0, DELTA);
            assertEquals("Wrong StdDev", 220.61732, Math.round(dataRows.get(1).getStdDev() * 100000) / 100000.0, DELTA);
            assertEquals("Wrong StdDev", 17.67767, Math.round(dataRows.get(2).getStdDev() * 100000) / 100000.0, DELTA);
            assertEquals("Wrong StdDev", 17.67767, Math.round(dataRows.get(3).getStdDev() * 100000) / 100000.0, DELTA);
            assertEquals("Wrong StdDev", 245.0125, Math.round(dataRows.get(4).getStdDev() * 100000) / 100000.0, DELTA);
            assertEquals("Wrong StdDev", 245.0125, Math.round(dataRows.get(5).getStdDev() * 100000) / 100000.0, DELTA);

            assertEquals("Wrong StdDev", 5000.0, Math.round(dataRows.get(6).getStdDev() * 100000) / 100000.0, DELTA);
            assertEquals("Wrong StdDev", 245.0125, Math.round(dataRows.get(7).getStdDev() * 100000) / 100000.0, DELTA);
        }

        @Test
        public void testStatsWithMultipleStandards()
        {
            List<LuminexDataRow> dataRows = new ArrayList<>();
            dataRows.add(new LuminexDataRow("S1", "A1", 62.0, "Stnd1"));
            dataRows.add(new LuminexDataRow("S1", "B1", 47.0, "Stnd1"));
            dataRows.add(new LuminexDataRow("S1", "A1", 63.0, "Stnd2"));
            dataRows.add(new LuminexDataRow("S1", "B1", 46.0, "Stnd2"));
            new LuminexDataHandler().ensureSummaryStats(dataRows);

            assertEquals("Wrong StdDev", 10.6066, Math.round(dataRows.get(0).getStdDev() * 100000) / 100000.0, DELTA);
            assertEquals("Wrong StdDev", 10.6066, Math.round(dataRows.get(1).getStdDev() * 100000) / 100000.0, DELTA);
            assertEquals("Wrong StdDev", 12.02082, Math.round(dataRows.get(2).getStdDev() * 100000) / 100000.0, DELTA);
            assertEquals("Wrong StdDev", 12.02082, Math.round(dataRows.get(3).getStdDev() * 100000) / 100000.0, DELTA);

            assertEquals("Wrong %CV", 0.1946165, Math.round(dataRows.get(0).getCv() * 10000000) / 10000000.0, DELTA);
            assertEquals("Wrong %CV", 0.1946165, Math.round(dataRows.get(1).getCv() * 10000000) / 10000000.0, DELTA);
            assertEquals("Wrong %CV", 0.2205654, Math.round(dataRows.get(2).getCv() * 10000000) / 10000000.0, DELTA);
            assertEquals("Wrong %CV", 0.2205654, Math.round(dataRows.get(3).getCv() * 10000000) / 10000000.0, DELTA);
        }

        @Test
        public void testGuideSetOutOfRange()
        {
            // check some actual values used in the first set of runs for the LuminexGuideSetTest (all are in range)
            assertNull("Wrong out of guide set range type", GuideSet.getOutOfRangeType(164.07, 177.15, 18.49));
            assertNull("Wrong out of guide set range type", GuideSet.getOutOfRangeType(190.23, 177.15, 18.49));
            assertNull("Wrong out of guide set range type", GuideSet.getOutOfRangeType(43988.21, 43426.10, 794.95));
            assertNull("Wrong out of guide set range type", GuideSet.getOutOfRangeType(42863.98, 43426.10, 794.95));
            assertNull("Wrong out of guide set range type", GuideSet.getOutOfRangeType(9031.46, 8662.50, 521.79));
            assertNull("Wrong out of guide set range type", GuideSet.getOutOfRangeType(8293.54, 8662.50, 521.79));
            assertNull("Wrong out of guide set range type", GuideSet.getOutOfRangeType(85464.34, 80851.83, 6523.08));
            assertNull("Wrong out of guide set range type", GuideSet.getOutOfRangeType(76239.32, 80851.83, 6523.08));
            assertNull("Wrong out of guide set range type", GuideSet.getOutOfRangeType(11845.50, 11457.15, 549.21));
            assertNull("Wrong out of guide set range type", GuideSet.getOutOfRangeType(11068.80, 11457.15, 549.21));

            // check some of the values used in the second set of runs for the LuminexGuideSetTest
            assertNull("Wrong out of guide set range type", GuideSet.getOutOfRangeType(138.90, 177.15, 18.49));
            assertNull("Wrong out of guide set range type", GuideSet.getOutOfRangeType(207.49, 177.15, 18.49));
            assertNull("Wrong out of guide set range type", GuideSet.getOutOfRangeType(151.95, 177.15, 18.49));
            assertEquals("Wrong out of guide set range type", "under", GuideSet.getOutOfRangeType(6333.02, 8662.50, 521.79));
            assertEquals("Wrong out of guide set range type", "under", GuideSet.getOutOfRangeType(7028.96, 8662.50, 521.79));
            assertNull("Wrong out of guide set range type", GuideSet.getOutOfRangeType(7491.69, 8662.50, 521.79));
            assertNull("Wrong out of guide set range type", GuideSet.getOutOfRangeType(28005.89, 42158.22, 4833.76));
            assertNull("Wrong out of guide set range type", GuideSet.getOutOfRangeType(36676.66, 42158.22, 4833.76));
            assertNull("Wrong out of guide set range type", GuideSet.getOutOfRangeType(45809.80, 42158.22, 4833.76));
            assertEquals("Wrong out of guide set range type", "under", GuideSet.getOutOfRangeType(78448.67, 85268.04, 738.55));
            assertNull("Wrong out of guide set range type", GuideSet.getOutOfRangeType(84451.16, 85268.04, 738.55));
            assertNull("Wrong out of guide set range type", GuideSet.getOutOfRangeType(85888.60, 85268.04, 738.55));

            // other checks using null values, etc.
            assertNull("Wrong out of guide set range type", GuideSet.getOutOfRangeType(90.0, 60.0, 10.0));
            assertNull("Wrong out of guide set range type", GuideSet.getOutOfRangeType(30.0, 60.0, 10.0));
            assertEquals("Wrong out of guide set range type", "over", GuideSet.getOutOfRangeType(91.0, 60.0, 10.0));
            assertEquals("Wrong out of guide set range type", "under", GuideSet.getOutOfRangeType(29.0, 60.0, 10.0));
            assertNull("Wrong out of guide set range type", GuideSet.getOutOfRangeType(null, 60.0, 10.0));
            assertNull("Wrong out of guide set range type", GuideSet.getOutOfRangeType(90.0, null, null));
            assertNull("Wrong out of guide set range type", GuideSet.getOutOfRangeType(60.0, 60.0, null));
            assertEquals("Wrong out of guide set range type", "over", GuideSet.getOutOfRangeType(60.01, 60.0, null));
            assertEquals("Wrong out of guide set range type", "under", GuideSet.getOutOfRangeType(59.99, 60.0, null));

            // Issue 16767
            assertEquals("Wrong out of guide set range type", "over", GuideSet.getOutOfRangeType(0.024779, 0.016229, null));
            assertEquals("Wrong out of guide set range type", "under", GuideSet.getOutOfRangeType(0.017644, 0.021754, null));
            assertEquals("Wrong out of guide set range type", null, GuideSet.getOutOfRangeType(0.017644, 0.017644, 0.0));
        }
    }

    @NotNull
    private CurveFit insertOrUpdateCurveFit(LuminexWellGroup wellGroup, User user, Titration titration, Analyte analyte, FitParameters params, Double ec50, Boolean flag, StatsService.CurveFitType fitType, String source, CurveFit[] existingCurveFits)
            throws FitFailedException
    {
        CurveFit fit = createCurveFit(titration, analyte, params, ec50, flag, fitType, source);
        return insertOrUpdateCurveFit(user, fit, existingCurveFits);
    }

    @NotNull
    private CurveFit insertOrUpdateCurveFit(User user, CurveFit fit, CurveFit[] existingCurveFits)
    {
        CurveFit matchingFit = null;
        for (CurveFit existingCurveFit : existingCurveFits)
        {
            if (existingCurveFit.getCurveType().equals(fit.getCurveType()))
            {
                matchingFit = existingCurveFit;
                break;
            }
        }
        if (matchingFit != null)
        {
            fit.setRowId(matchingFit.getRowId());
            return Table.update(user, LuminexProtocolSchema.getTableInfoCurveFit(), fit, fit.getRowId());
        }
        else
        {
            return Table.insert(user, LuminexProtocolSchema.getTableInfoCurveFit(), fit);
        }
    }

    private CurveFit createCurveFit(Titration titration, Analyte analyte, FitParameters params, Double ec50, Boolean flag, StatsService.CurveFitType fitType, String source)
            throws FitFailedException
    {
        CurveFit fit = new CurveFit();
        fit.setAnalyteId(analyte.getRowId());
        fit.setTitrationId(titration.getRowId());
        if (flag != null && flag)
        {
            fit.setFailureFlag(flag);
        }
        else
        {
            fit.setMinAsymptote(params.getMin());
            fit.setMaxAsymptote(params.getMax());
            fit.setInflection(params.getInflection());
            fit.setSlope(params.getSlope());
            if (fitType == StatsService.CurveFitType.FIVE_PARAMETER)
            {
                fit.setAsymmetry(params.getAsymmetry());
            }
            fit.setEC50(ec50);
        }

        // Don't calculate AUC for 4/5PL fits
        //double auc = curveImpl.calculateAUC(DilutionCurve.AUCType.NORMAL);

        String fitLabel = fitType.getLabel();
        if (source != null)
            fitLabel = source + " " + fitLabel;

        fit.setCurveType(fitLabel);
        return fit;
    }

    /** Insert or Update QC Flags for High MFI, 4PL EC50, 5PL EC50, or AUC values that are
      out of the guide set range if this AnalyteTitration record has a current GuideSet */
    public static void insertOrUpdateAnalyteTitrationQCFlags(User user, ExpRun expRun, ExpProtocol protocol, @NotNull AnalyteTitration analyteTitration, @NotNull Analyte analyte, @NotNull Titration titration, String isotype, String conjugate, List<CurveFit> curveFits)
    {
        AssayProvider provider = AssayService.get().getProvider(protocol);
        if (!(provider instanceof LuminexAssayProvider))
            throw new NotFoundException("Luminex assay provider not found");

        LuminexProtocolSchema schema = new LuminexProtocolSchema(user, expRun.getContainer(), (LuminexAssayProvider)provider, protocol, null);

        // query the QC Flags table to get any existing analyte/titration QC Flags
        ExpQCFlagTable qcFlagTable = schema.createAnalyteTitrationQCFlagTable();
        SimpleFilter analyteTitrationFilter = new SimpleFilter(FieldKey.fromParts("Analyte"), analyte.getRowId());
        analyteTitrationFilter.addCondition(FieldKey.fromParts("Titration"), titration.getRowId());
        List<AnalyteTitrationQCFlag> existingAnalyteTitrationQCFlags = new TableSelector(qcFlagTable, analyteTitrationFilter, null).getArrayList(AnalyteTitrationQCFlag.class);

        List<AnalyteTitrationQCFlag> newAnalyteTitrationQCFlags = new ArrayList<>();

        if (null != analyteTitration.getGuideSetId())
        {
            // query the guide set table to get the average and stddev values for the out of guide set range comparisons
            GuideSetTable guideSetTable = schema.createGuideSetTable(false);
            GuideSet guideSetRow = new TableSelector(guideSetTable).getObject(analyteTitration.getGuideSetId(), GuideSet.class);

            if (guideSetRow == null)
            {
                throw new IllegalStateException("Unable to find referenced guide set: " + analyteTitration.getGuideSetId());
            }
            else
            {
                String descriptionPrefix = titration.getName() + " " + analyte.getName() + " - "
                        + (isotype == null ? "[None]" : isotype) + " "
                        + (conjugate == null ? "[None]" : conjugate) + " ";

                // add QCFlag for High MFI, if out of guide set range
                String outOfRangeType = guideSetRow.getOutOfRangeTypeForMaxFI(analyteTitration.getMaxFI(), guideSetTable, "TitrationMaxFIAverage", "TitrationMaxFIStdDev");
                if (guideSetRow.isMaxFIEnabled() && null != outOfRangeType)
                {
                    newAnalyteTitrationQCFlags.add(new AnalyteTitrationQCFlag(expRun.getRowId(), QC_FLAG_HIGH_MFI_FLAG_TYPE, descriptionPrefix + outOfRangeType + " threshold for High MFI", analyte.getRowId(), titration.getRowId()));
                }

                // add QCFlag for curvefit values (4PL EC50, 4PL EC50, and AUC), if out of guide set range
                for (CurveFit curveFit : curveFits)
                {
                    String flagType = null;

                    if (guideSetRow.isEc504plEnabled() && curveFit.getCurveType().equals(StatsService.CurveFitType.FOUR_PARAMETER.getLabel()))
                    {
                        outOfRangeType = guideSetRow.getOutOfRangeTypeForEC504PL(curveFit.getEC50(), guideSetTable);
                        flagType = QC_FLAG_EC50_4PL_FLAG_TYPE;
                    }
                    else if (guideSetRow.isEc505plEnabled() && curveFit.getCurveType().equals(StatsService.CurveFitType.FIVE_PARAMETER.getLabel()))
                    {
                        outOfRangeType = guideSetRow.getOutOfRangeTypeForEC505PL(curveFit.getEC50(), guideSetTable);
                        flagType = QC_FLAG_EC50_5PL_FLAG_TYPE;
                    }
                    else if (guideSetRow.isAucEnabled() && curveFit.getCurveType().equals("Trapezoidal"))
                    {
                        outOfRangeType = guideSetRow.getOutOfRangeTypeForAUC(curveFit.getAUC(), guideSetTable);
                        flagType = QC_FLAG_AUC_FLAG_TYPE;
                    }

                    if (null != flagType && null != outOfRangeType)
                    {
                        newAnalyteTitrationQCFlags.add(new AnalyteTitrationQCFlag(expRun.getRowId(), flagType, descriptionPrefix + outOfRangeType + " threshold for " + flagType, analyte.getRowId(), titration.getRowId()));
                    }
                }
            }
        }

        // insert new flags if a matching QC Flag does not exist (based on runId, flagType, desription, analyteId, and titrationId)
        for (AnalyteTitrationQCFlag newAnalyteTitrationQCFlag : newAnalyteTitrationQCFlags)
        {
            if (!existingAnalyteTitrationQCFlags.contains(newAnalyteTitrationQCFlag))
            {
                Table.insert(user, ExperimentService.get().getTinfoAssayQCFlag(), newAnalyteTitrationQCFlag);
            }
        }

        // remove existing flags that are no longer relevant based on rerunning the transform script with well exclusions
        for (AnalyteTitrationQCFlag existingAnalyteTitrationQCFlag : existingAnalyteTitrationQCFlags)
        {
            if (!newAnalyteTitrationQCFlags.contains(existingAnalyteTitrationQCFlag))
            {
                Table.delete(ExperimentService.get().getTinfoAssayQCFlag(), existingAnalyteTitrationQCFlag.getRowId());
            }
        }
    }

    public static List<String> getAllGuideSetFlagTypes()
    {
        List<String> flagTypes = new ArrayList<>();
        flagTypes.add(QC_FLAG_EC50_4PL_FLAG_TYPE);
        flagTypes.add(QC_FLAG_EC50_5PL_FLAG_TYPE);
        flagTypes.add(QC_FLAG_AUC_FLAG_TYPE);
        flagTypes.add(QC_FLAG_HIGH_MFI_FLAG_TYPE);
        flagTypes.add(QC_FLAG_SINGLE_POINT_CONTROL_FI_FLAG_TYPE);
        return flagTypes;
    }

    private ParticipantVisitResolver findParticipantVisitResolver(ExpRun expRun, User user, LuminexAssayProvider provider)
            throws ExperimentException
    {
        try
        {
            return AssayService.get().createResolver(user, expRun, expRun.getProtocol(), provider, null);
        }
        catch (IOException e)
        {
            throw new ExperimentException(e);
        }
    }

    /** @return Name->Titration */
    private Map<String, Titration> insertTitrations(ExpRun expRun, User user, List<Titration> titrations) throws ExperimentException
    {
        Map<String, Titration> result = new CaseInsensitiveHashMap<>();

        // Insert the titrations first
        for (Titration titration : titrations)
        {
            SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("Name"), titration.getName());
            filter.addCondition(FieldKey.fromParts("RunId"), expRun.getRowId());
            Titration[] exitingTitrations = new TableSelector(LuminexProtocolSchema.getTableInfoTitration(), filter, null).getArray(Titration.class);
            assert exitingTitrations.length <= 1;

            if (exitingTitrations.length > 0)
            {
                titration = exitingTitrations[0];
            }
            else
            {
                titration.setRunId(expRun.getRowId());

                titration = Table.insert(user, LuminexProtocolSchema.getTableInfoTitration(), titration);
            }
            result.put(titration.getName(), titration);
        }
        return result;
    }

    /** @return Name->SinglePointControl */
        private Map<String, SinglePointControl> insertSinglePointControls(ExpRun expRun, User user, List<SinglePointControl> singlePointControls) throws ExperimentException
        {
            Map<String, SinglePointControl> result = new CaseInsensitiveHashMap<>();

            // Insert the singlePointControls first
            for (SinglePointControl singlePointControl : singlePointControls)
            {
                SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("Name"), singlePointControl.getName());
                filter.addCondition(FieldKey.fromParts("RunId"), expRun.getRowId());
                SinglePointControl exitingSinglePointControl = new TableSelector(LuminexProtocolSchema.getTableInfoSinglePointControl(), filter, null).getObject(SinglePointControl.class);

               if (exitingSinglePointControl != null)
                {
                    singlePointControl = exitingSinglePointControl;
                }
                else
                {
                    singlePointControl.setRunId(expRun.getRowId());

                    singlePointControl = Table.insert(user, LuminexProtocolSchema.getTableInfoSinglePointControl(), singlePointControl);
                }
                result.put(singlePointControl.getName(), singlePointControl);
            }
            return result;
        }

    private void insertExcelProperties(Domain domain, final ExpData data, LuminexExcelParser parser, User user, ExpProtocol protocol) throws SQLException, ValidationException, ExperimentException
    {
        Container container = data.getContainer();
        // Clear out the values - this is necessary if this is a XAR import where the run properties would
        // have been loaded as part of the ExperimentRun itself.
        Integer objectId = OntologyManager.ensureObject(container, data.getLSID());
        for (DomainProperty prop : domain.getProperties())
        {
            OntologyManager.deleteProperty(data.getLSID(), prop.getPropertyURI(), container, protocol.getContainer());
        }

        List<Map<String, Object>> excelRunPropsList = new ArrayList<>();
        Map<String, Object> excelRunPropsByPropertyId = new HashMap<>();
        for (Map.Entry<DomainProperty, String> entry : parser.getExcelRunProps(data.getFile()).entrySet())
        {
            excelRunPropsByPropertyId.put(entry.getKey().getPropertyURI(), entry.getValue());
        }
        excelRunPropsList.add(excelRunPropsByPropertyId);
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
        }, domain, excelRunPropsList, true);
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
            if (fiOORType != LuminexOORIndicator.IN_RANGE) // NOTE: don't need to reset the value if it is in range, see LuminexExcelParser.getCellDoubleValue
                dataRow.setFi(fiOORType.getValue(dataRow.getFiString(), dataRows, fiGetter, analyte));

            LuminexOORIndicator fiBackgroundOORType = determineOutOfRange(dataRow.getFiBackgroundString());
            dataRow.setFiBackgroundOORIndicator(fiBackgroundOORType.getOORIndicator(dataRow.getFiBackgroundString(), dataRows, fiBackgroundGetter));
            if (fiOORType != LuminexOORIndicator.IN_RANGE) // NOTE: don't need to reset the value if it is in range, see LuminexExcelParser.getCellDoubleValue
                dataRow.setFiBackground(fiBackgroundOORType.getValue(dataRow.getFiBackgroundString(), dataRows, fiBackgroundGetter, analyte));

            LuminexOORIndicator stdDevOORType = determineOutOfRange(dataRow.getStdDevString());
            dataRow.setStdDevOORIndicator(stdDevOORType.getOORIndicator(dataRow.getStdDevString(), dataRows, stdDevGetter));
            if (fiOORType != LuminexOORIndicator.IN_RANGE) // NOTE: don't need to reset the value if it is in range, see LuminexExcelParser.getCellDoubleValue
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
            if (fiOORType != LuminexOORIndicator.IN_RANGE) // NOTE: don't need to reset the value if it is in range, see LuminexExcelParser.getCellDoubleValue
                dataRow.setConcInRange(concInRangeOORType.getValue(dataRow.getConcInRangeString(), dataRows, concInRangeGetter, analyte));
        }
    }

    protected void handleParticipantResolver(LuminexDataRow dataRow, ParticipantVisitResolver resolver, Map<ExpMaterial, String> materialInputs, boolean parseDescription) throws ExperimentException
    {
        if (resolver == null)
        {
            return;
        }
        ParticipantVisit match = null;
        if (parseDescription || (dataRow.getSpecimenID() == null && dataRow.getVisitID() == null && dataRow.getParticipantID() == null && dataRow.getDate() == null))
        {
            String value = dataRow.getDescription();
            if (value != null)
            {
                value = value.trim();
                String specimenID = null;
                if (!value.contains(","))
                {
                    specimenID = value;
                }
                // First try resolving the whole description column as a specimen id
                match = resolver.resolve(specimenID, null, null, null, null);
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

                if (isResolved(match))
                {
                    dataRow.setParticipantID(match.getParticipantID());
                    dataRow.setVisitID(match.getVisitID());
                    dataRow.setDate(match.getDate());
                    dataRow.setSpecimenID(match.getSpecimenID());
                    dataRow.setExtraSpecimenInfo(extraSpecimenInfo == null ? null : extraSpecimenInfo.trim());
                }
                else if (match.getSpecimenID() != null && !value.equals(match.getSpecimenID()))
                {
                    // Issue 18601: a sample indices map may have been uploaded that just has a SpecimenID
                    dataRow.setSpecimenID(match.getSpecimenID());
                }
            }
        }
        else
        {
            match = resolver.resolve(dataRow.getSpecimenID(), dataRow.getParticipantID(), dataRow.getVisitID(), dataRow.getDate(), null);
        }

        if (match != null)
        {
            materialInputs.put(match.getMaterial(), null);
        }
    }

    private boolean isResolved(ParticipantVisit match)
    {
        return match.getParticipantID() != null || match.getVisitID() != null || match.getDate() != null;
    }

    private ParticipantVisit resolveParticipantVisitInfo(ParticipantVisitResolver resolver, String specimenID, String[] parts) throws ExperimentException
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

    public ActionURL getContentURL(ExpData data)
    {
        ExpRun run = data.getRun();
        if (run != null)
        {
            ExpProtocol protocol = run.getProtocol();
            ExpProtocol p = ExperimentService.get().getExpProtocol(protocol.getRowId());
            return PageFlowUtil.urlProvider(AssayUrls.class).getAssayResultsURL(data.getContainer(), p, run.getRowId());
        }
        return null;
    }

    public void beforeDeleteData(List<ExpData> data) throws ExperimentException
    {
        List<Integer> ids = new ArrayList<>();
        data.forEach(d -> ids.add(d.getRowId()));
        ListUtils.partition(ids, DELETE_BATCH_SIZE).forEach(this::deleteDatas);
    }

    /**
     * Delete all exclusions for a set of Ids
     * @param dataIds Set of Ids to remove exclusions for
     */
    private void cleanUpExclusions(List<Integer> dataIds, final SqlExecutor executor)
    {
        SQLFragment idSQL = new SQLFragment();
        OntologyManager.getTinfoObject().getSqlDialect().appendInClauseSql(idSQL, dataIds);

        executor.execute(new SQLFragment("DELETE FROM " + LuminexProtocolSchema.getTableInfoWellExclusionAnalyte() +
                " WHERE WellExclusionId IN (SELECT RowId FROM " + LuminexProtocolSchema.getTableInfoWellExclusion() +
                " WHERE DataId ").append(idSQL).append(")"));
        executor.execute(new SQLFragment("DELETE FROM " + LuminexProtocolSchema.getTableInfoWellExclusion() +
                " WHERE DataId ").append(idSQL));
        executor.execute(new SQLFragment("DELETE FROM " + LuminexProtocolSchema.getTableInfoRunExclusionAnalyte() +
                " WHERE RunId IN (SELECT RunId FROM " + ExperimentService.get().getTinfoProtocolApplication() +
                " WHERE RowId IN (SELECT SourceApplicationId FROM " + ExperimentService.get().getTinfoData() +
                " WHERE RowId ").append(idSQL).append("))"));
        executor.execute(new SQLFragment("DELETE FROM " + LuminexProtocolSchema.getTableInfoRunExclusion() +
                " WHERE RunId IN (SELECT RunId FROM " + ExperimentService.get().getTinfoProtocolApplication() +
                " WHERE RowId IN (SELECT SourceApplicationId FROM " + ExperimentService.get().getTinfoData() +
                " WHERE RowId ").append(idSQL).append("))"));
    }

    private void deleteDatas(List<Integer> dataIds)
    {
        SQLFragment idSQL = new SQLFragment();
        OntologyManager.getTinfoObject().getSqlDialect().appendInClauseSql(idSQL, dataIds);

        // Clean up data row properties
        SqlExecutor executor = new SqlExecutor(LuminexProtocolSchema.getSchema());
        executor.execute(new SQLFragment("DELETE FROM " + OntologyManager.getTinfoObjectProperty() + " WHERE ObjectId IN (SELECT o.ObjectID FROM " +
                LuminexProtocolSchema.getTableInfoDataRow() + " dr, " + OntologyManager.getTinfoObject() +
                " o WHERE o.ObjectURI = dr.LSID AND dr.DataId ").append(idSQL).append(")"));
        executor.execute(new SQLFragment("DELETE FROM " + OntologyManager.getTinfoObject() + " WHERE ObjectURI IN (SELECT LSID FROM " +
                LuminexProtocolSchema.getTableInfoDataRow() + " WHERE DataId ").append(idSQL).append(")"));

        // Clean up analyte properties
        executor.execute(new SQLFragment("DELETE FROM " + OntologyManager.getTinfoObjectProperty() + " WHERE ObjectId IN (SELECT o.ObjectID FROM " +
                LuminexProtocolSchema.getTableInfoDataRow() + " dr, " + OntologyManager.getTinfoObject() +
                " o, " + LuminexProtocolSchema.getTableInfoAnalytes() + " a WHERE a.RowId = dr.AnalyteId AND o.ObjectURI = " +
                "a.LSID AND dr.DataId ").append(idSQL).append(")"));
        executor.execute(new SQLFragment("DELETE FROM " + OntologyManager.getTinfoObject() + " WHERE ObjectURI IN (SELECT a.LSID FROM " +
                LuminexProtocolSchema.getTableInfoDataRow() + " dr, " + LuminexProtocolSchema.getTableInfoAnalytes() +
                " a WHERE dr.AnalyteId = a.RowId AND dr.DataId ").append(idSQL).append(")"));

        executor.execute(new SQLFragment("DELETE FROM " + LuminexProtocolSchema.getTableInfoDataRow() +
                " WHERE DataId ").append(idSQL));

        // Clean up exclusions
        cleanUpExclusions(dataIds, executor);

        // Clean up curve fits
        executor.execute(new SQLFragment("DELETE FROM " + LuminexProtocolSchema.getTableInfoCurveFit() +
                " WHERE AnalyteId IN (SELECT RowId FROM " + LuminexProtocolSchema.getTableInfoAnalytes() +
                " WHERE DataId ").append(idSQL).append(")"));

        // Clean up analytes
        executor.execute(new SQLFragment("DELETE FROM " + LuminexProtocolSchema.getTableInfoAnalyteTitration() +
                " WHERE AnalyteId IN (SELECT RowId FROM " + LuminexProtocolSchema.getTableInfoAnalytes() +
                " WHERE DataId ").append(idSQL).append(")"));
        executor.execute(new SQLFragment("DELETE FROM " + LuminexProtocolSchema.getTableInfoAnalyteSinglePointControl() +
                        " WHERE SinglePointControlId IN (SELECT RowId FROM " + LuminexProtocolSchema.getTableInfoSinglePointControl() +
                        " WHERE RunId IN (SELECT pa.RunId FROM " + ExperimentService.get().getTinfoProtocolApplication() +
                        " pa, " + ExperimentService.get().getTinfoData() +
                        " d WHERE pa.RowId = d.SourceApplicationId AND d.RowId ").append(idSQL).append("))"));
        executor.execute(new SQLFragment("DELETE FROM " + LuminexProtocolSchema.getTableInfoAnalytes() +
                " WHERE DataId ").append(idSQL));

        // Delete titrations and SinglePointControls
        executor.execute(new SQLFragment("DELETE FROM " + LuminexProtocolSchema.getTableInfoTitration() +
                " WHERE RunId IN (SELECT pa.RunId FROM " + ExperimentService.get().getTinfoProtocolApplication() +
                " pa, " + ExperimentService.get().getTinfoData() +
                " d WHERE pa.RowId = d.SourceApplicationId AND d.RowId ").append(idSQL).append(")"));
        executor.execute(new SQLFragment("DELETE FROM " + LuminexProtocolSchema.getTableInfoSinglePointControl() +
                        " WHERE RunId IN (SELECT pa.RunId FROM " + ExperimentService.get().getTinfoProtocolApplication() +
                        " pa, " + ExperimentService.get().getTinfoData() +
                        " d WHERE pa.RowId = d.SourceApplicationId AND d.RowId ").append(idSQL).append(")"));
    }

    public void deleteData(ExpData data, Container container, User user)
    {
        deleteDatas(Collections.singletonList(data.getRowId()));
    }

    public Map<DataType, List<Map<String, Object>>> getValidationDataMap(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context, DataLoaderSettings settings) throws ExperimentException
    {
        ExpRun run = data.getRun();
        ExpProtocol protocol = run.getProtocol();
        LuminexExcelParser parser = new LuminexExcelParser(protocol, Collections.singleton(dataFile));
        String dataFileHeaderKey = LuminexManager.get().getDataFileHeaderKey(protocol, dataFile);

        // create a temporary resolver to use for parsing the description value prior to creating the data file for the transform script
        // i.e. we don't care about about resolving the study or lookup information at this time (that will happen after the transform is run)
        ParticipantVisitResolver resolver = new StudyParticipantVisitResolverType().createResolver(run, null, info.getUser());

        Map<DataType, List<Map<String, Object>>> datas = new HashMap<>();
        List<Map<String, Object>> dataRows = new ArrayList<>();

        Set<String> titrations = parser.getTitrations();

        // either the run's RowId (for the re-run transform script case) or the replaced run's RowId (in the re-import run case)
        Integer runId = LuminexManager.get().getRunRowIdForUploadContext(run, ((AssayUploadXarContext)context).getContext());

        Set<String> excludedWells = LuminexManager.get().getWellExclusionKeysForRun(runId, protocol, info.getContainer(), info.getUser());

        for (Map.Entry<Analyte, List<LuminexDataRow>> entry : parser.getSheets().entrySet())
        {
            for (LuminexDataRow dataRow : entry.getValue())
            {
                handleParticipantResolver(dataRow, resolver, new LinkedHashMap<>(), true);
                Map<String, Object> dataMap = dataRow.toMap(entry.getKey());
                dataMap.put("titration", dataRow.getDescription() != null && titrations.contains(dataRow.getDescription()));
                dataMap.remove("data");

                // Merge in whether or not the data row is excluded via a "well exclusion".
                // We write out all the wells, excluded or not, but the transform script can choose to ignore them
                String dataRowWellKey = LuminexManager.get().createWellKey(dataFileHeaderKey, entry.getKey().getName(),
                        dataRow.getDescription(), dataRow.getType(), dataRow.getDilution(), dataRow.getWell());
                dataMap.put(LuminexDataTable.FLAGGED_AS_EXCLUDED_COLUMN_NAME, excludedWells.contains(dataRowWellKey));

                dataRows.add(dataMap);
            }
        }
        datas.put(LUMINEX_TRANSFORMED_DATA_TYPE, dataRows);
        return datas;
    }

    public void importTransformDataMap(ExpData data, AssayRunUploadContext context, ExpRun run, List<Map<String, Object>> dataMaps) throws ExperimentException
    {
        ObjectFactory<Analyte> analyteFactory = ObjectFactory.Registry.getFactory(Analyte.class);
        if (null == analyteFactory)
            throw new ExperimentException("Could not find a matching object factory for " + Analyte.class);

        ObjectFactory<LuminexDataRow> rowFactory = ObjectFactory.Registry.getFactory(LuminexDataRow.class);
        if (null == rowFactory)
            throw new ExperimentException("Could not find a matching object factory for " + LuminexDataRow.class);

        Map<Analyte, List<LuminexDataRow>> sheets = new LinkedHashMap<>();
        Lsid.LsidBuilder builder = new Lsid.LsidBuilder(LuminexAssayProvider.LUMINEX_DATA_ROW_LSID_PREFIX,"");
        for (Map<String, Object> dataMap : dataMaps)
        {
            // CONSIDER: subclass the rowFactory to ignore "titration"
            // titration==true/false so leaving it causes ConversionException(NumberFormatException)
            CaseInsensitiveHashMap<Object> row = new CaseInsensitiveHashMap<>(dataMap);
            row.remove("titration");
            Analyte analyte = analyteFactory.fromMap(row);
            LuminexDataRow dataRow = rowFactory.fromMap(row);
            dataRow._setExtraProperties(row);

            // since a transform script can generate new records for analytes with > 1 standard selected, set lsids for new records
            if (dataRow.getLsid() == null)
            {
                dataRow.setLsid(builder.setObjectId(GUID.makeGUID()).toString());
            }

            Map.Entry<Analyte, List<LuminexDataRow>> entry = LuminexExcelParser.ensureAnalyte(analyte, sheets);
            entry.getValue().add(dataRow);
        }

        LuminexRunContext form = (LuminexRunContext)context;
        importData(data, run, context.getUser(), Logger.getLogger(LuminexDataHandler.class), sheets, form.getParser(), form, false);
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

    public static class FitParameters implements Cloneable
    {
        public Double fitError;
        public double asymmetry;
        public double inflection;
        public double slope;
        public double max;
        public double min;

        public FitParameters copy()
        {
            try
            {
                return (FitParameters) super.clone();
            }
            catch (CloneNotSupportedException e)
            {
                throw new RuntimeException(e);
            }
        }

        public Double getFitError()
        {
            return fitError;
        }

        public double getAsymmetry()
        {
            return asymmetry;
        }

        public double getInflection()
        {
            return inflection;
        }

        public double getSlope()
        {
            return slope;
        }

        public double getMax()
        {
            return max;
        }

        public double getMin()
        {
            return min;
        }

        public Map<String, Object> toMap()
        {
            Map<String, Object> params = new HashMap<>();
            params.put("asymmetry", getAsymmetry());
            params.put("inflection", getInflection());
            params.put("slope", getSlope());
            params.put("max", getMax());
            params.put("min", getMin());

            return Collections.unmodifiableMap(params);
        }
    }
}
