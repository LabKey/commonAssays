/*
 * Copyright (c) 2006-2013 LabKey Corporation
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
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.reader.ProtXmlReader;

import javax.xml.stream.XMLStreamException;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * User: jeckels
 * Date: Feb 22, 2006
 */
public class ProteinGroup
{
    private int _rowId;
    private int _groupId;
    private float _probability;
    private int _proteinProphetFileId;
    private int _indistinguishableCollectionId;
    private Float _errorRate;
    private Float _pctSpectrumIds;
    private int _uniquePeptidesCount;
    private Float _percentCoverage;
    private int _totalNumberPeptides;
    private float _proteinProbability;

    private SimpleXMLStreamReader _parser;
    private MS2Run _run;
    private List<ProtXmlReader.Protein> _proteins = new ArrayList<>();
    private ITraqProteinQuantitation _iTraqProteinQuantitation;

    public void setParser(SimpleXMLStreamReader parser, MS2Run run)
    {
        _parser = parser;
        _run = run;
    }


    public int getGroupNumber()
    {
        return _groupId;
    }


    public void setGroupNumber(int groupNumber)
    {
        _groupId = groupNumber;
    }

    public float getProbability()
    {
        return _probability;
    }

    public void setProbability(float probability)
    {
        _probability = probability;
    }

    public float getGroupProbability()
    {
        return _probability;
    }

    public void setGroupProbability(float probability)
    {
        _probability = probability;
    }


    public List<ProtXmlReader.Protein> getProteins() throws XMLStreamException
    {
        if (!_proteins.isEmpty())
            return _proteins;

        if (_parser == null)
            return Collections.emptyList();

        while (_parser.hasNext() && !(_parser.isEndElement() && "protein_group".equals(_parser.getLocalName())))
        {
            _parser.next();

            if (_parser.isStartElement() && "protein".equals(_parser.getLocalName()))
            {
                ProtXmlReader.Protein p = new ProtXmlReader.Protein(_parser, _run);
                _proteins.add(p);
            }
        }

        return _proteins;
    }


    public String toString()
    {
        return _groupId + " " + _probability;
    }

    public int getRowId()
    {
        return _rowId;
    }

    public void setRowId(int rowId)
    {
        _rowId = rowId;
    }

    public int getProteinProphetFileId()
    {
        return _proteinProphetFileId;
    }

    public void setProteinProphetFileId(int proteinProphetFileId)
    {
        _proteinProphetFileId = proteinProphetFileId;
    }

    public int getIndistinguishableCollectionId()
    {
        return _indistinguishableCollectionId;
    }

    public void setIndistinguishableCollectionId(int indistinguishableCollectionId)
    {
        _indistinguishableCollectionId = indistinguishableCollectionId;
    }

    public void setPctSpectrumIds(Float pctSpectrumIds)
    {
        _pctSpectrumIds = pctSpectrumIds;
    }

    public Float getPctSpectrumIds()
    {
        return _pctSpectrumIds;
    }

    public void setUniquePeptidesCount(int uniquePeptidesCount)
    {
        _uniquePeptidesCount = uniquePeptidesCount;
    }

    public void setTotalNumberPeptides(int totalNumberPeptides)
    {
        _totalNumberPeptides = totalNumberPeptides;
    }

    public void setPercentCoverage(Float percentCoverage)
    {
        _percentCoverage = percentCoverage;
    }

    public int getUniquePeptidesCount()
    {
        return _uniquePeptidesCount;
    }

    public Float getPercentCoverage()
    {
        return _percentCoverage;
    }

    public int getTotalNumberPeptides()
    {
        return _totalNumberPeptides;
    }

    public void setProteinProbability(float proteinProbability)
    {
        _proteinProbability = proteinProbability;
    }

    public float getProteinProbability()
    {
        return _proteinProbability;
    }

    public Float getErrorRate()
    {
        return _errorRate;
    }

    public void setErrorRate(Float errorRate)
    {
        _errorRate = errorRate;
    }

    public ITraqProteinQuantitation getITraqProteinQuantitation()
    {
        if (_iTraqProteinQuantitation == null)
        {
            _iTraqProteinQuantitation = MS2Manager.getITraqProteinQuantitation(getRowId());
        }
        return _iTraqProteinQuantitation;
    }
}
