/*
 * Copyright (c) 2007-2008 LabKey Corporation
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
 * Date: Jan 24, 2007
 * Time: 10:13:54 AM
 */
public class SequestParamsV2 extends SequestParams
{
    public SequestParamsV2()
    {
        super();
    }
    
    void initProperties()
    {
        super.initProperties();

        _params.add(new SequestParam(
                  130,                                                       //sortOrder
                  "trypsin 1 1 KR P",                                                      //The value of the property
                  "enzyme_info",                                           // the sequest.params property name
                  "",                                                       // the input.xml label
                   ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
                   null,
                   false
      ).setInputXmlLabels("protein, cleavage site"));
    }
}
