/*
 * Copyright (c) 2017 LabKey Corporation
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
package org.labkey.luminex.model;

import org.labkey.luminex.LuminexManager;

import java.util.Date;

/**
 * Created by iansigmon on 1/27/17.
 */
public class WellExclusion
{
    private String _readerSerialNumber;
    private Date _acquisitionDate;
    private String _analyte;
    private String _description;
    private String _type;
    private String _dilution;


    public String getDilution()
    {
        return _dilution;
    }
    public void setDilution(String dilution)
    {
        _dilution = dilution;
    }

    public String getType()
    {
        return _type;
    }
    public void setType(String type)
    {
        _type = type;
    }

    public String getDescription()
    {
        return _description;
    }
    public void setDescription(String description)
    {
        _description = description;
    }

    public String getAnalyte()
    {
        return _analyte;
    }
    public void setAnalyte(String analyte)
    {
        _analyte = analyte;
    }

    public String getReaderSerialNumber()
    {
        return _readerSerialNumber;
    }
    public void setReaderSerialNumber(String readerSerialNumber)
    {
        _readerSerialNumber = readerSerialNumber;
    }

    public Date getAcquisitionDate()
    {
        return _acquisitionDate;
    }
    public void setAcquisitionDate(Date acquisitionDate)
    {
        _acquisitionDate = acquisitionDate;
    }

    public String getDataFileHeaderKey()
    {
        return LuminexManager.get().getDataFileHeaderKey(getReaderSerialNumber(), getAcquisitionDate());
    }

    public boolean wouldExclude(String dataFileHeaderKey, String analyteName, String description, String type, String dilution)
    {
        return getDataFileHeaderKey().equals(dataFileHeaderKey)
                && getAnalyte().equals(analyteName)
                && isNullOrValue(getDescription(), description)
                && isNullOrValue(getType(), type)
                && isNullOrValue(getDilution(), dilution);
    }

    private boolean isNullOrValue(String expected, String value)
    {
        return expected == null || expected.equals(value);
    }
}
