package org.labkey.elisa;

import org.labkey.api.assay.plate.Plate;
import org.labkey.api.exp.ExperimentException;

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
     * record multiple values per well (for each analyte). We will model this a multiple plates, one for
     * each analyte.
     */
    Map<Integer, Plate> getAnalyteToPlate(String plateName) throws ExperimentException;

    /**
     * Returns a map of replicate well group name to standard concentration value.
     */
    Map<String, Double> getStandardConcentrations(String plateName, int analyteNum) throws ExperimentException;
}
