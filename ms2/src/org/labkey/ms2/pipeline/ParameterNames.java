package org.labkey.ms2.pipeline;

public class ParameterNames
{
    public static String STATIC_MOD = "residue, modification mass";
    public static String DYNAMIC_MOD = "residue, potential modification mass";
    /** Peptide C-terminus modifications */
    public static String DYNAMIC_C_TERM_PEPTIDE_MOD = "refine, potential C-terminus modifications";
    /** Peptide N-terminus modifications */
    public static String DYNAMIC_N_TERM_PEPTIDE_MOD = "refine, potential N-terminus modifications";
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