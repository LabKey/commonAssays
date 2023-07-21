package org.labkey.elisa;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.assay.AssayUploadXarContext;
import org.labkey.api.assay.plate.Plate;
import org.labkey.api.assay.plate.PlateBasedAssayProvider;
import org.labkey.api.assay.plate.Position;
import org.labkey.api.assay.plate.Well;
import org.labkey.api.assay.plate.WellGroup;
import org.labkey.api.data.Container;
import org.labkey.api.data.statistics.CurveFit;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ProvenanceService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractElisaImportHelper implements ElisaImportHelper
{
    protected final ProvenanceService _pvs = ProvenanceService.get();
    protected AssayUploadXarContext _context;
    protected PlateBasedAssayProvider _provider;
    protected ExpProtocol _protocol;
    protected File _dataFile;
    protected Container _container;
    Map<Position, String> _specimenGroupMap;

    public AbstractElisaImportHelper(AssayUploadXarContext context, PlateBasedAssayProvider provider, ExpProtocol protocol, File dataFile)
    {
        _context = context;
        _provider = provider;
        _protocol = protocol;
        _dataFile = dataFile;
        _container = context.getContainer();
    }

    protected Map<Position, String> getSpecimenGroupMap()
    {
        if (_specimenGroupMap == null)
        {
            _specimenGroupMap = new HashMap<>();
            Plate template = _provider.getPlate(_protocol.getContainer(), _protocol);
            for (WellGroup sample : template.getWellGroups(WellGroup.Type.SPECIMEN))
            {
                for (Position pos : sample.getPositions())
                    _specimenGroupMap.put(pos, sample.getName());
            }
        }
        return _specimenGroupMap;
    }

    @Override
    public Map<String, Object> createWellRow(Domain domain, String plateName, Integer spot, WellGroup parentWellGroup, WellGroup replicate,
                                             Well well,
                                             Position position,
                                             @Nullable CurveFit stdCurveFit,
                                             Map<String, ExpMaterial> materialMap)
    {
        Map<String, Object> row = new HashMap<>();

        // initialize the row map with keys for all of the columns in the result domain to avoid having data filtered out
        // in AbstractAssayTsvDataHandler.checkColumns
        for (DomainProperty prop : domain.getProperties())
        {
            row.put(prop.getName(), null);
        }
        row.put(ElisaAssayProvider.WELL_LOCATION_PROPERTY, position.getDescription());
        row.put(ElisaAssayProvider.WELLGROUP_LOCATION_PROPERTY, replicate.getPositionDescription());
        row.put(ElisaAssayProvider.WELLGROUP_NAME_PROPERTY, parentWellGroup.getName());
        row.put(ElisaAssayProvider.ABSORBANCE_PROPERTY, well.getValue());
        if (stdCurveFit != null)
        {
            double calcConc = stdCurveFit.solveForX(well.getValue());
            if (!Double.isNaN(calcConc))
            {
                row.put(ElisaAssayProvider.CONCENTRATION_PROPERTY, calcConc);
            }
        }
        row.put(ElisaAssayProvider.MEAN_ABSORPTION_PROPERTY, replicate.getMean());
        row.put(ElisaAssayProvider.CV_ABSORPTION_PROPERTY, replicate.getStdDev() / replicate.getMean());

        return row;
    }

    @Override
    public String getMaterialKey(String plateName, Integer analyteNum, String sampleWellGroup)
    {
        return sampleWellGroup;
    }

    @Override
    public Map<String, Object> getExtraProperties(String plateName, Position position, Integer spot)
    {
        return Collections.emptyMap();
    }
}
