/*
 * Copyright (c) 2006-2013 LabKey Corporation
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

package org.labkey.ms2;

import org.labkey.api.util.NetworkDrive;
import org.labkey.ms2.reader.RelativeQuantAnalysisSummary;
import org.labkey.ms2.reader.RandomAccessMzxmlIteratorFactory;
import org.labkey.ms2.reader.RandomAccessMzxmlIterator;
import org.labkey.ms2.reader.SimpleScan;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: jeckels
 * Date: Mar 21, 2006
 */
public class PeptideQuantitation
{
    public static final int SCAN_RANGE = 25;
    public static final float DEFAULT_MASS_TOLERANCE = 1.0f;
    private static final float PROTON_MASS = (float)MS2Peptide.pMass;
    private static final DecimalFormat RATIO_FORMAT = new DecimalFormat();

    static
    {
        RATIO_FORMAT.setGroupingUsed(false);
        RATIO_FORMAT.setMaximumFractionDigits(2);
        RATIO_FORMAT.setMinimumFractionDigits(2);
    }

    private long _peptideId;
    private int _quantId;
    private int _lightFirstScan;
    private int _lightLastScan;
    private float _lightMass;
    private int _heavyFirstScan;
    private int _heavyLastScan;
    private float _heavyMass;
    private String _ratio;
    private String _heavy2LightRatio;
    private float _lightArea;
    private float _heavyArea;
    private float _decimalRatio;
    private Boolean _invalidated;

    private List<ScanInfo>[] _heavyProfiles;
    private List<ScanInfo>[] _lightProfiles;
    private boolean _noScansFound = true;

    private MS2Fraction _fraction;
    public static final int MAX_CHARGE = 10;

    public long getPeptideId()
    {
        return _peptideId;
    }

    public void setPeptideId(long peptideId)
    {
        _peptideId = peptideId;
    }

    public boolean isNoScansFound()
    {
        return _noScansFound;
    }

    public int getQuantId()
    {
        return _quantId;
    }

    public void setQuantId(int quantId)
    {
        _quantId = quantId;
    }

    public int getLightFirstScan()
    {
        return _lightFirstScan;
    }

    public void setLightFirstScan(int lightFirstScan)
    {
        _lightFirstScan = lightFirstScan;
    }

    public int getLightLastScan()
    {
        return _lightLastScan;
    }

    public void setLightLastScan(int lightLastScan)
    {
        _lightLastScan = lightLastScan;
    }

    public float getLightMass()
    {
        return _lightMass;
    }

    public void setLightMass(float lightMass)
    {
        _lightMass = lightMass;
    }

    public int getHeavyFirstScan()
    {
        return _heavyFirstScan;
    }

    public void setHeavyFirstScan(int heavyFirstScan)
    {
        _heavyFirstScan = heavyFirstScan;
    }

    public int getHeavyLastScan()
    {
        return _heavyLastScan;
    }

    public void setHeavyLastScan(int heavyLastScan)
    {
        _heavyLastScan = heavyLastScan;
    }

    public float getHeavyMass()
    {
        return _heavyMass;
    }

    public void setHeavyMass(float heavyMass)
    {
        _heavyMass = heavyMass;
    }

    public String getRatio()
    {
        if (null == _ratio)
            resetRatioStrings();
        return _ratio;
    }

    public void setRatio(String ratio)
    {
        _ratio = ratio;
    }

    public String getHeavy2LightRatio()
    {
        if (null == _heavy2LightRatio)
            resetRatioStrings();
        return _heavy2LightRatio;
    }

    public void setHeavy2LightRatio(String heavy2LightRatio)
    {
        _heavy2LightRatio = heavy2LightRatio;
    }

    public float getLightArea()
    {
        return _lightArea;
    }

    public void setLightArea(float lightArea)
    {
        _lightArea = lightArea;
    }

    public float getHeavyArea()
    {
        return _heavyArea;
    }

    public void setHeavyArea(float heavyArea)
    {
        _heavyArea = heavyArea;
    }

    public float getDecimalRatio()
    {
        return _decimalRatio;
    }

