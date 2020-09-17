package org.labkey.elisa;

import org.apache.log4j.Logger;
import org.labkey.api.assay.AssayUploadXarContext;
import org.labkey.api.assay.plate.Plate;
import org.labkey.api.assay.plate.PlateBasedAssayProvider;
import org.labkey.api.assay.plate.PlateService;
import org.labkey.api.assay.plate.PlateTemplate;
import org.labkey.api.assay.plate.Position;
import org.labkey.api.assay.plate.PositionImpl;
import org.labkey.api.assay.plate.Well;
import org.labkey.api.assay.plate.WellGroup;
import org.labkey.api.assay.plate.WellGroupTemplate;
import org.labkey.api.data.statistics.CurveFit;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ProvenanceService;
import org.labkey.api.reader.DataLoader;
import org.labkey.api.reader.DataLoaderFactory;
import org.labkey.api.reader.DataLoaderService;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.labkey.elisa.ElisaDataHandler.STANDARDS_WELL_GROUP_NAME;

public class HighThroughputImportHelper extends AbstractElisaImportHelper
{
    private static final Logger LOG = Logger.getLogger(HighThroughputImportHelper.class);
    private Map<String, AnalytePlate> _plateMap = new HashMap<>();
    private PlateTemplate _plateTemplate;

    public HighThroughputImportHelper(AssayUploadXarContext context, PlateBasedAssayProvider provider, ExpProtocol protocol, File dataFile) throws ExperimentException
    {
        super(context, provider, protocol, dataFile);
        ensureData();
    }

    private void ensureData() throws ExperimentException
    {
        try
        {
            _plateTemplate = _provider.getPlateTemplate(_protocol.getContainer(), _protocol);
            DataLoaderFactory factory = DataLoaderService.get().findFactory(_dataFile, null);
            DataLoader loader = factory.createLoader(_dataFile, true);

            String plateColumnName = "Plate Name";
            String sampleColumnName = "Sample";
            String wellLocationColumnName = "Well";
            String signalColumnName = "Signal";

            boolean hasPlateColumn = Arrays.stream(loader.getColumns()).anyMatch(c -> c.getColumnName().equalsIgnoreCase(plateColumnName));
            boolean hasSampleColumn = Arrays.stream(loader.getColumns()).anyMatch(c -> c.getColumnName().equalsIgnoreCase(sampleColumnName));
            boolean hasWellLocationColumn = Arrays.stream(loader.getColumns()).anyMatch(c -> c.getColumnName().equalsIgnoreCase(wellLocationColumnName));

            if (!hasPlateColumn)
                throw new ExperimentException("Sample metadata file does not contain required column \"" + plateColumnName + "\".");
            if (!hasSampleColumn)
                throw new ExperimentException("Sample metadata file does not contain required column \"" + sampleColumnName + "\".");
            if (!hasWellLocationColumn)
                throw new ExperimentException("Sample metadata file does not contain required column \"" + hasWellLocationColumn + "\".");

            for (Map<String, Object> row : loader)
            {
                String wellLocation = String.valueOf(row.get(wellLocationColumnName));
                String plateName = String.valueOf(row.get(plateColumnName));

                AnalytePlate analytePlate = _plateMap.computeIfAbsent(plateName, p -> new AnalytePlate(p, _plateTemplate));

                if (wellLocation != null && plateName != null)
                {
                    PositionImpl position = new PositionImpl(_container, wellLocation);
                    Integer spot = (Integer)row.get(ElisaAssayProvider.SPOT_PROPERTY);
                    Integer signal = (Integer)row.get(signalColumnName);
                    Double concentration = (Double)row.get(ElisaAssayProvider.CONCENTRATION_PROPERTY);

                    // store the raw signal values on a per analyte basis
                    analytePlate.setRawSignal(position, spot, signal);
                    analytePlate.setExtraProperties(row, position, spot);

                    if (concentration != null)
                    {
                        analytePlate.setStdConcentration(position, spot, concentration);
                    }
                }
                else
                    LOG.warn("No well location and plate name for row : " + row.toString());
            }
        }
        catch (IOException e)
        {
            throw new ExperimentException("Unable to parse sample properties file.  Please verify that the file is a valid Excel workbook.", e);
        }
    }

    @Override
    public Map<String, Object> createWellRow(String plateName, Integer spot, WellGroup replicate, Well well, Position position, CurveFit stdCurveFit, Map<String, ExpMaterial> materialMap)
    {
        Map<String, Object> row = super.createWellRow(plateName, spot, replicate, well, position, stdCurveFit, materialMap);

        row.put(ElisaAssayProvider.PLATE_PROPERTY, plateName);
        row.put(ElisaAssayProvider.SPOT_PROPERTY, spot);

        // add any extra properties
        AnalytePlate analytePlate = _plateMap.get(plateName);
        if (analytePlate != null)
        {
            // bail out if this isn't a valid row in the data file
            if (!analytePlate.hasData(position, spot))
                return Collections.emptyMap();

            row.putAll(analytePlate.getExtraProperties(position, spot));
        }

        // if this well is a sample well group add the material LSID
        Map<Position, String> specimenGroupMap = getSpecimenGroupMap();
        if (specimenGroupMap.containsKey(position))
        {
            String materialKey = getMaterialKey(plateName, specimenGroupMap.get(position));
            ExpMaterial material = materialMap.get(materialKey);
            if (material != null)
            {
                row.put(ElisaDataHandler.ELISA_INPUT_MATERIAL_DATA_PROPERTY, material.getLSID());
                // TODO: Support adding the material to existing provenance inputs on the row, if any
                if (_pvs != null)
                    row.put(ProvenanceService.PROVENANCE_INPUT_PROPERTY, List.of(material.getLSID()));
            }
        }
        return row;
    }

