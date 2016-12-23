/*
 * Copyright (c) 2009-2016 LabKey Corporation
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

package org.labkey.nab;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.assay.dilution.DilutionAssayRun;
import org.labkey.api.assay.dilution.DilutionDataHandler;
import org.labkey.api.assay.dilution.DilutionSummary;
import org.labkey.api.assay.nab.NabSpecimen;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.Container;
import org.labkey.api.data.statistics.StatsService;
import org.labkey.api.exp.*;
import org.labkey.api.exp.api.*;
import org.labkey.api.security.User;
import org.labkey.api.util.Pair;
import org.labkey.api.study.assay.AbstractPlateBasedAssayProvider;
import org.labkey.nab.query.NabProtocolSchema;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * User: klum
 * Date: May 15, 2009
 */
public abstract class NabDataHandler extends DilutionDataHandler
{
    public static final Logger LOG = Logger.getLogger(NabDataHandler.class);

    public static final DataType NAB_TRANSFORMED_DATA_TYPE = new DataType("AssayRunNabTransformedData"); // a marker data type
    public static final String NAB_DATA_ROW_LSID_PREFIX = "AssayRunNabDataRow";

    public NabDataHandler()
    {
        super(NAB_DATA_ROW_LSID_PREFIX);
    }

    public Map<DilutionSummary, DilutionAssayRun> getDilutionSummaries(User user, StatsService.CurveFitType fit, int... dataObjectIds) throws ExperimentException, SQLException
    {
        Map<DilutionSummary, DilutionAssayRun> summaries = new LinkedHashMap<>();
        if (dataObjectIds == null || dataObjectIds.length == 0)
            return summaries;

        Map<Integer, DilutionAssayRun> dataToAssay = new HashMap<>();
        List<Integer> nabSpecimenIds = new ArrayList<>(dataObjectIds.length);
        for (int nabSpecimenId : dataObjectIds)
            nabSpecimenIds.add(nabSpecimenId);
        List<NabSpecimen> nabSpecimens = NabManager.get().getNabSpecimens(nabSpecimenIds);
        for (NabSpecimen nabSpecimen : nabSpecimens)
        {
            String wellgroupName = nabSpecimen.getWellgroupName();
            if (null == wellgroupName)
                continue;

            int runId = nabSpecimen.getRunId();
            DilutionAssayRun assay = dataToAssay.get(runId);
            if (assay == null)
            {
                ExpRun run = ExperimentService.get().getExpRun(runId);
                if (null == run)
                    continue;
                assay = getAssayResults(run, user, fit);
                if (null == assay)
                    continue;
                dataToAssay.put(runId, assay);
            }

            for (DilutionSummary summary : assay.getSummaries())
            {
                if (wellgroupName.equals(summary.getFirstWellGroup().getName()))
                {
                    summaries.put(summary, assay);
                    break;
                }
            }
        }
        return summaries;
    }

