/*
 * Copyright (c) 2007-2016 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.exp.Handler;
import org.labkey.api.query.FieldKey;
import org.labkey.ms2.pipeline.UnknownMS2Run;
import org.labkey.ms2.pipeline.comet.CometRun;
import org.labkey.ms2.pipeline.comet.LegacyCometRun;
import org.labkey.ms2.pipeline.mascot.MascotRun;
import org.labkey.ms2.pipeline.phenyx.PhenyxRun;
import org.labkey.ms2.pipeline.sequest.SequestRun;
import org.labkey.ms2.pipeline.tandem.XCometRun;
import org.labkey.ms2.pipeline.tandem.XTandemRun;
import org.labkey.ms2.pipeline.tandem.XTandemcometRun;
import org.labkey.ms2.query.PeptidesTableInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * User: jeckels
 * Date: Jun 20, 2007
 */
public enum MS2RunType implements Handler<MS2RunType.SearchEngineInfo>
{
    LegacyComet(LegacyCometRun.class,
            new ScoreInfo("RawScore", "dotproduct"),
            new ScoreInfo("Delta", "delta"),
            new ScoreInfo("ZScore", "zscore")),
    Comet(CometRun.class,
            new ScoreInfo("SpScore", "spscore"),
            new ScoreInfo("DeltaCn", "deltacn"),
            new ScoreInfo("XCorr", "xcorr", "COMET XCORR"),
            new ScoreInfo("SpRank", "sprank"),
            new ScoreInfo("DeltaCnStar", "deltacnstar"),
            new ScoreInfo("Expect", "expect")),
    Mascot(MascotRun.class,
            new ScoreInfo("Ion", "ionscore", "MASCOT IONS SCORE"),
            new ScoreInfo("Identity", "identityscore"),
            new ScoreInfo("Homology", "homologyscore"),
            // Dummy score column so that Expect aligns at the same index for the score4 column with some of the other run types
            new ScoreInfo(PeptidesTableInfo.DUMMY_SCORE_COLUMN_NAME, "null"),
            new ScoreInfo("Expect", "expect")),
    Phenyx(PhenyxRun.class,
           new ScoreInfo("OrigScore", "origScore"),
           new ScoreInfo("Bogus", "bogus"),
           new ScoreInfo("ZScore", "zscore")),
    Sequest(SequestRun.class,
            new ScoreInfo("SpScore", "spscore"),
            new ScoreInfo("DeltaCn", "deltacn"),
            new ScoreInfo("XCorr", "xcorr", "SEQUEST XCORR"),
            new ScoreInfo("SpRank", "sprank")),
    XComet(XCometRun.class,
            new ScoreInfo("RawScore", "dotproduct"),
            new ScoreInfo("Delta", "delta"),
            new ScoreInfo("ZScore", "zscore"),
            new ScoreInfo("DeltaStar", "deltastar"),
            new ScoreInfo("Expect", "expect"))
    {
        public boolean isPeptideTableHidden()
        {
            return true;
        }
    },
    XTandem(XTandemRun.class,
            new ScoreInfo("Hyper", "hyperscore"),
            new ScoreInfo("Next", "nextscore"),
            new ScoreInfo("B", "bscore"),
            new ScoreInfo("Y", "yscore"),
            new ScoreInfo("Expect", "expect", "TANDEM EXPECTATION SCORE"))
            {
                @Override
                public Priority getPriority(SearchEngineInfo searchEngineInfo)
                {
                    Priority result = super.getPriority(searchEngineInfo);
                    if (result == null && searchEngineInfo.getType().toLowerCase().startsWith("x!"))
                    {
                        result = Priority.MEDIUM;
                    }
                    return result;
                }
            },
    XTandemcomet(XTandemcometRun.class,
            new ScoreInfo("RawScore", "dotproduct"),
            new ScoreInfo("Delta", "delta"),
            new ScoreInfo("ZScore", "zscore"),
            new ScoreInfo("DeltaStar", "deltastar"),
            new ScoreInfo("Expect", "expect"))
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