    public void setDecimalRatio(float decimalRatio)
    {
        _decimalRatio = decimalRatio;
    }

    public List<ScanInfo> getHeavyElutionProfile(int charge) throws IOException
    {
        if (charge < 1 || charge > MAX_CHARGE)
        {
            throw new IllegalArgumentException("Illegal charge state: " + charge);
        }
        if (_heavyProfiles == null)
        {
            fetchElutionProfiles();
        }
        if (_heavyProfiles == null)
        {
            return Collections.emptyList();
        }
        return _heavyProfiles[charge - 1];
    }

    public List<ScanInfo> getLightElutionProfile(int charge) throws IOException
    {
        if (charge < 1 || charge > MAX_CHARGE)
        {
            throw new IllegalArgumentException("Illegal charge state: " + charge);
        }
        if (_lightProfiles == null)
        {
            fetchElutionProfiles();
        }
        if (_lightProfiles == null)
        {
            return Collections.emptyList();
        }
        return _lightProfiles[charge - 1];
    }

    private MS2Fraction getFraction()
    {
        if (_fraction != null)
        {
            return _fraction;
        }

        MS2Peptide peptide = MS2Manager.getPeptide(_peptideId);
        if (peptide == null)
        {
            return null;
        }

        _fraction = MS2Manager.getFraction(peptide.getFraction());

        return _fraction;
    }

    public File findScanFile() throws IOException
    {
        MS2Fraction fraction = getFraction();
        if (fraction == null || fraction.getMzXmlURL() == null)
        {
            return null;
        }

        File mzXmlFile;
        try
        {
            URI uri = new URI(fraction.getMzXmlURL());
            mzXmlFile = new File(uri);
            if (!NetworkDrive.exists(mzXmlFile))
            {
                return null;
            }
        }
        catch (URISyntaxException e)
        {
            throw (IOException)new IOException("Unable to parse URI " + fraction.getMzXmlURL()).initCause(e);
        }
        return mzXmlFile;
    }

    private void fetchElutionProfiles() throws IOException
    {
        MS2Fraction fraction = getFraction();
        if (fraction == null)
        {
            return;
        }

        File mzXmlFile = findScanFile();
        if (mzXmlFile == null)
        {
            return;
        }

        // Try to get the mass tolerance associated with this quantitation run
        RelativeQuantAnalysisSummary summary = MS2Manager.getQuantSummaryForRun(fraction.getRun());
        float massTol = (null == summary || summary.getMassTol() <= 0f) ? DEFAULT_MASS_TOLERANCE : summary.getMassTol();


        int minScan = getMinDisplayScan();
        int maxScan = getMaxDisplayScan();
        _heavyProfiles = new List[MAX_CHARGE];
        _lightProfiles = new List[MAX_CHARGE];
        for (int i = 0; i < MAX_CHARGE; i++)
        {
            _heavyProfiles[i] = new ArrayList<>();
            _lightProfiles[i] = new ArrayList<>();
        }

        RandomAccessMzxmlIterator iterator = null;
        try
        {
            iterator = RandomAccessMzxmlIteratorFactory.newIterator(mzXmlFile, 1, minScan);

            while (iterator.hasNext())
            {
                _noScansFound = false;
                SimpleScan scan = iterator.next();
                if (scan.getScan() > maxScan)
                {
                    // Scans are in ascending order in the file, so break out as soon as we go past the scans
                    // that we care about
                    break;
                }

                for (int charge = 1; charge <= MAX_CHARGE; charge++)
                {
                    checkScan(_lightMass, charge, _lightProfiles[charge - 1], scan, massTol);
                    checkScan(_heavyMass, charge, _heavyProfiles[charge - 1], scan, massTol);
                }
            }
        }
        finally
        {
            if (iterator != null)
            {
                iterator.close();
            }
        }
    }

    public boolean includeInProteinCalc()
    {
        return _invalidated == null || !_invalidated.booleanValue();
    }

    public Boolean getInvalidated()
    {
        return _invalidated;
    }

