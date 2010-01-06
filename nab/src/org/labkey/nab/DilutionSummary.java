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

import org.labkey.api.study.WellGroup;
import org.labkey.api.study.WellData;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.labkey.api.study.DilutionCurve;

/**
 * User: brittp
 * Date: Jun 4, 2006
 * Time: 5:48:15 PM
 */
public class DilutionSummary implements Serializable
{
    private WellGroup _sampleGroup;
    private Map<DilutionCurve.FitType, DilutionCurve> _dilutionCurve = new HashMap<DilutionCurve.FitType, DilutionCurve>() ;
    private Luc5Assay _assay;
    private String _lsid;
    private DilutionCurve.FitType _curveFitType;

    public DilutionSummary(Luc5Assay assay, WellGroup sampleGroup, String lsid, DilutionCurve.FitType curveFitType)
    {
        assert sampleGroup!= null : "sampleGroup cannot be null";
        assert sampleGroup.getPlate() != null : "sampleGroup must have a plate.";
        assert assay != null : "assay cannot be null";
        _curveFitType = curveFitType;
        _sampleGroup = sampleGroup;
        _assay = assay;
        _lsid = lsid;
    }

    public double getDilution(WellData data)
    {
        return data.getDilution();
    }

    public double getCount(WellData data)
    {
        return data.getMean();
    }

    public double getStdDev(WellData data)
    {
        return data.getStdDev();
    }

    public String getSampleId()
    {
        return (String) _sampleGroup.getProperty(NabManager.SampleProperty.SampleId.name());
    }

    public String getSampleDescription()
    {
        return (String) _sampleGroup.getProperty(NabManager.SampleProperty.SampleDescription.name());
    }

    public double getPercent(WellData data) throws DilutionCurve.FitFailedException
    {
        return _assay.getPercent(_sampleGroup, data);
    }

    private DilutionCurve getDilutionCurve(DilutionCurve.FitType type) throws DilutionCurve.FitFailedException
    {
        if (!_dilutionCurve.containsKey(type))
        {
            _dilutionCurve.put(type, _sampleGroup.getDilutionCurve(_assay, getMethod() == SampleInfo.Method.Dilution, type));
        }
        return _dilutionCurve.get(type);
    }

    public double getPlusMinus(WellData data) throws DilutionCurve.FitFailedException
    {
        if (getPercent(data) == 0)
            return 0;
        else
            return getStdDev(data) / _assay.getControlRange();
    }

    public List<WellData> getWellData()
    {
        return _sampleGroup.getWellData(true);
    }

    public double getInitialDilution()
    {
        return (Double) _sampleGroup.getProperty(NabManager.SampleProperty.InitialDilution.name());
    }

    public double getFactor()
    {
        return (Double) _sampleGroup.getProperty(NabManager.SampleProperty.Factor.name());
    }

    public SampleInfo.Method getMethod()
    {
        String name = (String) _sampleGroup.getProperty(NabManager.SampleProperty.Method.name());
        return SampleInfo.Method.valueOf(name);
    }

    public double getCutoffDilution(double cutoff, DilutionCurve.FitType type) throws DilutionCurve.FitFailedException
    {
        return getDilutionCurve(type).getCutoffDilution(cutoff);
    }

    public double getInterpolatedCutoffDilution(double cutoff, DilutionCurve.FitType type) throws DilutionCurve.FitFailedException
    {
        return getDilutionCurve(type).getInterpolatedCutoffDilution(cutoff);
    }

    public DilutionCurve.DoublePoint[] getCurve() throws DilutionCurve.FitFailedException
    {
        return getDilutionCurve(_curveFitType).getCurve();
    }

    public double getFitError() throws DilutionCurve.FitFailedException
    {
        return getDilutionCurve(_curveFitType).getFitError();
    }

    public double getMinDilution(DilutionCurve.FitType type) throws DilutionCurve.FitFailedException
    {
        return getDilutionCurve(type).getMinDilution();
    }

    public double getMaxDilution(DilutionCurve.FitType type) throws DilutionCurve.FitFailedException
    {
        return getDilutionCurve(type).getMaxDilution();
    }

    public String getLSID()
    {
        return _lsid;
    }

    public Luc5Assay getAssay()
    {
        return _assay;
    }

    public WellGroup getWellGroup()
    {
        return _sampleGroup;
    }

    public DilutionCurve.Parameters getCurveParameters(DilutionCurve.FitType type) throws DilutionCurve.FitFailedException
    {
        return getDilutionCurve(type).getParameters();
    }

    public double getAUC(DilutionCurve.FitType type) throws DilutionCurve.FitFailedException
    {
        return getDilutionCurve(type).calculateAUC();
    }

    public double getAUC() throws DilutionCurve.FitFailedException
    {
        return getDilutionCurve(_assay.getRenderedCurveFitType()).calculateAUC();
    }

}
