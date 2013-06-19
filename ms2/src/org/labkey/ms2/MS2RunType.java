/*
 * Copyright (c) 2007-2013 LabKey Corporation
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
            new Pair<>("RawScore", "dotproduct"),
            new Pair<>("Delta", "delta"),
            new Pair<>("ZScore", "zscore")),
    Mascot(MascotRun.class,
            new Pair<>("Ion", "ionscore"),
            new Pair<>("Identity", "identityscore"),
            new Pair<>("Homology", "homologyscore"),
            new Pair<>("NullScore", "null"),
            new Pair<>("Expect", "expect")),
    Phenyx(PhenyxRun.class,
           new Pair<>("OrigScore", "origScore"),
           new Pair<>("Bogus", "bogus"),
           new Pair<>("ZScore", "zscore")),
    Sequest(SequestRun.class,
            new Pair<>("SpScore", "spscore"),
            new Pair<>("DeltaCn", "deltacn"),
            new Pair<>("XCorr", "xcorr"),
            new Pair<>("SpRank", "sprank")),
    XComet(XCometRun.class,
            new Pair<>("RawScore", "dotproduct"),
            new Pair<>("Delta", "delta"),
            new Pair<>("ZScore", "zscore"),
            new Pair<>("DeltaStar", "deltastar"),
            new Pair<>("Expect", "expect"))
    {
        public boolean isPeptideTableHidden()
        {
            return true;
        }
    },
    XTandem(XTandemRun.class,
            new Pair<>("Hyper", "hyperscore"),
            new Pair<>("Next", "nextscore"),
            new Pair<>("B", "bscore"),
            new Pair<>("Y", "yscore"),
            new Pair<>("Expect", "expect")),
    XTandemcomet(XTandemcometRun.class,
            new Pair<>("RawScore", "dotproduct"),
            new Pair<>("Delta", "delta"),
            new Pair<>("ZScore", "zscore"),
            new Pair<>("DeltaStar", "deltastar"),
            new Pair<>("Expect", "expect"))
    {
        public boolean isPeptideTableHidden()
        {
            return true;
        }
    },
    Unknown(UnknownMS2Run.class)
    {
        public boolean isPeptideTableHidden()
        {
            return true;
        }
    };

    private final Class<? extends MS2Run> _runClass;
    private final String _scoreColumnNames;
    private final List<String> _scoreColumnList = new ArrayList<>();
    private final List<String> _pepXmlScoreNames = new ArrayList<>();

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

    public String getPeptideTableName()
    {
        return this.toString() + "Peptides";
    }

    public boolean isPeptideTableHidden()
    {
        return false;
    }
}
