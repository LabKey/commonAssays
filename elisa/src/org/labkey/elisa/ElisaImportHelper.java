package org.labkey.elisa;

import org.labkey.api.assay.plate.Plate;
import org.labkey.api.assay.plate.Position;
import org.labkey.api.assay.plate.Well;
import org.labkey.api.assay.plate.WellGroup;
import org.labkey.api.data.statistics.CurveFit;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.property.Domain;

import java.util.Map;
import java.util.Set;

public interface ElisaImportHelper
{
    public static String PLACEHOLDER_PLATE_NAME = "PLACEHOLDER_PLATE";

    /**
     * Gets the set of plate names in this import
     */
    Set<String> getPlates();

    /**
     * Gets a map of analyte number to plate populated with raw signal values. Multi-plex plates will
     * record multiple values per well (for each analyte). We will model this as multiple plates, one for
     * each analyte.
     */
    Map<Integer, Plate> getAnalyteToPlate(String plateName) throws ExperimentException;

    /**
     * Returns a map of replicate well group name to standard concentration value.
     */
    Map<String, Double> getStandardConcentrations(String plateName, int analyteNum) throws ExperimentException;

    /**
     * Helper to create a standard well row
     *
     * @param domain the result domain of the assay
     * @param plateName the optional plate name parameter for multi plate imports
     * @param parentWellGroup the parent of the replicate well group (either control or sample)
     * @param replicate the replicate well group
     * @param well the well we are creating the row for
     * @param stdCurveFit the selected curvefit for the standard curve
     * @param materialMap map of specimen well group name to expMaterial
     */
    Map<String, Object> createWellRow(Domain domain, String plateName, Integer spot, WellGroup parentWellGroup, WellGroup replicate,
                                      Well well,
                                      Position position,
                                      CurveFit stdCurveFit,
                                      Map<String, ExpMaterial> materialMap);

    /**
     * Computes the key used to resolve material outputs. Multi-plate formatted data files may need
     * to incorporate the plate name unless samples are the same across plates. Otherwise the well
     * group name is sufficient.
     */
    String getMaterialKey(String plateName, Integer analyteNum, String sampleWellGroup);

    /**
     * Get any extra properties to include in the result data for the specified plate/location/analyte combination
     */
    Map<String, Object> getExtraProperties(String plateName, Position position, Integer spot);
}
