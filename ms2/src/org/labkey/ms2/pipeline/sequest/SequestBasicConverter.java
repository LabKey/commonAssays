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

/**
 * User: billnelson@uky.edu
 * Date: Sep 8, 2006
 * Time: 11:04:55 AM
 */
public class SequestBasicConverter implements IInputXMLConverter
{
    public String convert(Param param, String commentPrefix)
    {
        SequestParam sequestParam = (SequestParam) param;
        StringBuilder sb = new StringBuilder(sequestParam.getName());
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
            sb.append(commentPrefix);
            sb.append(" ");
            sb.append(sequestParam.getComment());
        }
        return sb.toString();
    }
}
