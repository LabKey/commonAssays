/*
 * Copyright (c) 2006-2009 LabKey Corporation
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
package org.labkey.nab;

import java.io.Serializable;
import java.io.File;
import java.util.List;
import java.util.Date;
import java.util.Map;

import org.labkey.api.study.*;
import org.labkey.api.study.DilutionCurve;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpRun;

/**
 * User: migra
 * Date: Feb 10, 2006
 * Time: 2:15:41 PM
 */
public class Luc5Assay implements Serializable, DilutionCurve.PercentCalculator
{
    private Integer _runRowId;
    private Plate _plate;
    private DilutionSummary[] _dilutionSummaries;
    private int[] _cutoffs;
    private Map<Integer, String> _cutoffFormats;
    private Map<WellGroup, ExpMaterial> _wellGroupMaterialMapping;
    private File _dataFile;
    private DilutionCurve.FitType _curveFitType;
    private boolean _lockAxes;

    public Luc5Assay(Plate plate, int[] cutoffs, DilutionCurve.FitType curveFitType)
    {
        _curveFitType = curveFitType;
        assert plate != null : "plate cannot be null";
        _runRowId = plate.getRowId();
        _cutoffs = cutoffs;
        _plate = plate;
        init();
    }

    public Luc5Assay(Plate plate, List<Integer> cutoffs, DilutionCurve.FitType fitType)
    {
        this(plate, toIntArray(cutoffs), fitType);
    }

    private static int[] toIntArray(List<Integer> cutoffs)
    {
        int[] cutoffArray = new int[cutoffs.size()];
        for (int i = 0; i < cutoffs.size(); i++)
            cutoffArray[i] = cutoffs.get(i);
        return cutoffArray;
    }

    private void init()
    {
        List<? extends WellGroup> specimenGroups = _plate.getWellGroups(WellGroup.Type.SPECIMEN);
        int sampleIndex = 0;
        _dilutionSummaries = new DilutionSummary[specimenGroups.size()];
        for (WellGroup specimenGroup : specimenGroups)
            _dilutionSummaries[sampleIndex++] = new DilutionSummary(this, specimenGroup, null, _curveFitType);
    }

    public DilutionSummary[] getSummaries()
    {
        return _dilutionSummaries;
    }

    public String getName()
    {
        return (String) _plate.getProperty(NabManager.PlateProperty.DataFile.name());
    }

    public Plate getPlate()
    {
        return _plate;
    }

    public Integer getRunRowId()
    {
        return _runRowId;
    }

    public static String intString(double d)
    {
        return String.valueOf((int) Math.round(d));
    }

    public static String percentString(double d)
    {
        return intString(d * 100) + "%";
    }

    public int[] getCutoffs()
    {
        return _cutoffs;
    }

    public double getControlRange()
    {
        WellData cellControl = _plate.getWellGroup(WellGroup.Type.CONTROL, NabManager.CELL_CONTROL_SAMPLE);
        WellData virusControl = _plate.getWellGroup(WellGroup.Type.CONTROL, NabManager.VIRUS_CONTROL_SAMPLE);
        return virusControl.getMean() - cellControl.getMean();
    }

    public double getPercent(WellGroup group, WellData data) throws DilutionCurve.FitFailedException
    {
        WellData cellControl = _plate.getWellGroup(WellGroup.Type.CONTROL, NabManager.CELL_CONTROL_SAMPLE);
        if (cellControl == null)
            throw new DilutionCurve.FitFailedException("Invalid plate template: no cell control well group was found.");
        WellData virusControl = _plate.getWellGroup(WellGroup.Type.CONTROL, NabManager.VIRUS_CONTROL_SAMPLE);
        if (virusControl == null)
            throw new DilutionCurve.FitFailedException("Invalid plate template: no virus control well group was found.");
        double controlRange = virusControl.getMean() - cellControl.getMean();
        double cellControlMean = cellControl.getMean();
        if (data.getMean() < cellControlMean)
            return 1.0;
        else
            return 1 - (data.getMean() - cellControlMean) / controlRange;
    }

    public double getVirusControlMean()
    {
        WellData virusControl = _plate.getWellGroup(WellGroup.Type.CONTROL, NabManager.VIRUS_CONTROL_SAMPLE);
        return virusControl.getMean();
    }

    public double getCellControlMean()
    {
        WellData cellControl = _plate.getWellGroup(WellGroup.Type.CONTROL, NabManager.CELL_CONTROL_SAMPLE);
        return cellControl.getMean();
    }

    public double getVirusControlPlusMinus()
    {
        WellData virusControl = _plate.getWellGroup(WellGroup.Type.CONTROL, NabManager.VIRUS_CONTROL_SAMPLE);
        double virusControlMean = virusControl.getMean();
        double virusControlStdDev = virusControl.getStdDev();
        return virusControlStdDev / virusControlMean;
    }

    public double getCellControlPlusMinus()
    {
        WellData cellControl = _plate.getWellGroup(WellGroup.Type.CONTROL, NabManager.CELL_CONTROL_SAMPLE);
        double cellControlMean = cellControl.getMean();
        double cellControlStdDev = cellControl.getStdDev();
        return cellControlStdDev / cellControlMean;
    }

    public Date getExperimentDate()
    {
        return (Date) _plate.getProperty(NabManager.PlateProperty.ExperimentDate.name());
    }

    public String getExperimentId()
    {
        return (String) _plate.getProperty(NabManager.PlateProperty.ExperimentId.name());
    }

    public String getExperimentPerformer()
    {
        return (String) _plate.getProperty(NabManager.PlateProperty.ExperimentPerformer.name());
    }

    public String getFileId()
    {
        return (String) _plate.getProperty(NabManager.PlateProperty.FileId.name());
    }

    public String getHostCell()
    {
        return (String) _plate.getProperty(NabManager.PlateProperty.HostCell.name());
    }

    public String getIncubationTime()
    {
        return (String) _plate.getProperty(NabManager.PlateProperty.IncubationTime.name());
    }

    public String getPlateNumber()
    {
        return (String) _plate.getProperty(NabManager.PlateProperty.PlateNumber.name());
    }

    public String getStudyName()
    {
        return (String) _plate.getProperty(NabManager.PlateProperty.StudyName.name());
    }

    public String getVirusId()
    {
        return (String) _plate.getProperty(NabManager.PlateProperty.VirusId.name());
    }

    public String getVirusName()
    {
        return (String) _plate.getProperty(NabManager.PlateProperty.VirusName.name());
    }

    public Map<Integer, String> getCutoffFormats()
    {
        return _cutoffFormats;
    }

    public void setCutoffFormats(Map<Integer, String> cutoffFormats)
    {
        _cutoffFormats = cutoffFormats;
    }

    public void setWellGroupMaterialMapping(Map<WellGroup, ExpMaterial> wellGroupMaterialMapping)
    {
        _wellGroupMaterialMapping = wellGroupMaterialMapping;
    }

    public ExpMaterial getMaterial(WellGroup wellgroup)
    {
        return _wellGroupMaterialMapping.get(wellgroup);
    }

    public File getDataFile()
    {
        return _dataFile;
    }

    public void setDataFile(File dataFile)
    {
        _dataFile = dataFile;
    }

    public boolean isLockAxes()
    {
        return _lockAxes;
    }

    public void setLockAxes(boolean lockAxes)
    {
        _lockAxes = lockAxes;
    }

    public DilutionCurve.FitType getCurveFitType()
    {
        return _curveFitType;
    }
}
