package org.labkey.elisa;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.assay.AssayUploadXarContext;
import org.labkey.api.assay.plate.PlateBasedAssayProvider;
import org.labkey.api.assay.plate.PlateTemplate;
import org.labkey.api.assay.plate.Position;
import org.labkey.api.assay.plate.Well;
import org.labkey.api.assay.plate.WellGroup;
import org.labkey.api.assay.plate.WellGroupTemplate;
import org.labkey.api.data.Container;
import org.labkey.api.data.statistics.CurveFit;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ProvenanceService;

import java.io.File;
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
            PlateTemplate template = _provider.getPlateTemplate(_protocol.getContainer(), _protocol);
            for (WellGroupTemplate sample : template.getWellGroups(WellGroup.Type.SPECIMEN))
            {
                for (Position pos : sample.getPositions())
                    _specimenGroupMap.put(pos, sample.getName());
            }
        }
        return _specimenGroupMap;
    }

    @Override
    public Map<String, Object> createWellRow(String plateName, Integer spot, WellGroup replicate, Well well, Position position,
                                             @Nullable CurveFit stdCurveFit,
                                             Map<String, ExpMaterial> materialMap)
    {
        Map<String, Object> row = new HashMap<>();

        row.put(ElisaAssayProvider.WELL_LOCATION_PROPERTY, position.getDescription());
        row.put(ElisaAssayProvider.WELLGROUP_PROPERTY, replicate.getPositionDescription());
        row.put(ElisaAssayProvider.ABSORBANCE_PROPERTY, well.getValue());
        if (stdCurveFit != null)
            row.put(ElisaAssayProvider.CONCENTRATION_PROPERTY, stdCurveFit.fitCurveY(well.getValue()));

        row.put(ElisaAssayProvider.MEAN_ABSORPTION_PROPERTY, replicate.getMean());
        row.put(ElisaAssayProvider.CV_ABSORPTION_PROPERTY, replicate.getStdDev() / replicate.getMean());

        return row;
    }

    @Override
    public String getMaterialKey(String plateName, String sampleWellGroup)
    {
        return sampleWellGroup;
    }
}