    public void setInvalidated(Boolean invalidated)
    {
        // So that we don't have to do a massively expensive upgrade script, use null
        // instead of false for the invalidated column
        if (invalidated != null && !invalidated.booleanValue())
        {
            invalidated = null;
        }
        _invalidated = invalidated;
    }

    public int getMaxDisplayScan()
    {
        return Math.max(_lightLastScan, _heavyLastScan) + SCAN_RANGE;
    }

    public int getMinDisplayScan()
    {
        return Math.max(0, Math.min(_lightFirstScan, _heavyFirstScan) - SCAN_RANGE);
    }

    /**
     * Extends the peak list representing the single-ion chromatogram around the given mass.
     * Note that mass is assumed to be singly-protonated and that if mutliple peaks are found
     * within the window the most intense is used.
     */
    private void checkScan(float mass, int charge, List<ScanInfo> profile, SimpleScan scan, float massTol)
        throws IOException
    {
        float minMZ = (mass - PROTON_MASS) / charge + PROTON_MASS - massTol;
        float maxMZ = (mass - PROTON_MASS) / charge + PROTON_MASS + massTol;

        float maxIntensity = 0;
        float[][] data = scan.getData();
        for (int i = 0; i < data[0].length; i++)
        {
            if (data[0][i] >= minMZ && data[0][i] <= maxMZ)
            {
                maxIntensity = Math.max(maxIntensity, data[1][i]);
            }
        }
        if (maxIntensity > 0)
        {
            profile.add(new ScanInfo(scan.getScan(), maxIntensity));
        }
    }

    public boolean resetRanges(int lightFirstScan, int lightLastScan, int heavyFirstScan, int heavyLastScan, int charge) throws SQLException, IOException
    {
        if (lightFirstScan > lightLastScan || heavyFirstScan > heavyLastScan)
        {
            return false;
        }

        _lightFirstScan = lightFirstScan;
        _lightLastScan = lightLastScan;
        _heavyFirstScan = heavyFirstScan;
        _heavyLastScan = heavyLastScan;

        _heavyProfiles = null;
        _lightProfiles = null;

        _lightArea = calculateArea(getLightElutionProfile(charge), _lightFirstScan, _lightLastScan);
        _heavyArea = calculateArea(getHeavyElutionProfile(charge), _heavyFirstScan, _heavyLastScan);
        if (_heavyArea == 0)
        {
            _decimalRatio = Float.POSITIVE_INFINITY;
            return false;
        }
        else
        {
            _decimalRatio = ((int)(_lightArea / _heavyArea * 100)) / 100f;
        }

        resetRatioStrings();
        return true;
    }

    private void resetRatioStrings()
    {
        if (_lightArea == _heavyArea)
        {
            _ratio = _heavy2LightRatio = "1:1";
        }
        else if (_lightArea > _heavyArea)
        {
            _ratio = "1:" + RATIO_FORMAT.format(_heavyArea / _lightArea);
            _heavy2LightRatio = "1:" + RATIO_FORMAT.format(_lightArea / _heavyArea);
        }
        else
        {
            _ratio = RATIO_FORMAT.format(_lightArea / _heavyArea) + ":1";
            _heavy2LightRatio = RATIO_FORMAT.format(_heavyArea / _lightArea) + ":1";
        }
    }

    private float calculateArea(List<ScanInfo> scanInfos, int firstScan, int lastScan)
    {
        float area = 0f;
        for (ScanInfo scanInfo : scanInfos)
        {
            if (scanInfo.getScan() >= firstScan && scanInfo.getScan() <= lastScan)
            {
                area += scanInfo.getIntensity();
            }
        }
        if (area != 0)
        {
            double x = Math.pow(10, Math.round(Math.log10(area)) - 4);
            area = (float)(Math.round(area / x) * x);
        }
        return area;
    }

    public static class ScanInfo
    {
        private final int _scan;
        private final float _intensity;

        public ScanInfo(int scan, float intensity)
        {
            _scan = scan;
            _intensity = intensity;
        }

        public int getScan()
        {
            return _scan;
        }

        public float getIntensity()
        {
            return _intensity;
        }
    }
}
