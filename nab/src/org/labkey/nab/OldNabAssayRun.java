package org.labkey.nab;

import org.labkey.api.assay.dilution.DilutionCurve;
import org.labkey.api.study.Plate;
import org.labkey.api.study.WellGroup;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2010-2011 LabKey Corporation
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p/>
 * <p/>
 * User: brittp
 * Date: Sep 2, 2010 10:42:39 AM
 */
public class OldNabAssayRun extends Luc5Assay
{

    protected Plate _plate;
    private DilutionSummary[] _dilutionSummaries;

    public OldNabAssayRun(Plate plate, int[] cutoffs, DilutionCurve.FitType renderCurveFitType)
    {
        super(plate.getRowId(), cutoffs, renderCurveFitType);
        _plate = plate;
        List<? extends WellGroup> specimenGroups = _plate.getWellGroups(WellGroup.Type.SPECIMEN);
        _dilutionSummaries = getDilutionSumariesForWellGroups(specimenGroups);
    }

    public Plate getPlate()
    {
        return _plate;
    }

    @Override
    public DilutionSummary[] getSummaries()
    {
        return _dilutionSummaries;
    }

    @Override
    public List<Plate> getPlates()
    {
        return Collections.singletonList(_plate);
    }

    public String getName()
    {
        return (String) _plate.getProperty(OldNabManager.PlateProperty.DataFile.name());
    }

    public Date getExperimentDate()
    {
        return (Date) _plate.getProperty(OldNabManager.PlateProperty.ExperimentDate.name());
    }

    public String getExperimentId()
    {
        return (String) _plate.getProperty(OldNabManager.PlateProperty.ExperimentId.name());
    }

    public String getExperimentPerformer()
    {
        return (String) _plate.getProperty(OldNabManager.PlateProperty.ExperimentPerformer.name());
    }

    public String getFileId()
    {
        return (String) _plate.getProperty(OldNabManager.PlateProperty.FileId.name());
    }

    public String getHostCell()
    {
        return (String) _plate.getProperty(OldNabManager.PlateProperty.HostCell.name());
    }

    public String getIncubationTime()
    {
        return (String) _plate.getProperty(OldNabManager.PlateProperty.IncubationTime.name());
    }

    public String getPlateNumber()
    {
        return (String) _plate.getProperty(OldNabManager.PlateProperty.PlateNumber.name());
    }

    public String getStudyName()
    {
        return (String) _plate.getProperty(OldNabManager.PlateProperty.StudyName.name());
    }

    public String getVirusId()
    {
        return (String) _plate.getProperty(OldNabManager.PlateProperty.VirusId.name());
    }

    public String getVirusName()
    {
        return (String) _plate.getProperty(OldNabManager.PlateProperty.VirusName.name());
    }

    @Override
    public String getRunName()
    {
        return (String) _plate.getProperty(OldNabManager.PlateProperty.DataFile.name());
    }

    public double getControlRange()
    {
        return super.getControlRange(_plate);
    }

    public double getVirusControlMean()
    {
        return super.getVirusControlMean(_plate);
    }

    public double getCellControlMean()
    {
        return super.getCellControlMean(_plate);
    }

    public double getVirusControlPlusMinus()
    {
        return super.getVirusControlPlusMinus(_plate);
    }

    public double getCellControlPlusMinus()
    {
        return super.getCellControlPlusMinus(_plate);
    }

}