    /**
     * For multiple plate uploads, we need to create unique specimen LSIDs for each plate/sample
     * combination.
     */
    public static String getSpecimenGroupKey(String plateName, String sampleWellGroup)
    {
        return plateName + "-" + sampleWellGroup;
    }

    @Override
    public String getMaterialKey(String plateName, String sampleWellGroup)
    {
        return getSpecimenGroupKey(plateName, sampleWellGroup);
    }

    @Override
    public Set<String> getPlates()
    {
        return _plateMap.keySet();
    }

    @Override
    public Map<Integer, Plate> getAnalyteToPlate(String plateName) throws ExperimentException
    {
        Map<Integer, Plate> analyteToPlate = new HashMap<>();
        for (Map.Entry<Integer, double[][]> entry : _plateMap.get(plateName).getDataMap().entrySet())
        {
            Plate plate = PlateService.get().createPlate(_plateTemplate, entry.getValue(), null);
            analyteToPlate.put(entry.getKey(), plate);
        }
        return analyteToPlate;
    }

    @Override
    public Map<String, Double> getStandardConcentrations(String plateName, int analyteNum) throws ExperimentException
    {
        return _plateMap.get(plateName).getStdConcentrations(analyteNum);
    }

    private static class AnalytePlate
    {
        private Map<Integer, Map<String, Double>> _stdConcentrations = new HashMap<>();
        private Map<Integer, double[][]> _dataMap = new HashMap<>();
        private String _plateName;
        private PlateTemplate _plateTemplate;
        // contains the mapping of (well/analyte) to extra row data to merge during data import
        private Map<String, Map<String, Object>> _extraWellData = new HashMap<>();
        public static final String CONTROL_ID_COLUMN = "Sample";

        public AnalytePlate(String plateName, PlateTemplate plateTemplate)
        {
            _plateName = plateName;
            _plateTemplate = plateTemplate;
        }

        public Map<String, Double> getStdConcentrations(int analyteNum)
        {
            if (_stdConcentrations.containsKey(analyteNum))
                return _stdConcentrations.get(analyteNum);
            else
                return Collections.emptyMap();
        }

        public Map<Integer, double[][]> getDataMap()
        {
            return _dataMap;
        }

        public String getPlateName()
        {
            return _plateName;
        }

        public void setStdConcentration(Position position, Integer spot, Double concentration)
        {
            WellGroupTemplate replicateWellGroup = null;
            WellGroupTemplate controlWellGroup = null;

            for (WellGroupTemplate wellGroup : _plateTemplate.getWellGroups(position))
            {
                if (wellGroup.getType() == WellGroup.Type.REPLICATE)
                    replicateWellGroup = wellGroup;
                else if (wellGroup.getType() == WellGroup.Type.CONTROL)
                    controlWellGroup = wellGroup;
            }

            if (controlWellGroup != null && replicateWellGroup != null)
            {
                // limit to only the Standards controls
                if (controlWellGroup.getName().equals(STANDARDS_WELL_GROUP_NAME))
                {
                    Map<String, Double> concentrations = _stdConcentrations.computeIfAbsent(spot, s -> new HashMap<>());
                    concentrations.put(replicateWellGroup.getPositionDescription(), concentration);
                }
            }
        }

        public void setRawSignal(Position position, Integer spot, Integer signal)
        {
            if (position != null && spot != null && signal != null)
            {
                double[][] data = _dataMap.computeIfAbsent(spot, s -> new double[_plateTemplate.getRows()][_plateTemplate.getColumns()]);
                data[position.getRow()][position.getColumn()-1] = signal.doubleValue();
            }
        }

        public void setExtraProperties(Map<String, Object> row, PositionImpl position, Integer spot)
        {
            Map<String, Object> extraProperties = new HashMap<>();
            // need to adjust the column value to be 0 based to match the template locations
            position.setColumn(position.getColumn()-1);

            for (WellGroupTemplate wellGroup : _plateTemplate.getWellGroups(position))
            {
                if (wellGroup.getType() == WellGroup.Type.CONTROL)
                {
                    // get the control ID for control well groups
                    if (row.containsKey(CONTROL_ID_COLUMN))
                        extraProperties.put(ElisaAssayProvider.CONTROL_ID_PROPERTY, row.get(CONTROL_ID_COLUMN));

                    break;
                }
            }

            if (row.containsKey(ElisaAssayProvider.DILUTION_PROPERTY))
            {
                extraProperties.put(ElisaAssayProvider.DILUTION_PROPERTY, row.get(ElisaAssayProvider.DILUTION_PROPERTY));
            }

            _extraWellData.put(getWellAnalyteKey(position, spot), extraProperties);
        }

        public Map<String, Object> getExtraProperties(Position position, Integer spot)
        {
            String key = getWellAnalyteKey(position, spot);
            if (_extraWellData.containsKey(key))
            {
                return _extraWellData.get(key);
            }
            return Collections.emptyMap();
        }

        /**
         * Helper to determine if the data file contained a record for the well/analyte combination
         */
        public boolean hasData(Position position, Integer spot)
        {
            return _extraWellData.containsKey(getWellAnalyteKey(position, spot));
        }

        private String getWellAnalyteKey(Position position, Integer spot)
        {
            return position.getDescription() + "-" + spot;
        }
    }
}
