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

import org.labkey.api.assay.dilution.DilutionCurve;
import org.labkey.api.study.PlateService;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.WellData;

import java.io.Serializable;
import java.util.*;

import org.labkey.api.study.assay.AbstractAssayProvider;

/**
 * User: brittp
 * Date: Jun 4, 2006
 * Time: 5:48:15 PM
 */
public class DilutionSummary implements Serializable
{
    private List<WellGroup> _sampleGroups;
    private WellGroup _firstGroup;
    private Map<DilutionCurve.FitType, DilutionCurve> _dilutionCurve = new HashMap<DilutionCurve.FitType, DilutionCurve>() ;
    private Luc5Assay _assay;
    private String _lsid;
    private DilutionCurve.FitType _curveFitType;
    private NabMaterialKey _materialKey = null;
    public static final NabMaterialKey BLANK_NAB_MATERIAL = new NabMaterialKey("Blank", null, null, null);


    public DilutionSummary(Luc5Assay assay, WellGroup sampleGroup, String lsid, DilutionCurve.FitType curveFitType)
    {
        this(assay, Collections.singletonList(sampleGroup), lsid, curveFitType);
    }

    public DilutionSummary(Luc5Assay assay, List<WellGroup> sampleGroups, String lsid, DilutionCurve.FitType curveFitType)
    {
        assert sampleGroups != null && !sampleGroups.isEmpty() : "sampleGroups cannot be null or empty";
        assert assay != null : "assay cannot be null";
        ensureSameSample(sampleGroups);
        _curveFitType = curveFitType;
        _sampleGroups = sampleGroups;
        _firstGroup = sampleGroups.get(0);
        _assay = assay;
        _lsid = lsid;
    }

    private void ensureSameSample(List<WellGroup> groups)
    {
        String templateName = groups.get(0).getPlate().getName();
        String wellgroupName = groups.get(0).getName();
        for (int groupIndex = 1; groupIndex < groups.size(); groupIndex++)
        {
            if (!templateName.equals(groups.get(groupIndex).getPlate().getName()))
            {
                throw new IllegalStateException("Cannot generate single dilution summary for multiple plate templates: " +
                        templateName + ", " + groups.get(groupIndex).getPlate().getName());
            }

            if (!wellgroupName.equals(groups.get(groupIndex).getName()))
            {
                throw new IllegalStateException("Cannot generate single dilution summary for multiple samples: " +
                        wellgroupName + ", " + groups.get(groupIndex).getName());
            }
        }
    }

    public NabMaterialKey getMaterialKey()
    {
        if (_materialKey == null)
        {
            WellGroup firstWellGroup = getFirstWellGroup();
            String specimenId = (String) firstWellGroup.getProperty(AbstractAssayProvider.SPECIMENID_PROPERTY_NAME);
            Double visitId = (Double) firstWellGroup.getProperty(AbstractAssayProvider.VISITID_PROPERTY_NAME);
            String participantId = (String) firstWellGroup.getProperty(AbstractAssayProvider.PARTICIPANTID_PROPERTY_NAME);
            Date visitDate = (Date) firstWellGroup.getProperty(AbstractAssayProvider.DATE_PROPERTY_NAME);
            _materialKey = new NabMaterialKey(specimenId, participantId, visitId, visitDate);
        }
        return _materialKey;
    }

    public boolean isBlank()
    {
        return BLANK_NAB_MATERIAL.equals(getMaterialKey());
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

    /**
     * @deprecated used only by old NAb assay; always null for new NAb runs
     */
    public String getSampleId()
    {
        return (String) _firstGroup.getProperty(NabManager.SampleProperty.SampleId.name());
    }

    /**
     * @deprecated used only by old NAb assay; always null for new NAb runs
     */
    public String getSampleDescription()
    {
        return (String) _firstGroup.getProperty(NabManager.SampleProperty.SampleDescription.name());
    }

    private Map<WellData, WellGroup> _dataToSample;
    private Map<WellData, WellGroup> getDataToSampleMap()
    {
        if (_dataToSample == null)
        {
            _dataToSample = new HashMap<WellData, WellGroup>();
            for (WellGroup sampleGroup : _sampleGroups)
            {
                for (WellData data : sampleGroup.getWellData(true))
                {
                    _dataToSample.put(data, sampleGroup);
                }
            }
        }
        return _dataToSample;
    }

    public double getPercent(WellData data) throws DilutionCurve.FitFailedException
    {
        return _assay.getPercent(getDataToSampleMap().get(data), data);
    }

    private DilutionCurve getDilutionCurve(DilutionCurve.FitType type) throws DilutionCurve.FitFailedException
    {
        if (!_dilutionCurve.containsKey(type))
        {
            DilutionCurve curve = PlateService.get().getDilutionCurve(_sampleGroups, getMethod() == SampleInfo.Method.Dilution, _assay, type);
            _dilutionCurve.put(type, curve);
        }
        return _dilutionCurve.get(type);
    }

    public double getPlusMinus(WellData data) throws DilutionCurve.FitFailedException
    {
        if (getPercent(data) == 0)
            return 0;
        else
            return getStdDev(data) / _assay.getControlRange(data.getPlate());
    }

    public List<WellData> getWellData()
    {
        List<WellData> data = new ArrayList<WellData>();
        for (WellGroup sampleGroup : _sampleGroups)
            data.addAll(sampleGroup.getWellData(true));
        return data;
    }

    public double getInitialDilution()
    {
        return (Double) _firstGroup.getProperty(NabManager.SampleProperty.InitialDilution.name());
    }

    public double getFactor()
    {
        return (Double) _firstGroup.getProperty(NabManager.SampleProperty.Factor.name());
    }

    public SampleInfo.Method getMethod()
    {
        String name = (String) _firstGroup.getProperty(NabManager.SampleProperty.Method.name());
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

    public List<WellGroup> getWellGroups()
    {
        return _sampleGroups;
    }

    public WellGroup getFirstWellGroup()
    {
        return _firstGroup;
    }

    public DilutionCurve.Parameters getCurveParameters(DilutionCurve.FitType type) throws DilutionCurve.FitFailedException
    {
        return getDilutionCurve(type).getParameters();
    }

    public double getAUC(DilutionCurve.FitType type, DilutionCurve.AUCType calc) throws DilutionCurve.FitFailedException
    {
        return getDilutionCurve(type).calculateAUC(calc);
    }

    public double getAUC() throws DilutionCurve.FitFailedException
    {
        return getDilutionCurve(_assay.getRenderedCurveFitType()).calculateAUC(DilutionCurve.AUCType.NORMAL);
    }

}
