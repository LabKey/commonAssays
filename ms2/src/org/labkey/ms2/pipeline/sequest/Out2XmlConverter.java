package org.labkey.ms2.pipeline.sequest;

import java.util.StringTokenizer;

/**
 * User: billnelson@uky.edu
 * Date: May 16, 2008
 */

/**
 * <code>Out2XmlConverter</code>
 */
public class Out2XmlConverter implements IInputXMLConverter
{
    public String convert(Param out2XmlParam)
    {
        String value = out2XmlParam.getValue();
        if (value.equals("")) return "";
        if (value.equals("1") &&
            out2XmlParam.getValidator().getClass().getName().equals("org.labkey.ms2.pipeline.sequest.BooleanParamsValidator"))
        {
            return out2XmlParam.getName();
        }
        return out2XmlParam.getName() + value;
    }
}
