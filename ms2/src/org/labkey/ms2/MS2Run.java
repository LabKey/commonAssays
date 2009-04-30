/*
 * Copyright (c) 2004-2009 Fred Hutchinson Cancer Research Center
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
import org.labkey.api.data.*;
import org.labkey.ms2.MS2Modification;
import org.labkey.api.util.MemTracker;

import java.io.Serializable;
import java.util.*;
import java.sql.SQLException;

public abstract class MS2Run implements Serializable
{
    private static Logger _log = Logger.getLogger(MS2Run.class);

    // Average AA masses
    // UNDONE: Add U == Selenocysteine??  Add Z == glutamic acid or glutamine?
    // B denotes N or D
    // X denotes L or I
    public static final double[] aaMassTable = new double[]
            {/* A */  71.07880, /* B */ 114.59622, /* C */ 103.13880, /* D */ 115.08860, /* E */ 129.11548,
                    /* F */ 147.17656, /* G */  57.05192, /* H */ 137.14108, /* I */ 113.15944, /* J */   0.00000,
                    /* K */ 128.17408, /* L */ 113.15944, /* M */ 131.19256, /* N */ 114.10384, /* O */ 114.14720,
                    /* P */  97.11668, /* Q */ 128.13072, /* R */ 156.18748, /* S */  87.07820, /* T */ 101.10508,
                    /* U */   0.00000, /* V */  99.13256, /* W */ 186.21320, /* X */ 113.15944, /* Y */ 163.17596,
                    /* Z */   0.00000};

    protected int run;
    protected Container container;
    protected String description;
    protected String path;
    protected String fileName;
    protected String status;
    protected String type;
    protected String searchEngine;
    protected String massSpecType;
    protected String fastaFileName;
    protected Date loaded;
    protected int fastaId;
    protected String searchEnzyme;
    protected MS2Modification[] modifications = null;
    protected Map<String, Double> varModifications = null;
    protected double[] massTable = null;
    protected MS2Fraction[] fractions;
    protected int statusId;
    protected boolean deleted;
    protected String experimentRunLSID;
    protected boolean hasPeptideProphet;
    protected int peptideCount;
    protected int spectrumCount;
    protected int negativeHitCount;

    private ProteinProphetFile _proteinProphetFile;

    public MS2Run()
    {
        assert MemTracker.put(this);
    }


    public String toString()
    {
        return getRun() + " " + getDescription() + " " + getFileName();
    }


    protected void initModifications()
    {
        if (null == modifications)
            modifications = MS2Manager.getModifications(run);

        varModifications = new HashMap<String, Double>(10);
        massTable = aaMassTable.clone();

        // Store variable modifications in HashMap; apply fixed modifications to massTable
        for (MS2Modification modification : modifications)
        {
            if (modification.getVariable())
                varModifications.put(modification.getAminoAcid() + modification.getSymbol(), (double)modification.getMassDiff());
            else
            {
                int index = modification.getAminoAcid().charAt(0) - 65;
                massTable[index] += modification.getMassDiff();
            }
        }
    }


    public MS2Modification[] getModifications()
    {
        if (null == modifications)
            initModifications();

        return modifications;
    }


    public Map getVarModifications()
    {
        if (null == varModifications)
            initModifications();

        return varModifications;
    }


    public double[] getMassTable()
    {
        if (null == massTable)
            initModifications();

        return massTable;
    }


    public MS2Fraction[] getFractions()
    {
        if (null == fractions)
            fractions = MS2Manager.getFractions(run);

        return fractions;
    }


    public boolean contains(int fractionId)
    {
        MS2Fraction[] fracs = getFractions();

        for (MS2Fraction frac : fracs)
            if (fractionId == frac.getFraction())
                return true;

        return false;
    }

    public String getCommonPeptideColumnNames()
    {
        return "Scan, EndScan, RetentionTime, Run, RunDescription, Fraction, FractionName, Charge, " + getRunType().getScoreColumnNames() + ", IonPercent, Mass, DeltaMass, DeltaMassPPM, FractionalDeltaMass, FractionalDeltaMassPPM, PrecursorMass, MZ, PeptideProphet, PeptideProphetErrorRate, Peptide, StrippedPeptide, PrevAA, TrimmedPeptide, NextAA, ProteinHits, SequencePosition, H, DeltaScan, Protein, Description, GeneName, SeqId";
    }

    public String getProteinProphetPeptideColumnNames()
    {
        return "NSPAdjustedProbability, Weight, NonDegenerateEvidence, EnzymaticTermini, SiblingPeptides, SiblingPeptidesBin, Instances, ContributingEvidence, CalcNeutralPepMass";
    }

    public String getQuantitationPeptideColumnNames()
    {
        return "LightFirstScan, LightLastScan, LightMass, HeavyFirstScan, HeavyLastScan, HeavyMass, Ratio, Heavy2LightRatio, LightArea, HeavyArea, DecimalRatio";
    }

    public String getPeptideProphetColumnNames()
    {
        return "ProphetFVal, ProphetDeltaMass, ProphetNumTrypticTerm, ProphetNumMissedCleav";
    }

    public static String getCommonProteinColumnNames()
    {
        return "Protein, SequenceMass, Peptides, UniquePeptides, AACoverage, BestName, BestGeneName, Description";
    }

    public static String getDefaultProteinProphetProteinColumnNames()
    {
        return "GroupNumber, GroupProbability, PctSpectrumIds";
    }

    public static String getProteinProphetProteinColumnNames()
    {
        return getDefaultProteinProphetProteinColumnNames() + ", ErrorRate, FirstProtein, FirstDescription, FirstGeneName, FirstBestName, " + TotalFilteredPeptidesColumn.NAME + ", " + UniqueFilteredPeptidesColumn.NAME;
    }

    public String getQuantitationProteinColumnNames()
    {
        return "RatioMean, RatioStandardDev, RatioNumberPeptides, Heavy2LightRatioMean, Heavy2LightRatioStandardDev";
    }

    // Get the list of SELECT column names by iterating through the MS2Peptides columns and
    // taking all requested columns plus primary keys
    public String getSQLPeptideColumnNames(String columnNames, boolean includeSeqId, TableInfo... tableInfos)
    {
        ColumnNameList columnNameList = new ColumnNameList(columnNames);
        ColumnNameList pkList = new ColumnNameList("RowId");
        if (includeSeqId)
        {
            pkList.add("SeqId");
        }
        ColumnNameList sqlColumns = new ColumnNameList();

        for (TableInfo tableInfo : tableInfos)
        {
            for (ColumnInfo column : tableInfo.getColumns())
            {
                String columnName = column.getName();
                if (columnNameList.contains(columnName) || pkList.contains(columnName) && !sqlColumns.contains(columnName))
                    sqlColumns.add(column.getValueSql().toString());
            }
        }

        return sqlColumns.toCSVString();
    }


    public abstract MS2RunType getRunType();

    public abstract String getParamsFileName();

    public abstract String getChargeFilterColumnName();

    public abstract String getChargeFilterParamName();

    public abstract String getDiscriminateExpressions();

    // The scores to read from pepXML files, specified in the order they appear in the prepared statement that
    // inserts rows into MS2PeptidesData
    protected abstract String getPepXmlScoreNames();

    public abstract String[] getGZFileExtensions();

    // PepXml score names in the order they get written to the database
    public Collection<String> getPepXmlScoreColumnNames()
    {
        String[] scoreNameArray = getPepXmlScoreNames().split(",");
        Collection<String> scoreNames = new ArrayList<String>(scoreNameArray.length);

        for (String scoreName : scoreNameArray)
            scoreNames.add(scoreName.trim());

        return scoreNames;
    }


    // Override this to check for missing scores and add default values.
    public void adjustScores(Map<String, String> map)
    {
    }


    public static MS2Run getRunFromTypeString(String type)
    {
        MS2RunType runType = MS2RunType.lookupType(type);
        if (runType == null)
        {
            _log.error("Unrecognized run type: " + type);
            return null;
        }

        try
        {
            MS2Run run = runType.getRunClass().newInstance();
            run.setType(runType.name());
            return run;
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
        catch (InstantiationException e)
        {
            throw new RuntimeException(e);
        }
    }


    public void setExperimentRunLSID(String experimentRunLSID)
    {
        this.experimentRunLSID = experimentRunLSID;
    }

    public String getExperimentRunLSID()
    {
        return experimentRunLSID;
    }


    public boolean getHasPeptideProphet()
    {
        return hasPeptideProphet;
    }


    public void setHasPeptideProphet(boolean hasPeptideProphet)
    {
        this.hasPeptideProphet = hasPeptideProphet;
    }

    // CONSIDER: extend Apache ListOrderedSet (ideally) or our ArrayListMap.
    public static class ColumnNameList extends ArrayList<String>
    {
        public ColumnNameList(Collection<String> columnNames)
        {
            for (String s : columnNames)
            {
                add(s);
            }
        }

        public ColumnNameList(String csvColumnNames)
        {
            super(20);

            String[] arrayColumnNames = csvColumnNames.split(",");
            for (String arrayColumnName : arrayColumnNames)
                add(arrayColumnName.trim());
        }

        public boolean add(String o)
        {
            return super.add(o.toLowerCase());
        }

        public ColumnNameList()
        {
            super(20);
        }

        public boolean contains(Object elem)
        {
            if (elem instanceof String)
            {
                return super.contains(((String)elem).toLowerCase());
            }
            return super.contains(elem);
        }

        public String toCSVString()
        {
            StringBuffer sb = new StringBuffer();
            for (Iterator iter = iterator(); iter.hasNext();)
            {
                if (sb.length() > 0)
                    sb.append(',');

                sb.append(iter.next());
            }
            return sb.toString();
        }
    }


    public int getRun()
    {
        return run;
    }


    public void setRun(int run)
    {
        this.run = run;
    }


    public Container getContainer()
    {
        return container;
    }


    public void setContainer(Container container)
    {
        this.container = container;
    }


    public String getDescription()
    {
        return description;
    }


    public void setDescription(String description)
    {
        this.description = description;
    }


    public String getPath()
    {
        return path;
    }


    public void setPath(String path)
    {
        this.path = path;
    }


    public String getFileName()
    {
        return fileName;
    }


    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }


    public String getFastaFileName()
    {
        return fastaFileName;
    }


    public void setFastaFileName(String fastaFileName)
    {
        this.fastaFileName = fastaFileName;
    }


    public Date getLoaded()
    {
        return this.loaded;
    }


    public void setLoaded(Date loaded)
    {
        this.loaded = loaded;
    }


    public int getFastaId()
    {
        return fastaId;
    }


    public void setFastaId(int fastaId)
    {
        this.fastaId = fastaId;
    }


    public String getStatus()
    {
        return status;
    }


    public void setStatus(String status)
    {
        this.status = status;
    }


    public int getStatusId()
    {
        return statusId;
    }


    public void setStatusId(int statusId)
    {
        this.statusId = statusId;
    }


    public String getType()
    {
        return type;
    }


    public void setType(String type)
    {
        this.type = type;
    }


    public String getSearchEngine()
    {
        return searchEngine;
    }


    public void setSearchEngine(String searchEngine)
    {
        this.searchEngine = searchEngine;
    }


    public String getSearchEnzyme()
    {
        return searchEnzyme;
    }


    public void setSearchEnzyme(String searchEnzyme)
    {
        this.searchEnzyme = searchEnzyme;
    }


    public String getMassSpecType()
    {
        return massSpecType;
    }


    public void setMassSpecType(String massSpecType)
    {
        this.massSpecType = massSpecType;
    }


    public boolean isDeleted()
    {
        return deleted;
    }


    /**
     * Do not use this directly to delete a run - use MS2Manager.markAsDeleted
     */
    public void setDeleted(boolean deleted)
    {
        this.deleted = deleted;
    }

    public ProteinProphetFile getProteinProphetFile() throws SQLException
    {
        if (_proteinProphetFile == null)
        {
            _proteinProphetFile = MS2Manager.getProteinProphetFileByRun(run);
        }
        return _proteinProphetFile;
    }

    public boolean hasProteinProphet() throws SQLException
    {
        return getProteinProphetFile() != null;
    }

    public int getPeptideCount()
    {
        return peptideCount;
    }

    public void setPeptideCount(int peptideCount)
    {
        this.peptideCount = peptideCount;
    }

    public int getNegativeHitCount()
    {
        return negativeHitCount;
    }

    public void setNegativeHitCount(int peptideCount)
    {
        this.negativeHitCount = peptideCount;
    }

    public int getSpectrumCount()
    {
        return spectrumCount;
    }

    public void setSpectrumCount(int spectrumCount)
    {
        this.spectrumCount = spectrumCount;
    }

    /**
     * Return the type of quantitation analysis (if any) associated with this run.
     * Assumes that only one kind of relative quantitation data is loaded for
     * any run.
     *
     * @return The quantitation analysis type or <CODE>null</CODE> if none 
     */
    public String getQuantAnalysisType()
    {
        return MS2Manager.getQuantAnalysisType(run);
    }
}
