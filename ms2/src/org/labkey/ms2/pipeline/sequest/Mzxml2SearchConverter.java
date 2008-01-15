package org.labkey.ms2.pipeline.sequest;

import java.util.StringTokenizer;

/**
 * User: billnelson@uky.edu
 * Date: Oct 26, 2006
 * Time: 12:14:42 PM
 */
public class Mzxml2SearchConverter implements IInputXMLConverter
{

    public String convert(Param mzxml2SearchParam)
    {
        String value = mzxml2SearchParam.getValue();
        StringBuilder sb = new StringBuilder("");
        if (value.equals("")) return "";
        StringTokenizer st = new StringTokenizer(value, ",");
        while (st.hasMoreTokens())
        {
            String token = st.nextToken();
            if (sb.length() > 0 && !token.equals("")) sb.append("-");
            sb.append(token);
        }
        return mzxml2SearchParam.getName() + sb.toString();
    }
}
