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

import java.util.List;

/**
 * User: billnelson@uky.edu
 * Date: Oct 26, 2006
 * Time: 9:29:43 AM
 */
public class Mzxml2SearchParam extends Param
{

    Mzxml2SearchParam(
        int sortOrder,
        String value,
        String name,
        List<String> inputXmlLabels,
        IInputXMLConverter converter,
        IParamsValidator validator)
    {
        super(sortOrder,
            value,
            name,
            inputXmlLabels,
            converter,
            validator);
    }

    Mzxml2SearchParam(
        int sortOrder,
        String value,
        String name,
        IInputXMLConverter converter,
        IParamsValidator validator)
    {
        super(sortOrder,
            value,
            name,
            converter,
            validator);
    }
}
