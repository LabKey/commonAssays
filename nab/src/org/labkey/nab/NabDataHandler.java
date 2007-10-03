package org.labkey.nab;

import org.labkey.api.exp.api.*;
import org.labkey.api.exp.*;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.util.URLHelper;
import org.labkey.api.data.Container;
import org.labkey.api.security.User;
import org.labkey.api.study.*;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;
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
            User user = info.getUser();
            PlateTemplate nabTemplate = provider.getPlateTemplate(container, protocol);
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

            // create plate, and set its properties:
            Plate plate = PlateService.get().createPlate(nabTemplate, run.getLSID(), cellValues);

            Map<String, PropertyDescriptor> runProperties = new HashMap<String, PropertyDescriptor>();
            for (PropertyDescriptor column : provider.getRunPropertyColumns(protocol))
                runProperties.put(column.getName(), column);
            for (PropertyDescriptor column : provider.getUploadSetColumns(protocol))
                runProperties.put(column.getName(), column);

            List<Integer> cutoffs = new ArrayList<Integer>();
            Set<String> cutoffPropertyNames = new HashSet<String>();
            for (String cutoffPropName : NabAssayProvider.CUTOFF_PROPERTIES)
            {
                cutoffPropertyNames.add(cutoffPropName);
                PropertyDescriptor cutoffProp = runProperties.get(cutoffPropName);
                Integer cutoff = (Integer) run.getProperty(cutoffProp);
                if (cutoff != null)
                    cutoffs.add(cutoff);
            }

            if (cutoffs.isEmpty())
            {
                cutoffs.add(50);
                cutoffs.add(80);
            }
            Collections.sort(cutoffs);
            StringBuilder cutoffString = new StringBuilder();
            for (int i = 0; i < cutoffs.size(); i++)
            {
                if (i > 0)
                    cutoffString.append(",");
                cutoffString.append(cutoffs.get(i));
            }
            plate.setProperty("Cutoffs", cutoffString);
            for (PropertyDescriptor column : runProperties.values())
            {
                if (!cutoffPropertyNames.contains(column.getName()))
                {
                    Object value = run.getProperty(column);
                    plate.setProperty(column.getName(), value);
                }
            }

            ExpData[] sampleInputs = run.getInputDatas(null, ExpProtocol.ApplicationType.ExperimentRun);
            // set sample group properties:
            List<? extends WellGroup> specimenGroups = plate.getWellGroups(WellGroup.Type.SPECIMEN);
            // UNDONE: Eliminate cast to NabAssayProvider here: there needs to be a more general way of retrieving
            // sample preparation information from a protocol/provider.
            PropertyDescriptor[] sampleProperties = ((NabAssayProvider) provider).getSampleWellGroupColumns(protocol);
            Map<String, PropertyDescriptor> samplePropertyMap = new HashMap<String, PropertyDescriptor>();
            for (PropertyDescriptor sampleProperty : sampleProperties)
                samplePropertyMap.put(sampleProperty.getName(), sampleProperty);

            for (int i = 0; i < sampleInputs.length; i++)
            {
                WellGroup group = specimenGroups.get(i);
                ExpData sampleInput = sampleInputs[i];
                for (PropertyDescriptor property : sampleProperties)
                    group.setProperty(property.getName(), sampleInput.getProperty(property));

                List<WellData> wells = group.getWellData(true);
                boolean first = true;
                Double dilution = (Double) sampleInput.getProperty(samplePropertyMap.get(NabAssayProvider.SAMPLE_INITIAL_DILUTION_PROPERTY_NAME));
                Double factor = (Double) sampleInput.getProperty(samplePropertyMap.get(NabAssayProvider.SAMPLE_DILUTION_FACTOR_PROPERTY_NAME));
                String methodString = (String) sampleInput.getProperty(samplePropertyMap.get(NabAssayProvider.SAMPLE_METHOD_PROPERTY_NAME));
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

            Luc5Assay temp = new Luc5Assay(plate, cutoffs);
            for (int summaryIndex = 0; summaryIndex < temp.getSummaries().length; summaryIndex++)
            {
                DilutionSummary dilution = temp.getSummaries()[summaryIndex];
                WellGroup group = dilution.getWellGroup();
                for (int cutoff : cutoffs)
                {
                    group.setProperty("Curve IC" + cutoff, dilution.getCutoffDilution((double) cutoff / 100.0));
                    group.setProperty("Point IC" + cutoff, dilution.getInterpolatedCutoffDilution((double) cutoff / 100.0));
                }
                group.setProperty("FitError", dilution.getFitError());

                String sampleRowLsid = data.getLSID() + "-sample" + summaryIndex;
                OntologyManager.ensureObject(container.getId(), sampleRowLsid,  data.getLSID());
                ObjectProperty[] properties = new ObjectProperty[group.getPropertyNames().size()];
                int propIndex = 0;
                for (String propName : group.getPropertyNames())
                    properties[propIndex++] = new ObjectProperty(sampleRowLsid, container.getId(), sampleRowLsid + "#" + propName, group.getProperty(propName));
                OntologyManager.insertProperties(container.getId(), properties, sampleRowLsid);
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
