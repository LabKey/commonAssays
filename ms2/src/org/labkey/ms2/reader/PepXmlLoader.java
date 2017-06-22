/*
 * Copyright (c) 2006-2017 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.reader.SimpleXMLStreamReader;
import org.labkey.ms2.MS2Modification;
import org.labkey.ms2.protein.fasta.Protein;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

public class PepXmlLoader extends MS2XmlLoader
{
    private PeptideProphetSummary _ppSummary;

    private ArrayList<RelativeQuantAnalysisSummary> _quantSummaries = new ArrayList<>();

    public PepXmlLoader(File f, Logger log) throws FileNotFoundException, XMLStreamException
    {
        init(f, log);
        readAnalysisSummaries();
    }

    // Read xpress, peptide prophet, etc. analysis summaries at the top of the file
    // Starts at the beginning and ends on the first msms_run_summary tag
    public void readAnalysisSummaries() throws XMLStreamException
    {
        while (_parser.hasNext())
        {
            _parser.next();

            if (_parser.isStartElement())
            {
                String element = _parser.getLocalName();

                if (element.equals("msms_run_summary"))
                    return;

                if (element.equals(PeptideProphetSummary.ANALYSIS_SUMMARY_TAG))
                {
                    String analysisType = _parser.getAttributeValue(null, "analysis");

                    switch (analysisType)
                    {
                        case "peptideprophet":
                            _ppSummary = PeptideProphetSummary.load(_parser);
                            break;
                        case XPressHandler.analysisType:
                            _quantSummaries.add(XPressAnalysisSummary.load(_parser));
                            break;
                        case Q3Handler.ANALYSIS_TYPE:
                            _quantSummaries.add(Q3AnalysisSummary.load(_parser));
                            break;
                        case LibraQuantHandler.ANALYSIS_TYPE:
                            _quantSummaries.add(LibraQuantHandler.load(_parser));
                            break;
                    }
                }
            }
        }
    }


    public PeptideProphetSummary getPeptideProphetSummary()
    {
        return _ppSummary;
    }

    public List<RelativeQuantAnalysisSummary> getQuantSummaries()
    {
        return _quantSummaries;
    }

    public FractionIterator getFractionIterator()
    {
        return new FractionIterator();
    }


    /** Doesn't implement Iterator so that we can throw XMLStreamExceptions instead of having to wrap them as RuntimeExceptions */
    public class FractionIterator
    {
        public boolean hasNext() throws XMLStreamException
        {
            boolean hasNext = true;

            // If we're not currently on the start of an msms_run_summary then attempt to skip to the next one
            if (!_parser.isStartElement() || !"msms_run_summary".equals(_parser.getLocalName()))
            {
                hasNext = _parser.skipToStart("msms_run_summary");
            }

            return hasNext;
        }


        public PepXmlFraction next() throws XMLStreamException
        {
            return PepXmlFraction.getNextFraction(_parser);
        }


        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }


    public static class PepXmlFraction extends PeptideFraction
    {
        private SimpleXMLStreamReader _parser;

        private int _searchConstraintMaxInternalCleavages;
        private int _searchConstraintMinTermini;


        public static PepXmlFraction getNextFraction(SimpleXMLStreamReader parser) throws XMLStreamException
        {
            PepXmlFraction fraction = new PepXmlFraction(parser);
            fraction.assembleRunInfo();
            return fraction;
        }


        private PepXmlFraction(SimpleXMLStreamReader parser)
        {
            _parser = parser;
        }


        // Pull run info together from the msms_run_summary and search_summary
        // We start on an msms_run_summary tag
        protected void assembleRunInfo() throws XMLStreamException
        {
            _massSpecType = null;
            _searchEngine = null;
            _searchEnzyme = null;
            _databaseLocalPaths = new LinkedHashSet<>();

            handleMsMsRunSummary();

            if (!_parser.skipToStart("search_summary"))
            {
                throw new XMLStreamException("No search_summary to skip to");
            }
            handleSearchSummary();
        }


        private void handleMsMsRunSummary()
        {
            String[] instrument = new String[]{
                    _parser.getAttributeValue(null, "msManufacturer"),
                    _parser.getAttributeValue(null, "msModel"),
            };

            _massSpecType = StringUtils.trimToNull(StringUtils.join(instrument, " "));
        }


        private boolean handleSearchSummary() throws XMLStreamException
        {
            boolean endOfSearchSummary = false;

            while (!endOfSearchSummary)
            {
                if (_parser.isWhiteSpace() || XMLStreamReader.COMMENT == _parser.getEventType())
                {
                    _parser.next();
                    continue;
                }

                String element = _parser.getLocalName();

                if (_parser.isStartElement())
                {
                    if (element.equals("search_query"))
                        endOfSearchSummary = true;
                    else if (element.equals("search_summary"))
                    {
                        _searchEngine = _parser.getAttributeValue(null, "search_engine");
                        _searchEngineVersion = _parser.getAttributeValue(null, "search_engine_version");
                        _dataBasename = _parser.getAttributeValue(null, "base_name");
                        _dataSuffix = _parser.getAttributeValue(null, "out_data");
                        if (_dataSuffix == null)
                            _dataSuffix = "tgz";
                        else if (_dataSuffix.startsWith("."))
                            _dataSuffix = _dataSuffix.substring(1);
                    }
                    else if (element.equals("search_database"))
                    {
                        String fastaPath = _parser.getAttributeValue(null, "local_path");
                        if (StringUtils.isEmpty(fastaPath))
                        {
                            throw new XMLStreamException("Reference to FASTA file in <search_database> element via local_path attribute is empty");
                        }
                        _databaseLocalPaths.add(fastaPath);
                    }
                    else if (element.equals("aminoacid_modification"))
                        handleModification();
                    else if (element.equals("enzymatic_search_constraint"))
                        handleEnzymaticSearchConstraint();
                    else if (element.equals("parameter"))
                    {
                        String name = _parser.getAttributeValue(null, "name");

                        if ("spectrum, path".equals(name))
                            _spectrumPath = _parser.getAttributeValue(null, "value");

                        if ("pipeline, import spectra min probability".equals(name))
                            _importSpectraMinProbability = Float.parseFloat(_parser.getAttributeValue(null, "value"));

                        if ("pipeline, load spectra".equals(name) || "pipeline, import spectra".equals(name))
                            _loadSpectra = !"no".equalsIgnoreCase(_parser.getAttributeValue(null, "value"));
                    }
                }
                else
                {
                    if (element.equals("search_summary"))
                        endOfSearchSummary = true;
                }

                if (!endOfSearchSummary)
                    _parser.next();
            }

            // Assign symbols to modifications that don't have them
            _modifications.initializeSymbols();

            // We should now have all the run info collected
            return true;
        }


        private void handleModification()
        {
            MS2Modification mod = new MS2Modification();
            mod.setAminoAcid(_parser.getAttributeValue(null, "aminoacid"));
            mod.setMassDiff(Float.parseFloat(_parser.getAttributeValue(null, "massdiff")));
            mod.setVariable("Y".equals(_parser.getAttributeValue(null, "variable")));
            mod.setMass(Float.parseFloat(_parser.getAttributeValue(null, "mass")));

            if (mod.getVariable())
                mod.setSymbol(_parser.getAttributeValue(null, "symbol"));
            else
                mod.setSymbol("?");
            _modifications.add(mod);
        }


        private void handleEnzymaticSearchConstraint()
        {
            _searchEnzyme = _parser.getAttributeValue(null, "enzyme");
             String maxCleavagesString = _parser.getAttributeValue(null, "max_num_internal_cleavages");
            if (maxCleavagesString != null)
                _searchConstraintMaxInternalCleavages = Integer.parseInt(maxCleavagesString);
            else
                _searchConstraintMaxInternalCleavages = 0;

            String minTerminiString = _parser.getAttributeValue(null, "min_number_termini");
            if (minTerminiString != null)
                _searchConstraintMinTermini = Integer.parseInt(minTerminiString);
            else
                _searchConstraintMinTermini = 0;
        }

        public int getSearchConstraintMaxInternalCleavages()
        {
            return _searchConstraintMaxInternalCleavages;
        }

        public int getSearchConstraintMinTermini()
        {
            return _searchConstraintMinTermini;
        }


        public PeptideIterator getPeptideIterator()
        {
            return new PeptideIterator(_parser, _modifications, this);
        }
    }


    public static class PeptideIterator implements Iterator<PepXmlPeptide>
    {
        private static Logger _log = Logger.getLogger(PepXmlLoader.class);

        private SimpleXMLStreamReader _parser;
        private PepXmlPeptide _peptide = null;
        private MS2ModificationList _modifications;
        private final PepXmlFraction _fraction;

        private Map<Character, Integer> _unknownNTerminalModifications = new HashMap<>();
        private Map<Character, Integer> _unknownNonNTerminalModifications = new HashMap<>();

        protected PeptideIterator(SimpleXMLStreamReader parser, MS2ModificationList modifications, PepXmlFraction fraction)
        {
            _parser = parser;
            _modifications = modifications;
            _fraction = fraction;
        }

        protected void incrementUnknownModCount(Map<Character, Integer> mapToIncrement,
                                                char charToIncrement)
        {
            Integer integerToIncrement = mapToIncrement.get(charToIncrement);
            if (integerToIncrement == null)
                integerToIncrement = 0;
            mapToIncrement.put(charToIncrement, ++integerToIncrement);
        }

        public boolean hasNext()
        {
            try
            {
                _peptide = PepXmlPeptide.getNextPeptide(_parser, _modifications, _fraction);

                boolean result = (null != _peptide);

                if (result)
                {
                    //Sift through any unknown modifications and record them
                    //dhmay fixing bug 5904, 5/20/2008: adding null-check
                    if (_peptide._unknownModArray != null)
                    {
                        for (int i = 0; i < _peptide._unknownModArray.length; i++)
                        {
                            if (_peptide._unknownModArray[i])
                            {
                                if (i == 0)
                                    incrementUnknownModCount(_unknownNTerminalModifications,
                                            _peptide.getTrimmedPeptide().charAt(0));
                                else
                                {
                                    incrementUnknownModCount(_unknownNonNTerminalModifications,
                                            _peptide.getTrimmedPeptide().charAt(i));
//System.err.println("Unknown mod: " + _peptide.getPeptide() + ", " + i + ", " + _peptide.getTrimmedPeptide().charAt(i));
                                }
                            }
                        }
                    }
                }
                else
                {
                    //End of the line, time to print out any unknown modifications
                    if (!_unknownNTerminalModifications.isEmpty())
                    {
                        _log.error("Error: Unknown N-Terminal Modifications.  Counts per residue:");
                        for (char residue : _unknownNTerminalModifications.keySet())
                        {
                            _log.error("\t" + residue + ": " + _unknownNTerminalModifications.get(residue));
                        }
                    }
                    if (!_unknownNonNTerminalModifications.isEmpty())
                    {
                        _log.error("Error: Unknown non-N-Terminal Modifications:");
                        for (char residue : _unknownNonNTerminalModifications.keySet())
                        {
                            _log.error("\t" + residue + ": " + _unknownNonNTerminalModifications.get(residue));
                        }
                    }
                }


                return result;
            }
            catch (XMLStreamException e)
            {
                _log.error(e);
                return false;
            }
        }


        public PepXmlPeptide next()
        {
            return _peptide;
        }


        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }


    public static class PepXmlPeptide extends MS2Loader.Peptide
    {
        private SimpleXMLStreamReader _parser;
        private String  _dtaFileName;
        private final PepXmlFraction _fraction;

        //This variable stays null unless there are actually modificationsIf there are,
        //then all elements are null except the actual modifications.  Index+1 = position of mod
        private ModifiedAminoAcid[] _modifiedAminoAcids = null; // TODO Delete this? This doesn't seem to be used ???

        private static Logger _log = Logger.getLogger(PepXmlPeptide.class);

        protected static PepXmlPeptide getNextPeptide(SimpleXMLStreamReader parser, MS2ModificationList modifications, PepXmlFraction fraction)
                throws XMLStreamException
        {
            PepXmlPeptide peptide = new PepXmlPeptide(parser, modifications, fraction);
            boolean success = peptide.load();

            if (success)
                return peptide;
            else
                return null;
        }


        private PepXmlPeptide(SimpleXMLStreamReader parser, MS2ModificationList modifications, PepXmlFraction fraction)
        {
            _parser = parser;
            _modifications = modifications;
            _fraction = fraction;
        }

        private static final int UNKNOWN = 0;
        private static final int SEARCH_RESULT = 1;
        private static final int SEARCH_HIT = 2;
        private static final int ALTERNATIVE_PROTEIN = 3;
        private static final int SEARCH_SCORE = 4;
        private static final int ANALYSIS_RESULT = 5;
        private static final int MSMS_RUN_SUMMARY = 6;
        private static final int MODIFICATION_INFO = 7;
        private static final int SPECTRUM_QUERY = 8;
        private boolean endOfSpectrumQuery, endOfRun;
        private static HashMap<String, Integer> elements;

        static
        {
            elements = new HashMap<>();
            elements.put("search_result", SEARCH_RESULT);
            elements.put("search_hit", SEARCH_HIT);
            elements.put("alternative_protein", ALTERNATIVE_PROTEIN);
            elements.put("search_score", SEARCH_SCORE);
            elements.put("analysis_result", ANALYSIS_RESULT);
            elements.put("msms_run_summary", MSMS_RUN_SUMMARY);
            elements.put("modification_info", MODIFICATION_INFO);
            elements.put("spectrum_query", SPECTRUM_QUERY);
        }

        protected boolean load() throws XMLStreamException
        {
            endOfSpectrumQuery = false;
            endOfRun = false;
            _hitRank = null;
            _alternativeProteins = new ArrayList<>();

            while (!endOfSpectrumQuery && !endOfRun)
            {
                if (_parser.isWhiteSpace())
                {
                    _parser.next();
                    continue;
                }

                Integer element = elements.get(_parser.getLocalName());
                int index = (null != element ? element.intValue() : UNKNOWN);

                if (_parser.isStartElement())
                    processStartElement(index);
                else
                    processEndElement(index);

                if (endOfRun)
                    return false;

                _parser.next();
            }

            setDerivedFieldValues();
            return true;
        }


        protected void processStartElement(int index) throws XMLStreamException
        {
            switch (index)
            {
                case(SPECTRUM_QUERY):
                    _dtaFileName = _parser.getAttributeValue(null, "spectrum");
                    _scan = Integer.parseInt(_parser.getAttributeValue(null, "start_scan"));

                    String endScan = _parser.getAttributeValue(null, "end_scan");
                    _endScan = (null == endScan ? _scan : Integer.parseInt(endScan));

                    _charge = Integer.parseInt(_parser.getAttributeValue(null, "assumed_charge"));

                    // Retention time is optional, but if we find it, set it.  If not, we'll retrieve it from the mzXML file and update
                    // the peptide record, but that's much more expensive.
                    String retentionTime = _parser.getAttributeValue(null, "retention_time_sec");
                    _retentionTime = (null != retentionTime ? Double.parseDouble(retentionTime) : null);

                    // Mascot exported pepXML can have start_scan="0" and end_scan="0"
                    if (0 == _scan)
                    {
                        Matcher m = MascotDatLoader.QUERY_TITLE_SCAN_REGEX.matcher(_dtaFileName);
                        if (m.find())
                        {
                            // endScan=m.group(2), charge=m.group(3)
                            setScan(Integer.parseInt(m.group(MascotDatLoader.START_SCAN_GROUP_NUM)));
                            setEndScan(Integer.parseInt(m.group(MascotDatLoader.END_SCAN_GROUP_NUM)));
                        }
                    }

                    break;
                case(SEARCH_RESULT):
                    // Start over again within each spectrum_query block
                    _hitRank = null;
                    break;
                case(SEARCH_HIT):
                    Integer h = Integer.valueOf(_parser.getAttributeValue(null, "hit_rank"));
                    if (_hitRank == null || h.compareTo(_hitRank) <= 0)
                    {
                        _hitRank = h;
                        _prevAA = _parser.getAttributeValue(null, "peptide_prev_aa");
                        _trimmedPeptide = _parser.getAttributeValue(null, "peptide");
                        _nextAA = _parser.getAttributeValue(null, "peptide_next_aa");
                        _proteinHits = Integer.parseInt(_parser.getAttributeValue(null, "num_tot_proteins"));
                        String numMatchedIons = _parser.getAttributeValue(null, "num_matched_ions");

                        //doing some null-checking here, just in case.  Since we're generating
                        //some pepXml files ourselves, some of this stuff might be null
                        if (numMatchedIons != null)
                            _matchedIons = Integer.parseInt(numMatchedIons.trim());
                        else
                            _matchedIons = 0;

                        String totNumIons = _parser.getAttributeValue(null, "tot_num_ions");
                        if (totNumIons!= null)
                            _totalIons = Integer.parseInt(totNumIons.trim());
                        else
                            _totalIons = 0;

                        // Mascot exported pepXML may not report "tot_num_ions"
                        if (0 == _totalIons && _matchedIons > 0)
                        {
                            // let's attempt to guess the total ions as per sashimi
                            _totalIons = (_trimmedPeptide.length() - 1) * 2;
                            if (_charge>2)
                            {
                               // do it iteratively for charge 3, 4, 5, etc
                               for(int ionPerm=2; ionPerm<_charge; ionPerm++)
                                   _totalIons *= 2;
                            }
                        }

                        _calculatedNeutralMass = Double.parseDouble(_parser.getAttributeValue(null, "calc_neutral_pep_mass"));
                        _unknownModArray = new boolean[_trimmedPeptide.length()];

                        // Handle illegal number in pepXML translator
                        String massDiff = _parser.getAttributeValue(null, "massdiff");
                        // For Sequest this needs to be a startsWith, since it outputs "+-0.00000"
                        if (massDiff == null || massDiff.startsWith("+-0.0"))
                            _deltaMass = 0.0f;
                        else
                            _deltaMass = Float.parseFloat(massDiff);

                        // Create protein lookup string that matches the way we import FASTA files (which matches what Comet does)
                        String proteinName = _parser.getAttributeValue(null, "protein");
                        if (proteinName != null)
                        {
                            if (_fraction.isSequest())
                            {
                                // Sequest splits FASTA header lines differently from us, on both '|' and whitespace.
                                // Normalize to the same description by moving them to the lookup and use same rules as protein.java
                                String proteinDescr = _parser.getAttributeValue(null, "protein_descr");
                                if (proteinDescr != null && proteinDescr.startsWith("|"))
                                {
                                    // Append the full header line again
                                    proteinDescr = proteinDescr.replaceAll("\t", " ");
                                    proteinName = proteinName + proteinDescr;
                                }
                            }

                            Protein p = new Protein(proteinName, new byte[0]);
                            _protein = p.getLookup();
                        }
                        else
                            _protein = null;
                    }
                    else
                    {
                        _parser.skipToEnd("search_hit");   // TODO: Talk to Damon; remove "activeHit"?
                    }
                    break;
                case(MODIFICATION_INFO):
                    _modifiedAminoAcids = new ModifiedAminoAcid[_trimmedPeptide.length()];

                    char[] modChars = new char[_trimmedPeptide.length()];
                    StringBuilder pep = new StringBuilder(_trimmedPeptide);

                    while (true)
                    {
                        _parser.next();

                        if (_parser.isWhiteSpace() || _parser.getEventType() == XMLStreamReader.COMMENT)
                            continue;

                        if ("mod_aminoacid_mass".equals(_parser.getLocalName()))
                        {
                            int position = Integer.parseInt(_parser.getAttributeValue(null, "position")) - 1;
                            char aa = _trimmedPeptide.charAt(position);
                            double modifiedMass = Rounder.round(Double.parseDouble(_parser.getAttributeValue(null, "mass")), 3);

                            MS2Modification mod = _modifications.get(String.valueOf(aa), modifiedMass);

                            // If null, it's either one of the mods that X! Tandem looks for on N-terminal amino acids Q, E, and C, and Tandem2XML isn't spitting out
                            // amino-acid tags OR it's a problem we don't understand
                            if (null == mod)
                            {
//System.err.println("No mod for " + String.valueOf(aa) + ", " + modifiedMass);
                                //record the unknown modification, but don't print out anything yet
                                _unknownModArray[position] = true;
                                _log.debug("Unknown modification at scan " + getScan() + ": " + aa +
                                           " " + modifiedMass);
                            }
                            else if (mod.getVariable())
                                modChars[position] =  mod.getSymbol().charAt(0);

                            //paranoia
                            if (position <= _modifiedAminoAcids.length)
                                _modifiedAminoAcids[position] = new ModifiedAminoAcid(aa, modifiedMass);

                            _parser.next();     // end element
                        }
                        else
                        {
                            // Iterate in reverse order, so inserts don't invalidate future positions
                            for (int i = modChars.length - 1; i >= 0; i--)
                                if (0 != modChars[i])
                                    pep.insert(i + 1, modChars[i]);
                            _peptide = pep.toString();
                            break;
                        }
                    }
                    break;
                case(ALTERNATIVE_PROTEIN):
                    //dhmay adding handling for alternative proteins, 04/23/2008
                    String altProteinName = _parser.getAttributeValue(null, "protein");
                    if (altProteinName != null)
                        _alternativeProteins.add(altProteinName);
                    break;
                case(SEARCH_SCORE):
                    String name = _parser.getAttributeValue(null, "name");
                    String value = _parser.getAttributeValue(null, "value");
                    setScore(name, value);
                    break;
                case(ANALYSIS_RESULT):
                    PepXmlAnalysisResultHandler.setAnalysisResult(_parser, this);
                    break;
                case(UNKNOWN):
//              _log.debug("unknown: " + parser.getLocalName());
                    break;
                default:
//              _log.debug("known, but no procedure: " + parser.getLocalName());
                    break;
            }
        }


        protected void processEndElement(int index) throws XMLStreamException
        {
            switch (index)
            {
                case(SPECTRUM_QUERY):
                    endOfSpectrumQuery = true;
                    break;
                case(MSMS_RUN_SUMMARY):
                    endOfRun = true;
                    break;
            }
        }
    }
}
