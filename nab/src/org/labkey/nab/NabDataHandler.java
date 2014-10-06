/*
 * Copyright (c) 2009-2014 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.assay.dilution.DilutionAssayRun;
import org.labkey.api.assay.dilution.DilutionDataHandler;
import org.labkey.api.assay.dilution.DilutionSummary;
import org.labkey.api.assay.nab.NabSpecimen;
import org.labkey.api.collections.RowMap;
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

    private static final int START_ROW = 6; //0 based, row 7 in the workshet
    private static final int START_COL = 0;

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
    protected void importRows(ExpData data, ExpRun run, ExpProtocol protocol, List<Map<String, Object>> rawData) throws ExperimentException
    {
        try
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
                nabSpecimenEntries.put("SpecimenLsid", group.get(DILUTION_INPUT_MATERIAL_DATA_PROPERTY));
                nabSpecimenEntries.put("FitError", group.get(FIT_ERROR_PROPERTY));
                nabSpecimenEntries.put("Auc_Poly", group.get(AUC_PREFIX + POLY_SUFFIX));
                nabSpecimenEntries.put("PositiveAuc_Poly", group.get(pAUC_PREFIX + POLY_SUFFIX));
                nabSpecimenEntries.put("Auc_4pl", group.get(AUC_PREFIX + PL4_SUFFIX));
                nabSpecimenEntries.put("PositiveAuc_4pl", group.get(pAUC_PREFIX + PL4_SUFFIX));
                nabSpecimenEntries.put("Auc_5pl", group.get(AUC_PREFIX + PL5_SUFFIX));
                nabSpecimenEntries.put("PositiveAuc_5pl", group.get(pAUC_PREFIX + PL5_SUFFIX));
                String virusWellGroupName = (String)group.get(AbstractPlateBasedAssayProvider.VIRUS_WELL_GROUP_NAME);
                nabSpecimenEntries.put("VirusLsid", createVirusWellGroupLsid(data, virusWellGroupName));

                int nabRowid = NabManager.get().insertNabSpecimenRow(null, nabSpecimenEntries);

                for (Integer cutoffValue : cutoffFormats.keySet())
                {
                    Map<String, Object> cutoffEntries = new HashMap<>();
                    cutoffEntries.put("NabSpecimenId", nabRowid);
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
                    NabManager.get().insertCutoffValueRow(null, cutoffEntries);
                }
                NabProtocolSchema.clearProtocolFromCutoffCache(protocol.getRowId());
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
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
    protected List<double[][]> parseList(File dataFile, List<Map<String, Object>> rows, String locationColumnHeader, String resultColumnHeader, int maxPlates, int expectedRows, int expectedCols) throws ExperimentException
    {
        int wellsPerPlate = expectedRows * expectedCols;

        int wellCount = 0;
        int plateCount = 0;
        double[][] wellValues = new double[expectedRows][expectedCols];
        List<double[][]> plates = new ArrayList<>();
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
                throw createParseError(dataFile, "No valid result value found on line " + line + ".  Expected integer " +
                        "result values in the last data file column (\"" + resultColumnHeader + "\") found: " + dataValue);
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
                throw createParseError(dataFile, "No valid result value found on line " + line + ".  Expected integer " +
                        "result values in the last data file column (\"" + resultColumnHeader + "\") found: " + dataValue);

            wellValues[plateRow - 1][plateCol - 1] = value;
            if (++wellCount == wellsPerPlate)
            {
                plates.add(wellValues);
                plateCount++;
                wellCount = 0;
            }

            // Stop if we've reached the expected number of plates
            if (maxPlates > 0 && plateCount == maxPlates)
                break;
        }

        if (wellCount != 0)
        {
            throw createParseError(dataFile, "Expected well data in multiples of " + wellsPerPlate + ".  The file provided included " +
                    plateCount + " complete plates of data, plus " + wellCount + " extra rows.");
        }

        if (plates.size() > 0)
            LOG.debug("found " + plates.size() + " list style plate data in " + dataFile.getName());

        return plates;
    }

    /**
     * Search for a grid of numbers that has the expected number of rows and columns.
     * TODO: Find multiple plates in "RC121306.xls" while ignoring duplicate plate found in "20131218_0004.txt"
     */
    protected double[][] parseGrid(File dataFile, List<Map<String, Object>> rows, int expectedRows, int expectedCols)
    {
        double[][] matrix;

        // try to find the top-left cell of the plate
        for (int rowIdx = 0; rowIdx < rows.size(); rowIdx++)
        {
            Map<String, Object> row = rows.get(rowIdx);
            RowMap<Object> rowMap = (RowMap<Object>)row;
            for (int colIdx = 0; colIdx < rowMap.size(); colIdx++)
            {
                Object value = rowMap.get(colIdx);

                // For Luc5, EnVision, and "16AUG11 KK CD3-1-1.8." plate formats:
                // look for labeled matrix with a header row (numbered 1, 2, 3...) and header column (lettered A, B, C...)
                if (isPlateMatrix(rows, rowIdx, colIdx, expectedRows, expectedCols))
                {
                    matrix = parseGridAt(rows, rowIdx+1, colIdx+1, expectedRows, expectedCols);
                    if (matrix != null)
                    {
                        LOG.debug(String.format("found labeled grid style plate data at (%d,%d) in %s", rowIdx+1, colIdx+1, dataFile.getName()));
                        return matrix;
                    }
                }
                else if (value instanceof String)
                {
                    if (colIdx == 0 && ((String)value).startsWith("Plate:"))
                    {
                        // CONSIDER: Detecting SpectraMax format seems fragile.  Create SpectraMax specific PlateReader parser that can be chosen in the assay design ala Elispot.
                        // For SpectraMax format: look for a matrix grid at rowIdx+2, colIdx+2
                        matrix = parseGridAt(rows, rowIdx+2, colIdx+2, expectedRows, expectedCols);
                        if (matrix != null)
                        {
                            LOG.debug(String.format("found SpectraMax grid style plate data at (%d,%d) in %s", rowIdx+1, colIdx+1, dataFile.getName()));
                            return matrix;
                        }
                    }
                    // NOTE: Commented out since finding an abitrary grid hits too many false positives containing null cells.
//                    else if (NumberUtilsLabKey.isNumber((String) value))
//                    {
//                        // if the cell is a number, attempt to find a grid of numbers at the location
//                        matrix = parseGridAt(rows, rowIdx, colIdx, expectedRows, expectedCols);
//                        if (matrix != null)
//                        {
//                            LOG.debug(String.format("found grid style plate data at (%d,%d) in %s", rowIdx+1, colIdx+1, dataFile.getName()));
//                            return matrix;
//                        }
//                    }
                }
            }
        }

        // attempt to parse a grid at the "well known" location (pun intended)
        matrix = parseGridAt(rows, START_ROW, START_COL, expectedRows, expectedCols);
        if (matrix != null)
            return matrix;

        // attempt to parse as grid at 0,0
        matrix = parseGridAt(rows, 0, 0, expectedRows, expectedCols);
        if (matrix != null)
            return matrix;

        return null;
    }

    /**
     * Look for a grid of numbers (or blank) staring from rowIdx, colIdx and matches the expected size.
     * The startRow may be a header row (numbered 1..12)
     */
    protected double[][] parseGridAt(List<Map<String, Object>> rows, int startRow, int startCol, int expectedRows, int expectedCols)
    {
        // Ensure there are enough available rows from startRow
        if (startRow + expectedRows > rows.size())
            return null;

        ArrayList<double[]> values = new ArrayList<>(expectedRows);

        for (int i = 0; i < expectedRows; i++)
        {
            Map<String, Object> row = rows.get(startRow + i);
            RowMap<Object> rowMap = (RowMap<Object>)row;

            double[] cells = parseRowAt(rowMap, startCol, expectedCols);
            if (cells == null)
                return null;

            values.add(cells);
        }

        // Check if this is a header row (numbered 1 through 12)
        // If it is, shift down one row and attempt to parse that.
        // If there is no header row or adding an additional row of numbers fails, accept the data as is.
        if (isSequentialNumbers(values.get(0)))
        {
            if (startRow + expectedRows + 1 <= rows.size())
            {
                Map<String, Object> row = rows.get(startRow + expectedRows + 1);
                RowMap<Object> rowMap = (RowMap<Object>)row;

                double[] cells = parseRowAt(rowMap, startCol, expectedCols);
                if (cells != null)
                {
                    // CONSIDER: Only accept this new row if it has non-null values?
                    values = new ArrayList<>(values.subList(1, values.size()));
                    values.add(cells);
                }
            }
        }

        return values.toArray(new double[values.size()][]);
    }

    protected double[] parseRowAt(RowMap<Object> rowMap, int startCol, int expectedCols)
    {
        // Ensure there are enough available columns from startCol
        if (startCol + expectedCols > rowMap.size())
            return null;

        double[] cells = new double[expectedCols];

        for (int j = 0; j < expectedCols; j++)
        {
            // Get the value at the location and convert a double if possible.
            // If the value is not null or a number, stop parsing.
            Object value = rowMap.get(startCol + j);
            if (value == null)
                cells[j] = 0.0d;
            else if (value instanceof String)
            {
                try
                {
                    cells[j] = Double.parseDouble((String) value);
                }
                catch (NumberFormatException nfe)
                {
                    // failed
                    return null;
                }
            }
            else
                return null;
        }

        return cells;
    }

    // Check if all values in first row are numbered sequentially starting at 1
    protected boolean isSequentialNumbers(double[] row)
    {
        for (int i = 0; i < row.length; i++)
        {
            if (row[i] != i+1)
                return false;
        }

        return true;
    }

    /**
     * Look for a matrix that has numbers 1..12 header row and A..H header column
     *
     * <pre>
     *    1  2  3  ... 12
     * A  .  .  .      .
     * B  .  .  .      .
     * C  .  .  .      .
     * ...
     * H  .  .  .      .
     * </pre>
     */
    protected boolean isPlateMatrix(List<Map<String, Object>> rows, int startRow, int startCol, int expectedRows, int expectedCols)
    {
        Map<String, Object> row = rows.get(startRow);
        RowMap<Object> rowMap = (RowMap<Object>)row;

        // make sure that there are plate_width + 1 cells to the right of startCol:
        if (startCol + expectedCols + 1 > rowMap.size())
            return false;

        // make sure that there are plate_height + 1 cells below startRow
        if (startRow + expectedRows + 1 > rows.size())
            return false;

        // check for 1-12 in the row:
        double[] headerRow = parseRowAt(rowMap, startCol+1, expectedCols);
        if (headerRow == null)
            return false;

        if (!isSequentialNumbers(headerRow))
            return false;

        // check for A-H in the startRow+1
        char start = 'A';
        for (int rowIndex = startRow + 1; rowIndex < startRow + expectedRows + 1; rowIndex++)
        {
            Map<String, Object> currentRow = rows.get(rowIndex);
            RowMap<Object> currentRowMap = (RowMap<Object>)currentRow;
            Object cell = currentRowMap.get(startCol);
            if (cell != null)
            {
                String indexString = String.valueOf(start++);
                if (!StringUtils.equals(String.valueOf(cell), indexString))
                    return false;
            }
        }

        return true;
    }

    protected ExperimentException createWellLocationParseError(File dataFile, String locationColumnHeader, int lineNumber, Object locationValue) throws ExperimentException
    {
        return createParseError(dataFile, "Failed to find valid location in column \"" + locationColumnHeader + "\" on line " + lineNumber +
                ".  Locations should be identified by a single row letter and column number, such as " +
                "A1 or P24.  Found \"" + (locationValue != null ? locationValue.toString() : "") + "\".");
    }

    /**
     * Translate a well location value, e.g. "B04", into a (row, column) pair of coordinates.
     */
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