    @Override
    protected void importRows(ExpData data, ExpRun run, ExpProtocol protocol, List<Map<String, Object>> rawData, User user) throws ExperimentException
    {
        try
        {
            Map<Integer, String> cutoffFormats = getCutoffFormats(protocol, run);
            Map<String, Pair<Integer, String>> wellGroupNameToNabSpecimen = new HashMap<>();

            populateDilutionStats(data, run, protocol, rawData, wellGroupNameToNabSpecimen);
            populateWellData(protocol, run, user, cutoffFormats, wellGroupNameToNabSpecimen);
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Populates cutoff and AUC information from the passed in raw data
     */
    public void populateDilutionStats(ExpData data, ExpRun run, ExpProtocol protocol, List<Map<String, Object>> rawData,
                                      Map<String, Pair<Integer, String>> wellgroupNameToNabSpecimen) throws ExperimentException, SQLException
    {
        _populateDilutionStats(data, run, protocol, rawData, wellgroupNameToNabSpecimen, true, Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Recalculate cutoff and AUC information from the passed in raw data
     */
    public void recalculateDilutionStats(ExpData data, ExpRun run, ExpProtocol protocol, List<Map<String, Object>> rawData,
                                         List<Map<String, Object>> specimenRows, List<Map<String, Object>> cutoffRows) throws ExperimentException, SQLException
    {
        _populateDilutionStats(data, run, protocol, rawData, Collections.emptyMap(), false, specimenRows, cutoffRows);
    }

    /**
     * Calculates cutoff and AUC information from the passed in raw data and optionally saves the data to the
     * specific tables.
     *
     * @param commitData true to persist dilution and well level data
     * @param wellGroupNameToNabSpecimen
     * @param specimenRows if commitData is false, then specimen data will be returned in this collection
     * @param cutoffRows if commitData is false, then cutoff data will be returned in this collection
     */
    private void _populateDilutionStats(ExpData data, ExpRun run, ExpProtocol protocol, List<Map<String, Object>> rawData,
                                        Map<String, Pair<Integer, String>> wellGroupNameToNabSpecimen, boolean commitData,
                                        List<Map<String, Object>> specimenRows, List<Map<String, Object>> cutoffRows) throws ExperimentException, SQLException
    {
        Container container = run.getContainer();
        OntologyManager.ensureObject(container, data.getLSID());
        Map<Integer, String> cutoffFormats = getCutoffFormats(protocol, run);
        Map<String, ExpMaterial> inputMaterialMap = new HashMap<>();

        for (ExpMaterial material : run.getMaterialInputs().keySet())
            inputMaterialMap.put(material.getLSID(), material);

        for (Map<String, Object> group : rawData)
        {
            if (!group.containsKey(WELLGROUP_NAME_PROPERTY))
                throw new ExperimentException("The row must contain a value for the well group name : " + WELLGROUP_NAME_PROPERTY);

            if (group.get(WELLGROUP_NAME_PROPERTY) == null)
                throw new ExperimentException("The row must contain a value for the well group name : " + WELLGROUP_NAME_PROPERTY);

            if (group.get(DILUTION_INPUT_MATERIAL_DATA_PROPERTY) == null)
                throw new ExperimentException("The row must contain a value for the specimen lsid : " + DILUTION_INPUT_MATERIAL_DATA_PROPERTY);

            String groupName = group.get(WELLGROUP_NAME_PROPERTY).toString();
            String specimenLsid = group.get(DILUTION_INPUT_MATERIAL_DATA_PROPERTY).toString();

            ExpMaterial material = inputMaterialMap.get(specimenLsid);

            if (material == null)
                throw new ExperimentException("The row must contain a value for the specimen lsid : " + DILUTION_INPUT_MATERIAL_DATA_PROPERTY);

            String dataRowLsid = getDataRowLSID(data, groupName, material.getPropertyValues()).toString();

            OntologyManager.ensureObject(container, dataRowLsid,  data.getLSID());
            int objectId = 0;

            // New code to insert into NAbSpecimen and CutoffValue tables instead of Ontology properties
            Map<String, Object> nabSpecimenEntries = new HashMap<>();
            nabSpecimenEntries.put(WELLGROUP_NAME_PROPERTY, groupName);
            nabSpecimenEntries.put("ObjectId", objectId);                       // TODO: this will go away  when nab table transfer is complete
            nabSpecimenEntries.put("ObjectUri", dataRowLsid);
            nabSpecimenEntries.put("ProtocolId", protocol.getRowId());
            nabSpecimenEntries.put("DataId", data.getRowId());
            nabSpecimenEntries.put("RunId", run.getRowId());
            nabSpecimenEntries.put("SpecimenLsid", specimenLsid);
            nabSpecimenEntries.put("FitError", group.get(FIT_ERROR_PROPERTY));
            nabSpecimenEntries.put("Auc_Poly", group.get(AUC_PREFIX + POLY_SUFFIX));
            nabSpecimenEntries.put("PositiveAuc_Poly", group.get(pAUC_PREFIX + POLY_SUFFIX));
            nabSpecimenEntries.put("Auc_4pl", group.get(AUC_PREFIX + PL4_SUFFIX));
            nabSpecimenEntries.put("PositiveAuc_4pl", group.get(pAUC_PREFIX + PL4_SUFFIX));
            nabSpecimenEntries.put("Auc_5pl", group.get(AUC_PREFIX + PL5_SUFFIX));
            nabSpecimenEntries.put("PositiveAuc_5pl", group.get(pAUC_PREFIX + PL5_SUFFIX));
            String virusWellGroupName = (String)group.get(AbstractPlateBasedAssayProvider.VIRUS_WELL_GROUP_NAME);
            nabSpecimenEntries.put("VirusLsid", createVirusWellGroupLsid(data, virusWellGroupName));

            int nabRowid = 0;
            if (commitData)
            {
                nabRowid = NabManager.get().insertNabSpecimenRow(null, nabSpecimenEntries);
                wellGroupNameToNabSpecimen.put(groupName, new Pair<>(nabRowid, specimenLsid));
            }
            else
                specimenRows.add(new CaseInsensitiveHashMap<>(nabSpecimenEntries));

            for (Integer cutoffValue : cutoffFormats.keySet())
            {
                Map<String, Object> cutoffEntries = new HashMap<>();
                if (commitData)
                    cutoffEntries.put("NabSpecimenId", nabRowid);
                else
                    cutoffEntries.put("NabSpecimenId", specimenLsid);
                cutoffEntries.put("Cutoff", (double)cutoffValue);

                String cutoffStr = cutoffValue.toString();
                String icKey = POINT_IC_PREFIX + cutoffStr;
                cutoffEntries.put("Point", group.get(icKey));
                icKey = POINT_IC_PREFIX + cutoffStr + OOR_SUFFIX;
                cutoffEntries.put("PointOORIndicator", group.get(icKey));
                icKey = CURVE_IC_PREFIX + cutoffStr + POLY_SUFFIX;
                cutoffEntries.put("IC_Poly", group.get(icKey));
                icKey = CURVE_IC_PREFIX + cutoffStr + POLY_SUFFIX + OOR_SUFFIX;
                cutoffEntries.put("IC_PolyOORIndicator", group.get(icKey));
                icKey = CURVE_IC_PREFIX + cutoffStr + PL4_SUFFIX;
                cutoffEntries.put("IC_4pl", group.get(icKey));
                icKey = CURVE_IC_PREFIX + cutoffStr + PL4_SUFFIX + OOR_SUFFIX;
                cutoffEntries.put("IC_4plOORIndicator", group.get(icKey));
                icKey = CURVE_IC_PREFIX + cutoffStr + PL5_SUFFIX;
                cutoffEntries.put("IC_5pl", group.get(icKey));
                icKey = CURVE_IC_PREFIX + cutoffStr + PL5_SUFFIX + OOR_SUFFIX;
                cutoffEntries.put("IC_5plOORIndicator", group.get(icKey));

                if (commitData)
                    NabManager.get().insertCutoffValueRow(null, cutoffEntries);
                else
                    cutoffRows.add(new CaseInsensitiveHashMap<>(cutoffEntries));
            }
            if (commitData)
                NabProtocolSchema.clearProtocolFromCutoffCache(protocol.getRowId());
        }
    }

    @Override
    public void beforeDeleteData(List<ExpData> datas) throws ExperimentException
    {
        try
        {
            NabManager.get().deleteRunData(datas);
        }
        catch(SQLException e)
        {
            throw new ExperimentException(e);
        }
    }

    /**
     * Parse a list of values into multiple plates.
     */
    protected List<double[][]> parseList(File dataFile, List<Map<String, Object>> rows, String locationColumnHeader, String resultColumnHeader,
                                         int maxPlates, int expectedRows, int expectedCols, List<ExperimentException> errors)
    {
        int wellsPerPlate = expectedRows * expectedCols;
        int wellCount = 0;
        int plateCount = 0;
        double[][] wellValues = new double[expectedRows][expectedCols];
        List<double[][]> plates = new ArrayList<>();

        try
        {
            for (Map<String, Object> row : rows)
            {
                // Current line in the data file is calculated by the number of wells we've already read,
                // plus one for the current row, plus one for the header row:
                int line = plateCount * wellsPerPlate + wellCount + 2;
                Pair<Integer, Integer> location = getWellLocation(dataFile, locationColumnHeader, expectedRows, expectedCols, row, line);
                if (location == null)
                    break;

                int plateRow = location.getKey();
                int plateCol = location.getValue();

                Object dataValue = row.get(resultColumnHeader);
                if (dataValue == null)
                {
                    errors.add(createParseError(dataFile, "No valid result value found on line " + line + ".  Expected integer " +
                            "result values in the last data file column (\"" + resultColumnHeader + "\") found: " + dataValue));
                    return plates;
                }

                Integer value = null;
                if (dataValue instanceof Integer)
                    value = (Integer)dataValue;
                if (dataValue instanceof String)
                {
                    try
                    {
                        Double d = Double.valueOf((String)dataValue);
                        value = (int)Math.round(d);
                    }
                    catch (NumberFormatException nfe)
                    {
                        // ignore
                    }
                }

                if (value == null)
                {
                    errors.add(createParseError(dataFile, "No valid result value found on line " + line + ".  Expected integer " +
                            "result values in the last data file column (\"" + resultColumnHeader + "\") found: " + dataValue));
                    return plates;
                }

                wellValues[plateRow - 1][plateCol - 1] = value;
                if (++wellCount == wellsPerPlate)
                {
                    plates.add(wellValues);
                    wellValues = new double[expectedRows][expectedCols];
                    plateCount++;
                    wellCount = 0;
                }

                // Stop if we've reached the expected number of plates
                if (maxPlates > 0 && plateCount == maxPlates)
                    break;
            }

            if (wellCount != 0)
            {
                errors.add(createParseError(dataFile, "Expected well data in multiples of " + wellsPerPlate + ".  The file provided included " +
                        plateCount + " complete plates of data, plus " + wellCount + " extra rows."));
            }
        }
        catch (ExperimentException e)
        {
            errors.add(e);
        }

        if (plates.size() > 0)
            LOG.debug("found " + plates.size() + " list style plate data in " + dataFile.getName());

        return plates;
    }

    /**
     * Translate a well location value, e.g. "B04", into a (row, column) pair of coordinates.
     */
    @Nullable
    protected Pair<Integer, Integer> getWellLocation(File dataFile, String locationColumnHeader, int expectedRows, int expectedCols, Map<String, Object> line, int lineNumber) throws ExperimentException
    {
        Object locationValue = line.get(locationColumnHeader);
        if (locationValue == null || !(locationValue instanceof String) || ((String) locationValue).length() < 2)
            //throw createWellLocationParseError(dataFile, locationColumnHeader, lineNumber, locationValue);
            return null;

        String location = (String) locationValue;
        Character rowChar = location.charAt(0);
        rowChar = Character.toUpperCase(rowChar);
        if (!(rowChar >= 'A' && rowChar <= 'Z'))
            //throw createWellLocationParseError(dataFile, locationColumnHeader, lineNumber, locationValue);
            return null;

        Integer col;
        try
        {
            col = Integer.parseInt(location.substring(1));
        }
        catch (NumberFormatException e)
        {
            //throw createWellLocationParseError(dataFile, locationColumnHeader, lineNumber, locationValue);
            return null;
        }
        int row = rowChar - 'A' + 1;

        // 1-based row and column indexing:
        if (row > expectedRows)
        {
            throw createParseError(dataFile, "Invalid row " + row + " specified on line " + lineNumber +
                    ".  The current plate template defines " + expectedRows + " rows.");
        }

        // 1-based row and column indexing:
        if (col > expectedCols)
        {
            throw createParseError(dataFile, "Invalid column " + col + " specified on line " + lineNumber +
                    ".  The current plate template defines " + expectedCols + " columns.");
        }

        return new Pair<>(row, col);
    }
}
