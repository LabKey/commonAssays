package org.labkey.ms2.pipeline.sequest;

import java.util.List;

/**
 * User: billnelson@uky.edu
 * Date: May 16, 2008
 */

/**
 * <code>Out2XmlParam</code>
 */
public class Out2XmlParam extends Param
{
   Out2XmlParam(
        int sortOrder,
        String value,
        String name,
        List<String> inputXmlLabels,
        IInputXMLConverter converter,
        IParamsValidator validator)
    {
        super(sortOrder,
            value,
            name,
            inputXmlLabels,
            converter,
            validator);
    }

    Out2XmlParam(
        int sortOrder,
        String value,
        String name,
        IInputXMLConverter converter,
        IParamsValidator validator)
    {
        super(sortOrder,
            value,
            name,
            converter,
            validator);
    }
}
