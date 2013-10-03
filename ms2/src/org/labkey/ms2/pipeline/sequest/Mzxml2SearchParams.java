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

import org.labkey.ms2.pipeline.AbstractMS2SearchTask;

/**
 * User: billnelson@uky.edu
 * Date: Oct 26, 2006
 * Time: 10:46:50 AM
 */
public class Mzxml2SearchParams extends Params
{

    public Mzxml2SearchParams()
    {
        initProperties();
    }

    public void initProperties()
    {
        _params.clear();
        _params.add(new Mzxml2SearchParam(
            10,
            "",
            "-F",
            ConverterFactory.getMzxml2SearchConverter(),
            ParamsValidatorFactory.getNaturalNumberParamsValidator()
        ).setInputXmlLabels("mzxml2search, first scan"));

        _params.add(new Mzxml2SearchParam(
            20,
            "",
            "-L",
            ConverterFactory.getMzxml2SearchConverter(),
            ParamsValidatorFactory.getNaturalNumberParamsValidator()
        ).setInputXmlLabels("mzxml2search, last scan"));

        _params.add(new Mzxml2SearchParam(
            30,
            "",
            "-C",
            ConverterFactory.getMzxml2SearchConverter(),
            ParamsValidatorFactory.getNaturalNumberParamsValidator()
        ).setInputXmlLabels("mzxml2search, charge"));

        _params.add(new Mzxml2SearchParam(
            40,
            "",
            "-c",
            ConverterFactory.getMzxml2SearchConverter(),
            ParamsValidatorFactory.getNaturalNumberParamsValidator()
        ).setInputXmlLabels("mzxml2search, charge defaults"));

        _params.add(new Mzxml2SearchParam(
            50,
            "",
            "-P",
            ConverterFactory.getMzxml2SearchConverter(),
            ParamsValidatorFactory.getNaturalNumberParamsValidator()
        ).setInputXmlLabels("spectrum, minimum peaks"));

         _params.add(new Mzxml2SearchParam(
            60,
            "",
            "-B",
            ConverterFactory.getMzxml2SearchConverter(),
            ParamsValidatorFactory.getPositiveDoubleParamsValidator()
        ).setInputXmlLabels(AbstractMS2SearchTask.MINIMUM_PARENT_M_H));

        _params.add(new Mzxml2SearchParam(
            70,
            "",
            "-T",
            ConverterFactory.getMzxml2SearchConverter(),
            ParamsValidatorFactory.getPositiveDoubleParamsValidator()
        ).setInputXmlLabels(AbstractMS2SearchTask.MAXIMUM_PARENT_M_H));

                _params.add(new Mzxml2SearchParam(
            80,
            "",
            "-h",
            ConverterFactory.getMzxml2SearchConverter(),
            ParamsValidatorFactory.getBooleanParamsValidator()
        ).setInputXmlLabels("mzxml2search, hydrogen mass"));
    }
}
