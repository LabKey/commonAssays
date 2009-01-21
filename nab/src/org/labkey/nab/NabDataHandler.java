/*
 * Copyright (c) 2007-2009 LabKey Corporation
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

import org.labkey.api.exp.api.*;
import org.labkey.api.exp.*;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.view.ActionURL;
import org.labkey.api.data.Container;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.security.User;
import org.labkey.api.study.*;
import org.labkey.api.study.assay.*;
import org.labkey.api.query.ValidationException;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.common.util.Pair;
import org.apache.log4j.Logger;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.sql.SQLException;

import jxl.WorkbookSettings;
import jxl.Workbook;
import jxl.Sheet;
import jxl.Cell;
import jxl.read.biff.BiffException;

/**
 * User: brittp
 * Date: Sep 21, 2007
 * Time: 3:21:18 PM
 */
public class NabDataHandler extends AbstractExperimentDataHandler
{
    public static final DataType NAB_DATA_TYPE = new DataType("AssayRunNabData");
    public static final String NAB_DATA_ROW_LSID_PREFIX = "AssayRunNabDataRow";
    public static final String NAB_PROPERTY_LSID_PREFIX = "NabProperty";
    public static final String NAB_INPUT_MATERIAL_DATA_PROPERTY = "SpecimenLsid";
    public static final String WELLGROUP_NAME_PROPERTY = "WellgroupName";
    private static final int START_ROW = 6; //0 based, row 7 inthe workshet
    private static final int START_COL = 0;
    private static final int PLATE_WIDTH = 12;
    private static final int PLATE_HEIGHT = 8;

