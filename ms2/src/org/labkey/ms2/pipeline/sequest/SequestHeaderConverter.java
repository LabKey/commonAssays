package org.labkey.ms2.pipeline.sequest;

/**
 * User: billnelson@uky.edu
 * Date: Sep 8, 2006
 * Time: 10:42:38 AM
 */
public class SequestHeaderConverter implements IInputXMLConverter
{

    public String convert(Param sequestParam)
    {
        return sequestParam.getValue();
    }
}
