/*
 * Copyright (c) 2013 LabKey Corporation
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

package org.labkey.ms2.pipeline.comet;

import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2RunType;

/**
 * User: arauch
 * Date: Sep 16, 2004
 * Time: 10:36:11 PM
 */
public class LegacyCometRun extends MS2Run
{
    public MS2RunType getRunType()
    {
        return MS2RunType.LegacyComet;
    }

    public String getParamsFileName()
    {
        return "comet.def";
    }


    public String getChargeFilterColumnName()
    {
        return "RawScore";
    }


    public String getChargeFilterParamName()
    {
        return "rawScore";
    }

    public String getDiscriminateExpressions()
    {
        return "-PeptideProphet, -ZScore, -DiffScore, -DiffScore * RawScore, -RawScore";
    }

    public String[] getGZFileExtensions()
    {
        return new String[]{"cmt", "dta"};
    }
}
