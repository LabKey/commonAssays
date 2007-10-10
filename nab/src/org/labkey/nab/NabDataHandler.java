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
    private static final int START_ROW = 6; //0 based, row 7 inthe workshet
    private static final int START_COL = 0;

    public void importFile(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        try
        {
            ExpRun run = data.getSourceApplication().getRun();
            ExpProtocol protocol = ExperimentService.get().getExpProtocol(run.getProtocol().getLSID());
            Container container = data.getContainer();
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


            Plate plate = PlateService.get().createPlate(nabTemplate, run.getLSID(), cellValues);
            List<? extends WellGroup> specimenGroups = plate.getWellGroups(WellGroup.Type.SPECIMEN);
            List<ExpMaterial> sampleInputs = run.getMaterialInputs();
            Map<WellGroup, ExpMaterial> inputs = getWellGroupMaterialPairings(sampleInputs, specimenGroups);

            prepareWellGroups(inputs, samplePropertyMap);

            List<Integer> sortedCutoffs = new ArrayList<Integer>(cutoffs.keySet());
            Collections.sort(sortedCutoffs);
            Luc5Assay assayResults = new Luc5Assay(plate, sortedCutoffs);
            OntologyManager.ensureObject(container.getId(), data.getLSID());
            for (int summaryIndex = 0; summaryIndex < assayResults.getSummaries().length; summaryIndex++)
            {
                DilutionSummary dilution = assayResults.getSummaries()[summaryIndex];
                WellGroup group = dilution.getWellGroup();
                ExpMaterial sampleInput = inputs.get(group);
                List<ObjectProperty> results = new ArrayList<ObjectProperty>();
                Lsid dataRowLsid = new Lsid(data.getLSID());
                dataRowLsid.setNamespacePrefix(NAB_DATA_ROW_LSID_PREFIX);
                dataRowLsid.setObjectId(dataRowLsid.getObjectId() + "-" + group.getName());
                for (Integer cutoff : sortedCutoffs)
                {
                    String format = cutoffs.get(cutoff);
                    results.add(getResultObjectProperty(container, protocol, dataRowLsid.toString(), "Curve IC" + cutoff,
                            dilution.getCutoffDilution(cutoff / 100.0), PropertyType.DOUBLE, format));
                    results.add(getResultObjectProperty(container, protocol, dataRowLsid.toString(), "Point IC" + cutoff,
                            dilution.getInterpolatedCutoffDilution(cutoff / 100.0), PropertyType.DOUBLE, format));
                }

                results.add(getResultObjectProperty(container, protocol, dataRowLsid.toString(), "Fit Error", dilution.getFitError(), PropertyType.DOUBLE, "0.0"));
                results.add(getResultObjectProperty(container, protocol, dataRowLsid.toString(), NAB_INPUT_MATERIAL_DATA_PROPERTY, sampleInput.getLSID(), PropertyType.STRING));

                OntologyManager.ensureObject(container.getId(), dataRowLsid.toString(),  data.getLSID());
                OntologyManager.insertProperties(container.getId(), results.toArray(new ObjectProperty[results.size()]), dataRowLsid.toString());
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        catch (BiffException e)
        {
            throw new RuntimeException(e);
        }
    }

    private Map<WellGroup, ExpMaterial> getWellGroupMaterialPairings(List<ExpMaterial> sampleInputs, List<? extends WellGroup> wellgroups)
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

    private double[][] getCellValues(File dataFile, PlateTemplate nabTemplate) throws IOException, BiffException
    {
        WorkbookSettings settings = new WorkbookSettings();
        settings.setGCDisabled(true);
        Workbook workbook = Workbook.getWorkbook(dataFile, settings);
        double[][] cellValues = new double[nabTemplate.getRows()][nabTemplate.getColumns()];

        Sheet plateSheet = workbook.getSheet(1);
        for (int row = 0; row < nabTemplate.getRows(); row++)
        {
            for (int col = 0; col < nabTemplate.getColumns(); col++)
            {
                Cell cell = plateSheet.getCell(col + START_COL, row + START_ROW);
                String cellContents = cell.getContents();
                cellValues[row][col] = Double.parseDouble(cellContents);
            }
        }
        return cellValues;
    }

    private void prepareWellGroups(Map<WellGroup, ExpMaterial> inputs, Map<String, PropertyDescriptor> properties)
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
        throw new UnsupportedOperationException("Not Yet Implemented");
    }

    public void deleteData(ExpData data, Container container, User user) throws ExperimentException
    {
        throw new UnsupportedOperationException("Not Yet Implemented");
    }

    public void runMoved(ExpData newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user, int oldDataRowID) throws ExperimentException
    {
        throw new UnsupportedOperationException("Not Yet Implemented");
    }

    public Priority getPriority(ExpData data)
    {
        Lsid lsid = new Lsid(data.getLSID());
        if (lsid.getNamespacePrefix().equals(NAB_DATA_LSID_PREFIX))
        {
            return Priority.HIGH;
        }
        return null;
    }
}
