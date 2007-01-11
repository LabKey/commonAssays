package cpas.assays.nab;

import java.io.Serializable;

/**
 * User: brittp
 * Date: Aug 18, 2006
 * Time: 3:55:25 PM
 */
public class SampleInfo implements Serializable
{
    private static final long serialVersionUID = -8338877129594854955L;

    public enum Method
    {
        Concentration
        {
            public String getAbbreviation()
            {
                return "Conc.";
            }
        },
        Dilution
        {
            public String getAbbreviation()
            {
                return "Dilution";
            }
        };

        public String getFullName()
        {
            return name();
        }

        public abstract String getAbbreviation();
    }

    //ISSUE: LSIDS??
    private String _sampleId;
    private SafeTextConverter.DoubleConverter _initialDilution = new SafeTextConverter.DoubleConverter(new Double(20));
    private SafeTextConverter.DoubleConverter _factor = new SafeTextConverter.DoubleConverter(new Double(3));
    private Double _fixedSlope;
    private boolean _endpointsOptional;
    private String _dilutionSummaryLsid;
    private String _sampleDescription;
    private Method _method;

    public SampleInfo(String sampleId)
    {
        this._sampleId = sampleId;
    }

    public String getSampleId()
    {
        return _sampleId;
    }

    public void setSampleId(String sampleId)
    {
        this._sampleId = sampleId;
    }

    public Double getInitialDilution()
    {
        return _initialDilution.getValue();
    }

    public void setInitialDilution(Double initialDilution)
    {
        _initialDilution.setValue(initialDilution);
    }

    public String getInitialDilutionText()
    {
        return _initialDilution.getText();
    }

    public void setInitialDilutionText(String initialDilutionText)
    {
        _initialDilution.setText(initialDilutionText);
    }

    public Double getFactor()
    {
        return _factor.getValue();
    }

    public void setFactor(Double factor)
    {
        _factor.setValue(factor);
    }

    public String getFactorText()
    {
        return _factor.getText();
    }

    public void setFactorText(String factorText)
    {
        _factor.setText(factorText);
    }

    public Double getFixedSlope()
    {
        return _fixedSlope;
    }

    public void setFixedSlope(Double fixedSlope)
    {
        _fixedSlope = fixedSlope;
    }

    public boolean isEndpointsOptional()
    {
        return _endpointsOptional;
    }

    public void setEndpointsOptional(boolean endpointsOptional)
    {
        _endpointsOptional = endpointsOptional;
    }

    public String getDilutionSummaryLsid()
    {
        return _dilutionSummaryLsid;
    }

    public void setDilutionSummaryLsid(String dilutionSummaryLsid)
    {
        _dilutionSummaryLsid = dilutionSummaryLsid;
    }

    public String getMethodName()
    {
        return _method.name();
    }

    public void setMethodName(String methodName)
    {
        this._method = Method.valueOf(methodName);
    }

    public Method getMethod()
    {
        return _method;
    }

    public String getSampleDescription()
    {
        return _sampleDescription;
    }

    public void setSampleDescription(String sampleDescription)
    {
        _sampleDescription = sampleDescription;
    }
}
