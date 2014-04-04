/*
 * Copyright (c) 2012-2014 LabKey Corporation
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
import org.junit.Before;
import org.junit.Test;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.NestableQueryView;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.peptideview.QueryPeptideMS2RunView;
import org.labkey.ms2.pipeline.tandem.XTandemRun;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * User: cnathe
 * Date: Aug 23, 2012
 */
public class ProteinCoverageMapBuilder
{
    private ViewContext _context;
    private Protein _protein;
    private MS2Run _ms2Run;
    private SimpleFilter _peptideFilter; // filter with clauses to get peptides for the given seqId
    private Pair<Integer, Integer> _allPeptideCounts; // counts of the number of peptides (total and distinct) that meet the filters set on the URL (besides the protein sequence match)
    private Pair<Integer, Integer> _targetPeptideCounts; // counts of the number of peptides (total and distinct) that match the target protein sequence
    private boolean _showAllPeptides;

    public ProteinCoverageMapBuilder(ViewContext context, Protein protein, MS2Run ms2Run, SimpleFilter peptideFilter, boolean showAllpeptides)
    {
        _context = context;
        _protein = protein;
        _ms2Run = ms2Run;
        _peptideFilter = peptideFilter;
        _showAllPeptides = showAllpeptides;
    }

    public Pair<Integer, Integer> getPeptideCountsForFilter(SimpleFilter filter)
    {
        String[] peptides = getPeptidesForFilter(filter);
        Set<String> distinct = Protein.getDistinctTrimmedPeptides(peptides);
        return new Pair<>(peptides.length, distinct.size());
    }

    public String[] getPeptidesForFilter(SimpleFilter filter)
    {
        QueryPeptideMS2RunView qpmv = new QueryPeptideMS2RunView(_context, _ms2Run);
        NestableQueryView qv = qpmv.createGridView(filter);
        return new TableSelector(qv.getTable(), Collections.singleton("Peptide"), filter, new Sort("Peptide")).getArray(String.class);
    }

    public void setShowAllPeptides(boolean showAllPeptides)
    {
        _showAllPeptides = showAllPeptides;
    }

    public void setProteinPeptides(String[] peptides)
    {
        _protein.setPeptides(peptides);
    }

    public void setTargetPeptideCounts(Pair<Integer, Integer> targetPeptideCounts)
    {
         _targetPeptideCounts = targetPeptideCounts;
    }
    public void setTargetPeptideCounts(SimpleFilter filter)
    {
        _targetPeptideCounts = getPeptideCountsForFilter(filter);
    }

    public void setAllPeptideCounts(Pair<Integer, Integer> allPeptideCounts)
    {
        _allPeptideCounts = allPeptideCounts;
    }
    public void setAllPeptideCounts()
    {
        // setup filter for querying the all peptides for the run (i.e. without the seqId filter clauses)
        SimpleFilter filter = new SimpleFilter();
        for (SimpleFilter.FilterClause clause : _peptideFilter.getClauses())
        {
            if (clause.needsTypeConversion())
                filter.addClause(clause);
        }
        filter.deleteConditions(FieldKey.fromParts("SeqId"));
        _allPeptideCounts = getPeptideCountsForFilter(filter);
    }

    public String getProteinExportHtml()
    {
        _protein.setShowEntireFragmentInCoverage(false);
        _protein.setForCoverageMapExport(true);

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<p> Protein: &nbsp; " + PageFlowUtil.filter(_protein.getBestName()) + (_showAllPeptides ? "  (all matching peptides) " : "  (search engine matches) ") + "<br/> ");
        sb.append("Run: &nbsp; " + (null!=_ms2Run.getDescription()?  PageFlowUtil.filter(_ms2Run.getDescription()) : _ms2Run.getRun()) +  "<br/> ");
        sb.append("Peptide Filter: &nbsp; " + PageFlowUtil.filter(_peptideFilter.getFilterText()) + "<br/> ");
        sb.append("Peptide Counts:<br/>");

        // display the counts of the number of peptides (total and distinct) that match the target protein sequence
        if (_targetPeptideCounts != null)
        {
            sb.append(_targetPeptideCounts.first + " Total peptide" + (_targetPeptideCounts.first != 1 ? "s" : "") + " matching sequence<br/>");
            sb.append(_targetPeptideCounts.second + " Distinct peptide" + (_targetPeptideCounts.second != 1 ? "s" : "") + " matching sequence<br/>");
        }

        // display the counts of the number of peptides (total and distinct) that meet the filters set on the URL (besides the protein sequence match)
        sb.append(_allPeptideCounts.first + " Total qualifying peptide" + (_allPeptideCounts.first != 1 ? "s" : "") + " in run<br/>");
        sb.append(_allPeptideCounts.second + " Distinct qualifying peptide" + (_allPeptideCounts.first != 1 ? "s" : "") + " in run</p>");
        sb.append(_protein.getCoverageMap(_ms2Run, null).toString());

        return sb.toString();
    }

