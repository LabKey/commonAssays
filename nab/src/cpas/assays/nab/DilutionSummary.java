package cpas.assays.nab;

import org.fhcrc.cpas.study.WellGroup;
import org.fhcrc.cpas.study.WellData;

import java.io.Serializable;
import java.util.List;

import org.fhcrc.cpas.study.DilutionCurve;

/**
 * User: brittp
 * Date: Jun 4, 2006
 * Time: 5:48:15 PM
 */
public class DilutionSummary implements Serializable
{
    private WellGroup _sampleGroup;
    private DilutionCurve _dilutionCurve;
    private Luc5Assay _assay;
    private String _lsid;

    public DilutionSummary(Luc5Assay assay, WellGroup sampleGroup, String lsid)
    {
        assert sampleGroup!= null : "sampleGroup cannot be null";
        assert sampleGroup.getPlate() != null : "sampleGroup must have a plate.";
        assert assay != null : "assay cannot be null";
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

    public Double getFixedSlope()
    {
        return (Double) _sampleGroup.getProperty(NabManager.SampleProperty.Slope.name());
    }

    public double getPercent(WellData data)
    {
        return _assay.getPercent(_sampleGroup, data);
    }

    private DilutionCurve getDilutionCurve()
    {
        if (_dilutionCurve == null)
        {
            if (getFixedSlope() != null)
                _dilutionCurve = _sampleGroup.getDilutionCurve(_assay, getFixedSlope(), isEndpointsOptional());
            else
                _dilutionCurve = _sampleGroup.getDilutionCurve(_assay, isEndpointsOptional());
        }
        return _dilutionCurve;
    }

    public double getPlusMinus(WellData data)
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

    protected boolean isEndpointsOptional()
    {
        Boolean endpointsOptional = (Boolean) _sampleGroup.getProperty(NabManager.SampleProperty.EndpointsOptional.name());
        return endpointsOptional != null && endpointsOptional.booleanValue();
    }

    public double getCutoffDilution(double cutoff)
    {
        return getDilutionCurve().getCutoffDilution(cutoff);
    }

    public double getInterpolatedCutoffDilution(double cutoff)
    {
        return getDilutionCurve().getInterpolatedCutoffDilution(cutoff);
    }

    public DilutionCurve.DoublePoint[] getCurve()
    {
        return getDilutionCurve().getCurve();
    }

    public double getFitError()
    {
        return getDilutionCurve().getFitError();
    }

    public Double getSlope()
    {
        return getDilutionCurve().getSlope();
    }

    public double getMinDilution()
    {
        return getDilutionCurve().getMinDilution();
    }

    public double getMaxDilution()
    {
        return getDilutionCurve().getMaxDilution();
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
}
