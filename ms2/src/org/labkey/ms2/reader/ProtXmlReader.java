/*
 * Copyright (c) 2005-2016 Fred Hutchinson Cancer Research Center
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

import org.labkey.api.reader.SimpleXMLStreamReader;
import org.labkey.api.util.PossiblyGZIPpedFileInputStreamFactory;
import org.labkey.ms2.IcatProteinQuantitation;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.pipeline.sequest.SequestRun;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class ProtXmlReader
{
    private File _file;
    private final MS2Run _run;

    public ProtXmlReader(File file, MS2Run run)
    {
        _file = file;
        _run = run;
    }

    public ProteinGroupIterator iterator() throws FileNotFoundException, XMLStreamException
    {
        return new ProteinGroupIterator();
    }

    public class ProteinGroupIterator implements Iterator<ProteinGroup>
    {
        private SimpleXMLStreamReader _parser;
        private ProteinGroup _nextProteinGroup = null;
        private java.io.InputStream _fIn;

        public ProteinGroupIterator() throws FileNotFoundException, XMLStreamException
        {
            // TPP treats .xml.gz as a native format
            _fIn = PossiblyGZIPpedFileInputStreamFactory.getStream(_file);
            _parser = new SimpleXMLStreamReader(_fIn);
        }

        public void close()
        {
            try
            {
                _fIn.close();
            }
            catch (IOException e) {}
            try
            {
                _parser.close();
            }
            catch (XMLStreamException e) {}
        }

        public boolean hasNext()
        {
            try
            {
                if (null == _nextProteinGroup)
                    _nextProteinGroup = getProteinGroup();
            }
            catch (XMLStreamException e)
            {
                throw new RuntimeException(e);
            }

            return null != _nextProteinGroup;
        }

        public ProteinGroup getProteinGroup() throws XMLStreamException
        {
            while (_parser.hasNext())
            {
                if (!_parser.skipToStart("protein_group"))
                {
                    return null;
                }

                if (_parser.hasNext())
                {
                    ProteinGroup group = new ProteinGroup();
                    String groupNumberString = _parser.getAttributeValue(null, "group_number");
                    String probabilityString = _parser.getAttributeValue(null, "probability");
                    if (groupNumberString != null && !"".equals(groupNumberString) &&
                        probabilityString != null && !"".equals(probabilityString))
                    {
                        group.setGroupNumber(Integer.parseInt(groupNumberString));
                        group.setProbability(Float.parseFloat(probabilityString));
                        group.setParser(_parser, _run);
                        return group;
                    }
                }
            }

            return null;
        }



        public ProteinGroup next()
        {
            ProteinGroup currentProteinGroup = _nextProteinGroup;
            _nextProteinGroup = null;
            return currentProteinGroup;
        }


        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        public SimpleXMLStreamReader getReader()
        {
            return _parser;
        }
    }

    private static int parseIntAllowingNulls(String s)
    {
        if (s == null)
        {
            return 0;
        }
        return Integer.parseInt(s);
    }

    private static float parseFloatAllowingNulls(String s)
    {
        if (s == null)
        {
            return 0;
        }
        return Float.parseFloat(s);
    }

    public static class Protein implements Cloneable
    {
        private String _proteinName;
        private float _probability;
        private Float _percentCoverage;
        private int _totalNumberPeptides;
        private int _uniquePeptidesCount;
        private Float _pctSpectrumIds;
        private String _proteinDescription;

        private List<Peptide> _peptides = new ArrayList<>();
        private List<String> _indistinguishableProteinNames = new ArrayList<>();
        private IcatProteinQuantitation _quantRatio;
        private ITraqProteinQuantitation _iTraqQuantRatio;
        private MS2Run _run;

        public Protein()
        {
            super();
        }

        public Protein(SimpleXMLStreamReader parser, MS2Run run) throws XMLStreamException
        {
            _run = run;
            String spectrumIdsString = parser.getAttributeValue(null, "pct_spectrum_ids");
            Float spectrumIds = null;
            if (spectrumIdsString != null)
            {
                spectrumIds = Float.parseFloat(spectrumIdsString) / 100f;
            }
            setPctSpectrumIds(spectrumIds);
            String coverageString = parser.getAttributeValue(null, "percent_coverage");
            Float coverage = null;
            if (coverageString != null)
            {
                coverage = Float.parseFloat(coverageString) / 100f;
            }
            setPercentCoverage(coverage);
            setProbability(Float.parseFloat(parser.getAttributeValue(null, "probability")));
            setProteinName(parser.getAttributeValue(null, "protein_name"));
            setTotalNumberPeptides(parseIntAllowingNulls(parser.getAttributeValue(null, "total_number_peptides")));

            String uniqueStrippedPeptides = parser.getAttributeValue(null, "unique_stripped_peptides");
            int uniquePeptidesCount = 1;
            int index = uniqueStrippedPeptides.indexOf('+');
            while (index != -1)
            {
                uniquePeptidesCount++;
                index = uniqueStrippedPeptides.indexOf('+', index + 1);
            }
            setUniquePeptidesCount(uniquePeptidesCount);

            loadChildren(parser);

            // Do this after the children have been loaded so that we have the protein description available
            if (_run instanceof SequestRun && getProteinDescription() != null && getProteinDescription().startsWith("|"))
            {
                setProteinName(getProteinName() + getProteinDescription().replaceAll("\t", " "));
            }
        }

        private void loadChildren(SimpleXMLStreamReader parser) throws XMLStreamException
        {
            while (parser.hasNext() && !(parser.isEndElement() && "protein".equals(parser.getLocalName())))
            {
                parser.next();

                if (parser.isStartElement())
                {
                    String name = parser.getLocalName();

                    if ("annotation".equalsIgnoreCase(name))
                        setProteinDescription(parser.getAttributeValue(null, "protein_description"));

                    if ("peptide".equals(name))
                        _peptides.add(new Peptide(parser));
                    else if ("indistinguishable_protein".equals(name))
                    {
                        String indistinguishableProteinName = null;
                        String proteinDescr = null;
                        for (int i = 0; i < parser.getAttributeCount(); i++)
                        {
                            if ("protein_name".equals(parser.getAttributeLocalName(i)))
                            {
                                indistinguishableProteinName = parser.getAttributeValue(i);
                            }
                        }

                        // Sequest has different rules on how to split FASTA header lines. It splits on the first |,
                        // and we don't when the parse and load FASTAs, so use their same logic to reconstitute the same
                        // name we have
                        if (_run instanceof SequestRun)
                        {
                            while (parser.hasNext() && !(parser.isEndElement() && "indistinguishable_protein".equals(parser.getLocalName())))
                            {
                                parser.next();

                                if (parser.isStartElement() && "annotation".equalsIgnoreCase(parser.getLocalName()))
                                {
                                    proteinDescr = parser.getAttributeValue(null, "protein_description");
                                }
                            }
                            if (proteinDescr != null && proteinDescr.startsWith("|"))
                            {
                                // Append the full header line again
                                proteinDescr = proteinDescr.replaceAll("\t", " ");
                                indistinguishableProteinName = indistinguishableProteinName + proteinDescr;
                            }
                        }

                        _indistinguishableProteinNames.add(indistinguishableProteinName);
                    }
                    else if ("XPressRatio".equals(name) || "Q3Ratio".equals(name))
                    {
                        _quantRatio = new IcatProteinQuantitation();
                        _quantRatio.setHeavy2lightRatioMean(Float.parseFloat(parser.getAttributeValue(null, "heavy2light_ratio_mean")));
                        _quantRatio.setHeavy2lightRatioStandardDev(Float.parseFloat(parser.getAttributeValue(null, "heavy2light_ratio_standard_dev")));
                        _quantRatio.setRatioMean(Float.parseFloat(parser.getAttributeValue(null, "ratio_mean")));
                        _quantRatio.setRatioStandardDev(Float.parseFloat(parser.getAttributeValue(null, "ratio_standard_dev")));
                        _quantRatio.setRatioNumberPeptides(Integer.parseInt(parser.getAttributeValue(null, "ratio_number_peptides")));
                    }
                    else if ("libra_result".equals(name))
                    {
                        int index = 1;
                        _iTraqQuantRatio = new ITraqProteinQuantitation();
                        while (!parser.isEndElement() || !"libra_result".equals(parser.getLocalName()))
                        {
                            parser.next();
                            if (parser.isStartElement() && "intensity".equals(parser.getLocalName()))
                            {
                                switch (index)
                                {
                                    case 1:
                                        _iTraqQuantRatio.setRatio1(Double.parseDouble(parser.getAttributeValue(null, "ratio")));
                                        _iTraqQuantRatio.setError1(Double.parseDouble(parser.getAttributeValue(null, "error")));
                                        break;
                                    case 2:
                                        _iTraqQuantRatio.setRatio2(Double.parseDouble(parser.getAttributeValue(null, "ratio")));
                                        _iTraqQuantRatio.setError2(Double.parseDouble(parser.getAttributeValue(null, "error")));
                                        break;
                                    case 3:
                                        _iTraqQuantRatio.setRatio3(Double.parseDouble(parser.getAttributeValue(null, "ratio")));
                                        _iTraqQuantRatio.setError3(Double.parseDouble(parser.getAttributeValue(null, "error")));
                                        break;
                                    case 4:
                                        _iTraqQuantRatio.setRatio4(Double.parseDouble(parser.getAttributeValue(null, "ratio")));
                                        _iTraqQuantRatio.setError4(Double.parseDouble(parser.getAttributeValue(null, "error")));
                                        break;
                                    case 5:
                                        _iTraqQuantRatio.setRatio5(Double.parseDouble(parser.getAttributeValue(null, "ratio")));
                                        _iTraqQuantRatio.setError5(Double.parseDouble(parser.getAttributeValue(null, "error")));
                                        break;
                                    case 6:
                                        _iTraqQuantRatio.setRatio6(Double.parseDouble(parser.getAttributeValue(null, "ratio")));
                                        _iTraqQuantRatio.setError6(Double.parseDouble(parser.getAttributeValue(null, "error")));
                                        break;
                                    case 7:
                                        _iTraqQuantRatio.setRatio7(Double.parseDouble(parser.getAttributeValue(null, "ratio")));
                                        _iTraqQuantRatio.setError7(Double.parseDouble(parser.getAttributeValue(null, "error")));
                                        break;
                                    case 8:
                                        _iTraqQuantRatio.setRatio8(Double.parseDouble(parser.getAttributeValue(null, "ratio")));
                                        _iTraqQuantRatio.setError8(Double.parseDouble(parser.getAttributeValue(null, "error")));
                                        break;
                                    case 9:
                                        _iTraqQuantRatio.setRatio9(Double.parseDouble(parser.getAttributeValue(null, "ratio")));
                                        _iTraqQuantRatio.setError9(Double.parseDouble(parser.getAttributeValue(null, "error")));
                                        break;
                                    case 10:
                                        _iTraqQuantRatio.setRatio10(Double.parseDouble(parser.getAttributeValue(null, "ratio")));
                                        _iTraqQuantRatio.setError10(Double.parseDouble(parser.getAttributeValue(null, "error")));
                                        break;
                                    default:
                                        throw new IllegalStateException("Invalid iTRAQ index: " + index + ". Supported values are 1-10.");
                                }
                                index++;
                            }
                        }
                    }
                }
            }
        }

        public Protein clone()
        {
            try
            {
                return (Protein)super.clone();
            }
            catch (CloneNotSupportedException e)
            {
                throw new Error("This shouldn't happen", e);
            }
        }

        public String getProteinName()
        {
            return _proteinName;
        }


        public void setProteinName(String proteinName)
        {
            this._proteinName = proteinName;
        }

        public String getProteinDescription()
        {
            return _proteinDescription;
        }

        public void setProteinDescription(String proteinDescription)
        {
            _proteinDescription = proteinDescription;
        }

        public Float getPctSpectrumIds()
        {
            return _pctSpectrumIds;
        }


        public void setPctSpectrumIds(Float pctSpectrumIds)
        {
            this._pctSpectrumIds = pctSpectrumIds;
        }


        public Float getPercentCoverage()
        {
            return _percentCoverage;
        }


        public void setPercentCoverage(Float percentCoverage)
        {
            this._percentCoverage = percentCoverage;
        }


        public float getProbability()
        {
            return _probability;
        }


        public void setProbability(float probability)
        {
            this._probability = probability;
        }


        public int getTotalNumberPeptides()
        {
            return _totalNumberPeptides;
        }


        public void setTotalNumberPeptides(int totalNumberPeptides)
        {
            this._totalNumberPeptides = totalNumberPeptides;
        }

        public List<Peptide> getPeptides()
        {
            return _peptides;
        }


        public String toString()
        {
            return _proteinName + " " + _probability;
        }

        public List<String> getIndistinguishableProteinNames()
        {
            return _indistinguishableProteinNames;
        }

        public int getUniquePeptidesCount()
        {
            return _uniquePeptidesCount;
        }

        public void setUniquePeptidesCount(int uniquePeptidesCount)
        {
            _uniquePeptidesCount = uniquePeptidesCount;
        }

        public IcatProteinQuantitation getQuantitationRatio()
        {
            return _quantRatio;
        }

        public ITraqProteinQuantitation getITraqQuantitationRatio()
        {
            return _iTraqQuantRatio;
        }
    }


    public static class Peptide
    {
        private String _peptideSequence;
        private int _charge;
        private float _nspAdjustedProbability;
        private float _weight;
        private boolean _isNondegenerateEvidence;
        private int _enzymaticTermini;
        private float _siblingPeptides;
        private int _siblingPeptidesBin;
        private int _instances;
        private boolean _contributingEvidence;
        private float _calcNeutralPepMass;

        public Peptide()
        {
            super();
        }

        public Peptide(SimpleXMLStreamReader parser)
        {
            setCalcNeutralPepMass(Float.parseFloat(parser.getAttributeValue(null, "calc_neutral_pep_mass")));
            setCharge(Integer.parseInt(parser.getAttributeValue(null, "charge")));
            setContributingEvidence("Y".equals(parser.getAttributeValue(null, "is_contributing_evidence")));

            //dhmay adding default:
            //In the case of an empty string here, which we've seen, fill in 2 as default
            //value for number of tryptic ends.  I checked with Jimmy, and he said
            //this would be the least harmful default.  Obviously it would be better
            //just to carry forward the ends correctly
            String enzymaticTerminiString = parser.getAttributeValue(null, "n_enzymatic_termini");
            if (enzymaticTerminiString == null ||
                enzymaticTerminiString.length() == 0)
                enzymaticTerminiString = "2";
            setEnzymaticTermini(Integer.parseInt(enzymaticTerminiString));

            setInstances(Integer.parseInt(parser.getAttributeValue(null, "n_instances")));
            setNondegenerateEvidence("Y".equals(parser.getAttributeValue(null, "is_nondegenerate_evidence")));
            setNspAdjustedProbability(Float.parseFloat(parser.getAttributeValue(null, "nsp_adjusted_probability")));
            setPeptideSequence(parser.getAttributeValue(null, "peptide_sequence"));
            setSiblingPeptides(Float.parseFloat(parser.getAttributeValue(null, "n_sibling_peptides")));
            setSiblingPeptidesBin(Integer.parseInt(parser.getAttributeValue(null, "n_sibling_peptides_bin")));
            setWeight(Float.parseFloat(parser.getAttributeValue(null, "weight")));
        }


        public float getCalcNeutralPepMass()
        {
            return _calcNeutralPepMass;
        }


        public void setCalcNeutralPepMass(float calcNeutralPepMass)
        {
            this._calcNeutralPepMass = calcNeutralPepMass;
        }


        public int getCharge()
        {
            return _charge;
        }


        public void setCharge(int charge)
        {
            this._charge = charge;
        }


        public void setContributingEvidence(boolean contributingEvidence)
        {
            _contributingEvidence = contributingEvidence;
        }


        public boolean isNondegenerateEvidence()
        {
            return _isNondegenerateEvidence;
        }


        public void setNondegenerateEvidence(boolean nondegenerateEvidence)
        {
            _isNondegenerateEvidence = nondegenerateEvidence;
        }


        public int getEnzymaticTermini()
        {
            return _enzymaticTermini;
        }

        public void setEnzymaticTermini(int enzymaticTermini)
        {
            this._enzymaticTermini = enzymaticTermini;
        }


        public int getInstances()
        {
            return _instances;
        }


        public void setInstances(int instances)
        {
            this._instances = instances;
        }


        public float getSiblingPeptides()
        {
            return _siblingPeptides;
        }


        public void setSiblingPeptides(float nSiblingPeptides)
        {
            this._siblingPeptides = nSiblingPeptides;
        }


        public int getSiblingPeptidesBin()
        {
            return _siblingPeptidesBin;
        }


        public void setSiblingPeptidesBin(int siblingPeptidesBin)
        {
            this._siblingPeptidesBin = siblingPeptidesBin;
        }


        public float getNspAdjustedProbability()
        {
            return _nspAdjustedProbability;
        }


        public void setNspAdjustedProbability(float nspAdjustedProbability)
        {
            this._nspAdjustedProbability = nspAdjustedProbability;
        }


        public String getPeptideSequence()
        {
            return _peptideSequence;
        }


        public void setPeptideSequence(String peptideSequence)
        {
            this._peptideSequence = peptideSequence;
        }


        public float getWeight()
        {
            return _weight;
        }


        public void setWeight(float weight)
        {
            this._weight = weight;
        }


        public String toString()
        {
            return _peptideSequence;
        }

        public boolean isContributingEvidence()
        {
            return _contributingEvidence;
        }
    }

}
