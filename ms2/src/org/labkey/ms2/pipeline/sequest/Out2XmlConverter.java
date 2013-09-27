/*
 * Copyright (c) 2008-2013 LabKey Corporation
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
 * Date: May 16, 2008
 */

import org.apache.commons.beanutils.ConvertUtils;

/**
 * <code>Out2XmlConverter</code>
 */
public class Out2XmlConverter implements IInputXMLConverter
{
    public String convert(Param out2XmlParam, String commentPrefix)
    {
        String value = out2XmlParam.getValue();
        if (value.equals("")) return "";
        if (out2XmlParam.getValidator() instanceof BooleanParamsValidator)
        {
            Boolean b = (Boolean) ConvertUtils.convert(value, Boolean.class);
            if (b != null && b.booleanValue())
            {
                return out2XmlParam.getName();
            }
            return "";
        }
        return out2XmlParam.getName() + value;
    }
}
