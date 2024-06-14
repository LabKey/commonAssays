package org.labkey.ms2.protein;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.protein.PeptideCharacteristic;
import org.labkey.api.protein.ProteinFeature;
import org.labkey.api.protein.SimpleProtein;
import org.labkey.api.util.HtmlString;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.ms2.MS2Peptide;
import org.labkey.ms2.MassType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;

public class CoverageProtein extends SimpleProtein
{
    private static final Logger _log = LogManager.getLogger(CoverageProtein.class);

    // _modifiedPeptideCharacteristics contains peptides and their modified forms
    protected List<PeptideCharacteristic> _modifiedPeptideCharacteristics;
    protected boolean _forCoverageMapExport = false;
    protected List<PeptideCharacteristic> _combinedPeptideCharacteristics;
    protected Map<String, List<PeptideCharacteristic>> _modifiedPeptides;
    protected boolean _showEntireFragmentInCoverage = false;
    protected boolean _showStakedPeptides = false;
    protected boolean _computeCoverage = true;

    private static final String PEPTIDE_START_TD="<td class=\"%s\" style=\"%s\" colspan=%d > %s </td>";

    // In the export cases, we need to in-line the styles since we don't have the external CSS to reference
    private static final String PEPTIDE_START_TD_EXPORT ="<td class=\"%s\" colspan=%d  bgcolor=\"#99ccff\" align=\"center\" > %s </td>";
    private static final String PEPTIDE_START_TD_EXPORT_MULTIPLE ="<td class=\"%s\" colspan=%d  bgcolor=\"#CC99FF\" align=\"center\" > %s </td>";
    private static final String PEPTIDE_START_CLASS =" peptide-marker ";
    private static final String PEPTIDE_MULTIPLE_CLASS =" peptide-marker-multiple ";
    private static final String COLUMN_DIVIDER_CLASS = "tenth-col";
    private static final String PEPTIDE_MIDDLE_TD ="";
    private static final String PEPTIDE_NONE_TD="<td %s />";
    private static final String SEQUENCE_CELL_TD="<td %s >%s</td>";
    private static final String TABLE_TAG="<table id=\"peptideMap\" width=\"%d\" class=\"protein-coverage-map\"  >";
    private static final String TABLE_CAPTION_TAG = "<caption>The number displayed on each peptide box is the rank</caption>";
    private static final String TABLE_TAG_EXPORT="<div><table id=\"peptideMap\" border=\"1\"  >";

    public CoverageProtein()
    {
    }

    public CoverageProtein(SimpleProtein simpleProtein)
    {
        super(simpleProtein);
    }

