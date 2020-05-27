/*
 * Copyright (c) 2005-2010 Fred Hutchinson Cancer Research Center
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

package org.labkey.ms2.pipeline.tandem;

import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2RunType;

/**
 * User: bmaclean
 * Date: July 20, 2005
 */
public class XCometRun extends MS2Run
{
    @Override
    public MS2RunType getRunType()
    {
        return MS2RunType.XComet;
    }

    @Override
    public String getParamsFileName()
    {
        return "tandem.xml";
    }


    @Override
    public String getChargeFilterColumnName()
    {
        return "RawScore";
    }


    @Override
    public String getChargeFilterParamName()
    {
        return "rawScore";
    }

    @Override
    public String getDiscriminateExpressions()
    {
        return "-PeptideProphet, Expect, -DiffScore * RawScore, -DiffScore, -RawScore";
    }

    @Override
    public String[] getGZFileExtensions()
    {
        return new String[]{};
    }
}
