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

package org.labkey.nab;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.exp.*;
import org.labkey.api.exp.api.*;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.qc.TransformDataHandler;
import org.labkey.api.security.User;
import org.labkey.api.study.*;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayDataType;
import org.labkey.api.util.Pair;
import org.labkey.api.util.FileType;
import org.labkey.api.view.ViewBackgroundInfo;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * User: brittp
 * Date: Sep 21, 2007
 * Time: 3:21:18 PM
 */
public class NabDataHandler extends AbstractNabDataHandler implements TransformDataHandler
{
    public static final AssayDataType NAB_DATA_TYPE = new AssayDataType("AssayRunNabData", new FileType(".xls"));
    public static final DataType NAB_TRANSFORMED_DATA_TYPE = new DataType("AssayRunNabTransformedData"); // a marker data type
    private static final int START_ROW = 6; //0 based, row 7 inthe workshet
    private static final int START_COL = 0;

    class NabExcelParser implements NabDataFileParser
    {
        private ExpData _data;
        private File _dataFile;
        private ViewBackgroundInfo _info;
        private Logger _log;
        private XarContext _context;

        public NabExcelParser(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context)
        {
            _data = data;
            _dataFile = dataFile;
            _info = info;
            _log = log;
            _context = context;
        }

        public List<Map<String, Object>> getResults() throws ExperimentException
        {
            try {
                ExpRun run = _data.getRun();
                ExpProtocol protocol = ExperimentService.get().getExpProtocol(run.getProtocol().getLSID());
                Container container = _data.getContainer();
                Luc5Assay assayResults = getAssayResults(run, _info.getUser(), _dataFile, null);
                List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
                Map<Integer, String> cutoffFormats = assayResults.getCutoffFormats();

                for (int summaryIndex = 0; summaryIndex < assayResults.getSummaries().length; summaryIndex++)
                {
                    DilutionSummary dilution = assayResults.getSummaries()[summaryIndex];
                    WellGroup group = dilution.getWellGroup();
                    ExpMaterial sampleInput = assayResults.getMaterial(group);

                    Map<String, Object> props = new HashMap<String, Object>();
                    results.add(props);

                    // generate curve ICs and AUCs for each curve fit type
                    for (DilutionCurve.FitType type : DilutionCurve.FitType.values())
                    {
                        for (Integer cutoff : assayResults.getCutoffs())
                        {
                            double value = dilution.getCutoffDilution(cutoff / 100.0, type);
                            saveICValue(getPropertyName(CURVE_IC_PREFIX, cutoff, type), value,
                                    dilution, protocol, container, cutoffFormats, props, type);

                            if (type == assayResults.getRenderedCurveFitType())
                            {
                                saveICValue(CURVE_IC_PREFIX + cutoff, value,
                                        dilution, protocol, container, cutoffFormats, props, type);
                            }
                        }
                        // compute both normal and positive AUC values
                        double auc = dilution.getAUC(type, DilutionCurve.AUCType.NORMAL);
                        if (!Double.isNaN(auc))
                        {
                            props.put(getPropertyName(AUC_PREFIX, type), auc);
                            if (type == assayResults.getRenderedCurveFitType())
                                props.put(AUC_PREFIX, auc);
                        }

                        double pAuc = dilution.getAUC(type, DilutionCurve.AUCType.POSITIVE);
                        if (!Double.isNaN(pAuc))
                        {
                            props.put(getPropertyName(pAUC_PREFIX, type), pAuc);
                            if (type == assayResults.getRenderedCurveFitType())
                                props.put(pAUC_PREFIX, pAuc);
                        }
                    }

                    // only need one set of interpolated ICs as they would be identical for all fit types
                    for (Integer cutoff : assayResults.getCutoffs())
                    {
                        saveICValue(POINT_IC_PREFIX + cutoff,
                                dilution.getInterpolatedCutoffDilution(cutoff / 100.0, assayResults.getRenderedCurveFitType()),
                                dilution, protocol, container, cutoffFormats, props, assayResults.getRenderedCurveFitType());
                    }
                    props.put(FIT_ERROR_PROPERTY, dilution.getFitError());
                    props.put(NAB_INPUT_MATERIAL_DATA_PROPERTY, sampleInput.getLSID());
                    props.put(WELLGROUP_NAME_PROPERTY, group.getName());
                }
                return results;
            }
            catch (DilutionCurve.FitFailedException e)
            {
                throw new ExperimentException(e.getMessage(), e);
            }
        }
    }

