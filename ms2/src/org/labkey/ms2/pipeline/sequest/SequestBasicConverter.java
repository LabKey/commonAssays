package org.labkey.ms2.pipeline.sequest;

/**
 * User: billnelson@uky.edu
 * Date: Sep 8, 2006
 * Time: 11:04:55 AM
 */
public class SequestBasicConverter implements IInputXMLConverter
{
    public String convert(Param param)
    {
        SequestParam sequestParam = (SequestParam) param;
        StringBuffer sb = new StringBuffer(sequestParam.getName());
        sb.append(" = ");
        sb.append(sequestParam.getValue());
        if (sequestParam.getComment() != null && !sequestParam.getComment().equals(""))
        {
            int spacer = 10;
            if (sb.length() < 36)
            {
                spacer = 40 - sb.length();
            }

            for (int i = 0; i < spacer; i++)
            {
                sb.append(" ");
            }

            sb.append("; ");
            sb.append(sequestParam.getComment());
        }
        return sb.toString();
    }
}
