package org.labkey.ms2.pipeline.sequest;

import java.util.List;

/**
 * User: billnelson@uky.edu
 * Date: Oct 26, 2006
 * Time: 9:29:43 AM
 */
public class Mzxml2SearchParam extends Param
{

    Mzxml2SearchParam(
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

    Mzxml2SearchParam(
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