    public NabDataFileParser getDataFileParser(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        return new NabExcelParser(data, dataFile, info, log, context);
    }

    public void importTransformDataMap(ExpData data, User user, ExpRun run, ExpProtocol protocol, AssayProvider provider, List<Map<String, Object>> dataMap) throws ExperimentException
    {
        importRows(data, run, protocol, dataMap);
    }

    public Map<DataType, List<Map<String, Object>>> getValidationDataMap(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        NabDataFileParser parser = getDataFileParser(data, dataFile, info, log, context);

        Map<DataType, List<Map<String, Object>>> datas = new HashMap<DataType, List<Map<String, Object>>>();
        datas.put(NAB_TRANSFORMED_DATA_TYPE, parser.getResults());

        return datas;
    }

    public static Map<DilutionSummary, NabAssayRun> getDilutionSummaries(User user, DilutionCurve.FitType fit, int... dataObjectIds) throws ExperimentException, SQLException
    {
        Map<DilutionSummary, NabAssayRun> summaries = new LinkedHashMap<DilutionSummary, NabAssayRun>();
        if (dataObjectIds == null || dataObjectIds.length == 0)
            return summaries;
        Map<String, NabAssayRun> dataToAssay = new HashMap<String, NabAssayRun>();
        for (int dataObjectId : dataObjectIds)
        {
            OntologyObject dataRow = OntologyManager.getOntologyObject(dataObjectId);
            if (dataRow == null || dataRow.getOwnerObjectId() == null)
                continue;
            Map<String, ObjectProperty> properties = OntologyManager.getPropertyObjects(dataRow.getContainer(), dataRow.getObjectURI());
            String wellgroupName = null;
            for (ObjectProperty property : properties.values())
            {
                if (WELLGROUP_NAME_PROPERTY.equals(property.getName()))
                {
                    wellgroupName = property.getStringValue();
                    break;
                }
            }
            if (wellgroupName == null)
                continue;

            OntologyObject dataParent = OntologyManager.getOntologyObject(dataRow.getOwnerObjectId());
            if (dataParent == null)
                continue;
            String dataLsid = dataParent.getObjectURI();
            NabAssayRun assay = dataToAssay.get(dataLsid);
            if (assay == null)
            {
                ExpData dataObject = ExperimentService.get().getExpData(dataLsid);
                if (dataObject == null)
                    continue;
                assay = getAssayResults(dataObject.getRun(), user, fit);
                if (assay == null)
                    continue;
                dataToAssay.put(dataLsid, assay);
            }

            for (DilutionSummary summary : assay.getSummaries())
            {
                if (wellgroupName.equals(summary.getWellGroup().getName()))
                {
                    summaries.put(summary, assay);
                    break;
                }
            }
        }
        return summaries;
    }

    protected static void saveICValue(String name, double icValue, DilutionSummary dilution, ExpProtocol protocol,
                                      Container container, Map<Integer, String> formats, Map<String, Object> results, DilutionCurve.FitType type) throws DilutionCurve.FitFailedException
    {
        String outOfRange = null;
        if (Double.NEGATIVE_INFINITY == icValue)
        {
            outOfRange = "<";
            icValue = dilution.getMinDilution(type);
        }
        else if (Double.POSITIVE_INFINITY == icValue)
        {
            outOfRange = ">";
            icValue = dilution.getMaxDilution(type);
        }
        results.put(name, icValue);
        results.put(name + OORINDICATOR_SUFFIX, outOfRange);
    }

