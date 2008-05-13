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
 * Time: 10:15:00 AM
 */
public class SequestParamsV1 extends SequestParams
{
    public SequestParamsV1()
    {
        super();
    }

    void initProperties()
    {
        super.initProperties();
        
        _params.add(new SequestParam(
            130,                                                       //sortOrder
            "1",                                                      //The value of the property
            "enzyme_number",                                           // the sequest.params property name
            "",                                                       // the input.xml label
            ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
            null,
            false                                                                //pass threw- no Xtandem counterpart
        ).setInputXmlLabels("protein, cleavage site"));

       _params.add(new SequestParam(
            580,                                                       //sortOrder
            "[SEQUEST_ENZYME_INFO]",                                            //The value of the property
            "enzyme header",                                // the sequest.params property name
            "",       // the sequest.params comment
            ConverterFactory.getSequestHeaderConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ));

        _params.add(new SequestParam(
            590,                                                       //sortOrder
            "nonspecific\t\t\t\t0\t0\t-\t\t-",                                            //The value of the property
            "enzyme0",                                // the sequest.params property name
            "0.\t",       // the sequest.params comment
            ConverterFactory.getSequestEnzymeConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ));

        _params.add(new SequestParam(
            600,                                                       //sortOrder
            "trypsin\t\t\t\t1\tKR\t\tP",                                            //The value of the property
            "enzyme1",                                // the sequest.params property name
            "1.\t",       // the sequest.params comment
            ConverterFactory.getSequestEnzymeConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ));
    }
}
