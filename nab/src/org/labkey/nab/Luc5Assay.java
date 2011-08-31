/*
 * Copyright (c) 2006-2011 LabKey Corporation
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
import java.util.Map;

import org.labkey.api.assay.dilution.DilutionCurve;
import org.labkey.api.study.*;

/**
 * User: migra
 * Date: Feb 10, 2006
 * Time: 2:15:41 PM
 */
public abstract class Luc5Assay implements Serializable, DilutionCurve.PercentCalculator
{
    private Integer _runRowId;
    private int[] _cutoffs;
    private Map<Integer, String> _cutoffFormats;
    private File _dataFile;
    protected DilutionCurve.FitType _renderedCurveFitType;
    private boolean _lockAxes;

    public Luc5Assay(Integer runRowId, int[] cutoffs, DilutionCurve.FitType renderCurveFitType)
    {
        _renderedCurveFitType = renderCurveFitType;
        _runRowId = runRowId;
        _cutoffs = cutoffs;
    }

    public Luc5Assay(int runRowId, List<Integer> cutoffs, DilutionCurve.FitType renderCurveFitType)
    {
        this(runRowId, toIntArray(cutoffs), renderCurveFitType);
    }

    private static int[] toIntArray(List<Integer> cutoffs)
    {
        int[] cutoffArray = new int[cutoffs.size()];
        for (int i = 0; i < cutoffs.size(); i++)
            cutoffArray[i] = cutoffs.get(i);
        return cutoffArray;
    }

    protected DilutionSummary[] getDilutionSumariesForWellGroups(List<? extends WellGroup> specimenGroups)
    {
        int sampleIndex = 0;
        DilutionSummary[] dilutionSummaries = new DilutionSummary[specimenGroups.size()];
        for (WellGroup specimenGroup : specimenGroups)
            dilutionSummaries[sampleIndex++] = new DilutionSummary(this, specimenGroup, null, _renderedCurveFitType);
        return dilutionSummaries;
    }

    public abstract String getRunName();

    public abstract DilutionSummary[] getSummaries();

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

    public double getPercent(WellGroup group, WellData data) throws DilutionCurve.FitFailedException
    {
        Plate plate = group.getPlate();
        WellData cellControl = plate.getWellGroup(WellGroup.Type.CONTROL, NabManager.CELL_CONTROL_SAMPLE);
        if (cellControl == null)
            throw new DilutionCurve.FitFailedException("Invalid plate template: no cell control well group was found.");
        WellData virusControl = plate.getWellGroup(WellGroup.Type.CONTROL, NabManager.VIRUS_CONTROL_SAMPLE);
        if (virusControl == null)
            throw new DilutionCurve.FitFailedException("Invalid plate template: no virus control well group was found.");
        double controlRange = virusControl.getMean() - cellControl.getMean();
        double cellControlMean = cellControl.getMean();
        if (data.getMean() < cellControlMean)
            return 1.0;
        else
            return 1 - (data.getMean() - cellControlMean) / controlRange;
    }

    public abstract List<Plate> getPlates();

    public double getControlRange(Plate plate)
    {
        WellData cellControl = plate.getWellGroup(WellGroup.Type.CONTROL, NabManager.CELL_CONTROL_SAMPLE);
        WellData virusControl = plate.getWellGroup(WellGroup.Type.CONTROL, NabManager.VIRUS_CONTROL_SAMPLE);
        return virusControl.getMean() - cellControl.getMean();
    }

    public double getVirusControlMean(Plate plate)
    {
        WellData virusControl = plate.getWellGroup(WellGroup.Type.CONTROL, NabManager.VIRUS_CONTROL_SAMPLE);
        return virusControl.getMean();
    }

    public double getCellControlMean(Plate plate)
    {
        WellData cellControl = plate.getWellGroup(WellGroup.Type.CONTROL, NabManager.CELL_CONTROL_SAMPLE);
        return cellControl.getMean();
    }

    public double getVirusControlPlusMinus(Plate plate)
    {
        WellData virusControl = plate.getWellGroup(WellGroup.Type.CONTROL, NabManager.VIRUS_CONTROL_SAMPLE);
        double virusControlMean = virusControl.getMean();
        double virusControlStdDev = virusControl.getStdDev();
        return virusControlStdDev / virusControlMean;
    }

    public double getCellControlPlusMinus(Plate plate)
    {
        WellData cellControl = plate.getWellGroup(WellGroup.Type.CONTROL, NabManager.CELL_CONTROL_SAMPLE);
        double cellControlMean = cellControl.getMean();
        double cellControlStdDev = cellControl.getStdDev();
        return cellControlStdDev / cellControlMean;
    }

    public Map<Integer, String> getCutoffFormats()
    {
        return _cutoffFormats;
    }

    public void setCutoffFormats(Map<Integer, String> cutoffFormats)
    {
        _cutoffFormats = cutoffFormats;
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

    public DilutionCurve.FitType getRenderedCurveFitType()
    {
        return _renderedCurveFitType;
    }
}