    public static File getDataFile(ExpRun run)
    {
        if (run == null)
            return null;
        ExpData[] outputDatas = run.getOutputDatas(NAB_DATA_TYPE);
        if (outputDatas == null || outputDatas.length != 1)
            throw new IllegalStateException("Nab runs should have a single data output.");
        File dataFile = outputDatas[0].getFile();
        if (!dataFile.exists())
            return null;
        return dataFile;
    }

    public static class MissingDataFileException extends ExperimentException
    {
        public MissingDataFileException(String message)
        {
            super(message);
        }
    }

    public static NabAssayRun getAssayResults(ExpRun run, User user) throws ExperimentException
    {
        return getAssayResults(run, user, null);
    }

    public static NabAssayRun getAssayResults(ExpRun run, User user, DilutionCurve.FitType fit) throws ExperimentException
    {
        File dataFile = getDataFile(run);
        if (dataFile == null)
            throw new MissingDataFileException("Nab data file could not be found for run " + run.getName() + ".  Deleted from file system?");
        return getAssayResults(run, user, dataFile, fit);
    }

    private static NabAssayRun getAssayResults(ExpRun run, User user, File dataFile, DilutionCurve.FitType fit) throws ExperimentException
    {
        ExpProtocol protocol = ExperimentService.get().getExpProtocol(run.getProtocol().getLSID());
        Container container = run.getContainer();
        NabAssayProvider provider = (NabAssayProvider) AssayService.get().getProvider(protocol);
        PlateTemplate nabTemplate = provider.getPlateTemplate(container, protocol);

        Map<String, DomainProperty> runProperties = new HashMap<String, DomainProperty>();
        for (DomainProperty column : provider.getRunDomain(protocol).getProperties())
            runProperties.put(column.getName(), column);
        for (DomainProperty column : provider.getBatchDomain(protocol).getProperties())
            runProperties.put(column.getName(), column);

        Map<Integer, String> cutoffs = getCutoffFormats(protocol, run);

        double[][] cellValues = getCellValues(dataFile, nabTemplate);

        // UNDONE: Eliminate cast to NabAssayProvider here: there needs to be a more general way of retrieving
        // sample preparation information from a protocol/provider.
        DomainProperty[] sampleProperties = provider.getSampleWellGroupDomain(protocol).getProperties();
        Map<String, DomainProperty> samplePropertyMap = new HashMap<String, DomainProperty>();
        for (DomainProperty sampleProperty : sampleProperties)
            samplePropertyMap.put(sampleProperty.getName(), sampleProperty);


        Plate plate = PlateService.get().createPlate(nabTemplate, cellValues);
        List<? extends WellGroup> specimenGroups = plate.getWellGroups(WellGroup.Type.SPECIMEN);
        Collection<ExpMaterial> sampleInputs = run.getMaterialInputs().keySet();
        Map<WellGroup, ExpMaterial> inputs = getWellGroupMaterialPairings(sampleInputs, specimenGroups);

        prepareWellGroups(inputs, samplePropertyMap);

        List<Integer> sortedCutoffs = new ArrayList<Integer>(cutoffs.keySet());
        Collections.sort(sortedCutoffs);

        DomainProperty curveFitPd = runProperties.get(NabAssayProvider.CURVE_FIT_METHOD_PROPERTY_NAME);
        if (fit == null)
        {
            fit = DilutionCurve.FitType.FIVE_PARAMETER;
            if (curveFitPd != null)
            {
                Object value = run.getProperty(curveFitPd);
                if (value != null)
                    fit = DilutionCurve.FitType.fromLabel((String) value);
            }
        }
        boolean lockAxes = false;
        DomainProperty lockAxesProperty = runProperties.get(NabAssayProvider.LOCK_AXES_PROPERTY_NAME);
        if (lockAxesProperty != null)
        {
            Boolean lock = (Boolean) run.getProperty(lockAxesProperty);
            if (lock != null)
                lockAxes = lock.booleanValue();
        }

        NabAssayRun assay = new NabAssayRun(provider, run, plate, user, sortedCutoffs, fit);
        assay.setCutoffFormats(cutoffs);
        assay.setWellGroupMaterialMapping(inputs);
        assay.setDataFile(dataFile);
        assay.setLockAxes(lockAxes);
        return assay;
    }

