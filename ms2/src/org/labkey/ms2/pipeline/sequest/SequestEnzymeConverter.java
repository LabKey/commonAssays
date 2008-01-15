package org.labkey.ms2.pipeline.sequest;

/**
 * User: billnelson@uky.edu
 * Date: Sep 20, 2006
 * Time: 5:36:15 PM
 */
public class SequestEnzymeConverter implements IInputXMLConverter
{

    public String convert(Param param)
    {
        SequestParam sequestParam = (SequestParam) param;
        StringBuffer sb = new StringBuffer();
        sb.append(sequestParam.getComment());
        sb.append(sequestParam.getValue());
        return sb.toString();
    }

}
