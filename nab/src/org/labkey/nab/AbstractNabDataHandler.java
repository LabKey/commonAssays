/*
 * Copyright (c) 2009-2010 LabKey Corporation
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

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.exp.*;
import org.labkey.api.exp.api.*;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.study.DilutionCurve;
import org.labkey.api.study.Plate;
import org.labkey.api.study.PlateService;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.WellData;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.assay.AssayDataType;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.AssayUrls;
import org.labkey.api.util.FileType;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: May 15, 2009
 */
public abstract class AbstractNabDataHandler extends AbstractExperimentDataHandler
{
    public static final String NAB_PROPERTY_LSID_PREFIX = "NabProperty";
    public static final String NAB_DATA_ROW_LSID_PREFIX = "AssayRunNabDataRow";

    public static final String NAB_INPUT_MATERIAL_DATA_PROPERTY = "SpecimenLsid";
    public static final String WELLGROUP_NAME_PROPERTY = "WellgroupName";
    public static final String FIT_ERROR_PROPERTY = "Fit Error";
    public static final String CURVE_IC_PREFIX = "Curve IC";
    public static final String POINT_IC_PREFIX = "Point IC";
    public static final String AUC_PREFIX = "AUC";
    public static final String pAUC_PREFIX = "PositiveAUC";
    public static final String OORINDICATOR_SUFFIX = "OORIndicator";
    public static final String DATA_ROW_LSID_PROPERTY = "Data Row LSID";
    public static final String AUC_PROPERTY_FORMAT = "0.000";

    private static final int START_ROW = 6; //0 based, row 7 inthe workshet
    private static final int START_COL = 0;
    public static final AssayDataType NAB_DATA_TYPE = new AssayDataType("AssayRunNabData", new FileType(".xls"));

    public static class MissingDataFileException extends ExperimentException
    {
        public MissingDataFileException(String message)
        {
            super(message);
        }
    }

    public interface NabDataFileParser
    {
        List<Map<String, Object>> getResults() throws ExperimentException;
    }

    public NabAssayRun getAssayResults(ExpRun run, User user) throws ExperimentException
    {
        return getAssayResults(run, user, null);
    }

    public NabAssayRun getAssayResults(ExpRun run, User user, DilutionCurve.FitType fit) throws ExperimentException
    {
        File dataFile = getDataFile(run);
        if (dataFile == null)
            throw new MissingDataFileException("Nab data file could not be found for run " + run.getName() + ".  Deleted from file system?");
        return getAssayResults(run, user, dataFile, fit);
    }

    public File getDataFile(ExpRun run)
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

    protected NabAssayRun getAssayResults(ExpRun run, User user, File dataFile, DilutionCurve.FitType fit) throws ExperimentException
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

