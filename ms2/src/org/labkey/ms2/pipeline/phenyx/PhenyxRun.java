/*
 * Copyright (c) 2007-2010 LabKey Corporation
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

package org.labkey.ms2.pipeline.phenyx;

import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2RunType;

import java.util.Map;

/**
 * User: adam
 * Date: Jun 11, 2007
 * Time: 2:14:26 PM
 */
public class PhenyxRun extends MS2Run
{
    @Override
    public void adjustScores(Map<String, String> map)
    {
        if (null == map.get("bogus"))
            map.put("bogus", "87");  // TODO: Get rid of this
    }

    public MS2RunType getRunType()
    {
        return MS2RunType.Phenyx;
    }

    public String getParamsFileName()
    {
        return "phenyx.xml";
    }

    public String getChargeFilterColumnName()
    {
        return "ZScore";
    }

    public String getChargeFilterParamName()
    {
        return "zScore";
    }

    public String getDiscriminateExpressions()
    {
        return null;
    }

    public String[] getGZFileExtensions()
    {
        return new String[0];
    }
}
