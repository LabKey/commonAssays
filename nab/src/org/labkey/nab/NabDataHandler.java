package org.labkey.nab;

import org.labkey.api.exp.api.*;
import org.labkey.api.exp.*;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.util.URLHelper;
import org.labkey.api.data.Container;
import org.labkey.api.security.User;
import org.labkey.api.study.*;
import org.labkey.api.study.assay.*;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
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
    public static final String NAB_DATA_LSID_PREFIX = "AssayRunNabData";
    public static final String NAB_DATA_ROW_LSID_PREFIX = "AssayRunNabDataRow";
    public static final String NAB_PROPERTY_LSID_PREFIX = "NabProperty";
    public static final String NAB_INPUT_MATERIAL_DATA_PROPERTY = "SpecimenLsid";
    public static final String WELLGROUP_NAME_PROPERTY = "WellgroupName";
    private static final int START_ROW = 6; //0 based, row 7 inthe workshet
    private static final int START_COL = 0;

    public void importFile(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        try
        {
            ExpRun run = data.getRun();
            ExpProtocol protocol = ExperimentService.get().getExpProtocol(run.getProtocol().getLSID());
            Container container = data.getContainer();
            Luc5Assay assayResults = getAssayResults(run, dataFile);
            OntologyManager.ensureObject(container.getId(), data.getLSID());
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

                OntologyManager.ensureObject(container.getId(), dataRowLsid.toString(),  data.getLSID());
                OntologyManager.insertProperties(container.getId(), results.toArray(new ObjectProperty[results.size()]), dataRowLsid.toString());
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static List<DilutionSummary> getDilutionSummaries(int... dataObjectIds) throws ExperimentException, SQLException
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
                assay = getAssayResults(dataObject.getRun());
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
                             ExpProtocol protocol, Container container, String format, List<ObjectProperty> results)
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
        ExpData[] outputDatas = run.getOutputDatas(new DataType(NAB_DATA_LSID_PREFIX));
        if (outputDatas == null || outputDatas.length != 1)
            throw new IllegalStateException("Nab runs should have a single data output.");
        File dataFile = outputDatas[0].getFile();
        if (!dataFile.exists())
            return null;
        return dataFile;
    }

    public static Luc5Assay getAssayResults(ExpRun run) throws ExperimentException
    {
        File dataFile = getDataFile(run);
        if (dataFile == null)
            throw new ExperimentException("Nab data file could not be found for run " + run.getName() + ".  Deleted from file system?");
        return getAssayResults(run, dataFile);
    }

    private static Luc5Assay getAssayResults(ExpRun run, File dataFile) throws ExperimentException
    {
        ExpProtocol protocol = ExperimentService.get().getExpProtocol(run.getProtocol().getLSID());
        Container container = run.getContainer();
        AssayProvider provider = AssayService.get().getProvider(protocol);
        PlateTemplate nabTemplate = provider.getPlateTemplate(container, protocol);

        Map<String, PropertyDescriptor> runProperties = new HashMap<String, PropertyDescriptor>();
        for (PropertyDescriptor column : provider.getRunPropertyColumns(protocol))
            runProperties.put(column.getName(), column);
        for (PropertyDescriptor column : provider.getUploadSetColumns(protocol))
            runProperties.put(column.getName(), column);

        Map<Integer, String> cutoffs = new HashMap<Integer, String>();
        for (String cutoffPropName : NabAssayProvider.CUTOFF_PROPERTIES)
        {
            PropertyDescriptor cutoffProp = runProperties.get(cutoffPropName);
            Integer cutoff = (Integer) run.getProperty(cutoffProp);
            if (cutoff != null)
                cutoffs.put(cutoff, cutoffProp.getFormat());
        }

        if (cutoffs.isEmpty())
        {
            cutoffs.put(50, "0.000");
            cutoffs.put(80, "0.000");
        }

        double[][] cellValues = getCellValues(dataFile, nabTemplate);

        // UNDONE: Eliminate cast to NabAssayProvider here: there needs to be a more general way of retrieving
        // sample preparation information from a protocol/provider.
        PropertyDescriptor[] sampleProperties = ((PlateBasedAssayProvider) provider).getSampleWellGroupColumns(protocol);
        Map<String, PropertyDescriptor> samplePropertyMap = new HashMap<String, PropertyDescriptor>();
        for (PropertyDescriptor sampleProperty : sampleProperties)
            samplePropertyMap.put(sampleProperty.getName(), sampleProperty);


        Plate plate = PlateService.get().createPlate(nabTemplate, cellValues);
        List<? extends WellGroup> specimenGroups = plate.getWellGroups(WellGroup.Type.SPECIMEN);
        Collection<ExpMaterial> sampleInputs = run.getMaterialInputs().keySet();
        Map<WellGroup, ExpMaterial> inputs = getWellGroupMaterialPairings(sampleInputs, specimenGroups);

        prepareWellGroups(inputs, samplePropertyMap);

        List<Integer> sortedCutoffs = new ArrayList<Integer>(cutoffs.keySet());
        Collections.sort(sortedCutoffs);

        PropertyDescriptor curveFitPd = runProperties.get(NabAssayProvider.CURVE_FIT_METHOD_PROPERTY_NAME);
        DilutionCurve.FitType fit = DilutionCurve.FitType.FIVE_PARAMETER;
        if (curveFitPd != null)
        {
            Object value = run.getProperty(curveFitPd);
            if (value != null)
                fit = DilutionCurve.FitType.fromLabel((String) value);
        }

        boolean lockAxes = false;
        PropertyDescriptor lockAxesProperty = runProperties.get(NabAssayProvider.LOCK_AXES_PROPERTY_NAME);
        if (lockAxesProperty != null)
        {
            Boolean lock = (Boolean) run.getProperty(lockAxesProperty);
            if (lock != null)
                lockAxes = lock.booleanValue();
        }

        Luc5Assay assay = new Luc5Assay(plate, sortedCutoffs, fit);
        assay.setCutoffFormats(cutoffs);
        assay.setWellGroupMaterialMapping(inputs);
        assay.setDataFile(dataFile);
        assay.setLockAxes(lockAxes);
        return assay;
    }

    public static Map<WellGroup, Map<PropertyDescriptor, Object>> getWellGroupProperties(ExpRun run) throws ExperimentException
    {
        ExpProtocol protocol = run.getProtocol();
        AssayProvider provider = AssayService.get().getProvider(protocol);
        Luc5Assay assay = getAssayResults(run);
        List<WellGroup> wellgroups = new ArrayList<WellGroup>();
        for (DilutionSummary summary : assay.getSummaries())
            wellgroups.add(summary.getWellGroup());
        Map<WellGroup, ExpMaterial> materials = getWellGroupMaterialPairings(run.getMaterialInputs().keySet(), wellgroups);
        PropertyDescriptor[] sampleProperties = ((PlateBasedAssayProvider) provider).getSampleWellGroupColumns(protocol);
        Map<WellGroup, Map<PropertyDescriptor, Object>> ret = new HashMap<WellGroup, Map<PropertyDescriptor, Object>>();
        for (Map.Entry<WellGroup,  ExpMaterial> entry : materials.entrySet())
        {
            Map<PropertyDescriptor, Object> properties = new HashMap<PropertyDescriptor, Object>();
            for (PropertyDescriptor pd : sampleProperties)
                properties.put(pd, entry.getValue().getProperty(pd));
            ret.put(entry.getKey(), properties);
        }
        return ret;
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

        if (workbook.getSheets().length < 2)
            throw new ExperimentException(dataFile.getName() + " does not appear to be a valid data file: did not find expected sheet 2.");
        Sheet plateSheet = workbook.getSheet(1);

        if (nabTemplate.getRows() + START_ROW > plateSheet.getRows() || nabTemplate.getColumns() + START_COL > plateSheet.getColumns())
        {
            throw new ExperimentException(dataFile.getName() + " does not appear to be a valid data file: expected " +
                    (nabTemplate.getRows() + START_ROW) + " rows and " + (nabTemplate.getColumns() + START_COL) + " colums, but found "+
                    plateSheet.getRows() + " rows and " + plateSheet.getColumns() + " colums.");
        }

        for (int row = 0; row < nabTemplate.getRows(); row++)
        {
            for (int col = 0; col < nabTemplate.getColumns(); col++)
            {
                Cell cell = plateSheet.getCell(col + START_COL, row + START_ROW);
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

    private static void prepareWellGroups(Map<WellGroup, ExpMaterial> inputs, Map<String, PropertyDescriptor> properties)
    {
        for (Map.Entry<WellGroup, ExpMaterial> input : inputs.entrySet())
        {
            WellGroup group = input.getKey();
            ExpMaterial sampleInput = input.getValue();
            for (PropertyDescriptor property : properties.values())
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
        ObjectProperty prop = new ObjectProperty(objectURI, container.getId(), propertyURI.toString(), value, type, propertyName);
        prop.setFormat(format);
        return prop;
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

    public void deleteData(ExpData data, Container container, User user) throws ExperimentException
    {
        try
        {
            OntologyManager.deleteOntologyObject(data.getLSID(), container, true);
        }
        catch (SQLException e)
        {
            throw new ExperimentException(e);
        }
    }

    public void runMoved(ExpData newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user, int oldDataRowID) throws ExperimentException
    {
        throw new UnsupportedOperationException("Not Yet Implemented");
    }

    public Priority getPriority(ExpData data)
    {
        Lsid lsid = new Lsid(data.getLSID());
        if (NAB_DATA_LSID_PREFIX.equals(lsid.getNamespacePrefix()))
        {
            return Priority.HIGH;
        }
        return null;
    }
}