    private static Map<WellGroup, ExpMaterial> getWellGroupMaterialPairings(Collection<ExpMaterial> sampleInputs, List<? extends WellGroup> wellgroups)
    {
        Map<String, ExpMaterial> nameToMaterial = new HashMap<String, ExpMaterial>();
        for (ExpMaterial material : sampleInputs)
            nameToMaterial.put(material.getName(), material);

        Map<WellGroup, ExpMaterial> mapping = new HashMap<WellGroup, ExpMaterial>();
        for (WellGroup wellgroup : wellgroups)
        {
            ExpMaterial material = nameToMaterial.get(wellgroup.getName());
            if (material == null)
                throw new IllegalStateException("Each wellgroup should have a matching input material.");
            mapping.put(wellgroup, material);
        }
        return mapping;
    }

    private static double[][] getCellValues(File dataFile, PlateTemplate nabTemplate) throws ExperimentException
    {
        WorkbookSettings settings = new WorkbookSettings();
        settings.setGCDisabled(true);
        Workbook workbook = null;
        try
        {
            workbook = Workbook.getWorkbook(dataFile, settings);
        }
        catch (IOException e)
        {
            throw new ExperimentException(dataFile.getName() + " does not appear to be a valid data file: " + e.getMessage(), e);
        }
        catch (BiffException e)
        {
            throw new ExperimentException(dataFile.getName() + " does not appear to be a valid data file: " + e.getMessage(), e);
        }
        double[][] cellValues = new double[nabTemplate.getRows()][nabTemplate.getColumns()];

        Sheet plateSheet = null;
        Pair<Integer, Integer> dataLocation = null;

        // search the workbook for a region that contains 96 cells of data labeled with A-H rows and 1-12 cols:
        for (int sheet = 0; sheet < workbook.getNumberOfSheets() && dataLocation == null; sheet++)
        {
            plateSheet = workbook.getSheet(sheet);
            dataLocation = getPlateDataLocation(plateSheet, nabTemplate.getRows(), nabTemplate.getColumns());
        }

        int startRow;
        int startColumn;
        if (dataLocation == null)
        {
            // if we couldn't find a labeled grid of plate data, we'll assume the default location at START_ROW/START_COL
            // within the second worksheet:
            startRow = START_ROW;
            startColumn = START_COL;
            if (workbook.getSheets().length < 2)
                throw new ExperimentException(dataFile.getName() + " does not appear to be a valid data file: no plate data was found.");
            plateSheet = workbook.getSheet(1);
        }
        else
        {
            startRow = dataLocation.getKey().intValue();
            startColumn = dataLocation.getValue().intValue();
        }

        if (nabTemplate.getRows() + startRow > plateSheet.getRows() || nabTemplate.getColumns() + startColumn > plateSheet.getColumns())
        {
            throw new ExperimentException(dataFile.getName() + " does not appear to be a valid data file: expected " +
                    (nabTemplate.getRows() + startRow) + " rows and " + (nabTemplate.getColumns() + startColumn) + " colums, but found "+
                    plateSheet.getRows() + " rows and " + plateSheet.getColumns() + " colums.");
        }

        for (int row = 0; row < nabTemplate.getRows(); row++)
        {
            for (int col = 0; col < nabTemplate.getColumns(); col++)
            {
                Cell cell = plateSheet.getCell(col + startColumn, row + startRow);
                String cellContents = cell.getContents();
                try
                {
                    cellValues[row][col] = Double.parseDouble(cellContents);
                }
                catch (NumberFormatException e)
                {
                    throw new ExperimentException(dataFile.getName() + " does not appear to be a valid data file: could not parse '" +
                            cellContents + "' as a number.", e);
                }
            }
        }
        return cellValues;
    }

