/*
 * Copyright (c) 2004-2016 Fred Hutchinson Cancer Research Center
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
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.MemTracker;
import org.labkey.api.view.ViewContext;
import org.springframework.web.servlet.ModelAndView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class MS2Run implements Serializable
{
    private static Logger _log = Logger.getLogger(MS2Run.class);

    protected final static String[] EMPTY_STRING_ARRAY = new String[0];

    protected int run;
    protected Container container;
    protected String description;
    protected String path;
    protected String fileName;
    protected String status;
    protected String type;
    protected String searchEngine;
    protected String massSpecType;
    protected String searchEnzyme;
    protected Map<MassType, List<MS2Modification>> modifications = new HashMap<>();
    protected Map<MassType, Map<String, Double>> varModifications = new HashMap<>();
    protected Map<MassType, double[]> massTables = new HashMap<>();
    protected MS2Fraction[] fractions;
    protected int statusId;
    protected boolean deleted;
    protected String experimentRunLSID;
    protected boolean hasPeptideProphet;
    protected int peptideCount;
    protected int spectrumCount;
    protected int negativeHitCount;

    private ProteinProphetFile _proteinProphetFile;
    private int[] _fastaIds;

    public MS2Run()
    {
        MemTracker.getInstance().put(this);
    }


    public String toString()
    {
        return getRun() + " " + getDescription() + " " + getFileName();
    }


    protected void initModifications(MassType massType)
    {
        List<MS2Modification> staticModifications = modifications.get(massType);
        if (null == staticModifications)
            staticModifications = MS2Manager.getModifications(this);

        Map<String, Double> variableModifications = new HashMap<>(10);
        double[] massTable = massType.getAaMasses().clone();

        // Store variable modifications in HashMap; apply fixed modifications to massTable
        for (MS2Modification modification : staticModifications)
        {
            if (modification.getVariable())
                variableModifications.put(modification.getAminoAcid() + modification.getSymbol(), (double)modification.getMassDiff());
            else
            {
                int index = modification.getAminoAcid().charAt(0) - 65;
                massTable[index] += modification.getMassDiff();
            }
        }

        modifications.put(massType, staticModifications);
        varModifications.put(massType, variableModifications);
        massTables.put(massType, massTable);
    }


    public List<MS2Modification> getModifications(MassType massType)
    {
        if (null ==  modifications.get(massType))
            initModifications(massType);

        return modifications.get(massType);
    }


    public Map<String, Double> getVarModifications(MassType massType)
    {
        if (null == varModifications.get(massType))
            initModifications(massType);

        return varModifications.get(massType);
    }


    public double[] getMassTable(MassType massType)
    {
        if (null == massTables.get(massType))
            initModifications(massType);

        return massTables.get(massType);
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
        return "LightFirstScan, LightLastScan, LightMass, HeavyFirstScan, HeavyLastScan, HeavyMass, Ratio, Heavy2LightRatio, LightArea, HeavyArea, DecimalRatio, Invalidated";
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
                    sqlColumns.add(column.getValueSql(tableInfo.toString()).getSQL());
            }
        }

        return sqlColumns.toCSVString();
    }


    public abstract MS2RunType getRunType();

    public abstract String getParamsFileName();

    public abstract String getChargeFilterColumnName();

    public abstract String getChargeFilterParamName();

    public abstract String getDiscriminateExpressions();

        public abstract String[] getGZFileExtensions();

    // PepXml score names in the order they get written to the database
    public Collection<FieldKey> getPepXmlScoreColumnNames()
    {
        return getRunType().getPepXmlScoreNames();
    }


    // Override this to check for missing scores and add default values.
    public void adjustScores(Map<String, String> map)
    {
    }


    public static MS2Run getRunFromTypeString(String type, String version)
    {
        MS2RunType runType = MS2RunType.lookupType(type, version);
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
        catch (IllegalAccessException | InstantiationException e)
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

    public int[] getFastaIds()
    {
        if (_fastaIds == null)
        {
            SQLFragment sql = new SQLFragment("SELECT FastaId FROM ");
            sql.append(MS2Manager.getTableInfoFastaRunMapping(), "frm");
            sql.append(" WHERE Run = ?");
            sql.add(getRun());
            List<Integer> ids = new SqlSelector(MS2Manager.getSchema(), sql).getArrayList(Integer.class);
            _fastaIds = new int[ids.size()];
            for (int i = 0; i < _fastaIds.length; i++)
            {
                _fastaIds[i] = ids.get(i);
            }
        }
        return _fastaIds;
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
            for (Object o : this)
            {
                if (sb.length() > 0)
                    sb.append(',');

                sb.append(o);
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

    public ProteinProphetFile getProteinProphetFile()
    {
        if (_proteinProphetFile == null)
        {
            _proteinProphetFile = MS2Manager.getProteinProphetFileByRun(run);
        }
        return _proteinProphetFile;
    }

    public boolean hasProteinProphet()
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

    protected ModelAndView getAdditionalRunSummaryView(MS2Controller.RunForm form)
    {
        return null;
    }

    protected ModelAndView getAdditionalPeptideSummaryView(ViewContext viewContext, MS2Peptide peptide, String grouping) throws Exception
    {
        return null;
    }
}