    public static class TestCase extends Assert
    {
        private Protein _protein;
        private MS2Run _run;

        @Before
        public void setUp()
        {
            _protein = new Protein();
            _protein.setBestName("Test Protein 1");
            _protein.setSeqId(-1);
            _protein.setSequence("SQKFGRIINTASPAGLFGNFGQANYSAAKMGRRVIGQLFEVGGGWCGQTRWQRSSGYVSIEQYFKLCTPTMPSNGTLKTLAKPLQVLDKNGKAALVVGGFETYDIKTKKLIAYNEGSFFIRGAHVPPEKE");

            _run = new XTandemRun();
            _run.setRun(-1);
            _run.setDescription("Test Run 1");
        }

        @Test
        public void testNoPrevAndNext()
        {
            SimpleFilter f = new SimpleFilter();
            f.addCondition(FieldKey.fromParts("SeqId"), -1, CompareType.EQUAL);

            String[] peptides = {"LC'TPTMPSNGTLK", "LC'TPTMPSNGTLK", "LC'TPTMPSNGTLK",
                    "LC'TPTMPSNGTLK", "LC'TPTMPSNGTLK.T", "LIAYNEGSFFIR", "IINTASPAGLFGNFGQANYSAAK",
                    "VIGQLFEVGGGWC'GQTR", "VIGQLFEVGGGWC'GQTR"};
            _protein.setShowEntireFragmentInCoverage(true);
            _protein.setForCoverageMapExport(true);
            _protein.setPeptides(peptides);

            String coverage = _protein.getCoverageMap(null, null).toString();
            String expectedHtml = "<div>" +
                    "<table id=\"peptideMap\" border=\"1\"  >" +
                    "<tr class=\"address-row\" ><td colspan=10 align=\"left\" >0</td><td colspan=10 align=\"left\" >10</td><td colspan=10 align=\"left\" >20</td><td colspan=10 align=\"left\" >30</td><td colspan=10 align=\"left\" >40</td><td colspan=10 align=\"left\" >50</td><td colspan=10 align=\"left\" >60</td><td colspan=10 align=\"left\" >70</td><td colspan=10 align=\"left\" >80</td><td colspan=10 align=\"left\" >90</td><td colspan=10 align=\"left\" >100</td><td colspan=10 align=\"left\" >110</td><td colspan=10 align=\"left\" >120</td></tr>" +
                    "<tr class=\"sequence-row\" ><td  >S</td><td  >Q</td><td  >K</td><td  >F</td><td  >G</td><td  >R</td><td  >I</td><td  >I</td><td  >N</td><td class=\" tenth-col \"  >T</td><td  >A</td><td  >S</td><td  >P</td><td  >A</td><td  >G</td><td  >L</td><td  >F</td><td  >G</td><td  >N</td><td class=\" tenth-col \"  >F</td><td  >G</td><td  >Q</td><td  >A</td><td  >N</td><td  >Y</td><td  >S</td><td  >A</td><td  >A</td><td  >K</td><td class=\" tenth-col \"  >M</td><td  >G</td><td  >R</td><td  >R</td><td  >V</td><td  >I</td><td  >G</td><td  >Q</td><td  >L</td><td  >F</td><td class=\" tenth-col \"  >E</td><td  >V</td><td  >G</td><td  >G</td><td  >G</td><td  >W</td><td  >C</td><td  >G</td><td  >Q</td><td  >T</td><td class=\" tenth-col \"  >R</td><td  >W</td><td  >Q</td><td  >R</td><td  >S</td><td  >S</td><td  >G</td><td  >Y</td><td  >V</td><td  >S</td><td class=\" tenth-col \"  >I</td><td  >E</td><td  >Q</td><td  >Y</td><td  >F</td><td  >K</td><td  >L</td><td  >C</td><td  >T</td><td  >P</td><td class=\" tenth-col \"  >T</td><td  >M</td><td  >P</td><td  >S</td><td  >N</td><td  >G</td><td  >T</td><td  >L</td><td  >K</td><td  >T</td><td class=\" tenth-col \"  >L</td><td  >A</td><td  >K</td><td  >P</td><td  >L</td><td  >Q</td><td  >V</td><td  >L</td><td  >D</td><td  >K</td><td class=\" tenth-col \"  >N</td><td  >G</td><td  >K</td><td  >A</td><td  >A</td><td  >L</td><td  >V</td><td  >V</td><td  >G</td><td  >G</td><td class=\" tenth-col \"  >F</td><td  >E</td><td  >T</td><td  >Y</td><td  >D</td><td  >I</td><td  >K</td><td  >T</td><td  >K</td><td  >K</td><td class=\" tenth-col \"  >L</td><td  >I</td><td  >A</td><td  >Y</td><td  >N</td><td  >E</td><td  >G</td><td  >S</td><td  >F</td><td  >F</td><td class=\" tenth-col \"  >I</td><td  >R</td><td  >G</td><td  >A</td><td  >H</td><td  >V</td><td  >P</td><td  >P</td><td  >E</td><td  >K</td><td class=\" tenth-col \"  >E</td></tr>" +
                    "<tr class=\"first-peptide-row\"><td  /><td  /><td  /><td  /><td  /><td  /><td class=\" peptide-marker \" colspan=23  bgcolor=\"#99ccff\" align=\"center\" > 1  </td><td class=\" tenth-col \"  /><td  /><td  /><td  /><td class=\" peptide-marker \" colspan=17  bgcolor=\"#99ccff\" align=\"center\" > 2  </td><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td class=\" tenth-col \"  /><td  /><td  /><td  /><td  /><td  /><td class=\" peptide-marker \" colspan=13  bgcolor=\"#99ccff\" align=\"center\" > 4  </td><td  /><td class=\" tenth-col \"  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td class=\" tenth-col \"  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td class=\" tenth-col \"  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td class=\" peptide-marker \" colspan=12  bgcolor=\"#99ccff\" align=\"center\" > 1  </td><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td class=\" tenth-col \"  /></tr>" +
                    "<tr><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td class=\" tenth-col \"  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td class=\" tenth-col \"  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td class=\" tenth-col \"  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td class=\" tenth-col \"  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td class=\" tenth-col \"  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td class=\" tenth-col \"  /><td  /><td  /><td  /><td  /><td  /><td class=\" peptide-marker \" colspan=14  bgcolor=\"#99ccff\" align=\"center\" > 1  </td><td class=\" tenth-col \"  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td class=\" tenth-col \"  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td class=\" tenth-col \"  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td class=\" tenth-col \"  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td class=\" tenth-col \"  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td class=\" tenth-col \"  /></tr>" +
                    "<tr class=\"spacer-row\" ><td class=\"spacer-row\" colspan=130 > </td></tr>" +
                    "</table></div>";
            assertEquals("Incorrect protein coverage map table HTML", expectedHtml, coverage);
        }

