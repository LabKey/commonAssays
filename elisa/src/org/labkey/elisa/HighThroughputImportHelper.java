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
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.reader.DataLoader;
import org.labkey.api.reader.DataLoaderFactory;
import org.labkey.api.reader.DataLoaderService;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
            String signalColumnName = "Signal";

            ElisaSampleFilePropertyHelper.validateRequiredColumns(loader.getColumns());
            List<? extends DomainProperty> resultDomain = _provider.getResultsDomain(_protocol).getProperties();

            for (Map<String, Object> row : loader)
            {
                String wellLocation = String.valueOf(row.get(ElisaSampleFilePropertyHelper.WELL_LOCATION_COLUMN_NAME));
                String plateName = String.valueOf(row.get(ElisaSampleFilePropertyHelper.PLATE_COLUMN_NAME));

                AnalytePlate analytePlate = _plateMap.computeIfAbsent(plateName, p -> new AnalytePlate(p, _plateTemplate));

                if (wellLocation != null && plateName != null)
                {
                    PositionImpl position = new PositionImpl(_container, wellLocation);
                    Integer spot = (Integer)row.get(ElisaAssayProvider.SPOT_PROPERTY);
                    Integer signal = (Integer)row.get(signalColumnName);
                    Double concentration = (Double)row.get(ElisaAssayProvider.CONCENTRATION_PROPERTY);

                    // store the raw signal values on a per analyte basis
                    analytePlate.setRawSignal(position, spot, signal);
                    analytePlate.setExtraProperties(row, position, spot, resultDomain);

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
    public Map<String, Object> createWellRow(Domain domain, String plateName, Integer spot, WellGroup parentWellGroup, WellGroup replicate,
                                             Well well, Position position, CurveFit stdCurveFit, Map<String, ExpMaterial> materialMap)
    {
        Map<String, Object> row = super.createWellRow(domain, plateName, spot, parentWellGroup, replicate, well, position, stdCurveFit, materialMap);

        row.put(ElisaAssayProvider.PLATE_PROPERTY, plateName);
        row.put(ElisaAssayProvider.SPOT_PROPERTY, spot);

        // add any extra properties
        row.putAll(getExtraProperties(plateName, position, spot));

        // if this well is a sample well group add the material LSID
        Map<Position, String> specimenGroupMap = getSpecimenGroupMap();
        if (specimenGroupMap.containsKey(position))
        {
            String materialKey = getMaterialKey(plateName, spot, specimenGroupMap.get(position));
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
     * For multiple plate uploads, we need to create unique specimen LSIDs for each plate/sample/analyte
     * combination.
     */
    public static String getSpecimenGroupKey(String plateName, Integer analyteNum, String sampleWellGroup)
    {
        return plateName + "-" + analyteNum + "-" + sampleWellGroup;
    }

    @Override
    public String getMaterialKey(String plateName, Integer analyteNum, String sampleWellGroup)
    {
        return getSpecimenGroupKey(plateName, analyteNum, sampleWellGroup);
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

    @Override
    public Map<String, Object> getExtraProperties(String plateName, Position position, Integer spot)
    {
        AnalytePlate analytePlate = _plateMap.get(plateName);
        if (analytePlate != null)
        {
            // bail out if this isn't a valid row in the data file
            if (!analytePlate.hasData(position, spot))
                return Collections.emptyMap();

            return analytePlate.getExtraProperties(position, spot);
        }
        return Collections.emptyMap();
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
            for (WellGroupTemplate wellGroup : _plateTemplate.getWellGroups(position))
            {
                if (wellGroup.getType() == WellGroup.Type.REPLICATE)
                {
                    Map<String, Double> concentrations = _stdConcentrations.computeIfAbsent(spot, s -> new HashMap<>());
                    concentrations.put(wellGroup.getPositionDescription(), concentration);

                    return;
                }
            }
        }

        public void setRawSignal(Position position, Integer spot, Integer signal) throws ExperimentException
        {
            if (position != null && spot != null && signal != null)
            {
                double[][] data = _dataMap.computeIfAbsent(spot, s -> new double[_plateTemplate.getRows()][_plateTemplate.getColumns()]);

                if (position.getRow() >= _plateTemplate.getRows() || position.getColumn()-1 >= _plateTemplate.getColumns())
                {
                    throw new ExperimentException("The data from the results file doesn't match the plate template, please check your assay configuration.  " +
                            "Data parsed at well location: " + position.getDescription() + " won't fit in the template size (" +
                            _plateTemplate.getRows() + " x " + _plateTemplate.getColumns() + ").");
                }
                data[position.getRow()][position.getColumn()-1] = signal.doubleValue();
            }
        }

        public void setExtraProperties(Map<String, Object> row, PositionImpl position, Integer spot, List<? extends DomainProperty> resultsDomain)
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

            // pick up any properties that match those in the results domain (outside of the built-in ones)
            for (DomainProperty prop : resultsDomain)
            {
                if (!ElisaAssayProvider.REQUIRED_RESULT_PROPERTIES.contains(prop.getName()))
                {
                    Object value = getValue(row, prop);
                    if (value != null)
                        extraProperties.put(prop.getName(), value);
                }
            }
            _extraWellData.put(getWellAnalyteKey(position, spot), extraProperties);
        }

        private Object getValue(Map<String, Object> row, DomainProperty property)
        {
            Object value = row.get(property.getName());
            if (value != null)
                return value;
            for (String alias : property.getImportAliasSet())
            {
                value = row.get(alias);
                if (value != null)
                    return value;
            }
            return null;
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
