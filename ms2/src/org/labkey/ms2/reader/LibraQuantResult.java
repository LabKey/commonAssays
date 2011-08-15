package org.labkey.ms2.reader;

/**
 * User: jeckels
 * Date: Aug 14, 2011
 */
public class LibraQuantResult extends PepXmlAnalysisResultHandler.PepXmlAnalysisResult
{
    private long _peptideId;

    private Double _targetMass1;
    private Double _absoluteMass1;
    private Double _normalized1;
    private Double _targetMass2;
    private Double _absoluteMass2;
    private Double _normalized2;
    private Double _targetMass3;
    private Double _absoluteMass3;
    private Double _normalized3;
    private Double _targetMass4;
    private Double _absoluteMass4;
    private Double _normalized4;
    private Double _targetMass5;
    private Double _absoluteMass5;
    private Double _normalized5;
    private Double _targetMass6;
    private Double _absoluteMass6;
    private Double _normalized6;
    private Double _targetMass7;
    private Double _absoluteMass7;
    private Double _normalized7;
    private Double _targetMass8;
    private Double _absoluteMass8;
    private Double _normalized8;

    public long getPeptideId()
    {
        return _peptideId;
    }

    public void setPeptideId(long peptideId)
    {
        _peptideId = peptideId;
    }

    public Double getTargetMass1()
    {
        return _targetMass1;
    }

    public void setTargetMass1(Double targetMass1)
    {
        _targetMass1 = targetMass1;
    }

    public Double getAbsoluteMass1()
    {
        return _absoluteMass1;
    }

    public void setAbsoluteMass1(Double absoluteMass1)
    {
        _absoluteMass1 = absoluteMass1;
    }

    public Double getNormalized1()
    {
        return _normalized1;
    }

    public void setNormalized1(Double normalized1)
    {
        _normalized1 = normalized1;
    }

    public Double getTargetMass2()
    {
        return _targetMass2;
    }

    public void setTargetMass2(Double targetMass2)
    {
        _targetMass2 = targetMass2;
    }

    public Double getAbsoluteMass2()
    {
        return _absoluteMass2;
    }

    public void setAbsoluteMass2(Double absoluteMass2)
    {
        _absoluteMass2 = absoluteMass2;
    }

    public Double getNormalized2()
    {
        return _normalized2;
    }

    public void setNormalized2(Double normalized2)
    {
        _normalized2 = normalized2;
    }

    public Double getTargetMass3()
    {
        return _targetMass3;
    }

    public void setTargetMass3(Double targetMass3)
    {
        _targetMass3 = targetMass3;
    }

    public Double getAbsoluteMass3()
    {
        return _absoluteMass3;
    }

    public void setAbsoluteMass3(Double absoluteMass3)
    {
        _absoluteMass3 = absoluteMass3;
    }

    public Double getNormalized3()
    {
        return _normalized3;
    }

    public void setNormalized3(Double normalized3)
    {
        _normalized3 = normalized3;
    }

    public Double getTargetMass4()
    {
        return _targetMass4;
    }

    public void setTargetMass4(Double targetMass4)
    {
        _targetMass4 = targetMass4;
    }

    public Double getAbsoluteMass4()
    {
        return _absoluteMass4;
    }

    public void setAbsoluteMass4(Double absoluteMass4)
    {
        _absoluteMass4 = absoluteMass4;
    }

    public Double getNormalized4()
    {
        return _normalized4;
    }

    public void setNormalized4(Double normalized4)
    {
        _normalized4 = normalized4;
    }

    public Double getTargetMass5()
    {
        return _targetMass5;
    }

    public void setTargetMass5(Double targetMass5)
    {
        _targetMass5 = targetMass5;
    }

    public Double getAbsoluteMass5()
    {
        return _absoluteMass5;
    }

    public void setAbsoluteMass5(Double absoluteMass5)
    {
        _absoluteMass5 = absoluteMass5;
    }

    public Double getNormalized5()
    {
        return _normalized5;
    }

    public void setNormalized5(Double normalized5)
    {
        _normalized5 = normalized5;
    }

    public Double getTargetMass6()
    {
        return _targetMass6;
    }

    public void setTargetMass6(Double targetMass6)
    {
        _targetMass6 = targetMass6;
    }

    public Double getAbsoluteMass6()
    {
        return _absoluteMass6;
    }

    public void setAbsoluteMass6(Double absoluteMass6)
    {
        _absoluteMass6 = absoluteMass6;
    }

    public Double getNormalized6()
    {
        return _normalized6;
    }

    public void setNormalized6(Double normalized6)
    {
        _normalized6 = normalized6;
    }

    public Double getTargetMass7()
    {
        return _targetMass7;
    }

    public void setTargetMass7(Double targetMass7)
    {
        _targetMass7 = targetMass7;
    }

    public Double getAbsoluteMass7()
    {
        return _absoluteMass7;
    }

    public void setAbsoluteMass7(Double absoluteMass7)
    {
        _absoluteMass7 = absoluteMass7;
    }

    public Double getNormalized7()
    {
        return _normalized7;
    }

    public void setNormalized7(Double normalized7)
    {
        _normalized7 = normalized7;
    }

    public Double getTargetMass8()
    {
        return _targetMass8;
    }

    public void setTargetMass8(Double targetMass8)
    {
        _targetMass8 = targetMass8;
    }

    public Double getAbsoluteMass8()
    {
        return _absoluteMass8;
    }

    public void setAbsoluteMass8(Double absoluteMass8)
    {
        _absoluteMass8 = absoluteMass8;
    }

    public Double getNormalized8()
    {
        return _normalized8;
    }

    public void setNormalized8(Double normalized8)
    {
        _normalized8 = normalized8;
    }

    @Override
    public String getAnalysisType()
    {
        return LibraQuantHandler.ANALYSIS_TYPE;
    }
}
