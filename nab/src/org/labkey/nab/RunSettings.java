package org.labkey.nab;

import java.io.Serializable;

/**
 * User: brittp
 * Date: Aug 31, 2006
 * Time: 8:05:57 PM
 */
public class RunSettings implements Serializable
{
    public static final int MAX_CUTOFF_OPTIONS = 3;
    private static final long serialVersionUID = -8338877129594854954L;
    private boolean _inferFromFile;
    private boolean _sameInitialValue;
    private boolean _sameMethod;
    private boolean _sameFactor;
    private SafeTextConverter.DoubleConverter _slope = new SafeTextConverter.DoubleConverter(new Double(-1));
    private boolean _autoSlope;
    private boolean _endpointsOptional;
    private SafeTextConverter.PercentConverter[] _cutoffs = new SafeTextConverter.PercentConverter[MAX_CUTOFF_OPTIONS];

    public RunSettings()
    {
        this(false);
    }

    /**
     * This is a hacky constructor to handle the fact that HTML posts simply exclude
     * unchecked checkboxes.  Since I want this form to provide the default values
     * for the HTML page, it's necessary to differentiate between the case of a reshow
     * after a post containing unchecked boxes and the case of initially showing the page.
     * In both cases, no setter will ever be called for the boolean parameters, but I want
     * to return different values.  Note that this is only a problem for checkboxes that
     * should default to true.
     *
     * @param returnDefaultForUnsetBools
     */
    public RunSettings(boolean returnDefaultForUnsetBools)
    {
        _inferFromFile = returnDefaultForUnsetBools;
        _sameInitialValue = returnDefaultForUnsetBools;
        _sameMethod = returnDefaultForUnsetBools;
        _sameFactor = returnDefaultForUnsetBools;
        _autoSlope = returnDefaultForUnsetBools;

        _cutoffs[0] = new SafeTextConverter.PercentConverter(50);
        _cutoffs[1] = new SafeTextConverter.PercentConverter(80);
        for (int i = 2; i < MAX_CUTOFF_OPTIONS; i++)
            _cutoffs[i] = new SafeTextConverter.PercentConverter(null);
    }

    public boolean isInferFromFile()
    {
        return _inferFromFile;
    }

    public void setInferFromFile(boolean inferFromFile)
    {
        _inferFromFile = inferFromFile;
    }


    public boolean isSameMethod()
    {
        return _sameMethod;
    }

    public void setSameMethod(boolean sameMethod)
    {
        _sameMethod = sameMethod;
    }

    public boolean isSameFactor()
    {
        return _sameFactor;
    }

    public void setSameFactor(boolean sameFactor)
    {
        _sameFactor = sameFactor;
    }

    public boolean isSameInitialValue()
    {
        return _sameInitialValue;
    }

    public void setSameInitialValue(boolean sameInitialValue)
    {
        _sameInitialValue = sameInitialValue;
    }

    public String getSlopeText()
    {
        return _slope.getText();
    }

    public void setSlopeText(String slopeText)
    {
        _slope.setText(slopeText);
    }

    public Double getSlope()
    {
        return _slope.getValue();
    }

    public void setSlope(double slope)
    {
        _slope.setValue(slope);
    }

    public boolean isAutoSlope()
    {
        return _autoSlope;
    }

    public void setAutoSlope(boolean autoSlope)
    {
        _autoSlope = autoSlope;
    }

    public boolean isEndpointsOptional()
    {
        return _endpointsOptional;
    }

    public void setEndpointsOptional(boolean endpointsOptional)
    {
        _endpointsOptional = endpointsOptional;
    }

    public SafeTextConverter.PercentConverter[] getCutoffs()
    {
        return _cutoffs;
    }

    public void setCutoffs(SafeTextConverter.PercentConverter[] cutoffs)
    {
        _cutoffs = cutoffs;
    }
}
