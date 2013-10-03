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

/**
 * <code>Out2XmlParams</code>
 */
public class Out2XmlParams extends Params
{
    public Out2XmlParams()
    {
        initProperties();
    }

    public void initProperties()
    {
        _params.clear();
        _params.add(new Out2XmlParam(
            10,
            "",
            "-H",
            ConverterFactory.getOut2XmlConverter(),
            ParamsValidatorFactory.getPositiveIntegerParamsValidator()
        ).setInputXmlLabels("out2xml, top hits"));

        _params.add(new Out2XmlParam(
            20,
            "",
            "-E",
            ConverterFactory.getOut2XmlConverter(),
            null
        ).setInputXmlLabels("out2xml, enzyme"));

        _params.add(new Out2XmlParam(
            30,
            "",
            "-M",
            ConverterFactory.getOut2XmlConverter(),
            ParamsValidatorFactory.getBooleanParamsValidator()
        ).setInputXmlLabels("out2xml, maldi mode"));

        _params.add(new Out2XmlParam(
            40,
            "",
            "-pI",
            ConverterFactory.getOut2XmlConverter(),
            ParamsValidatorFactory.getBooleanParamsValidator()
        ).setInputXmlLabels("out2xml, pI"));

        _params.add(new Out2XmlParam(
            50,
            "",
            "-all",
            ConverterFactory.getOut2XmlConverter(),
            ParamsValidatorFactory.getBooleanParamsValidator()
        ).setInputXmlLabels("out2xml, all"));
    }

}