        @Test
        public void testProteinCoverageMapExport()
        {
            SimpleFilter f = new SimpleFilter();
            f.addCondition(FieldKey.fromParts("SeqId"), -1, CompareType.EQUAL);
            String[] peptides1 = {"K.LC'TPTMPSNGTLK.T", "K.LC'TPTMPSNGTLK.T", "K.LC'TPTMPSNGTLK.T",
                    "K.LC'TPTMPSNGTLK.T", "K.LC'TPTMPSNGTLK.T", "K.LIAYNEGSFFIR.G", "R.IINTASPAGLFGNFGQANYSAAK.M", 
                    "R.VIGQLFEVGGGWC'GQTR.W", "R.VIGQLFEVGGGWC'GQTR.W"};
            String[] peptides2 = {"K.LC'TPTMPSNGTLK.T", "K.LC'TPTMPSNGTLK.T", "K.LIAYNEGSFFIR.G", "R.IINTASPAGLFGNFGQANYSAAK.M",
                    "R.VIGQLFEVGGGWC'GQTR.W", "R.VIGQLFEVGGGWC'GQTR.W", "R.VIGQLFEVGGGWC'GQTR.W"};            

            ProteinCoverageMapBuilder pcm1 = new ProteinCoverageMapBuilder(null, _protein, _run, f, false);
            pcm1.setProteinPeptides(peptides1);
            Set<String> distinct = new HashSet<>(Arrays.asList(peptides1));
            Pair<Integer, Integer> counts = new Pair<>(peptides1.length, distinct.size());
            pcm1.setAllPeptideCounts(counts);
            pcm1.setTargetPeptideCounts(counts);

            // verify the header information in the export html
            String exportHtml = pcm1.getProteinExportHtml();
            assertTrue("Unexpected protein name text", exportHtml.contains("Protein: &nbsp; Test Protein 1  (search engine matches)"));
            assertTrue("Unexpected run name text", exportHtml.contains("Run: &nbsp; Test Run 1"));
            assertTrue("Unexpected peptide filter text", exportHtml.contains("Peptide Filter: &nbsp; (SeqId = -1)"));
            assertTrue("Unexpected total peptide count text", exportHtml.contains("9 Total peptides matching sequence"));
            assertTrue("Unexpected distinct peptide count text", exportHtml.contains("4 Distinct peptides matching sequence"));
            assertTrue("Unexpected total peptide count text", exportHtml.contains("9 Total qualifying peptides in run"));
            assertTrue("Unexpected distinct peptide count text", exportHtml.contains("4 Distinct qualifying peptides in run"));

            pcm1.setShowAllPeptides(true);
            exportHtml = pcm1.getProteinExportHtml();
            assertTrue("Unexpected protein name text", exportHtml.contains("Protein: &nbsp; Test Protein 1  (all matching peptides)"));

            // verify the protein coverage map table html for peptides1
            exportHtml = _protein.getCoverageMap(_run, null).toString();
            String expectedHtml = "<div>"
                + "<table id=\"peptideMap\" border=\"1\"  >"
                + "<tr class=\"address-row\" ><td colspan=10 align=\"left\" >0</td><td colspan=10 align=\"left\" >10</td><td colspan=10 align=\"left\" >20</td><td colspan=10 align=\"left\" >30</td><td colspan=10 align=\"left\" >40</td><td colspan=10 align=\"left\" >50</td><td colspan=10 align=\"left\" >60</td><td colspan=10 align=\"left\" >70</td><td colspan=10 align=\"left\" >80</td><td colspan=10 align=\"left\" >90</td><td colspan=10 align=\"left\" >100</td><td colspan=10 align=\"left\" >110</td><td colspan=10 align=\"left\" >120</td></tr>"
                + "<tr class=\"sequence-row\" ><td  >S</td><td  >Q</td><td  >K</td><td  >F</td><td  >G</td><td  >R</td><td  >I</td><td  >I</td><td  >N</td><td class=\" tenth-col \"  >T</td><td  >A</td><td  >S</td><td  >P</td><td  >A</td><td  >G</td><td  >L</td><td  >F</td><td  >G</td><td  >N</td><td class=\" tenth-col \"  >F</td><td  >G</td><td  >Q</td><td  >A</td><td  >N</td><td  >Y</td><td  >S</td><td  >A</td><td  >A</td><td  >K</td><td class=\" tenth-col \"  >M</td><td  >G</td><td  >R</td><td  >R</td><td  >V</td><td  >I</td><td  >G</td><td  >Q</td><td  >L</td><td  >F</td><td class=\" tenth-col \"  >E</td><td  >V</td><td  >G</td><td  >G</td><td  >G</td><td  >W</td><td  >C</td><td  >G</td><td  >Q</td><td  >T</td><td class=\" tenth-col \"  >R</td><td  >W</td><td  >Q</td><td  >R</td><td  >S</td><td  >S</td><td  >G</td><td  >Y</td><td  >V</td><td  >S</td><td class=\" tenth-col \"  >I</td><td  >E</td><td  >Q</td><td  >Y</td><td  >F</td><td  >K</td><td  >L</td><td  >C</td><td  >T</td><td  >P</td><td class=\" tenth-col \"  >T</td><td  >M</td><td  >P</td><td  >S</td><td  >N</td><td  >G</td><td  >T</td><td  >L</td><td  >K</td><td  >T</td><td class=\" tenth-col \"  >L</td><td  >A</td><td  >K</td><td  >P</td><td  >L</td><td  >Q</td><td  >V</td><td  >L</td><td  >D</td><td  >K</td><td class=\" tenth-col \"  >N</td><td  >G</td><td  >K</td><td  >A</td><td  >A</td><td  >L</td><td  >V</td><td  >V</td><td  >G</td><td  >G</td><td class=\" tenth-col \"  >F</td><td  >E</td><td  >T</td><td  >Y</td><td  >D</td><td  >I</td><td  >K</td><td  >T</td><td  >K</td><td  >K</td><td class=\" tenth-col \"  >L</td><td  >I</td><td  >A</td><td  >Y</td><td  >N</td><td  >E</td><td  >G</td><td  >S</td><td  >F</td><td  >F</td><td class=\" tenth-col \"  >I</td><td  >R</td><td  >G</td><td  >A</td><td  >H</td><td  >V</td><td  >P</td><td  >P</td><td  >E</td><td  >K</td><td class=\" tenth-col \"  >E</td></tr>"
                + "<tr class=\"first-peptide-row\"><td  /><td  /><td  /><td  /><td  /><td  /><td class=\" peptide-marker \" colspan=23  bgcolor=\"#99ccff\" align=\"center\" > 1  </td><td class=\" tenth-col \"  /><td  /><td  /><td  /><td class=\" peptide-marker \" colspan=17  bgcolor=\"#99ccff\" align=\"center\" > 2  </td><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td class=\" tenth-col \"  /><td  /><td  /><td  /><td  /><td  /><td class=\" peptide-marker \" colspan=13  bgcolor=\"#99ccff\" align=\"center\" > 5  </td><td  /><td class=\" tenth-col \"  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td class=\" tenth-col \"  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td class=\" tenth-col \"  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td class=\" peptide-marker \" colspan=12  bgcolor=\"#99ccff\" align=\"center\" > 1  </td><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td class=\" tenth-col \"  /></tr>"
                + "<tr class=\"spacer-row\" ><td class=\"spacer-row\" colspan=130 > </td></tr></table>"
                + "</div>";
            assertEquals("Incorrect protein coverage map table HTML", expectedHtml, exportHtml);

            // add some peptide filter conditions and check the header info line
            f.addCondition(FieldKey.fromParts("Scan"), 100, CompareType.NEQ_OR_NULL);
            f.addCondition(FieldKey.fromParts("PeptideProhet"), 0.9, CompareType.GTE);
            ProteinCoverageMapBuilder pcm2 = new ProteinCoverageMapBuilder(null, _protein, _run, f, false);
            pcm2.setProteinPeptides(peptides2);
            distinct = new HashSet<>(Arrays.asList(peptides2));
            counts = new Pair<>(peptides2.length, distinct.size());
            pcm2.setAllPeptideCounts(counts);
            exportHtml = pcm2.getProteinExportHtml();

            assertTrue("Unexpected peptide filter text", exportHtml.contains("Peptide Filter: &nbsp; (SeqId = -1) AND (Scan &lt;&gt; 100) AND (PeptideProhet &gt;= 0.9)"));
            assertTrue("Unexpected total peptide count text", !exportHtml.contains("peptides matching sequence"));
            assertTrue("Unexpected total peptide count text", exportHtml.contains("7 Total qualifying peptides in run"));
            assertTrue("Unexpected distinct peptide count text", exportHtml.contains("4 Distinct qualifying peptides in run"));

            // verify the protein coverage map table html for peptides2
            exportHtml = _protein.getCoverageMap(_run, null).toString();
            expectedHtml = "<div>"
                + "<table id=\"peptideMap\" border=\"1\"  >"
                + "<tr class=\"address-row\" ><td colspan=10 align=\"left\" >0</td><td colspan=10 align=\"left\" >10</td><td colspan=10 align=\"left\" >20</td><td colspan=10 align=\"left\" >30</td><td colspan=10 align=\"left\" >40</td><td colspan=10 align=\"left\" >50</td><td colspan=10 align=\"left\" >60</td><td colspan=10 align=\"left\" >70</td><td colspan=10 align=\"left\" >80</td><td colspan=10 align=\"left\" >90</td><td colspan=10 align=\"left\" >100</td><td colspan=10 align=\"left\" >110</td><td colspan=10 align=\"left\" >120</td></tr>"
                + "<tr class=\"sequence-row\" ><td  >S</td><td  >Q</td><td  >K</td><td  >F</td><td  >G</td><td  >R</td><td  >I</td><td  >I</td><td  >N</td><td class=\" tenth-col \"  >T</td><td  >A</td><td  >S</td><td  >P</td><td  >A</td><td  >G</td><td  >L</td><td  >F</td><td  >G</td><td  >N</td><td class=\" tenth-col \"  >F</td><td  >G</td><td  >Q</td><td  >A</td><td  >N</td><td  >Y</td><td  >S</td><td  >A</td><td  >A</td><td  >K</td><td class=\" tenth-col \"  >M</td><td  >G</td><td  >R</td><td  >R</td><td  >V</td><td  >I</td><td  >G</td><td  >Q</td><td  >L</td><td  >F</td><td class=\" tenth-col \"  >E</td><td  >V</td><td  >G</td><td  >G</td><td  >G</td><td  >W</td><td  >C</td><td  >G</td><td  >Q</td><td  >T</td><td class=\" tenth-col \"  >R</td><td  >W</td><td  >Q</td><td  >R</td><td  >S</td><td  >S</td><td  >G</td><td  >Y</td><td  >V</td><td  >S</td><td class=\" tenth-col \"  >I</td><td  >E</td><td  >Q</td><td  >Y</td><td  >F</td><td  >K</td><td  >L</td><td  >C</td><td  >T</td><td  >P</td><td class=\" tenth-col \"  >T</td><td  >M</td><td  >P</td><td  >S</td><td  >N</td><td  >G</td><td  >T</td><td  >L</td><td  >K</td><td  >T</td><td class=\" tenth-col \"  >L</td><td  >A</td><td  >K</td><td  >P</td><td  >L</td><td  >Q</td><td  >V</td><td  >L</td><td  >D</td><td  >K</td><td class=\" tenth-col \"  >N</td><td  >G</td><td  >K</td><td  >A</td><td  >A</td><td  >L</td><td  >V</td><td  >V</td><td  >G</td><td  >G</td><td class=\" tenth-col \"  >F</td><td  >E</td><td  >T</td><td  >Y</td><td  >D</td><td  >I</td><td  >K</td><td  >T</td><td  >K</td><td  >K</td><td class=\" tenth-col \"  >L</td><td  >I</td><td  >A</td><td  >Y</td><td  >N</td><td  >E</td><td  >G</td><td  >S</td><td  >F</td><td  >F</td><td class=\" tenth-col \"  >I</td><td  >R</td><td  >G</td><td  >A</td><td  >H</td><td  >V</td><td  >P</td><td  >P</td><td  >E</td><td  >K</td><td class=\" tenth-col \"  >E</td></tr>"
                + "<tr class=\"first-peptide-row\"><td  /><td  /><td  /><td  /><td  /><td  /><td class=\" peptide-marker \" colspan=23  bgcolor=\"#99ccff\" align=\"center\" > 1  </td><td class=\" tenth-col \"  /><td  /><td  /><td  /><td class=\" peptide-marker \" colspan=17  bgcolor=\"#99ccff\" align=\"center\" > 3  </td><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td class=\" tenth-col \"  /><td  /><td  /><td  /><td  /><td  /><td class=\" peptide-marker \" colspan=13  bgcolor=\"#99ccff\" align=\"center\" > 2  </td><td  /><td class=\" tenth-col \"  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td class=\" tenth-col \"  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td class=\" tenth-col \"  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td class=\" peptide-marker \" colspan=12  bgcolor=\"#99ccff\" align=\"center\" > 1  </td><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td  /><td class=\" tenth-col \"  /></tr>"
                + "<tr class=\"spacer-row\" ><td class=\"spacer-row\" colspan=130 > </td></tr></table>"
                + "</div>";
            assertEquals("Incorrect protein coverage map table HTML", expectedHtml, exportHtml);
        }
    }
}
