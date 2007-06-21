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

import java.util.Map;

/**
 * User: arauch
 * Date: Jul 21, 2005
 * Time: 10:19:11 PM
 */
public class MascotRun extends MS2Run
{
    @Override
    public void adjustScores(Map<String, String> map)
    {
        // Mascot exported pepXML can exclude "homologyscore"
        if (null == map.get("homologyscore"))
            map.put("homologyscore", "-1");
    }

    public MS2RunType getRunType()
    {
        return MS2RunType.Mascot;
    }

    public String getParamsFileName()
    {
//wch: mascotdev
        return "mascot.xml";
//END-wch: mascotdev
    }


    public String getChargeFilterColumnName()
    {
        return "Ion";
    }


    public String getChargeFilterParamName()
    {
        return "ion";
    }

    public String getDiscriminateExpressions()
    {
        return "-Identity";
    }

    protected String getPepXmlScoreNames()
    {
        return "ionscore, identityscore, homologyscore, null, expect";  // TODO: star score?  qmatch?
    }


    public String[] getGZFileExtensions()
    {
        return new String[]{"out", "dta"};
    }
}
