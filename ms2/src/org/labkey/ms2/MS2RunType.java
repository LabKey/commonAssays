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

package org.labkey.ms2;

import org.labkey.ms2.pipeline.tandem.XCometRun;
import org.labkey.ms2.pipeline.tandem.XTandemcometRun;
import org.labkey.ms2.pipeline.tandem.XTandemRun;
import org.labkey.ms2.pipeline.phenyx.PhenyxRun;
import org.labkey.ms2.pipeline.mascot.MascotRun;
import org.labkey.ms2.pipeline.comet.CometRun;
import org.labkey.ms2.pipeline.sequest.SequestRun;
import org.labkey.ms2.pipeline.UnknownMS2Run;

import java.util.Arrays;
import java.util.List;

/**
 * User: jeckels
 * Date: Jun 20, 2007
 */
public enum MS2RunType
{
    Comet(CometRun.class, "RawScore", "DiffScore", "ZScore"),
    Mascot(MascotRun.class, "Ion", "Identity", "Homology", "Expect"),
    Phenyx(PhenyxRun.class, "OrigScore", "Bogus", "ZScore"),
    Sequest(SequestRun.class, "SpRank", "SpScore", "DeltaCn", "XCorr"),
    XComet(XCometRun.class, "RawScore", "DiffScore", "Expect"),
    XTandem(XTandemRun.class, "Hyper", "Next", "B", "Y", "Expect"),
    XTandemcomet(XTandemcometRun.class, "RawScore", "DiffScore", "Expect"),
    Unknown(UnknownMS2Run.class);

    private final Class<? extends MS2Run> _runClass;
    private final String _scoreColumnNames;
    private final List<String> _scoreColumnList;

    private MS2RunType(Class<? extends MS2Run> runClass, String... scoreColumnNames)
    {
        _runClass = runClass;
        StringBuilder sb = new StringBuilder();
        String separator = "";
        for (String name : scoreColumnNames)
        {
            sb.append(separator);
            separator = ", ";
            sb.append(name);
        }
        _scoreColumnNames = sb.toString();
        _scoreColumnList = Arrays.asList(scoreColumnNames);
    }

    public Class<? extends MS2Run> getRunClass()
    {
        return _runClass;
    }

    public String getScoreColumnNames()
    {
        return _scoreColumnNames;
    }

    public List<String> getScoreColumnList()
    {
        return _scoreColumnList;
    }

    public static MS2RunType lookupType(String type)
    {
        if (type == null)
        {
            return null;
        }

        type = type.toLowerCase();
        StringBuffer filteredTypeBuffer = new StringBuffer();

        // Eliminate all non-letter characters
        for (int i = 0; i < type.length(); i++)
        {
            char c = type.charAt(i);

            if (Character.isLowerCase(c) || Character.isDigit(c))
                filteredTypeBuffer.append(c);
        }

        String filteredType = filteredTypeBuffer.toString();
        for (MS2RunType runType : values())
        {
            if (runType.name().toLowerCase().equals(filteredType))
            {
                return runType;
            }
        }

        // If it was created by X!Tandem, then use the X!Tandem default class.
        if (type.startsWith("x!"))
        {
            return XTandem;
        }
        
        return Unknown;
    }
}