    public HtmlString getCoverageMap(@Nullable ModificationHandler modificationHandler, @Nullable String showRunViewUrl, int wrapCols, List<ProteinFeature> features)
    {
        if (_forCoverageMapExport)
            wrapCols = 16384;  //Excel's max number of columns

        List<SequencePos> seqStacks = new ArrayList<>();

        int featureIndex = 0;

        // build an arraylist of sequence objects and initialize them with the AA for their position.
        for (int i = 0; i < _sequence.length(); i++)
        {
            List<ProteinFeature> aaFeatures = new ArrayList<>();
            while (featureIndex < features.size() && features.get(featureIndex).getStartIndex() - 1 == i)
            {
                aaFeatures.add(features.get(featureIndex));
                featureIndex++;
            }

            SequencePos pos = new SequencePos(_sequence.charAt(i), i, aaFeatures);
            seqStacks.add(pos);
        }

        List<Range> ranges = getUncoalescedPeptideRanges(modificationHandler);

        // now add the information on covering peptides.
        int overallMaxLevels = 0;
        for (Range range : ranges)
        {
            int maxLevelsInRange = 0;

            // find out how many levels of peptides have already been marked to each sequence position in the range
            for (int j = range.start; j < (range.start + range.length ); j++)
                maxLevelsInRange = Math.max(maxLevelsInRange, seqStacks.get(j).getLevels());

            // Need to pass wrapping information to the SequencePos object because it affects the html  to be generated.
            for (int j = range.start; j < (range.start + range.length ); j++)
            {
                int nextRowStart = (int) Math.ceil(j/ wrapCols) * wrapCols;
                int curRowStart = (int) Math.floor(j/ wrapCols) * wrapCols;
                if (curRowStart == nextRowStart)
                    nextRowStart += wrapCols;

                // add a peptide marker at the array position beyond the current max marker level within the range
                seqStacks.get(j).addPeptide(range, maxLevelsInRange, j, curRowStart, nextRowStart, showRunViewUrl);
            }
            // keep track of the deepest marker level across the entire sequence regardless of wrapping
            overallMaxLevels = Math.max(overallMaxLevels, maxLevelsInRange + 1);
        }

        StringBuilder sb = new StringBuilder(seqStacks.size() *  overallMaxLevels * 5);
        StringBuilder address;
        StringBuilder seqs;

        sb.append(_forCoverageMapExport ? TABLE_TAG_EXPORT : String.format(TABLE_TAG, wrapCols * 10));
        if (!_forCoverageMapExport)
        {
            sb.append(TABLE_CAPTION_TAG);
        }
        int colst = 0;
        int lastCol;
        // now go back and asj each position to render their html fo the table
        // add in the tr's as needed
        // 4 types of rows for every wrapCols-long section of the protein
        //      1 address row giving the 0-based ordinal for every 10th position
        //      1 sequence row showing the protein AA at each position
        //      0 or more peptide marker rows showing coverage bars
        //      1 spacer row

        while (colst < seqStacks.size())
        {
            seqs = new StringBuilder(6 * wrapCols);
            address = new StringBuilder(6 * wrapCols);

            lastCol=Math.min(colst + wrapCols - 1, seqStacks.size() - 1);
            address.append("<tr class=\"address-row\" >");
            seqs.append("<tr class=\"sequence-row\" >");

            // loop through each wrapping level
            for (int j=colst; j<= lastCol; j++)
            {
                // generate the address row (not done b SequencePos object)
                if ((j % 10) == 0 )
                {
                    int colspan = Math.min(lastCol-j+1, 10);
                    String td = String.format("<td colspan=%d align=\"left\" >%d</td>", colspan, j + 1);
                    address.append(td);
                }
                // accumulate the sequence row showing protein AA (generated by the SequencePos object)
                seqs.append(seqStacks.get(j).renderCell(0, j));
            }
            address.append("</tr>");
            sb.append(address);
            seqs.append("</tr>");
            sb.append(seqs);

            // accumulate the marker row html (generated by the SequencePos objects)
            for (int i = 1; i < overallMaxLevels; i++)
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
        sb.append("</table>");

        return HtmlString.unsafe(sb.toString());
    }

    public static double getSequenceMass(String s)
    {
        double total = 0;

        for (int i = 0; i < s.length(); i++)
            total += MassType.Average.getAaMasses()[s.charAt(i) - 'A'];

        return total;
    }

    /*
        method extracted from getCoverageRanges. There are 3 different versions of the peptide.
        the UniqueMap holds "stripped" peptides -- they still have the prev and next AA, including a "-" if
        at the end of a protein. The stripped peptide is used to determine uniqueness, and then this
        method matches the stripped peptide to the protein and keeps only the matching trimmed peptide
        (no previous and next AAs) in a range object. the range object was modified to hold the counts
        needed by the ProteinCoverageMap

        TODO:
        unlike the old-style getCoverageRanges, the uncoalesced ranges are not cached in a class-level variable;
        would need to keep separate by run
     */
    protected List<Range> getUncoalescedPeptideRanges(@Nullable ModificationHandler modificationHandler)
    {
        List<Range> uncoalescedPeptideRanges = new ArrayList<>();
        if (_modifiedPeptideCharacteristics != null && !_modifiedPeptideCharacteristics.isEmpty())
        {
            _modifiedPeptides = new LinkedHashMap<>();
            // build the modifiedPeptideMap
            for (PeptideCharacteristic pep : _modifiedPeptideCharacteristics)
            {
                if (_modifiedPeptides.get(pep.getSequence()) == null)
                {
                    List<PeptideCharacteristic> peps = new ArrayList<>();
                    peps.add(pep);
                    _modifiedPeptides.put(pep.getSequence(), peps);
                }
                else
                {
                    _modifiedPeptides.get(pep.getSequence()).add(pep);
                }
            }
        }
        if (_showStakedPeptides)
        {
            return getModifiedPeptideRanges(modificationHandler);
        }
        else
        {
            if ("".equals(_sequence) || _combinedPeptideCharacteristics == null)     // Optimize case where sequence isn't available (FASTA not loaded)
                return uncoalescedPeptideRanges;

            Map<String,PeptideCounts> uniqueMap = getUniquePeptides(modificationHandler);

            List<Range> ranges = new ArrayList<>(uniqueMap.size());

            if (modificationHandler != null)  // in new style coverage map, we always have a run and are only looking for the trimmed part of the peptide
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
                        {
                            cnt.setCountInstances(instanceNum);
                            cnt.setStartIndex(start);
                            cnt.setEndIndex(start + trimmedPeptide.length() - 1);
                        }
                        ranges.add(new Range(start, trimmedPeptide.length(), cnt));
                        start = _sequence.indexOf(trimmedPeptide, start + 1);
                    }
                }
            }
            else // old style coverage. uses stripped peptide and matches beginning and end chars
            {
                for (String peptide : uniqueMap.keySet())
                {
                    PeptideCounts cnt = uniqueMap.get(peptide);
                    if (peptide.charAt(0) == '-')
                    {
                        cnt.setStartIndex(0);
                        cnt.setEndIndex(peptide.length() - 2 -1);
                        if (_sequence.startsWith(peptide.substring(1)))
                            ranges.add(new Range(0, peptide.length() - 2, cnt));
                        else
                            _log.debug("Can't find " + peptide + " at start of sequence");
                    }
                    else if (peptide.charAt(peptide.length() - 1) == '-')
                    {
                        var start = _sequence.length() - (peptide.length() - 2);
                        var length = peptide.length() - 2;
                        cnt.setStartIndex(start);
                        cnt.setEndIndex(start + length -1);
                        if (_sequence.endsWith(peptide.substring(0, peptide.length() - 1)))
                            ranges.add(new Range(start, length, cnt));
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
                            {
                                cnt.setStartIndex(start);
                                cnt.setEndIndex(start + peptide.length() - 1);
                                ranges.add(new Range(start, peptide.length(), uniqueMap.get(peptide)));             // Used when searching all proteins for a particular sequence (when prev/next AAs are not specified)
                            }
                            else
                            {
                                cnt.setStartIndex(start+1);
                                cnt.setEndIndex(start + 1 +peptide.length() - 2 - 1);
                                ranges.add(new Range(start + 1, peptide.length() - 2, uniqueMap.get(peptide)));     // Used when calculating coverage of peptides having specific prev/next AAs
                            }

                            start = _sequence.indexOf(peptide, start + 1);
                        }
                    }
                }
            }
            // Sort ranges based on starting point
            Collections.sort(ranges);
            return ranges;
        }
    }

    public List<Range> getModifiedPeptideRanges(@Nullable ModificationHandler modificationHandler)
    {
        List<Range> modifiedPeptides = new ArrayList<>();
        if (null == _modifiedPeptideCharacteristics || _modifiedPeptideCharacteristics.isEmpty())
            return modifiedPeptides;

        for (PeptideCharacteristic peptide : _modifiedPeptideCharacteristics)
        {
            String peptideToMap;
            if (modificationHandler == null)
                peptideToMap = MS2Peptide.stripPeptideAZDash(peptide.getSequence());
            else
                peptideToMap = MS2Peptide.stripPeptide(MS2Peptide.trimPeptide(peptide.getSequence()));

            int start = _sequence.indexOf(peptideToMap);
            if (start <= -1)
            {
                // In most cases we've pre-filtered to just the peptides for a certain protein, but there
                // are scenarios where we are looking at all peptides from the current run
                continue;
            }

            PeptideCounts peptideCounts = createPeptideCounts(peptide, false);
            peptideCounts.setStartIndex(start);
            peptideCounts.setEndIndex(start + peptideToMap.length() -1);
            peptideCounts.addPeptide(peptide.getSequence(), modificationHandler);

            Range range = new Range(start, peptideToMap.length(), peptideCounts);
            modifiedPeptides.add(range);
        }
        return modifiedPeptides;
    }

    private PeptideCounts createPeptideCounts(PeptideCharacteristic peptide, boolean isCombinedPeptides)
    {
        PeptideCounts peptideCounts = new PeptideCounts();
        peptideCounts.setIntensity(peptide.getIntensity());
        peptideCounts.setIntensityRank(peptide.getIntensityRank());
        peptideCounts.setRawIntensity(peptide.getRawIntensity());
        peptideCounts.setConfidence(peptide.getConfidence());
        peptideCounts.setRawConfidence(peptide.getRawConfidence());
        peptideCounts.setConfidenceRank(peptide.getConfidenceRank());
        peptideCounts.setForegroundColor(peptide.getForegroundColor());
        peptideCounts.setPeptideColor(peptide.getColor());
        peptideCounts.setSequence(peptide.getSequence());
        if (isCombinedPeptides)
        {
            peptideCounts.setModifiedSequence(peptide.getSequence()); // intentionally setting to sequence for combined peptides
        }
        else
        {
            peptideCounts.setModifiedSequence(peptide.getModifiedSequence());
        }
        return peptideCounts;
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
    public Map<String, PeptideCounts> getUniquePeptides(@Nullable ModificationHandler modificationHandler)
    {
        Map<String, PeptideCounts> uniquePeptides = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (null == _combinedPeptideCharacteristics || _combinedPeptideCharacteristics.size() <= 0)
            return uniquePeptides;

        for (PeptideCharacteristic peptide : _combinedPeptideCharacteristics)
        {
            String peptideToMap;
            if (modificationHandler == null)
                peptideToMap = MS2Peptide.stripPeptideAZDash(peptide.getSequence());
            else
                peptideToMap = MS2Peptide.stripPeptide(MS2Peptide.trimPeptide(peptide.getSequence()));

            PeptideCounts cnt;
            cnt = uniquePeptides.get(peptideToMap);
            if (null == cnt)
            {
                PeptideCounts peptideCounts = createPeptideCounts(peptide, true);
                uniquePeptides.put(peptideToMap, peptideCounts);
                cnt = uniquePeptides.get(peptideToMap);
            }
            cnt.addPeptide(peptide.getSequence(), modificationHandler);
        }
        return uniquePeptides;
    }

    public List<PeptideCharacteristic> getCombinedPeptideCharacteristics()
    {
        return _combinedPeptideCharacteristics;
    }

    public List<PeptideCharacteristic> getModifiedPeptideCharacteristics()
    {
        return _modifiedPeptideCharacteristics;
    }

    public void setShowEntireFragmentInCoverage(boolean showEntireFragmentInCoverage)
    {
        if (_showEntireFragmentInCoverage != showEntireFragmentInCoverage)
            _computeCoverage = true;
        _showEntireFragmentInCoverage = showEntireFragmentInCoverage;
    }

    public void setCombinedPeptideCharacteristics(List<PeptideCharacteristic> combinedPeptideCharacteristics)
    {
        _combinedPeptideCharacteristics = combinedPeptideCharacteristics;
        _computeCoverage = true;
    }

    public void setModifiedPeptideCharacteristics(List<PeptideCharacteristic> modifiedPeptideCharacteristics)
    {
        _modifiedPeptideCharacteristics = modifiedPeptideCharacteristics;
    }

    public void setShowStakedPeptides(boolean showStakedPeptides)
    {
        _showStakedPeptides = showStakedPeptides;
    }

    /*
        Used to build the proteinCoverageMap. Represents a single AA in the protein sequence and any peptides that
        overlap that sequence position. When a peptide is added, the necessary html for all the table cells that show
        that peptide is generated. The html for a non-covered AA in the protein is generated only at render time.
     */
    private class SequencePos
    {
        final char _c;
        int _levels;
        final Map<Integer, String> tdMap = new HashMap<>();

        public SequencePos(char c, int curIdx, List<ProteinFeature> features)
        {
            _c = c;

            List<String> cssClasses = new ArrayList<>();
            List<String> tooltip = new ArrayList<>();
            if (!features.isEmpty())
            {
                cssClasses.add("feature-aa");
            }
            for (ProteinFeature feature : features)
            {
                cssClasses.add("feature-" + feature.getType().replaceAll(" ", ""));
                tooltip.add(StringUtils.capitalize(feature.getType()));
            }

            tdMap.put(0, getTD(SEQUENCE_CELL_TD, cssClasses, curIdx, String.valueOf(c),
                    tooltip.isEmpty() ? null : StringUtils.join(tooltip, ", ")));
            _levels = 1;
        }

        String getTD(String template, List<String> cssClasses, int curIdx, String tdValue, String tooltip)
        {
            // add column divider class to put vertical border lines every 10th column
            if ((curIdx + 1) % 10 == 0)
            {
                cssClasses.add(COLUMN_DIVIDER_CLASS);
            }
            String extraAttributes = cssClasses.isEmpty() ? "" : " class=\"" + PageFlowUtil.filter(StringUtils.join(cssClasses, " ")) + "\"";
            if (tooltip != null)
            {
                extraAttributes += " title=" + PageFlowUtil.qh(tooltip);
            }
            return String.format(template, extraAttributes, tdValue);
        }

        int getLevels() {
            return _levels;
        }

        void addPeptide(Range range, Integer newLevel, int curIdx, int curRowStart, int nextRowStart, String showRunViewUrl)
        {
            String td;
            String label;
            int colsCurrentRow = range.length;
            int colsNextRow = 0;
            int colsPreviousRow = 0;
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
            String onClickScript = null;
            double mass;
            StringBuilder details;
            String continuationLeft = "";
            String continuationRight = "";
            PeptideCounts counts;

            if (!_forCoverageMapExport)
            {
                trimmedPeptide= _sequence.substring(range.start,(range.start + range.length));
                if (showRunViewUrl != null)
                {
                    onClickScript = "window.open('" +  showRunViewUrl + "&MS2Peptides.TrimmedPeptide~eq=" + trimmedPeptide
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
                    var txt = counts.countInstances;
                    if (counts.intensityRank != 0)
                    {
                        txt = counts.intensityRank;
                    }
                    else if (counts.confidenceRank != 0)
                    {
                        txt = counts.confidenceRank;
                    }
                    else
                    {
                        txt = counts.countScans;
                    }
                    String linkText = String.format("%d ", txt);
                    if (colsPreviousRow>0)
                        continuationLeft= " &lt;&lt; ";
                    if (colsNextRow>0)
                        continuationRight=" &gt;&gt; ";

                    details = new StringBuilder();

                    if(!_forCoverageMapExport)
                    {
                        mass = getSequenceMass(_sequence.substring(range.start,(range.start + range.length)));
                        details.append(String.format("Mass: %.2f ", mass));
                        if (_combinedPeptideCharacteristics.isEmpty()) // ms2 usage
                        {
                            details.append(String.format("<br/> Total Scans: %d", counts.countScans));
                        }
                    }

                    for (String modStr : counts.getCountModifications().keySet())
                    {
                        String varmod = String.format("%d(%s)", counts.getCountModifications().get(modStr), modStr );
                        linkText += " / " + varmod;
                        if (!_forCoverageMapExport)
                            details.append(varmod);
                    }
                    label = linkText;
                    if (!_forCoverageMapExport)
                    {
                        var showIntensity = range.pepcounts.intensityRank != 0;
                        var showConfidence = range.pepcounts.confidenceRank != 0;

                        details.append(String.format("<br/>Start: %d", range.pepcounts.startIndex + 1));
                        details.append(String.format("<br/>End: %d", range.pepcounts.endIndex + 1));
                        details.append(String.format("<br/>Unmodified: %d", counts.getCountUnmodifiedPeptides()));
                        if (showIntensity)
                        {
                            details.append(String.format("<br/>Intensity Rank: %d", range.pepcounts.intensityRank));
                            details.append(String.format("<br/>Raw Intensity: %.3E", range.pepcounts.rawIntensity));
                            details.append(String.format("<br/>Log 10 Base Intensity: %.2f", range.pepcounts.intensity));
                        }
                        if (showConfidence)
                        {
                            details.append(String.format("<br/>Confidence Score Rank: %d", range.pepcounts.confidenceRank));
                            details.append(String.format("<br/>Raw Confidence: %f", range.pepcounts.rawConfidence));
                            details.append(String.format("<br/>Log 10 Base Confidence Score: %.2f", range.pepcounts.confidence));
                        }
                        if (range.pepcounts.modifiedSequence != null)
                        {
                            if (_modifiedPeptides != null &&
                                    !_modifiedPeptides.isEmpty() &&
                                    _modifiedPeptides.get(range.pepcounts.getSequence()).size() > 1)
                            {
                                details.append("<table class='modified-details-table'>");
                                details.append("<tr>");
                                details.append("<th>Modified Forms</th>");
                                details.append("<th>Log</th>");
                                if (showIntensity)
                                {

                                    details.append("<th>Raw Intensity</th>");
                                }
                                if (showConfidence)
                                {
                                    details.append("<th>Raw Confidence</th>");
                                }
                                details.append("</tr>");

                                for (PeptideCharacteristic pep : _modifiedPeptides.get(range.pepcounts.getSequence()))
                                {
                                    details.append("<tr>");
                                    details.append("<td>").append(PageFlowUtil.filter(pep.getModifiedSequence())).append("</td>");
                                    if (showIntensity)
                                    {
                                        details.append(String.format("<td>%.2f</td>", pep.getIntensity()));
                                        details.append("<td>").append(String.format("%.3E",pep.getRawIntensity())).append("</td>");
                                    }
                                    if (showConfidence)
                                    {
                                        details.append(String.format("<td>%.2f</td>", pep.getConfidence()));
                                        details.append("<td>").append(pep.getRawConfidence()).append("</td>");
                                    }
                                    details.append("</tr>");
                                }
                                details.append("</table>");
                            }

                        }
                        label = PageFlowUtil.popupHelp(HtmlString.unsafe(details.toString()), "Peptide Details")
                                .link(HtmlString.unsafe("<div style=\"color:" + range.pepcounts.foregroundColor +"\">" + linkText + "</div>"))
                                .script(onClickScript)
                                .width(250)
                                .toString();
                    }
                    label = continuationLeft + label + continuationRight;
                }
                else
                    label= "&lt;";  // will  write the label on the next row
            }
            String cssClass = PEPTIDE_START_CLASS;
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

            if ((range.start == curIdx) || (curRowStart == curIdx))
            {
                if (baseOutput.equalsIgnoreCase(PEPTIDE_START_TD))
                {
                    td = String.format(baseOutput, cssClass, "background-color:" + range.pepcounts.peptideColor, colsCurrentRow, label);
                }
                else
                {
                    td = String.format(baseOutput, cssClass, colsCurrentRow, label);
                }
            }
            else
                td = PEPTIDE_MIDDLE_TD;

            tdMap.put(newLevel, td);
            _levels = newLevel + 1;
        }

        String renderCell(Integer level, int curIdx)
        {
            String tdout = tdMap.get(level);
            if (null == tdout)
                tdout = getTD(PEPTIDE_NONE_TD, new ArrayList<>(), curIdx, null, null);
            return tdout;
        }
    }

    /*
        holds counts of scans matching a single peptide sequence, as well as counts of peptides found with modifications
    */
    public static class PeptideCounts
    {
        @Getter
        int countScans;
        @Getter int countUnmodifiedPeptides;
        @Getter Map<String , Integer> countModifications;
        @Getter @Setter
        int countInstances;
        @Getter @Setter String peptideColor;
        @Getter @Setter int intensityRank;
        @Getter @Setter int confidenceRank;
        @Getter @Setter int startIndex;
        @Getter @Setter int endIndex;
        @Getter @Setter Double intensity;
        @Getter @Setter Double rawIntensity;
        @Getter @Setter Double confidence;
        @Getter @Setter Double rawConfidence;
        @Getter @Setter String foregroundColor;
        @Getter @Setter String sequence;
        @Getter @Setter String modifiedSequence;

        public PeptideCounts()
        {
            countModifications = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            countScans=0;
            countUnmodifiedPeptides=0;
            countInstances =0;
        }

        public void addPeptide(String peptide, ModificationHandler modificationHandler)
        {
            countScans++;
            boolean unmodified = true;
            if (null != modificationHandler)
            {
                unmodified = modificationHandler.apply(peptide, countModifications);
            }
            if (unmodified)
                countUnmodifiedPeptides++;
        }
    }

    public static class Range implements Comparable<Range>
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

        @Override
        public int compareTo(Range o)
        {
            return Integer.compare(start, o.start);
        }

        public PeptideCounts getCounts()
        {
            return pepcounts;
        }
    }

    // Given a peptide and a "count modifications" map, updates the map as appropriate and returns true if peptide contains any modifications
    public interface ModificationHandler extends BiFunction<String, Map<String, Integer>, Boolean>
    {
    }
}
