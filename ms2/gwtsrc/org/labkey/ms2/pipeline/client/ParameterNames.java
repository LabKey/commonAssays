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
}
