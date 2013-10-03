/*
 * Copyright (c) 2006-2013 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.ms2.pipeline.sequest;

import java.util.StringTokenizer;

/**
 * User: billnelson@uky.edu
 * Date: Oct 26, 2006
 * Time: 12:14:42 PM
 */
public class Mzxml2SearchConverter implements IInputXMLConverter
{

    public String convert(Param mzxml2SearchParam, String commentPrefix)
    {
        String value = mzxml2SearchParam.getValue();
        StringBuilder sb = new StringBuilder("");
        if (value.equals("")) return "";
        if (mzxml2SearchParam.getValidator().getClass().getName().equals("org.labkey.ms2.pipeline.sequest.BooleanParamsValidator"))
        {
            if(value.equals("1"))return mzxml2SearchParam.getName();
            return "";
        }
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
