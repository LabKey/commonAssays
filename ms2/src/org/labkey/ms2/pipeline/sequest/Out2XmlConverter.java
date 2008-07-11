/*
 * Copyright (c) 2008 LabKey Corporation
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
