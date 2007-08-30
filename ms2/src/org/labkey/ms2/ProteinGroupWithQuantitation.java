package org.labkey.ms2;

import java.sql.SQLException;

/**
 * User: jeckels
 * Date: Dec 1, 2006
 */
public class ProteinGroupWithQuantitation
{
    private int _rowId;
    private float _groupProbability;
    private int _proteinProphetFileId;
    private int _groupNumber;
    private int _indistinguishableCollectionId;
    private int _uniquePeptidesCount;
    private int _totalNumberPeptides;
    private Float _pctSpectrumIds;
    private Float _percentCoverage;
    private float _proteinProbability;

    private Float _ratioMean;
    private Float _ratioStandardDev;
    private Integer _ratioNumberPeptides;
    private Float _heavy2LightRatioMean;
    private Float _heavy2LightRatioStandardDev;

    private Protein[] _proteins;


    public int getGroupNumber()
    {
        return _groupNumber;
    }

    public void setGroupNumber(int groupNumber)
    {
        _groupNumber = groupNumber;
    }

    public float getGroupProbability()
    {
        return _groupProbability;
    }

    public void setGroupProbability(float groupProbability)
    {
        _groupProbability = groupProbability;
    }

    public Float getHeavy2LightRatioMean()
    {
        return _heavy2LightRatioMean;
    }

    public void setHeavy2LightRatioMean(Float heavy2LightRatioMean)
    {
        _heavy2LightRatioMean = heavy2LightRatioMean;
    }

    public Float getHeavy2LightRatioStandardDev()
    {
        return _heavy2LightRatioStandardDev;
    }

    public void setHeavy2LightRatioStandardDev(Float heavy2LightRatioStandardDev)
    {
        _heavy2LightRatioStandardDev = heavy2LightRatioStandardDev;
    }

    public int getIndistinguishableCollectionId()
    {
        return _indistinguishableCollectionId;
    }

    public void setIndistinguishableCollectionId(int indistinguishableCollectionId)
    {
        _indistinguishableCollectionId = indistinguishableCollectionId;
    }

    public Float getPctSpectrumIds()
    {
        return _pctSpectrumIds;
    }

    public void setPctSpectrumIds(Float pctSpectrumIds)
    {
        _pctSpectrumIds = pctSpectrumIds;
    }

    public Float getPercentCoverage()
    {
        return _percentCoverage;
    }

    public void setPercentCoverage(Float percentCoverage)
    {
        _percentCoverage = percentCoverage;
    }

    public float getProteinProbability()
    {
        return _proteinProbability;
    }

    public void setProteinProbability(float proteinProbability)
    {
        _proteinProbability = proteinProbability;
    }

    public int getProteinProphetFileId()
    {
        return _proteinProphetFileId;
    }

    public void setProteinProphetFileId(int proteinProphetFileId)
    {
        _proteinProphetFileId = proteinProphetFileId;
    }

    public Float getRatioMean()
    {
        return _ratioMean;
    }

    public void setRatioMean(Float ratioMean)
    {
        _ratioMean = ratioMean;
    }

    public Integer getRatioNumberPeptides()
    {
        return _ratioNumberPeptides;
    }

    public void setRatioNumberPeptides(Integer ratioNumberPeptides)
    {
        _ratioNumberPeptides = ratioNumberPeptides;
    }

    public Float getRatioStandardDev()
    {
        return _ratioStandardDev;
    }

    public void setRatioStandardDev(Float ratioStandardDev)
    {
        _ratioStandardDev = ratioStandardDev;
    }

    public int getRowId()
    {
        return _rowId;
    }

    public void setRowId(int rowId)
    {
        _rowId = rowId;
    }

    public int getTotalNumberPeptides()
    {
        return _totalNumberPeptides;
    }

    public void setTotalNumberPeptides(int totalNumberPeptides)
    {
        _totalNumberPeptides = totalNumberPeptides;
    }

    public int getUniquePeptidesCount()
    {
        return _uniquePeptidesCount;
    }

    public void setUniquePeptidesCount(int uniquePeptidesCount)
    {
        _uniquePeptidesCount = uniquePeptidesCount;
    }

    public Protein[] lookupProteins() throws SQLException
    {
        if (_proteins == null)
        {
            _proteins = MS2Manager.getProteinsForGroup(_proteinProphetFileId, _groupNumber, _indistinguishableCollectionId);
        }
        return _proteins;
    }
}
