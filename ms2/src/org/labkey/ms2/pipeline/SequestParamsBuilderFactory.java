package org.labkey.ms2.pipeline;

import java.net.URI;

/**
 * User: billnelson@uky.edu
 * Date: Jan 23, 2007
 * Time: 4:45:24 PM
 */
public class SequestParamsBuilderFactory {
    public static SequestParamsBuilder createVersion1Builder(SequestInputParser sequestInputParser, URI uriSequenceRoot)
    {
         return new SequestParamsV1Builder(sequestInputParser, uriSequenceRoot);
    }

    public static SequestParamsBuilder createVersion2Builder(SequestInputParser sequestInputParser, URI uriSequenceRoot)
    {
         return new SequestParamsV2Builder(sequestInputParser, uriSequenceRoot);
    }
}