    private static void prepareWellGroups(Map<WellGroup, ExpMaterial> inputs, Map<String, DomainProperty> properties)
    {
        for (Map.Entry<WellGroup, ExpMaterial> input : inputs.entrySet())
        {
            WellGroup group = input.getKey();
            ExpMaterial sampleInput = input.getValue();
            for (DomainProperty property : properties.values())
                group.setProperty(property.getName(), sampleInput.getProperty(property));

            List<WellData> wells = group.getWellData(true);
            boolean first = true;
            boolean reverseDirection = Boolean.parseBoolean((String) group.getProperty(NabManager.SampleProperty.ReverseDilutionDirection.name()));
            Double dilution = (Double) sampleInput.getProperty(properties.get(NabAssayProvider.SAMPLE_INITIAL_DILUTION_PROPERTY_NAME));
            Double factor = (Double) sampleInput.getProperty(properties.get(NabAssayProvider.SAMPLE_DILUTION_FACTOR_PROPERTY_NAME));
            String methodString = (String) sampleInput.getProperty(properties.get(NabAssayProvider.SAMPLE_METHOD_PROPERTY_NAME));
            SampleInfo.Method method = SampleInfo.Method.valueOf(methodString);
            int firstGroup = reverseDirection ? 0 : wells.size() - 1;
            int incrementor = reverseDirection ? 1 : -1;
            for (int groupIndex = firstGroup; groupIndex >= 0 && groupIndex < wells.size(); groupIndex = groupIndex + incrementor)
            {
                WellData well = wells.get(groupIndex);
                if (!first)
                {
                    if (method == SampleInfo.Method.Dilution)
                        dilution *= factor;
                    else if (method == SampleInfo.Method.Concentration)
                        dilution /= factor;
                }
                else
                    first = false;
                well.setDilution(dilution);
            }
        }
    }

    public Priority getPriority(ExpData data)
    {
        Lsid lsid = new Lsid(data.getLSID());
        if (NAB_DATA_TYPE.matches(lsid))
        {
            return Priority.HIGH;
        }
        return null;
    }

    private static Pair<Integer, Integer> getPlateDataLocation(Sheet plateSheet, int plateHeight, int plateWidth)
    {
        for (int row = 0; row < plateSheet.getRows() - plateHeight; row++)
        {
            for (int col = 0; col < plateSheet.getColumns() - plateWidth; col++)
            {
                if (isPlateMatrix(plateSheet, row, col, plateHeight, plateWidth))
                {
                    // add one to row and col, since (row,col) is the index of the data grid
                    // where the first row is column labels and the first column is row labels.
                    return new Pair<Integer, Integer>(row + 1, col + 1);
                }
            }
        }
        return null;
    }

    private static boolean isPlateMatrix(Sheet plateSheet, int startRow, int startCol, int plateHeight, int plateWidth)
    {
        Cell[] row = plateSheet.getRow(startRow);
        // make sure that there are plate_width + 1 cells to the right of startCol:
        if (startCol + plateWidth + 1 > row.length)
            return false;

        Cell[] column = plateSheet.getColumn(startCol);
        // make sure that there are plate_width + 1 cells to the right of startCol:
        if (startRow + plateHeight + 1 > column.length)
            return false;

        // check for 1-12 in the row:
        for (int colIndex = startCol + 1; colIndex < startCol + plateWidth + 1; colIndex++)
        {
            Cell current = row[colIndex];
            String indexString = String.valueOf(colIndex - startCol);
            if (!StringUtils.equals(current.getContents(), indexString))
                return false;
        }

        char start = 'A';
        for (int rowIndex = startRow + 1; rowIndex < startRow + plateHeight + 1; rowIndex++)
        {
            Cell current = column[rowIndex];
            String indexString = String.valueOf(start++);
            if (!StringUtils.equals(current.getContents(), indexString))
                return false;
        }
        return true;
    }
}
