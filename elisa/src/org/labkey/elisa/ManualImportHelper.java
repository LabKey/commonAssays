package org.labkey.elisa;

import org.apache.commons.lang3.math.NumberUtils;
import org.labkey.api.assay.AssayRunUploadContext;
import org.labkey.api.assay.AssayUploadXarContext;
import org.labkey.api.assay.plate.Plate;
import org.labkey.api.assay.plate.PlateBasedAssayProvider;
import org.labkey.api.assay.plate.PlateReader;
import org.labkey.api.assay.plate.PlateService;
import org.labkey.api.assay.plate.Position;
import org.labkey.api.assay.plate.Well;
import org.labkey.api.assay.plate.WellGroup;
import org.labkey.api.data.statistics.CurveFit;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ProvenanceService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.elisa.actions.ElisaRunUploadForm;
import org.labkey.elisa.plate.BioTekPlateReader;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Import helper that uses the standard import wizard form entry fields.
 */
public class ManualImportHelper extends AbstractElisaImportHelper
{
    public ManualImportHelper(AssayUploadXarContext context, PlateBasedAssayProvider provider, ExpProtocol protocol, File dataFile)
    {
        super(context, provider, protocol, dataFile);
    }

    @Override
    public Set<String> getPlates()
    {
        return Set.of(PLACEHOLDER_PLATE_NAME);
    }

    @Override
    public Map<Integer, Plate> getAnalyteToPlate(String plateName) throws ExperimentException
    {
        Map<Integer, Plate> analyteMap = new HashMap<>();
        PlateReader reader = _provider.getPlateReader(BioTekPlateReader.LABEL);
        if (reader != null)
        {
            Plate template = _provider.getPlateTemplate(_protocol.getContainer(), _protocol);
            double[][] cellValues = reader.loadFile(template, _dataFile);
            if (cellValues == null)
            {
                throw new ExperimentException("Error parsing the uploaded file. The data may not match the layout of the plate: " +
                        "(" + template.getRows() + " x " + template.getColumns() + ") associated with this assay.");
            }
            Plate plate = PlateService.get().createPlate(template, cellValues, null);

            analyteMap.put(1, plate);
        }
        return analyteMap;
    }

    @Override
    public Map<String, Double> getStandardConcentrations(String plateName, int analyteNum) throws ExperimentException
    {
        Map<String, Double> concentrations = new HashMap<>();
        AssayRunUploadContext runUploadContext = _context.getContext();
        if (runUploadContext instanceof ElisaRunUploadForm)
        {
            Map<String, Map<DomainProperty, String>> props = ((ElisaRunUploadForm)runUploadContext).getConcentrationProperties();

            for (Map.Entry<String, Map<DomainProperty, String>> entry : props.entrySet())
            {
                for (DomainProperty dp : entry.getValue().keySet())
                {
                    double conc = 0;
                    if (ElisaAssayProvider.CONCENTRATION_PROPERTY.equals(dp.getName()))
                    {
                        conc = NumberUtils.toDouble(entry.getValue().get(dp), 0);
                    }
                    concentrations.put(entry.getKey(), conc);
                }
            }
            return concentrations;
        }
        else
            throw new ExperimentException("The form is not an instance of ElisaRunUploadForm, concentration values were not accessible.");
    }

    @Override
    public Map<String, Object> createWellRow(Domain domain, String plateName, Integer spot, WellGroup parentWellGroup, WellGroup replicate,
                                             Well well, Position position, CurveFit stdCurveFit, Map<String, ExpMaterial> materialMap)
    {
        Map<String, Object> row = super.createWellRow(domain, plateName, spot, parentWellGroup, replicate, well, position, stdCurveFit, materialMap);

        // if this well is a sample well group add the material LSID
        Map<Position, String> specimenGroupMap = getSpecimenGroupMap();
        if (specimenGroupMap.containsKey(position))
        {
            ExpMaterial material = materialMap.get(specimenGroupMap.get(position));
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
}
