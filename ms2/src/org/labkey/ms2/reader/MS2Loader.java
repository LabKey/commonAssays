/*
 * Copyright (c) 2006-2016 LabKey Corporation
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

package org.labkey.ms2.reader;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.labkey.ms2.MS2Modification;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class MS2Loader
{
    protected Logger _log;
    protected long _fileLength;
    protected File _file;

    protected static final int STREAM_BUFFER_SIZE = 128 * 1024;

    protected void init(File f, Logger log) throws FileNotFoundException, XMLStreamException
    {
        if (f.exists())
        {
            _file = f;
            _fileLength = f.length();
        }
        else
            throw new FileNotFoundException(f.getAbsolutePath());

        _log = log;
    }

    public long getFileLength()
    {
        return _fileLength;
    }

    public abstract void close();

    public static class PeptideFraction
    {
        protected String _massSpecType = null;
        protected String _searchEngine = null;
        protected String _searchEngineVersion = null;
        protected String _searchEnzyme = null;

        protected Set<String> _databaseLocalPaths = new LinkedHashSet<>();
        protected String _dataBasename;
        protected String _dataSuffix;
        protected String _spectrumPath = null;
        protected Float _importSpectraMinProbability = null;
        protected boolean _loadSpectra = false;
        protected MS2ModificationList _modifications = new MS2ModificationList();
        protected String _mascotFile = null;
        protected String _distillerRawFile = null;

        public String getMassSpecType()
        {
            return _massSpecType;
        }

        public List<MS2Modification> getModifications()
        {
            return _modifications;
        }


        public String getSearchEngine()
        {
            return _searchEngine;
        }

        public String getSearchEngineVersion()
        {
            return _searchEngineVersion;
        }


        public String getSearchEnzyme()
        {
            return _searchEnzyme;
        }

        public String getDataBasename()
        {
            return _dataBasename;
        }


        public String getDataSuffix()
        {
            return _dataSuffix;
        }

        @NotNull
        public Set<String> getDatabaseLocalPaths()
        {
            return _databaseLocalPaths;
        }

        public String getSpectrumPath()
        {
            return _spectrumPath;
        }

        public void setSpectrumPath(String spectrumPath)
        {
            this._spectrumPath = spectrumPath;
        }

        public boolean shouldLoadSpectra()
        {
            return _loadSpectra;
        }

        public Float getImportSpectraMinProbability()
        {
            return _importSpectraMinProbability;
        }

        public boolean isSequest()
        {
            return "sequest".equalsIgnoreCase(getSearchEngine());
        }

        public void setSearchEngine(String searchEngine)
        {
            _searchEngine = searchEngine;
        }

        public void setSearchEngineVersion(String searchEngineVersion)
        {
            _searchEngineVersion = searchEngineVersion;
        }

        public void setDatabaseLocalPaths(Collection<String> databaseLocalPaths)
        {
            _databaseLocalPaths = new LinkedHashSet<>(databaseLocalPaths);
        }

        public void setSearchEnzyme(String searchEnzyme)
        {
            _searchEnzyme = searchEnzyme;
        }

        public void  addModification(MS2Modification modification)
        {
            _modifications.add(modification);
        }

        public String getMascotFile()
        {
            return _mascotFile;
        }

        public void setMascotFile(String mascotFile)
        {
            _mascotFile = mascotFile;
        }

        public String getDistillerRawFile()
        {
            return _distillerRawFile;
        }

        public void setDistillerRawFile(String distillerRawFile)
        {
            _distillerRawFile = distillerRawFile;
        }
    }

    public static class Peptide
    {
        protected Integer _fraction;
        protected Integer _scan;
        protected Integer _endScan;
        protected Integer _charge;
        protected Integer _proteinHits;
        protected Double _retentionTime = null;
        protected Float _ionPercent;
        protected Float _deltaMass;
        protected Double _calculatedNeutralMass;
        protected String _peptide;
        protected String _prevAA;
        protected String _trimmedPeptide;
        protected String _nextAA;
        protected Integer _matchedIons = 0;
        protected Integer _totalIons;
        protected String _protein;
        protected Integer _hitRank = 1;
        protected Map<String, String> _scores = new HashMap<>(10);
        protected MS2ModificationList _modifications;
        protected List<String> _alternativeProteins = new ArrayList<>();
        protected Integer _queryNumber = null;
        protected Boolean _decoy = false;

        //keeps track of all unknown modifications we find, for later reporting
        protected boolean[] _unknownModArray;

        //This variable stays null unless there are actually modifications.  If there are,
        //then all elements are null except the actual modifications.  Index+1 = position of mod
        protected ModifiedAminoAcid[] _modifiedAminoAcids = null;

        protected HashMap<String, PepXmlAnalysisResultHandler.PepXmlAnalysisResult> _analysisResultMap = null;

        public void setCalculatedNeutralMass(double calculatedNeutralMass)
        {
            _calculatedNeutralMass = calculatedNeutralMass;
        }

        public void setCharge(int charge)
        {
            _charge = charge;
        }

        public void setDeltaMass(float deltaMass)
        {
            _deltaMass = deltaMass;
        }

        public void setEndScan(int endScan)
        {
            _endScan = endScan;
        }

        public void setFraction(int fraction)
        {
            _fraction = fraction;
        }

        public void setIonPercent(float ionPercent)
        {
            _ionPercent = ionPercent;
        }

        public void setModifications(MS2ModificationList modifications)
        {
            _modifications = modifications;
        }

        public void setNextAA(String nextAA)
        {
            _nextAA = nextAA;
        }

        public void setPeptide(String peptide)
        {
            _peptide = peptide;
        }

        public void setPrevAA(String prevAA)
        {
            _prevAA = prevAA;
        }

        public void setProtein(String protein)
        {
            _protein = protein;
        }

        public void setProteinHits(int proteinHits)
        {
            _proteinHits = proteinHits;
        }

        public void setRetentionTime(Double retentionTime)
        {
            _retentionTime = retentionTime;
        }

        public void setScan(int scan)
        {
            _scan = scan;
        }

        public void setTrimmedPeptide(String trimmedPeptide)
        {
            _trimmedPeptide = trimmedPeptide;

        }

        public void setHitRank(Integer hitRank)
        {
            this._hitRank = hitRank;
        }

        public Integer getMatchedIons()
        {
            return _matchedIons;
        }

        public void setMatchedIons(Integer matchedIons)
        {
            _matchedIons = matchedIons;
        }

        public Integer getTotalIons()
        {
            return _totalIons;
        }

        public void setTotalIons(Integer totalIons)
        {
            _totalIons = totalIons;
        }

        public Integer getCharge()
        {
            return _charge;
        }

        public Double getRetentionTime()
        {
            return _retentionTime;
        }

        public Float getDeltaMass()
        {
            return _deltaMass;
        }

        public Float getIonPercent()
        {
            return _ionPercent;
        }

        public Double getCalculatedNeutralMass()
        {
            return _calculatedNeutralMass;
        }

        public String getNextAA()
        {
            return _nextAA;
        }

        public String getPeptide()
        {
            return _peptide;
        }

        public String getPrevAA()
        {
            return _prevAA;
        }

        public String getProtein()
        {
            return _protein;
        }

        public Integer getProteinHits()
        {
            return _proteinHits;
        }

        public Integer getScan()
        {
            return _scan;
        }

        public Integer getEndScan()
        {
            return _endScan;
        }

        public Map<String, String> getScores()
        {
            return _scores;
        }

        public void setScores(Map<String, String> scores)
        {
            _scores = scores;
        }

        public void setScore(String name, String value)
        {
            _scores.put(name, value);
        }

        public String getScore(String name)
        {
            return _scores.get(name);
        }

        public String getTrimmedPeptide()
        {
            return _trimmedPeptide;
        }

        public Integer getHitRank() { return _hitRank; }

        public ModifiedAminoAcid[] getModifiedAminoAcids()
        {
            return _modifiedAminoAcids;
        }

        public Integer getFraction()
        {
            return _fraction;
        }

        public HashMap<String, PepXmlAnalysisResultHandler.PepXmlAnalysisResult> getAnalysisResultMap()
        {
            return _analysisResultMap;
        }

        public MS2ModificationList getModifications()
        {
            return _modifications;
        }

        @NotNull
        public List<String> getAlternativeProteins()
        {
            return _alternativeProteins;
        }

        public void addAlternativeProtein(@NotNull String protein)
        {
            _alternativeProteins.add(protein);
        }

        protected void addAnalysisResult(String analysisType,
                                         PepXmlAnalysisResultHandler.PepXmlAnalysisResult analysisResult)
        {
            if (_analysisResultMap == null)
                _analysisResultMap = new HashMap<>();
            _analysisResultMap.put(analysisType, analysisResult);
        }

        public PepXmlAnalysisResultHandler.PepXmlAnalysisResult getAnalysisResult(String analysisType)
        {
            if (_analysisResultMap == null)
                return null;
            return _analysisResultMap.get(analysisType);
        }

        public PeptideProphetHandler.PeptideProphetResult getPeptideProphetResult()
        {
            return (PeptideProphetHandler.PeptideProphetResult)
                    getAnalysisResult(PeptideProphetHandler.analysisType);
        }


        public XPressHandler.XPressResult getXPressResult()
        {
            return (XPressHandler.XPressResult)
                    getAnalysisResult(XPressHandler.analysisType);
        }

        public Q3Handler.Q3Result getQ3Result()
        {
            return (Q3Handler.Q3Result)
                    getAnalysisResult(Q3Handler.ANALYSIS_TYPE);
        }

        public Integer getQueryNumber()
        {
            return _queryNumber;
        }

        public void setQueryNumber(Integer queryNumber)
        {
            _queryNumber = queryNumber;
        }

        public Boolean getDecoy()
        {
            return _decoy;
        }

        public void setDecoy(Boolean decoy)
        {
            _decoy = decoy;
        }

        /**
         * Called after peptide loading is complete
         */
        public void setDerivedFieldValues()
        {
            if (null == _peptide)
                _peptide = _trimmedPeptide;

            _peptide = (_prevAA != null ? _prevAA + "." : "") +
                    _peptide + (_nextAA != null ? "." + _nextAA : "");



            if (0 == _matchedIons)
                _ionPercent = 0.0f;
            else
            {
                if (null == _totalIons)
                {
                    // this formula is somewhat magical to me, the 2 multiplier represents ('num_ion_series' b and y)
                    _totalIons = (_trimmedPeptide.length() - 1) * 2;
                    if (_charge > 2)
                    {
                        // do it iteratively for charge 3, 4, 5, etc
                        for (int ionPerm = 2; ionPerm < _charge; ionPerm++)
                            _totalIons *= 2;
                    }
                }
                _ionPercent = Rounder.round((float) _matchedIons / _totalIons, 2);
            }

            if (getScore("expect") == null && getScore("identityscore") != null && getScore("ionscore") != null)
            {
                // ASMS Workshop and User Meeting 2005
                // as defined in http://www.matrixscience.com/pdf/2005WKSHP4.pdf
                // E = P{threshold} * (10 ** ((S{threshold}-score)/ 10))
                Float identityScore = Float.parseFloat(getScore("identityscore"));
                Float ionScore = Float.parseFloat(getScore("ionscore"));
                Double expectationValue = 0.05 * Math.pow(10, (identityScore - ionScore)/10);
                setScore("expect", String.format("%.2f", expectationValue));
            }


        }
    }

}
