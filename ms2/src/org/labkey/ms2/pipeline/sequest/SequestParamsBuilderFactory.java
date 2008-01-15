package org.labkey.ms2.pipeline.sequest;

import java.net.URI;
import java.util.Map;

/**
 * User: billnelson@uky.edu
 * Date: Jan 23, 2007
 * Time: 4:45:24 PM
 */
public class SequestParamsBuilderFactory {
    public static SequestParamsBuilder createVersion1Builder(Map<String, String> sequestInputParams, URI uriSequenceRoot)
    {
         return new SequestParamsV1Builder(sequestInputParams, uriSequenceRoot);
    }

    public static SequestParamsBuilder createVersion2Builder(Map<String, String> sequestInputParams, URI uriSequenceRoot)
    {
         return new SequestParamsV2Builder(sequestInputParams, uriSequenceRoot);
    }
}
