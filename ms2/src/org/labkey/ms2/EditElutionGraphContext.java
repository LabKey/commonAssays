package org.labkey.ms2;

import org.labkey.api.view.ViewURLHelper;

import java.util.List;

/**
 * User: jeckels
* Date: Feb 6, 2007
*/
public class EditElutionGraphContext
{
    private final List<Quantitation.ScanInfo> _lightElutionProfile;
    private final List<Quantitation.ScanInfo> _heavyElutionProfile;
    private final ViewURLHelper _url;
    private final MS2Peptide _peptide;

    public float getMaxLightIntensity()
    {
        return _maxLightIntensity;
    }

    public float getMaxHeavyIntensity()
    {
        return _maxHeavyIntensity;
    }

    private float _maxLightIntensity;
    private float _maxHeavyIntensity;

    public Quantitation getQuantitation()
    {
        return _quantitation;
    }

    private Quantitation _quantitation;

    public EditElutionGraphContext(List<Quantitation.ScanInfo> lightElutionProfile, List<Quantitation.ScanInfo> heavyElutionProfile, Quantitation quant, ViewURLHelper url, MS2Peptide peptide)
    {
        _lightElutionProfile = lightElutionProfile;
        _heavyElutionProfile = heavyElutionProfile;
        _maxLightIntensity = findMaxIntensity(_lightElutionProfile);
        _maxHeavyIntensity = findMaxIntensity(_heavyElutionProfile);
        _quantitation = quant;
        _url = url;
        _peptide = peptide;
    }


    public ViewURLHelper getUrl()
    {
        return _url;
    }

    private float findMaxIntensity(List<Quantitation.ScanInfo> scanInfos)
    {
        float max = 0f;
        for (Quantitation.ScanInfo scanInfo : scanInfos)
        {
            max = Math.max(max, scanInfo.getIntensity());
        }
        return max;
    }

    private Float getProfileValue(int scan, List<Quantitation.ScanInfo> infos)
    {
        for (Quantitation.ScanInfo scanInfo : infos)
        {
            if (scanInfo.getScan() == scan)
            {
                return new Float(scanInfo.getIntensity());
            }
        }
        return null;

    }

    public Float getLightValue(int scan)
    {
        return getProfileValue(scan, _lightElutionProfile);
    }

    public Float getHeavyValue(int scan)
    {
        return getProfileValue(scan, _heavyElutionProfile);
    }

    public List<Quantitation.ScanInfo> getLightElutionProfile()
    {
        return _lightElutionProfile;
    }

    public List<Quantitation.ScanInfo> getHeavyElutionProfile()
    {
        return _heavyElutionProfile;
    }

    public MS2Peptide getPeptide()
    {
        return _peptide;
    }
}
