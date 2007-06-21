/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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

package org.labkey.ms2;

/**
 * User: bmaclean
 * Date: July 20, 2005
 */
public class XCometRun extends MS2Run
{
    public MS2RunType getRunType()
    {
        return MS2RunType.XComet;
    }

    public String getParamsFileName()
    {
        return "tandem.xml";
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
        return "-PeptideProphet, Expect, -DiffScore * RawScore, -DiffScore, -RawScore";
    }

    protected String getPepXmlScoreNames()
    {
        return "dotproduct, delta, zscore, deltastar, expect";
    }

    public String[] getGZFileExtensions()
    {
        return new String[]{};
    }
}