        @Override
        public Priority getPriority(SearchEngineInfo object)
        {
            return Priority.LOW;
        }
    };

    private final Class<? extends MS2Run> _runClass;
    private final String _scoreColumnNames;
    private final List<FieldKey> _scoreColumnList = new ArrayList<>();
    private final List<FieldKey> _pepXmlScoreNames = new ArrayList<>();
    private final Integer _bibliospecScoreColumnIndex;
    private final String _bibliospecScoreName;

    MS2RunType(Class<? extends MS2Run> runClass, ScoreInfo... scoreInfos)
    {
        _runClass = runClass;
        StringBuilder sb = new StringBuilder();
        String separator = "";
        String bibliospecScoreName = null;
        Integer bibliospecScoreIndex = null;


        for (int i = 0; i < scoreInfos.length; i++)
        {
            ScoreInfo p = scoreInfos[i];
            String scoreColumnName = p._columnName;
            String pepXmlName = p._pepXMLName;
            sb.append(separator);
            separator = ", ";
            sb.append(scoreColumnName);
            _scoreColumnList.add(FieldKey.fromParts(scoreColumnName));
            _pepXmlScoreNames.add(FieldKey.fromParts(pepXmlName));
            if (p._bibliospecScoreName != null)
            {
                if (bibliospecScoreIndex != null)
                {
                    throw new IllegalArgumentException("More than one Bibliospec score column for " + this);
                }
                // Score column indices/suffices are one-based, not zero-based
                bibliospecScoreIndex = i + 1;
                bibliospecScoreName = p._bibliospecScoreName;
            }
        }
        _bibliospecScoreColumnIndex = bibliospecScoreIndex;
        _bibliospecScoreName = bibliospecScoreName;
        _scoreColumnNames = sb.toString();
    }

    public int getBibliospecScoreColumnIndex()
    {
        if (_bibliospecScoreColumnIndex == null)
        {
            throw new IllegalStateException(this + " has no Bibliospec score column");
        }
        return _bibliospecScoreColumnIndex.intValue();
    }

    public String getBibliospecScoreName()
    {
        return _bibliospecScoreName;
    }

    public Class<? extends MS2Run> getRunClass()
    {
        return _runClass;
    }

    public String getScoreColumnNames()
    {
        return _scoreColumnNames;
    }

    public List<FieldKey> getScoreColumnList()
    {
        return _scoreColumnList;
    }

    public static MS2RunType lookupType(String type, @Nullable String version)
    {
        if (type == null)
        {
            return null;
        }

        return Priority.findBestHandler(Arrays.asList(MS2RunType.values()), new SearchEngineInfo(type, version));
    }

    /** The scores to read from pepXML files, specified in the order they appear in the prepared statement that inserts rows into MS2PeptidesData */
    public List<FieldKey> getPepXmlScoreNames()
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

    @Override
    public Priority getPriority(SearchEngineInfo searchEngineInfo)
    {
        if (name().equalsIgnoreCase(searchEngineInfo.getFilteredType()))
        {
            return Priority.MEDIUM;
        }
        return null;
    }

    public static class SearchEngineInfo
    {
        private final String _type;
        private final String _version;
        private final String _filteredType;

        public SearchEngineInfo(@NotNull String type, String version)
        {
            _type = type;
            _version = version;

            type = type.toLowerCase();
            StringBuilder filteredTypeBuffer = new StringBuilder();

            // Eliminate all non-letter characters
            for (int i = 0; i < type.length(); i++)
            {
                char c = type.charAt(i);

                if (Character.isLowerCase(c) || Character.isDigit(c))
                    filteredTypeBuffer.append(c);
            }

            _filteredType = filteredTypeBuffer.toString();
        }

        public String getType()
        {
            return _type;
        }

        public String getFilteredType()
        {
            return _filteredType;
        }

        public String getVersion()
        {
            return _version;
        }
    }

    private static class ScoreInfo
    {
        private final String _columnName;
        private final String _pepXMLName;
        private final String _bibliospecScoreName;

        public ScoreInfo(String columnName, String pepXMLName)
        {
            this(columnName, pepXMLName, null);
        }

        public ScoreInfo(String columnName, String pepXMLName, String bibliospecScoreName)
        {
            _columnName = columnName;
            _pepXMLName = pepXMLName;
            _bibliospecScoreName = bibliospecScoreName;
        }
    }

    public static class TestCase extends Assert
    {
        @Test
        public void testMatching()
        {
            assertEquals(MS2RunType.XTandem, MS2RunType.lookupType("X!Tandem", null));
            assertEquals(MS2RunType.XTandem, MS2RunType.lookupType("X! Tandem (k-score)", null));
            assertEquals(MS2RunType.Comet, MS2RunType.lookupType("Comet", null));
            assertEquals(MS2RunType.LegacyComet, MS2RunType.lookupType("LegacyComet", null));
            assertEquals(MS2RunType.XTandem, MS2RunType.lookupType("XTandem", null));
            assertEquals(MS2RunType.Unknown, MS2RunType.lookupType("NoSuchRunType", null));
        }
    }
}
