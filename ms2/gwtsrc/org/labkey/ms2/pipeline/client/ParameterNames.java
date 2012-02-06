package org.labkey.ms2.pipeline.client;

/**
 * Can't have these constants in the associated SearchFormComposite subclasses because the server can't access them,
 * because GWT doesn't allow references to the UIObject class outside of the client
 * User: jeckels
 * Date: Jan 31, 2012
 */
public class ParameterNames
{
    public static String STATIC_MOD = "residue, modification mass";
    public static String DYNAMIC_MOD = "residue, potential modification mass";
    public static String ENZYME = "protein, cleavage site";
    public static String SEQUENCE_DB = "pipeline, database";
    public static String TAXONOMY = "protein, taxon";
    
    public static String MIN_PEPTIDE_PROPHET_PROBABILITY = "pipeline prophet, min probability";
    public static String MIN_PROTEIN_PROPHET_PROBABILITY = "pipeline prophet, min protein probability";
    public static final String PIPELINE_QUANT_PREFIX = "pipeline quantitation, ";
    public static final String LIBRA_CONFIG_NAME_PARAM = PIPELINE_QUANT_PREFIX + "libra config name";
    public static final String LIBRA_NORMALIZATION_CHANNEL_PARAM = PIPELINE_QUANT_PREFIX + "libra normalization channel";
    public static final String QUANTITATION_ALGORITHM = PIPELINE_QUANT_PREFIX + "algorithm";
    public static final String QUANTITATION_MASS_TOLERANCE = PIPELINE_QUANT_PREFIX + "mass tolerance";
    public static final String QUANTITATION_RESIDUE_LABEL_MASS = PIPELINE_QUANT_PREFIX + "residue label mass";
}
