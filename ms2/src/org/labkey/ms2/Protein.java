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

package org.labkey.ms2;

import org.junit.Assert;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.*;

import static org.labkey.api.util.PageFlowUtil.helpPopup;

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

    private boolean _forCoverageMapExport = false;

    private static final String PEPTIDE_START_TD="<td class=\"%s\" colspan=%d > %s </td>";

    // In the export cases, we need to in-line the styles since we don't have the external CSS to reference
    private static final String PEPTIDE_START_TD_EXPORT ="<td class=\"%s\" colspan=%d  bgcolor=\"#99ccff\" align=\"center\" > %s </td>";
    private static final String PEPTIDE_START_TD_EXPORT_MULTIPLE ="<td class=\"%s\" colspan=%d  bgcolor=\"#CC99FF\" align=\"center\" > %s </td>";
    private static final String PEPTIDE_START_CLASS =" peptide-marker ";
    private static final String PEPTIDE_MULTIPLE_CLASS =" peptide-marker-multiple ";
    private static final String COLUMN_DIVIDER_CLASS=" tenth-col ";
    private static final String PEPTIDE_MIDDLE_TD ="";
    private static final String PEPTIDE_NONE_TD="<td %s />";
    private static final String PEPTIDE_NONE_CLASS= "";
    private static final String SEQUENCE_CELL_TD="<td %s >%s</td>";
    private static final String SEQUENCE_CELL_CLASS="";
    private static final String TABLE_TAG="<div><table id=\"peptideMap\" width=\"%d\" class=\"protein-coverage-map\"  >";
    private static final String TABLE_TAG_EXPORT="<div><table id=\"peptideMap\" border=\"1\"  >";

    public static final int DEFAULT_WRAP_COLUMNS = 100;

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


    static final String startTag = "<font color=\"green\" ><u>";
    static final String endTag = "</u></font>";

    public StringBuffer getFormattedSequence(MS2Run run)
    {
        StringBuffer formatted = new StringBuffer(_sequence);

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
        return formatted;
    }
    /*
        Formats and returns an html table showing where peptides matched a specific portion of a protein.
        done in 3 passes. first pass builds up an array of SequencePos objects, one for each AA of the protein
        sequence.  Second pass loops through the range objectds which are the peptide evidence for the
        protein, marking each SequencePos object in the coverage region.  third pass loops through all SequencePos
        objects and accumlates their html output.
     */
    public StringBuilder getCoverageMap(@Nullable MS2Run run, @Nullable String showRunViewUrl)
    {
        return getCoverageMap(run, showRunViewUrl, DEFAULT_WRAP_COLUMNS);
    }

    public StringBuilder getCoverageMap(@Nullable MS2Run run, @Nullable String showRunViewUrl, int wrapCols)
    {
        if (_forCoverageMapExport)
            wrapCols = 16384;  //Excel's max number of columns

        List<SequencePos> seqStacks = new ArrayList<>();

        // build an arraylist of sequence objects and initialize them with the AA for their position.
        for (int i=0; i<_sequence.length(); i++)
        {
            SequencePos pos = new SequencePos(_sequence.charAt(i), i);
            seqStacks.add(pos);
        }

        List<Range> ranges= getUncoalescedPeptideRanges(run);

        // now add the information on on covering peptides.
        Integer  overallMaxLevels=0;
        for (Range range : ranges)
        {
            int maxLevelsInRange=0;

            // find out how many levels of peptides have alraady been marked to each sequence position in the range
            for (int j=range.start; j< (range.start + range.length ); j++)
                maxLevelsInRange = Math.max(maxLevelsInRange, seqStacks.get(j).getLevels());

            // Need to pass wrapping information to the SequencePos object because it affects the html  to be generated.
            for (int j=range.start; j< (range.start + range.length ); j++)
            {
                int nextRowStart = (int) Math.ceil(j/ wrapCols) * wrapCols;
                int curRowStart = (int) Math.floor(j/ wrapCols) * wrapCols;
                if (curRowStart==nextRowStart)
                    nextRowStart += wrapCols;

                // add a peptide marker at the array position beyond the current max marker level within the range
                seqStacks.get(j).addPeptide(range, maxLevelsInRange, j, curRowStart, nextRowStart, showRunViewUrl);
            }
            // keep track of the deepest marker level across the entire sequence regardless of wrapping
            overallMaxLevels = Math.max( overallMaxLevels, maxLevelsInRange + 1);
        }

        StringBuilder sb = new StringBuilder(seqStacks.size() *  overallMaxLevels * 5);
        StringBuilder address;
        StringBuilder seqs;

        sb.append(_forCoverageMapExport?TABLE_TAG_EXPORT:String.format(TABLE_TAG, wrapCols * 10));
        int colst=0;
        int lastCol;
        // now go back and asj each positsion to render their html fo the table
        // add in the tr's as needed
        // 4 types of rows for every wrapCols-long seciton of the protein
        //      1 address row giving the 0-based ordinal for every 10th position
        //      1 sequence row shoing the protein AA at each position
        //      0 or more peptide marker rows showing coverage bars
        //      1 spacer row

        while (colst<seqStacks.size())
        {
            seqs = new StringBuilder(6* wrapCols);
            address = new StringBuilder(6* wrapCols);

            lastCol=Math.min(colst + wrapCols -1, seqStacks.size()-1 );
            address.append("<tr class=\"address-row\" >");
            seqs.append("<tr class=\"sequence-row\" >");

            // loop through each wrapping level
            for (int j=colst; j<= lastCol; j++)
            {
                // generate the address row (not done b SequencePos object)
                if ((j % 10) == 0 )
                {
                    int colspan = Math.min(lastCol-j+1, 10);
                    String td = String.format("<td colspan=%d align=\"left\" >%d</td>", colspan, j);
                    address.append(td);
                }
                // accumulate the sequence row showing protein AA (generated by the SeuqeencePos object)
                seqs.append(seqStacks.get(j).renderCell(0, j));
            }
            address.append("</tr>");
            sb.append(address);
            seqs.append("</tr>");
            sb.append(seqs);

            // accumulate the marker row html (generated by the SequencePos objects)
            for (int i=1; i<  overallMaxLevels; i++)
            {
                sb.append("<tr");
                if (i == 1)
                {
                    sb.append(" class=\"first-peptide-row\"");
                }
                sb.append(">");
                for (int j=colst; j<=lastCol; j++)
                    sb.append(seqStacks.get(j).renderCell(i, j));

                sb.append("</tr>");
            }

            // generate the spacer row
            sb.append("<tr class=\"spacer-row\" ><td class=\"spacer-row\" colspan=");
            sb.append(lastCol-colst +1);
            sb.append(" > </td></tr>");
            colst = lastCol + 1;

        }   //  generate the 4 types of rows again for each wrapping level
        sb.append("</table></div>");

        return sb;
    }

    public void setForCoverageMapExport(boolean forCoverageMapExport)
    {
        _forCoverageMapExport = forCoverageMapExport;
    }

    /*
        new inner class used to build the proteinCoverageMap.  Reprsents a single AA in the protein sequence
        and any peptides that overlap that sequence position.  When a peptide is added, the necesssary html for
        all the table cells that show that peptide is generated. The html for a non-covered AA in the protein
        is generated only at reder time.
     */
    private class SequencePos {
        char _c;
        Integer _levels =0;
        HashMap<Integer, String> tdMap =new HashMap<>();

        public SequencePos(char c, int curIdx) {
            _c=c;
            tdMap.put(0, getTD(SEQUENCE_CELL_TD, SEQUENCE_CELL_CLASS, curIdx, String.valueOf(c) , null)) ;
            _levels=1;
        }

        String getTD(String template, String cssClass, int curIdx, String tdValue , Integer colSpan)
        {
            // add column divider class to put vertical border lines every 10th column
            if ((curIdx+1) % 10 == 0)
            {
                if (cssClass.length()==0)
                    cssClass = "class=\"" + COLUMN_DIVIDER_CLASS + "\" ";
                else
                    cssClass +=  COLUMN_DIVIDER_CLASS;
            }
            String td;
            if (null!=colSpan)
                td = String.format(template, cssClass, colSpan, tdValue );
            else
                td = String.format(template, cssClass, tdValue );
            return td;
        }

        int getLevels() {
            return _levels;
        }

        void addPeptide(Range range, Integer newLevel, int curIdx, int curRowStart, int nextRowStart, String showRunViewUrl) {
            String td;
            String label;
            int colsCurrentRow=range.length;
            int colsNextRow=0;
            int colsPreviousRow=0;
            if (range.start < curRowStart)  // continuation of a marker from the previous row
            {
                colsPreviousRow = curRowStart - range.start;
                colsCurrentRow = colsCurrentRow - colsPreviousRow;
            }
            if (range.start + range.length >= nextRowStart)
            {
                colsNextRow = range.start + range.length - nextRowStart;
                colsCurrentRow = colsCurrentRow - colsNextRow;
            }
            String trimmedPeptide;
            String onClickScript=null;
            Double mass;
            String details = null;
            String continuationLeft="";
            String continuationRight="";
            PeptideCounts counts;

            if (!_forCoverageMapExport)
            {
                trimmedPeptide= _sequence.substring(range.start,(range.start + range.length));
                if (showRunViewUrl != null)
                {
                    onClickScript = "window.open('" +  showRunViewUrl + "&" + MS2Manager.getDataRegionNamePeptides() + ".TrimmedPeptide~eq=" + trimmedPeptide
                            +"', 'showMatchingPeptides');";
                }
            }

            counts = range.getCounts();
            if (colsPreviousRow >= colsCurrentRow)
                label=" &gt;";  // continuation of peptide bar labeled on previous row
            else
            {
                if (colsCurrentRow >= colsNextRow)
                {
                    String linkText = String.format("%d ", counts.countScans );
                    if (colsPreviousRow>0)
                        continuationLeft= " &lt;&lt; ";
                    if (colsNextRow>0)
                        continuationRight=" &gt;&gt; ";

                    if(!_forCoverageMapExport)
                    {
                        mass = getSequenceMass(_sequence.substring(range.start,(range.start + range.length)));
                        details = String.format("Mass: %.2f  \nTotal Scans: %d ", mass, counts.countScans);
                    }

                    for (String modStr : counts.getCountModifications().keySet())
                    {
                        String varmod = String.format("%d(%s)", counts.getCountModifications().get(modStr), modStr );
                        linkText += " / " + varmod;
                        if (!_forCoverageMapExport)
                            details += "\n "+ varmod;
                    }
                    label = linkText;
                    if (!_forCoverageMapExport)
                    {
                        details += String.format("\nUnmodified: %d", counts.getCountUnmodifiedPeptides());
                        label = helpPopup("Peptide Details", details, false, linkText, 200, onClickScript );
                    }
                    label = continuationLeft + label + continuationRight;
                }
                else
                    label= "&lt;";  // will  write the label on the next row
            }
            String cssClass =PEPTIDE_START_CLASS;
            if (counts.getCountInstances() > 1)
                cssClass = PEPTIDE_MULTIPLE_CLASS;

            String baseOutput;
            if (_forCoverageMapExport)
            {
                // Choose the appropriate <TD> with in-lined styling
                if (counts.getCountInstances() > 1)
                {
                    baseOutput = PEPTIDE_START_TD_EXPORT_MULTIPLE;
                }
                else
                {
                    baseOutput = PEPTIDE_START_TD_EXPORT;
                }
            }
            else
            {
                baseOutput = PEPTIDE_START_TD;
            }

            if ((range.start==curIdx) || (curRowStart==curIdx))
                td=String.format(baseOutput, cssClass, colsCurrentRow, label);
            else
                td= PEPTIDE_MIDDLE_TD;

            tdMap.put(newLevel,td);
            _levels = newLevel + 1;
        }

        String renderCell(Integer level, int curIdx)
        {
            String tdout= tdMap.get(level);
            if (null == tdout)
                tdout=getTD(PEPTIDE_NONE_TD, PEPTIDE_NONE_CLASS, curIdx, null, null);
            return tdout;
        }
    }

    public void setPeptides(String... peptides)
    {
        _peptides = peptides;
        _computeCoverage = true;
    }
    /*
        new class to hold counts of scans matching a single peptide sequence, as well as counts of
        peptides found with modificiations
     */
    private class PeptideCounts
    {
        int countScans;
        int countUnmodifiedPeptides;
        Map<String , Integer> countModifications;
        int countInstances;

        public int getCountScans()
        {
            return countScans;
        }

        public int getCountUnmodifiedPeptides()
        {
            return countUnmodifiedPeptides;
        }

        public Map<String, Integer> getCountModifications()
        {
            return countModifications;
        }

        public int getCountInstances()
        {
            return countInstances;
        }

        public void setCountInstances(int n)
        {
            countInstances = n;
        }

        public PeptideCounts()
        {
            countModifications = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            countScans=0;
            countUnmodifiedPeptides=0;
            countInstances =0;
        }

        public void addPeptide(String peptide, List<MS2Modification> mods )
        {
            countScans++;
            boolean unmodified = true;
            if (null!=mods)
            {
                for (MS2Modification mod : mods)
                {
                    if (!mod.getVariable())
                        continue;
                    String marker= mod.getAminoAcid() + mod.getSymbol();
                    if (peptide.contains(marker))
                    {
                        Integer curCount = countModifications.get(marker);
                        if (null == curCount )
                        {
                            countModifications.put(marker, 0);
                            curCount = countModifications.get(marker);
                        }
                        curCount++;
                        countModifications.put(marker, curCount);
                        unmodified = false;
                    }
                }
            }
            if (unmodified)
                countUnmodifiedPeptides++;
        }
    }

    public void setShowEntireFragmentInCoverage(boolean showEntireFragmentInCoverage)
    {
        if (_showEntireFragmentInCoverage != showEntireFragmentInCoverage)
            _computeCoverage = true;
        _showEntireFragmentInCoverage = showEntireFragmentInCoverage;
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


    public static double getSequenceMass(String s)
    {
        double total = 0;

        for (int i = 0; i < s.length(); i++)
            total += MassType.Average.getAaMasses()[s.charAt(i) - 'A'];

        return total;
    }


    private List<Range> getCoverageRanges(MS2Run run)
    {
        if (!_computeCoverage)
            return _coverageRanges;

        if ("".equals(_sequence) || _peptides == null)     // Optimize case where sequence isn't available (FASTA not loaded)
        {
            _computeCoverage = false;
            _coverageRanges = new ArrayList<>(0);
            return _coverageRanges;
        }
        List<Range> ranges = getUncoalescedPeptideRanges(run);
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
    /*
        method extracted from getCoverageRanges.  There are 3 different versions of the peptide.
        the UniqueMap holds "stripped" peptides -- they still have the prev and next AA, including a "-" if
         at the end of a protein.  The stripped peptide is used to determine uniqueness, and then this
        method matches the stripped peptide to the protein and keeps only the matching tirmmed peptide
        (no previous and next AAs) in a range object.  the range object was modified to hold the counts
        needed by the ProteinCoverageMap

        TODO:
        unlike the old-style getCoverageRanges, the uncolaesced ranges are not cached in a class-level variable;
        would need to keep separate by run
     */
    private List<Range> getUncoalescedPeptideRanges(MS2Run run)
    {
        List<Range> uncoalescedPeptideRanges = new ArrayList<>();

        if ("".equals(_sequence) || _peptides == null)     // Optimize case where sequence isn't available (FASTA not loaded)
            return uncoalescedPeptideRanges;

        Map<String,PeptideCounts> uniqueMap = getUniquePeptides(run);

        List<Range> ranges = new ArrayList<>(uniqueMap.size());

        if (run != null)  // in new style coverage map, we always have a run and are only looking for the trimmed part of the peptide
        {
            for (String trimmedPeptide : uniqueMap.keySet())
            {
                int start = _sequence.indexOf(trimmedPeptide);
                if (start <= -1)
                {
                    // In most cases we've pre-filtered to just the peptides for a certain protein, but there
                    // are scenarios where we are looking at all of the peptides from the current run
                    continue;
                }
                int instanceNum=0;
                while (start > -1)
                {
                    instanceNum++;
                    PeptideCounts cnt = uniqueMap.get(trimmedPeptide);
                    if (null != cnt)
                        cnt.setCountInstances(instanceNum);
                    ranges.add(new Range(start, trimmedPeptide.length(), cnt));
                    start = _sequence.indexOf(trimmedPeptide, start + 1);
                }
            }
        }
        else // old style coverage. uses stripped peptide and matches beginning and end chars
        {
            for (String peptide : uniqueMap.keySet())
            {
                if (peptide.charAt(0) == '-')
                {
                    if (_sequence.startsWith(peptide.substring(1)))
                        ranges.add(new Range(0, peptide.length() - 2, uniqueMap.get(peptide)));
                    else
                        _log.debug("Can't find " + peptide + " at start of sequence");
                }
                else if (peptide.charAt(peptide.length() - 1) == '-')
                {
                    if (_sequence.endsWith(peptide.substring(0, peptide.length() - 1)))
                        ranges.add(new Range(_sequence.length() - (peptide.length() - 2), peptide.length() - 2, uniqueMap.get(peptide)));
                    else
                        _log.debug("Can't find " + peptide + " at end of sequence");
                }
                else
                {
                    int start = _sequence.indexOf(peptide);

                    if (start <= -1)
                    {
                        _log.debug("Can't find " + peptide + " in middle of sequence");
                        continue;
                    }

                    while (start > -1)
                    {
                        if (_showEntireFragmentInCoverage)
                            ranges.add(new Range(start, peptide.length(), uniqueMap.get(peptide)));             // Used when searching all proteins for a particular sequence (when prev/next AAs are not specified)
                        else
                            ranges.add(new Range(start + 1, peptide.length() - 2, uniqueMap.get(peptide)));     // Used when calculating coverage of peptides having specific prev/next AAs

                        start = _sequence.indexOf(peptide, start + 1);
                    }
                }
            }
        }
        // Sort ranges based on starting point
        Collections.sort(ranges);

        return ranges;
    }

    /*
         Method extracted from getCoverageRanges.
         Old style coverage list:  Get rid of variable modification chars and '.',
         leaving first & last AA (or '-') IF PRESENT and peptide AAs.
         Store stripped peptides in a Set to generate a list of unique peptides.

         New coverage map:  Get trimmed peptide, taking only the main peptide sequence
         without beginning and end markers, periods, and modification characters.
        Changed to a map to keep track of number of duplicates and counts of modification status
        the keyset of the map becomes the set of unique peptides.
    */
    public Map<String, PeptideCounts> getUniquePeptides(MS2Run run)
    {
        Map<String, PeptideCounts> uniquePeptides = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (null == _peptides || _peptides.length <= 0)
            return uniquePeptides;

        // if called from old-style getCoverageRanges, the run value is 0 and we don't care about modifications
        List<MS2Modification> mods = Collections.emptyList();
        if (run != null && run.getRun() > -1)
            mods = MS2Manager.getModifications(run);

        for (String peptide : _peptides)
        {
            String peptideToMap;
            if (run == null)
                peptideToMap = MS2Peptide.stripPeptideAZDash(peptide);
            else
                peptideToMap = MS2Peptide.stripPeptide(MS2Peptide.trimPeptide(peptide));

            PeptideCounts cnt;
            cnt = uniquePeptides.get(peptideToMap);
            if (null == cnt)
            {
                uniquePeptides.put(peptideToMap,new PeptideCounts());
                cnt = uniquePeptides.get(peptideToMap);
            }
            cnt.addPeptide(peptide, mods);
        }
        return uniquePeptides;
    }

    public static class Range implements Comparable
    {
        public int start;
        public int length;
        private PeptideCounts pepcounts;

        Range(int start, int length)
        {
            this.start = start;
            this.length = length;
        }

        Range(int start, int length, PeptideCounts counts)
        {
            this.start = start;
            this.length = length;
            this.pepcounts = counts;
        }
        public int compareTo(Object o)
        {
            if (start < ((Range) o).start) return -1;
            if (start > ((Range) o).start) return 1;
            return 0;
        }

        public PeptideCounts getCounts()
        {
            return pepcounts;
        }
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