    public void importFile(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        try
        {
            ExpRun run = data.getRun();
            ExpProtocol protocol = ExperimentService.get().getExpProtocol(run.getProtocol().getLSID());
            Container container = data.getContainer();
            Luc5Assay assayResults = getAssayResults(run, info.getUser(), dataFile);
            OntologyManager.ensureObject(container, data.getLSID());
            for (int summaryIndex = 0; summaryIndex < assayResults.getSummaries().length; summaryIndex++)
            {
                DilutionSummary dilution = assayResults.getSummaries()[summaryIndex];
                WellGroup group = dilution.getWellGroup();
                ExpMaterial sampleInput = assayResults.getMaterial(group);
                List<ObjectProperty> results = new ArrayList<ObjectProperty>();
                Lsid dataRowLsid = new Lsid(data.getLSID());
                dataRowLsid.setNamespacePrefix(NAB_DATA_ROW_LSID_PREFIX);
                dataRowLsid.setObjectId(dataRowLsid.getObjectId() + "-" + group.getName());
                for (Integer cutoff : assayResults.getCutoffs())
                {
                    String format = assayResults.getCutoffFormats().get(cutoff);
                    saveICValue("Curve IC" + cutoff, dilution.getCutoffDilution(cutoff / 100.0),
                            dilution, dataRowLsid, protocol, container, format, results);

                    saveICValue("Point IC" + cutoff, dilution.getInterpolatedCutoffDilution(cutoff / 100.0),
                            dilution, dataRowLsid, protocol, container, format, results);
                }

                results.add(getResultObjectProperty(container, protocol, dataRowLsid.toString(), "Fit Error", dilution.getFitError(), PropertyType.DOUBLE, "0.0"));
                results.add(getResultObjectProperty(container, protocol, dataRowLsid.toString(), NAB_INPUT_MATERIAL_DATA_PROPERTY, sampleInput.getLSID(), PropertyType.STRING));
                results.add(getResultObjectProperty(container, protocol, dataRowLsid.toString(), WELLGROUP_NAME_PROPERTY, group.getName(), PropertyType.STRING));

                OntologyManager.ensureObject(container, dataRowLsid.toString(),  data.getLSID());
                OntologyManager.insertProperties(container, dataRowLsid.toString(), results.toArray(new ObjectProperty[results.size()]));
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
        catch (DilutionCurve.FitFailedException e)
        {
            throw new ExperimentException(e.getMessage(), e);
        }
    }

    public static List<DilutionSummary> getDilutionSummaries(User user, int... dataObjectIds) throws ExperimentException, SQLException
    {
        Map<String, Luc5Assay> dataToAssay = new HashMap<String, Luc5Assay>();
        List<DilutionSummary> summaries = new ArrayList<DilutionSummary>();
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
            Luc5Assay assay = dataToAssay.get(dataLsid);
            if (assay == null)
            {
                ExpData dataObject = ExperimentService.get().getExpData(dataLsid);
                if (dataObject == null)
                    continue;
                assay = getAssayResults(dataObject.getRun(), user);
                if (assay == null)
                    continue;
                dataToAssay.put(dataLsid, assay);
            }

            for (DilutionSummary summary : assay.getSummaries())
            {
                if (wellgroupName.equals(summary.getWellGroup().getName()))
                {
                    summaries.add(summary);
                    break;
                }
            }
        }
        return summaries;
    }

    private void saveICValue(String name, double icValue, DilutionSummary dilution, Lsid dataRowLsid,
                             ExpProtocol protocol, Container container, String format, List<ObjectProperty> results) throws DilutionCurve.FitFailedException
    {
        String outOfRange = null;
        if (Double.NEGATIVE_INFINITY == icValue)
        {
            outOfRange = "<";
            icValue = dilution.getMinDilution();
        }
        else if (Double.POSITIVE_INFINITY == icValue)
        {
            outOfRange = ">";
            icValue = dilution.getMaxDilution();
        }
        ObjectProperty curveIC = getResultObjectProperty(container, protocol, dataRowLsid.toString(), name,
                icValue, PropertyType.DOUBLE, format);
        results.add(curveIC);
        results.add(getResultObjectProperty(container, protocol, dataRowLsid.toString(), curveIC.getName() + "OORIndicator",
                outOfRange, PropertyType.STRING, null));
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
        File dataFile = getDataFile(run);
        if (dataFile == null)
            throw new MissingDataFileException("Nab data file could not be found for run " + run.getName() + ".  Deleted from file system?");
        return getAssayResults(run, user, dataFile);
    }

    private static NabAssayRun getAssayResults(ExpRun run, User user, File dataFile) throws ExperimentException
    {
        ExpProtocol protocol = ExperimentService.get().getExpProtocol(run.getProtocol().getLSID());
        Container container = run.getContainer();
        NabAssayProvider provider = (NabAssayProvider) AssayService.get().getProvider(protocol);
        PlateTemplate nabTemplate = provider.getPlateTemplate(container, protocol);

        Map<String, DomainProperty> runProperties = new HashMap<String, DomainProperty>();
        for (DomainProperty column : provider.getRunDomain(protocol).getProperties())
            runProperties.put(column.getName(), column);
        for (DomainProperty column : provider.getUploadSetDomain(protocol).getProperties())
            runProperties.put(column.getName(), column);

        Map<Integer, String> cutoffs = new HashMap<Integer, String>();
        for (String cutoffPropName : NabAssayProvider.CUTOFF_PROPERTIES)
        {
            DomainProperty cutoffProp = runProperties.get(cutoffPropName);
            Integer cutoff = (Integer) run.getProperty(cutoffProp);
            if (cutoff != null)
                cutoffs.put(cutoff, cutoffProp.getPropertyDescriptor().getFormat());
        }

        if (cutoffs.isEmpty())
        {
            cutoffs.put(50, "0.000");
            cutoffs.put(80, "0.000");
        }

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
        DilutionCurve.FitType fit = DilutionCurve.FitType.FIVE_PARAMETER;
        if (curveFitPd != null)
        {
            Object value = run.getProperty(curveFitPd);
            if (value != null)
                fit = DilutionCurve.FitType.fromLabel((String) value);
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
            dataLocation = getPlateDataLocation(plateSheet);
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
            Double dilution = (Double) sampleInput.getProperty(properties.get(NabAssayProvider.SAMPLE_INITIAL_DILUTION_PROPERTY_NAME));
            Double factor = (Double) sampleInput.getProperty(properties.get(NabAssayProvider.SAMPLE_DILUTION_FACTOR_PROPERTY_NAME));
            String methodString = (String) sampleInput.getProperty(properties.get(NabAssayProvider.SAMPLE_METHOD_PROPERTY_NAME));
            SampleInfo.Method method = SampleInfo.Method.valueOf(methodString);
            for (int groupIndex = wells.size() - 1; groupIndex >= 0; groupIndex--)
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


    private ObjectProperty getResultObjectProperty(Container container, ExpProtocol protocol, String objectURI,
                                                   String propertyName, Object value, PropertyType type)
    {
        return getResultObjectProperty(container, protocol, objectURI, propertyName, value, type, null);
    }

    private ObjectProperty getResultObjectProperty(Container container, ExpProtocol protocol, String objectURI,
                                                   String propertyName, Object value, PropertyType type, String format)
    {
        Lsid propertyURI = new Lsid(NAB_PROPERTY_LSID_PREFIX, protocol.getName(), propertyName);
        ObjectProperty prop = new ObjectProperty(objectURI, container, propertyURI.toString(), value, type, propertyName);
        prop.setFormat(format);
        return prop;
    }

    public ActionURL getContentURL(Container container, ExpData data)
    {
        ExpRun run = data.getRun();
        if (run != null)
        {
            ExpProtocol protocol = run.getProtocol();
            ExpProtocol p = ExperimentService.get().getExpProtocol(protocol.getRowId());
            return PageFlowUtil.urlProvider(AssayUrls.class).getAssayDataURL(container, p, run.getRowId());
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

    public void runMoved(ExpData newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user, int oldDataRowID) throws ExperimentException
    {
        throw new UnsupportedOperationException("Not Yet Implemented");
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

    private static Pair<Integer, Integer> getPlateDataLocation(Sheet plateSheet)
    {
        for (int row = 0; row < plateSheet.getRows() - PLATE_HEIGHT; row++)
        {
            for (int col = 0; col < plateSheet.getColumns() - PLATE_WIDTH; col++)
            {
                if (isPlateMatrix(plateSheet, row, col))
                {
                    // add one to row and col, since (row,col) is the index of the data grid
                    // where the first row is column labels and the first column is row labels.
                    return new Pair<Integer, Integer>(row + 1, col + 1);
                }
            }
        }
        return null;
    }

    private static boolean isPlateMatrix(Sheet plateSheet, int startRow, int startCol)
    {
        Cell[] row = plateSheet.getRow(startRow);
        // make sure that there are plate_width + 1 cells to the right of startCol:
        if (startCol + PLATE_WIDTH + 1 > row.length)
            return false;

        Cell[] column = plateSheet.getColumn(startCol);
        // make sure that there are plate_width + 1 cells to the right of startCol:
        if (startRow + PLATE_HEIGHT + 1 > column.length)
            return false;

        // check for 1-12 in the row:
        for (int colIndex = startCol + 1; colIndex < startCol + PLATE_WIDTH + 1; colIndex++)
        {
            Cell current = row[colIndex];
            String indexString = String.valueOf(colIndex - startCol);
            if (!StringUtils.equals(current.getContents(), indexString))
                return false;
        }

        char start = 'A';
        for (int rowIndex = startRow + 1; rowIndex < startRow + PLATE_HEIGHT + 1; rowIndex++)
        {
            Cell current = column[rowIndex];
            String indexString = String.valueOf(start++);
            if (!StringUtils.equals(current.getContents(), indexString))
                return false;
        }
        return true;
    }
}
