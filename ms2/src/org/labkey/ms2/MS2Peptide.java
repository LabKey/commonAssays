/*
 * Copyright (c) 2004-2010 Fred Hutchinson Cancer Research Center
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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.ms2.Hydrophobicity3;
import org.labkey.ms2.protein.fasta.Peptide;
import org.labkey.api.util.Pair;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MS2Peptide
{
    private static Logger _log = Logger.getLogger(MS2Peptide.class);

    public static final double hMass = 1.00794;
    public static final double pMass = 1.007276;  // Mass of a proton, according to X! Tandem
    public static final double oMass = 15.9994;

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
    private float _peptideProphet;
    private String _peptide;
    private String _protein;
    private int _proteinHits;
    private long _rowId;
    private Integer _seqId;

    // Calculated variables
    private double[] _massTable = null;
    private Map _variableModifications = null;
    private double[][] _b = {};
    private double[][] _y = {};
    private boolean[][] _bMatches = {};
    private boolean[][] _yMatches = {};
    private List<String>[] _massMatches;
    private float[] _spectrumMZ = null;
    private float[] _spectrumIntensity = null;
    private String[] _aa = {};
    private int _aaCount = 0;
    private int _fragmentCount = 0;
    private int _ionCount = 0;
    private String _trimmedPeptide;

    private Quantitation _quantitation;
    private String _spectrumErrorMessage;

    public MS2Peptide()
    {
    }


    // TODO: Move into constructor?  Or rename?  Or just do on demand (when requesting _massMatches, etc.)?
    public void init(double tolerance, double xStart, double xEnd) throws IOException
    {
        MS2Run run = MS2Manager.getRun(_runId);
        _massTable = run.getMassTable();
        _variableModifications = run.getVarModifications();

        _ionCount = _charge;

        fragment();

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

        _massMatches = new ArrayList[_spectrumMZ.length];
        _bMatches = computeMatches(_b, "b", tolerance, xStart, xEnd);
        _yMatches = computeMatches(_y, "y", tolerance, xStart, xEnd);
    }


    public void renderGraph(HttpServletResponse response, double tolerance, double xStart, double xEnd, int width, int height) throws IOException
    {
        init(tolerance, xStart, xEnd);
        SpectrumGraph g = new SpectrumGraph(this, width, height, tolerance, xStart, xEnd);
        g.render(response);
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


    private void fragment()
    {
        // TODO: Rename to eliminate confusion between trimmedPeptide, trimPeptide(), and _trimmedPeptide
        String trimmedPeptide = trimPeptide(_peptide);

        // Break up peptide into amino acid ArrayList
        List<String> aaList = new ArrayList<String>(trimmedPeptide.length());
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
            aaMass[i] = _massTable[aa.charAt(0) - 65];

            // Variable modification... look it up and add the mass difference
            if (1 < aa.length())
            {
                Double massDiff = (Double) _variableModifications.get(aa);
                if (null != massDiff)
                    aaMass[i] += massDiff;
                else
                    _log.error("Unknown modification: " + aa);
            }
        }

        _fragmentCount = _aaCount - 1;

        // Handle case of spectrum that didn't get a match
        if (_fragmentCount <= 0)
            return;

        // Compute b+ and y+ ions
        _b = new double[_ionCount][_fragmentCount];
        _y = new double[_ionCount][_fragmentCount];
        double bTotal[] = new double[_ionCount];
        double yTotal[] = new double[_ionCount];

        for (int i = 0; i < _ionCount; i++)
        {
            bTotal[i] = hMass;                                      // Extra H on N-terminal
            yTotal[i] = (hMass * 2 + oMass) / (i + 1) + hMass;      // Extra OH on C-terminal
        }

        for (int i = 0; i < _fragmentCount; i++)
            for (int j = 0; j < _ionCount; j++)
            {
                bTotal[j] = bTotal[j] + aaMass[i] / (j + 1);
                _b[j][i] = bTotal[j];
                yTotal[j] = yTotal[j] + aaMass[_fragmentCount - i] / (j + 1);
                _y[j][i] = yTotal[j];
            }
    }


    private boolean[][] computeMatches(double fragment[][], String fragmentPrefix, double tolerance, double xStart, double xEnd)
    {
        // Handle case of spectrum that resulted in no matches
        if (0 == fragment.length)
            return new boolean[0][0];

        boolean[][] matches = new boolean[fragment.length][fragment[0].length];

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

                if (-1 != maxIdx)
                {
                    matches[i][j] = true;
                    if (null == _massMatches[maxIdx])
                        _massMatches[maxIdx] = new ArrayList<String>(1);

                    _massMatches[maxIdx].add(fragmentPrefix + (j + 1) + StringUtils.repeat("+", i + 1));
                }
            }

        return matches;
    }


    public double[][] getBFragments()
    {
        return _b;
    }


    public double[][] getYFragments()
    {
        return _y;
    }


    public boolean[][] getBMatches()
    {
        return _bMatches;
    }


    public boolean[][] getYMatches()
    {
        return _yMatches;
    }


    public List<String>[] getMassMatches()
    {
        return _massMatches;
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


    public float getPeptideProphet()
    {
        return _peptideProphet;
    }


    public void setPeptideProphet(float peptideProphet)
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

    public Quantitation getQuantitation()
    {
        if (_quantitation == null)
        {
            _quantitation = MS2Manager.getQuantitation(getRowId());
        }
        return _quantitation;
    }
}
