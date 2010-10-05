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

package org.labkey.ms2;

import org.labkey.api.util.Pair;
import org.labkey.ms2.pipeline.tandem.XCometRun;
import org.labkey.ms2.pipeline.tandem.XTandemcometRun;
import org.labkey.ms2.pipeline.tandem.XTandemRun;
import org.labkey.ms2.pipeline.phenyx.PhenyxRun;
import org.labkey.ms2.pipeline.mascot.MascotRun;
import org.labkey.ms2.pipeline.comet.CometRun;
import org.labkey.ms2.pipeline.sequest.SequestRun;
import org.labkey.ms2.pipeline.UnknownMS2Run;

import java.util.ArrayList;
import java.util.List;

/**
 * User: jeckels
 * Date: Jun 20, 2007
 */
public enum MS2RunType
{
    Comet(CometRun.class,
            new Pair<String, String>("RawScore", "dotproduct"),
            new Pair<String, String>("Delta", "delta"),
            new Pair<String, String>("ZScore", "zscore")),
    Mascot(MascotRun.class,
            new Pair<String, String>("Ion", "ionscore"),
            new Pair<String, String>("Identity", "identityscore"),
            new Pair<String, String>("Homology", "homologyscore"),
            new Pair<String, String>("NullScore", "null"),
            new Pair<String, String>("Expect", "expect")),
    Phenyx(PhenyxRun.class,
           new Pair<String, String>("OrigScore", "origScore"),
           new Pair<String, String>("Bogus", "bogus"),
           new Pair<String, String>("ZScore", "zscore")),
    Sequest(SequestRun.class,
            new Pair<String, String>("SpScore", "spscore"),
            new Pair<String, String>("DeltaCn", "deltacn"),
            new Pair<String, String>("XCorr", "xcorr"),
            new Pair<String, String>("SpRank", "sprank")),
    XComet(XCometRun.class,
            new Pair<String, String>("RawScore", "dotproduct"),
            new Pair<String, String>("Delta", "delta"),
            new Pair<String, String>("ZScore", "zscore"),
            new Pair<String, String>("DeltaStar", "deltastar"),
            new Pair<String, String>("Expect", "expect")),
    XTandem(XTandemRun.class,
            new Pair<String, String>("Hyper", "hyperscore"),
            new Pair<String, String>("Next", "nextscore"),
            new Pair<String, String>("B", "bscore"),
            new Pair<String, String>("Y", "yscore"),
            new Pair<String, String>("Expect", "expect")),
    XTandemcomet(XTandemcometRun.class,
            new Pair<String, String>("RawScore", "dotproduct"),
            new Pair<String, String>("Delta", "delta"),
            new Pair<String, String>("ZScore", "zscore"),
            new Pair<String, String>("DeltaStar", "deltastar"),
            new Pair<String, String>("Expect", "expect")),
    Unknown(UnknownMS2Run.class);

    private final Class<? extends MS2Run> _runClass;
    private final String _scoreColumnNames;
    private final List<String> _scoreColumnList = new ArrayList<String>();
    private final List<String> _pepXmlScoreNames = new ArrayList<String>();

    private MS2RunType(Class<? extends MS2Run> runClass, Pair<String, String>... scoreNames)
    {
        _runClass = runClass;
        StringBuilder sb = new StringBuilder();
        String separator = "";
        for (Pair<String, String> p : scoreNames)
        {
            String scoreColumnName = p.getKey();
            String pepXmlName = p.getValue(); 
            sb.append(separator);
            separator = ", ";
            sb.append(scoreColumnName);
            _scoreColumnList.add(scoreColumnName);
            _pepXmlScoreNames.add(pepXmlName);
        }
        _scoreColumnNames = sb.toString();
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

    /** The scores to read from pepXML files, specified in the order they appear in the prepared statement that inserts rows into MS2PeptidesData */
    public List<String> getPepXmlScoreNames()
    {
        return _pepXmlScoreNames;
    }
}