        NabAssayRun assay = createNabAssayRun(provider, run, plate, user, sortedCutoffs, fit);
        assay.setCutoffFormats(cutoffs);
        assay.setWellGroupMaterialMapping(inputs);
        assay.setDataFile(dataFile);
        assay.setLockAxes(lockAxes);
        return assay;
    }

    protected abstract NabAssayRun createNabAssayRun(NabAssayProvider provider, ExpRun run, Plate plate,
                                  User user, List<Integer> cutoffs, DilutionCurve.FitType renderCurveFitType);

    private Map<WellGroup, ExpMaterial> getWellGroupMaterialPairings(Collection<ExpMaterial> sampleInputs, List<? extends WellGroup> wellgroups)
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

    private double[][] getCellValues(File dataFile, PlateTemplate nabTemplate) throws ExperimentException
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

    private void prepareWellGroups(Map<WellGroup, ExpMaterial> inputs, Map<String, DomainProperty> properties)
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

    private Pair<Integer, Integer> getPlateDataLocation(Sheet plateSheet, int plateHeight, int plateWidth)
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

    private boolean isPlateMatrix(Sheet plateSheet, int startRow, int startCol, int plateHeight, int plateWidth)
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

    public Map<DilutionSummary, NabAssayRun> getDilutionSummaries(User user, DilutionCurve.FitType fit, int... dataObjectIds) throws ExperimentException, SQLException
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

    public void importFile(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        ExpRun run = data.getRun();
        ExpProtocol protocol = run.getProtocol();
        NabDataFileParser parser = getDataFileParser(data, dataFile, info, log, context);

        importRows(data, run, protocol, parser.getResults());
    }

    protected void importRows(ExpData data, ExpRun run, ExpProtocol protocol, List<Map<String, Object>> rawData) throws ExperimentException
    {
        try
        {
            Container container = run.getContainer();
            OntologyManager.ensureObject(container, data.getLSID());
            Map<Integer, String> cutoffFormats = getCutoffFormats(protocol, run);

            for (Map<String, Object> group : rawData)
            {
                if (!group.containsKey(WELLGROUP_NAME_PROPERTY))
                    throw new ExperimentException("The row must contain a value for the well group name : " + WELLGROUP_NAME_PROPERTY);

                if (group.get(WELLGROUP_NAME_PROPERTY) == null)
                    throw new ExperimentException("The row must contain a value for the well group name : " + WELLGROUP_NAME_PROPERTY);

                String groupName = group.get(WELLGROUP_NAME_PROPERTY).toString();
                String dataRowLsid = getDataRowLSID(data, groupName).toString();

                OntologyManager.ensureObject(container, dataRowLsid,  data.getLSID());
                List<ObjectProperty> results = new ArrayList<ObjectProperty>();

                for (Map.Entry<String, Object> prop : group.entrySet())
                {
                    if (prop.getKey().equals(DATA_ROW_LSID_PROPERTY))
                        continue;

                    ObjectProperty objProp = getObjectProperty(container, protocol, dataRowLsid, prop.getKey(), prop.getValue(), cutoffFormats);
                    if (objProp != null)
                        results.add(objProp);
                }
                OntologyManager.insertProperties(container, dataRowLsid, results.toArray(new ObjectProperty[results.size()]));
            }
        }
        catch (ValidationException ve)
        {
            throw new ExperimentException(ve.getMessage(), ve);
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    protected ObjectProperty getObjectProperty(Container container, ExpProtocol protocol, String objectURI, String propertyName, Object value, Map<Integer, String> cutoffFormats)
    {
        if (isValidDataProperty(propertyName))
        {
            PropertyType type = PropertyType.STRING;
            String format = null;

            if (propertyName.equals(FIT_ERROR_PROPERTY))
            {
                type = PropertyType.DOUBLE;
                format = "0.0";
            }
            else if (propertyName.startsWith(AUC_PREFIX) || propertyName.startsWith(pAUC_PREFIX))
            {
                type = PropertyType.DOUBLE;
                format = AUC_PROPERTY_FORMAT;
            }
            else if (propertyName.startsWith(CURVE_IC_PREFIX))
            {
                Integer cutoff = getCutoffFromPropertyName(propertyName);
                if (cutoff != null)
                {
                    format = cutoffFormats.get(cutoff);
                    type = PropertyType.DOUBLE;
                }
            }
            else if (propertyName.startsWith(POINT_IC_PREFIX))
            {
                Integer cutoff = getCutoffFromPropertyName(propertyName);
                if (cutoff != null)
                {
                    format = cutoffFormats.get(cutoff);
                    type = PropertyType.DOUBLE;
                }
            }
            return getResultObjectProperty(container, protocol, objectURI, propertyName, value, type, format);
        }
        return null;
    }

    protected boolean isValidDataProperty(String propertyName)
    {
        if (DATA_ROW_LSID_PROPERTY.equals(propertyName)) return true;
        if (NAB_INPUT_MATERIAL_DATA_PROPERTY.equals(propertyName)) return true;
        if (WELLGROUP_NAME_PROPERTY.equals(propertyName)) return true;
        if (FIT_ERROR_PROPERTY.equals(propertyName)) return true;

        if (propertyName.startsWith(AUC_PREFIX)) return true;
        if (propertyName.startsWith(pAUC_PREFIX)) return true;
        if (propertyName.startsWith(CURVE_IC_PREFIX)) return true;
        if (propertyName.startsWith(POINT_IC_PREFIX)) return true;

        return false;
    }

    public Lsid getDataRowLSID(ExpData data, String wellGroupName)
    {
        Lsid dataRowLsid = new Lsid(data.getLSID());
        dataRowLsid.setNamespacePrefix(NAB_DATA_ROW_LSID_PREFIX);
        dataRowLsid.setObjectId(dataRowLsid.getObjectId() + "-" + wellGroupName);
        return dataRowLsid;
    }

    protected ObjectProperty getResultObjectProperty(Container container, ExpProtocol protocol, String objectURI,
                                                   String propertyName, Object value, PropertyType type, String format)
    {
        Lsid propertyURI = new Lsid(NAB_PROPERTY_LSID_PREFIX, protocol.getName(), propertyName);
        ObjectProperty prop = new ObjectProperty(objectURI, container, propertyURI.toString(), value, type, propertyName);
        prop.setFormat(format);
        return prop;
    }

    protected Map<Integer, String> getCutoffFormats(ExpProtocol protocol, ExpRun run)
    {
        NabAssayProvider provider = (NabAssayProvider) AssayService.get().getProvider(protocol);

        Map<String, DomainProperty> runProperties = new HashMap<String, DomainProperty>();
        for (DomainProperty column : provider.getRunDomain(protocol).getProperties())
            runProperties.put(column.getName(), column);
        for (DomainProperty column : provider.getBatchDomain(protocol).getProperties())
            runProperties.put(column.getName(), column);

        Map<Integer, String> cutoffs = new HashMap<Integer, String>();
        for (String cutoffPropName : NabAssayProvider.CUTOFF_PROPERTIES)
        {
            DomainProperty cutoffProp = runProperties.get(cutoffPropName);
            if (cutoffProp != null)
            {
                Integer cutoff = (Integer) run.getProperty(cutoffProp);
                if (cutoff != null)
                    cutoffs.put(cutoff, cutoffProp.getPropertyDescriptor().getFormat());
            }
        }

        if (cutoffs.isEmpty())
        {
            cutoffs.put(50, "0.000");
            cutoffs.put(80, "0.000");
        }
        return cutoffs;
    }

    public abstract NabDataFileParser getDataFileParser(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException;

    public void runMoved(ExpData newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user, int oldDataRowID) throws ExperimentException
    {
        throw new UnsupportedOperationException("Not Yet Implemented");
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

    public void deleteData(ExpData data, Container container, User user)
    {
        try
        {
            OntologyManager.deleteOntologyObject(data.getLSID(), container, true);
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    public String getPropertyName(String prefix, int cutoff, DilutionCurve.FitType type)
    {
        return getPropertyName(prefix + cutoff, type);
    }

    public String getPropertyName(String prefix, DilutionCurve.FitType type)
    {
        return prefix + "_" + type.getColSuffix();
    }

    public Integer getCutoffFromPropertyName(String propertyName)
    {
        if (propertyName.startsWith(CURVE_IC_PREFIX) && !propertyName.endsWith(OORINDICATOR_SUFFIX))
        {
            // parse out the cutoff number
            int idx = propertyName.indexOf('_');
            String num;
            if (idx != -1)
                num = propertyName.substring(CURVE_IC_PREFIX.length(), propertyName.indexOf('_'));
            else
                num = propertyName.substring(CURVE_IC_PREFIX.length());

            return Integer.valueOf(num);
        }
        else if (propertyName.startsWith(POINT_IC_PREFIX) && !propertyName.endsWith(OORINDICATOR_SUFFIX))
        {
            return Integer.valueOf(propertyName.substring(POINT_IC_PREFIX.length()));
        }
        return null;
    }
}
