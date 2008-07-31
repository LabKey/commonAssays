/*
 * Copyright (c) 2004-2008 Fred Hutchinson Cancer Research Center
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

import java.util.*;

public class Protein
{
    private static Logger _log = Logger.getLogger(Protein.class);

    private double _mass;
    private String _sequence;
    private int _seqId;
    private String _description;
    private String _bestName;
    private String _bestGeneName;

    // TODO: Delete
    private String _lookupString;

    private String[] _peptides;
    private List<Range> _coverageRanges;
    private boolean _computeCoverage = true;
    private boolean _showEntireFragmentInCoverage = false;

    public Protein()
    {
    }


    public String getLookupString()
    {
        return _lookupString;
    }


    public void setLookupString(String lookupString)
    {
        _lookupString = lookupString;
    }


    public String getSequence()
    {
        return _sequence;
    }


    public void setSequence(String sequence)
    {
        _sequence = (sequence == null ? "" : sequence);    // Sequence can be null if FASTA is not loaded
        _computeCoverage = true;
    }


    public double getMass()
    {
        return _mass;
    }


    public void setMass(double mass)
    {
        _mass = mass;
    }


    public int getSeqId()
    {
        return _seqId;
    }


    public void setSeqId(int seqId)
    {
        _seqId = seqId;
    }


    public String getDescription()
    {
        return _description;
    }


    public void setDescription(String description)
    {
        _description = description;
    }


    public String getBestName()
    {
        return _bestName;
    }


    public void setBestName(String bestName)
    {
        _bestName = bestName;
    }


    public String getBestGeneName()
    {
        return _bestGeneName;
    }


    public void setBestGeneName(String bestGeneName)
    {
        _bestGeneName = bestGeneName;
    }


    static final String startTag = "<font class=\"labkey-message\"><u>";
    static final String endTag = "</u></font>";

    public StringBuffer getFormattedSequence()
    {
        StringBuffer formatted = new StringBuffer(_sequence);

        for (int i = 10; i < formatted.length(); i += 11)
            formatted.insert(i, ' ');

        formatted.append(' ');                           // Append a space to ensure that insert always works, even at the very end
        List<Range> ranges = getCoverageRanges();
        int offset = 0;

        for (Range range : ranges)
        {
            formatted.insert(range.start + Math.round(range.start / 10) + offset, startTag);
            offset += startTag.length();
            int end = range.start + range.length;
            formatted.insert(end + Math.round((end - 1) / 10) + offset, endTag);
            offset += endTag.length();
        }

        formatted.deleteCharAt(formatted.length() - 1);  // Get rid of extra space at end
        return formatted;
    }


    public void setPeptides(String[] peptides)
    {
        _peptides = peptides;
        _computeCoverage = true;
    }


    public void setShowEntireFragmentInCoverage(boolean showEntireFragmentInCoverage)
    {
        _showEntireFragmentInCoverage = showEntireFragmentInCoverage;
        _computeCoverage = true;
    }


    public double getAAPercent()
    {
        return (double) getAACoverage() / _sequence.length();
    }

    public int getAACoverage()
    {
        List<Range> ranges = getCoverageRanges();
        int total = 0;

        for (Range range : ranges)
            total += range.length;

        return total;
    }


    public double getMassPercent()
    {
        return getMassCoverage() / getMass();
    }


    public double getMassCoverage()
    {
        List<Range> ranges = getCoverageRanges();
        double total = 0;

        for (Range range : ranges)
            total += getSequenceMass(_sequence.substring(range.start, range.start + range.length));

        return total;
    }


    public static double getSequenceMass(String s)
    {
        double total = 0;

        for (int i = 0; i < s.length(); i++)
            total += MS2Run.aaMassTable[s.charAt(i) - 'A'];

        return total;
    }


    private List<Range> getCoverageRanges()
    {
        if (!_computeCoverage)
            return _coverageRanges;

        if ("".equals(_sequence) || _peptides == null)     // Optimize case where sequence isn't available (FASTA not loaded)
        {
            _computeCoverage = false;
            _coverageRanges = new ArrayList<Range>(0);
            return _coverageRanges;
        }

        // Get rid of variable modification chars and '.', leaving first & last AA (or '-') IF PRESENT
        // and peptide AAs.  Store stripped peptides in a Set to generate a list of unique peptides
        Set<String> unique = new HashSet<String>(_peptides.length);

        for (String peptide : _peptides)
            unique.add(MS2Peptide.stripPeptideAZDash(peptide));

        List<Range> ranges = new ArrayList<Range>(unique.size());

        for (String peptide : unique)
        {
            if (peptide.charAt(0) == '-')
            {
                if (peptide.charAt(peptide.length() - 1) == '-')  // Rare case where peptide is an entire protein sequence
                {
                    if (_sequence.equals(peptide.substring(1, peptide.length() - 1)))
                        ranges.add(new Range(0, peptide.length() - 2));
                    else
                        _log.error(peptide + " doesn't match sequence");
                }
                else
                {
                    if (_sequence.startsWith(peptide.substring(1)))
                        ranges.add(new Range(0, peptide.length() - 2));
                    else
                        _log.error("Can't find " + peptide + " at start of sequence");
                }
            }
            else if (peptide.charAt(peptide.length() - 1) == '-')
            {
                if (_sequence.endsWith(peptide.substring(0, peptide.length() - 1)))
                    ranges.add(new Range(_sequence.length() - (peptide.length() - 2), peptide.length() - 2));
                else
                    _log.error("Can't find " + peptide + " at end of sequence");
            }
            else
            {
                int start = _sequence.indexOf(peptide);

                if (start <= -1)
                {
                    _log.error("Can't find " + peptide + " in middle of sequence");
                    continue;
                }

                while (start > -1)
                {
                    if (_showEntireFragmentInCoverage)
                        ranges.add(new Range(start, peptide.length()));             // Used when searching all proteins for a particular sequence (when prev/next AAs are not specified)
                    else
                        ranges.add(new Range(start + 1, peptide.length() - 2));     // Used when calculating coverage of peptides having specific prev/next AAs

                    start = _sequence.indexOf(peptide, start + 1);
                }
            }
        }

        // Sort ranges based on starting point
        Collections.sort(ranges);

        // Coalesce ranges
        _coverageRanges = new ArrayList<Range>(ranges.size());
        int start = -1;
        int end = -1;

        for (Range range : ranges)
        {
            if (range.start <= end)
                end = Math.max(end, range.start + range.length);
            else
            {
                if (start > -1)
                    _coverageRanges.add(new Range(start, end - start));

                start = range.start;
                end = range.start + range.length;
            }
        }

        if (start > -1)
            _coverageRanges.add(new Range(start, end - start));

        _computeCoverage = false;
        return _coverageRanges;
    }


    public static Range findPeptide(String peptide, String sequence)
    {
        return findPeptide(peptide, sequence, 0);
    }


    public static Range findPeptide(String peptide, String sequence, int startIndex)
    {
        if (peptide.charAt(0) == '-')
        {
            if (peptide.charAt(peptide.length() - 1) == '-')  // Rare case where peptide is an entire protein sequence
            {
                if (sequence.equals(peptide.substring(1, peptide.length() - 1)))
                    return new Range(0, peptide.length() - 2);
            }
            else
            {
                if (sequence.startsWith(peptide.substring(1)))
                    return new Range(0, peptide.length() - 2);
            }
        }
        else if (peptide.charAt(peptide.length() - 1) == '-')
        {
            if (sequence.endsWith(peptide.substring(0, peptide.length() - 1)))
                return new Range(sequence.length() - (peptide.length() - 2), peptide.length() - 2);
        }
        else
        {
            int start = sequence.indexOf(peptide, startIndex);

            if (start > -1)
                return new Range(start + 1, peptide.length() - 2);
        }

        return null;
    }


    public static class Range implements Comparable
    {
        public int start;
        public int length;

        Range(int start, int length)
        {
            this.start = start;
            this.length = length;
        }

        public int compareTo(Object o)
        {
            if (start < ((Range) o).start) return -1;
            if (start > ((Range) o).start) return 1;
            return 0;
        }
    }
}
