/*
 * Copyright (c) 2004-2016 Fred Hutchinson Cancer Research Center
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

import org.apache.log4j.Logger;
import org.labkey.api.util.Pair;
import org.labkey.ms2.protein.fasta.Peptide;
import org.labkey.ms2.reader.LibraQuantResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MS2Peptide
{
    private static Logger _log = Logger.getLogger(MS2Peptide.class);

    public static final double pMass = 1.007276;  // Mass of a proton, according to X! Tandem

    // Bean variables
    private int _runId;
    private int _fractionId;
    private int _scan;
    private int _charge;
    private Float _rawScore;
    private Float _diffScore;
    private Float _zScore;
    private float _ionPercent;
    private double _mass;
    private float _deltaMass;
    private Float _peptideProphet;
    private String _peptide;
    private String _protein;
    private int _proteinHits;
    private long _rowId;
    private Integer _seqId;
    private int _hitRank;
    private Integer _queryNumber;
    private boolean _decoy;

    // Calculated variables
    private Map<MassType, double[]> _massTables = new HashMap<>();
    private Map<MassType, Map<String, Double>> _variableModifications = new HashMap<>();
    private Map<MassType, double[][]> _b = new HashMap<>();
    private Map<MassType, double[][]> _y = new HashMap<>();
    private Map<MassType, FragmentIon[][]> _yIons = new HashMap<>();
    private Map<MassType, FragmentIon[][]> _bIons = new HashMap<>();
    private float[] _spectrumMZ = null;
    private float[] _spectrumIntensity = null;
    private String[] _aa = {};
    private int _aaCount = 0;
    private int _fragmentCount = 0;
    private int _ionCount = 0;
    private String _trimmedPeptide;

    private PeptideQuantitation _quantitation;
    private LibraQuantResult _libraQuantResult;
    private String _spectrumErrorMessage;

    public MS2Peptide()
    {
    }


    // TODO: Move into constructor?  Or rename?  Or just do on demand (when requesting _massMatches, etc.)?
    public void init(double tolerance, double xStart, double xEnd)
    {
        MS2Run run = MS2Manager.getRun(_runId);

        try
        {
            Pair<float[], float[]> spectrum = MS2Manager.getSpectrum(_fractionId, _scan);
            _spectrumMZ = spectrum.first;
            _spectrumIntensity = spectrum.second;
        }
        catch (SpectrumException e)
        {
            _spectrumMZ = new float[0];
            _spectrumIntensity = new float[0];
            _spectrumErrorMessage = e.getMessage();
        }

        _ionCount = _charge;
        
        for (MassType massType : MassType.values())
        {
            _massTables.put(massType, run.getMassTable(massType));
            _variableModifications.put(massType, run.getVarModifications(massType));

            fragment(massType);

            _bIons.put(massType, computeMatches(_b, "b", tolerance, xStart, xEnd, massType));
            _yIons.put(massType, computeMatches(_y, "y", tolerance, xStart, xEnd, massType));
        }
    }


    // Get rid of previous and next amino acid
    public static String trimPeptide(String peptide)
    {
        String p[] = peptide.split("\\.");

        if (2 < p.length)
            return p[1];
        else
            return peptide;
    }


    // Remove variable modification characters, leaving only A-Z
    public static String stripPeptide(String peptide)
    {
        return stripPeptideAZ(peptide);
    }


    // Remove variable modifications and '.', leaving only A-Z
    public static String stripPeptideAZ(String peptide)
    {
        StringBuffer stripped = new StringBuffer();

        for (int i = 0; i < peptide.length(); i++)
        {
            char c = peptide.charAt(i);
            if (c >= 'A' && c <= 'Z')
                stripped.append(c);
        }

        return stripped.toString();
    }


    // String variable modifications and '.', leaving '-' and A-Z
    public static String stripPeptideAZDash(String peptide)
    {
        StringBuffer stripped = new StringBuffer();

        for (int i = 0; i < peptide.length(); i++)
        {
            char c = peptide.charAt(i);
            if (c >= 'A' && c <= 'Z' || c == '-')
                stripped.append(c);
        }

        return stripped.toString();
    }


    // Remove variable modifications, leaving '-', '.', and A-Z
    public static String stripPeptideAZDashPeriod(String peptide)
    {
        StringBuffer stripped = new StringBuffer();

        for (int i = 0; i < peptide.length(); i++)
        {
            char c = peptide.charAt(i);
            if (c >= 'A' && c <= 'Z' || c == '-' || c == '.')
                stripped.append(c);
        }

        return stripped.toString();
    }


    private void fragment(MassType massType)
    {
        // TODO: Rename to eliminate confusion between trimmedPeptide, trimPeptide(), and _trimmedPeptide
        String trimmedPeptide = trimPeptide(_peptide);

        // Break up peptide into amino acid ArrayList
        List<String> aaList = new ArrayList<>(trimmedPeptide.length());
        String prev = null;

        for (int i = 0; i < trimmedPeptide.length(); i++)
        {
            char current = trimmedPeptide.charAt(i);

            if ('A' <= current && 'Z' >= current)
            {
                if (null != prev)
                    aaList.add(prev);

                prev = String.valueOf(current);
            }
            else
            {
                if (null != prev)
                    prev += current;
            }
        }

        if (null != prev)
            aaList.add(prev);

        // Now that we have an amino acid count, convert into an array
        _aaCount = aaList.size();
        _aa = aaList.toArray(new String[_aaCount]);

        // Compute mass of each amino acid (including modifications)
        double aaMass[] = new double[_aaCount];

        for (int i = 0; i < _aaCount; i++)
        {
            String aa = _aa[i];
            aaMass[i] = _massTables.get(massType)[aa.charAt(0) - 65];

            // Variable modification... look it up and add the mass difference
            if (1 < aa.length())
            {
                Double massDiff = _variableModifications.get(massType).get(aa);
                if (null != massDiff)
                    aaMass[i] += massDiff;
                else
                    _log.error("Unknown modification: " + aa);
            }
        }

        _fragmentCount = _aaCount - 1;

        // Handle case of spectrum that didn't get a match
        if (_fragmentCount <= 0)
        {
            _b.put(massType, new double[0][]);
            _y.put(massType, new double[0][]);
            _yIons.put(massType, new FragmentIon[0][]);
            _bIons.put(massType, new FragmentIon[0][]);
            return;
        }

        double[][] b = new double[_ionCount][_fragmentCount];
        double[][] y = new double[_ionCount][_fragmentCount];

        _b.put(massType, b);
        _y.put(massType, y);

        // Compute b+ and y+ ions
        double bTotal[] = new double[_ionCount];
        double yTotal[] = new double[_ionCount];

        for (int i = 0; i < _ionCount; i++)
        {
            bTotal[i] = massType.getHydrogenMass();                                      // Extra H on N-terminal
            yTotal[i] = (massType.getHydrogenMass() * 2 + massType.getOxygenMass()) / (i + 1) + massType.getHydrogenMass();      // Extra OH on C-terminal
        }

        for (int i = 0; i < _fragmentCount; i++)
            for (int j = 0; j < _ionCount; j++)
            {
                bTotal[j] = bTotal[j] + aaMass[i] / (j + 1);
                b[j][i] = bTotal[j];
                yTotal[j] = yTotal[j] + aaMass[_fragmentCount - i] / (j + 1);
                y[j][i] = yTotal[j];
            }
    }

    public static class FragmentIon
    {
        private double _theoreticalMZ;
        private double _observedMZ;
        private double _intensity;
        private MassType _massType;

        private final static double NO_MATCH = -1.0;

        public FragmentIon(double theoreticalMZ, MassType massType)
        {
            this(theoreticalMZ, massType, NO_MATCH, NO_MATCH);
        }

        public FragmentIon(double theoreticalMZ, MassType massType, double observedMZ, double intensity)
        {
            _theoreticalMZ = theoreticalMZ;
            _observedMZ = observedMZ;
            _intensity = intensity;
            _massType = massType;
        }

        public boolean isMatch()
        {
            return _observedMZ != NO_MATCH;
        }

        public double getTheoreticalMZ()
        {
            return _theoreticalMZ;
        }

        public double getObservedMZ()
        {
            return _observedMZ;
        }

        public double getIntensity()
        {
            return _intensity;
        }

        public MassType getMassType()
        {
            return _massType;
        }
    }

    private FragmentIon[][] computeMatches(Map<MassType, double[][]> fragments, String fragmentPrefix, double tolerance, double xStart, double xEnd, MassType massType)
    {
        double[][] fragment = fragments.get(massType);
        // Handle case of spectrum that resulted in no matches
        if (0 == fragment.length)
            return new FragmentIon[0][0];

        FragmentIon[][] result = new FragmentIon[fragment.length][fragment[0].length];

        for (int i = 0; i < _ionCount; i++)
            for (int j = 0; j < _fragmentCount; j++)
            {
                double frag = fragment[i][j];
                int maxIdx = -1;

                for (int k = 0; k < _spectrumMZ.length; k++)
                {
                    if (frag > _spectrumMZ[k] - tolerance && frag < _spectrumMZ[k] + tolerance)
                    {
                        if (xStart <= _spectrumMZ[k] && xEnd >= _spectrumMZ[k])
                            if (-1 == maxIdx || _spectrumIntensity[k] > _spectrumIntensity[maxIdx])
                                maxIdx = k;
                    }
                    else if (-1 != maxIdx)
                        break;
                }

                FragmentIon ion;
                if (-1 != maxIdx)
                {
                    ion = new FragmentIon(frag, massType, _spectrumMZ[maxIdx], _spectrumIntensity[maxIdx]);
                }
                else
                {
                    ion = new FragmentIon(frag, massType);
                }
                result[i][j] = ion;
            }

        return result;
    }

    public FragmentIon[][] getBIons(MassType massType)
    {
        return _bIons.get(massType);
    }


    public FragmentIon[][] getYIons(MassType massType)
    {
        return _yIons.get(massType);
    }


    public String[] getAAs()
    {
        return _aa;
    }


    public int getIonCount()
    {
        return _ionCount;
    }


    public int getAACount()
    {
        return _aaCount;
    }


    public int getFragmentCount()
    {
        return _fragmentCount;
    }


    public float[] getSpectrumMZ()
    {
        return _spectrumMZ;
    }
    
    public String getSpectrumErrorMessage()
    {
        return _spectrumErrorMessage;
    }

    public float[] getSpectrumIntensity()
    {
        return _spectrumIntensity;
    }


    public String toString()
    {
        return _peptide;
    }


    public String getTrimmedPeptide()
    {
        if (null == _trimmedPeptide)
            _trimmedPeptide = stripPeptide(trimPeptide(_peptide));

        return _trimmedPeptide;
    }


    public void setTrimmedPeptide(String trimmedPeptide)
    {
        _trimmedPeptide = trimmedPeptide;
    }


    public static double hydrophobicity(String peptide)
    {
        // Trim and strip the peptide to ensure accurate AA length
        peptide = stripPeptide(trimPeptide(peptide));
        return Peptide.getHydrophobicity3(peptide);
    }


    public static String getHydrophobicityAlgorithm()
    {
        return Hydrophobicity3.VERSION;
    }


    public int getRun()
    {
        return _runId;
    }


    public void setRun(int runId)
    {
        _runId = runId;
    }

    public Integer getSeqId()
    {
        return _seqId;
    }

    public void setSeqId(Integer seqId)
    {
        _seqId = seqId;
    }
    
    public int getFraction()
    {
        return _fractionId;
    }


    public void setFraction(int fractionId)
    {
        _fractionId = fractionId;
    }


    public int getScan()
    {
        return _scan;
    }


    public void setScan(int scan)
    {
        _scan = scan;
    }


    public int getCharge()
    {
        return _charge;
    }


    public void setCharge(int charge)
    {
        _charge = charge;
    }


    public Float getRawScore()
    {
        return _rawScore;
    }


    public void setRawScore(Float rawScore)
    {
        _rawScore = rawScore;
    }


    public Float getDiffScore()
    {
        return _diffScore;
    }


    public void setDiffScore(Float diffScore)
    {
        _diffScore = diffScore;
    }


    public Float getZScore()
    {
        return _zScore;
    }


    public void setZScore(Float zScore)
    {
        _zScore = zScore;
    }


    public float getIonPercent()
    {
        return _ionPercent;
    }


    public void setIonPercent(float ionPercent)
    {
        _ionPercent = ionPercent;
    }


    public double getMass()
    {
        return _mass;
    }


    public void setMass(double mass)
    {
        _mass = mass;
    }


    public float getDeltaMass()
    {
        return _deltaMass;
    }


    public void setDeltaMass(float deltaMass)
    {
        _deltaMass = deltaMass;
    }


    public Float getPeptideProphet()
    {
        return _peptideProphet;
    }


    public void setPeptideProphet(Float peptideProphet)
    {
        _peptideProphet = peptideProphet;
    }


    public String getPeptide()
    {
        return _peptide;
    }


    public void setPeptide(String peptide)
    {
        _peptide = peptide;
    }


    public String getProtein()
    {
        return _protein;
    }


    public void setProtein(String protein)
    {
        _protein = protein;
    }


    public int getProteinHits()
    {
        return _proteinHits;
    }


    public void setProteinHits(int proteinHits)
    {
        _proteinHits = proteinHits;
    }

    public long getRowId()
    {
        return _rowId;
    }

    public void setRowId(long rowId)
    {
        _rowId = rowId;
    }

    public PeptideQuantitation getQuantitation()
    {
        if (_quantitation == null)
        {
            _quantitation = MS2Manager.getQuantitation(getRowId());
        }
        return _quantitation;
    }

    public LibraQuantResult getLibraQuantResult()
    {
        if (_libraQuantResult == null)
        {
            _libraQuantResult = MS2Manager.getLibraQuantResult(getRowId());
        }
        return _libraQuantResult;
    }

    public int getHitRank()
    {
        return _hitRank;
    }

    public void setHitRank(int hitRank)
    {
        _hitRank = hitRank;
    }

    public Integer getQueryNumber()
    {
        return _queryNumber;
    }

    public void setQueryNumber(Integer queryNumber)
    {
        _queryNumber = queryNumber;
    }

    public boolean isDecoy()
    {
        return _decoy;
    }

    public void setDecoy(boolean decoy)
    {
        _decoy = decoy;
    }
}
