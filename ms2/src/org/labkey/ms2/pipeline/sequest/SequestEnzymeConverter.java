/*
 * Copyright (c) 2006-2008 LabKey Corporation
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
