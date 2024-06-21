/*
 * Copyright (c) 2004-2017 Fred Hutchinson Cancer Research Center
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

package org.labkey.ms2.protein;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.protein.CoverageProtein;
import org.labkey.api.protein.SimpleProtein;
import org.labkey.api.util.HtmlString;
import org.labkey.ms2.MS2Controller.MS2ModificationHandler;
import org.labkey.ms2.MS2Run;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Protein extends CoverageProtein
{
    // TODO: Delete
    private String _lookupString;

    private List<Range> _coverageRanges;

    public static final int DEFAULT_WRAP_COLUMNS = 100;

    public Protein()
    {
    }

    public Protein(SimpleProtein simpleProtein)
    {
        super(simpleProtein);
    }

    public String getLookupString()
    {
        return _lookupString;
    }

    public void setLookupString(String lookupString)
    {
        _lookupString = lookupString;
    }

    @Override
    public void setSequence(String sequence)
    {
        super.setSequence(sequence);
        _computeCoverage = true;
    }

    /** Field alias for reflection-based object-relational-mapping */
    public String getProtSequence()
    {
        return getSequence();
    }

    /** Field alias for reflection-based object-relational-mapping */
    public void setProtSequence(String sequence)
    {
        setSequence(sequence);
    }

    public boolean isShowStakedPeptides()
    {
        return _showStakedPeptides;
    }

    static final String startTag = "<font color=\"green\" ><u>";
    static final String endTag = "</u></font>";

    public HtmlString getFormattedSequence(MS2Run run)
    {
        StringBuilder formatted = new StringBuilder(_sequence);

        for (int i = 10; i < formatted.length(); i += 11)
            formatted.insert(i, ' ');

        formatted.append(' ');                           // Append a space to ensure that insert always works, even at the very end
        List<Range> ranges = getCoverageRanges(run);
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
        return HtmlString.unsafe(formatted.toString());
    }

    /*
        Formats and returns an html table showing where peptides matched a specific portion of a protein.
        done in 3 passes. first pass builds up an array of SequencePos objects, one for each AA of the protein
        sequence. Second pass loops through the range objects which are the peptide evidence for the
        protein, marking each SequencePos object in the coverage region. third pass loops through all SequencePos
        objects and accumulates their html output.
     */
    public HtmlString getCoverageMap(@Nullable MS2Run run, @Nullable String showRunViewUrl)
    {
        return getCoverageMap(MS2ModificationHandler.of(run), showRunViewUrl, DEFAULT_WRAP_COLUMNS, Collections.emptyList());
    }

    public void setForCoverageMapExport(boolean forCoverageMapExport)
    {
        _forCoverageMapExport = forCoverageMapExport;
    }

    public void setPeptides(String... peptides)
    {
        _computeCoverage = true;
    }

    public double getAAPercent(MS2Run run)
    {
        return (double) getAACoverage(run) / _sequence.length();
    }

    public double getAAPercent()
    {
        return getAAPercent(null);
    }

    public int getAACoverage(MS2Run run)
    {
        List<Range> ranges = getCoverageRanges(run);
        int total = 0;

        for (Range range : ranges)
            total += range.length;

        return total;
    }

    public double getMassPercent(MS2Run run)
    {
        return getMassCoverage(run) / getMass();
    }

    public double getMassCoverage(MS2Run run)
    {
        List<Range> ranges = getCoverageRanges(run);
        double total = 0;

        for (Range range : ranges)
            total += getSequenceMass(_sequence.substring(range.start, range.start + range.length));

        return total;
    }

    private List<Range> getCoverageRanges(MS2Run run)
    {
        if (!_computeCoverage)
            return _coverageRanges;

        if ("".equals(_sequence) || _combinedPeptideCharacteristics == null)     // Optimize case where sequence isn't available (FASTA not loaded)
        {
            _computeCoverage = false;
            _coverageRanges = new ArrayList<>(0);
            return _coverageRanges;
        }
        List<Range> ranges = getUncoalescedPeptideRanges(MS2ModificationHandler.of(run));
        // Coalesce ranges
        // Code below is only used by the old-style collapsed sequence and is unchanged
        _coverageRanges = new ArrayList<>(ranges.size());
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

    /**
     * Returns the set of peptides that are distinct based on their amino acid sequence. Any leading or trailing amino
     * acids are trimmed off, and any modification characters are ignored.
     */
    public static Set<String> getDistinctTrimmedPeptides(String[] peptides)
    {
        Set<String> result = new HashSet<>();
        for (String peptide : peptides)
        {
            StringBuilder trimmedPeptide = new StringBuilder();
            String[] sections = peptide.split("\\.");
            // Should either be of the form "X.AAAAAA.X" or just "AAAAAA". We only care about the "AAAAAA" part
            assert sections.length == 3 || sections.length == 1;
            peptide = sections.length == 3 ? sections[1] : sections[0];
            peptide = peptide.toUpperCase();
            for (int i = 0; i < peptide.length(); i++)
            {
                // Ignore anything that's not an amino acid
                char c = peptide.charAt(i);
                if (c >= 'A' && c <= 'Z')
                {
                    trimmedPeptide.append(c);
                }
            }
            result.add(trimmedPeptide.toString());
        }
        return result;
    }

    public static class TestCase extends Assert
    {
        @Test
        public void testDistinctPeptides()
        {
            assertEquals(1, getDistinctTrimmedPeptides(new String[] {"ABCDE", "ABCDE", "X.ABCDE.R"}).size());
            assertEquals(1, getDistinctTrimmedPeptides(new String[] {"ABCD$E", "AB^CDE", "X.ABCDE.R"}).size());
            assertEquals(3, getDistinctTrimmedPeptides(new String[] {"ABCDE", "ABCE", "X.ABCDEF.R"}).size());
            assertEquals(3, getDistinctTrimmedPeptides(new String[] {"F.ABCDE.-", "ABCE", "X.ABCDEF.R"}).size());
        }
    }
}
