/*
 * Copyright (c) 2004-2016 Fred Hutchinson Cancer Research Center
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

import org.apache.log4j.Logger;

import java.io.Serializable;

public class MS2Fraction implements Serializable
{
    protected int runId;
    protected int fractionId;
    protected String description;
    protected String fileName;
    protected Float hydroB0;
    protected Float hydroB1;
    protected Float hydroR2;
    protected Float hydroSigma;
    private String _mzXmlURL;
    private String _pepXmlDataLSID;
    private Integer _scanCount;
    private Integer _ms1ScanCount;
    private Integer _ms2ScanCount;
    private Integer _ms3ScanCount;
    private Integer _ms4ScanCount;

    public MS2Fraction()
    {
    }


    public String toString()
    {
        return getFraction() + " " + getRun() + " " + getDescription() + " " + getFileName();
    }


    public int getFraction()
    {
        return fractionId;
    }


    public void setFraction(int fractionId)
    {
        this.fractionId = fractionId;
    }


    public int getRun()
    {
        return runId;
    }


    public void setRun(int runId)
    {
        this.runId = runId;
    }


    public String getDescription()
    {
        return description;
    }


    public void setDescription(String description)
    {
        this.description = description;
    }


    public String getFileName()
    {
        return fileName;
    }


    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }


    public Float getHydroB0()
    {
        return this.hydroB0;
    }


    public void setHydroB0(Float hydroB0)
    {
        this.hydroB0 = hydroB0;
    }


    public Float getHydroB1()
    {
        return this.hydroB1;
    }


    public void setHydroB1(Float hydroB1)
    {
        this.hydroB1 = hydroB1;
    }


    public Float getHydroR2()
    {
        return this.hydroR2;
    }


    public void setHydroR2(Float hydroR2)
    {
        this.hydroR2 = hydroR2;
    }


    public Float getHydroSigma()
    {
        return this.hydroSigma;
    }


    public void setHydroSigma(Float hydroSigma)
    {
        this.hydroSigma = hydroSigma;
    }


    public boolean wasloadedFromGzFile()
    {
        return endsWithIgnoreCase(fileName, ".tgz") || endsWithIgnoreCase(fileName, ".tar.gz");
    }


    private boolean endsWithIgnoreCase(String s, String ending)
    {
        return s != null && ending.equalsIgnoreCase(s.substring(s.length() - ending.length()));
    }

    public String getMzXmlURL()
    {
        return _mzXmlURL;
    }

    public void setMzXmlURL(String mzXmlURL)
    {
        _mzXmlURL = mzXmlURL;
    }

    public Integer getScanCount()
    {
        return _scanCount;
    }

    public void setScanCount(Integer scanCount)
    {
        _scanCount = scanCount;
    }

    public Integer getMS1ScanCount()
    {
        return _ms1ScanCount;
    }

    public void setMS1ScanCount(Integer ms1ScanCount)
    {
        _ms1ScanCount = ms1ScanCount;
    }

    public Integer getMS2ScanCount()
    {
        return _ms2ScanCount;
    }

    public void setMS2ScanCount(Integer ms2ScanCount)
    {
        _ms2ScanCount = ms2ScanCount;
    }

    public Integer getMS3ScanCount()
    {
        return _ms3ScanCount;
    }

    public void setMS3ScanCount(Integer ms3ScanCount)
    {
        _ms3ScanCount = ms3ScanCount;
    }

    public Integer getMS4ScanCount()
    {
        return _ms4ScanCount;
    }

    public void setMS4ScanCount(Integer ms4ScanCount)
    {
        _ms4ScanCount = ms4ScanCount;
    }

    public String getPepXmlDataLSID()
    {
        return _pepXmlDataLSID;
    }

    public void setPepXmlDataLSID(String pepXmlDataLSID)
    {
        _pepXmlDataLSID = pepXmlDataLSID;
    }
}
