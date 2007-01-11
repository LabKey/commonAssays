package cpas.assays.nab;

import java.io.Serializable;
import java.util.List;
import java.util.Date;

import org.fhcrc.cpas.study.*;
import org.fhcrc.cpas.study.DilutionCurve;

/**
 * Copyright (C) 2004 Fred Hutchinson Cancer Research Center. All Rights Reserved.
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

    public Luc5Assay(Plate plate, int[] cutoffs)
    {
        assert plate != null : "plate cannot be null";
        _runRowId = plate.getRowId();
        _cutoffs = cutoffs;
        _plate = plate;
        init();
    }

    private void init()
    {
        List<? extends WellGroup> specimenGroups = _plate.getWellGroups(WellGroup.Type.SPECIMEN);
        int sampleIndex = 0;
        _dilutionSummaries = new DilutionSummary[specimenGroups.size()];
        for (WellGroup specimenGroup : specimenGroups)
            _dilutionSummaries[sampleIndex++] = new DilutionSummary(this, specimenGroup, null);
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

    public double getPercent(WellGroup group, WellData data)
    {
        WellData cellControl = _plate.getWellGroup(WellGroup.Type.CONTROL, NabManager.CELL_CONTROL_SAMPLE);
        WellData virusControl = _plate.getWellGroup(WellGroup.Type.CONTROL, NabManager.VIRUS_CONTROL_SAMPLE);
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
}
